package fr.hyping.hypingauctions.service;

import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.manager.object.Auction;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

public class AveragePriceService {

    private static AveragePriceService instance;

    private final Map<String, CachedPrice> priceCache = new ConcurrentHashMap<>();

    private static final long CACHE_TTL_MS = 15 * 60 * 1000; // 15 minutes
    private static final int MIN_SAMPLES = 1; // Minimum number of sales required
    private static final int MAX_SAMPLES = 50; // Max number of sales to consider
    private static final long MAX_AGE_MS = 30L * 24 * 60 * 60 * 1000; // 30 days
    private static final int MAX_TRANSACTIONS_PER_PLAYER = 3; // Max txns per player
    private static final int LEGACY_SCAN_LIMIT = 500; // Max legacy docs to scan when item_fingerprint is missing

    private static boolean ENABLED = true;

    private AveragePriceService() {
        HypingAuctions.getInstance()
                .getServer()
                .getGlobalRegionScheduler()
                .runAtFixedRate(
                        HypingAuctions.getInstance(),
                        task -> cleanupExpiredCache(),
                        20 * 60, // delay 1m
                        20 * 60 // period 1m
                );
    }

    public static AveragePriceService getInstance() {
        if (instance == null) {
            instance = new AveragePriceService();
        }
        return instance;
    }

    public CompletableFuture<Integer> calculateAveragePrice(ItemStack item) {
        if (item == null) {
            return CompletableFuture.completedFuture(0);
        }
        // Forced log to verify method call
        HypingAuctions.getInstance().debug(() -> "calculateAveragePrice called for " + item.getType());

        if (!ENABLED) {
            HypingAuctions.getInstance().debug(() -> "AveragePriceService is DISABLED");
            return CompletableFuture.completedFuture(0);
        }

        ItemStack single = item.clone();
        single.setAmount(1);

        String fingerprint = generateItemFingerprint(single);
        HypingAuctions.getInstance().debug(() -> "Fingerprint: " + fingerprint);

        CachedPrice cached = priceCache.get(fingerprint);
        if (cached != null && !cached.isExpired()) {
            HypingAuctions.getInstance().debug(() -> "Cache HIT: " + cached.price);
            return CompletableFuture.completedFuture(cached.price);
        }
        HypingAuctions.getInstance().debug(() -> "Cache MISS, calculating async...");

        final String baseMaterialName = item.getType().name();

        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        HypingAuctions.getInstance().debug(
                                () -> "Starting average price calculation for: " + fingerprint + " baseMaterial: " + baseMaterialName);
                        int perUnit = calculateAveragePriceSync(fingerprint, baseMaterialName);
                        priceCache.put(fingerprint, new CachedPrice(perUnit, System.currentTimeMillis()));
                        HypingAuctions.getInstance().debug(() -> "Finished calculation for " + fingerprint + ": " + perUnit);
                        return perUnit;
                    } catch (Exception e) {
                        HypingAuctions.getInstance()
                                .getLogger()
                                .log(Level.WARNING, "Error calculating average price for item: " + fingerprint, e);
                        return 0;
                    }
                },
                HypingAuctions.getInstance().getAsyncExecutor());
    }

    private int calculateAveragePriceSync(String itemFingerprint, String baseMaterialName) {
        HypingAuctions.getInstance().debug(() -> "Sync calc started for " + itemFingerprint);
        List<SaleData> salesData = getSalesDataFromDatabase(itemFingerprint, baseMaterialName);

        HypingAuctions.getInstance().debug(() -> "DB returned " + salesData.size() + " raw sales");

        if (salesData.isEmpty()) {
            HypingAuctions.getInstance().debug(() -> "No sales found in DB.");
            return 0;
        }

        List<SaleData> processedSales = processSalesData(salesData);

        HypingAuctions.getInstance().debug(() -> "After filtering: " + processedSales.size() + " sales remain");

        if (processedSales.size() < MIN_SAMPLES) {
            HypingAuctions.getInstance()
                    .debug(() -> "Not enough samples (" + processedSales.size() + " < " + MIN_SAMPLES + ")");
            return 0;
        }

        double rawAverage = calculateAverageWithOutlierRemoval(processedSales);
        int roundedAverage = (int) Math.round(rawAverage);

        HypingAuctions.getInstance().debug(() -> "Calculated avg: " + roundedAverage);

        return roundedAverage;
    }

    private List<SaleData> getSalesDataFromDatabase(String itemFingerprint, String baseMaterialName) {
        List<SaleData> salesData = new ArrayList<>();
        try {
            var database = HypingAuctions.getInstance().getDatabase();
            var boughtCollection = database.getBoughtCollection();
            long cutoffTime = System.currentTimeMillis() - MAX_AGE_MS;

            // Time filter: prefer purchase_date when present, else fall back to sale_date
            org.bson.Document timeFilter = new org.bson.Document(
                    "$or",
                    java.util.List.of(
                            new org.bson.Document("purchase_date", new org.bson.Document("$gte", cutoffTime)),
                            new org.bson.Document(
                                    "$and",
                                    java.util.List.of(
                                            new org.bson.Document("purchase_date", new org.bson.Document("$exists", false)),
                                            new org.bson.Document("sale_date", new org.bson.Document("$gte", cutoffTime))))));

            // Build acceptable fingerprint variants
            java.util.List<String> variants = new java.util.ArrayList<>();
            if (itemFingerprint != null && !itemFingerprint.isBlank()) {
                variants.add(itemFingerprint);

                // Only add material name variants for non-enhanced identifiers
                // Enhanced identifiers (HYPING_ITEM, SPAWNER, HYPING_CRATE_KEY, etc.) should be
                // matched exactly
                // This prevents different crate keys from sharing the same average price
                boolean isEnhancedIdentifier = itemFingerprint.startsWith("HYPING_ITEM:")
                        || itemFingerprint.startsWith("SPAWNER:")
                        || itemFingerprint.startsWith("ENCHANTED_BOOK:")
                        || itemFingerprint.startsWith("HYPING_SPAWNER:")
                        || itemFingerprint.startsWith("HYPING_CRATE_KEY:");

                if (!isEnhancedIdentifier && baseMaterialName != null && !baseMaterialName.isBlank()) {
                    variants.add("MATERIAL:" + baseMaterialName);
                    variants.add(baseMaterialName);
                }
            } else if (baseMaterialName != null && !baseMaterialName.isBlank()) {
                // Fallback: if no fingerprint, use material name
                variants.add("MATERIAL:" + baseMaterialName);
                variants.add(baseMaterialName);
            }

            org.bson.Document query = new org.bson.Document("item_fingerprint", new org.bson.Document("$in", variants))
                    .append("$and", java.util.List.of(timeFilter));

            boughtCollection
                    .find(query)
                    .sort(new org.bson.Document("sale_date", -1))
                    .forEach(
                            document -> {
                                try {
                                    Auction auction = Auction.fromDocument(document);
                                    if (auction != null) { // documents in 'bought' are sales, even if buyer_id missing
                                        // Prefer owner_id from document to ensure stable grouping
                                        UUID sellerId = null;
                                        try {
                                            String owner = document.getString("owner_id");
                                            if (owner != null)
                                                sellerId = java.util.UUID.fromString(owner);
                                        } catch (Throwable ignored) {
                                        }
                                        if (sellerId == null && auction.getSeller() != null && auction.getSeller().getPlayer() != null) {
                                            sellerId = auction.getSeller().getPlayer().getUniqueId();
                                        }

                                        // Normalize to per-unit: database stores total price
                                        org.bukkit.inventory.ItemStack it = auction.getItem();
                                        int amt = (it != null && it.getAmount() > 0) ? it.getAmount() : 1;
                                        double perUnit = auction.getPrice() / Math.max(1.0, amt);
                                        if (perUnit <= 0)
                                            return;
                                        salesData.add(new SaleData(perUnit, auction.getSaleDate(), sellerId));
                                    }
                                } catch (Exception ignored) {
                                }
                            });

            org.bson.Document legacyQuery = new org.bson.Document("$and", java.util.List.of(timeFilter,
                    new org.bson.Document("item_fingerprint", new org.bson.Document("$exists", false))));

            boughtCollection
                    .find(legacyQuery)
                    .sort(new org.bson.Document("sale_date", -1))
                    .limit(Math.min(LEGACY_SCAN_LIMIT, 200))
                    .forEach(
                            document -> {
                                try {
                                    Auction auction = Auction.fromDocument(document);
                                    if (auction == null)
                                        return;
                                    org.bukkit.inventory.ItemStack it = auction.getItem();
                                    if (it == null)
                                        return;
                                    org.bukkit.inventory.ItemStack single = it.clone();
                                    single.setAmount(1);
                                    String fp = generateItemFingerprint(single);
                                    boolean matches = false;
                                    if (fp != null && !fp.isBlank()) {
                                        for (String v : variants) {
                                            if (v.equals(fp)) {
                                                matches = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (!matches)
                                        return;

                                    // Prefer owner_id from document for grouping
                                    UUID sellerId = null;
                                    try {
                                        String owner = document.getString("owner_id");
                                        if (owner != null)
                                            sellerId = java.util.UUID.fromString(owner);
                                    } catch (Throwable ignored) {
                                    }

                                    int amt = (it.getAmount() > 0) ? it.getAmount() : 1;
                                    double perUnit = auction.getPrice() / Math.max(1.0, amt);
                                    if (perUnit <= 0)
                                        return;
                                    salesData.add(new SaleData(perUnit, auction.getSaleDate(), sellerId));
                                } catch (Exception ignored) {
                                }
                            });

        } catch (Exception e) {
            HypingAuctions.getInstance()
                    .getLogger()
                    .log(Level.WARNING, "Error querying sales data for: " + itemFingerprint, e);
        }
        return salesData;
    }

    private List<SaleData> processSalesData(List<SaleData> salesData) {
        Map<UUID, List<SaleData>> bySeller = new HashMap<>();
        for (SaleData s : salesData) {
            UUID key = (s.sellerId != null) ? s.sellerId : UUID.randomUUID();
            bySeller.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
        }

        List<SaleData> processed = new ArrayList<>();
        for (List<SaleData> list : bySeller.values()) {
            list.sort((a, b) -> Long.compare(b.saleDate, a.saleDate));
            int limit = Math.min(list.size(), MAX_TRANSACTIONS_PER_PLAYER);
            for (int i = 0; i < limit; i++) {
                processed.add(list.get(i));
            }
        }

        processed.sort((a, b) -> Long.compare(b.saleDate, a.saleDate));
        if (processed.size() > MAX_SAMPLES) {
            return processed.subList(0, MAX_SAMPLES);
        }
        return processed;
    }

    private double calculateAverageWithOutlierRemoval(List<SaleData> salesData) {
        if (salesData.size() < 4) {
            return salesData.stream().mapToDouble(s -> s.price).average().orElse(0.0);
        }

        List<Double> prices = salesData.stream().mapToDouble(s -> s.price).sorted().boxed().toList();

        int n = prices.size();
        int q1Index = (int) Math.floor(0.25 * (n - 1));
        int q3Index = (int) Math.floor(0.75 * (n - 1));
        double q1 = prices.get(q1Index);
        double q3 = prices.get(q3Index);
        double iqr = q3 - q1;
        double lower = q1 - 1.5 * iqr;
        double upper = q3 + 1.5 * iqr;

        return prices.stream()
                .filter(p -> p >= lower && p <= upper)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    private void cleanupExpiredCache() {
        priceCache.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    public void clearCache() {
        priceCache.clear();
    }

    public CompletableFuture<Integer> forceRecalculatePrice(ItemStack item) {
        if (item == null || !ENABLED) {
            return CompletableFuture.completedFuture(0);
        }
        ItemStack single = item.clone();
        single.setAmount(1);
        String fingerprint = generateItemFingerprint(single);
        priceCache.remove(fingerprint);
        return calculateAveragePrice(item);
    }

    public void clearCacheForMaterial(org.bukkit.Material material) {
        String prefix = material.name();
        String enhancedKey = "MATERIAL:" + prefix;
        priceCache.entrySet().removeIf(
                e -> {
                    String k = e.getKey();
                    return k.equals(enhancedKey) || k.equals(prefix) || k.startsWith(prefix + "_");
                });
    }

    /**
     * Clears cache for a specific fingerprint (useful for enhanced identifiers like
     * HYPING_ITEM:...)
     */
    public void clearCacheForFingerprint(String fingerprint) {
        if (fingerprint == null)
            return;
        priceCache.remove(fingerprint);
    }

    public static void setEnabled(boolean enabled) {
        ENABLED = enabled;
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    /**
     * Return a cached value even if it is expired. Useful as a seamless UI
     * fallback.
     */
    public int getFromCacheIncludingExpired(ItemStack item) {
        if (item == null)
            return 0;
        ItemStack single = item.clone();
        single.setAmount(1);
        String fp = generateItemFingerprint(single);
        CachedPrice c = priceCache.get(fp);
        return (c != null) ? c.price : 0;
    }

    public int getFromCacheIncludingExpired(String fingerprint) {
        if (fingerprint == null)
            return 0;
        CachedPrice c = priceCache.get(fingerprint);
        return (c != null) ? c.price : 0;
    }

    /**
     * Convenience: compute total average price based on per-unit cached or computed
     * value.
     * Returns 0 when price is unavailable.
     */
    public int getTotalAveragePrice(ItemStack item) {
        if (item == null)
            return 0;
        int perUnit = getFromCacheIncludingExpired(item);
        return perUnit * item.getAmount();
    }

    /**
     * Exposes a safe fingerprint helper for other components.
     */
    public String getSafeFingerprint(org.bukkit.inventory.ItemStack item) {
        return generateItemFingerprint(item);
    }

    /**
     * Generates a unique fingerprint for an item to identify similar items for
     * price calculations.
     *
     * <p>
     * This method handles several item types:
     * <ul>
     * <li>Crate keys: Detects via hypingcrates:key-id NBT tag, generates
     * HYPING_CRATE_KEY:&lt;crate_id&gt;</li>
     * <li>Enhanced items: Uses EnhancedItemIdentifier for HYPING_ITEM, SPAWNER,
     * etc.</li>
     * <li>Legacy items: Falls back to MATERIAL:&lt;material_name&gt; for basic
     * items</li>
     * </ul>
     *
     * @param item The item to generate a fingerprint for
     * @return A unique string identifier for the item
     */
    private String generateItemFingerprint(ItemStack item) {
        if (item == null)
            return "null_item";

        // Check for crate keys first (before EnhancedItemIdentifier)
        // Crate keys are identified by the hypingcrates:key-id NBT tag in
        // PersistentDataContainer
        if (item.hasItemMeta()) {
            try {
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                org.bukkit.persistence.PersistentDataContainer container = meta.getPersistentDataContainer();
                org.bukkit.NamespacedKey keyId = new org.bukkit.NamespacedKey("hypingcrates", "key-id");

                if (container.has(keyId, org.bukkit.persistence.PersistentDataType.STRING)) {
                    String crateId = container.get(keyId, org.bukkit.persistence.PersistentDataType.STRING);
                    if (crateId != null && !crateId.isBlank()) {
                        return "HYPING_CRATE_KEY:" + crateId;
                    }
                }
            } catch (Throwable ignored) {
                // Crate key detection failed, continue to EnhancedItemIdentifier
            }
        }

        try {
            if (item.getType() == org.bukkit.Material.ENCHANTED_BOOK && item.hasItemMeta()) {
                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
                if (meta != null && !meta.getStoredEnchants().isEmpty()) {
                    return "ENCHANTED_BOOK:" + meta.getStoredEnchants().entrySet().stream()
                            .sorted(java.util.Map.Entry.comparingByKey(java.util.Comparator.comparing(e -> e.getKey().getKey())))
                            .map(e -> e.getKey().getKey() + ":" + e.getValue())
                            .reduce((a, b) -> a + "|" + b)
                            .orElse("EMPTY");
                }
            }
            fr.hokib.hyping.hypingcore.module.impl.prix.integration.EnhancedItemIdentifier.ItemIdentifier identifier = fr.hokib.hyping.hypingcore.module.impl.prix.integration.EnhancedItemIdentifier
                    .getItemIdentifier(item);
            // Standardize on enhanced identifiers, e.g., MATERIAL:STONE_BRICKS
            return identifier.getFullIdentifier();
        } catch (Throwable ignored) {
            // Fallback: legacy material name only
            return "MATERIAL:" + item.getType().name();
        }
    }

    private static class SaleData {
        final double price;
        final long saleDate;
        final UUID sellerId;

        SaleData(double price, long saleDate, UUID sellerId) {
            this.price = price;
            this.saleDate = saleDate;
            this.sellerId = sellerId;
        }
    }

    private static class CachedPrice {
        final int price;
        final long timestamp;

        CachedPrice(int price, long timestamp) {
            this.price = price;
            this.timestamp = timestamp;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
}

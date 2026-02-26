package fr.hyping.hypingauctions.manager.object;

import fr.hyping.hypingauctions.manager.CurrencyManager;
import fr.hyping.hypingauctions.manager.PlayerManager;
import fr.hyping.hypingauctions.util.CustomModelDataUtil;
import java.util.Base64;
import java.util.UUID;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

public class Auction {

    private ObjectId id;
    private final ItemStack item;
    private final long price;
    private final Currency currency;
    private final AuctionPlayer seller;
    private AuctionPlayer buyer;
    private final long saleDate;
    // Time when the auction was actually purchased (if sold). 0 when not sold or
    // unknown.
    private long purchaseDate;
    private long expirationTime;
    private boolean pickedUp;
    private long premiumExpiration; // 0 = not premium, timestamp = premium until
    private int premiumSlot; // -1 = not in premium slot, 0-5 = premium slot number

    public Auction(
            ObjectId id,
            ItemStack item,
            long price,
            Currency currency,
            AuctionPlayer seller,
            AuctionPlayer buyer,
            long saleDate,
            long expirationTime,
            boolean pickedUp) {
        this(id, item, price, currency, seller, buyer, saleDate, 0L, expirationTime, pickedUp, 0L, -1);
    }

    public Auction(
            ObjectId id,
            ItemStack item,
            long price,
            Currency currency,
            AuctionPlayer seller,
            AuctionPlayer buyer,
            long saleDate,
            long purchaseDate,
            long expirationTime,
            boolean pickedUp,
            long premiumExpiration,
            int premiumSlot) {
        this.id = id;
        this.item = item;
        this.price = price;
        this.currency = currency;
        this.seller = seller;
        this.buyer = buyer;
        this.saleDate = saleDate;
        this.purchaseDate = purchaseDate;
        this.expirationTime = expirationTime;
        this.pickedUp = pickedUp;
        this.premiumExpiration = premiumExpiration;
        this.premiumSlot = premiumSlot;
    }

    public Auction(
            ItemStack item,
            long price,
            Currency currency,
            AuctionPlayer seller,
            long saleDate,
            long expirationTime) {
        this(null, item, price, currency, seller, null, saleDate, 0L, expirationTime, false, 0L, -1);
    }

    public ObjectId getId() {
        return id;
    }

    public ItemStack getItem() {
        return item;
    }

    public long getPrice() {
        return price;
    }

    public Currency getCurrency() {
        return currency;
    }

    public AuctionPlayer getSeller() {
        return seller;
    }

    public AuctionPlayer getBuyer() {
        return buyer;
    }

    public long getSaleDate() {
        return saleDate;
    }

    /**
     * Returns the moment when the item was purchased. 0 when not sold or unknown.
     */
    public long getPurchaseDate() {
        return purchaseDate;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public long getExpirationDate() {
        return saleDate + expirationTime;
    }

    public void expire() {
        this.expirationTime = 0L;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public void setBuyer(AuctionPlayer buyer) {
        this.buyer = buyer;
    }

    public boolean isPickedUp() {
        return pickedUp;
    }

    public long getPremiumExpiration() {
        return premiumExpiration;
    }

    public void setPremiumExpiration(long premiumExpiration) {
        this.premiumExpiration = premiumExpiration;
    }

    public int getPremiumSlot() {
        return premiumSlot;
    }

    public void setPremiumSlot(int premiumSlot) {
        this.premiumSlot = premiumSlot;
    }

    public boolean isPremium() {
        return premiumExpiration > 0 && System.currentTimeMillis() < premiumExpiration;
    }

    public boolean isPremiumExpired() {
        return premiumExpiration > 0 && System.currentTimeMillis() >= premiumExpiration;
    }

    public long getPremiumRemainingTime() {
        if (!isPremium())
            return 0;
        return premiumExpiration - System.currentTimeMillis();
    }

    public Document toDocument() {
        return new Document()
                .append("owner_id", seller.getPlayer().getUniqueId().toString())
                .append("item", Base64.getEncoder().encodeToString(item.serializeAsBytes()))
                .append("price", price)
                .append("currency", currency.counter().getName())
                .append("sale_date", saleDate)
                .append("expiration_time", expirationTime)
                .append("premium_expiration", premiumExpiration)
                .append("premium_slot", premiumSlot);
    }

    public Document toSearchDocument() {
        return new Document().append("_id", id);
    }

    public static Auction fromDocument(Document document) {
        long price;
        try {
            price = document.getInteger("price");
        } catch (ClassCastException e) {
            price = document.getLong("price");
        }
        long derivedPurchaseDate = 0L;
        if (document.containsKey("purchase_date")) {
            derivedPurchaseDate = document.getLong("purchase_date");
        } else if (document.containsKey("_id") && document.get("_id") instanceof ObjectId oid) {
            // Fallback: use ObjectId timestamp (creation time) which matches insert time in
            // "bought"
            derivedPurchaseDate = oid.getTimestamp() * 1000L;
        }

        Auction auction = new Auction(
                document.getObjectId("_id"),
                ItemStack.deserializeBytes(Base64.getDecoder().decode(document.getString("item"))),
                price,
                CurrencyManager.getCurrency(document.getString("currency")),
                document.containsKey("owner_id")
                        ? PlayerManager.getPlayer(
                        Bukkit.getOfflinePlayer(UUID.fromString(document.getString("owner_id"))))
                        : null,
                document.containsKey("buyer_id")
                        ? PlayerManager.getPlayer(
                        Bukkit.getOfflinePlayer(UUID.fromString(document.getString("buyer_id"))))
                        : null,
                document.getLong("sale_date"),
                derivedPurchaseDate,
                document.getLong("expiration_time"),
                document.containsKey("picked_up")
                        ? document.getBoolean("picked_up")
                        : document.getBoolean("pick_up", false),
                document.containsKey("premium_expiration") ? document.getLong("premium_expiration") : 0L,
                document.containsKey("premium_slot") ? document.getInteger("premium_slot") : -1);

        // FIX: Detect and correct corrupted expiration times (bug where ms were treated
        // as seconds)
        // If expiration is > 100 days (8,640,000,000 ms), it's likely the bug (1000
        // days).
        if (auction.expirationTime > 8_640_000_000L) {
            auction.expirationTime /= 1000;
        }

        return auction;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > getExpirationDate();
    }

    /**
     * Checks if this auction has been sold.
     *
     * Backward compatibility: legacy records may lack buyer_id. In those cases,
     * when a
     * purchase timestamp can be derived (purchaseDate > 0), we still consider the
     * auction sold.
     *
     * @return true if the auction has a buyer or a purchase timestamp, false
     *         otherwise
     */
    public boolean isSold() {
        return buyer != null || purchaseDate > 0;
    }

    /**
     * Creates a unique fingerprint for this item to identify similar items for
     * price calculations.
     * This includes material, custom model data, enchantments, and other relevant
     * properties.
     *
     * <p>
     * This method handles several item types:
     * <ul>
     * <li>Crate keys: Detects via hypingcrates:key-id NBT tag, generates
     * HYPING_CRATE_KEY:&lt;crate_id&gt;</li>
     * <li>Enhanced items: Uses EnhancedItemIdentifier for HYPING_ITEM, SPAWNER,
     * HYPING_SPAWNER, HYPING_CRATE_KEY</li>
     * <li>Legacy items: Falls back to material-based fingerprint with custom model
     * data and enchantments</li>
     * </ul>
     *
     * @return A string that uniquely identifies similar items
     */
    public String getItemFingerprint() {
        if (item == null) {
            return "null_item";
        }

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

        // Use enhanced identifiers for special items (HypingItems, spawners, and crate
        // keys)
        try {
            fr.hokib.hyping.hypingcore.module.impl.prix.integration.EnhancedItemIdentifier.ItemIdentifier identifier = fr.hokib.hyping.hypingcore.module.impl.prix.integration.EnhancedItemIdentifier
                    .getItemIdentifier(item);
            String base = identifier.getBaseType();
            if ("HYPING_ITEM".equals(base) || "SPAWNER".equals(base) || "HYPING_SPAWNER".equals(base)
                    || "HYPING_CRATE_KEY".equals(base)) {
                return identifier.getFullIdentifier();
            }
        } catch (Throwable ignored) {
            // If enhanced identification is unavailable, fall back to legacy fingerprint
        }

        StringBuilder fingerprint = new StringBuilder();
        fingerprint.append(item.getType().name());

        if (item.hasItemMeta()) {
            var meta = item.getItemMeta();

            // Add custom model data (handles both vanilla CustomModelData and Oraxen
            // ItemModel)
            fingerprint.append(CustomModelDataUtil.getCustomModelDataFingerprint(item));

            // Add enchantments (sorted for consistency)
            if (meta.hasEnchants()) {
                fingerprint.append("_ench:");
                meta.getEnchants().entrySet().stream()
                        .sorted(
                                (e1, e2) -> e1.getKey().getKey().toString().compareTo(e2.getKey().getKey().toString()))
                        .forEach(
                                entry -> fingerprint
                                        .append(entry.getKey().getKey().toString())
                                        .append(":")
                                        .append(entry.getValue())
                                        .append(","));
            }

            // Add display name pattern (if it follows a pattern)
            if (meta.hasDisplayName()) {
                String displayName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                        .serialize(meta.displayName());
                // Remove color codes and numbers for pattern matching
                String pattern = displayName.replaceAll("§[0-9a-fk-or]", "").replaceAll("\\d+", "#");
                if (!pattern.trim().isEmpty()) {
                    fingerprint.append("_name:").append(pattern.hashCode());
                }
            }
        }

        return fingerprint.toString();
    }
}

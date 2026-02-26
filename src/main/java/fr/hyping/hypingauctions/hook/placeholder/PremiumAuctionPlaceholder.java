package fr.hyping.hypingauctions.hook.placeholder;

import fr.hyping.hypingauctions.cache.PlayerNameCache;
import fr.hyping.hypingauctions.hook.IPlaceholderExtension;
import fr.hyping.hypingauctions.manager.EnchantmentTranslationManager;
import fr.hyping.hypingauctions.manager.PotionTranslationManager;
import fr.hyping.hypingauctions.manager.PremiumSlotManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.service.AveragePriceService;
import fr.hyping.hypingauctions.util.CustomModelDataUtil;
import fr.hyping.hypingauctions.util.Format;
import fr.hyping.hypingcounters.HypingCountersPlugin;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Nullable;

public class PremiumAuctionPlaceholder implements IPlaceholderExtension {

    private static final LegacyComponentSerializer COMPONENT_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    @Override
    public @Nullable String onReplace(AuctionPlayer player, String name, String[] args) {
        if (args.length < 2) {
            return null;
        }

        // Determine slot number based on argument pattern
        int slotNumber;
        try {
            if (args[0].equalsIgnoreCase("premium") && args.length >= 3) {
                // Handle %hauctions_premium_premium_expiration_1% -> args = ["premium",
                // "expiration", "1"]
                // Handle %hauctions_premium_premium_remaining_1% -> args = ["premium",
                // "remaining", "1"]
                slotNumber = Integer.parseInt(args[2]) - 1; // Convert to 0-based
            } else if (args[0].equalsIgnoreCase("currency") && args.length >= 3) {
                // Handle %hauctions_premium_currency_formatted_1% -> args = ["currency",
                // "formatted", "1"]
                slotNumber = Integer.parseInt(args[2]) - 1; // Convert to 0-based
            } else if (args[0].equalsIgnoreCase("enchantment") && args.length >= 3) {
                // Handle %hauctions_premium_enchantment_has_1% -> args = ["enchantment", "has",
                // "1"]
                slotNumber = Integer.parseInt(args[2]) - 1; // Convert to 0-based
            } else {
                // Handle %hauctions_premium_price_1% -> args = ["price", "1"]
                // Handle %hauctions_premium_currency_1% -> args = ["currency", "1"]
                // Handle %hauctions_premium_enchantment_1% -> args = ["enchantment", "1"]
                slotNumber = Integer.parseInt(args[1]) - 1; // Convert to 0-based
            }

            if (slotNumber < 0 || slotNumber >= PremiumSlotManager.getMaxSlots()) {
                return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }

        Auction auction = PremiumSlotManager.getPremiumAuctionBySlot(slotNumber);
        if (auction == null) {
            return null; // No auction in this premium slot
        }

        ItemStack item = auction.getItem();
        ItemMeta meta = item.getItemMeta();

        return switch (args[0].toLowerCase()) {
            case "material" -> item.getType().name();
            case "custom-model-data" -> getCustomModelData(item);
            case "quantity" -> String.valueOf(item.getAmount());
            case "name" -> getItemDisplayName(item);
            case "lore" -> {
                if (meta == null || !meta.hasLore()) {
                    yield "";
                }
                String loreText = meta.lore().stream()
                        .map(LegacyComponentSerializer.legacyAmpersand()::serialize)
                        .map(this::sanitizeLegacy)
                        .reduce((first, second) -> first + "\n" + second)
                        .orElse("");

                // Fix firework flight duration display
                if (item.getType() == Material.FIREWORK_ROCKET) {
                    loreText = fixFireworkFlightDuration(item, loreText);
                }

                yield loreText;
            }
            case "price" -> Format.formatNumber(auction.getPrice());
            case "currency" -> {
                if (args.length >= 2 && args[1].equalsIgnoreCase("formatted")) {
                    // Handle %hauctions_premium_currency_formatted_1% -> args = ["currency",
                    // "formatted",
                    // "1"]
                    yield auction.getCurrency().name();
                } else {
                    // Handle %hauctions_premium_currency_1% -> args = ["currency", "1"]
                    yield auction.getCurrency().counter().getName();
                }
            }
            case "seller" -> PlayerNameCache.getInstance().getPlayerName(auction.getSeller().getPlayer());
            case "expiration" -> {
                long expiration = auction.getExpirationDate() - System.currentTimeMillis();
                if (expiration <= 0)
                    yield "§cExpiré";
                yield Format.formatTime(expiration);
            }
            case "premium" -> {
                if (args.length >= 2) {
                    // Handle %hauctions_premium_premium_expiration_1% -> args = ["premium",
                    // "expiration",
                    // "1"]
                    // Handle %hauctions_premium_premium_remaining_1% -> args = ["premium",
                    // "remaining", "1"]
                    if (args[1].equalsIgnoreCase("expiration") || args[1].equalsIgnoreCase("remaining")) {
                        long remaining = auction.getPremiumRemainingTime();
                        if (remaining <= 0)
                            yield "§cExpiré";
                        yield Format.formatTime(remaining);
                    } else {
                        yield null;
                    }
                } else {
                    yield null;
                }
            }
            case "exists" -> "true";
            case "shulkerbox" -> String.valueOf(item.getType().name().contains("SHULKER_BOX"));
            case "playerhead" -> getPlayerHead(item);
            case "enchantment_has" -> {
                if (item.getType() == Material.ENCHANTED_BOOK) {
                    yield "true";
                }
                try {
                    if (fr.natsu.items.entity.CustomItem.getCustomItem(item) != null) {
                        yield "true";
                    }
                } catch (NoClassDefFoundError | Exception ignored) {
                }
                Map<Enchantment, Integer> enchants = getItemEnchantments(item);
                yield String.valueOf(!enchants.isEmpty());
            }
            case "enchantment" -> {
                // Check if this is the "_has" variant
                if (args.length >= 2 && args[1].equalsIgnoreCase("has")) {
                    // Handle %hauctions_premium_enchantment_has_1% -> args = ["enchantment", "has",
                    // "1"]
                    if (item.getType() == Material.ENCHANTED_BOOK) {
                        yield "true";
                    }
                    try {
                        if (fr.natsu.items.entity.CustomItem.getCustomItem(item) != null) {
                            yield "true";
                        }
                    } catch (NoClassDefFoundError | Exception ignored) {
                    }
                    Map<Enchantment, Integer> enchants = getItemEnchantments(item);
                    yield String.valueOf(!enchants.isEmpty());
                } else {
                    // Handle %hauctions_premium_enchantment_1% -> args = ["enchantment", "1"]

                    // If it is an enchanted book, return empty string because enchants are already in lore
                    if (item.getType() == Material.ENCHANTED_BOOK) {
                        yield "";
                    }

                    Map<Enchantment, Integer> enchants = getItemEnchantments(item);
                    if (enchants.isEmpty())
                        yield "";

                    // Use the same translation logic as regular EnchantmentPlaceholder
                    EnchantmentTranslationManager translationManager = EnchantmentTranslationManager.getInstance();
                    yield enchants.entrySet().stream()
                            .map(
                                    entry -> translationManager.translateEnchantment(entry.getKey(), entry.getValue()))
                            .reduce((first, second) -> first + "\n" + second)
                            .orElse("");
                }
            }
            case "potion-type" -> {
                if (!(meta instanceof PotionMeta potionMeta))
                    yield "Not a potion";
                try {
                    // Try to use the newer API first (for newer Minecraft versions)
                    PotionType potionType = potionMeta.getBasePotionType();
                    if (potionType != null && !potionType.name().equals("UNCRAFTABLE")) {
                        yield potionType.name();
                    }
                } catch (NoSuchMethodError ignored) {
                    // Fall back to the older API for backward compatibility
                }

                // Legacy API or when newer API returned null/UNCRAFTABLE
                try {
                    PotionData potionData = potionMeta.getBasePotionData();
                    PotionType potionType = potionData.getType();
                    if (potionType != null) {
                        yield potionType.name();
                    }
                } catch (Throwable ignored) {
                    // Ignore and use default
                }
                yield "WATER"; // Default fallback for non-potions
            }
            case "potion-displayname" -> {
                if (!(meta instanceof PotionMeta))
                    yield "Not a potion";
                // Use the potion translation manager for consistent formatting
                yield PotionTranslationManager.getInstance().translatePotion(item);
            }
            case "averageprice" -> {
                CompletableFuture<Integer> future = AveragePriceService.getInstance().calculateAveragePrice(item);

                int perUnit = future.getNow(0);
                if (perUnit <= 0) {
                    perUnit = AveragePriceService.getInstance().getFromCacheIncludingExpired(item);
                }
                String na = fr.hyping.hypingauctions.HypingAuctions.getInstance()
                        .getConfig()
                        .getString("placeholders.average-price.no-price", "Unknown");

                if (perUnit <= 0) {
                    yield na;
                }
                int amount = (item != null && item.getAmount() > 0) ? item.getAmount() : 1;
                long total = (long) perUnit * amount;
                yield Format.formatNumber(total);
            }

            default -> null;
        };
    }

    /**
     * Gets the custom model data for an item using the unified CustomModelDataUtil
     */
    private String getCustomModelData(ItemStack itemStack) {
        return CustomModelDataUtil.getCustomModelDataAsString(itemStack);
    }

    /**
     * Get enchantments from an ItemStack, handling both regular enchanted items and
     * enchanted books
     *
     * @param itemStack The item to get enchantments from
     * @return Map of enchantments and their levels
     */
    private Map<Enchantment, Integer> getItemEnchantments(ItemStack itemStack) {
        // Check if this is an enchanted book
        if (itemStack.getType() == Material.ENCHANTED_BOOK) {
            // For enchanted books, we need to get stored enchantments
            if (itemStack.getItemMeta() instanceof EnchantmentStorageMeta bookMeta) {
                return bookMeta.getStoredEnchants();
            }
        }

        // For regular items, get normal enchantments
        return itemStack.getEnchantments();
    }

    /**
     * Get the player head name from an ItemStack, using the same logic as
     * PlayerHeadPlaceholder
     *
     * @param itemStack The item to check for player head
     * @return The player name if it's a player head, "Unknown" otherwise
     */
    private String getPlayerHead(ItemStack itemStack) {
        // Check if the item is a player head
        if (itemStack.getType() == Material.PLAYER_HEAD) {
            if (itemStack.getItemMeta() instanceof SkullMeta skullMeta) {
                // Try to get the player profile without blocking network calls
                try {
                    com.destroystokyo.paper.profile.PlayerProfile profile = skullMeta.getPlayerProfile();
                    if (profile != null && profile.getId() != null) {
                        return PlayerNameCache.getInstance().getPlayerName(profile.getId());
                    }
                } catch (Exception e) {
                }

                // If no owner is set, return a valid fallback name
                return "Unknown";
            }
        }

        return "Unknown";
    }

    /**
     * Get the display name of an item, checking Oraxen first, then ItemMeta, then
     * fallback to
     * material name
     */
    private String getItemDisplayName(ItemStack item) {
        // 1. Get Oraxen display name first (Oraxen items rely on config, not ItemMeta)
        String oraxenName = fr.hyping.hypingauctions.hook.OraxenHook.getOraxenDisplayName(item);
        if (oraxenName != null && !oraxenName.isEmpty()) {
            return sanitizeLegacy(oraxenName);
        }

        // 2. Get ItemMeta display name (for renamed items or other custom items)
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            String legacy = LegacyComponentSerializer.legacyAmpersand()
                    .serialize(Objects.requireNonNull(meta.displayName()));
            String metaName = sanitizeLegacy(legacy);
            if (metaName != null && !metaName.isEmpty()) {
                return metaName;
            }
        }

        // 3. Fallback to material translatable name
        String legacy = LegacyComponentSerializer.legacyAmpersand().serialize(Component.translatable(item));
        return sanitizeLegacy(legacy);
    }

    /**
     * Remove hard resets that cancel outer GUI formatting while keeping inner
     * colors
     */
    private String sanitizeLegacy(String legacyText) {
        if (legacyText == null)
            return null;
        return legacyText.replace("§r", "").replace("&r", "");
    }

    /**
     * Fix firework flight duration display by replacing the incorrect "Flight
     * Duration: 1" with the
     * actual flight duration based on the FireworkMeta power level.
     */
    private String fixFireworkFlightDuration(ItemStack item, String loreText) {
        if (!(item.getItemMeta() instanceof FireworkMeta fireworkMeta)) {
            return loreText;
        }

        // Check if the firework has power set
        if (!fireworkMeta.hasPower()) {
            return loreText;
        }

        int power = fireworkMeta.getPower();

        // Pattern to match "Flight Duration: X" in various formats (with or without
        // colors)
        Pattern flightDurationPattern = Pattern.compile(
                "(?i)(§[0-9a-fk-or]|&[0-9a-fk-or])*Flight Duration: (§[0-9a-fk-or]|&[0-9a-fk-or])*\\d+",
                Pattern.CASE_INSENSITIVE);

        // Replace with the correct flight duration
        return flightDurationPattern
                .matcher(loreText)
                .replaceAll(
                        matchResult -> {
                            String prefix = matchResult.group().replaceAll("\\d+$", ""); // Keep color codes and text
                            return prefix + power;
                        });
    }
}

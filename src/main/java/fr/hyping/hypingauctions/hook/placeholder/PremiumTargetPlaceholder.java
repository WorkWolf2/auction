package fr.hyping.hypingauctions.hook.placeholder;

import fr.hyping.hypingauctions.cache.PlayerNameCache;
import fr.hyping.hypingauctions.hook.IPlaceholderExtension;
import fr.hyping.hypingauctions.manager.EnchantmentTranslationManager;
import fr.hyping.hypingauctions.manager.PotionTranslationManager;
import fr.hyping.hypingauctions.manager.PremiumTargetManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.service.AveragePriceService;
import fr.hyping.hypingauctions.util.CustomModelDataUtil;
import fr.hyping.hypingauctions.util.Format;
import fr.hyping.hypingcounters.HypingCountersPlugin;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Nullable;

/**
 * Placeholders for the premium promotion target item (used in validation popup)
 */
public class PremiumTargetPlaceholder implements IPlaceholderExtension {

    private static final LegacyComponentSerializer COMPONENT_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    @Override
    public @Nullable String onReplace(AuctionPlayer player, String name, String[] args) {
        if (args.length == 0) {
            return null;
        }

        // Get the target auction for this player
        // We need to get the online Player object for PremiumTargetManager
        Player onlinePlayer = null;
        if (player.getPlayer().isOnline()) {
            onlinePlayer = player.getPlayer().getPlayer();
        }

        if (onlinePlayer == null) {
            return null; // Player not online, can't have premium targets
        }

        Auction auction = PremiumTargetManager.getTarget(onlinePlayer);
        if (auction == null) {
            return null; // No target set
        }

        ItemStack item = auction.getItem();
        ItemMeta meta = item.getItemMeta();

        return switch (args[0].toLowerCase()) {
            // Basic item information
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

            // Auction information
            case "price" -> Format.formatNumber(auction.getPrice());
            case "currency" -> {
                if (args.length >= 2 && args[1].equalsIgnoreCase("formatted")) {
                    // Handle %hauctions_premium-target_currency_formatted% -> args = ["currency",
                    // "formatted"]
                    yield auction.getCurrency().name();
                } else {
                    // Handle %hauctions_premium-target_currency% -> args = ["currency"]
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

            // Premium promotion information
            case "premium_cost" -> String.valueOf(PremiumTargetManager.getPremiumCost());
            case "premium_currency" -> PremiumTargetManager.getPremiumCurrency();
            case "premium_available_slots" -> String.valueOf(PremiumTargetManager.getAvailableSlots());
            case "premium_can_promote" ->
                    String.valueOf(PremiumTargetManager.canPromoteTarget(onlinePlayer));
            case "premium_error" -> PremiumTargetManager.getValidationError(onlinePlayer);

            // Item features
            case "exists" -> "true";
            case "shulkerbox" -> String.valueOf(item.getType().name().contains("SHULKER_BOX"));

            // Player head support
            case "playerhead" -> getPlayerHead(item);

            // Enchantment support - handle both enchantment and enchantment_has
            case "enchantment" -> {
                // Check if this is the "_has" variant
                if (args.length >= 2 && args[1].equalsIgnoreCase("has")) {
                    // Handle %hauctions_premium-target_enchantment_has% -> args = ["enchantment",
                    // "has"]
                    if (item.getType() == Material.ENCHANTED_BOOK) {
                        yield "true";
                    }
                    try {
                        if (fr.natsu.items.entity.CustomItem.getCustomItem(item) != null) {
                            yield "true";
                        }
                    } catch (NoClassDefFoundError | Exception ignored) {
                    }
                    java.util.Map<org.bukkit.enchantments.Enchantment, Integer> enchants = getItemEnchantments(item);
                    yield String.valueOf(!enchants.isEmpty());
                } else {
                    // Handle %hauctions_premium-target_enchantment% -> args = ["enchantment"]

                    // If it is an enchanted book, return empty string because enchants are already in lore
                    if (item.getType() == Material.ENCHANTED_BOOK) {
                        yield "";
                    }

                    java.util.Map<org.bukkit.enchantments.Enchantment, Integer> enchants = getItemEnchantments(item);
                    if (enchants.isEmpty())
                        yield "none";

                    EnchantmentTranslationManager translationManager = EnchantmentTranslationManager.getInstance();
                    yield enchants.entrySet().stream()
                            .map(
                                    entry -> translationManager.translateEnchantment(entry.getKey(), entry.getValue()))
                            .reduce((first, second) -> first + "\n" + second)
                            .orElse("none");
                }
            }

            // Potion support
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

            default -> null;
        };
    }

    /**
     * Gets the custom model data for an item using the unified CustomModelDataUtil
     */
    private String getCustomModelData(ItemStack itemStack) {
        return CustomModelDataUtil.getCustomModelDataAsString(itemStack);
    }

    /** Get player head name from an ItemStack */
    private String getPlayerHead(ItemStack item) {
        // Check if the item is a player head
        if (item.getType() == org.bukkit.Material.PLAYER_HEAD) {
            if (item.getItemMeta() instanceof org.bukkit.inventory.meta.SkullMeta skullMeta) {
                // Try to get the player profile without blocking network calls
                try {
                    com.destroystokyo.paper.profile.PlayerProfile profile = skullMeta.getPlayerProfile();
                    if (profile != null && profile.getId() != null) {
                        return PlayerNameCache.getInstance().getPlayerName(profile.getId());
                    }
                } catch (Exception e) {
                }
            }
        }
        return "Unknown";
    }

    /**
     * Get enchantments from an ItemStack, handling both regular enchanted items and
     * enchanted books
     * Uses the exact same logic as EnchantmentPlaceholder
     */
    private java.util.Map<org.bukkit.enchantments.Enchantment, Integer> getItemEnchantments(
            ItemStack item) {
        // Check if this is an enchanted book
        if (item.getType() == org.bukkit.Material.ENCHANTED_BOOK) {
            // For enchanted books, we need to get stored enchantments
            if (item.getItemMeta() instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta bookMeta) {
                return bookMeta.getStoredEnchants();
            }
        }

        // For regular items, get normal enchantments
        return item.getEnchantments();
    }

    /**
     * Get the display name of an item, checking Oraxen first, then ItemMeta, then
     * fallback to
     * material name
     */
    private String getItemDisplayName(ItemStack item) {
        // First try to get Oraxen display name
        String oraxenName = fr.hyping.hypingauctions.hook.OraxenHook.getOraxenDisplayName(item);
        if (oraxenName != null) {
            return sanitizeLegacy(oraxenName);
        }

        // Then check ItemMeta display name
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            String legacy = LegacyComponentSerializer.legacyAmpersand()
                    .serialize(Objects.requireNonNull(meta.displayName()));
            return sanitizeLegacy(legacy);
        }

        // Fallback to material translatable name
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

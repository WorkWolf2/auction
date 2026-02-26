package fr.hyping.hypingauctions.manager;

import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.manager.object.Permission;
import fr.hyping.hypingauctions.util.Configs;
import java.util.HashMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class LimitManager {

    private static final HashMap<Permission, Integer> limits = new HashMap<>();
    private static final HashMap<Permission, String> limitMessages = new HashMap<>();

    private static final HashMap<String, CategoryLimit> categoryLimits = new HashMap<>();

    // Global slot cap configuration
    private static boolean globalSlotsEnabled = true;
    private static int globalSlotsDefault = 10;
    private static String globalSlotsCannotBuyKey = "limits.global-slots.cannot-buy";
    private static String globalSlotsCannotSellKey = "limits.global-slots.cannot-sell";

    // Per-player count configuration (what types count toward global slot cap)
    private static boolean countActive = true;
    private static boolean countPurchases = true;
    private static boolean countExpired = true;

    private static void loadLimits() {
        FileConfiguration config = Configs.getConfig("limits");

        // Load global-slots configuration
        ConfigurationSection globalSlotsSection = config.getConfigurationSection("global-slots");
        if (globalSlotsSection != null) {
            globalSlotsEnabled = globalSlotsSection.getBoolean("enabled", true);
            globalSlotsDefault = globalSlotsSection.getInt("default", 10);

            // Messages are now handled via translation keys
            globalSlotsCannotBuyKey = "limits.global-slots.cannot-buy";
            globalSlotsCannotSellKey = "limits.global-slots.cannot-sell";
        }

        // Load count-types configuration (applies to global slot cap)
        ConfigurationSection countTypes = config.getConfigurationSection("count-types");
        if (countTypes != null) {
            countActive = countTypes.getBoolean("active", true);
            countPurchases = countTypes.getBoolean("purchases", true);
            countExpired = countTypes.getBoolean("expired", true);
        }

        ConfigurationSection section = config.getConfigurationSection("limits");

        if (section != null) {
            for (String key : section.getKeys(false)) {
                if (key.equals("categories"))
                    continue; // Skip the categories section here
                ConfigurationSection rankSection = section.getConfigurationSection(key);
                if (rankSection == null
                        || !rankSection.isConfigurationSection("permissions")
                        || !rankSection.isInt("limit")) {
                    HypingAuctions.getInstance().getLogger().warning("Invalid limit section: " + key);
                    continue;
                }

                Permission permission = new Permission(rankSection.getConfigurationSection("permissions"));
                limits.put(permission, rankSection.getInt("limit"));

                // Generate translation key based on section name
                String translationKey = "limits." + key;
                limitMessages.put(permission, translationKey);
            }
        }

        // Load category limits
        ConfigurationSection categoriesSection = config.getConfigurationSection("limits.categories");
        if (categoriesSection != null) {
            for (String key : categoriesSection.getKeys(false)) {
                ConfigurationSection catSection = categoriesSection.getConfigurationSection(key);
                if (catSection == null || !catSection.isInt("limits") || !catSection.isList("list")) {
                    HypingAuctions.getInstance().getLogger().warning("Invalid category limit section: " + key);
                    continue;
                }

                int limit = catSection.getInt("limits");
                java.util.List<String> materials = catSection.getStringList("list");
                java.util.Set<org.bukkit.Material> materialSet = new java.util.HashSet<>();

                for (String matName : materials) {
                    org.bukkit.Material mat = org.bukkit.Material.getMaterial(matName.toUpperCase());
                    if (mat != null) {
                        materialSet.add(mat);
                    } else {
                        HypingAuctions.getInstance().getLogger()
                                .warning("Invalid material in category limit " + key + ": " + matName);
                    }
                }

                java.util.Map<String, String> messages = new HashMap<>();
                // Use translation keys for category messages
                // Key format: limits.categories.<category>.<type>
                messages.put("expired", "limits.categories." + key + ".expired");
                messages.put("purchases", "limits.categories." + key + ".purchases");
                messages.put("sales", "limits.categories." + key + ".sales");
                messages.put("default", "limits.categories." + key + ".default");

                if (!materialSet.isEmpty()) {
                    categoryLimits.put(key, new CategoryLimit(limit, materialSet, messages));
                }
            }
        }
    }

    public static void reload() {
        limits.clear();
        limitMessages.clear();
        categoryLimits.clear();
        loadLimits();
    }

    /**
     * Gets the listing limit for a player based on their permissions.
     * This limit applies to ACTIVE LISTINGS only.
     *
     * @param player The player to get the limit for
     * @return The maximum number of items the player can list for sale
     */
    public static int getLimit(Player player) {
        int limit = 0;

        for (Permission permission : limits.keySet()) {
            if (permission.hasPermission(player))
                limit = Math.max(limit, limits.get(permission));
        }
        return limit;
    }

    /**
     * Gets the global slot cap (maximum total items in the system).
     *
     * @return The global slot cap, or Integer.MAX_VALUE if disabled
     */
    public static int getGlobalSlotCap() {
        return globalSlotsEnabled ? globalSlotsDefault : Integer.MAX_VALUE;
    }

    /**
     * Checks if global slots feature is enabled.
     *
     * @return true if global slots are enabled
     */
    public static boolean isGlobalSlotsEnabled() {
        return globalSlotsEnabled;
    }

    /**
     * Gets the count of ACTIVE LISTINGS only for a player.
     * Used for checking listing limit.
     *
     * @param auctionPlayer The player to count listings for
     * @return The number of active listings
     */
    public static int getActiveListingCount(fr.hyping.hypingauctions.manager.object.AuctionPlayer auctionPlayer) {
        return auctionPlayer.getSales().size();
    }

    /**
     * Gets the count of listings made by a player.
     */
    public static int getEscrowListings(fr.hyping.hypingauctions.manager.object.AuctionPlayer auctionPlayer) {
        return auctionPlayer.getSales().size() + auctionPlayer.getExpired().size();
    }

    /**
     * Gets the total count of items for a player.
     * Counts active listings, purchases, and expired items based on config.
     * Used for checking global slot cap.
     *
     * @param auctionPlayer The player to count items for
     * @return The total number of items counting toward the player's global slot
     *         cap
     */
    public static int getPlayerItemCount(fr.hyping.hypingauctions.manager.object.AuctionPlayer auctionPlayer) {
        int count = 0;
        if (countActive) {
            count += auctionPlayer.getSales().size();
        }
        if (countPurchases) {
            count += auctionPlayer.getPurchases().size();
        }
        if (countExpired) {
            count += auctionPlayer.getExpired().size();
        }
        return count;
    }

    /**
     * Checks if a player has reached their LISTING limit.
     * This only counts active listings, not purchases or expired items.
     *
     * @param player        The player to check
     * @param auctionPlayer The player's auction data
     * @return true if the player has reached their listing limit
     */
    public static boolean isLimitReached(Player player,
                                         fr.hyping.hypingauctions.manager.object.AuctionPlayer auctionPlayer) {
        int limit = getLimit(player);
        int current = getActiveListingCount(auctionPlayer);
        return current >= limit;
    }

    /**
     * Checks if a player has reached their global slot cap.
     * This counts all items: active listings + purchases + expired.
     *
     * @param auctionPlayer The player's auction data
     * @return true if the player has reached or exceeded the global slot cap
     */
    public static boolean isGlobalSlotCapReached(fr.hyping.hypingauctions.manager.object.AuctionPlayer auctionPlayer) {
        if (!globalSlotsEnabled) {
            return false;
        }
        int current = getPlayerItemCount(auctionPlayer);
        return current >= globalSlotsDefault;
    }

    /**
     * Gets the global slot cap message for buying.
     *
     * @param auctionPlayer The player's auction data
     * @return The formatted message
     */
    public static net.kyori.adventure.text.Component getGlobalSlotCapBuyMessage(
            fr.hyping.hypingauctions.manager.object.AuctionPlayer auctionPlayer) {
        int current = getPlayerItemCount(auctionPlayer);
        return HypingAuctions.getInstance().getLangComponent(
                globalSlotsCannotBuyKey,
                net.kyori.adventure.text.Component.text(current),
                net.kyori.adventure.text.Component.text(globalSlotsDefault));
    }

    /**
     * Gets the global slot cap message for selling.
     *
     * @param auctionPlayer The player's auction data
     * @return The formatted message
     */
    public static net.kyori.adventure.text.Component getGlobalSlotCapSellMessage(
            fr.hyping.hypingauctions.manager.object.AuctionPlayer auctionPlayer) {
        int current = getPlayerItemCount(auctionPlayer);
        return HypingAuctions.getInstance().getLangComponent(
                globalSlotsCannotSellKey,
                net.kyori.adventure.text.Component.text(current),
                net.kyori.adventure.text.Component.text(globalSlotsDefault));
    }

    /**
     * Gets the custom listing limit message for a player based on their highest
     * permission
     * level, with placeholders replaced.
     *
     * If the highest limit permission doesn't have a message, falls back to other
     * matching permissions, preferring the one with the lowest limit that has a
     * message.
     *
     * @param player        The player to get the limit message for
     * @param auctionPlayer The player's auction data
     * @return The formatted limit message, or null only if no permissions match at
     *         all
     */
    public static net.kyori.adventure.text.Component getLimitMessage(Player player,
                                                                     fr.hyping.hypingauctions.manager.object.AuctionPlayer auctionPlayer) {
        int highestLimit = 0;
        Permission highestPermission = null;

        // Find the permission with the highest limit that the player has
        for (Permission permission : limits.keySet()) {
            if (permission.hasPermission(player)) {
                int permissionLimit = limits.get(permission);
                if (permissionLimit > highestLimit) {
                    highestLimit = permissionLimit;
                    highestPermission = permission;
                }
            }
        }

        Permission targetPermission = highestPermission;
        int limitToDisplay = highestLimit;

        // Fallback if no message for highest (though we generate keys for all sections
        // now)
        if (targetPermission == null || !limitMessages.containsKey(targetPermission)) {
            int lowestLimitWithMessage = Integer.MAX_VALUE;
            Permission fallbackPermission = null;
            for (Permission permission : limits.keySet()) {
                if (permission.hasPermission(player) && limitMessages.containsKey(permission)) {
                    int permissionLimit = limits.get(permission);
                    if (permissionLimit < lowestLimitWithMessage) {
                        lowestLimitWithMessage = permissionLimit;
                        fallbackPermission = permission;
                    }
                }
            }
            targetPermission = fallbackPermission;
            if (targetPermission == null)
                return null;

            // Use the player's actual limit unless it's 0 (which shouldn't happen if they
            // have permission)
            if (limitToDisplay == 0)
                limitToDisplay = lowestLimitWithMessage;
        }

        String key = limitMessages.get(targetPermission);
        int current = getActiveListingCount(auctionPlayer);

        return HypingAuctions.getInstance().getLangComponent(
                key,
                net.kyori.adventure.text.Component.text(current),
                net.kyori.adventure.text.Component.text(limitToDisplay));
    }

    /**
     * @deprecated Use
     *             {@link #getLimitMessage(Player, fr.hyping.hypingauctions.manager.object.AuctionPlayer)}
     *             instead
     */
    @Deprecated
    public static net.kyori.adventure.text.Component getLimitMessage(Player player) {
        fr.hyping.hypingauctions.manager.object.AuctionPlayer ap = fr.hyping.hypingauctions.manager.PlayerManager
                .getPlayer(player);
        return getLimitMessage(player, ap);
    }

    public static net.kyori.adventure.text.Component getCategoryLimitMessage(Player player,
                                                                             org.bukkit.inventory.ItemStack item,
                                                                             fr.hyping.hypingauctions.manager.object.AuctionPlayer auctionPlayer) {
        if (item == null)
            return null;

        for (java.util.Map.Entry<String, CategoryLimit> entry : categoryLimits.entrySet()) {
            CategoryLimit catLimit = entry.getValue();
            if (catLimit.materials.contains(item.getType())) {
                // Count items in sales, purchases, and expired
                long salesCount = auctionPlayer.getSales().stream()
                        .filter(auction -> catLimit.materials.contains(auction.getItem().getType()))
                        .count();

                long purchasesCount = auctionPlayer.getPurchases().stream()
                        .filter(auction -> catLimit.materials.contains(auction.getItem().getType()))
                        .count();

                long expiredCount = auctionPlayer.getExpired().stream()
                        .filter(auction -> catLimit.materials.contains(auction.getItem().getType()))
                        .count();

                long totalCount = salesCount + purchasesCount + expiredCount;

                if (totalCount >= catLimit.limit) {
                    String key = null;
                    // Prioritize messages: expired > purchases > sales > default
                    if (expiredCount > 0 && catLimit.messages.containsKey("expired")) {
                        key = catLimit.messages.get("expired");
                    } else if (purchasesCount > 0 && catLimit.messages.containsKey("purchases")) {
                        key = catLimit.messages.get("purchases");
                    } else if (salesCount > 0 && catLimit.messages.containsKey("sales")) {
                        key = catLimit.messages.get("sales");
                    } else {
                        key = catLimit.messages.getOrDefault("default", null);
                    }

                    if (key != null) {
                        return HypingAuctions.getInstance().getLangComponent(
                                key,
                                net.kyori.adventure.text.Component.text(totalCount),
                                net.kyori.adventure.text.Component.text(catLimit.limit));
                    }
                }
            }
        }
        return null;
    }

    public static boolean isCategoryLimitReached(Player player, org.bukkit.inventory.ItemStack item,
                                                 fr.hyping.hypingauctions.manager.object.AuctionPlayer auctionPlayer) {
        if (item == null)
            return false;

        for (java.util.Map.Entry<String, CategoryLimit> entry : categoryLimits.entrySet()) {
            CategoryLimit catLimit = entry.getValue();
            if (catLimit.materials.contains(item.getType())) {
                long salesCount = auctionPlayer.getSales().stream()
                        .filter(auction -> catLimit.materials.contains(auction.getItem().getType()))
                        .count();

                long purchasesCount = auctionPlayer.getPurchases().stream()
                        .filter(auction -> catLimit.materials.contains(auction.getItem().getType()))
                        .count();

                long expiredCount = auctionPlayer.getExpired().stream()
                        .filter(auction -> catLimit.materials.contains(auction.getItem().getType()))
                        .count();

                if (salesCount + purchasesCount + expiredCount >= catLimit.limit) {
                    return true;
                }
            }
        }
        return false;
    }

    private static class CategoryLimit {
        final int limit;
        final java.util.Set<org.bukkit.Material> materials;
        final java.util.Map<String, String> messages;

        CategoryLimit(int limit, java.util.Set<org.bukkit.Material> materials, java.util.Map<String, String> messages) {
            this.limit = limit;
            this.materials = materials;
            this.messages = messages;
        }
    }
}

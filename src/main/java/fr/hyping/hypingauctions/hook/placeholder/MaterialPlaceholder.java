package fr.hyping.hypingauctions.hook.placeholder;

import fr.hyping.hypingauctions.hook.IPlaceholderExtension;
import fr.hyping.hypingauctions.manager.CategoryManager;
import fr.hyping.hypingauctions.manager.HistoryManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.PlayerContext;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;

public class MaterialPlaceholder implements IPlaceholderExtension {
    @Override
    public @Nullable String onReplace(AuctionPlayer player, String name, String[] args) {
        if (args.length != 2) return null;

        int index;
        try {
            index = Integer.parseInt(args[1]) - 1;
            if (index < 0) return null;
        } catch (NumberFormatException e) {
            return null;
        }

        PlayerContext context = player.getContext();
        if (context != null && args[0].equalsIgnoreCase("auctions"))
            index += context.getPage() * CategoryManager.getItemsPerPage();

        return switch (args[0].toLowerCase()) {
            case "auctions" ->
                    context != null
                            ? this.getMaterial(context.getFilteredAuctions(), index)
                            : this.getMaterial(player.getSales(), index);
            case "bought" -> this.getMaterial(player.getPurchases(), index);
            case "expired" -> this.getMaterial(player.getExpired(), index);
            case "sales" -> this.getMaterial(player.getSales(), index);
            case "expiredsales" -> this.getMaterial(player.getExpiredSales(), index);
            case "history" ->
                    this.getMaterial(HistoryManager.getPlayerHistory(player.getPlayer()), index);
            case "similar" ->
                    context != null ? this.getMaterial(context.getSimilarAuctions(), index) : null;
            default -> null;
        };
    }

    private String getMaterial(List<Auction> sales, int index) {
        if (index < 0 || index >= sales.size()) {
            return null;
        }
        ItemStack item = sales.get(index).getItem();
        if (item == null) {
            return null;
        }
        Material material = item.getType();

        if (material == Material.PLAYER_HEAD || material == Material.PLAYER_WALL_HEAD) {
            boolean isCustom = isHypingItemsOrHeadDatabase(item);
            if (isCustom) {
                return "PLAYER_HEAD";
            }
        }

        return material.name();
    }

    /**
     * Check if an item is a HypingItems CustomItem or has a headdatabase code.
     */
    private boolean isHypingItemsOrHeadDatabase(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        // Check if it's a HypingItems CustomItem
        try {
            fr.natsu.items.entity.CustomItem customItem =
                    fr.natsu.items.entity.CustomItem.getCustomItem(item);
            if (customItem != null) {
                // Check if it has headdatabase skin data
                try {
                    fr.natsu.items.entity.skin.SkinData skinData = customItem.getSkinData();
                    if (skinData != null) {
                        String defaultSkin = skinData.defaultSkin();
                        if (defaultSkin != null && defaultSkin.startsWith("hdb-")) {
                            return true;
                        }
                        // Also check allowed skins
                        String[] allowed = skinData.allowed();
                        if (allowed != null) {
                            for (String skin : allowed) {
                                if (skin != null && skin.startsWith("hdb-")) {
                                    return true;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Ignore skin data errors
                }
                // If it's a HypingItems CustomItem, return true even if no hdb- prefix
                return true;
            }
        } catch (NoClassDefFoundError | Exception e) {
            // HypingItems not available, continue
        }

        // Check for headdatabase in NBT or persistent data
        try {
            if (item.getItemMeta() instanceof SkullMeta skullMeta) {
                org.bukkit.persistence.PersistentDataContainer container =
                        skullMeta.getPersistentDataContainer();

                // Check for common headdatabase keys
                try {
                    org.bukkit.plugin.Plugin hdbPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("HeadDatabase");
                    if (hdbPlugin != null) {
                        org.bukkit.NamespacedKey hdbKey =
                                new org.bukkit.NamespacedKey(hdbPlugin, "head-id");
                        if (container.has(hdbKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                            return true;
                        }
                    }
                } catch (Exception e) {
                    // Ignore PDC errors
                }

                // Also check for HeadDatabaseUtil if available
                try {
                    if (fr.natsu.items.util.HeadDatabaseUtil.isLoaded()) {
                        try {
                            java.lang.reflect.Method method = fr.natsu.items.util.HeadDatabaseUtil.class.getMethod("isHeadDatabaseItem", ItemStack.class);
                            boolean result = (Boolean) method.invoke(null, item);
                            if (result) {
                                return true;
                            }
                        } catch (Exception e) {
                            // Method not available or error
                        }
                    }
                } catch (NoClassDefFoundError | Exception e) {
                    // HeadDatabaseUtil not available
                }
            }
        } catch (Exception e) {
            // Ignore SkullMeta errors
        }

        return false;
    }
}

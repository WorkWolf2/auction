package fr.hyping.hypingauctions.hook.placeholder;

import fr.hyping.hypingauctions.cache.PlayerNameCache;
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

/**
 * Placeholder for displaying player head information Format: %hauctions_playerhead_auctions_1%
 * Returns the player name of the head at the specified index
 */
public class PlayerHeadPlaceholder implements IPlaceholderExtension {
    @Override
    public @Nullable String onReplace(AuctionPlayer player, String name, String[] args) {
        if (args.length < 1) return null;

        if (args[0].equalsIgnoreCase("target")) {
            PlayerContext context = player.getContext();
            if (context != null) {
                Auction targetAuction = context.getTargetAuction();
                if (targetAuction != null) {
                    return this.getPlayerHeadFromAuction(targetAuction);
                }
            }
            return null;
        }

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
                            ? this.getPlayerHead(context.getFilteredAuctions(), index)
                            : this.getPlayerHead(player.getSales(), index);
            case "bought" -> this.getPlayerHead(player.getPurchases(), index);
            case "expired" -> this.getPlayerHead(player.getExpired(), index);
            case "sales" -> this.getPlayerHead(player.getSales(), index);
            case "expiredsales" -> this.getPlayerHead(player.getExpiredSales(), index);
            case "history" ->
                    this.getPlayerHead(HistoryManager.getPlayerHistory(player.getPlayer()), index);
            case "similar" ->
                    context != null ? this.getPlayerHead(context.getSimilarAuctions(), index) : null;
            default -> null;
        };
    }

    private String getPlayerHead(List<Auction> auctions, int index) {
        if (index >= auctions.size()) return null;

        ItemStack item = auctions.get(index).getItem();
        if (item == null) return null;

        return getPlayerHeadFromItem(item);
    }

    private String getPlayerHeadFromAuction(Auction auction) {
        if (auction == null) return null;

        ItemStack item = auction.getItem();
        if (item == null) return null;

        return getPlayerHeadFromItem(item);
    }

    private String getPlayerHeadFromItem(ItemStack item) {
        if (item == null) return null;

        if (item.getType() == Material.PLAYER_HEAD) {
            try {
                fr.natsu.items.entity.CustomItem customItem =
                        fr.natsu.items.entity.CustomItem.getCustomItem(item);
                if (customItem != null) {
                    String displayName = getItemDisplayName(item);
                    if (displayName != null && !displayName.trim().isEmpty()) {
                        boolean hasColorCodes = displayName.contains("§") || displayName.contains("&");
                        String upperDisplayName = displayName.toUpperCase().replaceAll("§[0-9A-FK-OR]", "").replaceAll("&[0-9A-FK-OR]", "");
                        boolean isMaterialName = upperDisplayName.equals("PLAYER_HEAD") ||
                                upperDisplayName.equals("PLAYER_WALL_HEAD") ||
                                upperDisplayName.equals("TÊTE DE JOUEUR") ||
                                upperDisplayName.equals("HEAD") ||
                                displayName.equals(item.getType().name());

                        if (hasColorCodes || !isMaterialName) {
                            return displayName;
                        }
                    }
                    return "HYPING_ITEM";
                }
            } catch (NoClassDefFoundError | Exception e) {}

            if (item.getItemMeta() instanceof SkullMeta skullMeta) {
                try {
                    com.destroystokyo.paper.profile.PlayerProfile profile = skullMeta.getPlayerProfile();
                    if (profile != null && profile.getId() != null) {
                        return PlayerNameCache.getInstance().getPlayerName(profile.getId());
                    }
                } catch (Exception e) {}
                return "Unknown";
            }
        }
        return "Unknown";
    }

    private String getItemDisplayName(ItemStack item) {
        if (item == null) return null;

        String oraxenName = fr.hyping.hypingauctions.hook.OraxenHook.getOraxenDisplayName(item);
        if (oraxenName != null && !oraxenName.isEmpty()) {
            return sanitizeLegacy(oraxenName);
        }

        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            net.kyori.adventure.text.Component display = meta.displayName();
            if (display != null) {
                String metaName = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand()
                        .serialize(display);
                if (metaName != null && !metaName.isEmpty()) {
                    return sanitizeLegacy(metaName);
                }
            }
        }

        String legacy = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand()
                .serialize(net.kyori.adventure.text.Component.translatable(item));
        return sanitizeLegacy(legacy);
    }

    private String sanitizeLegacy(String legacyText) {
        if (legacyText == null) return null;
        return legacyText.replace("§r", "").replace("&r", "");
    }
}

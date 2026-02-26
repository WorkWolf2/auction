package fr.hyping.hypingauctions.hook.placeholder;

import fr.hyping.hypingauctions.hook.IPlaceholderExtension;
import fr.hyping.hypingauctions.manager.CategoryManager;
import fr.hyping.hypingauctions.manager.HistoryManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.PlayerContext;
import java.util.List;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Nullable;

/**
 * Indexed placeholder for displaying raw potion type IDs Format: %hauctions_potion-type_auctions_1%
 * Returns the raw potion type ID (e.g., "NIGHT_VISION", "SPEED", etc.) This is used for HypingMenus
 * potion-type configuration
 */
public class PotionIdPlaceholder implements IPlaceholderExtension {
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
                            ? this.getPotionId(context.getFilteredAuctions(), index)
                            : this.getPotionId(player.getSales(), index);
            case "bought" -> this.getPotionId(player.getPurchases(), index);
            case "expired" -> this.getPotionId(player.getExpired(), index);
            case "sales" -> this.getPotionId(player.getSales(), index);
            case "expiredsales" -> this.getPotionId(player.getExpiredSales(), index);
            case "history" ->
                    this.getPotionId(HistoryManager.getPlayerHistory(player.getPlayer()), index);
            case "similar" ->
                    context != null ? this.getPotionId(context.getSimilarAuctions(), index) : null;
            default -> null;
        };
    }

    private String getPotionId(List<Auction> auctions, int index) {
        if (auctions == null || index >= auctions.size()) return null;

        ItemStack item = auctions.get(index).getItem();

        // Check if the item is a potion
        if (item.getItemMeta() instanceof PotionMeta potionMeta) {
            // Try to use the newer API first (for newer Minecraft versions)
            try {
                PotionType potionType = potionMeta.getBasePotionType();
                if (potionType != null && !potionType.name().equals("UNCRAFTABLE")) {
                    return potionType.name();
                }
            } catch (NoSuchMethodError ignored) {
                // Method not available, fall through to legacy API
            }

            // Fall back to the older API or when newer API returned null/UNCRAFTABLE
            try {
                PotionData potionData = potionMeta.getBasePotionData();
                PotionType potionType = potionData.getType();
                if (potionType != null) {
                    return potionType.name();
                }
            } catch (Throwable ignored) {
                // Ignore and use default fallback below
            }
        }

        return "WATER"; // Default fallback for non-potions or unknown potion types
    }
}

package fr.hyping.hypingauctions.hook.placeholder;

import fr.hyping.hypingauctions.hook.IPlaceholderExtension;
import fr.hyping.hypingauctions.manager.CategoryManager;
import fr.hyping.hypingauctions.manager.HistoryManager;
import fr.hyping.hypingauctions.manager.PotionTranslationManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.PlayerContext;
import java.util.List;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.jetbrains.annotations.Nullable;

/**
 * Indexed placeholder for displaying translated potion display names Format:
 * %hauctions_potion-displayname_auctions_1% Returns the translated, styled potion display name
 * (e.g., "Vision nocturne") This follows the new naming convention for HypingMenus integration
 */
public class PotionTypeIndexedPlaceholder implements IPlaceholderExtension {
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
                            ? this.getPotionType(context.getFilteredAuctions(), index)
                            : this.getPotionType(player.getSales(), index);
            case "bought" -> this.getPotionType(player.getPurchases(), index);
            case "expired" -> this.getPotionType(player.getExpired(), index);
            case "sales" -> this.getPotionType(player.getSales(), index);
            case "expiredsales" -> this.getPotionType(player.getExpiredSales(), index);
            case "history" ->
                    this.getPotionType(HistoryManager.getPlayerHistory(player.getPlayer()), index);
            case "similar" ->
                    context != null ? this.getPotionType(context.getSimilarAuctions(), index) : null;
            default -> null;
        };
    }

    private String getPotionType(List<Auction> auctions, int index) {
        if (auctions == null || index >= auctions.size()) return null;

        ItemStack item = auctions.get(index).getItem();

        // Check if the item is a potion
        if (item.getItemMeta() instanceof PotionMeta) {
            // Use the potion translation manager for consistent formatting
            PotionTranslationManager translationManager = PotionTranslationManager.getInstance();
            return translationManager.translatePotion(item);
        }

        return "Not a potion";
    }
}

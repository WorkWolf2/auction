package fr.hyping.hypingauctions.hook.placeholder;

import fr.hyping.hypingauctions.cache.PlayerNameCache;
import fr.hyping.hypingauctions.hook.IPlaceholderExtension;
import fr.hyping.hypingauctions.manager.CategoryManager;
import fr.hyping.hypingauctions.manager.HistoryManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.PlayerContext;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public class SellerPlaceholder implements IPlaceholderExtension {
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
                            ? this.getSellerName(context.getFilteredAuctions(), index)
                            : this.getSellerName(player.getSales(), index);
            case "bought" -> this.getSellerName(player.getPurchases(), index);
            case "expired" -> this.getSellerName(player.getExpired(), index);
            case "sales" -> this.getSellerName(player.getSales(), index);
            case "expiredsales" -> this.getSellerName(player.getExpiredSales(), index);
            case "history" ->
                    this.getSellerName(HistoryManager.getPlayerHistory(player.getPlayer()), index);
            case "similar" ->
                    context != null ? this.getSellerName(context.getSimilarAuctions(), index) : null;
            default -> null;
        };
    }

    private String getSellerName(List<Auction> sales, int index) {
        if (index >= sales.size()) return null;

        // Use cached player name lookup to avoid expensive OfflinePlayer.getName() calls
        // that cause thread saturation due to NBT file I/O operations
        return PlayerNameCache.getInstance().getPlayerName(sales.get(index).getSeller().getPlayer());
    }
}

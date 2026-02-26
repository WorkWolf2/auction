package fr.hyping.hypingauctions.hook.placeholder;

import fr.hyping.hypingauctions.hook.IPlaceholderExtension;
import fr.hyping.hypingauctions.manager.CategoryManager;
import fr.hyping.hypingauctions.manager.HistoryManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.PlayerContext;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public class CurrencyPlaceholder implements IPlaceholderExtension {
    @Override
    public @Nullable String onReplace(AuctionPlayer player, String name, String[] args) {
        if (args.length < 2 || args.length > 3) return null;

        boolean formatted = args.length == 3 && args[0].equals("formatted");

        int index;
        try {
            index = Integer.parseInt(args[args.length - 1]) - 1;
            if (index < 0) return null;
        } catch (NumberFormatException e) {
            return null;
        }

        PlayerContext context = player.getContext();
        if (context != null && args[formatted ? 1 : 0].equalsIgnoreCase("auctions"))
            index += context.getPage() * CategoryManager.getItemsPerPage();

        return switch (args[formatted ? 1 : 0].toLowerCase()) {
            case "auctions" ->
                    context != null
                            ? this.getCurrency(context.getFilteredAuctions(), index, formatted)
                            : this.getCurrency(player.getSales(), index, formatted);
            case "bought" -> this.getCurrency(player.getPurchases(), index, formatted);
            case "expired" -> this.getCurrency(player.getExpired(), index, formatted);
            case "sales" -> this.getCurrency(player.getSales(), index, formatted);
            case "expiredsales" -> this.getCurrency(player.getExpiredSales(), index, formatted);
            case "history" ->
                    this.getCurrency(HistoryManager.getPlayerHistory(player.getPlayer()), index, formatted);
            case "similar" ->
                    context != null ? this.getCurrency(context.getSimilarAuctions(), index, formatted) : null;
            default -> null;
        };
    }

    private String getCurrency(List<Auction> sales, int index, boolean formatted) {
        if (index >= sales.size()) return null;
        return formatted
                ? sales.get(index).getCurrency().name()
                : sales.get(index).getCurrency().counter().getName();
    }
}

package fr.hyping.hypingauctions.hook.placeholder;

import fr.hyping.hypingauctions.hook.IPlaceholderExtension;
import fr.hyping.hypingauctions.manager.CategoryManager;
import fr.hyping.hypingauctions.manager.HistoryManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.PlayerContext;
import fr.hyping.hypingauctions.util.CustomModelDataUtil;
import java.util.List;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class CustomModelDataPlaceholder implements IPlaceholderExtension {

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
                            ? this.getCustomModelData(context.getFilteredAuctions(), index)
                            : this.getCustomModelData(player.getSales(), index);
            case "bought" -> this.getCustomModelData(player.getPurchases(), index);
            case "expired" -> this.getCustomModelData(player.getExpired(), index);
            case "sales" -> this.getCustomModelData(player.getSales(), index);
            case "expiredsales" -> this.getCustomModelData(player.getExpiredSales(), index);
            case "history" ->
                    this.getCustomModelData(HistoryManager.getPlayerHistory(player.getPlayer()), index);
            case "similar" ->
                    context != null ? this.getCustomModelData(context.getSimilarAuctions(), index) : null;
            default -> null;
        };
    }

    private String getCustomModelData(List<Auction> sales, int index) {
        if (index >= sales.size()) return null;

        ItemStack itemStack = sales.get(index).getItem();
        return CustomModelDataUtil.getCustomModelDataAsString(itemStack);
    }
}

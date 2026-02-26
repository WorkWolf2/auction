package fr.hyping.hypingauctions.hook.placeholder;

import fr.hyping.hypingauctions.command.sub.ShulkerboxCommand;
import fr.hyping.hypingauctions.hook.IPlaceholderExtension;
import fr.hyping.hypingauctions.manager.CategoryManager;
import fr.hyping.hypingauctions.manager.HistoryManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.PlayerContext;
import java.util.List;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ShulkerboxPlaceholder implements IPlaceholderExtension {
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
                            ? this.isShulkerbox(context.getFilteredAuctions(), index)
                            : this.isShulkerbox(player.getSales(), index);
            case "bought" -> this.isShulkerbox(player.getPurchases(), index);
            case "expired" -> this.isShulkerbox(player.getExpired(), index);
            case "sales" -> this.isShulkerbox(player.getSales(), index);
            case "expiredsales" -> this.isShulkerbox(player.getExpiredSales(), index);
            case "history" ->
                    this.isShulkerbox(HistoryManager.getPlayerHistory(player.getPlayer()), index);
            default -> null;
        };
    }

    private String isShulkerbox(List<Auction> auctions, int index) {
        if (index >= auctions.size()) return "false";
        ItemStack item = auctions.get(index).getItem();
        return Boolean.toString(ShulkerboxCommand.isShulkerBox(item));
    }
}

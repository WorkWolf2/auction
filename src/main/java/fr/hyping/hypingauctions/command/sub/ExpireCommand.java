package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.manager.AuctionManager;
import fr.hyping.hypingauctions.manager.PlayerManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.util.Messages;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExpireCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 2 || args.length > 3) return false;

        OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
        if (!player.hasPlayedBefore()) {
            Messages.PLAYER_NOT_EXIST.send(sender);
            return true;
        }

        AuctionPlayer ap = PlayerManager.getPlayer(player);
        if (ap == null) {
            Messages.NO_CONTEXT.send(sender);
            return true;
        }

        List<Auction> sales = ap.getExpiredSales();

        int id;
        try {
            id = Integer.parseInt(args[1]) - 1;
        } catch (NumberFormatException e) {
            Messages.INVALID_ARGUMENTS.send(sender);
            return true;
        }

        if (args.length == 3 && args[2].equalsIgnoreCase("real")) sales = ap.getSales();

        if (id < 0 || id >= sales.size()) {
            Messages.INVALID_AUCTION_ID.send(sender);
            return true;
        }

        Auction auction = sales.get(id);
        AuctionManager.expireAuction(auction);
        if (ap.getContext() != null) ap.getContext().reloadFilteredAuctions();

        Messages.SUCCESS_AUCTION_EXPIRED.send(sender);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 2) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
            if (!player.hasPlayedBefore()) return null;
            List<String> list = new ArrayList<>();
            for (int i = 0; i < PlayerManager.getPlayer(player).getExpiredSales().size(); i++)
                list.add(String.valueOf(i + 1));
            return list;
        }
        if (args.length == 3) return List.of("real");
        return Arrays.stream(Bukkit.getOfflinePlayers()).map(OfflinePlayer::getName).toList();
    }
}

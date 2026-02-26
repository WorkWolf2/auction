package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.manager.PlayerManager;
import fr.hyping.hypingauctions.manager.PremiumTargetManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Command to set premium promotion target (called from HypingMenus) Usage: /hauctions
 * premium-target <player> <auction_index>
 */
public class PremiumTargetCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.premium-target.usage"));
            return true;
        }

        // Get target player
        Player targetPlayer = sender.getServer().getPlayer(args[0]);
        if (targetPlayer == null) {
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.premium-target.player-not-found",
                            Component.text(args[0])));
            return true;
        }

        // Parse auction index
        int auctionIndex;
        try {
            auctionIndex = Integer.parseInt(args[1]) - 1; // Convert to 0-based
            if (auctionIndex < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.premium-target.invalid-index"));
            return true;
        }

        // Get the auction from player's sales
        AuctionPlayer auctionPlayer = PlayerManager.getPlayer(targetPlayer);
        List<Auction> playerSales = auctionPlayer.getSales();

        if (auctionIndex >= playerSales.size()) {
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.premium-target.auction-not-found",
                            Component.text(auctionIndex + 1)));
            return true;
        }

        Auction auction = playerSales.get(auctionIndex);

        // Set the target
        PremiumTargetManager.setTarget(targetPlayer, auction);

        // Success message (optional, usually silent for menu commands)
        if (sender instanceof Player) {
            // Only send message if executed by a player (not from console/menu)
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.premium-target.success"));
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 1) {
            // Tab complete player names
            return sender.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        } else if (args.length == 2) {
            // Tab complete auction indices
            Player targetPlayer = sender.getServer().getPlayer(args[0]);
            if (targetPlayer != null) {
                AuctionPlayer auctionPlayer = PlayerManager.getPlayer(targetPlayer);
                List<Auction> sales = auctionPlayer.getSales();

                return sales.stream()
                        .filter(auction -> !auction.isPremium() && !auction.isExpired())
                        .map(auction -> String.valueOf(sales.indexOf(auction) + 1))
                        .toList();
            }
        }
        return null;
    }
}

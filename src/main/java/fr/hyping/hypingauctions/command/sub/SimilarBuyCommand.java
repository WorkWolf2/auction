package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.manager.BanManager;
import fr.hyping.hypingauctions.manager.PlayerManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.util.Messages;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Command for opening the purchase menu for a specific similar item Usage: /hauctions similar-buy
 * <player> <auction_index>
 *
 * <p>This command allows players to open the purchase menu for any specific auction by its index in
 * the similar items list, which is perfect for similar items comparative viewing where you want to
 * purchase a specific similar item.
 */
public class SimilarBuyCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length < 2) {
            return false;
        }

        // Get the target player
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[0]);
        if (!targetPlayer.hasPlayedBefore()) {
            Messages.PLAYER_NOT_EXIST.send(sender);
            return true;
        }

        // Check if player is banned
        if (BanManager.isBanned(targetPlayer)) {
            Messages.YOUR_ARE_BANNED.send(sender);
            return true;
        }

        // Parse auction index (1-based, convert to 0-based)
        int auctionIndex;
        try {
            auctionIndex = Integer.parseInt(args[1]) - 1;
            if (auctionIndex < 0) {
                Messages.INVALID_ARGUMENTS.send(sender);
                return true;
            }
        } catch (NumberFormatException e) {
            Messages.INVALID_ARGUMENTS.send(sender);
            return true;
        }

        // Get the auction player and their context
        AuctionPlayer auctionPlayer = PlayerManager.getPlayer(targetPlayer);

        // Ensure player has a context
        if (auctionPlayer.getContext() == null) {
            Messages.NO_CONTEXT.send(sender);
            return true;
        }

        // Get the auction from the similar items list
        List<Auction> similarAuctions = auctionPlayer.getContext().getSimilarAuctions();

        if (auctionIndex >= similarAuctions.size()) {
            Messages.SALE_NOT_EXIST.send(sender);
            return true;
        }

        Auction targetAuction = similarAuctions.get(auctionIndex);

        if (targetAuction == null) {
            Messages.SALE_NOT_EXIST.send(sender);
            return true;
        }

        // Check if auction is expired
        if (targetAuction.isExpired()) {
            Messages.SALE_EXPIRED.send(sender);
            return true;
        }

        // Check if player is trying to buy their own auction
        if (targetPlayer.getUniqueId().equals(targetAuction.getSeller().getPlayer().getUniqueId())) {
            Messages.CANNOT_BUY_OWN.send(sender);
            return true;
        }

        // Set the target auction for purchase
        auctionPlayer.getContext().setTarget(targetAuction);

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
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Auction IDs are too dynamic and numerous for tab completion
        return List.of();
    }
}

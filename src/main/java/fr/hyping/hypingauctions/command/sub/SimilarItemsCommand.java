package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.manager.AuctionManager;
import fr.hyping.hypingauctions.manager.BanManager;
import fr.hyping.hypingauctions.manager.PlayerManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.MenuType;
import fr.hyping.hypingauctions.util.Messages;
import java.util.ArrayList;
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
 * Command for viewing similar items based on an auction ID Usage: /hauctions similar <auction_id>
 *
 * <p>This command finds an auction by its ID, sets it as the target auction, and switches the
 * player's menu to SIMILAR_ITEMS view to show all similar items (items with the same Material and
 * CustomModelData).
 */
public class SimilarItemsCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.similar.player-only"));
            return true;
        }

        if (args.length < 1) {
            return false; // Show usage message
        }

        // Check if player is banned
        if (BanManager.isBanned(player)) {
            Messages.YOUR_ARE_BANNED.send(sender);
            return true;
        }

        // Parse auction index (1-based, convert to 0-based)
        int auctionIndex;
        try {
            auctionIndex = Integer.parseInt(args[0]) - 1;
            if (auctionIndex < 0) {
                Messages.INVALID_ARGUMENTS.send(sender);
                return true;
            }
        } catch (NumberFormatException e) {
            Messages.INVALID_ARGUMENTS.send(sender);
            return true;
        }

        // Get the auction player to access their current context
        AuctionPlayer auctionPlayer = PlayerManager.getPlayer(player);

        // Ensure player has a context with filtered auctions
        if (auctionPlayer.getContext() == null) {
            Messages.NO_CONTEXT.send(sender);
            return true;
        }

        // Get the auction from the current filtered auctions list
        List<Auction> filteredAuctions = auctionPlayer.getContext().getFilteredAuctions();
        if (auctionIndex >= filteredAuctions.size()) {
            Messages.SALE_NOT_EXIST.send(sender);
            return true;
        }

        Auction targetAuction = filteredAuctions.get(auctionIndex);

        // Check if auction is expired
        if (targetAuction.isExpired()) {
            Messages.SALE_EXPIRED.send(sender);
            return true;
        }

        // Open the menu and set the target auction
        // This will automatically populate similar items via setTarget()
        if (!auctionPlayer.openMenu(MenuType.SIMILAR_ITEMS, true)) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.similar.open-failed"));
            return true;
        }

        // Set the target auction (this auto-populates similar items)
        auctionPlayer.getContext().setTarget(targetAuction);

        // Reload filtered auctions to show similar items
        auctionPlayer.getContext().reloadFilteredAuctions();


        AuctionManager.executeSimilarItemsCommands(player);
        // Send confirmation message
        int similarCount = auctionPlayer.getContext().getSimilarAuctions().size();
        sender.sendMessage(
                Component.translatable(
                        "hyping.hypingauctions.command.similar.showing",
                        Component.text(similarCount),
                        Component.text(auctionIndex + 1)));

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            // Tab complete auction indices based on player's current filtered auctions
            AuctionPlayer auctionPlayer = PlayerManager.getPlayer(player);
            if (auctionPlayer.getContext() != null) {
                List<Auction> filteredAuctions = auctionPlayer.getContext().getFilteredAuctions();
                List<String> indices = new ArrayList<>();

                // Generate indices 1 to size of filtered auctions
                for (int i = 1; i <= Math.min(filteredAuctions.size(), 20); i++) {
                    String index = String.valueOf(i);
                    if (index.startsWith(args[0])) {
                        indices.add(index);
                    }
                }

                return indices;
            }
        }

        return new ArrayList<>();
    }
}

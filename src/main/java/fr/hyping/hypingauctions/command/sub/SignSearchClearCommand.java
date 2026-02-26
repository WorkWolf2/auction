package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.manager.PlayerManager;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.MenuType;
import fr.hyping.hypingauctions.manager.object.PlayerContext;
import fr.hyping.hypingauctions.util.Messages;
import java.util.List;
import net.kyori.adventure.text.Component;
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
 * Console command for clearing search parameters for players Usage: /hauctions sign-search-clear
 * <player>
 */
public class SignSearchClearCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {

        if (args.length < 1) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.sign-search-clear.usage"));
            return true;
        }

        // Get target player
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore()) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.sign-search-clear.never-played"));
            return true;
        }

        // Check if player is online (required for context)
        Player onlineTarget = target.getPlayer();
        if (onlineTarget == null) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.sign-search-clear.target-offline"));
            return true;
        }

        try {
            // Get auction player
            AuctionPlayer auctionPlayer = PlayerManager.getPlayer(target);

            // Check if player has a context (menu open)
            PlayerContext context = auctionPlayer.getContext();
            if (context == null) {
                // Open auction menu first
                auctionPlayer.openMenu(MenuType.AUCTION);
                context = auctionPlayer.getContext();
            }

            // Clear the search filter
            String previousSearch = context.getFilter().getSearch();
            context.getFilter().setSearch(null);
            context.reloadFilteredAuctions();

            // Send confirmation messages
            if (previousSearch != null && !previousSearch.isEmpty()) {
                sender.sendMessage(
                        Component.translatable(
                                "hyping.hypingauctions.command.sign-search-clear.cleared",
                                Component.text(previousSearch),
                                Component.text(target.getName())));
                Messages.SEARCH_FILTER_CLEARED.send(onlineTarget);
            } else {
                sender.sendMessage(
                        Component.translatable(
                                "hyping.hypingauctions.command.sign-search-clear.no-parameter",
                                Component.text(target.getName())));
                // REMOVE MESSAGE -> onlineTarget.sendMessage("§7No search filter was active to clear.");
            }

        } catch (Exception e) {
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.sign-search-clear.error",
                            Component.text(e.getMessage())));
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
            // Tab complete online player names
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }

        return List.of();
    }
}

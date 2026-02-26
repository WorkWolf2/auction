package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.manager.PlayerManager;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.MenuType;
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
 * Console command for setting search parameters for players via sign search Usage: /hauctions
 * sign-search-set <player> <search_term>
 */
public class SignSearchSetCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {

        if (args.length < 2) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.sign-search-set.usage"));
            return true;
        }

        // Get target player
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore()) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.sign-search-set.never-played"));
            return true;
        }

        // Check if player is online (required for opening menu)
        Player onlineTarget = target.getPlayer();
        if (onlineTarget == null) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.sign-search-set.target-offline"));
            return true;
        }

        // Combine all arguments after player name as search term
        StringBuilder searchTermBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) searchTermBuilder.append(" ");
            searchTermBuilder.append(args[i]);
        }
        String searchTerm = searchTermBuilder.toString().trim();

        if (searchTerm.isEmpty()) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.sign-search-set.empty-term"));
            return true;
        }

        try {
            // Get auction player and open auction menu
            AuctionPlayer auctionPlayer = PlayerManager.getPlayer(target);
            auctionPlayer.openMenu(MenuType.AUCTION);

            // Set the search filter
            auctionPlayer.getContext().getFilter().setSearch(searchTerm);
            auctionPlayer.getContext().reloadFilteredAuctions();

            // Send confirmation messages
            Messages.SEARCH_SET.send(sender, searchTerm);

            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.sign-search-set.sender-success",
                            Component.text(searchTerm),
                            Component.text(target.getName())));
            onlineTarget.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.sign-search-set.player-success",
                            Component.text(searchTerm)));

        } catch (Exception e) {
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.sign-search-set.error",
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

        if (args.length == 2) {
            // Suggest common search terms
            return List.of("diamond", "iron", "gold", "enchanted", "sword", "armor", "tool", args[1]);
        }

        return List.of();
    }
}

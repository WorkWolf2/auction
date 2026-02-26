package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.manager.PlayerManager;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.PlayerContext;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RefreshCommand implements CommandExecutor {
    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {

        // If no arguments provided and sender is a player, refresh their own auctions
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(
                        Component.translatable("hyping.hypingauctions.command.refresh.console-requires-target"));
                return true;
            }

            AuctionPlayer ap = PlayerManager.getPlayer(player);
            if (ap == null) {
                sender.sendMessage(
                        Component.translatable("hyping.hypingauctions.command.refresh.player-data-not-found"));
                return true;
            }

            PlayerContext context = ap.getContext();
            if (context == null) {
                sender.sendMessage(
                        Component.translatable("hyping.hypingauctions.command.refresh.no-open-menu"));
                return true;
            }

            context.reloadFilteredAuctions();
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.refresh.self-success"));
            return true;
        }

        // Admin functionality - refresh another player's auctions
        if (!sender.hasPermission("hauctions.admin")) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.refresh.no-permission"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        AuctionPlayer ap = PlayerManager.getPlayer(target);

        if (ap == null) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.refresh.player-not-found"));
            return true;
        }

        PlayerContext context = ap.getContext();
        if (context == null) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.refresh.target-no-context"));
            return true;
        }

        context.reloadFilteredAuctions();
        sender.sendMessage(
                Component.translatable(
                        "hyping.hypingauctions.command.refresh.target-success",
                        Component.text(target.getName())));
        return true;
    }
}

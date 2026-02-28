package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.manager.PlayerManager;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.MenuType;
import fr.hyping.hypingauctions.util.Messages;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OpenCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        // If only one argument provided and sender is a player, open their own menu
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(
                        Component.translatable("hyping.hypingauctions.command.open.console-requires-target"));
                return true;
            }

            MenuType type = parseMenuType(args[0]);
            if (type == null) {
                Messages.INVALID_TYPE.send(sender, args[0]);
                return true;
            }

            AuctionPlayer ap = PlayerManager.getPlayer(player);
            if (ap == null) {
                Messages.PLAYER_NOT_EXIST.send(sender);
                return true;
            }

            // Check if player has an active SignSearch filter and preserve context only in that case
            boolean shouldReset = true;
            if (ap.getContext() != null
                    && ap.getContext().getMenu() == type
                    && ap.getContext().getFilter() != null
                    && ap.getContext().getFilter().getSearch() != null) {
                shouldReset = false;
            }

            if (!ap.openMenu(type, shouldReset)) {
                sender.sendMessage(
                        Component.translatable(
                                "hyping.hypingauctions.command.open.open-failed",
                                Component.text(args[0])));
            }
            return true;
        }

        // Two arguments - admin functionality to open menu for another player
        if (args.length < 2) return false;

        if (!sender.hasPermission("hauctions.admin")) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.open.no-permission"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        AuctionPlayer ap = PlayerManager.getPlayer(target);

        System.out.println(target.getName());

        if (ap == null) {
            Messages.PLAYER_NOT_EXIST.send(sender);
            return true;
        }

        MenuType type = parseMenuType(args[0]);
        if (type == null) {
            Messages.INVALID_TYPE.send(sender, args[0]);
            return true;
        }

        // Check if player has an active SignSearch filter and preserve context only in that case
        boolean shouldReset = true;
        if (ap.getContext() != null
                && ap.getContext().getMenu() == type
                && ap.getContext().getFilter() != null
                && ap.getContext().getFilter().getSearch() != null) {
            shouldReset = false;
        }

        if (!ap.openMenu(type, shouldReset))
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.open.open-failed-for-player",
                            Component.text(args[1]),
                            Component.text(args[0])));
        return true;
    }

    private MenuType parseMenuType(String menuName) {
        return switch (menuName.toLowerCase()) {
            case "auctions" -> MenuType.AUCTION;
            case "bought" -> MenuType.BOUGHT;
            case "expired" -> MenuType.EXPIRED;
            case "history" -> MenuType.HISTORY;
            default -> null;
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("auctions", "bought", "expired", "history");
        }
        if (args.length == 2 && sender.hasPermission("hauctions.admin")) {
            return List.of("auctions", "bought", "expired", "history");
        }
        return null;
    }
}

package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.manager.PlayerManager;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.MenuType;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length != 2) return false;

        OfflinePlayer opener = sender.getServer().getOfflinePlayer(args[0]);
        if (!opener.hasPlayedBefore()) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.player.never-played"));
            return true;
        }

        AuctionPlayer auctionPlayer = PlayerManager.getPlayer(opener);
        auctionPlayer.openMenu(MenuType.AUCTION);

        if (args[1].equalsIgnoreCase("clear")) auctionPlayer.getContext().getFilter().setPlayer(null);
        else {
            OfflinePlayer target = sender.getServer().getOfflinePlayer(args[1]);
            if (!target.hasPlayedBefore()) {
                sender.sendMessage(
                        Component.translatable("hyping.hypingauctions.command.player.never-played"));
                return true;
            }
            auctionPlayer.getContext().getFilter().setPlayer(target);
        }

        auctionPlayer.getContext().reloadFilteredAuctions();
        sender.sendMessage(
                Component.translatable(
                        "hyping.hypingauctions.command.player.set",
                        Component.text(args[1]),
                        Component.text(opener.getName())));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return null;

        return Arrays.stream(Bukkit.getOfflinePlayers()).map(OfflinePlayer::getName).toList();
    }
}

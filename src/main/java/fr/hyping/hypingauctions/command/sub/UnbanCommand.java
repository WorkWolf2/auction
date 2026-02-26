package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.manager.BanManager;
import fr.hyping.hypingauctions.util.Messages;
import java.util.List;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UnbanCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length < 1) return false;

        OfflinePlayer target = sender.getServer().getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore()) {
            Messages.PLAYER_NOT_EXIST.send(sender);
            return true;
        }

        if (!BanManager.isBanned(target)) {
            Messages.PLAYER_NOT_BANNED.send(sender);
            return true;
        }

        // Unban the player
        BanManager.unbanPlayer(target);
        Messages.PLAYER_UNBANNED.send(sender);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        return BanManager.getBanned().stream().map(OfflinePlayer::getName).toList();
    }
}

package fr.hyping.hypingauctions.command.sub;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SearchCommand implements CommandExecutor, TabCompleter {

    private final SignSearchCommand signSearchCommand = new SignSearchCommand();

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {

        // Only allow players to use this command
        if (!(sender instanceof Player player)) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.search.player-only"));
            return true;
        }

        // Always redirect to SignSearchCommand - it handles both sign GUI and direct search
        return signSearchCommand.onCommand(player, command, label, args);
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {

        // Redirect tab completion to SignSearchCommand
        return signSearchCommand.onTabComplete(sender, command, label, args);
    }
}

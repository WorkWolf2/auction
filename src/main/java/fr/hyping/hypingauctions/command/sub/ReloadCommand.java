package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.HypingAuctions;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        sender.sendMessage(
                Component.translatable("hyping.hypingauctions.command.reload.start"));

        HypingAuctions.getInstance().reloadConfigs();

        sender.sendMessage(
                Component.translatable("hyping.hypingauctions.command.reload.complete"));
        return true;
    }
}

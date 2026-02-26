package fr.hyping.hypingauctions.command;

import fr.hyping.hypingauctions.command.sub.SearchCommand;
import fr.hyping.hypingauctions.manager.AuctionManager;
import fr.hyping.hypingauctions.util.CommandBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AuctionCommand implements CommandExecutor, TabCompleter {

    private static final HashMap<String, CommandBuilder> commands = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(
                        Component.translatable("hyping.hypingauctions.command.root.player-only"));
                return true;
            }

            AuctionManager.executeMainCommands(player);
            return true;
        }

        CommandBuilder commandBuilder = commands.get(args[0]);
        if (commandBuilder == null) {
            if (sender instanceof Player) {
                new SearchCommand().onCommand(sender, command, label, args);
                return true;
            }
            fr.hyping.hypingauctions.util.Messages.INVALID_COMMAND.send(sender);
            return true;
        }

        if (commandBuilder.getPermission() != null
                && !sender.hasPermission(commandBuilder.getPermission())) {
            fr.hyping.hypingauctions.util.Messages.NO_PERMISSION.send(sender);
            return true;
        }

        if (commandBuilder.getCommandExecutor() == null) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.root.not-available"));
            return true;
        }

        boolean ok =
                commandBuilder
                        .getCommandExecutor()
                        .onCommand(sender, command, label, Arrays.copyOfRange(args, 1, args.length));
        if (!ok) {
            // Custom usage for sell subcommand if configured
            if ("sell".equalsIgnoreCase(commandBuilder.getName())) {
                fr.hyping.hypingauctions.util.Messages.SELL_USAGE.send(sender);
            } else {
                sender.sendMessage(
                        Component.translatable(
                                "hyping.hypingauctions.command.root.usage",
                                Component.text(commandBuilder.getUsage())));
            }
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length >= 1) {
            CommandBuilder first = commands.get(args[0]);
            if (first == null && sender instanceof Player) {
                return new SearchCommand().onTabComplete(sender, command, label, args);
            }
        }

        if (args.length == 1) {
            List<String> allowedCommands = new ArrayList<>();
            for (CommandBuilder commandBuilder : commands.values()) {
                if (commandBuilder.getPermission() == null
                        || sender.hasPermission(commandBuilder.getPermission()))
                    allowedCommands.add(commandBuilder.getName());
            }

            StringUtil.copyPartialMatches(args[0], allowedCommands, completions);
        } else if (args.length > 1) {
            CommandBuilder commandBuilder = commands.get(args[0]);
            if (commandBuilder != null
                    && commandBuilder.getTabCompleter() != null
                    && commandBuilder.getPermission() != null
                    && sender.hasPermission(commandBuilder.getPermission()))
                return commandBuilder
                        .getTabCompleter()
                        .onTabComplete(sender, command, label, Arrays.copyOfRange(args, 1, args.length));
        }
        return completions;
    }

    public static void registerCommand(CommandBuilder commandBuilder) {
        commands.put(commandBuilder.getName(), commandBuilder);
    }

    public static List<CommandBuilder> getCommands() {
        return new ArrayList<>(commands.values());
    }
}

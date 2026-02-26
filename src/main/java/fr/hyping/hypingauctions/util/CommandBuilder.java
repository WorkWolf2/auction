package fr.hyping.hypingauctions.util;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class CommandBuilder {

    private final String name;
    private String description = null;
    private String usage = null;
    private String permission = null;
    private TabCompleter tabCompleter = null;
    private CommandExecutor commandExecutor = null;

    public CommandBuilder(String name) {
        this.name = name;
    }

    public boolean canExecute(CommandSender sender) {
        return permission == null || sender.hasPermission(permission);
    }

    public CommandBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public CommandBuilder setUsage(String usage) {
        this.usage = usage;
        return this;
    }

    public CommandBuilder setPermission(String permission) {
        this.permission = permission;
        return this;
    }

    public CommandBuilder setTabCompleter(TabCompleter tabCompleter) {
        this.tabCompleter = tabCompleter;
        return this;
    }

    public CommandBuilder setCommandExecutor(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
        return this;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getUsage() {
        return usage;
    }

    public String getPermission() {
        return permission;
    }

    public TabCompleter getTabCompleter() {
        return tabCompleter;
    }

    public CommandExecutor getCommandExecutor() {
        return commandExecutor;
    }
}

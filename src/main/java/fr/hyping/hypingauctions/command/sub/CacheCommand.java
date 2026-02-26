package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.cache.PlayerNameCache;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
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
 * Command for managing the player name cache. Provides administrators with tools to monitor and
 * control the cache.
 */
public class CacheCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "info":
                handleInfo(sender);
                break;
            case "clear":
                handleClear(sender);
                break;
            case "invalidate":
                if (args.length < 2) {
                    sender.sendMessage(
                            Component.translatable("hyping.hypingauctions.command.cache.usage.invalidate"));
                    return true;
                }
                handleInvalidate(sender, args[1]);
                break;
            case "reload":
                handleReload(sender);
                break;
            case "stats":
                handleStats(sender);
                break;
            default:
                sendUsage(sender);
                break;
        }

        return true;
    }

    private void handleInfo(CommandSender sender) {
        PlayerNameCache cache = PlayerNameCache.getInstance();
        sender.sendMessage(
                Component.translatable("hyping.hypingauctions.command.cache.info.header"));
        sender.sendMessage(
                Component.translatable(
                        "hyping.hypingauctions.command.cache.info.details",
                        Component.text(cache.getCacheInfo())));
    }

    private void handleClear(CommandSender sender) {
        PlayerNameCache cache = PlayerNameCache.getInstance();
        cache.clear();
        sender.sendMessage(
                Component.translatable("hyping.hypingauctions.command.cache.clear.success"));
    }

    private void handleInvalidate(CommandSender sender, String playerName) {
        try {
            // Try to find the player by name first
            Player onlinePlayer = Bukkit.getPlayer(playerName);
            if (onlinePlayer != null) {
                PlayerNameCache.getInstance().invalidate(onlinePlayer);
                sender.sendMessage(
                        Component.translatable(
                                "hyping.hypingauctions.command.cache.invalidate.online",
                                Component.text(onlinePlayer.getName())));
                return;
            }

            // Try to parse as UUID
            UUID uuid;
            try {
                uuid = UUID.fromString(playerName);
            } catch (IllegalArgumentException e) {
                // Not a UUID, try to get offline player by name
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
                if (offlinePlayer.hasPlayedBefore()) {
                    PlayerNameCache.getInstance().invalidate(offlinePlayer);
                    sender.sendMessage(
                            Component.translatable(
                                    "hyping.hypingauctions.command.cache.invalidate.offline",
                                    Component.text(playerName)));
                } else {
                    sender.sendMessage(
                            Component.translatable(
                                    "hyping.hypingauctions.command.cache.invalidate.player-not-found",
                                    Component.text(playerName)));
                }
                return;
            }

            // Invalidate by UUID
            PlayerNameCache.getInstance().invalidate(uuid);
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.cache.invalidate.uuid",
                            Component.text(uuid.toString())));

        } catch (Exception e) {
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.cache.invalidate.error",
                            Component.text(e.getMessage())));
        }
    }

    private void handleReload(CommandSender sender) {
        try {
            PlayerNameCache.getInstance().reload();
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.cache.reload.success"));
        } catch (Exception e) {
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.cache.reload.error",
                            Component.text(e.getMessage())));
        }
    }

    private void handleStats(CommandSender sender) {
        try {
            PlayerNameCache cache = PlayerNameCache.getInstance();
            cache.logStatistics();
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.cache.stats.logged"));
        } catch (Exception e) {
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.cache.stats.error",
                            Component.text(e.getMessage())));
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(
                Component.translatable("hyping.hypingauctions.command.cache.usage.header"));
        sender.sendMessage(
                Component.translatable("hyping.hypingauctions.command.cache.usage.info"));
        sender.sendMessage(
                Component.translatable("hyping.hypingauctions.command.cache.usage.clear"));
        sender.sendMessage(
                Component.translatable("hyping.hypingauctions.command.cache.usage.invalidate"));
        sender.sendMessage(
                Component.translatable("hyping.hypingauctions.command.cache.usage.reload"));
        sender.sendMessage(
                Component.translatable("hyping.hypingauctions.command.cache.usage.stats"));
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("info", "clear", "invalidate", "reload", "stats");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("invalidate")) {
            // Return online player names for tab completion
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }

        return null;
    }
}

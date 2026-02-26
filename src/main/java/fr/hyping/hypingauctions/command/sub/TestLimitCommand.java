package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.manager.LimitManager;
import fr.hyping.hypingauctions.test.LimitMessageTest;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Test command to verify that custom limit messages work correctly. Usage:
 * /hauctions testlimit
 *
 * <p>
 * This command will: 1. Show the player's current limit 2. Show the custom
 * message for their
 * permission level 3. Simulate the limit reached scenario to test the message
 * display
 */
public class TestLimitCommand implements CommandExecutor {

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.testlimit.player-only"));
            return true;
        }

        // Get player's current limit and custom message
        int limit = LimitManager.getLimit(player);
        net.kyori.adventure.text.Component customMessage = LimitManager.getLimitMessage(player);

        // Send test results to player
        player.sendMessage(
                Component.translatable("hyping.hypingauctions.command.testlimit.header"));
        player.sendMessage(
                Component.translatable(
                        "hyping.hypingauctions.command.testlimit.current-limit",
                        Component.text(limit)));

        if (customMessage != null) {
            player.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.testlimit.custom-message"));
            player.sendMessage(customMessage);
            player.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.testlimit.custom-found"));
        } else {
            player.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.testlimit.custom-missing"));
            player.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.testlimit.default-message"));
        }

        // Show detailed test results
        String testResults = LimitMessageTest.testLimitMessage(player);
        String[] lines = testResults.split("\n");
        for (String line : lines) {
            player.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.testlimit.detail-line",
                            Component.text(line)));
        }

        player.sendMessage(
                Component.translatable("hyping.hypingauctions.command.testlimit.footer"));

        return true;
    }
}

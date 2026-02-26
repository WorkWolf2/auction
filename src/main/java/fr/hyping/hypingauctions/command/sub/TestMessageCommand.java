package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.util.Format;
import fr.hyping.hypingauctions.util.Messages;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/** A test command to verify that messages are working correctly with HypingRealtimeTranslation */
public class TestMessageCommand implements CommandExecutor {

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        // Messages now use HypingRealtimeTranslation - always available
        sender.sendMessage(
                Component.translatable("hyping.hypingauctions.command.testmessage.testing"));

        // Test the bought format message
        // Format: {0}=date, {1}=item, {2}=seller, {3}=price
        sender.sendMessage(
                Component.translatable("hyping.hypingauctions.command.testmessage.bought-test"));
        Messages.TRANSACTION_BOUGHT_FORMAT.send(
                sender,
                Format.formatDateFrench(System.currentTimeMillis()),
                "Diamond Sword",
                "Seller123",
                "1,000$");

        // Test the sold format message
        // Format: {0}=date, {1}=item, {2}=buyer, {3}=price
        sender.sendMessage(
                Component.translatable("hyping.hypingauctions.command.testmessage.sold-test"));
        Messages.TRANSACTION_SOLD_FORMAT.send(
                sender,
                Format.formatDateFrench(System.currentTimeMillis()),
                "Diamond Pickaxe",
                "Buyer456",
                "500$");

        return true;
    }
}

package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.gui.ShulkerboxGui;
import fr.hyping.hypingauctions.manager.PremiumSlotManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.util.NumberUtil;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PremiumShulkerboxCommand implements TabExecutor {
    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length != 2) {
            return false;
        }

        Player target = sender.getServer().getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.premium-shulkerbox.player-not-on-server"));
            return true;
        }

        if (!NumberUtil.isInteger(args[1])) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.premium-shulkerbox.invalid-number"));
            return true;
        }
        int slotNumber = Integer.parseInt(args[1]) - 1; // Convert to 0-based

        // Validate slot number range
        if (slotNumber < 0 || slotNumber >= PremiumSlotManager.getMaxSlots()) {
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.premium-shulkerbox.invalid-slot",
                            Component.text(PremiumSlotManager.getMaxSlots())));
            return true;
        }

        // Get the premium auction from the specified slot
        Auction premiumAuction = PremiumSlotManager.getPremiumAuctionBySlot(slotNumber);
        if (premiumAuction == null) {
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.premium-shulkerbox.auction-not-found",
                            Component.text(slotNumber + 1)));
            return true;
        }

        ItemStack item = premiumAuction.getItem();
        if (!ShulkerboxCommand.isShulkerBox(item)) {
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.premium-shulkerbox.not-shulkerbox",
                            Component.text(slotNumber + 1)));
            return true;
        }

        // Open the shulker box inspection GUI
        final InventoryView inventoryView = target.getOpenInventory();
        new ShulkerboxGui(item, inventoryView).open(target);

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 1) {
            // Tab complete player names
            return Arrays.stream(Bukkit.getOfflinePlayers())
                    .map(OfflinePlayer::getName)
                    .filter(name -> name != null && name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        } else if (args.length == 2) {
            // Tab complete slot numbers (1 to max slots)
            return java.util.stream.IntStream.rangeClosed(1, PremiumSlotManager.getMaxSlots())
                    .mapToObj(String::valueOf)
                    .filter(slot -> slot.startsWith(args[1]))
                    .toList();
        }

        return null;
    }
}

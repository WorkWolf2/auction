package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.manager.AuctionManager;
import fr.hyping.hypingauctions.manager.PlayerManager;
import fr.hyping.hypingauctions.manager.TaxesManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.Currency;
import fr.hyping.hypingauctions.util.Messages;
import fr.hyping.hypingcounters.api.CountersAPI;
import fr.hyping.hypingcounters.counter.AbstractCounter;
import fr.hyping.hypingcounters.counter.data.DataParser;
import fr.hyping.hypingcounters.counter.value.AbstractValue;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CancelCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            Messages.PLAYER_ONLY_COMMAND.send(sender);
            return true;
        }

        if (!AuctionManager.isListingCancelEnabled()) {
            sendOrDefault(
                    sender,
                    Messages.CANCEL_DISABLED,
                    Component.translatable("hyping.hypingauctions.command.cancel.disabled"));
            return true;
        }

        long windowMs = AuctionManager.getListingCancelWindowSeconds() * 1000L;
        long now = System.currentTimeMillis();

        AuctionPlayer ap = PlayerManager.getPlayer(player);
        List<Auction> sales = ap.getSales();
        if (sales == null || sales.isEmpty()) {
            sendOrDefault(
                    player,
                    Messages.CANCEL_NOTHING_TO_CANCEL,
                    Component.translatable("hyping.hypingauctions.command.cancel.nothing-to-cancel"));
            return true;
        }

        // Pick the most recent eligible sale by saleDate
        Auction candidate =
                sales.stream()
                        .filter(a -> a.getBuyer() == null)
                        .filter(a -> (now - a.getSaleDate()) <= windowMs)
                        .max(Comparator.comparingLong(Auction::getSaleDate))
                        .orElse(null);

        if (candidate == null) {
            sendOrDefault(
                    player,
                    Messages.CANCEL_NOTHING_TO_CANCEL,
                    Component.translatable("hyping.hypingauctions.command.cancel.window-expired"));
            return true;
        }

        // Atomic DB cancel; if not present in DB, it was already bought/removed
        boolean removed = HypingAuctions.getInstance().getDatabase().cancelAuction(candidate);
        if (!removed) {
            sendOrDefault(
                    player,
                    Messages.CANCEL_ALREADY_SOLD,
                    Component.translatable("hyping.hypingauctions.command.cancel.already-sold"));
            return true;
        }

        // Update in-memory collections now that DB delete succeeded
        AuctionManager.onAuctionCanceled(candidate);

        // Refund tax that was paid at listing time
        long price = candidate.getPrice();
        Currency currency = candidate.getCurrency();
        double taxPercent = TaxesManager.getTax(player, price);
        double taxToRefund = price * taxPercent / 100.0;

        UUID sellerId = player.getUniqueId();
        var data = CountersAPI.getOrCreateIfNotExists(sellerId);
        AbstractCounter counter = currency.counter();
        DataParser<? extends AbstractCounter> parser = counter.getDataParser();
        AbstractValue<?> value = parser.getOrCreate(data, counter);
        value.setDoubleValue(value.getDoubleValue() + taxToRefund, sellerId.toString());

        // Return item to player's inventory (or drop if full)
        ItemStack item = candidate.getItem().clone();
        var leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(s -> player.getWorld().dropItemNaturally(player.getLocation(), s));
        }

        Messages.CANCEL_SUCCESS.send(player);
        return true;
    }

    private void sendOrDefault(
            CommandSender sender,
            Messages message,
            Component fallback) {
        try {
            message.send(sender);
        } catch (Throwable t) {
            sender.sendMessage(fallback);
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        return null;
    }
}

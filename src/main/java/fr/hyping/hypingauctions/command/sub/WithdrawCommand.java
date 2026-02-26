package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.manager.AuctionManager;
import fr.hyping.hypingauctions.manager.PlayerManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.PlayerContext;
import fr.hyping.hypingauctions.util.Configs;
import fr.hyping.hypingauctions.util.Messages;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WithdrawCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length != 3)
            return false;

        OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(args[0]);
        if (op == null) {
            sender.sendMessage(
                    Configs.getLangComponent("command.withdraw.player-never-played"));
            return true;
        }

        Player player = op.getPlayer();
        if (player == null) {
            sender.sendMessage(
                    Configs.getLangComponent("command.withdraw.player-not-connected"));
            return true;
        }

        AuctionPlayer auctionPlayer = PlayerManager.getPlayer(op);
        List<Auction> auctions;
        if (args[1].equalsIgnoreCase("bought")) {
            auctions = auctionPlayer.getPurchases();
        } else if (args[1].equalsIgnoreCase("expired")) {
            auctions = auctionPlayer.getExpired();
        } else {
            return false;
        }

        int id;
        try {
            if (args[2].equalsIgnoreCase("last"))
                id = auctions.size() - 1;
            else
                id = Integer.parseInt(args[2]) - 1;
        } catch (NumberFormatException e) {
            sender.sendMessage(
                    Configs.getLangComponent("command.withdraw.invalid-id"));
            return true;
        }

        if (id < 0 || id >= auctions.size()) {
            sender.sendMessage(
                    Configs.getLangComponent("command.withdraw.auction-not-found"));
            return true;
        }

        PlayerInventory inventory = player.getInventory();
        if (inventory.firstEmpty() == -1) {
            sender.sendMessage(
                    Configs.getLangComponent("command.withdraw.target-inventory-full"));
            player.sendMessage(
                    Configs.getLangComponent("command.withdraw.self-inventory-full"));
            return true;
        }

        Auction auction = auctions.get(id);
        AuctionManager.removeAuction(auction);

        PlayerContext playerCtx = auctionPlayer.getContext();
        if (playerCtx != null)
            playerCtx.reloadFilteredAuctions();

        ItemStack item = auction.getItem();

        inventory.addItem(item);
        net.kyori.adventure.text.Component localizedName = fr.hyping.hypingauctions.manager.AutomaticMaterialTranslationManager
                .getInstance().getLocalizedComponent(player, item);
        Messages.SUCCESS_WITHDRAW.send(player.isOnline() ? player : sender,
                net.kyori.adventure.text.Component.text(item.getAmount()),
                localizedName);

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 2)
            return List.of("bought", "expired");
        if (args.length == 3) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(args[0]);
            if (!op.hasPlayedBefore()) {
                return null;
            }

            AuctionPlayer auctionPlayer = PlayerManager.getPlayer(op);
            List<Auction> auctions = switch (args[1].toLowerCase()) {
                case "bought" -> auctionPlayer.getPurchases();
                case "expired" -> auctionPlayer.getExpired();
                default -> List.of();
            };

            return auctions.stream()
                    .map(auction -> String.valueOf(auctions.indexOf(auction) + 1))
                    .toList();
        }
        return null;
    }
}

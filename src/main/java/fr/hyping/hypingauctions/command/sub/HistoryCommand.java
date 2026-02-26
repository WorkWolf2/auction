package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.cache.PlayerNameCache;
import fr.hyping.hypingauctions.gui.HistoryMenu;
import fr.hyping.hypingauctions.manager.HistoryManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.util.Configs;
import fr.hyping.hypingauctions.util.Format;
import fr.hyping.hypingauctions.util.Messages;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HistoryCommand implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        // Messages now use HypingRealtimeTranslation - always available

        OfflinePlayer targetPlayer;
        String filterArg = null;

        if (args.length > 0) {
            // Check if first argument is a filter keyword (buy/sell variants)
            String firstArg = args[0].toLowerCase();
            boolean isFilter = firstArg.startsWith("buy") || firstArg.startsWith("sell");

            if (isFilter) {
                // First arg is a filter, use sender as target
                if (commandSender instanceof Player) {
                    targetPlayer = (Player) commandSender;
                    filterArg = firstArg;
                } else {
                    // Console must specify a player name
                    Messages.HISTORY_USAGE.send(commandSender);
                    return true;
                }
            } else {
                // First arg is a player name
                // Check permission if sender is a player and trying to view others' history
                if (commandSender instanceof Player
                        && !((Player) commandSender)
                        .getUniqueId()
                        .toString()
                        .equals(Bukkit.getOfflinePlayer(args[0]).getUniqueId().toString())
                        && !commandSender.hasPermission("hypingauctions.history.others")) {
                    Messages.HISTORY_PERMISSION_DENIED.send(commandSender);
                    return true;
                }
                targetPlayer = Bukkit.getOfflinePlayer(args[0]);
                if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
                    Messages.PLAYER_NOT_EXIST.send(commandSender);
                    return true;
                }
                // Check for filter in second argument
                if (args.length > 1) {
                    filterArg = args[1].toLowerCase();
                }
            }
        } else if (commandSender instanceof Player) {
            targetPlayer = (Player) commandSender;
        } else {
            // Console must specify a player name
            Messages.HISTORY_USAGE.send(commandSender);
            return true;
        }
        List<Auction> history = HistoryManager.getPlayerHistory(targetPlayer);

        // Apply filter if specified
        if (filterArg != null) {
            history = switch (filterArg) {
                case "buy", "bought", "buying", "buys" ->
                        history.stream()
                                .filter(
                                        auction -> auction.getBuyer() != null
                                                && auction.getBuyer().getPlayer() != null
                                                && auction.getBuyer().getPlayer().getUniqueId()
                                                .equals(targetPlayer.getUniqueId()))
                                .toList();
                case "sell", "sold", "selling", "sells" ->
                        history.stream()
                                .filter(
                                        auction -> auction.getSeller() != null
                                                && auction.getSeller().getPlayer() != null
                                                && auction.getSeller().getPlayer().getUniqueId()
                                                .equals(targetPlayer.getUniqueId()))
                                .toList();
                default -> history;
            };
        }

        // If sender is a player, open the new configurable GUI instead of chat list
        if (commandSender instanceof Player player) {
            HistoryMenu.Tab tab = HistoryMenu.Tab.BUYS;
            if (filterArg != null) {
                if (filterArg.startsWith("sell"))
                    tab = HistoryMenu.Tab.SELLS;
                else if (filterArg.startsWith("buy"))
                    tab = HistoryMenu.Tab.BUYS;
            }
            new HistoryMenu(player, targetPlayer, tab).open(player);
            return true;
        }

        // Fallback for console: keep legacy chat output
        String targetName = PlayerNameCache.getInstance().getPlayerName(targetPlayer);
        if (targetName == null)
            targetName = "Unknown";

        if (history == null || history.isEmpty()) {
            Messages.NO_TRANSACTION_FOR_PLAYER.send(commandSender, targetName);
            return true;
        }

        Messages.TRANSACTION_HISTORY_TITLE_FOR_PLAYER.send(commandSender, targetName);

        int transactionsToShow = Configs.getTransactionHistorySize();
        UUID perspectiveUuid = targetPlayer.getUniqueId();

        for (int i = 0; i < Math.min(transactionsToShow, history.size()); i++) {
            Auction auction = history.get(i);
            String itemName = getItemDisplayName(auction.getItem());
            String buyerName = auction.getBuyer() != null && auction.getBuyer().getPlayer() != null
                    ? PlayerNameCache.getInstance().getPlayerName(auction.getBuyer().getPlayer())
                    : "Unknown";
            String sellerName = auction.getSeller() != null && auction.getSeller().getPlayer() != null
                    ? PlayerNameCache.getInstance().getPlayerName(auction.getSeller().getPlayer())
                    : "Unknown";

            // Indexed placeholders: {0}=item-name, {1}=amount, {2}=seller, {3}=buyer,
            // {4}=price, {5}=date
            String amount = String.valueOf(auction.getItem().getAmount());
            String price = String.valueOf(auction.getPrice());
            String date = Format.formatDateFrench(auction.getSaleDate());

            Messages messageToSend = null;
            if (auction.getBuyer() != null
                    && auction.getBuyer().getPlayer() != null
                    && perspectiveUuid.equals(auction.getBuyer().getPlayer().getUniqueId())) {
                messageToSend = Messages.TRANSACTION_BOUGHT_FORMAT;
            } else if (auction.getSeller() != null
                    && auction.getSeller().getPlayer() != null
                    && perspectiveUuid.equals(auction.getSeller().getPlayer().getUniqueId())) {
                messageToSend = Messages.TRANSACTION_SOLD_FORMAT;
            }
            if (messageToSend != null) {
                messageToSend.send(commandSender, itemName, amount, sellerName, buyerName, price, date);
            }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 1) { // Tab completion for player name or filter
            String arg = args[0].toLowerCase();
            List<String> suggestions = new java.util.ArrayList<>();

            // Add filter keywords
            if ("buys".startsWith(arg))
                suggestions.add("buys");
            if ("sells".startsWith(arg))
                suggestions.add("sells");

            // Allow console or anyone with hypingauctions.history.others to tab complete
            // player names
            if (!(sender instanceof Player) || sender.hasPermission("hypingauctions.history.others")) {
                Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(arg))
                        .forEach(suggestions::add);
            }
            return suggestions.stream().sorted().toList();
        } else if (args.length == 2) {
            // Second arg could be filter if first arg was player name
            String arg = args[1].toLowerCase();
            List<String> filters = new java.util.ArrayList<>();
            if ("buys".startsWith(arg))
                filters.add("buys");
            if ("sells".startsWith(arg))
                filters.add("sells");
            return filters;
        }
        return List.of();
    }

    /**
     * Get the display name of an item, checking Oraxen first, then ItemMeta, then
     * fallback to
     * material name
     */
    private String getItemDisplayName(ItemStack item) {
        // First try to get Oraxen display name
        String oraxenName = getOraxenDisplayName(item);
        if (oraxenName != null) {
            return oraxenName;
        }

        // Then check ItemMeta display name
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return LegacyComponentSerializer.legacyAmpersand()
                    .serialize(java.util.Objects.requireNonNull(meta.displayName()));
        }

        // Fallback to material name (not translatable since this is for chat)
        return item.getType().toString();
    }

    /** Get Oraxen display name if the item is an Oraxen item */
    private String getOraxenDisplayName(ItemStack item) {
        try {
            // Check if Oraxen is available and if this is an Oraxen item
            Class<?> oraxenItemsClass = Class.forName("io.th0rgal.oraxen.api.OraxenItems");
            java.lang.reflect.Method getIdByItemMethod = oraxenItemsClass.getMethod("getIdByItem", ItemStack.class);
            String oraxenId = (String) getIdByItemMethod.invoke(null, item);

            if (oraxenId != null) {
                // Get the ItemBuilder for this Oraxen item
                java.lang.reflect.Method getItemByIdMethod = oraxenItemsClass.getMethod("getItemById", String.class);
                Object itemBuilder = getItemByIdMethod.invoke(null, oraxenId);

                if (itemBuilder != null) {
                    // Get the display name from the ItemBuilder
                    java.lang.reflect.Method getDisplayNameMethod = itemBuilder.getClass().getMethod("getDisplayName");
                    String displayName = (String) getDisplayNameMethod.invoke(itemBuilder);

                    if (displayName != null && !displayName.isEmpty()) {
                        return displayName;
                    }
                }
            }
        } catch (Exception e) {
            // Oraxen not available or error, continue to fallback
        }

        return null;
    }
}

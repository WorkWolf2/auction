package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.manager.AuctionManager;
import fr.hyping.hypingauctions.manager.AutomaticMaterialTranslationManager;
import fr.hyping.hypingauctions.manager.BanManager;
import fr.hyping.hypingauctions.menu.sellconfirm.SellConfirmSession;
import fr.hyping.hypingauctions.util.Configs;
import fr.hyping.hypingauctions.util.Format;
import fr.hyping.hypingauctions.util.Messages;
import fr.hyping.hypingcounters.api.CountersAPI;
import fr.hyping.hypingcounters.counter.value.AbstractValue;
import fr.hyping.hypingcounters.player.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SellConfirmActionCommand implements CommandExecutor {

    private final HypingAuctions plugin;

    // Session storage - maps player UUID to their active sell session
    private static final Map<UUID, SellConfirmSession> ACTIVE_SESSIONS = new ConcurrentHashMap<>();

    public SellConfirmActionCommand(HypingAuctions plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {

        // Security: Only console can execute this command
        if (!(sender instanceof ConsoleCommandSender)) {
            return true;
        }

        // Validate arguments
        if (args.length < 1) {
            return true;
        }

        // Parse player UUID
        UUID playerUUID;
        try {
            playerUUID = UUID.fromString(args[0]);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid UUID in internal sell command: " + args[0]);
            return true;
        }

        // Get player
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null || !player.isOnline()) {
            removeSession(playerUUID);
            return true;
        }

        // Retrieve session
        SellConfirmSession session = ACTIVE_SESSIONS.get(playerUUID);
        if (session == null) {
            plugin.getLogger().warning("No active sell session for player: " + player.getName());
            Messages.SELL_INPUT_CANCELED.send(player);
            player.closeInventory();
            return true;
        }

        // Route to appropriate handler
        String action = command.getName().toLowerCase();
        if (action.equals("sellconfirm")) {
            handleConfirm(player, session);
        } else if (action.equals("sellcancel")) {
            handleCancel(player, session);
        }

        return true;
    }

    /**
     * Handle confirm action - execute the sale
     */
    private void handleConfirm(Player player, SellConfirmSession session) {
        // Check if player is banned
        if (BanManager.isBanned(player)) {
            Messages.YOUR_ARE_BANNED.send(player);
            player.closeInventory();
            removeSession(player.getUniqueId());
            return;
        }

        // Verify the item is still in the player's hand
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!isSameItem(session.getItemToSell(), hand)) {
            Messages.SELL_ITEM_CHANGED.send(player);
            player.closeInventory();
            removeSession(player.getUniqueId());
            return;
        }

        // Verify player has enough balance for tax
        PlayerData data = CountersAPI.getOrCreateIfNotExists(player.getUniqueId());
        AbstractValue<?> sellerCounter = session.getCurrency()
                .counter()
                .getDataParser()
                .getOrCreate(data, session.getCurrency().counter());

        double balance = sellerCounter.getDoubleValue();
        double taxToPay = session.getTaxToPay();

        if (balance < taxToPay) {
            Messages.NOT_ENOUGH_MONEY_SELL.send(player);
            player.closeInventory();
            removeSession(player.getUniqueId());
            return;
        }

        // Deduct tax from balance
        sellerCounter.setDoubleValue(balance - taxToPay, player.getUniqueId().toString());

        // Execute the sale
        AuctionManager.sellAuction(
                session.getAuctionPlayer(),
                session.getItemToSell(),
                session.getPrice(),
                session.getCurrency()
        );

        // Remove item from player's hand
        player.getInventory().setItemInMainHand(null);

        // Send success message
        sendSuccessSellMessage(player, session);

        // Close inventory
        player.closeInventory();

        // Clean up session
        removeSession(player.getUniqueId());
    }

    /**
     * Handle cancel action - abort the sale
     */
    private void handleCancel(Player player, SellConfirmSession session) {
        Messages.SELL_INPUT_CANCELED.send(player);
        player.closeInventory();
        removeSession(player.getUniqueId());
    }

    /**
     * Send success message to player after sale is executed
     */
    private void sendSuccessSellMessage(Player player, SellConfirmSession session) {
        Component itemNameComp = AutomaticMaterialTranslationManager.getInstance()
                .getLocalizedComponent(player, session.getItemToSell());

        Component amountComp = Component.text(session.getItemToSell().getAmount());
        Component priceComp = Component.text(Format.formatNumber(session.getPrice()));
        Component clickComp = Component.empty();

        // Optional success-cancel-hint configuration
        FileConfiguration cfg = Configs.getConfig("config");
        ConfigurationSection sec = cfg.getConfigurationSection("success-cancel-hint");

        if (sec != null && sec.getBoolean("enabled", true)) {
            String labelText = sec.getString("click-label", "&b&n[Click here]");
            String hoverText = sec.getString("click-hover", "&7Click to cancel");
            String command = sec.getString("command", "/ah cancel");

            clickComp = Configs.deserializeWithHex(labelText)
                    .hoverEvent(Configs.deserializeWithHex(hoverText))
                    .clickEvent(ClickEvent.runCommand(command));
        }

        player.sendMessage(Configs.getLangComponent(
                "success-item-sold",
                itemNameComp,
                amountComp,
                priceComp,
                clickComp
        ));
    }

    /**
     * Check if two items are identical
     */
    private boolean isSameItem(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (!a.getType().equals(b.getType())) return false;
        if (a.getAmount() != b.getAmount()) return false;

        ItemMeta ma = a.getItemMeta();
        ItemMeta mb = b.getItemMeta();

        if (ma == null && mb == null) return true;
        if (ma == null || mb == null) return false;

        return ma.equals(mb);
    }

    // ==================== SESSION MANAGEMENT ====================

    /**
     * Store a session for a player
     */
    public static void storeSession(UUID playerUUID, SellConfirmSession session) {
        ACTIVE_SESSIONS.put(playerUUID, session);
    }

    /**
     * Remove a session for a player
     */
    public static void removeSession(UUID playerUUID) {
        ACTIVE_SESSIONS.remove(playerUUID);
    }

    /**
     * Get the active session for a player
     */
    public static SellConfirmSession getSession(UUID playerUUID) {
        return ACTIVE_SESSIONS.get(playerUUID);
    }

    /**
     * Check if a player has an active session
     */
    public static boolean hasSession(UUID playerUUID) {
        return ACTIVE_SESSIONS.containsKey(playerUUID);
    }

    /**
     * Clear all sessions (called on plugin disable)
     */
    public static void clearAllSessions() {
        ACTIVE_SESSIONS.clear();
    }

    /**
     * Get the number of active sessions
     */
    public static int getActiveSessionCount() {
        return ACTIVE_SESSIONS.size();
    }
}
package fr.hyping.hypingauctions.menu.sellconfirm;

import be.darkkraft.hypingmenus.menu.holder.AbstractMenuHolder;
import be.darkkraft.hypingmenus.menu.holder.slot.ItemSlot;
import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.config.menu.TemplateItemConfigEntry;
import fr.hyping.hypingauctions.manager.AuctionManager;
import fr.hyping.hypingauctions.menu.AbstractHAuctionMenu;
import fr.hyping.hypingauctions.service.AveragePriceService;
import fr.hyping.hypingauctions.util.Messages;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public class SellConfirmMenu extends AbstractHAuctionMenu {

    private final SellConfirmSession session;

    public SellConfirmMenu(HypingAuctions plugin, SellConfirmSession session) {
        super(plugin, "sell_confirm", session);
        this.session = session;
    }

    @Override
    public SellConfirmSession getSession() {
        return session;
    }

    @Override
    public void postSlotsRead(Player viewer) {
        // Start calculating average price asynchronously
        AveragePriceService.getInstance()
                .calculateAveragePrice(session.getItemToSell())
                .thenAccept(avgPrice -> {
                    session.setAveragePrice(avgPrice != null ? avgPrice : 0);

                    // Refresh the menu to update placeholders
                    viewer.getScheduler().run(getPlugin(), task -> {
                        if (getInvHolder() != null && getInvHolder().getOwner().equals(viewer)) {
                            refresh();
                        }
                    }, null);
                })
                .exceptionally(ex -> {
                    getPlugin().getLogger().warning("Failed to calculate average price: " + ex.getMessage());
                    session.setAveragePrice(0);
                    return null;
                });
    }

    @Override
    public void postSlotsClean(Player viewer) {
        // No dynamic slots to clean in confirmation menu
    }

    @Override
    public void postSlotsApply(Player viewer) {
        // Apply the item preview
        TemplateItemConfigEntry previewConfig = getConfigEntry().getTemplateItem("preview-item");
        if (previewConfig != null) {
            int[] destSlots = getConfigEntry().destSlots().get("preview");
            if (destSlots != null && destSlots.length > 0) {
                int destSlot = destSlots[0];

                AbstractMenuHolder holder = getInvHolder();
                int srcSlot = previewConfig.srcSlot();

                ItemStack templateItem = holder.getInventory().getItem(srcSlot);
                ItemSlot templateButton = holder.getSlots()[srcSlot];

                if (templateItem != null) {
                    // Clone the actual item being sold
                    ItemStack previewItem = session.getItemToSell().clone();

                    // Preserve the item's original display name and lore
                    Map<String, String> placeholders = getPlaceholderMap();

                    applyTemplateItem(destSlot, previewItem, templateButton, placeholders);
                }
            }
        }

        // Apply placeholders to all other items
        setPlaceholders(getPlaceholderMap());
        applyRequirementReplacements(getPlaceholderMap(), viewer);
    }

    @Override
    public Map<String, String> getPlaceholderMap() {
        Map<String, String> placeholders = new HashMap<>(getCommonPlaceholders());

        placeholders.put("{PLAYER}", session.getPlaceholder("PLAYER"));
        placeholders.put("{PLAYER_UUID}", session.getPlaceholder("PLAYER_UUID"));

        placeholders.put("{PRICE}", session.getPlaceholder("PRICE"));
        placeholders.put("{PRICE_FORMATTED}", session.getPlaceholder("PRICE_FORMATTED"));

        placeholders.put("{CURRENCY}", session.getPlaceholder("CURRENCY"));
        placeholders.put("{CURRENCY_COUNTER}", session.getPlaceholder("CURRENCY_COUNTER"));

        placeholders.put("{TAX}", session.getPlaceholder("TAX"));
        placeholders.put("{TAX_FORMATTED}", session.getPlaceholder("TAX_FORMATTED"));

        placeholders.put("{NET_EARNINGS}", session.getPlaceholder("NET_EARNINGS"));
        placeholders.put("{NET_EARNINGS_FORMATTED}", session.getPlaceholder("NET_EARNINGS_FORMATTED"));

        placeholders.put("{ITEM_NAME}", session.getPlaceholder("ITEM_NAME"));
        placeholders.put("{ITEM_TYPE}", session.getPlaceholder("ITEM_TYPE"));
        placeholders.put("{ITEM_MATERIAL}", session.getPlaceholder("ITEM_MATERIAL"));

        placeholders.put("{QUANTITY}", session.getPlaceholder("QUANTITY"));
        placeholders.put("{AMOUNT}", session.getPlaceholder("AMOUNT"));

        placeholders.put("{AVERAGE_PRICE}", session.getPlaceholder("AVERAGE_PRICE"));
        placeholders.put("{AVERAGE_PRICE_FORMATTED}", session.getPlaceholder("AVERAGE_PRICE_FORMATTED"));
        placeholders.put("{AVERAGE_PRICE_TOTAL}", session.getPlaceholder("AVERAGE_PRICE_TOTAL"));
        placeholders.put("{AVERAGE_PRICE_TOTAL_FORMATTED}", session.getPlaceholder("AVERAGE_PRICE_TOTAL_FORMATTED"));

        return placeholders;
    }

    /**
     * Called when the player clicks confirm
     */
    public void handleConfirm() {
        Player player = session.getPlayer();

        // Verify the item is still in the player's hand
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!isSameItem(session.getItemToSell(), hand)) {
            Messages.SELL_ITEM_CHANGED.send(player);
            player.closeInventory();
            return;
        }

        // Execute the sale
        AuctionManager.sellAuction(
                session.getAuctionPlayer(),
                session.getItemToSell(),
                session.getPrice(),
                session.getCurrency()
        );

        // Remove item from hand
        player.getInventory().setItemInMainHand(null);

        // Send success message
        Messages.SUCCESS_ITEM_SOLD.send(player); // You'll need to implement this message

        player.closeInventory();
    }

    /**
     * Called when the player clicks cancel
     */
    public void handleCancel() {
        Player player = session.getPlayer();
        Messages.SELL_INPUT_CANCELED.send(player);
        player.closeInventory();
    }

    /**
     * Check if two items are the same
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
}

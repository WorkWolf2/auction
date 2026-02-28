package fr.hyping.hypingauctions.menu.sellconfirm;

import be.darkkraft.hypingmenus.menu.holder.AbstractMenuHolder;
import be.darkkraft.hypingmenus.menu.holder.slot.ItemSlot;
import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.config.menu.TemplateItemConfigEntry;
import fr.hyping.hypingauctions.manager.AuctionManager;
import fr.hyping.hypingauctions.menu.AbstractHAuctionMenu;
import fr.hyping.hypingauctions.service.AveragePriceService;
import fr.hyping.hypingauctions.util.Messages;
import fr.hyping.hypingcounters.api.CountersAPI;
import fr.hyping.hypingcounters.counter.value.AbstractValue;
import fr.hyping.hypingcounters.player.PlayerData;
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
        AveragePriceService.getInstance()
                .calculateAveragePrice(session.getItemToSell())
                .thenAccept(avgPrice -> {
                    session.setAveragePrice(avgPrice != null ? avgPrice : 0);
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
    }

    @Override
    public void postSlotsApply(Player viewer) {
        TemplateItemConfigEntry previewConfig = getConfigEntry().getTemplateItem("preview-item");
        if (previewConfig != null) {
            int[] destSlots = getConfigEntry().destSlots().get("preview");
            if (destSlots != null && destSlots.length > 0) {
                int destSlot = destSlots[0];

                AbstractMenuHolder holder = getInvHolder();
                ItemSlot templateButton = holder.getSlots()[previewConfig.srcSlot()];

                ItemStack previewItem = session.getItemToSell().clone();
                Map<String, String> placeholders = getPlaceholderMap();

                applyTemplateItem(destSlot, previewItem, templateButton, placeholders,
                        () -> {
                            setPlaceholders(placeholders);
                            applyRequirementReplacements(placeholders, viewer);
                        });
                return;
            }
        }

        Map<String, String> placeholders = getPlaceholderMap();
        setPlaceholders(placeholders);
        applyRequirementReplacements(placeholders, viewer);
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

    public boolean handleConfirm() {
        Player player = session.getPlayer();

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!isSameItem(session.getItemToSell(), hand)) {
            Messages.SELL_ITEM_CHANGED.send(player);
            player.closeInventory();
            return false;
        }

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
            return false;
        }

        sellerCounter.setDoubleValue(balance - taxToPay, player.getUniqueId().toString());

        AuctionManager.sellAuction(
                session.getAuctionPlayer(),
                session.getItemToSell(),
                session.getPrice(),
                session.getCurrency()
        );

        player.getInventory().setItemInMainHand(null);

        Messages.SUCCESS_ITEM_SOLD.send(player).;
        player.closeInventory();
        return true;
    }

    public void handleCancel() {
        Messages.SELL_INPUT_CANCELED.send(session.getPlayer());
        session.getPlayer().closeInventory();
    }

    private boolean isSameItem(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (a.getType() != b.getType()) return false;
        if (a.getAmount() != b.getAmount()) return false;

        ItemMeta ma = a.getItemMeta();
        ItemMeta mb = b.getItemMeta();

        if (ma == null && mb == null) return true;
        if (ma == null || mb == null) return false;
        return ma.equals(mb);
    }
}

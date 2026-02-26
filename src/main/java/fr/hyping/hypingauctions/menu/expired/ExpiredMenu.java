package fr.hyping.hypingauctions.menu.expired;

import be.darkkraft.hypingmenus.menu.holder.AbstractMenuHolder;
import be.darkkraft.hypingmenus.menu.holder.slot.ItemSlot;
import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.config.menu.TemplateItemConfigEntry;
import fr.hyping.hypingauctions.manager.AutomaticMaterialTranslationManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.menu.AbstractHAuctionMenu;
import fr.hyping.hypingauctions.util.Format;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpiredMenu extends AbstractHAuctionMenu {

    public ExpiredMenu(HypingAuctions plugin, ExpiredMenuSession session) {
        super(plugin, "expired", session);
    }

    @Override
    public ExpiredMenuSession getSession() {
        return (ExpiredMenuSession) super.getSession();
    }

    @Override
    public void postSlotsRead(Player viewer) {}

    @Override
    public void postSlotsClean(Player viewer) {
        int[] destSlots = getConfigEntry().destSlots().get("auction-items");
        if (destSlots == null) return;

        AbstractMenuHolder holder = getInvHolder();
        Inventory inventory = holder.getInventory();
        ItemSlot[] slots = holder.getSlots();

        for (int destSlot : destSlots) {
            if (destSlot >= 0 && destSlot < inventory.getSize()) {
                inventory.setItem(destSlot, null);
                slots[destSlot] = null;
            }
        }
    }

    @Override
    public void postSlotsApply(Player viewer) {
        TemplateItemConfigEntry auctionItemConfig = getConfigEntry().getTemplateItem("auction-item");
        int[] destSlots = getConfigEntry().destSlots().get("auction-items");

        if (auctionItemConfig == null || destSlots == null) {
            getPlugin().getLogger().warning("Missing auction-item template or dest-slots for menu: " + configId());
            return;
        }

        AbstractMenuHolder holder = getInvHolder();
        ItemStack templateItem = holder.getInventory().getItem(auctionItemConfig.srcSlot());
        ItemSlot templateButton = holder.getSlots()[auctionItemConfig.srcSlot()];

        if (templateItem == null) {
            setPlaceholders(getPlaceholderMap());
            return;
        }

        List<Auction> auctionsToDisplay = getSession().getAuctionsForCurrentPage();
        int maxItems = Math.min(auctionsToDisplay.size(), destSlots.length);

        List<TemplateItemEntry> entries = new ArrayList<>(maxItems);
        for (int i = 0; i < maxItems; i++) {
            entries.add(new TemplateItemEntry(destSlots[i], templateItem, templateButton,
                    createAuctionPlaceholders(auctionsToDisplay.get(i), viewer)));
        }

        Map<String, String> staticPlaceholders = getPlaceholderMap();
        applyTemplateItemsBatch(entries, staticPlaceholders, () -> setPlaceholders(staticPlaceholders));
    }

    @Override
    public Map<String, String> getPlaceholderMap() {
        Map<String, String> placeholders = new HashMap<>(getCommonPlaceholders());
        ExpiredMenuSession session = getSession();
        placeholders.put("{CURRENT_PAGE}", String.valueOf(session.getPage()));
        placeholders.put("{MAX_PAGES}", String.valueOf(session.getLastPage()));
        placeholders.put("{TOTAL_EXPIRED}", String.valueOf(session.getAuctionPlayer().getExpired().size()));
        return placeholders;
    }

    private Map<String, String> createAuctionPlaceholders(Auction auction, Player viewer) {
        Map<String, String> placeholders = new HashMap<>();

        Component itemComponent = AutomaticMaterialTranslationManager.getInstance()
                .getLocalizedComponent(viewer, auction.getItem());
        String itemName = LegacyComponentSerializer.legacySection().serialize(itemComponent);

        placeholders.put("{ITEM_NAME}", itemName);
        placeholders.put("{PRICE}", Format.formatNumber(auction.getPrice()));
        placeholders.put("{SELLER}", auction.getSeller().getPlayer().getName());
        placeholders.put("{QUANTITY}", String.valueOf(auction.getItem().getAmount()));
        placeholders.put("{ITEM_TYPE}", auction.getItem().getType().name());
        placeholders.put("{AUCTION_ID}", String.valueOf(auction.getId()));
        placeholders.put("{EXPIRED_DATE}", Format.formatTime(auction.getExpirationTime()));
        return placeholders;
    }
}

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
    
    private ItemStack cachedTemplateItem;
    private ItemSlot  cachedTemplateButton;

    public ExpiredMenu(HypingAuctions plugin, ExpiredMenuSession session) {
        super(plugin, "expired", session);
    }

    @Override
    public ExpiredMenuSession getSession() {
        return (ExpiredMenuSession) super.getSession();
    }

    @Override
    public void postSlotsRead(Player viewer) {
        TemplateItemConfigEntry cfg = getConfigEntry().getTemplateItem("auction-item");
        if (cfg == null) {
            getPlugin().getLogger().warning("[ExpiredMenu] Missing 'auction-item' template in config.");
            return;
        }

        AbstractMenuHolder holder = getInvHolder();
        int srcSlot = cfg.srcSlot();
        ItemStack raw = holder.getInventory().getItem(srcSlot);

        if (raw == null) {
            getPlugin().getLogger().warning("[ExpiredMenu] Template item is null at slot " + srcSlot + ".");
            return;
        }

        this.cachedTemplateItem   = raw.clone();
        this.cachedTemplateButton = holder.getSlots()[srcSlot];
    }

    @Override
    public void postSlotsClean(Player viewer) {
        int[] destSlots = getConfigEntry().destSlots().get("auction-items");
        if (destSlots == null) return;

        AbstractMenuHolder holder = getInvHolder();
        Inventory  inventory = holder.getInventory();
        ItemSlot[] slots     = holder.getSlots();

        for (int destSlot : destSlots) {
            if (destSlot >= 0 && destSlot < inventory.getSize()) {
                inventory.setItem(destSlot, null);
                slots[destSlot] = null;
            }
        }
    }

    @Override
    public void postSlotsApply(Player viewer) {
        int[] destSlots = getConfigEntry().destSlots().get("auction-items");

        if (destSlots == null) {
            getPlugin().getLogger().warning("[ExpiredMenu] Missing 'dest-slots.auction-items' in config.");
            setPlaceholders(getPlaceholderMap());
            return;
        }

        if (cachedTemplateItem == null) {
            getPlugin().getLogger().warning("[ExpiredMenu] cachedTemplateItem is null — check src-slot in config.");
            setPlaceholders(getPlaceholderMap());
            return;
        }

        List<Auction> page    = getSession().getAuctionsForCurrentPage();
        int           max     = Math.min(page.size(), destSlots.length);
        List<TemplateItemEntry> entries = new ArrayList<>(max);

        for (int i = 0; i < max; i++) {
            Auction auction = page.get(i);
            entries.add(new TemplateItemEntry(
                    destSlots[i],
                    cachedTemplateItem,              // template  — source of name/lore
                    cachedTemplateButton,
                    createAuctionPlaceholders(auction, viewer),
                    auction.getItem()                // real item — source of material/type
            ));
        }

        Map<String, String> staticPh = getPlaceholderMap();
        applyTemplateItemsBatch(entries, staticPh, () -> setPlaceholders(staticPh));
    }

    @Override
    public Map<String, String> getPlaceholderMap() {
        Map<String, String> ph = new HashMap<>(getCommonPlaceholders());
        ExpiredMenuSession s = getSession();
        ph.put("{CURRENT_PAGE}",  String.valueOf(s.getPage()));
        ph.put("{MAX_PAGES}",     String.valueOf(s.getLastPage()));
        ph.put("{TOTAL_EXPIRED}", String.valueOf(s.getAuctionPlayer().getExpired().size()));
        return ph;
    }

    private Map<String, String> createAuctionPlaceholders(Auction auction, Player viewer) {
        Map<String, String> ph = new HashMap<>();

        Component itemComponent = AutomaticMaterialTranslationManager.getInstance()
                .getLocalizedComponent(viewer, auction.getItem());
        String itemName = LegacyComponentSerializer.legacySection().serialize(itemComponent);

        ph.put("{ITEM_NAME}",    itemName);
        ph.put("{PRICE}",        Format.formatNumber(auction.getPrice()));
        ph.put("{SELLER}",       auction.getSeller().getPlayer().getName());
        ph.put("{QUANTITY}",     String.valueOf(auction.getItem().getAmount()));
        ph.put("{ITEM_TYPE}",    auction.getItem().getType().name());
        ph.put("{AUCTION_ID}",   String.valueOf(auction.getId()));
        ph.put("{EXPIRED_DATE}", Format.formatTime(auction.getExpirationTime()));
        return ph;
    }
}
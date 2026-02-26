package fr.hyping.hypingauctions.menu;

import be.darkkraft.hypingmenus.HypingMenus;
import be.darkkraft.hypingmenus.action.Action;
import be.darkkraft.hypingmenus.action.impl.ConsoleCommandAction;
import be.darkkraft.hypingmenus.action.impl.DelayAction;
import be.darkkraft.hypingmenus.action.impl.MultipleAction;
import be.darkkraft.hypingmenus.action.impl.PlayerCommandAction;
import be.darkkraft.hypingmenus.menu.RegisteredMenu;
import be.darkkraft.hypingmenus.menu.click.ClickAction;
import be.darkkraft.hypingmenus.menu.holder.AbstractMenuHolder;
import be.darkkraft.hypingmenus.menu.holder.slot.ItemSlot;
import be.darkkraft.hypingmenus.menu.item.BaseMenuItem;
import be.darkkraft.hypingmenus.menu.item.MenuItem;
import be.darkkraft.hypingmenus.requirement.Requirement;
import be.darkkraft.hypingmenus.requirement.Requirements;
import be.darkkraft.hypingmenus.requirement.executable.ExecutableRequirement;
import be.darkkraft.hypingmenus.requirement.executable.WrappedExecutableRequirement;
import be.darkkraft.hypingmenus.requirement.impl.PlaceholderRequirement;
import be.darkkraft.hypingmenus.util.predicate.PredicateType;
import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.config.menu.MenuConfigEntry;
import fr.hyping.hypingauctions.manager.PlayerManager;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.menu.auctionList.AuctionListSession;
import fr.hyping.hypingauctions.menu.bought.BoughtMenuSession;
import fr.hyping.hypingauctions.menu.expired.ExpiredMenuSession;
import fr.hyping.hypingauctions.menu.history.HistoryMenuSession;
import fr.hyping.hypingauctions.menu.similar.SimilarItemsMenuSession;
import fr.hyping.hypingauctions.sessions.PlaceholderableSession;
import fr.hyping.hypingauctions.util.ColorUtil;
import fr.hyping.hypingauctions.util.Format;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Consumer;

public abstract class AbstractHAuctionMenu implements HAuctionMenu {

    private final HypingAuctions plugin;
    private final String configId;

    @Getter
    private final PlaceholderableSession session;

    @Getter
    private final MenuConfigEntry configEntry;

    @Getter
    @Setter
    private AbstractMenuHolder invHolder;

    public AbstractHAuctionMenu(HypingAuctions plugin, String configId, PlaceholderableSession session) {
        this.plugin = plugin;
        this.configId = configId;
        this.session = session;
        this.configEntry = plugin.getConfiguration().menus().menuEntries().get(configId);

        if (this.configEntry == null) {
            throw new IllegalStateException("Menu config entry not found: " + configId);
        }
    }

    protected HypingAuctions getPlugin() {
        return plugin;
    }

    @Override
    public String configId() {
        return configId;
    }

    @Override
    public void open(Player viewer) {
        plugin.getSessions().set(viewer.getUniqueId(), session);

        RegisteredMenu hMenu = HypingMenus.getInstance().getMenu("hypingauctions/" + this.configEntry.menuId());
        if (hMenu == null) {
            throw new IllegalStateException("Menu not registered: " + this.configEntry.menuId());
        }

        this.invHolder = (AbstractMenuHolder) hMenu.create(viewer, false);
        this.invHolder.setMeta(this.plugin, this);

        this.applyAll(true);

        this.invHolder.open();
    }

    @Override
    public void preApply(Player viewer) {
        Component title = this.invHolder.getCurrentTitle();
        title = replaceComponentPlaceholder(getPlaceholderMap(), title);
        this.invHolder.setCurrentTitle(title);
    }

    @Override
    public void refresh() {
        this.applyAll(false);
    }

    public void applyAll(boolean firstOpen) {
        if (this.invHolder == null) {
            throw new IllegalStateException("Inventory holder is null for menu: " + this.configId());
        }

        Player viewer = this.invHolder.getOwner();

        if (firstOpen) {
            this.preApply(viewer);
            this.invHolder.buildSlots(); // not placed in inventory

            this.invHolder.buildInventory(false); // places slots in inventory
            this.postSlotsRead(viewer);
        }

        // Even if the menu is already open, clean all the slots & apply templated items
        this.postSlotsClean(viewer);
        this.postSlotsApply(viewer);
    }

    protected void setPlaceholders(Map<String, String> placeholderMap) {
        AbstractMenuHolder holder = this.getInvHolder();

        Inventory inventory = holder.getInventory();
        ItemStack[] contents = inventory.getContents();

        ItemSlot[] buttonSlots = holder.getSlots();

        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            item = applyPlaceholders(placeholderMap, item);
            inventory.setItem(slot, item);

            ItemSlot buttonSlot = buttonSlots[slot];
            if (buttonSlot == null) {
                continue;
            }

            Player viewer = holder.getOwner();
            ItemSlot newSlot = applyActionPlaceholders(buttonSlot.item(), slot, placeholderMap, viewer);
            buttonSlots[slot] = newSlot;
        }
    }

    protected ItemStack applyPlaceholders(Map<String, String> placeholderMap, ItemStack originalItem) {
        if (originalItem == null) return null;

        ItemStack item = originalItem.clone();

        Component customName = item.getItemMeta().customName();
        if (customName != null) {
            customName = customName.decoration(TextDecoration.ITALIC, false);
            customName = replaceComponentPlaceholder(placeholderMap, customName);
        }

        List<Component> origLore = item.lore();
        List<Component> lore = origLore == null
                ? new ArrayList<>()
                : new ArrayList<>(origLore);

        for (int i = 0; i < lore.size(); i++) {
            Component line = lore.get(i);
            for (Map.Entry<String, String> entry : placeholderMap.entrySet()) {
                line = line.replaceText(b -> b.matchLiteral(entry.getKey()).replacement(entry.getValue()));
            }
            lore.set(i, line);
        }

        Component finalDispName = customName;
        boolean res = item.editMeta(meta -> {
            if (finalDispName != null) meta.customName(finalDispName);
            meta.lore(lore);
        });

        if (!res) {
            System.out.println("Failed to edit item meta for item: " + item);
        }

        return item;
    }

    private static Component replaceComponentPlaceholder(Map<String, String> placeholderMap, Component customName) {
        for (Map.Entry<String, String> entry : placeholderMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            Component valueComp = ColorUtil.parseLegacy(value);

            Consumer<TextReplacementConfig.Builder> replacer = builder ->
                    builder.matchLiteral(key).replacement(valueComp);

            customName = customName.replaceText(replacer);
        }
        return customName;
    }

    protected ItemSlot applyActionPlaceholders(MenuItem menuButton, int destSlot, Map<String, String> placeholders, Player viewer) {
        if (!(menuButton instanceof BaseMenuItem baseMenuButton)) {
            return null;
        }

        Int2ObjectArrayMap<ClickAction> origClickActions = baseMenuButton.getClickActions();
        Int2ObjectArrayMap<ClickAction> newClickActions = new Int2ObjectArrayMap<>();

        for (Int2ObjectMap.Entry<ClickAction> entry : origClickActions.int2ObjectEntrySet()) {
            ClickAction origClickAction = entry.getValue();
            ClickAction newClickAction = convertClickAction(placeholders, origClickAction);
            newClickActions.put(entry.getIntKey(), newClickAction);
        }

        BaseMenuItem newButton = new BaseMenuItem(
                baseMenuButton.getId(),
                baseMenuButton.getPriority(),
                baseMenuButton.getItemBuilder(),
                baseMenuButton.getViewRequirement(),
                newClickActions,
                new int[]{destSlot},
                baseMenuButton.getSection()
        );

        return newButton.buildSlot(viewer, destSlot);
    }

    private @NonNull ClickAction convertClickAction(Map<String, String> placeholders, ClickAction origClickAction) {
        Action origAction = origClickAction.getAction();
        Action newAction = convertAction(placeholders, origClickAction, origAction);

        return ClickAction.wrap(origClickAction.getRequirement(), newAction);
    }

    private Action convertAction(Map<String, String> placeholders, ClickAction origClickAction, Action origAction) {
        Action newAction = origAction;

        if (origAction instanceof PlayerCommandAction(String command)) {
            newAction = new PlayerCommandAction(applyStringPlaceholders(placeholders, command));
        }
        else if (origAction instanceof ConsoleCommandAction(String command)) {
            newAction = new ConsoleCommandAction(applyStringPlaceholders(placeholders, command));
        }
        else if (origAction instanceof DelayAction(long delay, Action callback)) {
            Action newSubAction = convertAction(placeholders, origClickAction, callback);
            newAction = new DelayAction(delay, newSubAction);
        }
        else if (origAction instanceof MultipleAction(Action[] subActions)) {
            subActions = subActions.clone();

            for (int i = 0; i < subActions.length; i++) {
                Action subAction = subActions[i];
                Action newSubAction = convertClickAction(placeholders, ClickAction.wrap(origClickAction.getRequirement(), subAction)).getAction();
                subActions[i] = newSubAction;
            }

            newAction = new MultipleAction(subActions);
        }
        return newAction;
    }

    protected String applyStringPlaceholders(Map<String, String> placeholders, String str) {
        String result = str;

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        return result;
    }

    protected void applyRequirementReplacements(Map<String, String> placeholders, Player viewer) {
        List<MenuItem> items = this.getInvHolder().getAvailableItems();

        ListIterator<MenuItem> iter = items.listIterator();
        while (iter.hasNext()) {
            MenuItem item = iter.next();
            if (!(item instanceof BaseMenuItem baseItem)) continue;

            BaseMenuItem newBaseItem = this.applyReqReplacementsForItem(placeholders, baseItem);
            if (newBaseItem == null) continue;

            iter.set(newBaseItem);
        }
    }

    protected BaseMenuItem applyReqReplacementsForItem(Map<String, String> placeholders, BaseMenuItem baseItem) {
        Requirement viewReq = baseItem.getViewRequirement();
        Requirement newReq = replaceRequirement(placeholders, viewReq);

        if (newReq == viewReq) {
            return baseItem;
        }

        return new BaseMenuItem(
                baseItem.getId(),
                baseItem.getPriority(),
                baseItem.getItemBuilder(),
                newReq,
                baseItem.getClickActions(),
                baseItem.getSlots(),
                baseItem.getSection()
        );
    }

    private Requirement replaceRequirement(Map<String, String> placeholders, Requirement viewReq) {
        Requirement newReq = viewReq;

        if (viewReq instanceof Requirements multiReq) {
            ExecutableRequirement[] copy = multiReq.requirements().clone();
            boolean edited = false;

            for (int j = 0; j < copy.length; j++) {
                ExecutableRequirement execReq = copy[j];
                if (!(execReq instanceof WrappedExecutableRequirement wrappedExecReq)) continue;

                Requirement original = wrappedExecReq.requirement();
                Requirement replacedReq = replaceRequirement(placeholders, original);

                if (replacedReq == original) {
                    continue;
                }

                Action success = wrappedExecReq.successAction();
                Action deny = wrappedExecReq.denyAction();
                copy[j] = ExecutableRequirement.wrap(replacedReq, success, deny);

                edited = true;
            }

            if (edited) {
                newReq = new Requirements(copy);
            }
        }
        else if (viewReq instanceof PlaceholderRequirement papiReq) {
            String placeholder = papiReq.placeholder();
            PredicateType predicateType = papiReq.predicateType();
            String requiredValue = papiReq.requiredValue();

            String replaced = this.applyStringPlaceholders(placeholders, requiredValue);

            if (replaced.equals(requiredValue)) {
                return viewReq;
            }

            newReq = new PlaceholderRequirement(placeholder, predicateType, replaced);
        }
        return newReq;
    }

    protected void applyTemplateItem(int slot, ItemStack item, ItemSlot button, Map<String, String> placeholders) {
        ItemStack clone = this.applyPlaceholders(placeholders, item);
        AbstractMenuHolder hMenuHolder = this.getInvHolder();
        Player viewer = hMenuHolder.getOwner();
        Inventory inventory = hMenuHolder.getInventory();

        MenuItem menuButton = button.item();
        ItemSlot hMenuButtonSlot = applyActionPlaceholders(menuButton, slot, placeholders, viewer);

        viewer.getScheduler().run(getPlugin(), t -> {
            inventory.setItem(slot, clone);

            ItemSlot[] slots = hMenuHolder.getSlots();
            slots[slot] = hMenuButtonSlot;
        }, null);
    }

    protected Map<String, String> getCommonPlaceholders() {
        Map<String, String> placeholders = new HashMap<>();

        PlaceholderableSession session = getSession();
        if (session == null) {
            return placeholders;
        }

        // Try to get the player from session
        Player player = null;
        if (session instanceof AuctionListSession als) {
            player = als.getPlayer();
        } else if (session instanceof BoughtMenuSession bms) {
            player = bms.getPlayer();
        } else if (session instanceof ExpiredMenuSession ems) {
            player = ems.getPlayer();
        } else if (session instanceof HistoryMenuSession hms) {
            player = hms.getPlayer();
        } else if (session instanceof SimilarItemsMenuSession sms) {
            player = sms.getPlayer();
        }

        if (player == null) {
            return placeholders;
        }

        // Get AuctionPlayer
        AuctionPlayer auctionPlayer = PlayerManager.getPlayer(player);
        if (auctionPlayer == null) {
            return placeholders;
        }

        // Revenue and earnings
        placeholders.put("{TOTAL_REVENUE}", Format.formatNumber(auctionPlayer.getTotalEarned()));
        placeholders.put("{ITEMS_SOLD_COUNT}", String.valueOf(auctionPlayer.getSoldCount()));

        // Active listings
        int activeListings = auctionPlayer.getSales().size();
        placeholders.put("{ACTIVE_LISTINGS_COUNT}", String.valueOf(activeListings));

        // Total listings (active + expired)
        int totalListings = auctionPlayer.getSales().size() + auctionPlayer.getExpired().size();
        placeholders.put("{TOTAL_LISTINGS}", String.valueOf(totalListings));

        // Spending
        placeholders.put("{AH_TOTAL_SPENT}", Format.formatNumber(auctionPlayer.getTotalSpent()));
        placeholders.put("{ITEMS_PURCHASED_COUNT}", String.valueOf(auctionPlayer.getBoughtCount()));

        return placeholders;
    }
}

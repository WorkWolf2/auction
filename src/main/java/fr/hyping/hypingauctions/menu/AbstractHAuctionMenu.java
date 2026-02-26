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
import java.util.concurrent.atomic.AtomicInteger;
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
            this.invHolder.buildSlots();
            this.invHolder.buildInventory(false);
            this.postSlotsRead(viewer);
        }

        this.postSlotsClean(viewer);
        this.postSlotsApply(viewer);
    }

    /**
     * Applica i placeholder a tutti gli item statici nell'inventario (non-dinamici).
     * Deve essere chiamato DOPO che tutti gli item dinamici sono stati inseriti.
     */
    protected void setPlaceholders(Map<String, String> placeholderMap) {
        AbstractMenuHolder holder = this.getInvHolder();
        Inventory inventory = holder.getInventory();
        ItemStack[] contents = inventory.getContents();
        ItemSlot[] buttonSlots = holder.getSlots();

        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null) continue;

            ItemStack replaced = applyPlaceholders(placeholderMap, item);
            inventory.setItem(slot, replaced);

            ItemSlot buttonSlot = buttonSlots[slot];
            if (buttonSlot == null) continue;

            Player viewer = holder.getOwner();
            ItemSlot newSlot = applyActionPlaceholders(buttonSlot.item(), slot, placeholderMap, viewer);
            buttonSlots[slot] = newSlot;
        }
    }

    protected ItemStack applyPlaceholders(Map<String, String> placeholderMap, ItemStack originalItem) {
        if (originalItem == null) return null;

        ItemStack item = originalItem.clone();

        Component customName = item.getItemMeta() != null ? item.getItemMeta().customName() : null;
        if (customName != null) {
            customName = customName.decoration(TextDecoration.ITALIC, false);
            customName = replaceComponentPlaceholder(placeholderMap, customName);
        }

        List<Component> origLore = item.lore();
        List<Component> lore = origLore == null ? new ArrayList<>() : new ArrayList<>(origLore);

        for (int i = 0; i < lore.size(); i++) {
            Component line = lore.get(i);
            for (Map.Entry<String, String> entry : placeholderMap.entrySet()) {
                line = line.replaceText(b -> b.matchLiteral(entry.getKey()).replacement(entry.getValue()));
            }
            lore.set(i, line);
        }

        Component finalDispName = customName;
        item.editMeta(meta -> {
            if (finalDispName != null) meta.customName(finalDispName);
            meta.lore(lore);
        });

        return item;
    }

    private static Component replaceComponentPlaceholder(Map<String, String> placeholderMap, Component component) {
        for (Map.Entry<String, String> entry : placeholderMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            Component valueComp = ColorUtil.parseLegacy(value);
            Consumer<TextReplacementConfig.Builder> replacer = builder ->
                    builder.matchLiteral(key).replacement(valueComp);
            component = component.replaceText(replacer);
        }
        return component;
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
        if (origAction instanceof PlayerCommandAction(String command)) {
            return new PlayerCommandAction(applyStringPlaceholders(placeholders, command));
        } else if (origAction instanceof ConsoleCommandAction(String command)) {
            return new ConsoleCommandAction(applyStringPlaceholders(placeholders, command));
        } else if (origAction instanceof DelayAction(long delay, Action callback)) {
            Action newSubAction = convertAction(placeholders, origClickAction, callback);
            return new DelayAction(delay, newSubAction);
        } else if (origAction instanceof MultipleAction(Action[] subActions)) {
            subActions = subActions.clone();
            for (int i = 0; i < subActions.length; i++) {
                subActions[i] = convertClickAction(placeholders,
                        ClickAction.wrap(origClickAction.getRequirement(), subActions[i])).getAction();
            }
            return new MultipleAction(subActions);
        }
        return origAction;
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
        if (newReq == viewReq) return baseItem;

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
        if (viewReq instanceof Requirements multiReq) {
            ExecutableRequirement[] copy = multiReq.requirements().clone();
            boolean edited = false;
            for (int j = 0; j < copy.length; j++) {
                ExecutableRequirement execReq = copy[j];
                if (!(execReq instanceof WrappedExecutableRequirement wrappedExecReq)) continue;
                Requirement original = wrappedExecReq.requirement();
                Requirement replacedReq = replaceRequirement(placeholders, original);
                if (replacedReq == original) continue;
                copy[j] = ExecutableRequirement.wrap(replacedReq, wrappedExecReq.successAction(), wrappedExecReq.denyAction());
                edited = true;
            }
            return edited ? new Requirements(copy) : viewReq;
        } else if (viewReq instanceof PlaceholderRequirement papiReq) {
            String replaced = this.applyStringPlaceholders(placeholders, papiReq.requiredValue());
            if (replaced.equals(papiReq.requiredValue())) return viewReq;
            return new PlaceholderRequirement(papiReq.placeholder(), papiReq.predicateType(), replaced);
        }
        return viewReq;
    }

    protected void applyTemplateItem(int slot, ItemStack item, ItemSlot button,
                                     Map<String, String> placeholders) {
        applyTemplateItem(slot, item, button, placeholders, null);
    }

    protected void applyTemplateItem(int slot, ItemStack item, ItemSlot button,
                                     Map<String, String> placeholders, Runnable onComplete) {
        ItemStack clone = this.applyPlaceholders(placeholders, item);
        AbstractMenuHolder hMenuHolder = this.getInvHolder();
        Player viewer = hMenuHolder.getOwner();
        Inventory inventory = hMenuHolder.getInventory();

        MenuItem menuButton = button != null ? button.item() : null;
        ItemSlot hMenuButtonSlot = menuButton != null
                ? applyActionPlaceholders(menuButton, slot, placeholders, viewer)
                : null;

        viewer.getScheduler().run(getPlugin(), t -> {
            inventory.setItem(slot, clone);
            ItemSlot[] slots = hMenuHolder.getSlots();
            if (hMenuButtonSlot != null) {
                slots[slot] = hMenuButtonSlot;
            }
            if (onComplete != null) {
                onComplete.run();
            }
        }, null);
    }

    protected void applyTemplateItemsBatch(List<TemplateItemEntry> entries,
                                           Map<String, String> staticPlaceholders,
                                           Runnable onAllComplete) {
        if (entries.isEmpty()) {
            if (onAllComplete != null) onAllComplete.run();
            return;
        }

        AbstractMenuHolder hMenuHolder = this.getInvHolder();
        Player viewer = hMenuHolder.getOwner();
        Inventory inventory = hMenuHolder.getInventory();
        ItemSlot[] slots = hMenuHolder.getSlots();

        AtomicInteger remaining = new AtomicInteger(entries.size());

        for (TemplateItemEntry entry : entries) {
            ItemStack clone = this.applyPlaceholders(entry.placeholders(), entry.item());
            MenuItem menuButton = entry.button() != null ? entry.button().item() : null;
            ItemSlot hMenuButtonSlot = menuButton != null
                    ? applyActionPlaceholders(menuButton, entry.slot(), entry.placeholders(), viewer)
                    : null;

            viewer.getScheduler().run(getPlugin(), t -> {
                inventory.setItem(entry.slot(), clone);
                if (hMenuButtonSlot != null) {
                    slots[entry.slot()] = hMenuButtonSlot;
                }

                if (remaining.decrementAndGet() == 0 && onAllComplete != null) {
                    onAllComplete.run();
                }
            }, null);
        }
    }

    public record TemplateItemEntry(int slot, ItemStack item, ItemSlot button, Map<String, String> placeholders) {}

    protected Map<String, String> getCommonPlaceholders() {
        Map<String, String> placeholders = new HashMap<>();
        PlaceholderableSession session = getSession();
        if (session == null) return placeholders;

        Player player = extractPlayer(session);
        if (player == null) return placeholders;

        AuctionPlayer auctionPlayer = PlayerManager.getPlayer(player);
        if (auctionPlayer == null) return placeholders;

        placeholders.put("{TOTAL_REVENUE}", Format.formatNumber(auctionPlayer.getTotalEarned()));
        placeholders.put("{ITEMS_SOLD_COUNT}", String.valueOf(auctionPlayer.getSoldCount()));
        placeholders.put("{ACTIVE_LISTINGS_COUNT}", String.valueOf(auctionPlayer.getSales().size()));
        placeholders.put("{TOTAL_LISTINGS}", String.valueOf(auctionPlayer.getSales().size() + auctionPlayer.getExpired().size()));
        placeholders.put("{AH_TOTAL_SPENT}", Format.formatNumber(auctionPlayer.getTotalSpent()));
        placeholders.put("{ITEMS_PURCHASED_COUNT}", String.valueOf(auctionPlayer.getBoughtCount()));

        return placeholders;
    }

    private Player extractPlayer(PlaceholderableSession session) {
        return switch (session) {
            case AuctionListSession als -> als.getPlayer();
            case BoughtMenuSession bms -> bms.getPlayer();
            case ExpiredMenuSession ems -> ems.getPlayer();
            case HistoryMenuSession hms -> hms.getPlayer();
            case SimilarItemsMenuSession sms -> sms.getPlayer();
            default -> null;
        };
    }
}
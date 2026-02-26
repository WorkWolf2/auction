package fr.hyping.hypingauctions.gui;

import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.cache.PlayerNameCache;
import fr.hyping.hypingauctions.manager.AutomaticMaterialTranslationManager;
import fr.hyping.hypingauctions.manager.HistoryManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.util.Configs;
import fr.hyping.hypingauctions.util.Format;
import fr.mrmicky.fastinv.FastInv;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Independent, fully-configurable history GUI with two tabs (buys and sells)
 * and paging. Config is
 * under history.yml (see example in resources).
 */
public class HistoryMenu extends FastInv {

    public enum Tab {
        BUYS,
        SELLS
    }

    private final Player viewer;
    private final OfflinePlayer target;
    private final FileConfiguration config;
    private final ConfigurationSection menuSection;

    private final List<Integer> contentSlots;
    private final int itemsPerPage;
    private final int size;
    private final int slotPrev;
    private final int slotNext;
    private final int slotClose;
    private final int slotTabCycle;
    private final ConfigurationSection tabSection;
    private final AtomicBoolean isOpen = new AtomicBoolean(false);

    private ScheduledTask refreshTask;

    private int currentPage = 0;
    private Tab currentTab;

    public HistoryMenu(@NotNull Player viewer, @NotNull OfflinePlayer target, @NotNull Tab tab) {
        super(
                owner -> {
                    FileConfiguration cfg = Configs.getConfig("history");
                    ConfigurationSection sec = Objects.requireNonNull(cfg.getConfigurationSection("menu"));
                    int invSize = Math.max(9, Math.min(54, sec.getInt("size", 54)));
                    // Support distinct titles per tab with fallback to generic title
                    String genericTitle = sec.getString("title", "hyping.hypingauctions.gui.history.title");
                    String tabTitleKey = (tab == Tab.BUYS) ? "title-buys" : "title-sells";
                    String title = sec.getString(tabTitleKey, genericTitle);
                    Component component = Component.translatable(title).decoration(TextDecoration.ITALIC, false);
                    return Bukkit.createInventory(owner, invSize, component);
                });
        this.viewer = viewer;
        this.target = target;
        this.currentTab = tab;

        this.config = Configs.getConfig("history");
        this.menuSection = config.getConfigurationSection("menu");
        if (this.menuSection == null) {
            throw new IllegalStateException("history.yml: 'menu' section is missing");
        }

        // Read layout
        this.size = Math.max(9, Math.min(54, menuSection.getInt("size", 54)));
        this.contentSlots = new ArrayList<>();
        for (int slot : menuSection.getIntegerList("content-slots")) {
            if (slot >= 0 && slot < size)
                this.contentSlots.add(slot);
        }
        this.itemsPerPage = Math.max(1, menuSection.getInt("items-per-page", contentSlots.size()));

        // Navigation slots
        ConfigurationSection navSec = menuSection.getConfigurationSection("navigation");
        if (navSec == null)
            navSec = menuSection.createSection("navigation");
        ConfigurationSection prevSlotSec = navSec.getConfigurationSection("previous");
        this.slotPrev = Math.max(0, Math.min(size - 1, prevSlotSec != null ? prevSlotSec.getInt("slot", 45) : 45));
        ConfigurationSection nextSlotSec = navSec.getConfigurationSection("next");
        this.slotNext = Math.max(0, Math.min(size - 1, nextSlotSec != null ? nextSlotSec.getInt("slot", 53) : 53));
        ConfigurationSection closeSlotSec = navSec.getConfigurationSection("close");
        this.slotClose = Math.max(
                0, Math.min(size - 1, closeSlotSec != null ? closeSlotSec.getInt("slot", 49) : 49));

        // Single cyclic tab section
        ConfigurationSection tabSec = menuSection.getConfigurationSection("tab");
        if (tabSec == null)
            tabSec = menuSection.createSection("tab");
        this.tabSection = tabSec;
        this.slotTabCycle = Math.max(0, Math.min(size - 1, tabSec.getInt("slot", 4)));

        // Ensure content slots never overlap with navigation or tab slots to avoid
        // clearing them
        contentSlots.removeIf(
                s -> s == slotPrev || s == slotNext || s == slotClose || s == slotTabCycle);
    }

    // No-op helper removed; inventory is built directly in constructor lambda using
    // FastInv holder

    @Override
    protected void onOpen(org.bukkit.event.inventory.InventoryOpenEvent event) {
        isOpen.set(true);
        // Render immediately
        render();

        // Optional auto-refresh
        int refresh = menuSection.getInt("refresh-ticks", -1);
        if (refresh > 0) {
            this.refreshTask = Bukkit.getGlobalRegionScheduler()
                    .runAtFixedRate(
                            HypingAuctions.getInstance(),
                            task -> {
                                if (!viewer.isOnline()) {
                                    task.cancel();
                                    return;
                                }
                                // If menu is not top inventory anymore, stop
                                if (viewer.getOpenInventory() == null
                                        || viewer.getOpenInventory().getTopInventory() != getInventory()) {
                                    task.cancel();
                                    return;
                                }
                                render();
                            },
                            refresh,
                            refresh);
        }
    }

    @Override
    protected void onClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        isOpen.set(false);
        if (refreshTask != null) {
            try {
                refreshTask.cancel();
            } catch (Throwable ignored) {
            }
            refreshTask = null;
        }
    }

    @Override
    protected void onClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        // Prevent moving items
        event.setCancelled(true);
    }

    private void render() {
        // Ensure data is present; if not, load asynchronously then re-render when ready
        List<Auction> cached = HistoryManager.getPlayerHistory(target);
        if (cached == null) {
            UUID targetId = target.getUniqueId();
            HypingAuctions.getInstance()
                    .getDatabase()
                    .getPlayerHistory(targetId)
                    .thenAcceptAsync(
                            list -> {
                                Bukkit.getGlobalRegionScheduler()
                                        .execute(
                                                HypingAuctions.getInstance(),
                                                () -> {
                                                    if (!viewer.isOnline() || !isOpen.get())
                                                        return;
                                                    renderWithData(list);
                                                });
                            });
            // Render a temporary loading state as empty
        }
        renderWithData(cached);
    }

    private void renderWithData(@Nullable List<Auction> source) {
        // Clear inventory first
        for (int i = 0; i < size; i++)
            setItem(i, null);

        // Filler
        ConfigurationSection fillerSec = menuSection.getConfigurationSection("filler");
        if (fillerSec != null && fillerSec.getBoolean("enabled", false)) {
            Material fillerMat = materialOf(fillerSec.getString("material", "BLACK_STAINED_GLASS_PANE"));
            for (int s : fillerSec.getIntegerList("slots"))
                if (s >= 0 && s < size)
                    setItem(s, new ItemStack(fillerMat));
        }

        // Single cyclic tab
        placeCyclicTabItem();

        // Entries
        List<Auction> filtered = filterByTab(source);
        if (filtered == null)
            filtered = List.of();
        int startIndex = Math.max(0, currentPage) * Math.max(1, itemsPerPage);
        int endIndex = Math.min(filtered.size(), startIndex + contentSlots.size());

        ConfigurationSection entrySec = menuSection.getConfigurationSection("entry");
        if (entrySec == null)
            entrySec = menuSection.createSection("entry");
        boolean useAuctionItem = entrySec.getBoolean("use-auction-item", true);

        for (int i = 0; i < contentSlots.size(); i++) {
            int itemIndex = startIndex + i;
            int slot = contentSlots.get(i);
            if (itemIndex >= endIndex) {
                setItem(slot, null);
                continue;
            }
            Auction auction = filtered.get(itemIndex);

            // Capture original item lore from the actual auction item before overriding
            // meta
            List<Component> originalItemLore = null;
            try {
                ItemStack src = auction.getItem();
                if (src != null && src.hasItemMeta()) {
                    List<Component> tmp = src.getItemMeta().lore();
                    if (tmp != null && !tmp.isEmpty())
                        originalItemLore = new ArrayList<>(tmp);
                }
            } catch (Throwable ignored) {
            }

            ItemStack base = useAuctionItem
                    ? auction.getItem().clone()
                    : new ItemStack(materialOf(entrySec.getString("material", "PAPER")));
            ItemMeta meta = base.getItemMeta();
            if (meta == null)
                meta = base.getItemMeta();

            // Build HRT arguments
            Component itemNameComp = AutomaticMaterialTranslationManager.getInstance().getLocalizedComponent(viewer,
                    auction.getItem());
            Component[] nameArgs = buildEntryNameArgs(auction, itemNameComp);
            String itemNameStr = AutomaticMaterialTranslationManager.getInstance().getFrenchName(auction.getItem());

            String buyerName = auction.getBuyer() != null && auction.getBuyer().getPlayer() != null
                    ? nonNullOr(
                    PlayerNameCache.getInstance().getPlayerName(auction.getBuyer().getPlayer()),
                    safeName(auction.getBuyer().getPlayer()))
                    : "Unknown";
            String sellerName = auction.getSeller() != null && auction.getSeller().getPlayer() != null
                    ? nonNullOr(
                    PlayerNameCache.getInstance().getPlayerName(auction.getSeller().getPlayer()),
                    safeName(auction.getSeller().getPlayer()))
                    : "Unknown";
            long date = auction.getPurchaseDate() > 0 ? auction.getPurchaseDate() : auction.getSaleDate();
            String priceFormatted = formatNumber(auction.getPrice());
            String relevantName = currentTab == Tab.BUYS ? sellerName : buyerName;
            Component[] loreArgs = buildEntryLoreArgs(auction, relevantName, date, priceFormatted);

            meta.displayName(Configs.getLangComponent("gui.history.entry.name", nameArgs));

            // Support distinct lores per tab
            String loreKeyBase = (currentTab == Tab.BUYS) ? "lore-buys" : "lore-sells";
            List<String> loreTemplates = (currentTab == Tab.BUYS)
                    ? entrySec.getStringList("lore-buys")
                    : entrySec.getStringList("lore-sells");

            // Use size of templates to determine loop count, but use HRT keys for content
            List<Component> combinedLore = new ArrayList<>();

            boolean includeItemLore = entrySec.getBoolean("include-item-lore", true);

            if (loreTemplates != null && !loreTemplates.isEmpty()) {
                for (int j = 0; j < loreTemplates.size(); j++) {
                    combinedLore.add(Configs.getLangComponent("gui.history.entry." + loreKeyBase + "." + j, loreArgs));
                }
            }

            if (includeItemLore && originalItemLore != null && !originalItemLore.isEmpty()) {
                combinedLore.addAll(originalItemLore);
            }

            if (!combinedLore.isEmpty())
                meta.lore(combinedLore);

            int cmd = entrySec.getInt("custom-model-data", -1);
            if (cmd != -1)
                meta.setCustomModelData(cmd);
            base.setItemMeta(meta);

            final Auction auctionRef = auction;
            setItem(
                    slot,
                    base,
                    e -> {
                        e.setCancelled(true);
                        e.setCancelled(true);
                        Map<String, String> ph = new HashMap<>();
                        ph.put("%item_name%", itemNameStr); // Minimal support
                        boolean right = e.isRightClick();
                        List<String> actions = right
                                ? menuSection.getStringList("entry-actions.right-click")
                                : menuSection.getStringList("entry-actions.left-click");
                        executeActions(viewer, actions, ph);
                    });
        }

        // Navigation (render last so it always wins over fillers/entries)
        placeNavigationItems(source);
    }

    private void placeNavigationItems(@Nullable List<Auction> source) {
        ConfigurationSection navSec = menuSection.getConfigurationSection("navigation");
        if (navSec == null) {
            navSec = menuSection.createSection("navigation");
        }
        List<Auction> filtered = filterByTab(source);
        int totalItems = filtered == null ? 0 : filtered.size();
        int maxPage = Math.max(0, (int) Math.ceil(totalItems / (double) itemsPerPage) - 1);
        if (currentPage > maxPage)
            currentPage = maxPage;

        boolean isFirstPage = currentPage <= 0;

        ConfigurationSection prevSec = navSec.getConfigurationSection("previous");
        if (isFirstPage) {
            ConfigurationSection firstPageSec = prevSec != null ? prevSec.getConfigurationSection("first-page") : null;
            boolean firstEnabled = firstPageSec != null && firstPageSec.getBoolean("enabled", true);
            if (firstEnabled) {
                ItemStack firstBtn = buildSimpleItem(firstPageSec, Material.ARROW);
                int firstSlot = Math.max(0, Math.min(size - 1, firstPageSec.getInt("slot", slotPrev)));
                setItem(
                        firstSlot,
                        firstBtn,
                        e -> {
                            e.setCancelled(true);

                            // Conditional actions based on tab
                            List<String> actions;
                            if (currentTab == Tab.BUYS) {
                                actions = firstPageSec.getStringList("actions-buys");
                                if (actions.isEmpty())
                                    actions = firstPageSec.getStringList("actions");
                            } else {
                                actions = firstPageSec.getStringList("actions-sells");
                                if (actions.isEmpty())
                                    actions = firstPageSec.getStringList("actions");
                            }

                            Map<String, String> ph = new HashMap<>();
                            ph.put("%player%", viewer.getName());
                            executeActions(viewer, actions, ph);
                        });
            } else {
                ItemStack prev = (prevSec != null)
                        ? buildSimpleItem(prevSec, Material.ARROW)
                        : new ItemStack(Material.ARROW);
                setItem(
                        slotPrev,
                        prev,
                        e -> {
                            e.setCancelled(true);
                        });
            }
        } else {
            boolean prevEnabled = prevSec == null || prevSec.getBoolean("enabled", true);
            if (prevEnabled) {
                ItemStack prev = (prevSec != null)
                        ? buildSimpleItem(prevSec, Material.ARROW)
                        : new ItemStack(Material.ARROW);
                setItem(
                        slotPrev,
                        prev,
                        e -> {
                            e.setCancelled(true);
                            if (currentPage > 0) {
                                currentPage--;
                                render();
                            }
                        });
            }
        }

        // Next (only if a next page exists)
        ConfigurationSection nextSec = navSec.getConfigurationSection("next");
        if (nextSec != null && nextSec.getBoolean("enabled", true) && currentPage < maxPage) {
            ItemStack next = buildSimpleItem(nextSec, Material.ARROW);
            setItem(
                    slotNext,
                    next,
                    e -> {
                        e.setCancelled(true);
                        int total = totalItems;
                        int max = Math.max(0, (int) Math.ceil(total / (double) itemsPerPage) - 1);
                        if (currentPage < max) {
                            currentPage++;
                            render();
                        }
                    });
        }

        // Close (can be disabled via config)
        ConfigurationSection closeSec = navSec.getConfigurationSection("close");
        if (closeSec != null && closeSec.getBoolean("enabled", true)) {
            ItemStack close = buildSimpleItem(closeSec, Material.BARRIER);
            setItem(
                    slotClose,
                    close,
                    e -> {
                        e.setCancelled(true);
                        viewer.closeInventory();
                    });
        }
    }

    private void placeCyclicTabItem() {
        if (tabSection == null)
            return;
        ConfigurationSection stateSec = currentTab == Tab.BUYS
                ? tabSection.getConfigurationSection("buys")
                : tabSection.getConfigurationSection("sells");
        if (stateSec == null)
            return;

        String matKey = stateSec.getString("material", currentTab == Tab.BUYS ? "EMERALD" : "GOLD_INGOT");
        ItemStack item = new ItemStack(materialOf(matKey));
        ItemMeta meta = item.getItemMeta();

        // Prefer templated rendering if provided; fallback to simple name/lore
        ConfigurationSection templateSec = tabSection.getConfigurationSection("template");
        if (templateSec != null) {
            ConfigurationSection labels = templateSec.getConfigurationSection("labels");
            String buyLabel = labels != null ? labels.getString("buy", "Buy Menu") : "Buy Menu";
            String sellLabel = labels != null ? labels.getString("sell", "Sell Menu") : "Sell Menu";
            String currentLabel = currentTab == Tab.BUYS ? buyLabel : sellLabel;
            String otherLabel = currentTab == Tab.BUYS ? sellLabel : buyLabel;

            ConfigurationSection bullets = templateSec.getConfigurationSection("bullets");
            String bulletCurrent = bullets != null ? bullets.getString("current", "&e●") : "&e●";
            String bulletOther = bullets != null ? bullets.getString("other", "&7○") : "&7○";

            String bulletBuy = currentTab == Tab.BUYS ? bulletCurrent : bulletOther;
            String bulletSell = currentTab == Tab.SELLS ? bulletCurrent : bulletOther;

            Map<String, String> ph = new HashMap<>();
            ph.put("%current_label%", currentLabel);
            ph.put("%other_label%", otherLabel);
            ph.put("%buy_label%", buyLabel);
            ph.put("%sell_label%", sellLabel);
            ph.put("%bullet_buy%", bulletBuy);
            ph.put("%bullet_sell%", bulletSell);

            String display = templateSec.getString("display-name", "&7Currently Viewing %current_label%");
            meta.displayName(Configs.getLangComponent("gui.history.tab.template.display-name"));

            List<String> customLore = templateSec.getStringList("lore");
            if (customLore != null && !customLore.isEmpty()) {
                List<Component> lc = new ArrayList<>();
                String buyLabelColored = currentTab == Tab.BUYS ? "<white>" + buyLabel : "<gray>" + buyLabel;
                String sellLabelColored = currentTab == Tab.SELLS ? "<white>" + sellLabel : "<gray>" + sellLabel;

                Component[] tabArgs = new Component[] {
                        Configs.deserializeWithHex(bulletBuy), Configs.deserializeWithHex(buyLabelColored),
                        Configs.deserializeWithHex(bulletSell), Configs.deserializeWithHex(sellLabelColored)
                };
                for (int i = 0; i < customLore.size(); i++) {
                    lc.add(Configs.getLangComponent("gui.history.tab.template.lore." + i, tabArgs));
                }
                meta.lore(lc);
            }
        } else {
        }

        int cmd = stateSec.getInt("custom-model-data", -1);
        if (cmd != -1)
            meta.setCustomModelData(cmd);
        item.setItemMeta(meta);

        setItem(
                slotTabCycle,
                item,
                e -> {
                    e.setCancelled(true);
                    Tab nextTab = (currentTab == Tab.BUYS) ? Tab.SELLS : Tab.BUYS;
                    // Re-open with a new inventory so the per-tab title applies
                    new HistoryMenu(viewer, target, nextTab).open(viewer);
                });
    }

    private List<Auction> filterByTab(@Nullable List<Auction> source) {
        if (source == null || source.isEmpty())
            return List.of();
        UUID targetId = target.getUniqueId();
        return source.stream()
                .filter(Objects::nonNull)
                .filter(
                        auction -> currentTab == Tab.BUYS
                                ? (auction.getBuyer() != null
                                && auction.getBuyer().getPlayer() != null
                                && auction.getBuyer().getPlayer().getUniqueId().equals(targetId))
                                : (auction.getSeller() != null
                                && auction.getSeller().getPlayer() != null
                                && auction.getSeller().getPlayer().getUniqueId().equals(targetId)))
                .toList();
    }

    private ItemStack buildSimpleItem(ConfigurationSection sec, Material def) {
        Material mat = materialOf(sec.getString("material", def.name()));
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = null;
        try {
            ItemMeta existing = stack.getItemMeta();
            if (existing != null)
                meta = (ItemMeta) existing.clone();
        } catch (Throwable ignored) {
        }
        if (meta == null)
            meta = stack.getItemMeta();
        String name = sec.getString("name", null);
        if (name != null) {
            meta.displayName(
                    Configs.deserializeOrTranslate(name).decoration(TextDecoration.ITALIC, false));
        }
        List<String> lore = sec.getStringList("lore");
        if (lore != null && !lore.isEmpty()) {
            List<Component> lc = new ArrayList<>();
            for (String line : lore) {
                addSplitLoreLines(lc, line);
            }
            meta.lore(lc);
        }
        int cmd = sec.getInt("custom-model-data", -1);
        if (cmd != -1)
            meta.setCustomModelData(cmd);
        stack.setItemMeta(meta);
        return stack;
    }

    private Map<String, String> buildPlaceholders(Auction auction) {
        Map<String, String> map = new HashMap<>();
        return map;
    }

    private Component[] buildEntryNameArgs(Auction auction, Component itemName) {
        return new Component[] {
                itemName,
                Component.text(String.valueOf(auction.getItem().getAmount()))
        };
    }

    private Component[] buildEntryLoreArgs(Auction auction, String sellerOrBuyerName, long date, String price) {
        return new Component[] {
                Component.text(Format.formatDateFrench(date)),
                Component.text(price),
                Component.text(sellerOrBuyerName)
        };
    }

    private void executeActions(
            Player player, List<String> actions, Map<String, String> placeholders) {
        if (actions == null || actions.isEmpty())
            return;
        for (String raw : actions) {
            if (raw == null || raw.isBlank())
                continue;
            String line = applyPlaceholders(raw, placeholders);
            if (line.startsWith("[message]")) {
                String msg = line.substring("[message]".length()).trim();
                if (!msg.isEmpty()) {
                    player.sendMessage(Configs.deserializeOrTranslate(msg));
                }
            } else {
                // Treat as console command by default; replace %player%
                String cmd = line.replace("%player%", player.getName()).replace("&", "§");
                // Folia-safe: dispatch on global scheduler to avoid async command errors
                Bukkit.getGlobalRegionScheduler()
                        .execute(
                                HypingAuctions.getInstance(),
                                () -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd));
            }
        }
    }

    private static String applyPlaceholders(String input, Map<String, String> placeholders) {
        String out = input;
        if (placeholders != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                out = out.replace(e.getKey(), e.getValue());
            }
        }
        try {
            return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(null, out);
        } catch (Throwable ignored) {
            return out;
        }
    }

    private static void addSplitLoreLines(List<Component> target, String rawLine) {
        if (rawLine == null)
            return;
        String normalized = rawLine.replace("\\n", "\n");
        String[] parts = normalized.split("\n", -1);
        for (String part : parts) {
            Component lineComponent = (part == null || part.isBlank())
                    ? Component.text(" ")
                    : Configs.deserializeOrTranslate(part)
                    .decoration(TextDecoration.ITALIC, false);
            // Skip leading blank lore line to prevent extra gap below name
            // if (target.isEmpty()) {
            // if (isComponentBlank(lineComponent)) {
            // continue;
            // }
            // }
            target.add(lineComponent);
        }
    }

    // Helper to detect whether a component is effectively blank (whitespace)
    private static boolean isComponentBlank(Component c) {
        if (c == null)
            return true;
        try {
            String s = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().serialize(c);
            return s == null || s.trim().isEmpty();
        } catch (Throwable ignored) {
            return false;
        }
    }

    // Helper to check whether the last line in a list is blank
    private static boolean isLastLineBlank(List<Component> list) {
        if (list == null || list.isEmpty())
            return true;
        return isComponentBlank(list.get(list.size() - 1));
    }

    private static String nonNullOr(String s, String fallback) {
        return (s != null && !s.isBlank()) ? s : fallback;
    }

    private static String safeName(OfflinePlayer player) {
        String name = player != null ? player.getName() : null;
        return (name != null && !name.isBlank()) ? name : "Unknown";
    }

    private static Material materialOf(String name) {
        try {
            Material m = Material.getMaterial(name.toUpperCase());
            return m != null ? m : Material.BARRIER;
        } catch (Throwable ignored) {
            return Material.BARRIER;
        }
    }

    private static String formatNumber(long value) {
        try {
            return fr.hyping.hypingauctions.util.Format.formatNumber(value);
        } catch (Throwable ignored) {
            return String.valueOf(value);
        }
    }
}

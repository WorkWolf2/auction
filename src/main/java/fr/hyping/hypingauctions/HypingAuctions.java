package fr.hyping.hypingauctions;

import fr.hyping.hypingauctions.cache.PlayerNameCache;
import fr.hyping.hypingauctions.command.AuctionCommand;
import fr.hyping.hypingauctions.command.sub.*;
import fr.hyping.hypingauctions.config.HAuctionConfig;
import fr.hyping.hypingauctions.database.AuctionDatabase;
import fr.hyping.hypingauctions.hook.HypingAuctionsExtension;
import fr.hyping.hypingauctions.hook.placeholder.*;
import fr.hyping.hypingauctions.listener.PlayerListener;
import fr.hyping.hypingauctions.manager.*;
import fr.hyping.hypingauctions.sessions.UiSessions;
import fr.hyping.hypingauctions.util.CommandBuilder;
import fr.hyping.hypingauctions.util.Configs;
import fr.mrmicky.fastinv.FastInvManager;
import lombok.Getter;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Getter
public final class HypingAuctions extends JavaPlugin {

    @Getter
    private static HypingAuctions instance;
    private AuctionDatabase database;
    private ExecutorService asyncExecutor;

    private HAuctionConfig configuration;
    private UiSessions sessions;

    // ==================== LIFECYCLE ====================

    @Override
    public void onEnable() {
        instance = this;

        this.configuration = HAuctionConfig.read(getConfig());
        this.sessions = new UiSessions(this);

        initializeAsyncExecutor();
        initializeConfigs();
        initializeManagers();
        initializeCommands();
        initializePlaceholders();
        initializeSchedulers();
        initializeListeners();
        initializeFastInv();
    }

    @Override
    public void onDisable() {
        SellConfirmManager.clearAllSessions();
        shutdownManagers();
        shutdownCache();
        shutdownAsyncExecutor();
        shutdownDatabase();
    }

    // ==================== INITIALIZATION ====================

    private void initializeAsyncExecutor() {
        int threads = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        this.asyncExecutor = Executors.newFixedThreadPool(threads, new DaemonThreadFactory("HAH-async"));
    }

    private void initializeConfigs() {
        registerConfigs();
        reloadConfigs();
    }

    private void initializeManagers() {
        HistoryManager.start();
    }

    private void initializeCommands() {
        PluginCommand command = getCommand("hauctions");
        if (command == null) {
            getLogger().severe("Failed to register the command 'hauctions', reason: command is null");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        AuctionCommand auctionCommand = new AuctionCommand();
        command.setExecutor(auctionCommand);
        command.setTabCompleter(auctionCommand);
        registerAllCommands();
    }

    private void initializePlaceholders() {
        HypingAuctionsExtension extension = new HypingAuctionsExtension();
        registerBasicPlaceholders(extension);
        registerAdvancedPlaceholders(extension);
        registerPremiumPlaceholders(extension);
        extension.register();
    }

    private void initializeSchedulers() {
        getServer().getGlobalRegionScheduler()
                .runAtFixedRate(this, (t) -> AuctionManager.manageExpiredAuctions(), 20, 20);

        getServer().getGlobalRegionScheduler()
                .runAtFixedRate(this, (t) -> PremiumSlotManager.manageExpiredPremiumAuctions(), 20, 20);

        long refreshInterval = getConfig().getLong("refresh-interval", 20L);
        getServer().getGlobalRegionScheduler()
                .runAtFixedRate(this,
                        (t) -> new fr.hyping.hypingauctions.task.RealTimeUpdateTask().run(),
                        refreshInterval,
                        refreshInterval);
    }

    private void initializeListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
    }

    private void initializeFastInv() {
        try {
            FastInvManager.register(this);
        } catch (IllegalStateException e) {
            getLogger().info("FastInv is already registered by another plugin. Continuing...");
        }
    }

    // ==================== PLACEHOLDERS ====================

    private void registerBasicPlaceholders(HypingAuctionsExtension extension) {
        extension.registerPlaceholder("name", new NamePlaceholder());
        extension.registerPlaceholder("price", new PricePlaceholder());
        extension.registerPlaceholder("material", new MaterialPlaceholder());
        extension.registerPlaceholder("quantity", new QuantityPlaceholder());
        extension.registerPlaceholder("lore", new LorePlaceholder());
        extension.registerPlaceholder("currency", new CurrencyPlaceholder());
        extension.registerPlaceholder("expiration", new ExpirationPlaceholder());
        extension.registerPlaceholder("seller", new SellerPlaceholder());
        extension.registerPlaceholder("exists", new ExistsPlaceholder());
        extension.registerPlaceholder("category", new CategoryPlaceholder());
        extension.registerPlaceholder("enchantment", new EnchantmentPlaceholder());
    }

    private void registerAdvancedPlaceholders(HypingAuctionsExtension extension) {
        extension.registerPlaceholder("shulkerbox", new ShulkerboxPlaceholder());
        extension.registerPlaceholder("custom-model-data", new CustomModelDataPlaceholder());
        extension.registerPlaceholder("potiontype", new PotionTypePlaceholder());
        extension.registerPlaceholder("potion-displayname", new PotionTypeIndexedPlaceholder());
        extension.registerPlaceholder("potion-type", new PotionIdPlaceholder());
        extension.registerPlaceholder("playerhead", new PlayerHeadPlaceholder());
        extension.registerPlaceholder("averageprice", new AveragePricePlaceholder());
    }

    private void registerPremiumPlaceholders(HypingAuctionsExtension extension) {
        extension.registerPlaceholder("page", new PagePlaceholder());
        extension.registerPlaceholder("pages", new PagesPlaceholder());
        extension.registerPlaceholder("menu", new MenuPlaceholder());
        extension.registerPlaceholder("limit", new LimitPlaceholder());
        extension.registerPlaceholder("limit_info", new LimitInfoPlaceholder());
        extension.registerPlaceholder("sales", new SalesPlaceholder());
        extension.registerPlaceholder("sales-current", new SalesPlaceholder());
        extension.registerPlaceholder("sales-total", new SalesTotalPlaceholder());
        extension.registerPlaceholder("bought", new BoughtPlaceholder());
        extension.registerPlaceholder("banned", new BannedPlaceholder());
        extension.registerPlaceholder("sales-id", new SalesIdPlaceholder());
        extension.registerPlaceholder("target", new TargetPlaceholder());
        extension.registerPlaceholder("target-id", new TargetIdPlaceholder());
        extension.registerPlaceholder("allowed", new AllowedItemPlacehoder());
        extension.registerPlaceholder("expired-item", new ExpiredItemPlaceholder());
        extension.registerPlaceholder("premium", new PremiumAuctionPlaceholder());
        extension.registerPlaceholder("premium-slot", new PremiumSlotPlaceholder());
        extension.registerPlaceholder("premium-target", new PremiumTargetPlaceholder());
        extension.registerPlaceholder("is_owner", new IsOwnerPlaceholder());
    }

    // ==================== COMMANDS ====================

    private void registerAllCommands() {
        registerAdminCommands();
        registerPlayerCommands();
        registerPremiumCommands();
        registerSearchCommands();
        registerTestCommands();
        registerSimilarItemsCommand();
    }

    private void registerAdminCommands() {
        register("reload", new ReloadCommand(), "Reload the plugin.", "/hauctions reload", "hauctions.admin");
        register("ban", new BanCommand(), "Ban a player.", "/hauctions ban <player> <duration_in_s>", "hauctions.admin");
        register("unban", new UnbanCommand(), "Unban a player.", "/hauctions unban <player>", "hauctions.admin");
        register("expire", new ExpireCommand(), "Expire an auction.", "/hauctions expire <pseudo> <id>", "hauctions.admin");
        register("find", new FindCommand(), "Find an auction.", "/hauctions find", "hauctions.admin");
        register("player", new PlayerCommand(), "Display player's auctions.", "/hauctions player <pseudo> <target>", "hauctions.admin");
        register("sort", new SortCommand(), "Sort auctions.", "/hauctions sort <player> <NAME/PRICE/DATE/CATEGORY/RESET> [ASC/DESC/<CATEGORY>]", "hauctions.admin");

        register("shulkerinspect", new ShulkerboxCommand(), "Inspect shulker box.", "/hauctions shulkerinspect <target> <id>", "hauction.admin");
        register("shulkerinspect-premium", new PremiumShulkerboxCommand(), "Inspect premium shulker.", "/hauctions shulkerinspect-premium <player> <slot_number>", "hauction.admin");
        register("shulkerinspect-target", new ShulkerboxTargetCommand(), "Inspect targeted shulker.", "/hauctions shulkerinspect-target <player>", "hauction.admin");

        register("cache", new CacheCommand(), "Manage player name cache.", "/hauctions cache <info|clear|invalidate|reload|stats>", "hauctions.admin");

        register("sign-search-set", new SignSearchSetCommand(), "Set search parameters.", "/hauctions sign-search-set <player> <search_term>", "hauctions.admin");
        register("sign-search-clear", new SignSearchClearCommand(), "Clear search parameters.", "/hauctions sign-search-clear <player>", "hauctions.admin");
        register("searchreset", new SignSearchClearCommand(), "Reset search (alias).", "/hauctions searchreset <player>", "hauctions.admin");

        register("pricereset", new PriceResetCommand(), "Reset prices.", "/hauctions pricereset <all|material|hand|vanilla|info>", "hauctions.admin");
    }

    private void registerPlayerCommands() {
        register("help", new HelpCommand(), "Display help message.", "/hauctions help", null);
        register("sell", new SellCommand(), "Sell an item.", "/hauctions sell <price> [devise]", null);
        register("cancel", new CancelCommand(), "Cancel recent listing.", "/hauctions cancel", null);
        register("buy", new BuyCommand(), "Buy an item.", "/hauctions buy <player> <id>", null);
        register("buying", new BuyingCommand(), "Display buying item.", "/hauctions buying <player> <item-id>", null);
        register("withdraw", new WithdrawCommand(), "Withdraw money.", "/hauctions withdraw <player> <BOUGHT/EXPIRED> <id>", null);

        register("page", new PageCommand(this), "Change page.", "/hauctions page <next/previous/number> [player]", null);
        register("back", new BackCommand(), "Go back to main menu.", "/hauctions back", null);
        register("open", new OpenCommand(), "Open auction house.", "/hauctions open <menu> [player]", null);
        register("refresh", new RefreshCommand(), "Refresh auction house.", "/hauctions refresh [player]", null);

        register("history", new HistoryCommand(), "Get auction history.", "/hauctions history <player name>", "hauction.history");
        register("sellconfirm", new SellConfirmCommand(this), "Confirm sell.", "/hauctions sellconfirm <uuid>", "hauction.sellaction");
        register("sellcancel",  new SellCancelCommand(this),  "Cancel sell.",  "/hauctions sellcancel <uuid>",  "hauction.sellaction");
    }

    private void registerPremiumCommands() {
        register("premium", new PremiumCommand(), "Promote to premium slot.", "/hauctions premium <auction_id>", null);
        register("premium-target", new PremiumTargetCommand(), "Set premium target.", "/hauctions premium-target <player> <auction_index>", null);
        register("premium-promote", new PremiumPromoteCommand(), "Promote targeted auction.", "/hauctions premium-promote <player>", null);
        register("premium-buying", new PremiumBuyingCommand(), "Set premium buying target.", "/hauctions premium-buying <player> <slot_number>", null);
    }

    private void registerSearchCommands() {
        register("search", new SearchCommand(), "Search for an item.", "/hauctions search [item]", null);
        register("signsearch", new SignSearchCommand(), "Search using sign GUI.", "/hauctions signsearch [search_term]", null);
        register("similar-buy", new SimilarBuyCommand(), "Open purchase menu for similar item.", "/hauctions similar-buy <player> <similar_item_index>", null);
    }

    private void registerTestCommands() {
        register("testlimit", new TestLimitCommand(), "Test custom limit messages.", "/hauctions testlimit", "hauctions.admin");
        register("testmsg", new TestMessageCommand(), "Test message loading.", "/hauctions testmsg", "hauction.admin");
        register("cachetest", new CacheTestCommand(), "Test cache performance.", "/hauctions cachetest <performance|functionality|stress>", "hauctions.admin");

        register("avgtest", new AveragePriceTestCommand("avgtest"), "Test average price.", "/hauctions avgtest", "hauctions.admin");
        register("avgclear", new AveragePriceTestCommand("avgclear"), "Clear price cache.", "/hauctions avgclear", "hauctions.admin");
        register("avgenable", new AveragePriceTestCommand("avgenable"), "Enable avg price service.", "/hauctions avgenable", "hauctions.admin");
        register("avgdisable", new AveragePriceTestCommand("avgdisable"), "Disable avg price service.", "/hauctions avgdisable", "hauctions.admin");
        register("avgdebug", new AveragePriceTestCommand("avgdebug"), "Debug avg price entries.", "/hauctions avgdebug", "hauctions.admin");
    }

    private void registerSimilarItemsCommand() {
        try {
            Class<?> clazz = Class.forName("fr.hyping.hypingauctions.command.sub.SimilarItemsCommand");
            Object instance = clazz.getDeclaredConstructor().newInstance();

            CommandBuilder builder = new CommandBuilder("similar")
                    .setDescription("View similar items (same Material + CustomModelData)")
                    .setUsage("/hauctions similar <auction_id>");

            if (instance instanceof org.bukkit.command.CommandExecutor) {
                builder.setCommandExecutor((org.bukkit.command.CommandExecutor) instance);
            }
            if (instance instanceof org.bukkit.command.TabCompleter) {
                builder.setTabCompleter((org.bukkit.command.TabCompleter) instance);
            }

            AuctionCommand.registerCommand(builder);
            getLogger().info("Successfully registered SimilarItemsCommand");
        } catch (Exception e) {
            getLogger().warning("Failed to register SimilarItemsCommand: " + e.getMessage());
        }
    }

    private void register(String name, Object command, String description, String usage, String permission) {
        CommandBuilder builder = new CommandBuilder(name)
                .setDescription(description)
                .setUsage(usage);

        if (command instanceof org.bukkit.command.CommandExecutor) {
            builder.setCommandExecutor((org.bukkit.command.CommandExecutor) command);
        }
        if (command instanceof org.bukkit.command.TabCompleter) {
            builder.setTabCompleter((org.bukkit.command.TabCompleter) command);
        }
        if (permission != null) {
            builder.setPermission(permission);
        }

        AuctionCommand.registerCommand(builder);
    }

    // ==================== CONFIGS ====================

    private void registerConfigs() {
        Configs.register("auctions");
        Configs.register("categories");
        Configs.register("currencies");
        Configs.register("limits");
        Configs.register("storage");
        Configs.register("taxes");
        Configs.register("config");
        Configs.register("enchantments");
        Configs.register("potions");
        Configs.register("materials");
        Configs.register("history");
    }

    public void reloadConfigs() {
        Configs.reload();

        shutdownDatabase();
        initializeDatabase();

        CategoryManager.reload();
        CurrencyManager.reload();
        ExpirationManager.reload();
        LimitManager.reload();
        TaxesManager.reload();
        BanManager.reload();
        AuctionManager.reload();
        PremiumSlotManager.reload();

        PlayerNameCache.getInstance().reload();
        EnchantmentTranslationManager.getInstance().reload();
        PotionTranslationManager.getInstance().reload();
        AutomaticMaterialTranslationManager.getInstance().reload();
    }

    private void initializeDatabase() {
        try {
            database = new AuctionDatabase();
        } catch (Exception e) {
            getLogger().severe("Failed to connect to database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    // ==================== SHUTDOWN ====================

    private void shutdownManagers() {
        HistoryManager.shutdown();
    }

    private void shutdownCache() {
        try {
            if (PlayerNameCache.isInitialized()) {
                PlayerNameCache.getInstance().shutdown();
            }
        } catch (Exception e) {
            getLogger().warning("Error shutting down PlayerNameCache: " + e.getMessage());
        }
    }

    private void shutdownAsyncExecutor() {
        if (asyncExecutor != null) {
            asyncExecutor.shutdownNow();
            try {
                asyncExecutor.awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void shutdownDatabase() {
        if (database != null) {
            database.close();
            database = null;
        }
    }

    // ==================== GETTERS & UTILITIES ====================

    public net.kyori.adventure.text.Component getLangComponent(String key, net.kyori.adventure.text.Component... args) {
        return net.kyori.adventure.text.Component.translatable(normalizeTranslationKey(key), args);
    }

    private String normalizeTranslationKey(String key) {
        if (key == null || key.isBlank()) {
            return "hyping.hypingauctions.missing-translation-key";
        }
        return key.startsWith("hyping.") ? key : "hyping.hypingauctions." + key;
    }

    public void debug(Supplier<String> msg) {
        if (getConfig().getBoolean("debug", false)) {
            getLogger().info("[DEBUG] " + msg.get());
        }
    }

    // ==================== THREAD FACTORY ====================

    private static class DaemonThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(1);
        private final String prefix;

        public DaemonThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, prefix + "-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
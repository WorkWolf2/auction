package fr.hyping.hypingauctions.manager;

import be.darkkraft.hypingmenus.HypingMenus;
import be.darkkraft.hypingmenus.menu.RegisteredMenu;
import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.data.ItemData;
import fr.hyping.hypingauctions.data.ShulkerboxGuiData;
import fr.hyping.hypingauctions.database.AuctionDatabase;

import fr.hyping.hypingauctions.manager.object.*;
import fr.hyping.hypingauctions.menu.auctionList.AuctionListMenu;
import fr.hyping.hypingauctions.menu.auctionList.AuctionListSession;
import fr.hyping.hypingauctions.util.Configs;
import fr.hyping.hypingauctions.service.AveragePriceService;
import fr.natsu.items.entity.CustomItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Map;
import java.util.HashMap;

public class AuctionManager {

  private static final LegacyComponentSerializer LEGACY_COMPONENT = LegacyComponentSerializer.legacyAmpersand();
  private static final List<Auction> auctions = new ArrayList<>();
  public static List<MaterialData> blacklistedItems = new ArrayList<>();
  public static List<String> mainCommands = new ArrayList<>();
  private static List<String> findCommands = new ArrayList<>();
  private static List<String> similarItemsCommands = new ArrayList<>();
  private static List<String> createCommands = new ArrayList<>();

  public static int minimumPrice = 0;
  public static int maximumPrice = 0;
  public static int minimumDurabilityPercentage = 0;
  public static ShulkerboxGuiData shulkerboxData;
  private static boolean sellerNotificationsEnabled = true;
  private static String sellerNotificationFormat;
  private static boolean includeEnchantments = false;
  private static boolean showTaxInfo = false;

  // Reconnection sales summary configuration
  private static boolean reconnectionSalesSummaryEnabled = true;
  private static long minimumOfflineTime = 60; // seconds
  private static int maxItemsShown = 5;

  // Recent sales (join summary) rendering and sequencing
  private static int recentSalesDelayTicks = 20;
  // Multi-line message support (each may be a string or list in config)
  private static List<String> recentSalesHeader = new ArrayList<>();
  private static List<String> recentSalesItemFormat = new ArrayList<>();
  private static List<String> recentSalesFooter = new ArrayList<>();
  private static List<String> recentSalesNoSales = new ArrayList<>();

  // Listing cancel feature
  private static boolean listingCancelEnabled = true;
  private static long listingCancelWindowSeconds = 30; // seconds

  public static void reload() {
    FileConfiguration config = Configs.getConfig("auctions");
    minimumPrice = config.getInt("minimum-price", -1);
    maximumPrice = config.getInt("maximum-price", -1);
    minimumDurabilityPercentage = config.getInt("minimum-durability-percentage", 0);
    blacklistedItems = config.getStringList("blacklisted-items").stream().map(MaterialData::parse).toList();
    mainCommands = config.getStringList("main-commands");
    findCommands = config.getStringList("find-commands");
    similarItemsCommands = config.getStringList("similar-items-commands");

    // Check config.yml first for on-create-commands, then fall back to auctions.yml
    FileConfiguration mainConfig = Configs.getConfig("config");
    createCommands = mainConfig.getStringList("on-create-commands");
    if (createCommands.isEmpty()) {
      createCommands = config.getStringList("on-create-commands");
    }

    // Listing cancel configuration
    listingCancelEnabled = config.getBoolean("listing-cancel.enabled", true);
    listingCancelWindowSeconds = config.getLong("listing-cancel.window-seconds", 30L);

    // Load seller notification configuration
    sellerNotificationsEnabled = config.getBoolean("seller-notifications.enabled", true);
    sellerNotificationFormat = config.getString(
        "seller-notifications.message.format",
        "&a&lSALE! &a%buyer% has bought your %amount%x %item_name% for %price% %currency%!");
    includeEnchantments = config.getBoolean("seller-notifications.message.include-enchantments", false);
    showTaxInfo = config.getBoolean("seller-notifications.message.show-tax-info", false);

    // Load reconnection sales summary configuration
    reconnectionSalesSummaryEnabled = config.getBoolean("reconnection-sales-summary.enabled", true);
    minimumOfflineTime = config.getLong("reconnection-sales-summary.minimum-offline-time", 60);
    maxItemsShown = config.getInt("reconnection-sales-summary.max-items-shown", 5);

    // Load recent-sales delay (alias + backward compatibility)
    int defaultDelay = 20;
    if (config.isInt("recent-sales.delay-ticks")) {
      recentSalesDelayTicks = config.getInt("recent-sales.delay-ticks", defaultDelay);
    } else {
      recentSalesDelayTicks = config.getInt("reconnection-sales-summary.delay-ticks", defaultDelay);
    }

    // Load recent-sales multi-line messages with backward compatibility
    // Priority:
    // 1) auctions.yml recent-sales.<key>
    // 2) auctions.yml reconnection-sales-summary.messages.<key>
    // 3) fall back to Messages enum single string (wrapped into list)
    // Prefer the existing reconnection-sales-summary.messages.* keys (no
    // duplication),
    // still accept legacy/alias recent-sales.* if present.
    recentSalesHeader = readListOrString(config, "reconnection-sales-summary.messages.header");
    if (recentSalesHeader.isEmpty()) {
      recentSalesHeader = readListOrString(config, "recent-sales.header");
    }
    if (recentSalesHeader.isEmpty()) {
      recentSalesHeader = List.of("&6--- Sales Summary ---");
    }

    recentSalesItemFormat = readListOrString(config, "reconnection-sales-summary.messages.item-format");
    if (recentSalesItemFormat.isEmpty()) {
      recentSalesItemFormat = readListOrString(config, "recent-sales.item-format");
    }
    if (recentSalesItemFormat.isEmpty()) {
      recentSalesItemFormat = List.of("&7- &e%item_name% &7x%amount% sold for &a%price%");
    }

    recentSalesFooter = readListOrString(config, "reconnection-sales-summary.messages.footer");
    if (recentSalesFooter.isEmpty()) {
      recentSalesFooter = readListOrString(config, "recent-sales.footer");
    }
    if (recentSalesFooter.isEmpty()) {
      recentSalesFooter = List.of("&6Total: &a%total_earnings%");
    }

    recentSalesNoSales = readListOrString(config, "reconnection-sales-summary.messages.no-sales");
    if (recentSalesNoSales.isEmpty()) {
      recentSalesNoSales = readListOrString(config, "recent-sales.no-sales");
    }
    if (recentSalesNoSales.isEmpty()) {
      recentSalesNoSales = List.of("&7No sales while you were away.");
    }

    String titleKey = config.getString("title", "hyping.hypingauctions.gui.shulkerbox.title");
    Component title = Component.translatable(titleKey);

    ConfigurationSection closeButtonSection = config.getConfigurationSection("close-button");
    if (closeButtonSection == null) {
      throw new IllegalStateException(
          "La section 'close-button' est manquante dans la configuration");
    }

    Component buttonTitle = Configs.getLangComponent("gui.shulkerbox.close-button.name");

    List<Component> buttonLore = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      buttonLore.add(Configs.getLangComponent("gui.shulkerbox.close-button.lore." + i));
    }

    int customModelData = closeButtonSection.getInt("custom-model-data", -1);
    int slot = closeButtonSection.getInt("slot", 49);
    Material material = Material.getMaterial(Objects.requireNonNull(closeButtonSection.getString("material")));

    ItemData closeButtonData = new ItemData(buttonTitle, buttonLore, material, customModelData, slot);

    shulkerboxData = new ShulkerboxGuiData(title, closeButtonData);

    AuctionDatabase db = HypingAuctions.getInstance().getDatabase();
    auctions.clear();
    PlayerManager.reset();
    db.registerAllAuctions();
    db.registerAllBought();

    HypingAuctions.getInstance().getLogger()
        .info("on-create-commands loaded: " + createCommands);
  }

  public static List<Auction> getAuctions() {
    return auctions;
  }

  /**
   * Check if an item matches the search term by checking multiple sources: 1.
   * Custom display name
   * (if present) 2. Material name (English) 3. French material translation (if
   * available) 4. Oraxen
   * item name (if applicable)
   */
  public static boolean matchesSearchTerm(ItemStack item, String searchTerm) {
    if (item == null || searchTerm == null || searchTerm.trim().isEmpty()) {
      return true;
    }

    String search = searchTerm.toLowerCase().trim();

    // 1. Check custom display name (if present)
    if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
      String displayName = PlainTextComponentSerializer.plainText()
          .serialize(item.getItemMeta().displayName())
          .toLowerCase();
      if (displayName.contains(search)) {
        return true;
      }
    }

    // 2. Check material name (English)
    String materialName = item.getType().toString().toLowerCase().replace("_", " ");
    if (materialName.contains(search)) {
      return true;
    }

    // 3. Check automatic French translation (if available)
    try {
      AutomaticMaterialTranslationManager autoTranslationManager = AutomaticMaterialTranslationManager.getInstance();
      if (autoTranslationManager != null) {
        String autoFrenchName = autoTranslationManager.getFrenchName(item).toLowerCase();
        if (autoFrenchName.contains(search)) {
          return true;
        }
      }
    } catch (Exception e) {
    }

    // 4. Check enchantment names
    try {
      EnchantmentTranslationManager enchantmentManager = EnchantmentTranslationManager.getInstance();
      if (enchantmentManager != null && item.hasItemMeta()) {
        java.util.Map<org.bukkit.enchantments.Enchantment, Integer> enchantments;

        // Handle enchanted books separately
        if (item.getType() == Material.ENCHANTED_BOOK
            && item.getItemMeta() instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta bookMeta) {
          enchantments = bookMeta.getStoredEnchants();
        } else {
          enchantments = item.getEnchantments();
        }

        // Check if any enchantment's French name matches the search term
        for (java.util.Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : enchantments.entrySet()) {
          String frenchName = enchantmentManager.translateEnchantment(entry.getKey(), entry.getValue());
          Component frenchComponent = LEGACY_COMPONENT.deserialize(frenchName);
          String plainFrenchName = PlainTextComponentSerializer.plainText()
              .serialize(frenchComponent)
              .toLowerCase();
          if (plainFrenchName.contains(search)) {
            return true;
          }
        }
      }
    } catch (Exception e) {
    }

    return false;
  }

  private static List<Auction> applyFilter(List<Auction> auctions, Filter filter) {
    return filter == null
        ? auctions
        : auctions.stream()
            .filter(auction -> filter.getCategory().isItem(auction.getItem()))
            .filter(
                auction -> filter.getPlayer() == null
                    || auction.getSeller().getPlayer().equals(filter.getPlayer()))
            .filter(
                auction -> filter.getSearch() == null
                    || matchesSearchTerm(auction.getItem(), filter.getSearch()))
            .filter(
                auction -> filter.getItemFilter() == null
                    || filter.getItemFilter().isSimilar(auction.getItem()))
            .sorted(
                (a1, a2) -> {
                  int order = switch (filter.getSortType()) {
                    case NAME ->
                      PlainTextComponentSerializer.plainText()
                          .serialize(a1.getItem().displayName())
                          .compareTo(
                              PlainTextComponentSerializer.plainText()
                                  .serialize(a2.getItem().displayName()));
                    case PRICE -> Double.compare(a1.getPrice(), a2.getPrice());
                    case DATE -> Long.compare(a1.getSaleDate(), a2.getSaleDate());
                  };
                  return (filter.getSortOrder() == Filter.SortOrder.ASCENDING) ? order : -order;
                })
            .toList();
  }

  public static List<Auction> getAuctions(Filter filter) {
    return applyFilter(auctions, filter);
  }

  public static void addAuction(Auction auction) {
    auctions.add(auction);
  }

  /**
   * Called after a listing is canceled successfully in the database. Removes it
   * from in-memory
   * structures without touching the database again.
   */
  public static void onAuctionCanceled(Auction auction) {
    auctions.remove(auction);
    if (auction.getSeller() != null) {
      auction.getSeller().getSales().remove(auction);
    }
  }

  public static void buyAuction(AuctionPlayer player, Auction auction) {
    try {
      // Execute database operation first to ensure consistency
      HypingAuctions.getInstance().getDatabase().buyAuction(player, auction);

      // Only update in-memory state if database operation succeeded
      auction.setBuyer(player);
      player.getPurchases().add(auction);
      auction.getSeller().getSales().remove(auction);
      HistoryManager.addToHistory(auction);
      auctions.remove(auction);
    } catch (Exception e) {
      HypingAuctions.getInstance()
          .getLogger()
          .log(
              Level.SEVERE,
              "Failed to buy auction " + auction.getId() + " - database operation failed",
              e);
      // Don't update in-memory state if database operation failed
      throw e; // Re-throw to let caller handle the error
    }
  }

  public static void sellAuction(
      AuctionPlayer player, ItemStack is, long price, Currency currency) {
    long expirationTime = ExpirationManager.getExpirationTime(player.getPlayer().getPlayer());
    long saleDate = System.currentTimeMillis();
    Auction auction = new Auction(is, price, currency, player, saleDate, expirationTime);
    HypingAuctions.getInstance().getDatabase().addAuction(auction);
    auctions.add(auction);
    player.getSales().add(auction);

    var plugin = HypingAuctions.getInstance();
    plugin
        .getAsyncExecutor()
        .execute(
            () -> {
              List<String> preparedCommands = new ArrayList<>();
              try {
                for (String tpl : createCommands) {
                  if (tpl == null || tpl.isBlank())
                    continue;
                  String resolved = tpl;

                  Map<String, String> ph = buildCreatePlaceholders(auction);
                  for (Map.Entry<String, String> e : ph.entrySet()) {
                    resolved = resolved.replace(e.getKey(), e.getValue());
                  }
                  resolved = resolved.replace("&", "§");
                  preparedCommands.add(resolved);
                }
              } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING, "Error while preparing on-create commands", t);
              }
              if (preparedCommands.isEmpty())
                return;
              Bukkit.getGlobalRegionScheduler()
                  .execute(
                      plugin,
                      () -> {
                        for (String cmd : preparedCommands) {
                          try {
                            var off = auction.getSeller().getPlayer();
                            if (off != null && off.getPlayer() != null) {
                              try {
                                cmd = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(off.getPlayer(), cmd);
                              } catch (Throwable t) {
                                plugin.getLogger().log(Level.WARNING,
                                    "PlaceholderAPI error in on-create command: " + cmd, t);
                              }
                            }
                            cmd = cmd.trim();
                            if (cmd.startsWith("/"))
                              cmd = cmd.substring(1);
                            if (cmd.isEmpty())
                              continue;
                            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
                          } catch (Throwable t) {
                            plugin.getLogger().log(Level.WARNING, "Failed to execute on-create command: " + cmd, t);
                          }
                        }
                      });
            });

  }

  private static Map<String, String> buildCreatePlaceholders(Auction auction) {
    Map<String, String> map = new HashMap<>();
    ItemStack item = auction.getItem();

    var off = auction.getSeller().getPlayer();
    Player online = off == null ? null : off.getPlayer();
    String playerName = null;
    try {
      playerName = online != null ? online.getName() : off.getName();
    } catch (Throwable ignored) {
    }
    map.put("%player%", playerName == null ? "" : playerName);
    map.put("%player_uuid%", off == null ? "" : off.getUniqueId().toString());

    map.put("%price%", fr.hyping.hypingauctions.util.Format.formatNumber(auction.getPrice()));
    map.put("%price_raw%", String.valueOf(auction.getPrice()));
    map.put("%currency%", auction.getCurrency() == null ? "" : auction.getCurrency().name());
    map.put("%currency_counter%", auction.getCurrency() == null ? "" : auction.getCurrency().counter().getName());

    String itemDisplayName = "";
    try {
      if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
        itemDisplayName = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
      }
    } catch (Throwable ignored) {
    }
    String fallbackName = AutomaticMaterialTranslationManager.getInstance().getFrenchName(item);
    String resolvedName = !itemDisplayName.isEmpty() ? itemDisplayName : fallbackName;
    map.put("%item_name%", resolvedName == null ? "" : resolvedName);
    map.put("%item_display_name%", itemDisplayName == null ? "" : itemDisplayName);
    try {
      map.put("%item_material%", AutomaticMaterialTranslationManager.getInstance().getFrenchName(item.getType()));
    } catch (Throwable ignored) {
      map.put("%item_material%", "");
    }
    map.put("%amount%", String.valueOf(item.getAmount()));
    map.put("%quantity%", String.valueOf(item.getAmount()));
    map.put("%seller%", map.get("%player%"));

    return map;
  }

  public static void removeAuction(Auction auction) {
    HypingAuctions.getInstance().getDatabase().removeAuction(auction);
    HypingAuctions.getInstance().getDatabase().removeBought(auction);
    auction.getSeller().getSales().remove(auction);
    auction.getSeller().getExpired().remove(auction);
    if (auction.getBuyer() != null)
      auction.getBuyer().getPurchases().remove(auction);
    auctions.remove(auction);
  }

  public static void expireAuction(Auction auction) {
    HypingAuctions.getInstance().getDatabase().expireAuction(auction);
    auction.expire();
    if (auctions.remove(auction)) {
      auction.getSeller().getSales().remove(auction);
      auction.getSeller().getExpired().add(auction);
    }
  }

  public static void manageExpiredAuctions() {
    auctions.stream().filter(Auction::isExpired).toList().forEach(AuctionManager::expireAuction);
  }

  public static boolean isAllowed(ItemStack is) {
    return blacklistedItems.stream().noneMatch(item -> item.isSimilar(is));
  }

  public static boolean passesDurabilityCheck(ItemStack is) {
    if (is == null)
      return false;

    ItemMeta meta = is.getItemMeta();
    if (meta != null && meta.isUnbreakable()) {
      return true;
    }

    // check for HItems item
    CustomItem customItem = CustomItem.getCustomItem(is);
    if (customItem != null) {
      if (!customItem.hasUses(is))
        return true; // item has no uses, passes check
      int maxUses = customItem.attributes().uses();
      int usesLeft = customItem.getUses(is);

      int durabilityPercentage = (usesLeft * 100) / maxUses;
      return durabilityPercentage >= minimumDurabilityPercentage;
    }

    // is not HItems item, check vanilla durability
    if (meta == null) {
      return true;
    }

    // does item have max durability
    if (meta instanceof Damageable damageable) {
      int maxDurability = is.getType().getMaxDurability();
      if (maxDurability > 0 && damageable.hasDamageValue()) {
        int damage = damageable.getDamage();
        int durability = maxDurability - damage;
        int durabilityPercentage = (durability * 100) / maxDurability;
        return durabilityPercentage >= minimumDurabilityPercentage;
      }
      return true; // Item is not damageable, passes check
    }

    return true; // Item has no durability, passes check
  }

  public static boolean isSellerNotificationsEnabled() {
    return sellerNotificationsEnabled;
  }

  public static String getSellerNotificationFormat() {
    return sellerNotificationFormat;
  }

  public static boolean isIncludeEnchantments() {
    return includeEnchantments;
  }

  public static boolean isShowTaxInfo() {
    return showTaxInfo;
  }

  public static boolean isListingCancelEnabled() {
    return listingCancelEnabled;
  }

  public static long getListingCancelWindowSeconds() {
    return listingCancelWindowSeconds;
  }

  public static boolean isReconnectionSalesSummaryEnabled() {
    return reconnectionSalesSummaryEnabled;
  }

  public static int getRecentSalesDelayTicks() {
    return recentSalesDelayTicks;
  }

  public static List<String> getRecentSalesHeaderLines() {
    return recentSalesHeader;
  }

  public static List<String> getRecentSalesItemFormatLines() {
    return recentSalesItemFormat;
  }

  public static List<String> getRecentSalesFooterLines() {
    return recentSalesFooter;
  }

  public static List<String> getRecentSalesNoSalesLines() {
    return recentSalesNoSales;
  }

  public static long getMinimumOfflineTime() {
    return minimumOfflineTime;
  }

  public static int getMaxItemsShown() {
    return maxItemsShown;
  }

  public static void executeMainCommands(Player player) {
    executeMainCommands(player, true);
  }

  public static void executeMainCommands(Player player, boolean resetContext) {
      boolean useTemplates = HypingAuctions.getInstance()
              .getConfig()
              .getBoolean("use-template-system", false);

      if (useTemplates) {
          AuctionPlayer ap = PlayerManager.getPlayer(player);
          List<Auction> auctions = ap.getContext() != null
                  ? ap.getContext().getFilteredAuctions()
                  : AuctionManager.getAuctions();

          AuctionListSession session = new AuctionListSession(player, auctions, HypingAuctions.getInstance().getConfig().getInt("auction-list-menu.auction-per-page", 27));
          AuctionListMenu menu = new AuctionListMenu(HypingAuctions.getInstance(), session);

          System.out.println("eseguito quello bello");

          menu.open(player);
      } else {
          RegisteredMenu menu = HypingMenus.getInstance().getMenu("hypingauctions/auction_list");
          System.out.println("eseguito quello brutto");
          if (menu != null) {
              menu.open(player, false);
          }
      }
  }

  public static void executeFindCommands(Player player) {
    var plugin = HypingAuctions.getInstance();
    plugin
        .getAsyncExecutor()
        .execute(
            () -> {
              // Build items list in async thread
              List<org.bukkit.inventory.ItemStack> items = new ArrayList<>();
              for (Auction a : auctions) {
                if (a != null && a.getItem() != null) {
                  org.bukkit.inventory.ItemStack single = a.getItem().clone();
                  single.setAmount(1);
                  items.add(single);
                }
              }

              try {
                CompletableFuture<Void> future = plugin.getDatabase().warmCacheForMultipleItems(items);
                future.get(2000, TimeUnit.MILLISECONDS);
                plugin.debug(() -> "Prewarmed average-price cache for find menu: " + items.size() + " items");
              } catch (TimeoutException te) {
                plugin
                    .getLogger()
                    .log(Level.WARNING, "Average-price cache prewarming (find) timed out after 2000ms");
              } catch (Exception e) {
                plugin
                    .getLogger()
                    .log(Level.FINE, "Average-price cache prewarming (find) encountered an error", e);
              }

              Bukkit.getGlobalRegionScheduler()
                  .execute(
                      plugin,
                      () -> findCommands.forEach(
                          command -> plugin
                              .getServer()
                              .dispatchCommand(
                                  plugin.getServer().getConsoleSender(),
                                  command.replace("%player%", player.getName()))));
            });
  }

  public static void executeSimilarItemsCommands(Player player) {
    var plugin = HypingAuctions.getInstance();
    plugin
        .getAsyncExecutor()
        .execute(
            () -> {
              // Build items list in async thread
              List<org.bukkit.inventory.ItemStack> items = new ArrayList<>();
              for (Auction a : auctions) {
                if (a != null && a.getItem() != null) {
                  org.bukkit.inventory.ItemStack single = a.getItem().clone();
                  single.setAmount(1);
                  items.add(single);
                }
              }

              try {
                CompletableFuture<Void> future = plugin.getDatabase().warmCacheForMultipleItems(items);
                future.get(2000, TimeUnit.MILLISECONDS);
                plugin.debug(
                    () -> "Prewarmed average-price cache for similar-items menu: " + items.size() + " items");
              } catch (TimeoutException te) {
                plugin
                    .getLogger()
                    .log(
                        Level.WARNING,
                        "Average-price cache prewarming (similar-items) timed out after 2000ms");
              } catch (Exception e) {
                plugin
                    .getLogger()
                    .log(
                        Level.FINE,
                        "Average-price cache prewarming (similar-items) encountered an error",
                        e);
              }

              Bukkit.getGlobalRegionScheduler()
                  .execute(
                      plugin,
                      () -> similarItemsCommands.forEach(
                          command -> plugin
                              .getServer()
                              .dispatchCommand(
                                  plugin.getServer().getConsoleSender(),
                                  command.replace("%player%", player.getName()))));
            });
  }

  // Utility: normalize config entries that accept string or list into a list
  private static List<String> readListOrString(FileConfiguration cfg, String path) {
    try {
      if (cfg.isList(path)) {
        return cfg.getStringList(path);
      }
      String s = cfg.getString(path);
      if (s != null) {
        return List.of(s);
      }
    } catch (Exception ignored) {
    }
    return new ArrayList<>();
  }

}

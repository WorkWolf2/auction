package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.cache.PlayerNameCache;
import fr.hyping.hypingauctions.manager.AuctionManager;
import fr.hyping.hypingauctions.manager.AutomaticMaterialTranslationManager;
import fr.hyping.hypingauctions.manager.BanManager;
import fr.hyping.hypingauctions.manager.CategoryManager;
import fr.hyping.hypingauctions.manager.HistoryManager;
import fr.hyping.hypingauctions.manager.LimitManager;
import fr.hyping.hypingauctions.manager.PlayerManager;

import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.PlayerContext;
import fr.hyping.hypingauctions.util.Configs;
import fr.hyping.hypingauctions.util.Messages;
import net.kyori.adventure.text.Component;

import fr.hyping.hypingcounters.api.CountersAPI;
import fr.hyping.hypingcounters.counter.AbstractCounter;
import fr.hyping.hypingcounters.counter.data.DataParser;
import fr.hyping.hypingcounters.counter.value.AbstractValue;
import fr.hyping.hypingcounters.player.PlayerData;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bson.types.ObjectId;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BuyCommand implements CommandExecutor, TabCompleter {
  /**
   * Set of auction IDs currently being processed for purchase.
   * This prevents double-charging in case of server lag or rapid clicks
   * by ensuring only one purchase attempt can be in progress for a given auction.
   */
  private static final Set<ObjectId> purchasesInProgress = ConcurrentHashMap.newKeySet();

  @Override
  public boolean onCommand(@NotNull CommandSender buyerSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (args.length < 1 || args.length > 2)
          return false;

      OfflinePlayer offlineBuyer = Bukkit.getOfflinePlayer(args[0]);
      if (!offlineBuyer.hasPlayedBefore()) {
          Messages.PLAYER_NOT_EXIST.send(buyerSender);
          return true;
      }

      if (offlineBuyer.isConnected())
          buyerSender = offlineBuyer.getPlayer();

      int id;

      if (args.length == 1) {
          id = 0;
      } else {
          try {
              id = Integer.parseInt(args[1]) - 1;
          } catch (NumberFormatException e) {
              Messages.INVALID_ARGUMENTS.send(buyerSender);
              return true;
          }
      }

      if (BanManager.isBanned(offlineBuyer)) {
          Messages.YOUR_ARE_BANNED.send(buyerSender);
          return true;
      }

      AuctionPlayer auctionBuyer = PlayerManager.getPlayer(offlineBuyer);

      // Check global slot cap before buying (only if item won't be delivered
      // directly)
      // Note: We check this early to prevent purchases when slots are full
      // If inventory is full, item goes to purchases storage, which needs slot space
      if (LimitManager.isGlobalSlotCapReached(auctionBuyer)) {
          net.kyori.adventure.text.Component globalCapMessage = LimitManager.getGlobalSlotCapBuyMessage(auctionBuyer);

          buyerSender.sendMessage(globalCapMessage);
          return true;
      }

      if (auctionBuyer.getContext() == null) {
          Messages.NO_CONTEXT.send(buyerSender);
          return true;
      }

      PlayerContext buyerCtx = auctionBuyer.getContext();

      Auction auction = buyerCtx.getTargetAuction();

      if (args.length == 2) {
          int page = buyerCtx.getPage();
          int itemPerPage = CategoryManager.getItemsPerPage();
          id += page * itemPerPage;

          if (id < 0 || id >= buyerCtx.getFilteredAuctions().size()) {
              Messages.SALE_NOT_EXIST.send(buyerSender);
              return true;
          }

          auction = buyerCtx.getFilteredAuctions().get(id);
      }

      if (auction == null) {
          Messages.SALE_NOT_EXIST.send(buyerSender);
          return true;
      }

      UUID sellerId = auction.getSeller().getPlayer().getUniqueId();
      UUID buyerId = offlineBuyer.getUniqueId();

      if (buyerId.equals(sellerId)) {
          Messages.CANNOT_BUY_OWN.send(buyerSender);
          return true;
      }

      // Buy the item
      if (auction.isExpired()) {
          Messages.SALE_EXPIRED.send(buyerSender);
          return true;
      }

      PlayerData buyerCounterData = CountersAPI.getOrCreateIfNotExists(buyerId);
      AbstractCounter currencyCounter = auction.getCurrency().counter();
      DataParser<? extends AbstractCounter> dataParser = currencyCounter.getDataParser();
      AbstractValue<?> buyerCounter = dataParser.getOrCreate(buyerCounterData, currencyCounter);

      // Fix for precision issues with large amounts (e.g., $5M+)
      // Use double precision for both price and balance to avoid rounding errors
      double auctionPrice = (double) auction.getPrice();
      double buyerBalance = buyerCounter.getDoubleValue();

      if (auctionPrice > buyerBalance) {
          Messages.NOT_ENOUGH_MONEY.send(buyerSender);
          return true;
      }

      if (auction.getBuyer() != null) {
          Messages.ALREADY_BOUGHT.send(buyerSender);
          return true;
      }

      // Safeguard: Prevent double purchase attempts due to server lag or rapid
      // clicks.
      // If another thread is already processing this auction, reject the duplicate
      // attempt.
      ObjectId auctionId = auction.getId();
      if (auctionId == null || !purchasesInProgress.add(auctionId)) {
          Messages.ALREADY_BOUGHT.send(buyerSender);
          return true;
      }

      PlayerData sellerData = CountersAPI.getOrCreateIfNotExists(sellerId);

      ItemStack itemStack = auction.getItem();

      // Wrap purchase logic in try-finally to ensure the lock is always released
      try {
          // Check if buyer is online and has inventory space
          boolean deliveredToInventory = false;
          if (offlineBuyer.isOnline()) {

              Player onlineBuyer = (Player) offlineBuyer;

              if (onlineBuyer.getInventory().firstEmpty() != -1) {

                  try {
                      HypingAuctions.getInstance().getDatabase().buyAuction(auctionBuyer, auction);

                      CountersAPI.applyDifference(buyerCounterData, currencyCounter, v -> v - auctionPrice);
                      CountersAPI.applyDifference(sellerData, currencyCounter, v -> v + auctionPrice);
                      HypingAuctions.getInstance().getDatabase().markSellerCredited(auction);

                      // Update in-memory state and deliver item
                      onlineBuyer.getInventory().addItem(itemStack.clone());
                      deliveredToInventory = true;

                      auction.setBuyer(auctionBuyer);
                      auction.getSeller().getSales().remove(auction);
                      AuctionManager.getAuctions().remove(auction);

                      HypingAuctions.getInstance().getDatabase().removeBought(auction);

                      // Args: 1=ItemName, 2=Amount, 3=Price
                      Component[] purchaseArgs = new Component[] {
                              AutomaticMaterialTranslationManager.getInstance().getLocalizedComponent(onlineBuyer, itemStack),
                              Component.text(itemStack.getAmount()),
                              Component.text(fr.hyping.hypingauctions.util.Format.formatNumber(auctionPrice))
                      };
                      onlineBuyer.sendMessage(Configs.getLangComponent("purchase-success-delivered-to-inventory", purchaseArgs));

                      // Send notification to seller if they are online and notifications are enabled
                      String buyerResolvedName = PlayerNameCache.getInstance().getPlayerName(onlineBuyer);
                      if (buyerResolvedName == null || buyerResolvedName.trim().isEmpty()) {
                          buyerResolvedName = onlineBuyer.getName();
                      }
                      if (buyerResolvedName == null || buyerResolvedName.trim().isEmpty()) {
                          buyerResolvedName = "Unknown";
                      }
                      sendSellerNotification(auction, buyerResolvedName, "Item", itemStack.getAmount());

                      // Record transaction in history (only once)
                      HistoryManager.addToHistory(auction);
                  } catch (IllegalStateException e) {
                      if (auction.getBuyer() == null) {
                          Messages.ALREADY_BOUGHT.send(onlineBuyer);
                      }
                      return true;
                  } catch (Exception e) {
                      if (auction.getBuyer() == null) {
                          Messages.INVALID_ARGUMENTS.send(onlineBuyer);
                      }
                      HypingAuctions.getInstance().getLogger().severe("Error during purchase: " + e.getMessage());
                      return true;
                  }
              }
          }

          if (!deliveredToInventory) {
              // Player is offline or inventory is full, use current behavior
              try {
                  // Execute database operation first
                  AuctionManager.buyAuction(auctionBuyer, auction);

                  CountersAPI.applyDifference(buyerCounterData, currencyCounter, v -> v - auctionPrice);
                  CountersAPI.applyDifference(sellerData, currencyCounter, v -> v + auctionPrice);
                  HypingAuctions.getInstance().getDatabase().markSellerCredited(auction);

                  Messages.PURCHASE_SUCCESS_ADDED_TO_BOUGHT_ITEMS.send(
                          offlineBuyer.isOnline() ? (Player) offlineBuyer : buyerSender);

                  Component[] purchaseArgs = new Component[] {
                          AutomaticMaterialTranslationManager.getInstance().getLocalizedComponent(
                                  offlineBuyer.getClass().isInstance(Player.class) ? (Player) offlineBuyer : null, itemStack), // Best
                          // effort
                          // if
                          // offline
                          Component.text(itemStack.getAmount()),
                          Component.text(fr.hyping.hypingauctions.util.Format.formatNumber(auctionPrice))
                  };
                  Player target = offlineBuyer.isOnline() ? (Player) offlineBuyer
                          : (buyerSender instanceof Player ? (Player) buyerSender : null);
                  if (target != null) {
                      target.sendMessage(Configs.getLangComponent("purchase-success-added-to-bought-items", purchaseArgs));
                  } else {
                      buyerSender.sendMessage(Configs.getLangComponent("purchase-success-added-to-bought-items", purchaseArgs));
                  }

                  // Send notification to seller if they are online and notifications are enabled
                  String buyerResolvedName = PlayerNameCache.getInstance().getPlayerName(offlineBuyer);
                  if (buyerResolvedName == null || buyerResolvedName.trim().isEmpty()) {
                      buyerResolvedName = offlineBuyer.getName();
                  }
                  if (buyerResolvedName == null || buyerResolvedName.trim().isEmpty()) {
                      buyerResolvedName = "Unknown";
                  }
                  sendSellerNotification(auction, buyerResolvedName, "Item", itemStack.getAmount());

                  // Note: History is already recorded in AuctionManager.buyAuction()
              } catch (IllegalStateException e) {
                  if (auction.getBuyer() == null) {
                      Messages.ALREADY_BOUGHT.send(buyerSender);
                  }
                  return true;
              } catch (Exception e) {
                  if (auction.getBuyer() == null) {
                      Messages.INVALID_ARGUMENTS.send(buyerSender);
                  }
                  return true;
              }
          }

          if (auctionBuyer.getContext() != null)
              auctionBuyer.getContext().reloadFilteredAuctions();
          return true;
      } finally {
          // Always release the purchase lock when done (success or failure)
          purchasesInProgress.remove(auctionId);
      }
  }

  /**
   * Sends a notification to the seller when their item is bought, if they are
   * online and
   * notifications are enabled.
   *
   * @param auction    The auction that was purchased
   * @param buyerName  The name of the buyer
   * @param itemName   The name of the item that was sold
   * @param itemAmount The amount of items sold
   */
  private void sendSellerNotification(Auction auction, String buyerName, String itemName, int itemAmount) {
      // Check if seller notifications are enabled
      if (!AuctionManager.isSellerNotificationsEnabled()) {
          return;
      }

      // Check if seller is online
      OfflinePlayer sellerOfflinePlayer = auction.getSeller().getPlayer();
      if (!sellerOfflinePlayer.isOnline()) {
          return;
      }

      Player sellerPlayer = sellerOfflinePlayer.getPlayer();
      ItemStack item = auction.getItem();

      // Get item display name
      String itemDisplayName = "";
      if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
          itemDisplayName = item.getItemMeta().getDisplayName();
      }

      // Build args for HRT notification: {0}=buyer, {1}=itemName,
      // {2}=itemDisplayName, {3}=amount, {4}=price
      Component[] notifArgs = new Component[] {
              Component.text(buyerName),
              AutomaticMaterialTranslationManager.getInstance().getLocalizedComponent(sellerPlayer, item),
              Configs.deserializeWithHex(itemDisplayName),
              Component.text(itemAmount),
              Component.text(fr.hyping.hypingauctions.util.Format.formatNumber(auction.getPrice()))
      };

      sellerPlayer.sendMessage(Configs.getLangComponent("seller-notification-format", notifArgs));
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (args.length == 2) {
          OfflinePlayer player = sender.getServer().getOfflinePlayer(args[0]);

          if (!player.hasPlayedBefore()) return null;

          AuctionPlayer ap = PlayerManager.getPlayer(player);

          List<String> completions = new ArrayList<>();

          for (int i = 0; i < ap.getSales().size(); i++)
              completions.add(String.valueOf(i + 1));

          return completions;
      }
      return null;
  }
}

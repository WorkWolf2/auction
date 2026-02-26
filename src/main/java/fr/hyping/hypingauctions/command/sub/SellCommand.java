package fr.hyping.hypingauctions.command.sub;

import com.tcoded.bedrockutil.api.BedrockAPI;
import de.rapha149.signgui.SignGUI;
import de.rapha149.signgui.SignGUIAction;
import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.manager.*;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.Currency;
import fr.hyping.hypingauctions.menu.sellconfirm.SellConfirmMenu;
import fr.hyping.hypingauctions.menu.sellconfirm.SellConfirmSession;
import fr.hyping.hypingauctions.util.Configs;
import fr.hyping.hypingauctions.util.Messages;
import fr.hyping.hypingcounters.api.CountersAPI;
import fr.hyping.hypingcounters.counter.value.AbstractValue;
import fr.hyping.hypingcounters.player.PlayerData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SellCommand implements CommandExecutor, TabCompleter {

    private static final Pattern PRICE_CAPTURE = Pattern
            .compile("^\\s*([0-9 .,'`_]+(?:[.,][0-9]{1,4})?(?:[KMBkmb]+)?)\\s*$");

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            Messages.PLAYER_ONLY_COMMAND.send(sender);
            return true;
        }

        FileConfiguration config = Configs.getConfig("config");
        AuctionPlayer auctionPlayer = PlayerManager.getPlayer(player);

        // Check per-player listing limit (active listings only)
        if (LimitManager.isLimitReached(player, auctionPlayer)) {
            Component limitMessage = LimitManager.getLimitMessage(player, auctionPlayer);
            if (limitMessage != null) {
                player.sendMessage(limitMessage);
            } else {
                Messages.SALES_LIMIT_REACHED.send(sender);
            }
            return true;
        }

        // Check global slot cap (all items: active + purchases + expired)
        if (LimitManager.isGlobalSlotCapReached(auctionPlayer)) {
            Component globalCapMessage = LimitManager.getGlobalSlotCapSellMessage(auctionPlayer);
            player.sendMessage(globalCapMessage);
            return true;
        }

        ItemStack current = player.getInventory().getItemInMainHand();
        if (current == null || current.getType().isAir()) {
            Messages.NO_ITEM_IN_HAND.send(sender);
            return true;
        }

        // Check category limits
        Component categoryLimitMsg = LimitManager.getCategoryLimitMessage(player, current, auctionPlayer);
        if (categoryLimitMsg != null) {
            player.sendMessage(categoryLimitMsg);
            return true;
        } else if (LimitManager.isCategoryLimitReached(player, current, auctionPlayer)) {
            Messages.SALES_LIMIT_REACHED.send(sender);
            return true;
        }

        if (!AuctionManager.isAllowed(current)) {
            Messages.ITEM_NOT_ALLOWED.send(sender);
            return true;
        }

        if (!AuctionManager.passesDurabilityCheck(current)) {
            Messages.ITEM_DURABILITY_TOO_LOW.send(sender);
            return true;
        }

        if (BanManager.isBanned(player)) {
            Messages.YOUR_ARE_BANNED.send(sender);
            return true;
        }

        if (args.length < 1) {
            // Open price input sign
            openPriceSign(player, current.clone());
            return true;
        }

        long price;
        try {
            price = normalizePrice(args[0], config);
        } catch (NumberFormatException ex) {
            Messages.SELL_INVALID_NUMBER.send(player);
            return true;
        }

        Currency currency;
        if (args.length > 1) {
            currency = CurrencyManager.getCurrency(args[1]);
            if (currency == null) {
                Messages.INVALID_CURRENCY.send(sender);
                return true;
            }
        } else {
            currency = CurrencyManager.getDefaultCurrency();
        }

        // Optional currency permission
        if (!player.hasPermission("hauctions.sell.currency." + currency.counter().getName())) {
            Messages.SELL_CURRENCY_NOT_ALLOWED.send(player);
            return true;
        }

        if (AuctionManager.minimumPrice != -1
                && price < AuctionManager.minimumPrice
                && !player.hasPermission("hauctions.sell.bypassmin")) {
            Messages.MINIMUM_PRICE.send(
                    sender,
                    fr.hyping.hypingauctions.util.Format.formatNumber(AuctionManager.minimumPrice));
            return true;
        }

        if (AuctionManager.maximumPrice != -1
                && price > AuctionManager.maximumPrice
                && !player.hasPermission("hauctions.sell.bypassmax")) {
            Messages.MAXIMUM_PRICE.send(
                    sender,
                    fr.hyping.hypingauctions.util.Format.formatNumber(AuctionManager.maximumPrice));
            return true;
        }

        double tax = TaxesManager.getTax(player, price);
        double taxToPay = price * tax / 100.0;

        PlayerData data = CountersAPI.getOrCreateIfNotExists(player.getUniqueId());
        AbstractValue<?> sellerCounter = currency.counter().getDataParser().getOrCreate(data, currency.counter());
        double balance = sellerCounter.getDoubleValue();

        if (balance < taxToPay) {
            Messages.NOT_ENOUGH_MONEY_SELL.send(sender);
            return true;
        }

        // Open confirmation menu
        openConfirmationMenu(player, auctionPlayer, current.clone(), price, currency, taxToPay);
        return true;
    }

    private double parseAmountWithSuffix(@NotNull String input) throws NumberFormatException {
        String s = input.trim().toUpperCase();
        double multiplier = 1.0;
        if (s.endsWith("B")) {
            multiplier = 1e9;
            s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("M")) {
            multiplier = 1e6;
            s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("K")) {
            multiplier = 1e3;
            s = s.substring(0, s.length() - 1);
        }
        s = s.replace(',', '.');
        if (s.isEmpty()) {
            throw new NumberFormatException("no number to parse");
        }
        return Double.parseDouble(s) * multiplier;
    }

    private long normalizePrice(String raw, FileConfiguration cfg) {
        String s = Objects.requireNonNullElse(raw, "").trim();
        Matcher m = PRICE_CAPTURE.matcher(s);
        if (!m.matches())
            throw new NumberFormatException("bad");
        String num = m.group(1);

        // Check if the input has K, M, or B suffix and use parseAmountWithSuffix
        String upperNum = num.toUpperCase();
        if (upperNum.endsWith("K") || upperNum.endsWith("M") || upperNum.endsWith("B")) {
            return Math.round(parseAmountWithSuffix(num));
        }

        // Original logic for numbers without suffixes
        boolean allowComma = cfg.getBoolean("sign-input.allow-comma-decimal", true);
        num = num.replace("`", "").replace("_", "").replace(" ", "").replace("'", "");
        if (allowComma) {
            int d = Math.max(num.lastIndexOf('.'), num.lastIndexOf(','));
            String intp = d >= 0 ? num.substring(0, d) : num;
            String frac = d >= 0 ? num.substring(d + 1) : "";
            intp = intp.replace(".", "").replace(",", "");
            num = intp + (d >= 0 ? "." + frac : "");
        } else {
            num = num.replace(",", "");
        }
        double value = Double.parseDouble(num);
        return Math.round(value);
    }

    private void openPriceSign(Player player, ItemStack snapshot) {
        openPriceSign(player, PlayerManager.getPlayer(player), snapshot, 0, CurrencyManager.getDefaultCurrency(), 0,
                false);
    }

    private void openPriceSign(
            Player player,
            AuctionPlayer auctionPlayer,
            ItemStack snapshot,
            long currentPrice,
            Currency currentCurrency,
            double prevTax,
            boolean canReturn) {
        FileConfiguration cfg = Configs.getConfig("config");
        if (!cfg.getBoolean("sign-input.enabled", true)) {
            Messages.SELL_SIGN_UNAVAILABLE.send(player);
            Messages.SELL_USAGE.send(player);
            return;
        }

        boolean bedrock;
        try {
            bedrock = BedrockAPI.isBedrock(player);
        } catch (Throwable t) {
            bedrock = false;
        }
        String l1 = bedrock ? "" : cfg.getString("sign-input.java.line1", "&6&lPRICE").replace("&", "§");
        String l2 = bedrock ? "" : cfg.getString("sign-input.java.line2", "&7Enter price").replace("&", "§");
        String l3 = bedrock
                ? ""
                : cfg.getString("sign-input.java.line3", "&7e.g. 150K, 1.5M, 2B").replace("&", "§");

        SignGUI gui;
        try {
            gui = SignGUI.builder()
                    .setLine(0, "")
                    .setLine(1, l1)
                    .setLine(2, l2)
                    .setLine(3, l3)
                    .setHandler(
                            (p, result) -> {
                                String input = result.getLine(0) != null ? result.getLine(0).trim() : "";
                                List<String> cancel = cfg.getStringList("sign-input.cancel-keywords");
                                if (cancel != null
                                        && cancel.stream().anyMatch(k -> k.equalsIgnoreCase(input))) {
                                    if (canReturn) {
                                        return List.of(SignGUIAction.run(() -> openConfirmationMenu(p, auctionPlayer,
                                                snapshot, currentPrice, currentCurrency, prevTax)));
                                    }
                                    Messages.SELL_INPUT_CANCELED.send(p);
                                    return List.of();
                                }
                                if (input.isEmpty()) {
                                    if (canReturn) {
                                        return List.of(SignGUIAction.run(() -> openConfirmationMenu(p, auctionPlayer,
                                                snapshot, currentPrice, currentCurrency, prevTax)));
                                    }
                                    Messages.SELL_INPUT_CANCELED.send(p);
                                    return List.of();
                                }
                                // Optional trailing currency token
                                String numPart = input;
                                Currency currency = CurrencyManager.getDefaultCurrency();
                                int sp = input.lastIndexOf(' ');
                                if (sp > 0 && sp < input.length() - 1) {
                                    String maybeCur = input.substring(sp + 1).trim();
                                    Currency found = CurrencyManager.getCurrency(maybeCur);
                                    if (found != null) {
                                        currency = found;
                                        numPart = input.substring(0, sp).trim();
                                    }
                                }

                                long price;
                                try {
                                    price = normalizePrice(numPart, cfg);
                                } catch (NumberFormatException e) {
                                    Messages.SELL_INVALID_NUMBER.send(p);
                                    if (cfg.getBoolean("sign-input.reopen-on-error", true)) {
                                        return List.of(SignGUIAction.run(() -> openPriceSign(p, auctionPlayer,
                                                snapshot.clone(), currentPrice, currentCurrency, prevTax, canReturn)));
                                    }
                                    return List.of();
                                }

                                if (AuctionManager.minimumPrice != -1
                                        && price < AuctionManager.minimumPrice
                                        && !p.hasPermission("hauctions.sell.bypassmin")) {
                                    Messages.MINIMUM_PRICE.send(
                                            p,
                                            fr.hyping.hypingauctions.util.Format
                                                    .formatNumber(AuctionManager.minimumPrice));
                                    if (cfg.getBoolean("sign-input.reopen-on-error", true)) {
                                        return List.of(SignGUIAction.run(() -> openPriceSign(p, auctionPlayer,
                                                snapshot.clone(), currentPrice, currentCurrency, prevTax, canReturn)));
                                    }
                                    return List.of();
                                }
                                if (AuctionManager.maximumPrice != -1
                                        && price > AuctionManager.maximumPrice
                                        && !p.hasPermission("hauctions.sell.bypassmax")) {
                                    Messages.MAXIMUM_PRICE.send(
                                            p,
                                            fr.hyping.hypingauctions.util.Format
                                                    .formatNumber(AuctionManager.maximumPrice));
                                    if (cfg.getBoolean("sign-input.reopen-on-error", true)) {
                                        return List.of(SignGUIAction.run(() -> openPriceSign(p, auctionPlayer,
                                                snapshot.clone(), currentPrice, currentCurrency, prevTax, canReturn)));
                                    }
                                    return List.of();
                                }

                                // Optional currency permission
                                if (!p.hasPermission(
                                        "hauctions.sell.currency." + currency.counter().getName())) {
                                    Messages.SELL_CURRENCY_NOT_ALLOWED.send(p);
                                    return List.of();
                                }
                                double tax = TaxesManager.getTax(p, price);
                                double newTax = price * tax / 100.0;
                                PlayerData data = CountersAPI.getOrCreateIfNotExists(p.getUniqueId());
                                AbstractValue<?> sellerCounter = currency.counter().getDataParser().getOrCreate(data,
                                        currency.counter());
                                if (sellerCounter.getDoubleValue() < newTax) {
                                    Messages.NOT_ENOUGH_MONEY_SELL.send(p);
                                    return List.of();
                                }

                                final long finalPrice = price;
                                final Currency finalCurrency = currency;
                                Bukkit.getGlobalRegionScheduler()
                                        .execute(
                                                HypingAuctions.getInstance(),
                                                () -> openConfirmationMenu(
                                                        p,
                                                        PlayerManager.getPlayer(p),
                                                        snapshot.clone(),
                                                        finalPrice,
                                                        finalCurrency,
                                                        newTax));
                                return List.of();
                            })
                    .callHandlerSynchronously(HypingAuctions.getInstance())
                    .build();
        } catch (Exception any) {
            Messages.SELL_SIGN_UNAVAILABLE.send(player);
            Messages.SELL_USAGE.send(player);
            return;
        }

        try {
            gui.open(player);
        } catch (Throwable t) {
            Messages.SELL_SIGN_UNAVAILABLE.send(player);
            Messages.SELL_USAGE.send(player);
        }
    }

    /**
     * Open the new menu-based confirmation GUI
     */
    private void openConfirmationMenu(
            Player player,
            AuctionPlayer auctionPlayer,
            ItemStack snapshot,
            long price,
            Currency currency,
            double taxToPay) {

        // Create session
        SellConfirmSession session = new SellConfirmSession(
                player,
                auctionPlayer,
                snapshot,
                price,
                currency,
                taxToPay
        );

        // Create and open menu
        SellConfirmMenu menu = new SellConfirmMenu(HypingAuctions.getInstance(), session);
        menu.open(player);
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("<price>");
        } else if (args.length == 2) {
            return CurrencyManager.getCurrencies().stream().map(Currency::name).toList();
        }
        return null;
    }
}
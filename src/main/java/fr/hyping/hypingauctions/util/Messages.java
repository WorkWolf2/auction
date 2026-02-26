package fr.hyping.hypingauctions.util;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

public enum Messages {
    PLAYER_NOT_EXIST("player-not-exist"),
    INVALID_ARGUMENTS("invalid-arguments"),
    INVALID_COMMAND("invalid-command"),
    NO_PERMISSION("no-permission"),

    SUCCESS_PLAYER_BANNED("success-player-banned"),
    YOUR_ARE_BANNED("your-are-banned"),
    PLAYER_NOT_BANNED("player-not-banned"),
    PLAYER_UNBANNED("player-unbanned"),

    NO_CONTEXT("no-context"),
    SALE_NOT_EXIST("sale-not-exist"),
    CANNOT_BUY_OWN("cannot-buy-own"),
    SALE_EXPIRED("sale-expired"),

    NOT_ENOUGH_MONEY("not-enough-money"),
    NOT_ENOUGH_MONEY_SELL("not-enough-money-sell"),

    ALREADY_BOUGHT("already-bought"),
    PURCHASE_SUCCESS("purchase-success"),
    PURCHASE_SUCCESS_DELIVERED_TO_INVENTORY("purchase-success-delivered-to-inventory"),
    PURCHASE_SUCCESS_ADDED_TO_BOUGHT_ITEMS("purchase-success-added-to-bought-items"),

    INVALID_AUCTION_ID("invalid-auction-id"),
    SUCCESS_AUCTION_EXPIRED("success-auction-expired"),

    INVALID_TYPE("invalid-type"),
    ITEM_DURABILITY_TOO_LOW("item-durability-too-low"),

    SEARCH_SET("search-set"),
    SEARCH_FILTER_CLEARED("search-filter-cleared"),

    SALES_LIMIT_REACHED("sales-limit-reached"),
    INVALID_CURRENCY("invalid-currency"),
    MINIMUM_PRICE("minimum-price"),
    MAXIMUM_PRICE("maximum-price"),
    NO_ITEM_IN_HAND("no-item-in-hand"),
    ITEM_NOT_ALLOWED("item-not-allowed"),
    SELL_USAGE("sell-usage"),
    SELL_MISSING_PRICE("sell-missing-price"),
    SELL_INVALID_NUMBER("sell-invalid-number"),
    SELL_OUT_OF_RANGE("sell-out-of-range"),
    SELL_BLACKLISTED("sell-blacklisted"),
    SELL_ITEM_CHANGED("sell-item-changed"),
    SELL_INPUT_CANCELED("sell-input-canceled"),
    SELL_SIGN_UNAVAILABLE("sell-sign-unavailable"),
    SELL_AUCTION_HOUSE_FULL("sell-auction-house-full"),
    SELL_CURRENCY_NOT_ALLOWED("sell-currency-not-allowed"),
    SUCCESS_ITEM_SOLD("success-item-sold"),
    SUCCESS_WITHDRAW("success-withdraw"),

    TRANSACTION_HISTORY_TITLE("transaction-history-title"),
    TRANSACTION_HISTORY_TITLE_FOR_PLAYER("transaction-history-title-for-player"),
    TRANSACTION_FORMAT("transaction-format"),
    NO_TRANSACTION("no-transaction"),
    NO_TRANSACTION_FOR_PLAYER("no-transaction-for-player"),
    TRANSACTION_BOUGHT_FORMAT("transaction-format-bought-format"),
    TRANSACTION_SOLD_FORMAT("transaction-format-sold-format"),

    HISTORY_PERMISSION_DENIED("history-permission-denied"),
    HISTORY_USAGE("history-usage"),

    PLAYER_ONLY_COMMAND("player-only-command"),

    PREMIUM_PROMOTION_SUCCESS("premium-promotion-success"),
    PREMIUM_PROMOTION_FAILED("premium-promotion-failed"),
    PREMIUM_NO_SLOTS_AVAILABLE("premium-no-slots-available"),
    PREMIUM_INSUFFICIENT_FUNDS("premium-insufficient-funds"),
    PREMIUM_ALREADY_PREMIUM("premium-already-premium"),
    PREMIUM_EXPIRED_ITEM("premium-expired-item"),
    PREMIUM_DURATION_INFO("premium-duration-info"),

    SELLER_ITEM_SOLD_NOTIFICATION("seller-item-sold-notification"),

    RECONNECTION_SALES_SUMMARY_HEADER("reconnection-sales-summary-header"),
    RECONNECTION_SALES_SUMMARY_ITEM("reconnection-sales-summary-item"),
    RECONNECTION_SALES_SUMMARY_FOOTER("reconnection-sales-summary-footer"),
    RECONNECTION_SALES_SUMMARY_NO_SALES("reconnection-sales-summary-no-sales"),

    CANCEL_DISABLED("cancel-disabled"),
    CANCEL_NOTHING_TO_CANCEL("cancel-nothing-to-cancel"),
    CANCEL_ALREADY_SOLD("cancel-already-sold"),
    CANCEL_SUCCESS("cancel-success"),
    ;

    private static final String TRANSLATION_PREFIX = "hyping.hypingauctions.";

    private final String id;
    private final String translationKey;

    Messages(String id) {
        this.id = id;
        this.translationKey = TRANSLATION_PREFIX + id;
    }

    public String getId() {
        return id;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public Component toComponent() {
        return fr.hyping.hypingauctions.HypingAuctions.getInstance().getLangComponent(translationKey);
    }

    public Component toComponent(Component... args) {
        return fr.hyping.hypingauctions.HypingAuctions.getInstance().getLangComponent(translationKey, args);
    }

    public void send(CommandSender sender) {
        sender.sendMessage(toComponent());
    }

    public void send(CommandSender sender, String... args) {
        Component[] components = new Component[args.length];
        for (int i = 0; i < args.length; i++) {
            components[i] = Component.text(args[i]);
        }
        sender.sendMessage(toComponent(components));
    }

    public void send(CommandSender sender, Component... args) {
        sender.sendMessage(toComponent(args));
    }

    public static String formatMoney(long amount) {
        return String.format("%,d", amount);
    }
}

package fr.hyping.hypingauctions.menu.sellconfirm;

import fr.hyping.hypingauctions.manager.AutomaticMaterialTranslationManager;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.Currency;
import fr.hyping.hypingauctions.sessions.PlaceholderableSession;
import fr.hyping.hypingauctions.util.Format;
import lombok.Data;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@Data
public class SellConfirmSession implements PlaceholderableSession {

    private final Player player;
    private final AuctionPlayer auctionPlayer;
    private final ItemStack itemToSell;
    private final long price;
    private final Currency currency;
    private final double taxToPay;

    private int averagePrice; // Will be updated asynchronously

    public SellConfirmSession(
            Player player,
            AuctionPlayer auctionPlayer,
            ItemStack itemToSell,
            long price,
            Currency currency,
            double taxToPay) {
        this.player = player;
        this.auctionPlayer = auctionPlayer;
        this.itemToSell = itemToSell.clone(); // Clone to prevent modifications
        this.price = price;
        this.currency = currency;
        this.taxToPay = taxToPay;
        this.averagePrice = 0; // Will be calculated
    }

    @Override
    public String getPlaceholder(String string) {
        return switch (string) {
            case "PLAYER" -> player.getName();
            case "PLAYER_UUID" -> player.getUniqueId().toString();

            case "PRICE" -> String.valueOf(price);
            case "PRICE_FORMATTED" -> Format.formatNumber(price);

            case "CURRENCY" -> currency.name();
            case "CURRENCY_COUNTER" -> currency.counter().getName();

            case "TAX" -> String.valueOf((long) taxToPay);
            case "TAX_FORMATTED" -> Format.formatNumber((long) taxToPay);

            case "NET_EARNINGS" -> String.valueOf(price - (long) taxToPay);
            case "NET_EARNINGS_FORMATTED" -> Format.formatNumber(price - (long) taxToPay);

            case "ITEM_NAME" -> {
                yield LegacyComponentSerializer.legacySection().serialize(
                        AutomaticMaterialTranslationManager.getInstance()
                                .getLocalizedComponent(player, itemToSell));
            }

            case "ITEM_TYPE" -> itemToSell.getType().name();
            case "ITEM_MATERIAL" -> AutomaticMaterialTranslationManager.getInstance()
                    .getFrenchName(itemToSell.getType());

            case "QUANTITY", "AMOUNT" -> String.valueOf(itemToSell.getAmount());

            case "AVERAGE_PRICE" -> averagePrice > 0 ? String.valueOf(averagePrice) : "Calculating...";
            case "AVERAGE_PRICE_FORMATTED" -> averagePrice > 0
                    ? Format.formatNumber(averagePrice)
                    : "Calculating...";

            case "AVERAGE_PRICE_TOTAL" -> {
                long total = averagePrice > 0 ? (long) averagePrice * itemToSell.getAmount() : 0;
                yield total > 0 ? String.valueOf(total) : "Calculating...";
            }
            case "AVERAGE_PRICE_TOTAL_FORMATTED" -> {
                long total = averagePrice > 0 ? (long) averagePrice * itemToSell.getAmount() : 0;
                yield total > 0 ? Format.formatNumber(total) : "Calculating...";
            }

            default -> "404:" + string;
        };
    }

}

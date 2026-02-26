package fr.hyping.hypingauctions.hook.placeholder;

import fr.hyping.hypingauctions.cache.PlayerNameCache;
import fr.hyping.hypingauctions.command.sub.ShulkerboxCommand;
import fr.hyping.hypingauctions.hook.IPlaceholderExtension;
import fr.hyping.hypingauctions.manager.EnchantmentTranslationManager;
import fr.hyping.hypingauctions.manager.PotionTranslationManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.service.AveragePriceService;
import fr.hyping.hypingauctions.util.CustomModelDataUtil;
import fr.hyping.hypingauctions.util.Format;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Nullable;

public class TargetPlaceholder implements IPlaceholderExtension {

    private static final LegacyComponentSerializer COMPONENT_SERIALIZER =
            LegacyComponentSerializer.legacyAmpersand();

    @Override
    public @Nullable String onReplace(AuctionPlayer player, String name, String[] args) {
        if (args.length < 1) return null;

        Auction auction = player.getContext().getTargetAuction();
        if (auction == null) return null;

        ItemStack item = auction.getItem();
        ItemMeta meta = item.getItemMeta();

        return switch (args[0].toLowerCase()) {

            // REAL ITEM. PRESERVES ARMOR TRIMS AND ALL NBT.
            case "item" -> "HYPING_ITEM";

            // ---------------- AUCTION ----------------
            case "price" -> {
                if (args.length >= 2 && args[1].equalsIgnoreCase("raw")) {
                    yield String.valueOf(auction.getPrice());
                }
                yield Format.formatNumber(auction.getPrice());
            }

            case "currency" -> {
                if (args.length >= 2 && args[1].equalsIgnoreCase("formatted")) {
                    yield auction.getCurrency().name();
                }
                yield auction.getCurrency().counter().getName();
            }

            case "seller" ->
                    PlayerNameCache.getInstance().getPlayerName(auction.getSeller().getPlayer());

            case "expiration" -> {
                long t = auction.getExpirationDate() - System.currentTimeMillis();
                if (t <= 0) yield "§cExpiré";
                yield Format.formatTime(t);
            }

            case "expiration_formatted" -> {
                long t = auction.getExpirationDate() - System.currentTimeMillis();
                if (t <= 0) yield "§cExpiré";
                yield Format.formatTimeDetailed(t);
            }

            case "expiration_hours" -> {
                long t = auction.getExpirationDate() - System.currentTimeMillis();
                if (t <= 0) yield "0";
                yield String.valueOf(t / 3600000);
            }

            case "expiration_minutes" -> {
                long t = auction.getExpirationDate() - System.currentTimeMillis();
                if (t <= 0) yield "0";
                yield String.valueOf((t % 3600000) / 60000);
            }

            case "expiration_seconds" -> {
                long t = auction.getExpirationDate() - System.currentTimeMillis();
                if (t <= 0) yield "0";
                yield String.valueOf((t % 60000) / 1000);
            }

            case "averageprice" -> {
                CompletableFuture<Integer> future =
                        AveragePriceService.getInstance().calculateAveragePrice(item);
                int perUnit = future.getNow(0);

                if (perUnit <= 0) {
                    perUnit = AveragePriceService.getInstance().getFromCacheIncludingExpired(item);
                }

                String na =
                        fr.hyping.hypingauctions.HypingAuctions.getInstance()
                                .getConfig()
                                .getString("placeholders.average-price.no-price", "Unknown");

                if (perUnit <= 0) yield na;

                int amount = item.getAmount() > 0 ? item.getAmount() : 1;
                yield Format.formatNumber((long) perUnit * amount);
            }

            case "material" -> item.getType().name();
            case "custom-model-data" -> CustomModelDataUtil.getCustomModelDataAsString(item);
            case "quantity" -> String.valueOf(item.getAmount());
            case "name" -> getItemDisplayName(item);

            case "lore" -> {
                if (meta == null || !meta.hasLore()) yield "";

                String lore =
                        meta.lore().stream()
                                .map(COMPONENT_SERIALIZER::serialize)
                                .map(this::sanitizeLegacy)
                                .reduce((a, b) -> a + "\n" + b)
                                .orElse("");

                if (item.getType() == Material.FIREWORK_ROCKET) {
                    lore = fixFireworkFlightDuration(item, lore);
                }

                yield lore;
            }

            case "exists" -> "true";
            case "shulkerbox" -> String.valueOf(ShulkerboxCommand.isShulkerBox(item));
            case "playerhead" -> getPlayerHead(item);

            case "enchantment" -> {
                if (args.length >= 2 && args[1].equalsIgnoreCase("has")) {
                    yield String.valueOf(!getItemEnchantments(item).isEmpty());
                }

                if (item.getType() == Material.ENCHANTED_BOOK) yield "";

                Map<Enchantment, Integer> enchants = getItemEnchantments(item);
                if (enchants.isEmpty()) yield "none";

                EnchantmentTranslationManager m = EnchantmentTranslationManager.getInstance();
                yield enchants.entrySet().stream()
                        .map(e -> m.translateEnchantment(e.getKey(), e.getValue()))
                        .reduce((a, b) -> a + "\n" + b)
                        .orElse("none");
            }

            case "potion-type" -> {
                if (!(meta instanceof PotionMeta potionMeta)) yield "Not a potion";

                try {
                    PotionType t = potionMeta.getBasePotionType();
                    if (t != null && !t.name().equals("UNCRAFTABLE")) yield t.name();
                } catch (NoSuchMethodError ignored) {}

                try {
                    yield potionMeta.getBasePotionData().getType().name();
                } catch (Throwable ignored) {}

                yield "WATER";
            }

            case "potion-displayname" -> {
                if (!(meta instanceof PotionMeta)) yield "Not a potion";
                yield PotionTranslationManager.getInstance().translatePotion(item);
            }

            default -> null;
        };
    }


    private String getPlayerHead(ItemStack item) {
        if (item.getType() != Material.PLAYER_HEAD) return "Unknown";

        try {
            fr.natsu.items.entity.CustomItem ci =
                    fr.natsu.items.entity.CustomItem.getCustomItem(item);
            if (ci != null) return "HYPING_ITEM";
        } catch (Throwable ignored) {}

        if (item.getItemMeta() instanceof SkullMeta skull) {
            try {
                var profile = skull.getPlayerProfile();
                if (profile != null && profile.getId() != null) {
                    return PlayerNameCache.getInstance().getPlayerName(profile.getId());
                }
            } catch (Throwable ignored) {}
        }
        return "Unknown";
    }

    private Map<Enchantment, Integer> getItemEnchantments(ItemStack item) {
        if (item.getType() == Material.ENCHANTED_BOOK &&
                item.getItemMeta() instanceof EnchantmentStorageMeta book) {
            return book.getStoredEnchants();
        }
        return item.getEnchantments();
    }

    private String getItemDisplayName(ItemStack item) {
        String oraxen = fr.hyping.hypingauctions.hook.OraxenHook.getOraxenDisplayName(item);
        if (oraxen != null) return sanitizeLegacy(oraxen);

        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return sanitizeLegacy(
                    LegacyComponentSerializer.legacyAmpersand()
                            .serialize(Objects.requireNonNull(meta.displayName())));
        }

        return sanitizeLegacy(
                LegacyComponentSerializer.legacyAmpersand().serialize(Component.translatable(item)));
    }

    private String sanitizeLegacy(String text) {
        return text == null ? null : text.replace("§r", "").replace("&r", "");
    }

    private String fixFireworkFlightDuration(ItemStack item, String lore) {
        if (!(item.getItemMeta() instanceof FireworkMeta meta) || !meta.hasPower()) return lore;

        int power = meta.getPower();

        Pattern p =
                Pattern.compile(
                        "(?i)(§[0-9a-fk-or]|&[0-9a-fk-or])*Flight Duration:(§[0-9a-fk-or]|&[0-9a-fk-or])*\\d+");

        return p.matcher(lore).replaceAll(m -> m.group().replaceAll("\\d+$", String.valueOf(power)));
    }
}

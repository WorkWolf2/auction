package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.service.AveragePriceService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Command to reset prices for items that were previously affected by custom model data
 * inconsistencies. This command helps fix pricing issues caused by the previous inconsistent
 * handling of CustomModelData vs ItemModel.
 */
public class PriceResetCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 0) {
            return false; // Show usage
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "all" -> {
                // Clear all cached prices
                AveragePriceService.getInstance().clearCache();
                sender.sendMessage(
                        Component.translatable("hyping.hypingauctions.command.pricereset.all-cleared"));
                return true;
            }
            case "material" -> {
                if (args.length < 2) {
                    sender.sendMessage(
                            Component.translatable("hyping.hypingauctions.command.pricereset.usage.material"));
                    return true;
                }

                String materialName = args[1].toUpperCase();
                try {
                    Material material = Material.valueOf(materialName);
                    AveragePriceService.getInstance().clearCacheForMaterial(material);
                    sender.sendMessage(
                            Component.translatable(
                                    "hyping.hypingauctions.command.pricereset.material-cleared",
                                    Component.text(material.name())));
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(
                            Component.translatable(
                                    "hyping.hypingauctions.command.pricereset.invalid-material",
                                    Component.text(materialName)));
                }
                return true;
            }
            case "hand" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(
                            Component.translatable("hyping.hypingauctions.command.pricereset.player-only"));
                    return true;
                }

                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                if (itemInHand.getType() == Material.AIR) {
                    sender.sendMessage(
                            Component.translatable("hyping.hypingauctions.command.pricereset.must-hold-item"));
                    return true;
                }

                sender.sendMessage(
                        Component.translatable(
                                "hyping.hypingauctions.command.pricereset.recalculating",
                                Component.text(itemInHand.getType().name())));

                CompletableFuture<Integer> future =
                        AveragePriceService.getInstance().forceRecalculatePrice(itemInHand);

                future
                        .thenAccept(
                                price -> {
                                    if (price > 0) {
                                        sender.sendMessage(
                                                Component.translatable(
                                                        "hyping.hypingauctions.command.pricereset.recalculated",
                                                        Component.text(price)));
                                    } else {
                                        sender.sendMessage(
                                                Component.translatable(
                                                        "hyping.hypingauctions.command.pricereset.no-price-data"));
                                    }
                                })
                        .exceptionally(
                                throwable -> {
                                    sender.sendMessage(
                                            Component.translatable(
                                                    "hyping.hypingauctions.command.pricereset.error",
                                                    Component.text(throwable.getMessage())));
                                    return null;
                                });

                return true;
            }
            case "vanilla" -> {
                // Reset prices for common vanilla items that were likely affected
                List<Material> vanillaItems =
                        Arrays.asList(
                                Material.PAPER,
                                Material.BOOK,
                                Material.STICK,
                                Material.STRING,
                                Material.LEATHER,
                                Material.FEATHER,
                                Material.WHEAT,
                                Material.CARROT,
                                Material.POTATO,
                                Material.BEETROOT,
                                Material.SUGAR_CANE,
                                Material.BAMBOO,
                                Material.KELP,
                                Material.DRIED_KELP,
                                Material.APPLE,
                                Material.BREAD,
                                Material.COOKED_BEEF,
                                Material.COOKED_PORKCHOP,
                                Material.COOKED_CHICKEN,
                                Material.COOKED_COD,
                                Material.COOKED_SALMON,
                                Material.BAKED_POTATO,
                                Material.PUMPKIN_PIE,
                                Material.COOKIE,
                                Material.MELON_SLICE,
                                Material.SWEET_BERRIES,
                                Material.GLOW_BERRIES,
                                Material.CHORUS_FRUIT,
                                Material.POISONOUS_POTATO,
                                Material.SPIDER_EYE,
                                Material.ROTTEN_FLESH,
                                Material.BONE,
                                Material.GUNPOWDER,
                                Material.BLAZE_POWDER,
                                Material.MAGMA_CREAM,
                                Material.GHAST_TEAR,
                                Material.ENDER_PEARL,
                                Material.SLIME_BALL,
                                Material.PRISMARINE_SHARD,
                                Material.PRISMARINE_CRYSTALS,
                                Material.RABBIT_FOOT,
                                Material.RABBIT_HIDE,
                                Material.PHANTOM_MEMBRANE,
                                Material.NAUTILUS_SHELL,
                                Material.HEART_OF_THE_SEA,
                                Material.HONEYCOMB,
                                Material.HONEY_BOTTLE);

                sender.sendMessage(
                        Component.translatable(
                                "hyping.hypingauctions.command.pricereset.vanilla-clearing",
                                Component.text(vanillaItems.size())));

                for (Material material : vanillaItems) {
                    AveragePriceService.getInstance().clearCacheForMaterial(material);
                }

                sender.sendMessage(
                        Component.translatable("hyping.hypingauctions.command.pricereset.vanilla-cleared"));
                return true;
            }
            case "info" -> {
                sender.sendMessage(
                        Component.translatable("hyping.hypingauctions.command.pricereset.info.header"));
                sender.sendMessage(
                        Component.translatable("hyping.hypingauctions.command.pricereset.info.description"));
                sender.sendMessage(
                        Component.translatable("hyping.hypingauctions.command.pricereset.info.spacer"));
                sender.sendMessage(
                        Component.translatable("hyping.hypingauctions.command.pricereset.info.all"));
                sender.sendMessage(
                        Component.translatable("hyping.hypingauctions.command.pricereset.info.material"));
                sender.sendMessage(
                        Component.translatable("hyping.hypingauctions.command.pricereset.info.hand"));
                sender.sendMessage(
                        Component.translatable("hyping.hypingauctions.command.pricereset.info.vanilla"));
                sender.sendMessage(
                        Component.translatable("hyping.hypingauctions.command.pricereset.info.help"));
                return true;
            }
            default -> {
                return false; // Show usage
            }
        }
    }

    @Nullable
    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("all", "material", "hand", "vanilla", "info");
            StringUtil.copyPartialMatches(args[0], subCommands, completions);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("material")) {
            // Suggest common materials that might need price reset
            List<String> materials =
                    Arrays.asList(
                            "PAPER",
                            "BOOK",
                            "STICK",
                            "STRING",
                            "LEATHER",
                            "FEATHER",
                            "WHEAT",
                            "CARROT",
                            "POTATO",
                            "BEETROOT",
                            "SUGAR_CANE",
                            "BAMBOO",
                            "KELP",
                            "DRIED_KELP",
                            "APPLE",
                            "BREAD",
                            "DIAMOND",
                            "EMERALD",
                            "GOLD_INGOT",
                            "IRON_INGOT",
                            "COPPER_INGOT",
                            "NETHERITE_INGOT");
            StringUtil.copyPartialMatches(args[1], materials, completions);
        }

        return completions;
    }
}

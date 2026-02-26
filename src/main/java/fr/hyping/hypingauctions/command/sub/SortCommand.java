package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.manager.CategoryManager;
import fr.hyping.hypingauctions.manager.PlayerManager;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.Category;
import fr.hyping.hypingauctions.manager.object.Filter;
import fr.hyping.hypingauctions.manager.object.PlayerContext;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SortCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length < 2) return false;

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        AuctionPlayer ap = PlayerManager.getPlayer(target);

        if (ap == null) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.sort.player-not-found"));
            return true;
        }

        PlayerContext context = ap.getContext();

        if (context == null) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.sort.target-no-context"));
            return true;
        }

        if (args[1].equalsIgnoreCase("reset")) {
            context.getFilter().reset();
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.sort.reset",
                            Component.text(target.getName())));
            return true;
        }

        if (args[1].equalsIgnoreCase("category")) {
            if (args.length < 3) {
                sender.sendMessage(
                        Component.translatable("hyping.hypingauctions.command.sort.missing-category"));
                return true;
            }

            Category category = CategoryManager.getCategory(args[2]);
            if (category == null) {
                sender.sendMessage(
                        Component.translatable(
                                "hyping.hypingauctions.command.sort.invalid-category",
                                Component.text(args[2])));
                return true;
            }
            context.getFilter().setCategory(category);
            context.reloadFilteredAuctions();
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.sort.category-set",
                            Component.text(args[2]),
                            Component.text(target.getName())));
            return true;
        }

        Filter.SortType type;
        if (args[1].equalsIgnoreCase("price")) {
            type = Filter.SortType.PRICE;
        } else if (args[1].equalsIgnoreCase("date")) {
            type = Filter.SortType.DATE;
        } else if (args[1].equalsIgnoreCase("name")) {
            type = Filter.SortType.NAME;
        } else {
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.sort.invalid-sort-type",
                            Component.text(args[1])));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.sort.missing-order"));
            return true;
        }

        Filter.SortOrder order;
        if (args[2].equalsIgnoreCase("asc") || args[2].equalsIgnoreCase("ascending")) {
            order = Filter.SortOrder.ASCENDING;
        } else if (args[2].equalsIgnoreCase("desc") || args[2].equalsIgnoreCase("descending")) {
            order = Filter.SortOrder.DESCENDING;
        } else {
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.sort.invalid-order",
                            Component.text(args[2])));
            return true;
        }

        context.getFilter().setSortType(type);
        context.getFilter().setSortOrder(order);
        context.reloadFilteredAuctions();
        sender.sendMessage(
                Component.translatable(
                        "hyping.hypingauctions.command.sort.set",
                        Component.text(type.name().toLowerCase()),
                        Component.text(order.name().toLowerCase()),
                        Component.text(target.getName())));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 1) return null;
        if (args.length == 2) return List.of("reset", "category", "price", "date", "name");
        if (args.length == 3 && args[1].equalsIgnoreCase("category"))
            return CategoryManager.getCategories().stream().map(Category::getName).toList();
        if (args.length == 3) return List.of("asc", "desc");
        return List.of();
    }
}

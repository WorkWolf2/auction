package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.manager.CategoryManager;
import fr.hyping.hypingauctions.manager.object.Category;
import fr.hyping.hypingauctions.menu.HAuctionMenu;
import fr.hyping.hypingauctions.menu.auctionList.AuctionListMenu;
import fr.hyping.hypingauctions.menu.auctionList.AuctionListSession;
import fr.hyping.hypingauctions.util.HAuctionMenuHelper;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CategoryCommand implements TabExecutor {

    private final HypingAuctions plugin;

    public CategoryCommand(HypingAuctions plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.translatable("hyping.hypingauctions.command.category.player-only"));
            return true;
        }

        HAuctionMenu openMenu = HAuctionMenuHelper.getOpenMenu(plugin, player);
        if (!(openMenu instanceof AuctionListMenu auctionListMenu)) {
            sender.sendMessage(Component.translatable("hyping.hypingauctions.command.category.no-auction-list-open"));
            return true;
        }

        AuctionListSession session = auctionListMenu.getSession();

        if (args.length == 0 || args[0].equalsIgnoreCase("tous") || args[0].equalsIgnoreCase("all")) {
            session.setSelectedCategory(null);
            auctionListMenu.refresh();
            sender.sendMessage(Component.translatable("hyping.hypingauctions.command.category.reset"));
            return true;
        }

        Category category = CategoryManager.getCategory(args[0]);
        if (category == null) {
            sender.sendMessage(Component.translatable(
                    "hyping.hypingauctions.command.category.invalid",
                    Component.text(args[0])));
            return true;
        }

        session.setSelectedCategory(category);
        auctionListMenu.refresh();
        sender.sendMessage(Component.translatable(
                "hyping.hypingauctions.command.category.set",
                Component.text(args[0])));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> names = new java.util.ArrayList<>();
            names.add("Tous");
            CategoryManager.getCategories().stream()
                    .map(Category::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .forEach(names::add);
            return names;
        }
        return List.of();
    }
}

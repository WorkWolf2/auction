package fr.hyping.hypingauctions.command.sub;

import be.darkkraft.hypingmenus.menu.holder.AbstractMenuHolder;
import be.darkkraft.hypingmenus.menu.holder.CustomMenuHolder;
import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.menu.HAuctionMenu;
import fr.hyping.hypingauctions.sessions.PaginatedSession;
import fr.hyping.hypingauctions.sessions.PlaceholderableSession;
import fr.hyping.hypingauctions.util.HAuctionMenuHelper;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;


public class PageCommand implements CommandExecutor {

    private final HypingAuctions plugin;

    public PageCommand(HypingAuctions plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.page.console-not-supported"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.page.usage"));
            return true;
        }

        CustomMenuHolder holder = HAuctionMenuHelper.getOpenMenuHolder(player);
        if (holder == null) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.page.no-menu-open"));
            return true;
        }

        HAuctionMenu menu = HAuctionMenuHelper.getOpenMenu(plugin, holder);
        if (menu == null) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.page.invalid-menu"));
            return true;
        }

        PlaceholderableSession session = menu.getSession();
        if (!(session instanceof PaginatedSession paginatedSession)) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.page.not-paginated"));
            return true;
        }

        String pageArg = args[0];
        int targetPage;

        try {
            if (pageArg.equalsIgnoreCase("next")) {
                if (!paginatedSession.hasNextPage()) {
                    sender.sendMessage(
                            Component.translatable("hyping.hypingauctions.command.page.already-last"));
                    return true;
                }
                targetPage = paginatedSession.nextPage();

            } else if (pageArg.equalsIgnoreCase("previous")
                    || pageArg.equalsIgnoreCase("prev")) {
                if (!paginatedSession.hasPreviousPage()) {
                    sender.sendMessage(
                            Component.translatable("hyping.hypingauctions.command.page.already-first"));
                    return true;
                }
                targetPage = paginatedSession.previousPage();

            } else {
                int requested = Integer.parseInt(pageArg);
                targetPage = Math.max(paginatedSession.getFirstPage(),
                        Math.min(requested, paginatedSession.getLastPage()));
            }

        } catch (NumberFormatException e) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.page.invalid-number"));
            return true;
        }

        paginatedSession.setPage(targetPage);

        menu.refresh();

        sender.sendMessage(Component.translatable(
                "hyping.hypingauctions.command.page.set",
                Component.text(targetPage),
                Component.text(paginatedSession.getLastPage())));

        return true;
    }
}
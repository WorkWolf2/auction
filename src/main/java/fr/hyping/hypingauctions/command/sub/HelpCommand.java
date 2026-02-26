package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.command.AuctionCommand;
import fr.hyping.hypingauctions.util.CommandBuilder;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class HelpCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }

        sendHelpMessage(sender, page);
        return true;
    }

    private List<Component> getPlayerHelpMessages(CommandSender sender) {
        List<Component> messages = new ArrayList<>();

        for (CommandBuilder commandBuilder : AuctionCommand.getCommands()) {
            if (!commandBuilder.canExecute(sender)) continue;

            Component component =
                    Component.translatable("hyping.hypingauctions.command.help.entry.prefix")
                            .append(
                                    Component.text(commandBuilder.getUsage(), NamedTextColor.GREEN)
                                            .hoverEvent(
                                                    Component.translatable("hyping.hypingauctions.command.help.entry.hover")
                                                            .color(NamedTextColor.GRAY))
                                            .clickEvent(
                                                    ClickEvent.suggestCommand(
                                                            "/hauctions " + commandBuilder.getName() + " ")))
                            .append(Component.translatable("hyping.hypingauctions.command.help.entry.separator"))
                            .append(Component.text(commandBuilder.getDescription()));
            messages.add(component);
        }
        return messages;
    }

    private void sendHelpMessage(CommandSender sender, int page) {
        List<Component> messages = getPlayerHelpMessages(sender);
        int HELP_PER_PAGE = 8;
        int maxPage = (int) Math.ceil(messages.size() / (double) HELP_PER_PAGE);

        if (page < 1 || page > maxPage) {
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.help.invalid-page",
                            Component.text(maxPage)));
            return;
        }

        Component leftDecorator =
                Component.translatable("hyping.hypingauctions.command.help.decorator.left");
        Component rightDecorator =
                Component.translatable("hyping.hypingauctions.command.help.decorator.right");
        Component title = Component.translatable("hyping.hypingauctions.command.help.title");

        sender.sendMessage(leftDecorator.append(title).append(rightDecorator));
        for (int i = 0; i < HELP_PER_PAGE; i++) {
            int index = (page - 1) * HELP_PER_PAGE + i;
            if (index >= messages.size()) break;
            sender.sendMessage(messages.get(index));
        }

        Component component = leftDecorator;
        if (page > 1) component = component.append(previousPage(page));
        component =
                component.append(
                        Component.translatable(
                                "hyping.hypingauctions.command.help.page",
                                Component.text(page),
                                Component.text(maxPage)));
        if (page < maxPage) component = component.append(nextPage(page));
        sender.sendMessage(component.append(rightDecorator));
    }

    private Component nextPage(int page) {
        return Component.translatable("hyping.hypingauctions.command.help.next")
                .hoverEvent(
                        Component.translatable("hyping.hypingauctions.command.help.next.hover")
                                .color(NamedTextColor.RED))
                .clickEvent(ClickEvent.runCommand("/hauctions help " + (page + 1)));
    }

    private Component previousPage(int page) {
        return Component.translatable("hyping.hypingauctions.command.help.previous")
                .hoverEvent(
                        Component.translatable("hyping.hypingauctions.command.help.previous.hover")
                                .color(NamedTextColor.RED))
                .clickEvent(ClickEvent.runCommand("/hauctions help " + (page - 1)));
    }
}

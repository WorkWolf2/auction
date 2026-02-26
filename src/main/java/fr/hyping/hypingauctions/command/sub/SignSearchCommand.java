package fr.hyping.hypingauctions.command.sub;

import com.tcoded.bedrockutil.api.BedrockAPI;
import de.rapha149.signgui.SignGUI;
import de.rapha149.signgui.SignGUIAction;
import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.manager.AuctionManager;
import fr.hyping.hypingauctions.manager.PlayerManager;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.MenuType;
import fr.hyping.hypingauctions.util.Configs;
import fr.hyping.hypingauctions.util.Messages;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Command that opens a sign GUI for searching items using French material names */
public class SignSearchCommand implements CommandExecutor, TabCompleter {

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

        // Check if sign search is enabled
        FileConfiguration config = Configs.getConfig("config");
        if (!config.getBoolean("sign-search.enabled", true)) {
            player.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.sign-search.disabled"));
            return true;
        }


        // If arguments are provided, perform direct search
        if (args.length > 0) {
            StringBuilder searchTermBuilder = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                if (i > 0) searchTermBuilder.append(" ");
                searchTermBuilder.append(args[i]);
            }
            String searchTerm = searchTermBuilder.toString().trim();

            if (!searchTerm.isEmpty()) {
                performDirectSearch(player, searchTerm, config);
                return true;
            }
        }

        try {
            // Get configurable sign text
            String line1 = config.getString("sign-search.line1", "&6&lRECHERCHE");
            String line2 = config.getString("sign-search.line2", "&7Tapez le nom");
            String line3 = config.getString("sign-search.line3", "&7de l'objet");


            // Create the sign GUI with configurable text
            boolean java = !BedrockAPI.isBedrock(player);
            String finalLine1 = java ? line1.replace("&", "§") : "";
            String finalLine2 = java ? line2.replace("&", "§") : "";
            String finalLine3 = java ? line3.replace("&", "§") : "";
            SignGUI signGUI =
                    SignGUI.builder()
                            .setLine(0, "") // Line 0 is the input field (empty by default)
                            .setLine(1, finalLine1)
                            .setLine(2, finalLine2)
                            .setLine(3, finalLine3)
                            .setHandler(
                                    (signPlayer, result) -> {
                                        String[] lines = result.getLines();

                                        // Get the search term from line 0 only (the input field)
                                        String search = lines[0] != null ? lines[0].trim() : "";

                                        if (search.isEmpty()) {
                                            String noSearchMsg =
                                                    config.getString(
                                                            "sign-search.messages.no-search-term",
                                                            "hyping.hypingauctions.command.sign-search.no-search-term");
                                            player.sendMessage(Component.translatable(noSearchMsg));
                                            return List.of(
                                                    SignGUIAction.run(
                                                            () -> {
                                                                // Reopen the sign GUI
                                                                openSignSearch(player);
                                                            }));
                                        }

                                        // Directly perform search with the entered term
                                        // This will filter auctions in the main GUI instead of showing material names
                                        performDirectSearch(player, search, config);

                                        return List.of(); // Close the sign GUI
                                    })
                            .callHandlerSynchronously(HypingAuctions.getInstance())
                            .build();

            signGUI.open(player);

        } catch (Exception e) {
            HypingAuctions.getInstance()
                    .getLogger()
                    .severe("Failed to open sign search GUI: " + e.getMessage());
            String guiErrorMsg =
                    config.getString(
                            "sign-search.messages.gui-error",
                            "hyping.hypingauctions.command.sign-search.gui-error");
            player.sendMessage(Component.translatable(guiErrorMsg));
        }

        return true;
    }

    /** Opens the sign search GUI for a player */
    private void openSignSearch(Player player) {
        // Delay the reopening slightly to avoid issues
        org.bukkit.Bukkit.getGlobalRegionScheduler()
                .runDelayed(
                        HypingAuctions.getInstance(), (task) -> onCommand(player, null, "", new String[0]), 5L);
    }

    /** Performs direct search with the entered term and opens the auction menu */
    private void performDirectSearch(Player player, String searchTerm, FileConfiguration config) {
        try {
            AuctionPlayer auctionPlayer = PlayerManager.getPlayer(player);
            auctionPlayer.openMenu(MenuType.AUCTION);

            // Set the search filter directly with the entered search term
            // This will filter auctions by their display names containing the search term
            auctionPlayer.getContext().getFilter().setSearch(searchTerm);
            auctionPlayer.getContext().reloadFilteredAuctions();

            // Send confirmation message
            Messages.SEARCH_SET.send(player, searchTerm);

            AuctionManager.executeMainCommands(player, false);

            String successMsg =
                    config.getString(
                            "sign-search.messages.search-success",
                            "hyping.hypingauctions.command.sign-search.search-success");
            player.sendMessage(
                    Component.translatable(successMsg, Component.text(searchTerm)));

        } catch (Exception e) {
            HypingAuctions.getInstance()
                    .getLogger()
                    .severe("Failed to perform search: " + e.getMessage());
            String errorMsg =
                    config.getString(
                            "sign-search.messages.error", "hyping.hypingauctions.command.sign-search.error");
            player.sendMessage(Component.translatable(errorMsg));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {

        if (args.length == 1) {
            // Suggest common search terms for the first argument
            return List.of("[item]", args[0]);
        }

        // For additional arguments, suggest the current argument (allows multi-word searches)
        if (args.length > 1) {
            return List.of(args[args.length - 1]);
        }

        return List.of();
    }
}

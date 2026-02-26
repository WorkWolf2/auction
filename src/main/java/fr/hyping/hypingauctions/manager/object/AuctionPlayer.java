package fr.hyping.hypingauctions.manager.object;

import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.manager.object.SessionFactory;
import fr.hyping.hypingauctions.menu.HAuctionMenu;
import fr.hyping.hypingauctions.menu.auctionList.AuctionListMenu;
import fr.hyping.hypingauctions.menu.auctionList.AuctionListSession;
import fr.hyping.hypingauctions.menu.bought.BoughtMenu;
import fr.hyping.hypingauctions.menu.bought.BoughtMenuSession;
import fr.hyping.hypingauctions.sessions.PlaceholderableSession;

import java.util.ArrayList;
import java.util.List;

import lombok.Setter;
import org.bukkit.OfflinePlayer;

public class AuctionPlayer {
    private final OfflinePlayer player;
    private final List<Auction> sales;
    private final List<Auction> purchases;
    private final List<Auction> expired;
    private PlayerContext context;

    public AuctionPlayer(OfflinePlayer player) {
        this.player = player;
        this.sales = new ArrayList<>();
        this.purchases = new ArrayList<>();
        this.expired = new ArrayList<>();
        this.context = null;
    }

    public OfflinePlayer getPlayer() {
        return player;
    }

    public List<Auction> getSales() {
        return sales;
    }

    public List<Auction> getPurchases() {
        return purchases;
    }

    public List<Auction> getExpired() {
        return expired;
    }

    public List<Auction> getExpiredSales() {
        List<Auction> auctions = new ArrayList<>(this.expired);
        auctions.addAll(this.sales);
        return auctions;
    }

    public PlayerContext getContext() {
        return context;
    }

    public boolean openMenu(MenuType type, boolean reset) {
        if (player.getPlayer() == null)
            return false;

        // Initialize or reset context
        if (context == null || reset) {
            this.context = new PlayerContext(this, type);
        } else {
            context.setMenu(type);
            // Reset to page 1 when switching menu types
            context.setPage(1);
        }

        // Create session from context
        int itemsPerPage = getItemsPerPageForMenu(type);
        PlaceholderableSession session = SessionFactory.createFromContext(context, itemsPerPage);

        // Create and open the appropriate menu
        HAuctionMenu menu = createMenuForType(type, session);
        menu.open(player.getPlayer());

        return true;
    }

    public boolean openMenu(MenuType type) {
        return openMenu(type, false);
    }

    private int getItemsPerPageForMenu(MenuType type) {
        HypingAuctions plugin = HypingAuctions.getInstance();
        // Get from config or use default based on menu type
        return plugin.getConfig().getInt("menus." + type.name().toLowerCase() + ".items-per-page", 45);
    }

    private HAuctionMenu createMenuForType(MenuType type, PlaceholderableSession session) {
        HypingAuctions plugin = HypingAuctions.getInstance();

        return switch (type) {
            case AUCTION -> new AuctionListMenu(plugin, (AuctionListSession) session);
            case BOUGHT -> new BoughtMenu(plugin, (BoughtMenuSession) session);
            case EXPIRED -> new fr.hyping.hypingauctions.menu.expired.ExpiredMenu(plugin,
                    (fr.hyping.hypingauctions.menu.expired.ExpiredMenuSession) session);
            case HISTORY -> new fr.hyping.hypingauctions.menu.history.HistoryMenu(plugin,
                    (fr.hyping.hypingauctions.menu.history.HistoryMenuSession) session);
            case SIMILAR_ITEMS -> new fr.hyping.hypingauctions.menu.similar.SimilarItemsMenu(plugin,
                    (fr.hyping.hypingauctions.menu.similar.SimilarItemsMenuSession) session);
            default -> throw new IllegalArgumentException("Unknown menu type: " + type);
        };
    }

    @Setter
    private int soldCount = 0;
    @Setter
    private int boughtCount = 0;

    public int getSoldCount() {
        return soldCount;
    }

    public void incrementSoldCount() {
        this.soldCount++;
    }

    public int getBoughtCount() {
        return boughtCount;
    }

    public void incrementBoughtCount() {
        this.boughtCount++;
    }

    @Setter
    private long totalSpent = 0;
    @Setter
    private long totalEarned = 0;

    public long getTotalSpent() {
        return totalSpent;
    }

    public long getTotalEarned() {
        return totalEarned;
    }

}
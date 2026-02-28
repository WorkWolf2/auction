package fr.hyping.hypingauctions.menu.bought;

import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.sessions.PaginatedSession;
import fr.hyping.hypingauctions.sessions.PlaceholderableSession;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.entity.Player;

import java.util.List;

@Data
public class BoughtMenuSession implements PlaceholderableSession, PaginatedSession {

    private final Player player;
    private final AuctionPlayer auctionPlayer;
    private int page;
    private final int itemsPerPage;

    public BoughtMenuSession(Player player, AuctionPlayer auctionPlayer, int itemsPerPage) {
        this.player = player;
        this.auctionPlayer = auctionPlayer;
        this.page = 1;
        this.itemsPerPage = itemsPerPage;
    }

    public BoughtMenuSession(Player player, AuctionPlayer auctionPlayer, int page, int itemsPerPage) {
        this.player = player;
        this.auctionPlayer = auctionPlayer;
        this.page = Math.max(1, page);
        this.itemsPerPage = itemsPerPage;
    }

    @Override
    public String getPlaceholder(String string) {
        return switch (string) {
            case "CURRENT_PAGE" -> String.valueOf(page);
            case "MAX_PAGES"    -> String.valueOf(getLastPage());
            case "TOTAL_PURCHASES" -> String.valueOf(auctionPlayer.getPurchases().size());
            default -> null;
        };
    }

    @Override
    public void setPage(int page) {
        this.page = Math.max(getFirstPage(), Math.min(page, getLastPage()));
    }

    @Override
    public int getFirstPage() {
        return 1;
    }

    @Override
    public int getLastPage() {
        List<Auction> purchases = auctionPlayer.getPurchases();
        if (purchases.isEmpty() || itemsPerPage <= 0) return 1;
        return (int) Math.ceil((double) purchases.size() / itemsPerPage);
    }

    public List<Auction> getAuctionsForCurrentPage() {
        List<Auction> allPurchases = auctionPlayer.getPurchases();
        if (allPurchases.isEmpty()) return List.of();

        int startIndex = (page - 1) * itemsPerPage;
        if (startIndex >= allPurchases.size()) {
            this.page = getLastPage();
            startIndex = (this.page - 1) * itemsPerPage;
        }

        int endIndex = Math.min(startIndex + itemsPerPage, allPurchases.size());
        return allPurchases.subList(startIndex, endIndex);
    }
}

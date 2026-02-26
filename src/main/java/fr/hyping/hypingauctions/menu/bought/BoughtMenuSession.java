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
@AllArgsConstructor
public class BoughtMenuSession implements PlaceholderableSession, PaginatedSession {

    private Player player;
    private AuctionPlayer auctionPlayer;
    private int page;
    private int itemsPerPage;

    @Override
    public String getPlaceholder(String string) {
        return switch (string) {
            case "CURRENT_PAGE" -> String.valueOf(page);
            case "MAX_PAGES" -> String.valueOf(getLastPage());
            case "TOTAL_PURCHASES" -> String.valueOf(auctionPlayer.getPurchases().size());
            default -> null;
        };
    }

    @Override
    public void setPage(int page) {
        this.page = page;
    }

    @Override
    public int getFirstPage() {
        return 1;
    }

    @Override
    public int getLastPage() {
        List<Auction> purchases = auctionPlayer.getPurchases();
        if (purchases.isEmpty()) {
            return 1;
        }
        return (int) Math.ceil((double) purchases.size() / itemsPerPage);
    }

    public List<Auction> getAuctionsForCurrentPage() {
        List<Auction> allPurchases = auctionPlayer.getPurchases();
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allPurchases.size());

        if (startIndex >= allPurchases.size()) {
            return List.of();
        }

        return allPurchases.subList(startIndex, endIndex);
    }
}

package fr.hyping.hypingauctions.menu.expired;

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
public class ExpiredMenuSession implements PlaceholderableSession, PaginatedSession {

    private final Player player;
    private final AuctionPlayer auctionPlayer;
    private int page;
    private final int itemsPerPage;


    @Override
    public String getPlaceholder(String string) {
        return switch (string) {
            case "CURRENT_PAGE" -> String.valueOf(page);
            case "MAX_PAGES" -> String.valueOf(getLastPage());
            case "TOTAL_EXPIRED" -> String.valueOf(auctionPlayer.getExpired().size());
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
        List<Auction> expired = auctionPlayer.getExpired();
        if (expired.isEmpty()) {
            return 1;
        }
        return (int) Math.ceil((double) expired.size() / itemsPerPage);
    }

    public List<Auction> getAuctionsForCurrentPage() {
        List<Auction> allExpired = auctionPlayer.getExpired();
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allExpired.size());

        if (startIndex >= allExpired.size()) {
            return List.of();
        }

        return allExpired.subList(startIndex, endIndex);
    }
}

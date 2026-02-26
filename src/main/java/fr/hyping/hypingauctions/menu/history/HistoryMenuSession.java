package fr.hyping.hypingauctions.menu.history;

import fr.hyping.hypingauctions.manager.HistoryManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.Filter;
import fr.hyping.hypingauctions.sessions.PaginatedSession;
import fr.hyping.hypingauctions.sessions.PlaceholderableSession;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.entity.Player;

import java.util.List;

@Data
@AllArgsConstructor
public class HistoryMenuSession implements PlaceholderableSession, PaginatedSession {

    private final Player player;
    private final AuctionPlayer auctionPlayer;
    private int page;
    private final int itemsPerPage;

    @Override
    public String getPlaceholder(String string) {
        return switch (string) {
            case "CURRENT_PAGE" -> String.valueOf(page);
            case "MAX_PAGES" -> String.valueOf(getLastPage());
            case "TOTAL_HISTORY" -> String.valueOf(getAllHistory().size());
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
        List<Auction> history = getAllHistory();
        if (history.isEmpty()) {
            return 1;
        }
        return (int) Math.ceil((double) history.size() / itemsPerPage);
    }

    public List<Auction> getAllHistory() {
        return HistoryManager.getPlayerHistory(player);
    }

    public List<Auction> getAuctionsForCurrentPage() {
        List<Auction> allHistory = getAllHistory();
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allHistory.size());

        if (startIndex >= allHistory.size()) {
            return List.of();
        }

        return allHistory.subList(startIndex, endIndex);
    }
}

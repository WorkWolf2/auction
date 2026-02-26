package fr.hyping.hypingauctions.menu.auctionList;

import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.sessions.PaginatedSession;
import fr.hyping.hypingauctions.sessions.PlaceholderableSession;
import fr.hyping.hypingauctions.util.Format;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.entity.Player;

import java.util.List;

@Data
public class AuctionListSession implements PlaceholderableSession, PaginatedSession {

    private final Player player;
    private final List<Auction> allAuctions;

    private int page = 1;
    private final int itemsPerPage;

    @Override
    public String getPlaceholder(String string) {
        return switch (string) {
            case "CURRENT_PAGE" -> String.valueOf(page);
            case "MAX_PAGES" -> String.valueOf(getLastPage());
            case "TOTAL_AUCTIONS" -> String.valueOf(getAllAuctions().size());
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
        if (allAuctions.isEmpty()) {
            return 1;
        }
        return (int) Math.ceil((double) allAuctions.size() / itemsPerPage);
    }

    public List<Auction> getAuctionsForCurrentPage() {
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allAuctions.size());

        if (startIndex >= allAuctions.size()) {
            return List.of();
        }

        return allAuctions.subList(startIndex, endIndex);
    }
}

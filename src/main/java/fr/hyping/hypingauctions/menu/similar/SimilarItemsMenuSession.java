package fr.hyping.hypingauctions.menu.similar;

import fr.hyping.hypingauctions.manager.AuctionManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.sessions.PaginatedSession;
import fr.hyping.hypingauctions.sessions.PlaceholderableSession;
import fr.hyping.hypingauctions.util.CustomModelDataUtil;
import lombok.Data;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class SimilarItemsMenuSession implements PlaceholderableSession, PaginatedSession {

    private final Player player;
    private final Auction targetAuction;
    private int page;
    private final int itemsPerPage;

    public SimilarItemsMenuSession(Player player, Auction targetAuction, int initialPage, int itemsPerPage) {
        this.player = player;
        this.targetAuction = targetAuction;
        this.page = initialPage;
        this.itemsPerPage = itemsPerPage;
    }

    @Override
    public String getPlaceholder(String string) {
        return switch (string) {
            case "CURRENT_PAGE" -> String.valueOf(page);
            case "MAX_PAGES" -> String.valueOf(getLastPage());
            case "TOTAL_SIMILAR" -> String.valueOf(getAllSimilarAuctions().size());
            case "TARGET_ITEM" -> targetAuction != null ? targetAuction.getItem().getType().name() : "N/A";
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
        List<Auction> similar = getAllSimilarAuctions();
        if (similar.isEmpty()) {
            return 1;
        }
        return (int) Math.ceil((double) similar.size() / itemsPerPage);
    }

    public List<Auction> getAllSimilarAuctions() {
        if (targetAuction == null || targetAuction.getItem() == null) {
            return List.of();
        }

        ItemStack targetItem = targetAuction.getItem();
        List<Auction> allAuctions = AuctionManager.getAuctions();

        return allAuctions.stream()
                .filter(auction -> auction != targetAuction)
                .filter(auction -> CustomModelDataUtil.areSimilarItems(auction.getItem(), targetItem))
                .filter(auction -> !auction.isExpired())
                .collect(Collectors.toList());
    }

    public List<Auction> getAuctionsForCurrentPage() {
        List<Auction> allSimilar = getAllSimilarAuctions();
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allSimilar.size());

        if (startIndex >= allSimilar.size()) {
            return List.of();
        }

        return allSimilar.subList(startIndex, endIndex);
    }
}

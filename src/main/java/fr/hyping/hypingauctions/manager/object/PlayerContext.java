package fr.hyping.hypingauctions.manager.object;

import fr.hyping.hypingauctions.manager.AuctionManager;
import fr.hyping.hypingauctions.manager.HistoryManager;
import fr.hyping.hypingauctions.util.CustomModelDataUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class PlayerContext {
    private final AuctionPlayer player;
    private final Filter filter;

    private Auction target;
    @Setter
    private MenuType menu;
    @Setter
    private int page;

    /**
     * -- GETTER --
     *  Get filtered auctions based on current menu type
     *  This returns the cached filteredAuctions list
     */
    private final List<Auction> filteredAuctions;
    private final List<Auction> similarAuctions;

    public PlayerContext(AuctionPlayer player, MenuType menu) {
        this.filter = new Filter();
        this.player = player;
        this.menu = menu;
        this.page = 1; // Start from page 1 (1-indexed for display)
        this.filteredAuctions = new ArrayList<>();
        this.similarAuctions = new ArrayList<>();

        reloadFilteredAuctions();
    }

    public Auction getTargetAuction() {
        return target;
    }

    public void setTarget(Auction target) {
        this.target = target;
        // Auto-populate similar items when target is set
        if (target != null) {
            populateSimilarItems(target);
        }
    }

    /**
     * Reload filtered auctions based on current menu type
     * Call this when the menu type changes or when data needs to be refreshed
     */
    public void reloadFilteredAuctions() {
        filteredAuctions.clear();

        switch (menu) {
            case AUCTION -> filteredAuctions.addAll(AuctionManager.getAuctions(filter));
            case BOUGHT -> filteredAuctions.addAll(player.getPurchases());
            case EXPIRED -> filteredAuctions.addAll(player.getExpired());
            case HISTORY -> filteredAuctions.addAll(HistoryManager.getPlayerHistory(player.getPlayer(), filter));
            case SIMILAR_ITEMS -> filteredAuctions.addAll(similarAuctions);
            default -> {} // Do nothing for unknown menu types
        }
    }

    /**
     * Refresh the similar items cache
     */
    public void refreshSimilarItems() {
        if (target != null) {
            populateSimilarItems(target);
        }
    }

    /**
     * Automatically populates similar items based on the target auction
     * Finds auctions with the same material and CustomModelData, excluding the target itself
     *
     * @param targetAuction The auction to find similar items for
     */
    private void populateSimilarItems(Auction targetAuction) {
        similarAuctions.clear();

        if (targetAuction == null || targetAuction.getItem() == null) {
            return;
        }

        ItemStack targetItem = targetAuction.getItem();
        List<Auction> allAuctions = AuctionManager.getAuctions();

        similarAuctions.addAll(
                allAuctions.stream()
                        .filter(auction -> auction != targetAuction) // Exclude the target auction itself
                        .filter(auction -> isSimilarItem(auction.getItem(), targetItem))
                        .filter(auction -> !auction.isExpired()) // Only active auctions
                        .collect(Collectors.toList()));
    }

    /**
     * Checks if two items are similar based on material and CustomModelData
     * Uses the unified custom model data utility for consistent comparison
     *
     * @param item1 First item to compare
     * @param item2 Second item to compare
     * @return true if items have the same material and CustomModelData
     */
    private boolean isSimilarItem(ItemStack item1, ItemStack item2) {
        return CustomModelDataUtil.areSimilarItems(item1, item2);
    }
}
package fr.hyping.hypingauctions.manager.object;

import fr.hyping.hypingauctions.manager.AuctionManager;
import fr.hyping.hypingauctions.menu.auctionList.AuctionListSession;
import fr.hyping.hypingauctions.menu.bought.BoughtMenuSession;
import fr.hyping.hypingauctions.menu.expired.ExpiredMenuSession;
import fr.hyping.hypingauctions.menu.history.HistoryMenuSession;
import fr.hyping.hypingauctions.menu.similar.SimilarItemsMenuSession;
import fr.hyping.hypingauctions.sessions.PlaceholderableSession;

import java.util.List;

public class SessionFactory {

    public static PlaceholderableSession createFromContext(PlayerContext context, int itemsPerPage) {
        AuctionPlayer auctionPlayer = context.getPlayer();

        return switch (context.getMenu()) {
            case AUCTION -> new AuctionListSession(
                    auctionPlayer.getPlayer().getPlayer(),
                    AuctionManager.getAuctions(),
                    itemsPerPage
            );
            case BOUGHT -> new BoughtMenuSession(
                    auctionPlayer.getPlayer().getPlayer(),
                    auctionPlayer,
                    context.getPage(),
                    itemsPerPage
            );
            case EXPIRED -> new ExpiredMenuSession(
                    auctionPlayer.getPlayer().getPlayer(),
                    auctionPlayer,
                    context.getPage(),
                    itemsPerPage
            );
            case HISTORY -> new HistoryMenuSession(
                    auctionPlayer.getPlayer().getPlayer(),
                    auctionPlayer,
                    context.getPage(),
                    itemsPerPage
            );
            case SIMILAR_ITEMS -> new SimilarItemsMenuSession(
                    auctionPlayer.getPlayer().getPlayer(),
                    context.getTargetAuction(),
                    context.getPage(),
                    itemsPerPage
            );
            default -> throw new IllegalArgumentException("Unknown menu type: " + context.getMenu());
        };
    }
}

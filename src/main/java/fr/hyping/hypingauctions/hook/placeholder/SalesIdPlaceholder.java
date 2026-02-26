package fr.hyping.hypingauctions.hook.placeholder;

import fr.hyping.hypingauctions.hook.IPlaceholderExtension;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.PlayerContext;
import org.jetbrains.annotations.Nullable;

public class SalesIdPlaceholder implements IPlaceholderExtension {
    @Override
    public @Nullable String onReplace(AuctionPlayer player, String name, String[] args) {
        if (args.length != 1) return null;

        PlayerContext context = player.getContext();
        if (context == null) return null;

        int index;
        try {
            index = Integer.parseInt(args[0]) - 1;
            if (index < 0) return null;
        } catch (NumberFormatException e) {
            return null;
        }

        if (index >= context.getFilteredAuctions().size()) return null;

        Auction auction = context.getFilteredAuctions().get(index);
        return String.valueOf(auction.getSeller().getSales().indexOf(auction) + 1);
    }
}

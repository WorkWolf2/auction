package fr.hyping.hypingauctions.hook.placeholder;

import fr.hyping.hypingauctions.hook.IPlaceholderExtension;
import fr.hyping.hypingauctions.manager.CategoryManager;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import org.jetbrains.annotations.Nullable;

public class PagesPlaceholder implements IPlaceholderExtension {
    @Override
    public @Nullable String onReplace(AuctionPlayer player, String name, String[] args) {
        if (player.getContext() == null) return "1";
        int auctions = player.getContext().getFilteredAuctions().size();
        int auctionsPerPage = CategoryManager.getItemsPerPage();
        return String.valueOf(Math.max(1, (auctions + auctionsPerPage - 1) / auctionsPerPage));
    }
}

package fr.hyping.hypingauctions.hook.placeholder;

import fr.hyping.hypingauctions.hook.IPlaceholderExtension;
import fr.hyping.hypingauctions.manager.AuctionManager;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Placeholder returning the total number of items currently on sale across all players. Usage:
 * %hauctions_sales-total%
 */
public class SalesTotalPlaceholder implements IPlaceholderExtension {

    @Override
    public @Nullable String onReplace(AuctionPlayer player, String name, String[] args) {
        return String.valueOf(AuctionManager.getAuctions().size());
    }
}

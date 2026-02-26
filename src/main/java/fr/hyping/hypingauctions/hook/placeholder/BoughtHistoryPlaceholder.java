package fr.hyping.hypingauctions.hook.placeholder;

import fr.hyping.hypingauctions.hook.IPlaceholderExtension;

import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Placeholder returning the total number of items the player has bought in
 * their history.
 *
 * <p>
 * Usage: %hauctions_bought_history%
 */
public class BoughtHistoryPlaceholder implements IPlaceholderExtension {
    @Override
    public @Nullable String onReplace(AuctionPlayer player, String name, String[] args) {
        return String.valueOf(player.getBoughtCount());
    }
}

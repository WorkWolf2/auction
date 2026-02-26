package fr.hyping.hypingauctions.hook.placeholder;

import fr.hyping.hypingauctions.hook.IPlaceholderExtension;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Placeholder returning the number of items the player has bought and that are waiting in their
 * "bought" slots (pending pickup).
 *
 * <p>Usage: %hauctions_bought%
 */
public class BoughtPlaceholder implements IPlaceholderExtension {
    @Override
    public @Nullable String onReplace(AuctionPlayer player, String name, String[] args) {
        return String.valueOf(player.getPurchases().size());
    }
}

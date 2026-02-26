package fr.hyping.hypingauctions.hook.placeholder;

import fr.hyping.hypingauctions.hook.IPlaceholderExtension;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.util.Messages;
import org.jetbrains.annotations.Nullable;

/**
 * Placeholder returning the total amount of money the player has earned in the
 * auction house.
 *
 * <p>
 * Usage: %hauctions_total_earned%
 */
public class TotalEarnedPlaceholder implements IPlaceholderExtension {
    @Override
    public @Nullable String onReplace(AuctionPlayer player, String name, String[] args) {
        return Messages.formatMoney(player.getTotalEarned());
    }
}

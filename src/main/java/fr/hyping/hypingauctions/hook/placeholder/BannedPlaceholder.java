package fr.hyping.hypingauctions.hook.placeholder;

import fr.hyping.hypingauctions.hook.IPlaceholderExtension;
import fr.hyping.hypingauctions.manager.BanManager;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import org.jetbrains.annotations.Nullable;

public class BannedPlaceholder implements IPlaceholderExtension {
    @Override
    public @Nullable String onReplace(AuctionPlayer player, String name, String[] args) {
        if (args.length == 0) return BanManager.isBanned(player.getPlayer()) ? "true" : "false";

        if (args.length < 2
                || args.length > 3
                || !args[0].equalsIgnoreCase("remaining")
                || !args[1].equalsIgnoreCase("time")) return null;

        if (args.length == 2) return String.valueOf(BanManager.getRemainingTime(player.getPlayer()));

        if (!args[2].equalsIgnoreCase("formatted")) return null;

        long remainingTime = BanManager.getRemainingTime(player.getPlayer());
        if (remainingTime == -1) return "Permanent";

        if (remainingTime <= 0) return "Expired";

        int days = (int) (remainingTime / 86400000);
        int hours = (int) (remainingTime % 86400000 / 3600000);
        int minutes = (int) (remainingTime % 3600000 / 60000);
        int seconds = (int) (remainingTime % 60000 / 1000);

        return days + "j " + hours + "h " + minutes + "m " + seconds + "s";
    }
}

package fr.hyping.hypingauctions.menu;

import fr.hyping.hypingauctions.sessions.PlaceholderableSession;
import org.bukkit.entity.Player;

import java.util.Map;

public interface HAuctionMenu {

    String configId();

    void refresh();

    default void preApply(Player viewer) {}

    void postSlotsRead(Player viewer);

    void postSlotsClean(Player viewer);

    void postSlotsApply(Player viewer);

    void open(Player viewer);

    PlaceholderableSession getSession();

    Map<String, String> getPlaceholderMap();
}

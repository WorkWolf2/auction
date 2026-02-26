package fr.hyping.hypingauctions.test;

import fr.hyping.hypingauctions.manager.LimitManager;
import org.bukkit.entity.Player;

/**
 * Simple test class to verify that custom limit messages work correctly. This
 * is not a unit test
 * but rather a utility class for manual testing.
 */
public class LimitMessageTest {

    /**
     * Test method to verify that custom limit messages are loaded and returned
     * correctly. This method
     * should be called after the plugin has loaded its configuration.
     *
     * <p>
     * Usage example in a command or during plugin testing: - Give a player the
     * "group-vip"
     * permission - Call testLimitMessage(player) - Verify the returned message
     * matches the VIP custom
     * message
     *
     * @param player The player to test limit messages for
     * @return A test result string describing what was found
     */
    public static String testLimitMessage(Player player) {
        if (player == null) {
            return "ERROR: Player is null";
        }

        int limit = LimitManager.getLimit(player);
        net.kyori.adventure.text.Component customMessage = LimitManager.getLimitMessage(player);

        StringBuilder result = new StringBuilder();
        result.append("=== LIMIT MESSAGE TEST RESULTS ===\n");
        result.append("Player: ").append(player.getName()).append("\n");
        result.append("Limit: ").append(limit).append("\n");

        if (customMessage != null) {
            result.append("Custom Message: ")
                    .append(fr.hyping.hypingauctions.util.Configs.COMPONENT_SERIALIZER.serialize(customMessage)).append("\n");
            result.append("Status: SUCCESS - Custom message found\n");
        } else {
            result.append("Custom Message: None (will use default)\n");
            result.append("Status: INFO - No custom message configured for this player's permissions\n");
        }

        result.append("=== END TEST RESULTS ===");

        return result.toString();
    }

    /**
     * Test method to verify limit message functionality for different permission
     * scenarios. This
     * method can be used to test various permission combinations.
     *
     * @param player                  The player to test
     * @param expectedLimit           The expected limit for this player
     * @param expectedMessageContains A substring that should be in the custom
     *                                message (or null if no
     *                                custom message expected)
     * @return true if the test passes, false otherwise
     */
    public static boolean verifyLimitMessage(
            Player player, int expectedLimit, String expectedMessageContains) {
        if (player == null) {
            return false;
        }

        int actualLimit = LimitManager.getLimit(player);
        net.kyori.adventure.text.Component customMessage = LimitManager.getLimitMessage(player);

        // Check limit
        if (actualLimit != expectedLimit) {
            return false;
        }

        // Check message
        if (expectedMessageContains == null) {
            return customMessage == null;
        } else {
            String serialized = customMessage != null
                    ? fr.hyping.hypingauctions.util.Configs.COMPONENT_SERIALIZER.serialize(customMessage)
                    : null;
            return serialized != null && serialized.contains(expectedMessageContains);
        }
    }
}

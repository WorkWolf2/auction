package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.cache.PlayerNameCache;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Test command to verify cache performance and functionality. This command is intended for
 * development and testing purposes.
 */
public class CacheTestCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        String testType = args[0].toLowerCase();

        switch (testType) {
            case "performance":
                runPerformanceTest(sender);
                break;
            case "functionality":
                runFunctionalityTest(sender);
                break;
            case "stress":
                runStressTest(sender);
                break;
            default:
                sendUsage(sender);
                break;
        }

        return true;
    }

    private void runPerformanceTest(CommandSender sender) {
        sender.sendMessage(
                Component.translatable("hyping.hypingauctions.command.cachetest.performance.start"));

        PlayerNameCache cache = PlayerNameCache.getInstance();
        List<OfflinePlayer> testPlayers = new ArrayList<>();

        // Get some offline players for testing
        OfflinePlayer[] allPlayers = Bukkit.getOfflinePlayers();
        int testCount = Math.min(10, allPlayers.length);

        for (int i = 0; i < testCount; i++) {
            testPlayers.add(allPlayers[i]);
        }

        if (testPlayers.isEmpty()) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.cachetest.no-offline-players"));
            return;
        }

        // Test without cache (direct calls)
        long startTime = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            for (OfflinePlayer player : testPlayers) {
                player.getName(); // Direct call
            }
        }
        long directTime = System.nanoTime() - startTime;

        // Test with cache (first run - cache misses)
        startTime = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            for (OfflinePlayer player : testPlayers) {
                cache.getPlayerName(player); // Cached call
            }
        }
        long cachedTime = System.nanoTime() - startTime;

        // Test with cache (second run - cache hits)
        startTime = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            for (OfflinePlayer player : testPlayers) {
                cache.getPlayerName(player); // Cached call (should be hits)
            }
        }
        long cachedHitTime = System.nanoTime() - startTime;

        sender.sendMessage(
                Component.translatable("hyping.hypingauctions.command.cachetest.performance.results"));
        sender.sendMessage(
                Component.translatable(
                        "hyping.hypingauctions.command.cachetest.performance.direct-calls",
                        Component.text(directTime / 1_000_000)));
        sender.sendMessage(
                Component.translatable(
                        "hyping.hypingauctions.command.cachetest.performance.cached-misses",
                        Component.text(cachedTime / 1_000_000)));
        sender.sendMessage(
                Component.translatable(
                        "hyping.hypingauctions.command.cachetest.performance.cached-hits",
                        Component.text(cachedHitTime / 1_000_000)));
        sender.sendMessage(
                Component.translatable(
                        "hyping.hypingauctions.command.cachetest.cache-info",
                        Component.text(cache.getCacheInfo())));
    }

    private void runFunctionalityTest(CommandSender sender) {
        sender.sendMessage(
                Component.translatable("hyping.hypingauctions.command.cachetest.functionality.start"));

        PlayerNameCache cache = PlayerNameCache.getInstance();

        // Test 1: Null handling
        String nullResult = cache.getPlayerName((OfflinePlayer) null);
        boolean nullTest = nullResult == null;
        sender.sendMessage(
                Component.translatable(
                        "hyping.hypingauctions.command.cachetest.functionality.null-handling",
                        Component.translatable(
                                nullTest
                                        ? "hyping.hypingauctions.command.cachetest.result.pass"
                                        : "hyping.hypingauctions.command.cachetest.result.fail")));

        // Test 2: Cache info
        String info = cache.getCacheInfo();
        boolean infoTest = info != null && info.contains("PlayerNameCache");
        sender.sendMessage(
                Component.translatable(
                        "hyping.hypingauctions.command.cachetest.functionality.cache-info",
                        Component.translatable(
                                infoTest
                                        ? "hyping.hypingauctions.command.cachetest.result.pass"
                                        : "hyping.hypingauctions.command.cachetest.result.fail")));

        // Test 3: Invalidation
        UUID testUUID = UUID.randomUUID();
        try {
            cache.invalidate(testUUID);
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.cachetest.functionality.invalidation",
                            Component.translatable("hyping.hypingauctions.command.cachetest.result.pass")));
        } catch (Exception e) {
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.cachetest.functionality.invalidation-failed",
                            Component.text(e.getMessage())));
        }

        // Test 4: Clear cache
        try {
            cache.clear();
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.cachetest.functionality.clear",
                            Component.translatable("hyping.hypingauctions.command.cachetest.result.pass")));
        } catch (Exception e) {
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.cachetest.functionality.clear-failed",
                            Component.text(e.getMessage())));
        }

        // Test 5: Reload
        try {
            cache.reload();
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.cachetest.functionality.reload",
                            Component.translatable("hyping.hypingauctions.command.cachetest.result.pass")));
        } catch (Exception e) {
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.cachetest.functionality.reload-failed",
                            Component.text(e.getMessage())));
        }

        sender.sendMessage(
                Component.translatable("hyping.hypingauctions.command.cachetest.functionality.complete"));
    }

    private void runStressTest(CommandSender sender) {
        sender.sendMessage(
                Component.translatable("hyping.hypingauctions.command.cachetest.stress.start"));

        PlayerNameCache cache = PlayerNameCache.getInstance();
        final int threadCount = 5;
        final int operationsPerThread = 100;

        Thread[] threads = new Thread[threadCount];
        final boolean[] results = new boolean[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] =
                    new Thread(
                            () -> {
                                try {
                                    for (int j = 0; j < operationsPerThread; j++) {
                                        // Mix of operations
                                        cache.getCacheInfo();
                                        cache.invalidate(UUID.randomUUID());

                                        // Test with online players if available
                                        for (Player player : Bukkit.getOnlinePlayers()) {
                                            cache.getPlayerName(player);
                                            break; // Just test one
                                        }
                                    }
                                    results[threadId] = true;
                                } catch (Exception e) {
                                    sender.sendMessage(
                                            Component.translatable(
                                                    "hyping.hypingauctions.command.cachetest.stress.thread-failed",
                                                    Component.text(threadId),
                                                    Component.text(e.getMessage())));
                                    results[threadId] = false;
                                }
                            });
        }

        // Start all threads
        long startTime = System.currentTimeMillis();
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for completion
        try {
            for (Thread thread : threads) {
                thread.join(10000); // 10 second timeout
            }
        } catch (InterruptedException e) {
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.cachetest.stress.interrupted",
                            Component.text(e.getMessage())));
            return;
        }

        long duration = System.currentTimeMillis() - startTime;

        // Check results
        int successCount = 0;
        for (boolean result : results) {
            if (result) successCount++;
        }

        sender.sendMessage(
                Component.translatable("hyping.hypingauctions.command.cachetest.stress.results"));
        sender.sendMessage(
                Component.translatable(
                        "hyping.hypingauctions.command.cachetest.stress.threads",
                        Component.text(threadCount),
                        Component.text(operationsPerThread)));
        sender.sendMessage(
                Component.translatable(
                        "hyping.hypingauctions.command.cachetest.stress.duration",
                        Component.text(duration)));
        sender.sendMessage(
                Component.translatable(
                        "hyping.hypingauctions.command.cachetest.stress.successful",
                        Component.text(successCount),
                        Component.text(threadCount)));
        sender.sendMessage(
                Component.translatable(
                        "hyping.hypingauctions.command.cachetest.cache-info",
                        Component.text(cache.getCacheInfo())));

        if (successCount == threadCount) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.cachetest.stress.passed"));
        } else {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.cachetest.stress.failed"));
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(
                Component.translatable("hyping.hypingauctions.command.cachetest.usage.header"));
        sender.sendMessage(
                Component.translatable("hyping.hypingauctions.command.cachetest.usage.performance"));
        sender.sendMessage(
                Component.translatable("hyping.hypingauctions.command.cachetest.usage.functionality"));
        sender.sendMessage(
                Component.translatable("hyping.hypingauctions.command.cachetest.usage.stress"));
    }
}

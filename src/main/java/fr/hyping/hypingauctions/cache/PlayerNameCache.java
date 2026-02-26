package fr.hyping.hypingauctions.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.util.Configs;
import java.time.Duration;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Thread-safe cache for player names to optimize performance and reduce expensive
 * OfflinePlayer.getName() calls that cause thread saturation.
 *
 * <p>This cache uses Caffeine for high-performance caching with TTL support and automatic eviction
 * policies.
 */
public class PlayerNameCache {

    private static PlayerNameCache instance;
    private Cache<UUID, String> cache;
    private boolean enabled;
    private boolean logStatistics;
    private boolean isShuttingDown = false;

    private PlayerNameCache() {
        reload();
    }

    /**
     * Get the singleton instance of the player name cache.
     *
     * @return The cache instance
     */
    public static PlayerNameCache getInstance() {
        if (instance == null) {
            instance = new PlayerNameCache();
        }
        return instance;
    }

    /**
     * Check if the cache instance exists without creating it.
     *
     * @return true if instance exists, false otherwise
     */
    public static boolean isInitialized() {
        return instance != null;
    }

    /** Reload the cache configuration and rebuild the cache if needed. */
    public void reload() {
        FileConfiguration config = Configs.getConfig("config");

        // Load configuration
        this.enabled = config.getBoolean("player-name-cache.enabled", true);
        int ttlSeconds = config.getInt("player-name-cache.ttl-seconds", 300);
        int maxSize = config.getInt("player-name-cache.max-size", 1000);
        this.logStatistics = config.getBoolean("player-name-cache.log-statistics", true);

        // Build new cache with updated configuration
        Caffeine<Object, Object> cacheBuilder =
                Caffeine.newBuilder().maximumSize(maxSize).expireAfterWrite(Duration.ofSeconds(ttlSeconds));

        if (logStatistics) {
            cacheBuilder.recordStats();
        }

        this.cache = cacheBuilder.build();

        HypingAuctions.getInstance()
                .getLogger()
                .info(
                        String.format(
                                "PlayerNameCache initialized: enabled=%s, ttl=%ds, maxSize=%d",
                                enabled, ttlSeconds, maxSize));
    }

    /**
     * Get a player's name from cache or fetch it if not cached. This method is thread-safe and
     * provides fallback to the original method.
     *
     * @param player The offline player
     * @return The player's name, or null if player is null
     */
    @Nullable
    public String getPlayerName(@Nullable OfflinePlayer player) {
        if (player == null) {
            return null;
        }
        return getPlayerName(player.getUniqueId(), player);
    }

    /**
     * Get a player's name from cache or fetch it if not cached.
     *
     * @param uuid The player's UUID
     * @return The player's name, or null if not found
     */
    @Nullable
    public String getPlayerName(@NotNull UUID uuid) {
        return getPlayerName(uuid, null);
    }

    /** Internal method to get player name with optional OfflinePlayer fallback. */
    @Nullable
    private String getPlayerName(@NotNull UUID uuid, @Nullable OfflinePlayer fallbackPlayer) {
        if (!enabled) {
            // Cache disabled, use fallback or get OfflinePlayer
            String name;
            if (fallbackPlayer != null) {
                name = fallbackPlayer.getName();
            } else {
                name = Bukkit.getOfflinePlayer(uuid).getName();
            }

            // If the Bukkit name is invalid or null, try to resolve via Floodgate (Bedrock)
            if (name == null || !isValidPlayerName(name)) {
                String bedrockName = getBedrockNameIfAvailable(uuid);
                if (bedrockName != null) {
                    return bedrockName; // Bedrock gamertags may contain spaces; allow as-is
                }

                HypingAuctions.getInstance()
                        .getLogger()
                        .warning(
                                String.format(
                                        "Invalid player name '%s' for UUID %s (cache disabled), using safe fallback: Unknown",
                                        name, uuid));
                return "Unknown";
            }

            return name;
        }

        try {
            return cache.get(
                    uuid,
                    key -> {
                        // Cache miss - fetch the name
                        try {
                            OfflinePlayer player =
                                    fallbackPlayer != null ? fallbackPlayer : Bukkit.getOfflinePlayer(uuid);
                            String name = player.getName();

                            // Try Bukkit name first, then Floodgate (Bedrock) as a fallback
                            if (name != null && isValidPlayerName(name)) {
                                if (logStatistics) {
                                    HypingAuctions.getInstance()
                                            .getLogger()
                                            .fine(String.format("Cache miss for UUID %s, fetched name: %s", uuid, name));
                                }
                                return name;
                            }

                            String bedrockName = getBedrockNameIfAvailable(uuid);
                            if (bedrockName != null) {
                                if (logStatistics) {
                                    HypingAuctions.getInstance()
                                            .getLogger()
                                            .fine(
                                                    String.format(
                                                            "Resolved Bedrock name via Floodgate for UUID %s: %s",
                                                            uuid, bedrockName));
                                }
                                return bedrockName; // Allow Bedrock gamertags even if they contain spaces
                            }

                            // Return a safe fallback name if neither source yields a valid name
                            String safeName = "Unknown";
                            if (logStatistics) {
                                HypingAuctions.getInstance()
                                        .getLogger()
                                        .fine(
                                                String.format(
                                                        "Invalid player name '%s' for UUID %s, using fallback: %s",
                                                        name, uuid, safeName));
                            }
                            return safeName;
                        } catch (Exception e) {
                            HypingAuctions.getInstance()
                                    .getLogger()
                                    .log(Level.WARNING, "Failed to fetch player name for UUID: " + uuid, e);
                            return "Unknown";
                        }
                    });
        } catch (Exception e) {
            HypingAuctions.getInstance()
                    .getLogger()
                    .log(
                            Level.WARNING,
                            "Cache error for UUID: " + uuid + ", falling back to direct lookup",
                            e);

            // Fallback to direct lookup on cache error
            String fallbackName;
            if (fallbackPlayer != null) {
                fallbackName = fallbackPlayer.getName();
            } else {
                fallbackName = Bukkit.getOfflinePlayer(uuid).getName();
            }

            if (fallbackName != null && isValidPlayerName(fallbackName)) {
                return fallbackName;
            }

            String bedrockName = getBedrockNameIfAvailable(uuid);
            if (bedrockName != null) {
                return bedrockName;
            }

            HypingAuctions.getInstance()
                    .getLogger()
                    .warning(
                            String.format(
                                    "Invalid fallback player name '%s' for UUID %s, using safe fallback: Unknown",
                                    fallbackName, uuid));
            return "Unknown";
        }
    }

    /**
     * Invalidate a specific player's cached name. This should be called when a player's name might
     * have changed.
     *
     * @param uuid The player's UUID
     */
    public void invalidate(@NotNull UUID uuid) {
        if (enabled && cache != null) {
            cache.invalidate(uuid);
            HypingAuctions.getInstance().getLogger().fine("Invalidated cache entry for UUID: " + uuid);
        }
    }

    /**
     * Invalidate a specific player's cached name.
     *
     * @param player The offline player
     */
    public void invalidate(@Nullable OfflinePlayer player) {
        if (player != null) {
            invalidate(player.getUniqueId());
        }
    }

    /** Clear all cached entries. */
    public void clear() {
        if (cache != null) {
            long sizeBefore = cache.estimatedSize();
            cache.invalidateAll();
            HypingAuctions.getInstance()
                    .getLogger()
                    .info("Cleared player name cache (" + sizeBefore + " entries)");
        }
    }

    /**
     * Get cache statistics for monitoring.
     *
     * @return Cache statistics, or null if statistics are not enabled
     */
    @Nullable
    public CacheStats getStats() {
        if (cache != null && logStatistics) {
            return cache.stats();
        }
        return null;
    }

    /**
     * Get cache information as a formatted string.
     *
     * @return Cache information string
     */
    @NotNull
    public String getCacheInfo() {
        if (!enabled) {
            return "PlayerNameCache: DISABLED";
        }

        if (cache == null) {
            return "PlayerNameCache: NOT INITIALIZED";
        }

        StringBuilder info = new StringBuilder();
        info.append("PlayerNameCache: enabled=").append(enabled);
        info.append(", size=").append(cache.estimatedSize());

        CacheStats stats = getStats();
        if (stats != null) {
            info.append(", hitRate=").append(String.format("%.2f%%", stats.hitRate() * 100));
            info.append(", hits=").append(stats.hitCount());
            info.append(", misses=").append(stats.missCount());
            info.append(", evictions=").append(stats.evictionCount());
        }

        return info.toString();
    }

    /**
     * Validate if a player name is valid for use in player profiles. Player names cannot contain
     * spaces or other invalid characters.
     *
     * @param name The player name to validate
     * @return true if the name is valid, false otherwise
     */
    /**
     * Validate if a player name is acceptable for display. Bedrock (Floodgate) gamertags can contain
     * spaces and extended characters, so we only enforce non-empty and a reasonable max length.
     */
    private boolean isValidPlayerName(String name) {
        if (name == null) return false;
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return false;
        // Accept up to 32 chars to support Bedrock gamertags and server customizations
        return trimmed.length() >= 1 && trimmed.length() <= 32;
    }

    /**
     * Attempt to resolve a Bedrock player's name via Floodgate if available. This method is safe to
     * call even when Floodgate isn't installed; it'll simply return null.
     */
    @Nullable
    private String getBedrockNameIfAvailable(@NotNull UUID uuid) {
        try {
            // Use Floodgate API if present to resolve Bedrock usernames
            org.geysermc.floodgate.api.FloodgateApi api =
                    org.geysermc.floodgate.api.FloodgateApi.getInstance();
            if (api != null && api.isFloodgatePlayer(uuid)) {
                org.geysermc.floodgate.api.player.FloodgatePlayer player = api.getPlayer(uuid);
                if (player != null) {
                    String username = player.getUsername();
                    if (username != null && !username.trim().isEmpty()) {
                        return username; // Allow spaces and Bedrock-specific characters
                    }
                }
            }
        } catch (Throwable t) {
            // Floodgate not present or API error; ignore and fallback to default behavior
        }
        return null;
    }

    /**
     * Log current cache statistics on demand. This method can be called manually to log cache
     * statistics.
     */
    public void logStatistics() {
        if (enabled && cache != null) {
            HypingAuctions.getInstance().getLogger().info(getCacheInfo());
        }
    }

    /** Shutdown the cache. */
    public void shutdown() {
        isShuttingDown = true;

        if (cache != null) {
            cache.invalidateAll();
        }

        HypingAuctions.getInstance().getLogger().info("PlayerNameCache shutdown complete");
    }
}

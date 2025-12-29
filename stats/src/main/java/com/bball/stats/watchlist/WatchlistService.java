package com.bball.stats.watchlist;

import com.bball.stats.events.RedisEventPublisher;
import com.bball.stats.events.WatchlistEvent;
import com.bball.stats.player.PlayerService;
import com.bball.stats.player.PlayerStatsResponse;
import com.watchlist.Watchlist;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WatchlistService {

    private final WatchlistRepository repository;
    private final PlayerService playerService;
    private final RedisEventPublisher eventPublisher;

    @Cacheable(value = "watchlist", key = "#userId")
    public List<WatchlistResponse> getUserWatchlist(Integer userId) {
        List<Watchlist> watchlists = repository.findByUserId(userId);

        return watchlists.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Caching(evict = {
            @CacheEvict(value = "watchlist", key = "#userId"),
            @CacheEvict(value = "watchlist_details", key = "#userId") // If you cache the detailed view separately
    })
    public WatchlistResponse addPlayerToWatchlist(Integer userId, String playerName) {
        // Validation
        var existing = repository.findByUserIdAndPlayerName(userId, playerName);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Player already in watchlist");
        }

        LocalDateTime now = LocalDateTime.now();
        var watchlist = Watchlist.builder()
                .userId(userId)
                .playerName(playerName)
                .addedAt(now)
                .lastStatsFetch(null) // Will be set on first stats fetch
                .build();

        Watchlist savedWatchlist = repository.save(watchlist);

        // Publish event to Redis Pub/Sub
        WatchlistEvent event = WatchlistEvent.builder()
                .eventType("PLAYER_ADDED")
                .userId(userId)
                .playerName(playerName)
                .timestamp(now)
                .watchlistId(savedWatchlist.getId())
                .build();
        eventPublisher.publishWatchlistEvent(event);
        log.info("Published PLAYER_ADDED event for user {} and player {}", userId, playerName);

        return mapToResponse(savedWatchlist, "Player added to watchlist successfully");
    }

    @Caching(evict = {
            @CacheEvict(value = "watchlist", key = "#userId"),
            @CacheEvict(value = "watchlist_details", key = "#userId")
    })
    @Transactional
    public WatchlistResponse removePlayerFromWatchlist(Integer userId, String playerName) {
        var watchlist = repository.findByUserIdAndPlayerName(userId, playerName)
                .orElseThrow(() -> new IllegalArgumentException("Player not found in watchlist"));

        Long watchlistId = watchlist.getId();
        repository.delete(watchlist);

        // Publish event to Redis Pub/Sub
        WatchlistEvent event = WatchlistEvent.builder()
                .eventType("PLAYER_REMOVED")
                .userId(userId)
                .playerName(playerName)
                .timestamp(LocalDateTime.now())
                .watchlistId(watchlistId)
                .build();
        eventPublisher.publishWatchlistEvent(event);
        log.info("Published PLAYER_REMOVED event for user {} and player {}", userId, playerName);

        return WatchlistResponse.builder()
                .playerName(playerName)
                .userId(userId)
                .message("Player removed from watchlist successfully")
                .build();
    }

    public WatchlistResponse checkPlayerInWatchlist(Integer userId, String playerName) {
        boolean inWatchlist = repository.findByUserIdAndPlayerName(userId, playerName).isPresent();

        return WatchlistResponse.builder()
                .playerName(playerName)
                .userId(userId)
                .message(inWatchlist ? "Player is in watchlist" : "Player is not in watchlist")
                .build();
    }

    private WatchlistResponse mapToResponse(Watchlist watchlist) {
        return mapToResponse(watchlist, null);
    }

    private WatchlistResponse mapToResponse(Watchlist watchlist, String message) {
        return WatchlistResponse.builder()
                .id(watchlist.getId())
                .playerName(watchlist.getPlayerName())
                .userId(watchlist.getUserId())
                .message(message)
                .build();
    }

    /**
     * Get user's watchlist with player details (stats cached in Redis)
     * This method intelligently decides when to refresh player data based on last fetch time
     */
    @Cacheable(value = "watchlist_details", key = "#userId")
    public List<WatchlistDetailedResponse> getUserWatchlistWithDetails(Integer userId) {
        List<Watchlist> watchlists = repository.findByUserId(userId);

        // 1. Launch all tasks in parallel (non-blocking creation)
        List<CompletableFuture<WatchlistDetailedResponse>> futures = watchlists.stream()
                .map(watchlist -> CompletableFuture.supplyAsync(() -> {
                    WatchlistDetailedResponse.WatchlistDetailedResponseBuilder builder =
                            WatchlistDetailedResponse.builder()
                                    .id(watchlist.getId())
                                    .playerName(watchlist.getPlayerName())
                                    .userId(watchlist.getUserId())
                                    .addedAt(watchlist.getAddedAt())
                                    .lastStatsFetch(watchlist.getLastStatsFetch());

                    try {
                        // Fetch stats (parallel network/cache call)
                        PlayerStatsResponse stats = playerService.getPlayerStats(watchlist.getPlayerName());
                        builder.stats(stats);

                        // Update metadata (parallel DB call)
                        if (shouldUpdateFetchTime(watchlist.getLastStatsFetch())) {
                            watchlist.setLastStatsFetch(LocalDateTime.now());
                            repository.save(watchlist);
                        }
                    } catch (Exception e) {
                        log.error("Failed to fetch stats for player {}: {}",
                                watchlist.getPlayerName(), e.getMessage());
                        builder.stats(null);
                    }

                    return builder.build();
                }))
                .collect(Collectors.toList());

        // 2. Aggregate results (waits for the slowest request, not the sum of all requests)
        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    /**
     * Determines if we should update the last fetch timestamp
     * Updates if: never fetched OR fetched more than 6 hours ago
     */
    private boolean shouldUpdateFetchTime(LocalDateTime lastFetch) {
        if (lastFetch == null) {
            return true;
        }
        return lastFetch.isBefore(LocalDateTime.now().minusHours(6));
    }
}
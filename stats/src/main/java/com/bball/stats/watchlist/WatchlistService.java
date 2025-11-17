package com.bball.stats.watchlist;

import com.watchlist.Watchlist;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final WatchlistRepository repository;

    /**
     * Get all player IDs in the user's watchlist
     * @param userId The user's ID
     * @return List of watchlist responses containing player IDs
     */
    public List<WatchlistResponse> getUserWatchlist(Integer userId) {
        List<Watchlist> watchlists = repository.findByUserId(userId);
        
        return watchlists.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Add a player to the user's watchlist
     * @param userId The user's ID
     * @param playerId The player's ID to add
     * @return Response with the added watchlist entry
     * @throws IllegalArgumentException if player is already in watchlist
     */
    public WatchlistResponse addPlayerToWatchlist(Integer userId, Long playerId) {
        // Business logic: Check if player is already in watchlist
        var existing = repository.findByUserIdAndPlayerId(userId, playerId);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Player already in watchlist");
        }

        // Create and save the watchlist entry
        var watchlist = Watchlist.builder()
                .userId(userId)
                .playerId(playerId)
                .build();

        Watchlist savedWatchlist = repository.save(watchlist);
        
        return WatchlistResponse.builder()
                .id(savedWatchlist.getId())
                .playerId(savedWatchlist.getPlayerId())
                .userId(savedWatchlist.getUserId())
                .message("Player added to watchlist successfully")
                .build();
    }

    /**
     * Remove a player from the user's watchlist
     * @param userId The user's ID
     * @param playerId The player's ID to remove
     * @return Response confirming removal
     * @throws IllegalArgumentException if player is not in watchlist
     */
    @Transactional
    public WatchlistResponse removePlayerFromWatchlist(Integer userId, Long playerId) {
        // Business logic: Validate player exists in watchlist
        var watchlist = repository.findByUserIdAndPlayerId(userId, playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found in watchlist"));
        
        repository.delete(watchlist);
        
        return WatchlistResponse.builder()
                .playerId(playerId)
                .userId(userId)
                .message("Player removed from watchlist successfully")
                .build();
    }

    /**
     * Check if a player is in the user's watchlist
     * @param userId The user's ID
     * @param playerId The player's ID to check
     * @return Response with check result
     */
    public WatchlistResponse checkPlayerInWatchlist(Integer userId, Long playerId) {
        boolean inWatchlist = repository.findByUserIdAndPlayerId(userId, playerId).isPresent();
        
        return WatchlistResponse.builder()
                .playerId(playerId)
                .userId(userId)
                .message(inWatchlist ? "Player is in watchlist" : "Player is not in watchlist")
                .build();
    }

    /**
     * Helper method to map Watchlist entity to WatchlistResponse DTO
     */
    private WatchlistResponse mapToResponse(Watchlist watchlist) {
        return WatchlistResponse.builder()
                .id(watchlist.getId())
                .playerId(watchlist.getPlayerId())
                .userId(watchlist.getUserId())
                .build();
    }
}


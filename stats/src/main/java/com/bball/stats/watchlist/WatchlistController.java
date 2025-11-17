package com.bball.stats.watchlist;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watchlists")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService service;

    /**
     * Get all player IDs in the user's watchlist
     * TODO: Extract userId from JWT token once security integration is complete
     * Note: Player details should be fetched separately via the player search/stats endpoints
     */
    @GetMapping
    public ResponseEntity<List<WatchlistResponse>> getUserWatchlist(
            @RequestParam(defaultValue = "1") Integer userId  // Temporary: will come from JWT
    ) {
        List<WatchlistResponse> response = service.getUserWatchlist(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Add a player to the user's watchlist
     * TODO: Extract userId from JWT token once security integration is complete
     * Note: Player should be validated via playersearch endpoint before adding
     */
    @PostMapping
    public ResponseEntity<WatchlistResponse> addPlayerToWatchlist(
            @RequestParam(defaultValue = "1") Integer userId,  // Temporary: will come from JWT
            @RequestBody WatchlistRequest request
    ) {
        try {
            WatchlistResponse response = service.addPlayerToWatchlist(userId, request.getPlayerId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            WatchlistResponse errorResponse = WatchlistResponse.builder()
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Remove a player from the user's watchlist
     * TODO: Extract userId from JWT token once security integration is complete
     */
    @DeleteMapping("/{playerId}")
    public ResponseEntity<WatchlistResponse> removePlayerFromWatchlist(
            @PathVariable Long playerId,
            @RequestParam(defaultValue = "1") Integer userId  // Temporary: will come from JWT
    ) {
        try {
            WatchlistResponse response = service.removePlayerFromWatchlist(userId, playerId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            WatchlistResponse errorResponse = WatchlistResponse.builder()
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    /**
     * Check if a player is in the user's watchlist
     * TODO: Extract userId from JWT token once security integration is complete
     */
    @GetMapping("/check/{playerId}")
    public ResponseEntity<WatchlistResponse> checkPlayerInWatchlist(
            @PathVariable Long playerId,
            @RequestParam(defaultValue = "1") Integer userId  // Temporary: will come from JWT
    ) {
        WatchlistResponse response = service.checkPlayerInWatchlist(userId, playerId);
        return ResponseEntity.ok(response);
    }
}


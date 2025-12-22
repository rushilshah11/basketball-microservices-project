package com.bball.stats.watchlist;

import com.bball.stats.auth.AuthGrpcClient;
import jakarta.validation.Valid;
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
    private final AuthGrpcClient authClient;

    @GetMapping
    public ResponseEntity<List<WatchlistResponse>> getUserWatchlist(
            @RequestHeader("Authorization") String token
    ) {
        Integer userId = authClient.verifyUser(token);
        return ResponseEntity.ok(service.getUserWatchlist(userId));
    }

    // NEW: Get watchlist with full player details (stats cached in Redis)
    @GetMapping("/details")
    public ResponseEntity<List<WatchlistDetailedResponse>> getUserWatchlistWithDetails(
            @RequestHeader("Authorization") String token
    ) {
        Integer userId = authClient.verifyUser(token);
        return ResponseEntity.ok(service.getUserWatchlistWithDetails(userId));
    }

    @PostMapping
    public ResponseEntity<WatchlistResponse> addPlayerToWatchlist(
            @RequestHeader("Authorization") String token,
            @RequestBody @Valid WatchlistRequest request
    ) {
        Integer userId = authClient.verifyUser(token);
        try {
            // Now accepts Name
            WatchlistResponse response = service.addPlayerToWatchlist(userId, request.getPlayerName());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(WatchlistResponse.builder().message(e.getMessage()).build());
        }
    }

    // CHANGED: @PathVariable is now a String name, not Long id
    @DeleteMapping("/{playerName}")
    public ResponseEntity<WatchlistResponse> removePlayerFromWatchlist(
            @PathVariable String playerName,
            @RequestHeader("Authorization") String token
    ) {
        Integer userId = authClient.verifyUser(token);
        try {
            WatchlistResponse response = service.removePlayerFromWatchlist(userId, playerName);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // CHANGED: @PathVariable is now a String name
    @GetMapping("/check/{playerName}")
    public ResponseEntity<WatchlistResponse> checkPlayerInWatchlist(
            @PathVariable String playerName,
            @RequestHeader("Authorization") String token
    ) {
        Integer userId = authClient.verifyUser(token);
        WatchlistResponse response = service.checkPlayerInWatchlist(userId, playerName);
        return ResponseEntity.ok(response);
    }
}
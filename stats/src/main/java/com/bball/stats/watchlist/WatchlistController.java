package com.bball.stats.watchlist;

import com.bball.stats.auth.AuthGrpcClient;
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
        // 1. Call Security Service via gRPC to get User ID
        Integer userId = authClient.verifyUser(token);

        // 2. Proceed with trusted ID
        List<WatchlistResponse> response = service.getUserWatchlist(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<WatchlistResponse> addPlayerToWatchlist(
            @RequestHeader("Authorization") String token,
            @RequestBody WatchlistRequest request
    ) {
        // 1. Call Security Service via gRPC to get User ID
        Integer userId = authClient.verifyUser(token);

        try {
            WatchlistResponse response = service.addPlayerToWatchlist(userId, request.getPlayerId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(WatchlistResponse.builder().message(e.getMessage()).build());
        }
    }

    @DeleteMapping("/{playerId}")
    public ResponseEntity<WatchlistResponse> removePlayerFromWatchlist(
            @PathVariable Long playerId,
            @RequestHeader("Authorization") String token
    ) {
        Integer userId = authClient.verifyUser(token);

        try {
            WatchlistResponse response = service.removePlayerFromWatchlist(userId, playerId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/check/{playerId}")
    public ResponseEntity<WatchlistResponse> checkPlayerInWatchlist(
            @PathVariable Long playerId,
            @RequestHeader("Authorization") String token
    ) {
        Integer userId = authClient.verifyUser(token);

        WatchlistResponse response = service.checkPlayerInWatchlist(userId, playerId);
        return ResponseEntity.ok(response);
    }
}
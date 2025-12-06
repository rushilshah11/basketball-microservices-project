package com.bball.stats.player;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    /**
     * Search for players by name
     */
    @GetMapping("/search")
    public ResponseEntity<List<PlayerResponse>> searchPlayers(
            @RequestParam String name
    ) {
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<PlayerResponse> players = playerService.searchPlayersByName(name);
        return ResponseEntity.ok(players);
    }

    /**
     * Get player by ID (Legacy/Placeholder)
     */
    @GetMapping("/{playerId}")
    public ResponseEntity<PlayerResponse> getPlayerById(
            @PathVariable Long playerId
    ) {
        try {
            PlayerResponse player = playerService.getPlayerById(playerId);
            return ResponseEntity.ok(player);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get Stats by Name
     * Endpoint: GET /api/players/stats?name=LeBron%20James
     */
    @GetMapping("/stats")
    public ResponseEntity<PlayerStatsResponse> viewStatsOfPlayer(
            @RequestParam String name
    ) {
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        PlayerStatsResponse stats = playerService.getPlayerStats(name);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get Trending Players (Top 10)
     * Endpoint: GET /api/players/trending
     */
    @GetMapping("/trending")
    public ResponseEntity<List<PlayerResponse>> getTrendingPlayers() {
        List<String> topStars = List.of(
                "LeBron James",
                "Stephen Curry",
                "Kevin Durant",
                "Nikola Jokic",
                "Luka Doncic",
                "Giannis Antetokounmpo",
                "Anthony Davis",
                "Anthony Edwards",
                "Cade Cunningham"
        );

        List<PlayerResponse> defaultPlayerList = new ArrayList<>();

        for (String starName : topStars) {
            List<PlayerResponse> searchResults = playerService.searchPlayersByName(starName);
            if (!searchResults.isEmpty()) {
                defaultPlayerList.add(searchResults.get(0));
            }
        }

        return ResponseEntity.ok(defaultPlayerList);
    }

    /**
     * Get Game Log by Name (Fixed)
     * Endpoint: GET /api/players/games?name=LeBron%20James
     */
    @GetMapping("/games")
    public ResponseEntity<List<GameLogResponse>> getPlayerGameLog(@RequestParam String name) {
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(playerService.getPlayerGameLog(name));
    }
}
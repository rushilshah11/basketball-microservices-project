package com.bball.stats.player;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    @GetMapping("/search")
    public ResponseEntity<List<PlayerResponse>> searchPlayers(@RequestParam String name) {
        if (name == null || name.trim().isEmpty()) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(playerService.searchPlayersByName(name));
    }

    @GetMapping("/{playerId}")
    public ResponseEntity<PlayerResponse> getPlayerById(@PathVariable Long playerId) {
        return ResponseEntity.ok(playerService.getPlayerById(playerId));
    }

    @GetMapping("/stats")
    public ResponseEntity<PlayerStatsResponse> viewStatsOfPlayer(@RequestParam String name) {
        if (name == null || name.trim().isEmpty()) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(playerService.getPlayerStats(name));
    }

    // UPDATED: Use Batch Search
    @GetMapping("/trending")
    public ResponseEntity<List<PlayerResponse>> getTrendingPlayers() {
        List<String> topStars = List.of(
                "LeBron James", "Stephen Curry", "Kevin Durant", "Nikola Jokic",
                "Luka Doncic", "Giannis Antetokounmpo", "Anthony Davis",
                "Anthony Edwards", "Jayson Tatum", "Shai Gilgeous-Alexander"
        );
        return ResponseEntity.ok(playerService.searchPlayersBatch(topStars));
    }

    // UPDATED: With Limit
    @GetMapping("/games")
    public ResponseEntity<List<GameLogResponse>> getPlayerGameLog(
            @RequestParam String name,
            @RequestParam(defaultValue = "5") int limit
    ) {
        if (name == null || name.trim().isEmpty()) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(playerService.getPlayerGameLog(name, limit));
    }
}
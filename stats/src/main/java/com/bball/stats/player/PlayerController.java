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

    /**
     * Search for players by name
     * This endpoint is called by the watchlist to find players before adding them
     * Returns only player ID and full name - keeping it simple
     * 
     * @param name Player name to search for (first or last name)
     * @return List of matching players with ID and full name
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
     * Get player by ID
     * Returns player ID and full name
     * 
     * @param playerId The player's ID
     * @return Player with ID and full name
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

}


package com.bball.stats.watchlist;

import com.bball.stats.player.PlayerStatsResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WatchlistDetailedResponse {
    
    private Long id;
    private String playerName;
    private Integer userId;
    private LocalDateTime addedAt;
    private LocalDateTime lastStatsFetch;
    
    // Player stats data (cached from Redis)
    private PlayerStatsResponse stats;
}


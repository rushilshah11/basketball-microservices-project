package com.bball.stats.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistEvent {
    
    private String eventType;  // "PLAYER_ADDED" or "PLAYER_REMOVED"
    private Integer userId;
    private String playerName;
    private LocalDateTime timestamp;
    private Long watchlistId;
}


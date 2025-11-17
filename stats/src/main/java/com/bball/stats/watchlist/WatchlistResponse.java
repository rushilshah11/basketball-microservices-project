package com.bball.stats.watchlist;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WatchlistResponse {
    
    private Long id;
    private Long playerId;
    private Integer userId;
    private String message;
}


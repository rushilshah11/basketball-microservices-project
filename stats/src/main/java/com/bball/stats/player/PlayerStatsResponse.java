package com.bball.stats.player;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlayerStatsResponse {
    private Long id;
    private String season;
    private Integer gamesPlayed;
    private Double ppg;      // Points Per Game
    private Double apg;      // Assists Per Game
    private Double rpg;      // Rebounds Per Game
    private Double topg;     // Turnovers Per Game
}
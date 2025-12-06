package com.bball.stats.player;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlayerStatsResponse {

    private String season;
    private Integer gamesPlayed;

    // Using Double for averages to handle decimal points (e.g. 25.7)
    private Double ppg;      // Points Per Game
    private Double apg;      // Assists Per Game
    private Double rpg;      // Rebounds Per Game
}
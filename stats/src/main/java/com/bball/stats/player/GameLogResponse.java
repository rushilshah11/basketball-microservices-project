package com.bball.stats.player;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameLogResponse {
    private String gameId;
    private String gameDate;
    private String matchup; // e.g., "LAL @ GSW"
    private String wl;      // "W" or "L"

    // Stats
    private int points;
    private int assists;
    private int rebounds;
    private int steals;
    private int blocks;
    private int turnovers;

    // Optional: Shooting percentages if you want them
    private double fgPct;
}
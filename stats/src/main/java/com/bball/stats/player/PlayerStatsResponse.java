package com.bball.stats.player;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlayerStatsResponse implements Serializable {

    private String season;
    private Integer gamesPlayed;

    // UPDATED: Renamed fields to match Python JSON & Frontend expectations
    private Double pointsPerGame;
    private Double assistsPerGame;
    private Double reboundsPerGame;

    // NEW: Added percentage fields
    private Double fieldGoalPercentage;
    private Double threePointPercentage;
    private Double freeThrowPercentage;
}
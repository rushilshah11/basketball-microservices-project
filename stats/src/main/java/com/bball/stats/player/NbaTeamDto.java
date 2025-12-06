package com.bball.stats.player;

import lombok.Data;

@Data
public class NbaTeamDto {
    private Long teamId;
    private String teamName;
    private String teamCity;
    private String teamAbbreviation;
}
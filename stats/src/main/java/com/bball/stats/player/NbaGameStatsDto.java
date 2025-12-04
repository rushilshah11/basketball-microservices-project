package com.bball.stats.player;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NbaGameStatsDto {

    @JsonProperty("points")
    private Integer points;

    @JsonProperty("assists")
    private Integer assists;

    @JsonProperty("turnovers")
    private Integer turnovers;

    // API-Sports sometimes returns rebounds as an object or number.
    // Usually in the players stats endpoint it is distinct fields "off_reb", "def_reb", "tot_reb"
    // or just "rebounds". We will try to map "tot_reb" or "rebounds".
    // Adjust strictly based on the exact JSON you receive if this field stays null.
    @JsonProperty("rebounds")
    private Integer rebounds;

    @JsonProperty("min")
    private String minutes; // To check if they actually played
}
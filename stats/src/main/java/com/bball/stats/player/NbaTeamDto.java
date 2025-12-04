package com.bball.stats.player;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NbaTeamDto {
    @JsonProperty("team_id")
    private Long teamId;

    @JsonProperty("team_name")
    private String teamName;

    @JsonProperty("team_city")
    private String teamCity;

    @JsonProperty("team_abbreviation")
    private String teamAbbreviation;
}
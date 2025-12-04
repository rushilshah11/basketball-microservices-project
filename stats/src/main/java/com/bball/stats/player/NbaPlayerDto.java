package com.bball.stats.player;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO matching the Basketball API response structure
 * Only includes fields we actually use (id, name)
 * All other fields from the API are ignored
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NbaPlayerDto {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("firstname")
    private String firstname;
    
    @JsonProperty("lastname")
    private String lastname;

    @JsonProperty("team")
    private NbaTeamDto team;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NbaTeamDto {
        private Long id;
        private String name;
    }
}


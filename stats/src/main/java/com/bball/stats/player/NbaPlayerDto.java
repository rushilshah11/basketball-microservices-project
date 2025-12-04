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
    private Long id;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("is_active")
    private boolean isActive;
}


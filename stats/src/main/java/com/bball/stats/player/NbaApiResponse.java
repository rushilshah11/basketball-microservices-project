package com.bball.stats.player;

import lombok.Data;

import java.util.List;

/**
 * Response wrapper from Basketball API
 * API returns data in format: { "response": [...] }
 */
@Data
public class NbaApiResponse {
    private List<NbaPlayerDto> response;
}


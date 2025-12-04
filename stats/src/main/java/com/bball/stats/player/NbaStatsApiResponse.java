package com.bball.stats.player;

import lombok.Data;
import java.util.List;

@Data
public class NbaStatsApiResponse {
    private List<NbaGameStatsDto> response;
}
package com.bball.stats.watchlist;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WatchlistRequest {

    @NotBlank(message = "Player name cannot be empty")
    private String playerName;
}


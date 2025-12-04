package com.bball.stats.player;

import com.bball.stats.config.NbaApiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerService {

    private final RestTemplate restTemplate;
    private final NbaApiConfig nbaApiConfig;

    /**
     * Search players using the Python Sidecar.
     * 1. Get Player Info
     * 2. Get Team Info
     * 3. Merge
     */
    public List<PlayerResponse> searchPlayersByName(String name) {
        log.info("Searching python sidecar for: {}", name);
        // Note: nbaApiConfig.getBaseUrl() should be "http://nba-fetcher:5000"
        String playerUrl = nbaApiConfig.getBaseUrl() + "/player/" + name;
        String teamUrl = nbaApiConfig.getBaseUrl() + "/player/" + name + "/team";

        try {
            // 1. Fetch Player Basic Info
            NbaPlayerDto player = restTemplate.getForObject(playerUrl, NbaPlayerDto.class);
            if (player == null) return Collections.emptyList();

            // 2. Fetch Team Info (Separate call)
            String teamName = "Unknown";
            try {
                NbaTeamDto team = restTemplate.getForObject(teamUrl, NbaTeamDto.class);
                if (team != null) {
                    teamName = team.getTeamCity() + " " + team.getTeamName();
                }
            } catch (Exception e) {
                log.warn("Could not fetch team for player: {}", name);
            }

            // 3. Build Response
            PlayerResponse response = PlayerResponse.builder()
                    .id(player.getId())
                    .fullName(player.getFullName())
                    .teamName(teamName)
                    .build();

            return Collections.singletonList(response);

        } catch (HttpClientErrorException.NotFound e) {
            log.info("Player not found in Python service");
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error communicating with nba-fetcher: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Fetch Stats using Python Sidecar by ID
     */
    public PlayerStatsResponse getPlayerStats(Long playerId) {
        // Python endpoint supports ID lookup: /player/{id}/stats
        String url = nbaApiConfig.getBaseUrl() + "/player/" + playerId + "/stats";

        try {
            return restTemplate.getForObject(url, PlayerStatsResponse.class);
        } catch (Exception e) {
            log.error("Error fetching stats from python for ID {}: {}", playerId, e.getMessage());
            throw new RuntimeException("Failed to fetch stats");
        }
    }

    /**
     * Get player details by ID
     * @param playerId The player's ID
     * @return Player details
    */
    public PlayerResponse getPlayerById(Long playerId) {
        // Implementation left as exercise or reused logic
        // Python currently relies on Name lookup primarily, but we can add ID lookup later.
        return PlayerResponse.builder().id(playerId).fullName("Unknown").build();
    }
}


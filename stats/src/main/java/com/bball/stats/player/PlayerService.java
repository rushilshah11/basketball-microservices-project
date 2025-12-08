package com.bball.stats.player;

import com.bball.stats.config.NbaApiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerService {

    private final RestTemplate restTemplate;
    private final NbaApiConfig nbaApiConfig;

    // --- 1. Search (Single) ---
    @Cacheable(value = "player_info", key = "#name")
    public List<PlayerResponse> searchPlayersByName(String name) {
        log.info("Searching python sidecar for: {}", name);

        // Fix: Use UriComponentsBuilder to encode "LeBron James" -> "LeBron%20James"
        String playerUrl = UriComponentsBuilder
                .fromHttpUrl(nbaApiConfig.getBaseUrl())
                .path("/player/{name}")
                .buildAndExpand(name)
                .toUriString();

        try {
            NbaPlayerDto player = restTemplate.getForObject(playerUrl, NbaPlayerDto.class);
            if (player == null) return Collections.emptyList();

            // Fetch Team
            String teamUrl = UriComponentsBuilder
                    .fromHttpUrl(nbaApiConfig.getBaseUrl())
                    .path("/player/{name}/team")
                    .buildAndExpand(name)
                    .toUriString();

            String teamName = "Unknown";
            try {
                NbaTeamDto team = restTemplate.getForObject(teamUrl, NbaTeamDto.class);
                if (team != null) {
                    teamName = team.getTeamCity() + " " + team.getTeamName();
                }
            } catch (Exception e) {
                log.warn("Could not fetch team for player: {}", name);
            }

            return Collections.singletonList(
                    PlayerResponse.builder()
                            .id(player.getId())
                            .fullName(player.getFullName())
                            .teamName(teamName)
                            .build()
            );

        } catch (HttpClientErrorException.NotFound e) {
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error communicating with nba-fetcher: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // --- 2. Batch Search (Trending) ---
    @Cacheable(value = "trending_players", unless = "#result.isEmpty()")
    public List<PlayerResponse> searchPlayersBatch(List<String> names) {
        String url = nbaApiConfig.getBaseUrl() + "/players/batch";

        try {
            // Prepare Request Body: { "names": ["LeBron", "Curry"] }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, List<String>> body = Map.of("names", names);
            HttpEntity<Map<String, List<String>>> request = new HttpEntity<>(body, headers);

            // Fetch
            ResponseEntity<List<PlayerResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<List<PlayerResponse>>() {}
            );

            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error in batch fetch: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // --- 3. Stats ---
    @Cacheable(value = "player_stats", key = "#name")
    public PlayerStatsResponse getPlayerStats(String name) {
        String url = UriComponentsBuilder
                .fromHttpUrl(nbaApiConfig.getBaseUrl())
                .path("/player/{name}/stats")
                .buildAndExpand(name)
                .toUriString();

        try {
            return restTemplate.getForObject(url, PlayerStatsResponse.class);
        } catch (Exception e) {
            log.error("Error fetching stats for {}: {}", name, e.getMessage());
            throw new RuntimeException("Failed to fetch stats");
        }
    }

    // --- 4. Game Log (With Limit) ---
    @Cacheable(value = "game_logs", key = "#name + '-' + #limit")
    public List<GameLogResponse> getPlayerGameLog(String name, int limit) {
        String url = UriComponentsBuilder
                .fromHttpUrl(nbaApiConfig.getBaseUrl())
                .path("/player/{name}/games")
                .queryParam("limit", limit)
                .buildAndExpand(name)
                .toUriString();

        try {
            ResponseEntity<List<GameLogResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<GameLogResponse>>() {}
            );
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error fetching game log for {}: {}", name, e.getMessage());
            return Collections.emptyList();
        }
    }

    public PlayerResponse getPlayerById(Long playerId) {
        return PlayerResponse.builder().id(playerId).fullName("Unknown").build();
    }
}
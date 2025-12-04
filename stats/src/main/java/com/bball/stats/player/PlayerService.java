package com.bball.stats.player;

import com.bball.stats.config.NbaApiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
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

    private static final String CURRENT_SEASON = "2025";

    /**
     * Search for players by name using NBA API
     * @param name The player name to search for (first or last name)
     * @return List of matching players
     */
    public List<PlayerResponse> searchPlayersByName(String name) {
        log.info("Searching for players: {} (Season: {})", name, CURRENT_SEASON);
        String url = nbaApiConfig.getBaseUrl() + "/players";

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("search", name)
                .queryParam("season", CURRENT_SEASON);

        HttpEntity<String> entity = new HttpEntity<>(createHeaders());

        try {
            ResponseEntity<NbaApiResponse> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    NbaApiResponse.class
            );

            if (response.getBody() != null && response.getBody().getResponse() != null) {
                return response.getBody().getResponse().stream()
                        .map(this::mapToPlayerResponse)
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        } catch (HttpClientErrorException.Forbidden e) {
            log.error("Error searching for players: 403 Forbidden on GET request for \"{}\": \"{}\"", url, e.getResponseBodyAsString());
            throw new RuntimeException("Failed to search for players: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error searching for players: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search for players: " + e.getMessage());
        }
    }

    /**
     * Get player details by ID
     * @param playerId The player's ID
     * @return Player details
     */
    public PlayerResponse getPlayerById(Long playerId) {
        log.info("Fetching player with ID: {}", playerId);
        String url = nbaApiConfig.getBaseUrl() + "/players";

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("id", playerId);

        HttpEntity<String> entity = new HttpEntity<>(createHeaders());

        try {
            ResponseEntity<NbaApiResponse> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    NbaApiResponse.class
            );

            if (response.getBody() != null && response.getBody().getResponse() != null && !response.getBody().getResponse().isEmpty()) {
                return mapToPlayerResponse(response.getBody().getResponse().get(0));
            }
            throw new IllegalArgumentException("Player with ID " + playerId + " not found.");
        } catch (HttpClientErrorException.Forbidden e) {
            log.error("Error fetching player by ID: 403 Forbidden on GET request for \"{}\": \"{}\"", url, e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch player: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error fetching player by ID: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch player: " + e.getMessage());
        }
    }

    /**
     * Map Basketball API DTO to our PlayerResponse
     * Only extracts ID and full name - keeping it simple
     */
    private PlayerResponse mapToPlayerResponse(NbaPlayerDto dto) {
        // Basketball API can have either 'name' field or 'firstname'/'lastname' fields
        String fullName;

        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            fullName = dto.getName();
        } else if (dto.getFirstname() != null || dto.getLastname() != null) {
            String first = dto.getFirstname() != null ? dto.getFirstname() : "";
            String last = dto.getLastname() != null ? dto.getLastname() : "";
            fullName = (first + " " + last).trim();
        } else {
            fullName = "Unknown Player";
        }

        String teamName = "Unknown Team";
        if (dto.getTeam() != null && dto.getTeam().getName() != null) {
            teamName = dto.getTeam().getName();
        }

        return PlayerResponse.builder()
                .id(dto.getId())
                .fullName(fullName)
                .teamName(teamName) // <--- Make sure you set this!
                .build();
    }

    /**
     * Fetch all game stats for a player in a season and calculate averages.
     */
    public PlayerStatsResponse getPlayerStats(Long playerId) {
        // You can make this dynamic or pass it as a parameter
        log.info("Fetching stats for player ID: {} (Season: {})", playerId, CURRENT_SEASON);
        // Endpoint from your docs: /games/statistics/players
        String url = nbaApiConfig.getBaseUrl() + "/games/statistics/players";

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("player", playerId)
                .queryParam("season", CURRENT_SEASON);

        HttpEntity<String> entity = new HttpEntity<>(createHeaders());

        try {
            // Use the new NbaStatsApiResponse wrapper
            ResponseEntity<NbaStatsApiResponse> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    NbaStatsApiResponse.class
            );

            List<NbaGameStatsDto> games = Collections.emptyList();
            if (response.getBody() != null && response.getBody().getResponse() != null) {
                games = response.getBody().getResponse();
            }

            return calculateAverages(playerId, CURRENT_SEASON, games);

        } catch (Exception e) {
            log.error("Error fetching stats for player {}: {}", playerId, e.getMessage());
            throw new RuntimeException("Failed to fetch stats");
        }
    }

    private PlayerStatsResponse calculateAverages(Long playerId, String season, List<NbaGameStatsDto> games) {
        if (games.isEmpty()) {
            return PlayerStatsResponse.builder()
                    .id(playerId)
                    .season(season)
                    .gamesPlayed(0)
                    .ppg(0.0).apg(0.0).rpg(0.0).topg(0.0)
                    .build();
        }

        double totalPoints = 0;
        double totalAssists = 0;
        double totalRebounds = 0;
        double totalTurnovers = 0;
        int gamesPlayed = 0;

        for (NbaGameStatsDto game : games) {
            // Only count games where they actually have stats (played > 0 min)
            // or where points is not null
            if (game.getPoints() != null) {
                totalPoints += game.getPoints();
                totalAssists += (game.getAssists() != null ? game.getAssists() : 0);
                totalRebounds += (game.getRebounds() != null ? game.getRebounds() : 0);
                totalTurnovers += (game.getTurnovers() != null ? game.getTurnovers() : 0);
                gamesPlayed++;
            }
        }

        return PlayerStatsResponse.builder()
                .id(playerId)
                .season(season)
                .gamesPlayed(gamesPlayed)
                .ppg(gamesPlayed > 0 ? Math.round((totalPoints / gamesPlayed) * 10.0) / 10.0 : 0.0)
                .apg(gamesPlayed > 0 ? Math.round((totalAssists / gamesPlayed) * 10.0) / 10.0 : 0.0)
                .rpg(gamesPlayed > 0 ? Math.round((totalRebounds / gamesPlayed) * 10.0) / 10.0 : 0.0)
                .topg(gamesPlayed > 0 ? Math.round((totalTurnovers / gamesPlayed) * 10.0) / 10.0 : 0.0)
                .build();
    }

    /**
     * Create HTTP headers with API-Sports authentication
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apisports-key", nbaApiConfig.getApiKey());
        return headers;
    }

}


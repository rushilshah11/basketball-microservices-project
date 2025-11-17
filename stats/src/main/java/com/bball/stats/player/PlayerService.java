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
     * Search for players by name using NBA API
     * @param name The player name to search for (first or last name)
     * @return List of matching players
     */
    public List<PlayerResponse> searchPlayersByName(String name) {
        log.info("Searching for players with name: {}", name);
        String url = nbaApiConfig.getBaseUrl() + "/players";

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("search", name);

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
     * Create HTTP headers with API-Sports authentication
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apisports-key", nbaApiConfig.getApiKey());
        return headers;
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
        
        return PlayerResponse.builder()
                .id(dto.getId())
                .fullName(fullName)
                .build();
    }
}


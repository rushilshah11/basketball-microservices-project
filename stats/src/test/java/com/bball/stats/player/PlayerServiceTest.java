package com.bball.stats.player;

import com.bball.stats.config.NbaApiConfig;
import com.bball.stats.exception.PlayerNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class) // Pure Unit Test (No Spring Context)
class PlayerServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private NbaApiConfig nbaApiConfig;

    @InjectMocks
    private PlayerService playerService;

    @Test
    @DisplayName("Should return a list of players when API finds matches")
    void shouldReturnPlayers_WhenFound() {
        // 1. Mock Config
        when(nbaApiConfig.getBaseUrl()).thenReturn("http://mock-api");

        // 2. Mock API Response Data
        NbaPlayerDto mockDto = new NbaPlayerDto();
        mockDto.setId(1L);
        mockDto.setFullName("LeBron James");
        mockDto.setTeamName("Lakers");
        mockDto.setPosition("F");
        List<NbaPlayerDto> mockResponse = List.of(mockDto);

        // 3. Mock RestTemplate behavior
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(mockResponse));

        // 4. Run Logic
        List<PlayerResponse> result = playerService.searchPlayersByName("LeBron");

        // 5. Assertions
        assertFalse(result.isEmpty());
        assertEquals("LeBron James", result.get(0).getFullName());
        assertEquals("Lakers", result.get(0).getTeamName());
    }

    @Test
    @DisplayName("Should return empty list when API returns null/empty")
    void shouldReturnEmpty_WhenNoMatches() {
        when(nbaApiConfig.getBaseUrl()).thenReturn("http://mock-api");

        // Mock empty response
        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.GET), any(), any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(Collections.emptyList()));

        List<PlayerResponse> result = playerService.searchPlayersByName("Unknown");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should throw PlayerNotFoundException when API fails")
    void shouldThrowException_WhenApiFails() {
        when(nbaApiConfig.getBaseUrl()).thenReturn("http://mock-api");

        // Mock an exception (e.g., 404 or connection error)
        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.GET), any(), any(ParameterizedTypeReference.class)
        )).thenThrow(new RuntimeException("API Down"));

        // Assert exception is thrown
        assertThrows(PlayerNotFoundException.class, () -> {
            playerService.searchPlayersByName("Error");
        });
    }
}
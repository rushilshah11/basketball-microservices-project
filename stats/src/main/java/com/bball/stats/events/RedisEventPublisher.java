package com.bball.stats.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisEventPublisher {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String WATCHLIST_CHANNEL = "watchlist-events";
    
    /**
     * Publish a watchlist event to Redis Pub/Sub
     */
    public void publishWatchlistEvent(WatchlistEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(WATCHLIST_CHANNEL, eventJson);
            log.info("Published event to Redis: {}", event.getEventType());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event: {}", e.getMessage());
        }
    }
    
    /**
     * Get the channel name for watchlist events
     */
    public String getWatchlistChannel() {
        return WATCHLIST_CHANNEL;
    }
}


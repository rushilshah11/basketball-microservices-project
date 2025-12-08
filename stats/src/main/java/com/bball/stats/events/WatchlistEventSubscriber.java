package com.bball.stats.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Example Redis Pub/Sub subscriber for watchlist events.
 * This can be used by other services or components to react to watchlist changes.
 * 
 * To enable this subscriber, add it to RedisMessageListenerContainer in configuration.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WatchlistEventSubscriber implements MessageListener {
    
    private final ObjectMapper objectMapper;
    
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String messageBody = new String(message.getBody());
            WatchlistEvent event = objectMapper.readValue(messageBody, WatchlistEvent.class);
            
            log.info("Received watchlist event: {} for user {} and player {}", 
                    event.getEventType(), event.getUserId(), event.getPlayerName());
            
            // Handle the event based on type
            switch (event.getEventType()) {
                case "PLAYER_ADDED":
                    handlePlayerAdded(event);
                    break;
                case "PLAYER_REMOVED":
                    handlePlayerRemoved(event);
                    break;
                default:
                    log.warn("Unknown event type: {}", event.getEventType());
            }
            
        } catch (Exception e) {
            log.error("Error processing watchlist event: {}", e.getMessage());
        }
    }
    
    private void handlePlayerAdded(WatchlistEvent event) {
        // Example: Could trigger notifications, analytics, etc.
        log.info("Player {} was added to watchlist by user {}", 
                event.getPlayerName(), event.getUserId());
        
        // TODO: Add your business logic here
        // - Send notification to user
        // - Update analytics
        // - Trigger prediction service
        // - Cache player data
    }
    
    private void handlePlayerRemoved(WatchlistEvent event) {
        // Example: Could clean up caches, update analytics, etc.
        log.info("Player {} was removed from watchlist by user {}", 
                event.getPlayerName(), event.getUserId());
        
        // TODO: Add your business logic here
        // - Update analytics
        // - Clean up cached data
        // - Stop monitoring player
    }
}


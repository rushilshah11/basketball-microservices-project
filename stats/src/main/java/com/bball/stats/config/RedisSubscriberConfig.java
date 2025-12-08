package com.bball.stats.config;

import com.bball.stats.events.WatchlistEventSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * Configuration for Redis Pub/Sub subscribers.
 * This enables listening to watchlist events published to Redis.
 */
@Configuration
public class RedisSubscriberConfig {
    
    private static final String WATCHLIST_CHANNEL = "watchlist-events";
    
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter watchlistEventListenerAdapter) {
        
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        
        // Subscribe to watchlist-events channel
        container.addMessageListener(watchlistEventListenerAdapter, new PatternTopic(WATCHLIST_CHANNEL));
        
        return container;
    }
    
    @Bean
    public MessageListenerAdapter watchlistEventListenerAdapter(WatchlistEventSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber);
    }
}


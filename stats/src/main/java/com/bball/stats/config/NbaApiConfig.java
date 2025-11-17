package com.bball.stats.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "nba.api")
@Data
public class NbaApiConfig {
    
    private String baseUrl;
    private String apiKey;
    private String apiHost;
}


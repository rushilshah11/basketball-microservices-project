package com.bball.stats.player;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlayerResponse implements Serializable {
    
    private Long id;
    private String fullName;
    private String teamName;
}


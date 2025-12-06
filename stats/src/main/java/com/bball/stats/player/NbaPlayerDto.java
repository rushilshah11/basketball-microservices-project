package com.bball.stats.player;

import lombok.Data;

@Data
public class NbaPlayerDto {
    private Long id;
    private String fullName;
    private String firstName;
    private String lastName;
    private boolean isActive;
}
package com.bball.stats.exception;

public class PlayerNotFoundException extends RuntimeException {
    public PlayerNotFoundException(String name) {
        super(name + " data could not be loaded. Please check the spelling or try again later.");
    }
}
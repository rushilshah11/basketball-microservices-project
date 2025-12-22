package com.bball.security.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

public class AuditLogger {

    private static final Path LOG_PATH = Path.of("logs", "audit-security.log");

    private static void write(String level, String message) {
        try {
            Files.createDirectories(LOG_PATH.getParent());
            String line = String.format("%s %s %s\n", Instant.now().toString(), level, message);
            Files.writeString(LOG_PATH, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // Best-effort only; avoid throwing from logger
        }
    }

    public static void info(String message) {
        write("INFO", message);
    }

    public static void warn(String message) {
        write("WARN", message);
    }

    public static void error(String message) {
        write("ERROR", message);
    }
}

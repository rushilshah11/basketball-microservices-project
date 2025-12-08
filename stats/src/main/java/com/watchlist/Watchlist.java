package com.watchlist;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "watchlist")
public class Watchlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // CHANGED: Store Name instead of ID
    @Column(name = "player_name", nullable = false)
    private String playerName;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    // Track when player stats were last fetched
    @Column(name = "last_stats_fetch")
    private LocalDateTime lastStatsFetch;

    // Track when player was added to watchlist
    @Column(name = "added_at")
    private LocalDateTime addedAt;
}
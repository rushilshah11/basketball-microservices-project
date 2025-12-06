package com.bball.stats.watchlist;

import com.watchlist.Watchlist;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final WatchlistRepository repository;

    public List<WatchlistResponse> getUserWatchlist(Integer userId) {
        List<Watchlist> watchlists = repository.findByUserId(userId);

        return watchlists.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public WatchlistResponse addPlayerToWatchlist(Integer userId, String playerName) {
        // Validation
        var existing = repository.findByUserIdAndPlayerName(userId, playerName);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Player already in watchlist");
        }

        var watchlist = Watchlist.builder()
                .userId(userId)
                .playerName(playerName)
                .build();

        Watchlist savedWatchlist = repository.save(watchlist);

        return mapToResponse(savedWatchlist, "Player added to watchlist successfully");
    }

    @Transactional
    public WatchlistResponse removePlayerFromWatchlist(Integer userId, String playerName) {
        var watchlist = repository.findByUserIdAndPlayerName(userId, playerName)
                .orElseThrow(() -> new IllegalArgumentException("Player not found in watchlist"));

        repository.delete(watchlist);

        return WatchlistResponse.builder()
                .playerName(playerName)
                .userId(userId)
                .message("Player removed from watchlist successfully")
                .build();
    }

    public WatchlistResponse checkPlayerInWatchlist(Integer userId, String playerName) {
        boolean inWatchlist = repository.findByUserIdAndPlayerName(userId, playerName).isPresent();

        return WatchlistResponse.builder()
                .playerName(playerName)
                .userId(userId)
                .message(inWatchlist ? "Player is in watchlist" : "Player is not in watchlist")
                .build();
    }

    private WatchlistResponse mapToResponse(Watchlist watchlist) {
        return mapToResponse(watchlist, null);
    }

    private WatchlistResponse mapToResponse(Watchlist watchlist, String message) {
        return WatchlistResponse.builder()
                .id(watchlist.getId())
                .playerName(watchlist.getPlayerName())
                .userId(watchlist.getUserId())
                .message(message)
                .build();
    }
}
package com.bball.stats.watchlist;

import com.watchlist.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {
    
    List<Watchlist> findByUserId(Integer userId);
    
    Optional<Watchlist> findByUserIdAndPlayerId(Integer userId, Long playerId);
    
    void deleteByUserIdAndPlayerId(Integer userId, Long playerId);
}


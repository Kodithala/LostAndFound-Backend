package com.example.lostfound.repository;

import com.example.lostfound.entity.Match;
import com.example.lostfound.entity.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByLostItemId(Long lostItemId);
    List<Match> findByFoundItemId(Long foundItemId);
    Optional<Match> findByLostItemIdAndFoundItemId(Long lostItemId, Long foundItemId);

    @Query("SELECT m FROM Match m WHERE m.lostItem.reportedBy.id = :userId OR m.foundItem.reportedBy.id = :userId")
    List<Match> findByUserId(@Param("userId") Long userId);

    List<Match> findByStatus(MatchStatus status);
}

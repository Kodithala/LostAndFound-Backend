package com.example.lostfound.controller;

import com.example.lostfound.entity.Match;
import com.example.lostfound.entity.MatchStatus;
import com.example.lostfound.entity.User;
import com.example.lostfound.exception.ResourceNotFoundException;
import com.example.lostfound.repository.MatchRepository;
import com.example.lostfound.service.MatchingService;
import com.example.lostfound.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class MatchController {

    private final MatchRepository matchRepository;
    private final MatchingService matchingService;
    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Match>> getAllMatches() {
        return ResponseEntity.ok(matchRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Match> getMatchById(@PathVariable Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + id));
        return ResponseEntity.ok(match);
    }

    @GetMapping("/my-matches")
    public ResponseEntity<List<Match>> getMyMatches() {
        User currentUser = userService.getCurrentUser();
        return ResponseEntity.ok(matchRepository.findByUserId(currentUser.getId()));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Match>> getMatchesByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(matchRepository.findByUserId(userId));
    }

    @GetMapping("/item/{itemId}")
    public ResponseEntity<List<Match>> getMatchesByItemId(@PathVariable Long itemId) {
        List<Match> lostMatches = matchRepository.findByLostItemId(itemId);
        if (lostMatches.isEmpty()) {
            return ResponseEntity.ok(matchRepository.findByFoundItemId(itemId));
        }
        return ResponseEntity.ok(lostMatches);
    }

    @PostMapping("/generate/{itemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Match>> generateMatchesForItem(@PathVariable Long itemId) {
        return ResponseEntity.ok(matchingService.generateMatchesForItem(itemId));
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<Match> confirmMatch(@PathVariable Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + id));
        match.setStatus(MatchStatus.CONFIRMED);
        return ResponseEntity.ok(matchRepository.save(match));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<Match> rejectMatch(@PathVariable Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + id));
        match.setStatus(MatchStatus.REJECTED);
        return ResponseEntity.ok(matchRepository.save(match));
    }
}

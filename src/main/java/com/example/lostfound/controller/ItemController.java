package com.example.lostfound.controller;

import com.example.lostfound.dto.ItemRequestDTO;
import com.example.lostfound.entity.Item;
import com.example.lostfound.entity.ItemStatus;
import com.example.lostfound.entity.ItemType;
import com.example.lostfound.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    public ResponseEntity<Item> createItem(@Valid @RequestBody ItemRequestDTO dto) {
        return ResponseEntity.ok(itemService.createItem(dto));
    }

    @GetMapping
    public ResponseEntity<List<Item>> getAllItems() {
        return ResponseEntity.ok(itemService.getAllItems());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable Long id) {
        return ResponseEntity.ok(itemService.getItemById(id));
    }

    @GetMapping("/lost")
    public ResponseEntity<List<Item>> getLostItems() {
        return ResponseEntity.ok(itemService.getItemsByType(ItemType.LOST));
    }

    @GetMapping("/found")
    public ResponseEntity<List<Item>> getFoundItems() {
        return ResponseEntity.ok(itemService.getItemsByType(ItemType.FOUND));
    }

    @GetMapping("/my-reports")
    public ResponseEntity<List<Item>> getMyReports() {
        return ResponseEntity.ok(itemService.getItemsByCurrentUser());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Item>> searchItems(
            @RequestParam(required = false) ItemType type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) ItemStatus status,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(itemService.searchItems(type, categoryId, status, location, startDate, endDate, keyword));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Item> updateItem(@PathVariable Long id, @Valid @RequestBody ItemRequestDTO dto) {
        return ResponseEntity.ok(itemService.updateItem(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<Map<String, String>> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        String imageUrl = itemService.saveItemImage(id, file);
        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }
}

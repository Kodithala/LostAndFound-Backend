package com.example.lostfound.service;

import com.example.lostfound.dto.ItemRequestDTO;
import com.example.lostfound.entity.*;
import com.example.lostfound.exception.BadRequestException;
import com.example.lostfound.exception.ResourceNotFoundException;
import com.example.lostfound.repository.CategoryRepository;
import com.example.lostfound.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class ItemService {

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final UserService userService;
    private final MatchingService matchingService;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Transactional
    public Item createItem(ItemRequestDTO dto) {
        User currentUser = userService.getCurrentUser();
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + dto.getCategoryId()));

        Item item = Item.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .type(dto.getType())
                .category(category)
                .location(dto.getLocation())
                .dateLostOrFound(dto.getDateLostOrFound() != null ? dto.getDateLostOrFound() : LocalDate.now())
                .color(dto.getColor())
                .brand(dto.getBrand())
                .imageUrl(dto.getImageUrl())
                .status(ItemStatus.ACTIVE)
                .reportedBy(currentUser)
                .build();

        item = itemRepository.save(item);

        // Auto-run AI matching!
        try {
            matchingService.processItemMatching(item);
        } catch (Exception e) {
            log.error("AI Matching failed for item {}: {}", item.getId(), e.getMessage());
        }

        return item;
    }

    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    public Item getItemById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));
    }

    public List<Item> getItemsByType(ItemType type) {
        return itemRepository.findByType(type);
    }

    public List<Item> getItemsByCurrentUser() {
        User currentUser = userService.getCurrentUser();
        return itemRepository.findByReportedById(currentUser.getId());
    }

    public List<Item> searchItems(ItemType type, Long categoryId, ItemStatus status, String location, LocalDate startDate, LocalDate endDate, String keyword) {
        return itemRepository.searchItems(type, categoryId, status, location, startDate, endDate, keyword);
    }

    @Transactional
    public Item updateItem(Long id, ItemRequestDTO dto) {
        Item item = getItemById(id);
        User currentUser = userService.getCurrentUser();

        if (!item.getReportedBy().getId().equals(currentUser.getId()) && currentUser.getRole() == Role.USER) {
            throw new BadRequestException("You are not authorized to update this item");
        }

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + dto.getCategoryId()));

        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        item.setType(dto.getType());
        item.setCategory(category);
        item.setLocation(dto.getLocation());
        item.setColor(dto.getColor());
        item.setBrand(dto.getBrand());
        if (dto.getDateLostOrFound() != null) {
            item.setDateLostOrFound(dto.getDateLostOrFound());
        }
        if (dto.getImageUrl() != null) {
            item.setImageUrl(dto.getImageUrl());
        }

        item = itemRepository.save(item);
        matchingService.processItemMatching(item);
        return item;
    }

    @Transactional
    public void deleteItem(Long id) {
        Item item = getItemById(id);
        User currentUser = userService.getCurrentUser();

        if (!item.getReportedBy().getId().equals(currentUser.getId()) && currentUser.getRole() == Role.USER) {
            throw new BadRequestException("You are not authorized to delete this item");
        }
        itemRepository.delete(item);
    }

    @Transactional
    public Item updateItemStatus(Long id, ItemStatus status) {
        Item item = getItemById(id);
        item.setStatus(status);
        return itemRepository.save(item);
    }

    public String saveItemImage(Long itemId, MultipartFile file) {
        Item item = getItemById(itemId);
        User currentUser = userService.getCurrentUser();

        if (!item.getReportedBy().getId().equals(currentUser.getId()) && currentUser.getRole() == Role.USER) {
            throw new BadRequestException("Not authorized to upload image for this item");
        }

        if (file.isEmpty()) {
            throw new BadRequestException("File cannot be empty");
        }

        String rawFilename = file.getOriginalFilename();
        String originalFilename = StringUtils.cleanPath(rawFilename != null ? rawFilename : "image.png");
        String extension = "";
        int i = originalFilename.lastIndexOf('.');
        if (i > 0) {
            extension = originalFilename.substring(i);
        }

        String fileName = UUID.randomUUID().toString() + extension;

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/uploads/" + fileName;
            item.setImageUrl(fileUrl);
            itemRepository.save(item);

            return fileUrl;
        } catch (IOException ex) {
            throw new BadRequestException("Could not store file " + fileName + ". Please try again!");
        }
    }
}

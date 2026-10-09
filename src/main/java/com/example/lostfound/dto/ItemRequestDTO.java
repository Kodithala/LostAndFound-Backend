package com.example.lostfound.dto;

import com.example.lostfound.entity.ItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ItemRequestDTO {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Item type (LOST or FOUND) is required")
    private ItemType type;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotBlank(message = "Location is required")
    private String location;

    private LocalDate dateLostOrFound;

    private String color;

    private String brand;

    private String imageUrl;
}

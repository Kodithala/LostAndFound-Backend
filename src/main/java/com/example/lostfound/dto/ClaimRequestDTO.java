package com.example.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClaimRequestDTO {

    @NotNull(message = "Item ID is required")
    private Long itemId;

    @NotBlank(message = "Description is required")
    private String description;

    private String proofDetails;
}

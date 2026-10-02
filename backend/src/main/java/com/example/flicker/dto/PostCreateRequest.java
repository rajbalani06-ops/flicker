package com.example.flicker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PostCreateRequest {

    @NotBlank(message = "Content cannot be empty")
    @Size(max = 280, message = "Post cannot exceed 280 characters")
    private String content;
}
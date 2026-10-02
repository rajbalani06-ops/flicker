package com.example.flicker.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(@Size(max = 160) String bio) {}

package com.example.flicker.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record UpdatePrivacyRequest(
        @JsonProperty("isPrivate")
        @JsonAlias("private")
        @NotNull Boolean isPrivate
) {}

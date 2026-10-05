package com.rahul.urlshortener.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ShortenRequest(
        @NotBlank @Size(max = 2048) @Pattern(regexp = "^https?://\\S+$", message = "must be an http or https URL") String url,
        @Pattern(regexp = "^[A-Za-z0-9_-]{3,32}$") String customAlias,
        @Min(1) @Max(3650) Integer ttlDays) {
}

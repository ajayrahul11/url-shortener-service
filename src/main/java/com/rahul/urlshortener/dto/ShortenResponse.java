package com.rahul.urlshortener.dto;

import java.time.Instant;

public record ShortenResponse(String shortCode, String shortUrl, String originalUrl, Instant createdAt,
                              Instant expiresAt) {
}

package com.rahul.urlshortener.dto;

import java.time.Instant;

public record AnalyticsResponse(String shortCode, String originalUrl, long totalClicks, Instant createdAt,
                                Instant expiresAt) {
}

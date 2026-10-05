package com.rahul.urlshortener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Security security, RateLimit rateLimit, Link link,
                            Analytics analytics, Cache cache, String baseUrl) {

    public record Security(String apiKey) {
    }

    public record RateLimit(int requestsPerMinute) {
    }

    public record Link(int defaultTtlDays) {
    }

    public record Analytics(long flushIntervalMs) {
    }

    public record Cache(long redirectTtlSeconds) {
    }
}

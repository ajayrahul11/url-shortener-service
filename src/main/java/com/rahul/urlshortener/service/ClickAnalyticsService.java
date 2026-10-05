package com.rahul.urlshortener.service;

import com.rahul.urlshortener.domain.Link;
import com.rahul.urlshortener.dto.AnalyticsResponse;
import com.rahul.urlshortener.exception.LinkExpiredException;
import com.rahul.urlshortener.exception.LinkNotFoundException;
import com.rahul.urlshortener.repository.LinkRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ClickAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(ClickAnalyticsService.class);
    static final String CLICKS_PREFIX = "shortener:clicks:";

    private final StringRedisTemplate redis;
    private final LinkRepository repository;

    public ClickAnalyticsService(StringRedisTemplate redis, LinkRepository repository) {
        this.redis = redis;
        this.repository = repository;
    }

    public void recordClick(String shortCode) {
        try {
            redis.opsForValue().increment(CLICKS_PREFIX + shortCode);
        } catch (RuntimeException e) {
            log.warn("Could not record click for {}", shortCode, e);
        }
    }

    public AnalyticsResponse getAnalytics(String shortCode) {
        Link link = repository.findByShortCode(shortCode).orElseThrow(() -> new LinkNotFoundException(shortCode));
        if (link.isExpired(Instant.now())) {
            throw new LinkExpiredException(shortCode);
        }
        long pending = 0;
        try {
            String v = redis.opsForValue().get(CLICKS_PREFIX + shortCode);
            if (v != null) {
                pending = Long.parseLong(v);
            }
        } catch (RuntimeException e) {
            log.warn("Could not read pending clicks for {}", shortCode, e);
        }
        return new AnalyticsResponse(link.getShortCode(), link.getOriginalUrl(), link.getClickCount() + pending,
                link.getCreatedAt(), link.getExpiresAt());
    }
}

package com.rahul.urlshortener.service;

import com.rahul.urlshortener.config.AppProperties;
import com.rahul.urlshortener.domain.Link;
import com.rahul.urlshortener.dto.ShortenRequest;
import com.rahul.urlshortener.dto.ShortenResponse;
import com.rahul.urlshortener.exception.AliasConflictException;
import com.rahul.urlshortener.exception.LinkExpiredException;
import com.rahul.urlshortener.exception.LinkNotFoundException;
import com.rahul.urlshortener.repository.LinkRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class LinkService {

    private static final Logger log = LoggerFactory.getLogger(LinkService.class);
    static final String URL_KEY_PREFIX = "shortener:url:";

    private final LinkRepository repository;
    private final StringRedisTemplate redis;
    private final ShortCodeGenerator generator;
    private final AppProperties props;

    public LinkService(LinkRepository repository, StringRedisTemplate redis, ShortCodeGenerator generator,
                       AppProperties props) {
        this.repository = repository;
        this.redis = redis;
        this.generator = generator;
        this.props = props;
    }

    public ShortenResponse create(ShortenRequest req) {
        String alias = req.customAlias();
        boolean custom = alias != null && !alias.isBlank();
        if (custom && repository.existsByShortCode(alias)) {
            throw new AliasConflictException(alias);
        }
        int days = req.ttlDays() != null ? req.ttlDays() : props.link().defaultTtlDays();
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Instant expires = now.plus(days, ChronoUnit.DAYS);

        long id = generator.nextId();
        String code = custom ? alias : generator.encode(id);
        while (!custom && repository.existsByShortCode(code)) {
            id = generator.nextId();
            code = generator.encode(id);
        }
        Link link = new Link(id, code, req.url(), now, expires);
        try {
            repository.save(link);
        } catch (DataIntegrityViolationException e) {
            throw new AliasConflictException(code);
        }
        evict(code);
        String base = props.baseUrl() == null ? "" : props.baseUrl().replaceAll("/+$", "");
        return new ShortenResponse(code, base + "/" + code, req.url(), now, expires);
    }

    public String resolve(String shortCode) {
        String key = URL_KEY_PREFIX + shortCode;
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                return cached;
            }
        } catch (RuntimeException e) {
            log.warn("Redis read failed for {}, falling back to Postgres", key, e);
        }
        Link link = repository.findByShortCode(shortCode).orElseThrow(() -> new LinkNotFoundException(shortCode));
        Instant now = Instant.now();
        if (link.isExpired(now)) {
            throw new LinkExpiredException(shortCode);
        }
        long ttl = props.cache().redirectTtlSeconds();
        if (link.getExpiresAt() != null) {
            ttl = Math.min(ttl, Duration.between(now, link.getExpiresAt()).getSeconds());
        }
        if (ttl > 0) {
            try {
                redis.opsForValue().set(key, link.getOriginalUrl(), Duration.ofSeconds(ttl));
            } catch (RuntimeException e) {
                log.warn("Redis cache write failed for {}", key, e);
            }
        }
        return link.getOriginalUrl();
    }

    public void evict(String shortCode) {
        try {
            redis.delete(URL_KEY_PREFIX + shortCode);
        } catch (RuntimeException e) {
            log.warn("Redis cache eviction failed for {}", shortCode, e);
        }
    }
}

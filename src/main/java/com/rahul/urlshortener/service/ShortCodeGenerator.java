package com.rahul.urlshortener.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ShortCodeGenerator {

    private static final Logger log = LoggerFactory.getLogger(ShortCodeGenerator.class);
    static final String COUNTER_KEY = "shortener:id-counter";
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    /** 62^3 - 1: guarantees generated codes have at least 4 characters. */
    private static final long OFFSET = 238327L;

    private final StringRedisTemplate redis;

    public ShortCodeGenerator(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public long nextId() {
        try {
            Long v = redis.opsForValue().increment(COUNTER_KEY);
            if (v == null) {
                throw new IllegalStateException("null counter");
            }
            return v + OFFSET;
        } catch (RuntimeException e) {
            log.error("Unable to allocate id from Redis", e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ID generator unavailable");
        }
    }

    public String encode(long id) {
        if (id < 0) {
            throw new IllegalArgumentException("id must be non-negative");
        }
        if (id == 0) {
            return "0";
        }
        StringBuilder sb = new StringBuilder();
        long n = id;
        while (n > 0) {
            sb.append(ALPHABET.charAt((int) (n % 62)));
            n /= 62;
        }
        return sb.reverse().toString();
    }
}

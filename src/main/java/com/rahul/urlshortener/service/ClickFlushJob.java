package com.rahul.urlshortener.service;

import com.rahul.urlshortener.repository.LinkRepository;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class ClickFlushJob {

    private static final Logger log = LoggerFactory.getLogger(ClickFlushJob.class);

    private final StringRedisTemplate redis;
    private final LinkRepository repository;
    private final TransactionTemplate tx;

    public ClickFlushJob(StringRedisTemplate redis, LinkRepository repository, PlatformTransactionManager tm) {
        this.redis = redis;
        this.repository = repository;
        this.tx = new TransactionTemplate(tm);
    }

    @Scheduled(fixedDelayString = "${app.analytics.flush-interval-ms}")
    public void flush() {
        List<String> keys = new ArrayList<>();
        try (Cursor<String> cursor = redis.scan(
                ScanOptions.scanOptions().match(ClickAnalyticsService.CLICKS_PREFIX + "*").count(200).build())) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        } catch (RuntimeException e) {
            log.warn("Click flush scan failed", e);
            return;
        }
        for (String key : keys) {
            flushKey(key);
        }
    }

    private void flushKey(String key) {
        long delta = 0;
        try {
            String v = redis.opsForValue().getAndDelete(key);
            if (v == null) {
                return;
            }
            delta = Long.parseLong(v);
            if (delta <= 0) {
                return;
            }
            String code = key.substring(ClickAnalyticsService.CLICKS_PREFIX.length());
            final long d = delta;
            tx.executeWithoutResult(s -> repository.incrementClickCount(code, d));
        } catch (RuntimeException e) {
            log.warn("Click flush failed for {}", key, e);
            if (delta > 0) {
                try {
                    redis.opsForValue().increment(key, delta);
                } catch (RuntimeException ex) {
                    log.error("Could not restore click delta for {}", key, ex);
                }
            }
        }
    }
}

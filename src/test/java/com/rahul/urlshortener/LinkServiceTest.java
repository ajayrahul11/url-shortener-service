package com.rahul.urlshortener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rahul.urlshortener.config.AppProperties;
import com.rahul.urlshortener.domain.Link;
import com.rahul.urlshortener.exception.LinkExpiredException;
import com.rahul.urlshortener.exception.LinkNotFoundException;
import com.rahul.urlshortener.repository.LinkRepository;
import com.rahul.urlshortener.service.LinkService;
import com.rahul.urlshortener.service.ShortCodeGenerator;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class LinkServiceTest {

    private final ShortCodeGenerator generator = new ShortCodeGenerator(null);

    @Test
    void base62Encoding() {
        assertEquals("0", generator.encode(0));
        assertEquals("9", generator.encode(9));
        assertEquals("A", generator.encode(10));
        assertEquals("a", generator.encode(36));
        assertEquals("10", generator.encode(62));
        assertEquals("1000", generator.encode(238328L));
    }

    @Test
    void expiryLogic() {
        Instant now = Instant.now();
        Link expired = new Link(1L, "abc", "https://e.com", now.minus(2, ChronoUnit.DAYS),
                now.minus(1, ChronoUnit.DAYS));
        Link live = new Link(2L, "def", "https://e.com", now, now.plus(1, ChronoUnit.DAYS));
        Link never = new Link(3L, "ghi", "https://e.com", now, null);
        assertTrue(expired.isExpired(now));
        assertFalse(live.isExpired(now));
        assertFalse(never.isExpired(now));
    }

    @SuppressWarnings("unchecked")
    @Test
    void resolveBehaviour() {
        LinkRepository repo = mock(LinkRepository.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get("shortener:url:cached")).thenReturn("https://cached.example.com");
        Instant now = Instant.now();
        when(repo.findByShortCode("live1")).thenReturn(
                Optional.of(new Link(1L, "live1", "https://live.example.com", now, now.plus(1, ChronoUnit.DAYS))));
        when(repo.findByShortCode("old1")).thenReturn(
                Optional.of(new Link(2L, "old1", "https://old.example.com", now.minus(2, ChronoUnit.DAYS),
                        now.minus(1, ChronoUnit.DAYS))));
        when(repo.findByShortCode("none")).thenReturn(Optional.empty());

        AppProperties props = new AppProperties(new AppProperties.Security("k"), new AppProperties.RateLimit(60),
                new AppProperties.Link(30), new AppProperties.Analytics(5000), new AppProperties.Cache(3600),
                "http://localhost:8080");
        LinkService service = new LinkService(repo, redis, generator, props);

        assertEquals("https://cached.example.com", service.resolve("cached"));
        assertEquals("https://live.example.com", service.resolve("live1"));
        assertThrows(LinkExpiredException.class, () -> service.resolve("old1"));
        assertThrows(LinkNotFoundException.class, () -> service.resolve("none"));
    }
}

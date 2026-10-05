package com.rahul.urlshortener.filter;

import com.rahul.urlshortener.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final StringRedisTemplate redis;
    private final AppProperties props;

    public RateLimitFilter(StringRedisTemplate redis, AppProperties props) {
        this.redis = redis;
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        long epoch = Instant.now().getEpochSecond();
        long window = epoch / 60;
        long retryAfter = 60 - (epoch % 60);
        boolean limited = false;
        try {
            String key = "shortener:ratelimit:" + req.getRemoteAddr() + ":" + window;
            Long count = redis.opsForValue().increment(key);
            if (count != null) {
                if (count == 1L) {
                    redis.expire(key, Duration.ofSeconds(61));
                }
                limited = count > props.rateLimit().requestsPerMinute();
            }
        } catch (RuntimeException e) {
            log.warn("Rate limiter unavailable, failing open", e);
        }
        if (limited) {
            res.setStatus(429);
            res.setHeader("Retry-After", String.valueOf(retryAfter));
            res.setContentType("application/problem+json");
            res.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Too Many Requests\",\"status\":429,"
                    + "\"detail\":\"Rate limit exceeded\"}");
            return;
        }
        chain.doFilter(req, res);
    }
}

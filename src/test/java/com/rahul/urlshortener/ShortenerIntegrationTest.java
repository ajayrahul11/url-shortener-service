package com.rahul.urlshortener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.security.api-key=test-key",
        "app.rate-limit.requests-per-minute=1000",
        "app.analytics.flush-interval-ms=500"})
class ShortenerIntegrationTest {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("spring.data.redis.host", REDIS::getHost);
        r.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Value("${local.server.port}")
    int port;

    @Autowired
    JdbcTemplate jdbc;

    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String body, String key) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/shorten"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (key != null) {
            b.header("X-API-Key", key);
        }
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void shortenRedirectAnalytics() throws Exception {
        HttpResponse<String> created = post("{\"url\":\"https://example.com/page\"}", "test-key");
        assertEquals(201, created.statusCode());
        Matcher m = Pattern.compile("\"shortCode\"\\s*:\\s*\"([^\"]+)\"").matcher(created.body());
        assertTrue(m.find());
        String code = m.group(1);

        HttpResponse<String> redirect = get("/" + code);
        assertEquals(302, redirect.statusCode());
        assertEquals("https://example.com/page", redirect.headers().firstValue("Location").orElse(null));

        HttpResponse<String> analytics = get("/api/analytics/" + code);
        assertEquals(200, analytics.statusCode());
        Matcher c = Pattern.compile("\"totalClicks\"\\s*:\\s*(\\d+)").matcher(analytics.body());
        assertTrue(c.find());
        assertTrue(Long.parseLong(c.group(1)) >= 1);
    }

    @Test
    void unauthorizedWithoutKey() throws Exception {
        assertEquals(401, post("{\"url\":\"https://example.com\"}", null).statusCode());
        assertEquals(401, post("{\"url\":\"https://example.com\"}", "wrong").statusCode());
    }

    @Test
    void invalidUrlIs400() throws Exception {
        assertEquals(400, post("{\"url\":\"not-a-url\"}", "test-key").statusCode());
    }

    @Test
    void unknownCodeIs404() throws Exception {
        assertEquals(404, get("/nope123").statusCode());
    }

    @Test
    void expiredLinkIs410() throws Exception {
        jdbc.update("insert into links (id, short_code, original_url, created_at, expires_at) values "
                + "(1, 'expired1', 'https://example.com', now() - interval '2 day', now() - interval '1 day')");
        assertEquals(410, get("/expired1").statusCode());
    }
}

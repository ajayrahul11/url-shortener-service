package com.rahul.urlshortener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.security.api-key=test-key",
        "app.rate-limit.requests-per-minute=5"})
class RateLimitIntegrationTest {

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

    @Test
    void returns429AfterLimit() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        int tooMany = 0;
        String retryAfter = null;
        for (int i = 0; i < 12; i++) {
            HttpResponse<String> r = client.send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/nope123")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() == 429) {
                tooMany++;
                retryAfter = r.headers().firstValue("Retry-After").orElse(null);
            }
        }
        assertTrue(tooMany > 0, "expected at least one 429");
        assertTrue(retryAfter != null && Integer.parseInt(retryAfter) > 0);
        assertEquals(true, tooMany >= 5);
    }
}

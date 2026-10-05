package com.rahul.urlshortener;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.yaml.snakeyaml.Yaml;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.security.api-key=test-key",
        "app.rate-limit.requests-per-minute=1000"})
class ContractTest {

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

    private static final List<String> METHODS = List.of("get", "post", "put", "patch", "delete", "head");

    @SuppressWarnings("unchecked")
    @Test
    void everyDeclaredOperationIsServed() throws Exception {
        Map<String, Object> root;
        try (InputStream in = Files.newInputStream(Path.of("docs/openapi.yaml"))) {
            root = new Yaml().load(in);
        }
        Map<String, Object> paths = (Map<String, Object>) root.get("paths");
        assertTrue(paths != null && !paths.isEmpty(), "no paths in docs/openapi.yaml");

        HttpClient client = HttpClient.newHttpClient();
        String base = "http://localhost:" + port;

        HttpResponse<String> created = client.send(HttpRequest.newBuilder(URI.create(base + "/api/shorten"))
                .header("Content-Type", "application/json").header("X-API-Key", "test-key")
                .POST(HttpRequest.BodyPublishers.ofString("{\"url\":\"https://example.com/contract\"}")).build(),
                HttpResponse.BodyHandlers.ofString());
        Matcher m = Pattern.compile("\"shortCode\"\\s*:\\s*\"([^\"]+)\"").matcher(created.body());
        if (!m.find()) {
            fail("could not create a link for contract test: " + created.statusCode());
        }
        String code = m.group(1);

        for (Map.Entry<String, Object> p : paths.entrySet()) {
            Map<String, Object> ops = (Map<String, Object>) p.getValue();
            String path = p.getKey().replaceAll("\\{[^}]+}", code);
            for (String method : METHODS) {
                if (!ops.containsKey(method)) {
                    continue;
                }
                HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path))
                        .header("X-API-Key", "test-key");
                switch (method) {
                    case "get" -> b.GET();
                    case "delete" -> b.DELETE();
                    case "head" -> b.method("HEAD", HttpRequest.BodyPublishers.noBody());
                    default -> b.header("Content-Type", "application/json").method(method.toUpperCase(),
                            HttpRequest.BodyPublishers.ofString("{\"url\":\"https://example.com/c2\"}"));
                }
                int status = client.send(b.build(), HttpResponse.BodyHandlers.ofString()).statusCode();
                if (status == 404 || status == 405) {
                    fail(method.toUpperCase() + " " + p.getKey() + " not served by the application (status "
                            + status + ")");
                }
            }
        }
    }
}

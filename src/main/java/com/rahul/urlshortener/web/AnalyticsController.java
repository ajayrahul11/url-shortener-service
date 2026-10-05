package com.rahul.urlshortener.web;

import com.rahul.urlshortener.dto.AnalyticsResponse;
import com.rahul.urlshortener.service.ClickAnalyticsService;
import java.util.regex.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalyticsController {

    private static final Pattern CODE = Pattern.compile("^[0-9A-Za-z_-]{3,32}$");

    private final ClickAnalyticsService service;

    public AnalyticsController(ClickAnalyticsService service) {
        this.service = service;
    }

    @GetMapping("/api/analytics/{shortCode}")
    public AnalyticsResponse analytics(@PathVariable String shortCode) {
        if (!CODE.matcher(shortCode).matches()) {
            throw new IllegalArgumentException("Invalid short code format");
        }
        return service.getAnalytics(shortCode);
    }
}

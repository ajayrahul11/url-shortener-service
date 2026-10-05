package com.rahul.urlshortener.web;

import com.rahul.urlshortener.service.ClickAnalyticsService;
import com.rahul.urlshortener.service.LinkService;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {

    private static final Pattern CODE = Pattern.compile("^[0-9A-Za-z_-]{3,32}$");

    private final LinkService linkService;
    private final ClickAnalyticsService analytics;

    public RedirectController(LinkService linkService, ClickAnalyticsService analytics) {
        this.linkService = linkService;
        this.analytics = analytics;
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        if (!CODE.matcher(shortCode).matches()) {
            throw new IllegalArgumentException("Invalid short code format");
        }
        String url = linkService.resolve(shortCode);
        analytics.recordClick(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, url).build();
    }
}

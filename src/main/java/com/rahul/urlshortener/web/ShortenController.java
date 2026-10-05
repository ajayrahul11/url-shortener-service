package com.rahul.urlshortener.web;

import com.rahul.urlshortener.dto.ShortenRequest;
import com.rahul.urlshortener.dto.ShortenResponse;
import com.rahul.urlshortener.service.LinkService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShortenController {

    private final LinkService linkService;

    public ShortenController(LinkService linkService) {
        this.linkService = linkService;
    }

    @PostMapping("/api/shorten")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(linkService.create(req));
    }
}

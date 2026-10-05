package com.rahul.urlshortener.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiKeyFilter extends OncePerRequestFilter {

    static final String HEADER = "X-API-Key";

    private final byte[] expected;

    public ApiKeyFilter(String apiKey) {
        this.expected = apiKey == null || apiKey.isBlank() ? null : apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String provided = req.getHeader(HEADER);
        if (expected != null && provided != null
                && MessageDigest.isEqual(expected, provided.getBytes(StandardCharsets.UTF_8))) {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "api-client", null, List.of(new SimpleGrantedAuthority("ROLE_API"))));
        }
        chain.doFilter(req, res);
    }
}

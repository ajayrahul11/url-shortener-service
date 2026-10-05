package com.rahul.urlshortener.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(LinkNotFoundException.class)
    public ProblemDetail notFound(LinkNotFoundException e) {
        return pd(HttpStatus.NOT_FOUND, "Link not found", e.getMessage());
    }

    @ExceptionHandler(LinkExpiredException.class)
    public ProblemDetail expired(LinkExpiredException e) {
        return pd(HttpStatus.GONE, "Link expired", e.getMessage());
    }

    @ExceptionHandler(AliasConflictException.class)
    public ProblemDetail conflict(AliasConflictException e) {
        return pd(HttpStatus.CONFLICT, "Alias already taken", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail invalid(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b).orElse("Validation failed");
        return pd(HttpStatus.BAD_REQUEST, "Validation error", detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail unreadable(HttpMessageNotReadableException e) {
        return pd(HttpStatus.BAD_REQUEST, "Malformed request", "Request body is missing or malformed");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail illegalArgument(IllegalArgumentException e) {
        return pd(HttpStatus.BAD_REQUEST, "Invalid request", e.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail status(ResponseStatusException e) {
        HttpStatus s = HttpStatus.resolve(e.getStatusCode().value());
        log.warn("Request failed with status {}", e.getStatusCode().value());
        return pd(s == null ? HttpStatus.INTERNAL_SERVER_ERROR : s, s == null ? "Error" : s.getReasonPhrase(),
                e.getReason());
    }

    private static ProblemDetail pd(HttpStatus status, String title, String detail) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
        p.setTitle(title);
        return p;
    }
}

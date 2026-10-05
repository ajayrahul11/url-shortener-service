package com.rahul.urlshortener.exception;

public class LinkExpiredException extends RuntimeException {
    public LinkExpiredException(String code) {
        super("Link '" + code + "' has expired");
    }
}

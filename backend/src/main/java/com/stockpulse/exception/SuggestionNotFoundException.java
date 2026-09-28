package com.stockpulse.exception;

public class SuggestionNotFoundException extends RuntimeException {
    public SuggestionNotFoundException(String id) { super("Suggestion not found: " + id); }
}

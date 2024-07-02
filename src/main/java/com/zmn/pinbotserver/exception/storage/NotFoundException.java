package com.zmn.pinbotserver.exception.storage;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}

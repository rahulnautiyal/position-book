package com.example.positionbook.exception;

public class PositionOverflowException extends RuntimeException {
    public PositionOverflowException(String message, Throwable cause) {
        super(message, cause);
    }
}

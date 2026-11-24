package com.darna.service;

/**
 * Domain exception thrown when a Darna business rule is violated.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}

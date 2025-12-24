package com.ga.bankapp.exception;

public class OverdraftLimitExceededException extends Exception {
    public OverdraftLimitExceededException(String message) {
        super(message);
    }
}


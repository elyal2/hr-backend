package com.humanrsc.exceptions;

public class JobPositionValidationException extends RuntimeException {
    
    private final String field;
    private final String errorCode;
    
    public JobPositionValidationException(String message) {
        super(message);
        this.field = null;
        this.errorCode = "VALIDATION_ERROR";
    }
    
    public JobPositionValidationException(String field, String message) {
        super(message);
        this.field = field;
        this.errorCode = "VALIDATION_ERROR";
    }
    
    public JobPositionValidationException(String field, String errorCode, String message) {
        super(message);
        this.field = field;
        this.errorCode = errorCode;
    }
    
    public String getField() {
        return field;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}

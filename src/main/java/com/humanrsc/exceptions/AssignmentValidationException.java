package com.humanrsc.exceptions;

public class AssignmentValidationException extends RuntimeException {
    
    private final String field;
    private final String errorCode;
    
    public AssignmentValidationException(String message) {
        super(message);
        this.field = null;
        this.errorCode = "VALIDATION_ERROR";
    }
    
    public AssignmentValidationException(String field, String message) {
        super(message);
        this.field = field;
        this.errorCode = "VALIDATION_ERROR";
    }
    
    public AssignmentValidationException(String field, String errorCode, String message) {
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

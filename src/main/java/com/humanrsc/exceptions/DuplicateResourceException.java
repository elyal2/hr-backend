package com.humanrsc.exceptions;

public class DuplicateResourceException extends RuntimeException {
    
    private final String field;
    private final String value;
    private final String resourceType;
    
    public DuplicateResourceException(String field, String value, String resourceType) {
        super(String.format("%s with %s '%s' already exists", resourceType, field, value));
        this.field = field;
        this.value = value;
        this.resourceType = resourceType;
    }
    
    public DuplicateResourceException(String message) {
        super(message);
        this.field = null;
        this.value = null;
        this.resourceType = null;
    }
    
    public String getField() {
        return field;
    }
    
    public String getValue() {
        return value;
    }
    
    public String getResourceType() {
        return resourceType;
    }
}

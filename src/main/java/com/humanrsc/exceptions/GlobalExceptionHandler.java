package com.humanrsc.exceptions;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {
    
    private static final Logger LOG = Logger.getLogger(GlobalExceptionHandler.class);
    
    @ConfigProperty(name = "app.logging.404.level", defaultValue = "DEBUG")
    String logLevel404;
    
    @ConfigProperty(name = "app.logging.validation.level", defaultValue = "INFO")
    String logLevelValidation;
    
    @ConfigProperty(name = "app.logging.unexpected.level", defaultValue = "ERROR")
    String logLevelUnexpected;
    
    private void logException(String level, String message, Exception exception) {
        switch (level.toUpperCase()) {
            case "DEBUG":
                LOG.debugf(message, exception.getMessage());
                break;
            case "INFO":
                LOG.infof(message, exception.getMessage());
                break;
            case "WARN":
                LOG.warnf(message, exception.getMessage());
                break;
            case "ERROR":
            default:
                LOG.error(message, exception);
                break;
        }
    }
    
    @Override
    public Response toResponse(Exception exception) {
        // Para NotFoundException, usar configuración específica
        if (exception instanceof jakarta.ws.rs.NotFoundException) {
            logException(logLevel404, "Endpoint not found - Request details: %s", exception);
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(
                        "Endpoint not found", 
                        "The requested endpoint does not exist. Check the URL path and HTTP method.", 
                        "ENDPOINT_NOT_FOUND",
                        "url",
                        exception.getMessage()
                    ))
                    .build();
        }
        
        // Para excepciones de validación conocidas, usar configuración específica
        if (exception instanceof DuplicateResourceException ||
            exception instanceof ResourceNotFoundException ||
            exception instanceof EmployeeValidationException ||
            exception instanceof JobPositionValidationException ||
            exception instanceof OrganizationalUnitValidationException ||
            exception instanceof AssignmentValidationException ||
            exception instanceof IllegalArgumentException ||
            exception instanceof jakarta.ws.rs.NotAllowedException ||
            exception instanceof jakarta.ws.rs.BadRequestException ||
            exception instanceof jakarta.validation.ConstraintViolationException) {
            
            logException(logLevelValidation, "Handled exception: %s - %s", exception);
        } else {
            // Solo usar ERROR para excepciones realmente inesperadas
            logException(logLevelUnexpected, "Unhandled exception occurred", exception);
        }
        
        if (exception instanceof DuplicateResourceException) {
            DuplicateResourceException e = (DuplicateResourceException) exception;
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse("Duplicate resource", e.getMessage(), "DUPLICATE_RESOURCE", e.getField(), e.getValue()))
                    .build();
        }
        
        if (exception instanceof ResourceNotFoundException) {
            ResourceNotFoundException e = (ResourceNotFoundException) exception;
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Resource not found", e.getMessage(), "RESOURCE_NOT_FOUND"))
                    .build();
        }
        
        if (exception instanceof EmployeeValidationException) {
            EmployeeValidationException e = (EmployeeValidationException) exception;
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation error", e.getMessage(), "VALIDATION_ERROR", e.getField(), null))
                    .build();
        }
        
        if (exception instanceof JobPositionValidationException) {
            JobPositionValidationException e = (JobPositionValidationException) exception;
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation error", e.getMessage(), "VALIDATION_ERROR", e.getField(), null))
                    .build();
        }
        
        if (exception instanceof OrganizationalUnitValidationException) {
            OrganizationalUnitValidationException e = (OrganizationalUnitValidationException) exception;
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation error", e.getMessage(), "VALIDATION_ERROR", e.getField(), null))
                    .build();
        }
        
        if (exception instanceof AssignmentValidationException) {
            AssignmentValidationException e = (AssignmentValidationException) exception;
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation error", e.getMessage(), "VALIDATION_ERROR", e.getField(), null))
                    .build();
        }
        
        if (exception instanceof IllegalArgumentException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation error", exception.getMessage(), "VALIDATION_ERROR"))
                    .build();
        }
        
        if (exception instanceof jakarta.ws.rs.NotAllowedException) {
            jakarta.ws.rs.NotAllowedException e = (jakarta.ws.rs.NotAllowedException) exception;
            String allowedMethods = e.getResponse().getAllowedMethods() != null 
                ? String.join(", ", e.getResponse().getAllowedMethods()) 
                : "POST";
            
            return Response.status(Response.Status.METHOD_NOT_ALLOWED)
                    .entity(new ErrorResponse(
                        "Method not allowed", 
                        String.format("This endpoint requires HTTP %s method. Please check your frontend proxy configuration or use the correct HTTP method.", allowedMethods), 
                        "METHOD_NOT_ALLOWED",
                        "httpMethod",
                        allowedMethods
                    ))
                    .build();
        }
        
        if (exception instanceof jakarta.ws.rs.BadRequestException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(
                        "Bad request", 
                        exception.getMessage() != null ? exception.getMessage() : "Invalid request format", 
                        "BAD_REQUEST",
                        "request",
                        null
                    ))
                    .build();
        }
        
        if (exception instanceof jakarta.validation.ConstraintViolationException) {
            jakarta.validation.ConstraintViolationException e = (jakarta.validation.ConstraintViolationException) exception;
            String violations = e.getConstraintViolations().stream()
                    .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                    .collect(java.util.stream.Collectors.joining(", "));
            
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(
                        "Validation error", 
                        "Request validation failed: " + violations, 
                        "VALIDATION_ERROR",
                        "request",
                        violations
                    ))
                    .build();
        }
        
        // Default case - internal server error
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Internal server error", "An unexpected error occurred", "INTERNAL_ERROR"))
                .build();
    }
    
    public static class ErrorResponse {
        private final String error;
        private final String message;
        private final String errorCode;
        private final String field;
        private final String details;

        public ErrorResponse(String error, String message) {
            this(error, message, null, null, null);
        }

        public ErrorResponse(String error, String message, String errorCode) {
            this(error, message, errorCode, null, null);
        }

        public ErrorResponse(String error, String message, String errorCode, String field, String details) {
            this.error = error;
            this.message = message;
            this.errorCode = errorCode;
            this.field = field;
            this.details = details;
        }

        public String getError() { return error; }
        public String getMessage() { return message; }
        public String getErrorCode() { return errorCode; }
        public String getField() { return field; }
        public String getDetails() { return details; }
    }
}

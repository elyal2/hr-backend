package com.humanrsc.exceptions;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {
    
    private static final Logger LOG = Logger.getLogger(GlobalExceptionHandler.class);
    
    @Override
    public Response toResponse(Exception exception) {
        LOG.error("Unhandled exception occurred", exception);
        
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
        
        if (exception instanceof jakarta.ws.rs.NotFoundException) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(
                        "Endpoint not found", 
                        "The requested endpoint does not exist. Check the URL path.", 
                        "ENDPOINT_NOT_FOUND",
                        "url",
                        null
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

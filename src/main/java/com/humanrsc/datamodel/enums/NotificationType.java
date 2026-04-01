package com.humanrsc.datamodel.enums;

/**
 * Notification types for system events that trigger user notifications.
 * These events represent significant organizational changes that stakeholders
 * (typically managers and HR staff) should be informed about.
 */
public enum NotificationType {
    
    /**
     * Triggered when a new employee is hired and added to the system
     */
    EMPLOYEE_HIRED,
    
    /**
     * Triggered when an employee is terminated (involuntary separation)
     */
    EMPLOYEE_TERMINATED,
    
    /**
     * Triggered when an employee's salary is modified
     */
    SALARY_CHANGED,
    
    /**
     * Triggered when a temporary replacement assignment begins
     */
    REPLACEMENT_STARTED,
    
    /**
     * Triggered when a temporary replacement assignment is completed
     */
    REPLACEMENT_COMPLETED,
    
    /**
     * Triggered when an employee's assignment (position/unit) changes
     */
    ASSIGNMENT_CHANGED
}

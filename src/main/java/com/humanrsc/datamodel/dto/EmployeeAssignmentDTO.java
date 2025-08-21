package com.humanrsc.datamodel.dto;

import com.humanrsc.datamodel.abstraction.ObjectID;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class EmployeeAssignmentDTO {
    
    private ObjectID objectID;
    
    @NotNull
    private String employeeId;
    
    private String positionId;
    private String unitId;
    private String managerId;
    
    @NotNull
    private LocalDate startDate;
    
    private LocalDate endDate;
    private BigDecimal salary;
    private String currency = "USD";
    private String movementReason;
    private String notes;
    
    // Constructors
    public EmployeeAssignmentDTO() {}
    
    public EmployeeAssignmentDTO(ObjectID objectID, String employeeId, String positionId, String unitId, 
                                String managerId, LocalDate startDate, LocalDate endDate, BigDecimal salary, 
                                String currency, String movementReason, String notes) {
        this.objectID = objectID;
        this.employeeId = employeeId;
        this.positionId = positionId;
        this.unitId = unitId;
        this.managerId = managerId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.salary = salary;
        this.currency = currency;
        this.movementReason = movementReason;
        this.notes = notes;
    }
    
    // Getters and Setters
    public ObjectID getObjectID() { return objectID; }
    public void setObjectID(ObjectID objectID) { this.objectID = objectID; }
    
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    
    public String getPositionId() { return positionId; }
    public void setPositionId(String positionId) { this.positionId = positionId; }
    
    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }
    
    public String getManagerId() { return managerId; }
    public void setManagerId(String managerId) { this.managerId = managerId; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    
    public BigDecimal getSalary() { return salary; }
    public void setSalary(BigDecimal salary) { this.salary = salary; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public String getMovementReason() { return movementReason; }
    public void setMovementReason(String movementReason) { this.movementReason = movementReason; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

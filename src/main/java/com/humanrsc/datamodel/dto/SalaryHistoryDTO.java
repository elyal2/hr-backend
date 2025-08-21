package com.humanrsc.datamodel.dto;

import com.humanrsc.datamodel.abstraction.ObjectID;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SalaryHistoryDTO {
    
    private ObjectID objectID;
    
    @NotNull
    private String employeeId;
    
    private BigDecimal oldSalary;
    
    @NotNull
    private BigDecimal newSalary;
    
    private String currency = "USD";
    
    @NotNull
    private LocalDate effectiveDate;
    
    private String reason;
    private String approvedById;
    
    // Constructors
    public SalaryHistoryDTO() {}
    
    public SalaryHistoryDTO(ObjectID objectID, String employeeId, BigDecimal oldSalary, BigDecimal newSalary, 
                           String currency, LocalDate effectiveDate, String reason, String approvedById) {
        this.objectID = objectID;
        this.employeeId = employeeId;
        this.oldSalary = oldSalary;
        this.newSalary = newSalary;
        this.currency = currency;
        this.effectiveDate = effectiveDate;
        this.reason = reason;
        this.approvedById = approvedById;
    }
    
    // Getters and Setters
    public ObjectID getObjectID() { return objectID; }
    public void setObjectID(ObjectID objectID) { this.objectID = objectID; }
    
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    
    public BigDecimal getOldSalary() { return oldSalary; }
    public void setOldSalary(BigDecimal oldSalary) { this.oldSalary = oldSalary; }
    
    public BigDecimal getNewSalary() { return newSalary; }
    public void setNewSalary(BigDecimal newSalary) { this.newSalary = newSalary; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getApprovedById() { return approvedById; }
    public void setApprovedById(String approvedById) { this.approvedById = approvedById; }
}

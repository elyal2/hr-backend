package com.humanrsc.datamodel.dto;

import com.humanrsc.datamodel.abstraction.ObjectID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class TemporaryReplacementDTO {
    
    private ObjectID objectID;
    
    @NotNull
    private String originalEmployeeId;
    
    @NotNull
    private String replacementEmployeeId;
    
    private String positionId;
    
    @NotNull
    private LocalDate startDate;
    
    private LocalDate endDate;
    private String reason;
    
    @NotBlank
    private String status;
    
    // Constructors
    public TemporaryReplacementDTO() {}
    
    public TemporaryReplacementDTO(ObjectID objectID, String originalEmployeeId, String replacementEmployeeId, 
                                  String positionId, LocalDate startDate, LocalDate endDate, String reason, String status) {
        this.objectID = objectID;
        this.originalEmployeeId = originalEmployeeId;
        this.replacementEmployeeId = replacementEmployeeId;
        this.positionId = positionId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.status = status;
    }
    
    // Getters and Setters
    public ObjectID getObjectID() { return objectID; }
    public void setObjectID(ObjectID objectID) { this.objectID = objectID; }
    
    public String getOriginalEmployeeId() { return originalEmployeeId; }
    public void setOriginalEmployeeId(String originalEmployeeId) { this.originalEmployeeId = originalEmployeeId; }
    
    public String getReplacementEmployeeId() { return replacementEmployeeId; }
    public void setReplacementEmployeeId(String replacementEmployeeId) { this.replacementEmployeeId = replacementEmployeeId; }
    
    public String getPositionId() { return positionId; }
    public void setPositionId(String positionId) { this.positionId = positionId; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

package com.humanrsc.datamodel.dto;

import com.humanrsc.datamodel.abstraction.ObjectID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class JobPositionDTO {
    
    private ObjectID objectID;
    
    @NotBlank
    private String title;
    
    private String description;
    private String jobCode;
    
    @NotBlank
    private String status;
    
    @NotNull
    private Integer hierarchicalLevel;
    
    private String unitId; // Solo el ID como string
    private String categoryId; // Solo el ID como string
    
    // Constructors
    public JobPositionDTO() {}
    
    public JobPositionDTO(ObjectID objectID, String title, String description, String jobCode, 
                         String status, Integer hierarchicalLevel, String unitId, String categoryId) {
        this.objectID = objectID;
        this.title = title;
        this.description = description;
        this.jobCode = jobCode;
        this.status = status;
        this.hierarchicalLevel = hierarchicalLevel;
        this.unitId = unitId;
        this.categoryId = categoryId;
    }
    
    // Getters and Setters
    public ObjectID getObjectID() { return objectID; }
    public void setObjectID(ObjectID objectID) { this.objectID = objectID; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getJobCode() { return jobCode; }
    public void setJobCode(String jobCode) { this.jobCode = jobCode; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Integer getHierarchicalLevel() { return hierarchicalLevel; }
    public void setHierarchicalLevel(Integer hierarchicalLevel) { this.hierarchicalLevel = hierarchicalLevel; }
    
    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }
    
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
}

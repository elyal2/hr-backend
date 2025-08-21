package com.humanrsc.datamodel.dto;

import com.humanrsc.datamodel.abstraction.ObjectID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class OrganizationalUnitDTO {
    
    private ObjectID objectID;
    
    @NotBlank
    private String name;
    
    private String description;
    private String costCenter;
    private String location;
    private String country;
    
    @NotBlank
    private String status;
    
    @NotNull
    private Integer organizationalLevel;
    
    private String parentUnitId; // Solo el ID como string
    
    // Constructors
    public OrganizationalUnitDTO() {}
    
    public OrganizationalUnitDTO(ObjectID objectID, String name, String description, String costCenter, 
                                String location, String country, String status, 
                                Integer organizationalLevel, String parentUnitId) {
        this.objectID = objectID;
        this.name = name;
        this.description = description;
        this.costCenter = costCenter;
        this.location = location;
        this.country = country;
        this.status = status;
        this.organizationalLevel = organizationalLevel;
        this.parentUnitId = parentUnitId;
    }
    
    // Getters and Setters
    public ObjectID getObjectID() { return objectID; }
    public void setObjectID(ObjectID objectID) { this.objectID = objectID; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCostCenter() { return costCenter; }
    public void setCostCenter(String costCenter) { this.costCenter = costCenter; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Integer getOrganizationalLevel() { return organizationalLevel; }
    public void setOrganizationalLevel(Integer organizationalLevel) { this.organizationalLevel = organizationalLevel; }
    
    public String getParentUnitId() { return parentUnitId; }
    public void setParentUnitId(String parentUnitId) { this.parentUnitId = parentUnitId; }
}

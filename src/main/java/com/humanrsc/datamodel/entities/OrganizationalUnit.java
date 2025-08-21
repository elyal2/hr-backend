package com.humanrsc.datamodel.entities;

import com.humanrsc.datamodel.abstraction.ExtendedAttribute;
import com.humanrsc.datamodel.abstraction.ObjectID;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "organizational_units", schema = "hr_app")
@Getter
@Setter
@Audited
public class OrganizationalUnit {

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_INACTIVE = "inactive";
    public static final String STATUS_DELETED = "deleted";

    // Organizational level constants - extensible for future levels
    public static final int LEVEL_1 = 1;  // Executive/C-Suite
    public static final int LEVEL_2 = 2;  // Senior Management
    public static final int LEVEL_3 = 3;  // Middle Management
    public static final int LEVEL_4 = 4;  // Team Lead/Supervisor
    public static final int LEVEL_5 = 5;  // Senior Individual Contributor
    public static final int LEVEL_6 = 6;  // Individual Contributor
    public static final int LEVEL_7 = 7;  // Junior Individual Contributor
    public static final int LEVEL_8 = 8;  // Entry Level
    public static final int LEVEL_9 = 9;  // Intern/Trainee
    public static final int LEVEL_10 = 10; // Special/Advisory

    @EmbeddedId
    @NotNull
    private ObjectID objectID;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumns({
        @JoinColumn(name = "parent_unit_id", referencedColumnName = "id"),
        @JoinColumn(name = "parent_unit_tenant_id", referencedColumnName = "tenant_id")
    })
    @com.fasterxml.jackson.annotation.JsonIgnore
    private OrganizationalUnit parentUnit;

    @NotNull
    @Column(name = "organizational_level", nullable = false)
    private Integer organizationalLevel = 1;



    @Column(name = "cost_center")
    private String costCenter;

    private String location;

    private String country;

    @Audited
    @NotBlank
    @Column(nullable = false, length = 50)
    private String status = STATUS_ACTIVE;

    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    @Audited
    @Column(name = "date_updated")
    private LocalDateTime dateUpdated;

    @NotAudited
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "organizational_units_extended_attributes",
        schema = "hr_app",
        joinColumns = {
            @JoinColumn(name = "id"), 
            @JoinColumn(name = "tenant_id")
        }
    )
    private Set<ExtendedAttribute> attributes;

    public OrganizationalUnit() {
        this.dateCreated = LocalDateTime.now();
        this.dateUpdated = LocalDateTime.now();
    }

    // Attributes management
    public Set<ExtendedAttribute> getAttributes() {
        if (attributes == null) {
            attributes = new HashSet<>();
        }
        return attributes;
    }

    public void setAttributes(Set<ExtendedAttribute> attributes) {
        throw new UnsupportedOperationException("Cannot set an external attribute array");
    }

    public ExtendedAttribute getAttribute(String key) {
        for (ExtendedAttribute attribute : getAttributes()) {
            if (attribute.getKey().equals(key)) {
                return attribute;
            }
        }
        return null;
    }

    public void setAttribute(ExtendedAttribute attribute) {
        if (attribute != null) {
            getAttributes().removeIf(attr -> attr.getKey().equals(attribute.getKey()));
            getAttributes().add(attribute);
        }
    }

    public void setAttribute(String key, String value) {
        setAttribute(ExtendedAttribute.of(key, value));
    }

    // Status checks
    public boolean isActive() {
        return STATUS_ACTIVE.equals(this.status);
    }

    public boolean isInactive() {
        return STATUS_INACTIVE.equals(this.status);
    }

    public boolean isDeleted() {
        return STATUS_DELETED.equals(this.status);
    }

    // Utility methods
    public void activate() {
        this.status = STATUS_ACTIVE;
        this.dateUpdated = LocalDateTime.now();
    }

    public void deactivate() {
        this.status = STATUS_INACTIVE;
        this.dateUpdated = LocalDateTime.now();
    }
    
    // Getter para parentUnitId como string (para serialización JSON)
    public String getParentUnitId() {
        return parentUnit != null ? parentUnit.getObjectID().getId() : null;
    }

    public void markAsDeleted() {
        this.status = STATUS_DELETED;
        this.dateUpdated = LocalDateTime.now();
    }

    public void updateTimestamp() {
        this.dateUpdated = LocalDateTime.now();
    }

    // Hierarchy methods
    public boolean isRoot() {
        return parentUnit == null;
    }

    public boolean hasParent() {
        return parentUnit != null;
    }



    @Override
    public String toString() {
        return "ORGANIZATIONAL_UNIT{" + objectID + ", name='" + name + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrganizationalUnit)) return false;
        OrganizationalUnit that = (OrganizationalUnit) o;
        return objectID != null ? objectID.equals(that.objectID) : that.objectID == null;
    }

    @Override
    public int hashCode() {
        return objectID != null ? objectID.hashCode() : 0;
    }
}

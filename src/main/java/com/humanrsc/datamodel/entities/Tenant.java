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
@Table(name = "tenant", schema = "hr_app")
@Getter
@Setter
public class Tenant {

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_INACTIVE = "inactive";
    public static final String STATUS_SUSPENDED = "suspended";
    public static final String STATUS_PENDING = "pending";

    @EmbeddedId
    @NotNull
    private ObjectID objectID;

    @NotBlank
    @Column(nullable = false)
    private String name;
    
    private String domain;
    
    @Audited
    @NotBlank
    @Column(nullable = false, length = 50)
    private String status = STATUS_PENDING;

    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    @Audited
    @Column(name = "date_status_update")
    private LocalDateTime dateStatusUpdate;

    @Column(name = "max_users")
    private Integer maxUsers = 10;

    @Column(name = "subscription_plan", length = 50)
    private String subscriptionPlan = "basic";

    @NotAudited
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "tenant_extended_attributes",
        schema = "hr_app",
        joinColumns = {
            @JoinColumn(name = "id"), 
            @JoinColumn(name = "tenant_id")
        }
    )
    private Set<ExtendedAttribute> attributes;

    // Constructor
    public Tenant() {
        this.dateCreated = LocalDateTime.now();
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

    public boolean isSuspended() {
        return STATUS_SUSPENDED.equals(this.status);
    }

    public boolean isPending() {
        return STATUS_PENDING.equals(this.status);
    }

    // Utility methods
    public void activate() {
        this.status = STATUS_ACTIVE;
        this.dateStatusUpdate = LocalDateTime.now();
    }

    public void suspend() {
        this.status = STATUS_SUSPENDED;
        this.dateStatusUpdate = LocalDateTime.now();
    }

    public void deactivate() {
        this.status = STATUS_INACTIVE;
        this.dateStatusUpdate = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "TENANT{" + objectID + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tenant)) return false;
        Tenant tenant = (Tenant) o;
        return objectID != null ? objectID.equals(tenant.objectID) : tenant.objectID == null;
    }

    @Override
    public int hashCode() {
        return objectID != null ? objectID.hashCode() : 0;
    }
}

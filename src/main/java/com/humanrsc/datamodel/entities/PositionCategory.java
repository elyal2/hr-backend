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
@Table(name = "position_categories", schema = "hr_app")
@Getter
@Setter
@Audited
public class PositionCategory {

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_INACTIVE = "inactive";
    public static final String STATUS_DELETED = "deleted";

    @EmbeddedId
    @NotNull
    private ObjectID objectID;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

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
        name = "position_categories_extended_attributes",
        schema = "hr_app",
        joinColumns = {
            @JoinColumn(name = "id"), 
            @JoinColumn(name = "tenant_id")
        }
    )
    private Set<ExtendedAttribute> attributes;

    public PositionCategory() {
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

    public void addAttribute(String key, String value) {
        getAttributes().add(new ExtendedAttribute(key, value));
    }

    public void removeAttribute(String key) {
        getAttributes().removeIf(attr -> attr.getKey().equals(key));
    }

    // Status management
    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    public boolean isInactive() {
        return STATUS_INACTIVE.equals(status);
    }

    public boolean isDeleted() {
        return STATUS_DELETED.equals(status);
    }

    public void activate() {
        this.status = STATUS_ACTIVE;
        this.dateUpdated = LocalDateTime.now();
    }

    public void deactivate() {
        this.status = STATUS_INACTIVE;
        this.dateUpdated = LocalDateTime.now();
    }

    public void delete() {
        this.status = STATUS_DELETED;
        this.dateUpdated = LocalDateTime.now();
    }

    // Timestamp management
    public void updateTimestamp() {
        this.dateUpdated = LocalDateTime.now();
    }
}

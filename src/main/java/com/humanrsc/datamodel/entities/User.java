package com.humanrsc.datamodel.entities;

import com.humanrsc.datamodel.abstraction.ExtendedAttribute;
import com.humanrsc.datamodel.abstraction.ObjectID;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
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
@Table(name = "users", schema = "hr_app")
@Getter
@Setter
public class User {

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_INACTIVE = "inactive";
    public static final String STATUS_SUSPENDED = "suspended";
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_DELETED = "deleted";

    @EmbeddedId
    @NotNull
    private ObjectID objectID;

    private String firstName;
    private String lastName;

    @NotBlank
    @Email
    @Column(nullable = false, length = 320)
    private String email;

    @Audited
    @NotBlank
    @Column(nullable = false, length = 50)
    private String status = STATUS_PENDING;

    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    @Audited
    @Column(name = "date_status_update")
    private LocalDateTime dateStatusUpdate;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_roles",
        schema = "hr_app",
        joinColumns = {
            @JoinColumn(name = "id"), 
            @JoinColumn(name = "tenant_id")
        }
    )
    @Column(name = "role")
    private Set<String> roles = new HashSet<>();

    @NotAudited
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_extended_attributes",
        schema = "hr_app",
        joinColumns = {
            @JoinColumn(name = "id"), 
            @JoinColumn(name = "tenant_id")
        }
    )
    private Set<ExtendedAttribute> attributes;

    public User() {
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

    // Roles management
    public Set<String> getRoles() {
        if (roles == null) {
            roles = new HashSet<>();
        }
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles != null ? roles : new HashSet<>();
    }

    public void addRole(String role) {
        getRoles().add(role);
    }

    public void removeRole(String role) {
        getRoles().remove(role);
    }

    public boolean hasRole(String role) {
        return getRoles().contains(role);
    }

    // Email validation
    public void setEmail(String email) {
        String regexPattern = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@"
                + "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
        if (patternMatches(email, regexPattern)) {
            this.email = email;
        } else {
            throw new IllegalArgumentException("Invalid email: " + email);
        }
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

    public boolean isDeleted() {
        return STATUS_DELETED.equals(this.status);
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

    public void markAsDeleted() {
        this.status = STATUS_DELETED;
        this.dateStatusUpdate = LocalDateTime.now();
    }

    public void updateLastLogin() {
        this.lastLogin = LocalDateTime.now();
    }

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "").trim();
    }

    // Utility methods
    public static boolean patternMatches(String string, String regexPattern) {
        return java.util.regex.Pattern.compile(regexPattern)
                .matcher(string)
                .matches();
    }

    @Override
    public String toString() {
        return "USER{" + objectID + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return objectID != null ? objectID.equals(user.objectID) : user.objectID == null;
    }

    @Override
    public int hashCode() {
        return objectID != null ? objectID.hashCode() : 0;
    }
}

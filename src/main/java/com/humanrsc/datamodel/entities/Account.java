package com.humanrsc.datamodel.entities;

import com.humanrsc.datamodel.abstraction.ExtendedAttribute;
import com.humanrsc.datamodel.abstraction.ObjectID;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Entity
@Table(name = "account", schema = "hr_app")
@Getter
@Setter
public class Account {

    public static final int STATUS_FREE = 1;
    public static final int STATUS_PAY = 2;
    public static final int STATUS_SUSPENDED = 3;
    public static final int STATUS_DELETED = 4;

    @EmbeddedId
    private ObjectID objectID;

    private String name;
    private String surname;

    @Audited
    private Integer status = STATUS_FREE;

    private String email;

    @Column(name = "date_registered")
    private LocalDateTime dateRegistered;

    @Audited
    @Column(name = "date_status_update")
    private LocalDateTime dateStatusUpdate;

    @NotAudited
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "account_extended_attributes",
        schema = "hr_app",
        joinColumns = {
            @JoinColumn(name = "id"), 
            @JoinColumn(name = "tenant_id")
        }
    )
    private Set<ExtendedAttribute> attributes;

    // Constructor
    public Account() {
        this.dateRegistered = LocalDateTime.now();
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
    public boolean isFree() {
        return STATUS_FREE == this.status;
    }

    public boolean isPay() {
        return STATUS_PAY == this.status;
    }

    public boolean isSuspended() {
        return STATUS_SUSPENDED == this.status;
    }

    public boolean isDeleted() {
        return STATUS_DELETED == this.status;
    }

    // Utility methods
    public static boolean patternMatches(String string, String regexPattern) {
        return Pattern.compile(regexPattern)
                .matcher(string)
                .matches();
    }

    @Override
    public String toString() {
        return "ACCOUNT{" + objectID + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account)) return false;
        Account account = (Account) o;
        return objectID != null ? objectID.equals(account.objectID) : account.objectID == null;
    }

    @Override
    public int hashCode() {
        return objectID != null ? objectID.hashCode() : 0;
    }
}
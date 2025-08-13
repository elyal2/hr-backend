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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "employees", schema = "hr_app")
@Getter
@Setter
@Audited
public class Employee {

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_INACTIVE = "inactive";
    public static final String STATUS_TERMINATED = "terminated";
    public static final String STATUS_RESIGNED = "resigned";

    public static final String EMPLOYEE_TYPE_EMPLOYEE = "employee";
    public static final String EMPLOYEE_TYPE_CONTRACTOR = "contractor";
    public static final String EMPLOYEE_TYPE_INTERN = "intern";
    public static final String EMPLOYEE_TYPE_CONSULTANT = "consultant";
    public static final String EMPLOYEE_TYPE_TEMPORARY = "temporary";

    public static final String CONTRACT_TYPE_FULL_TIME = "full_time";
    public static final String CONTRACT_TYPE_PART_TIME = "part_time";
    public static final String CONTRACT_TYPE_FIXED_TERM = "fixed_term";
    public static final String CONTRACT_TYPE_INDEFINITE = "indefinite";
    public static final String CONTRACT_TYPE_PROJECT_BASED = "project_based";

    @EmbeddedId
    @NotNull
    private ObjectID objectID;

    @NotBlank
    @Column(name = "employee_id", nullable = false)
    private String employeeId;

    @NotBlank
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @NotBlank
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @NotBlank
    @Email
    @Column(nullable = false, length = 320)
    private String email;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    private String gender;

    @Column(name = "national_id")
    private String nationalId;

    @Column(name = "tax_id")
    private String taxId;

    @NotBlank
    @Column(name = "employee_type", nullable = false, length = 50)
    private String employeeType = "employee";

    @Column(name = "contract_type", length = 50)
    private String contractType;

    @NotNull
    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "current_salary", precision = 15, scale = 2)
    private BigDecimal currentSalary;

    @Column(length = 3)
    private String currency = "USD";

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
        name = "employees_extended_attributes",
        schema = "hr_app",
        joinColumns = {
            @JoinColumn(name = "id"), 
            @JoinColumn(name = "tenant_id")
        }
    )
    private Set<ExtendedAttribute> attributes;

    public Employee() {
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

    public boolean isTerminated() {
        return STATUS_TERMINATED.equals(this.status);
    }

    public boolean isResigned() {
        return STATUS_RESIGNED.equals(this.status);
    }

    public boolean isEmployed() {
        return isActive() || isInactive();
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

    public void terminate(LocalDate terminationDate) {
        this.status = STATUS_TERMINATED;
        this.terminationDate = terminationDate;
        this.dateUpdated = LocalDateTime.now();
    }

    public void resign(LocalDate resignationDate) {
        this.status = STATUS_RESIGNED;
        this.terminationDate = resignationDate;
        this.dateUpdated = LocalDateTime.now();
    }

    public void updateTimestamp() {
        this.dateUpdated = LocalDateTime.now();
    }

    // Business logic methods
    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "").trim();
    }

    public boolean hasSalary() {
        return currentSalary != null;
    }

    public boolean isContractor() {
        return EMPLOYEE_TYPE_CONTRACTOR.equals(employeeType);
    }

    public boolean isIntern() {
        return EMPLOYEE_TYPE_INTERN.equals(employeeType);
    }

    public boolean isFullTime() {
        return CONTRACT_TYPE_FULL_TIME.equals(contractType);
    }

    public boolean isPartTime() {
        return CONTRACT_TYPE_PART_TIME.equals(contractType);
    }

    public int getAge() {
        if (dateOfBirth == null) {
            return 0;
        }
        return LocalDate.now().getYear() - dateOfBirth.getYear();
    }

    public long getTenureInDays() {
        if (hireDate == null) {
            return 0;
        }
        LocalDate endDate = terminationDate != null ? terminationDate : LocalDate.now();
        return java.time.temporal.ChronoUnit.DAYS.between(hireDate, endDate);
    }

    public long getTenureInYears() {
        if (hireDate == null) {
            return 0;
        }
        LocalDate endDate = terminationDate != null ? terminationDate : LocalDate.now();
        return java.time.temporal.ChronoUnit.YEARS.between(hireDate, endDate);
    }

    @Override
    public String toString() {
        return "EMPLOYEE{" + objectID + ", employeeId='" + employeeId + "', name='" + getFullName() + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Employee)) return false;
        Employee employee = (Employee) o;
        return objectID != null ? objectID.equals(employee.objectID) : employee.objectID == null;
    }

    @Override
    public int hashCode() {
        return objectID != null ? objectID.hashCode() : 0;
    }

    // Enums for type safety
    public enum EmployeeType {
        EMPLOYEE,
        CONTRACTOR,
        INTERN,
        CONSULTANT,
        TEMPORARY
    }

    public enum ContractType {
        FULL_TIME,
        PART_TIME,
        FIXED_TERM,
        INDEFINITE,
        PROJECT_BASED
    }
}

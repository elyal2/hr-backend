package com.humanrsc.datamodel.entities;

import com.humanrsc.datamodel.abstraction.ObjectID;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_assignments", schema = "hr_app")
@Getter
@Setter
public class EmployeeAssignment {

    public static final String MOVEMENT_REASON_PROMOTION = "promotion";
    public static final String MOVEMENT_REASON_LATERAL_MOVE = "lateral_move";
    public static final String MOVEMENT_REASON_DEMOTION = "demotion";
    public static final String MOVEMENT_REASON_RESTRUCTURING = "restructuring";
    public static final String MOVEMENT_REASON_NEW_POSITION = "new_position";
    public static final String MOVEMENT_REASON_TEMPORARY_ASSIGNMENT = "temporary_assignment";
    public static final String MOVEMENT_REASON_RETURN_FROM_ASSIGNMENT = "return_from_assignment";
    public static final String MOVEMENT_REASON_TERMINATION = "termination";
    public static final String MOVEMENT_REASON_RESIGNATION = "resignation";

    @EmbeddedId
    @NotNull
    private ObjectID objectID;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumns({
        @JoinColumn(name = "employee_id", referencedColumnName = "id"),
        @JoinColumn(name = "employee_tenant_id", referencedColumnName = "tenant_id")
    })
    @NotNull
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Employee employee;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumns({
        @JoinColumn(name = "position_id", referencedColumnName = "id"),
        @JoinColumn(name = "position_tenant_id", referencedColumnName = "tenant_id")
    })
    @com.fasterxml.jackson.annotation.JsonIgnore
    private JobPosition position;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumns({
        @JoinColumn(name = "unit_id", referencedColumnName = "id"),
        @JoinColumn(name = "unit_tenant_id", referencedColumnName = "tenant_id")
    })
    @com.fasterxml.jackson.annotation.JsonIgnore
    private OrganizationalUnit unit;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumns({
        @JoinColumn(name = "manager_id", referencedColumnName = "id"),
        @JoinColumn(name = "manager_tenant_id", referencedColumnName = "tenant_id")
    })
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Employee manager;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(precision = 15, scale = 2)
    private BigDecimal salary;

    @Column(length = 3)
    private String currency = "USD";

    @Column(name = "movement_reason", length = 50)
    private String movementReason;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    public EmployeeAssignment() {
        this.dateCreated = LocalDateTime.now();
    }

    // Business logic methods
    public boolean isActive() {
        return endDate == null || endDate.isAfter(LocalDate.now());
    }

    public boolean isHistorical() {
        return endDate != null && endDate.isBefore(LocalDate.now());
    }

    public boolean isCurrent() {
        return endDate == null;
    }

    public boolean hasManager() {
        return manager != null;
    }

    public boolean hasPosition() {
        return position != null;
    }

    public boolean hasUnit() {
        return unit != null;
    }

    public boolean hasSalary() {
        return salary != null;
    }

    public long getDurationInDays() {
        if (startDate == null) {
            return 0;
        }
        LocalDate endDate = this.endDate != null ? this.endDate : LocalDate.now();
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
    }

    public long getDurationInMonths() {
        if (startDate == null) {
            return 0;
        }
        LocalDate endDate = this.endDate != null ? this.endDate : LocalDate.now();
        return java.time.temporal.ChronoUnit.MONTHS.between(startDate, endDate);
    }

    public long getDurationInYears() {
        if (startDate == null) {
            return 0;
        }
        LocalDate endDate = this.endDate != null ? this.endDate : LocalDate.now();
        return java.time.temporal.ChronoUnit.YEARS.between(startDate, endDate);
    }

    public boolean isPromotion() {
        return MOVEMENT_REASON_PROMOTION.equals(movementReason);
    }

    public boolean isDemotion() {
        return MOVEMENT_REASON_DEMOTION.equals(movementReason);
    }

    public boolean isLateralMove() {
        return MOVEMENT_REASON_LATERAL_MOVE.equals(movementReason);
    }

    public boolean isTermination() {
        return MOVEMENT_REASON_TERMINATION.equals(movementReason);
    }

    public boolean isResignation() {
        return MOVEMENT_REASON_RESIGNATION.equals(movementReason);
    }
    
    // Getters para IDs de relaciones (para serialización JSON)
    public String getEmployeeId() {
        return employee != null ? employee.getObjectID().getId() : null;
    }
    
    public String getPositionId() {
        return position != null ? position.getObjectID().getId() : null;
    }
    
    public String getUnitId() {
        return unit != null ? unit.getObjectID().getId() : null;
    }
    
    public String getManagerId() {
        return manager != null ? manager.getObjectID().getId() : null;
    }

    @Override
    public String toString() {
        return "EMPLOYEE_ASSIGNMENT{" + objectID + ", employee='" + 
               (employee != null ? employee.getFullName() : "null") + 
               "', startDate=" + startDate + ", endDate=" + endDate + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmployeeAssignment)) return false;
        EmployeeAssignment that = (EmployeeAssignment) o;
        return objectID != null ? objectID.equals(that.objectID) : that.objectID == null;
    }

    @Override
    public int hashCode() {
        return objectID != null ? objectID.hashCode() : 0;
    }

    // Enum for movement reasons
    public enum MovementReason {
        PROMOTION,
        LATERAL_MOVE,
        DEMOTION,
        RESTRUCTURING,
        NEW_POSITION,
        TEMPORARY_ASSIGNMENT,
        RETURN_FROM_ASSIGNMENT,
        TERMINATION,
        RESIGNATION
    }
}

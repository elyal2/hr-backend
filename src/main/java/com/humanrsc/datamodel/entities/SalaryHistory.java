package com.humanrsc.datamodel.entities;

import com.humanrsc.datamodel.abstraction.ObjectID;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "salary_history", schema = "hr_app")
@Getter
@Setter
public class SalaryHistory {

    @EmbeddedId
    @NotNull
    private ObjectID objectID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "employee_id", referencedColumnName = "id"),
        @JoinColumn(name = "employee_tenant_id", referencedColumnName = "tenant_id")
    })
    @NotNull
    private Employee employee;

    @Column(name = "old_salary", precision = 15, scale = 2)
    private BigDecimal oldSalary;

    @NotNull
    @Column(name = "new_salary", precision = 15, scale = 2, nullable = false)
    private BigDecimal newSalary;

    @Column(length = 3)
    private String currency = "USD";

    @NotNull
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(columnDefinition = "text")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "approved_by", referencedColumnName = "id"),
        @JoinColumn(name = "approved_by_tenant_id", referencedColumnName = "tenant_id")
    })
    private Employee approvedBy;

    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    public SalaryHistory() {
        this.dateCreated = LocalDateTime.now();
    }

    // Business logic methods
    public BigDecimal getSalaryIncrease() {
        if (oldSalary == null || newSalary == null) {
            return BigDecimal.ZERO;
        }
        return newSalary.subtract(oldSalary);
    }

    public BigDecimal getSalaryIncreasePercentage() {
        if (oldSalary == null || newSalary == null || oldSalary.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getSalaryIncrease().divide(oldSalary, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    public boolean isIncrease() {
        return getSalaryIncrease().compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isDecrease() {
        return getSalaryIncrease().compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isNoChange() {
        return getSalaryIncrease().compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean hasApproval() {
        return approvedBy != null;
    }

    public boolean isRecent() {
        return effectiveDate.isAfter(LocalDate.now().minusMonths(6));
    }

    @Override
    public String toString() {
        return "SALARY_HISTORY{" + objectID + 
               ", employee='" + (employee != null ? employee.getFullName() : "null") + 
               "', oldSalary=" + oldSalary + ", newSalary=" + newSalary + 
               ", effectiveDate=" + effectiveDate + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SalaryHistory)) return false;
        SalaryHistory that = (SalaryHistory) o;
        return objectID != null ? objectID.equals(that.objectID) : that.objectID == null;
    }

    @Override
    public int hashCode() {
        return objectID != null ? objectID.hashCode() : 0;
    }
}

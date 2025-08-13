package com.humanrsc.datamodel.entities;

import com.humanrsc.datamodel.abstraction.ObjectID;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "temporary_replacements", schema = "hr_app")
@Getter
@Setter
public class TemporaryReplacement {

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_CANCELLED = "cancelled";

    @EmbeddedId
    @NotNull
    private ObjectID objectID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "original_employee_id", referencedColumnName = "id"),
        @JoinColumn(name = "original_employee_tenant_id", referencedColumnName = "tenant_id")
    })
    @NotNull
    private Employee originalEmployee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "replacement_employee_id", referencedColumnName = "id"),
        @JoinColumn(name = "replacement_employee_tenant_id", referencedColumnName = "tenant_id")
    })
    @NotNull
    private Employee replacementEmployee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "position_id", referencedColumnName = "id"),
        @JoinColumn(name = "position_tenant_id", referencedColumnName = "tenant_id")
    })
    private JobPosition position;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(columnDefinition = "text")
    private String reason;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String status = STATUS_ACTIVE;

    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    @Column(name = "date_updated")
    private LocalDateTime dateUpdated;

    public TemporaryReplacement() {
        this.dateCreated = LocalDateTime.now();
        this.dateUpdated = LocalDateTime.now();
    }

    // Business logic methods
    public boolean isActive() {
        return STATUS_ACTIVE.equals(this.status);
    }

    public boolean isCompleted() {
        return STATUS_COMPLETED.equals(this.status);
    }

    public boolean isCancelled() {
        return STATUS_CANCELLED.equals(this.status);
    }

    public boolean isCurrent() {
        return isActive() && (endDate == null || endDate.isAfter(LocalDate.now()));
    }

    public boolean isExpired() {
        return endDate != null && endDate.isBefore(LocalDate.now());
    }

    public long getDurationInDays() {
        if (startDate == null) {
            return 0;
        }
        LocalDate endDate = this.endDate != null ? this.endDate : LocalDate.now();
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
    }

    public void complete() {
        this.status = STATUS_COMPLETED;
        this.dateUpdated = LocalDateTime.now();
    }

    public void cancel() {
        this.status = STATUS_CANCELLED;
        this.dateUpdated = LocalDateTime.now();
    }

    public void updateTimestamp() {
        this.dateUpdated = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "TEMPORARY_REPLACEMENT{" + objectID + 
               ", original='" + (originalEmployee != null ? originalEmployee.getFullName() : "null") + 
               "', replacement='" + (replacementEmployee != null ? replacementEmployee.getFullName() : "null") + 
               "', startDate=" + startDate + ", endDate=" + endDate + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TemporaryReplacement)) return false;
        TemporaryReplacement that = (TemporaryReplacement) o;
        return objectID != null ? objectID.equals(that.objectID) : that.objectID == null;
    }

    @Override
    public int hashCode() {
        return objectID != null ? objectID.hashCode() : 0;
    }
}

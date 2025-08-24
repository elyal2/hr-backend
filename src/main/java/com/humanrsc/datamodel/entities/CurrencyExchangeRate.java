package com.humanrsc.datamodel.entities;

import com.humanrsc.datamodel.abstraction.ObjectID;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "currency_exchange_rates", schema = "hr_app")
@Getter
@Setter
@Audited
public class CurrencyExchangeRate {

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_INACTIVE = "inactive";
    public static final String STATUS_EXPIRED = "expired";

    public static final String SOURCE_MANUAL = "manual";
    public static final String SOURCE_SYSTEM = "system";
    public static final String SOURCE_API = "api";

    @EmbeddedId
    @NotNull
    private ObjectID objectID;

    @NotNull
    @Column(name = "from_currency", nullable = false, length = 3)
    private String fromCurrency;

    @NotNull
    @Column(name = "to_currency", nullable = false, length = 3)
    private String toCurrency;

    @NotNull
    @Column(name = "exchange_rate", precision = 15, scale = 6, nullable = false)
    private BigDecimal exchangeRate;

    @NotNull
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "source", length = 100)
    private String source = SOURCE_MANUAL;

    @Column(columnDefinition = "text")
    private String notes;

    @NotNull
    @Column(nullable = false, length = 50)
    private String status = STATUS_ACTIVE;

    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    @Column(name = "date_updated")
    private LocalDateTime dateUpdated;

    public CurrencyExchangeRate() {
        this.dateCreated = LocalDateTime.now();
        this.dateUpdated = LocalDateTime.now();
    }

    // Business logic methods
    public boolean isActive() {
        return STATUS_ACTIVE.equals(this.status);
    }

    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    public boolean isEffective() {
        return effectiveDate != null && !effectiveDate.isAfter(LocalDate.now());
    }

    public boolean isValid() {
        return isActive() && isEffective() && !isExpired();
    }

    public BigDecimal convert(BigDecimal amount) {
        if (amount == null || exchangeRate == null) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(exchangeRate);
    }

    public BigDecimal convertReverse(BigDecimal amount) {
        if (amount == null || exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return amount.divide(exchangeRate, 6, java.math.RoundingMode.HALF_UP);
    }

    // Utility methods
    public void updateTimestamp() {
        this.dateUpdated = LocalDateTime.now();
    }

    public void expire() {
        this.status = STATUS_EXPIRED;
        this.expiryDate = LocalDate.now();
        updateTimestamp();
    }

    public void deactivate() {
        this.status = STATUS_INACTIVE;
        updateTimestamp();
    }

    public void activate() {
        this.status = STATUS_ACTIVE;
        updateTimestamp();
    }
}

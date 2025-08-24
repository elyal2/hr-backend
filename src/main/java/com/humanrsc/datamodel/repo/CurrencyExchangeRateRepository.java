package com.humanrsc.datamodel.repo;

import com.humanrsc.datamodel.abstraction.ObjectID;
import com.humanrsc.datamodel.entities.CurrencyExchangeRate;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CurrencyExchangeRateRepository implements PanacheRepositoryBase<CurrencyExchangeRate, ObjectID> {

    // Basic finder methods - RLS handles tenant filtering automatically
    
    public Optional<CurrencyExchangeRate> findByObjectID(ObjectID objectID) {
        return find("objectID = ?1", objectID).firstResultOptional();
    }

    public Optional<CurrencyExchangeRate> findById(String id) {
        return find("objectID.id = ?1", id).firstResultOptional();
    }

    // Business logic methods - RLS filters by tenant automatically
    
    public Optional<CurrencyExchangeRate> findValidRate(String fromCurrency, String toCurrency) {
        LocalDate today = LocalDate.now();
        return find("fromCurrency = ?1 and toCurrency = ?2 and status = ?3 and effectiveDate <= ?4 and (expiryDate is null or expiryDate > ?5) order by effectiveDate desc", 
                   fromCurrency, toCurrency, CurrencyExchangeRate.STATUS_ACTIVE, today, today)
               .firstResultOptional();
    }

    public Optional<CurrencyExchangeRate> findValidRate(String fromCurrency, String toCurrency, LocalDate date) {
        return find("fromCurrency = ?1 and toCurrency = ?2 and status = ?3 and effectiveDate <= ?4 and (expiryDate is null or expiryDate > ?5) order by effectiveDate desc", 
                   fromCurrency, toCurrency, CurrencyExchangeRate.STATUS_ACTIVE, date, date)
               .firstResultOptional();
    }

    public List<CurrencyExchangeRate> findValidRates(String fromCurrency) {
        LocalDate today = LocalDate.now();
        return find("fromCurrency = ?1 and status = ?2 and effectiveDate <= ?3 and (expiryDate is null or expiryDate > ?4) order by toCurrency", 
                   fromCurrency, CurrencyExchangeRate.STATUS_ACTIVE, today, today).list();
    }

    public List<CurrencyExchangeRate> findValidRates(String fromCurrency, LocalDate date) {
        return find("fromCurrency = ?1 and status = ?2 and effectiveDate <= ?3 and (expiryDate is null or expiryDate > ?4) order by toCurrency", 
                   fromCurrency, CurrencyExchangeRate.STATUS_ACTIVE, date, date).list();
    }

    public List<CurrencyExchangeRate> findAllValidRates() {
        LocalDate today = LocalDate.now();
        return find("status = ?1 and effectiveDate <= ?2 and (expiryDate is null or expiryDate > ?3) order by fromCurrency, toCurrency", 
                   CurrencyExchangeRate.STATUS_ACTIVE, today, today).list();
    }

    public List<CurrencyExchangeRate> findAllValidRates(LocalDate date) {
        return find("status = ?1 and effectiveDate <= ?2 and (expiryDate is null or expiryDate > ?3) order by fromCurrency, toCurrency", 
                   CurrencyExchangeRate.STATUS_ACTIVE, date, date).list();
    }

    // Count methods - RLS filters by tenant automatically
    
    public long countByStatus(String status) {
        return count("status = ?1", status);
    }

    public long countValidRates() {
        LocalDate today = LocalDate.now();
        return count("status = ?1 and effectiveDate <= ?2 and (expiryDate is null or expiryDate > ?3)", 
                    CurrencyExchangeRate.STATUS_ACTIVE, today, today);
    }

    public long countByCurrency(String currency) {
        return count("fromCurrency = ?1 or toCurrency = ?1", currency);
    }

    // Existence checks - RLS filters by tenant automatically
    
    public boolean existsById(String id) {
        return count("objectID.id = ?1", id) > 0;
    }

    public boolean existsValidRate(String fromCurrency, String toCurrency) {
        LocalDate today = LocalDate.now();
        return count("fromCurrency = ?1 and toCurrency = ?2 and status = ?3 and effectiveDate <= ?4 and (expiryDate is null or expiryDate > ?5)", 
                    fromCurrency, toCurrency, CurrencyExchangeRate.STATUS_ACTIVE, today, today) > 0;
    }

    // Utility methods for conversion
    
    public BigDecimal convertAmount(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null || fromCurrency == null || toCurrency == null) {
            return BigDecimal.ZERO;
        }
        
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }
        
        Optional<CurrencyExchangeRate> rate = findValidRate(fromCurrency, toCurrency);
        return rate.map(r -> r.convert(amount)).orElse(BigDecimal.ZERO);
    }

    public BigDecimal convertAmount(BigDecimal amount, String fromCurrency, String toCurrency, LocalDate date) {
        if (amount == null || fromCurrency == null || toCurrency == null) {
            return BigDecimal.ZERO;
        }
        
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }
        
        Optional<CurrencyExchangeRate> rate = findValidRate(fromCurrency, toCurrency, date);
        return rate.map(r -> r.convert(amount)).orElse(BigDecimal.ZERO);
    }
}

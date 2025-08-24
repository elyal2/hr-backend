package com.humanrsc.services;

import com.humanrsc.datamodel.abstraction.ObjectID;
import com.humanrsc.datamodel.entities.CurrencyExchangeRate;
import com.humanrsc.datamodel.repo.CurrencyExchangeRateRepository;
import com.humanrsc.config.ThreadLocalStorage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CurrencyService {

    @Inject
    CurrencyExchangeRateRepository currencyExchangeRateRepository;

    // ========== CRUD OPERATIONS ==========

    @Transactional
    public CurrencyExchangeRate createExchangeRate(CurrencyExchangeRate rate) {
        if (rate.getObjectID() == null) {
            String id = UUID.randomUUID().toString();
            String tenantID = ThreadLocalStorage.getTenantID();
            rate.setObjectID(ObjectID.of(id, tenantID));
        }
        
        // Validate required fields
        if (rate.getFromCurrency() == null || rate.getToCurrency() == null) {
            throw new IllegalArgumentException("From currency and to currency are required");
        }
        
        if (rate.getExchangeRate() == null || rate.getExchangeRate().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Exchange rate must be greater than zero");
        }
        
        if (rate.getEffectiveDate() == null) {
            rate.setEffectiveDate(LocalDate.now());
        }
        
        currencyExchangeRateRepository.persist(rate);
        return rate;
    }

    public Optional<CurrencyExchangeRate> findById(String id) {
        return currencyExchangeRateRepository.findById(id);
    }

    public List<CurrencyExchangeRate> findAllValidRates() {
        return currencyExchangeRateRepository.findAllValidRates();
    }

    public List<CurrencyExchangeRate> findAllValidRates(LocalDate date) {
        return currencyExchangeRateRepository.findAllValidRates(date);
    }

    public List<CurrencyExchangeRate> findValidRates(String fromCurrency) {
        return currencyExchangeRateRepository.findValidRates(fromCurrency);
    }

    public List<CurrencyExchangeRate> findValidRates(String fromCurrency, LocalDate date) {
        return currencyExchangeRateRepository.findValidRates(fromCurrency, date);
    }

    public Optional<CurrencyExchangeRate> findValidRate(String fromCurrency, String toCurrency) {
        return currencyExchangeRateRepository.findValidRate(fromCurrency, toCurrency);
    }

    public Optional<CurrencyExchangeRate> findValidRate(String fromCurrency, String toCurrency, LocalDate date) {
        return currencyExchangeRateRepository.findValidRate(fromCurrency, toCurrency, date);
    }

    @Transactional
    public CurrencyExchangeRate updateExchangeRate(String id, CurrencyExchangeRate updatedRate) {
        Optional<CurrencyExchangeRate> existing = currencyExchangeRateRepository.findById(id);
        if (existing.isPresent()) {
            CurrencyExchangeRate rate = existing.get();
            
            if (updatedRate.getFromCurrency() != null) {
                rate.setFromCurrency(updatedRate.getFromCurrency());
            }
            if (updatedRate.getToCurrency() != null) {
                rate.setToCurrency(updatedRate.getToCurrency());
            }
            if (updatedRate.getExchangeRate() != null) {
                rate.setExchangeRate(updatedRate.getExchangeRate());
            }
            if (updatedRate.getEffectiveDate() != null) {
                rate.setEffectiveDate(updatedRate.getEffectiveDate());
            }
            if (updatedRate.getExpiryDate() != null) {
                rate.setExpiryDate(updatedRate.getExpiryDate());
            }
            if (updatedRate.getSource() != null) {
                rate.setSource(updatedRate.getSource());
            }
            if (updatedRate.getNotes() != null) {
                rate.setNotes(updatedRate.getNotes());
            }
            if (updatedRate.getStatus() != null) {
                rate.setStatus(updatedRate.getStatus());
            }
            
            rate.updateTimestamp();
            return currencyExchangeRateRepository.getEntityManager().merge(rate);
        }
        throw new IllegalArgumentException("Exchange rate not found with id: " + id);
    }

    @Transactional
    public boolean deleteExchangeRate(String id) {
        Optional<CurrencyExchangeRate> rate = currencyExchangeRateRepository.findById(id);
        if (rate.isPresent()) {
            rate.get().deactivate();
            currencyExchangeRateRepository.getEntityManager().merge(rate.get());
            return true;
        }
        return false;
    }

    // ========== CONVERSION METHODS ==========

    public BigDecimal convertAmount(BigDecimal amount, String fromCurrency, String toCurrency) {
        return currencyExchangeRateRepository.convertAmount(amount, fromCurrency, toCurrency);
    }

    public BigDecimal convertAmount(BigDecimal amount, String fromCurrency, String toCurrency, LocalDate date) {
        return currencyExchangeRateRepository.convertAmount(amount, fromCurrency, toCurrency, date);
    }

    // ========== UTILITY METHODS ==========

    public boolean existsValidRate(String fromCurrency, String toCurrency) {
        return currencyExchangeRateRepository.existsValidRate(fromCurrency, toCurrency);
    }

    public long countValidRates() {
        return currencyExchangeRateRepository.countValidRates();
    }

    public long countByCurrency(String currency) {
        return currencyExchangeRateRepository.countByCurrency(currency);
    }
}

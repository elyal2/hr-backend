package com.humanrsc.services;

import com.humanrsc.datamodel.abstraction.ObjectID;
import com.humanrsc.datamodel.entities.Account;
import com.humanrsc.datamodel.repo.AccountRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AccountService {

    private static final Logger LOG = Logger.getLogger(AccountService.class);

    @Inject
    AccountRepository accountRepository;

    @Inject
    TenantContextService tenantContextService;

    /**
     * Create new account
     */
    @Transactional
    public Account createAccount(String name, String surname, String email, String tenantId) {
        // Validate email doesn't exist
        if (accountRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        // Generate new ID
        String accountId = UUID.randomUUID().toString();

        // Create account
        Account account = new Account();
        account.setObjectID(ObjectID.of(accountId, tenantId));
        account.setName(name);
        account.setSurname(surname);
        account.setEmail(email);
        account.setStatus(Account.STATUS_FREE);
        account.setDateRegistered(LocalDateTime.now());

        accountRepository.persist(account);

        LOG.infof("Created account: %s for tenant: %s", accountId, tenantId);
        return account;
    }

    /**
     * Find account by ID
     */
    public Optional<Account> findById(String id, String tenantId) {
        return accountRepository.findById(id, tenantId);
    }

    /**
     * Find account by email
     */
    public Optional<Account> findByEmail(String email) {
        return accountRepository.findByEmail(email);
    }

    /**
     * Get all accounts for current tenant
     */
    public List<Account> getAllAccounts() {
        return accountRepository.findAllByTenant();
    }

    /**
     * Update account status
     */
    @Transactional
    public Account updateStatus(String id, String tenantId, Integer newStatus) {
        Optional<Account> accountOpt = accountRepository.findById(id, tenantId);
        
        if (accountOpt.isEmpty()) {
            throw new IllegalArgumentException("Account not found: " + id);
        }

        Account account = accountOpt.get();
        account.setStatus(newStatus);
        account.setDateStatusUpdate(LocalDateTime.now());

        accountRepository.persist(account);

        LOG.infof("Updated account status: %s to %d", id, newStatus);
        return account;
    }

    /**
     * Set extended attribute
     */
    @Transactional
    public Account setAccountAttribute(String id, String tenantId, String key, String value) {
        Optional<Account> accountOpt = accountRepository.findById(id, tenantId);
        
        if (accountOpt.isEmpty()) {
            throw new IllegalArgumentException("Account not found: " + id);
        }

        Account account = accountOpt.get();
        account.setAttribute(key, value);

        accountRepository.persist(account);

        LOG.infof("Set attribute %s=%s for account: %s", key, value, id);
        return account;
    }

    /**
     * Delete account (soft delete - mark as deleted)
     */
    @Transactional
    public void deleteAccount(String id, String tenantId) {
        updateStatus(id, tenantId, Account.STATUS_DELETED);
        LOG.infof("Soft deleted account: %s", id);
    }

    /**
     * Get account statistics
     */
    public AccountStats getAccountStats() {
        long totalAccounts = accountRepository.count();
        long activeAccounts = accountRepository.countByStatus(Account.STATUS_FREE);
        long payAccounts = accountRepository.countByStatus(Account.STATUS_PAY);
        long suspendedAccounts = accountRepository.countByStatus(Account.STATUS_SUSPENDED);
        long deletedAccounts = accountRepository.countByStatus(Account.STATUS_DELETED);

        return new AccountStats(totalAccounts, activeAccounts, payAccounts, suspendedAccounts, deletedAccounts);
    }

    // Inner class for statistics
    public static class AccountStats {
        public final long total;
        public final long active;
        public final long pay;
        public final long suspended;
        public final long deleted;

        public AccountStats(long total, long active, long pay, long suspended, long deleted) {
            this.total = total;
            this.active = active;
            this.pay = pay;
            this.suspended = suspended;
            this.deleted = deleted;
        }
    }
}
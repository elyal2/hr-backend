package com.humanrsc.datamodel.repo;

import com.humanrsc.datamodel.abstraction.ObjectID;
import com.humanrsc.datamodel.entities.Account;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class AccountRepository implements PanacheRepositoryBase<Account, ObjectID> {

    /**
     * Find account by ID within current tenant context
     * RLS will automatically filter by tenant
     */
    public Optional<Account> findById(String id, String tenantId) {
        ObjectID objectId = ObjectID.of(id, tenantId);
        return findByIdOptional(objectId);
    }

    /**
     * Find all accounts for current tenant
     * RLS will automatically filter
     */
    public List<Account> findAllByTenant() {
        return listAll();
    }

    /**
     * Find by email within tenant
     */
    public Optional<Account> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }

    /**
     * Find by status within tenant
     */
    public List<Account> findByStatus(Integer status) {
        return find("status", status).list();
    }

    /**
     * Find active accounts
     */
    public List<Account> findActiveAccounts() {
        return find("status", Account.STATUS_FREE).list();
    }

    /**
     * Count accounts by status
     */
    public long countByStatus(Integer status) {
        return count("status", status);
    }

    /**
     * Check if email exists in tenant
     */
    public boolean existsByEmail(String email) {
        return find("email", email).count() > 0;
    }

    /**
     * Delete by composite ID
     */
    public boolean deleteById(String id, String tenantId) {
        ObjectID objectId = ObjectID.of(id, tenantId);
        return deleteById(objectId);
    }
}
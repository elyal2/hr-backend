package com.humanrsc.services;

import com.humanrsc.config.ThreadLocalStorage;
import com.humanrsc.datamodel.abstraction.ObjectID;
import com.humanrsc.datamodel.entities.Tenant;
import com.humanrsc.datamodel.repo.TenantRepository;
import com.humanrsc.datamodel.repo.UserRepository;
import com.humanrsc.security.JwtTokenUtils;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class TenantService {

    

    @Inject TenantRepository tenantRepository;
    @Inject UserRepository userRepository;
    @Inject JwtTokenUtils jwtTokenUtils;

    @Transactional
    public Tenant createTenant(Tenant tenant) {
        if (tenant.getObjectID() == null) {
            String tenantID = UUID.randomUUID().toString();
            tenant.setObjectID(ObjectID.of(tenantID, tenantID));
        }
        if (tenant.getDomain() != null && tenantRepository.existsByDomain(tenant.getDomain())) {
            throw new IllegalArgumentException("Domain already exists: " + tenant.getDomain());
        }
        return tenantRepository.createTenant(tenant, tenant.getObjectID().getTenantID());
    }

    public Optional<Tenant> findById(ObjectID objectID) { return tenantRepository.findByObjectID(objectID); }
    public Optional<Tenant> findById(String id) { return tenantRepository.findById(id); }
    public Optional<Tenant> findByDomain(String domain) { return tenantRepository.findByDomain(domain); }
    public List<Tenant> findAllTenants() { return tenantRepository.findAllOrdered(); }
    public List<Tenant> findByStatus(String status) { return tenantRepository.findByStatus(status); }
    public List<Tenant> findBySubscriptionPlan(String subscriptionPlan) { return tenantRepository.findBySubscriptionPlan(subscriptionPlan); }

    // Overloads con paginación para datasets grandes
    public List<Tenant> findAllTenants(int page, int size) {
        PanacheQuery<Tenant> q = tenantRepository.findAllOrderedQuery();
        return q.page(Page.of(page, size)).list();
    }

    public List<Tenant> findByStatus(String status, int page, int size) {
        PanacheQuery<Tenant> q = tenantRepository.findByStatusQuery(status);
        return q.page(Page.of(page, size)).list();
    }

    public List<Tenant> findBySubscriptionPlan(String subscriptionPlan, int page, int size) {
        PanacheQuery<Tenant> q = tenantRepository.findBySubscriptionPlanQuery(subscriptionPlan);
        return q.page(Page.of(page, size)).list();
    }

    @Transactional
    public Tenant updateTenant(Tenant tenant) {
        if (tenant.getDomain() != null) {
            Optional<Tenant> existingTenant = tenantRepository.findByDomain(tenant.getDomain());
            if (existingTenant.isPresent() && !existingTenant.get().getObjectID().equals(tenant.getObjectID())) {
                throw new IllegalArgumentException("Domain already exists: " + tenant.getDomain());
            }
        }
        return tenantRepository.updateTenant(tenant);
    }

    @Transactional public boolean activateTenant(ObjectID objectID) { return tenantRepository.activateTenant(objectID); }
    @Transactional public boolean suspendTenant(ObjectID objectID) { return tenantRepository.suspendTenant(objectID); }
    @Transactional public boolean deactivateTenant(ObjectID objectID) { return tenantRepository.deactivateTenant(objectID); }

    public boolean existsById(String id) { return tenantRepository.existsById(id); }
    public boolean isDomainInUse(String domain) { return tenantRepository.existsByDomain(domain); }
    public long countByStatus(String status) { return tenantRepository.countByStatus(status); }

    public Optional<Tenant> getCurrentTenant() {
        String tenantID = ThreadLocalStorage.getTenantID();
        if (tenantID == null) { return Optional.empty(); }
        Optional<Tenant> byTenantId = tenantRepository.findByTenantId(tenantID);
        if (byTenantId.isPresent()) return byTenantId;
        String domain = jwtTokenUtils.extractTenantDomainFromJWT();
        if (domain != null) {
            Optional<Tenant> byDomain = tenantRepository.findByDomain(domain);
            return byDomain;
        }
        return Optional.empty();
    }

    public boolean isCurrentTenantActive() {
        Optional<Tenant> currentTenant = getCurrentTenant();
        return currentTenant.isPresent() && currentTenant.get().isActive();
    }

    public boolean canCreateMoreUsers() {
        Optional<Tenant> currentTenant = getCurrentTenant();
        if (currentTenant.isEmpty()) { return false; }
        Tenant tenant = currentTenant.get();
        if (tenant.getMaxUsers() == null) { return true; }
        long currentUserCount = userRepository.countByTenant(tenant.getObjectID().getTenantID());
        return currentUserCount < tenant.getMaxUsers();
    }

    // Re-added: compute stats for a given tenant using user repository
    public TenantStats getTenantStats(ObjectID tenantObjectID) {
        String tenantID = tenantObjectID.getTenantID();
        long totalUsers = userRepository.countByTenant(tenantID);
        long activeUsers = userRepository.findByStatusAndTenant("active", tenantID).size();
        long pendingUsers = userRepository.findByStatusAndTenant("pending", tenantID).size();
        long suspendedUsers = userRepository.findByStatusAndTenant("suspended", tenantID).size();
        return new TenantStats(totalUsers, activeUsers, pendingUsers, suspendedUsers);
    }

    public static class TenantStats {
        private final long totalUsers;
        private final long activeUsers;
        private final long pendingUsers;
        private final long suspendedUsers;
        public TenantStats(long totalUsers, long activeUsers, long pendingUsers, long suspendedUsers) {
            this.totalUsers = totalUsers; this.activeUsers = activeUsers; this.pendingUsers = pendingUsers; this.suspendedUsers = suspendedUsers;
        }
        public long getTotalUsers() { return totalUsers; }
        public long getActiveUsers() { return activeUsers; }
        public long getPendingUsers() { return pendingUsers; }
        public long getSuspendedUsers() { return suspendedUsers; }
    }
}

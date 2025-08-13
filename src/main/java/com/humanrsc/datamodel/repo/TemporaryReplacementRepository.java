package com.humanrsc.datamodel.repo;

import com.humanrsc.datamodel.abstraction.ObjectID;
import com.humanrsc.datamodel.entities.TemporaryReplacement;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TemporaryReplacementRepository implements PanacheRepository<TemporaryReplacement> {

    // Basic CRUD operations
    @Transactional
    public TemporaryReplacement createTemporaryReplacement(TemporaryReplacement replacement, String tenantID) {
        persist(replacement);
        return replacement;
    }

    public Optional<TemporaryReplacement> findByObjectID(ObjectID objectID) {
        return find("objectID = ?1", objectID).firstResultOptional();
    }

    public Optional<TemporaryReplacement> findById(String id) {
        String tenantID = getCurrentTenantID();
        return find("objectID.id = ?1 and objectID.tenantID = ?2", id, tenantID).firstResultOptional();
    }

    @Transactional
    public TemporaryReplacement updateTemporaryReplacement(TemporaryReplacement replacement) {
        replacement.updateTimestamp();
        return getEntityManager().merge(replacement);
    }

    // Query methods
    public List<TemporaryReplacement> findByOriginalEmployee(String employeeId) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and originalEmployee.objectID.id = ?2 order by startDate desc", 
                   tenantID, employeeId).list();
    }

    public List<TemporaryReplacement> findByReplacementEmployee(String employeeId) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and replacementEmployee.objectID.id = ?2 order by startDate desc", 
                   tenantID, employeeId).list();
    }

    public List<TemporaryReplacement> findByPosition(String positionId) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and position.objectID.id = ?2 order by startDate desc", 
                   tenantID, positionId).list();
    }

    public List<TemporaryReplacement> findActiveReplacements() {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 order by startDate desc", 
                   tenantID, TemporaryReplacement.STATUS_ACTIVE).list();
    }

    public List<TemporaryReplacement> findCurrentReplacements() {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 and (endDate is null or endDate >= ?3) order by startDate desc", 
                   tenantID, TemporaryReplacement.STATUS_ACTIVE, LocalDate.now()).list();
    }

    public List<TemporaryReplacement> findByStatus(String status) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 order by startDate desc", tenantID, status).list();
    }

    public List<TemporaryReplacement> findByDateRange(LocalDate startDate, LocalDate endDate) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and startDate <= ?2 and (endDate is null or endDate >= ?3) order by startDate desc", 
                   tenantID, endDate, startDate).list();
    }

    // Pagination methods
    public List<TemporaryReplacement> findActiveReplacementsPage(int page, int size) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 order by startDate desc", 
                   tenantID, TemporaryReplacement.STATUS_ACTIVE)
               .page(io.quarkus.panache.common.Page.of(page, size))
               .list();
    }

    public List<TemporaryReplacement> findByStatusPage(String status, int page, int size) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 order by startDate desc", tenantID, status)
               .page(io.quarkus.panache.common.Page.of(page, size))
               .list();
    }

    // Count methods
    public long countByStatus(String status) {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and status = ?2", tenantID, status);
    }

    public long countActiveReplacements() {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and status = ?2", tenantID, TemporaryReplacement.STATUS_ACTIVE);
    }

    public long countCurrentReplacements() {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and status = ?2 and (endDate is null or endDate >= ?3)", 
                    tenantID, TemporaryReplacement.STATUS_ACTIVE, LocalDate.now());
    }

    public long countByOriginalEmployee(String employeeId) {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and originalEmployee.objectID.id = ?2", tenantID, employeeId);
    }

    public long countByReplacementEmployee(String employeeId) {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and replacementEmployee.objectID.id = ?2", tenantID, employeeId);
    }

    public long countTotal() {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1", tenantID);
    }

    // Existence checks
    public boolean existsById(String id) {
        String tenantID = getCurrentTenantID();
        return count("objectID.id = ?1 and objectID.tenantID = ?2", id, tenantID) > 0;
    }

    public boolean hasActiveReplacement(String employeeId) {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and status = ?2 and originalEmployee.objectID.id = ?3 and (endDate is null or endDate >= ?4)", 
                    tenantID, TemporaryReplacement.STATUS_ACTIVE, employeeId, LocalDate.now()) > 0;
    }

    // Business logic methods
    public List<TemporaryReplacement> findExpiredReplacements() {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 and endDate < ?3 order by endDate desc", 
                   tenantID, TemporaryReplacement.STATUS_ACTIVE, LocalDate.now()).list();
    }

    public List<TemporaryReplacement> findReplacementsByReason(String reason) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and reason like ?2 order by startDate desc", 
                   tenantID, "%" + reason + "%").list();
    }

    // Utility method to get current tenant ID
    private String getCurrentTenantID() {
        return com.humanrsc.config.ThreadLocalStorage.getTenantID();
    }
}

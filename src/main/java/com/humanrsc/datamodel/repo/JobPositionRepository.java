package com.humanrsc.datamodel.repo;

import com.humanrsc.datamodel.abstraction.ObjectID;
import com.humanrsc.datamodel.entities.JobPosition;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class JobPositionRepository implements PanacheRepository<JobPosition> {

    // Basic CRUD operations
    @Transactional
    public JobPosition createJobPosition(JobPosition position, String tenantID) {
        persist(position);
        return position;
    }

    public Optional<JobPosition> findByObjectID(ObjectID objectID) {
        return find("objectID = ?1", objectID).firstResultOptional();
    }

    public Optional<JobPosition> findById(String id) {
        String tenantID = getCurrentTenantID();
        return find("objectID.id = ?1 and objectID.tenantID = ?2", id, tenantID).firstResultOptional();
    }

    public Optional<JobPosition> findByJobCode(String jobCode) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and jobCode = ?2", tenantID, jobCode).firstResultOptional();
    }

    @Transactional
    public JobPosition updateJobPosition(JobPosition position) {
        position.updateTimestamp();
        return getEntityManager().merge(position);
    }

    @Transactional
    public boolean activateJobPosition(ObjectID objectID) {
        return update("status = ?1, dateUpdated = ?2 where objectID = ?3", 
                     JobPosition.STATUS_ACTIVE, java.time.LocalDateTime.now(), objectID) > 0;
    }

    @Transactional
    public boolean deactivateJobPosition(ObjectID objectID) {
        return update("status = ?1, dateUpdated = ?2 where objectID = ?3", 
                     JobPosition.STATUS_INACTIVE, java.time.LocalDateTime.now(), objectID) > 0;
    }

    @Transactional
    public boolean deleteJobPosition(ObjectID objectID) {
        return update("status = ?1, dateUpdated = ?2 where objectID = ?3", 
                     JobPosition.STATUS_DELETED, java.time.LocalDateTime.now(), objectID) > 0;
    }

    // Query methods for large datasets
    public List<JobPosition> findAllActive() {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 order by title", 
                   tenantID, JobPosition.STATUS_ACTIVE).list();
    }

    public List<JobPosition> findAllOrdered() {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 order by title", tenantID).list();
    }

    public List<JobPosition> findByStatus(String status) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 order by title", tenantID, status).list();
    }

    public List<JobPosition> findByUnit(String unitId) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 and unit.objectID.id = ?3 order by title", 
                   tenantID, JobPosition.STATUS_ACTIVE, unitId).list();
    }

    public List<JobPosition> findByUnit(ObjectID unitObjectID) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 and unit.objectID = ?3 order by title", 
                   tenantID, JobPosition.STATUS_ACTIVE, unitObjectID).list();
    }

    public List<JobPosition> findByCategory(String categoryId) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 and category.objectID.id = ?3 order by title", 
                   tenantID, JobPosition.STATUS_ACTIVE, categoryId).list();
    }

    public List<JobPosition> findByCategory(ObjectID categoryObjectID) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 and category.objectID = ?3 order by title", 
                   tenantID, JobPosition.STATUS_ACTIVE, categoryObjectID).list();
    }

    public List<JobPosition> findByHierarchicalLevel(Integer level) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 and hierarchicalLevel = ?3 order by title", 
                   tenantID, JobPosition.STATUS_ACTIVE, level).list();
    }

    public List<JobPosition> findByLevelRange(Integer minLevel, Integer maxLevel) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 and hierarchicalLevel >= ?3 and hierarchicalLevel <= ?4 order by hierarchicalLevel, title", 
                   tenantID, JobPosition.STATUS_ACTIVE, minLevel, maxLevel).list();
    }

    public List<JobPosition> findByTitleContaining(String titleFragment) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 and title like ?3 order by title", 
                   tenantID, JobPosition.STATUS_ACTIVE, "%" + titleFragment + "%").list();
    }

    // Pagination methods for large datasets
    public List<JobPosition> findActivePage(int page, int size) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 order by title", 
                   tenantID, JobPosition.STATUS_ACTIVE)
               .page(io.quarkus.panache.common.Page.of(page, size))
               .list();
    }

    public List<JobPosition> findAllOrderedPage(int page, int size) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 order by title", tenantID)
               .page(io.quarkus.panache.common.Page.of(page, size))
               .list();
    }

    public List<JobPosition> findByStatusPage(String status, int page, int size) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 order by title", tenantID, status)
               .page(io.quarkus.panache.common.Page.of(page, size))
               .list();
    }

    public List<JobPosition> findByUnitPage(String unitId, int page, int size) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 and unit.objectID.id = ?3 order by title", 
                   tenantID, JobPosition.STATUS_ACTIVE, unitId)
               .page(io.quarkus.panache.common.Page.of(page, size))
               .list();
    }

    public List<JobPosition> findByCategoryPage(String categoryId, int page, int size) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 and category.objectID.id = ?3 order by title", 
                   tenantID, JobPosition.STATUS_ACTIVE, categoryId)
               .page(io.quarkus.panache.common.Page.of(page, size))
               .list();
    }

    public List<JobPosition> findByHierarchicalLevelPage(Integer level, int page, int size) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 and hierarchicalLevel = ?3 order by hierarchicalLevel, title", 
                   tenantID, JobPosition.STATUS_ACTIVE, level)
               .page(io.quarkus.panache.common.Page.of(page, size))
               .list();
    }

    // Query objects for complex operations
    public io.quarkus.hibernate.orm.panache.PanacheQuery<JobPosition> findAllActiveQuery() {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 order by title", 
                   tenantID, JobPosition.STATUS_ACTIVE);
    }

    public io.quarkus.hibernate.orm.panache.PanacheQuery<JobPosition> findAllOrderedQuery() {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 order by title", tenantID);
    }

    public io.quarkus.hibernate.orm.panache.PanacheQuery<JobPosition> findByStatusQuery(String status) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 order by title", tenantID, status);
    }

    public io.quarkus.hibernate.orm.panache.PanacheQuery<JobPosition> findByUnitQuery(String unitId) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 and unit.objectID.id = ?3 order by title", 
                   tenantID, JobPosition.STATUS_ACTIVE, unitId);
    }

    public io.quarkus.hibernate.orm.panache.PanacheQuery<JobPosition> findByCategoryQuery(String categoryId) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 and category.objectID.id = ?3 order by title", 
                   tenantID, JobPosition.STATUS_ACTIVE, categoryId);
    }

    public io.quarkus.hibernate.orm.panache.PanacheQuery<JobPosition> findByHierarchicalLevelQuery(Integer level) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 and hierarchicalLevel = ?3 order by hierarchicalLevel, title", 
                   tenantID, JobPosition.STATUS_ACTIVE, level);
    }

    // Count methods
    public long countByStatus(String status) {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and status = ?2", tenantID, status);
    }

    public long countActive() {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and status = ?2", tenantID, JobPosition.STATUS_ACTIVE);
    }

    public long countTotal() {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1", tenantID);
    }

    public long countByUnit(ObjectID unitObjectID) {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and unit.objectID = ?2", tenantID, unitObjectID);
    }

    public long countByCategory(ObjectID categoryObjectID) {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and category.objectID = ?2", tenantID, categoryObjectID);
    }

    public long countByHierarchicalLevel(Integer level) {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and hierarchicalLevel = ?2", tenantID, level);
    }

    // Existence checks
    public boolean existsById(String id) {
        String tenantID = getCurrentTenantID();
        return count("objectID.id = ?1 and objectID.tenantID = ?2", id, tenantID) > 0;
    }

    public boolean existsByJobCode(String jobCode) {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and jobCode = ?2", tenantID, jobCode) > 0;
    }

    public boolean existsByTitle(String title) {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and title = ?2", tenantID, title) > 0;
    }

    // Business logic methods
    public List<JobPosition> findVacantPositions() {
        String tenantID = getCurrentTenantID();
        // Find positions that don't have current employee assignments
        return find("objectID.tenantID = ?1 and status = ?2 and objectID not in " +
                   "(select ea.position.objectID from EmployeeAssignment ea " +
                   "where ea.endDate is null) order by title", 
                   tenantID, JobPosition.STATUS_ACTIVE).list();
    }

    public List<JobPosition> findPositionsByUnitAndCategory(String unitId, String categoryId) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 and unit.objectID.id = ?3 and category.objectID.id = ?4 order by title", 
                   tenantID, JobPosition.STATUS_ACTIVE, unitId, categoryId).list();
    }

    public List<JobPosition> findPositionsByUnitAndHierarchicalLevel(String unitId, Integer level) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 and unit.objectID.id = ?3 and hierarchicalLevel = ?4 order by hierarchicalLevel, title", 
                   tenantID, JobPosition.STATUS_ACTIVE, unitId, level).list();
    }

    // Utility method to get current tenant ID
    private String getCurrentTenantID() {
        return com.humanrsc.config.ThreadLocalStorage.getTenantID();
    }
}

package com.humanrsc.datamodel.repo;

import com.humanrsc.datamodel.abstraction.ObjectID;
import com.humanrsc.datamodel.entities.PositionCategory;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PositionCategoryRepository implements PanacheRepositoryBase<PositionCategory, ObjectID> {

    // Usar métodos estándar de PanacheRepositoryBase

    public Optional<PositionCategory> findByObjectID(ObjectID objectID) {
        return find("objectID = ?1", objectID).firstResultOptional();
    }

    public Optional<PositionCategory> findById(String id) {
        String tenantID = getCurrentTenantID();
        return find("objectID.id = ?1 and objectID.tenantID = ?2", id, tenantID).firstResultOptional();
    }

    // Usar métodos estándar de PanacheRepositoryBase

    // Query methods for large datasets
    public List<PositionCategory> findAllActive() {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 order by name", tenantID, PositionCategory.STATUS_ACTIVE).list();
    }

    public List<PositionCategory> findActivePage(int page, int size) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 order by name", tenantID, PositionCategory.STATUS_ACTIVE)
                .page(page, size).list();
    }

    public List<PositionCategory> findByStatus(String status) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 order by name", tenantID, status).list();
    }

    public List<PositionCategory> findByName(String name) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and name ILIKE ?2 order by name", tenantID, "%" + name + "%").list();
    }

    // Count methods
    public long countByStatus(String status) {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and status = ?2", tenantID, status);
    }

    public long countActive() {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and status = ?2", tenantID, PositionCategory.STATUS_ACTIVE);
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

    public boolean existsByName(String name) {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and name = ?2", tenantID, name) > 0;
    }

    // Business logic methods
    public long countPositionsByCategory(String categoryId) {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and category.objectID.id = ?2 and status = ?3", 
                    tenantID, categoryId, PositionCategory.STATUS_ACTIVE);
    }

    @Transactional
    public boolean deletePositionCategory(ObjectID objectID) {
        String tenantID = getCurrentTenantID();
        long deleted = delete("objectID = ?1 and objectID.tenantID = ?2", objectID, tenantID);
        return deleted > 0;
    }

    private String getCurrentTenantID() {
        return com.humanrsc.config.ThreadLocalStorage.getTenantID();
    }
}

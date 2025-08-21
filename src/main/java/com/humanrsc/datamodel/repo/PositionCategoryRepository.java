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

    // Basic finder methods - RLS handles tenant filtering automatically

    public Optional<PositionCategory> findByObjectID(ObjectID objectID) {
        return find("objectID = ?1", objectID).firstResultOptional();
    }

    public Optional<PositionCategory> findById(String id) {
        return find("objectID.id = ?1", id).firstResultOptional();
    }

    // Query methods - RLS filters by tenant automatically
    
    public List<PositionCategory> findAllActive() {
        return find("status = ?1 order by name", PositionCategory.STATUS_ACTIVE).list();
    }

    public List<PositionCategory> findActivePage(int page, int size) {
        return find("status = ?1 order by name", PositionCategory.STATUS_ACTIVE)
                .page(page, size).list();
    }

    public List<PositionCategory> findByStatus(String status) {
        return find("status = ?1 order by name", status).list();
    }

    public List<PositionCategory> findByName(String name) {
        return find("name ILIKE ?1 order by name", "%" + name + "%").list();
    }

    // Count methods - RLS filters by tenant automatically
    
    public long countByStatus(String status) {
        return count("status = ?1", status);
    }

    public long countActive() {
        return count("status = ?1", PositionCategory.STATUS_ACTIVE);
    }

    public long countTotal() {
        return count(); // RLS handles tenant filtering
    }

    // Existence checks - RLS filters by tenant automatically
    
    public boolean existsById(String id) {
        return count("objectID.id = ?1", id) > 0;
    }

    public boolean existsByName(String name) {
        return count("name = ?1", name) > 0;
    }

    // Business logic methods - RLS filters by tenant automatically
    
    public long countPositionsByCategory(String categoryId) {
        return count("category.objectID.id = ?1 and status = ?2", 
                    categoryId, PositionCategory.STATUS_ACTIVE);
    }

    @Transactional
    public boolean deletePositionCategory(ObjectID objectID) {
        long deleted = delete("objectID = ?1", objectID);
        return deleted > 0;
    }
}
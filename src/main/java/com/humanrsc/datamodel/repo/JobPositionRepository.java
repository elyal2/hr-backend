package com.humanrsc.datamodel.repo;

import com.humanrsc.datamodel.abstraction.ObjectID;
import com.humanrsc.datamodel.entities.JobPosition;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class JobPositionRepository implements PanacheRepositoryBase<JobPosition, ObjectID> {

    // Basic finder methods - RLS handles tenant filtering automatically

    public Optional<JobPosition> findByObjectID(ObjectID objectID) {
        return find("objectID = ?1", objectID).firstResultOptional();
    }

    public Optional<JobPosition> findById(String id) {
        return find("objectID.id = ?1", id).firstResultOptional();
    }

    public Optional<JobPosition> findByJobCode(String jobCode) {
        return find("jobCode = ?1", jobCode).firstResultOptional();
    }

    // Status change operations - RLS handles tenant filtering automatically
    
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

    // Query methods - RLS filters by tenant automatically
    
    public List<JobPosition> findAllActive() {
        return find("status = ?1 order by title", JobPosition.STATUS_ACTIVE).list();
    }

    public List<JobPosition> findByStatus(String status) {
        return find("status = ?1 order by title", status).list();
    }

    public List<JobPosition> findByUnit(String unitId) {
        return find("status = ?1 and unit.objectID.id = ?2 order by title", 
                   JobPosition.STATUS_ACTIVE, unitId).list();
    }

    public List<JobPosition> findByUnit(ObjectID unitObjectID) {
        return find("status = ?1 and unit.objectID = ?2 order by title", 
                   JobPosition.STATUS_ACTIVE, unitObjectID).list();
    }

    public List<JobPosition> findByCategory(String categoryId) {
        return find("status = ?1 and category.objectID.id = ?2 order by title", 
                   JobPosition.STATUS_ACTIVE, categoryId).list();
    }

    public List<JobPosition> findByCategory(ObjectID categoryObjectID) {
        return find("status = ?1 and category.objectID = ?2 order by title", 
                   JobPosition.STATUS_ACTIVE, categoryObjectID).list();
    }

    public List<JobPosition> findByHierarchicalLevel(Integer level) {
        return find("status = ?1 and hierarchicalLevel = ?2 order by title", 
                   JobPosition.STATUS_ACTIVE, level).list();
    }

    public List<JobPosition> findByLevelRange(Integer minLevel, Integer maxLevel) {
        return find("status = ?1 and hierarchicalLevel >= ?2 and hierarchicalLevel <= ?3 order by hierarchicalLevel, title", 
                   JobPosition.STATUS_ACTIVE, minLevel, maxLevel).list();
    }

    // Pagination methods - RLS filters by tenant automatically
    
    public List<JobPosition> findActivePage(int page, int size) {
        return find("status = ?1 order by title", JobPosition.STATUS_ACTIVE)
               .page(io.quarkus.panache.common.Page.of(page, size))
               .list();
    }

    // Count methods - RLS filters by tenant automatically
    
    public long countByStatus(String status) {
        return count("status = ?1", status);
    }

    public long countActive() {
        return count("status = ?1", JobPosition.STATUS_ACTIVE);
    }

    public long countTotal() {
        return count(); // RLS handles tenant filtering
    }

    public long countByUnit(ObjectID unitObjectID) {
        return count("unit.objectID = ?1", unitObjectID);
    }

    public long countByCategory(ObjectID categoryObjectID) {
        return count("category.objectID = ?1", categoryObjectID);
    }

    public long countByHierarchicalLevel(Integer level) {
        return count("hierarchicalLevel = ?1", level);
    }

    // Existence checks - RLS filters by tenant automatically
    
    public boolean existsById(String id) {
        return count("objectID.id = ?1", id) > 0;
    }

    public boolean existsByJobCode(String jobCode) {
        return count("jobCode = ?1", jobCode) > 0;
    }

    public boolean existsByTitle(String title) {
        return count("title = ?1", title) > 0;
    }

    // Business logic methods - RLS filters by tenant automatically
    
    public List<JobPosition> findVacantPositions() {
        // Find positions that don't have current employee assignments
        return find("status = ?1 and objectID not in " +
                   "(select ea.position.objectID from EmployeeAssignment ea " +
                   "where ea.endDate is null) order by title", 
                   JobPosition.STATUS_ACTIVE).list();
    }

    public List<JobPosition> findPositionsByUnitAndCategory(String unitId, String categoryId) {
        return find("status = ?1 and unit.objectID.id = ?2 and category.objectID.id = ?3 order by title", 
                   JobPosition.STATUS_ACTIVE, unitId, categoryId).list();
    }

    public List<JobPosition> findPositionsByUnitAndHierarchicalLevel(String unitId, Integer level) {
        return find("status = ?1 and unit.objectID.id = ?2 and hierarchicalLevel = ?3 order by hierarchicalLevel, title", 
                   JobPosition.STATUS_ACTIVE, unitId, level).list();
    }

    // Dynamic filtering methods - RLS handles tenant filtering automatically
    
    public List<JobPosition> findWithFilters(java.util.Map<String, Object> filters, int page, int size) {
        java.util.List<Object> parameters = new java.util.ArrayList<>();
        String query = buildFilterQuery(filters, parameters);
        
        return find(query, parameters.toArray())
               .page(io.quarkus.panache.common.Page.of(page, size))
               .list();
    }
    
    public List<JobPosition> findWithFilters(java.util.Map<String, Object> filters) {
        java.util.List<Object> parameters = new java.util.ArrayList<>();
        String query = buildFilterQuery(filters, parameters);
        
        return find(query, parameters.toArray()).list();
    }
    
    public long countWithFilters(java.util.Map<String, Object> filters) {
        java.util.List<Object> parameters = new java.util.ArrayList<>();
        String query = buildFilterQuery(filters, parameters);
        
        return count(query, parameters.toArray());
    }
    
    // Helper method to build filter query - no tenant filtering needed (RLS handles it)
    private String buildFilterQuery(java.util.Map<String, Object> filters, java.util.List<Object> parameters) {
        StringBuilder queryBuilder = new StringBuilder();
        boolean firstCondition = true;
        
        // Fields that should be case-insensitive (string fields)
        java.util.Set<String> caseInsensitiveFields = java.util.Set.of(
            "title", "description"
        );
        
        // Add filters dynamically
        for (java.util.Map.Entry<String, Object> entry : filters.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();
            
            if (value != null) {
                if (!firstCondition) {
                    queryBuilder.append(" and ");
                }
                firstCondition = false;
                
                if (value instanceof Object[] || value.getClass().isArray()) {
                    // Handle array values (e.g., status in ['active', 'inactive'])
                    Object[] array = (Object[]) value;
                    if (array.length > 0) {
                        if (caseInsensitiveFields.contains(field)) {
                            // Case-insensitive array search
                            queryBuilder.append("LOWER(").append(field).append(") in (");
                            for (int i = 0; i < array.length; i++) {
                                if (i > 0) queryBuilder.append(", ");
                                queryBuilder.append("LOWER(?").append(parameters.size() + i + 1).append(")");
                            }
                            queryBuilder.append(")");
                        } else {
                            // Case-sensitive array search
                            queryBuilder.append(field).append(" in (?");
                            queryBuilder.append(parameters.size() + 1);
                            for (int i = 1; i < array.length; i++) {
                                queryBuilder.append(", ?").append(parameters.size() + i + 1);
                            }
                            queryBuilder.append(")");
                        }
                        for (Object item : array) {
                            parameters.add(item);
                        }
                    }
                } else if (!value.toString().trim().isEmpty()) {
                    // Handle single values
                    if (caseInsensitiveFields.contains(field)) {
                        // Case-insensitive partial search for string fields (LIKE with %)
                        queryBuilder.append("LOWER(").append(field).append(") LIKE LOWER(?").append(parameters.size() + 1).append(")");
                        parameters.add("%" + value + "%");
                    } else {
                        // Case-sensitive exact search for other fields
                        queryBuilder.append(field).append(" = ?").append(parameters.size() + 1);
                        parameters.add(value);
                    }
                }
            }
        }
        
        // If no conditions were added, return a query that matches all records
        if (firstCondition) {
            queryBuilder.append("1 = 1");
        }
        
        queryBuilder.append(" order by title");
        return queryBuilder.toString();
    }
}
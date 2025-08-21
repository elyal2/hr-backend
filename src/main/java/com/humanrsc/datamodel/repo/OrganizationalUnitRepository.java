package com.humanrsc.datamodel.repo;

import com.humanrsc.datamodel.abstraction.ObjectID;
import com.humanrsc.datamodel.entities.OrganizationalUnit;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class OrganizationalUnitRepository implements PanacheRepositoryBase<OrganizationalUnit, ObjectID> {

    // Basic finder methods - RLS handles tenant filtering automatically

    public Optional<OrganizationalUnit> findByObjectID(ObjectID objectID) {
        return find("objectID = ?1", objectID).firstResultOptional();
    }

    public Optional<OrganizationalUnit> findById(String id) {
        return find("objectID.id = ?1", id).firstResultOptional();
    }

    // Query methods - RLS filters by tenant automatically
    
    public List<OrganizationalUnit> findAllActive() {
        return find("status = ?1 order by name", OrganizationalUnit.STATUS_ACTIVE).list();
    }

    public List<OrganizationalUnit> findActivePage(int page, int size) {
        return find("status = ?1 order by name", OrganizationalUnit.STATUS_ACTIVE)
                .page(page, size).list();
    }

    public List<OrganizationalUnit> findRootUnits() {
        return find("status = ?1 and parentUnit is null order by name", 
                   OrganizationalUnit.STATUS_ACTIVE).list();
    }

    public List<OrganizationalUnit> findByParentUnit(String parentUnitId) {
        return find("status = ?1 and parentUnit.objectID.id = ?2 order by name", 
                   OrganizationalUnit.STATUS_ACTIVE, parentUnitId).list();
    }

    public long countByParentUnit(ObjectID parentUnitObjectID) {
        return count("parentUnit.objectID = ?1", parentUnitObjectID);
    }

    public long countActive() {
        return count("status = ?1", OrganizationalUnit.STATUS_ACTIVE);
    }

    // Organizational level methods - RLS filters by tenant automatically
    
    public List<OrganizationalUnit> findByOrganizationalLevel(Integer level) {
        return find("status = ?1 and organizationalLevel = ?2 order by name", 
                   OrganizationalUnit.STATUS_ACTIVE, level).list();
    }

    public List<OrganizationalUnit> findByOrganizationalLevelRange(Integer minLevel, Integer maxLevel) {
        return find("status = ?1 and organizationalLevel between ?2 and ?3 order by organizationalLevel, name", 
                   OrganizationalUnit.STATUS_ACTIVE, minLevel, maxLevel).list();
    }

    public long countByOrganizationalLevel(Integer level) {
        return count("status = ?1 and organizationalLevel = ?2", 
                    OrganizationalUnit.STATUS_ACTIVE, level);
    }

    // Delete operation - RLS handles tenant filtering automatically
    
    @Transactional
    public boolean deleteOrganizationalUnit(ObjectID objectID) {
        long deleted = delete("objectID = ?1", objectID);
        return deleted > 0;
    }

    // Native SQL query for units with counts - RLS handles tenant filtering automatically
    public List<Object[]> getUnitsWithCounts() {
        String sql = """
            SELECT 
                ou.id, ou.tenant_id, ou.name, ou.description, ou.cost_center, 
                ou.location, ou.country, ou.status, ou.date_created, ou.date_updated,
                ou.parent_unit_id, ou.organizational_level,
                COUNT(DISTINCT ea.id) as employee_count,
                COUNT(DISTINCT ea.position_id) as position_count,
                MIN(jp.hierarchical_level) as min_level,
                MAX(jp.hierarchical_level) as max_level
            FROM hr_app.organizational_units ou
            LEFT JOIN hr_app.employee_assignments ea ON ou.id = ea.unit_id 
                AND ou.tenant_id = ea.tenant_id 
                AND ea.end_date IS NULL
            LEFT JOIN hr_app.job_positions jp ON ea.position_id = jp.id 
                AND ea.tenant_id = jp.tenant_id
            WHERE ou.status = 'active'
            GROUP BY ou.id, ou.tenant_id, ou.name, ou.description, ou.cost_center, 
                     ou.location, ou.country, ou.status, ou.date_created, ou.date_updated,
                     ou.parent_unit_id, ou.organizational_level
            ORDER BY ou.organizational_level, ou.name
            """;
        
        @SuppressWarnings("unchecked")
        List<Object[]> result = getEntityManager().createNativeQuery(sql)
                .getResultList();
        return result;
    }

    // Dynamic filtering methods - RLS handles tenant filtering automatically
    
    public List<OrganizationalUnit> findWithFilters(java.util.Map<String, Object> filters, int page, int size) {
        java.util.List<Object> parameters = new java.util.ArrayList<>();
        String query = buildFilterQuery(filters, parameters);
        
        return find(query, parameters.toArray())
               .page(io.quarkus.panache.common.Page.of(page, size))
               .list();
    }
    
    public List<OrganizationalUnit> findWithFilters(java.util.Map<String, Object> filters) {
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
            "name", "description", "location", "country", "costCenter"
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
        
        queryBuilder.append(" order by name");
        return queryBuilder.toString();
    }
}
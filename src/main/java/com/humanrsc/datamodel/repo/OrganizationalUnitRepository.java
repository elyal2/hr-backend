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

    // Basic CRUD operations - usar métodos estándar de Panache

    public Optional<OrganizationalUnit> findByObjectID(ObjectID objectID) {
        return find("objectID = ?1", objectID).firstResultOptional();
    }

    public Optional<OrganizationalUnit> findById(String id) {
        String tenantID = getCurrentTenantID();
        return find("objectID.id = ?1 and objectID.tenantID = ?2", id, tenantID).firstResultOptional();
    }

    // Usar métodos estándar de PanacheRepositoryBase

    // Query methods for large datasets
    public List<OrganizationalUnit> findAllActive() {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 order by name", tenantID, OrganizationalUnit.STATUS_ACTIVE).list();
    }

    public List<OrganizationalUnit> findActivePage(int page, int size) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 order by name", tenantID, OrganizationalUnit.STATUS_ACTIVE)
                .page(page, size).list();
    }

    public List<OrganizationalUnit> findRootUnits() {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 and parentUnit is null order by name", 
                   tenantID, OrganizationalUnit.STATUS_ACTIVE).list();
    }

    public List<OrganizationalUnit> findByParentUnit(String parentUnitId) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 and parentUnit.objectID.id = ?3 order by name", 
                   tenantID, OrganizationalUnit.STATUS_ACTIVE, parentUnitId).list();
    }

    public long countByParentUnit(ObjectID parentUnitObjectID) {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and parentUnit.objectID = ?2", tenantID, parentUnitObjectID);
    }



    public long countActive() {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and status = ?2", tenantID, OrganizationalUnit.STATUS_ACTIVE);
    }

    @Transactional
    public boolean deleteOrganizationalUnit(ObjectID objectID) {
        String tenantID = getCurrentTenantID();
        long deleted = delete("objectID = ?1 and objectID.tenantID = ?2", objectID, tenantID);
        return deleted > 0;
    }



    // SQL nativo simple: unidades con contadores
    public List<Object[]> getUnitsWithCounts() {
        String tenantID = getCurrentTenantID();
        
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
            WHERE ou.tenant_id = ?1 AND ou.status = 'active'
            GROUP BY ou.id, ou.tenant_id, ou.name, ou.description, ou.cost_center, 
                     ou.location, ou.country, ou.status, ou.date_created, ou.date_updated,
                     ou.parent_unit_id, ou.organizational_level
            ORDER BY ou.organizational_level, ou.name
            """;
        
        @SuppressWarnings("unchecked")
        List<Object[]> result = getEntityManager().createNativeQuery(sql)
                .setParameter(1, tenantID)
                .getResultList();
        return result;
    }

    private String getCurrentTenantID() {
        return com.humanrsc.config.ThreadLocalStorage.getTenantID();
    }

    /**
     * Busca unidades por nivel organizacional
     */
    public List<OrganizationalUnit> findByOrganizationalLevel(Integer level) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 and organizationalLevel = ?3 order by name", 
                   tenantID, OrganizationalUnit.STATUS_ACTIVE, level).list();
    }

    /**
     * Busca unidades por rango de niveles organizacionales
     */
    public List<OrganizationalUnit> findByOrganizationalLevelRange(Integer minLevel, Integer maxLevel) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 and organizationalLevel between ?3 and ?4 order by organizationalLevel, name", 
                   tenantID, OrganizationalUnit.STATUS_ACTIVE, minLevel, maxLevel).list();
    }

    /**
     * Cuenta unidades por nivel organizacional
     */
    public long countByOrganizationalLevel(Integer level) {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and status = ?2 and organizationalLevel = ?3", 
                    tenantID, OrganizationalUnit.STATUS_ACTIVE, level);
    }
}

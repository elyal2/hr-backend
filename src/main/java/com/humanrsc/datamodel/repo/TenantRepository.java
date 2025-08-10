package com.humanrsc.datamodel.repo;

import com.humanrsc.datamodel.abstraction.ObjectID;
import com.humanrsc.datamodel.entities.Tenant;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TenantRepository implements PanacheRepositoryBase<Tenant, ObjectID> {

    

    /**
     * Busca un tenant por su ObjectID
     */
    public Optional<Tenant> findByObjectID(ObjectID objectID) {
        if (objectID == null) return Optional.empty();
        Optional<Tenant> res = Optional.ofNullable(findById(objectID));
        return res;
    }

    /**
     * Busca un tenant por ID interno
     */
    public Optional<Tenant> findById(String id) {
        if (id == null) return Optional.empty();
        Optional<Tenant> res = find("objectID.id", id).firstResultOptional();
        return res;
    }

    public Optional<Tenant> findByTenantId(String tenantID) {
        if (tenantID == null) return Optional.empty();
        Optional<Tenant> res = find("objectID.tenantID", tenantID).firstResultOptional();
        return res;
    }

    /**
     * Busca un tenant por dominio
     */
    public Optional<Tenant> findByDomain(String domain) {
        if (domain == null) return Optional.empty();
        Optional<Tenant> res = find("domain", domain).firstResultOptional();
        return res;
    }

    /**
     * Busca tenants por estado
     */
    public List<Tenant> findByStatus(String status) {
        if (status == null) return List.of();
        return find("status = ?1", Sort.by("dateCreated").descending(), status).list();
    }

    // Paginación: exponer PanacheQuery para datasets grandes
    public PanacheQuery<Tenant> findByStatusQuery(String status) {
        return find("status = ?1", Sort.by("dateCreated").descending(), status);
    }

    /**
     * Busca tenants por plan de suscripción
     */
    public List<Tenant> findBySubscriptionPlan(String subscriptionPlan) {
        if (subscriptionPlan == null) return List.of();
        return find("subscriptionPlan = ?1", Sort.by("dateCreated").descending(), subscriptionPlan).list();
    }

    // Paginación: exponer PanacheQuery para datasets grandes
    public PanacheQuery<Tenant> findBySubscriptionPlanQuery(String subscriptionPlan) {
        return find("subscriptionPlan = ?1", Sort.by("dateCreated").descending(), subscriptionPlan);
    }

    /**
     * Lista todos los tenants ordenados por fecha de creación
     */
    public List<Tenant> findAllOrdered() {
        return listAll(Sort.by("dateCreated").descending());
    }

    // Paginación: exponer PanacheQuery para datasets grandes
    public PanacheQuery<Tenant> findAllOrderedQuery() {
        return findAll(Sort.by("dateCreated").descending());
    }

    /**
     * Cuenta tenants por estado
     */
    public long countByStatus(String status) {
        if (status == null) return 0;
        return count("status = ?1", status);
    }

    /**
     * Crea un nuevo tenant con ObjectID generado
     */
    @Transactional
    public Tenant createTenant(Tenant tenant, String tenantID) {
        if (tenant.getObjectID() == null) {
            tenant.setObjectID(ObjectID.of(java.util.UUID.randomUUID().toString(), tenantID));
        }
        
        persist(tenant);
        return tenant;
    }

    /**
     * Actualiza un tenant existente
     */
    @Transactional
    public Tenant updateTenant(Tenant tenant) {
        return getEntityManager().merge(tenant);
    }

    /**
     * Activa un tenant
     */
    @Transactional
    public boolean activateTenant(ObjectID objectID) {
        Optional<Tenant> tenantOpt = findByObjectID(objectID);
        if (tenantOpt.isPresent()) {
            Tenant tenant = tenantOpt.get();
            tenant.activate();
            updateTenant(tenant);
            return true;
        }
        return false;
    }

    /**
     * Suspende un tenant
     */
    @Transactional
    public boolean suspendTenant(ObjectID objectID) {
        Optional<Tenant> tenantOpt = findByObjectID(objectID);
        if (tenantOpt.isPresent()) {
            Tenant tenant = tenantOpt.get();
            tenant.suspend();
            updateTenant(tenant);
            return true;
        }
        return false;
    }

    /**
     * Desactiva un tenant
     */
    @Transactional
    public boolean deactivateTenant(ObjectID objectID) {
        Optional<Tenant> tenantOpt = findByObjectID(objectID);
        if (tenantOpt.isPresent()) {
            Tenant tenant = tenantOpt.get();
            tenant.deactivate();
            updateTenant(tenant);
            return true;
        }
        return false;
    }

    /**
     * Verifica si existe un tenant con el dominio especificado
     */
    public boolean existsByDomain(String domain) {
        return findByDomain(domain).isPresent();
    }

    /**
     * Verifica si existe un tenant con el ID especificado
     */
    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    public boolean existsByTenantId(String tenantID) { return findByTenantId(tenantID).isPresent(); }
}

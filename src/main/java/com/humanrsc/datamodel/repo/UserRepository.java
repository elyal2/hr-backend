package com.humanrsc.datamodel.repo;

import com.humanrsc.datamodel.abstraction.ObjectID;
import com.humanrsc.datamodel.entities.User;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserRepository implements PanacheRepositoryBase<User, ObjectID> {

    /**
     * Busca un usuario por su ObjectID
     */
    public Optional<User> findByObjectID(ObjectID objectID) {
        if (objectID == null) return Optional.empty();
        return Optional.ofNullable(findById(objectID));
    }

    /**
     * Busca un usuario por email dentro de un tenant específico
     */
    public Optional<User> findByEmailAndTenant(String email, String tenantID) {
        if (email == null || tenantID == null) return Optional.empty();
        return find("email = ?1 and objectID.tenantID = ?2", email, tenantID).firstResultOptional();
    }

    /**
     * Busca usuarios por tenant
     */
    public List<User> findByTenant(String tenantID) {
        if (tenantID == null) return List.of();
        return find("objectID.tenantID", Sort.by("dateCreated").descending(), tenantID).list();
    }

    // Paginación: exponer PanacheQuery para datasets grandes
    public PanacheQuery<User> findByTenantQuery(String tenantID) {
        return find("objectID.tenantID", Sort.by("dateCreated").descending(), tenantID);
    }

    /**
     * Busca usuarios por estado dentro de un tenant
     */
    public List<User> findByStatusAndTenant(String status, String tenantID) {
        if (status == null || tenantID == null) return List.of();
        return find("status = ?1 and objectID.tenantID = ?2", Sort.by("dateCreated").descending(), status, tenantID).list();
    }

    // Paginación: exponer PanacheQuery para datasets grandes
    public PanacheQuery<User> findByStatusAndTenantQuery(String status, String tenantID) {
        return find("status = ?1 and objectID.tenantID = ?2", Sort.by("dateCreated").descending(), status, tenantID);
    }

    /**
     * Busca usuarios por rol dentro de un tenant
     */
    public List<User> findByRoleAndTenant(String role, String tenantID) {
        if (role == null || tenantID == null) return List.of();
        return find("?1 in elements(roles) and objectID.tenantID = ?2", Sort.by("dateCreated").descending(), role, tenantID).list();
    }

    // Paginación: exponer PanacheQuery para datasets grandes
    public PanacheQuery<User> findByRoleAndTenantQuery(String role, String tenantID) {
        return find("?1 in elements(roles) and objectID.tenantID = ?2", Sort.by("dateCreated").descending(), role, tenantID);
    }

    /**
     * Cuenta usuarios por tenant
     */
    public long countByTenant(String tenantID) {
        if (tenantID == null) return 0;
        return count("objectID.tenantID", tenantID);
    }

    /**
     * Crea un nuevo usuario con ObjectID generado
     */
    @Transactional
    public User createUser(User user, String tenantID) {
        if (user.getObjectID() == null) {
            String userId = UUID.randomUUID().toString();
            user.setObjectID(ObjectID.of(userId, tenantID));
        }
        persist(user);
        return user;
    }

    /**
     * Actualiza un usuario existente
     */
    @Transactional
    public User updateUser(User user) {
        return getEntityManager().merge(user);
    }

    /**
     * Elimina un usuario (marca como eliminado)
     */
    @Transactional
    public boolean deleteUser(ObjectID objectID) {
        Optional<User> userOpt = findByObjectID(objectID);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.markAsDeleted();
            updateUser(user);
            return true;
        }
        return false;
    }

    /**
     * Activa un usuario
     */
    @Transactional
    public boolean activateUser(ObjectID objectID) {
        Optional<User> userOpt = findByObjectID(objectID);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.activate();
            updateUser(user);
            return true;
        }
        return false;
    }

    /**
     * Suspende un usuario
     */
    @Transactional
    public boolean suspendUser(ObjectID objectID) {
        Optional<User> userOpt = findByObjectID(objectID);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.suspend();
            updateUser(user);
            return true;
        }
        return false;
    }

    /**
     * Actualiza el último login de un usuario
     */
    @Transactional
    public boolean updateLastLogin(ObjectID objectID) {
        Optional<User> userOpt = findByObjectID(objectID);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.updateLastLogin();
            updateUser(user);
            return true;
        }
        return false;
    }
}

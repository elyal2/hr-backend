package com.humanrsc.services;

import com.humanrsc.config.ThreadLocalStorage;
import com.humanrsc.datamodel.abstraction.ObjectID;
import com.humanrsc.datamodel.entities.User;
import com.humanrsc.datamodel.entities.Tenant;
import com.humanrsc.datamodel.repo.UserRepository;
import com.humanrsc.datamodel.repo.TenantRepository;
import com.humanrsc.security.JwtTokenUtils;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import io.quarkus.logging.Log;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class UserService {

    @Inject
    UserRepository userRepository;

    @Inject
    TenantRepository tenantRepository;

    @Inject
    JwtTokenUtils jwtTokenUtils;

    @Inject
    TenantContextService tenantContextService;

    @Transactional
    public User createOrUpdateUserFromJWT() {
        String tenantID = jwtTokenUtils.extractTenantFromJWT();
        String email = lower(jwtTokenUtils.extractEmailFromJWT());
        String subject = jwtTokenUtils.extractSubjectFromJWT();

        if (tenantID == null || tenantID.isBlank()) {
            throw new IllegalArgumentException("Tenant is required");
        }

        if (email == null || email.isBlank()) {
            String base = (subject != null && !subject.isBlank()) ? subject : UUID.randomUUID().toString();
            String safeLocal = base.replaceAll("[^A-Za-z0-9._%+-]", ".");
            email = lower(safeLocal + "@unknown.local");
            Log.warnf("Email claim not found in JWT. Using placeholder: %s", email);
        }

        ensureTenantExists(tenantID);

        Optional<User> existingUser = userRepository.findByEmailAndTenant(email, tenantID);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            updateUserFromJWT(user, email);
            return userRepository.getEntityManager().merge(user);
        }

        if (subject != null && !subject.isBlank()) {
            String safeLocal = subject.replaceAll("[^A-Za-z0-9._%+-]", ".");
            String placeholder = lower(safeLocal + "@unknown.local");
            Optional<User> placeholderUser = userRepository.findByEmailAndTenant(placeholder, tenantID);
            if (placeholderUser.isPresent()) {
                User user = placeholderUser.get();
                user.setEmail(email);
                updateUserFromJWT(user, email);
                user.setDateStatusUpdate(LocalDateTime.now());
                return userRepository.getEntityManager().merge(user);
            }
        }

        User newUser = createUserFromJWT(email, subject, tenantID);
        userRepository.persist(newUser);
        return newUser;
    }

    private User createUserFromJWT(String email, String subject, String tenantID) {
        User user = new User();
        user.setEmail(lower(email));
        // Prefer names from JWT custom claims
        String firstFromJwt = jwtTokenUtils.extractFirstNameFromJWT();
        String lastFromJwt = jwtTokenUtils.extractLastNameFromJWT();
        if (firstFromJwt != null && !firstFromJwt.isBlank()) user.setFirstName(firstFromJwt);
        if (lastFromJwt != null && !lastFromJwt.isBlank()) user.setLastName(lastFromJwt);

        // Derive from email if still missing
        if ((user.getFirstName() == null || user.getFirstName().isBlank()) && email.contains("@")) {
            String localPart = email.substring(0, email.indexOf('@'));
            if (localPart.contains(".")) {
                String[] parts = localPart.split("\\.", 2);
                user.setFirstName(parts[0]);
                if (user.getLastName() == null || user.getLastName().isBlank()) {
                    user.setLastName(parts.length > 1 ? parts[1] : null);
                }
            } else {
                user.setFirstName(localPart);
            }
        }

        user.setStatus(User.STATUS_PENDING);
        Set<String> roles = extractRolesFromJWT();
        user.setRoles(roles);
        String userId = UUID.randomUUID().toString();
        user.setObjectID(ObjectID.of(userId, tenantID));
        return user;
    }

    private void updateUserFromJWT(User user, String normalizedEmail) {
        boolean changed = false;

        // Email: asegurar lowercase
        if (normalizedEmail != null && !normalizedEmail.equals(user.getEmail())) {
            user.setEmail(normalizedEmail);
            changed = true;
        }

        // Nombres desde JWT si presentes
        String firstFromJwt = jwtTokenUtils.extractFirstNameFromJWT();
        String lastFromJwt = jwtTokenUtils.extractLastNameFromJWT();
        if (firstFromJwt != null && !firstFromJwt.isBlank() && !firstFromJwt.equals(user.getFirstName())) {
            user.setFirstName(firstFromJwt);
            changed = true;
        }
        if (lastFromJwt != null && !lastFromJwt.isBlank() && !lastFromJwt.equals(user.getLastName())) {
            user.setLastName(lastFromJwt);
            changed = true;
        }

        // Roles
        Set<String> currentRoles = extractRolesFromJWT();
        if (currentRoles != null && !currentRoles.equals(user.getRoles())) {
            user.setRoles(currentRoles);
            changed = true;
        }

        // Último login siempre
        user.updateLastLogin();

        // Fecha de actualización si hubo cambios
        if (changed) {
            user.setDateStatusUpdate(LocalDateTime.now());
        }
    }

    private Set<String> extractRolesFromJWT() {
        Set<String> roles = new java.util.HashSet<>();
        try {
            Object rolesClaim = jwtTokenUtils.getClaim("roles");
            if (rolesClaim instanceof String s) {
                roles.add(s);
            } else if (rolesClaim instanceof java.util.Collection<?> c) {
                c.forEach(r -> roles.add(String.valueOf(r)));
            }
            Object permissionsClaim = jwtTokenUtils.getClaim("permissions");
            if (permissionsClaim instanceof String s) {
                roles.add(s);
            } else if (permissionsClaim instanceof java.util.Collection<?> c) {
                c.forEach(p -> roles.add(String.valueOf(p)));
            }
            if (roles.isEmpty()) roles.add("user");
        } catch (Exception e) {
            Log.warnf("Error extracting roles from JWT: %s", e.getMessage());
            roles.add("user");
        }
        return roles;
    }

    private void ensureTenantExists(String tenantID) {
        Optional<Tenant> existing = tenantRepository.findByTenantId(tenantID);
        String domain = jwtTokenUtils.extractTenantDomainFromJWT();
        if (existing.isPresent()) {
            Tenant t = existing.get();
            if (t.getDomain() == null && domain != null) {
                t.setDomain(domain);
                tenantRepository.getEntityManager().merge(t);
            }
            return;
        }
        
        // Verificar si ya existe un tenant con este tenant_id antes de crear
        if (tenantRepository.existsByTenantId(tenantID)) {
            Log.warnf("Tenant already exists but not found by findByTenantId: %s", tenantID);
            return;
        }
        
        Log.infof("Creating new tenant: %s", tenantID);
        Tenant tenant = new Tenant();
        tenant.setName(tenantID.replace('-', ' '));
        tenant.setDomain(domain);
        tenant.setStatus(Tenant.STATUS_PENDING);
        // Use UUID for id and stable tenant_id
        tenant.setObjectID(ObjectID.of(UUID.randomUUID().toString(), tenantID));
        tenantRepository.persist(tenant);
    }

    private static String lower(String s) { return s == null ? null : s.toLowerCase(); }

    public Optional<User> findById(ObjectID objectID) { return userRepository.findByObjectID(objectID); }
    public Optional<User> findByEmail(String email) {
        String tenantID = ThreadLocalStorage.getTenantID();
        if (tenantID == null) tenantID = jwtTokenUtils.extractTenantFromJWT();
        return userRepository.findByEmailAndTenant(lower(email), tenantID);
    }
    public List<User> findAllUsers() {
        String tenantID = ThreadLocalStorage.getTenantID();
        if (tenantID == null) tenantID = jwtTokenUtils.extractTenantFromJWT();
        return userRepository.findByTenant(tenantID);
    }
    // Overload con paginación
    public List<User> findAllUsers(int page, int size) {
        String tenantID = ThreadLocalStorage.getTenantID();
        if (tenantID == null) tenantID = jwtTokenUtils.extractTenantFromJWT();
        PanacheQuery<User> q = userRepository.findByTenantQuery(tenantID);
        return q.page(Page.of(page, size)).list();
    }
    @Transactional public User createUser(User user) {
        String tenantID = ThreadLocalStorage.getTenantID();
        if (tenantID == null) tenantID = jwtTokenUtils.extractTenantFromJWT();
        user.setEmail(lower(user.getEmail()));
        userRepository.persist(user);
        return user;
    }
    @Transactional public User updateUser(User user) { return userRepository.getEntityManager().merge(user); }
    @Transactional public boolean deleteUser(ObjectID objectID) { return userRepository.deleteUser(objectID); }
    @Transactional public boolean activateUser(ObjectID objectID) { return userRepository.activateUser(objectID); }
    @Transactional public boolean suspendUser(ObjectID objectID) { return userRepository.suspendUser(objectID); }
    @Transactional public boolean updateLastLogin(ObjectID objectID) { return userRepository.updateLastLogin(objectID); }
    public List<User> findByStatus(String status) {
        String tenantID = ThreadLocalStorage.getTenantID();
        if (tenantID == null) tenantID = jwtTokenUtils.extractTenantFromJWT();
        return userRepository.findByStatusAndTenant(status, tenantID);
    }
    // Overload con paginación
    public List<User> findByStatus(String status, int page, int size) {
        String tenantID = ThreadLocalStorage.getTenantID();
        if (tenantID == null) tenantID = jwtTokenUtils.extractTenantFromJWT();
        PanacheQuery<User> q = userRepository.findByStatusAndTenantQuery(status, tenantID);
        return q.page(Page.of(page, size)).list();
    }
    public List<User> findByRole(String role) {
        String tenantID = ThreadLocalStorage.getTenantID();
        if (tenantID == null) tenantID = jwtTokenUtils.extractTenantFromJWT();
        return userRepository.findByRoleAndTenant(role, tenantID);
    }
    // Overload con paginación
    public List<User> findByRole(String role, int page, int size) {
        String tenantID = ThreadLocalStorage.getTenantID();
        if (tenantID == null) tenantID = jwtTokenUtils.extractTenantFromJWT();
        PanacheQuery<User> q = userRepository.findByRoleAndTenantQuery(role, tenantID);
        return q.page(Page.of(page, size)).list();
    }
    public long countUsers() {
        String tenantID = ThreadLocalStorage.getTenantID();
        if (tenantID == null) tenantID = jwtTokenUtils.extractTenantFromJWT();
        return userRepository.countByTenant(tenantID);
    }
}

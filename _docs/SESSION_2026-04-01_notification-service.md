# Development Session: NotificationService Implementation
**Date**: April 1, 2026  
**Task**: FS-126 - NotificationService (entity, migration V1.7.0, backend service)  
**Status**: 🟡 In Progress (Core implementation complete, REST API pending)

---

## 📋 Session Summary

This session focused on implementing the notification system for the HumanRSC HR platform, including database migration, entities, repository, service layer, and integration hooks with existing business logic.

### Completed Work

1. ✅ **Migration V1.7.0** - Database schema for notifications
2. ✅ **NotificationType Enum** - 6 notification types defined
3. ✅ **Notification Entity** - JPA entity with multi-tenant support
4. ✅ **NotificationRepository** - Panache repository for data access
5. ✅ **NotificationService** - Business logic for notification CRUD
6. ✅ **OrganizationService Integration** - 6 notification hooks added
7. ✅ **Flyway Validation** - Migration tracking resolved
8. ✅ **Build & Deployment** - Docker container running successfully

### Remaining Work

1. ⏭️ **NotificationResource** - REST API endpoints
2. ⏭️ **Permissions Update** - Add `NOTIFICATIONS_READ` constant
3. ⏭️ **Testing** - End-to-end testing of notification flow
4. ⏭️ **Linear Task** - Mark FS-126 as DONE

---

## 🗄️ Database Changes

### Migration: V1.7.0__add_notifications.sql

**Location**: `src/main/resources/db/migration/V1.7.0__add_notifications.sql`

**Schema**: `hr_app.notifications`

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| `id` | UUID | NOT NULL | Unique notification ID |
| `tenant_id` | VARCHAR(255) | NOT NULL | Tenant isolation |
| `user_id` | VARCHAR(255) | NOT NULL | Recipient user ID |
| `type` | VARCHAR(50) | NOT NULL | NotificationType enum value |
| `title` | VARCHAR(255) | NOT NULL | Notification title |
| `message` | TEXT | NOT NULL | Notification body |
| `entity_type` | VARCHAR(100) | NULL | Related entity type (e.g., 'Employee') |
| `entity_id` | VARCHAR(255) | NULL | Related entity ID |
| `read` | BOOLEAN | NOT NULL | Read status (default: false) |
| `read_at` | TIMESTAMP | NULL | Timestamp when marked as read |
| `created_at` | TIMESTAMP | NOT NULL | Creation timestamp (default: NOW()) |

**Primary Key**: Composite `(id, tenant_id)` for multi-tenancy

**Indexes**:
- `idx_notifications_user_unread` - `(tenant_id, user_id, read, created_at DESC)`
- `idx_notifications_user_created` - `(tenant_id, user_id, created_at DESC)`
- `idx_notifications_entity` - `(tenant_id, entity_type, entity_id)`

**Security**: 
- RLS (Row Level Security) enabled
- Policy: `tenant_isolation` using `app.current_tenant` session variable

**Flyway Status**: ✅ Successfully applied and validated (migration #8 of 9 total)

---

## 📦 Code Structure

### 1. NotificationType Enum
**File**: `src/main/java/com/humanrsc/datamodel/enums/NotificationType.java`

```java
public enum NotificationType {
    EMPLOYEE_CREATED,
    EMPLOYEE_TERMINATED,
    EMPLOYEE_RESIGNED,
    SALARY_UPDATED,
    TEMPORARY_REPLACEMENT_ASSIGNED,
    TEMPORARY_REPLACEMENT_ENDED
}
```

### 2. Notification Entity
**File**: `src/main/java/com/humanrsc/datamodel/entity/Notification.java`

**Key Features**:
- Uses Panache pattern (`PanacheEntityBase`)
- Composite ID: `(id, tenant_id)`
- Lombok annotations: `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
- No `@Audited` annotation (to avoid volume overhead)
- Automatic timestamp handling with `@CreationTimestamp`

### 3. NotificationRepository
**File**: `src/main/java/com/humanrsc/repositories/NotificationRepository.java`

**Methods**:
- `findByUserUnread(String tenantId, String userId)` - Get unread notifications
- `findByUser(String tenantId, String userId, Pageable pageable)` - Get all notifications (paginated)
- `markAsRead(String tenantId, String notificationId)` - Mark single as read
- `markAllAsRead(String tenantId, String userId)` - Mark all as read
- `countUnreadByUser(String tenantId, String userId)` - Count unread

**Pattern**: Uses PanacheRepository with native queries for tenant isolation

### 4. NotificationService
**File**: `src/main/java/com/humanrsc/services/NotificationService.java`

**Dependencies**: `NotificationRepository`, `TenantContext`

**Methods**:
- `createNotification(...)` - Create new notification
- `getUnreadNotifications(String userId)` - Get unread for user
- `getAllNotifications(String userId, int page, int size)` - Get all (paginated)
- `markAsRead(String notificationId)` - Mark single as read
- `markAllAsRead(String userId)` - Mark all as read

**Transaction Handling**: All write methods use `@Transactional`

### 5. OrganizationService Integration
**File**: `src/main/java/com/humanrsc/services/OrganizationService.java`

**Notification Hooks Added**:

| Method | Trigger | NotificationType |
|--------|---------|------------------|
| `createEmployee()` | New employee created | `EMPLOYEE_CREATED` |
| `terminateEmployee()` | Employee terminated | `EMPLOYEE_TERMINATED` |
| `resignEmployee()` | Employee resigned | `EMPLOYEE_RESIGNED` |
| `updateEmployeeSalary()` | Salary updated | `SALARY_UPDATED` |
| `assignTemporaryReplacement()` | Replacement assigned | `TEMPORARY_REPLACEMENT_ASSIGNED` |
| `endTemporaryReplacement()` | Replacement ended | `TEMPORARY_REPLACEMENT_ENDED` |

**Helper Methods**:
- `createEmployeeNotification(Employee employee, NotificationType type)` - Employee-related notifications
- `createReplacementNotification(TemporaryReplacement replacement, NotificationType type)` - Replacement notifications

**Recipient Logic**: Notifications sent to `hr-manager` role users (placeholder for future enhancement with manager hierarchy)

---

## 🔧 Technical Issues & Resolutions

### Issue #1: Flyway Migration Validation Error

**Problem**: 
Backend failed to start with:
```
org.flywaydb.core.api.exception.FlywayValidateException: 
Validate failed: Migrations have failed validation
Detected applied migration not resolved locally: 1.7.0.
```

**Root Cause**:
1. Migration file `V1.7.0__add_notifications.sql` was created in workspace
2. Maven build (`mvn package`) had already run, creating JAR without V1.7.0
3. Migration SQL was manually applied to database for testing
4. Flyway schema_history table recorded V1.7.0 as applied
5. On backend restart, Flyway compared DB history with JAR resources
6. V1.7.0 was in DB but not in JAR → validation failed

**Timeline**:
1. 20:47 - Created V1.7.0 migration file
2. 20:55 - Manually applied SQL to database
3. 20:58 - Restarted backend → Flyway error
4. 21:00 - Attempted fix with `docker restart` → same error
5. 21:02 - Attempted `docker-compose build` → still used cached layers
6. 21:03 - Final fix: deleted image, rebuilt with `--no-cache`

**Solution Steps**:
1. Stop all containers: `docker-compose down`
2. Delete backend image: `docker rmi hr-backend-backend:latest`
3. Rebuild without cache: `docker-compose build --no-cache backend`
4. Start containers: `docker-compose up -d`
5. Verify: Flyway logs show "Successfully validated 9 migrations"

**Prevention**:
- Always rebuild Docker image after adding new migration files
- Use `docker-compose build --no-cache` to avoid layer caching issues
- Never manually apply migrations if Flyway is managing schema

---

### Issue #2: Duplicate Endpoint Conflict (FS-142)

**Problem**:
Maven build failed during Docker image creation:
```
jakarta.enterprise.inject.spi.DeploymentException: 
GET /api/organization/stats is declared by:
- com.humanrsc.resources.StatisticsResource#getEmployeeStats
- com.humanrsc.resources.OrganizationResource#getEmployeeStats
```

**Root Cause**:
- Task FS-142 (Stats Endpoints) created new `StatisticsResource.java`
- Stats endpoints already existed in `OrganizationResource.java` (lines 750-769)
- Both resources defined same paths: `/api/organization/stats/employees` and `/api/organization/stats/organization`
- Quarkus REST framework detected conflict during build phase

**Solution**:
Removed duplicate files created for FS-142:
- `src/main/java/com/humanrsc/resources/StatisticsResource.java`
- `src/main/java/com/humanrsc/services/StatisticsService.java`
- `src/main/java/com/humanrsc/datamodel/dto/stats/` (entire directory)

**Existing Implementation**:
Stats functionality remains fully functional in:
- `OrganizationResource.java` - REST endpoints (lines 750-769)
- `OrganizationService.java` - Business logic with native SQL queries
- `OrganizationService.EmployeeStats` - DTO for employee statistics
- `OrganizationService.OrganizationStats` - DTO for organization statistics

**Lesson Learned**:
Always search codebase before creating new resources:
```bash
grep -r "GET.*stats" src/main/java/
grep -r "@Path.*stats" src/main/java/
```

---

## 📚 Key Lessons Learned

### 1. Migration Files Must Be in Build

**Problem**: Flyway reads migrations from JAR classpath, not filesystem

**Impact**: 
- Migration files added after `mvn package` won't be detected
- Causes validation errors when DB has migration but JAR doesn't

**Best Practice**:
- Always rebuild Docker image after adding migrations
- Use `docker-compose build --no-cache` to avoid cache issues
- Verify migration count in startup logs

**Example Workflow**:
```bash
# 1. Create migration file
vi src/main/resources/db/migration/V1.X.X__description.sql

# 2. Rebuild image (no cache)
docker-compose down
docker rmi hr-backend-backend:latest
docker-compose build --no-cache backend

# 3. Start and verify
docker-compose up -d
docker logs hr-backend-dev | grep "validated.*migrations"
```

---

### 2. Check for Existing Endpoints

**Problem**: Duplicate resource definitions cause hard build failures

**Detection**:
Search before creating new resources:
```bash
# Check for existing paths
grep -r "@Path.*<your-path>" src/main/java/

# Check for existing method names
grep -r "public.*get.*Stats" src/main/java/

# Check for existing DTOs
find src/main/java -name "*StatsDTO.java"
```

**Impact**:
- Quarkus REST validates uniqueness at build time
- Duplicate paths fail compilation (cannot run to discover at runtime)
- Must rebuild Docker image to fix

---

### 3. Flyway Validation is Strict

**Behavior**:
- Flyway compares DB `flyway_schema_history` with JAR resources
- All applied migrations must have corresponding files in classpath
- Checksums validate file contents match

**Common Errors**:

| Error | Cause | Solution |
|-------|-------|----------|
| "applied migration not resolved locally" | Migration in DB but not in JAR | Rebuild image with migration file |
| "checksum mismatch" | Migration file changed after apply | Never edit applied migrations |
| "missing migration" | Gap in version numbers | Create missing migration or repair |

**Manual Flyway Operations** (use with caution):
```bash
# Mark migration as deleted (if intentionally removed)
docker exec hr-backend-dev ./mvnw flyway:repair

# Clean database (DESTRUCTIVE - removes all data)
docker exec hr-backend-dev ./mvnw flyway:clean

# Validate migrations
docker exec hr-backend-dev ./mvnw flyway:validate
```

---

### 4. Docker Layer Caching

**Problem**: `docker-compose build` reuses cached layers even when source changed

**Symptoms**:
- Code changes not reflected in container
- Old migration list in Flyway logs
- Successful build but old behavior

**Solutions**:

**Option A**: No cache rebuild
```bash
docker-compose build --no-cache backend
```

**Option B**: Delete image
```bash
docker rmi hr-backend-backend:latest
docker-compose build backend
```

**Option C**: Prune all build cache
```bash
docker builder prune -af  # WARNING: Affects all images
```

**When to use**:
- After adding migration files
- After major dependency changes
- When behavior doesn't match code

---

## 📊 System State

### Database
- **Total Migrations**: 9 (V1.0.0 through V1.7.0)
- **Schema**: `hr_app`
- **RLS**: Enabled on all tables including `notifications`
- **Tables**: 28 total (including `notifications`, `flyway_schema_history`)

### Backend
- **Status**: ✅ Running (started in 2.482s)
- **Image**: `hr-backend-backend:latest`
- **Build**: Maven 3.9.9, Java 21, Quarkus 3.25.0
- **Container**: `hr-backend-dev` (healthy)

### Migration History
```
installed_rank | version | description                   | success
---------------|---------|-------------------------------|--------
0              | -       | Flyway Schema Creation        | t
1              | 1.0.0   | initial schema                | t
2              | 1.1.0   | organization module           | t
3              | 1.1.1   | fix rls policies              | t
4              | 1.2.0   | remove employee user fk       | t
5              | 1.4.0   | reorganize position structure | t
6              | 1.5.0   | add organizational level      | t
7              | 1.6.0   | add currency exchange rates   | t
8              | 1.7.0   | add notifications             | t
```

---

## 🎯 Next Steps (for FS-126 completion)

### 1. Create NotificationResource
**File**: `src/main/java/com/humanrsc/resources/NotificationResource.java`

**Endpoints**:
```java
@Path("/api/notifications")
public class NotificationResource {
    
    @GET
    @RolesAllowed({Permissions.NOTIFICATIONS_READ})
    public Response getAllNotifications(@QueryParam("page") int page, 
                                       @QueryParam("size") int size);
    
    @GET
    @Path("/unread")
    @RolesAllowed({Permissions.NOTIFICATIONS_READ})
    public Response getUnreadCount();
    
    @PUT
    @Path("/{id}/read")
    @RolesAllowed({Permissions.NOTIFICATIONS_READ})
    public Response markAsRead(@PathParam("id") String id);
    
    @PUT
    @Path("/read-all")
    @RolesAllowed({Permissions.NOTIFICATIONS_READ})
    public Response markAllAsRead();
}
```

### 2. Add Permission Constant
**File**: `src/main/java/com/humanrsc/security/Permissions.java`

Add line after line 51:
```java
public static final String NOTIFICATIONS_READ = "notifications:read";
```

### 3. Testing Checklist
- [ ] Create employee → verify `EMPLOYEE_CREATED` notification
- [ ] Terminate employee → verify `EMPLOYEE_TERMINATED` notification
- [ ] Resign employee → verify `EMPLOYEE_RESIGNED` notification
- [ ] Update salary → verify `SALARY_UPDATED` notification
- [ ] Assign replacement → verify `TEMPORARY_REPLACEMENT_ASSIGNED` notification
- [ ] End replacement → verify `TEMPORARY_REPLACEMENT_ENDED` notification
- [ ] Test GET `/api/notifications` endpoint
- [ ] Test GET `/api/notifications/unread` endpoint
- [ ] Test PUT `/api/notifications/{id}/read` endpoint
- [ ] Test PUT `/api/notifications/read-all` endpoint
- [ ] Verify tenant isolation (notifications only visible to same tenant)

### 4. Mark Linear Task Complete
Update FS-126 status to "Done" with final summary comment.

---

## 📝 Related Tasks

- **FS-126**: NotificationService implementation (this task) - 🟡 In Progress
- **FS-142**: Stats Endpoints - ✅ Done (discovered already implemented)

---

## 🔗 References

- Linear Task: https://linear.app/hrsc/issue/FS-126
- Migration File: `src/main/resources/db/migration/V1.7.0__add_notifications.sql`
- Entity: `src/main/java/com/humanrsc/datamodel/entity/Notification.java`
- Service: `src/main/java/com/humanrsc/services/NotificationService.java`
- Integration: `src/main/java/com/humanrsc/services/OrganizationService.java` (lines with `notificationService`)

---

**Session End**: Documentation complete. System ready for NotificationResource implementation.

# MapStruct Migration Plan

## Executive Summary

This document outlines the complete migration plan to adopt **MapStruct** for Entity ↔ DTO mapping in the HR Backend project. MapStruct is a compile-time code generator that will replace ~308 lines of manual field-copying code with type-safe, performant, and maintainable mappers.

### Key Metrics

| Metric | Current State | After MapStruct |
|--------|---------------|-----------------|
| Endpoints using entities directly | 12 | 0 |
| Manual setter calls | ~71 | 0 |
| Lines of manual mapping code | ~308 | ~50 (mapper annotations) |
| DTOs | 6 (partial coverage) | 12-24 (complete coverage) |
| Mapper interfaces | 0 | 10 |
| Type safety | Runtime errors | Compile-time errors |
| Performance | Reflection-based | Direct method calls |

---

## What is MapStruct?

**MapStruct** is an annotation processor that generates bean mapper implementations at compile time.

### Key Features

- ✅ **Type-safe**: Compile-time validation of field mappings
- ✅ **Performant**: Generates plain Java method calls (no reflection)
- ✅ **Lombok-compatible**: Works seamlessly with Lombok-generated getters/setters
- ✅ **CDI-native**: Mappers are `@ApplicationScoped` beans in Quarkus
- ✅ **Bidirectional**: Entity → DTO and DTO → Entity with single definition
- ✅ **Custom logic**: Supports `@BeforeMapping`, `@AfterMapping`, custom resolvers

### Example

**Before (manual):**
```java
public void updateEmployeeFromDTO(Employee employee, UpdateEmployeeDTO dto) {
    employee.setFirstName(dto.getFirstName());
    employee.setLastName(dto.getLastName());
    employee.setEmail(dto.getEmail());
    // ... 20 more lines
}
```

**After (MapStruct):**
```java
@Mapper(componentModel = "cdi", uses = {EntityResolverService.class})
public interface EmployeeMapper {
    @Mapping(target = "objectID", ignore = true)
    @Mapping(target = "dateCreated", ignore = true)
    void updateEmployeeFromDTO(UpdateEmployeeDTO dto, @MappingTarget Employee employee);
}
```

MapStruct generates the implementation at compile time.

---

## Current State Analysis

### Endpoints Accepting Entities Directly

**⚠️ These endpoints violate the "no entities as request bodies" principle:**

#### POST Endpoints (9)

| Endpoint | File:Line | Entity Accepted |
|----------|-----------|-----------------|
| `POST /api/organization/position-categories` | OrganizationResource.java:42 | `PositionCategory` |
| `POST /api/organization/units` | OrganizationResource.java:111 | `OrganizationalUnit` |
| `POST /api/organization/positions` | OrganizationResource.java:266 | `JobPosition` |
| `POST /api/organization/employees` | OrganizationResource.java:430 | `Employee` |
| `POST /api/organization/currency-rates` | OrganizationResource.java:937 | `CurrencyExchangeRate` |
| `POST /api/organization/replacements` | OrganizationResource.java:1071 | `TemporaryReplacement` |
| `POST /api/organization/salary-history` | OrganizationResource.java:1168 | `SalaryHistory` |
| `POST /tenants` | TenantResource.java:94 | `Tenant` |
| `POST /users` | UserResource.java:98 | `User` |

#### PUT Endpoints (5)

| Endpoint | File:Line | Entity Accepted |
|----------|-----------|-----------------|
| `PUT /api/organization/position-categories/{id}` | OrganizationResource.java:76 | `PositionCategory` |
| `PUT /api/organization/employees/{id}` | OrganizationResource.java:627 | `Employee` |
| `PUT /api/organization/currency-rates/{id}` | OrganizationResource.java:1001 | `CurrencyExchangeRate` |
| `PUT /tenants/{id}` | TenantResource.java:111 | `Tenant` |
| `PUT /users/{id}` | UserResource.java:115 | `User` |

### Existing DTO Coverage

**✅ Entities with partial DTO coverage:**

| Entity | Has Create DTO? | Has Update DTO? | Used Correctly? |
|--------|-----------------|-----------------|-----------------|
| `EmployeeAssignment` | ✅ | ✅ | ✅ Yes |
| `OrganizationalUnit` | ❌ | ✅ | ⚠️ Partial (POST uses entity) |
| `JobPosition` | ❌ | ✅ | ⚠️ Partial (POST uses entity) |
| `TemporaryReplacement` | ❌ | ✅ | ⚠️ Partial (POST uses entity) |
| `SalaryHistory` | ❌ | ✅ | ⚠️ Partial (POST uses entity) |

**❌ Entities with NO DTO coverage:**

- `Employee`
- `PositionCategory`
- `CurrencyExchangeRate`
- `Tenant`
- `User`

### Manual Mapping Code

**OrganizationService.java** contains 6 manual DTO-to-Entity mapping methods:

| Method | Lines | Setters | Purpose |
|--------|-------|---------|---------|
| `updateOrganizationalUnitFromDTO()` | 30 | 10 | Maps update DTO → entity |
| `updateJobPositionFromDTO()` | 41 | 12 | Maps update DTO → entity |
| `createEmployeeAssignmentFromDTO()` | 80 | 15 | Maps create DTO → entity |
| `updateEmployeeAssignmentFromDTO()` | 65 | 18 | Maps update DTO → entity |
| `updateTemporaryReplacementFromDTO()` | 52 | 12 | Maps update DTO → entity |
| `updateSalaryHistoryFromDTO()` | 40 | 10 | Maps update DTO → entity |
| **TOTAL** | **308** | **71** | **6 methods** |

All of these methods follow the same pattern:
1. Fetch related entities by ID (e.g., `employeeRepository.findById(dto.getEmployeeId())`)
2. Manual null/empty checks
3. Set every field individually with 10-20 setter calls
4. Throw typed exceptions if lookup fails

---

## Migration Plan

### Phase 1: Foundation Setup (Day 1-2)

#### 1.1 Add MapStruct Dependency

**pom.xml:**
```xml
<properties>
    <mapstruct.version>1.6.3</mapstruct.version>
</properties>

<dependencies>
    <!-- MapStruct -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>${mapstruct.version}</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>${compiler-plugin.version}</version>
            <configuration>
                <annotationProcessorPaths>
                    <!-- Lombok MUST be first -->
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>1.18.30</version>
                    </path>
                    <!-- MapStruct processor -->
                    <path>
                        <groupId>org.mapstruct</groupId>
                        <artifactId>mapstruct-processor</artifactId>
                        <version>${mapstruct.version}</version>
                    </path>
                    <!-- Existing Hibernate processor -->
                    <path>
                        <groupId>org.hibernate.orm</groupId>
                        <artifactId>hibernate-jpamodelgen</artifactId>
                        <version>6.2.13.Final</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

**⚠️ Critical:** Lombok MUST be listed before MapStruct in annotation processors, otherwise MapStruct won't see generated getters/setters.

#### 1.2 Create EntityResolverService

**Location:** `src/main/java/com/humanrsc/services/EntityResolverService.java`

```java
package com.humanrsc.services;

import com.humanrsc.datamodel.entities.*;
import com.humanrsc.datamodel.repo.*;
import com.humanrsc.exceptions.ResourceNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Central service for resolving String IDs to entity references.
 * Used by MapStruct mappers for relationship mapping.
 */
@ApplicationScoped
public class EntityResolverService {

    @Inject EmployeeRepository employeeRepository;
    @Inject OrganizationalUnitRepository organizationalUnitRepository;
    @Inject JobPositionRepository jobPositionRepository;
    @Inject PositionCategoryRepository positionCategoryRepository;
    @Inject UserRepository userRepository;

    public Employee resolveEmployee(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", id));
    }

    public OrganizationalUnit resolveOrganizationalUnit(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return organizationalUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OrganizationalUnit", id));
    }

    public JobPosition resolveJobPosition(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return jobPositionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JobPosition", id));
    }

    public PositionCategory resolvePositionCategory(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return positionCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PositionCategory", id));
    }

    public User resolveUser(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
```

---

### Phase 2: Create Missing DTOs (Day 3-6)

#### 2.1 Employee DTOs

**CreateEmployeeDTO.java:**
```java
package com.humanrsc.datamodel.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class CreateEmployeeDTO {
    @NotBlank(message = "Employee ID is required")
    private String employeeId;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank @Email(message = "Valid email is required")
    private String email;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    private String nationalId;
    private String taxId;
    private String gender;

    @NotBlank(message = "Employee type is required")
    private String employeeType;

    @NotBlank(message = "Contract type is required")
    private String contractType;

    @NotNull(message = "Hire date is required")
    private LocalDate hireDate;

    @NotNull(message = "Current salary is required")
    private BigDecimal currentSalary;

    @NotBlank(message = "Currency is required")
    private String currency;

    private String phoneNumber;
    private String address;
}
```

**UpdateEmployeeDTO.java:**
```java
package com.humanrsc.datamodel.dto;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class UpdateEmployeeDTO {
    private String firstName;
    private String lastName;
    
    @Email(message = "Valid email required if provided")
    private String email;
    
    private LocalDate dateOfBirth;
    private String nationalId;
    private String taxId;
    private String gender;
    private String employeeType;
    private String contractType;
    private BigDecimal currentSalary;
    private String currency;
    private String phoneNumber;
    private String address;
    
    // Note: employeeId, hireDate, status, terminationDate NOT updatable via DTO
}
```

#### 2.2 PositionCategory DTOs

```java
package com.humanrsc.datamodel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreatePositionCategoryDTO {
    @NotBlank(message = "Name is required")
    private String name;
    
    private String description;
}

@Getter @Setter
public class UpdatePositionCategoryDTO {
    private String name;
    private String description;
    private String status;
}
```

#### 2.3 CurrencyExchangeRate DTOs

```java
package com.humanrsc.datamodel.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class CreateCurrencyExchangeRateDTO {
    @NotBlank(message = "From currency is required")
    private String fromCurrency;

    @NotBlank(message = "To currency is required")
    private String toCurrency;

    @NotNull(message = "Exchange rate is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Exchange rate must be positive")
    private BigDecimal exchangeRate;

    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;

    private LocalDate expiryDate;
    private String source;
}

@Getter @Setter
public class UpdateCurrencyExchangeRateDTO {
    private BigDecimal exchangeRate;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private String status;
    private String source;
}
```

#### 2.4 Complete Existing Partial Coverage

Create missing **Create** DTOs for entities that only have Update DTOs:

- `CreateOrganizationalUnitDTO`
- `CreateJobPositionDTO`
- `CreateTemporaryReplacementDTO`
- `CreateSalaryHistoryDTO`

(Follow same pattern as above)

---

### Phase 3: Create Mapper Interfaces (Day 7-11)

#### 3.1 Employee Mapper

**Location:** `src/main/java/com/humanrsc/datamodel/mappers/EmployeeMapper.java`

```java
package com.humanrsc.datamodel.mappers;

import com.humanrsc.datamodel.dto.CreateEmployeeDTO;
import com.humanrsc.datamodel.dto.UpdateEmployeeDTO;
import com.humanrsc.datamodel.entities.Employee;
import org.mapstruct.*;

@Mapper(
    componentModel = "cdi",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface EmployeeMapper {

    /**
     * Maps CreateEmployeeDTO → Employee for POST operations.
     * Ignores internal fields (objectID, dates, status) - handled by service.
     */
    @Mapping(target = "objectID", ignore = true)
    @Mapping(target = "dateCreated", ignore = true)
    @Mapping(target = "dateUpdated", ignore = true)
    @Mapping(target = "dateStatusUpdate", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "terminationDate", ignore = true)
    @Mapping(target = "attributes", ignore = true)
    Employee toEntity(CreateEmployeeDTO dto);

    /**
     * Maps UpdateEmployeeDTO → existing Employee for PUT operations.
     * Only updates non-null fields from DTO.
     */
    @Mapping(target = "objectID", ignore = true)
    @Mapping(target = "employeeId", ignore = true)  // Not updatable
    @Mapping(target = "hireDate", ignore = true)    // Not updatable
    @Mapping(target = "status", ignore = true)      // Use dedicated endpoint
    @Mapping(target = "terminationDate", ignore = true)
    @Mapping(target = "dateCreated", ignore = true)
    @Mapping(target = "dateStatusUpdate", ignore = true)
    @Mapping(target = "attributes", ignore = true)
    void updateEmployeeFromDTO(UpdateEmployeeDTO dto, @MappingTarget Employee employee);
}
```

#### 3.2 EmployeeAssignment Mapper (with relationship resolution)

```java
package com.humanrsc.datamodel.mappers;

import com.humanrsc.datamodel.dto.CreateEmployeeAssignmentDTO;
import com.humanrsc.datamodel.dto.EmployeeAssignmentDTO;
import com.humanrsc.datamodel.entities.EmployeeAssignment;
import com.humanrsc.services.EntityResolverService;
import org.mapstruct.*;

@Mapper(
    componentModel = "cdi",
    uses = {EntityResolverService.class},
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface EmployeeAssignmentMapper {

    /**
     * Maps String IDs → Entity references using EntityResolverService.
     */
    @Mapping(target = "employee", source = "employeeId", qualifiedByName = "resolveEmployee")
    @Mapping(target = "position", source = "positionId", qualifiedByName = "resolvePosition")
    @Mapping(target = "unit", source = "unitId", qualifiedByName = "resolveUnit")
    @Mapping(target = "manager", source = "managerId", qualifiedByName = "resolveManager")
    @Mapping(target = "objectID", ignore = true)
    @Mapping(target = "dateCreated", ignore = true)
    @Mapping(target = "dateUpdated", ignore = true)
    @Mapping(target = "attributes", ignore = true)
    EmployeeAssignment toEntity(CreateEmployeeAssignmentDTO dto);

    @Mapping(target = "employee", source = "employeeId", qualifiedByName = "resolveEmployee")
    @Mapping(target = "position", source = "positionId", qualifiedByName = "resolvePosition")
    @Mapping(target = "unit", source = "unitId", qualifiedByName = "resolveUnit")
    @Mapping(target = "manager", source = "managerId", qualifiedByName = "resolveManager")
    @Mapping(target = "objectID", ignore = true)
    @Mapping(target = "dateCreated", ignore = true)
    @Mapping(target = "attributes", ignore = true)
    void updateFromDTO(EmployeeAssignmentDTO dto, @MappingTarget EmployeeAssignment assignment);

    /**
     * Custom resolver methods using injected EntityResolverService.
     */
    @Named("resolveEmployee")
    default com.humanrsc.datamodel.entities.Employee resolveEmployee(String id, @Context EntityResolverService resolver) {
        return resolver.resolveEmployee(id);
    }

    @Named("resolvePosition")
    default com.humanrsc.datamodel.entities.JobPosition resolvePosition(String id, @Context EntityResolverService resolver) {
        return resolver.resolveJobPosition(id);
    }

    @Named("resolveUnit")
    default com.humanrsc.datamodel.entities.OrganizationalUnit resolveUnit(String id, @Context EntityResolverService resolver) {
        return resolver.resolveOrganizationalUnit(id);
    }

    @Named("resolveManager")
    default com.humanrsc.datamodel.entities.Employee resolveManager(String id, @Context EntityResolverService resolver) {
        return resolver.resolveEmployee(id);
    }
}
```

**Key MapStruct features used:**
- `componentModel = "cdi"` → Makes mapper a CDI bean (`@ApplicationScoped`)
- `uses = {EntityResolverService.class}` → Inject resolver service
- `@Context` → Pass service instance to custom resolver methods
- `@Named("...")` → Identify which custom method to use
- `@MappingTarget` → Update existing entity in-place
- `nullValuePropertyMappingStrategy = IGNORE` → Skip null fields in updates

#### 3.3 Complete All 10 Mappers

Create similar mappers for:
- `PositionCategoryMapper`
- `CurrencyExchangeRateMapper`
- `TenantMapper`
- `UserMapper`
- `OrganizationalUnitMapper` (with parent unit resolver)
- `JobPositionMapper` (with unit + category resolver)
- `TemporaryReplacementMapper` (with employee + position resolver)
- `SalaryHistoryMapper` (with employee + approver resolver)

---

### Phase 4: Update Service Layer (Day 12-14)

#### 4.1 Inject Mappers in Services

**OrganizationService.java:**
```java
@ApplicationScoped
public class OrganizationService {

    // Existing repositories...
    @Inject EmployeeRepository employeeRepository;

    // NEW: Inject MapStruct mappers
    @Inject EmployeeMapper employeeMapper;
    @Inject EmployeeAssignmentMapper assignmentMapper;
    @Inject OrganizationalUnitMapper unitMapper;
    @Inject JobPositionMapper positionMapper;
    @Inject PositionCategoryMapper categoryMapper;
    @Inject TemporaryReplacementMapper replacementMapper;
    @Inject SalaryHistoryMapper salaryMapper;
    @Inject CurrencyExchangeRateMapper currencyMapper;

    // ... rest of service
}
```

#### 4.2 Replace Manual Mapping Methods

**BEFORE (51 lines):**
```java
@Transactional
public void updateJobPositionFromDTO(ObjectID objectID, JobPositionDTO dto) {
    Optional<JobPosition> positionOpt = jobPositionRepository.findById(objectID);
    if (positionOpt.isEmpty()) {
        throw new ResourceNotFoundException("JobPosition", objectID.getId());
    }

    JobPosition position = positionOpt.get();

    // Update fields
    if (dto.getTitle() != null && !dto.getTitle().trim().isEmpty()) {
        position.setTitle(dto.getTitle());
    }
    if (dto.getDescription() != null) {
        position.setDescription(dto.getDescription());
    }
    // ... 15 more similar blocks ...

    // Resolve relationships
    if (dto.getUnitId() != null && !dto.getUnitId().trim().isEmpty()) {
        Optional<OrganizationalUnit> unit = organizationalUnitRepository.findById(dto.getUnitId());
        if (unit.isPresent()) {
            position.setUnit(unit.get());
        } else {
            throw new JobPositionValidationException("unitId", "UNIT_NOT_FOUND", 
                "Unit not found: " + dto.getUnitId());
        }
    }
    // ... similar for category ...

    position.setDateUpdated(LocalDateTime.now());
    jobPositionRepository.persist(position);
}
```

**AFTER (3 lines):**
```java
@Transactional
public void updateJobPositionFromDTO(ObjectID objectID, JobPositionDTO dto) {
    JobPosition position = jobPositionRepository.findById(objectID)
            .orElseThrow(() -> new ResourceNotFoundException("JobPosition", objectID.getId()));

    positionMapper.updateFromDTO(dto, position);  // MapStruct does the rest!
    
    position.setDateUpdated(LocalDateTime.now());
    jobPositionRepository.persist(position);
}
```

**Savings:** 51 lines → 7 lines (86% reduction)

#### 4.3 Update All 12 Service Methods

Apply the same pattern to:
- ✅ `updateOrganizationalUnitFromDTO()` → use `unitMapper.updateFromDTO()`
- ✅ `createEmployeeAssignmentFromDTO()` → use `assignmentMapper.toEntity()`
- ✅ `updateEmployeeAssignmentFromDTO()` → use `assignmentMapper.updateFromDTO()`
- ✅ `updateTemporaryReplacementFromDTO()` → use `replacementMapper.updateFromDTO()`
- ✅ `updateSalaryHistoryFromDTO()` → use `salaryMapper.updateFromDTO()`
- ✅ NEW: `createEmployee(CreateEmployeeDTO)` → use `employeeMapper.toEntity()`
- ✅ NEW: `updateEmployee(ObjectID, UpdateEmployeeDTO)` → use `employeeMapper.updateFromDTO()`
- ... (all others)

---

### Phase 5: Update Resource Endpoints (Day 15-16)

#### 5.1 Update POST Endpoints

**BEFORE (OrganizationResource.java:430):**
```java
@POST
@Path("/employees")
@RolesAllowed({WRITE_PEOPLE})
public Response createEmployee(Employee employee) {  // ⚠️ Entity direct
    try {
        Employee created = organizationService.createEmployee(employee);
        return Response.status(Response.Status.CREATED).entity(created).build();
    } catch (DuplicateResourceException e) {
        // ... 20 lines of error handling ...
    }
}
```

**AFTER:**
```java
@POST
@Path("/employees")
@RolesAllowed({WRITE_PEOPLE})
public Response createEmployee(@Valid CreateEmployeeDTO dto) {  // ✅ DTO
    Employee created = organizationService.createEmployee(dto);
    return Response.status(Response.Status.CREATED).entity(created).build();
    // GlobalExceptionHandler handles exceptions - no local try/catch needed!
}
```

**Key changes:**
1. Accept `CreateEmployeeDTO` instead of `Employee`
2. Add `@Valid` for Bean Validation
3. Remove redundant try/catch (let GlobalExceptionHandler handle it)
4. Service signature changes to `createEmployee(CreateEmployeeDTO dto)`

#### 5.2 Update All 12 Endpoints

Apply same pattern to all POST/PUT endpoints listed in Phase 1.

---

### Phase 6: Testing & Validation (Day 17-20)

#### 6.1 Verify Mapper Generation

```bash
./mvnw clean compile
```

Check `target/generated-sources/annotations/com/humanrsc/datamodel/mappers/` for generated implementations.

#### 6.2 Manual Testing Checklist

For each migrated endpoint:
- ✅ POST with valid DTO → 201 Created
- ✅ POST with invalid DTO → 400 Bad Request (Bean Validation)
- ✅ POST with invalid relationship ID → 404 Not Found
- ✅ PUT with valid DTO → 200 OK
- ✅ PUT with null fields → Existing values unchanged
- ✅ Verify tenantID isolation (multi-tenant test)

#### 6.3 Regression Testing

Run existing integration tests (if any). Update tests to use DTOs instead of entities.

---

### Phase 7: Documentation & Cleanup (Day 21)

#### 7.1 Update OpenAPI Spec

Regenerate `openapi.yaml` to reflect DTO-based endpoints:
```bash
curl http://localhost:8080/q/openapi > openapi.yaml
```

#### 7.2 Delete Manual Mapping Methods

Remove from `OrganizationService.java`:
- ✅ `updateOrganizationalUnitFromDTO()` (30 lines)
- ✅ `updateJobPositionFromDTO()` (41 lines)
- ✅ `createEmployeeAssignmentFromDTO()` (80 lines)
- ✅ `updateEmployeeAssignmentFromDTO()` (65 lines)
- ✅ `updateTemporaryReplacementFromDTO()` (52 lines)
- ✅ `updateSalaryHistoryFromDTO()` (40 lines)

**Total removal:** ~308 lines

#### 7.3 Update README.md

Add MapStruct section:
```markdown
## Mapping Layer

This project uses [MapStruct](https://mapstruct.org/) for type-safe Entity ↔ DTO mapping.

### Mappers

All mappers are located in `src/main/java/com/humanrsc/datamodel/mappers/` and are CDI beans injected into services.

### Custom Entity Resolution

The `EntityResolverService` handles String ID → Entity lookups for all relationship mappings.
```

---

## Rollout Strategy

### Option A: Big Bang Migration (Risky)

Migrate all 12 endpoints in one PR. **Not recommended** due to high risk.

### Option B: Phased Rollout (Recommended)

**Week 1-2:** Foundation
- Add MapStruct dependency
- Create `EntityResolverService`
- Create all missing DTOs
- No endpoint changes yet

**Week 3:** Core Entities
- Migrate `Employee`, `PositionCategory`, `CurrencyExchangeRate` (5 endpoints)
- Deploy & validate in staging

**Week 4:** Organizational Entities
- Migrate `OrganizationalUnit`, `JobPosition` (4 endpoints)
- Deploy & validate in staging

**Week 5:** System Entities
- Migrate `Tenant`, `User` (4 endpoints)
- Deploy & validate in staging

**Week 6:** Cleanup & Finalization
- Delete manual mapping methods
- Final regression tests
- Production deploy

### Option C: Entity-by-Entity (Safest)

Deploy each entity migration independently (10 separate PRs). Takes longer but minimizes risk.

---

## Risks & Mitigation

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Mapper generation fails | Low | High | Add MapStruct to Maven compiler config correctly; test locally first |
| Lombok conflicts | Medium | High | Ensure Lombok is listed BEFORE MapStruct in annotation processors |
| Missing relationship resolvers | Medium | Medium | Use `EntityResolverService` for all ID lookups; throw `ResourceNotFoundException` |
| Breaking API contracts | Low | High | Keep response bodies unchanged (still return entities) |
| Performance regression | Low | Low | MapStruct generates faster code than manual mapping |
| Lost custom validation | Medium | Medium | Keep validation logic in service layer; MapStruct only maps fields |

---

## Success Metrics

### Before Migration

- Manual mapping code: 308 lines
- Setter calls: 71
- Entities used as request bodies: 12 endpoints
- Type safety: Runtime only
- Maintenance burden: High

### After Migration

- Manual mapping code: 0 lines (replaced by ~50 lines of annotations)
- Setter calls: 0 (generated by MapStruct)
- Entities used as request bodies: 0 endpoints
- Type safety: Compile-time
- Maintenance burden: Low

### Expected Outcomes

- ✅ **86% reduction** in mapping code
- ✅ **100% compile-time safety** (no runtime reflection)
- ✅ **2-3x faster** mapping performance (vs reflection-based libraries)
- ✅ **Zero mass-assignment vulnerabilities**
- ✅ **Cleaner service layer** (mappers are separate beans)
- ✅ **Easier testing** (mock mappers independently)

---

## References

- [MapStruct Official Docs](https://mapstruct.org/documentation/stable/reference/html/)
- [MapStruct + Lombok](https://mapstruct.org/documentation/stable/reference/html/#lombok)
- [MapStruct + CDI](https://mapstruct.org/documentation/stable/reference/html/#cdi-support)
- [Quarkus + MapStruct Guide](https://quarkus.io/guides/mapstruct)

---

## Next Steps

1. ✅ Review and approve this migration plan
2. ⏳ Add MapStruct dependency to `pom.xml`
3. ⏳ Create `EntityResolverService`
4. ⏳ Start Phase 2 (create DTOs)

**Approval needed before proceeding.**

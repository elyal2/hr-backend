package com.humanrsc.services;

import com.humanrsc.config.ThreadLocalStorage;
import com.humanrsc.datamodel.abstraction.ObjectID;
import com.humanrsc.datamodel.entities.*;
import com.humanrsc.datamodel.entities.PositionCategory;
import com.humanrsc.datamodel.repo.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;


@ApplicationScoped
public class OrganizationService {

    @Inject PositionCategoryRepository positionCategoryRepository;
    @Inject OrganizationalUnitRepository organizationalUnitRepository;
    @Inject JobPositionRepository jobPositionRepository;
    @Inject EmployeeRepository employeeRepository;
    @Inject EmployeeAssignmentRepository employeeAssignmentRepository;
    @Inject TemporaryReplacementRepository temporaryReplacementRepository;
    @Inject SalaryHistoryRepository salaryHistoryRepository;
    @Inject CurrencyExchangeRateRepository currencyExchangeRateRepository;

    // ========== POSITION CATEGORIES ==========

    @Transactional
    public PositionCategory createPositionCategory(PositionCategory category) {
        if (category.getObjectID() == null) {
            String id = UUID.randomUUID().toString();
            String tenantID = ThreadLocalStorage.getTenantID();
            category.setObjectID(ObjectID.of(id, tenantID));
        }
        
        // Validate name uniqueness
        if (positionCategoryRepository.existsByName(category.getName())) {
            throw new IllegalArgumentException("Category name " + category.getName() + " already exists");
        }
        
        positionCategoryRepository.persist(category);
        return category;
    }

    public Optional<PositionCategory> findCategoryById(String id) {
        return positionCategoryRepository.findById(id);
    }

    public List<PositionCategory> findAllCategories() {
        return positionCategoryRepository.find("status = ?1 order by name", PositionCategory.STATUS_ACTIVE).list();
    }

    public List<PositionCategory> findAllCategories(int page, int size) {
        return positionCategoryRepository.find("status = ?1 order by name", PositionCategory.STATUS_ACTIVE)
               .page(io.quarkus.panache.common.Page.of(page, size))
               .list();
    }

    @Transactional
    public PositionCategory updatePositionCategory(PositionCategory category) {
        // Check if name is being changed and if it conflicts
        Optional<PositionCategory> existing = positionCategoryRepository.findById(category.getObjectID().getId());
        if (existing.isPresent() && !existing.get().getName().equals(category.getName())) {
            if (positionCategoryRepository.existsByName(category.getName())) {
                throw new IllegalArgumentException("Category name " + category.getName() + " already exists");
            }
        }
        
        return positionCategoryRepository.getEntityManager().merge(category);
    }

    @Transactional
    public boolean deletePositionCategory(String id) {
        Optional<PositionCategory> category = positionCategoryRepository.findById(id);
        if (category.isPresent()) {
            // Check if category is being used by positions
            long positionCount = jobPositionRepository.countByCategory(category.get().getObjectID());
            
            if (positionCount > 0) {
                throw new IllegalStateException("Cannot delete category that is being used by " + 
                    positionCount + " positions");
            }
            
            return positionCategoryRepository.deletePositionCategory(category.get().getObjectID());
        }
        return false;
    }

    // ========== ORGANIZATIONAL UNITS ==========

    @Transactional
    public OrganizationalUnit createOrganizationalUnit(OrganizationalUnit unit) {
        if (unit.getObjectID() == null) {
            String id = UUID.randomUUID().toString();
            String tenantID = ThreadLocalStorage.getTenantID();
            unit.setObjectID(ObjectID.of(id, tenantID));
        }
        
        // Validate organizational level
        if (unit.getOrganizationalLevel() != null && unit.getOrganizationalLevel() <= 0) {
            throw new com.humanrsc.exceptions.OrganizationalUnitValidationException("organizationalLevel", "INVALID_ORGANIZATIONAL_LEVEL", 
                "Organizational level must be greater than zero");
        }
        
        // Validate name uniqueness (optional business rule)
        if (unit.getName() != null && !unit.getName().trim().isEmpty()) {
            // You could add a repository method to check name uniqueness if needed
            // if (organizationalUnitRepository.existsByName(unit.getName())) {
            //     throw new DuplicateResourceException("name", unit.getName(), "OrganizationalUnit");
            // }
        }
        
        organizationalUnitRepository.persist(unit);
        return unit;
    }

    public Optional<OrganizationalUnit> findUnitById(String id) {
        return organizationalUnitRepository.findById(id);
    }

    public List<OrganizationalUnit> findAllUnits() {
        return organizationalUnitRepository.find("status = ?1 order by name", OrganizationalUnit.STATUS_ACTIVE).list();
    }

    public List<OrganizationalUnit> findAllUnits(int page, int size) {
        return organizationalUnitRepository.find("status = ?1 order by name", OrganizationalUnit.STATUS_ACTIVE)
               .page(io.quarkus.panache.common.Page.of(page, size))
               .list();
    }

    public List<OrganizationalUnit> findRootUnits() {
        return organizationalUnitRepository.find("status = ?1 and parentUnit is null order by name", 
                   OrganizationalUnit.STATUS_ACTIVE).list();
    }

    public List<OrganizationalUnit> findChildUnits(String parentUnitId) {
        return organizationalUnitRepository.find("status = ?1 and parentUnit.objectID.id = ?2 order by name", 
                   OrganizationalUnit.STATUS_ACTIVE, parentUnitId).list();
    }

    @Transactional
    public OrganizationalUnit updateOrganizationalUnit(OrganizationalUnit unit) {
        return organizationalUnitRepository.getEntityManager().merge(unit);
    }
    
    @Transactional
    public OrganizationalUnit updateOrganizationalUnitFromDTO(String id, com.humanrsc.datamodel.dto.OrganizationalUnitDTO dto) {
        Optional<OrganizationalUnit> existingUnit = organizationalUnitRepository.findById(id);
        if (existingUnit.isEmpty()) {
            throw new IllegalArgumentException("Organizational unit not found: " + id);
        }
        
        OrganizationalUnit unit = existingUnit.get();
        
        // Actualizar campos del DTO
        unit.setName(dto.getName());
        unit.setDescription(dto.getDescription());
        unit.setCostCenter(dto.getCostCenter());
        unit.setLocation(dto.getLocation());
        unit.setCountry(dto.getCountry());
        unit.setStatus(dto.getStatus());
        unit.setOrganizationalLevel(dto.getOrganizationalLevel());
        
        // Manejar parentUnitId
        if (dto.getParentUnitId() != null && !dto.getParentUnitId().trim().isEmpty()) {
            Optional<OrganizationalUnit> parentUnit = organizationalUnitRepository.findById(dto.getParentUnitId());
            if (parentUnit.isPresent()) {
                unit.setParentUnit(parentUnit.get());
            } else {
                throw new IllegalArgumentException("Parent unit not found: " + dto.getParentUnitId());
            }
        } else {
            unit.setParentUnit(null); // Sin padre
        }
        
        return organizationalUnitRepository.getEntityManager().merge(unit);
    }
    
    @Transactional
    public boolean setParentUnit(String unitId, String parentUnitId) {
        try {
            Optional<OrganizationalUnit> unit = organizationalUnitRepository.findById(unitId);
            if (unit.isEmpty()) {
                throw new IllegalArgumentException("Unit not found: " + unitId);
            }
            
            Optional<OrganizationalUnit> parent = organizationalUnitRepository.findById(parentUnitId);
            if (parent.isEmpty()) {
                throw new IllegalArgumentException("Parent unit not found: " + parentUnitId);
            }
            
            OrganizationalUnit unitToUpdate = unit.get();
            unitToUpdate.setParentUnit(parent.get());
            
            organizationalUnitRepository.getEntityManager().merge(unitToUpdate);
            return true;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to set parent unit: " + e.getMessage());
        }
    }
    
    @Transactional
    public boolean deleteOrganizationalUnit(String id) {
        Optional<OrganizationalUnit> unit = organizationalUnitRepository.findById(id);
        if (unit.isPresent()) {
            // Check if unit has child units
            long childCount = organizationalUnitRepository.countByParentUnit(unit.get().getObjectID());
            if (childCount > 0) {
                throw new IllegalStateException("Cannot delete unit that has " + childCount + " child units");
            }
            
            // Check if unit is being used by positions
            long positionCount = jobPositionRepository.countByUnit(unit.get().getObjectID());
            if (positionCount > 0) {
                throw new IllegalStateException("Cannot delete unit that is being used by " + positionCount + " positions");
            }
            
            return organizationalUnitRepository.deleteOrganizationalUnit(unit.get().getObjectID());
        }
        return false;
    }

    // Dynamic filtering methods for units
    public List<OrganizationalUnit> findUnitsWithFilters(java.util.Map<String, Object> filters, int page, int size) {
        return organizationalUnitRepository.findWithFilters(filters, page, size);
    }
    
    public List<OrganizationalUnit> findUnitsWithFilters(java.util.Map<String, Object> filters) {
        return organizationalUnitRepository.findWithFilters(filters);
    }
    
    public long countUnitsWithFilters(java.util.Map<String, Object> filters) {
        return organizationalUnitRepository.countWithFilters(filters);
    }
    
    // Dynamic filtering methods for positions
    public List<JobPosition> findPositionsWithFilters(java.util.Map<String, Object> filters, int page, int size) {
        return jobPositionRepository.findWithFilters(filters, page, size);
    }
    
    public List<JobPosition> findPositionsWithFilters(java.util.Map<String, Object> filters) {
        return jobPositionRepository.findWithFilters(filters);
    }
    
    public long countPositionsWithFilters(java.util.Map<String, Object> filters) {
        return jobPositionRepository.countWithFilters(filters);
    }

    // ========== JOB POSITIONS ==========

    @Transactional
    public JobPosition createJobPosition(JobPosition position) {
        if (position.getObjectID() == null) {
            String id = UUID.randomUUID().toString();
            String tenantID = ThreadLocalStorage.getTenantID();
            position.setObjectID(ObjectID.of(id, tenantID));
        }
        
        // Validate hierarchical level
        if (!isValidHierarchicalLevel(position.getHierarchicalLevel())) {
            throw new com.humanrsc.exceptions.JobPositionValidationException("hierarchicalLevel", "INVALID_HIERARCHICAL_LEVEL", 
                "Hierarchical level must be between " + JobPosition.HIERARCHICAL_LEVEL_1 + " and " + JobPosition.HIERARCHICAL_LEVEL_10);
        }
        
        // Validate title uniqueness (optional business rule)
        if (position.getTitle() != null && !position.getTitle().trim().isEmpty()) {
            // You could add a repository method to check title uniqueness if needed
            // if (jobPositionRepository.existsByTitle(position.getTitle())) {
            //     throw new DuplicateResourceException("title", position.getTitle(), "JobPosition");
            // }
        }
        
        jobPositionRepository.persist(position);
        return position;
    }

    public Optional<JobPosition> findPositionById(String id) {
        return jobPositionRepository.findById(id);
    }

    public List<JobPosition> findAllPositions() {
        return jobPositionRepository.find("status = ?1 order by title", JobPosition.STATUS_ACTIVE).list();
    }

    public List<JobPosition> findAllPositions(int page, int size) {
        return jobPositionRepository.find("status = ?1 order by title", JobPosition.STATUS_ACTIVE)
               .page(io.quarkus.panache.common.Page.of(page, size))
               .list();
    }

    public List<JobPosition> findPositionsByUnit(String unitId) {
        return jobPositionRepository.find("status = ?1 and unit.objectID.id = ?2 order by title", 
                   JobPosition.STATUS_ACTIVE, unitId).list();
    }

    public List<JobPosition> findPositionsByCategory(String categoryId) {
        return jobPositionRepository.find("status = ?1 and category.objectID.id = ?2 order by title", 
                   JobPosition.STATUS_ACTIVE, categoryId).list();
    }

    public List<JobPosition> findPositionsByHierarchicalLevel(Integer level) {
        return jobPositionRepository.find("status = ?1 and hierarchicalLevel = ?2 order by title", 
                   JobPosition.STATUS_ACTIVE, level).list();
    }

    public List<JobPosition> findPositionsByLevelRange(Integer minLevel, Integer maxLevel) {
        return jobPositionRepository.find("status = ?1 and hierarchicalLevel >= ?2 and hierarchicalLevel <= ?3 order by hierarchicalLevel, title", 
                   JobPosition.STATUS_ACTIVE, minLevel, maxLevel).list();
    }

    @Transactional
    public JobPosition updateJobPosition(JobPosition position) {
        position.updateTimestamp();
        return jobPositionRepository.getEntityManager().merge(position);
    }
    
    @Transactional
    public JobPosition updateJobPositionFromDTO(String id, com.humanrsc.datamodel.dto.JobPositionDTO dto) {
        Optional<JobPosition> existingPosition = jobPositionRepository.findById(id);
        if (existingPosition.isEmpty()) {
            throw new IllegalArgumentException("Job position not found: " + id);
        }
        
        JobPosition position = existingPosition.get();
        
        // Actualizar campos del DTO
        position.setTitle(dto.getTitle());
        position.setDescription(dto.getDescription());
        position.setJobCode(dto.getJobCode());
        position.setStatus(dto.getStatus());
        position.setHierarchicalLevel(dto.getHierarchicalLevel());
        
        // Manejar unitId
        if (dto.getUnitId() != null && !dto.getUnitId().trim().isEmpty()) {
            Optional<OrganizationalUnit> unit = organizationalUnitRepository.findById(dto.getUnitId());
            if (unit.isPresent()) {
                position.setUnit(unit.get());
            } else {
                throw new IllegalArgumentException("Unit not found: " + dto.getUnitId());
            }
        } else {
            position.setUnit(null);
        }
        
        // Manejar categoryId
        if (dto.getCategoryId() != null && !dto.getCategoryId().trim().isEmpty()) {
            Optional<PositionCategory> category = positionCategoryRepository.findById(dto.getCategoryId());
            if (category.isPresent()) {
                position.setCategory(category.get());
            } else {
                throw new IllegalArgumentException("Category not found: " + dto.getCategoryId());
            }
        } else {
            position.setCategory(null);
        }
        
        position.updateTimestamp();
        return jobPositionRepository.getEntityManager().merge(position);
    }

    @Transactional
    public boolean deleteJobPosition(String id) {
        Optional<JobPosition> position = jobPositionRepository.findById(id);
        if (position.isPresent()) {
            // Check if position is being used by employees
            long employeeCount = employeeRepository.countByCurrentPosition(position.get().getObjectID());
            if (employeeCount > 0) {
                throw new IllegalStateException("Cannot delete position that is being used by " + employeeCount + " employees");
            }
            
            return jobPositionRepository.deleteJobPosition(position.get().getObjectID());
        }
        return false;
    }

    // ========== EMPLOYEES ==========

    @Transactional
    public Employee createEmployee(Employee employee) {
        if (employee.getObjectID() == null) {
            String id = UUID.randomUUID().toString();
            String tenantID = ThreadLocalStorage.getTenantID();
            employee.setObjectID(ObjectID.of(id, tenantID));
        }
        
        // Validate employee ID uniqueness and check if terminated
        Optional<Employee> existingEmployee = employeeRepository.findByEmployeeId(employee.getEmployeeId());
        if (existingEmployee.isPresent()) {
            Employee existing = existingEmployee.get();
            if (existing.isTerminated()) {
                throw new com.humanrsc.exceptions.EmployeeValidationException("employeeId", "EMPLOYEE_TERMINATED", 
                    String.format("Employee with ID '%s' exists but is terminated (termination date: %s). Cannot create new employee with same ID.", 
                        employee.getEmployeeId(), existing.getTerminationDate()));
            } else if (existing.isResigned()) {
                throw new com.humanrsc.exceptions.EmployeeValidationException("employeeId", "EMPLOYEE_RESIGNED", 
                    String.format("Employee with ID '%s' exists but is resigned (resignation date: %s). Cannot create new employee with same ID.", 
                        employee.getEmployeeId(), existing.getTerminationDate()));
            } else {
                throw new com.humanrsc.exceptions.DuplicateResourceException("employeeId", employee.getEmployeeId(), "Employee");
            }
        }
        
        // Validate email uniqueness
        if (employeeRepository.existsByEmail(employee.getEmail())) {
            throw new com.humanrsc.exceptions.DuplicateResourceException("email", employee.getEmail(), "Employee");
        }
        
        // Validate hire date logic
        if (employee.getHireDate() != null && employee.getDateOfBirth() != null) {
            if (employee.getHireDate().isBefore(employee.getDateOfBirth())) {
                throw new com.humanrsc.exceptions.EmployeeValidationException("hireDate", "HIRE_DATE_BEFORE_BIRTH", 
                    "Hire date cannot be before date of birth");
            }
        }
        
        // Validate hire date is not too far in the future (optional business rule)
        if (employee.getHireDate() != null && employee.getHireDate().isAfter(LocalDate.now().plusYears(1))) {
            throw new com.humanrsc.exceptions.EmployeeValidationException("hireDate", "HIRE_DATE_TOO_FUTURE", 
                "Hire date cannot be more than 1 year in the future");
        }
        
        // Validate salary is positive
        if (employee.getCurrentSalary() != null && employee.getCurrentSalary().compareTo(BigDecimal.ZERO) <= 0) {
            throw new com.humanrsc.exceptions.EmployeeValidationException("currentSalary", "INVALID_SALARY", 
                "Salary must be greater than zero");
        }
        
        // Validate gender if provided
        if (!isValidGender(employee.getGender())) {
            throw new com.humanrsc.exceptions.EmployeeValidationException("gender", "INVALID_GENDER", 
                "Gender must be one of: " + Employee.GENDER_MALE + ", " + Employee.GENDER_FEMALE + ", " + 
                Employee.GENDER_OTHER + ", " + Employee.GENDER_PREFER_NOT_TO_SAY + ", " + Employee.GENDER_NON_BINARY);
        }
        
        // Validate currency
        if (!isValidCurrency(employee.getCurrency())) {
            throw new com.humanrsc.exceptions.EmployeeValidationException("currency", "INVALID_CURRENCY", 
                "Currency must be a valid ISO 4217 code (e.g., EUR, USD, GBP, JPY, etc.)");
        }
        
        employeeRepository.persist(employee);
        return employee;
    }

    public Optional<Employee> findEmployeeById(String id) {
        return employeeRepository.findById(id);
    }

    public Optional<Employee> findEmployeeByEmployeeId(String employeeId) {
        return employeeRepository.findByEmployeeId(employeeId);
    }

    public List<Employee> findAllEmployees() {
        return employeeRepository.findAll().list();
    }

    public List<Employee> findAllEmployees(int page, int size) {
        return employeeRepository.findAll()
               .page(io.quarkus.panache.common.Page.of(page, size))
               .list();
    }
    
    public List<Employee> findActiveEmployees() {
        return employeeRepository.find("status = ?1 order by lastName, firstName", Employee.STATUS_ACTIVE).list();
    }
    
    // Dynamic filtering methods
    public List<Employee> findEmployeesWithFilters(java.util.Map<String, Object> filters) {
        return employeeRepository.findWithFilters(filters);
    }
    
    public List<Employee> findEmployeesWithFilters(java.util.Map<String, Object> filters, int page, int size) {
        return employeeRepository.findWithFilters(filters, page, size);
    }
    
    public long countEmployeesWithFilters(java.util.Map<String, Object> filters) {
        return employeeRepository.countWithFilters(filters);
    }
    
    public List<Employee> findEmployeesWithAdvancedFilters(java.util.Map<String, Object> exactFilters, 
                                                         java.util.Map<String, java.util.Map<String, Object>> rangeFilters,
                                                         int page, int size) {
        return employeeRepository.findWithAdvancedFilters(exactFilters, rangeFilters, page, size);
    }

    @Transactional
    public Employee updateEmployee(Employee employee) {
        employee.updateTimestamp();
        return employeeRepository.getEntityManager().merge(employee);
    }

    @Transactional
    public boolean terminateEmployee(String id, LocalDate terminationDate) {
        Optional<Employee> employee = employeeRepository.findById(id);
        if (employee.isPresent()) {
            return employeeRepository.terminateEmployee(employee.get().getObjectID(), terminationDate);
        }
        return false;
    }

    @Transactional
    public boolean resignEmployee(String id, LocalDate resignationDate) {
        Optional<Employee> employee = employeeRepository.findById(id);
        if (employee.isPresent()) {
            return employeeRepository.resignEmployee(employee.get().getObjectID(), resignationDate);
        }
        return false;
    }

    // ========== EMPLOYEE ASSIGNMENTS ==========



    @Transactional
    public EmployeeAssignment createEmployeeAssignmentFromDTO(com.humanrsc.datamodel.dto.CreateEmployeeAssignmentDTO dto) {
        EmployeeAssignment assignment = new EmployeeAssignment();
        
        // Auto-generate ID
        String id = UUID.randomUUID().toString();
        String tenantID = ThreadLocalStorage.getTenantID();
        assignment.setObjectID(ObjectID.of(id, tenantID));
        
        // Set basic fields
        assignment.setStartDate(dto.getStartDate());
        assignment.setEndDate(dto.getEndDate());
        assignment.setSalary(dto.getSalary());
        assignment.setCurrency(dto.getCurrency());
        assignment.setMovementReason(dto.getMovementReason());
        assignment.setNotes(dto.getNotes());
        
        // Validate and set employee
        if (dto.getEmployeeId() == null || dto.getEmployeeId().trim().isEmpty()) {
            throw new com.humanrsc.exceptions.AssignmentValidationException("employeeId", "MISSING_EMPLOYEE_ID", 
                "Employee ID is required");
        }
        Optional<Employee> employee = employeeRepository.findById(dto.getEmployeeId());
        if (employee.isEmpty()) {
            throw new com.humanrsc.exceptions.AssignmentValidationException("employeeId", "EMPLOYEE_NOT_FOUND", 
                "Employee not found: " + dto.getEmployeeId());
        }
        assignment.setEmployee(employee.get());
        
        // Validate and set position
        if (dto.getPositionId() != null && !dto.getPositionId().trim().isEmpty()) {
            Optional<JobPosition> position = jobPositionRepository.findById(dto.getPositionId());
            if (position.isEmpty()) {
                throw new com.humanrsc.exceptions.AssignmentValidationException("positionId", "POSITION_NOT_FOUND", 
                    "Position not found: " + dto.getPositionId());
            }
            assignment.setPosition(position.get());
        }
        
        // Validate and set unit
        if (dto.getUnitId() != null && !dto.getUnitId().trim().isEmpty()) {
            Optional<OrganizationalUnit> unit = organizationalUnitRepository.findById(dto.getUnitId());
            if (unit.isEmpty()) {
                throw new com.humanrsc.exceptions.AssignmentValidationException("unitId", "UNIT_NOT_FOUND", 
                    "Unit not found: " + dto.getUnitId());
            }
            assignment.setUnit(unit.get());
        }
        
        // Validate and set manager
        if (dto.getManagerId() != null && !dto.getManagerId().trim().isEmpty()) {
            Optional<Employee> manager = employeeRepository.findById(dto.getManagerId());
            if (manager.isEmpty()) {
                throw new com.humanrsc.exceptions.AssignmentValidationException("managerId", "MANAGER_NOT_FOUND", 
                "Manager not found: " + dto.getManagerId());
            }
            assignment.setManager(manager.get());
        }
        
        // Validate start date
        if (assignment.getStartDate() == null) {
            throw new com.humanrsc.exceptions.AssignmentValidationException("startDate", "MISSING_START_DATE", 
                "Start date is required");
        }
        
        // Validate end date logic
        if (assignment.getEndDate() != null && assignment.getStartDate() != null) {
            if (assignment.getEndDate().isBefore(assignment.getStartDate())) {
                throw new com.humanrsc.exceptions.AssignmentValidationException("endDate", "END_DATE_BEFORE_START", 
                    "End date cannot be before start date");
            }
        }
        
        // Validate salary is positive if provided
        if (assignment.getSalary() != null && assignment.getSalary().compareTo(BigDecimal.ZERO) <= 0) {
            throw new com.humanrsc.exceptions.AssignmentValidationException("salary", "INVALID_SALARY", 
                "Salary must be greater than zero");
        }
        
        employeeAssignmentRepository.persist(assignment);
        return assignment;
    }

    public List<EmployeeAssignment> findEmployeeAssignments(String employeeId) {
        return employeeAssignmentRepository.findByEmployee(employeeId);
    }

    public Optional<EmployeeAssignment> findCurrentAssignment(String employeeId) {
        return employeeAssignmentRepository.findCurrentByEmployee(employeeId);
    }

    @Transactional
    public EmployeeAssignment updateEmployeeAssignment(EmployeeAssignment assignment) {
        return employeeAssignmentRepository.getEntityManager().merge(assignment);
    }
    
    @Transactional
    public EmployeeAssignment updateEmployeeAssignmentFromDTO(String id, com.humanrsc.datamodel.dto.EmployeeAssignmentDTO dto) {
        Optional<EmployeeAssignment> existingAssignment = employeeAssignmentRepository.findById(id);
        if (existingAssignment.isEmpty()) {
            throw new IllegalArgumentException("Employee assignment not found: " + id);
        }
        
        EmployeeAssignment assignment = existingAssignment.get();
        
        // Actualizar campos del DTO
        assignment.setStartDate(dto.getStartDate());
        assignment.setEndDate(dto.getEndDate());
        assignment.setSalary(dto.getSalary());
        assignment.setCurrency(dto.getCurrency());
        assignment.setMovementReason(dto.getMovementReason());
        assignment.setNotes(dto.getNotes());
        
        // Manejar employeeId
        if (dto.getEmployeeId() != null && !dto.getEmployeeId().trim().isEmpty()) {
            Optional<Employee> employee = employeeRepository.findById(dto.getEmployeeId());
            if (employee.isPresent()) {
                assignment.setEmployee(employee.get());
            } else {
                throw new IllegalArgumentException("Employee not found: " + dto.getEmployeeId());
            }
        } else {
            throw new IllegalArgumentException("Employee ID is required");
        }
        
        // Manejar positionId
        if (dto.getPositionId() != null && !dto.getPositionId().trim().isEmpty()) {
            Optional<JobPosition> position = jobPositionRepository.findById(dto.getPositionId());
            if (position.isPresent()) {
                assignment.setPosition(position.get());
            } else {
                throw new IllegalArgumentException("Position not found: " + dto.getPositionId());
            }
        } else {
            assignment.setPosition(null);
        }
        
        // Manejar unitId
        if (dto.getUnitId() != null && !dto.getUnitId().trim().isEmpty()) {
            Optional<OrganizationalUnit> unit = organizationalUnitRepository.findById(dto.getUnitId());
            if (unit.isPresent()) {
                assignment.setUnit(unit.get());
            } else {
                throw new IllegalArgumentException("Unit not found: " + dto.getUnitId());
            }
        } else {
            assignment.setUnit(null);
        }
        
        // Manejar managerId
        if (dto.getManagerId() != null && !dto.getManagerId().trim().isEmpty()) {
            Optional<Employee> manager = employeeRepository.findById(dto.getManagerId());
            if (manager.isPresent()) {
                assignment.setManager(manager.get());
            } else {
                throw new IllegalArgumentException("Manager not found: " + dto.getManagerId());
            }
        } else {
            assignment.setManager(null);
        }
        
        return employeeAssignmentRepository.getEntityManager().merge(assignment);
    }

    // ========== TEMPORARY REPLACEMENTS ==========

    @Transactional
    public TemporaryReplacement createTemporaryReplacement(TemporaryReplacement replacement) {
        if (replacement.getObjectID() == null) {
            String id = UUID.randomUUID().toString();
            String tenantID = ThreadLocalStorage.getTenantID();
            replacement.setObjectID(ObjectID.of(id, tenantID));
        }
        
        // Validate original employee exists
        if (replacement.getOriginalEmployee() == null) {
            throw new com.humanrsc.exceptions.EmployeeValidationException("originalEmployee", "MISSING_ORIGINAL_EMPLOYEE", 
                "Original employee is required");
        }
        
        // Validate replacement employee exists
        if (replacement.getReplacementEmployee() == null) {
            throw new com.humanrsc.exceptions.EmployeeValidationException("replacementEmployee", "MISSING_REPLACEMENT_EMPLOYEE", 
                "Replacement employee is required");
        }
        
        // Validate employees are different
        if (replacement.getOriginalEmployee().equals(replacement.getReplacementEmployee())) {
            throw new com.humanrsc.exceptions.EmployeeValidationException("replacementEmployee", "SAME_EMPLOYEE", 
                "Original and replacement employee cannot be the same");
        }
        
        // Validate start date
        if (replacement.getStartDate() == null) {
            throw new com.humanrsc.exceptions.EmployeeValidationException("startDate", "MISSING_START_DATE", 
                "Start date is required");
        }
        
        // Validate end date logic
        if (replacement.getEndDate() != null && replacement.getStartDate() != null) {
            if (replacement.getEndDate().isBefore(replacement.getStartDate())) {
                throw new com.humanrsc.exceptions.EmployeeValidationException("endDate", "END_DATE_BEFORE_START", 
                    "End date cannot be before start date");
            }
        }
        
        temporaryReplacementRepository.persist(replacement);
        return replacement;
    }

    public List<TemporaryReplacement> findTemporaryReplacementsByEmployee(String employeeId) {
        return temporaryReplacementRepository.findByOriginalEmployee(employeeId);
    }

    public List<TemporaryReplacement> findActiveTemporaryReplacements() {
        return temporaryReplacementRepository.findActiveReplacements();
    }

    public List<TemporaryReplacement> findCurrentTemporaryReplacements() {
        return temporaryReplacementRepository.findCurrentReplacements();
    }

    @Transactional
    public TemporaryReplacement updateTemporaryReplacement(TemporaryReplacement replacement) {
        replacement.updateTimestamp();
        return temporaryReplacementRepository.getEntityManager().merge(replacement);
    }
    
    @Transactional
    public TemporaryReplacement updateTemporaryReplacementFromDTO(String id, com.humanrsc.datamodel.dto.TemporaryReplacementDTO dto) {
        Optional<TemporaryReplacement> existingReplacement = temporaryReplacementRepository.findById(id);
        if (existingReplacement.isEmpty()) {
            throw new IllegalArgumentException("Temporary replacement not found: " + id);
        }
        
        TemporaryReplacement replacement = existingReplacement.get();
        
        // Actualizar campos del DTO
        replacement.setStartDate(dto.getStartDate());
        replacement.setEndDate(dto.getEndDate());
        replacement.setReason(dto.getReason());
        replacement.setStatus(dto.getStatus());
        
        // Manejar originalEmployeeId
        if (dto.getOriginalEmployeeId() != null && !dto.getOriginalEmployeeId().trim().isEmpty()) {
            Optional<Employee> originalEmployee = employeeRepository.findById(dto.getOriginalEmployeeId());
            if (originalEmployee.isPresent()) {
                replacement.setOriginalEmployee(originalEmployee.get());
            } else {
                throw new IllegalArgumentException("Original employee not found: " + dto.getOriginalEmployeeId());
            }
        } else {
            throw new IllegalArgumentException("Original employee ID is required");
        }
        
        // Manejar replacementEmployeeId
        if (dto.getReplacementEmployeeId() != null && !dto.getReplacementEmployeeId().trim().isEmpty()) {
            Optional<Employee> replacementEmployee = employeeRepository.findById(dto.getReplacementEmployeeId());
            if (replacementEmployee.isPresent()) {
                replacement.setReplacementEmployee(replacementEmployee.get());
            } else {
                throw new IllegalArgumentException("Replacement employee not found: " + dto.getReplacementEmployeeId());
            }
        } else {
            throw new IllegalArgumentException("Replacement employee ID is required");
        }
        
        // Manejar positionId
        if (dto.getPositionId() != null && !dto.getPositionId().trim().isEmpty()) {
            Optional<JobPosition> position = jobPositionRepository.findById(dto.getPositionId());
            if (position.isPresent()) {
                replacement.setPosition(position.get());
            } else {
                throw new IllegalArgumentException("Position not found: " + dto.getPositionId());
            }
        } else {
            replacement.setPosition(null);
        }
        
        replacement.updateTimestamp();
        return temporaryReplacementRepository.getEntityManager().merge(replacement);
    }

    @Transactional
    public boolean completeTemporaryReplacement(String id) {
        Optional<TemporaryReplacement> replacement = temporaryReplacementRepository.findById(id);
        if (replacement.isPresent()) {
            replacement.get().complete();
            replacement.get().updateTimestamp();
            temporaryReplacementRepository.getEntityManager().merge(replacement.get());
            return true;
        }
        return false;
    }

    @Transactional
    public boolean cancelTemporaryReplacement(String id) {
        Optional<TemporaryReplacement> replacement = temporaryReplacementRepository.findById(id);
        if (replacement.isPresent()) {
            replacement.get().cancel();
            replacement.get().updateTimestamp();
            temporaryReplacementRepository.getEntityManager().merge(replacement.get());
            return true;
        }
        return false;
    }

    // ========== SALARY HISTORY ==========

    @Transactional
    public SalaryHistory createSalaryHistory(SalaryHistory salaryHistory) {
        if (salaryHistory.getObjectID() == null) {
            String id = UUID.randomUUID().toString();
            String tenantID = ThreadLocalStorage.getTenantID();
            salaryHistory.setObjectID(ObjectID.of(id, tenantID));
        }
        
        // Validate employee exists
        if (salaryHistory.getEmployee() == null) {
            throw new com.humanrsc.exceptions.EmployeeValidationException("employee", "MISSING_EMPLOYEE", 
                "Employee is required");
        }
        
        // Validate new salary
        if (salaryHistory.getNewSalary() == null || salaryHistory.getNewSalary().compareTo(BigDecimal.ZERO) <= 0) {
            throw new com.humanrsc.exceptions.EmployeeValidationException("newSalary", "INVALID_SALARY", 
                "New salary must be greater than zero");
        }
        
        // Validate effective date
        if (salaryHistory.getEffectiveDate() == null) {
            throw new com.humanrsc.exceptions.EmployeeValidationException("effectiveDate", "MISSING_EFFECTIVE_DATE", 
                "Effective date is required");
        }
        
        // Validate effective date is not in the future (optional business rule)
        if (salaryHistory.getEffectiveDate().isAfter(LocalDate.now())) {
            throw new com.humanrsc.exceptions.EmployeeValidationException("effectiveDate", "FUTURE_EFFECTIVE_DATE", 
                "Effective date cannot be in the future");
        }
        
        salaryHistoryRepository.persist(salaryHistory);
        return salaryHistory;
    }

    public List<SalaryHistory> findSalaryHistoryByEmployee(String employeeId) {
        return salaryHistoryRepository.findByEmployee(employeeId);
    }

    public List<SalaryHistory> findRecentSalaryChanges() {
        return salaryHistoryRepository.findRecentChanges();
    }

    public List<SalaryHistory> findSalaryIncreases() {
        return salaryHistoryRepository.findIncreases();
    }

    public List<SalaryHistory> findSalaryDecreases() {
        return salaryHistoryRepository.findDecreases();
    }

    @Transactional
    public SalaryHistory updateSalaryHistory(SalaryHistory salaryHistory) {
        return salaryHistoryRepository.getEntityManager().merge(salaryHistory);
    }
    
    @Transactional
    public SalaryHistory updateSalaryHistoryFromDTO(String id, com.humanrsc.datamodel.dto.SalaryHistoryDTO dto) {
        Optional<SalaryHistory> existingHistory = salaryHistoryRepository.findById(id);
        if (existingHistory.isEmpty()) {
            throw new IllegalArgumentException("Salary history not found: " + id);
        }
        
        SalaryHistory history = existingHistory.get();
        
        // Actualizar campos del DTO
        history.setOldSalary(dto.getOldSalary());
        history.setNewSalary(dto.getNewSalary());
        history.setCurrency(dto.getCurrency());
        history.setEffectiveDate(dto.getEffectiveDate());
        history.setReason(dto.getReason());
        
        // Manejar employeeId
        if (dto.getEmployeeId() != null && !dto.getEmployeeId().trim().isEmpty()) {
            Optional<Employee> employee = employeeRepository.findById(dto.getEmployeeId());
            if (employee.isPresent()) {
                history.setEmployee(employee.get());
            } else {
                throw new IllegalArgumentException("Employee not found: " + dto.getEmployeeId());
            }
        } else {
            throw new IllegalArgumentException("Employee ID is required");
        }
        
        // Manejar approvedById
        if (dto.getApprovedById() != null && !dto.getApprovedById().trim().isEmpty()) {
            Optional<Employee> approver = employeeRepository.findById(dto.getApprovedById());
            if (approver.isPresent()) {
                history.setApprovedBy(approver.get());
            } else {
                throw new IllegalArgumentException("Approver not found: " + dto.getApprovedById());
            }
        } else {
            history.setApprovedBy(null);
        }
        
        return salaryHistoryRepository.getEntityManager().merge(history);
    }

    // ========== BUSINESS LOGIC METHODS ==========

    @Transactional
    public void updateEmployeeSalary(String employeeId, BigDecimal newSalary, String reason, String approvedById) {
        Optional<Employee> employee = employeeRepository.findById(employeeId);
        if (employee.isPresent()) {
            Employee emp = employee.get();
            BigDecimal oldSalary = emp.getCurrentSalary();
            
            // Create salary history record
            SalaryHistory salaryHistory = new SalaryHistory();
            salaryHistory.setEmployee(emp);
            salaryHistory.setOldSalary(oldSalary);
            salaryHistory.setNewSalary(newSalary);
            salaryHistory.setEffectiveDate(LocalDate.now());
            salaryHistory.setReason(reason);
            
            if (approvedById != null) {
                Optional<Employee> approver = employeeRepository.findById(approvedById);
                approver.ifPresent(salaryHistory::setApprovedBy);
            }
            
            createSalaryHistory(salaryHistory);
            
            // Update employee salary
            emp.setCurrentSalary(newSalary);
            emp.updateTimestamp();
            employeeRepository.getEntityManager().merge(emp);
        }
    }

    public List<Employee> findEmployeesByManager(String managerId) {
        return employeeAssignmentRepository.findByManager(managerId)
                .stream()
                .filter(EmployeeAssignment::isCurrent)
                .map(EmployeeAssignment::getEmployee)
                .distinct()
                .toList();
    }

    public List<Employee> findEmployeesByUnit(String unitId) {
        return employeeAssignmentRepository.findByUnit(unitId)
                .stream()
                .filter(EmployeeAssignment::isCurrent)
                .map(EmployeeAssignment::getEmployee)
                .distinct()
                .toList();
    }

    public List<Employee> findEmployeesByPosition(String positionId) {
        return employeeAssignmentRepository.findByPosition(positionId)
                .stream()
                .filter(EmployeeAssignment::isCurrent)
                .map(EmployeeAssignment::getEmployee)
                .distinct()
                .toList();
    }

    // ========== ORGANIZATION STATISTICS ==========

    public OrganizationStats getOrganizationStats() {
        long totalEmployees = employeeRepository.count();
        long activeEmployees = employeeRepository.count("status = ?1", Employee.STATUS_ACTIVE);
        long totalUnits = organizationalUnitRepository.count("status = ?1", OrganizationalUnit.STATUS_ACTIVE);
        long totalPositions = jobPositionRepository.count("status = ?1", JobPosition.STATUS_ACTIVE);
        long totalCategories = positionCategoryRepository.count("status = ?1", PositionCategory.STATUS_ACTIVE);
        
        BigDecimal avgSalary = employeeRepository.getAverageSalary();
        BigDecimal maxSalary = employeeRepository.getMaxSalary();
        BigDecimal minSalary = employeeRepository.getMinSalary();
        
        return new OrganizationStats(
            totalEmployees, activeEmployees, totalUnits, totalPositions, totalCategories,
            avgSalary, maxSalary, minSalary
        );
    }

    // ========== COUNT METHODS FOR DASHBOARD ==========

    // Employee counts
    public long countEmployees() {
        return employeeRepository.count();
    }

    public long countEmployeesByStatus(String status) {
        return employeeRepository.count("status = ?1", status);
    }

    // Unit counts
    public long countUnits() {
        return organizationalUnitRepository.count();
    }

    public long countActiveUnits() {
        return organizationalUnitRepository.count("status = ?1", OrganizationalUnit.STATUS_ACTIVE);
    }

    public long countInactiveUnits() {
        return organizationalUnitRepository.count("status = ?1", OrganizationalUnit.STATUS_INACTIVE);
    }

    public long countDeletedUnits() {
        return organizationalUnitRepository.count("status = ?1", OrganizationalUnit.STATUS_DELETED);
    }

    public long countUnitsByStatus(String status) {
        return organizationalUnitRepository.count("status = ?1", status);
    }

    public long countRootUnits() {
        return organizationalUnitRepository.count("status = ?1 and parentUnit is null", OrganizationalUnit.STATUS_ACTIVE);
    }

    public long countUnitsByLevel(Integer level) {
        return organizationalUnitRepository.count("status = ?1 and organizationalLevel = ?2", 
            OrganizationalUnit.STATUS_ACTIVE, level);
    }

    public long countUnitsWithChildren() {
        return organizationalUnitRepository.count("status = ?1 and objectID in " +
            "(select distinct ou.parentUnit.objectID from OrganizationalUnit ou where ou.parentUnit is not null)", 
            OrganizationalUnit.STATUS_ACTIVE);
    }

    public long countLeafUnits() {
        return organizationalUnitRepository.count("status = ?1 and objectID not in " +
            "(select distinct ou.parentUnit.objectID from OrganizationalUnit ou where ou.parentUnit is not null)", 
            OrganizationalUnit.STATUS_ACTIVE);
    }

    // Organizational statistics
    public OrganizationStructureStats getOrganizationStructureStats() {
        long totalUnits = organizationalUnitRepository.count("status = ?1", OrganizationalUnit.STATUS_ACTIVE);
        long rootUnits = countRootUnits();
        long unitsWithChildren = countUnitsWithChildren();
        long leafUnits = countLeafUnits();
        
        // Count by levels (1-10)
        Map<Integer, Long> unitsByLevel = new HashMap<>();
        for (int level = 1; level <= 10; level++) {
            long count = countUnitsByLevel(level);
            if (count > 0) {
                unitsByLevel.put(level, count);
            }
        }
        
        // Find max level used (highest in hierarchy - level 1 is root)
        int maxLevel = unitsByLevel.keySet().stream()
            .mapToInt(Integer::intValue)
            .min()
            .orElse(0);
        
        // Find min level used (lowest in hierarchy - deepest level)
        int minLevel = unitsByLevel.keySet().stream()
            .mapToInt(Integer::intValue)
            .max()
            .orElse(0);
        
        return new OrganizationStructureStats(
            totalUnits, rootUnits, unitsWithChildren, leafUnits, 
            maxLevel, minLevel, unitsByLevel
        );
    }

    // Position counts
    public long countPositions() {
        return jobPositionRepository.count();
    }

    public long countActivePositions() {
        return jobPositionRepository.count("status = ?1", JobPosition.STATUS_ACTIVE);
    }

    public long countInactivePositions() {
        return jobPositionRepository.count("status = ?1", JobPosition.STATUS_INACTIVE);
    }

    public long countDeletedPositions() {
        return jobPositionRepository.count("status = ?1", JobPosition.STATUS_DELETED);
    }

    public long countPositionsByStatus(String status) {
        return jobPositionRepository.count("status = ?1", status);
    }

    public long countVacantPositions() {
        return jobPositionRepository.count("status = ?1 and objectID not in " +
                   "(select ea.position.objectID from EmployeeAssignment ea " +
                   "where ea.endDate is null)", 
                   JobPosition.STATUS_ACTIVE);
    }

    // Category counts
    public long countPositionCategories() {
        return positionCategoryRepository.count();
    }

    public long countActivePositionCategories() {
        return positionCategoryRepository.count("status = ?1", PositionCategory.STATUS_ACTIVE);
    }

    public long countInactivePositionCategories() {
        return positionCategoryRepository.count("status = ?1", PositionCategory.STATUS_INACTIVE);
    }

    public long countDeletedPositionCategories() {
        return positionCategoryRepository.count("status = ?1", PositionCategory.STATUS_DELETED);
    }

    public long countPositionCategoriesByStatus(String status) {
        return positionCategoryRepository.count("status = ?1", status);
    }

    // Assignment counts
    public long countAssignments() {
        return employeeAssignmentRepository.count();
    }

    public long countActiveAssignments() {
        return employeeAssignmentRepository.count("endDate is null");
    }

    public long countHistoricalAssignments() {
        return employeeAssignmentRepository.count("endDate is not null");
    }

    // Assignment statistics
    public AssignmentStats getAssignmentStats() {
        long totalEmployees = employeeRepository.count("status = ?1", Employee.STATUS_ACTIVE);
        long activeAssignments = employeeAssignmentRepository.count("endDate is null");
        long employeesWithAssignments = employeeRepository.count("status = ?1 and objectID in " +
            "(select distinct ea.employee.objectID from EmployeeAssignment ea where ea.endDate is null)", 
            Employee.STATUS_ACTIVE);
        
        double assignmentPercentage = totalEmployees > 0 ? 
            (double) employeesWithAssignments / totalEmployees * 100 : 0.0;
        
        return new AssignmentStats(activeAssignments, employeesWithAssignments, totalEmployees, assignmentPercentage);
    }

    // Salary statistics
    public SalaryStats getSalaryStats() {
        // Get all active employees with salary
        List<Employee> activeEmployees = employeeRepository.find("status = ?1 and currentSalary is not null", Employee.STATUS_ACTIVE).list();
        
        BigDecimal totalBudgetEUR = BigDecimal.ZERO;
        BigDecimal totalSalaryEUR = BigDecimal.ZERO;
        BigDecimal maxSalaryEUR = BigDecimal.ZERO;
        BigDecimal minSalaryEUR = BigDecimal.valueOf(Double.MAX_VALUE);
        Map<String, Long> currencyDistribution = new HashMap<>();
        
        for (Employee employee : activeEmployees) {
            BigDecimal salary = employee.getCurrentSalary();
            String currency = employee.getCurrency();
            
            // Count currency distribution
            currencyDistribution.merge(currency, 1L, Long::sum);
            
            // Convert to EUR for calculations
            BigDecimal salaryEUR = currencyExchangeRateRepository.convertAmount(salary, currency, "EUR");
            
            totalSalaryEUR = totalSalaryEUR.add(salaryEUR);
            
            if (salaryEUR.compareTo(maxSalaryEUR) > 0) {
                maxSalaryEUR = salaryEUR;
            }
            
            if (salaryEUR.compareTo(minSalaryEUR) < 0) {
                minSalaryEUR = salaryEUR;
            }
        }
        
        totalBudgetEUR = totalSalaryEUR;
        
        // Calculate average
        BigDecimal avgSalaryEUR = activeEmployees.isEmpty() ? BigDecimal.ZERO : 
            totalSalaryEUR.divide(BigDecimal.valueOf(activeEmployees.size()), 2, java.math.RoundingMode.HALF_UP);
        
        // Set min salary to 0 if no employees found
        if (minSalaryEUR.compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) == 0) {
            minSalaryEUR = BigDecimal.ZERO;
        }
        
        return new SalaryStats(totalBudgetEUR, avgSalaryEUR, maxSalaryEUR, minSalaryEUR, "EUR", currencyDistribution);
    }

    public EmployeeStats getEmployeeStats() {
        long totalEmployees = employeeRepository.count();
        long activeEmployees = employeeRepository.count("status = ?1", Employee.STATUS_ACTIVE);
        long inactiveEmployees = employeeRepository.count("status = ?1", Employee.STATUS_INACTIVE);
        long terminatedEmployees = employeeRepository.count("status = ?1", Employee.STATUS_TERMINATED);
        long resignedEmployees = employeeRepository.count("status = ?1", Employee.STATUS_RESIGNED);
        
        long fullTimeEmployees = employeeRepository.count("contractType = ?1", Employee.CONTRACT_TYPE_FULL_TIME);
        long partTimeEmployees = employeeRepository.count("contractType = ?1", Employee.CONTRACT_TYPE_PART_TIME);
        long contractors = employeeRepository.count("employeeType = ?1", Employee.EMPLOYEE_TYPE_CONTRACTOR);
        long interns = employeeRepository.count("employeeType = ?1", Employee.EMPLOYEE_TYPE_INTERN);
        
        return new EmployeeStats(
            totalEmployees, activeEmployees, inactiveEmployees, terminatedEmployees, resignedEmployees,
            fullTimeEmployees, partTimeEmployees, contractors, interns
        );
    }

    // ========== ORGANIZATION CHART ==========

        public SimpleOrganizationChart getOrganizationChart() {
        List<PositionCategory> categories = positionCategoryRepository.findAllActive();
        List<Object[]> unitsData = organizationalUnitRepository.getUnitsWithCounts();

        List<UnitWithCounts> units = unitsData.stream()
                .map(this::mapToUnitWithCounts)
                .toList();

        return new SimpleOrganizationChart(units, categories);
    }

    // ========== ORGANIZATIONAL LEVELS ==========

    public List<OrganizationalUnit> findUnitsByOrganizationalLevel(Integer level) {
        return organizationalUnitRepository.find("status = ?1 and organizationalLevel = ?2 order by name", 
                   OrganizationalUnit.STATUS_ACTIVE, level).list();
    }

    public List<OrganizationalUnit> findUnitsByOrganizationalLevelRange(Integer minLevel, Integer maxLevel) {
        return organizationalUnitRepository.find("status = ?1 and organizationalLevel between ?2 and ?3 order by organizationalLevel, name", 
                   OrganizationalUnit.STATUS_ACTIVE, minLevel, maxLevel).list();
    }

    public long countUnitsByOrganizationalLevel(Integer level) {
        return organizationalUnitRepository.count("status = ?1 and organizationalLevel = ?2", 
                    OrganizationalUnit.STATUS_ACTIVE, level);
    }

    private UnitWithCounts mapToUnitWithCounts(Object[] row) {
        String id = (String) row[0];
        String name = (String) row[2];
        String description = (String) row[3];
        String costCenter = (String) row[4];
        String location = (String) row[5];
        String country = (String) row[6];
        String status = (String) row[7];
        String parentUnitId = (String) row[10];
        Integer organizationalLevel = (Integer) row[11];
        Long employeeCount = (Long) row[12];
        Long positionCount = (Long) row[13];
        Integer minLevel = (Integer) row[14];
        Integer maxLevel = (Integer) row[15];
        
        return new UnitWithCounts(
            id, name, description, costCenter, location, country, status, parentUnitId,
            employeeCount != null ? employeeCount : 0L,
            positionCount != null ? positionCount : 0L,
            minLevel, maxLevel, organizationalLevel
        );
    }



    // ========== INNER CLASSES ==========

    public static class OrganizationStats {
        private final long totalEmployees;
        private final long activeEmployees;
        private final long totalUnits;
        private final long totalPositions;
        private final long totalCategories;
        private final BigDecimal avgSalary;
        private final BigDecimal maxSalary;
        private final BigDecimal minSalary;

        public OrganizationStats(long totalEmployees, long activeEmployees, long totalUnits, 
                               long totalPositions, long totalCategories, BigDecimal avgSalary, 
                               BigDecimal maxSalary, BigDecimal minSalary) {
            this.totalEmployees = totalEmployees;
            this.activeEmployees = activeEmployees;
            this.totalUnits = totalUnits;
            this.totalPositions = totalPositions;
            this.totalCategories = totalCategories;
            this.avgSalary = avgSalary;
            this.maxSalary = maxSalary;
            this.minSalary = minSalary;
        }

        // Getters
        public long getTotalEmployees() { return totalEmployees; }
        public long getActiveEmployees() { return activeEmployees; }
        public long getTotalUnits() { return totalUnits; }
        public long getTotalPositions() { return totalPositions; }
        public long getTotalCategories() { return totalCategories; }
        public BigDecimal getAvgSalary() { return avgSalary; }
        public BigDecimal getMaxSalary() { return maxSalary; }
        public BigDecimal getMinSalary() { return minSalary; }
    }

    public static class EmployeeStats {
        private final long totalEmployees;
        private final long activeEmployees;
        private final long inactiveEmployees;
        private final long terminatedEmployees;
        private final long resignedEmployees;
        private final long fullTimeEmployees;
        private final long partTimeEmployees;
        private final long contractors;
        private final long interns;

        public EmployeeStats(long totalEmployees, long activeEmployees, long inactiveEmployees,
                           long terminatedEmployees, long resignedEmployees, long fullTimeEmployees,
                           long partTimeEmployees, long contractors, long interns) {
            this.totalEmployees = totalEmployees;
            this.activeEmployees = activeEmployees;
            this.inactiveEmployees = inactiveEmployees;
            this.terminatedEmployees = terminatedEmployees;
            this.resignedEmployees = resignedEmployees;
            this.fullTimeEmployees = fullTimeEmployees;
            this.partTimeEmployees = partTimeEmployees;
            this.contractors = contractors;
            this.interns = interns;
        }

        // Getters
        public long getTotalEmployees() { return totalEmployees; }
        public long getActiveEmployees() { return activeEmployees; }
        public long getInactiveEmployees() { return inactiveEmployees; }
        public long getTerminatedEmployees() { return terminatedEmployees; }
        public long getResignedEmployees() { return resignedEmployees; }
        public long getFullTimeEmployees() { return fullTimeEmployees; }
        public long getPartTimeEmployees() { return partTimeEmployees; }
        public long getContractors() { return contractors; }
        public long getInterns() { return interns; }
    }

    public static class SalaryStats {
        private final BigDecimal totalBudget;
        private final BigDecimal avgSalary;
        private final BigDecimal maxSalary;
        private final BigDecimal minSalary;
        private final String primaryCurrency;
        private final Map<String, Long> currencyDistribution;

        public SalaryStats(BigDecimal totalBudget, BigDecimal avgSalary, BigDecimal maxSalary, BigDecimal minSalary, 
                          String primaryCurrency, Map<String, Long> currencyDistribution) {
            this.totalBudget = totalBudget;
            this.avgSalary = avgSalary;
            this.maxSalary = maxSalary;
            this.minSalary = minSalary;
            this.primaryCurrency = primaryCurrency;
            this.currencyDistribution = currencyDistribution;
        }

        // Getters
        public BigDecimal getTotalBudget() { return totalBudget; }
        public BigDecimal getAvgSalary() { return avgSalary; }
        public BigDecimal getMaxSalary() { return maxSalary; }
        public BigDecimal getMinSalary() { return minSalary; }
        public String getPrimaryCurrency() { return primaryCurrency; }
        public Map<String, Long> getCurrencyDistribution() { return currencyDistribution; }
    }

    public static class AssignmentStats {
        private final long activeAssignments;
        private final long employeesWithAssignments;
        private final long totalEmployees;
        private final double assignmentPercentage;

        public AssignmentStats(long activeAssignments, long employeesWithAssignments, long totalEmployees, double assignmentPercentage) {
            this.activeAssignments = activeAssignments;
            this.employeesWithAssignments = employeesWithAssignments;
            this.totalEmployees = totalEmployees;
            this.assignmentPercentage = assignmentPercentage;
        }

        // Getters
        public long getActiveAssignments() { return activeAssignments; }
        public long getEmployeesWithAssignments() { return employeesWithAssignments; }
        public long getTotalEmployees() { return totalEmployees; }
        public double getAssignmentPercentage() { return assignmentPercentage; }
    }

    public static class OrganizationStructureStats {
        private final long totalUnits;
        private final long rootUnits;
        private final long unitsWithChildren;
        private final long leafUnits;
        private final int maxLevel;
        private final int minLevel;
        private final Map<Integer, Long> unitsByLevel;

        public OrganizationStructureStats(long totalUnits, long rootUnits, long unitsWithChildren, long leafUnits, int maxLevel, int minLevel, Map<Integer, Long> unitsByLevel) {
            this.totalUnits = totalUnits;
            this.rootUnits = rootUnits;
            this.unitsWithChildren = unitsWithChildren;
            this.leafUnits = leafUnits;
            this.maxLevel = maxLevel;
            this.minLevel = minLevel;
            this.unitsByLevel = unitsByLevel;
        }

        // Getters
        public long getTotalUnits() { return totalUnits; }
        public long getRootUnits() { return rootUnits; }
        public long getUnitsWithChildren() { return unitsWithChildren; }
        public long getLeafUnits() { return leafUnits; }
        public int getMaxLevel() { return maxLevel; }
        public int getMinLevel() { return minLevel; }
        public Map<Integer, Long> getUnitsByLevel() { return unitsByLevel; }
    }

    public static class UnitWithCounts {
        private final String id;
        private final String name;
        private final String description;
        private final String costCenter;
        private final String location;
        private final String country;
        private final String status;
        private final String parentUnitId;
        private final long employeeCount;
        private final long positionCount;
        private final Integer minHierarchicalLevel;
        private final Integer maxHierarchicalLevel;
        private final boolean hasPositions;
        private final Integer organizationalLevel;

        public UnitWithCounts(String id, String name, String description, String costCenter,
                            String location, String country, String status, String parentUnitId,
                            long employeeCount, long positionCount, 
                            Integer minHierarchicalLevel, Integer maxHierarchicalLevel,
                            Integer organizationalLevel) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.costCenter = costCenter;
            this.location = location;
            this.country = country;
            this.status = status;
            this.parentUnitId = parentUnitId;
            this.employeeCount = employeeCount;
            this.positionCount = positionCount;
            this.minHierarchicalLevel = minHierarchicalLevel;
            this.maxHierarchicalLevel = maxHierarchicalLevel;
            this.hasPositions = positionCount > 0;
            this.organizationalLevel = organizationalLevel;
        }

        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getCostCenter() { return costCenter; }
        public String getLocation() { return location; }
        public String getCountry() { return country; }
        public String getStatus() { return status; }
        public String getParentUnitId() { return parentUnitId; }
        public long getEmployeeCount() { return employeeCount; }
        public long getPositionCount() { return positionCount; }
        public Integer getMinHierarchicalLevel() { return minHierarchicalLevel; }
        public Integer getMaxHierarchicalLevel() { return maxHierarchicalLevel; }
        public boolean isHasPositions() { return hasPositions; }
        public Integer getOrganizationalLevel() { return organizationalLevel; }
    }

    public static class SimpleOrganizationChart {
        private final List<UnitWithCounts> units;
        private final List<PositionCategory> categories;

        public SimpleOrganizationChart(List<UnitWithCounts> units, List<PositionCategory> categories) {
            this.units = units;
            this.categories = categories;
        }

        public List<UnitWithCounts> getUnits() { return units; }
        public List<PositionCategory> getCategories() { return categories; }
    }

    // Validation helper methods for standardized fields
    
    private boolean isValidGender(String gender) {
        if (gender == null || gender.trim().isEmpty()) {
            return true; // Gender is optional
        }
        return gender.equals(Employee.GENDER_MALE) ||
               gender.equals(Employee.GENDER_FEMALE) ||
               gender.equals(Employee.GENDER_OTHER) ||
               gender.equals(Employee.GENDER_PREFER_NOT_TO_SAY) ||
               gender.equals(Employee.GENDER_NON_BINARY);
    }
    
    private boolean isValidCurrency(String currency) {
        if (currency == null || currency.trim().isEmpty()) {
            return false; // Currency is required
        }
        return currency.equals(Employee.CURRENCY_EUR) ||
               currency.equals(Employee.CURRENCY_USD) ||
               currency.equals(Employee.CURRENCY_GBP) ||
               currency.equals(Employee.CURRENCY_JPY) ||
               currency.equals(Employee.CURRENCY_CAD) ||
               currency.equals(Employee.CURRENCY_AUD) ||
               currency.equals(Employee.CURRENCY_CHF) ||
               currency.equals(Employee.CURRENCY_CNY) ||
               currency.equals(Employee.CURRENCY_INR) ||
               currency.equals(Employee.CURRENCY_BRL) ||
               currency.equals(Employee.CURRENCY_MXN) ||
               currency.equals(Employee.CURRENCY_ARS) ||
               currency.equals(Employee.CURRENCY_CLP) ||
               currency.equals(Employee.CURRENCY_COP) ||
               currency.equals(Employee.CURRENCY_PEN) ||
               currency.equals(Employee.CURRENCY_UYU) ||
               currency.equals(Employee.CURRENCY_VES);
    }
    

    
    private boolean isValidHierarchicalLevel(Integer level) {
        if (level == null) {
            return false; // Level is required
        }
        return level >= JobPosition.HIERARCHICAL_LEVEL_1 && level <= JobPosition.HIERARCHICAL_LEVEL_10;
    }


}

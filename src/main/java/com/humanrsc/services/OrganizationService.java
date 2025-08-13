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


@ApplicationScoped
public class OrganizationService {

    @Inject PositionCategoryRepository positionCategoryRepository;
    @Inject OrganizationalUnitRepository organizationalUnitRepository;
    @Inject JobPositionRepository jobPositionRepository;
    @Inject EmployeeRepository employeeRepository;
    @Inject EmployeeAssignmentRepository employeeAssignmentRepository;
    @Inject TemporaryReplacementRepository temporaryReplacementRepository;
    @Inject SalaryHistoryRepository salaryHistoryRepository;

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
        return positionCategoryRepository.findAllActive();
    }

    public List<PositionCategory> findAllCategories(int page, int size) {
        return positionCategoryRepository.findActivePage(page, size);
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
        
        organizationalUnitRepository.persist(unit);
        return unit;
    }

    public Optional<OrganizationalUnit> findUnitById(String id) {
        return organizationalUnitRepository.findById(id);
    }

    public List<OrganizationalUnit> findAllUnits() {
        return organizationalUnitRepository.findAllActive();
    }

    public List<OrganizationalUnit> findAllUnits(int page, int size) {
        return organizationalUnitRepository.findActivePage(page, size);
    }

    public List<OrganizationalUnit> findRootUnits() {
        return organizationalUnitRepository.findRootUnits();
    }

    public List<OrganizationalUnit> findChildUnits(String parentUnitId) {
        return organizationalUnitRepository.findByParentUnit(parentUnitId);
    }

    @Transactional
    public OrganizationalUnit updateOrganizationalUnit(OrganizationalUnit unit) {
        return organizationalUnitRepository.getEntityManager().merge(unit);
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

    // ========== JOB POSITIONS ==========

    @Transactional
    public JobPosition createJobPosition(JobPosition position) {
        if (position.getObjectID() == null) {
            String id = UUID.randomUUID().toString();
            String tenantID = ThreadLocalStorage.getTenantID();
            position.setObjectID(ObjectID.of(id, tenantID));
        }
        
        jobPositionRepository.persist(position);
        return position;
    }

    public Optional<JobPosition> findPositionById(String id) {
        return jobPositionRepository.findById(id);
    }

    public List<JobPosition> findAllPositions() {
        return jobPositionRepository.findAllActive();
    }

    public List<JobPosition> findAllPositions(int page, int size) {
        return jobPositionRepository.findActivePage(page, size);
    }

    public List<JobPosition> findPositionsByUnit(String unitId) {
        return jobPositionRepository.findByUnit(unitId);
    }

    public List<JobPosition> findPositionsByCategory(String categoryId) {
        return jobPositionRepository.findByCategory(categoryId);
    }

    public List<JobPosition> findPositionsByHierarchicalLevel(Integer level) {
        return jobPositionRepository.findByHierarchicalLevel(level);
    }

    public List<JobPosition> findPositionsByLevelRange(Integer minLevel, Integer maxLevel) {
        return jobPositionRepository.findByLevelRange(minLevel, maxLevel);
    }

    @Transactional
    public JobPosition updateJobPosition(JobPosition position) {
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
        
        // Validate employee ID uniqueness
        if (employeeRepository.existsByEmployeeId(employee.getEmployeeId())) {
            throw new IllegalArgumentException("Employee ID " + employee.getEmployeeId() + " already exists");
        }
        
        // Validate email uniqueness
        if (employeeRepository.existsByEmail(employee.getEmail())) {
            throw new IllegalArgumentException("Email " + employee.getEmail() + " already exists");
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
        return employeeRepository.findAllEmployed();
    }

    public List<Employee> findAllEmployees(int page, int size) {
        return employeeRepository.findAllEmployedPage(page, size);
    }

    public List<Employee> findActiveEmployees() {
        return employeeRepository.findAllActive();
    }

    public List<Employee> findEmployeesByStatus(String status) {
        return employeeRepository.findByStatus(status);
    }

    public List<Employee> findEmployeesByType(Employee.EmployeeType employeeType) {
        return employeeRepository.findByEmployeeType(employeeType);
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
    public EmployeeAssignment createEmployeeAssignment(EmployeeAssignment assignment) {
        if (assignment.getObjectID() == null) {
            String id = UUID.randomUUID().toString();
            String tenantID = ThreadLocalStorage.getTenantID();
            assignment.setObjectID(ObjectID.of(id, tenantID));
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

    // ========== TEMPORARY REPLACEMENTS ==========

    @Transactional
    public TemporaryReplacement createTemporaryReplacement(TemporaryReplacement replacement) {
        if (replacement.getObjectID() == null) {
            String id = UUID.randomUUID().toString();
            String tenantID = ThreadLocalStorage.getTenantID();
            replacement.setObjectID(ObjectID.of(id, tenantID));
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
        long totalEmployees = employeeRepository.countTotal();
        long activeEmployees = employeeRepository.countActive();
        long totalUnits = organizationalUnitRepository.countActive();
        long totalPositions = jobPositionRepository.countActive();
        long totalCategories = positionCategoryRepository.countActive();
        
        BigDecimal avgSalary = employeeRepository.getAverageSalary();
        BigDecimal maxSalary = employeeRepository.getMaxSalary();
        BigDecimal minSalary = employeeRepository.getMinSalary();
        
        return new OrganizationStats(
            totalEmployees, activeEmployees, totalUnits, totalPositions, totalCategories,
            avgSalary, maxSalary, minSalary
        );
    }

    public EmployeeStats getEmployeeStats() {
        long totalEmployees = employeeRepository.countTotal();
        long activeEmployees = employeeRepository.countActive();
        long inactiveEmployees = employeeRepository.countByStatus(Employee.STATUS_INACTIVE);
        long terminatedEmployees = employeeRepository.countByStatus(Employee.STATUS_TERMINATED);
        long resignedEmployees = employeeRepository.countByStatus(Employee.STATUS_RESIGNED);
        
        long fullTimeEmployees = employeeRepository.countByContractType(Employee.ContractType.FULL_TIME);
        long partTimeEmployees = employeeRepository.countByContractType(Employee.ContractType.PART_TIME);
        long contractors = employeeRepository.countByEmployeeType(Employee.EmployeeType.CONTRACTOR);
        long interns = employeeRepository.countByEmployeeType(Employee.EmployeeType.INTERN);
        
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
        return organizationalUnitRepository.findByOrganizationalLevel(level);
    }

    public List<OrganizationalUnit> findUnitsByOrganizationalLevelRange(Integer minLevel, Integer maxLevel) {
        return organizationalUnitRepository.findByOrganizationalLevelRange(minLevel, maxLevel);
    }

    public long countUnitsByOrganizationalLevel(Integer level) {
        return organizationalUnitRepository.countByOrganizationalLevel(level);
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


}

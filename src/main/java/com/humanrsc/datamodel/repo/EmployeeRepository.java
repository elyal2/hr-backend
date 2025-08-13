package com.humanrsc.datamodel.repo;

import com.humanrsc.datamodel.abstraction.ObjectID;
import com.humanrsc.datamodel.entities.Employee;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class EmployeeRepository implements PanacheRepositoryBase<Employee, ObjectID> {

    // Usar métodos estándar de PanacheRepositoryBase

    public Optional<Employee> findByObjectID(ObjectID objectID) {
        return find("objectID = ?1", objectID).firstResultOptional();
    }

    public Optional<Employee> findById(String id) {
        String tenantID = getCurrentTenantID();
        return find("objectID.id = ?1 and objectID.tenantID = ?2", id, tenantID).firstResultOptional();
    }

    public Optional<Employee> findByEmployeeId(String employeeId) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and employeeId = ?2", tenantID, employeeId).firstResultOptional();
    }

    public Optional<Employee> findByEmail(String email) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and email = ?2", tenantID, email).firstResultOptional();
    }

    // Usar métodos estándar de PanacheRepositoryBase

    @Transactional
    public boolean activateEmployee(ObjectID objectID) {
        return update("status = ?1, dateUpdated = ?2 where objectID = ?3", 
                     Employee.STATUS_ACTIVE, java.time.LocalDateTime.now(), objectID) > 0;
    }

    @Transactional
    public boolean deactivateEmployee(ObjectID objectID) {
        return update("status = ?1, dateUpdated = ?2 where objectID = ?3", 
                     Employee.STATUS_INACTIVE, java.time.LocalDateTime.now(), objectID) > 0;
    }

    @Transactional
    public boolean terminateEmployee(ObjectID objectID, LocalDate terminationDate) {
        return update("status = ?1, terminationDate = ?2, dateUpdated = ?3 where objectID = ?4", 
                     Employee.STATUS_TERMINATED, terminationDate, java.time.LocalDateTime.now(), objectID) > 0;
    }

    @Transactional
    public boolean resignEmployee(ObjectID objectID, LocalDate resignationDate) {
        return update("status = ?1, terminationDate = ?2, dateUpdated = ?3 where objectID = ?4", 
                     Employee.STATUS_RESIGNED, resignationDate, java.time.LocalDateTime.now(), objectID) > 0;
    }

    // Query methods for large datasets
    public List<Employee> findAllActive() {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 order by lastName, firstName", 
                   tenantID, Employee.STATUS_ACTIVE).list();
    }

    public List<Employee> findAllEmployed() {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status in (?2, ?3) order by lastName, firstName", 
                   tenantID, Employee.STATUS_ACTIVE, Employee.STATUS_INACTIVE).list();
    }

    public List<Employee> findByStatus(String status) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 order by lastName, firstName", tenantID, status).list();
    }

    public List<Employee> findByEmployeeType(Employee.EmployeeType employeeType) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and employeeType = ?2 order by lastName, firstName", 
                   tenantID, employeeType).list();
    }

    public List<Employee> findByContractType(Employee.ContractType contractType) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and contractType = ?2 order by lastName, firstName", 
                   tenantID, contractType).list();
    }

    public List<Employee> findByHireDateRange(LocalDate startDate, LocalDate endDate) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and hireDate between ?2 and ?3 order by hireDate", 
                   tenantID, startDate, endDate).list();
    }

    public List<Employee> findBySalaryRange(java.math.BigDecimal minSalary, java.math.BigDecimal maxSalary) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and currentSalary between ?2 and ?3 order by currentSalary", 
                   tenantID, minSalary, maxSalary).list();
    }

    // Pagination methods for large datasets
    public List<Employee> findActivePage(int page, int size) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 order by lastName, firstName", 
                   tenantID, Employee.STATUS_ACTIVE)
               .page(io.quarkus.panache.common.Page.of(page, size))
               .list();
    }

    public List<Employee> findAllEmployedPage(int page, int size) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status in (?2, ?3) order by lastName, firstName", 
                   tenantID, Employee.STATUS_ACTIVE, Employee.STATUS_INACTIVE)
               .page(io.quarkus.panache.common.Page.of(page, size))
               .list();
    }

    public List<Employee> findByStatusPage(String status, int page, int size) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 order by lastName, firstName", tenantID, status)
               .page(io.quarkus.panache.common.Page.of(page, size))
               .list();
    }

    public List<Employee> findByEmployeeTypePage(Employee.EmployeeType employeeType, int page, int size) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and employeeType = ?2 order by lastName, firstName", 
                   tenantID, employeeType)
               .page(io.quarkus.panache.common.Page.of(page, size))
               .list();
    }

    // Query objects for complex operations
    public io.quarkus.hibernate.orm.panache.PanacheQuery<Employee> findAllActiveQuery() {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 order by lastName, firstName", 
                   tenantID, Employee.STATUS_ACTIVE);
    }

    public io.quarkus.hibernate.orm.panache.PanacheQuery<Employee> findAllEmployedQuery() {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status in (?2, ?3) order by lastName, firstName", 
                   tenantID, Employee.STATUS_ACTIVE, Employee.STATUS_INACTIVE);
    }

    public io.quarkus.hibernate.orm.panache.PanacheQuery<Employee> findByStatusQuery(String status) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 order by lastName, firstName", tenantID, status);
    }

    public io.quarkus.hibernate.orm.panache.PanacheQuery<Employee> findByEmployeeTypeQuery(Employee.EmployeeType employeeType) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and employeeType = ?2 order by lastName, firstName", 
                   tenantID, employeeType);
    }

    // Count methods
    public long countByStatus(String status) {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and status = ?2", tenantID, status);
    }

    public long countActive() {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and status = ?2", tenantID, Employee.STATUS_ACTIVE);
    }

    public long countEmployed() {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and status in (?2, ?3)", 
                    tenantID, Employee.STATUS_ACTIVE, Employee.STATUS_INACTIVE);
    }

    public long countByEmployeeType(Employee.EmployeeType employeeType) {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and employeeType = ?2", tenantID, employeeType);
    }

    public long countByContractType(Employee.ContractType contractType) {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and contractType = ?2", tenantID, contractType);
    }

    public long countTotal() {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1", tenantID);
    }

    public long countByCurrentPosition(ObjectID positionObjectID) {
        String tenantID = getCurrentTenantID();
        // Count employees currently assigned to this position
        return count("objectID.tenantID = ?1 and objectID in " +
                    "(select ea.employee.objectID from EmployeeAssignment ea " +
                    "where ea.position.objectID = ?2 and ea.endDate is null)", 
                    tenantID, positionObjectID);
    }

    // Existence checks
    public boolean existsById(String id) {
        String tenantID = getCurrentTenantID();
        return count("objectID.id = ?1 and objectID.tenantID = ?2", id, tenantID) > 0;
    }

    public boolean existsByEmployeeId(String employeeId) {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and employeeId = ?2", tenantID, employeeId) > 0;
    }

    public boolean existsByEmail(String email) {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and email = ?2", tenantID, email) > 0;
    }

    // Business logic methods
    public List<Employee> findEmployeesHiredInYear(int year) {
        String tenantID = getCurrentTenantID();
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        return find("objectID.tenantID = ?1 and hireDate between ?2 and ?3 order by hireDate", 
                   tenantID, startDate, endDate).list();
    }

    public List<Employee> findEmployeesTerminatedInYear(int year) {
        String tenantID = getCurrentTenantID();
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        return find("objectID.tenantID = ?1 and terminationDate between ?2 and ?3 order by terminationDate", 
                   tenantID, startDate, endDate).list();
    }

    public List<Employee> findEmployeesByTenureRange(int minYears, int maxYears) {
        String tenantID = getCurrentTenantID();
        LocalDate maxHireDate = LocalDate.now().minusYears(minYears);
        LocalDate minHireDate = LocalDate.now().minusYears(maxYears);
        return find("objectID.tenantID = ?1 and hireDate between ?2 and ?3 and status in (?4, ?5) order by hireDate", 
                   tenantID, minHireDate, maxHireDate, Employee.STATUS_ACTIVE, Employee.STATUS_INACTIVE).list();
    }

    public List<Employee> findEmployeesByAgeRange(int minAge, int maxAge) {
        String tenantID = getCurrentTenantID();
        LocalDate maxBirthDate = LocalDate.now().minusYears(minAge);
        LocalDate minBirthDate = LocalDate.now().minusYears(maxAge);
        return find("objectID.tenantID = ?1 and dateOfBirth between ?2 and ?3 order by dateOfBirth", 
                   tenantID, minBirthDate, maxBirthDate).list();
    }

    public java.math.BigDecimal getAverageSalary() {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 and currentSalary is not null", 
                   tenantID, Employee.STATUS_ACTIVE)
               .stream()
               .mapToDouble(e -> e.getCurrentSalary().doubleValue())
               .average()
               .stream()
               .mapToObj(avg -> java.math.BigDecimal.valueOf(avg))
               .findFirst()
               .orElse(java.math.BigDecimal.ZERO);
    }

    public java.math.BigDecimal getMaxSalary() {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 and currentSalary is not null order by currentSalary desc", 
                   tenantID, Employee.STATUS_ACTIVE)
               .firstResultOptional()
               .map(Employee::getCurrentSalary)
               .orElse(java.math.BigDecimal.ZERO);
    }

    public java.math.BigDecimal getMinSalary() {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and status = ?2 and currentSalary is not null order by currentSalary", 
                   tenantID, Employee.STATUS_ACTIVE)
               .firstResultOptional()
               .map(Employee::getCurrentSalary)
               .orElse(java.math.BigDecimal.ZERO);
    }

    // Utility method to get current tenant ID
    private String getCurrentTenantID() {
        return com.humanrsc.config.ThreadLocalStorage.getTenantID();
    }
}

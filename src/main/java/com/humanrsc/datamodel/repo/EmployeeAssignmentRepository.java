package com.humanrsc.datamodel.repo;

import com.humanrsc.datamodel.abstraction.ObjectID;
import com.humanrsc.datamodel.entities.EmployeeAssignment;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class EmployeeAssignmentRepository implements PanacheRepository<EmployeeAssignment> {

    // Basic CRUD operations
    @Transactional
    public EmployeeAssignment createEmployeeAssignment(EmployeeAssignment assignment, String tenantID) {
        persist(assignment);
        return assignment;
    }

    public Optional<EmployeeAssignment> findByObjectID(ObjectID objectID) {
        return find("objectID = ?1", objectID).firstResultOptional();
    }

    public Optional<EmployeeAssignment> findById(String id) {
        String tenantID = getCurrentTenantID();
        return find("objectID.id = ?1 and objectID.tenantID = ?2", id, tenantID).firstResultOptional();
    }

    @Transactional
    public EmployeeAssignment updateEmployeeAssignment(EmployeeAssignment assignment) {
        return getEntityManager().merge(assignment);
    }

    // Query methods for large datasets
    public List<EmployeeAssignment> findByEmployee(String employeeId) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and employee.objectID.id = ?2 order by startDate desc", 
                   tenantID, employeeId).list();
    }

    public List<EmployeeAssignment> findByEmployee(ObjectID employeeObjectID) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and employee.objectID = ?2 order by startDate desc", 
                   tenantID, employeeObjectID).list();
    }

    public Optional<EmployeeAssignment> findCurrentByEmployee(String employeeId) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and employee.objectID.id = ?2 and endDate is null order by startDate desc", 
                   tenantID, employeeId).firstResultOptional();
    }

    public Optional<EmployeeAssignment> findCurrentByEmployee(ObjectID employeeObjectID) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and employee.objectID = ?2 and endDate is null order by startDate desc", 
                   tenantID, employeeObjectID).firstResultOptional();
    }

    public List<EmployeeAssignment> findByPosition(String positionId) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and position.objectID.id = ?2 order by startDate desc", 
                   tenantID, positionId).list();
    }

    public List<EmployeeAssignment> findByPosition(ObjectID positionObjectID) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and position.objectID = ?2 order by startDate desc", 
                   tenantID, positionObjectID).list();
    }

    public List<EmployeeAssignment> findByUnit(String unitId) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and unit.objectID.id = ?2 order by startDate desc", 
                   tenantID, unitId).list();
    }

    public List<EmployeeAssignment> findByUnit(ObjectID unitObjectID) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and unit.objectID = ?2 order by startDate desc", 
                   tenantID, unitObjectID).list();
    }

    public List<EmployeeAssignment> findByManager(String managerId) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and manager.objectID.id = ?2 order by startDate desc", 
                   tenantID, managerId).list();
    }

    public List<EmployeeAssignment> findByManager(ObjectID managerObjectID) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and manager.objectID = ?2 order by startDate desc", 
                   tenantID, managerObjectID).list();
    }

    public List<EmployeeAssignment> findCurrentAssignments() {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and endDate is null order by startDate desc", tenantID).list();
    }

    public List<EmployeeAssignment> findHistoricalAssignments() {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and endDate is not null order by startDate desc", tenantID).list();
    }

    public List<EmployeeAssignment> findByDateRange(LocalDate startDate, LocalDate endDate) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and startDate <= ?2 and (endDate is null or endDate >= ?3) order by startDate desc", 
                   tenantID, endDate, startDate).list();
    }

    public List<EmployeeAssignment> findByMovementReason(EmployeeAssignment.MovementReason reason) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and movementReason = ?2 order by startDate desc", 
                   tenantID, reason).list();
    }

    // Pagination methods for large datasets
    public List<EmployeeAssignment> findByEmployeePage(String employeeId, int page, int size) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and employee.objectID.id = ?2 order by startDate desc", 
                   tenantID, employeeId)
               .page(io.quarkus.panache.common.Page.of(page, size))
               .list();
    }

    public List<EmployeeAssignment> findCurrentAssignmentsPage(int page, int size) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and endDate is null order by startDate desc", tenantID)
               .page(io.quarkus.panache.common.Page.of(page, size))
               .list();
    }

    public List<EmployeeAssignment> findHistoricalAssignmentsPage(int page, int size) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and endDate is not null order by startDate desc", tenantID)
               .page(io.quarkus.panache.common.Page.of(page, size))
               .list();
    }

    // Query objects for complex operations
    public io.quarkus.hibernate.orm.panache.PanacheQuery<EmployeeAssignment> findByEmployeeQuery(String employeeId) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and employee.objectID.id = ?2 order by startDate desc", 
                   tenantID, employeeId);
    }

    public io.quarkus.hibernate.orm.panache.PanacheQuery<EmployeeAssignment> findCurrentAssignmentsQuery() {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and endDate is null order by startDate desc", tenantID);
    }

    public io.quarkus.hibernate.orm.panache.PanacheQuery<EmployeeAssignment> findHistoricalAssignmentsQuery() {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and endDate is not null order by startDate desc", tenantID);
    }

    // Count methods
    public long countByEmployee(String employeeId) {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and employee.objectID.id = ?2", tenantID, employeeId);
    }

    public long countByEmployee(ObjectID employeeObjectID) {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and employee.objectID = ?2", tenantID, employeeObjectID);
    }

    public long countCurrentAssignments() {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and endDate is null", tenantID);
    }

    public long countHistoricalAssignments() {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and endDate is not null", tenantID);
    }

    public long countByPosition(ObjectID positionObjectID) {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and position.objectID = ?2", tenantID, positionObjectID);
    }

    public long countByUnit(ObjectID unitObjectID) {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and unit.objectID = ?2", tenantID, unitObjectID);
    }

    public long countByManager(ObjectID managerObjectID) {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and manager.objectID = ?2", tenantID, managerObjectID);
    }

    public long countByMovementReason(EmployeeAssignment.MovementReason reason) {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and movementReason = ?2", tenantID, reason);
    }

    public long countTotal() {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1", tenantID);
    }

    // Existence checks
    public boolean existsById(String id) {
        String tenantID = getCurrentTenantID();
        return count("objectID.id = ?1 and objectID.tenantID = ?2", id, tenantID) > 0;
    }

    public boolean hasCurrentAssignment(String employeeId) {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and employee.objectID.id = ?2 and endDate is null", 
                    tenantID, employeeId) > 0;
    }

    // Business logic methods
    public List<EmployeeAssignment> findPromotionsInPeriod(LocalDate startDate, LocalDate endDate) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and movementReason = ?2 and startDate between ?3 and ?4 order by startDate desc", 
                   tenantID, EmployeeAssignment.MovementReason.PROMOTION, startDate, endDate).list();
    }

    public List<EmployeeAssignment> findLateralMovesInPeriod(LocalDate startDate, LocalDate endDate) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and movementReason = ?2 and startDate between ?3 and ?4 order by startDate desc", 
                   tenantID, EmployeeAssignment.MovementReason.LATERAL_MOVE, startDate, endDate).list();
    }

    public List<EmployeeAssignment> findTerminationsInPeriod(LocalDate startDate, LocalDate endDate) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and movementReason = ?2 and startDate between ?3 and ?4 order by startDate desc", 
                   tenantID, EmployeeAssignment.MovementReason.TERMINATION, startDate, endDate).list();
    }

    public List<EmployeeAssignment> findResignationsInPeriod(LocalDate startDate, LocalDate endDate) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and movementReason = ?2 and startDate between ?3 and ?4 order by startDate desc", 
                   tenantID, EmployeeAssignment.MovementReason.RESIGNATION, startDate, endDate).list();
    }

    public List<EmployeeAssignment> findAssignmentsBySalaryRange(java.math.BigDecimal minSalary, java.math.BigDecimal maxSalary) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and salary between ?2 and ?3 order by salary desc", 
                   tenantID, minSalary, maxSalary).list();
    }

    // Utility method to get current tenant ID
    private String getCurrentTenantID() {
        return com.humanrsc.config.ThreadLocalStorage.getTenantID();
    }
}

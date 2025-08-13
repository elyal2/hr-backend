package com.humanrsc.datamodel.repo;

import com.humanrsc.datamodel.abstraction.ObjectID;
import com.humanrsc.datamodel.entities.SalaryHistory;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class SalaryHistoryRepository implements PanacheRepository<SalaryHistory> {

    // Basic CRUD operations
    @Transactional
    public SalaryHistory createSalaryHistory(SalaryHistory salaryHistory, String tenantID) {
        persist(salaryHistory);
        return salaryHistory;
    }

    public Optional<SalaryHistory> findByObjectID(ObjectID objectID) {
        return find("objectID = ?1", objectID).firstResultOptional();
    }

    public Optional<SalaryHistory> findById(String id) {
        String tenantID = getCurrentTenantID();
        return find("objectID.id = ?1 and objectID.tenantID = ?2", id, tenantID).firstResultOptional();
    }

    @Transactional
    public SalaryHistory updateSalaryHistory(SalaryHistory salaryHistory) {
        return getEntityManager().merge(salaryHistory);
    }

    // Query methods
    public List<SalaryHistory> findByEmployee(String employeeId) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and employee.objectID.id = ?2 order by effectiveDate desc", 
                   tenantID, employeeId).list();
    }

    public List<SalaryHistory> findByEmployee(ObjectID employeeObjectID) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and employee.objectID = ?2 order by effectiveDate desc", 
                   tenantID, employeeObjectID).list();
    }

    public List<SalaryHistory> findByApprovedBy(String approvedById) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and approvedBy.objectID.id = ?2 order by effectiveDate desc", 
                   tenantID, approvedById).list();
    }

    public List<SalaryHistory> findByEffectiveDateRange(LocalDate startDate, LocalDate endDate) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and effectiveDate between ?2 and ?3 order by effectiveDate desc", 
                   tenantID, startDate, endDate).list();
    }

    public List<SalaryHistory> findIncreases() {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and newSalary > oldSalary order by effectiveDate desc", tenantID).list();
    }

    public List<SalaryHistory> findDecreases() {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and newSalary < oldSalary order by effectiveDate desc", tenantID).list();
    }

    public List<SalaryHistory> findBySalaryRange(BigDecimal minSalary, BigDecimal maxSalary) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and newSalary between ?2 and ?3 order by newSalary desc", 
                   tenantID, minSalary, maxSalary).list();
    }

    public List<SalaryHistory> findByReason(String reason) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and reason like ?2 order by effectiveDate desc", 
                   tenantID, "%" + reason + "%").list();
    }

    public List<SalaryHistory> findRecentChanges() {
        String tenantID = getCurrentTenantID();
        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);
        return find("objectID.tenantID = ?1 and effectiveDate >= ?2 order by effectiveDate desc", 
                   tenantID, sixMonthsAgo).list();
    }

    // Pagination methods
    public List<SalaryHistory> findByEmployeePage(String employeeId, int page, int size) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and employee.objectID.id = ?2 order by effectiveDate desc", 
                   tenantID, employeeId)
               .page(io.quarkus.panache.common.Page.of(page, size))
               .list();
    }

    public List<SalaryHistory> findRecentChangesPage(int page, int size) {
        String tenantID = getCurrentTenantID();
        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);
        return find("objectID.tenantID = ?1 and effectiveDate >= ?2 order by effectiveDate desc", 
                   tenantID, sixMonthsAgo)
               .page(io.quarkus.panache.common.Page.of(page, size))
               .list();
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

    public long countIncreases() {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and newSalary > oldSalary", tenantID);
    }

    public long countDecreases() {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and newSalary < oldSalary", tenantID);
    }

    public long countByApprovedBy(String approvedById) {
        String tenantID = getCurrentTenantID();
        return count("objectID.tenantID = ?1 and approvedBy.objectID.id = ?2", tenantID, approvedById);
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

    // Business logic methods
    public List<SalaryHistory> findSalaryChangesInYear(int year) {
        String tenantID = getCurrentTenantID();
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        return find("objectID.tenantID = ?1 and effectiveDate between ?2 and ?3 order by effectiveDate desc", 
                   tenantID, startDate, endDate).list();
    }

    public BigDecimal getAverageSalaryIncrease() {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and newSalary > oldSalary", tenantID)
               .stream()
               .mapToDouble(sh -> sh.getSalaryIncrease().doubleValue())
               .average()
               .stream()
               .mapToObj(avg -> BigDecimal.valueOf(avg))
               .findFirst()
               .orElse(BigDecimal.ZERO);
    }

    public BigDecimal getAverageSalaryIncreasePercentage() {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and newSalary > oldSalary and oldSalary > 0", tenantID)
               .stream()
               .mapToDouble(sh -> sh.getSalaryIncreasePercentage().doubleValue())
               .average()
               .stream()
               .mapToObj(avg -> BigDecimal.valueOf(avg))
               .findFirst()
               .orElse(BigDecimal.ZERO);
    }

    public BigDecimal getMaxSalaryIncrease() {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and newSalary > oldSalary order by (newSalary - oldSalary) desc", tenantID)
               .firstResultOptional()
               .map(SalaryHistory::getSalaryIncrease)
               .orElse(BigDecimal.ZERO);
    }

    public BigDecimal getMaxSalaryIncreasePercentage() {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and newSalary > oldSalary and oldSalary > 0 order by ((newSalary - oldSalary) / oldSalary * 100) desc", tenantID)
               .firstResultOptional()
               .map(SalaryHistory::getSalaryIncreasePercentage)
               .orElse(BigDecimal.ZERO);
    }

    public List<SalaryHistory> findTopSalaryIncreases(int limit) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and newSalary > oldSalary order by (newSalary - oldSalary) desc", tenantID)
               .range(0, limit - 1)
               .list();
    }

    public List<SalaryHistory> findTopSalaryIncreasePercentages(int limit) {
        String tenantID = getCurrentTenantID();
        return find("objectID.tenantID = ?1 and newSalary > oldSalary and oldSalary > 0 order by ((newSalary - oldSalary) / oldSalary * 100) desc", tenantID)
               .range(0, limit - 1)
               .list();
    }

    // Utility method to get current tenant ID
    private String getCurrentTenantID() {
        return com.humanrsc.config.ThreadLocalStorage.getTenantID();
    }
}

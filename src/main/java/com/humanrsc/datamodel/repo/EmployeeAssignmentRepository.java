package com.humanrsc.datamodel.repo;

import com.humanrsc.datamodel.abstraction.ObjectID;
import com.humanrsc.datamodel.entities.EmployeeAssignment;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class EmployeeAssignmentRepository implements PanacheRepositoryBase<EmployeeAssignment, ObjectID> {

    // Basic finder methods - RLS handles tenant filtering automatically

    public Optional<EmployeeAssignment> findByObjectID(ObjectID objectID) {
        return find("objectID = ?1", objectID).firstResultOptional();
    }

    public Optional<EmployeeAssignment> findById(String id) {
        return find("objectID.id = ?1", id).firstResultOptional();
    }

    // Query methods used by OrganizationService - RLS filters by tenant automatically
    
    public List<EmployeeAssignment> findByEmployee(String employeeId) {
        return find("employee.objectID.id = ?1 order by startDate desc", employeeId).list();
    }

    public Optional<EmployeeAssignment> findCurrentByEmployee(String employeeId) {
        return find("employee.objectID.id = ?1 and endDate is null order by startDate desc", employeeId)
                .firstResultOptional();
    }

    public List<EmployeeAssignment> findByPosition(String positionId) {
        return find("position.objectID.id = ?1 order by startDate desc", positionId).list();
    }

    public List<EmployeeAssignment> findByUnit(String unitId) {
        return find("unit.objectID.id = ?1 order by startDate desc", unitId).list();
    }

    public List<EmployeeAssignment> findByManager(String managerId) {
        return find("manager.objectID.id = ?1 order by startDate desc", managerId).list();
    }

    // Additional useful methods - RLS filters by tenant automatically
    
    public List<EmployeeAssignment> findCurrentAssignments() {
        return find("endDate is null order by startDate desc").list();
    }

    public List<EmployeeAssignment> findHistoricalAssignments() {
        return find("endDate is not null order by startDate desc").list();
    }

    // Count methods - RLS filters by tenant automatically
    
    public long countByEmployee(String employeeId) {
        return count("employee.objectID.id = ?1", employeeId);
    }

    public long countCurrentAssignments() {
        return count("endDate is null");
    }

    public long countTotal() {
        return count(); // RLS handles tenant filtering
    }

    // Existence checks - RLS filters by tenant automatically
    
    public boolean existsById(String id) {
        return count("objectID.id = ?1", id) > 0;
    }

    public boolean hasCurrentAssignment(String employeeId) {
        return count("employee.objectID.id = ?1 and endDate is null", employeeId) > 0;
    }
}
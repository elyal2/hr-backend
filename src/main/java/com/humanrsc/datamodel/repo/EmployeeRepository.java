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

    // Basic finder methods - RLS handles tenant filtering automatically
    
    public Optional<Employee> findByObjectID(ObjectID objectID) {
        return find("objectID = ?1", objectID).firstResultOptional();
    }

    public Optional<Employee> findById(String id) {
        return find("objectID.id = ?1", id).firstResultOptional();
    }

    public Optional<Employee> findByEmployeeId(String employeeId) {
        return find("employeeId = ?1", employeeId).firstResultOptional();
    }

    public Optional<Employee> findByEmail(String email) {
        return find("email = ?1", email).firstResultOptional();
    }

    // Status change operations - RLS handles tenant filtering automatically
    
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

    // Existence checks - RLS filters by tenant automatically
    
    public boolean existsById(String id) {
        return count("objectID.id = ?1", id) > 0;
    }

    public boolean existsByEmployeeId(String employeeId) {
        return count("employeeId = ?1", employeeId) > 0;
    }

    public boolean existsByEmail(String email) {
        return count("email = ?1", email) > 0;
    }

    // Business logic methods - RLS filters by tenant automatically
    
    public long countByCurrentPosition(ObjectID positionObjectID) {
        return count("objectID in " +
                    "(select ea.employee.objectID from EmployeeAssignment ea " +
                    "where ea.position.objectID = ?1 and ea.endDate is null)", 
                    positionObjectID);
    }

    // Salary statistics - RLS filters by tenant automatically
    
    public java.math.BigDecimal getAverageSalary() {
        return find("status = ?1 and currentSalary is not null", Employee.STATUS_ACTIVE)
               .stream()
               .mapToDouble(e -> e.getCurrentSalary().doubleValue())
               .average()
               .stream()
               .mapToObj(avg -> java.math.BigDecimal.valueOf(avg))
               .findFirst()
               .orElse(java.math.BigDecimal.ZERO);
    }

    public java.math.BigDecimal getMaxSalary() {
        return find("status = ?1 and currentSalary is not null order by currentSalary desc", 
                   Employee.STATUS_ACTIVE)
               .firstResultOptional()
               .map(Employee::getCurrentSalary)
               .orElse(java.math.BigDecimal.ZERO);
    }

    public java.math.BigDecimal getMinSalary() {
        return find("status = ?1 and currentSalary is not null order by currentSalary", 
                   Employee.STATUS_ACTIVE)
               .firstResultOptional()
               .map(Employee::getCurrentSalary)
               .orElse(java.math.BigDecimal.ZERO);
    }

    public java.math.BigDecimal getTotalSalaryBudget() {
        return find("status = ?1 and currentSalary is not null", Employee.STATUS_ACTIVE)
               .stream()
               .map(Employee::getCurrentSalary)
               .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    public String getPrimaryCurrency() {
        // Get the most common currency among active employees with salary
        return find("status = ?1 and currentSalary is not null", Employee.STATUS_ACTIVE)
               .stream()
               .map(Employee::getCurrency)
               .collect(java.util.stream.Collectors.groupingBy(
                   currency -> currency, 
                   java.util.stream.Collectors.counting()
               ))
               .entrySet()
               .stream()
               .max(java.util.Map.Entry.comparingByValue())
               .map(java.util.Map.Entry::getKey)
               .orElse("USD");
    }

    public java.util.Map<String, Long> getCurrencyDistribution() {
        // Get distribution of currencies among active employees with salary
        return find("status = ?1 and currentSalary is not null", Employee.STATUS_ACTIVE)
               .stream()
               .map(Employee::getCurrency)
               .collect(java.util.stream.Collectors.groupingBy(
                   currency -> currency, 
                   java.util.stream.Collectors.counting()
               ));
    }

    // Dynamic filtering methods - RLS handles tenant filtering automatically
    
    public List<Employee> findWithFilters(java.util.Map<String, Object> filters, int page, int size) {
        java.util.List<Object> parameters = new java.util.ArrayList<>();
        String query = buildFilterQuery(filters, parameters);
        
        return find(query, parameters.toArray())
               .page(io.quarkus.panache.common.Page.of(page, size))
               .list();
    }
    
    public List<Employee> findWithFilters(java.util.Map<String, Object> filters) {
        java.util.List<Object> parameters = new java.util.ArrayList<>();
        String query = buildFilterQuery(filters, parameters);
        
        return find(query, parameters.toArray()).list();
    }
    
    public long countWithFilters(java.util.Map<String, Object> filters) {
        java.util.List<Object> parameters = new java.util.ArrayList<>();
        String query = buildFilterQuery(filters, parameters);
        
        return count(query, parameters.toArray());
    }
    
    // Helper method to build filter query - no tenant filtering needed (RLS handles it)
    private String buildFilterQuery(java.util.Map<String, Object> filters, java.util.List<Object> parameters) {
        StringBuilder queryBuilder = new StringBuilder();
        boolean firstCondition = true;
        
        // Fields that should be case-insensitive (string fields)
        java.util.Set<String> caseInsensitiveFields = java.util.Set.of(
            "firstName", "lastName", "email", "nationalId", "employeeId"
        );
        
        // Add filters dynamically
        for (java.util.Map.Entry<String, Object> entry : filters.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();
            
            if (value != null) {
                if (!firstCondition) {
                    queryBuilder.append(" and ");
                }
                firstCondition = false;
                
                if (value instanceof Object[] || value.getClass().isArray()) {
                    // Handle array values (e.g., status in ['active', 'inactive'])
                    Object[] array = (Object[]) value;
                    if (array.length > 0) {
                        if (caseInsensitiveFields.contains(field)) {
                            // Case-insensitive array search
                            queryBuilder.append("LOWER(").append(field).append(") in (");
                            for (int i = 0; i < array.length; i++) {
                                if (i > 0) queryBuilder.append(", ");
                                queryBuilder.append("LOWER(?").append(parameters.size() + i + 1).append(")");
                            }
                            queryBuilder.append(")");
                        } else {
                            // Case-sensitive array search
                            queryBuilder.append(field).append(" in (?");
                            queryBuilder.append(parameters.size() + 1);
                            for (int i = 1; i < array.length; i++) {
                                queryBuilder.append(", ?").append(parameters.size() + i + 1);
                            }
                            queryBuilder.append(")");
                        }
                        for (Object item : array) {
                            parameters.add(item);
                        }
                    }
                } else if (!value.toString().trim().isEmpty()) {
                    // Handle single values
                    if (caseInsensitiveFields.contains(field)) {
                        // Case-insensitive partial search for string fields (LIKE with %)
                        queryBuilder.append("LOWER(").append(field).append(") LIKE LOWER(?").append(parameters.size() + 1).append(")");
                        parameters.add("%" + value + "%");
                    } else {
                        // Case-sensitive exact search for other fields
                        queryBuilder.append(field).append(" = ?").append(parameters.size() + 1);
                        parameters.add(value);
                    }
                }
            }
        }
        
        // If no conditions were added, return a query that matches all records
        if (firstCondition) {
            queryBuilder.append("1 = 1");
        }
        
        queryBuilder.append(" order by lastName, firstName");
        return queryBuilder.toString();
    }
    
    // Advanced filtering with range queries - RLS handles tenant filtering
    public List<Employee> findWithAdvancedFilters(java.util.Map<String, Object> exactFilters, 
                                                 java.util.Map<String, java.util.Map<String, Object>> rangeFilters,
                                                 int page, int size) {
        
        StringBuilder queryBuilder = new StringBuilder();
        java.util.List<Object> parameters = new java.util.ArrayList<>();
        boolean firstCondition = true;
        
        // Add exact filters
        for (java.util.Map.Entry<String, Object> entry : exactFilters.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();
            
            if (value != null && !value.toString().trim().isEmpty()) {
                if (!firstCondition) {
                    queryBuilder.append(" and ");
                }
                firstCondition = false;
                queryBuilder.append(field).append(" = ?").append(parameters.size() + 1);
                parameters.add(value);
            }
        }
        
        // Add range filters
        for (java.util.Map.Entry<String, java.util.Map<String, Object>> entry : rangeFilters.entrySet()) {
            String field = entry.getKey();
            java.util.Map<String, Object> range = entry.getValue();
            
            if (range.containsKey("min") && range.get("min") != null) {
                if (!firstCondition) {
                    queryBuilder.append(" and ");
                }
                firstCondition = false;
                queryBuilder.append(field).append(" >= ?").append(parameters.size() + 1);
                parameters.add(range.get("min"));
            }
            
            if (range.containsKey("max") && range.get("max") != null) {
                if (!firstCondition) {
                    queryBuilder.append(" and ");
                }
                firstCondition = false;
                queryBuilder.append(field).append(" <= ?").append(parameters.size() + 1);
                parameters.add(range.get("max"));
            }
        }
        
        // If no conditions were added, return a query that matches all records
        if (firstCondition) {
            queryBuilder.append("1 = 1");
        }
        
        queryBuilder.append(" order by lastName, firstName");
        
        return find(queryBuilder.toString(), parameters.toArray())
               .page(io.quarkus.panache.common.Page.of(page, size))
               .list();
    }
}
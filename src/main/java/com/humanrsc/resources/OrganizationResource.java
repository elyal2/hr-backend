package com.humanrsc.resources;

import com.humanrsc.datamodel.entities.*;
import com.humanrsc.datamodel.entities.PositionCategory;
import com.humanrsc.datamodel.abstraction.ObjectID;
import com.humanrsc.config.ThreadLocalStorage;
import com.humanrsc.security.JWTSecured;
import com.humanrsc.config.ConnectionPoolIntercepted;
import com.humanrsc.config.ConfigDefaults;
import com.humanrsc.services.OrganizationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.annotation.security.RolesAllowed;
import static com.humanrsc.security.Permissions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Path("/api/organization")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@JWTSecured
@ConnectionPoolIntercepted
public class OrganizationResource {

    @Inject
    OrganizationService organizationService;

    // ========== POSITION CATEGORIES ENDPOINTS ==========

    @POST
    @Path("/position-categories")
    @RolesAllowed({WRITE_POSITION_CATEGORIES})
    public Response createPositionCategory(PositionCategory category) {
        try {
            PositionCategory created = organizationService.createPositionCategory(category);
            return Response.status(Response.Status.CREATED).entity(created).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/position-categories")
    @RolesAllowed({READ_POSITION_CATEGORIES})
    public Response getAllCategories(@QueryParam("page") Integer page, @QueryParam("size") Integer size) {
        int pageNum = page != null ? page : ConfigDefaults.DEFAULT_PAGE;
        int pageSize = size != null ? size : ConfigDefaults.DEFAULT_SIZE;
        List<PositionCategory> categories = organizationService.findAllCategories(pageNum, pageSize);
        return Response.ok(categories).build();
    }

    @GET
    @Path("/position-categories/{id}")
    @RolesAllowed({READ_POSITION_CATEGORIES})
    public Response getCategoryById(@PathParam("id") String id) {
        Optional<PositionCategory> category = organizationService.findCategoryById(id);
        return category.map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    @PUT
    @Path("/position-categories/{id}")
    @RolesAllowed({WRITE_POSITION_CATEGORIES})
    public Response updatePositionCategory(@PathParam("id") String id, PositionCategory category) {
        try {
            category.getObjectID().setId(id);
            PositionCategory updated = organizationService.updatePositionCategory(category);
            return Response.ok(updated).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation error", e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/position-categories/{id}")
    @RolesAllowed({WRITE_POSITION_CATEGORIES})
    public Response deletePositionCategory(@PathParam("id") String id) {
        try {
            boolean deleted = organizationService.deletePositionCategory(id);
            if (deleted) {
                return Response.noContent().build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse("Cannot delete", e.getMessage()))
                    .build();
        }
    }

    // ========== ORGANIZATIONAL UNITS ENDPOINTS ==========

    @POST
    @Path("/units")
    @RolesAllowed({WRITE_ORG_UNITS})
    public Response createOrganizationalUnit(OrganizationalUnit unit) {
        try {
            OrganizationalUnit created = organizationService.createOrganizationalUnit(unit);
            return Response.status(Response.Status.CREATED).entity(created).build();
        } catch (com.humanrsc.exceptions.DuplicateResourceException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse("Duplicate resource", e.getMessage(), "DUPLICATE_RESOURCE", e.getField(), e.getValue()))
                    .build();
        } catch (com.humanrsc.exceptions.OrganizationalUnitValidationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation error", e.getMessage(), "VALIDATION_ERROR", e.getField(), null))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Internal server error", "An unexpected error occurred while creating the organizational unit", "INTERNAL_ERROR"))
                    .build();
        }
    }

    @GET
    @Path("/units")
    @RolesAllowed({READ_ORG_UNITS})
    public Response getAllUnits(@QueryParam("page") Integer page, @QueryParam("size") Integer size,
                               // Dynamic filters for organizational units
                               @QueryParam("name") String name,
                               @QueryParam("description") String description,
                               @QueryParam("location") String location,
                               @QueryParam("country") String country,
                               @QueryParam("costCenter") String costCenter,
                               @QueryParam("status") String status,
                               @QueryParam("organizationalLevel") Integer organizationalLevel) {
        
        // Build filters map
        java.util.Map<String, Object> filters = new java.util.HashMap<>();
        
        if (name != null && !name.trim().isEmpty()) {
            filters.put("name", name);
        }
        
        if (description != null && !description.trim().isEmpty()) {
            filters.put("description", description);
        }
        
        if (location != null && !location.trim().isEmpty()) {
            filters.put("location", location);
        }
        
        if (country != null && !country.trim().isEmpty()) {
            filters.put("country", country);
        }
        
        if (costCenter != null && !costCenter.trim().isEmpty()) {
            filters.put("costCenter", costCenter);
        }
        
        if (status != null && !status.trim().isEmpty()) {
            filters.put("status", status);
        }
        
        if (organizationalLevel != null) {
            filters.put("organizationalLevel", organizationalLevel);
        }
        
        int pageNum = page != null ? page : ConfigDefaults.DEFAULT_PAGE;
        int pageSize = size != null ? size : ConfigDefaults.DEFAULT_SIZE;
        
        List<OrganizationalUnit> units;
        if (filters.isEmpty()) {
            // No filters, use default method
            units = organizationService.findAllUnits(pageNum, pageSize);
        } else {
            // Use dynamic filtering
            units = organizationService.findUnitsWithFilters(filters, pageNum, pageSize);
        }
        
        return Response.ok(units).build();
    }

    @GET
    @Path("/units/root")
    @RolesAllowed({READ_ORG_UNITS})
    public Response getRootUnits() {
        List<OrganizationalUnit> rootUnits = organizationService.findRootUnits();
        return Response.ok(rootUnits).build();
    }

    @GET
    @Path("/units/{id}")
    @RolesAllowed({READ_ORG_UNITS})
    public Response getUnitById(@PathParam("id") String id) {
        Optional<OrganizationalUnit> unit = organizationService.findUnitById(id);
        return unit.map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    @GET
    @Path("/units/{id}/children")
    @RolesAllowed({READ_ORG_UNITS})
    public Response getChildUnits(@PathParam("id") String id) {
        List<OrganizationalUnit> childUnits = organizationService.findChildUnits(id);
        return Response.ok(childUnits).build();
    }

    @PUT
    @Path("/units/{id}")
    @RolesAllowed({WRITE_ORG_UNITS})
    public Response updateOrganizationalUnit(@PathParam("id") String id, com.humanrsc.datamodel.dto.OrganizationalUnitDTO dto) {
        try {
            // Establecer el ObjectID si no existe
            if (dto.getObjectID() == null) {
                String tenantID = ThreadLocalStorage.getTenantID();
                dto.setObjectID(ObjectID.of(id, tenantID));
            } else {
                dto.getObjectID().setId(id);
            }
            
            OrganizationalUnit updated = organizationService.updateOrganizationalUnitFromDTO(id, dto);
            return Response.ok(updated).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation error", e.getMessage()))
                    .build();
        }
    }
    


    @DELETE
    @Path("/units/{id}")
    @RolesAllowed({WRITE_ORG_UNITS})
    public Response deleteOrganizationalUnit(@PathParam("id") String id) {
        try {
            boolean deleted = organizationService.deleteOrganizationalUnit(id);
            if (deleted) {
                return Response.noContent().build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse("Cannot delete", e.getMessage()))
                    .build();
        }
    }

    // ========== JOB POSITIONS ENDPOINTS ==========

    @POST
    @Path("/positions")
    @RolesAllowed({WRITE_POSITIONS})
    public Response createJobPosition(JobPosition position) {
        try {
            JobPosition created = organizationService.createJobPosition(position);
            return Response.status(Response.Status.CREATED).entity(created).build();
        } catch (com.humanrsc.exceptions.DuplicateResourceException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse("Duplicate resource", e.getMessage(), "DUPLICATE_RESOURCE", e.getField(), e.getValue()))
                    .build();
        } catch (com.humanrsc.exceptions.JobPositionValidationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation error", e.getMessage(), "VALIDATION_ERROR", e.getField(), null))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Internal server error", "An unexpected error occurred while creating the job position", "INTERNAL_ERROR"))
                    .build();
        }
    }

    @GET
    @Path("/positions")
    @RolesAllowed({READ_POSITIONS})
    public Response getAllPositions(@QueryParam("page") Integer page, @QueryParam("size") Integer size,
                                   // Dynamic filters for job positions
                                   @QueryParam("title") String title,
                                   @QueryParam("description") String description,
                                   @QueryParam("hierarchicalLevel") Integer hierarchicalLevel,
                                   @QueryParam("status") String status,
                                   @QueryParam("unitId") String unitId,
                                   @QueryParam("categoryId") String categoryId) {
        
        // Build filters map
        java.util.Map<String, Object> filters = new java.util.HashMap<>();
        
        if (title != null && !title.trim().isEmpty()) {
            filters.put("title", title);
        }
        
        if (description != null && !description.trim().isEmpty()) {
            filters.put("description", description);
        }
        
        if (hierarchicalLevel != null) {
            filters.put("hierarchicalLevel", hierarchicalLevel);
        }
        
        if (status != null && !status.trim().isEmpty()) {
            filters.put("status", status);
        }
        
        if (unitId != null && !unitId.trim().isEmpty()) {
            filters.put("unit.objectID.id", unitId);
        }
        
        if (categoryId != null && !categoryId.trim().isEmpty()) {
            filters.put("category.objectID.id", categoryId);
        }
        
        int pageNum = page != null ? page : ConfigDefaults.DEFAULT_PAGE;
        int pageSize = size != null ? size : ConfigDefaults.DEFAULT_SIZE;
        
        List<JobPosition> positions;
        if (filters.isEmpty()) {
            // No filters, use default method
            positions = organizationService.findAllPositions(pageNum, pageSize);
        } else {
            // Use dynamic filtering
            positions = organizationService.findPositionsWithFilters(filters, pageNum, pageSize);
        }
        
        return Response.ok(positions).build();
    }

    @GET
    @Path("/positions/unit/{unitId}")
    @RolesAllowed({READ_POSITIONS})
    public Response getPositionsByUnit(@PathParam("unitId") String unitId) {
        List<JobPosition> positions = organizationService.findPositionsByUnit(unitId);
        return Response.ok(positions).build();
    }

    @GET
    @Path("/positions/category/{categoryId}")
    @RolesAllowed({READ_POSITIONS})
    public Response getPositionsByCategory(@PathParam("categoryId") String categoryId) {
        List<JobPosition> positions = organizationService.findPositionsByCategory(categoryId);
        return Response.ok(positions).build();
    }

    @GET
    @Path("/positions/level/{level}")
    @RolesAllowed({READ_POSITIONS})
    public Response getPositionsByHierarchicalLevel(@PathParam("level") Integer level) {
        List<JobPosition> positions = organizationService.findPositionsByHierarchicalLevel(level);
        return Response.ok(positions).build();
    }

    @GET
    @Path("/positions/level-range")
    @RolesAllowed({READ_POSITIONS})
    public Response getPositionsByLevelRange(@QueryParam("min") Integer minLevel, @QueryParam("max") Integer maxLevel) {
        List<JobPosition> positions = organizationService.findPositionsByLevelRange(minLevel, maxLevel);
        return Response.ok(positions).build();
    }

    @GET
    @Path("/positions/{id}")
    @RolesAllowed({READ_POSITIONS})
    public Response getPositionById(@PathParam("id") String id) {
        Optional<JobPosition> position = organizationService.findPositionById(id);
        return position.map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    @PUT
    @Path("/positions/{id}")
    @RolesAllowed({WRITE_POSITIONS})
    public Response updateJobPosition(@PathParam("id") String id, com.humanrsc.datamodel.dto.JobPositionDTO dto) {
        try {
            // Establecer el ObjectID si no existe
            if (dto.getObjectID() == null) {
                String tenantID = ThreadLocalStorage.getTenantID();
                dto.setObjectID(ObjectID.of(id, tenantID));
            } else {
                dto.getObjectID().setId(id);
            }
            
            JobPosition updated = organizationService.updateJobPositionFromDTO(id, dto);
            return Response.ok(updated).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation error", e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/positions/{id}")
    @RolesAllowed({WRITE_POSITIONS})
    public Response deleteJobPosition(@PathParam("id") String id) {
        try {
            boolean deleted = organizationService.deleteJobPosition(id);
            if (deleted) {
                return Response.noContent().build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse("Cannot delete", e.getMessage()))
                    .build();
        }
    }

    // ========== EMPLOYEES ENDPOINTS ==========

    @POST
    @Path("/employees")
    @RolesAllowed({WRITE_PEOPLE})
    public Response createEmployee(Employee employee) {
        try {
            Employee created = organizationService.createEmployee(employee);
            return Response.status(Response.Status.CREATED).entity(created).build();
        } catch (com.humanrsc.exceptions.DuplicateResourceException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse("Duplicate resource", e.getMessage(), "DUPLICATE_RESOURCE", e.getField(), e.getValue()))
                    .build();
        } catch (com.humanrsc.exceptions.EmployeeValidationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation error", e.getMessage(), "VALIDATION_ERROR", e.getField(), null))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Internal server error", "An unexpected error occurred while creating the employee", "INTERNAL_ERROR"))
                    .build();
        }
    }

    @GET
    @Path("/employees")
    @RolesAllowed({READ_PEOPLE})
    public Response getAllEmployees(@QueryParam("page") Integer page, @QueryParam("size") Integer size,
                                   @QueryParam("status") String status,
                                   @QueryParam("type") String employeeType,
                                   @QueryParam("contractType") String contractType,
                                   @QueryParam("gender") String gender,
                                   @QueryParam("currency") String currency,
                                   @QueryParam("terminated") Boolean terminated,
                                   // Dynamic filters for any employee field
                                   @QueryParam("nationalId") String nationalId,
                                   @QueryParam("firstName") String firstName,
                                   @QueryParam("lastName") String lastName,
                                   @QueryParam("email") String email,
                                   @QueryParam("employeeId") String employeeId,
                                   @QueryParam("hireDate") String hireDate,
                                   @QueryParam("dateOfBirth") String dateOfBirth) {
        List<Employee> employees;
        
        // Build filters map
        java.util.Map<String, Object> filters = new java.util.HashMap<>();
        
        // Legacy specific filters (for backward compatibility)
        if (status != null) {
            filters.put("status", status);
        }
        
        if (employeeType != null) {
            try {
                Employee.EmployeeType type = Employee.EmployeeType.valueOf(employeeType.toUpperCase());
                filters.put("employeeType", type);
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Invalid employee type", e.getMessage()))
                        .build();
            }
        }
        
        if (contractType != null) {
            filters.put("contractType", contractType);
        }
        
        if (gender != null) {
            filters.put("gender", gender);
        }
        
        if (currency != null) {
            filters.put("currency", currency);
        }
        
        // New dynamic filters
        if (nationalId != null && !nationalId.trim().isEmpty()) {
            filters.put("nationalId", nationalId);
        }
        
        if (firstName != null && !firstName.trim().isEmpty()) {
            filters.put("firstName", firstName);
        }
        
        if (lastName != null && !lastName.trim().isEmpty()) {
            filters.put("lastName", lastName);
        }
        
        if (email != null && !email.trim().isEmpty()) {
            filters.put("email", email);
        }
        
        if (employeeId != null && !employeeId.trim().isEmpty()) {
            filters.put("employeeId", employeeId);
        }
        
        if (hireDate != null && !hireDate.trim().isEmpty()) {
            try {
                java.time.LocalDate.parse(hireDate);
                filters.put("hireDate", hireDate);
            } catch (java.time.format.DateTimeParseException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Invalid hire date format", "Use YYYY-MM-DD format"))
                        .build();
            }
        }
        
        if (dateOfBirth != null && !dateOfBirth.trim().isEmpty()) {
            try {
                java.time.LocalDate.parse(dateOfBirth);
                filters.put("dateOfBirth", dateOfBirth);
            } catch (java.time.format.DateTimeParseException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Invalid date of birth format", "Use YYYY-MM-DD format"))
                        .build();
            }
        }
        
        // Handle terminated parameter
        if (terminated != null) {
            if (terminated) {
                // User wants terminated employees
                filters.put("status", Employee.STATUS_TERMINATED);
            } else {
                // User wants non-terminated employees (active and inactive only)
                filters.put("status", new String[]{Employee.STATUS_ACTIVE, Employee.STATUS_INACTIVE});
            }
        }
        // If terminated is null, no status filter is applied (returns all employees)
        
        int pageNum = page != null ? page : ConfigDefaults.DEFAULT_PAGE;
        int pageSize = size != null ? size : ConfigDefaults.DEFAULT_SIZE;
        
        if (filters.isEmpty()) {
            // No filters, use default method
            employees = organizationService.findAllEmployees(pageNum, pageSize);
        } else {
            // Use dynamic filtering
            employees = organizationService.findEmployeesWithFilters(filters, pageNum, pageSize);
        }
        
        return Response.ok(employees).build();
    }

    @GET
    @Path("/employees/active")
    @RolesAllowed({READ_PEOPLE})
    public Response getActiveEmployees() {
        List<Employee> employees = organizationService.findActiveEmployees();
        return Response.ok(employees).build();
    }

    @GET
    @Path("/employees/{id}")
    @RolesAllowed({READ_PEOPLE})
    public Response getEmployeeById(@PathParam("id") String id) {
        Optional<Employee> employee = organizationService.findEmployeeById(id);
        return employee.map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    @GET
    @Path("/employees/employee-id/{employeeId}")
    @RolesAllowed({READ_PEOPLE})
    public Response getEmployeeByEmployeeId(@PathParam("employeeId") String employeeId) {
        Optional<Employee> employee = organizationService.findEmployeeByEmployeeId(employeeId);
        return employee.map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    @PUT
    @Path("/employees/{id}")
    @RolesAllowed({WRITE_PEOPLE})
    public Response updateEmployee(@PathParam("id") String id, Employee employee) {
        try {
            employee.getObjectID().setId(id);
            Employee updated = organizationService.updateEmployee(employee);
            return Response.ok(updated).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/employees/{id}/terminate")
    @RolesAllowed({WRITE_PEOPLE})
    public Response terminateEmployee(@PathParam("id") String id, 
                                    @QueryParam("terminationDate") String terminationDateStr) {
        try {
            LocalDate terminationDate = LocalDate.parse(terminationDateStr);
            boolean terminated = organizationService.terminateEmployee(id, terminationDate);
            if (terminated) {
                return Response.ok().build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid date format", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/employees/{id}/resign")
    @RolesAllowed({WRITE_PEOPLE})
    public Response resignEmployee(@PathParam("id") String id, 
                                 @QueryParam("resignationDate") String resignationDateStr) {
        try {
            LocalDate resignationDate = LocalDate.parse(resignationDateStr);
            boolean resigned = organizationService.resignEmployee(id, resignationDate);
            if (resigned) {
                return Response.ok().build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid date format", e.getMessage()))
                    .build();
        }
    }

    // ========== EMPLOYEE ASSIGNMENTS ENDPOINTS ==========

    @POST
    @Path("/assignments")
    @RolesAllowed({WRITE_ASSIGNMENTS})
    public Response createEmployeeAssignment(EmployeeAssignment assignment) {
        try {
            EmployeeAssignment created = organizationService.createEmployeeAssignment(assignment);
            return Response.status(Response.Status.CREATED).entity(created).build();
        } catch (com.humanrsc.exceptions.DuplicateResourceException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse("Duplicate resource", e.getMessage(), "DUPLICATE_RESOURCE", e.getField(), e.getValue()))
                    .build();
        } catch (com.humanrsc.exceptions.AssignmentValidationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation error", e.getMessage(), "VALIDATION_ERROR", e.getField(), null))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Internal server error", "An unexpected error occurred while creating the employee assignment", "INTERNAL_ERROR"))
                    .build();
        }
    }

    @GET
    @Path("/assignments/employee/{employeeId}")
    @RolesAllowed({READ_ASSIGNMENTS})
    public Response getEmployeeAssignments(@PathParam("employeeId") String employeeId) {
        List<EmployeeAssignment> assignments = organizationService.findEmployeeAssignments(employeeId);
        return Response.ok(assignments).build();
    }

    @GET
    @Path("/assignments/employee/{employeeId}/current")
    @RolesAllowed({READ_ASSIGNMENTS})
    public Response getCurrentAssignment(@PathParam("employeeId") String employeeId) {
        Optional<EmployeeAssignment> assignment = organizationService.findCurrentAssignment(employeeId);
        return assignment.map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    @PUT
    @Path("/assignments/{id}")
    @RolesAllowed({WRITE_ASSIGNMENTS})
    public Response updateEmployeeAssignment(@PathParam("id") String id, com.humanrsc.datamodel.dto.EmployeeAssignmentDTO dto) {
        try {
            // Establecer el ObjectID si no existe
            if (dto.getObjectID() == null) {
                String tenantID = ThreadLocalStorage.getTenantID();
                dto.setObjectID(ObjectID.of(id, tenantID));
            } else {
                dto.getObjectID().setId(id);
            }
            
            EmployeeAssignment updated = organizationService.updateEmployeeAssignmentFromDTO(id, dto);
            return Response.ok(updated).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation error", e.getMessage()))
                    .build();
        }
    }

    // ========== STATISTICS ENDPOINTS ==========

    @GET
    @Path("/stats/organization")
    @RolesAllowed({STATS_READ})
    public Response getOrganizationStats() {
        OrganizationService.OrganizationStats stats = organizationService.getOrganizationStats();
        return Response.ok(stats).build();
    }

    @GET
    @Path("/stats/employees")
    @RolesAllowed({STATS_READ})
    public Response getEmployeeStats() {
        OrganizationService.EmployeeStats stats = organizationService.getEmployeeStats();
        return Response.ok(stats).build();
    }

    // ========== ORGANIZATION CHART ENDPOINTS ==========

    @GET
    @Path("/chart")
    @RolesAllowed({READ_ORG_UNITS, READ_PEOPLE})
    public Response getOrganizationChart() {
        OrganizationService.SimpleOrganizationChart chart = organizationService.getOrganizationChart();
        return Response.ok(chart).build();
    }

    // ========== TEMPORARY REPLACEMENTS ENDPOINTS ==========

    @POST
    @Path("/replacements")
    @RolesAllowed({WRITE_REPLACEMENTS})
    public Response createTemporaryReplacement(TemporaryReplacement replacement) {
        try {
            TemporaryReplacement created = organizationService.createTemporaryReplacement(replacement);
            return Response.status(Response.Status.CREATED).entity(created).build();
        } catch (com.humanrsc.exceptions.DuplicateResourceException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse("Duplicate resource", e.getMessage(), "DUPLICATE_RESOURCE", e.getField(), e.getValue()))
                    .build();
        } catch (com.humanrsc.exceptions.EmployeeValidationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation error", e.getMessage(), "VALIDATION_ERROR", e.getField(), null))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Internal server error", "An unexpected error occurred while creating the temporary replacement", "INTERNAL_ERROR"))
                    .build();
        }
    }
    
    @PUT
    @Path("/replacements/{id}")
    @RolesAllowed({WRITE_REPLACEMENTS})
    public Response updateTemporaryReplacement(@PathParam("id") String id, com.humanrsc.datamodel.dto.TemporaryReplacementDTO dto) {
        try {
            // Establecer el ObjectID si no existe
            if (dto.getObjectID() == null) {
                String tenantID = ThreadLocalStorage.getTenantID();
                dto.setObjectID(ObjectID.of(id, tenantID));
            } else {
                dto.getObjectID().setId(id);
            }
            
            TemporaryReplacement updated = organizationService.updateTemporaryReplacementFromDTO(id, dto);
            return Response.ok(updated).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/replacements")
    @RolesAllowed({READ_REPLACEMENTS})
    public Response getTemporaryReplacements(@QueryParam("status") String status) {
        List<TemporaryReplacement> replacements;
        if ("active".equals(status)) {
            replacements = organizationService.findActiveTemporaryReplacements();
        } else if ("current".equals(status)) {
            replacements = organizationService.findCurrentTemporaryReplacements();
        } else {
            replacements = organizationService.findActiveTemporaryReplacements();
        }
        return Response.ok(replacements).build();
    }

    @GET
    @Path("/replacements/employee/{employeeId}")
    @RolesAllowed({READ_REPLACEMENTS})
    public Response getTemporaryReplacementsByEmployee(@PathParam("employeeId") String employeeId) {
        List<TemporaryReplacement> replacements = organizationService.findTemporaryReplacementsByEmployee(employeeId);
        return Response.ok(replacements).build();
    }

    @POST
    @Path("/replacements/{id}/complete")
    @RolesAllowed({WRITE_REPLACEMENTS})
    public Response completeTemporaryReplacement(@PathParam("id") String id) {
        boolean completed = organizationService.completeTemporaryReplacement(id);
        if (completed) {
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @POST
    @Path("/replacements/{id}/cancel")
    @RolesAllowed({WRITE_REPLACEMENTS})
    public Response cancelTemporaryReplacement(@PathParam("id") String id) {
        boolean cancelled = organizationService.cancelTemporaryReplacement(id);
        if (cancelled) {
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    // ========== SALARY HISTORY ENDPOINTS ==========

    @POST
    @Path("/salary-history")
    @RolesAllowed({WRITE_SALARIES})
    public Response createSalaryHistory(SalaryHistory salaryHistory) {
        try {
            SalaryHistory created = organizationService.createSalaryHistory(salaryHistory);
            return Response.status(Response.Status.CREATED).entity(created).build();
        } catch (com.humanrsc.exceptions.DuplicateResourceException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse("Duplicate resource", e.getMessage(), "DUPLICATE_RESOURCE", e.getField(), e.getValue()))
                    .build();
        } catch (com.humanrsc.exceptions.EmployeeValidationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation error", e.getMessage(), "VALIDATION_ERROR", e.getField(), null))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Internal server error", "An unexpected error occurred while creating the salary history", "INTERNAL_ERROR"))
                    .build();
        }
    }
    
    @PUT
    @Path("/salary-history/{id}")
    @RolesAllowed({WRITE_SALARIES})
    public Response updateSalaryHistory(@PathParam("id") String id, com.humanrsc.datamodel.dto.SalaryHistoryDTO dto) {
        try {
            // Establecer el ObjectID si no existe
            if (dto.getObjectID() == null) {
                String tenantID = ThreadLocalStorage.getTenantID();
                dto.setObjectID(ObjectID.of(id, tenantID));
            } else {
                dto.getObjectID().setId(id);
            }
            
            SalaryHistory updated = organizationService.updateSalaryHistoryFromDTO(id, dto);
            return Response.ok(updated).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/salary-history/employee/{employeeId}")
    @RolesAllowed({READ_SALARIES})
    public Response getSalaryHistoryByEmployee(@PathParam("employeeId") String employeeId) {
        List<SalaryHistory> salaryHistory = organizationService.findSalaryHistoryByEmployee(employeeId);
        return Response.ok(salaryHistory).build();
    }

    @GET
    @Path("/salary-history/recent")
    @RolesAllowed({READ_SALARIES})
    public Response getRecentSalaryChanges() {
        List<SalaryHistory> salaryHistory = organizationService.findRecentSalaryChanges();
        return Response.ok(salaryHistory).build();
    }

    @GET
    @Path("/salary-history/increases")
    @RolesAllowed({READ_SALARIES})
    public Response getSalaryIncreases() {
        List<SalaryHistory> increases = organizationService.findSalaryIncreases();
        return Response.ok(increases).build();
    }

    @GET
    @Path("/salary-history/decreases")
    @RolesAllowed({READ_SALARIES})
    public Response getSalaryDecreases() {
        List<SalaryHistory> decreases = organizationService.findSalaryDecreases();
        return Response.ok(decreases).build();
    }

    @POST
    @Path("/employees/{id}/salary")
    @RolesAllowed({WRITE_SALARIES})
    public Response updateEmployeeSalary(@PathParam("id") String id,
                                       @QueryParam("newSalary") String newSalaryStr,
                                       @QueryParam("reason") String reason,
                                       @QueryParam("approvedBy") String approvedById) {
        try {
            BigDecimal newSalary = new BigDecimal(newSalaryStr);
            organizationService.updateEmployeeSalary(id, newSalary, reason, approvedById);
            return Response.ok().build();
        } catch (NumberFormatException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid salary format", e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Error updating salary", e.getMessage()))
                    .build();
        }
    }

    // ========== BUSINESS LOGIC ENDPOINTS ==========

    @GET
    @Path("/employees/manager/{managerId}")
    @RolesAllowed({READ_PEOPLE})
    public Response getEmployeesByManager(@PathParam("managerId") String managerId) {
        List<Employee> employees = organizationService.findEmployeesByManager(managerId);
        return Response.ok(employees).build();
    }

    @GET
    @Path("/employees/unit/{unitId}")
    @RolesAllowed({READ_PEOPLE})
    public Response getEmployeesByUnit(@PathParam("unitId") String unitId) {
        List<Employee> employees = organizationService.findEmployeesByUnit(unitId);
        return Response.ok(employees).build();
    }

    @GET
    @Path("/employees/position/{positionId}")
    @RolesAllowed({READ_PEOPLE})
    public Response getEmployeesByPosition(@PathParam("positionId") String positionId) {
        List<Employee> employees = organizationService.findEmployeesByPosition(positionId);
        return Response.ok(employees).build();
    }

    @GET
    @Path("/positions/vacant")
    @RolesAllowed({READ_POSITIONS})
    public Response getVacantPositions() {
        List<JobPosition> vacantPositions = organizationService.findAllPositions().stream()
                .filter(position -> organizationService.findEmployeesByPosition(position.getObjectID().getId()).isEmpty())
                .toList();
        return Response.ok(vacantPositions).build();
    }

    // ========== ORGANIZATIONAL LEVELS ENDPOINTS ==========

    @GET
    @Path("/units/level/{level}")
    @RolesAllowed({READ_ORG_UNITS})
    public Response getUnitsByOrganizationalLevel(@PathParam("level") Integer level) {
        List<OrganizationalUnit> units = organizationService.findUnitsByOrganizationalLevel(level);
        return Response.ok(units).build();
    }

    @GET
    @Path("/units/level-range")
    @RolesAllowed({READ_ORG_UNITS})
    public Response getUnitsByOrganizationalLevelRange(@QueryParam("min") Integer minLevel, 
                                                      @QueryParam("max") Integer maxLevel) {
        if (minLevel == null || maxLevel == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Missing parameters", "Both min and max level parameters are required"))
                    .build();
        }
        List<OrganizationalUnit> units = organizationService.findUnitsByOrganizationalLevelRange(minLevel, maxLevel);
        return Response.ok(units).build();
    }

    @GET
    @Path("/units/level/{level}/count")
    @RolesAllowed({READ_ORG_UNITS})
    public Response getUnitCountByOrganizationalLevel(@PathParam("level") Integer level) {
        long count = organizationService.countUnitsByOrganizationalLevel(level);
        return Response.ok(new CountResponse(count)).build();
    }

    // ========== INNER CLASSES ==========

    public static class CountResponse {
        private final long count;

        public CountResponse(long count) {
            this.count = count;
        }

        public long getCount() { return count; }
    }

    public static class ErrorResponse {
        private final String error;
        private final String message;
        private final String errorCode;
        private final String field;
        private final String details;

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
            this.errorCode = null;
            this.field = null;
            this.details = null;
        }

        public ErrorResponse(String error, String message, String errorCode) {
            this.error = error;
            this.message = message;
            this.errorCode = errorCode;
            this.field = null;
            this.details = null;
        }

        public ErrorResponse(String error, String message, String errorCode, String field, String details) {
            this.error = error;
            this.message = message;
            this.errorCode = errorCode;
            this.field = field;
            this.details = details;
        }

        public String getError() { return error; }
        public String getMessage() { return message; }
        public String getErrorCode() { return errorCode; }
        public String getField() { return field; }
        public String getDetails() { return details; }
    }
}

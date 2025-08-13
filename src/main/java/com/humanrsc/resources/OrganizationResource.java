package com.humanrsc.resources;

import com.humanrsc.datamodel.entities.*;
import com.humanrsc.datamodel.entities.PositionCategory;
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
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/units")
    @RolesAllowed({READ_ORG_UNITS})
    public Response getAllUnits(@QueryParam("page") Integer page, @QueryParam("size") Integer size) {
        int pageNum = page != null ? page : ConfigDefaults.DEFAULT_PAGE;
        int pageSize = size != null ? size : ConfigDefaults.DEFAULT_SIZE;
        List<OrganizationalUnit> units = organizationService.findAllUnits(pageNum, pageSize);
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
    public Response updateOrganizationalUnit(@PathParam("id") String id, OrganizationalUnit unit) {
        try {
            unit.getObjectID().setId(id);
            OrganizationalUnit updated = organizationService.updateOrganizationalUnit(unit);
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
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/positions")
    @RolesAllowed({READ_POSITIONS})
    public Response getAllPositions(@QueryParam("page") Integer page, @QueryParam("size") Integer size) {
        int pageNum = page != null ? page : ConfigDefaults.DEFAULT_PAGE;
        int pageSize = size != null ? size : ConfigDefaults.DEFAULT_SIZE;
        List<JobPosition> positions = organizationService.findAllPositions(pageNum, pageSize);
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
    public Response updateJobPosition(@PathParam("id") String id, JobPosition position) {
        try {
            position.getObjectID().setId(id);
            JobPosition updated = organizationService.updateJobPosition(position);
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
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/employees")
    @RolesAllowed({READ_PEOPLE})
    public Response getAllEmployees(@QueryParam("page") Integer page, @QueryParam("size") Integer size,
                                   @QueryParam("status") String status,
                                   @QueryParam("type") String employeeType) {
        List<Employee> employees;
        
        if (status != null) {
            employees = organizationService.findEmployeesByStatus(status);
        } else if (employeeType != null) {
            try {
                Employee.EmployeeType type = Employee.EmployeeType.valueOf(employeeType.toUpperCase());
                employees = organizationService.findEmployeesByType(type);
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Invalid employee type", e.getMessage()))
                        .build();
            }
        } else {
            int pageNum = page != null ? page : ConfigDefaults.DEFAULT_PAGE;
            int pageSize = size != null ? size : ConfigDefaults.DEFAULT_SIZE;
            employees = organizationService.findAllEmployees(pageNum, pageSize);
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
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation error", e.getMessage()))
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
    public Response updateEmployeeAssignment(@PathParam("id") String id, EmployeeAssignment assignment) {
        try {
            assignment.getObjectID().setId(id);
            EmployeeAssignment updated = organizationService.updateEmployeeAssignment(assignment);
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

    // ========== INNER CLASSES ==========

    public static class ErrorResponse {
        private final String error;
        private final String message;

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }

        public String getError() { return error; }
        public String getMessage() { return message; }
    }
}

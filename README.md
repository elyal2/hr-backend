# HR Backend

A comprehensive Human Resources management system built with Quarkus and Panache.

## Features

- Multi-tenant architecture
- Organizational structure management
- Employee lifecycle management
- Job position management with hierarchical levels
- Position categories for grouping positions
- Salary history tracking
- Temporary replacements
- Audit logging
- JWT-based security

## Data Model

### Organizational Structure
- **Organizational Units**: Hierarchical structure representing company departments with organizational levels
- **Job Positions**: Roles within units with numerical hierarchical levels (1 = CEO, 2 = Directors, etc.)
- **Position Categories**: Labels for grouping positions (e.g., "Directors", "Managers", "Administrative")
- **Employees**: Assigned to units and occupying positions

### Key Concepts
- **Organizational Levels**: Numerical values assigned to units (1 = Top level, 2 = Departments, 3 = Sub-departments, etc.)
- **Hierarchical Levels**: Numerical values assigned to positions (1 = CEO, 2 = Directors, 3 = Managers, etc.)
- **Position Categories**: Text labels for grouping similar positions
- **Organizational Units**: Company structure with parent-child relationships and organizational levels

## API Endpoints

### Organization Chart

The system provides an efficient endpoint for retrieving organizational structure data:

#### Organization Chart
```
GET /api/organization/chart
```
Returns organizational structure with aggregated counts:
- All organizational units with their metadata
- Employee count per unit
- Position count per unit
- Position categories
- Parent-child relationships

The endpoint uses optimized SQL with JOINs and GROUP BY to provide efficient data retrieval.

### Position Categories

```
POST /api/organization/position-categories
GET /api/organization/position-categories
GET /api/organization/position-categories/{id}
PUT /api/organization/position-categories/{id}
DELETE /api/organization/position-categories/{id}
```

### Job Positions by Category and Level

```
GET /api/organization/positions/category/{categoryId}
GET /api/organization/positions/level/{level}
GET /api/organization/positions/level-range?min=X&max=Y
```

### Organizational Units by Level

```
GET /api/organization/units/level/{level}
GET /api/organization/units/level-range?min=X&max=Y
GET /api/organization/units/level/{level}/count
```

### Example Response Structure

```json
{
  "units": [
    {
      "id": "unit-1",
    "name": "Recursos Humanos",
    "description": "Departamento de RRHH",
      "location": "Madrid",
      "parentUnitId": null,
      "employeeCount": 5,
      "positionCount": 3,
      "minHierarchicalLevel": 1,
      "maxHierarchicalLevel": 3,
      "hasPositions": true,
      "organizationalLevel": 1
    },
    {
      "id": "unit-2", 
      "name": "Reclutamiento",
      "description": "Subdepartamento de reclutamiento",
    "location": "Madrid",
      "parentUnitId": "unit-1",
      "employeeCount": 2,
      "positionCount": 1,
      "minHierarchicalLevel": 2,
      "maxHierarchicalLevel": 2,
      "hasPositions": true,
      "organizationalLevel": 2
    }
  ],
  "categories": [
    {
      "id": "cat-1",
      "name": "Directores",
      "description": "Posiciones de dirección"
    },
    {
      "id": "cat-2", 
      "name": "Managers",
      "description": "Posiciones de gestión"
    }
  ]
}
```

## Getting Started

1. Clone the repository
2. Configure your database connection in `application.properties`
3. Run the application with: `./mvnw quarkus:dev`

## Database Migrations

The application uses Flyway for database migrations. The latest migrations include:

### V1.4.0 - Reorganize Position Structure
- **New**: `position_categories` table for grouping positions
- **New**: `hierarchical_level` column in `job_positions` table
- **Removed**: `organizational_levels` table and related columns
- **Updated**: Foreign key relationships and indices

### V1.5.0 - Add Organizational Levels
- **New**: `organizational_level` column in `organizational_units` table
- **New**: Indexes for efficient level-based queries
- **Updated**: Organization chart to include organizational levels

### Key Changes
- Job positions have numerical hierarchical levels (1, 2, 3, etc.)
- Organizational units have organizational levels (1 = Top level, 2 = Departments, etc.)
- Position categories provide text labels for grouping positions
- Two separate hierarchies: organizational (units) and hierarchical (positions)
- Improved data model alignment with business concepts

Initial schema is created automatically on startup.
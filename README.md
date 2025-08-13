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
- **Organizational Units**: Hierarchical structure representing company departments
- **Job Positions**: Roles within units with numerical hierarchical levels (1 = CEO, 2 = Directors, etc.)
- **Position Categories**: Labels for grouping positions (e.g., "Directors", "Managers", "Administrative")
- **Employees**: Assigned to units and occupying positions

### Key Concepts
- **Hierarchical Levels**: Numerical values assigned to positions (1, 2, 3, etc.)
- **Position Categories**: Text labels for grouping similar positions
- **Organizational Units**: Company structure with parent-child relationships

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
      "hasLevels": true
    },
    {
      "id": "unit-2", 
      "name": "Reclutamiento",
      "description": "Subdepartamento de reclutamiento",
      "location": "Madrid",
      "parentUnitId": "unit-1",
      "employeeCount": 2,
      "positionCount": 1,
      "hasLevels": true
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

The application uses Flyway for database migrations. The latest migration (V1.4.0) includes:

### V1.4.0 - Reorganize Position Structure
- **New**: `position_categories` table for grouping positions
- **New**: `hierarchical_level` column in `job_positions` table
- **Removed**: `organizational_levels` table and related columns
- **Updated**: Foreign key relationships and indices

### Key Changes
- Job positions now have numerical hierarchical levels (1, 2, 3, etc.)
- Position categories provide text labels for grouping positions
- Organizational units no longer have direct level associations
- Improved data model alignment with business concepts

Initial schema is created automatically on startup.
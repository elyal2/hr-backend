# HR Backend

A comprehensive Human Resources management system built with Quarkus and Panache.

## Features

- **Multi-tenant architecture** with Row Level Security (RLS)
- **Organizational structure management** with hierarchical levels
- **Employee lifecycle management** with status tracking
- **Job position management** with hierarchical levels and categories
- **Salary management** with multi-currency support and exchange rates
- **Assignment tracking** with historical data
- **Temporary replacements** management
- **Audit logging** with Hibernate Envers
- **JWT-based security** with role-based permissions
- **Dashboard KPIs** and analytics endpoints
- **Currency exchange rates** system with EUR base conversion

## Data Model

### Organizational Structure
- **Organizational Units**: Hierarchical structure with organizational levels (1 = Top level, 2 = Departments, etc.)
- **Job Positions**: Roles with hierarchical levels (1 = CEO, 2 = Directors, 3 = Managers, etc.)
- **Position Categories**: Labels for grouping positions (e.g., "Directors", "Managers", "Administrative")
- **Employees**: Assigned to units and occupying positions with salary and currency information

### Key Concepts
- **Organizational Levels**: Numerical values for units (1 = Top level, 2 = Departments, 3 = Sub-departments, etc.)
- **Hierarchical Levels**: Numerical values for positions (1 = CEO, 2 = Directors, 3 = Managers, etc.)
- **Position Categories**: Text labels for grouping similar positions
- **Multi-currency Support**: Salary tracking with exchange rates to EUR base currency

## API Endpoints

### Dashboard KPIs and Analytics

#### Employee Counts
```http
GET /api/organization/employees/count
GET /api/organization/employees/count?active=true
GET /api/organization/employees/count?terminated=true
GET /api/organization/employees/count?inactive=true
GET /api/organization/employees/count?resigned=true
```

#### Unit Counts
```http
GET /api/organization/units/count
GET /api/organization/units/count?active=true
GET /api/organization/units/count?root=true
GET /api/organization/units/count?leaf=true
GET /api/organization/units/count?withChildren=true
GET /api/organization/units/count?level=2
```

#### Position Counts
```http
GET /api/organization/positions/count
GET /api/organization/positions/count?active=true
GET /api/organization/positions/count?vacant=true
GET /api/organization/positions/count?inactive=true
GET /api/organization/positions/count?deleted=true
```

#### Category Counts
```http
GET /api/organization/position-categories/count
GET /api/organization/position-categories/count?active=true
GET /api/organization/position-categories/count?inactive=true
GET /api/organization/position-categories/count?deleted=true
```

#### Assignment Counts
```http
GET /api/organization/assignments/count
GET /api/organization/assignments/count?active=true
GET /api/organization/assignments/count?historical=true
```

### Statistics Endpoints

#### Salary Statistics
```http
GET /api/organization/employees/salary-stats
```
Returns salary statistics converted to EUR:
```json
{
  "totalBudget": 2450000.00,
  "avgSalary": 49000.00,
  "maxSalary": 180000.00,
  "minSalary": 28000.00,
  "primaryCurrency": "EUR",
  "currencyDistribution": {
    "USD": 38,
    "EUR": 7,
    "GBP": 3,
    "CAD": 2
  }
}
```

#### Assignment Statistics
```http
GET /api/organization/assignments/stats
```
Returns assignment coverage statistics:
```json
{
  "activeAssignments": 1156,
  "employeesWithAssignments": 1156,
  "totalEmployees": 1234,
  "assignmentPercentage": 93.7
}
```

#### Organizational Structure Statistics
```http
GET /api/organization/units/structure-stats
```
Returns organizational hierarchy analysis:
```json
{
  "totalUnits": 24,
  "rootUnits": 3,
  "unitsWithChildren": 8,
  "leafUnits": 16,
  "maxLevel": 1,
  "minLevel": 4,
  "unitsByLevel": {
    "1": 3,
    "2": 5,
    "3": 12,
    "4": 4
  }
}
```

### Currency Exchange Rates

#### Manage Exchange Rates
```http
POST /api/organization/currency-rates
GET /api/organization/currency-rates
GET /api/organization/currency-rates/{id}
PUT /api/organization/currency-rates/{id}
DELETE /api/organization/currency-rates/{id}
```

#### Convert Amounts
```http
POST /api/organization/currency-rates/convert?amount=50000&fromCurrency=USD&toCurrency=EUR
```

### Organization Chart

#### Organization Chart
```http
GET /api/organization/chart
```
Returns organizational structure with aggregated counts:
- All organizational units with their metadata
- Employee count per unit
- Position count per unit
- Position categories
- Parent-child relationships

### Position Categories

```http
POST /api/organization/position-categories
GET /api/organization/position-categories
GET /api/organization/position-categories/{id}
PUT /api/organization/position-categories/{id}
DELETE /api/organization/position-categories/{id}
```

### Job Positions by Category and Level

```http
GET /api/organization/positions/category/{categoryId}
GET /api/organization/positions/level/{level}
GET /api/organization/positions/level-range?min=X&max=Y
```

### Organizational Units by Level

```http
GET /api/organization/units/level/{level}
GET /api/organization/units/level-range?min=X&max=Y
GET /api/organization/units/level/{level}/count
```

## Getting Started

1. **Clone the repository**
2. **Configure your database connection** in `application.properties`
3. **Run the application**: `./mvnw quarkus:dev`

## Database Migrations

The application uses Flyway for database migrations. The latest migrations include:

### V1.6.0 - Add Currency Exchange Rates
- **New**: `currency_exchange_rates` table for multi-currency support
- **New**: EUR-based conversion system for salary statistics
- **New**: Audit tables for exchange rate tracking
- **New**: RLS policies for multi-tenant isolation

### V1.5.0 - Add Organizational Levels
- **New**: `organizational_level` column in `organizational_units` table
- **New**: Indexes for efficient level-based queries
- **Updated**: Organization chart to include organizational levels

### V1.4.0 - Reorganize Position Structure
- **New**: `position_categories` table for grouping positions
- **New**: `hierarchical_level` column in `job_positions` table
- **Removed**: `organizational_levels` table and related columns
- **Updated**: Foreign key relationships and indices

### Key Changes
- Job positions have numerical hierarchical levels (1, 2, 3, etc.)
- Organizational units have organizational levels (1 = Top level, 2 = Departments, etc.)
- Position categories provide text labels for grouping positions
- Multi-currency support with EUR base conversion
- Two separate hierarchies: organizational (units) and hierarchical (positions)

## Logging Configuration

### Error Logging Levels

The system is configured to be less "noisy" with common errors:

#### 1. 404 Errors (Endpoint not found)
- **Default level**: `DEBUG`
- **Configuration**: `app.logging.404.level=DEBUG`
- **Behavior**: 404 errors are normal when testing non-existent endpoints

#### 2. Validation Errors
- **Default level**: `INFO`
- **Configuration**: `app.logging.validation.level=INFO`
- **Behavior**: Input validation errors are expected and logged at INFO level

#### 3. Unexpected Errors
- **Default level**: `ERROR`
- **Configuration**: `app.logging.unexpected.level=ERROR`
- **Behavior**: Only truly unexpected errors are logged at ERROR level

### Configuration in application.properties

```properties
# Granular error logging configuration
app.logging.404.level=DEBUG
app.logging.validation.level=INFO
app.logging.unexpected.level=ERROR

# In development, you can change to DEBUG for more details
%dev.app.logging.404.level=DEBUG
%dev.app.logging.validation.level=DEBUG
```

### Profile-based Configuration

#### Development (`%dev`)
```properties
%dev.quarkus.log.category."com.humanrsc".level=DEBUG
%dev.quarkus.log.category."com.humanrsc.exceptions.GlobalExceptionHandler".level=DEBUG
```

#### Production
```properties
quarkus.log.category."com.humanrsc.exceptions.GlobalExceptionHandler".level=INFO
```

## Debug Endpoints

### Health Check
```bash
GET /debug/health
```
Verifies the general application status.

### Route List
```bash
GET /debug/routes
```
Shows all available routes.

### User Information
```bash
GET /debug/me
```
Shows authenticated user information and current tenant.

## Error Types and Logging Levels

### 404 Errors (DEBUG)
- Non-existent endpoints
- Malformed URLs
- Incorrect HTTP methods

### Validation Errors (INFO)
- `DuplicateResourceException`
- `ResourceNotFoundException`
- `EmployeeValidationException`
- `JobPositionValidationException`
- `OrganizationalUnitValidationException`
- `AssignmentValidationException`
- `IllegalArgumentException`
- `NotAllowedException`
- `BadRequestException`
- `ConstraintViolationException`

### Unexpected Errors (ERROR)
- Unhandled exceptions
- Database errors
- Configuration errors
- Authentication/authorization errors

## Best Practices

1. **Don't worry about 404 errors**: They're normal when testing non-existent endpoints
2. **Use debug endpoints**: For quick problem diagnosis
3. **Configure logging appropriately**: Use DEBUG in development, INFO in production
4. **Monitor unexpected errors**: Only these should be cause for concern

## Troubleshooting

### If you see many 404 errors:
1. Verify URLs are correct
2. Review API documentation
3. Use `/debug/routes` to see available endpoints

### If you see validation errors:
1. Verify data format being sent
2. Review specific error messages
3. Consult validation documentation

### If you see unexpected errors:
1. Review complete logs
2. Verify configuration
3. Contact development team

## Security

### Multi-tenant Architecture
- Row Level Security (RLS) for data isolation
- Tenant context propagation
- Automatic tenant filtering

### JWT Authentication
- Auth0 integration
- Role-based permissions
- Token validation

### Permissions
- Granular permissions per resource
- Read/Write permissions for each entity type
- Statistics and audit permissions

## Performance

### Optimized Queries
- Efficient organization chart with JOINs and GROUP BY
- Pagination support for large datasets
- Indexed queries for common operations

### Caching
- Entity caching with Hibernate
- Query result caching
- Tenant-aware caching

Initial schema is created automatically on startup.
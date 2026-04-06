# AGENTS.md — hr-backend

Multi-tenant HR SaaS API built with Quarkus 3.25, Java 21, PostgreSQL 16, Hibernate Envers, and Auth0.

---

## Build / Lint / Test

```bash
# Compilation (source of truth — trust this over IDE/LSP)
./mvnw compile

# Full test suite
./mvnw test
./mvnw verify              # tests + packaging

# Run a single test class
./mvnw test -Dtest=MyTest

# Run a single test method
./mvnw test -Dtest=MyTest#myTestMethod

# Lint & format
./mvnw checkstyle:check                    # lint
./mvnw com.coveo:fmt-maven-plugin:format   # format code

# Quick dev cycle (hot reload, no Docker)
./mvnw quarkus:dev

# Docker
make up                                    # build + start
make down                                  # stop
make health                                # check status
make logs-api                              # tail backend logs
make openapi-gen                           # regenerate openapi.yaml from running server
make shell-db                              # open psql
```

---

## Code Style

### Formatting
- **google-java-format** via `com.coveo:fmt-maven-plugin`. Run `make format` before committing.
- 2-space indentation is NOT used — google-java-format enforces its own style.
- LSP errors may be stale — always verify with `./mvnw compile`.

### Imports
- Group: `java.*`, `javax.*`/`jakarta.*`, third-party, then project imports.
- No wildcard imports (`import java.util.*`) — always fully qualify.

### Naming
- Entities: PascalCase nouns (`Employee`, `OrganizationalUnit`)
- Repositories: `{Entity}Repository`
- Services: `{Entity}Service`
- Resources: `{Domain}Resource`
- DTOs: `Create{Entity}DTO`, `Update{Entity}DTO`, `{Entity}DTO`
- Exceptions: `{Entity}ValidationException`
- Constants: `UPPER_SNAKE_CASE` (e.g., `STATUS_ACTIVE`, `CONTRACT_TYPE_FULL_TIME`)
- Database: `snake_case` for tables/columns, plural table names

### Types & Entities
- Java 21: use records, sealed classes, pattern matching, text blocks, `var` where appropriate.
- Lombok: `@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor` — never write manual getters/setters.
- All entities use `@EmbeddedId ObjectID objectID` (composite `id + tenantID`).
- All `@ManyToOne` use `@JoinColumns` with both `id` and `tenant_id` columns.
- All relationship fields annotated with `@com.fasterxml.jackson.annotation.JsonIgnore`.
- Status as `public static final String` constants, not enums.
- `@NotAudited` on `@ElementCollection` extended attributes.

### Annotations
- `@ApplicationScoped` for services and repositories (never `@Singleton`).
- `@Transactional` on all mutating service methods.
- `@Audited` on entities that need audit history; `@NotAudited` to exclude fields.
- `@JWTSecured` + `@ConnectionPoolIntercepted` on all resource classes.
- `@RolesAllowed({PERMISSION})` on every endpoint method.

### Error Handling
- Let `GlobalExceptionHandler` catch exceptions — only use inline try/catch for specific error responses.
- Use typed exceptions: `ResourceNotFoundException`, `DuplicateResourceException`, `{Entity}ValidationException`.
- Never swallow exceptions silently.
- Log levels: 404 → DEBUG, validation → INFO, unexpected → ERROR.

### DTOs
- Never accept JPA entities as `@POST`/`@PUT` request bodies (mass-assignment risk).
- Use `Create*DTO` for POST, `Update*DTO` or `*DTO` for PUT/PATCH.
- DTOs use flat String ID references (`String unitId`) not nested entities.

### Multi-Tenancy
- Rely on PostgreSQL RLS for tenant filtering — do NOT add explicit `tenantID` filters in queries.
- Get tenant from `ThreadLocalStorage.getTenantID()` only when generating new IDs.
- Never fall back to a hardcoded default tenant — reject the request.

### Database Migrations (Flyway)
- Location: `src/main/resources/db/migration/`
- Naming: `V{major}.{minor}.{patch}__{description}.sql`
- Every new table: composite PK `(id, tenant_id)`, RLS policy, `_aud` table if audited, indexes on FKs.
- Never edit existing migrations.

### OpenAPI
- Regenerate after any endpoint change: `make openapi-gen`
- Do NOT hand-edit `openapi.yaml` — it is generated from the running server.

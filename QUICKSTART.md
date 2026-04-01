# HR Backend - Guía de Inicio Rápido

## Prerrequisitos

- Docker y Docker Compose instalados
- Cuenta de Auth0 configurada

## Configuración Inicial

### 1. Configura el archivo `.env`

Copia el archivo de ejemplo y edita las credenciales:

```bash
cp .env.example .env
```

Edita `.env` y configura las siguientes variables:

**1. POSTGRES_PASSWORD** (requerido):
```bash
POSTGRES_PASSWORD=your-secure-password-here  # Elige una contraseña fuerte
```

**2. AUTH0_CLIENT_SECRET** (requerido):
1. Ve a [Auth0 Dashboard](https://manage.auth0.com/dashboard/)
2. Navega a: **Applications** → **HumanRSC API (Server Application M2M)**
3. Copia el **Client Secret**
4. Pégalo en el archivo `.env`:
```bash
AUTH0_CLIENT_SECRET=your-auth0-client-secret-here
```

### 2. Inicia los servicios

```bash
make up
```

Este comando:
- ✅ Verifica que el `.env` esté configurado
- ✅ Construye la imagen Docker del backend
- ✅ Inicia PostgreSQL (puerto 5432)
- ✅ Ejecuta las migraciones de Flyway
- ✅ Inicia el backend (puerto 8080)
- ✅ Verifica el estado de salud de los servicios

### 3. Verifica que todo funciona

```bash
# Ver el estado de los servicios
make health

# Ver logs en tiempo real
make logs

# Ver solo logs del backend
make logs-api
```

## URLs Disponibles

Una vez que los servicios estén corriendo:

- **API Base:** http://localhost:8080
- **Health Check:** http://localhost:8080/q/health
- **Swagger UI:** http://localhost:8080/q/swagger-ui
- **PostgreSQL:** localhost:5432 (usuario: `postgres`, password: el que configuraste en `.env`)

## Comandos Útiles

```bash
make help           # Lista todos los comandos disponibles
make up             # Inicia los servicios
make down           # Detiene los servicios
make restart        # Reinicia los servicios
make logs           # Ver logs de todos los servicios
make logs-api       # Ver logs solo del backend
make logs-db        # Ver logs solo de PostgreSQL
make ps             # Ver estado de contenedores
make shell-db       # Abrir psql en PostgreSQL
make shell-api      # Abrir shell en el backend
make clean          # Detener y eliminar volúmenes (¡borra la BD!)
make db-reset       # Resetear base de datos completamente
```

## Desarrollo Local (sin Docker)

Si prefieres correr el backend localmente sin Docker:

```bash
# 1. Inicia solo PostgreSQL con Docker
docker-compose up -d postgres

# 2. Corre el backend en modo desarrollo (hot reload)
make dev
```

## Probar la API

### Obtener un token de Auth0 (Machine-to-Machine)

```bash
curl --request POST \
  --url https://dev-ygblu06dwvh8ags3.us.auth0.com/oauth/token \
  --header 'content-type: application/json' \
  --data '{
    "client_id": "DyJO3hx1dJ4LmZ2oqz5KwNpNApTaeUp4",
    "client_secret": "TU_CLIENT_SECRET_AQUI",
    "audience": "https://api.humanrsc.com",
    "grant_type": "client_credentials"
  }'
```

### Llamar a la API con el token

```bash
# Reemplaza {TOKEN} con el access_token obtenido
curl http://localhost:8080/api/v1/employees \
  -H "Authorization: Bearer {TOKEN}"
```

## Solución de Problemas

### El backend no arranca

```bash
# Ver logs detallados
make logs-api

# Verificar que PostgreSQL está listo
make health

# Reintentar
make restart
```

### Error de autenticación

Verifica que:
1. El `AUTH0_CLIENT_SECRET` en `.env` es correcto
2. El `AUTH0_AUDIENCE` coincide con tu API en Auth0
3. El token JWT incluye el claim `https://hrplus.api/tenant`

### La base de datos no tiene datos

Las migraciones de Flyway se ejecutan automáticamente al arrancar. Si necesitas resetear:

```bash
make db-reset
```

## Arquitectura

- **Backend:** Quarkus 3.16 (Java 21), JAX-RS, Hibernate ORM
- **Base de datos:** PostgreSQL 16 con Row-Level Security (RLS)
- **Autenticación:** Auth0 con JWT
- **Multi-tenancy:** RLS policies con `tenant_id` en todas las tablas

Ver documentación completa en [`_docs/`](./_docs/README.md)

## Siguiente Paso

Una vez que todo funcione, revisa la [Guía de Migración a MapStruct](./_docs/MAPSTRUCT_MIGRATION.md) para mejorar el código.

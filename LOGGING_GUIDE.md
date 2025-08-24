# Guía de Logging - HR Backend

## Configuración de Logging

### Niveles de Logging por Tipo de Error

El sistema de logging está configurado para ser menos "escandaloso" con errores comunes:

#### 1. Errores 404 (Endpoint no encontrado)
- **Nivel por defecto**: `DEBUG`
- **Configuración**: `app.logging.404.level=DEBUG`
- **Comportamiento**: Los errores 404 son normales cuando alguien prueba endpoints que no existen, por lo que se registran en nivel DEBUG

#### 2. Errores de Validación
- **Nivel por defecto**: `INFO`
- **Configuración**: `app.logging.validation.level=INFO`
- **Comportamiento**: Errores de validación de entrada son esperados y se registran en nivel INFO

#### 3. Errores Inesperados
- **Nivel por defecto**: `ERROR`
- **Configuración**: `app.logging.unexpected.level=ERROR`
- **Comportamiento**: Solo errores realmente inesperados se registran en nivel ERROR

### Configuración en application.properties

```properties
# Configuración granular para logging de errores
app.logging.404.level=DEBUG
app.logging.validation.level=INFO
app.logging.unexpected.level=ERROR

# En desarrollo, puedes cambiar a DEBUG para ver más detalles
%dev.app.logging.404.level=DEBUG
%dev.app.logging.validation.level=DEBUG
```

### Configuración por Perfil

#### Desarrollo (`%dev`)
```properties
%dev.quarkus.log.category."com.humanrsc".level=DEBUG
%dev.quarkus.log.category."com.humanrsc.exceptions.GlobalExceptionHandler".level=DEBUG
```

#### Producción
```properties
quarkus.log.category."com.humanrsc.exceptions.GlobalExceptionHandler".level=INFO
```

## Tipos de Errores y Niveles de Log

### Errores 404 (DEBUG)
- Endpoints que no existen
- URLs malformadas
- Métodos HTTP incorrectos

### Errores de Validación (INFO)
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

### Errores Inesperados (ERROR)
- Excepciones no manejadas
- Errores de base de datos
- Errores de configuración
- Errores de autenticación/autorización

## Endpoints de Diagnóstico

### Health Check
```bash
GET /debug/health
```
Verifica el estado general de la aplicación.

### Lista de Rutas
```bash
GET /debug/routes
```
Muestra todas las rutas disponibles.

### Información del Usuario
```bash
GET /debug/me
```
Muestra información del usuario autenticado y el tenant actual.

## Mejores Prácticas

1. **No te alarmes por errores 404**: Son normales cuando alguien prueba endpoints que no existen
2. **Usa los endpoints de debug**: Para diagnosticar problemas rápidamente
3. **Configura el logging apropiadamente**: En desarrollo usa DEBUG, en producción usa INFO
4. **Monitorea errores inesperados**: Solo estos deberían ser motivo de preocupación

## Troubleshooting

### Si ves muchos errores 404:
1. Verifica que las URLs sean correctas
2. Revisa la documentación de la API
3. Usa `/debug/routes` para ver endpoints disponibles

### Si ves errores de validación:
1. Verifica el formato de los datos enviados
2. Revisa los mensajes de error específicos
3. Consulta la documentación de validación

### Si ves errores inesperados:
1. Revisa los logs completos
2. Verifica la configuración
3. Contacta al equipo de desarrollo

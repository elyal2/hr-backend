.PHONY: help up down restart logs logs-db logs-api ps clean shell-db shell-api build test lint format check-env health openapi-gen

# Colores para output
GREEN  := \033[0;32m
YELLOW := \033[1;33m
RED    := \033[0;31m
NC     := \033[0m # No Color

help: ## Muestra esta ayuda
	@echo "$(GREEN)HR Backend - Comandos disponibles:$(NC)"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(YELLOW)%-15s$(NC) %s\n", $$1, $$2}'
	@echo ""
	@echo "$(YELLOW)URLs:$(NC)"
	@echo "  - API:        http://localhost:8080"
	@echo "  - Health:     http://localhost:8080/q/health"
	@echo "  - Swagger UI: http://localhost:8080/q/swagger-ui"
	@echo "  - PostgreSQL: localhost:5432"

check-env: ## Verifica que el archivo .env esté configurado
	@if [ ! -f .env ]; then \
		echo "$(RED)❌ Error: No se encuentra el archivo .env$(NC)"; \
		echo "$(YELLOW)Copia .env.example a .env y configura AUTH0_CLIENT_SECRET:$(NC)"; \
		echo "  cp .env.example .env"; \
		exit 1; \
	fi
	@if grep -q "YOUR_CLIENT_SECRET_HERE" .env; then \
		echo "$(RED)❌ Error: AUTH0_CLIENT_SECRET no está configurado$(NC)"; \
		echo "$(YELLOW)Edita .env y reemplaza AUTH0_CLIENT_SECRET con el valor real de Auth0$(NC)"; \
		exit 1; \
	fi
	@echo "$(GREEN)✓ Archivo .env configurado correctamente$(NC)"

up: check-env ## Inicia los contenedores (build + start)
	@echo "$(GREEN)🚀 Iniciando servicios...$(NC)"
	docker-compose up --build -d
	@echo ""
	@echo "$(GREEN)✓ Servicios iniciados$(NC)"
	@$(MAKE) --no-print-directory health

down: ## Detiene y elimina los contenedores
	@echo "$(YELLOW)⏹️  Deteniendo servicios...$(NC)"
	docker-compose down
	@echo "$(GREEN)✓ Servicios detenidos$(NC)"

restart: ## Reinicia los contenedores
	@echo "$(YELLOW)🔄 Reiniciando servicios...$(NC)"
	docker-compose restart
	@echo "$(GREEN)✓ Servicios reiniciados$(NC)"

logs: ## Muestra logs en tiempo real (Ctrl+C para salir)
	@echo "$(GREEN)📋 Mostrando logs (Ctrl+C para salir)...$(NC)"
	docker-compose logs -f

logs-db: ## Muestra logs de PostgreSQL
	@echo "$(GREEN)📋 Logs de PostgreSQL (Ctrl+C para salir)...$(NC)"
	docker-compose logs -f postgres

logs-api: ## Muestra logs del backend
	@echo "$(GREEN)📋 Logs del backend (Ctrl+C para salir)...$(NC)"
	docker-compose logs -f backend

ps: ## Lista contenedores en ejecución
	@echo "$(GREEN)📊 Contenedores en ejecución:$(NC)"
	@docker-compose ps

clean: ## Detiene contenedores y elimina volúmenes (¡borra la BD!)
	@echo "$(RED)⚠️  ATENCIÓN: Esto eliminará todos los datos de la base de datos$(NC)"
	@read -p "¿Estás seguro? (escribe 'yes' para confirmar): " confirm; \
	if [ "$$confirm" = "yes" ]; then \
		echo "$(YELLOW)🧹 Limpiando contenedores y volúmenes...$(NC)"; \
		docker-compose down -v; \
		echo "$(GREEN)✓ Limpieza completada$(NC)"; \
	else \
		echo "Operación cancelada"; \
	fi

shell-db: ## Abre shell en PostgreSQL (psql)
	@echo "$(GREEN)🐘 Abriendo shell en PostgreSQL...$(NC)"
	docker-compose exec postgres psql -U postgres -d humanrsc

shell-api: ## Abre shell en el contenedor del backend
	@echo "$(GREEN)☕ Abriendo shell en el backend...$(NC)"
	docker-compose exec backend sh

build: ## Construye la imagen Docker sin iniciar
	@echo "$(GREEN)🔨 Construyendo imagen Docker...$(NC)"
	docker-compose build
	@echo "$(GREEN)✓ Imagen construida$(NC)"

health: ## Verifica el estado de salud de los servicios
	@echo ""
	@echo "$(YELLOW)Esperando a que los servicios estén listos...$(NC)"
	@sleep 5
	@echo ""
	@echo "$(GREEN)Estado de los servicios:$(NC)"
	@echo -n "  PostgreSQL: "
	@docker-compose exec -T postgres pg_isready -U postgres -d humanrsc > /dev/null 2>&1 && echo "$(GREEN)✓ OK$(NC)" || echo "$(RED)✗ Error$(NC)"
	@echo -n "  Backend:    "
	@curl -sf http://localhost:8080/q/health/live > /dev/null 2>&1 && echo "$(GREEN)✓ OK$(NC)" || echo "$(YELLOW)⏳ Iniciando...$(NC)"
	@echo ""
	@echo "$(YELLOW)Si el backend no está listo, espera ~30-60 segundos y ejecuta:$(NC)"
	@echo "  make health"

test: ## Ejecuta tests (Maven)
	@echo "$(GREEN)🧪 Ejecutando tests...$(NC)"
	./mvnw test

lint: ## Verifica estilo de código (Checkstyle)
	@echo "$(GREEN)🔍 Verificando estilo de código...$(NC)"
	./mvnw checkstyle:check

format: ## Formatea el código (google-java-format)
	@echo "$(GREEN)✨ Formateando código...$(NC)"
	./mvnw com.coveo:fmt-maven-plugin:format

package: ## Construye el JAR (sin tests)
	@echo "$(GREEN)📦 Construyendo JAR...$(NC)"
	./mvnw package -DskipTests
	@echo "$(GREEN)✓ JAR construido en target/quarkus-app/$(NC)"

dev: ## Ejecuta en modo desarrollo (hot reload, sin Docker)
	@echo "$(GREEN)🔥 Iniciando en modo desarrollo...$(NC)"
	@echo "$(YELLOW)Asegúrate de que PostgreSQL esté corriendo en localhost:5432$(NC)"
	./mvnw quarkus:dev

db-migrate: ## Ejecuta migraciones de Flyway manualmente
	@echo "$(GREEN)🗄️  Ejecutando migraciones de base de datos...$(NC)"
	docker-compose exec backend sh -c "curl -X POST http://localhost:8080/q/flyway/migrate"

db-info: ## Muestra información de las migraciones
	@echo "$(GREEN)📊 Información de migraciones:$(NC)"
	docker-compose exec postgres psql -U postgres -d humanrsc -c "SELECT * FROM hr_app.flyway_schema_history ORDER BY installed_rank;"

db-reset: clean up ## Resetea la base de datos (borra todo y reinicia)
	@echo "$(GREEN)✓ Base de datos reseteada$(NC)"

openapi-gen: ## Genera openapi.yaml desde el spec de Quarkus
	@echo "$(GREEN)📝 Generando OpenAPI spec desde Quarkus...$(NC)"
	@curl -sf http://localhost:8080/q/openapi -o openapi.yaml && echo "$(GREEN)✓ openapi.yaml actualizado$(NC)" || echo "$(RED)✗ Error: Asegúrate de que el backend esté corriendo (make up)$(NC)"

# Alias útiles
start: up ## Alias para 'up'
stop: down ## Alias para 'down'
status: ps ## Alias para 'ps'

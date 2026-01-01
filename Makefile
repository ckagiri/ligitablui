.PHONY: local-start local-stop local-logs local-restart local-clean local-test

local-start: ## Start local demo
	docker-compose -f docker-compose.local.yml up -d --build
	@echo "🚀 Demo starting at http://localhost:8080"
	@echo "📊 Run 'make local-logs' to watch logs"

local-stop: ## Stop local demo
	docker-compose -f docker-compose.local.yml down
	@echo "✅ Demo stopped"

local-logs: ## Watch logs
	docker-compose -f docker-compose.local.yml logs -f

local-restart: ## Restart local demo
	docker-compose -f docker-compose.local.yml restart
	@echo "🔄 Demo restarted"

local-clean: ## Stop and remove everything
	docker-compose -f docker-compose.local.yml down --rmi all --volumes
	@echo "🧹 Cleaned up"

local-test: ## Run quick health check
	@echo "Testing http://localhost:8080..."
	@curl -f http://localhost:8080 > /dev/null 2>&1 && echo "✅ Demo is healthy!" || echo "❌ Demo is not responding"

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'
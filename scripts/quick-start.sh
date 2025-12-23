#!/bin/bash
# Quick Start Script - Workflow Service

set -e

echo "╔════════════════════════════════════════════════════╗"
echo "║   🚀 Workflow Service - Quick Start               ║"
echo "╚════════════════════════════════════════════════════╝"
echo ""

# Check prerequisites
echo "📋 Verificando pré-requisitos..."

command -v docker &> /dev/null || { echo "❌ Docker não instalado. Instale em https://docker.com"; exit 1; }
command -v java &> /dev/null || { echo "❌ Java não instalado. Instale Java 25+"; exit 1; }
command -v mvn &> /dev/null || { echo "❌ Maven não instalado"; exit 1; }

echo "✅ Docker instalado"
echo "✅ Java instalado: $(java -version 2>&1 | head -1)"
echo "✅ Maven instalado"
echo ""

# Create .env if not exists
if [ ! -f ".env" ]; then
    echo "📝 Criando arquivo .env..."
    cp .env.example .env
    echo "✅ Arquivo .env criado (copie do .env.example)"
fi

echo ""
echo "🐳 Iniciando Docker Compose (PostgreSQL + PgAdmin)..."
docker-compose up -d

echo "⏳ Aguardando PostgreSQL iniciar..."
sleep 10

# Check if database is ready
max_attempts=30
attempts=0
until docker exec workflow-postgres pg_isready -U postgres > /dev/null 2>&1 || [ $attempts -eq $max_attempts ]; do
    attempts=$((attempts+1))
    echo "  Tentativa $attempts/$max_attempts..."
    sleep 2
done

if [ $attempts -eq $max_attempts ]; then
    echo "❌ PostgreSQL não respondeu após 60 segundos"
    docker-compose logs postgres
    exit 1
fi

echo "✅ PostgreSQL está pronto"
echo ""

# Compile project
echo "🔨 Compilando projeto..."
./mvnw clean compile -q

echo "✅ Projeto compilado com sucesso"
echo ""

# Show next steps
echo "╔════════════════════════════════════════════════════╗"
echo "║          ✅ Configuração Completa!                ║"
echo "╚════════════════════════════════════════════════════╝"
echo ""
echo "📦 Serviços em execução:"
echo "  • PostgreSQL:  localhost:5432"
echo "  • PgAdmin:     http://localhost:5050"
echo "     Email: admin@workflow.local"
echo "     Senha: admin123"
echo ""
echo "🚀 Próximos passos:"
echo "  1. Execute em desenvolvimento:"
echo "     ./mvnw quarkus:dev"
echo ""
echo "  2. API será acessível em:"
echo "     http://localhost:8080"
echo ""
echo "  3. Health check:"
echo "     curl http://localhost:8080/q/health/live"
echo ""
echo "  4. Logs de desenvolvimento:"
echo "     tail -f logs/dev.log"
echo ""
echo "📚 Documentação:"
echo "  • README.md - Visão geral"
echo "  • CONFIGURATION_CHECKLIST.md - Checklist completo"
echo "  • STRUCTURE_COMPLETE.md - Estrutura do projeto"
echo ""
echo "🛑 Para parar os serviços:"
echo "   docker-compose down"
echo ""

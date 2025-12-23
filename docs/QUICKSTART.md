# 🚀 Guia Rápido - Começar a Desenvolver em 5 Minutos

## Pré-requisitos

- Docker instalado
- Java 25+ instalado
- Maven 3.8+ instalado

---

## 1️⃣ **Preparar Ambiente**

```bash
# Entre no diretório do projeto
cd /home/joaopedro/workflow-engine

# Copie o arquivo de variáveis de ambiente
cp .env.example .env

# Visualize o arquivo (opcional para confirmar)
cat .env
```

---

## 2️⃣ **Iniciar Infraestrutura (PostgreSQL + PgAdmin)**

```bash
# Inicie os serviços com Docker Compose
docker-compose up -d

# Aguarde cerca de 30 segundos para o PostgreSQL estar pronto

# Verifique se PostgreSQL está saudável
docker exec workflow-postgres pg_isready -U postgres
# Esperado: "accepting connections"
```

---

## 3️⃣ **Compilar e Rodar em Desenvolvimento**

```bash
# Compile o projeto
./mvnw clean compile

# Inicie em modo desenvolvimento (com hot-reload)
./mvnw quarkus:dev

# A saída deve incluir:
# [io.quarkus] Quarkus ... started in ...
# [io.quarkus] Listen on: http://0.0.0.0:8080
```

---

## 4️⃣ **Testar a API**

Em outro terminal:

```bash
# Health check (liveness probe)
curl http://localhost:8080/q/health/live

# Esperado:
# {"status":"UP"}

# Health check (readiness probe)
curl http://localhost:8080/q/health/ready
```

---

## 5️⃣ **Acessar Interfaces**

| Serviço | URL | Credenciais |
|---------|-----|-------------|
| **API REST** | http://localhost:8080 | - |
| **PgAdmin** | http://localhost:5050 | admin@workflow.local / admin123 |
| **Health** | http://localhost:8080/q/health/live | - |
| **Metrics** | http://localhost:8080/q/metrics | - |

---

## 🧪 **Testar Endpoints**

### Exemplo 1: Listar Pipelines

```bash
curl -X GET http://localhost:8080/api/pipelines/1 \
  -H "Authorization: Bearer seu-jwt-token" \
  -H "Content-Type: application/json"
```

### Exemplo 2: Mover Card

```bash
curl -X POST http://localhost:8080/api/pipelines/1/cards/1/move \
  -H "Authorization: Bearer seu-jwt-token" \
  -H "Content-Type: application/json" \
  -d '{
    "fromColumnId": "1",
    "toColumnId": "2"
  }'
```

---

## 📂 **Estrutura de Arquivos Importantes**

```
workflow-engine/
├── src/main/java/com/apporte/     # Código fonte
├── src/main/resources/
│   ├── application.properties          # Config base
│   ├── application-dev.properties      # Config desenvolvimento
│   ├── application-prod.properties     # Config produção
│   ├── schema.sql                      # Schema do banco
│   └── import.sql                      # Dados iniciais
│
├── docker-compose.yml                  # Infraestrutura (Docker)
├── .env.example                        # Variáveis (template)
├── .env                                # Variáveis (cria do example)
├── pom.xml                             # Dependências Maven
└── README.md                           # Este arquivo
```

---

## 🛑 **Comandos Úteis**

### Parar o Servidor
```bash
# Pressione Ctrl+C no terminal onde mvn quarkus:dev está rodando
# Ou em outro terminal:
pkill -f "quarkus:dev"
```

### Parar Infraestrutura
```bash
# Parar containers
docker-compose down

# Parar e remover volumes (CUIDADO - apaga dados!)
docker-compose down -v
```

### Ver Logs em Tempo Real
```bash
# Logs da aplicação Quarkus (vendo output do console)
# Já visível no terminal onde mvnw quarkus:dev está rodando

# Logs do PostgreSQL
docker-compose logs -f postgres

# Logs do PgAdmin
docker-compose logs -f pgadmin
```

### Acessar Banco de Dados
```bash
# Conectar ao PostgreSQL via psql
docker exec -it workflow-postgres psql -U postgres -d workflow_db

# Ver tabelas
\dt

# Executar query
SELECT COUNT(*) FROM pipeline_cards;

# Sair
\q
```

---

## 🐛 **Troubleshooting**

### ❌ Porta 5432 já está em uso
```bash
# Verifique se há container anterior
docker ps -a | grep postgres

# Se houver, remova
docker rm workflow-postgres

# Ou mude a porta no docker-compose.yml de 5432:5432 para 5433:5432
```

### ❌ Docker não consegue criar volume
```bash
# Verifique permissões
sudo chown -R $USER:$USER /home/joaopedro/workflow-engine

# Ou rode com sudo (não recomendado)
sudo docker-compose up -d
```

### ❌ Quarkus não conecta ao banco
```bash
# Verifique se PostgreSQL está pronto
docker exec workflow-postgres pg_isready -U postgres

# Se não estiver pronto, aguarde mais tempo (até 60 segundos)
sleep 30

# Verifique .env possui as credenciais corretas
cat .env | grep DB_
```

### ❌ Hot-reload não está funcionando
```bash
# Quarkus dev mode detecta mudanças de arquivo
# Se não funcionar, reinicie manualmente:
# 1. Pressione 'r' no terminal do quarkus:dev
# ou
# 2. Ctrl+C para parar e ./mvnw quarkus:dev para reiniciar
```

---

## 📚 **Documentação Completa**

- [CONFIGURATION_CHECKLIST.md](./CONFIGURATION_CHECKLIST.md) - Configuração detalhada
- [STRUCTURE_COMPLETE.md](./STRUCTURE_COMPLETE.md) - Estrutura do projeto
- [IMPLEMENTATION_COMPLETE.md](./IMPLEMENTATION_COMPLETE.md) - Implementação
- [README.md](./README.md) - Visão geral

---

## ✅ **Checklist de Sucesso**

- [ ] .env foi criado do .env.example
- [ ] `docker-compose up -d` executado sem erros
- [ ] PostgreSQL respondendo: `docker exec workflow-postgres pg_isready -U postgres`
- [ ] `./mvnw quarkus:dev` rodando sem erros
- [ ] Health check retorna 200: `curl http://localhost:8080/q/health/live`
- [ ] Consegue acessar PgAdmin em http://localhost:5050
- [ ] Testes passando: `./mvnw test`

---

## 🎯 **Próximos Passos**

1. **Explorar endpoints** - Use Postman ou curl para testar
2. **Modificar código** - Quarkus recarrega automaticamente
3. **Ver logs** - Acompanhe no console do `quarkus:dev`
4. **Criar dados de teste** - Insira dados via SQL ou API
5. **Implementar novas features** - Crie novos endpoints

---

## 💡 **Dica Final**

A primeira vez que você acessa um endpoint, Quarkus compila as classes necessárias. Isso pode levar alguns segundos. Não se preocupe - é normal!

```bash
# Primeira requisição é mais lenta
curl http://localhost:8080/q/health/live
# Próximas requisições são imediatas ⚡
```

---

**Dúvidas? Consulte [CONFIGURATION_CHECKLIST.md](./CONFIGURATION_CHECKLIST.md)**

Happy coding! 🚀

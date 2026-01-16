#!/bin/bash

# Script de testes do Keycloak - Apporte 2.0
# Testa autenticação e autorização end-to-end

set -e

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuração
KEYCLOAK_URL="https://auth.apporte.work"
REALM="development"
CLIENT_ID="apporte-frontend-dev"
API_URL="${API_URL:-http://localhost:8081}"

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}  Teste de Integração Keycloak${NC}"
echo -e "${YELLOW}  Apporte 2.0${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""

# Função para testar se serviço está disponível
check_service() {
    local url=$1
    local name=$2
    
    echo -n "Verificando $name... "
    if curl -s -f -o /dev/null "$url"; then
        echo -e "${GREEN}✓ OK${NC}"
        return 0
    else
        echo -e "${RED}✗ FALHOU${NC}"
        return 1
    fi
}

# Função para obter token
get_token() {
    local username=$1
    local password=$2
    
    echo ""
    echo -e "${YELLOW}Obtendo token para $username...${NC}"
    
    local response=$(curl -s -X POST \
        "${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "client_id=${CLIENT_ID}" \
        -d "username=${username}" \
        -d "password=${password}" \
        -d "grant_type=password")
    
    local token=$(echo "$response" | jq -r '.access_token')
    
    if [ "$token" == "null" ] || [ -z "$token" ]; then
        echo -e "${RED}✗ Falha ao obter token${NC}"
        echo "Response: $response"
        return 1
    fi
    
    echo -e "${GREEN}✓ Token obtido com sucesso${NC}"
    echo "$token"
}

# Função para decodificar token
decode_token() {
    local token=$1
    
    echo ""
    echo -e "${YELLOW}Decodificando token...${NC}"
    
    # Decodificar payload do JWT
    local payload=$(echo "$token" | cut -d'.' -f2)
    # Adicionar padding se necessário
    local padding=$((4 - ${#payload} % 4))
    [ $padding -ne 4 ] && payload="${payload}$(printf '%*s' $padding | tr ' ' '=')"
    
    echo "$payload" | base64 -d 2>/dev/null | jq '.'
}

# Função para testar endpoint
test_endpoint() {
    local method=$1
    local endpoint=$2
    local token=$3
    local expected_status=$4
    local description=$5
    
    echo ""
    echo -e "${YELLOW}Testando: $description${NC}"
    echo "  $method $endpoint"
    
    if [ -z "$token" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "$method" "${API_URL}${endpoint}")
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" \
            -H "Authorization: Bearer $token" \
            "${API_URL}${endpoint}")
    fi
    
    status_code=$(echo "$response" | tail -n 1)
    body=$(echo "$response" | head -n -1)
    
    if [ "$status_code" == "$expected_status" ]; then
        echo -e "  ${GREEN}✓ Status: $status_code (esperado: $expected_status)${NC}"
        if [ ! -z "$body" ] && [ "$body" != "" ]; then
            echo "  Response:"
            echo "$body" | jq '.' 2>/dev/null || echo "$body"
        fi
        return 0
    else
        echo -e "  ${RED}✗ Status: $status_code (esperado: $expected_status)${NC}"
        echo "  Response: $body"
        return 1
    fi
}

# Iniciar testes
echo ""
echo -e "${YELLOW}=== Fase 1: Verificação de Serviços ===${NC}"
echo ""

check_service "${KEYCLOAK_URL}/realms/${REALM}/.well-known/openid-configuration" "Keycloak"
check_service "${API_URL}/q/health/ready" "Backend API"

# Testar OIDC configuration
echo ""
echo -e "${YELLOW}=== Fase 2: Configuração OIDC ===${NC}"
echo ""
echo "Verificando configuração OIDC do realm $REALM..."
curl -s "${KEYCLOAK_URL}/realms/${REALM}/.well-known/openid-configuration" | jq '{
    issuer,
    authorization_endpoint,
    token_endpoint,
    userinfo_endpoint,
    jwks_uri
}'

# Testar autenticação
echo ""
echo -e "${YELLOW}=== Fase 3: Autenticação ===${NC}"
echo ""

# Teste 1: Endpoint público (sem token)
test_endpoint "GET" "/api/auth/public" "" "200" "Endpoint público sem autenticação"

# Teste 2: Endpoint protegido sem token (deve falhar)
test_endpoint "GET" "/api/auth/me" "" "401" "Endpoint protegido sem token"

# Teste 3: Login como proponente
PROPONENTE_TOKEN=$(get_token "proponente@exemplo.com" "Proponente@123")

if [ ! -z "$PROPONENTE_TOKEN" ]; then
    decode_token "$PROPONENTE_TOKEN"
    
    echo ""
    echo -e "${YELLOW}=== Fase 4: Autorização (Proponente) ===${NC}"
    
    test_endpoint "GET" "/api/auth/me" "$PROPONENTE_TOKEN" "200" "Obter informações do usuário"
    test_endpoint "GET" "/api/auth/proponente" "$PROPONENTE_TOKEN" "200" "Endpoint de proponente"
    test_endpoint "GET" "/api/auth/admin" "$PROPONENTE_TOKEN" "403" "Endpoint admin (deve negar)"
    test_endpoint "GET" "/api/auth/org-check" "$PROPONENTE_TOKEN" "200" "Verificar organização"
fi

# Teste 4: Login como parecerista
echo ""
PARECERISTA_TOKEN=$(get_token "parecerista@exemplo.com" "Parecerista@123")

if [ ! -z "$PARECERISTA_TOKEN" ]; then
    echo ""
    echo -e "${YELLOW}=== Fase 5: Autorização (Parecerista) ===${NC}"
    
    test_endpoint "GET" "/api/auth/parecerista" "$PARECERISTA_TOKEN" "200" "Endpoint de parecerista"
    test_endpoint "GET" "/api/auth/proponente" "$PARECERISTA_TOKEN" "403" "Endpoint de proponente (deve negar)"
fi

# Teste 5: Login como admin
echo ""
ADMIN_TOKEN=$(get_token "admin@apporte.dev" "Admin@123")

if [ ! -z "$ADMIN_TOKEN" ]; then
    decode_token "$ADMIN_TOKEN"
    
    echo ""
    echo -e "${YELLOW}=== Fase 6: Autorização (Admin) ===${NC}"
    
    test_endpoint "GET" "/api/auth/admin" "$ADMIN_TOKEN" "200" "Endpoint admin"
    test_endpoint "GET" "/api/auth/proponente" "$ADMIN_TOKEN" "200" "Endpoint proponente (admin herda)"
    test_endpoint "GET" "/api/auth/parecerista" "$ADMIN_TOKEN" "200" "Endpoint parecerista (admin herda)"
fi

# Teste 6: Token expirado/inválido
echo ""
echo -e "${YELLOW}=== Fase 7: Segurança ===${NC}"
test_endpoint "GET" "/api/auth/me" "invalid-token-12345" "401" "Token inválido"

# Resumo
echo ""
echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}  Testes Concluídos!${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""
echo "Para ver logs do backend:"
echo "  tail -f logs/application.log"
echo ""
echo "Para testar manualmente:"
echo "  export TOKEN=\$(curl -s -X POST '${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token' \\"
echo "    -d 'client_id=${CLIENT_ID}' \\"
echo "    -d 'username=proponente@exemplo.com' \\"
echo "    -d 'password=Proponente@123' \\"
echo "    -d 'grant_type=password' | jq -r '.access_token')"
echo ""
echo "  curl -H \"Authorization: Bearer \$TOKEN\" ${API_URL}/api/auth/me | jq"
echo ""

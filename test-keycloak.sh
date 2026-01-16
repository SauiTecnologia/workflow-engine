#!/bin/bash
# Script de teste para autenticação Keycloak

set -e

echo "🔐 Testando Autenticação Keycloak..."
echo ""

# Cores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuração
KEYCLOAK_URL="https://auth.apporte.work"
REALM="development"
CLIENT_ID="workflow-engine-dev"
CLIENT_SECRET="E6Vy7He2wemRyUDdfXDfyNsOAIwNk43u"
USERNAME="admin@example.com"
PASSWORD="senha123"
API_URL="http://localhost:8080"

echo "📍 Configuração:"
echo "   Keycloak: $KEYCLOAK_URL"
echo "   Realm: $REALM"
echo "   Client ID: $CLIENT_ID"
echo "   Username: $USERNAME"
echo ""

# 1. Obter Token
echo "1️⃣  Obtendo token do Keycloak..."
TOKEN_RESPONSE=$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=$CLIENT_ID" \
  -d "client_secret=$CLIENT_SECRET" \
  -d "grant_type=password" \
  -d "username=$USERNAME" \
  -d "password=$PASSWORD")

# Verificar se houve erro
if echo "$TOKEN_RESPONSE" | grep -q "error"; then
    echo -e "${RED}❌ Erro ao obter token:${NC}"
    echo "$TOKEN_RESPONSE" | jq '.'
    exit 1
fi

# Extrair access_token
ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.access_token')

if [ "$ACCESS_TOKEN" == "null" ] || [ -z "$ACCESS_TOKEN" ]; then
    echo -e "${RED}❌ Token não encontrado na resposta${NC}"
    echo "$TOKEN_RESPONSE" | jq '.'
    exit 1
fi

echo -e "${GREEN}✅ Token obtido com sucesso!${NC}"
echo "   Token (primeiros 50 chars): ${ACCESS_TOKEN:0:50}..."
echo ""

# 2. Decodificar Token (payload)
echo "2️⃣  Informações do Token:"
PAYLOAD=$(echo "$ACCESS_TOKEN" | cut -d'.' -f2)
# Adicionar padding se necessário
PAYLOAD_PADDED=$(echo "$PAYLOAD" | awk '{while(length%4!=0)$0=$0"="}1')
echo "$PAYLOAD_PADDED" | base64 -d 2>/dev/null | jq '.' || echo "   (não foi possível decodificar)"
echo ""

# 3. Testar endpoint /api/auth/me
echo "3️⃣  Testando endpoint /api/auth/me..."
ME_RESPONSE=$(curl -s -X GET "$API_URL/api/auth/me" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json")

if echo "$ME_RESPONSE" | grep -q "error\|unauthorized"; then
    echo -e "${RED}❌ Erro ao acessar /api/auth/me:${NC}"
    echo "$ME_RESPONSE"
else
    echo -e "${GREEN}✅ Endpoint /api/auth/me acessado com sucesso!${NC}"
    echo "$ME_RESPONSE" | jq '.'
fi
echo ""

# 4. Testar endpoint de pipelines (exemplo)
echo "4️⃣  Testando endpoint /api/pipelines..."
PIPELINES_RESPONSE=$(curl -s -X GET "$API_URL/api/pipelines" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json")

if echo "$PIPELINES_RESPONSE" | grep -q "error\|unauthorized"; then
    echo -e "${YELLOW}⚠️  Endpoint pode não estar implementado ou erro:${NC}"
    echo "$PIPELINES_RESPONSE"
else
    echo -e "${GREEN}✅ Endpoint /api/pipelines acessado!${NC}"
    echo "$PIPELINES_RESPONSE" | jq '.'
fi
echo ""

# 5. Salvar token em arquivo para uso manual
echo "$ACCESS_TOKEN" > .token
echo -e "${GREEN}✅ Token salvo em .token${NC}"
echo ""

echo "🎉 Testes concluídos!"
echo ""
echo "💡 Para usar o token manualmente:"
echo "   export TOKEN=\"$ACCESS_TOKEN\""
echo "   curl -H \"Authorization: Bearer \$TOKEN\" $API_URL/api/auth/me"

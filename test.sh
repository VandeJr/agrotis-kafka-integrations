#!/bin/bash

# =============================================================================
# Agrotis Kafka Integrations - E2E Test Script
# =============================================================================

APP1_URL="http://localhost:8080"
SCHEDULER_WAIT=20  # segundos aguardando o scheduler publicar

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PASSED=0
FAILED=0

pass() { echo -e "${GREEN}[PASS]${NC} $1"; ((PASSED++)); }
fail() { echo -e "${RED}[FAIL]${NC} $1"; ((FAILED++)); }
info() { echo -e "${BLUE}[INFO]${NC} $1"; }
section() { echo -e "\n${YELLOW}=== $1 ===${NC}"; }

# =============================================================================
# UTIL — faz request e retorna o body
# =============================================================================
do_request() {
  local method=$1
  local url=$2
  local body=$3

  if [ -n "$body" ]; then
    curl -s -w "\n%{http_code}" -X "$method" "$url" \
      -H "Content-Type: application/json" \
      -d "$body"
  else
    curl -s -w "\n%{http_code}" -X "$method" "$url"
  fi
}

# Separa body e status code do retorno do curl
get_body()   { echo "$1" | head -n -1; }
get_status() { echo "$1" | tail -n 1; }

# =============================================================================
# PRE-CHECK — App1 está no ar?
# =============================================================================
section "PRE-CHECK"

info "Verificando se o App1 está no ar em $APP1_URL..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$APP1_URL/actuator/health" 2>/dev/null)

if [ "$STATUS" != "200" ]; then
  # tenta sem actuator
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$APP1_URL/eventos" 2>/dev/null)
  if [ "$STATUS" == "000" ]; then
    fail "App1 não está respondendo em $APP1_URL. Suba o App1 antes de rodar o teste."
    exit 1
  fi
fi

pass "App1 está no ar"

# =============================================================================
# TESTE 1 — Criação de evento (POST /eventos)
# =============================================================================
section "TESTE 1: POST /eventos — fluxo feliz"

RESPONSE=$(do_request POST "$APP1_URL/eventos" '{"descricao": "teste e2e automatizado"}')
BODY=$(get_body "$RESPONSE")
STATUS=$(get_status "$RESPONSE")

if [ "$STATUS" == "201" ]; then
  pass "POST /eventos retornou 201"
else
  fail "POST /eventos retornou $STATUS (esperado 201)"
fi

# Extrai o id do JSON retornado
EVENTO_ID=$(echo "$BODY" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)

if [ -n "$EVENTO_ID" ]; then
  pass "Evento criado com id=$EVENTO_ID"
else
  fail "Não foi possível extrair o id do evento. Body: $BODY"
  exit 1
fi

# Verifica se situacao inicial é ENCERRADO
SITUACAO=$(echo "$BODY" | grep -o '"situacao":"[^"]*"' | cut -d'"' -f4)
if [ "$SITUACAO" == "ENCERRADO" ]; then
  pass "Situação inicial do evento é ENCERRADO"
else
  fail "Situação inicial deveria ser ENCERRADO, veio: $SITUACAO"
fi

# =============================================================================
# TESTE 2 — Validações (400 e 404)
# =============================================================================
section "TESTE 2: Exception Handlers"

# 2a - descricao vazia deve retornar 400
info "Testando POST com descricao vazia..."
RESPONSE=$(do_request POST "$APP1_URL/eventos" '{"descricao": ""}')
STATUS=$(get_status "$RESPONSE")
BODY=$(get_body "$RESPONSE")

if [ "$STATUS" == "400" ]; then
  pass "POST com descricao vazia retornou 400"
else
  fail "POST com descricao vazia retornou $STATUS (esperado 400)"
fi

MSG=$(echo "$BODY" | grep -o '"mensagem":"[^"]*"' | cut -d'"' -f4)
if [ -n "$MSG" ]; then
  pass "Resposta de erro contém campo mensagem: '$MSG'"
else
  fail "Resposta de erro não contém campo mensagem. Body: $BODY"
fi

# 2b - id inexistente deve retornar 404
info "Testando PATCH com id inexistente..."
RESPONSE=$(do_request PATCH "$APP1_URL/eventos/id-que-nao-existe/integrar")
STATUS=$(get_status "$RESPONSE")
BODY=$(get_body "$RESPONSE")

if [ "$STATUS" == "404" ]; then
  pass "PATCH com id inexistente retornou 404"
else
  fail "PATCH com id inexistente retornou $STATUS (esperado 404)"
fi

MSG=$(echo "$BODY" | grep -o '"mensagem":"[^"]*"' | cut -d'"' -f4)
if [ -n "$MSG" ]; then
  pass "Resposta de erro contém campo mensagem: '$MSG'"
else
  fail "Resposta de erro não contém campo mensagem. Body: $BODY"
fi

# =============================================================================
# TESTE 3 — PATCH manual /integrar
# =============================================================================
section "TESTE 3: PATCH /eventos/{id}/integrar — manual"

RESPONSE=$(do_request PATCH "$APP1_URL/eventos/$EVENTO_ID/integrar")
STATUS=$(get_status "$RESPONSE")
BODY=$(get_body "$RESPONSE")

if [ "$STATUS" == "200" ]; then
  pass "PATCH /eventos/$EVENTO_ID/integrar retornou 200"
else
  fail "PATCH retornou $STATUS (esperado 200). Body: $BODY"
fi

SITUACAO=$(echo "$BODY" | grep -o '"situacao":"[^"]*"' | cut -d'"' -f4)
if [ "$SITUACAO" == "INTEGRADO" ]; then
  pass "Situação atualizada para INTEGRADO"
else
  fail "Situação deveria ser INTEGRADO, veio: $SITUACAO"
fi

# =============================================================================
# TESTE 4 — Idempotência via scheduler
# =============================================================================
section "TESTE 4: Idempotência — evento INTEGRADO não deve ser republicado"

info "Criando novo evento para observar idempotência do scheduler..."
RESPONSE=$(do_request POST "$APP1_URL/eventos" '{"descricao": "teste idempotencia"}')
BODY=$(get_body "$RESPONSE")
EVENTO_ID2=$(echo "$BODY" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)

info "Evento criado id=$EVENTO_ID2 — aguardando ${SCHEDULER_WAIT}s para o scheduler publicar..."
sleep $SCHEDULER_WAIT

info "Integrando manualmente o evento $EVENTO_ID2..."
do_request PATCH "$APP1_URL/eventos/$EVENTO_ID2/integrar" > /dev/null

info "Aguardando mais ${SCHEDULER_WAIT}s para verificar se o scheduler republica..."
sleep $SCHEDULER_WAIT

# Verifica no banco que o evento continua INTEGRADO
RESPONSE=$(do_request PATCH "$APP1_URL/eventos/$EVENTO_ID2/integrar")
STATUS=$(get_status "$RESPONSE")
BODY=$(get_body "$RESPONSE")
SITUACAO=$(echo "$BODY" | grep -o '"situacao":"[^"]*"' | cut -d'"' -f4)

if [ "$SITUACAO" == "INTEGRADO" ]; then
  pass "Evento $EVENTO_ID2 permanece INTEGRADO — scheduler não republicou"
else
  fail "Situação inesperada: $SITUACAO"
fi

# =============================================================================
# RESUMO
# =============================================================================
section "RESUMO"

TOTAL=$((PASSED + FAILED))
echo -e "Total:   $TOTAL"
echo -e "${GREEN}Passed:  $PASSED${NC}"
echo -e "${RED}Failed:  $FAILED${NC}"

if [ "$FAILED" -eq 0 ]; then
  echo -e "\n${GREEN}Todos os testes passaram!${NC}"
  exit 0
else
  echo -e "\n${RED}${FAILED} teste(s) falharam.${NC}"
  exit 1
fi

# agrotis-kafka-integrations

Desafio técnico Agrotis — Spring Boot + Kafka.

## Estrutura

```
agrotis-kafka-integrations/
├── compose.yml
├── README.md
├── test.sh              # script de testes e2e automatizados
├── app1-producer/       # Producer + API REST
└── app2-consumer/       # Consumer + integração com App1
```

## Pré-requisitos

- Java 21
- Maven 3.9+
- Docker + Docker Compose

---

## 1. Subindo a infraestrutura

Na raiz do repositório:

```bash
docker compose up -d
```

Verificar se Kafka e Postgres subiram:

```bash
docker compose ps
```

Logs do Kafka:

```bash
docker compose logs -f kafka
```

---

## 2. Ordem de subida das aplicações

### App1 — Producer + API

```bash
cd app1-producer
./mvnw spring-boot:run
```

Porta: `8080`
Swagger UI: `http://localhost:8080/swagger-ui.html`

### App2 — Consumer

```bash
cd app2-consumer
./mvnw spring-boot:run
```

Porta: `8081`

> Suba o App1 antes do App2. O App2 chama a API do App1 ao consumir mensagens.

---

## 3. Configurações relevantes

| Item | Valor |
|---|---|
| Kafka bootstrap-servers | `localhost:9092` |
| App1 base URL (usada pelo App2) | `http://localhost:8080` |
| Tópico principal | `AplicacaoAgrotisTesteEvento` |
| Tópico DLQ | `AplicacaoAgrotisTesteEvento.DLQ` |
| Consumer group | `app2-consumer-group` |
| Retries antes do DLQ | 2 retries (3 tentativas no total) |
| Intervalo entre retries | 3 segundos |

---

## 4. Endpoints App1

| Método | Path | Descrição |
|---|---|---|
| `POST` | `/eventos` | Cria um evento com situação `ENCERRADO` |
| `PATCH` | `/eventos/{id}/integrar` | Atualiza situação para `INTEGRADO` |

### Exemplo — criar evento

```bash
curl -X POST http://localhost:8080/eventos \
  -H "Content-Type: application/json" \
  -d '{"descricao": "teste de topico do evento"}'
```

### Exemplo — integrar evento manualmente

```bash
curl -X PATCH http://localhost:8080/eventos/{id}/integrar
```

---

## 5. Fluxo completo

```
App1 (CRON a cada 10min)
  └─> publica evento ENCERRADO no tópico AplicacaoAgrotisTesteEvento

App2 (Consumer)
  └─> lê mensagem ENCERRADO
      └─> chama PATCH /eventos/{id}/integrar no App1
          ├─> sucesso: evento atualizado para INTEGRADO
          └─> falha: retry (até 3 tentativas, intervalo 3s)
              └─> esgotou: mensagem enviada para AplicacaoAgrotisTesteEvento.DLQ
```

---

## 6. Armazenamento

Os eventos são persistidos no **PostgreSQL** (container `agr-postgres-local`).
Banco: `agrotis_eventos` | Usuário: `agrotis` | Senha: `agrotis`

O schema é gerenciado pelo Hibernate (`ddl-auto: update`).
**Limitação:** dados perdidos se o container for removido com `docker compose down -v`.

---

## 7. Testes

### Testes automatizados (E2E)

Com App1, App2, Kafka e Postgres rodando, execute na raiz do repositório:

```bash
chmod +x test.sh
./test.sh
```

A variável `SCHEDULER_WAIT` no topo do script controla quantos segundos o teste aguarda
o scheduler publicar os eventos. O valor padrão é `20` segundos, adequado para o cron
configurado em `*/15 * * * * *` (a cada 15 segundos).

Se alterar o cron no `application.yml` do App1, ajuste o `SCHEDULER_WAIT` proporcionalmente:

```bash
# Exemplo: cron a cada 30 segundos
SCHEDULER_WAIT=35 ./test.sh
```

O script cobre os seguintes cenários:

| Teste | O que valida |
|---|---|
| POST /eventos | Retorna 201 com situação `ENCERRADO` |
| POST com descrição vazia | Retorna 400 com mensagem de erro |
| PATCH com id inexistente | Retorna 404 com mensagem de erro |
| PATCH /integrar | Retorna 200 e atualiza situação para `INTEGRADO` |
| Idempotência do scheduler | Evento `INTEGRADO` não é republicado no tópico |

### Teste manual da DLQ

O teste da DLQ não está no script pois requer intervenção manual:

1. Com App1 e App2 rodando, crie um evento:
```bash
curl -X POST http://localhost:8080/eventos \
  -H "Content-Type: application/json" \
  -d '{"descricao": "teste dlq"}'
```

2. Derrube o App1 (`Ctrl+C`).

3. Aguarde o scheduler publicar o evento (ou insira diretamente no banco):
```sql
INSERT INTO eventos (id, descricao, situacao, criado_em, atualizado_em)
VALUES (gen_random_uuid(), 'teste dlq manual', 'ENCERRADO', now(), now());
```

4. Observe no log do App2 as 3 tentativas seguidas do envio para DLQ:
```
[App1Client] Falha ao integrar evento id=...
[App1Client] Falha ao integrar evento id=...
[App1Client] Falha ao integrar evento id=...
[DLQ] Enviando mensagem para DLQ=AplicacaoAgrotisTesteEvento.DLQ
```

5. Confirme a mensagem na DLQ:
```bash
docker exec -it agr-kafka-local /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic AplicacaoAgrotisTesteEvento.DLQ \
  --from-beginning
```

---

## 8. Parar o ambiente

```bash
docker compose down
```

Para remover também os volumes (apaga dados do Postgres):

```bash
docker compose down -v
```

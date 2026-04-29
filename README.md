# agrotis-kafka-integrations

Desafio técnico Agrotis — Spring Boot + Kafka.

## Estrutura

```
agrotis-kafka-integrations/
├── compose.yml
├── README.md
├── app1-producer/   # Producer + API REST
└── app2-consumer/   # Consumer + integração com App1
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

## 7. Parar o ambiente

```bash
docker compose down
```

Para remover também os volumes (apaga dados do Postgres):

```bash
docker compose down -v
```

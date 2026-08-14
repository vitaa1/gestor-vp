# Backend

API do Vence Fácil construída com Java 21, Spring Boot, PostgreSQL e Flyway.

## Executar localmente

Na raiz do repositório, inicie o banco:

```powershell
docker compose up -d database
```

Depois execute a API:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

A verificação de saúde estará disponível em
`http://localhost:8080/actuator/health`.

## Estoque

A API expõe o primeiro fluxo funcional em `/api/stock-entries`:

- `POST /api/stock-entries`: cadastra nome, quantidade e data de validade;
- `GET /api/stock-entries`: lista entradas com saldo disponível pela validade
  mais próxima.

Exemplo de cadastro:

```json
{
  "productName": "Leite Integral",
  "quantity": 12,
  "expirationDate": "2026-08-20"
}
```

## Testes

Os testes de integração utilizam Testcontainers e exigem o Docker em execução:

```powershell
.\mvnw.cmd verify
```

As configurações aceitam `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SERVER_PORT`,
`APP_USERNAME` e `APP_PASSWORD` como variáveis de ambiente. A API não inicia
sem uma conta de operador configurada e deve ser publicada somente por HTTPS.

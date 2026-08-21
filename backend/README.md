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

A API expõe o primeiro fluxo funcional em `/api/v1/stock-entries`:

- `POST /api/v1/stock-entries`: cadastra nome, quantidade e data de validade;
- `GET /api/v1/stock-entries`: lista entradas com saldo disponível pela validade
  mais próxima, com paginação por cursor estável mesmo quando o estoque muda
  entre os carregamentos. Os parâmetros opcionais `query` e `status` combinam
  busca parcial normalizada com os filtros `EXPIRED`, `ATTENTION`, `WATCH` e
  `OK`.

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
`APP_USERNAME`, `APP_PASSWORD` e `APP_DEFAULT_TIME_ZONE` como variáveis de
ambiente. Em publicação,
`SPRING_FLYWAY_URL`, `SPRING_FLYWAY_USER` e `SPRING_FLYWAY_PASSWORD` fornecem
uma credencial de migration separada do usuário de runtime. A API não inicia
sem uma conta de operador configurada e deve ser publicada somente por HTTPS.

O modo de demonstração exige `DEMO_MODE=true`, um `DEMO_INSTANCE_ID` exclusivo
e um banco vazio dedicado. Ele restaura somente dados sintéticos após o
intervalo de `DEMO_RESET_AFTER` e falha sem apagar nada se o marcador do banco
não corresponder à instância.

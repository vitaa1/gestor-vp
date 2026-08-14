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

## Testes

Os testes de integração utilizam Testcontainers e exigem o Docker em execução:

```powershell
.\mvnw.cmd verify
```

As configurações aceitam `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` e
`SERVER_PORT` como variáveis de ambiente.

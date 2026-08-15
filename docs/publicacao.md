# Publicação inicial

Este guia prepara a demonstração do Vence Fácil no Render com PostgreSQL no
Neon. O ambiente contém somente dados sintéticos e descartáveis.

## 1. Criar o banco e os papéis no Neon

Crie um projeto, escolha uma região próxima da região do serviço no Render e
mantenha o hostname fornecido pelo Neon. No SQL Editor, conectado com o papel
proprietário, substitua as senhas e execute:

```sql
create role vence_facil_migration login password '<senha-forte-de-migration>';
create role vence_facil_runtime login password '<senha-forte-de-runtime>';

grant usage, create on schema public to vence_facil_migration;
grant usage on schema public to vence_facil_runtime;
```

Depois, conecte o SQL Editor como `vence_facil_migration` e configure os
privilégios que serão aplicados aos objetos criados pelo Flyway:

```sql
alter default privileges in schema public
    grant select, insert, update, delete on tables to vence_facil_runtime;

alter default privileges in schema public
    grant usage, select on sequences to vence_facil_runtime;
```

Não conceda `CREATE` no schema ao papel de runtime. O `DELETE` é necessário
nesta demonstração porque o reset diário substitui os dados sintéticos; ele
deve ser removido de uma implantação que passe a guardar dados reais e não use
`DEMO_MODE`.

Use conexões diretas nesta primeira publicação. Converta as URLs do Neon para o
formato JDBC, mantendo o hostname e acrescentando `sslmode=verify-full`:

```text
jdbc:postgresql://<hostname-neon>/<database>?sslmode=verify-full
```

## 2. Criar o Blueprint no Render

Conecte o repositório ao Render e crie um Blueprint a partir do `render.yaml`.
O arquivo fixa a branch `main`, o Dockerfile, o healthcheck e o deploy somente
após os checks do GitHub.

Preencha os valores marcados como secretos:

| Variável | Valor |
| --- | --- |
| `DB_URL` | URL JDBC do Neon com o papel de runtime |
| `DB_USERNAME` | `vence_facil_runtime` |
| `DB_PASSWORD` | senha exclusiva do runtime |
| `SPRING_FLYWAY_URL` | mesma URL JDBC direta |
| `SPRING_FLYWAY_USER` | `vence_facil_migration` |
| `SPRING_FLYWAY_PASSWORD` | senha exclusiva de migration |
| `APP_USERNAME` | usuário compartilhado da demonstração |
| `APP_PASSWORD` | senha exclusiva da demonstração, com 12 a 200 caracteres |
| `DEMO_INSTANCE_ID` | identificador aleatório e exclusivo deste banco |

Nunca reutilize essas senhas em desenvolvimento ou em outra implantação. O
Render fornece HTTPS, injeta `PORT` e encaminha o IP validado pelo cabeçalho
configurado no Blueprint.

## 3. Validar o primeiro deploy

Após os checks e o deploy concluírem:

1. confirme `200` em `/actuator/health`;
2. abra a raiz e verifique o aviso de versão em desenvolvimento;
3. faça login com a credencial da demonstração;
4. confirme os quatro registros sintéticos;
5. cadastre uma entrada e atualize a página para validar a persistência;
6. verifique nos logs que as migrations chegaram à versão mais recente.

Se o startup falhar por permissão, corrija os `GRANT` no Neon. Não troque a
aplicação para o papel de migration como atalho.

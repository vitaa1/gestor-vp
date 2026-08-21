# Publicação inicial

Este guia prepara a demonstração do gestorVP no Railway com PostgreSQL no
Neon. O ambiente contém somente dados sintéticos e descartáveis.

## 1. Criar o banco e os papéis no Neon

Crie um projeto na região AWS Oregon, próxima da região US West do Railway, e
mantenha o hostname fornecido pelo Neon. Use o papel proprietário apenas para o
bootstrap. Papéis criados pelo botão **Add role** do Console recebem associação
com `neon_superuser` e não servem para a separação de privilégios desta
aplicação.

No SQL Editor, conectado como proprietário, execute o bloco abaixo. As senhas
são geradas dentro do PostgreSQL e aparecem somente no resultado do segundo
comando, sem serem gravadas no texto da consulta:

```sql
create or replace function pg_temp.bootstrap_gestor_vp_roles()
returns table(role_name text, generated_password text)
language plpgsql
as $bootstrap$
declare
    migration_password text := gen_random_uuid()::text || gen_random_uuid()::text;
    runtime_password text := gen_random_uuid()::text || gen_random_uuid()::text;
begin
    execute format(
        'create role gestor_vp_migration login password %L',
        migration_password
    );

    execute format(
        'create role gestor_vp_runtime login password %L',
        runtime_password
    );

    execute 'grant usage, create on schema public to gestor_vp_migration';
    execute 'grant usage on schema public to gestor_vp_runtime';

    execute 'grant gestor_vp_migration to neondb_owner
             with set true, inherit false';
    execute 'set local role gestor_vp_migration';

    execute 'alter default privileges in schema public
             grant select, insert, update, delete on tables
             to gestor_vp_runtime';
    execute 'alter default privileges in schema public
             grant usage, select on sequences to gestor_vp_runtime';

    execute 'reset role';
    execute 'grant gestor_vp_migration to neondb_owner
             with set false, inherit false';

    return query values
        ('gestor_vp_migration'::text, migration_password),
        ('gestor_vp_runtime'::text, runtime_password);
end
$bootstrap$;

select * from pg_temp.bootstrap_gestor_vp_roles();
```

Guarde cada senha em um gerenciador de segredos. Não conceda `CREATE` no schema
ao papel de runtime. O `DELETE` é necessário nesta demonstração porque o reset
diário substitui os dados sintéticos; remova-o de uma implantação que passe a
guardar dados reais e não utilize `DEMO_MODE`. A migration V6 revoga do runtime
`gestor_vp_runtime` todo acesso a `flyway_schema_history`, evitando que os
privilégios padrão sobre tabelas alcancem o histórico do Flyway. A V3 permanece
imutável para proteger instalações criadas com o nome anterior.

Use conexões diretas nesta primeira publicação. Converta a URL do Neon para o
formato JDBC, mantenha o hostname e configure o pgJDBC para validar certificado
e hostname com o truststore padrão do Java:

```text
jdbc:postgresql://<hostname-neon>/<database>?sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory
```

Sem `DefaultJavaSSLFactory`, a fábrica compatível com libpq procura por padrão o
arquivo `~/.postgresql/root.crt`, que não existe na imagem de publicação. Não
substitua `verify-full` por `require` como atalho: `require` criptografa a
conexão, mas não valida o certificado e o hostname do servidor.

## 2. Criar o serviço no Railway

Conecte a conta do GitHub ao Railway, crie um projeto vazio e adicione um
serviço a partir do repositório `vitaa1/gestor-vp`. Selecione a branch `main`.
O `railway.json` fixa o Dockerfile, o healthcheck e a política de reinício.

Nas configurações do serviço:

1. habilite **Wait for CI** para impedir deploy quando o GitHub Actions falhar;
2. habilite **Serverless** para reduzir consumo enquanto a demonstração estiver
   inativa;
3. escolha a região **US West**;
4. gere um domínio público do Railway;
5. não adicione volume ou banco do Railway.

No plano Free, novos deploys em US West são recusados entre 8h e 20h no horário
do Pacífico. Faça o primeiro deploy fora dessa janela ou use temporariamente um
plano pago; o serviço que já estiver ativo não é removido por essa restrição.

Cadastre as variáveis abaixo. Insira senhas somente pela área de variáveis do
Railway e nunca no repositório:

| Variável                              | Valor                                                               |
| ------------------------------------- | ------------------------------------------------------------------- |
| `DB_URL`                              | URL JDBC direta com `sslmode=verify-full` e `DefaultJavaSSLFactory` |
| `DB_USERNAME`                         | `gestor_vp_runtime`                                                  |
| `DB_PASSWORD`                         | senha exclusiva do runtime                                          |
| `SPRING_FLYWAY_URL`                   | mesma URL JDBC direta                                               |
| `SPRING_FLYWAY_USER`                  | `gestor_vp_migration`                                                |
| `SPRING_FLYWAY_PASSWORD`              | senha exclusiva de migration                                        |
| `APP_USERNAME`                        | usuário compartilhado da demonstração                               |
| `APP_PASSWORD`                        | senha exclusiva da demonstração, com 12 a 200 caracteres            |
| `APP_DEFAULT_TIME_ZONE`               | `America/Sao_Paulo` como fallback para clientes antigos ou externos |
| `DEMO_MODE`                           | `true`                                                              |
| `DEMO_INSTANCE_ID`                    | identificador aleatório e exclusivo deste banco                     |
| `DEMO_RESET_AFTER`                    | `24h`                                                               |
| `TRUSTED_PROXY_HEADER`                | `X-Real-IP`                                                         |
| `AUTH_RATE_LIMIT_MAX_FAILURES`        | `5`                                                                 |
| `AUTH_RATE_LIMIT_MAX_FAILURES_PER_IP` | `20`                                                                |
| `AUTH_RATE_LIMIT_WINDOW`              | `15m`                                                               |
| `AUTH_RATE_LIMIT_MAX_KEYS`            | `10000`                                                             |
| `DB_POOL_MINIMUM_IDLE`                | `0`                                                                 |
| `DB_POOL_IDLE_TIMEOUT`                | `60000`                                                             |

Nunca reutilize essas senhas em desenvolvimento ou em outra implantação. O
Railway fornece HTTPS, injeta `PORT` e encaminha o endereço validado do cliente
em `X-Real-IP`.

O frontend envia automaticamente o fuso IANA informado pelo navegador em cada
requisição de estoque. `APP_DEFAULT_TIME_ZONE` não substitui essa detecção; ele
impede que clientes sem o cabeçalho dependam do fuso do servidor e define a
data operacional confiável para regras de retirada. O cabeçalho controlado pelo
cliente altera apenas a classificação e a contagem de dias exibidas.

O endereço definitivo do ambiente será registrado aqui depois que o cutover
descrito na próxima seção for executado e validado. Até lá, não presuma que o
slug do projeto corresponde ao domínio público efetivamente gerado pelo
Railway.

### Renomear a implantação existente

Não execute o bootstrap de uma instalação nova na produção existente. Para
preservar ownership, associações e privilégios, renomeie os dois papéis atuais
no Neon: o PostgreSQL mantém essas concessões ligadas ao mesmo identificador
interno do papel.

Faça o cutover em uma janela coordenada:

1. impeça novos deploys e pause o serviço no Railway;
2. conectado ao Neon como proprietário, execute `ALTER ROLE` para mudar os
   papéis de migration e runtime dos nomes anteriores para
   `gestor_vp_migration` e `gestor_vp_runtime`;
3. confirme que as credenciais usam SCRAM ou rotacione as duas senhas durante a
   pausa; senhas SCRAM sobrevivem ao rename, mas o PostgreSQL apaga hashes MD5
   porque eles dependem do nome do papel;
4. atualize `SPRING_FLYWAY_USER`, `DB_USERNAME` e qualquer senha rotacionada no
   Railway;
5. renomeie o repositório no GitHub para `gestor-vp` e confirme que a integração
   do Railway continua apontando para a `main` e com **Wait for CI** ativo;
6. renomeie o projeto e o serviço no Railway, gere ou associe o novo domínio e
   então retome o serviço;
7. confirme o healthcheck, o login, leitura e escrita do runtime, execução do
   Flyway pelo papel de migration e ausência de qualquer privilégio do runtime
   sobre `flyway_schema_history`;
8. atualize o remote local e substitua nesta documentação o endereço somente
   pelo domínio realmente emitido e validado com HTTPS.

Os renames do passo 2 são:

```sql
begin;

do $$
begin
    if not exists (select 1 from pg_roles where rolname = 'vence_facil_migration')
       or not exists (select 1 from pg_roles where rolname = 'vence_facil_runtime')
       or exists (select 1 from pg_roles where rolname = 'gestor_vp_migration')
       or exists (select 1 from pg_roles where rolname = 'gestor_vp_runtime') then
        raise exception 'estado dos papeis incompativel com o cutover';
    end if;
end
$$;

alter role vence_facil_migration rename to gestor_vp_migration;
alter role vence_facil_runtime rename to gestor_vp_runtime;

commit;
```

Se a V6 já tiver sido executada antes do rename, a proteção feita pela V3
acompanha o papel antigo porque o `ALTER ROLE` preserva seu identificador
interno. Se o rename ocorrer antes do deploy, a V6 aplicará a mesma proteção ao
novo nome. Não crie papéis paralelos nem conceda privilégios amplos para
contornar a transição.

## 3. Validar o primeiro deploy

Após os checks e o deploy concluírem:

1. confirme `200` em `/actuator/health`;
2. abra a raiz e verifique o aviso de versão em desenvolvimento;
3. faça login com a credencial da demonstração;
4. confirme os quatro registros sintéticos;
5. cadastre uma entrada e atualize a página para validar a persistência;
6. verifique nos logs que as migrations chegaram à versão mais recente e que a
   V6 protegeu o histórico do Flyway;
7. de uma única conexão, faça 20 logins inválidos com usuários diferentes e um
   valor de `X-Real-IP` diferente em cada requisição; a 21ª tentativa deve
   receber `429`, confirmando que a borda sobrescreve o cabeçalho enviado pelo
   cliente e mantém o limite pelo IP real;
8. depois que o pool liberar as conexões ociosas, aguarde ao menos 10 minutos e
   confirme que o primeiro acesso reativa o serviço. O repouso é uma otimização
   de melhor esforço e pode ser impedido por outro tráfego de saída da JVM.

Se o startup falhar por permissão, corrija os `GRANT` no Neon. Não troque a
aplicação para o papel de migration ou proprietário como atalho.

## 4. Diagnosticar falhas de inicialização

O healthcheck informa apenas que a aplicação não ficou disponível. Consulte
**Deploy Logs** para encontrar a causa antes de aumentar o timeout ou repetir o
deploy.

| Mensagem principal                                                | Causa provável                                                       | Correção                                                                         |
| ----------------------------------------------------------------- | -------------------------------------------------------------------- | -------------------------------------------------------------------------------- |
| `Connection to localhost:5432 refused`                            | `DB_URL` e `SPRING_FLYWAY_URL` ausentes                              | cadastre as URLs diretas do Neon nas variáveis do serviço                        |
| `Could not open SSL root certificate file`                        | URL com `verify-full`, mas usando a fábrica SSL compatível com libpq | acrescente `sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory` às duas URLs    |
| `password authentication failed for user 'gestor_vp_migration'` | senha do Flyway não corresponde ao papel de migration                | corrija somente `SPRING_FLYWAY_PASSWORD` ou redefina a senha desse papel no Neon |
| `password authentication failed for user 'gestor_vp_runtime'`   | senha do datasource não corresponde ao papel de runtime              | corrija somente `DB_PASSWORD` ou redefina a senha desse papel no Neon            |

Não inclua usuário ou senha na URL JDBC e não copie os valores locais sugeridos
automaticamente pelo Railway. `DB_PASSWORD` e `SPRING_FLYWAY_PASSWORD` são
segredos distintos.

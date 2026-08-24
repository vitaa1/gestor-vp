# Benchmarks de desempenho

> **Atenção:** `seed.sql` começa com `TRUNCATE ... CASCADE` e apaga os dados do
> banco informado. Execute-o somente no contêiner local descartável criado pelo
> `compose.yaml`; nunca aponte estes comandos para um banco compartilhado ou de
> produção.

Os cenários usam PostgreSQL 18 e uma carga sintética determinística de 100 mil
produtos, 100 mil entradas e 400 mil movimentações. As consultas correspondem
às páginas de estoque, busca rara e histórico (primeira página e cursor).

Com o banco do `compose.yaml` ativo e as migrations aplicadas:

```powershell
docker cp benchmarks/performance/seed.sql gestor-vp-database-1:/tmp/seed.sql
docker cp benchmarks/performance/queries.sql gestor-vp-database-1:/tmp/queries.sql
docker exec gestor-vp-database-1 psql -U gestor_vp -d gestor_vp -f /tmp/seed.sql
docker exec gestor-vp-database-1 psql -U gestor_vp -d gestor_vp -f /tmp/queries.sql
```

Para reproduzir a linha de base, use um banco criado a partir de `main`, rode o
mesmo `seed.sql`, copie `queries-before.sql` e `pgbench-before/`, e execute os
quatro cenários não parametrizados com os mesmos parâmetros (`-n -c 1 -j 1 -T
10`). O quinto script, parametrizado, usa o modo indicado abaixo. Para o estado
otimizado, migre o banco para V8, copie `queries.sql` e `pgbench/` e repita. Não
compare execuções que reutilizem dados ou estatísticas diferentes.

Execute `queries.sql` duas vezes e descarte a primeira execução (aquecimento de
cache). Compare `Execution Time`, buffers e os nós de ordenação/varredura do
segundo resultado. A compilação de produção do frontend é medida com
`npm run build`; registre os tamanhos bruto e transferido exibidos pelo Angular.

Para medir throughput e latência com cache aquecido, copie `pgbench/` para o
contêiner e execute cada cenário por 10 segundos, com um cliente e sem o custo
de reconexão:

```powershell
docker cp benchmarks/performance/pgbench gestor-vp-database-1:/tmp/pgbench
docker exec gestor-vp-database-1 pgbench -U gestor_vp -d gestor_vp -n -c 1 -j 1 -T 10 -f /tmp/pgbench/active-rare-search.sql
```

A busca parametrizada deve ser medida em protocolo estendido sem statement
nomeado, equivalente a `prepareThreshold=0` configurado no datasource:

```powershell
docker exec gestor-vp-database-1 pgbench -U gestor_vp -d gestor_vp -n -M extended -c 1 -j 1 -T 10 -f /tmp/pgbench/active-rare-search-parameterized.sql
```

Não use `-M prepared` como representante da configuração padrão do app: ele
promove o statement a um plano genérico, justamente o comportamento desativado
porque esta consulta é sensível à seletividade do termo.

Para demonstrar o comportamento anterior do driver após a promoção para plano
nomeado/genérico, execute o script equivalente da linha de base com
`-M prepared`. A comparação representativa da configuração final é o script
otimizado com `-M extended`.

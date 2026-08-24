# Desempenho

## Objetivo e ambiente

O benchmark cobre os caminhos de leitura mais usados e os que tendem a degradar
com o crescimento da base: estoque inicial, busca por trecho do nome, histórico
inicial e próxima página do histórico. A medição de 23/08/2026 usou PostgreSQL
18.6 em Docker Desktop, um cliente persistente e uma massa determinística de
100 mil produtos, 100 mil entradas (90 mil ativas) e 400 mil movimentações.

Cada cenário foi aquecido antes da coleta. O throughput e a latência foram
medidos por 10 segundos com `pgbench`, um cliente e uma thread. A busca também
foi medida com parâmetro: `prepared` representa a promoção anterior a statement
nomeado, e `extended` representa o statement sem nome usado com
`prepareThreshold=0`. Os planos foram inspecionados separadamente com `EXPLAIN
(ANALYZE, BUFFERS)`. Os scripts e instruções estão em
`benchmarks/performance/`.

## Gargalos encontrados

- A busca por trecho usava `strpos`/`locate`, que não aproveitava o índice
  B-tree de `products.search_name`. Uma busca rara fazia 90 mil consultas ao
  índice primário de produtos, tocava 360.468 buffers e levava 161,9 ms no
  plano registrado.
- O cursor do histórico era expresso como uma disjunção. O PostgreSQL lia e
  descartava 54.400 movimentações antes da página desejada, tocando 1.479
  buffers e levando 13,0 ms no plano registrado.
- Os caminhos de primeira página já eram rápidos. Eles foram mantidos como
  controles para detectar regressões, não como alvos principais.

## Melhorias

- A busca passou a usar `LIKE '%termo%'`, cuja seletividade estimada faz o
  PostgreSQL filtrar produtos uma vez e usar hash join, em vez de realizar uma
  busca aleatória por produto para cada entrada ativa.
- O driver PostgreSQL não promove consultas a prepared statements nomeados
  (`prepareThreshold=0`). A busca é muito sensível ao valor do termo e um plano
  genérico voltaria ao nested loop lento; o pequeno custo de planejamento por
  execução mantém o plano seletivo. A configuração é global e pode ser
  revertida por `DB_PREPARE_THRESHOLD` se o perfil de produção demonstrar outro
  trade-off.
- O cursor do histórico passou a usar comparação lexicográfica por tupla, que preserva
  exatamente a ordenação estável da API e pode virar condição de índice.
- Um índice composto `(created_at desc, id desc)` foi adicionado ao histórico.
  O índice original, menor, foi preservado para não penalizar outros planos.
- A migration Flyway `V8__optimize_inventory_queries.sql` instala o índice de
  cursor de forma versionada; `ddl-auto=validate` permanece inalterado. A
solução não exige extensões nem privilégios adicionais no banco.

Em uma base de produção grande, `CREATE INDEX` pode bloquear escritas em
`stock_movements` durante a migration. O deploy deve ser feito em janela de
manutenção e monitorado; se o volume superar o perfil atual, a alternativa é
uma migration não transacional com `CREATE INDEX CONCURRENTLY`, preparada e
validada separadamente.

## Antes e depois

| Cenário | Latência antes | Latência depois | TPS antes | TPS depois | Resultado |
| --- | ---: | ---: | ---: | ---: | ---: |
| Estoque, primeira página | 0,564 ms | 0,598 ms | 1.774,1 | 1.672,6 | variação de +0,034 ms |
| Busca rara parametrizada | 129,175 ms | 28,871 ms | 7,7 | 34,6 | **77,6% menos latência; 4,5x TPS** |
| Histórico, primeira página | 4,449 ms | 4,737 ms | 224,8 | 211,1 | variação de +0,288 ms |
| Histórico, página por cursor | 11,141 ms | 4,832 ms | 89,8 | 206,9 | **56,6% menos latência; 2,3x TPS** |

As pequenas diferenças nos dois controles rápidos ficaram abaixo de 0,3 ms em
valor absoluto. Os ganhos dos gargalos são acompanhados por mudanças de plano:
a busca rara passa de 90 mil buscas individuais e 360.468 buffers para duas
varreduras sequenciais paralelas e 2.778 buffers (redução de 99,2%); o cursor
passa de filtro com 54.400 descartes para `Index Cond`, retornando a página após
tocar 110 buffers (redução de 92,6%).

## Frontend

A compilação Angular de produção permanece dentro do orçamento configurado:
344,05 kB brutos e 85,62 kB estimados para transferência no carregamento
inicial. Não foi alterada, pois o perfil encontrou ordens de grandeza maiores
nos dois caminhos de banco e não havia evidência de um gargalo no bundle atual.

## Limitações

Os números são microbenchmarks locais com cache aquecido e uma única conexão;
não representam latência de rede nem contenção de produção. A distribuição é
sintética e deliberadamente adversa para buscas raras. O custo adicional é um
índice B-tree, que aumenta armazenamento e trabalho de escrita; o app é
orientado a leitura e o ganho no cursor degradado justificou esse trade-off.
Uma medição em produção deve observar p95/p99 e taxa real de escritas antes de
novos ajustes.

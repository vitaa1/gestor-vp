# Roadmap do MVP

Este roadmap ordena o trabalho restante do gestorVP após a entrega do
primeiro fluxo vertical. Cada incremento deve usar branch isolada, Pull Request
focado, validações automatizadas e as revisões delegadas definidas no
`AGENTS.md`.

## Princípios de entrega

- A `main` permanece executável e é a única branch publicada.
- O Railway inicia o deploy somente depois que os checks do GitHub passam.
- O ambiente remoto é uma demonstração em desenvolvimento até a conclusão do
  MVP.
- Cada implantação representa um estabelecimento e utiliza uma única conta de
  operador durante o MVP.
- Regras de domínio, segurança, contratos e correções seguem TDD quando o teste
  automatizado consegue expressar o comportamento antes da implementação.
- Mudanças de schema usam Flyway e devem ser aditivas e retrocompatíveis com a
  versão anterior enquanto o plano gratuito executar migrations no startup.
- Flyway usa uma credencial de migration própria; a aplicação usa outra
  credencial, limitada às operações necessárias nas tabelas.
- Edição, exclusão e correção retroativa de estoque permanecem fora do MVP.

## Incrementos

### 1. Alinhar a documentação

- incorporar o glossário de domínio em `CONTEXT.md`;
- harmonizar o documento do produto com a autenticação single-tenant;
- registrar as regras acordadas para retirada, busca e detalhes opcionais;
- manter este roadmap como ordem de referência para os próximos PRs.

### 2. Publicar o primeiro fluxo — validação operacional quase concluída

O container está online desde 15/08/2026, com domínio público, healthcheck,
TLS, migrations V1–V3, **Wait for CI** e **Serverless** configurados. A
validação operacional confirmou:

- CI aprovado para o merge do PR #6 e autodeploy da `main` iniciado pelo
  Railway somente depois do check;
- healthcheck `200` com estado `UP`, login, os quatro registros sintéticos e um
  cadastro preservado após recarregar a aplicação;
- schema na versão 3 e ausência de permissão do papel de runtime então chamado
  `vence_facil_runtime` para consultar `flyway_schema_history`;
- vinte respostas `401` seguidas de `429` na 21ª autenticação inválida, mesmo
  com valores diferentes de `X-Real-IP` enviados pelo cliente;
- entrada do serviço em repouso após a inatividade.

Resta confirmar que o primeiro acesso após ao menos dez minutos de repouso
reativa o serviço.

Em 21/08/2026, a infraestrutura foi renomeada para gestorVP: repositório
`vitaa1/gestor-vp`, projeto e serviço `gestor-vp`, domínio público
`gestor-vp-production.up.railway.app` e papéis Neon `gestor_vp_migration` e
`gestor_vp_runtime`. O cutover preservou os OIDs e privilégios dos papéis
existentes. Após o deploy, healthcheck e aplicação responderam `200`; somente
os dois papéis da aplicação, agora com os novos nomes, permaneceram, e os nomes
anteriores não existem. O runtime retornou `false` para `SELECT`, `INSERT`,
`UPDATE` e `DELETE` sobre `flyway_schema_history`. O PR #20 também publicou a
identidade visual azul-escuro e laranja, confirmada no bundle de produção sem a
paleta verde anterior.

- gerar o build Angular e servi-lo pelo Spring Boot em um único container;
- publicar a aplicação no Railway Free e o PostgreSQL no Neon Free;
- configurar HTTPS, healthcheck e conexão com o Neon usando
  `sslmode=verify-full`, `DefaultJavaSSLFactory`, hostname oficial e cadeia de
  CA confiável;
- executar Flyway no startup com usuário de migration separado do usuário de
  runtime;
- migrar os endpoints do produto para `/api/v1/**` antes da publicação;
- tornar públicos somente `GET` dos artefatos do Angular, rotas do SPA e
  healthcheck, mantendo `/api/**` autenticado;
- manter saúde e informações mínimas em `/actuator/health` e `/actuator/info`,
  sem expor endpoint para migrations;
- limitar autenticações inválidas pela combinação de identidade e IP e também
  pelo IP, evitando contorno por rotação de identidades;
- adicionar `DEMO_MODE`, dados representativos e restauração após 24 horas;
- executar Playwright contra frontend, backend e PostgreSQL reais para login,
  cadastro e listagem;
- publicar automaticamente a `main` somente após o CI aprovado.

### 3. Detalhes e retirada de unidades — concluído

O PR #8 entregou este incremento com testes automatizados de backend e
frontend. O merge `a38da33` faz parte da `main` publicada pelo Railway depois
da aprovação do CI.

- [x] abrir uma entrada pela identificação do produto e da validade;
- [x] mostrar dados completos, quantidades inicial e disponível e situação;
- [x] confirmar ou cancelar a retirada antes de persistir;
- [x] aceitar os motivos **Venda**, **Uso**, **Doação**, **Perda** e **Vencimento**;
- [x] permitir somente **Perda** e **Vencimento** em entradas vencidas;
- [x] rejeitar integralmente retiradas superiores à quantidade disponível;
- [x] atualizar o saldo atomicamente e registrar uma movimentação imutável.

### 4. Histórico — concluído

O PR #13 entregou este incremento com paginação por cursor estável, migration
Flyway V4 e testes automatizados de backend, frontend e do fluxo completo em
desktop e celular. O merge `5743f1a` está na `main` publicada pelo Railway: o
healthcheck respondeu `200` com TLS válido, a aplicação serviu o bundle Angular
do incremento e a nova rota autenticada rejeitou acesso anônimo com `401`.

- [x] listar uma movimentação por linha, com registros recentes primeiro;
- [x] mostrar produto, validade, tipo, quantidade, data/hora e motivo aplicável;
- [x] paginar a consulta;
- [x] permitir consultar entradas encerradas em modo somente leitura.

### 5. Consulta de produtos — concluído

O PR #17 entregou este incremento com paginação por cursor, migration Flyway
V5 compatível com o rollout, 51 testes de backend, 39 testes de frontend e o
fluxo Playwright aprovado em desktop e celular. O merge `bb7ae1f` está na
`main` publicada pelo Railway: o healthcheck respondeu `200` com status `UP` e
o bundle Angular `main-FRG2PRMQ.js` contém a nova área **Produtos**.

- [x] criar a área **Produtos**;
- [x] buscar parte do nome ignorando caixa, acentos e espaços excedentes;
- [x] combinar a busca com os filtros **Todos**, **Vencido**, **Atenção**,
  **Fique de olho** e **Tudo certo**;
- [x] mostrar uma mensagem amigável quando não houver resultados.

### 6. Detalhes opcionais — concluído

O PR #22 entregou este incremento com a migration Flyway V7 e testes de
backend, frontend e Playwright em desktop e celular. O merge `40b55ac` faz
parte da `main` publicada pelo Railway.

- [x] adicionar código de barras e categoria ao produto;
- [x] adicionar custo unitário, fornecedor e número do lote à entrada;
- [x] manter os campos recolhidos em **Mais detalhes** e todos opcionais;
- [x] validar unicidade e formato do código de barras;
- [x] criar a migration Flyway correspondente.

### 7. Início e experiência integrada — concluído

Durante o refinamento do produto, a proposta inicial de contagens e quatro
grupos recolhíveis foi substituída pelo resumo mais simples definido em
`docs/produto.md`: uma única lista com até cinco entradas, ordenada por
validade, e acesso à consulta completa. O PR #25 entregou esse escopo e as URLs
das seções; o PR #26 concluiu a validação responsiva em desktop, tablet e
celular. Os merges `6d20197` e `0584042` fazem parte da `main` publicada pelo
Railway.

- [x] limitar a página inicial às cinco entradas que vencem primeiro;
- [x] permitir abrir a consulta completa pela ação **Ver todos os produtos**;
- [x] concluir a navegação com **Início**, **Produtos** e **Histórico**;
- [x] manter **Adicionar produto** em destaque;
- [x] sincronizar as seções com as URLs `/`, `/produtos` e `/historico`;
- [x] confirmar o cadastro com texto e ícone de sucesso;
- [x] validar o fluxo integrado em desktop, tablet e celular.

### 8. Fechar e promover o MVP

Auditoria realizada em 24/08/2026 contra HU01–HU05, a implementação e as
suítes automatizadas:

| História | Evidência automatizada existente | Evidência Playwright |
| --- | --- | --- |
| HU01 | limite de cinco itens, atalho, conteúdo textual, classificação e ordenação cobertos por testes de componente e integração | comprova limite, atalho, classificação, ordenação e dados textuais nas quatro situações |
| HU02 | obrigatoriedade, quantidade positiva, data válida, confirmação com ícone e detalhes opcionais cobertos por componente e integração | comprova cadastros básico e completo sem tornar os detalhes obrigatórios |
| HU03 | busca parcial normalizada, retorno das entradas ativas e estado vazio cobertos por componente e integração | comprova busca por parte do nome, estado vazio e os cinco filtros |
| HU04 | limites de saldo, atualização atômica, cancelamento, entrada escolhida e restrições de motivo vencido cobertos por componente e integração | comprova cancelamento, concorrência com saldo insuficiente, retirada parcial, atualização imediata, encerramento e motivos de entrada vencida |
| HU05 | campos, ordenação recente, paginação e consulta somente leitura cobertos por componente e integração | comprova os campos de entradas e retiradas, registros recentes primeiro e ausência de ações de alteração |

O Playwright também comprova login válido nos três viewports, login inválido no
desktop e ausência de overflow em tablet e celular.

Com a cobertura dos critérios concluída, o próximo PR deve tratar das capturas
de tela e da apresentação da demonstração. O aviso de desenvolvimento só pode
ser removido depois das validações finais de produção e do registro das
limitações operacionais.

- [x] cobrir com Playwright cadastro básico;
- [x] cobrir classificação e ordenação por validade;
- [x] cobrir busca por parte do nome e os cinco filtros de situação;
- [x] cobrir retirada parcial, revisão e cancelamento;
- [x] cobrir saldo insuficiente sem alteração da quantidade;
- [x] cobrir motivos permitidos para entrada vencida;
- [x] cobrir login válido e inválido, cadastro completo, retirada,
  encerramento e histórico;
- [x] auditar todos os critérios de HU01–HU05;
- executar as revisões de código e segurança;
- adicionar capturas de tela e credenciais da demonstração ao repositório;
- remover o aviso de versão em desenvolvimento e divulgar o endereço no
  portfólio.

## Publicação inicial

O portfólio usa um único ambiente remoto: Railway Free para o container da
aplicação e Neon Free para o PostgreSQL. Cold start é aceitável e deve ser
explicado ao visitante. O endpoint `/actuator/health` controla a saúde do
deploy, mas rollback de código não reverte migrations nem dados.

As funcionalidades do produto são publicadas sob `/api/v1/**`. Endpoints do
Actuator permanecem separados da API versionada, e o estado do Flyway é
observado por startup e logs protegidos, nunca por um endpoint público de
migration.

O modo de demonstração restaura os dados de forma transacional na primeira
inicialização ou requisição após completar 24 horas. O reset é idempotente,
protegido por lock transacional ou advisory e só opera em um banco exclusivo
que possua um marcador persistido de demonstração. Na dúvida, a operação falha
sem apagar dados; não existe endpoint público de reset.

A credencial compartilhada da demonstração é intencionalmente pública,
exclusiva e nunca reutilizada. Seu valor ainda é injetado por variável do
Railway, e o banco contém somente dados sintéticos e descartáveis. Segredos de
ambientes não demonstrativos permanecem proibidos no repositório.

A conexão JDBC usa `sslmode=verify-full`, `DefaultJavaSSLFactory`, o hostname
oficial do Neon e uma cadeia de CA confiável. Flyway recebe credenciais próprias
de migration; o datasource da aplicação usa um papel de runtime com privilégios
mínimos e sem acesso ao histórico de migrations.

O rate limiting usa somente o cabeçalho de IP explicitamente confiado para a
plataforma (`X-Real-IP` no Railway) e rejeita cadeias e hostnames. Há
limites pela combinação de identidade e IP e também pelo IP isolado, com
armazenamento limitado e resposta `429` sem informar se o usuário existe.

# 0001 — Base técnica do projeto

- Status: aceita
- Data: 14 de agosto de 2026

## Contexto

O gestorVP precisa demonstrar uma aplicação fullstack profissional sem
adicionar complexidade que não ajude o pequeno comerciante. A primeira etapa
deve produzir um ambiente reproduzível, testável e adequado para entregas
incrementais.

## Decisão

- Manter backend, frontend e documentação no mesmo repositório.
- Utilizar Java 21 e Spring Boot 4.1 com Maven Wrapper.
- Utilizar Angular 21 porque ele é compatível com o Node.js 20 disponível no
  ambiente de desenvolvimento.
- Utilizar PostgreSQL 18 e controlar mudanças de schema exclusivamente com
  Flyway.
- Executar testes de integração com PostgreSQL real por meio do Testcontainers.
- Disponibilizar somente o banco no Docker Compose durante o desenvolvimento.
- Validar backend e frontend separadamente no GitHub Actions.
- Evoluir como monólito modular, adicionando módulos apenas junto aos fluxos
  funcionais que os justificarem.

## Consequências

- O ambiente local depende de Docker para banco e testes de integração.
- O primeiro download de dependências e imagens é mais demorado.
- Mudanças no banco deverão ser versionadas e nunca dependerão da geração
  automática do Hibernate.
- Microsserviços, mensageria e orquestração de containers permanecem fora do
  escopo enquanto não houver uma necessidade concreta.

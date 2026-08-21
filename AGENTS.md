# Instruções do repositório para agentes

## Contexto

- O gestorVP é um monólito modular com backend Spring Boot e frontend
  Angular.
- Código e identificadores são escritos em inglês; interface e documentação,
  em pt-BR.
- `docs/produto.md` define o comportamento do produto.
- `docs/decisoes/` registra decisões técnicas aceitas.
- Não introduza microsserviços, mensageria ou infraestrutura sem necessidade
  documentada.

## Desenvolvimento

- Nunca desenvolva diretamente na `main`.
- Use branch isolada e integre por Pull Request conforme `CONTRIBUTING.md`.
- Preserve alterações do usuário e mantenha cada PR focado em um objetivo.
- Mudança de schema exige migration Flyway; Hibernate permanece com
  `ddl-auto=validate`.

## Validação

- Backend: execute `backend\\mvnw.cmd --batch-mode verify` com Docker ativo.
- Frontend: em `frontend/`, execute `npm run format:check`, `npm run test:ci` e
  `npm run build`.
- Não declare a branch pronta enquanto as verificações aplicáveis falharem.

## Revisão delegada

- Antes de abrir um Pull Request com mudança de código ou configuração, use o
  agente `code-reviewer` em modo somente leitura.
- Use também `security-guard` quando a mudança atingir backend, autenticação,
  autorização, persistência, dependências, Docker, CI, configuração, dados ou
  logs.
- Quando os dois forem aplicáveis, execute-os em paralelo e espere ambos.
- Não peça que esses agentes editem código. O agente principal avalia os
  achados, corrige bloqueadores e repete a revisão quando houver mudança
  material.
- Alterações exclusivamente documentais podem dispensar `security-guard`, salvo
  quando a documentação tratar de configuração ou segurança operacional.

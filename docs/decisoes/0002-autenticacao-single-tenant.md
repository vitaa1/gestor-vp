# ADR 0002 — Autenticação single-tenant no MVP

## Status

Aceita.

## Contexto

O primeiro fluxo funcional passa a armazenar dados reais de estoque. O MVP
exclui múltiplos estabelecimentos e diferentes níveis de permissão, mas não
pode expor leitura ou escrita sem autenticação.

## Decisão

- Cada implantação representa um único estabelecimento.
- Uma única conta de operador protege `/api/**`, cujas funcionalidades do
  produto são versionadas sob `/api/v1/**`. Somente `GET` dos artefatos
  estáticos e das rotas do SPA e `GET /actuator/health/**` permanecem públicos;
  liberar o frontend nunca pode ampliar o acesso à API. Migrations não possuem
  endpoint.
- Usuário e senha são obrigatórios e fornecidos por `APP_USERNAME` e
  `APP_PASSWORD`; não existem credenciais padrão na aplicação, e a senha deve
  possuir pelo menos 12 caracteres.
- A API usa HTTP Basic sem sessão. O frontend mantém a credencial somente em
  memória e exige novo login quando a página é recarregada.
- A implantação publicada deve usar HTTPS, pois HTTP Basic não cifra a
  credencial durante o transporte.
- A publicação deve limitar tentativas repetidas de autenticação por IP para
  reduzir força bruta e consumo de CPU por verificações BCrypt. Enquanto o
  proxy não oferecer essa proteção, a aplicação responde com `429` (`Too Many
Requests`); um proxy externo poderá assumir essa responsabilidade no futuro.
- O limitador usa identidade e IP do cliente sem revelar se o usuário existe.
  Atrás de proxy, a aplicação aceita somente a cadeia de encaminhamento
  documentada e validada para a plataforma; cabeçalhos arbitrários enviados
  pelo cliente não são fonte confiável de IP.

## Consequências

- Os registros pertencem à implantação, não a usuários individuais.
- Não há compartilhamento seguro de um banco entre estabelecimentos.
- A credencial de uma demonstração pública é exclusiva desse ambiente, nunca é
  reutilizada e não protege dados reais.
- Múltiplos usuários, recuperação de senha e permissões exigirão uma nova
  decisão e evolução do schema antes de serem implementados.

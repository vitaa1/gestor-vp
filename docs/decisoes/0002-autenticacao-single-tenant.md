# ADR 0002 — Autenticação single-tenant no MVP

## Status

Aceita.

## Contexto

O primeiro fluxo funcional passa a armazenar dados reais de estoque. O MVP
exclui múltiplos estabelecimentos e diferentes níveis de permissão, mas não
pode expor leitura ou escrita sem autenticação.

## Decisão

- Cada implantação representa um único estabelecimento.
- Uma única conta de operador protege todos os endpoints, exceto o healthcheck.
- Usuário e senha são obrigatórios e fornecidos por `APP_USERNAME` e
  `APP_PASSWORD`; não existem credenciais padrão na aplicação, e a senha deve
  possuir pelo menos 12 caracteres.
- A API usa HTTP Basic sem sessão. O frontend mantém a credencial somente em
  memória e exige novo login quando a página é recarregada.
- A implantação publicada deve usar HTTPS, pois HTTP Basic não cifra a
  credencial durante o transporte.
- O proxy de publicação deve limitar tentativas repetidas de autenticação para
  reduzir força bruta e consumo de CPU por verificações BCrypt.

## Consequências

- Os registros pertencem à implantação, não a usuários individuais.
- Não há compartilhamento seguro de um banco entre estabelecimentos.
- Múltiplos usuários, recuperação de senha e permissões exigirão uma nova
  decisão e evolução do schema antes de serem implementados.

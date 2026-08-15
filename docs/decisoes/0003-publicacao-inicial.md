# ADR 0003 — Publicação inicial em container único

## Status

Aceita.

## Contexto e decisão

O portfólio precisa validar o mesmo fluxo vertical em desenvolvimento e em um
ambiente remoto sem antecipar uma arquitetura distribuída. A aplicação será
publicada como um único container, com o Angular servido pelo Spring Boot no
Render e o PostgreSQL hospedado no Neon. Essa escolha reduz operação e custo no
MVP; separar frontend e backend permanece uma opção futura se surgir uma
necessidade concreta.

O Render publica somente a `main` após os checks do GitHub, termina HTTPS e usa
`/actuator/health` para saúde. O Neon exige TLS com verificação do hostname, e
Flyway utiliza uma credencial de migration distinta do papel de runtime. O
ambiente remoto é uma demonstração descartável: contém apenas dados sintéticos,
possui marcador exclusivo e pode restaurá-los após 24 horas sem expor endpoint
de reset.

## Consequências

- O primeiro acesso pode sofrer cold start do plano gratuito.
- Um rollback do container não desfaz migrations nem dados.
- A demonstração não pode compartilhar banco ou credenciais com dados reais.
- PWA, múltiplas instâncias e infraestrutura distribuída continuam fora do MVP.

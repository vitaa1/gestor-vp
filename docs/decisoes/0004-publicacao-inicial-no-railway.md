# ADR 0004 — Publicação inicial no Railway

## Status

Aceita.

## Contexto e decisão

O portfólio continua publicado em um único container, com o Angular servido
pelo Spring Boot e o PostgreSQL hospedado no Neon. O Railway substitui o Render
como plataforma do container porque o crédito inicial e o modo Serverless são
adequados à demonstração esporádica, preservando deploy Docker, TLS gerenciado,
healthcheck e integração com os checks do GitHub sem introduzir nova arquitetura.

O serviço usa o plano gratuito enquanto o consumo couber no crédito disponível.
Depois do período inicial, a demonstração pode ficar indisponível ao esgotar o
crédito mensal; nesse caso, a primeira opção é reduzir o tempo ativo com
Serverless e a segunda é promover o serviço para o plano Hobby. O banco permanece
no Neon, com papéis distintos para Flyway e runtime e TLS com verificação do
hostname.

## Consequências

- O modo Serverless pode causar cold start e depende de ausência de tráfego de
  saída. O pool libera conexões ociosas, mas outro tráfego da JVM ainda pode
  impedir o repouso, que permanece uma otimização de melhor esforço.
- O ambiente gratuito é uma demonstração pública, não produção com SLA.
- O plano Free restringe novos deploys em horários de pico da região escolhida.
- O Railway publica a `main` somente com **Wait for CI** habilitado.
- Um rollback do container não desfaz migrations nem dados.
- A demonstração não compartilha banco nem credenciais com dados reais.

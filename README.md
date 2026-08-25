# gestorVP

> Gestor de Validade de Produtos · Controle de validade sem complicação.

O **gestorVP** ajuda pequenos mercados, mercearias e comércios de bairro a
identificar produtos próximos do vencimento e registrar entradas e retiradas
pelo celular.

O projeto está sendo construído com foco em pessoas com pouca familiaridade
com tecnologia. Por isso, prioriza linguagem simples, poucos passos e uma
interface mobile-first.

## Problema que o projeto resolve

Em pequenos estabelecimentos, a validade das mercadorias costuma ser
controlada visualmente, em cadernos ou planilhas. Isso pode causar desperdício,
prejuízo e produtos vencidos nas prateleiras.

O gestorVP deve responder rapidamente:

- O que já venceu?
- O que vence nesta semana?
- Qual produto deve sair primeiro?
- Quantas unidades ainda estão disponíveis?
- O que foi vendido, utilizado, doado ou perdido?

## MVP

- Cadastro de produtos com quantidade e data de validade.
- Classificação automática por urgência.
- Busca e consulta do estoque ativo.
- Retirada de unidades com motivo.
- Histórico de entradas e retiradas.
- Experiência responsiva para celular.

O escopo detalhado está em [docs/produto.md](docs/produto.md).

## Tecnologias

### Backend

- Java 21
- Spring Boot 4.1
- Spring Data JPA
- PostgreSQL e Flyway
- Spring Security
- Actuator
- JUnit e Testcontainers

### Frontend

- Angular 21
- TypeScript
- Componentes standalone
- TypeScript estrito
- Vitest e Prettier
- Abordagem mobile-first

### Infraestrutura

- Docker Compose
- GitHub Actions
- Playwright
- Railway e Neon para a demonstração publicada

Angular Material, PWA e OpenAPI não são requisitos do MVP. Essas tecnologias
serão reavaliadas somente quando trouxerem benefício concreto ao produto.

## Estrutura do repositório

```text
gestor-vp/
├── backend/        # API Spring Boot
├── frontend/       # Aplicação Angular
├── docs/           # Produto e decisões técnicas
├── compose.yaml    # PostgreSQL para desenvolvimento local
└── README.md
```

## Status

🚧 Em desenvolvimento — base técnica executável, cadastro de entradas,
consulta de detalhes e retirada de unidades publicados no ambiente de
demonstração.

A versão atual está disponível em
[gestor-vp-production.up.railway.app](https://gestor-vp-production.up.railway.app/).
Como o serviço usa o modo Serverless do Railway, o primeiro acesso após um
período de inatividade pode levar alguns segundos.

## Executar localmente

Pré-requisitos:

- Java 21;
- Node.js 20.19 ou superior da linha 20;
- Docker Desktop.

Inicie o PostgreSQL na raiz do projeto:

```powershell
docker compose up -d database
```

Em um terminal, execute a API:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Em outro terminal, execute o frontend:

```powershell
cd frontend
npm ci
npm start
```

- Frontend: `http://localhost:4200`
- Saúde da API: `http://localhost:8080/actuator/health`

Para personalizar o PostgreSQL executado pelo Docker Compose, copie
`.env.example` para `.env`. O arquivo `.env` não deve ser versionado.

A mudança do nome do projeto Compose cria o volume local
`gestor-vp_postgres-data`. O volume usado antes da renomeação não é apagado,
mas também não é montado automaticamente. Exporte os dados que desejar manter
antes de subir a nova composição; os dados sintéticos podem ser recriados.

O Spring Boot não carrega esse arquivo automaticamente. Antes de iniciar a API,
defina `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SERVER_PORT`, `APP_USERNAME` e
`APP_PASSWORD` no terminal que executará o Maven. Por exemplo, no PowerShell:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/gestor_vp"
$env:DB_USERNAME = "gestor_vp"
$env:DB_PASSWORD = "gestor_vp_local"
$env:SERVER_PORT = "8080"
$env:APP_USERNAME = "operador"
$env:APP_PASSWORD = "troque-esta-senha-local"

cd backend
.\mvnw.cmd spring-boot:run
```

Ao alterar banco, usuário, senha ou porta do PostgreSQL, mantenha as variáveis
`DB_*` correspondentes alinhadas com os valores `POSTGRES_*` do Compose.

## Qualidade e integração contínua

O GitHub Actions executa, a cada pull request:

- testes do backend com PostgreSQL real via Testcontainers;
- verificação de formatação do frontend;
- testes unitários do Angular;
- build de produção do frontend.
- fluxo E2E de login, cadastro e listagem no container publicado.

## Executar como a aplicação publicada

O build de publicação reúne Angular e Spring Boot em um único container. Para
compilar e iniciar essa topologia localmente:

```powershell
docker compose --profile application up --build --wait
```

A aplicação completa ficará em `http://localhost:8080`. Para encerrá-la:

```powershell
docker compose --profile application down
```

O arquivo `railway.json` descreve o build Docker, o healthcheck e a política de
reinício no Railway. O banco remoto é criado separadamente no Neon, com uma
credencial para o Flyway e outra, de privilégios mínimos, para a aplicação.
URLs JDBC remotas usam `sslmode=verify-full` e a fábrica SSL padrão do Java para
validar a cadeia de certificados com o truststore da imagem. O Railway termina
TLS na borda e, com **Wait for CI**, só publica a `main` depois que os checks do
GitHub forem aprovados. O provisionamento completo está em
[docs/publicacao.md](docs/publicacao.md).

## Fluxo de desenvolvimento

Toda alteração é desenvolvida em uma branch isolada e integrada à `main` por
Pull Request depois da revisão e da aprovação do pipeline. As convenções de
branches, commits e merge estão em [CONTRIBUTING.md](CONTRIBUTING.md).

## Fluxos funcionais disponíveis

A versão publicada permite:

- cadastrar uma entrada de produto e exibi-la na lista em ordem de validade;
- carregar o estoque ativo por cursor, sem deslocar as entradas posteriores
  quando o estoque muda entre os carregamentos; novas entradas anteriores ao
  cursor aparecem após atualizar a lista;
- abrir uma entrada para consultar produto, validade, situação e quantidades
  inicial e disponível;
- retirar unidades pelos motivos **Venda**, **Uso**, **Doação**, **Perda** ou
  **Vencimento**, depois de revisar e confirmar a operação;
- encerrar uma entrada quando a quantidade disponível chega a zero;
- buscar na área **Produtos** por parte do nome, ignorando caixa, acentos e
  espaços excedentes, e combinar a busca com filtros de situação;
- consultar entradas e retiradas no **Histórico**, com os registros recentes
  primeiro e carregamento paginado;
- abrir pelo Histórico uma entrada encerrada em modo somente leitura.

Retiradas acima do saldo são rejeitadas integralmente. Para entradas vencidas,
somente os motivos **Perda** e **Vencimento** ficam disponíveis. Cada retirada
atualiza o saldo de forma atômica e preserva uma movimentação imutável para o
histórico. Cada linha do Histórico identifica produto, validade, tipo,
quantidade, data e hora e, nas retiradas, o motivo aplicável.

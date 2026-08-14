# Vence Fácil

> Controle de validade sem complicação.

O **Vence Fácil** ajuda pequenos mercados, mercearias e comércios de bairro a
identificar produtos próximos do vencimento e registrar entradas e retiradas
pelo celular.

O projeto está sendo construído com foco em pessoas com pouca familiaridade
com tecnologia. Por isso, prioriza linguagem simples, poucos passos e uma
interface mobile-first.

## Problema que o projeto resolve

Em pequenos estabelecimentos, a validade das mercadorias costuma ser
controlada visualmente, em cadernos ou planilhas. Isso pode causar desperdício,
prejuízo e produtos vencidos nas prateleiras.

O Vence Fácil deve responder rapidamente:

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
- OpenAPI/Swagger

Spring Security, Angular Material, PWA e OpenAPI serão adicionados quando o
fluxo funcional que realmente os utiliza for implementado.

## Estrutura do repositório

```text
vence-facil/
├── backend/        # API Spring Boot
├── frontend/       # Aplicação Angular
├── docs/           # Produto e decisões técnicas
├── compose.yaml    # PostgreSQL para desenvolvimento local
└── README.md
```

## Status

🚧 Em desenvolvimento — base técnica executável e primeiro fluxo funcional em
preparação.

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

O Spring Boot não carrega esse arquivo automaticamente. Antes de iniciar a API,
defina `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` e `SERVER_PORT` no terminal que
executará o Maven. Por exemplo, no PowerShell:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/vence_facil"
$env:DB_USERNAME = "vence_facil"
$env:DB_PASSWORD = "vence_facil_local"
$env:SERVER_PORT = "8080"

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

## Fluxo de desenvolvimento

Toda alteração é desenvolvida em uma branch isolada e integrada à `main` por
Pull Request depois da revisão e da aprovação do pipeline. As convenções de
branches, commits e merge estão em [CONTRIBUTING.md](CONTRIBUTING.md).

## Próximo incremento

O primeiro fluxo vertical será:

> Cadastrar uma entrada de produto e exibi-la na lista em ordem de validade.

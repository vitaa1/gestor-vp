# Documento do produto

## 1. Visão

O gestorVP é um sistema simples para ajudar pequenos comerciantes a
controlar a validade dos produtos do estoque.

O objetivo não é substituir um ERP. O produto deve resolver bem um único
problema: **evitar que mercadorias sejam esquecidas no estoque e acabem
vencendo**.

## 2. Público-alvo

Proprietários e funcionários de pequenos mercados, mercearias, padarias e
estabelecimentos semelhantes, especialmente pessoas que:

- utilizam principalmente o celular;
- não possuem experiência com sistemas de estoque complexos;
- precisam cadastrar produtos rapidamente;
- não conhecem termos técnicos de logística;
- querem informações claras sem configurar relatórios.

### Persona principal

**Antônio, proprietário de uma mercearia de bairro.**

Antônio administra o estabelecimento, atende clientes e organiza o estoque.
Ele usa o celular para tarefas cotidianas, mas não tem familiaridade com
sistemas empresariais. Precisa saber o que está próximo de vencer sem navegar
por menus complexos ou preencher formulários extensos.

## 3. Proposta de valor

> Em poucos segundos, o comerciante registra uma mercadoria e descobre o que
> precisa ser vendido ou utilizado primeiro.

## 4. Princípios do produto

### Simplicidade

Cada funcionalidade deve resolver um problema real. Recursos que aumentem a
complexidade sem benefício evidente ficam fora do MVP.

### Linguagem comum

O sistema não deve apresentar termos como FEFO, movimentação de estoque,
baixa operacional ou persistência de dados.

Exemplos:

- **Retirar unidades** em vez de **Registrar movimentação de saída**;
- **Você possui apenas 8 unidades** em vez de **Saldo insuficiente**;
- **Produto adicionado!** em vez de **Registro persistido com sucesso**.

### Uso pelo celular

A interface deve ser responsiva, possuir botões grandes, fontes legíveis e
poucos elementos por tela.

### Poucos passos

O cadastro básico solicita apenas nome, quantidade e validade. Informações
adicionais são opcionais.

### Complexidade invisível

Conceitos como lotes e ordenação por validade existem internamente, sem
obrigar o usuário a compreender regras técnicas.

## 5. Escopo do MVP

### Início

A página inicial responde: **O que precisa da minha atenção?**

Ela apresenta:

- produtos vencidos;
- produtos que vencem nos próximos 7 dias;
- produtos que vencem nos próximos 30 dias;
- produtos sem risco próximo;
- resumo de até cinco itens que exigem atenção;
- acesso à consulta completa de produtos.

Os cinco produtos mais urgentes aparecem primeiro. A opção **Produtos** mantém
a consulta completa, com busca, filtros e paginação.

### Adicionar produto

Campos obrigatórios:

- nome;
- quantidade;
- data de validade.

Campos opcionais em **Mais detalhes**:

- código de barras numérico de 8 a 14 dígitos, único por produto;
- categoria textual do produto;
- preço de custo unitário em reais;
- fornecedor textual da entrada;
- número do lote textual da entrada, sem unicidade global.

### Consultar produtos

A listagem permite:

- buscar pelo nome;
- visualizar quantidade e validade;
- identificar a situação por texto e cor;
- filtrar vencidos ou próximos da validade;
- abrir os detalhes de uma entrada.

Exemplo:

> Leite Integral<br>
> 12 unidades - vence em 6 dias<br>
> Atenção

### Retirar unidades

O usuário informa a quantidade e um motivo:

- venda;
- uso;
- doação;
- perda;
- vencimento.

A retirada parte de uma entrada identificada pelo produto e pela validade. A
quantidade disponível é atualizada e a operação permanece no histórico.

### Histórico

O histórico apresenta as entradas e retiradas mais recentes, com produto,
quantidade, data e motivo quando aplicável.

## 6. Regras de negócio

1. Cada entrada possui quantidade e data de validade próprias.
2. Um produto pode possuir várias entradas com validades diferentes.
3. As entradas que vencem primeiro aparecem primeiro.
4. Uma entrada com data passada é classificada como **Vencido**.
5. Uma entrada que vence em até 7 dias é classificada como **Atenção**.
6. Uma entrada que vence entre 8 e 30 dias é classificada como **Fique de olho**.
7. Uma entrada que vence em mais de 30 dias é classificada como **Tudo certo**.
8. A quantidade retirada não pode superar a quantidade disponível.
9. Uma entrada com quantidade zero não aparece no estoque ativo.
10. Toda entrada e retirada é registrada no histórico.
11. Cores são acompanhadas de textos ou ícones.
12. Uma retirada exibe um resumo antes da confirmação.
13. Uma retirada é aplicada integralmente ou rejeitada, sem atualização
    parcial quando o saldo é insuficiente.
14. Entradas vencidas não podem ser retiradas pelos motivos **Venda** ou
    **Doação**.
15. A busca por nome ignora maiúsculas, minúsculas, acentos e espaços
    excedentes.
16. O nome normalizado é único; o código de barras também é único quando
    informado.
17. Entradas encerradas permanecem disponíveis no histórico em modo somente
    leitura.
18. A situação da validade e a contagem de dias consideram a data local do
    operador, obtida automaticamente do dispositivo que acessa o sistema.
19. As regras que autorizam retiradas de entradas vencidas consideram a data
    operacional do estabelecimento, definida por configuração protegida.

## 7. Histórias de usuário

### HU01 — Visualizar itens urgentes

Como comerciante, quero abrir o sistema e ver os produtos que exigem atenção
para decidir o que devo vender ou utilizar primeiro.

Critérios de aceitação:

- vencidos aparecem antes dos demais;
- itens próximos da validade aparecem em ordem de urgência;
- a página inicial mostra no máximo cinco entradas;
- um atalho permite abrir a consulta completa de produtos;
- cada item mostra nome, quantidade, data e situação;
- a situação não depende apenas de cor.

### HU02 — Registrar uma entrada

Como comerciante, quero cadastrar uma mercadoria com poucos dados para manter
o estoque atualizado sem perder tempo.

Critérios de aceitação:

- nome, quantidade e validade são obrigatórios;
- a quantidade deve ser maior que zero;
- a data deve ser válida;
- o sistema confirma a inclusão com texto e ícone de sucesso;
- detalhes adicionais são opcionais.

### HU03 — Buscar um produto

Como comerciante, quero buscar um produto pelo nome para consultar rapidamente
sua quantidade e validade.

Critérios de aceitação:

- a busca aceita parte do nome;
- mostra todas as entradas ativas encontradas;
- apresenta uma mensagem amigável quando não há resultado.

### HU04 — Retirar unidades

Como comerciante, quero informar que unidades saíram do estoque para manter a
quantidade correta.

Critérios de aceitação:

- o usuário informa quantidade e motivo;
- não é possível retirar mais que o disponível;
- a nova quantidade aparece imediatamente;
- a operação fica no histórico;
- é possível cancelar antes da confirmação.
- a retirada atua sobre a entrada escolhida pelo produto e pela validade;
- entradas vencidas aceitam apenas os motivos **Perda** ou **Vencimento**.

### HU05 — Consultar histórico

Como comerciante, quero visualizar entradas e retiradas recentes para entender
o que aconteceu com o estoque.

Critérios de aceitação:

- cada registro mostra produto, operação, quantidade, data e motivo;
- registros recentes aparecem primeiro;
- o histórico não pode ser alterado no MVP.

## 8. Telas

1. Login.
2. Início.
3. Adicionar produto.
4. Lista de produtos.
5. Detalhes do produto.
6. Retirar unidades.
7. Histórico.

A navegação principal possui no máximo três opções visíveis: **Início**,
**Produtos** e **Histórico**. O botão **Adicionar produto** permanece em
destaque. Cada seção possui uma URL própria: `/` para **Início**, `/produtos`
para **Produtos** e `/historico` para **Histórico**. A URL acompanha a tela
ativa para permitir atualização, compartilhamento do endereço e uso dos
controles de voltar e avançar do navegador.

## 9. Fora do MVP

- microsserviços;
- RabbitMQ ou Kafka;
- inteligência artificial;
- integração automática com WhatsApp;
- leitura de código de barras pela câmera;
- múltiplos estabelecimentos por usuário;
- diferentes níveis de permissão;
- relatórios contábeis;
- emissão de nota fiscal;
- controle de vendas ou fluxo de caixa;
- aplicativo móvel nativo.
- edição ou exclusão de produtos, entradas e movimentações;
- correção retroativa de estoque;
- adoção de OpenAPI/Swagger;
- adoção de Angular Material;
- transformação da aplicação em PWA.

Esses itens somente serão considerados após o fluxo principal estar completo,
testado e publicado.

## 10. Stack planejada

### Frontend

- Angular e TypeScript;
- formulários reativos;
- layout responsivo e mobile-first.

### Backend

- Java 21 e Spring Boot;
- Spring Web;
- Spring Data JPA;
- Bean Validation;
- Spring Security.

### Dados, infraestrutura e testes

- PostgreSQL e Flyway;
- Docker Compose;
- GitHub Actions;
- JUnit 5, Mockito e Testcontainers;
- testes de componentes Angular;
- Playwright para fluxos ponta a ponta com frontend, backend e PostgreSQL
  reais.

## 11. Arquitetura

O backend será um **monólito modular**, inicialmente dividido em:

- autenticação;
- produtos;
- estoque;
- histórico.

O MVP será publicado por uma única aplicação Spring Boot e utilizará um único
banco PostgreSQL.

### Convenções da API

- endpoints das funcionalidades do produto usam o prefixo `/api/v1/**`;
- endpoints em `/api/**` exigem autenticação;
- saúde e informações mínimas de build permanecem em `/actuator/health` e
  `/actuator/info`;
- migrations são executadas internamente pelo Flyway e não possuem endpoint;
- uma futura versão incompatível da API recebe um novo prefixo, sem alterar o
  significado de `/api/v1/**` silenciosamente.

## 12. Modelo conceitual

### Operador

- uma única identidade por implantação durante o MVP;
- credenciais configuradas por ambiente e não persistidas no banco;
- acesso a todo o estoque do estabelecimento representado pela implantação.

### Produto

- identificador;
- nome único normalizado;
- categoria opcional;
- código de barras opcional e único;
- data de criação.

### Entrada de estoque

- identificador;
- produto;
- quantidade inicial;
- quantidade disponível;
- data de validade;
- preço de custo opcional;
- fornecedor opcional;
- número do lote opcional;
- data de criação.

### Movimentação

- identificador;
- entrada de estoque;
- tipo: entrada ou retirada;
- quantidade;
- motivo opcional;
- data e hora.

## 13. Definição de pronto do MVP

O MVP estará concluído quando:

- um usuário conseguir acessar a aplicação publicada;
- for possível cadastrar uma entrada pelo celular;
- a entrada aparecer na classificação correta;
- for possível buscar produtos e retirar unidades;
- a quantidade for atualizada corretamente;
- entradas e retiradas aparecerem no histórico;
- os fluxos principais possuírem testes automatizados;
- o projeto puder ser executado localmente com instruções claras;
- existir uma demonstração publicada com HTTPS e dados restauráveis;
- o repositório possuir capturas de tela e a credencial intencionalmente
  pública e exclusiva da demonstração.

O primeiro ambiente remoto utiliza uma única implantação de demonstração. A
promoção pública no portfólio ocorre somente após HU01–HU05 e os testes ponta a
ponta estarem concluídos. Esse ambiente contém somente dados sintéticos e
descartáveis; nenhuma credencial ou dado real pode ser reutilizado nele.

## 14. Indicadores de simplicidade

- cadastrar uma entrada leva menos de 30 segundos;
- a tela inicial comunica itens urgentes sem navegação adicional;
- o fluxo principal funciona em uma tela de celular;
- um novo usuário consegue utilizar o sistema sem treinamento;
- os formulários explicam erros em linguagem comum.

## 15. Primeiro incremento

O primeiro fluxo vertical do produto será:

```text
Cadastrar entrada
        ↓
Informar nome, quantidade e validade
        ↓
Salvar
        ↓
Exibir na lista em ordem de validade
```

Esse incremento deve incluir backend, banco, interface e testes suficientes
para validar o fluxo de ponta a ponta antes da inclusão de novas funções.

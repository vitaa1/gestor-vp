# Estoque do gestorVP

O gestorVP — Gestor de Validade de Produtos — usa este contexto para descrever
a linguagem de controle de produtos, validades e quantidades em um pequeno
estabelecimento.

## Linguagem

**Estabelecimento**:
O pequeno comércio representado por uma implantação do gestorVP e ao qual
pertencem todos os dados de estoque dessa implantação.
_Evitar_: Tenant, conta, organização

**Operador**:
A única identidade que acessa uma implantação durante o MVP, compartilhando a
visão completa do estoque do estabelecimento.
_Evitar_: Usuário, administrador

**Data local do operador**:
O dia civil no local em que o operador acessa a implantação, usado para exibir
a situação da validade e a contagem de dias.
_Evitar_: Data do servidor, horário do servidor

**Data operacional do estabelecimento**:
O dia civil no fuso configurado para o estabelecimento, usado nas regras que
autorizam retiradas de entradas vencidas.
_Evitar_: Data do operador, data do servidor

**Produto**:
A mercadoria identificada por um nome único e, quando informado, por um código
de barras único, que reúne entradas de estoque com quantidades e validades
próprias.
_Evitar_: Item, lote

**Código de barras**:
O identificador numérico opcional e único de um produto, preservado como foi
impresso, inclusive quando começa com zero.
_Evitar_: Código do produto, identificador da entrada

**Categoria do produto**:
Uma classificação textual opcional usada para organizar produtos semelhantes.
_Evitar_: Tipo, grupo

**Entrada de estoque**:
Um conjunto de unidades de um produto que compartilha a mesma validade e cuja
quantidade disponível é controlada separadamente das demais entradas.
_Evitar_: Produto, estoque, lote

**Estoque ativo**:
O conjunto de entradas de estoque que ainda possuem quantidade disponível.
_Evitar_: Todos os produtos, histórico

**Quantidade disponível**:
O número de unidades que permanece em uma entrada de estoque após suas
retiradas.
_Evitar_: Quantidade, saldo do produto

**Preço de custo**:
O valor opcional em reais pago por uma unidade pertencente a uma entrada de
estoque.
_Evitar_: Preço de venda, custo total

**Fornecedor**:
A identificação textual opcional de quem forneceu uma entrada de estoque.
_Evitar_: Fabricante

**Número do lote**:
O identificador textual opcional atribuído pelo fornecedor a uma entrada de
estoque, sem unicidade global no estabelecimento.
_Evitar_: Entrada, código de barras

**Retirada**:
A remoção de unidades de uma entrada de estoque específica, identificada para
o operador pelo produto e pela validade.
_Evitar_: Saída, baixa, retirada do produto

**Motivo da retirada**:
A causa informada para uma retirada, limitada a venda, uso, doação, perda ou
vencimento.
_Evitar_: Observação, comentário, motivo livre

**Movimentação**:
O registro imutável da criação ou de uma retirada realizada sobre uma entrada
de estoque.
_Evitar_: Histórico, edição, operação

**Entrada encerrada**:
Uma entrada de estoque sem quantidade disponível, preservada para consulta por
meio do histórico, mas ausente do estoque ativo.
_Evitar_: Entrada excluída, produto removido

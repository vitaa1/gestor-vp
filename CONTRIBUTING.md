# Como contribuir

O gestorVP utiliza desenvolvimento baseado em branches e Pull Requests.
Alterações não devem ser desenvolvidas ou commitadas diretamente na `main`.

## Fluxo de trabalho

1. Atualize a `main` local:

   ```powershell
   git switch main
   git pull --ff-only
   ```

2. Crie uma branch curta e focada:

   ```powershell
   git switch -c feat/nome-da-funcionalidade
   ```

   Para alterações realizadas pelo Codex, use o prefixo `codex/`, seguido do
   tipo e do objetivo, como `codex/feat-cadastro-entrada`.

3. Implemente e execute as verificações locais relacionadas à mudança.

   Para regras de domínio, segurança, contratos HTTP e correções de bugs,
   prefira TDD: escreva primeiro um teste que reproduza o comportamento
   esperado e falhe pelo motivo correto; implemente o mínimo para fazê-lo
   passar; depois refatore mantendo a suíte verde. Configurações de Docker, CI
   e publicação devem possuir validação executável e cobertura E2E quando não
   couber um teste unitário.

4. Faça commits pequenos com mensagens no padrão Conventional Commits:

   ```text
   feat: adiciona cadastro de entrada
   fix: impede retirada acima do estoque
   test: cobre classificação por validade
   docs: documenta execução local
   chore: configura integração contínua
   ```

5. Envie a branch ao GitHub:

   ```powershell
   git push -u origin nome-da-branch
   ```

6. Abra um Pull Request para a `main`, descrevendo objetivo, principais
   decisões e como a alteração foi testada.

7. Faça o merge somente depois que a revisão estiver concluída e o pipeline
   estiver aprovado. Após o merge, exclua a branch remota e a local.

## Regras para Pull Requests

- Cada PR deve possuir um único objetivo claro.
- A descrição deve informar o que mudou e como validar.
- Testes e documentação devem acompanhar a implementação quando aplicável.
- O pipeline de integração contínua deve estar aprovado.
- Mudanças de banco devem incluir uma migration Flyway.
- Credenciais, arquivos `.env` e outros segredos não podem ser versionados.
- O merge deve preservar uma `main` sempre executável.

## Estratégia de merge

Preferimos **Squash and merge** para manter um commit coeso por Pull Request na
`main`. O título do PR deve seguir o padrão Conventional Commits, pois será
utilizado como mensagem do commit resultante.

## Revisão com agentes do Codex

Antes de abrir o Pull Request, o projeto utiliza agentes especializados em
modo somente leitura:

- `code-reviewer`: bugs, regressões, arquitetura, banco, frontend e testes;
- `security-guard`: autenticação, autorização, segredos, injeções, dados,
  dependências e infraestrutura.

Mudanças de código passam pelo `code-reviewer`. Mudanças que afetem backend,
dados, configuração, dependências, Docker ou CI passam também pelo
`security-guard`. Achados críticos devem ser resolvidos e revisados novamente
antes do merge.

Exemplo de solicitação:

```text
Revise esta branch contra a main usando code-reviewer e security-guard em
paralelo. Aguarde os dois e consolide os achados por gravidade.
```

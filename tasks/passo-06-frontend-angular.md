# Passo 6 — Frontend Angular: autenticação e peladas

**Status:** roteiro do passo 5 concluído — login, cadastro, lista, detalhe, sorteio, súmula e rankings
**Base:** commit `e49c1ed` (documentação OpenAPI)

---

## 1. Objetivo

Fechar a Etapa 5 do [Passo 5](passo-05-infraestrutura-seguranca-e-frontend.md),
a última do roteiro. A ordem defendida lá se cumpriu: o frontend chegou depois
de ambiente reprodutível, schema versionado, autenticação definida e **contrato
formal** — e é esse contrato que dispensou reescrever os 28 DTOs à mão.

Escopo entregue: **login, cadastro, lista de peladas com filtros e paginação,
detalhe com confirmação de presença, sorteio de times, súmula e rankings** —
o ciclo completo da pelada, do cadastro do jogador à classificação da liga.

---

## 2. As três decisões

| Decisão | Escolha | Por quê |
|---|---|---|
| Framework | **Angular 22** | Estruturado e opinativo, filosofia próxima do Spring (DI, camadas). Standalone components, signals e `resource()` — nada de NgModule |
| Cliente HTTP | **Gerado do OpenAPI** (`ng-openapi-gen` 1.0.5) | Era o motivo declarado da Etapa 4. Modelo e endpoint mudam no backend e o front quebra no *build*, não em produção |
| Estilo | **Tailwind CSS 4** | `ng add tailwindcss` já escreve o `.postcssrc.json`. Sem inventar um design system nem manter CSS por tela |

Angular roda na **4200**, então o padrão do `CORS_ORIGENS` — que apontava para a
5173 do Vite, de quando a stack ainda não estava decidida — passou a apontar
para a porta certa.

---

## 3. O gate do gerador, e por que ele veio primeiro

`ng-openapi-gen` declara suporte a OpenAPI 3.1 e Angular 16+, mas a última
publicação é de **novembro de 2025**, antes do Angular 22. Em vez de apostar,
repetiu-se a disciplina que funcionou no passo anterior (springdoc × Jackson 3):
**criar o projeto, gerar o cliente e rodar `ng build` antes de escrever a
primeira tela.**

Passou de primeira. E o gate pagou por si de outro jeito: ao ler o código
gerado, apareceram **dois defeitos no contrato** que ninguém veria pelo Swagger
UI.

### 3.1 `listar1`, `buscarPorId1`, `atualizar1`

Sem `operationId` explícito, o springdoc deriva o nome do método Java e
desempata com sufixo numérico. O cliente gerado herdava isso: `buscarPorId1()`
era o detalhe da pelada, e `listar2()` a súmula.

Pior que feio: o sufixo depende da **ordem de varredura dos controllers**.
Acrescentar um método em outra classe poderia renomear a função que o frontend
chama. Os 25 endpoints ganharam `@Operation(operationId = "...")`, e
`DocumentacaoOpenApiTest` agora trava os nomes.

### 3.2 O contrato dizia que a resposta era binária

Mais sério. O cliente gerado montava as chamadas com:

```ts
rb.build({ responseType: 'blob', accept: '*/*' })
```

Ou seja: o front receberia um `Blob` em vez do objeto. A causa é que
`@RestController` **sem `produces`** faz o springdoc publicar o media type
curinga:

```json
"200": { "content": { "*/*": { "schema": { "$ref": "…/PageResponsePeladaResumoResponse" } } } }
```

O schema estava certo — só o tipo de mídia estava vago, e o gerador tomou a
decisão conservadora. Com `produces = APPLICATION_JSON_VALUE` no
`@RequestMapping` dos 6 controllers, virou `responseType: 'json'`.

**A lição de método:** o Swagger UI escondia os dois problemas, porque ele
*interpreta* o contrato com tolerância. Um gerador de código não interpreta — e
é por isso que gerar o cliente é também um teste do contrato. Os dois casos
viraram teste no backend.

---

## 4. Autenticação no front

O `sub` do token é o id do usuário (ver `TokenService.java`), e é dele que
dependem "sou o organizador desta pelada?" e "já estou escalado?". O
`AuthService` decodifica a carga do JWT — base64url com `TextDecoder`, porque
`atob` sozinho estraga nickname com acento — e expõe `usuarioId`, `nickname` e
`autenticado` como **signals**.

Três cuidados que evitam bug silencioso:

1. **Token expirado é descartado na inicialização.** Sem isso o app abre
   "logado" e só descobre o contrário no primeiro 401.
2. **O 401 é tratado no interceptor**, em um lugar só: derruba a sessão e leva
   para `/login?redirect=<rota>`. Nenhuma tela trata 401 por conta própria.
3. **Toda leitura de `localStorage` é protegida**, porque em aba anônima com
   dados bloqueados o acessor lança exceção.

> **Limitação assumida:** o token vive no `localStorage` e portanto é
> vulnerável a XSS. É a escolha pragmática para uma SPA sem
> *backend-for-frontend*, e está registrada nas *Limitações conhecidas* do
> README ao lado de "sem HTTPS" e "sem refresh token".

---

## 5. As telas

A URL é a **fonte de verdade dos filtros** da listagem: recarregar a página ou
mandar o link para alguém reproduz a mesma busca, e o botão voltar funciona.
O formulário é reabastecido a partir da query string, nunca o contrário.

Nenhuma conta de negócio foi refeita no front. Vagas restantes, total de
confirmados e promoção da lista de espera vêm calculados do backend
(`PeladaResumoResponse`, `PeladaResponse`), e **toda ação recarrega a pelada**
em vez de ajustar o estado local — duas verdades sobre a mesma escalação seria
o começo de uma divergência.

A rota `/minhas-peladas` reaproveita o componente da listagem fixando
`organizadorId` com o id do token. É o que dá uso real ao `authGuard`: as
listagens e o detalhe são públicos na API, e continuam públicos aqui.

### 5.1 Sorteio

A regra que molda a tela inteira está em `SorteioServiceImpl`: **só o
organizador sorteia** (403 para os demais), **só quem confirmou presença
entra** — convidado e lista de espera não são jogadores da pelada ainda — e o
critério é `quantidadeTimes` **ou** `jogadoresPorTime`, nunca os dois (o
`@AssertTrue` de `SorteioRequest` recusa o par, com 400).

Três decisões de interface saíram daí:

1. **Quem não organiza vê o motivo, não um botão que falha.** A tela checa o
   organizador com o id do token e mostra de quem é a pelada.
2. **Prévia antes de chamar.** Com os confirmados em mãos, a tela já diz
   "vai formar 2 times de 5 e deixar 1 de reserva", e desabilita o botão quando
   a divisão é impossível. O backend continua sendo quem decide — a prévia só
   evita que o organizador descubra no erro.
3. **A semente é exposta.** Ela volta na resposta, aparece embaixo do
   resultado e alimenta "Repetir este sorteio"; "Sortear de novo" limpa o
   campo. É o que transforma um detalhe do algoritmo em recurso de uso real:
   refazer no celular a divisão já combinada em quadra.

O resultado mostra os times lado a lado com soma e média de estrelas, marca o
goleiro (🧤), avisa quando um time ficou **sem goleiro**, lista as reservas
explicando por que elas existem (time com um jogador a mais nasce em vantagem)
e classifica a diferença de estrelas entre o time mais forte e o mais fraco.

Construir a tela também expôs uma aspereza no backend, **já corrigida**: pedir
6 jogadores por time com 10 confirmados caía no primeiro ramo de
`validarDivisao` e respondia *"é preciso pelo menos 4 para formar 2 times de
2"* — verdadeiro, e inútil para quem pediu times de 6. A mensagem agora cita o
critério pedido:

```
Para formar times de 6 jogadores são necessários pelo menos 12 confirmados,
já que o sorteio precisa de ao menos 2 times, mas a pelada tem 10
```

No mesmo lugar havia um ramo **inalcançável** — "confirmados a menos do que
times × jogadores por time". Ele não podia acontecer: o valor deduzido vem
sempre de uma divisão inteira, e o resto vira reserva. Saiu, e o invariante
ficou escrito no Javadoc. `SorteioServiceImplTest` cobre as duas mensagens,
a posse, o status e o repasse da semente.

### 5.2 Súmula

O mesmo par de regras do backend molda a tela: **só o organizador lança**, e a
pelada precisa estar **em andamento ou finalizada** — antes disso a tela
explica isso em vez de oferecer um formulário que daria 409.

A decisão de interface que mais economiza erro é a **ficha por posição**:
defesa e defesa difícil só existem para quem pegou o gol, desarme é de jogador
de linha (`AtributoPontuacao.exclusivoDeGoleiro` / `exclusivoDeLinha`). Em vez
de mostrar cinco campos e deixar a API recusar, a tela troca os campos quando a
posição jogada muda, e zera no envio o que não vale para aquela posição. O
seletor de jogador também só lista **confirmados**, que é quem pode ter súmula.

Lançar de novo corrige o anterior — o `PUT` é idempotente por jogador — então
"Editar" só recarrega a linha no mesmo formulário. Clicar no nickname abre o
`detalhamento`, a quebra que mostra de onde vieram os pontos (`2 × 10 = 20`)
em vez de só o total.

### 5.3 Rankings

Tela pública, com o mesmo padrão de filtro na URL: `peladaId` recorta a
classificação a uma pelada e `limite` corta o tamanho das listas. As duas
consultas saem juntas — a classificação geral e os destaques, que trazem o
topo de cada atributo em **uma chamada só** (é para isso que
`DestaqueResponse` existe).

A tabela de pontuação aparece escrita na tela (gol 10, defesa difícil 8,
assistência 7, defesa 4, desarme 3), e o rodapé explica o critério de empate
que o backend já aplica: colocação dividida e a seguinte pulada (1º, 2º, 2º,
4º). Nada disso é recalculado no front.

---

## 6. Testes

44 testes em Vitest + jsdom, sem browser e sem backend. Eles cobrem justamente
o que quebra calado:

| Suíte | Cobre |
|---|---|
| `auth.service.spec.ts` | Id e nickname lidos do `sub`, nickname com acento, token expirado ou malformado descartado, logout limpando o storage |
| `auth.interceptor.spec.ts` | Header anexado só quando há sessão, logout no 401, sessão preservada nos demais erros |
| `peladas-lista.spec.ts` | Query string virando parâmetro da chamada, formulário reabastecido pela URL, estado vazio |
| `pelada-detalhe.spec.ts` | Escalação agrupada, ação bloqueada para quem não está logado, `POST` com o id do token, recarga após a ação, controles só para o organizador, erro 409 visível na tela |
| `sorteio.spec.ts` | Formulário escondido de quem não organiza, prévia da divisão, envio de **um só** critério, times/goleiro/reservas/semente desenhados, repetição pela semente, 409 na tela e botão travado com poucos confirmados |
| `sumula.spec.ts` | Leitura pública sem formulário, quebra de pontos, aviso de pelada que não rolou, só confirmados no seletor, ficha trocando com a posição, `PUT` zerando o que não vale, `DELETE` e 409 na tela |
| `rankings.spec.ts` | Limite padrão, recorte por pelada vindo da URL, classificação e destaques desenhados, e o estado vazio explicando que falta lançar súmula |
| `app.spec.ts` | Header alternando entre "Entrar" e o nickname |

Uma armadilha vale registro: `fixture.whenStable()` **trava** quando há
requisição pendente no `HttpTestingController` — o teste estoura o tempo em vez
de falhar explicando. Entre o clique e o `flush`, só dá para esperar uma volta
da fila de tarefas. E, sendo zoneless, o `resource()` só refaz a chamada quando
o template o lê de novo, o que no teste exige um `detectChanges()` explícito.

---

## 7. O que ficou

- Criar e editar pelada pela interface (a API já aceita; falta a tela)
- Campos de resposta chegam todos opcionais no TypeScript, porque o contrato só
  marca `required` onde há `@NotNull`. Anotar os DTOs de resposta deixaria os
  tipos mais firmes
- Servir o build do front pelo Compose, hoje fora do `docker compose up`

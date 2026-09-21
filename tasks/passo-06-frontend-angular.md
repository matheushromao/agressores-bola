# Passo 6 — Frontend Angular: autenticação e peladas

**Status:** fatia 1 entregue — login, cadastro, lista e detalhe
**Base:** commit `e49c1ed` (documentação OpenAPI)

---

## 1. Objetivo

Fechar a Etapa 5 do [Passo 5](passo-05-infraestrutura-seguranca-e-frontend.md),
a última do roteiro. A ordem defendida lá se cumpriu: o frontend chegou depois
de ambiente reprodutível, schema versionado, autenticação definida e **contrato
formal** — e é esse contrato que dispensou reescrever os 28 DTOs à mão.

Escopo desta fatia: **login, cadastro, lista de peladas com filtros e paginação,
e detalhe com confirmação de presença.** Sorteio, súmula e rankings ficam para
a fatia 2.

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

---

## 6. Testes

21 testes em Vitest + jsdom, sem browser e sem backend. Eles cobrem justamente
o que quebra calado:

| Suíte | Cobre |
|---|---|
| `auth.service.spec.ts` | Id e nickname lidos do `sub`, nickname com acento, token expirado ou malformado descartado, logout limpando o storage |
| `auth.interceptor.spec.ts` | Header anexado só quando há sessão, logout no 401, sessão preservada nos demais erros |
| `peladas-lista.spec.ts` | Query string virando parâmetro da chamada, formulário reabastecido pela URL, estado vazio |
| `pelada-detalhe.spec.ts` | Escalação agrupada, ação bloqueada para quem não está logado, `POST` com o id do token, recarga após a ação, controles só para o organizador, erro 409 visível na tela |
| `app.spec.ts` | Header alternando entre "Entrar" e o nickname |

Uma armadilha vale registro: `fixture.whenStable()` **trava** quando há
requisição pendente no `HttpTestingController` — o teste estoura o tempo em vez
de falhar explicando. Entre o clique e o `flush`, só dá para esperar uma volta
da fila de tarefas. E, sendo zoneless, o `resource()` só refaz a chamada quando
o template o lê de novo, o que no teste exige um `detectChanges()` explícito.

---

## 7. O que ficou para a fatia 2

- Criar e editar pelada pela interface (a API já aceita; falta a tela)
- Tela de sorteio com os times lado a lado — a mais gratificante visualmente
- Súmula e rankings
- Campos de resposta chegam todos opcionais no TypeScript, porque o contrato só
  marca `required` onde há `@NotNull`. Anotar os DTOs de resposta deixaria os
  tipos mais firmes
- Servir o build do front pelo Compose, hoje fora do `docker compose up`

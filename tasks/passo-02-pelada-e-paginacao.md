# Passo 2 — Entidade `Pelada`, escalação de participantes e paginação

**Status:** concluído
**Inspiração:** app **Appito** — criar a partida (data, horário, local, valor) e montar o grupo de jogadores.

---

## 1. Objetivo

1. Modelar a **Pelada**: quando, onde, que tipo de campo, quanto custa, quantos cabem.
2. Modelar o **grupo de participantes**, com confirmação de presença e lista de espera.
3. Passar a **paginação para o backend** (`Pageable`) — tanto em `Usuario` quanto em `Pelada`.

---

## 2. Arquivos criados e alterados

### Criados

```
model/Pelada.java                                  entidade principal
model/ParticipacaoPelada.java                      escalação (pelada ⇄ usuário)
model/enums/TipoCampo.java                         CAMPO, SOCIETY, FUTSAL, QUADRA, AREIA
model/enums/StatusPelada.java                      AGENDADA … CANCELADA
model/enums/StatusParticipacao.java                CONVIDADO, CONFIRMADO, RECUSADO, LISTA_DE_ESPERA

dto/PeladaRequest.java                             entrada, com validações
dto/PeladaResponse.java                            saída completa (com escalação)
dto/PeladaResumoResponse.java                      saída enxuta das listagens
dto/PeladaFiltro.java                              agrupa os filtros da listagem
dto/ParticipacaoRequest.java                       incluir jogador
dto/ParticipanteResponse.java                      jogador na escalação
dto/StatusPeladaRequest.java                       alterar status da pelada
dto/StatusParticipacaoRequest.java                 confirmar/recusar presença
dto/UsuarioResumoResponse.java                     visão curta do usuário
dto/PageResponse.java                              envelope de paginação (genérico)

repository/PeladaRepository.java
repository/ParticipacaoPeladaRepository.java
repository/specification/PeladaSpecification.java  filtros combináveis
repository/specification/UsuarioSpecification.java

mapper/PeladaMapper.java
service/PeladaService.java                         contrato
service/impl/PeladaServiceImpl.java                regras de negócio
controller/PeladaController.java
```

### Alterados

| Arquivo | Mudança |
|---------|---------|
| `UsuarioController` | listagem passou a receber `Pageable` e devolver `PageResponse` |
| `UsuarioService` / `UsuarioServiceImpl` | `listarTodos()` + `listarPorPosicao()` viraram um único `listar(posicao, busca, nacionalidade, pageable)` |
| `UsuarioRepository` | passou a estender `JpaSpecificationExecutor`; queries derivadas de filtro removidas (agora são Specifications) |
| `UsuarioMapper` | ganhou `toResumoResponse()` |
| `GlobalExceptionHandler` | 4 handlers novos (parâmetro inválido, ordenação inválida, corpo ilegível, violação de integridade) |
| `application.yaml` / `application-example.yaml` | configuração padrão de paginação |

---

## 3. Modelagem

### 3.1 `Pelada` → tabela `tb_peladas`

| Campo | Tipo | Observação |
|-------|------|-----------|
| `id` | Long | PK |
| `nome` | String(100) | obrigatório |
| `descricao` | String(500) | opcional |
| `data` | LocalDate | obrigatório |
| `horaInicio` / `horaFim` | LocalTime | obrigatórios |
| `localNome` | String(120) | nome da quadra/campo |
| `endereco` | String(200) | endereço |
| `cidade` / `estado` | String(80) / String(2) | usados no filtro de busca |
| `tipoCampo` | enum `TipoCampo` | society, futsal, campo… |
| `maxParticipantes` | Integer | 2 a 50 |
| `valorPorJogador` | BigDecimal(10,2) | opcional (pelada de graça) |
| `status` | enum `StatusPelada` | nasce `AGENDADA` |
| `organizador` | `Usuario` | `@ManyToOne` obrigatório |
| `participacoes` | `List<ParticipacaoPelada>` | `@OneToMany`, cascade + orphanRemoval |

> **Por que `BigDecimal` no valor?** `double` tem erro de arredondamento —
> dinheiro nunca deve usar ponto flutuante binário.

### 3.2 `ParticipacaoPelada` → tabela `tb_participacoes_pelada`

A ligação pelada ⇄ usuário **não** é um `@ManyToMany` simples, porque a relação
carrega dados próprios: o status da presença e a data de inscrição. Isso é o que
permite a mecânica de confirmar presença e de lista de espera do Appito.

| Campo | Observação |
|-------|-----------|
| `pelada` / `usuario` | `@ManyToOne` obrigatórios |
| `status` | `CONVIDADO`, `CONFIRMADO`, `RECUSADO`, `LISTA_DE_ESPERA` |
| `dataInscricao` | preenchida por `@PrePersist`; define a ordem da fila |

Constraint única `(pelada_id, usuario_id)` — o mesmo jogador não entra duas
vezes na mesma pelada, garantido **pelo banco**, não só pelo Java.

### 3.3 Comportamento na entidade, não no service

A `Pelada` protege as próprias invariantes em vez de deixar o service mexer na
coleção por fora:

```java
pelada.adicionarParticipacao(p);   // sincroniza os dois lados da relação
pelada.buscarParticipacaoDoUsuario(id);
pelada.totalConfirmados();
pelada.vagasRestantes();
pelada.estaLotada();
pelada.aceitaAlteracoes();
```

Isso é coesão (o "S" do SOLID): quem conhece as regras da escalação é a própria
pelada; o service só orquestra.

---

## 4. Validações

### 4.1 De formato — em `PeladaRequest` (Bean Validation)

- `@NotBlank` / `@Size` nos textos;
- `@FutureOrPresent` na data;
- `@Pattern` de 2 letras no estado (`SP`);
- `@Min(2)` / `@Max(50)` em `maxParticipantes`;
- `@DecimalMin(0)` / `@DecimalMax(9999.99)` no valor.

**Validação de campos cruzados** — o Bean Validation olha um campo por vez, então
a relação entre início e fim virou um `@AssertTrue`:

```java
@JsonIgnore
@AssertTrue(message = "O horário de término deve ser posterior ao horário de início")
public boolean isHorarioValido() {
    return horaInicio == null || horaFim == null || horaFim.isAfter(horaInicio);
}
```

O erro aparece no campo `horarioValido` da resposta 400.

### 4.2 De negócio — em `PeladaServiceImpl`

| Regra | Resposta |
|-------|----------|
| Organizador precisa existir | 404 |
| Início não pode estar no passado (data **+ hora**) | 409 |
| Organizador não pode ter outra pelada na mesma data/hora | 409 |
| Pelada `FINALIZADA`/`CANCELADA` não aceita alterações | 409 |
| Não pode reduzir o limite abaixo dos já confirmados | 409 |
| Troca de organizador não é permitida no `PUT` | 409 |
| Jogador não pode entrar duas vezes | 409 |
| Confirmar em pelada lotada | 409 (sugere a lista de espera) |
| Confirmar pelada com menos de 2 confirmados | 409 |
| Status de pelada encerrada não muda mais | 409 |
| Organizador não pode sair nem ser removido da própria pelada | 409 |

**Automatismos**

- Quem cria a pelada já entra escalado como `CONFIRMADO` — a pelada nunca nasce vazia.
- Quando um confirmado sai (ou recusa), o **primeiro da lista de espera é
  promovido automaticamente** a `CONFIRMADO`, pela ordem de `dataInscricao`.

---

## 5. Paginação no backend

### 5.1 Como funciona

O controller recebe um `Pageable` montado pelo Spring a partir da query string,
e o `@PageableDefault` define o comportamento quando o cliente não manda nada:

```java
@GetMapping
public ResponseEntity<PageResponse<PeladaResumoResponse>> listar(
        // … filtros …
        @PageableDefault(size = 10, sort = {"data", "horaInicio"},
                         direction = Sort.Direction.ASC) Pageable pageable) {
```

O banco devolve **apenas a fatia pedida** (`LIMIT`/`OFFSET`) e uma consulta de
`count` para o total. Nada de trazer a tabela inteira para filtrar em memória.

### 5.2 Configuração global (`application.yaml`)

```yaml
spring:
  data:
    web:
      pageable:
        default-page-size: 10
        max-page-size: 50        # teto: ?size=5000 vira 50
        one-indexed-parameters: false   # a primeira página é a 0
```

### 5.3 Envelope `PageResponse<T>`

O `Page` do Spring Data não é exposto direto — o JSON dele muda entre versões e
carrega metadados desnecessários. A API devolve um envelope próprio e estável:

```json
{
  "conteudo": [ ... ],
  "pagina": 0,
  "tamanho": 10,
  "totalElementos": 37,
  "totalPaginas": 4,
  "primeira": true,
  "ultima": false
}
```

### 5.4 Filtros dinâmicos com Specifications

Filtro opcional em HQL vira `(:param is null or campo = :param)`, que é frágil e
polui a query. A solução foi a **Criteria API via `Specification`**: cada filtro
é um método isolado, e filtro não informado devolve `Specification.unrestricted()`
(elemento neutro), então o service só empilha os critérios:

```java
Specification<Pelada> filtros = Specification.allOf(
        PeladaSpecification.comStatus(filtro.status()),
        PeladaSpecification.naCidade(filtro.cidade()),
        PeladaSpecification.aPartirDe(filtro.dataInicial()),
        // …
);
Page<Pelada> pagina = peladaRepository.findAll(filtros, pageable);
```

Um filtro novo = um método novo, sem tocar no que já existe (princípio
aberto/fechado).

> ⚠️ **Atenção para Spring Data JPA 4.x:** o `Specification.allOf` **não aceita
> mais `null`** (`IllegalArgumentException: Other specification must not be null`).
> Use `Specification.unrestricted()` para representar "sem filtro".

### 5.5 Cuidado com N+1

- `@EntityGraph(attributePaths = "organizador")` no `findAll` paginado: traz o
  organizador no mesmo `SELECT` (é `ToOne`, não atrapalha a paginação).
- `@BatchSize(size = 20)` na coleção `participacoes`: o contador de confirmados
  do resumo carrega as escalações em lote, não uma consulta por pelada.
- No detalhe (`GET /api/peladas/{id}`), um `@EntityGraph` traz pelada +
  organizador + escalação + usuários de uma vez.

---

## 6. Endpoints

### Pelada

| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/api/peladas` | Cria a pelada (201 + `Location`); organizador entra confirmado |
| `GET` | `/api/peladas` | **Listagem paginada** com filtros |
| `GET` | `/api/peladas/{id}` | Detalhe com a escalação completa |
| `PUT` | `/api/peladas/{id}` | Atualiza os dados |
| `PATCH` | `/api/peladas/{id}/status` | Confirma, inicia, finaliza ou cancela |
| `DELETE` | `/api/peladas/{id}` | Remove (204) — a escalação vai junto |

**Filtros da listagem:** `status`, `tipoCampo`, `cidade` (parcial),
`dataInicial`, `dataFinal`, `organizadorId`, `participanteId`
— combináveis com `page`, `size` e `sort`.

```
GET /api/peladas?cidade=soroc&status=AGENDADA&page=0&size=10&sort=data,asc
GET /api/peladas?participanteId=3          → "minhas peladas"
GET /api/peladas?organizadorId=1           → "peladas que eu organizo"
```

### Escalação (o grupo)

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET` | `/api/peladas/{id}/participantes` | Lista a escalação por ordem de entrada |
| `POST` | `/api/peladas/{id}/participantes` | Inclui jogador (`status` opcional, padrão `CONFIRMADO`) |
| `PATCH` | `/api/peladas/{id}/participantes/{usuarioId}` | Confirma, recusa ou põe na lista de espera |
| `DELETE` | `/api/peladas/{id}/participantes/{usuarioId}` | Tira o jogador (204) |

### Usuário (atualizado)

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET` | `/api/usuarios` | **Paginado**, filtros: `posicao`, `busca` (nome ou nickname), `nacionalidade` |

```
GET /api/usuarios?busca=pel&posicao=ATACANTE&page=0&size=10&sort=nomeCompleto,asc
```

---

## 7. Exemplos

**Criar pelada**

```http
POST /api/peladas
Content-Type: application/json

{
  "nome": "Pelada dos Agressores",
  "descricao": "Racha de quinta",
  "data": "2026-08-15",
  "horaInicio": "19:00",
  "horaFim": "21:00",
  "localNome": "Arena Central",
  "endereco": "Rua das Bolas, 100",
  "cidade": "Sorocaba",
  "estado": "SP",
  "tipoCampo": "SOCIETY",
  "maxParticipantes": 14,
  "valorPorJogador": 25.00,
  "organizadorId": 1
}
```

Resposta `201` (trecho):

```json
{
  "id": 1,
  "status": "AGENDADA",
  "statusDescricao": "Agendada",
  "maxParticipantes": 14,
  "totalConfirmados": 1,
  "vagasRestantes": 13,
  "organizador": { "id": 1, "nickname": "jog1", "posicaoDescricao": "Atacante" },
  "participantes": [
    { "participacaoId": 1, "status": "CONFIRMADO", "usuario": { "nickname": "jog1" } }
  ]
}
```

**Confirmar presença de um jogador**

```http
POST /api/peladas/1/participantes
{ "usuarioId": 2 }
```

**Entrar na lista de espera (pelada lotada)**

```http
POST /api/peladas/1/participantes
{ "usuarioId": 3, "status": "LISTA_DE_ESPERA" }
```

**Desistir** — libera a vaga e promove o primeiro da fila automaticamente:

```http
DELETE /api/peladas/1/participantes/2
```

---

## 8. Tratamento de erros ampliado

| Situação | Antes | Agora |
|----------|-------|-------|
| `?status=XPTO` (enum inexistente) | 500 | **400** listando os valores aceitos |
| `?sort=campoQueNaoExiste` | 500 | **400** "Não existe o campo … para ordenar" |
| JSON malformado / enum inválido no corpo | 500 | **400** |
| Constraint do banco violada | 500 | **409** |

Exemplo:

```json
{
  "status": 400,
  "erro": "Parâmetro inválido",
  "mensagem": "O parâmetro 'tipoCampo' recebeu um valor inválido: PELADAO. Valores aceitos: CAMPO, SOCIETY, FUTSAL, QUADRA, AREIA"
}
```

---

## 9. Verificação executada

Aplicação subida contra o MySQL local, tabelas geradas pelo Hibernate
(`tb_peladas`, `tb_participacoes_pelada`, com FKs e constraint única) e os
endpoints exercitados via HTTP:

- ✅ paginação, `sort`, filtros combinados e teto de `max-page-size` (`?size=5000` → 50);
- ✅ criação da pelada com organizador já confirmado;
- ✅ lotação → 409 sugerindo lista de espera; duplicata → 409;
- ✅ saída de confirmado promovendo o primeiro da lista de espera;
- ✅ organizador bloqueado de sair/ser removido;
- ✅ transições de status e bloqueio após `FINALIZADA`;
- ✅ 400 com o mapa de campos inválidos; 404 de pelada/usuário/participante;
- ✅ dados de teste removidos do banco ao final.

**Dois bugs encontrados e corrigidos nessa verificação:**

1. `Specification.allOf` com `null` estourava 500 no Spring Data JPA 4 → trocado
   por `Specification.unrestricted()`.
2. `participacaoId` voltava `null` ao incluir jogador, porque o `id` só era
   gerado no flush do commit (depois do mapper) → a participação passou a ser
   salva pelo próprio `ParticipacaoPeladaRepository`.

---

## 10. Possíveis próximos passos

- Autenticação (Spring Security + JWT) — hoje o `organizadorId` vem no corpo da
  requisição; com login autenticado ele viria do token, e os endpoints da
  escalação poderiam distinguir "eu entro" de "o organizador me convida".
- Sorteio de times equilibrado por posição.
- Notificação de convite e lembrete da partida.
- Testes automatizados (`@DataJpaTest` nos repositories, `@WebMvcTest` nos
  controllers) — a verificação deste passo foi manual via HTTP.
- Migrations com Flyway no lugar do `ddl-auto: update`, quando o projeto for para produção.

# Passo 3 — Estatísticas de jogo, rankings de pontuação e sorteio de times

**Status:** concluído
**Commits:** `8daa053` (estatísticas, rankings e sorteio), `e85f269` (README do projeto)

---

## 1. Objetivo

1. Registrar a **súmula de cada jogador em cada pelada** — gols, assistências e
   desarmes para quem joga na linha; gols, assistências, defesas e defesas
   difíceis para quem joga no gol.
2. Transformar esses números em **pontos**, com peso maior para as jogadas mais
   difíceis, e montar o **ranking geral** (do primeiro ao último) e os
   **rankings por atributo** (artilharia, assistências, defesas difíceis…).
3. **Sortear times equilibrados** a partir da classificação em estrelas de cada
   jogador (1 a 5, de meia em meia).

---

## 2. Arquivos criados e alterados

### Criados

```
model/EstatisticaPartida.java                      súmula do jogador na pelada
model/ResumoEstatistico.java                       objeto de valor com os números
model/enums/AtributoPontuacao.java                 tabela de pontos (peso + extrator)

dto/EstatisticaRequest.java                        lançamento da súmula
dto/EstatisticaResponse.java                       súmula + pontuação detalhada
dto/PontuacaoAtributoResponse.java                 quebra da pontuação por atributo
dto/RankingResponse.java                           linha do ranking geral
dto/RankingAtributoResponse.java                   linha de um ranking por atributo
dto/DestaqueResponse.java                          todos os rankings de uma vez
dto/SorteioRequest.java                            critério do sorteio
dto/SorteioResponse.java                           times, reservas e semente
dto/TimeSorteadoResponse.java                      um time formado
dto/JogadorSorteadoResponse.java                   jogador dentro do time

repository/EstatisticaPartidaRepository.java       CRUD + agregação dos rankings
repository/projection/TotaisJogador.java           projeção do "group by"

mapper/EstatisticaMapper.java
mapper/RankingMapper.java
mapper/SorteioMapper.java

service/EstatisticaService.java                    contratos
service/RankingService.java
service/SorteioService.java
service/impl/EstatisticaServiceImpl.java           regras de negócio
service/impl/RankingServiceImpl.java
service/impl/SorteioServiceImpl.java
service/sorteio/BalanceadorDeTimes.java            algoritmo de balanceamento
service/sorteio/JogadorSorteavel.java              entrada do algoritmo
service/sorteio/TimesBalanceados.java              saída do algoritmo

controller/EstatisticaController.java
controller/RankingController.java
controller/SorteioController.java

test/service/sorteio/BalanceadorDeTimesTest.java
test/repository/EstatisticaPartidaRepositoryTest.java
```

### Alterados

| Arquivo | Mudança |
|---------|---------|
| `Usuario` | novo campo `estrelas` (1.0 a 5.0), constante `ESTRELAS_PADRAO`, métodos `estrelasOuPadrao()` e `ehGoleiro()` |
| `ParticipacaoPelada` | lado inverso `@OneToOne` da súmula, com `definirEstatistica()`, `removerEstatistica()` e `estaConfirmado()` |
| `StatusPelada` | métodos `aceitaEstatistica()` e `aceitaSorteio()` |
| `UsuarioRequest` | campo `estrelas` opcional, com validação de faixa e de escala |
| `UsuarioResponse` / `UsuarioResumoResponse` | passaram a expor `estrelas` |
| `UsuarioMapper` | mapeia `estrelas` nos três sentidos |

---

## 3. Modelagem

### 3.1 `EstatisticaPartida` → tabela `tb_estatisticas_partida`

A súmula pende da **`ParticipacaoPelada`**, e não do `Usuario`. Essa é a decisão
central do passo: a estatística é **do jogo**, então o mesmo jogador tem uma
linha diferente em cada pelada. Pendurar no usuário obrigaria a somar tudo em um
campo só e perderia o histórico por partida.

| Campo | Tipo | Observação |
|-------|------|-----------|
| `id` | Long | PK |
| `participacao` | `ParticipacaoPelada` | `@OneToOne` obrigatório, **único** |
| `posicaoJogada` | enum `Posicao` | posição efetivamente jogada |
| `gols` | Integer | padrão 0 |
| `assistencias` | Integer | padrão 0 |
| `desarmes` | Integer | padrão 0 |
| `defesas` | Integer | padrão 0 |
| `defesasDificeis` | Integer | padrão 0 |
| `registradaEm` | LocalDateTime | `@PrePersist` |
| `atualizadaEm` | LocalDateTime | `@PreUpdate` |

Constraint única em `participacao_id`: a súmula de um jogador em uma pelada é
uma só, garantido **pelo banco**.

> **Por que `posicaoJogada` separada da posição do cadastro?**
> Na pelada é comum um jogador de linha pegar o gol. São as defesas *dele
> naquele jogo* que definem quais números valem, não a ficha dele. Sem esse
> campo, ou o goleiro improvisado ficaria sem defesas, ou a validação por
> posição seria impossível.

### 3.2 A pontuação **não** é persistida

Não existe coluna `pontuacao`. Ela é sempre derivada dos números brutos pelos
pesos do enum. Gravar o total criaria duas fontes de verdade: no dia em que o
peso do gol mudar de 10 para 12, todo o histórico ficaria inconsistente — as
peladas antigas manteriam o total velho e as novas usariam o novo.

### 3.3 `estrelas` no `Usuario`

Nota de 1.0 a 5.0, com passo de meia estrela. É o insumo do sorteio.

A coluna é **nullable** de propósito: o campo entrou depois que já havia
cadastros, e uma coluna `NOT NULL` quebraria as linhas existentes no
`ddl-auto: update`. Quem não tem nota entra no sorteio como mediano (`3.0`),
resolvido no domínio por `estrelasOuPadrao()` — não com um `if` espalhado pelos
services.

---

## 4. Tabela de pontuação

### 4.1 O enum `AtributoPontuacao`

| Atributo | Peso | Vale para |
|----------|:----:|-----------|
| `GOL` | **10** | Todos |
| `DEFESA_DIFICIL` | **8** | Goleiro |
| `ASSISTENCIA` | **7** | Todos |
| `DEFESA` | **4** | Goleiro |
| `DESARME` | **3** | Jogador de linha |

Os pesos seguem a dificuldade da jogada: o gol decide a partida; a defesa difícil
vale quase um gol porque evita um; desarme e defesa comum são o trabalho de base.

O enum não guarda só o peso — guarda também **como extrair a quantidade** de um
`ResumoEstatistico`:

```java
GOL("Gols", 10, ResumoEstatistico::gols),
DEFESA_DIFICIL("Defesas difíceis", 8, ResumoEstatistico::defesasDificeis),
// …

public int pontos(ResumoEstatistico resumo) {
    return quantidade(resumo) * peso;
}
```

Com isso, a pontuação individual, o ranking geral e os rankings por atributo
consomem **exatamente a mesma regra**. Mudar um peso muda tudo junto, sem risco
de um lugar ficar para trás. Um atributo novo (cartão amarelo, por exemplo) é
uma linha no enum — nenhum service muda.

### 4.2 `ResumoEstatistico`

Objeto de valor imutável com os cinco números. Existe porque a soma de pontos
precisa funcionar em dois contextos diferentes:

- a súmula de **uma** pelada (`EstatisticaPartida.resumo()`);
- o total de **várias** peladas somadas pelo banco (`TotaisJogador.resumo()`).

Sem ele, a fórmula da pontuação apareceria duas vezes.

---

## 5. Rankings

### 5.1 Agregação no banco

O somatório por jogador é um único `group by`, não um carregamento do histórico
inteiro na memória:

```java
@Query("""
        select new com.hmz.agressores_da_bola.repository.projection.TotaisJogador(
            u.id, u.nickname, u.nomeCompleto, u.posicao, u.estrelas,
            count(e.id), sum(e.gols), sum(e.assistencias),
            sum(e.desarmes), sum(e.defesas), sum(e.defesasDificeis))
        from EstatisticaPartida e
            join e.participacao p
            join p.usuario u
        where (:peladaId is null or p.pelada.id = :peladaId)
        group by u.id, u.nickname, u.nomeCompleto, u.posicao, u.estrelas
        """)
List<TotaisJogador> somarPorJogador(@Param("peladaId") Long peladaId);
```

**A divisão de responsabilidade é proposital:** o banco só *soma quantidades*; os
*pesos* continuam no domínio. Se a fórmula de pontos estivesse no SQL, ela
existiria em dois lugares e sairia do controle do enum.

> Os campos numéricos de `TotaisJogador` são `Long`, e não `long`. `count` e
> `sum` do JPQL são de precisão longa, e o wrapper evita depender de autoboxing
> na hora de o Hibernate casar a expressão de construtor.

O parâmetro `peladaId` nulo significa "histórico completo" — o mesmo método
serve ao ranking geral e ao ranking de uma pelada específica.

### 5.2 Colocação com empate

Empate divide a posição e pula a seguinte (`1, 2, 2, 4`), como em qualquer
tabela. O algoritmo é genérico e serve aos dois tipos de ranking:

| Ranking | Critério | Desempate |
|---------|----------|-----------|
| Geral | Pontos | gols → assistências → ordem alfabética |
| Por atributo | Quantidade do atributo | menos jogos → pontuação geral → alfabética |

Nos rankings por atributo, quem está zerado **não aparece**: uma artilharia com
jogadores de zero gols só poluiria a tela.

### 5.3 `destaques`

Uma única leitura do banco alimenta o ranking de todos os atributos, em vez de
uma consulta por atributo:

```java
List<TotaisJogador> totais = somarPorJogador(peladaId);   // 1 consulta
return Arrays.stream(AtributoPontuacao.values())
        .map(atributo -> new DestaqueResponse(..., rankingDe(atributo, totais, limite)))
        .toList();
```

---

## 6. Sorteio de times

### 6.1 Onde o algoritmo mora

`service/sorteio/BalanceadorDeTimes` **não conhece JPA nem Spring**: recebe uma
lista de `JogadorSorteavel` e um `Random`, e devolve `TimesBalanceados`. É o que
permite testá-lo sem subir contexto — e o que mantém a regra de negócio separada
da infraestrutura.

O service faz o resto: busca a pelada, filtra os confirmados, valida a divisão e
delega.

### 6.2 As quatro etapas

**1. Embaralhamento.** A lista é embaralhada com um `Random` semeado. É daí que
vem a variação entre sorteios.

**2. Goleiros primeiro.** No máximo um por time — dois goleiros no mesmo time
desequilibram muito mais que qualquer diferença de nota. Goleiro excedente volta
para o bolo da linha e disputa vaga como qualquer outro, que é o que acontece na
pelada.

**3. Distribuição gulosa.** Do mais estrelado para o menos, cada jogador entra no
time mais fraco naquele momento. A ordenação é **estável**, então jogadores de
mesma nota mantêm a ordem aleatória do embaralhamento — é aí que mora o sorteio.

**4. Refino por trocas.** Troca-se um jogador do time mais forte por um do mais
fraco sempre que isso encurtar a diferença, até não haver mais troca que melhore.
Trocar dois jogadores move `2 × (nota A − nota B)` de um lado para o outro, então
uma troca só é aceita quando `|diferença − 2×(a−b)| < diferença`. Goleiros ficam
de fora das trocas, para não quebrar a regra de um por time.

> **Por que não é um sorteio cego?** Porque a intenção é jogo equilibrado. O
> acaso decide quem joga com quem **entre jogadores de mesmo nível**; as estrelas
> garantem que nenhum time saia muito mais forte. Sorteio puramente aleatório
> junta os cinco melhores no mesmo time com frequência.

### 6.3 Semente reproduzível

A resposta devolve a `semente` usada. Reenviá-la refaz exatamente o mesmo
sorteio — útil para reconstituir uma divisão já combinada no grupo.

### 6.4 Times iguais, resto na reserva

Os times saem sempre do mesmo tamanho. Quem sobra da divisão exata vira reserva,
porque um time com um jogador a mais já nasce em vantagem.

### 6.5 Nada é persistido

O sorteio é uma **sugestão**: `@Transactional(readOnly = true)`, sem gravar. É
`POST` mesmo assim, porque cada chamada produz uma divisão diferente e a operação
não é idempotente como um `GET` precisaria ser.

---

## 7. Regras de negócio

### 7.1 Estatísticas (`EstatisticaServiceImpl`)

| Regra | Resposta |
|-------|----------|
| Pelada precisa estar `EM_ANDAMENTO` ou `FINALIZADA` | 409 |
| Jogador precisa estar `CONFIRMADO` na pelada | 409 |
| Defesas difíceis não podem superar o total de defesas | 400 |
| Jogador de linha não soma defesa nem defesa difícil | 409 |
| Goleiro não soma desarme | 409 |
| Jogador não participa da pelada | 404 |
| Súmula ainda não lançada (no `GET`/`DELETE`) | 404 |

**Antes do jogo não há súmula.** Pelada `AGENDADA` não tem números, e
`CANCELADA` nunca terá — por isso o status é validado.

**Lançar de novo corrige.** O endpoint é `PUT` e faz *upsert*: relançar
sobrescreve os números, o que permite ao organizador corrigir um lançamento sem
precisar apagar antes.

### 7.2 Sorteio (`SorteioServiceImpl`)

| Regra | Resposta |
|-------|----------|
| Pelada `CANCELADA` ou `FINALIZADA` não é sorteável | 409 |
| Mínimo de 2 times com 2 jogadores cada | 409 |
| Confirmados insuficientes para a divisão pedida | 409 |
| Informar os dois critérios (ou nenhum) | 400 |

Só entram jogadores `CONFIRMADO`: convidado e lista de espera ainda não são
jogadores da pelada.

### 7.3 Validação da escala de estrelas

O Bean Validation não expressa "múltiplo de 0.5", então a regra virou um
`@AssertTrue` no `UsuarioRequest`, no mesmo padrão do `isHorarioValido()` do
Passo 2:

```java
@JsonIgnore
@AssertTrue(message = "A classificação deve variar de meia em meia estrela, por exemplo 3.5")
public boolean isEstrelasNaEscala() {
    return estrelas == null
            || estrelas.stripTrailingZeros().scale() <= 1
            && estrelas.remainder(PASSO_DA_ESCALA).compareTo(BigDecimal.ZERO) == 0;
}
```

---

## 8. Endpoints

### Estatísticas

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET` | `/api/peladas/{peladaId}/estatisticas` | Súmula da pelada, ordenada por pontuação |
| `GET` | `/api/peladas/{peladaId}/participantes/{usuarioId}/estatistica` | Súmula de um jogador |
| `PUT` | `/api/peladas/{peladaId}/participantes/{usuarioId}/estatistica` | Lança ou corrige |
| `DELETE` | `/api/peladas/{peladaId}/participantes/{usuarioId}/estatistica` | Apaga (204) |

### Rankings

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET` | `/api/ranking` | Classificação geral por pontos |
| `GET` | `/api/ranking/atributos/{atributo}` | Ranking de um atributo |
| `GET` | `/api/ranking/destaques` | Todos os rankings de uma vez (`limite` padrão 5) |

Parâmetros comuns: `peladaId` (restringe a uma pelada) e `limite` (corta o topo).

### Sorteio

| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/api/peladas/{peladaId}/sorteio` | Sorteia times equilibrados |

---

## 9. Exemplos

**Lançar súmula de jogador de linha**

```http
PUT /api/peladas/1/participantes/7/estatistica
{ "gols": 2, "assistencias": 1, "desarmes": 4 }
```

**Lançar súmula de goleiro** (ou de quem pegou o gol, via `posicaoJogada`)

```http
PUT /api/peladas/1/participantes/9/estatistica
{ "posicaoJogada": "GOLEIRO", "defesas": 8, "defesasDificeis": 3, "assistencias": 1 }
```

Resposta `200` (trecho) — a conta dos pontos vem aberta:

```json
{
  "goleiro": true,
  "defesas": 8,
  "defesasDificeis": 3,
  "assistencias": 1,
  "pontuacao": 63,
  "detalhamento": [
    { "atributo": "DEFESA_DIFICIL", "quantidade": 3, "peso": 8, "pontos": 24 },
    { "atributo": "ASSISTENCIA",    "quantidade": 1, "peso": 7, "pontos": 7 },
    { "atributo": "DEFESA",         "quantidade": 8, "peso": 4, "pontos": 32 }
  ]
}
```

**Rankings**

```
GET /api/ranking?limite=10
GET /api/ranking/atributos/GOL
GET /api/ranking/atributos/DEFESA_DIFICIL?peladaId=1
GET /api/ranking/destaques?limite=3
```

**Sortear times**

```http
POST /api/peladas/1/sorteio
{ "quantidadeTimes": 2 }
```

```json
{
  "quantidadeTimes": 2,
  "jogadoresPorTime": 5,
  "totalConfirmados": 11,
  "diferencaEntreTimes": 0.0,
  "times": [
    { "nome": "Time A", "totalEstrelas": 15.0, "mediaEstrelas": 3.00, "temGoleiro": true, "jogadores": [ "…" ] },
    { "nome": "Time B", "totalEstrelas": 15.0, "mediaEstrelas": 3.00, "temGoleiro": true, "jogadores": [ "…" ] }
  ],
  "reservas": [ "…" ],
  "semente": 1733412000
}
```

Repetir o `POST` com `"semente": 1733412000` refaz o mesmo sorteio.

---

## 10. Testes automatizados

Este é o passo em que o projeto ganhou testes — item que estava aberto no
"próximos passos" do Passo 2.

| Suíte | Testes | Cobre |
|-------|:------:|-------|
| `BalanceadorDeTimesTest` | 6 | Equilíbrio das estrelas, goleiros espalhados, formação de reservas, reprodutibilidade por semente, nota padrão |
| `EstatisticaPartidaRepositoryTest` | 3 | Agregação do ranking, filtro por pelada, remoção da súmula órfã |
| `AgressoresDaBolaApplicationTests` | 1 | Carga de contexto |

**Por que o teste de repositório roda contra o MySQL real**
(`@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)`): é a única forma
de garantir que o `group by` e a expressão de construtor **executam**, e não só
compilam. A transação é desfeita ao fim de cada teste, então nada sobra na base.

O teste mais direto verifica o caso exato do equilíbrio: 10 jogadores somando 30
estrelas fecham **15 × 15**, com `diferencaEntreTimes` igual a `0.0`.

---

## 11. Verificação executada

- ✅ `./mvnw test` — **10 testes, 0 falhas**;
- ✅ carga de contexto, que valida os mapeamentos JPA e faz o Spring Data
  **parsear o JPQL** da agregação no boot;
- ✅ agregação do ranking executada contra o MySQL, com os pontos conferidos na
  mão (3 gols × 10 + 1 assistência × 7 + 5 desarmes × 3 = **52**; 1 assistência
  × 7 + 8 defesas × 4 + 3 defesas difíceis × 8 = **63**);
- ✅ `orphanRemoval` da súmula confirmado apagando de fato o registro — é um
  mapeamento que costuma falhar em silêncio;
- ✅ tabela `tb_estatisticas_partida` e coluna `estrelas` geradas pelo Hibernate.

**Ressalva honesta:** ao contrário do Passo 2, **não houve exercício manual dos
endpoints via HTTP**. A verificação foi automatizada e cobre domínio, persistência
e agregação, mas a camada de controller (rotas, códigos de status, serialização)
não foi exercitada ponta a ponta.

**Dois ajustes necessários durante a implementação:**

1. **Inferência de tipo em `Comparator` encadeado.** `Comparator.comparingLong(X::getY).reversed()`
   dentro de um `thenComparing` não compila: a cadeia não é *poly expression*, então
   o compilador não infere o tipo pelo alvo. Resolvido declarando os comparadores em
   variáveis tipadas.
2. **Pacotes de teste do Spring Boot 4.** As anotações mudaram de lugar em relação
   ao Boot 3:
   `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`,
   `org.springframework.boot.jpa.test.autoconfigure.TestEntityManager`,
   `org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase`.

---

## 12. Impacto em contratos já existentes

`UsuarioResumoResponse` **ganhou o campo `estrelas`**. Ele aparece dentro de
outros recursos, então a mudança alcança respostas que já existiam:

- `GET /api/peladas/{id}` → `organizador` e `participantes[].usuario`
- `GET /api/peladas/{id}/participantes` → `usuario`
- `GET /api/peladas` → resumo do organizador

É **adição de campo**, não remoção nem renomeação: clientes existentes continuam
funcionando. `UsuarioResponse` também passou a expor `estrelas`, e
`UsuarioRequest` aceita o campo como opcional.

Nenhum endpoint anterior foi removido ou teve rota alterada.

---

## 13. Documentação

O passo também entregou o **`README.md` na raiz do projeto** (commit `e85f269`),
com: visão geral, stack, arquitetura, diagrama do modelo de domínio, referência
completa da API, regras de negócio, tabela de pontuação, explicação do algoritmo
de sorteio, tratamento de erros e uma seção de **segurança** — práticas em vigor,
limitações conhecidas (a API ainda não tem autenticação) e recomendações para
credenciais.

---

## 14. Possíveis próximos passos

- **Autenticação (Spring Security + JWT)** — continua sendo a lacuna mais
  relevante. Hoje qualquer cliente lança súmula de qualquer jogador; com login,
  só o organizador da pelada poderia fazê-lo.
- **Testes de controller** (`@WebMvcTest`) e de service, fechando a lacuna da
  seção 11.
- **Persistir o sorteio**, para guardar o histórico dos times de cada pelada e
  permitir registrar o placar por time.
- **Placar da partida** — hoje a súmula é individual; o resultado do jogo (e a
  vitória como critério de pontuação) ainda não existe.
- **Ranking por temporada**, com recorte de período além do recorte por pelada.
- **Avaliação entre jogadores** alimentando as estrelas automaticamente, em vez
  da nota fixa no cadastro.
- Migrations com Flyway no lugar do `ddl-auto: update`.

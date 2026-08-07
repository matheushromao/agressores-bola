# Passo 4 — Auditoria de camadas (SOLID/MVC) e testes de endpoints

**Status:** auditoria concluída — correções ainda **não aplicadas**
**Base auditada:** commit `a5e4ea7`

---

## 1. Objetivo

O Passo 3 fechou com uma ressalva explícita na seção 11: *"não houve exercício
manual dos endpoints via HTTP"*. Este passo existe para fechar essa lacuna e,
de quebra, revisar se o código continua respeitando a arquitetura que o próprio
projeto declara — **cada camada com uma responsabilidade só**.

São três entregas:

1. auditoria de camadas, SOLID e MVC;
2. exercício ponta a ponta de **todos** os endpoints da API contra o MySQL real;
3. lista de próximos passos, priorizada.

> **Este documento não altera código.** Ele registra o que foi encontrado e o
> que fazer a respeito. As correções são o conteúdo do Passo 5.

---

## 2. Testes de endpoints

### 2.1 Como foi feito

Aplicação empacotada (`mvnw package`) e subida de verdade
(`java -jar target/agressores-da-bola-0.0.1-SNAPSHOT.jar`), contra o MySQL
local. Um script de `curl` percorreu a API inteira comparando **o status code
recebido com o esperado**, em um roteiro que segue o ciclo de vida real de uma
pelada: cadastrar jogadores → criar a pelada → escalar o grupo → sortear os
times → lançar a súmula → ler os rankings → encerrar → apagar.

Cada endpoint foi exercitado no caminho feliz **e** nos caminhos de erro, que é
onde o contrato costuma escorregar.

### 2.2 Resultado

**132 verificações, 0 falhas.**

| Grupo | Verificações | Cobre |
|-------|:------------:|-------|
| Usuários | 33 | CRUD, paginação, 3 filtros, ordenação, unicidade, validações de campo |
| Peladas | 17 | CRUD, 7 filtros combináveis, agenda do organizador, campos cruzados |
| Participantes | 24 | escalação, troca de status, lista de espera, proteção do organizador |
| Sorteio | 10 | por nº de times, por jogadores/time, semente, critérios inválidos |
| Estatísticas | 20 | lançamento, correção (upsert), regras por posição, remoção |
| Rankings | 14 | geral, por atributo, destaques, recorte por pelada, limite |
| Ciclo de vida | 8 | bloqueios após `FINALIZADA`, transições de status |
| Rotas e deleções | 6 | rota inexistente, método não suportado, cascatas |

Distribuição dos status devolvidos: `200` ×47 · `201` ×24 · `204` ×3 ·
`400` ×23 · `404` ×18 · `405` ×1 · `409` ×16.

### 2.3 Verificações de conteúdo, não só de status

Status certo com corpo errado continua sendo defeito. Além dos códigos, foram
conferidos:

| Verificação | Resultado |
|-------------|-----------|
| Pontuação do jogador de linha (3 gols + 1 assist. + 5 desarmes) | **52** ✅ |
| Pontuação do goleiro (1 assist. + 8 defesas + 3 difíceis) | **63** ✅ |
| Mesma semente devolve o **mesmo** sorteio (comparação byte a byte) | ✅ |
| Empate no ranking gera `1, 2, 2, 4` | ✅ |
| `limite=0` e `limite=-5` significam "sem corte" (e não lista vazia) | ✅ |
| Ranking por atributo esconde quem zerou naquele atributo | ✅ |
| Promoção automática do primeiro da lista de espera ao liberar vaga | ✅ |
| Remover participante apaga a súmula dele (`orphanRemoval` via HTTP) | ✅ |
| Apagar a pelada limpa escalação e súmulas em cascata | ✅ |
| `estado` normalizado para maiúsculas (`sp` → `SP`) | ✅ |
| `?size=100000` é cortado pelo teto de 50 do `application.yaml` | ✅ |
| `?participanteId=` não duplica linhas na paginação (join) | ✅ |
| Suíte automatizada do Passo 3 | 10 testes, 0 falhas ✅ |

### 2.4 Nenhum erro 500

Varredura no log da aplicação após a bateria completa: **zero stack traces**,
zero `ERROR`. Todo erro chegou ao cliente como status tratado. O
`GlobalExceptionHandler` está fazendo o trabalho dele — com uma ressalva na
seção 3.5.

> **Falso positivo do próprio teste, registrado por honestidade:** a primeira
> rodada acusou 49 falhas em cascata. Duas eram do script, não da API — um
> `sed` guloso capturava o **último** `"id"` do JSON (o do participante) em vez
> do primeiro (o da pelada), e um caso de teste reduzia `maxParticipantes` antes
> de escalar o grupo. Corrigidos os dois, a API passou limpa. Vale como lembrete
> de que teste que falha nem sempre acusa o código.

---

## 3. Auditoria de camadas, SOLID e MVC

O desenho geral **está sólido e é coerente**: controller fino, service com a
regra, repository só com acesso a dados, mapper isolando a conversão, entidade
protegendo as próprias invariantes, DTO em `record`, exceções tratadas em um
lugar só. O que segue são desvios pontuais, ordenados por impacto.

### 3.1 🔴 N+1: a súmula é carregada sem ninguém pedir

**O achado mais grave, e o único com efeito de produção.**

```
GET /api/peladas/21              → 1 select da pelada + 4 selects de estatística
GET /api/peladas/21/participantes → 2 selects + 4 selects de estatística
GET /api/peladas?size=10         → 2 selects + 5 selects de estatística
```

Uma consulta extra **por participação**, para carregar uma súmula que **nenhuma
dessas respostas usa**. Com uma pelada de 20 jogadores são 20 consultas
desperdiçadas; em uma página de 20 peladas cheias, 400.

**Causa.** Em `ParticipacaoPelada`:

```java
@OneToOne(mappedBy = "participacao", cascade = CascadeType.ALL,
          orphanRemoval = true, fetch = FetchType.LAZY)
private EstatisticaPartida estatistica;
```

O `fetch = LAZY` **não tem efeito no lado inverso de um `@OneToOne`**. A chave
estrangeira mora do outro lado, então o Hibernate não consegue devolver um proxy:
para saber se o campo é `null` ou não, ele **precisa** ir ao banco. É um dos
poucos casos em que o JPA ignora silenciosamente o que foi pedido — e o
`@BatchSize` de `Pelada.participacoes`, que resolve bem a coleção, não alcança
esta associação.

**Caminhos de correção** (a decidir no Passo 5):

- `@BatchSize` na associação, trocando N consultas por `N/tamanho`;
- **remover o lado inverso** e passar a apagar a súmula pelo
  `EstatisticaPartidaRepository` — o `orphanRemoval` é a única coisa que o
  mapeamento entrega hoje, e nenhuma leitura navega `participacao → estatistica`;
- ativar o *bytecode enhancement* do Hibernate (`enableLazyInitialization`).

A segunda é a mais alinhada ao projeto: a súmula já é sempre lida pelo
repositório dela.

### 3.2 🟠 `totalConfirmados()` obriga a carregar a escalação inteira

`PeladaResumoResponse` traz `totalConfirmados` e `vagasRestantes`. Os dois saem
de `Pelada.totalConfirmados()`, que **itera a coleção de participações** — então
listar peladas carrega a escalação de todas elas só para contar.

O `@BatchSize(size = 20)` segura o estrago (uma consulta em lote, não uma por
pelada), mas o trabalho continua desnecessário: **contar é serviço do banco**.
Uma projeção com `count` no `PeladaRepository` deixaria a listagem em uma
consulta só — e ainda cortaria pela raiz metade do N+1 da seção 3.1.

É também o que dispara o problema: se a listagem não tocasse na coleção, as
súmulas nunca seriam carregadas ali.

### 3.3 🟠 O mapper depende da camada de service

```java
// mapper/SorteioMapper.java
import com.hmz.agressores_da_bola.service.sorteio.JogadorSorteavel;
import com.hmz.agressores_da_bola.service.sorteio.TimesBalanceados;
```

A seta aponta para o lado errado. Pela regra de dependência declarada no
`tasks/README.md`, o mapper converte **domínio ⇄ DTO**; ele não deveria conhecer
nada de dentro de `service/`.

O problema é de **endereço, não de conteúdo**: `JogadorSorteavel` e
`TimesBalanceados` são objetos de valor de domínio — imutáveis, sem Spring, sem
JPA — que foram parar debaixo de `service/` por conveniência. Movê-los para
`model/sorteio/` resolve o acoplamento sem mexer em uma linha de lógica.

### 3.4 🟠 O sorteio é a única regra sem interface — e a única que mente sobre Spring

Dois desvios na mesma classe.

**a) DIP.** Todo o projeto segue `Service` (contrato) + `impl` (implementação), e
o `README` afirma: *"o controller depende da interface do service, nunca da
implementação"*. Mas `SorteioServiceImpl` injeta a **classe concreta**:

```java
private final BalanceadorDeTimes balanceador;   // classe, não interface
```

É a exceção à regra da casa. E é justamente o ponto onde uma interface pagaria:
"como equilibrar os times" é uma estratégia — equilibrar por estrelas hoje, por
pontuação do ranking amanhã. Com `BalanceadorDeTimes` como interface e
`BalanceadorPorEstrelas` como implementação, trocar de critério vira um `@Bean`,
e o Open/Closed passa a valer de fato.

**b) A documentação não bate com o código.** O Javadoc da classe diz:

> *"A classe é isolada de JPA e de Spring de propósito"*

e o `passo-03` repete: *"não conhece JPA nem Spring"*. Mas a primeira linha do
arquivo é:

```java
import org.springframework.stereotype.Component;
```

O **espírito** está certo — ela recebe uma `List` e um `Random`, devolve os times,
e é testável sem contexto (o `BalanceadorDeTimesTest` prova). Mas a afirmação,
como está escrita, é falsa. Ou tira-se o `@Component` e registra-se a classe em
uma `@Configuration`, ou ajusta-se o texto para "não conhece JPA nem o contexto
do Spring".

### 3.5 🟠 O contrato de erro tem duas caras

O `GlobalExceptionHandler` cobre sete exceções e devolve sempre o mesmo
envelope. Só que **três erros comuns nunca chegam nele**:

| Situação | Status | Corpo devolvido |
|----------|:------:|-----------------|
| `GET /api/naoexiste` | 404 | `{"timestamp","status","error","path"}` ← Spring |
| `DELETE /api/ranking` | 405 | `{"timestamp","status","error","path"}` ← Spring |
| `POST` sem `Content-Type` | 415 | `{"timestamp","status","error","path"}` ← Spring |
| Qualquer erro tratado | 4xx | `{"timestamp","status","erro","mensagem","campos"}` ← app |

Repare que os campos têm **nomes diferentes** (`error` × `erro`, e o `path` só
existe em um) e o timestamp vem em fusos diferentes (UTC × local). O cliente que
tentar ler o erro de forma uniforme quebra justamente quando erra a rota — que é
quando mais precisa da mensagem.

Faltam `NoResourceFoundException`, `HttpRequestMethodNotSupportedException`,
`HttpMediaTypeNotSupportedException` e uma rede de segurança para `Exception`,
além de `spring.web.resources.add-mappings: false` para o 404 de rota cair no
handler.

### 3.6 🟠 Uma regra de negócio mora no banco

```
DELETE /api/usuarios/{id}   (jogador escalado em alguma pelada)
→ 409 "A operação viola uma restrição de integridade do banco de dados"
```

Funciona — a `DataIntegrityViolationException` é capturada e vira 409. Mas:

- a regra ("jogador escalado não pode ser excluído") **não existe em lugar
  nenhum do `UsuarioServiceImpl`**: quem a aplica é a foreign key;
- a mensagem não diz ao usuário o que ele fez de errado nem como resolver;
- é incoerente com o `PeladaServiceImpl`, que valida **tudo** no service e
  devolve mensagens específicas ("O organizador não pode sair da própria
  pelada", "A pelada já atingiu o limite de 12 jogadores…").

O handler de integridade deve continuar existindo — como rede de segurança. Mas
a regra pertence ao service, com a mensagem que o organizador precisa ler.

### 3.7 🟡 `open-in-view` ligado

```
WARN JpaBaseConfiguration$JpaWebConfiguration :
     spring.jpa.open-in-view is enabled by default...
```

Nunca configurado, então vale o padrão `true`: a sessão JPA fica aberta durante
a serialização da resposta. Isso **contradiz a separação de camadas do
projeto** — a camada web passa a poder disparar consultas, e um `LAZY` esquecido
não estoura em desenvolvimento, vira consulta escondida em produção.

Como todos os services já são `@Transactional` e já usam `@EntityGraph` para
trazer o que a resposta precisa, o projeto **não depende** do OSIV. Basta
`spring.jpa.open-in-view: false` — e o dia em que faltar um `EntityGraph`, o
erro aparece no teste em vez de virar N+1 silencioso.

### 3.8 🟡 Cálculo de negócio no mapper

`RankingMapper.mediaPorJogo(pontuacao, jogos)` faz uma conta de domínio
(rendimento por jogo, arredondado) dentro da camada de conversão. É pequeno,
mas é o mesmo tipo de vazamento que o projeto evitou com capricho em
`AtributoPontuacao`: lá, a regra de pontuação foi concentrada no enum
justamente para não se espalhar. O lugar natural é `ResumoEstatistico` ou
`TotaisJogador`.

Na mesma linha, `EstatisticaRequest.paraResumo()` faz um DTO de entrada
construir um objeto de domínio. É um acoplamento leve e defensável (o DTO só
normaliza nulos), mas está anotado aqui para a decisão ser consciente.

### 3.9 🟡 Código sem uso

Métodos e consultas que ninguém chama — todos verificados por busca no projeto
inteiro, incluindo os testes:

| Onde | O quê |
|------|-------|
| `ResumoEstatistico` | `mais()`, `ZERO`, `quantidadeDe()` |
| `PeladaFiltro` | `vazio()` |
| `Pelada` | `inicio()` |
| `UsuarioRepository` | `findByEmail()` |
| `ParticipacaoPeladaRepository` | `existsByPeladaIdAndUsuarioId()`, `countByPeladaIdAndStatus()` |

Consulta de repositório sem uso não é neutra: o Spring Data **valida a derivação
do nome no boot**, então cada uma é custo de arranque e superfície de manutenção
sem contrapartida. É o Interface Segregation aplicado ao repositório — a
interface deve expor o que os clientes usam, e só.

### 3.10 ✅ O que está certo e vale registrar

Não é só ressalva. Estes pontos foram auditados e **passaram**:

- **`AtributoPontuacao`** — peso e extração no mesmo enum, consumidos pela
  pontuação individual, pelo ranking geral e pelos rankings por atributo. Um
  atributo novo é uma linha, sem tocar em service nenhum. É Open/Closed de
  verdade, não decorativo.
- **Divisão banco × domínio no ranking** — o `group by` soma quantidades, os
  pesos ficam no enum. Uma consulta alimenta os cinco rankings de `destaques`.
- **`Specification`** — um critério por método, filtro ausente vira
  `unrestricted()`. Zero `if` no service. SRP exemplar.
- **Entidades com comportamento** — `adicionarParticipacao`, `estaLotada`,
  `estrelasOuPadrao`, `aceitaEstatistica`. Não são sacos de getters.
- **Pontuação derivada, nunca persistida** — decisão correta e bem defendida no
  Passo 3; mudar um peso não corrompe o histórico.
- **`PageResponse`** — o `Page` do Spring Data não vaza para o contrato da API.
- **Controllers realmente finos** — nenhum tem lógica de negócio; o único que
  faz algo além de delegar é o `PeladaController`, que monta o `PeladaFiltro` a
  partir dos sete `@RequestParam` (aceitável, e melhor que sete parâmetros
  soltos no service).

---

## 4. Um comportamento do sorteio que não é defeito, mas precisa ser sabido

Cenário testado: 6 confirmados, dois goleiros com notas **5.0 e 1.0**, e quatro
jogadores de linha `5.0, 5.0, 1.0, 1.0`. Total 18 estrelas — o ideal seria 9×9.

O resultado foi **11 × 7**, com `diferencaEntreTimes: 4.0`. Parece falha do
balanceamento, mas **é o ótimo possível**: com a regra de um goleiro por time,
as únicas divisões existentes são `11/7`, `7/11` e `15/3`. Nenhuma chega perto
de 9×9.

A causa é a regra — deliberada — de que **um goleiro por time vale mais que a
soma das estrelas**. Quando os goleiros têm notas muito distantes, essa restrição
impõe um piso à diferença que nenhum refino derruba, porque o refino também
(corretamente) recusa mexer em goleiro.

Duas melhorias possíveis, ambas baratas:

1. permitir a troca **goleiro ↔ goleiro** no refino: preserva um por time e abre
   um grau de liberdade que hoje não existe;
2. expor no `SorteioResponse` a **diferença mínima teórica**, para o organizador
   distinguir "o algoritmo não achou" de "não existe divisão melhor".

---

## 5. Placar da auditoria

| # | Achado | Camada | Princípio ferido | Prioridade |
|---|--------|--------|------------------|:----------:|
| 3.1 | N+1 da súmula (`@OneToOne` inverso) | model | — (desempenho) | 🔴 Alta |
| 3.2 | Contagem de confirmados em memória | model/repository | — (desempenho) | 🟠 Média |
| 3.3 | `SorteioMapper` importa `service.sorteio` | mapper | Dependência de camada | 🟠 Média |
| 3.4a | Service depende da classe concreta do balanceador | service | **DIP** / OCP | 🟠 Média |
| 3.4b | `@Component` numa classe documentada como sem Spring | service | Doc × código | 🟠 Média |
| 3.5 | 404/405/415 fora do envelope de erro | exception | **SRP** do handler | 🟠 Média |
| 3.6 | Exclusão de usuário barrada só pela FK | service | Regra fora do service | 🟠 Média |
| 3.7 | `open-in-view` ligado | config | Separação de camadas | 🟡 Baixa |
| 3.8 | `mediaPorJogo` no mapper | mapper | **SRP** | 🟡 Baixa |
| 3.9 | Código morto (7 membros) | vários | **ISP** | 🟡 Baixa |

**Nenhum achado é de correção funcional.** A API respondeu certo em 132 de 132
verificações; o que está na lista é desempenho, coerência de camadas e contrato
de erro.

---

## 6. Próximos passos

### 6.1 Passo 5 — pagar a dívida desta auditoria

Curto, mecânico e com teste que prova cada item:

1. **Matar o N+1** (3.1) e trocar a contagem de confirmados por `count` no banco
   (3.2). Contar as consultas antes e depois é a prova.
2. **Desligar o `open-in-view`** (3.7) — depois de 1 e 2, não de antes: com o
   OSIV desligado, qualquer `LAZY` esquecido vira erro na hora.
3. **Mover `sorteio` para `model/`** (3.3) e **extrair a interface do
   balanceador** (3.4a), com `@Component` na implementação — o que resolve 3.4b
   junto.
4. **Fechar o `GlobalExceptionHandler`** (3.5): 404 de rota, 405, 415 e
   `Exception`.
5. **Trazer a regra de exclusão de usuário para o service** (3.6).
6. **Apagar o código morto** (3.9) e mover `mediaPorJogo` para o domínio (3.8).

### 6.2 Passo 6 — autenticação (Spring Security + JWT)

Continua sendo **a maior lacuna do projeto**, herdada do Passo 3, e a única que
impede a API de ir para produção. Hoje qualquer cliente lança súmula de qualquer
jogador, altera qualquer pelada e apaga qualquer usuário. Com login:

- só o organizador altera a própria pelada e lança a súmula;
- o jogador confirma a própria presença;
- `POST /api/usuarios` vira cadastro, com senha (`BCrypt`) — hoje nem existe
  campo de senha;
- some a necessidade de mandar `organizadorId` no corpo: ele sai do token.

### 6.3 Passo 7 — testes automatizados nas camadas que faltam

A bateria deste passo é **manual e não roda no CI**. O que ela verificou precisa
virar teste de verdade:

| Camada | Ferramenta | O que fixar |
|--------|-----------|-------------|
| Controller | `@WebMvcTest` + `MockMvc` | rotas, status, serialização, validação |
| Service | JUnit + Mockito | as regras de negócio, uma a uma |
| Integração | `@SpringBootTest` + Testcontainers | o roteiro deste documento, automatizado |

Testcontainers ainda resolve um incômodo do Passo 3: os testes de repositório
hoje exigem **o MySQL do desenvolvedor no ar**.

### 6.4 Passo 8 — Flyway no lugar do `ddl-auto: update`

`ddl-auto: update` cria coluna, mas **não renomeia, não remove e não migra
dado**. A coluna `estrelas` já nasceu `nullable` por causa disso (Passo 3, §3.3).
Cada esquema novo aumenta a dívida, e migrar depois só fica mais caro.

### 6.5 Funcionalidades

Herdadas do Passo 3, agora com ordem sugerida:

1. **Placar da partida** — hoje só existe súmula individual; o resultado do jogo
   e a vitória como critério de pontuação não existem.
2. **Persistir o sorteio** — guardar os times de cada pelada, pré-requisito do
   placar por time.
3. **Ranking por temporada** — recorte por período, além do recorte por pelada.
4. **Avaliação entre jogadores** alimentando as estrelas, no lugar da nota fixa
   do cadastro.
5. **Documentação da API com OpenAPI/Swagger** — o `README` é completo, mas
   escrito à mão; ele e o código vão divergir.

### 6.6 Higiene do repositório

- `.idea/` está **versionado** (inclusive `workspace.xml`, que é estado local do
  editor) — deveria estar no `.gitignore`;
- `HELP.md` é o arquivo gerado pelo Spring Initializr e não descreve o projeto;
- `pom.xml` tem `<name/>`, `<description/>`, `<url/>` e blocos de licença e
  desenvolvedor **vazios**;
- o Passo 3 já registrou que a senha antiga do MySQL pode persistir no histórico
  do GitHub — trocar a senha do banco continua pendente.

---

## 7. Como reproduzir esta bateria

```bash
# 1. suba o MySQL e a aplicação
./mvnw -DskipTests package
java -jar target/agressores-da-bola-0.0.1-SNAPSHOT.jar

# 2. exercite a API (o script está fora do repositório; ver seção 6.3 —
#    a intenção é convertê-lo em teste de integração no Passo 7)
bash smoke.sh

# 3. suíte automatizada existente
./mvnw test
```

Para conferir o N+1 da seção 3.1 com os próprios olhos, `show-sql: true` já está
ligado no `application.yaml`: basta contar as linhas `Hibernate: select` do log
entre uma requisição e outra.

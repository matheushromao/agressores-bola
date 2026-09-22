# Passo 7 — Clean Architecture no backend

**Status:** refatoração concluída — contrato da API, schema e frontend intactos
**Base:** commit `9deba75` (DTOs de resposta com `required`)

---

## 1. Objetivo

Refatorar o backend inteiro seguindo Clean Code, SOLID e Clean Architecture,
**sem mudar comportamento**: mesmas rotas, mesmos status, mesmas mensagens de
erro, mesmo schema no banco.

O código anterior já era organizado e testado. O problema não era bagunça,
e sim **onde as regras moravam**:

| Sintoma | Onde estava |
|---|---|
| Domínio anêmico: promoção da lista de espera, transição de status, vaga, pelada aberta, atributos por posição e divisão do sorteio decididos fora das entidades | `PeladaServiceImpl`, `EstatisticaServiceImpl`, `SorteioServiceImpl` |
| Entidades com `@Setter` público, alteradas por fora | `PeladaMapper.copyToEntity`, `EstatisticaServiceImpl.aplicar` |
| Mesma busca e mesma mensagem em vários lugares (`obterPelada`, `garantirQuePeladaExiste`, "Pelada não encontrada...") | 4 services |
| `exigirOrganizador` copiado | 3 services |
| Casos de uso recebendo DTO HTTP (`PeladaRequest`, `EstatisticaRequest.paraResumo()`) | todos os services |
| Casos de uso presos a detalhes: `Specification`, `JpaRepository`, `PasswordEncoder`, `TokenService` | services |
| `LocalDateTime.now()` espalhado, sem como fixar o relógio nos testes | services |
| `PeladaService` com dez métodos e dois assuntos (agenda e elenco) | ISP |
| Algoritmos de domínio dentro do service (classificação com empate, resolução da divisão) | `RankingServiceImpl`, `SorteioServiceImpl` |

---

## 2. As decisões

**Até onde ir.** Havia três caminhos: só Clean Code/SOLID mantendo os pacotes;
camadas Clean com as entidades JPA fazendo papel de domínio; ou hexagonal
completo, com POJOs puros e um segundo modelo só para persistência. Escolhido
o do meio: ele resolve todos os sintomas da tabela, sem duplicar as cinco
entidades e seus mapeamentos.

**Só o backend.** O contrato OpenAPI não muda, então o cliente gerado do
Angular continua válido e o frontend fica como está.

---

## 3. A nova estrutura

```
com.hmz.agressores_da_bola
├── domain/                 sem Spring
│   ├── model/              Pelada, ParticipacaoPelada, Usuario, EstatisticaPartida,
│   │                       ResumoEstatistico, DadosPelada, PerfilUsuario, AgendaDePeladas
│   ├── model/enums/        + Descritivel (descrição legível de qualquer enum)
│   ├── exception/          RecursoNaoEncontrado, RegraDeNegocio, AcessoNegado
│   ├── ranking/            TotaisJogador, Classificacao, OrdenacaoDoRanking
│   └── sorteio/            BalanceadorDeTimes, DivisaoDeTimes, JogadorSorteavel, TimesBalanceados
├── application/
│   ├── port/               PeladaRepository, ParticipacaoRepository, UsuarioRepository,
│   │                       EstatisticaRepository, CodificadorDeSenha, EmissorDeToken
│   ├── pelada/             PeladaService, EscalacaoService (+Impl), PeladaFiltro, mapper, dto
│   ├── usuario/            UsuarioService, AuthService (+Impl), UsuarioFiltro, mapper, dto
│   ├── estatistica/        EstatisticaService (+Impl), LancamentoEstatistica, mapper, dto
│   ├── ranking/            RankingService (+Impl), mapper, dto
│   ├── sorteio/            SorteioService (+Impl), CriterioSorteio, mapper, dto
│   └── common/             PageResponse
├── infrastructure/
│   ├── persistence/        *JpaRepository (Spring Data, package-private) + *RepositoryAdapter
│   │   └── specification/  PeladaSpecification, UsuarioSpecification
│   ├── security/           JwtConfig, JwtTokenEmissor, CodificadorDeSenhaBCrypt, JwtProperties
│   └── config/             DominioConfig (BalanceadorDeTimes e Clock)
└── web/
    ├── controller/         Auth, Pelada, Escalacao, Estatistica, Ranking, Sorteio, Usuario
    ├── dto/                requests + conversão em comando (paraDados, paraPerfil, ...)
    ├── error/              GlobalExceptionHandler, ErroResponse
    ├── openapi/            OpenApiConfig e as anotações de resposta
    └── security/           SecurityConfig, CorsProperties, UsuarioLogado
```

`SecurityConfig` e `OpenApiConfig` ficaram em `web` e não em
`infrastructure` porque escrevem o `ErroResponse`, que é contrato HTTP. Deixá-los
na infraestrutura faria `infrastructure` apontar para `web`. A chave e o
decodificador do JWT, que não sabem nada de HTTP, foram para
`infrastructure.security.JwtConfig`.

---

## 4. Domínio rico

As regras saíram dos services e foram para as entidades. O estado só muda por
métodos com nome de negócio:

| Antes (service) | Agora (domínio) |
|---|---|
| `criar`: valida horário, agenda, monta pelo mapper, adiciona o organizador | `Pelada.agendar(dados, organizador, agora, agenda)` |
| `atualizar` + `copyToEntity` | `pelada.atualizar(dados, agora, agenda)` |
| `validarTransicaoDeStatus` + `setStatus` | `pelada.alterarStatus(novo)` |
| `adicionarParticipante` (duplicidade, vaga, montagem) | `pelada.escalar(jogador, status, agora)` |
| `alterarStatusParticipacao` + `promoverPrimeiroDaListaDeEspera` | `pelada.alterarParticipacao(usuarioId, status)` |
| `removerParticipante` | `pelada.removerParticipante(usuarioId)` |
| `exigirOrganizador` em três services | `pelada.exigirOrganizador(usuarioId, "ação")` |
| `validarPeladaAberta` / `validarPeladaSorteavel` / `validarPeladaComJogo` | `garantirAberta()` / `garantirSorteavel()` / `garantirComJogo()` |
| `EstatisticaServiceImpl.registrar` + `aplicar` + `validarAtributosDaPosicao` | `participacao.lancarEstatistica(posicao, numeros)` |
| `UsuarioMapper.toEntity` / `copyToEntity` + `setSenha` | `Usuario.cadastrar(perfil, hash)` / `atualizarPerfil(perfil)` |
| `resolverQuantidadeTimes` / `resolverJogadoresPorTime` / `validarDivisao` | `DivisaoDeTimes.resolver(...)` |
| `classificar` + comparadores do ranking | `Classificacao.classificar(...)` + `OrdenacaoDoRanking` |

Resultado: um caso de uso típico passou a ter quatro linhas — carregar, checar
a posse, delegar, gravar:

```java
Pelada pelada = peladaRepository.obterComParticipantes(peladaId);
pelada.exigirOrganizadorOuProprioJogador(usuarioId, usuarioLogadoId);
ParticipacaoPelada participacao = pelada.alterarParticipacao(usuarioId, novoStatus);
peladaRepository.salvar(pelada);
```

### 4.1 A agenda do organizador

"Não marcar duas peladas no mesmo horário" é regra da pelada, mas precisa
consultar o banco. Em vez de deixá-la no service, o domínio declara o que
precisa saber, na interface `AgendaDePeladas`, e a porta `PeladaRepository` a
estende. A entidade recebe a agenda como parâmetro.

A ordem das checagens não mudou, mas o motivo vai além da compatibilidade.
Consultar a agenda **depois** de alterar a pelada faria o Hibernate dar flush
da própria pelada antes da query, e ela colidiria consigo mesma. Por isso a
agenda é consultada antes de `aplicar(dados)`.

### 4.2 Mensagens idênticas

Cada mensagem de erro foi preservada letra por letra. Elas aparecem na tela do
frontend, e os testes de mensagem continuam os mesmos. As que se repetiam viraram
fábricas (`RecursoNaoEncontradoException.pelada(id)`) ou um helper privado
(`situacao()` → "A pelada está finalizada").

---

## 5. Portas e adapters

Os casos de uso falam com o mundo por seis interfaces em `application.port`.
Os repositórios trazem `default obter*(id)`, que lançam a exceção nomeada. Isso
eliminou as buscas duplicadas:

```java
default Pelada obterComParticipantes(Long id) {
    return buscarComParticipantes(id).orElseThrow(() -> RecursoNaoEncontradoException.pelada(id));
}
```

Na infraestrutura, as interfaces Spring Data (mesmas queries, mesmos
`@EntityGraph`) viraram `*JpaRepository` **package-private**. Só o adapter do
mesmo pacote as enxerga, então nenhum caso de uso consegue pular a porta. A
montagem das `Specification` saiu do service e foi para `PeladaSpecification.de(filtro)`.

Os métodos `existsByPeladaIdAndUsuarioId` e `countByPeladaIdAndStatus` não
tinham uso e foram removidos.

---

## 6. Compromissos conscientes

| Compromisso | Por quê |
|---|---|
| Entidades de domínio com anotações JPA | Um segundo modelo só para persistência dobraria as entidades e os mapeamentos sem regra nova nenhuma. O ArchUnit barra o Spring no domínio, mas deixa passar `jakarta.persistence` |
| Responses (records com `@Schema`) são o modelo de saída dos casos de uso | São montados dentro da transação, o que evita `LazyInitializationException` na escalação e no organizador. Um modelo de saída paralelo repetiria os 15 records campo a campo |
| Portas de listagem usam `Page`/`Pageable` | É o Spring Data **Commons**, sem JPA. Uma abstração própria de paginação teria de reimplementar ordenação e o tratamento de `sort` inválido |
| `@Builder` público nas entidades | Serve só para reconstituir estado nos testes (`CenarioDePelada`). Em produção o estado nasce pelas fábricas (`agendar`, `cadastrar`, `inscrever`) |

---

## 7. Testes

| Suíte | Camada | O que mudou |
|---|---|---|
| `ArquiteturaTest` | todas | **novo** — sete regras ArchUnit da regra de dependência |
| `PeladaTest` | domínio | **novo** — as regras de pelada, direto na entidade, sem mocks |
| `ParticipacaoPeladaTest` | domínio | **novo** — súmula, posição jogada, correção do lançamento |
| `ClassificacaoTest` | domínio | **novo** — empate 1, 2, 2, 4 e limite |
| `DivisaoDeTimesTest` | domínio | **novo** — recebeu os testes de mensagem que estavam no service |
| `PeladaServiceImplTest` / `EscalacaoServiceImplTest` | aplicação | passam a testar orquestração: organizador do token, relógio, consulta à agenda, nada gravado sem posse |
| `EstatisticaServiceImplTest` / `SorteioServiceImplTest` | aplicação | idem; as regras migraram para os testes de domínio |
| `EstatisticaJpaRepositoryTest` | infraestrutura | renomeado, mesmo conteúdo |
| `PeladaControllerWebMvcTest` | web | carrega os dois controllers de pelada e o `JwtConfig` |

O `CenarioDePelada` monta a pelada no estado que cada teste precisa e usa um
relógio fixo. Ele substituiu os helpers `pelada()`/`participar()`, que se
repetiam em três classes de teste.

---

## 8. Verificação

1. **Suíte completa verde** (`./mvnw test`, com Testcontainers), antes e
   depois da refatoração.
2. **Contrato OpenAPI idêntico byte a byte.** O `/v3/api-docs` foi salvo antes
   de mexer no código e comparado com o de depois.
3. **Cliente Angular regenerado** (`ng-openapi-gen`) a partir do contrato novo,
   sem nenhuma diferença em `frontend/src/app/api`.
4. **Smoke test HTTP com 58 verificações** contra a aplicação e o MySQL reais
   (Docker Compose num projeto isolado, com volume próprio descartado no fim).
   O roteiro percorre o ciclo de vida inteiro: cadastro e login, pelada, agenda
   ocupada, lotação e lista de espera, promoção ao sair, posse, limite de vagas,
   sorteio reproduzível por semente, súmula por posição, rankings, filtros,
   perfis e exclusão. Todos os status bateram com o esperado, e o log da
   aplicação ficou sem exceções.
5. **Flyway `validate`** subiu sem divergência, já que nenhuma migration foi criada.

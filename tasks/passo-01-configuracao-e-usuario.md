# Passo 1 — Configuração do projeto e entidade `Usuario`

**Status:** concluído
**Commits:** `05d966e` (início do projeto), `28bbc4b` (model Usuario, repository JPA e estrutura MVC/SOLID)

---

## 1. Objetivo

Levantar a base do projeto: Spring Boot conectado ao MySQL, a primeira entidade
(`Usuario` — o jogador) e a estrutura de camadas que todas as próximas
funcionalidades vão seguir.

---

## 2. Configuração

### 2.1 Dependências (`pom.xml`)

| Dependência | Para quê |
|-------------|----------|
| `spring-boot-starter-webmvc` | Controllers REST |
| `spring-boot-starter-data-jpa` | Persistência com Hibernate |
| `spring-boot-starter-validation` | Bean Validation (`@NotBlank`, `@Email`, …) |
| `mysql-connector-j` | Driver do MySQL (runtime) |
| `lombok` | Getters/setters/builders sem boilerplate |
| `spring-boot-devtools` | Restart automático em desenvolvimento |

Java **25**, Spring Boot **4.1.0**.

### 2.2 Banco de dados

`application.yaml` guarda as credenciais locais e **está no `.gitignore`** —
nunca deve ser commitado. O repositório versiona apenas o
`application-example.yaml`, que serve de modelo:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/agressores-db?createDatabaseIfNotExist=true
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:senha_local}
  jpa:
    hibernate:
      ddl-auto: update   # Hibernate cria/atualiza as tabelas
    show-sql: true
```

`ddl-auto: update` é adequado ao desenvolvimento: o schema acompanha as
entidades sem migration manual.

---

## 3. Estrutura de camadas

```
com.hmz.agressores_da_bola
├── controller/UsuarioController        → endpoints REST
├── service/UsuarioService              → contrato (interface)
├── service/impl/UsuarioServiceImpl     → regras de negócio + @Transactional
├── repository/UsuarioRepository        → JpaRepository
├── mapper/UsuarioMapper                → entidade ⇄ DTO
├── model/Usuario                       → entidade JPA
├── model/enums/Posicao                 → posições em campo
├── dto/UsuarioRequest / UsuarioResponse
└── exception/                          → erros de domínio + handler global
```

### Como os princípios SOLID aparecem aqui

- **S (Responsabilidade única):** o controller só cuida de HTTP; o service, de
  regra de negócio; o mapper, de conversão; o repository, de dados. O mapper
  existe justamente para o service não acumular a responsabilidade de montar DTO.
- **O (Aberto/fechado):** novas regras entram no service sem alterar controller
  ou repository. Novas posições entram no enum `Posicao` sem tocar em nada mais.
- **L (Substituição de Liskov):** `UsuarioServiceImpl` cumpre integralmente o
  contrato de `UsuarioService` — qualquer outra implementação pode substituí-la.
- **I (Segregação de interfaces):** `UsuarioService` expõe só as operações do
  caso de uso de usuário, sem métodos genéricos que ninguém usa.
- **D (Inversão de dependência):** o controller declara
  `private final UsuarioService usuarioService` (a abstração). O Spring injeta a
  implementação via construtor, gerado pelo `@RequiredArgsConstructor`.

---

## 4. A entidade `Usuario`

Tabela `tb_usuarios`.

| Campo | Tipo | Restrições |
|-------|------|-----------|
| `id` | Long | PK, auto increment |
| `nomeCompleto` | String(120) | obrigatório |
| `nickname` | String(30) | obrigatório, **único** |
| `descricao` | String(500) | opcional |
| `numeroCelular` | String(20) | obrigatório |
| `email` | String(150) | obrigatório, **único** |
| `idade` | Integer | obrigatório |
| `posicao` | enum `Posicao` | obrigatório, gravado como texto |
| `nacionalidade` | String(60) | obrigatório |

Detalhes de implementação:

- `@Enumerated(EnumType.STRING)` grava `"ATACANTE"` no banco em vez do índice
  numérico — se a ordem do enum mudar, os dados continuam corretos.
- `equals`/`hashCode` baseados apenas no `id`, com `hashCode` fixo por classe.
  É o padrão recomendado para entidades JPA: evita que um objeto "suma" de um
  `HashSet` quando o Hibernate atribui o id depois do `persist`.

### Enum `Posicao`

`GOLEIRO`, `ZAGUEIRO`, `LATERAL_DIREITO`, `LATERAL_ESQUERDO`, `VOLANTE`,
`MEIA`, `PONTA_DIREITA`, `PONTA_ESQUERDA`, `ATACANTE` — cada um com uma
`descricao` legível, devolvida na API como `posicaoDescricao`.

---

## 5. DTOs e validação

A entidade nunca aparece na API. A entrada e a saída são `records` imutáveis:

- **`UsuarioRequest`** — o que o cliente envia, com as validações declarativas:
  `@NotBlank`, `@Size`, `@Email`, `@Min`/`@Max` para idade (12 a 100) e
  `@Pattern` no celular (`(11) 91234-5678`).
- **`UsuarioResponse`** — o que a API devolve, incluindo o campo derivado
  `posicaoDescricao`.

O `@Valid` no controller dispara as validações antes de qualquer código de
negócio rodar.

---

## 6. Regras de negócio (no service)

Validação de formato fica no DTO; validação que **precisa consultar o banco**
fica no service:

- `nickname` único — 409 se já existir.
- `email` único — 409 se já existir.
- Na atualização, o próprio registro é ignorado na checagem (senão o usuário não
  conseguiria salvar mantendo o próprio nickname).

Controle transacional: `@Transactional` na escrita, `@Transactional(readOnly = true)`
na leitura.

---

## 7. Tratamento de erros

Todo erro sai no mesmo formato (`ErroResponse`), montado pelo
`@RestControllerAdvice`:

```json
{
  "timestamp": "2026-07-30T23:05:07.38",
  "status": 409,
  "erro": "Regra de negócio violada",
  "mensagem": "O nickname 'pelé' já está em uso",
  "campos": null
}
```

| Exceção | HTTP | Quando |
|---------|------|--------|
| `RecursoNaoEncontradoException` | 404 | id/nickname inexistente |
| `RegraDeNegocioException` | 409 | nickname ou e-mail duplicado |
| `MethodArgumentNotValidException` | 400 | falha de Bean Validation (preenche `campos`) |

---

## 8. Endpoints entregues

| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/api/usuarios` | Cria usuário (201 + header `Location`) |
| `GET` | `/api/usuarios` | Lista usuários |
| `GET` | `/api/usuarios/{id}` | Busca por id |
| `GET` | `/api/usuarios/nickname/{nickname}` | Busca por nickname |
| `PUT` | `/api/usuarios/{id}` | Atualiza |
| `DELETE` | `/api/usuarios/{id}` | Remove (204) |

> A listagem foi alterada no Passo 2 para devolver resultado paginado.

---

## 9. Observação de segurança

O histórico do Git foi reescrito neste passo para remover o `application.yaml`
com a senha do banco do commit inicial. A partir daqui, credenciais só entram
via `application.yaml` local (ignorado) ou variáveis de ambiente.

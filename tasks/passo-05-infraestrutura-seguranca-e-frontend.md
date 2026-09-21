# Passo 5 — Infraestrutura, segurança e o caminho até o frontend

**Status:** etapas 1 a 4 aplicadas; etapa 5 em andamento (ver [passo 06](passo-06-frontend-angular.md))
**Base:** commit `db41f19` (posições de futsal)

---

## 1. Objetivo

Definir **em que ordem** atacar o roadmap do README e, principalmente, **por
quê** nessa ordem. A pergunta que originou este documento foi direta: *melhoro
algo no backend ou parto para Docker e frontend?*

A resposta curta: **Docker sim, agora; frontend ainda não.** As seções abaixo
justificam as duas metades dessa frase.

> Este documento não altera código. É o roteiro a executar.

---

## 2. A evidência: o que a sessão de 08/09/2026 revelou

A troca das posições para futsal (Passo 5.0, já commitada em `db41f19`) foi uma
mudança de duas linhas em dois enums. Mesmo assim consumiu a maior parte de uma
sessão. **O código não foi o problema** — o ambiente foi. Dois itens do roadmap
cobraram a fatura no mesmo dia:

### 2.1 `ddl-auto: update` não migra dados

Descoberta não óbvia: apesar de `@Enumerated(EnumType.STRING)` com
`@Column(length = 20)`, o Hibernate 7 **não** gera `VARCHAR(20)` no MySQL. Ele
gera uma coluna **`ENUM('A','B',...)` nativa**.

Consequência: quando a lista de valores muda e existem linhas que não cabem na
lista nova, o `ddl-auto: update` **não reescreve a coluna** e a inserção falha
com uma mensagem que não menciona enum nenhum:

```
Data truncated for column 'posicao' at row 1
```

Curiosidade que confirma o diagnóstico: `tipo_campo` migrou sozinho, porque as
duas peladas existentes já eram `FUTSAL`/`SOCIETY` e couberam na lista nova.
Só `posicao` travou, porque havia linhas com `ZAGUEIRO` e `MEIA`.

O desfecho foi apagar uma base com 57 registros. **Com Flyway isso teria sido um
arquivo** — versionado, revisável e aplicável em qualquer ambiente:

```sql
-- V2__posicoes_futsal.sql
ALTER TABLE tb_usuarios MODIFY posicao
  ENUM('GOLEIRO','ZAGUEIRO','LATERAL_DIREITO','LATERAL_ESQUERDO','VOLANTE',
       'MEIA','PONTA_DIREITA','PONTA_ESQUERDA','ATACANTE',
       'ALA','FIXO','PIVO','AMADOR') NOT NULL;   -- superconjunto

UPDATE tb_usuarios SET posicao = CASE posicao
  WHEN 'ZAGUEIRO'        THEN 'FIXO'
  WHEN 'VOLANTE'         THEN 'FIXO'
  WHEN 'LATERAL_DIREITO' THEN 'ALA'
  WHEN 'LATERAL_ESQUERDO'THEN 'ALA'
  WHEN 'MEIA'            THEN 'ALA'
  WHEN 'PONTA_DIREITA'   THEN 'ALA'
  WHEN 'PONTA_ESQUERDA'  THEN 'ALA'
  WHEN 'ATACANTE'        THEN 'PIVO'
  ELSE posicao END;                              -- GOLEIRO permanece

ALTER TABLE tb_usuarios MODIFY posicao
  ENUM('GOLEIRO','ALA','FIXO','PIVO','AMADOR') NOT NULL;   -- lista final
```

A sequência **superconjunto → de-para → lista final** é o padrão para qualquer
mudança de enum daqui em diante.

### 2.2 Os testes dependem da máquina do desenvolvedor

`EstatisticaPartidaRepositoryTest` usa `@DataJpaTest` com
`@AutoConfigureTestDatabase(replace = Replace.NONE)`, e não existe
`src/test/resources`. Ou seja: a suíte roda contra o **MySQL real da máquina**.

Na prática havia dois MySQL disputando a porta 3306 — o MariaDB do XAMPP
(serviço `mysql`, root sem senha) e o MySQL Server 8.0 (serviço `MySQL80`, o
que casa com o `application.yaml`). O XAMPP estava ocupando a porta, e a suíte
falhava com `Access denied for user 'root'@'localhost'` — erro que *parece* bug
do código recém-alterado e não é.

Isso é exatamente o que Testcontainers resolve.

### 2.3 A conclusão que importa

No estado atual do projeto, **Docker não é desvio da qualidade — é a
qualidade**. Ele não compete com "melhorar alguma coisa"; ele é a melhoria mais
rentável disponível, porque elimina uma classe inteira de problema que já se
manifestou.

---

## 3. Etapa 1 — Flyway e perfis de configuração

**Faça primeiro, e faça agora.** A base está vazia neste momento (foi recriada
em 08/09), o que é a condição ideal para baselinar: sem drift e sem dado a
preservar. Daqui a algumas semanas de uso, o mesmo trabalho custa bem mais.

### 3.1 Dependências

O Spring Boot gerencia as versões do Flyway, então **não** pinar versão aqui.
Para MySQL o módulo específico é obrigatório desde o Flyway 8.

⚠️ **No Spring Boot 4 só `flyway-core` não basta.** A autoconfiguração saiu do
núcleo para o módulo `spring-boot-flyway`; sem o starter o Flyway fica no
classpath e simplesmente não roda:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-flyway</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

### 3.2 Baseline

1. Gere o DDL atual do schema limpo — o jeito mais confiável é subir a app uma
   última vez com `ddl-auto: update` e extrair:
   ```bash
   mysqldump -u root -p --no-data --skip-add-drop-table agressores_db \
     > src/main/resources/db/migration/V1__schema_inicial.sql
   ```
   Limpe do arquivo o cabeçalho do dump (linhas `/*!40101 ...`), os
   `AUTO_INCREMENT=n` e o comentário de versão do servidor.
2. Troque para `ddl-auto: validate`. **Este é o ponto da etapa inteira:** o
   Hibernate passa a *conferir* o schema em vez de mexer nele, e qualquer
   divergência entre entidade e banco vira erro no start — não surpresa em
   produção.
3. Da segunda migration em diante, o nome segue `V2__descricao.sql`.

### 3.3 Perfis

Divida `application.yaml` em três, mantendo o `application.yaml` base apenas com
o que é comum:

| Perfil | `ddl-auto` | Uso |
|--------|-----------|-----|
| `dev`  | `validate` | Máquina local, `show-sql: true` |
| `test` | `validate` | Testcontainers (Etapa 2) |
| `prod` | `validate` | Nunca `update`, nunca `create` |

Credenciais saem do arquivo e viram variável de ambiente
(`${DB_USERNAME}`, `${DB_PASSWORD}`) — o `application-example.yaml` já usa esse
formato. Vale lembrar que o histórico deste repositório já precisou ser
reescrito uma vez para remover uma senha commitada; variável de ambiente é o
que impede a segunda vez.

---

## 4. Etapa 2 — Docker Compose e Testcontainers

⚠️ **Atenção a uma confusão comum:** Compose e Testcontainers resolvem
problemas **diferentes**. Docker Compose sobe o ambiente para você *rodar* a
aplicação; ele **não** conserta a dependência dos testes com o MySQL da sua
máquina. Pegue os dois.

### 4.1 Docker Compose — rodar a aplicação

`compose.yaml` na raiz, com dois serviços (app + MySQL 8), volume nomeado para
os dados e `healthcheck` no banco para a app não subir antes do MySQL aceitar
conexão. O `Dockerfile` da app deve ser multi-stage (build com Maven, runtime
só com o JRE) para a imagem final não carregar o Maven junto.

Ganho concreto: `docker compose up` passa a ser a única instrução do "como
rodar" no README, e some o problema de qual serviço MySQL está ligado.

### 4.2 Testcontainers — rodar os testes

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-mysql</artifactId>
    <scope>test</scope>
</dependency>
```

⚠️ **O Boot 4.1 traz o Testcontainers 2.x**, que renomeou os artefatos
(`testcontainers-mysql`, não `mysql`) e moveu a classe para
`org.testcontainers.mysql.MySQLContainer`, sem o genérico `<?>`.

Com `@ServiceConnection`, o Spring Boot injeta sozinho URL, usuário e senha do
contêiner — não é preciso `@DynamicPropertySource`. Declarar o contêiner como
bean numa `@TestConfiguration` permite que todos os testes com banco a importem:

```java
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        return new MySQLContainer(DockerImageName.parse("mysql:8.0"));
    }
}

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class EstatisticaPartidaRepositoryTest { ... }
```

No Compose, o MySQL fica exposto no host na **3308** por padrão
(`DB_HOST_PORT` no `.env` troca), para conviver com um MySQL local na 3306 e
com outros contêineres que já usem a 3307.

Ganhos: `./mvnw test` deixa de exigir MySQL instalado, o Flyway roda contra um
banco descartável (o que **testa as migrations**, não só o código), e cada
execução parte de um estado idêntico.

Custo honesto: exige Docker rodando e a suíte fica alguns segundos mais lenta.
Vale cada segundo.

---

## 5. Etapa 3 — Spring Security, JWT e testes das regras

### 5.1 Por que antes do frontend

Hoje a API **não tem autenticação nenhuma**: qualquer um cria, edita ou apaga
qualquer usuário e qualquer pelada. Além do risco óbvio, existe um custo de
ordem: se o frontend for construído contra uma API aberta e a segurança entrar
depois, **refaz-se toda a camada HTTP do front** — tela de login, storage do
token, interceptor, guards de rota, tratamento de 401, refresh.

Autenticação é o que molda a arquitetura de um frontend desde a primeira tela.
Fazer depois não é catastrófico, mas é retrabalho garantido e evitável.

### 5.2 O que implementar

- `spring-boot-starter-security-oauth2-resource-server` (nome do Boot 4), que
  traz o Spring Security e o Nimbus: o próprio Resource Server valida o JWT
  HS256, sem biblioteca de terceiros nem filtro escrito à mão
- Campo de senha em `Usuario` com `BCryptPasswordEncoder` — **nunca** em texto
  puro, e nunca exposto em `UsuarioResponse`
- `POST /api/auth/login` devolvendo o token; filtro de autenticação na cadeia
- Papéis: um organizador só administra **a pelada que ele criou**. Isso é regra
  de negócio, então vive no service, não no controller
- CORS liberado para a origem do frontend (necessário na Etapa 5)

### 5.3 Testes, junto e não depois

Esta é a etapa em que os testes de service e controller entram — **não como
fase separada**. É aqui que as regras ficam interessantes o bastante para
justificar o teste.

Cobertura atual: 10 testes, sendo 6 do balanceador (unitários, puros) e 3 de uma
query. As **regras de negócio não têm teste nenhum** — nem a validação de
atributos de goleiro em `EstatisticaServiceImpl`, nem lotação e status de pelada
em `PeladaServiceImpl`. Comece por essas duas com Mockito, e use `@WebMvcTest`
para os controllers.

---

## 6. Etapa 4 — OpenAPI / Swagger UI

Barato e alto retorno: uma dependência e um pouco de configuração dão
documentação interativa e, principalmente, **um contrato formal** para o
frontend consumir — inclusive gerando cliente tipado.

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>${springdoc.version}</version>   <!-- 3.1.1 -->
</dependency>
```

> ⚠️ O springdoc **não** é gerenciado pelo BOM do Spring Boot, então a versão é
> obrigatória. E este projeto está em **Spring Boot 4.1**, bem à frente da
> maioria dos tutoriais (escritos para Boot 3.x / springdoc 2.x). Confira a
> versão compatível na documentação oficial antes de fixar — não copie de
> tutorial.

Fazer o frontend **antes** desta etapa significa escrever os DTOs à mão duas
vezes e vê-los divergir com o tempo.

### 6.1 O que foi feito

**Versão: springdoc 3.1.1.** A conferência do aviso acima deu no que se
esperava — a linha **2.x é do Boot 3** e a **3.x é a do Boot 4**. Copiar a
versão de um tutorial teria colocado uma 2.x incompatível no `pom`.

Havia um risco a mais, não previsto quando este documento foi escrito: o Boot 4
usa **Jackson 3** (`tools.jackson`, visível no `SecurityConfig`) e o springdoc
3.1.1 ainda carrega Jackson 2 internamente. Os dois pacotes coexistem, então
funciona — mas isso foi **verificado antes de anotar qualquer coisa**, subindo a
aplicação e pedindo o `/v3/api-docs`. Anotar 25 endpoints e só então descobrir
uma incompatibilidade de base seria a ordem errada de trabalho.

**Segurança.** `anyRequest().authenticated()` barraria o Swagger com 401, então
`SecurityConfig` ganhou a constante `DOCUMENTACAO_PUBLICA` ao lado de
`LEITURAS_PUBLICAS`. O contrato declara o esquema `bearer-jwt` e uma exigência
**global** de token; os endpoints públicos se eximem com
`@SecurityRequirements({})`, um a um, espelhando exatamente a cadeia de filtros.
O resultado é conferível: no JSON gerado, as 10 operações públicas trazem
`"security": []` e as 15 protegidas herdam a exigência global.

**401 e 403 sem repetição.** Em vez de dois `@ApiResponse` em cada um dos 25
métodos, um `OpenApiCustomizer` injeta as duas respostas em toda operação que
exija token. O que sobra — 400, 404 e 409 — virou metanotação em
`config/openapi/`, todas apontando para o `ErroResponse`, que já é o contrato
único de erro da API.

**Fora de produção.** `springdoc.api-docs.enabled: false` no perfil `prod`.
Documentação interativa é ferramenta de desenvolvimento; em produção ela
publicaria o mapa completo da API. Verificado: com `SPRING_PROFILES_ACTIVE=prod`
a API responde normalmente e as rotas da documentação dão 404.

**Teste.** `DocumentacaoOpenApiTest` guarda a regressão clássica — alguém aperta
o `SecurityConfig`, o Swagger volta a dar 401 e ninguém percebe até o frontend
reclamar.

---

## 7. Etapa 5 — Frontend

Só agora. Chegando aqui, o frontend encontra: contrato documentado em OpenAPI,
autenticação definida, ambiente reprodutível em Docker e schema versionado.

Sugestão de escopo para a primeira versão, na ordem em que agregam valor:

1. Login e cadastro de jogador
2. Lista de peladas com os filtros que a API já oferece (`status`, `tipoCampo`,
   `cidade`, período) e paginação — o backend já entrega tudo pronto em
   `PageResponse`
3. Detalhe da pelada com confirmação de presença
4. Tela de sorteio exibindo os times lado a lado — visualmente a mais gratificante,
   e a que melhor demonstra o projeto
5. Súmula e rankings

---

## 8. Extra — melhoria do sorteio, quando quiser algo pequeno

Já especificada na seção 4 do [Passo 4](passo-04-auditoria-de-camadas-e-testes-de-endpoints.md):
permitir a troca **goleiro ↔ goleiro** no refino do `BalanceadorDeTimes`.

Hoje o refino recusa mexer em qualquer goleiro, o que preserva a regra de um por
time mas fecha um grau de liberdade. Com dois goleiros de notas distantes
(5.0 e 1.0), o resultado trava em `11×7`. Trocar goleiro por goleiro mantém a
regra intacta e abre a possibilidade que falta.

É bem delimitado, o código já tem teste (`BalanceadorDeTimesTest`), e não
bloqueia nada. Cerca de uma hora.

---

## 9. Checklist

```
Etapa 1 — Flyway e perfis
  [x] spring-boot-starter-flyway + flyway-mysql no pom
  [x] V1__schema_inicial.sql a partir do schema limpo
  [x] ddl-auto: validate
  [x] perfis dev / prod (main) e test (src/test/resources)
  [x] credenciais em variável de ambiente / .env

Etapa 2 — Docker
  [x] Dockerfile multi-stage
  [x] compose.yaml (app + mysql, healthcheck, volume)
  [x] Testcontainers com @ServiceConnection
  [x] ./mvnw test passando sem MySQL instalado

Etapa 3 — Segurança
  [x] Spring Security + JWT, senha com BCrypt
  [x] POST /api/auth/login (e POST /api/auth/cadastro)
  [x] autorização por dono da pelada (no service)
  [x] CORS para a origem do front
  [x] testes de EstatisticaServiceImpl e PeladaServiceImpl
  [~] @WebMvcTest nos controllers — feito para PeladaController; os demais seguem o mesmo padrão

Etapa 4 — OpenAPI
  [x] springdoc 3.1.1 (a linha 3.x é a do Boot 4)
  [x] anotar os controllers — 25 operações, 6 tags
  [x] Swagger UI acessível, com Authorize funcionando
  [x] desligado no perfil prod
  [x] DocumentacaoOpenApiTest cobrindo o contrato

Etapa 5 — Frontend (Angular; ver passo 06)
  [x] login e cadastro
  [x] lista de peladas com filtros e paginação
  [x] detalhe e confirmação de presença
  [ ] tela de sorteio
  [ ] súmula e rankings

Extra
  [ ] troca goleiro ↔ goleiro no refino do sorteio
```

---

## 10. Se a prioridade for outra

O roteiro acima assume que o objetivo é **portfólio ou uso real**. Se o que
estiver faltando for **motivação**, a ordem muda: o frontend é o que faz o
projeto parecer vivo, e o retrabalho de autenticação descrito em 5.1 é chato mas
limitado — algumas horas, não semanas.

Nesse caso, o mínimo que eu ainda faria antes é a **Etapa 1**, porque é a única
cujo custo cresce de verdade com o tempo: cada semana de uso adiciona dados que
tornam o baseline do Flyway mais difícil.

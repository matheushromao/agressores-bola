# Tasks — Agressores da Bola

Documentação incremental do projeto. Cada passo do desenvolvimento vira um
arquivo aqui, descrevendo **o que foi feito**, **por quê** e **como usar**.

## Índice

| Passo | Documento | Conteúdo |
|-------|-----------|----------|
| 1 | [passo-01-configuracao-e-usuario.md](passo-01-configuracao-e-usuario.md) | Setup do projeto, conexão MySQL, entidade `Usuario`, camadas MVC, tratamento global de erros |
| 2 | [passo-02-pelada-e-paginacao.md](passo-02-pelada-e-paginacao.md) | Entidade `Pelada`, escalação de participantes, paginação no backend (`Pageable`) |
| 3 | [passo-03-estatisticas-ranking-e-sorteio.md](passo-03-estatisticas-ranking-e-sorteio.md) | Súmula por jogador, tabela de pontuação, rankings agregados e sorteio de times equilibrado por estrelas |
| 4 | [passo-04-auditoria-de-camadas-e-testes-de-endpoints.md](passo-04-auditoria-de-camadas-e-testes-de-endpoints.md) | Auditoria de SOLID/MVC camada a camada, exercício de todos os endpoints via HTTP (132 verificações) e roteiro dos próximos passos |
| 5 | [passo-05-infraestrutura-seguranca-e-frontend.md](passo-05-infraestrutura-seguranca-e-frontend.md) | Roteiro priorizado: Flyway e perfis, Docker Compose e Testcontainers, Spring Security com JWT, OpenAPI e, por fim, o frontend |
| 6 | [passo-06-frontend-angular.md](passo-06-frontend-angular.md) | SPA em Angular com cliente gerado do OpenAPI: login, cadastro, peladas, escalação, sorteio de times, súmula e rankings |
| 7 | [passo-07-clean-architecture.md](passo-07-clean-architecture.md) | Refatoração do backend para Clean Architecture: domínio rico, casos de uso com portas e adapters, regra de dependência verificada pelo ArchUnit, contrato da API intacto |

## Sobre o projeto

API REST para organização de peladas (futebol amador), inspirada no app
**Appito**: o usuário cria uma partida com data, horário, local e valor, e
monta o grupo de jogadores que vai jogar.

**Backend:** Java 25 · Spring Boot 4.1.0 · Spring Data JPA · Bean Validation ·
Spring Security (JWT) · Flyway · springdoc-openapi · MySQL 8 · Docker ·
Testcontainers · Lombok · Maven

**Frontend:** Angular 22 · Tailwind CSS 4 · cliente gerado do contrato OpenAPI

## Arquitetura

Desde o passo 7 o backend segue Clean Architecture, com as dependências
apontando só para dentro:

```
domain/          → regras de negócio: entidades com comportamento, objetos de valor,
                   classificação do ranking e sorteio — sem Spring
application/     → casos de uso (interface + implementação), comandos, mappers e responses
  port/          → portas que os casos de uso exigem: repositórios, senha, token
infrastructure/  → adapters: Spring Data JPA, specifications, JWT, BCrypt, beans do domínio
web/             → controllers, requests com Bean Validation, handler de erros,
                   OpenAPI e filtro de segurança
```

**Regra de dependência:** `web → application → domain`; `infrastructure`
implementa as portas de `application`. O domínio não conhece Spring, HTTP nem
banco, e o `ArquiteturaTest` (ArchUnit) quebra o build se alguma seta apontar
para fora. A entidade JPA continua nunca exposta na API — tudo entra como
comando e sai como response.

## Como rodar

```bash
# 1. Informe as credenciais do MySQL num .env na raiz (ignorado pelo Git)
printf 'DB_USERNAME=root\nDB_PASSWORD=sua_senha\nJWT_SECRET=%s\n' "$(openssl rand -base64 48)" > .env

# 2. Suba o projeto inteiro — front, API e MySQL (só exige Docker)
docker compose up --build       # o app fica em http://localhost:3000
#    ...ou rode fora do Docker, contra o MySQL local
./mvnw spring-boot:run

# 3. Rode a suíte de testes (exige o Docker no ar; o MySQL vem do Testcontainers)
./mvnw test
```

O app sobe em `http://localhost:3000`, a API em `http://localhost:8080`, e a
documentação interativa fica em `http://localhost:8080/swagger-ui.html`.

> A referência completa da API — endpoints, exemplos, regras de negócio e notas
> de segurança — está no [README do projeto](../README.md). Os documentos desta
> pasta contam **como cada passo foi construído e por quê**; o README conta
> **como usar o que existe hoje**.

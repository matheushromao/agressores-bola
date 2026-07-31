# Tasks — Agressores da Bola

Documentação incremental do projeto. Cada passo do desenvolvimento vira um
arquivo aqui, descrevendo **o que foi feito**, **por quê** e **como usar**.

## Índice

| Passo | Documento | Conteúdo |
|-------|-----------|----------|
| 1 | [passo-01-configuracao-e-usuario.md](passo-01-configuracao-e-usuario.md) | Setup do projeto, conexão MySQL, entidade `Usuario`, camadas MVC, tratamento global de erros |
| 2 | [passo-02-pelada-e-paginacao.md](passo-02-pelada-e-paginacao.md) | Entidade `Pelada`, escalação de participantes, paginação no backend (`Pageable`) |

## Sobre o projeto

API REST para organização de peladas (futebol amador), inspirada no app
**Appito**: o usuário cria uma partida com data, horário, local e valor, e
monta o grupo de jogadores que vai jogar.

**Stack:** Java 25 · Spring Boot 4.1.0 · Spring Data JPA · Bean Validation ·
MySQL 8 · Lombok · Maven

## Arquitetura

O projeto segue MVC com separação de camadas e princípios SOLID:

```
controller/   → recebe HTTP, valida o formato da entrada, devolve status code
service/      → interface do caso de uso (contrato)
service/impl/ → regras de negócio e controle transacional
repository/   → acesso a dados (Spring Data JPA)
  specification/ → filtros dinâmicos e combináveis das listagens
mapper/       → conversão entidade ⇄ DTO
model/        → entidades JPA
  enums/      → domínios fechados (posição, status, tipo de campo)
dto/          → contratos de entrada e saída da API (records)
exception/    → exceções de domínio e handler global
```

**Regra de dependência:** o controller depende da *interface* do service, nunca
da implementação (Dependency Inversion). A entidade JPA nunca é exposta na API —
tudo entra e sai como DTO.

## Como rodar

```bash
# 1. Configure o banco (o application.yaml está no .gitignore)
cp src/main/resources/application-example.yaml src/main/resources/application.yaml
# edite usuário e senha do MySQL

# 2. Suba a aplicação
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`.

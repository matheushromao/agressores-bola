# Frontend — Agressores da Bola

SPA em **Angular 22** que consome a API REST da raiz deste repositório.
Standalone components, signals e `resource()`; estilo com **Tailwind CSS 4**.

## Rodar

Tudo junto, pelo Compose na raiz do repositório — não precisa de Node instalado:

```bash
docker compose up --build       # http://localhost:3000
```

Para desenvolver, com recarga automática:

```bash
docker compose up -d app        # só a API, na 8080
cd frontend && npm install
npm start                       # http://localhost:4200
```

Nos dois modos o browser conversa com uma origem só e CORS não entra no
caminho: em desenvolvimento pelo `proxy.conf.json`, e no Compose pelo nginx do
`Dockerfile`, que serve os estáticos e encaminha `/api`, `/v3` e `/swagger-ui`
para a aplicação.

## Cliente da API

Os tipos e as funções de chamada em `src/app/api` são **gerados** a partir do
contrato OpenAPI publicado pelo backend — não edite nada ali:

```bash
npm run gen:api    # exige a API no ar
```

## Testes

```bash
npm test           # Vitest + jsdom, sem browser e sem backend
```

## Estrutura

```
Dockerfile     build dos estáticos e runtime nginx (usado pelo Compose)
nginx.conf     fallback de SPA, cache dos arquivos com hash e proxy da API
src/app/
  api/         cliente gerado do OpenAPI (não editar)
  core/        sessão, interceptor de token, guard e tradução de erros
  shared/      rótulos de enum e formatação de data/hora
  paginas/     login, cadastro, peladas (lista, formulário, detalhe, sorteio,
               súmula) e rankings
```

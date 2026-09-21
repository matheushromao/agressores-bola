# Frontend — Agressores da Bola

SPA em **Angular 22** que consome a API REST da raiz deste repositório.
Standalone components, signals e `resource()`; estilo com **Tailwind CSS 4**.

## Rodar

```bash
# 1. suba a API (na raiz do repositório)
docker compose up -d

# 2. suba o frontend
cd frontend
npm install
npm start          # http://localhost:4200
```

O `proxy.conf.json` manda `/api` e `/v3` para `localhost:8080`, então o browser
fala só com a origem 4200 e CORS não entra no caminho em desenvolvimento.

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
src/app/
  api/         cliente gerado do OpenAPI (não editar)
  core/        sessão, interceptor de token, guard e tradução de erros
  shared/      rótulos de enum e formatação de data/hora
  paginas/     login, cadastro, peladas (lista, detalhe, sorteio, súmula) e rankings
```

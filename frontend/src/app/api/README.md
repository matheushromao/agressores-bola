# Cliente gerado — não edite à mão

Tudo neste diretório sai do contrato OpenAPI da API
(`http://localhost:8080/v3/api-docs`), gerado por `ng-openapi-gen`.

```bash
npm run gen:api     # exige o backend no ar
```

Qualquer alteração feita aqui é perdida na próxima geração. Precisa mudar um
tipo ou o nome de uma operação? Mude no **backend** — as anotações
`@Schema`, `@Operation(operationId = ...)` e `@Parameter` dos controllers são
a fonte. O arquivo é versionado para que o frontend compile sem o backend no ar.

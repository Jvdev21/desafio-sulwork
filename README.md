# Café da Manhã MV — Desafio Sulwork

Aplicação web para organizar cafés da manhã entre colaboradores, cadastrar participantes e itens, evitar duplicidades e acompanhar se cada item foi levado.

## Sobre o desafio

Projeto full stack desenvolvido para o Desafio Sulwork com Angular, Spring Boot, PostgreSQL, Docker, persistência por NativeQuery e testes automatizados.

## Como executar o projeto

> Para executar a aplicação completa localmente, basta ter Docker Desktop disponível e executar o Docker Compose na raiz do projeto.

```bash
docker compose up --build
```

- Frontend: `http://localhost:4200`
- Swagger: `http://localhost:8080/swagger-ui.html`
- OpenAPI: `http://localhost:8080/v3/api-docs`

Verifique os serviços com:

```bash
docker compose ps
```

Para parar:

```bash
docker compose down
```

O comando `docker compose down -v` também remove os dados persistidos do PostgreSQL e deve ser usado com cuidado.

### Execução manual

Requer Java 17 e Node.js. Inicie cada parte em seu terminal.

#### PostgreSQL

Na raiz do projeto:

```bash
docker compose up -d postgres
```

#### Backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

#### Frontend

Em outro terminal:

```bash
cd frontend
npm install
npm start
```

O frontend fica em `http://localhost:4200` e encaminha `/api` para o backend local em `http://localhost:8080`.

## Testes

Resultados revalidados em 28/08/2026:

### Backend

```powershell
cd backend
.\mvnw.cmd test
```

**85 testes aprovados**, sem falhas, erros ou testes ignorados.

### Frontend

```bash
cd frontend
npm test
```

**26 testes aprovados** com Vitest.

### Cypress E2E

Com a stack Docker em execução e dentro de `frontend`:

```bash
npm run e2e
```

**7 cenários aprovados**, sem falhas, pendências ou testes ignorados.

## Funcionalidades

- CRUD de colaboradores com CPF válido e único.
- Cadastro, listagem, edição e exclusão de cafés futuros.
- Inclusão e remoção de participantes.
- Vários itens diferentes em uma única participação.
- Item único por café e permitido novamente em outro café.
- Estados `PENDENTE`, `TROUXE` e `NAO_TROUXE`.
- Swagger/OpenAPI e mensagens objetivas de erro.
- Interface responsiva com Angular Material e identidade visual MV.

## Regras de negócio

### Colaborador

- CPF válido, normalizado, com 11 dígitos e único.
- Apenas uma participação do colaborador por café.

### Café

- A data deve ser futura no cadastro.
- Existe um único café por data, por decisão de modelagem.

### Itens

- Cada colaborador pode levar vários itens diferentes na mesma participação.
- A mesma opção não pode se repetir no mesmo café, inclusive entre colaboradores.
- O mesmo item é permitido em outro café.
- A comparação normaliza caixa, espaços e acentos, preservando o nome de apresentação.

### Status

- Antes do café: `PENDENTE`, sem alteração permitida.
- No dia: permite `TROUXE` ou `NAO_TROUXE`, sem retorno para `PENDENTE`.
- Depois: um pendente é apresentado como `NAO_TROUXE`, sem atualização silenciosa no banco.

## Tecnologias

- Java 17 e Spring Boot 3.5.16
- Maven 3.9.11 via Wrapper
- PostgreSQL 17 e Flyway
- Spring Data JPA e NativeQuery
- Swagger/OpenAPI com springdoc 2.9.0
- JUnit 5, Mockito e Testcontainers
- Angular 22.1.3 e Angular Material 22.1.3
- TypeScript 6.0.3 e Vitest 4.1.11
- Cypress 15.21.1
- Docker, Docker Compose e Nginx

## Arquitetura

```text
Angular
   ↓ HTTP/JSON
Spring Boot REST
   ↓
Service
   ↓
Repository
   ↓ NativeQuery
PostgreSQL
```

- **Controller:** expõe os endpoints REST e define os códigos HTTP.
- **DTO:** estabelece o contrato da API sem expor entities.
- **Service:** concentra regras de negócio e transações.
- **Repository:** executa as operações em SQL nativo.

## Estrutura do projeto

```text
desafio-sulwork/
├── backend/            # API Spring Boot, migrations e testes
├── frontend/           # Angular, testes Vitest e Cypress
├── docker-compose.yml
├── README.md
└── .gitignore
```

No backend, os pacotes principais são `controller`, `dto`, `service`, `repository`, `domain`, `validation`, `exception`, `mapper` e `config`. No frontend, `core` reúne models e serviços, `features` contém as telas e `shared` os componentes reutilizáveis.

## NativeQuery

- As operações principais usam `@Query(nativeQuery = true)` e `@Modifying` quando necessário.
- `INSERT`, `SELECT`, `UPDATE` e `DELETE` são executados com SQL nativo.
- O CRUD principal não depende de `save()`, `findById()`, `deleteById()` ou métodos derivados.

## Banco de dados e Flyway

Flyway é a fonte de verdade do schema e o Hibernate utiliza `ddl-auto: validate`.

Tabelas: `colaborador`, `cafe_da_manha`, `participacao` e `item_cafe`.

Constraints principais:

- CPF único.
- Data do café única.
- Par colaborador/café único.
- Item normalizado único por café.
- Chaves estrangeiras entre as tabelas do domínio.

## Swagger

Acesse `http://localhost:8080/swagger-ui.html` para consultar e testar os endpoints REST. A especificação está em `http://localhost:8080/v3/api-docs`.

## Principais endpoints

| Domínio | Rotas |
|---|---|
| Colaboradores | `POST /api/colaboradores`<br>`GET /api/colaboradores`<br>`GET /api/colaboradores/{id}`<br>`PUT /api/colaboradores/{id}`<br>`DELETE /api/colaboradores/{id}` |
| Cafés | `POST /api/cafes`<br>`GET /api/cafes`<br>`GET /api/cafes/{id}`<br>`PUT /api/cafes/{id}`<br>`DELETE /api/cafes/{id}` |
| Participações | `POST /api/cafes/{cafeId}/participantes`<br>`GET /api/cafes/{cafeId}/participantes`<br>`DELETE /api/cafes/{cafeId}/participantes/{participacaoId}` |
| Itens | `POST /api/participacoes/{id}/itens`<br>`PUT /api/itens/{id}`<br>`PATCH /api/itens/{id}/status`<br>`DELETE /api/itens/{id}` |

## Decisões arquiteturais e Design Patterns

- **Repository Pattern:** isola o acesso ao PostgreSQL e as NativeQueries.
- **Service Layer:** centraliza regras de negócio e transações.
- **DTO:** separa os contratos HTTP das entidades persistidas.
- **Mapper:** converte dados de domínio em respostas quando aplicável.
- **Dependency Injection:** conecta componentes sem acoplamento manual.
- **Provider de data:** `CurrentDateProvider` e `Clock` tornam regras temporais testáveis.
- **Tratamento global de erros:** padroniza respostas `400`, `404`, `409` e falhas inesperadas.
- **Flyway:** versiona o schema, enquanto o JPA apenas o valida.
- **Testcontainers:** executa integrações contra PostgreSQL real e isolado.

## Interface

O frontend usa Angular Material, identidade visual MV, locale `pt-BR` e layout responsivo. Inclui página inicial, colaboradores, cafés, participantes, múltiplos itens, disponibilidade e status.

## Variáveis de ambiente

| Variável | Exemplo/default |
|---|---|
| `POSTGRES_DB` | `desafio_sulwork` |
| `POSTGRES_USER` | `desafio_sulwork` |
| `POSTGRES_PASSWORD` | `defina_uma_senha` |
| `POSTGRES_PORT` | `5432` |
| `SPRING_PROFILES_ACTIVE` | `dev` |
| `DB_URL` | `jdbc:postgresql://localhost:5432/desafio_sulwork` |
| `DB_USER` | `desafio_sulwork` |
| `DB_PASSWORD` | `defina_uma_senha` |
| `SERVER_PORT` | `8080` |
| `BACKEND_PORT` | `8080` |
| `FRONTEND_PORT` | `4200` |

O frontend utiliza a rota relativa `/api`; no Docker, o Nginx a encaminha para `backend:8080`.

## Deploy

```text
Aplicação: PENDENTE
Repositório GitHub: PENDENTE
```

Os links serão adicionados após a publicação.

## Troubleshooting

Confirme que o Docker Desktop está ativo, verifique portas ocupadas e consulte os logs caso algum serviço não suba:

```bash
docker compose ps
docker compose logs backend
docker compose logs frontend
docker compose logs postgres
```

## Estado do projeto

- Implementação concluída.
- Testes automatizados aprovados.
- Bateria manual concluída.
- Aguardando publicação e deploy final.

# ☕ Café da Manhã MV — Desafio Sulwork

Sistema Full Stack para organizar cafés da manhã entre colaboradores.

A aplicação permite cadastrar colaboradores, criar cafés, selecionar vários itens por participante, evitar itens duplicados no mesmo café e acompanhar a entrega dos itens.

Projeto desenvolvido como solução para o **Desafio Técnico Sulwork**.

---

## 🛠 Tecnologias Utilizadas

### Backend

- Java 17
- Spring Boot 3.5.16
- Spring Data JPA
- NativeQuery / SQL nativo
- PostgreSQL 17
- Flyway
- Maven
- Swagger / OpenAPI
- JUnit 5
- Mockito
- Testcontainers

### Frontend

- Angular 22.1.3
- Angular Material
- TypeScript
- Vitest
- Cypress
- Nginx

### Infraestrutura

- Docker
- Docker Compose
- Railway

---

## 📦 Como executar com Docker

### Pré-requisitos

- Docker Desktop instalado e iniciado
- Git instalado

### 1. Clonar o projeto

```bash
git clone https://github.com/Jvdev21/desafio-sulwork.git
cd desafio-sulwork
```

### 2. Subir a aplicação

Na raiz do projeto:

```bash
docker compose up --build
```

O comando inicia:

- PostgreSQL
- Backend Spring Boot
- Frontend Angular/Nginx

### 3. Acessar localmente

- 🌐 Frontend: `http://localhost:4200`
- 📚 Swagger: `http://localhost:8080/swagger-ui.html`
- 📄 OpenAPI: `http://localhost:8080/v3/api-docs`

### 4. Parar

```bash
docker compose down
```

> `docker compose down -v` também remove os dados persistidos do PostgreSQL.

---

## ✅ Funcionalidades

✅ CRUD de colaboradores

✅ Validação e unicidade de CPF

✅ Cadastro de cafés em datas futuras

✅ Inclusão de participantes no café

✅ Vários itens para o mesmo colaborador

✅ Bloqueio de item repetido no mesmo café

✅ Mesmo item permitido em outro café

✅ Status `PENDENTE`, `TROUXE` e `NAO_TROUXE`

✅ Validações com mensagens no frontend

✅ API documentada com Swagger

✅ Interface responsiva com Angular Material

---

## 📋 Principais Regras

- O CPF deve possuir 11 dígitos, ser válido e único.
- Um colaborador participa apenas uma vez do mesmo café.
- A data do café deve ser futura.
- Um colaborador pode levar vários itens diferentes.
- O mesmo item não pode ser escolhido por duas pessoas no mesmo café.
- O mesmo item pode ser escolhido novamente em outro café.
- Antes da data do café, o item fica `PENDENTE`.
- No dia do café, pode ser marcado como `TROUXE` ou `NAO_TROUXE`.
- A aplicação considera um único café por data como decisão de modelagem.

---

## 💾 NativeQuery

As operações principais de banco utilizam SQL nativo com:

```java
@Query(nativeQuery = true)
```

São utilizadas operações nativas de:

- `INSERT`
- `SELECT`
- `UPDATE`
- `DELETE`

O CRUD principal não depende de `save()` ou `deleteById()`.

---

## 🔧 Arquitetura

```text
Angular
   ↓ HTTP / JSON
Spring Boot REST API
   ↓
Service
   ↓
Repository
   ↓ NativeQuery
PostgreSQL
```

---

## 📚 Swagger / API

Swagger local:

`http://localhost:8080/swagger-ui.html`

Swagger publicado:

`https://desafio-sulwork-production.up.railway.app/swagger-ui.html`

OpenAPI publicado:

`https://desafio-sulwork-production.up.railway.app/v3/api-docs`

---

## 🧪 Testes

### Backend

```bash
cd backend
./mvnw test
```

**85 testes aprovados**

### Frontend

```bash
cd frontend
npm test
```

**26 testes aprovados**

### Cypress E2E

```bash
cd frontend
npm run e2e
```

**7 cenários aprovados**

---

## 🌐 Deploy

### Aplicação

[https://desafio-sulwork-production-b3a9.up.railway.app/](https://desafio-sulwork-production-b3a9.up.railway.app/)

### Backend / Swagger

[https://desafio-sulwork-production.up.railway.app/swagger-ui.html](https://desafio-sulwork-production.up.railway.app/swagger-ui.html)

### Código fonte

[https://github.com/Jvdev21/desafio-sulwork](https://github.com/Jvdev21/desafio-sulwork)

Hospedagem realizada no **Railway** com serviços separados para:

- Frontend
- Backend
- PostgreSQL

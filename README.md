# ElectroTrack ERP: API

REST API for **ElectroTrack ERP**, a system for a business that sells electronics. It handles products, stock, customers, orders, invoices, accounting and admin notifications. The React frontend lives in the separate **ERP_Cistim** project.

## Features

- **Authentication**: register and log in; JWT (HS256, valid 24 hours) with the role inside the token; passwords hashed with BCrypt.
- **Roles**: `ADMIN` (owner or manager) and `USER` (customer), enforced on the server.
- **Products**: catalogue with SKU, category and price.
- **Inventory**: one stock row per product, stock status, and a full history of every stock change (`StockAdjustment`).
- **Customers**: records with status Active, Inactive or Suspended.
- **Orders**: shipping status `PENDING` → `DELIVERED` or `DECLINED` (both final). Declining returns the stock.
- **Checkout and invoices**: one transaction creates the orders, reduces stock, creates a numbered invoice (for example `INV-2026-00001`) and notifies admins. Admins mark invoices paid or refunded.
- **Accounting**: income and expense entries and a summary of revenue, expenses and net profit.
- **Notifications**: admins are told when a new order arrives.
- **Stock safety**: stock rows are locked while changing, so two orders cannot take the last unit, and stock never goes below zero.
- **Money** is stored in whole cents (`priceCents`, `amountCents`).

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.1.1 (Web MVC, Data JPA, Security, Validation) |
| Database | PostgreSQL 16 |
| Auth | jjwt 0.12.6, BCrypt |
| Build | Maven (wrapper included) |
| Packaging | Docker and Docker Compose |
| Tests | JUnit 5, Mockito |

## Project structure

```
src/main/java/com/example/ERP_Api/
├── Config/       security, JWT filter, CORS, password encoder, exception handler
├── Contoller/    REST controllers
├── Entity/       JPA entities and enums
├── Repository/   Spring Data repositories
├── Service/      business rules and transactions
└── ErpApiApplication.java
```

## Getting started

### Option A: Docker (recommended)

You need Docker with Compose.

1. Create a `.env` file next to `docker-compose.yml`:

   ```
   DB_PASSWORD=choose-a-strong-password
   ```

2. Start the database and the API:

   ```bash
   docker compose up -d --build
   docker compose ps
   ```

The API listens on **http://localhost:8088**. PostgreSQL is reachable from your machine on port **5433** (for tools like DBeaver; database `erp_db`, user `erp_user`). Data is kept in the `erp_pgdata` volume.

Useful commands:

```bash
docker compose logs -f api     # follow the API logs
docker compose restart         # restart both services
docker compose down            # stop (keeps data)
docker compose down -v         # stop AND delete the database data
```

### Option B: run locally

You need JDK 25 and a running PostgreSQL.

1. Create the database and user:

   ```sql
   CREATE USER erp_user WITH PASSWORD 'your-password';
   CREATE DATABASE erp_db OWNER erp_user;
   ```

2. Set the connection (environment variables override `application.properties`):

   ```bash
   export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/erp_db
   export SPRING_DATASOURCE_USERNAME=erp_user
   export SPRING_DATASOURCE_PASSWORD=your-password
   ```

3. Run:

   ```bash
   ./mvnw spring-boot:run
   ```

Tables are created automatically on first start (`ddl-auto=update`).

### First admin account

Registration creates a `USER`. To create an admin, register, then promote the account in the database:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'you@example.com';
```

Log in again afterwards, because the role is stored in the token.

## Configuration

| Variable | Purpose | Default |
|---|---|---|
| `SPRING_DATASOURCE_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/erp_db` |
| `SPRING_DATASOURCE_USERNAME` | Database user | `erp_user` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | set it yourself |
| `DB_PASSWORD` | Used by `docker-compose.yml` for both services | none, required |

Server port: `8088`. The allowed browser origins are set in `Config/CorsConfig.java` (currently `http://localhost:5173` and `http://127.0.0.1:5173`). Add your deployed frontend address there when you deploy.

## API overview

Base path `/api`. Send `Authorization: Bearer <token>` on protected routes.

| Area | Endpoints | Access |
|---|---|---|
| Auth | `POST /auth/register`, `POST /auth/login` | Public |
| Products | `GET /products`, `GET /products/{id}` | Admin, User |
| Products | `POST /products`, `PATCH /products/{id}`, `DELETE /products/{id}` | Admin |
| Checkout and own invoices | `POST /invoices/checkout`, `GET /invoices/mine`, `GET /invoices/{id}` | Admin, User |
| Invoices (all) | `GET /invoices`, `PATCH /invoices/{id}/pay`, `PATCH /invoices/{id}/refund` | Admin |
| Customers | `GET/POST /customers`, `GET/PUT/DELETE /customers/{id}` | Admin |
| Inventory | `GET /inventory`, `GET /inventory/stats`, `GET/POST /inventory/{productId}`, `POST /inventory/{productId}/adjustments` | Admin |
| Orders | `GET/POST /orders`, `GET/DELETE /orders/{id}`, `PATCH /orders/{id}/status`, `GET /orders/stats` | Admin |
| Accounting | `GET/POST /transactions`, `GET/DELETE /transactions/{id}`, `GET /accounting/summary` | Admin |
| Notifications | `GET /notifications`, `PATCH /notifications/{id}/read`, `PATCH /notifications/read-all` | Admin |

Any route not listed as open is **Admin-only** by default. A missing or invalid token returns **401**; a valid token with the wrong role returns **403**. Business-rule errors return **409** (for example insufficient stock or an invalid status change), and unknown ids return **404**.

Opening `http://localhost:8088/` in a browser shows a 404. That is normal, because the API has no page at `/`.

### Quick test

```bash
curl -X POST http://localhost:8088/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","email":"test@example.com","password":"secret123"}'

curl -X POST http://localhost:8088/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"secret123"}'
```

## Tests

```bash
./mvnw test
```

Existing tests cover the JWT filter (valid, malformed and missing tokens, USER and ADMIN roles), the order stock rules (creating, declining and delivering), and application start-up (needs the database). The Docker build skips tests (`-DskipTests`), so run them locally or in CI before deploying.

## Known limitations

- The JWT signing key is generated at random on every start, so **every restart signs all users out**.
- `ddl-auto=update` is used instead of versioned migrations (for example Flyway).
- CORS origins are hardcoded.
- Login attempts are not throttled, and the API does not provide HTTPS itself (put it behind a reverse proxy).
- Never commit `.env`. It is listed in `.gitignore`; add it to `.dockerignore` too so it is not copied into build contexts.

## Related project

**ERP_Cistim**: the React 19 + Vite frontend. Set `VITE_API_BASE_URL` at build time to point it at this API (default `http://localhost:8088/api`).
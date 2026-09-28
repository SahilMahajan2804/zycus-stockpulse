# StockPulse Backend

StockPulse is a Spring Boot modular monolith for product inventory, simulated sales, and human-reviewed pricing and reorder suggestions. Pricing is never applied automatically.

## Architecture

Java 17, Spring Boot 3.5, Spring Web, Spring Data JPA, Jakarta Validation, PostgreSQL, H2 for tests, Jackson, and `RestClient`. `ProductService` handles transactional product/stock operations; `SuggestionService` coordinates dedupe, strategies, and persistence. Separate async transaction-event listeners run after commit. Pricing and reorder use independent strategy interfaces; AI failures fall back to rule strategies. Product updates use optimistic locking.

## Run

Requirements: JDK 17 and Maven (or the included wrapper).

Windows PowerShell:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

macOS/Linux:

```sh
cd backend
./mvnw spring-boot:run
```

The backend listens at `http://localhost:8081` and connects by default to PostgreSQL at `localhost:5432/stock_pulse`. Ensure the database exists before starting the application.

## Environment

Set `GROQ_API_KEY` to enable Groq. The key is optional and is never hardcoded; without it or on an AI failure, the rule-based fallback is used. Database overrides: `DATABASE_URL` (default `jdbc:postgresql://localhost:5432/stock_pulse`), `DB_USERNAME` (default `postgres`), and `DB_PASSWORD` (default `postgres`). Optional AI/strategy configuration: `LLM_PROVIDER`, `LLM_MODEL`, `LLM_BASE_URL`, `STOCKPULSE_PRICING_STRATEGY`, and `STOCKPULSE_REORDER_STRATEGY`.

## API endpoints

- `POST /api/products`
- `GET /api/products?status=ACTIVE&category=APPAREL`
- `GET /api/products/{id}`
- `PATCH /api/products/{id}/stock`
- `POST /api/products/{id}/orders`
- `POST /api/products/{id}/suggest-pricing` (202)
- `POST /api/products/{id}/suggest-reorder` (202)
- `GET /api/pricing-suggestions`
- `PATCH /api/pricing-suggestions/{id}`
- `GET /api/reorder-suggestions`
- `PATCH /api/reorder-suggestions/{id}`
- `GET /api/recommendations/pending`
- `GET /api/admin/strategy`
- `PUT /api/admin/strategy`

## Example order flow

```http
POST /api/products/PRD-003/orders
Content-Type: application/json

{"quantity":1}
```

A successful sale reduces stock and increases the cumulative demand-velocity counter. Low-stock and demand-spike signals publish events; after commit, listeners generate pending pricing and reorder suggestions asynchronously. Accepting a pricing suggestion is the only operation that applies its recommended price.

## Strategy switch

```http
PUT /api/admin/strategy
Content-Type: application/json

{"strategy":"RULE"}
```

Use `AI` to select the AI strategies. Switching takes effect without restarting the application.

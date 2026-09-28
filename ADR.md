# StockPulse Architecture Decision Records

## ADR-001 — Commerce logic placement

### Context
Order processing changes product stock and demand; recommendation generation also needs shared persistence, validation, and state transitions.

### Options
- Put business rules in HTTP controllers.
- Distribute generation decisions across asynchronous listeners.
- Keep coordination in application services with small domain operations.

### Decision
Use a modular monolith: controllers validate/route; `ProductService` coordinates stock and sale transactions; `SuggestionService` coordinates strategy selection, dedupe and persistence; strategy beans calculate recommendations; repositories persist. Product and suggestion domain objects own simple state rules.

### Tradeoffs
Modules are independently understandable and potential future split points, while remaining one deployable process and one database.

## ADR-002 — Separate pricing and reorder strategy interfaces

### Context
Price and replenishment use different inputs, output shapes, constraints, prompts, and failure behavior.

### Options
- One combined advisor interface.
- Independent `PricingStrategy` and `ReorderStrategy` interfaces.

### Decision
Use separate interfaces and context/result records. AI pricing and AI reorder each have their own rule fallback.

### Tradeoffs
A little more wiring avoids coupled evolution and permits independent selection/fallback.

## ADR-003 — Runtime strategy switching

### Context
The demo needs to switch strategies without restarting Spring Boot.

### Options
- Restart with a different property.
- Keep bean maps and atomically switch active strategy identifiers.

### Decision
`StrategyRegistry` receives Spring bean maps, stores active pricing/reorder identifiers in `AtomicReference`, initializes from properties, and is changed through `PATCH /api/admin/strategy`. Manual requests and event-triggered generation resolve through this same registry.

### Tradeoffs
Unknown strategy names fail with HTTP 400. The demo endpoint switches both pricing and reorder strategies together.

## ADR-004 — LLM failure handling

### Context
LLM output is untrusted and provider calls may time out or return invalid data.

### Options
- Surface provider errors to API clients.
- Parse and validate responses, then use deterministic rules on any failure.

### Decision
Use `RestClient` with finite connect/read timeouts, trigger-specific prompts, defensive extraction of JSON from optional fences, Jackson parsing, bounds/direction validation, and `RULE_FALLBACK` results. API keys come only from environment configuration. Provider selection supports Groq and Gemini request formats; Gemini is selected by configuration and is not an automatic second call after Groq fails.

### Tradeoffs
A fallback keeps the recommendation flow available, but the result may be less nuanced than AI. AI output never directly changes product price.

## ADR-005 — Post-commit advisor loop and deduplication

### Context
Order/stock HTTP calls must not wait for recommendation work, and listeners must not observe uncommitted stock.

### Options
- Synchronous advisor calls inside the request.
- An external message broker/outbox.
- In-process application events handled asynchronously after commit.

### Decision
`ProductService` publishes ID-only events within its transaction. Separate `@Async("advisorExecutor")` listeners use `@TransactionalEventListener(AFTER_COMMIT)` and delegate to the shared `SuggestionService`. Dedupe uses `existsByDedupeKey` plus unique nullable `dedupeKey` constraints; accept/reject clears the key.

### Tradeoffs
The bounded in-process event queue is not durable across process failure. The requested solution intentionally excludes an outbox/broker. Listener failures are logged. Demand velocity is a cumulative counter and does not decay. A future design may add timestamped orders and `countByProductIdAndCreatedAtAfter(...)`; that upgrade is not implemented.

## ADR-006 — Extensibility and deliberate exclusions

### Context
StockPulse needs a focused demo while preserving fields and seams for later extension.

### Decision
Product retains optional `costPrice`, `marginFloor`, and `supplierId` fields. Strategy interfaces provide a future seam for a `CompetitorAwareStrategy`, supplier catalog integration, category pricing caps, and cooldown policy; none are implemented. Human approval is required for all suggestion effects. The UI is React 18 + Vite with plain `fetch` polling.

### Deliberate exclusions
Authentication, competitor scraping, automatic price application, automated purchase orders, storefront/payment functionality, outbox, external brokers, Spring AI, and multi-model councils are not part of this implementation.

### Tradeoffs
The in-memory H2 demo database resets on restart. The LLM model is configurable so providers/models can be substituted without introducing Spring AI.

# Distributed Chat Application — A Resilient, Real-Time Chat Backend

A Spring Boot chat backend built to explore how a real messaging system behaves under failure, not just when everything works. Two backend instances run behind shared infrastructure, state is coordinated through Redis and Kafka instead of kept in memory, and the main failure modes were triggered and observed against a running stack, not only reasoned about on paper.

## Why

Most tutorial chat apps stop at "messages appear in real time." This one asks what happens when the pieces holding it together stop working: Redis goes down mid-conversation, two instances both need to deliver the same message, a client floods the server with requests, a refresh token is reused after logout. The goal was a system that degrades predictably, with tests that run against real infrastructure as well as mocks.

## Architecture

```
                        ┌─────────────┐
                        │   Client    │
                        └──────┬──────┘
                               │ REST / WebSocket (STOMP)
                     ┌─────────┴─────────┐
                     │                   │
              ┌──────▼─────┐      ┌──────▼─────┐
              │  backend1  │      │  backend2  │
              └──────┬─────┘      └──────┬─────┘
                     │                   │
        ┌────────────┼───────────────────┼────────────┐
        │            │                   │            │
  ┌─────▼────┐ ┌─────▼──────┐     ┌──────▼─────┐ ┌────▼──────┐
  │  MySQL   │ │   Redis    │     │   Kafka    │ │  Grafana  │
  │(users,   │ │ (streams,  │     │ (message-  │ │ + Loki +  │
  │ messages,│ │ unread     │     │  sent      │ │Prometheus │
  │ read     │ │ counters,  │     │  events)   │ │           │
  │ status)  │ │ rate limit,│     │            │ │           │
  │          │ │ refresh    │     │            │ │           │
  │          │ │ tokens)    │     │            │ │           │
  └──────────┘ └────────────┘     └────────────┘ └───────────┘
```

Both backend instances are stateless. Live delivery, unread counts, rate limiting, and refresh tokens all live in Redis, so a request handled by `backend1` and the next one handled by `backend2` see the same state.

## Core features

- **Authentication**: BCrypt password hashing, JWT access tokens valid for 15 minutes, and opaque refresh tokens stored in Redis with a 7-day expiry. Logout deletes the refresh token, so no new access tokens can be issued for that session.
- **Input validation**: request DTOs use Bean Validation (`@NotBlank`, `@Email`, `@Size`, `@NotNull`) with `@Valid` on every endpoint that accepts a body. Errors come back as a consistent, typed response.
- **Authorization at two layers**: REST endpoints and STOMP messages both check conversation participation and resource ownership independently.
- **Real-time messaging over WebSocket/STOMP**, with JWT validated on the CONNECT frame and a dedicated error channel (`/queue/errors`) for rejected sends.
- **Cross-instance delivery via Redis Streams**: a message sent through `backend1` reaches a client connected to `backend2`. Each instance tracks its own read position.
- **Kafka event pipeline**: message creation publishes a `message-sent` event consumed by two independent consumer groups, one for notifications and one for unread counts. Each has its own retry policy and dead-letter topic.
- **Redis-backed sliding-window rate limiting** on login (5 attempts per minute per username) and message creation (20 per 10 seconds per user). The check runs in the service layer, so REST and WebSocket sends share the same limit.
- **Read receipts (1-on-1 scope)**: each message carries a `SENT` or `READ` status. When a participant opens a conversation, messages sent by the other participant flip to `READ` in a single bulk `UPDATE`, and the sender's own messages are excluded.
- **Circuit breaker around the Redis publish path**: a programmatic Resilience4j breaker. When Redis is unavailable, messages still save to MySQL and still reach Kafka; only live WebSocket delivery is skipped.
- **Unread counts**: served from a Redis counter with a MySQL fallback, maintained by a Kafka consumer and reset on `markAsRead`.
- **Observability**: a correlation ID propagated through HTTP, WebSocket, and Kafka consumer threads, shipped to Loki and queryable per request. Prometheus metrics and a Grafana dashboard are tagged per instance.
- **Health probes, graceful shutdown, and per-instance identifiers** for container orchestration.

## Engineering decisions worth asking about

- **Redis Streams over Redis Pub/Sub** for live delivery. Pub/Sub drops messages for subscribers that are not connected at publish time. Streams persist entries and let each instance track its own position, so an instance that restarts mid-conversation can resume.
- **Horizontal scaling over microservices.** The app is distributed in the way that matters here: multiple stateless instances coordinating through shared infrastructure, surviving the loss of a dependency. A microservices split would add service discovery, distributed transactions, and an API gateway without a corresponding benefit at this scope.
- **Core Resilience4j instead of the Spring Boot 3 starter**, because the starter is not compatible with Spring Boot 4. The breaker wraps the one call that needs it (the Redis publish), configured programmatically.
- **Authorization before existence checks** in several service methods. A request for a resource that does not exist, from a caller who is also not authorized, returns 403 instead of 404, so an unauthorized caller cannot probe which IDs exist.
- **Rate limiting is Redis-backed, not in-memory.** With two instances, an in-memory counter on one node would never see requests handled by the other, and the limit would be trivially bypassed by alternating between them.
- **Refresh tokens are random opaque values stored in Redis, not JWTs.** A JWT is self-verifying and stateless, which is exactly why it cannot be revoked early. A refresh token needs the opposite property: the server must be able to delete it on demand.
- **Read status is per message, not per participant, by design for the current scope.** The conversation model supports more than two participants, but the read-status logic is written for 1-on-1 conversations. Per-participant receipts would need a join table, which is a deliberate later extension rather than something this version pretends to support.
- **Access tokens are short-lived (15 minutes) to bound the damage of a stolen token.** Logout is immediate for future access, but an already-issued access token remains valid until it expires. That trade-off is accepted explicitly.

## Tech stack

| Layer | Technology |
|---|---|
| Language / Runtime | Java 21 |
| Framework | Spring Boot 4 (Web, Security, Data JPA, Data Redis, Kafka, WebSocket) |
| Database | MySQL |
| Caching / real-time / rate limiting / refresh tokens | Redis (Streams, Sorted Sets, key-value) |
| Messaging | Apache Kafka (KRaft mode) |
| Resilience | Resilience4j (circuit breaker) |
| Auth | JWT (access), opaque refresh tokens, BCrypt |
| Observability | Micrometer, Prometheus, Grafana, Loki, Promtail |
| Testing | JUnit 5, Mockito |
| CI | GitHub Actions |
| Containerization | Docker, Docker Compose |

## Running locally

```bash
git clone https://github.com/dalipshakya-dev/Distributed-Chat-Application.git
cd Distributed-Chat-Application
cp docker-compose.env.example docker-compose.env   # set DB credentials and JWT secret
docker compose up -d --build
```

This starts two backend instances, MySQL, Redis, Kafka, and the observability stack. Backends are reachable on `8080` and `8081`.

## Testing

Unit tests cover the service layer: authorization paths, cache behavior, Redis interactions, and edge cases. One branch in `markAsRead` was identified as unreachable from the code as written and is documented in the source rather than covered by a test that could never pass.

```bash
mvn test
```

Tests run on every push to `main` and on pull requests via GitHub Actions.

## Failure scenarios, tested against a running stack

- **Redis killed mid-session**: messages continue to save to MySQL and to Kafka. Live WebSocket delivery pauses until Redis returns. The circuit breaker's transitions (CLOSED → OPEN → HALF_OPEN → OPEN) were confirmed from the logs.
- **Six rapid login attempts for one username**: the sixth returns `429 Too Many Requests` before the password check runs.
- **Typo'd or already-logged-out refresh token**: returns `401` with a specific message. An earlier version silently returned `204`; the bug was found during manual testing and fixed.
- **Two instances, one conversation**: a message sent through one instance is delivered to a client connected to the other.
- **Read receipts over WebSocket**: messages sent by one participant were marked `READ` after the other participant opened the conversation, and the reader's own messages were left as `SENT`.
- **Request flood on message creation**: requests above the limit are rejected with a 429 and a matching STOMP error message. Under heavy concurrency, some requests failed with a Redis command timeout instead, which is a real limit of the rate limiter when every check makes a network round-trip.
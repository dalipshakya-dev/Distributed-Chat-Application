# Distributed Chat Application — A Resilient, Real-Time Chat Backend

A Spring Boot chat backend built to explore how a real messaging system behaves under failure, not just how it behaves when everything works. Two backend instances run behind the same infrastructure, state is coordinated through Redis and Kafka instead of kept in memory, and every major failure mode — a dead Redis node, a flood of requests, a stolen token — was deliberately triggered and observed, not just reasoned about on paper.

## Why

Most tutorial chat apps stop at "messages appear in real time." This one asks a harder question: what happens when the pieces holding it together stop working? Redis goes down mid-conversation. Two instances both try to deliver the same message. A client sends 50 requests in a second. The goal here was to build a system that degrades predictably instead of falling over, and to prove that with real tests against a real running stack — not just unit tests against mocks.

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
        │            │                   │             │
  ┌─────▼────┐ ┌─────▼─────┐      ┌──────▼─────┐ ┌─────▼─────┐
  │  MySQL   │ │   Redis    │      │   Kafka    │ │  Grafana  │
  │(messages,│ │ (streams,  │      │ (async     │ │ (+ Loki,  │
  │ users)   │ │ cache,     │      │  events)   │ │ Prometheus)│
  │          │ │ rate limit,│      │            │ │           │
  │          │ │ refresh    │      │            │ │           │
  │          │ │ tokens)    │      │            │ │           │
  └──────────┘ └────────────┘      └────────────┘ └───────────┘
```

Both backend instances are stateless and interchangeable. Live message delivery, unread counts, rate limiting, and session state all live in Redis, which is what makes it safe to run more than one instance in the first place — a request handled by `backend1` and the next one handled by `backend2` see the same world.

## Core features

- JWT authentication with BCrypt password hashing, plus short-lived access tokens (15 min) backed by revocable refresh tokens (7 days, stored in Redis) — logging out actually invalidates the session instead of just discarding a token client-side
- Real-time messaging over WebSocket/STOMP, with participation and ownership checks enforced independently at both the HTTP and WebSocket layers
- Cross-instance message delivery via Redis Streams: a message sent through `backend1` reaches a client connected to `backend2`, verified by running two containers side by side and watching delivery logs
- Kafka-based event pipeline: message creation publishes an event that two independent consumers act on — one pushes notifications, one updates unread counts — each with its own consumer group, retry policy, and dead-letter topic
- Redis-backed sliding-window rate limiting on login and message creation, enforced identically whether the request comes through REST or the WebSocket, with a dedicated 429 response and matching STOMP error channel
- A programmatic Resilience4j circuit breaker around the Redis publish path: when Redis is killed outright, messages still save to MySQL and still reach Kafka — only the live-delivery hop is skipped, and the breaker correctly cycles CLOSED → OPEN → HALF_OPEN → OPEN under sustained failure
- Centralized validation and error handling: every endpoint rejects malformed input before it reaches business logic, and every error path returns a consistent, typed response
- Structured logging with a correlation ID propagated through HTTP, WebSocket, and Kafka consumer threads, shipped to Loki and queryable per-request across both instances
- Prometheus metrics and a Grafana dashboard tagged per instance, so a problem on one node is visible without guessing which container is responsible

## Engineering decisions worth asking about

- **Redis Streams over Redis Pub/Sub** for live delivery. Pub/Sub drops messages for consumers who aren't actively subscribed at the moment of publish; Streams persist entries and let each instance track its own read position, which matters when an instance restarts mid-conversation.
- **Horizontal scaling over microservices.** The app is distributed in the sense that matters for this project — multiple stateless instances coordinating through shared infrastructure, surviving the failure of any one dependency — without the added surface area of service discovery, distributed transactions, and an API gateway that a microservices split would require without a clear corresponding benefit here.
- **A core Resilience4j circuit breaker instead of the Spring Boot 3 starter**, which isn't compatible with Spring Boot 4. The breaker is wired programmatically around the one call that actually needs it (the Redis publish), rather than applied broadly via annotations.
- **Authorization checks happen before existence checks** in several service methods — a request for a resource that doesn't exist, from a user who also isn't authorized, returns 403 rather than 404. This is deliberate: it avoids confirming to an unauthorized caller whether a given resource ID exists at all.
- **Rate limiting is Redis-backed, not in-memory**, specifically because the app runs as two instances. An in-memory counter on `backend1` would never see requests handled by `backend2`, making the limit trivially bypassable by alternating between them.
- **Refresh tokens are opaque random values stored in Redis, not JWTs.** A JWT is self-verifying and stateless by design, which is exactly what makes it impossible to revoke early. A refresh token needs the opposite property — the server has to be able to kill it on command — so it's a random string whose only meaning comes from a Redis key the server controls.

## Tech stack

| Layer | Technology |
|---|---|
| Language / Runtime | Java 21 |
| Framework | Spring Boot 4 (Web, Security, Data JPA, Data Redis, Kafka, WebSocket) |
| Database | MySQL |
| Caching / real-time / rate limiting | Redis (Streams, Sorted Sets, key-value) |
| Messaging | Apache Kafka (KRaft mode) |
| Resilience | Resilience4j (circuit breaker) |
| Auth | JWT (access + refresh), BCrypt |
| Observability | Micrometer, Prometheus, Grafana, Loki, Promtail |
| Testing | JUnit 5, Mockito |
| CI | GitHub Actions |
| Containerization | Docker, Docker Compose |

## Running locally

```bash
git clone https://github.com/dalipshakya-dev/Distributed-Chat-Application.git
cd Distributed-Chat-Application
cp docker-compose.env.example docker-compose.env   # fill in DB credentials and JWT secret
docker compose up -d --build
```

This starts two backend instances, MySQL, Redis, Kafka, and the full Grafana/Loki/Prometheus observability stack. Backend instances are reachable on `8080` and `8081`.

## Testing

Unit tests cover the service layer across users, conversations, participants, and messages — authorization paths, cache behavior, Redis interactions, and edge cases including at least one branch identified as unreachable in production code and documented rather than force-tested.

```bash
mvn test
```

Tests run automatically on every push via GitHub Actions.

## Resilience, demonstrated rather than assumed

A few of the failure scenarios this project was actually tested against, not just designed for in theory:

- **Redis killed mid-session**: messages continue to save and continue to reach Kafka; only live delivery over WebSocket pauses until Redis returns, and the circuit breaker's state transitions were confirmed directly from logs.
- **Six rapid login attempts against one username**: the sixth is rejected with a 429 before it ever reaches the authentication check.
- **A refresh token logged out, then reused**: the second attempt is correctly rejected rather than silently succeeding — a gap that surfaced during manual testing and was closed with a dedicated validity check rather than assumed to be fine.
- **Two instances, one conversation**: a message sent through one instance is confirmed to arrive at a client connected to the other, by running both containers side by side and reading the delivery logs.
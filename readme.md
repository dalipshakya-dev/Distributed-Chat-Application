# Distributed Chat Application — A Resilient, Real-Time Chat Backend

A Spring Boot chat backend built to explore how real chat systems scale
and survive failure — not just send messages between two users locally.
Runs as multiple backend instances behind Redis Streams for cross-instance
message delivery, Kafka for async notifications, and a circuit breaker
that keeps the app usable even when Redis goes down.

## Why this exists

Most chat-app tutorials stop at "two users, one server, WebSocket works."
This project pushes past that: what happens when there are two backend
instances and a user on instance A needs a live message from a user on
instance B? What happens when Redis — the thing carrying that live
delivery — goes down mid-conversation? Those two questions shaped most
of the architecture here.

## Architecture

```
                    ┌─────────────┐
      Client ───────┤   Nginx/LB   │
                    └──────┬──────┘
                 ┌─────────┴─────────┐
           ┌─────▼─────┐       ┌─────▼─────┐
           │ backend1  │       │ backend2  │
           │ (Spring)  │       │ (Spring)  │
           └─────┬─────┘       └─────┬─────┘
                 │                   │
     ┌───────────┼───────────────────┼───────────┐
     │           │                   │           │
┌────▼────┐ ┌────▼─────┐      ┌──────▼─────┐ ┌───▼────┐
│  MySQL  │ │  Redis   │      │   Kafka    │ │ Prom/  │
│(source  │ │ Streams  │      │ (events)   │ │ Grafana│
│ of truth)│ │(live relay)│    └────────────┘ │ /Loki  │
└─────────┘ └──────────┘                      └────────┘
```

- **MySQL** is the durable source of truth — every message is persisted
  here first, before anything else happens.
- **Redis Streams** relay live messages between backend instances over
  WebSocket/STOMP, so a message sent on instance A reaches a subscriber
  connected to instance B.
- **Kafka** decouples side effects (push notifications, unread-count
  updates) from the message-send path — those consumers can lag or fail
  without blocking message delivery.
- **Prometheus + Grafana + Loki** give metrics, dashboards, and
  correlation-ID-tagged, cross-instance log search.

## Key features

- **JWT authentication** with Spring Security, BCrypt password hashing
- **REST API** for users, conversations, participants, and messages,
  with pagination and unread-count tracking
- **Real-time messaging** over WebSocket/STOMP, with per-subscription
  and per-send authorization checks (not just connection-level auth)
- **Horizontal scaling**: two backend instances verified to relay
  messages to each other correctly via Redis Streams, each tracking its
  own consumer offset
- **Event-driven side effects** via Kafka: push notifications and
  unread-count updates run as independent consumers with their own
  retry policy and dead-letter topic
- **Circuit breaker on Redis**: if Redis goes down, messages still save
  to MySQL and the REST API still responds successfully — only live
  delivery is skipped, and the breaker stops hammering a dead Redis
  instead of timing out on every request
- **Full observability stack**: Prometheus metrics per instance,
  Grafana dashboards, and Loki log aggregation with a correlation ID
  threaded through every request — filterable across both backend
  instances
- **Production-shape details**: graceful shutdown, health/liveness/
  readiness probes, profile-split config (dev/prod)

## Tech stack

| Layer            | Technology                                   |
|-------------------|----------------------------------------------|
| Language/Runtime  | Java 21, Spring Boot 4                        |
| API               | Spring Web (REST), Spring Security + JWT      |
| Real-time         | WebSocket, STOMP                              |
| Persistence       | MySQL, Spring Data JPA                        |
| Caching / relay   | Redis (cache + Streams)                       |
| Messaging         | Apache Kafka (KRaft mode)                     |
| Resilience        | Resilience4j (circuit breaker)                |
| Observability     | Prometheus, Grafana, Loki, Promtail           |
| Containerization  | Docker, Docker Compose                        |

## Engineering decisions worth asking about

A few things in this codebase were deliberate design calls, not defaults —
these are the areas with the most to talk through in an interview:

- **Graceful degradation over hard failure.** When Redis is down, the
  system doesn't fail the request — it falls back to MySQL for reads and
  skips only the live-delivery step on writes. The message is never
  lost; only real-time push is delayed until the next poll/reconnect.
- **Circuit breaker over naive retries.** Rather than retrying every
  failed Redis call (which just piles up latency under an outage), a
  Resilience4j breaker trips after a failure threshold and fails fast,
  then probes recovery in a controlled HALF_OPEN state — verified end-to-
  end by killing the Redis container mid-load and watching the breaker
  cycle CLOSED → OPEN → HALF_OPEN → OPEN → CLOSED through logs and
  metrics.
- **Redis Streams over Pub/Sub for cross-instance relay.** Pub/Sub drops
  messages for disconnected subscribers; Streams gives each instance a
  durable, resumable consumer position, so a backend restart doesn't
  silently drop in-flight relay messages.
- **Kafka for side effects, not the critical path.** Notification
  delivery and unread-count updates are consumers off a Kafka topic, not
  inline calls in the message-send flow — so a slow or failing consumer
  (with its own retry policy and dead-letter topic) never blocks message
  persistence or the API response.
- **Authorization checked twice.** Every conversation/message access is
  checked at both the HTTP layer and the WebSocket SUBSCRIBE/SEND layer
  — an authorization audit specifically found and fixed cases where
  WebSocket subscriptions bypassed checks the REST endpoints already
  had.
- **Structured, correlated logs over scattered print statements.** Every
  request carries a correlation ID through MDC, propagated across HTTP,
  WebSocket, and async Kafka consumer threads, shipped via Promtail to
  Loki — so a single user-reported issue can be traced across both
  backend instances with one query.

## Running locally

```bash
git clone https://github.com/dalipshakya-dev/Distributed-Chat-Application.git
cd Distributed-Chat-Application
cp docker-compose.env.example docker-compose.env   # fill in your own secrets
docker compose up -d --build
```

Services:
- Backend instance 1: `http://localhost:8080`
- Backend instance 2: `http://localhost:8081`
- Grafana: `http://localhost:3000` (admin/admin)
- Prometheus: `http://localhost:9090`
- Swagger/OpenAPI docs: `http://localhost:8080/swagger-ui.html`

## Resilience design notes

When Redis is unreachable, the system makes a deliberate choice: **a
message always saves to MySQL and the API always returns success** —
only the live WebSocket relay is skipped. This was verified by stopping
the Redis container mid-session and confirming message history was
still correct on reload. The circuit breaker (window of 10 calls, 50%
failure threshold, 10s open wait) prevents every subsequent send from
blocking on a Redis timeout once the outage is detected — it fails fast
instead.
# Virtual Cinema Sync (movie-share)

Lightweight watch-party backend: friends join a room with a short code, share a YouTube embed in the browser, and keep **play / pause / seek** loosely in sync over **STOMP/WebSockets**. **PostgreSQL** stores rooms; **Redis** powers pub/sub (multi-instance friendly) and cached playback hints for late joiners. Only the **room host** (who receives an `adminToken` at creation) may drive playback; everyone else follows and can use **chat**.

## Requirements

- Java **21**
- **PostgreSQL** and **Redis** (local or Docker)
- Maven (project includes `./mvnw`)

## Quick start

1. Start infra (mapped ports match default `application.yml`):

   ```bash
   docker compose up -d
   ```

2. Run the app:

   ```bash
   ./mvnw spring-boot:run
   ```

3. Open the UI (default port **8081** unless you override):

   ```text
   http://localhost:8081/
   ```

To use standard local ports instead (Postgres `5432`, Redis `6379`), run with:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local-standard-ports
```

To accept connections from other devices on your LAN, add `address: 0.0.0.0` under `server:` in [`application.yml`](src/main/resources/application.yml) (Spring Boot’s default is often already all interfaces; use this if you only get `127.0.0.1`).

### Configuration

Main settings live in [`src/main/resources/application.yml`](src/main/resources/application.yml): datasource, Redis, CORS patterns, and `server.port` (`SERVER_PORT` env overrides).

## Features

- **Rooms**: `POST /api/rooms` returns `roomCode`, `adminToken` (host secret), and WebSocket hints.
- **Join**: `GET /api/rooms/{code}` returns room metadata plus **estimated** playback state so late joiners seek near live progress when the host was playing.
- **Sync**: Browser uses SockJS + STOMP; sync messages go to `/app/room/{code}/sync` with a valid `adminToken` only from the host.
- **Chat**: `/app/room/{code}/chat` (no host token required).

**Important:** Save the `adminToken` from room creation if you need to host from another device; it is not returned on `GET` and is stripped from broadcast payloads.

## API overview

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/rooms` | Create room JSON `{ "name": "optional" }` → `roomCode`, `adminToken`, `wsPath`, `stompDestinationPrefix` |
| `GET` | `/api/rooms/{code}` | Room info + optional `state` (video id, position, playing) for join |

## Tests

Requires Docker (Testcontainers):

```bash
./mvnw test
```

## Tech stack

Spring Boot 4, Spring Web, JPA, Flyway, PostgreSQL, Redis, WebSocket/STOMP, Testcontainers.

## Legal note

Video playback uses **YouTube’s embed** in the browser; this service does not host or proxy video bytes.

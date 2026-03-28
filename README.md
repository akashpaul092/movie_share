# Virtual Cinema Sync (movie-share)

Lightweight watch-party backend: friends join a room with a short code, share a YouTube embed in the browser, and keep **play / pause / seek** loosely in sync over **STOMP/WebSockets**. **PostgreSQL** stores rooms; **Redis** powers pub/sub (multi-instance friendly) and cached playback hints for late joiners. Only the **room host** (who receives an `adminToken` at creation) may drive playback; everyone else follows and can use **chat**.

## Requirements

- Java **21**
- **PostgreSQL** and **Redis** (local or Docker)
- Maven (project includes `./mvnw`)

## Quick start

1. **Secrets and local config** live in a root **`.env`** file (gitignored). Copy the template and edit values:

   ```bash
   cp .env.example .env
   ```

   [`docker-compose.yml`](docker-compose.yml) reads `.env` via `env_file` and uses `POSTGRES_*` plus the same Compose-time variables documented in [`.env.example`](.env.example).

2. Start infra (host ports **5433** / **6380** match the default JDBC and Redis settings in `.env.example`):

   ```bash
   docker compose up -d
   ```

3. **Load `.env` into your shell** before running Spring. Docker Compose loads `.env` by itself; **`spring-boot:run` does not** read `.env` unless you export the variables (or configure them in your IDE). For bash/zsh:

   ```bash
   set -a && source .env && set +a
   ./mvnw spring-boot:run
   ```

4. Open the UI (default port **8081** unless you set `SERVER_PORT`):

   ```text
   http://localhost:8081/
   ```

If Postgres and Redis run on the usual local ports (`5432` / `6379`) instead of Docker’s mapped ports, set `SPRING_DATASOURCE_URL`, `SPRING_DATA_REDIS_HOST`, and `SPRING_DATA_REDIS_PORT` in `.env` accordingly.

To listen on all interfaces for LAN access, set **`SERVER_ADDRESS=0.0.0.0`** in `.env` and **export** it as in step 3 (Spring maps `SERVER_ADDRESS` to `server.address`).

### Configuration

[`src/main/resources/application.yml`](src/main/resources/application.yml) wires **PostgreSQL**, **Redis**, CORS, and rate limits from **environment variables** (see [`.env.example`](.env.example)). No database passwords belong in git.

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

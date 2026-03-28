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

2. Choose how you run the app:

   **A — Spring Boot on your machine, only Postgres + Redis in Docker** (host ports **5433** / **6380** match `.env.example`):

   ```bash
   docker compose up -d postgres redis
   ```

   **B — Full stack in Docker** (API + UI + DB + Redis). Compose sets JDBC/Redis hosts to the internal service names (`postgres`, `redis`) so you do not need to change `.env` for that:

   ```bash
   docker compose up -d --build
   ```

   Map a different host port with `APP_HOST_PORT` (default **8081**), e.g. `APP_HOST_PORT=9080 docker compose up -d --build`.

3. If you chose **A**, run `./mvnw spring-boot:run` from the project root: the app **loads a root `.env` file automatically** (via `DotEnvLoader`). You can still **`export`** variables or use IDE env if you prefer; OS environment **overrides** `.env` when both set. Docker Compose continues to read `.env` on its own.

4. Open the UI (default mapped port **8081** unless you changed `APP_HOST_PORT` or `SERVER_PORT`):

   ```text
   http://localhost:8081/
   ```

If Postgres and Redis run on the usual local ports (`5432` / `6379`) instead of Docker’s mapped ports, set `SPRING_DATASOURCE_URL`, `SPRING_DATA_REDIS_HOST`, and `SPRING_DATA_REDIS_PORT` in `.env` accordingly.

To listen on all interfaces for LAN access, set **`SERVER_ADDRESS=0.0.0.0`** in `.env` and **export** it when running locally (**A**), or add under the `app` service in [`docker-compose.yml`](docker-compose.yml): `environment: SERVER_ADDRESS: "0.0.0.0"` (Spring maps `SERVER_ADDRESS` to `server.address`).

### Configuration

[`src/main/resources/application.yml`](src/main/resources/application.yml) wires **PostgreSQL**, **Redis**, CORS, and rate limits from **environment variables** (see [`.env.example`](.env.example)). No database passwords belong in git.

### Deploying to Render (or any single-container host)

`localhost:5433` only makes sense **on your laptop** when Postgres is exposed by Docker Compose. On **Render**, only your **web service** container runs; there is **no** Postgres on `localhost` inside that container.

1. Create **PostgreSQL** and **Redis** (Render *Key Value* or external) in the Render dashboard.
2. On your **Web Service**, set **Environment** variables (do **not** reuse `.env` meant for local Docker):

   | Variable | Value |
   |----------|--------|
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://HOST:5432/DATABASE` using the Render Postgres **internal** hostname and database name from the dashboard. |
   | `SPRING_DATASOURCE_USERNAME` | Postgres user from the dashboard. |
   | `SPRING_DATASOURCE_PASSWORD` | Postgres password. |
   | `SPRING_DATA_REDIS_HOST` | Redis host from your Redis provider. |
   | `SPRING_DATA_REDIS_PORT` | Usually `6379` (or the port Render shows). |

   Render sets **`PORT`**; the app is configured to use **`${PORT}`** first, so Tomcat binds correctly.

3. If Redis requires a password, set **`SPRING_DATA_REDIS_PASSWORD`** (Spring Boot maps it to `spring.data.redis.password`).

4. Optional: **`MOVIESHARE_CORS_ALLOWED_ORIGIN_PATTERNS`** = your frontend origin (e.g. `https://your-app.onrender.com`).

**Why you saw `localhost:5433 refused`:** the service was still using a **local** JDBC URL. Point `SPRING_DATASOURCE_URL` at Render’s Postgres **hostname**, not `localhost`.

### Troubleshooting: `password authentication failed for user "movieshare"`

1. **Same password in two places** — In `.env`, `SPRING_DATASOURCE_PASSWORD` must be **exactly** the same as `POSTGRES_PASSWORD` (Compose also passes `POSTGRES_PASSWORD` into the app container for Docker runs).

2. **Old Docker volume** — Postgres sets the user password only when the `pg_data` volume is **first** initialized. If you changed `.env` after the database was created, the running Postgres still has the **old** password. Either:
   - Put the **old** password back in `.env`, or  
   - Reset the volume (deletes all room data) and start clean:

     ```bash
     docker compose down -v
     docker compose up -d postgres redis   # or full stack with --build
     ```

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

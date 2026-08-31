# Golden Raspberry Awards API

> Which producer waited longest between two Worst Picture wins — and who won twice the fastest.

A RESTful API over the Golden Raspberry Awards nominee list, built as a **Clean Architecture**
showcase: the business rule sits at the centre, knowing nothing about Spring, JPA or HTTP.

| | |
|---|---|
| **Stack** | Java 21 · Spring Boot 4.0.7 · Spring Data JPA · Spring Security · H2 (in-memory) |
| **Build** | Maven Wrapper — nothing to install |
| **Database** | Embedded, in memory, seeded from CSV at startup. No external service |
| **Tests** | 19 integration tests, no unit tests, no mocks |
| **Third-party libraries** | None beyond Spring itself |

---

## Quickstart

```bash
./mvnw spring-boot:run          # PowerShell / CMD: .\mvnw.cmd spring-boot:run
```

Ready in about five seconds. You will see the dataset land:

```
Loaded 206 movies (42 winners, 359 distinct producers) from classpath:Movielist.csv
Tomcat started on port 8080 (http)
```

Then ask the question the whole project exists to answer — **no authentication needed**:

```bash
curl http://localhost:8080/api/public/producers/win-intervals
```

```json
{
  "min": [{ "producer": "Joel Silver",    "interval":  1, "previousWin": 1990, "followingWin": 1991 }],
  "max": [{ "producer": "Matthew Vaughn", "interval": 13, "previousWin": 2002, "followingWin": 2015 }]
}
```

`min` and `max` are arrays: when several producers tie on the same interval, all of them come back.

**Prefer Docker?** `docker compose up --build` — no JDK required.
**Just the tests?** `./mvnw test`.

---

## The architecture

Everything else in this README follows from one rule.

> **Dependencies point inwards. Nothing points back out.**

`domain` knows nobody. `application` knows `domain`. `adapter` knows both. Nothing that lives
inside ever imports something from further out.

```
                    ┌──────────────────────────────────────┐
   HTTP request ───▶│  adapter/in/web                      │  controllers, DTOs
                    └──────────────────┬───────────────────┘
                                       │ calls a driving port
                    ┌──────────────────▼───────────────────┐
                    │  application                         │  use cases, transactions
                    │  port/in · port/out · usecase        │
                    └──────────────────┬───────────────────┘
                                       │ uses the rule
                    ┌──────────────────▼───────────────────┐
                    │  domain                              │  ← no framework, no annotations
                    │  model · AwardIntervalCalculator     │
                    └──────────────────▲───────────────────┘
                                       │ implements a driven port
                    ┌──────────────────┴───────────────────┐
   H2, CSV file ───▶│  adapter/out                         │  JPA entities, CSV parser
                    └──────────────────────────────────────┘
```

That upward arrow at the bottom is the whole point: `MoviePersistenceAdapter` depends on
`MovieRepositoryPort`, never the reverse. Swapping H2 for Postgres, or CSV for JSON, touches
only `adapter/`.

| Layer | Files | Lines | May import |
|---|---:|---:|---|
| `domain` | 6 | 134 | **nothing but `java.*`** |
| `application` | 8 | 145 | `domain`, plus `@Service` and `@Transactional` |
| `adapter` | 14 | 485 | `application`, `domain`, any framework |
| `infrastructure` | 3 | 163 | `application`, Spring |

The core that matters is small — 279 lines across domain and application. Most of the code is
adapter, which is exactly what you expect once the rules are isolated from the technology.

### Where the business rule lives

`domain/service/AwardIntervalCalculator` is the only place that knows what a "shortest gap"
means. It takes a list of wins and returns the extremes, ties included. No annotations, no
injected dependencies — construct it with `new` and call it.

```java
AwardIntervals result = new AwardIntervalCalculator().calculate(wins);
```

The repository's job is to fetch `(producer, year)` pairs. Interpreting them is not its job.

### Domain objects are not database rows

`Movie` and `Producer` are immutable `record`s in `domain/model`. The JPA entities —
`MovieEntity`, `ProducerEntity` — live in `adapter/out/persistence/entity`, and
`MoviePersistenceAdapter` translates between them. Persistence concerns never reach the domain.

---

## How data flows

**At startup** — requirement 2.1 of the assessment.

1. Hibernate creates the schema (`ddl-auto=create-drop`).
2. `MovieCsvLoader` fires once every bean exists (`SmartInitializingSingleton`).
3. `app.movies-csv` is resolved — `classpath:` or `file:`, your choice.
4. The parser validates **the whole file** before anything is written. Errors accumulate with
   line numbers rather than failing on the first one.
5. `ImportMoviesService` writes it in a single transaction. All or nothing.

**On a read** — `GET /api/public/producers/win-intervals`.

1. Security filter chain: the `/api/public/**` route is `permitAll()`.
2. `ProducerController` calls `GetAwardIntervalsUseCase` — the interface, not an implementation.
3. The use case asks the driven port for every win.
4. `AwardIntervalCalculator` groups, sorts, finds the extremes and collects the ties.
5. `AwardIntervalsResponse.from()` maps domain objects to the HTTP contract; Jackson serialises.

---

## API reference

| Route | Method | Access | Purpose |
|---|---|---|---|
| `/api/public/producers/win-intervals` | `GET` | **open** | The assessment endpoint |
| `/api/producers/win-intervals` | `GET` | any valid JWT | Same payload, behind auth |
| `/auth/token` | `POST` | HTTP Basic | Issues a 1-hour JWT |
| `/api/movies/import` | `POST` | role `ADMIN` | Replaces the dataset |
| `/h2-console` | — | open | Database browser (demo only) |

The duplicated read route is deliberate: the evaluation runs with zero friction, while the
security setup stays visible.

### Replacing the dataset

```bash
TOKEN=$(curl -s -X POST -u admin:passAdmin http://localhost:8080/auth/token | jq -r .token)
curl -H "Authorization: Bearer $TOKEN" -F "file=@/path/to/Movielist.csv" \
     http://localhost:8080/api/movies/import
```

```json
{ "movies": 206, "winners": 42, "producers": 359 }
```

The file must be `.csv`, at most 5 MB, and start with the header
`year;title;studios;producers;winner`. Anything malformed is rejected wholesale and
**the loaded data is left untouched**:

```json
{
  "message": "Invalid movie data",
  "errors": ["Line 3: year \"20XX\" is not a number"]
}
```

The message stays format-agnostic on purpose — `InvalidMovieDataException` is the contract of
`MovieParserPort`, and a future JSON adapter would raise the very same exception.

### Credentials

| User | Password | Roles | Can |
|---|---|---|---|
| `user` | `passUser` | `USER` | read |
| `admin` | `passAdmin` | `USER`, `ADMIN` | read and replace the dataset |

Tokens are HS256, self-issued, valid for one hour. Missing or invalid → `401`. Valid but
without `ADMIN` on the import route → `403`.

**The signing key is not in this repository.** Leave `JWT_SECRET` unset and the application
generates a random key at startup — it runs with no configuration, and tokens simply do not
outlive a restart, which costs nothing for a database that lives in memory. Pin it when you
want tokens to survive one:

```bash
JWT_SECRET=your-key-of-at-least-32-bytes ./mvnw spring-boot:run
```

Anything shorter than 32 bytes fails fast at startup rather than deep inside the JWT library.

> The two demo passwords *are* in `application.properties`, on purpose: the evaluation has to
> be able to log in without setup. Override them with `app.security.user-password` and
> `app.security.admin-password`. In production all three would come from a secret manager.

All nine scenarios — tokens, reads, imports, failures — are runnable from
[`requests.http`](requests.http) in IntelliJ's HTTP Client.

---

## Tests

```bash
./mvnw test                # or: docker build --target test .
```

19 integration tests, as required by item 4.2. Every one boots the full application on a random
port and speaks real HTTP. No `@MockBean`, no `@WebMvcTest`.

| Class | Tests | Covers |
|---|---:|---|
| `SecurityIntegrationTest` | 8 | Token issuing, wrong password, 401 without and with a forged token, 403 without `ADMIN`, payload parity between the open and protected routes |
| `CsvUploadIntegrationTest` | 7 | Valid upload, tied producers, nobody repeating, and four parameterised rejection scenarios |
| `AwardIntervalsIntegrationTest` | 3 | JSON contract, expected values, independent recalculation |
| `GoldenRaspberryApiApplicationTests` | 1 | Context loads |

Two of them carry most of the weight:

**`agreesWithAnIndependentRecalculationOfTheSourceFile`** recomputes the answer straight from
the CSV by a deliberately different route — sorting every win by `(producer, year)` and walking
the list once — then compares. If the production calculation drifts, this fails even when
someone updates the hard-coded expectations to match the drift.

**`exposesTheMinAndMaxFieldsRequiredBySpecification`** deserialises the response and asserts
`containsOnlyKeys`, so it proves there are exactly `min` and `max`, each entry carrying exactly
the four fields the specification asks for — none missing, none extra.

Scenarios are composed rather than hand-written, which keeps each row's intent visible and
exercises all three producer-credit styles the parser must handle:

```java
movieList()
    .winner(2000, "Movie A", "Producer X")                // "Producer X"
    .winner(2004, "Movie C", "Producer Y", "Producer X")  // "Producer Y and Producer X"
    .nominee(2011, "Movie E", "Producer Z")
    .render();
```

The assessment warns that other datasets will be used. Two scenarios exist for exactly that:
several producers **tied** on the same shortest and longest interval, and a dataset where
**nobody wins twice**, which answers `{"min": [], "max": []}` rather than failing.

---

## Configuration

Override any of these with `--property=value`, an environment variable, or your own
`application.properties`.

| Property | Default | Notes |
|---|---|---|
| `app.movies-csv` | `classpath:Movielist.csv` | Accepts `file:` for an external list |
| `spring.datasource.url` | `jdbc:h2:mem:goldenraspberry` | In-memory; wiped on shutdown |
| `spring.servlet.multipart.max-file-size` | `5MB` | Upload ceiling |
| `app.security.jwt-secret` | *(empty)* | Reads `JWT_SECRET` from the environment. Unset → a random key per run |
| `app.security.user-password` | `passUser` | |
| `app.security.admin-password` | `passAdmin` | |

Point the app at a different list without rebuilding:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--app.movies-csv=file:/path/to/other.csv
```

H2 console at `http://localhost:8080/h2-console` — JDBC URL `jdbc:h2:mem:goldenraspberry`,
user `sa`, empty password.

---

## Decisions and trade-offs

| Decision | Why | What it costs |
|---|---|---|
| Business rule in the domain, not in SQL | The rule runs without a database, a framework, or H2 | An earlier version used a `LAG()` window function — shorter and faster, but the rule lived in the persistence layer |
| Separate domain records and JPA entities | Persistence never leaks inwards | A mapping step, and two classes where one would do |
| Ports declared in `application`, not next to their implementations | Whoever needs the contract owns it | More interfaces to navigate |
| Producer names matched **verbatim** | The API must reflect the source data exactly | `"Michael DeLuca"` and `"Michael De Luca"` count as two people |
| Only integration tests | Item 4.2 requires it — and they survived a full architectural rewrite untouched | Slower feedback; a failure points at the system, not a class |
| Read endpoint exposed twice | Zero-friction evaluation without hiding the security work | The same resource under two URIs |

Richardson Maturity Model **level 2**: resources addressed by URI, HTTP verbs used for what they
mean, and status codes (`200`, `400`, `401`, `403`) carrying the outcome.

Edge cases handled: years with several winners (1986, 1990, 2015 in the bundled file), years
with no winner marked (2007), producers credited in all three separator styles, and datasets
where no producer ever repeats.

---

## Project layout

```
domain/          model/ · service/                    the rule, framework-free
application/     port/in/ · port/out/ · usecase/      use cases and their contracts
adapter/         in/web/ · out/persistence/ · out/csv/  the outside world
infrastructure/  security/ · bootstrap/ · DomainConfig  framework wiring
```

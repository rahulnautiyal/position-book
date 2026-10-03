POSITION BOOK - TECHNICAL EXERCISE
==================================

1. PURPOSE
----------
This project implements the Position Book technical exercise supplied for the
interview.

The exercise requires an in-memory system that maintains the real-time quantity
of a traded security, aggregated by trading account and security identifier.
The Position Book must also retain the details of events involving that account
and security. The system processes BUY, SELL and CANCEL trade events and exposes
REST APIs for event processing and position retrieval.

The implementation is intentionally kept in memory as required by the exercise;
no database or filesystem is used for business state.

2. TECHNOLOGY
-------------
- Java 21
- Spring Boot 4.1.1
- Spring MVC
- Jakarta Bean Validation
- Spring Boot Actuator
- springdoc-openapi for OpenAPI 3 / Swagger UI
- JUnit 5
- Spring MockMvc
- Gradle 8.14.3 bootstrap wrapper

3. PROJECT STRUCTURE
--------------------
src/main/java/com/example/positionbook/
  api/          REST controllers, DTOs and HTTP exception handling
  config/       OpenAPI configuration
  domain/       Position and trade-event domain objects
  exception/    Meaningful business exceptions
  repository/   In-memory state abstraction and implementation
  service/      Position Book business logic

src/test/java/
  service/      Business-rule/unit tests
  api/          Web/API contract tests

4. API CONTRACT
----------------
Base path: /api/v1
Content type for requests: application/json
Normal response content type: application/json
Error response content type: application/problem+json

4.1 POST /api/v1/trade-events
------------------------------
Purpose:
    Process a BUY, SELL or CANCEL event.

Request example - BUY:
    {
      "eventId": 1,
      "type": "BUY",
      "tradingAccount": "ACC1",
      "securityId": "SEC1",
      "quantity": 100
    }

Request example - SELL:
    {
      "eventId": 2,
      "type": "SELL",
      "tradingAccount": "ACC1",
      "securityId": "SEC1",
      "quantity": 50
    }

Request example - CANCEL:
    {
      "eventId": 1,
      "type": "CANCEL"
    }

For CANCEL, eventId identifies the original event to cancel. The exercise states
that cancellation quantity and security ID are meaningless. Therefore they are
optional and ignored by the service. tradingAccount is also ignored for a CANCEL.
If supplied, these fields are not used to determine what is cancelled.

Success:
    HTTP 201 Created
Errors:
    400 VALIDATION_ERROR       Request validation failed.
    400 MALFORMED_REQUEST      JSON/body cannot be parsed.
    400 INVALID_TRADE_EVENT    Business rule makes the event invalid.
    409 TRADE_EVENT_CONFLICT   Duplicate trade ID or repeated cancellation.
    422 POSITION_OVERFLOW      Resulting position cannot fit in a signed long.
    500 INTERNAL_ERROR         Unexpected server-side failure.

4.2 GET /api/v1/positions/{tradingAccount}/{securityId}
---------------------------------------------------------
Purpose:
    Retrieve the current aggregate position for an account/security pair.

Example:
    GET /api/v1/positions/ACC1/SEC1

Response 200:
    {
      "tradingAccount": "ACC1",
      "securityId": "SEC1",
      "quantity": 150
    }

Errors:
    400 VALIDATION_ERROR       Path parameter is invalid.
    404 POSITION_NOT_FOUND     No position has ever been materialized for the pair.

4.3 GET /api/v1/positions/{tradingAccount}/{securityId}/details
----------------------------------------------------------------
Purpose:
    Retrieve the current position plus the event drilldown in processing order.

Example response:
    {
      "position": {
        "tradingAccount": "ACC1",
        "securityId": "SEC1",
        "quantity": 50
      },
      "events": [
        {
          "eventId": 10,
          "type": "BUY",
          "tradingAccount": "ACC1",
          "securityId": "SEC1",
          "quantity": 100
        },
        {
          "eventId": 11,
          "type": "SELL",
          "tradingAccount": "ACC1",
          "securityId": "SEC1",
          "quantity": 50
        }
      ]
    }

Errors:
    400 VALIDATION_ERROR
    404 POSITION_NOT_FOUND

5. ERROR HANDLING
-----------------
The API uses RFC 7807-compatible Problem Details responses. Errors contain:
- type      URI identifying the problem category
- title     human-readable problem title
- status    HTTP status
- detail    meaningful description
- instance  request URI
- code      stable application error code
- timestamp diagnostic timestamp
- errors    field-level validation details when applicable

The application deliberately does not expose exception stack traces or internal
implementation details to API consumers.

6. INPUT VALIDATION
--------------------
BUY and SELL:
- eventId must be greater than zero.
- type is required and must be BUY or SELL.
- tradingAccount is required and must not be blank.
- securityId is required and must not be blank.
- quantity is required and must be greater than zero.
- account/security identifiers are limited to 100 characters.

CANCEL:
- eventId must be greater than zero.
- type must be CANCEL.
- tradingAccount, securityId and quantity are ignored at the API boundary because
they are meaningless for cancellation. The domain command is normalized to the
canonical cancellation form: quantity = 0.

Path parameters:
- tradingAccount and securityId must be non-blank and no longer than 100 chars.

Malformed JSON and unsupported enum values are rejected with a meaningful 400
Problem Details response.

7. BUSINESS RULES AND ASSUMPTIONS
----------------------------------
The supplied exercise leaves some behavior unspecified. The following assumptions
are explicit and documented so that behavior is deterministic.

7.1 Event IDs
    An original BUY/SELL event ID must be unique. Reusing an original trade ID
    results in HTTP 409.

7.2 Cancellation ID semantics
    A CANCEL intentionally uses the same event ID as the original trade. This is
    required to reproduce the supplied example:

      21 BUY ACC1 SEC1 100
      21 CANCEL ACC1 SEC1 0

    The cancellation reverses the original event.

7.3 Unknown cancellation
    Cancelling an event ID that has not been processed is rejected with HTTP 400.

7.4 Repeated cancellation
    A trade can be cancelled only once. A second CANCEL for the same event ID is
    rejected with HTTP 409.

7.5 Cancellation payload
    The cancellation's quantity/account/security do not identify the trade and
    therefore are ignored. The original trade's account/security are used when
    reversing the position and recording the drilldown event.

7.6 Quantity
    BUY/SELL quantity must be positive. Zero has no meaningful trade effect and
    is therefore rejected. CANCEL quantity is ignored.

7.7 Negative positions
    Negative positions are allowed because the supplied exercise does not state
    that short positions are prohibited. A SELL can therefore produce a negative
    position.

7.8 Zero positions
    A position that becomes zero after a cancellation remains materialized and is
    retrievable. A pair with no processed events is returned as HTTP 404.

7.9 Ordering
    Events in the drilldown are returned in processing order.

7.10 Numeric range
    Position quantity is represented by Java long. Arithmetic overflow is rejected
    with HTTP 422 rather than silently wrapping around.

7.10.1 Numeric type rationale
    Event IDs and quantities use Java long for this exercise. BigInteger is not
    necessary merely because the domain is financial: quantity is a security unit
    count, not a monetary amount, and long provides a very large bounded range.
    Math.addExact is used so overflow is rejected instead of silently wrapping.
    If an external contract later requires identifiers beyond long or arbitrary
    precision quantities, the API/domain types should be changed deliberately as
    part of that contract rather than pre-emptively introducing BigInteger.

7.11 Concurrency
    Trade and cancellation state transitions are atomic inside the in-memory
    repository under a write lock. Reads use the read lock and therefore cannot
    observe a partially applied transition. The service itself does not use a
    global JVM lock, allowing independent positions to be processed concurrently.

    This guarantee is limited to one JVM instance. Multiple application instances
    do not share this in-memory state, so they cannot provide a single consistent
    position book. Horizontal scaling therefore requires shared/durable state or a
    single logical state owner; sticky sessions alone are not a correctness mechanism.

7.12 Persistence
    State is intentionally process-local and is lost on application restart,
    because the exercise explicitly requires in-memory storage.

7.13 Security
    Authentication and authorization are not implemented because the exercise
    does not define an identity/security model. A production bank deployment
    would normally enforce this at the platform/API gateway level.

8. TEST COVERAGE
----------------
The test suite covers:
- BUY aggregation.
- SELL reduction.
- Independent account/security positions.
- Negative/short positions.
- Same-ID cancellation.
- Cancellation of BUY and SELL events.
- Cancellation ignoring its own payload.
- Duplicate trade IDs.
- Duplicate cancellations.
- Unknown cancellations.
- Zero quantities.
- Unknown positions.
- Position drilldown ordering.
- Zero position after cancellation.
- Long overflow protection.
- Valid REST requests.
- CANCEL requests without meaningless fields.
- Bean validation failures.
- Malformed JSON.
- Business exceptions mapped to correct HTTP statuses.
- RFC 7807 Problem Details response shape.
- Position retrieval.
- Not-found response.
- Path parameter validation.
- Domain invariant validation without relying on the REST DTO.
- Repository atomicity and overflow rollback behavior.
- Concurrent trade processing.
- Concurrent duplicate cancellation, proving exactly-once cancellation state.

The tests are split between service tests (business behavior) and web-layer tests
(API contract/error handling). This keeps failures diagnostic and prevents the
REST layer tests from depending on repository implementation details.

9. OPENAPI / API DOCUMENTATION
------------------------------
When the application is running:

Swagger UI:
    http://localhost:8080/swagger-ui.html

OpenAPI JSON:
    http://localhost:8080/v3/api-docs

OpenAPI YAML:
    http://localhost:8080/v3/api-docs.yaml

Actuator health:
    http://localhost:8080/actuator/health

10. SETUP
---------
Prerequisite:
    JDK 21

Gradle does NOT need to be installed. The project includes the Gradle bootstrap
wrapper scripts.

Linux / macOS:
    chmod +x gradlew
    ./gradlew --version

Windows:
    gradlew.bat --version

The first execution downloads Gradle 8.14.3 from services.gradle.org if it is not
already present in the user's Gradle cache. Subsequent executions reuse the cached
copy.

11. BUILD
---------
Linux / macOS:
    ./gradlew clean build

Windows:
    gradlew.bat clean build

12. TEST
--------
Linux / macOS:
    ./gradlew clean test

Windows:
    gradlew.bat clean test

To see the HTML test report:
    build/reports/tests/test/index.html

13. RUN
-------
Linux / macOS:
    ./gradlew bootRun

Windows:
    gradlew.bat bootRun

The application starts on:
    http://localhost:8080

14. EXAMPLE CURL COMMANDS
-------------------------
Create BUY:
    curl -i -X POST http://localhost:8080/api/v1/trade-events \
      -H 'Content-Type: application/json' \
      -d '{"eventId":1,"type":"BUY","tradingAccount":"ACC1","securityId":"SEC1","quantity":100}'

Create SELL:
    curl -i -X POST http://localhost:8080/api/v1/trade-events \
      -H 'Content-Type: application/json' \
      -d '{"eventId":2,"type":"SELL","tradingAccount":"ACC1","securityId":"SEC1","quantity":50}'

Cancel event 1:
    curl -i -X POST http://localhost:8080/api/v1/trade-events \
      -H 'Content-Type: application/json' \
      -d '{"eventId":1,"type":"CANCEL"}'

Get position:
    curl -i http://localhost:8080/api/v1/positions/ACC1/SEC1

Get position drilldown:
    curl -i http://localhost:8080/api/v1/positions/ACC1/SEC1/details

15. SAMPLE EXERCISE STREAMS
----------------------------
The implementation is designed to reproduce the supplied examples:

Example 1:
    1 BUY ACC1 SEC1 100
    2 BUY ACC1 SEC1 50
    => ACC1 / SEC1 = 150

Example 2:
    3 BUY ACC1 SEC1 12
    4 BUY ACC1 SECXYZ 50
    5 BUY ACC2 SECXYZ 33
    6 BUY ACC1 SEC1 20
    => ACC1 / SEC1 = 32
    => ACC1 / SECXYZ = 50
    => ACC2 / SECXYZ = 33

Example 3:
    10 BUY ACC1 SEC1 100
    11 SELL ACC1 SEC1 50
    => ACC1 / SEC1 = 50

Example 4:
    21 BUY ACC1 SEC1 100
    21 CANCEL ACC1 SEC1 0
    22 BUY ACC1 SEC1 5
    => ACC1 / SEC1 = 5

16. PRODUCTION CONSIDERATIONS
-----------------------------
The implementation uses production-oriented engineering practices within the explicit
constraints of the exercise. It is not a horizontally scalable shared position book
because its state is intentionally process-local. A real multi-instance
investment-bank service would additionally need:
- durable persistence/event sourcing;
- durable idempotency and event audit storage;
- authentication and authorization;
- encryption and secrets management;
- structured logging and correlation IDs;
- metrics, tracing and alerting;
- shared durable state plus distributed concurrency/ordering guarantees;
- operational retention/replay strategy;
- rate limiting and API gateway controls;
- deployment/readiness/liveness configuration;
- resilience and disaster-recovery design.

These are deliberately outside the exercise's in-memory scope rather than being
silently introduced into the solution.

17. REPOSITORY / SUBMISSION
----------------------------
The technical exercise asks for a public GitHub repository and states that it
should be deleted after the interview process. Before submission, push this
project to the requested repository and remove it after the process as required.

END OF README

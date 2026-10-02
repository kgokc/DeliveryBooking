# Delivery Slots

[![Build Status](https://dev.azure.com/kgokc-ci/DeliveryBooking/_apis/build/status%2Fkgokc.DeliveryBooking?branchName=main)](https://dev.azure.com/kgokc-ci/DeliveryBooking/_build/latest?definitionId=1&branchName=main)

A small Spring Boot REST API for booking grocery delivery slots. Each slot has a fixed capacity, and a slot can never be overbooked.

Stack is Java 21, Spring Boot 4, Spring Data JPA, PostgreSQL.

## Run it

Requires JDK 21 and Docker.

```bash
docker compose up -d          # starts Postgres on localhost:5432
./mvnw spring-boot:run        # starts the API on localhost:8080
```

Hibernate creates the tables on startup. There is no endpoint for creating slots (out of scope), so insert one to try it:

```bash
docker compose exec postgres psql -U delivery -d delivery -c \
  "insert into slot(start_time, end_time, capacity, booked, version) \
   values (now() + interval '1 day', now() + interval '1 day 1 hour', 2, 0, 0)"
```

## API

| Method | Path | Result |
| --- | --- | --- |
| `GET` | `/slots` | List slots with capacity and free places |
| `POST` | `/slots/{slotId}/bookings` | `201` with the booking, `409` if the slot is full, `404` if unknown |
| `DELETE` | `/bookings/{bookingId}` | `200` with the cancelled booking, `404` if unknown |

```bash
curl localhost:8080/slots
curl -X POST localhost:8080/slots/1/bookings
curl -X DELETE localhost:8080/bookings/1
```

Cancelling is idempotent: cancelling an already-cancelled booking does not free a second place.

## Tests

```bash
./mvnw test
```

- `SlotTest`: unit tests for the capacity rule, plain Java with no Spring or database.
- `BookingIntegrationTest`: runs the full app against a throwaway Postgres started by Testcontainers (Docker required). It checks the 201/409 behaviour and races 10 threads for 3 places.

## Design decision: optimistic locking

The invariant is that `booked` never exceeds `capacity`. The rule itself lives in `Slot.reserve()`, so it can be unit-tested without a database. The hard part is two requests taking the last place at the same time: both read `booked = 9`, both write `10`.

I guard against this with a `@Version` column on `Slot` (optimistic locking). Every update is `... WHERE id = ? AND version = ?`, so when two transactions race, the second one matches zero rows and fails instead of silently overwriting. That failure is returned as `409`.

Why not a pessimistic lock (`SELECT ... FOR UPDATE`)? Slots are read far more often than they are booked, and most bookings do not collide. Optimistic locking takes no locks on the common path and needs no extra query.

The tradeoff: under heavy contention on one slot, a request can lose the race and get `409` even though places remain. The client is expected to retry. If contention on hot slots became a real problem, switching that one query to a pessimistic lock, or adding automatic retry in the service, would be the next step.

## Out of scope

Auth, users, a frontend, creating slots via the API, and deployment.

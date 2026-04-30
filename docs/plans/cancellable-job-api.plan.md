# Plan: CancellableJobApi (API Layer Only)

## Goal
Define and implement the API-layer contract for `CancellableJobApi` (routes, HTTP status mapping, headers, and API tests), aligned with `docs/articles/cancellable-job-api.art.md`.

## Context
- Existing async API style:
  - `src/main/scala/async/rest/AsyncJobApi.scala`
  - `src/test/scala/async/rest/AsyncJobApiSpec.scala`
- Shared header utility:
  - `src/main/scala/async/rest/http.scala`
- Article intent: start job, cancel job, and query job status via `HEAD /jobs/{jobId}`.
- Development process as per TDD and API contract-first design.

## Scope

### In scope
- TDD: red, green, refactor cycle to drive API design and implementation.
- API routes for:
  - `POST /jobs` (no request body, just starts a new job and returns job ID and count)
  - `DELETE /jobs/{jobId}`
  - `HEAD /jobs/{jobId}`
- API-only translation from service outcomes to HTTP responses.
- Header models/codecs required by these routes.
- API-layer tests for route behavior and headers.

### Out of scope
- Service/job-runner internals, persistence, and scheduling.
- Concurrency/cancellation mechanics inside domain/service layer.
- File-generation internals for completed job result.
- `GET /jobs/{jobId}` (deferred to a later iteration; response-body/file semantics are intentionally excluded from this plan)

## API Contract Decisions
- `POST /jobs`
  - `202 Accepted`
  - Headers: `Location`, `X-Job-Id`, `X-Total-Count`
- `DELETE /jobs/{jobId}`
  - `204 No Content` when cancellation accepted or the job is already terminal (`cancelled`, `failed`, `completed`), idempotent behavior.
  - `404 Not Found` if job does not exist
  - Success headers: `X-Job-Id`, `X-Done-Count`, `X-Job-Status=cancelled`
- `HEAD /jobs/{jobId}`
  - `404 Not Found` if missing
  - `200 OK` for known states (`running`, `cancelled`, `failed`, `completed`)
  - Response headers: `X-Job-Id`, `X-Done-Count`, `X-Job-Status`
  - For `failed`, additionally expose `X-Failure-Reason`


### Failed-state rule
`Failed` is a valid job lifecycle outcome for long-running jobs. It is not an internal server error and must be represented as a known job state, not as `500 Internal Server Error`.

## Testing Plan (API Only)
Create `src/test/scala/async/rest/CancellableJobApiSpec.scala` and cover:
- `POST /jobs` -> `202` + `Location`, `X-Job-Id`, `X-Total-Count`
- `DELETE /jobs/{jobId}` success -> `204` + cancellation headers
- `DELETE /jobs/{jobId}` terminal states (`cancelled`, `failed`, `completed`) -> still `204` (idempotent behavior)
- `DELETE /jobs/{jobId}` missing -> `404`
- `HEAD /jobs/{jobId}` each state -> `200` + expected headers
- `HEAD /jobs/{jobId}` missing -> `404`
- Explicit assertion: failed state is not mapped to `500`

## Implementation Plan
1. Use TDD to drive API design and implementation, starting with tests that define expected status codes and headers for each route and job state.
2. Implement API routes in `CancellableJobApi` to satisfy tests, mapping service outcomes to HTTP responses as per the contract.
3. Implementation circle should be: red (write failing test), green (implement just enough to pass), refactor (clean up code while keeping tests green).
4. Freeze API boundaries and status/header contract in this plan.
5. Add/extend custom headers in `src/main/scala/async/rest/http.scala`:
   - `X-Job-Id`
   - `X-Done-Count`
   - `X-Job-Status`
   - `X-Failure-Reason`
6. Create `src/main/scala/async/rest/CancellableJobApi.scala` with http4s routes for `POST`, `DELETE`, `HEAD`.
7. Map service outcomes to HTTP responses exactly as documented above.
8. Keep API layer orchestration-only; avoid embedding business logic.
9. `CancellableService` contains dummy implementation for API development/testing, but is not the focus of this plan.


## Clarifications
- `DELETE` should be idempotent `204` for already terminal jobs
- `HEAD` should not include `X-Total-Count` for completed jobs, as the count is only relevant for in-progress jobs.

## Definition of Done
- [ ] Contract is documented for `POST`, `DELETE`, `HEAD`.
- [ ] Failed state is modeled as a normal job outcome, not `500`.
- [ ] Required headers are defined and used consistently.
- [ ] `CancellableJobApi` route implementation is API-layer only.
- [ ] API tests cover all status/header branches and pass.
- [ ] API tests explicitly verify idempotent `DELETE` behavior for terminal states.


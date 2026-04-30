# Plan: CancellableJobApi (API Layer Only)

## Goal
Define and implement the API-layer contract for `CancellableJobApi` (routes, HTTP status mapping, headers, and API tests), aligned with `docs/articles/cancellable-job-api.art.md`.

## Context
- Existing async API style:
  - `src/main/scala/async/rest/AsyncJobApi.scala`
  - `src/test/scala/async/rest/AsyncJobApiSpec.scala`
- Shared header utility:
  - `src/main/scala/async/rest/http.scala`
- Article intent: start job, cancel job, and query job status.
- Development process as per TDD and API contract-first design.

## Scope

### In scope
- API routes for:
  - `POST /jobs`
  - `DELETE /jobs/{jobId}`
  - `HEAD /jobs/{jobId}`
- API-only translation from service outcomes to HTTP responses.
- Header models/codecs required by these routes.
- API-layer tests for route behavior and headers.

### Out of scope
- Service/job-runner internals, persistence, and scheduling.
- Concurrency/cancellation mechanics inside domain/service layer.
- File-generation internals for completed job result.
- `GET /jobs/{jobId}`

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
  - Headers: `X-Job-Id`, `X-Done-Count`, `X-Job-Status`
- Headers for non-completed states: `X-Job-Id`, `X-Done-Count`, `X-Job-Status`
  - `failed` additionally exposes `X-Failure-Reason`


### Failed-state rule
`Failed` is a valid job lifecycle outcome for long-running jobs. It is not an internal server error.

## Testing Plan (API Only)
Create `src/test/scala/async/rest/CancellableJobApiSpec.scala` and cover:
- `POST /jobs` -> `202` + `Location`, `X-Job-Id`, `X-Total-Count`
- `DELETE /jobs/{jobId}` success -> `204` + cancellation headers
- `DELETE /jobs/{jobId}` missing -> `404`
- `HEAD /jobs/{jobId}` each state -> `200` + expected headers
- `HEAD /jobs/{jobId}` missing -> `404`
- Explicit assertion: failed state is not mapped to `500`

## Implementation Plan
1. Use TDD to drive API design and implementation, starting with tests that define expected status codes and headers for each route and job state.
2. Freeze API boundaries and status/header contract in this plan.
3. Add/extend custom headers in `src/main/scala/async/rest/http.scala`:
   - `X-Job-Id`
   - `X-Done-Count`
   - `X-Job-Status`
   - `X-Failure-Reason`
4. Create `src/main/scala/async/rest/CancellableJobApi.scala` with http4s routes for `POST`, `DELETE`, `HEAD`.
5. Map service outcomes to HTTP responses exactly as documented above.
6. Keep API layer orchestration-only; avoid embedding business logic.
7. `CancellableService` contains dummy implementation for API development/testing, but is not the focus of this plan.


## Clarifications
- `DELETE` should be idempotent `204` for already terminal jobs
- `HEAD` should not include `X-Total-Count` for completed jobs, as the count is only relevant for in-progress jobs.

## Definition of Done
- [ ] Contract is documented for `POST`, `DELETE`, `HEAD`.
- [ ] Failed state is modeled as a normal job outcome, not `500`.
- [ ] Required headers are defined and used consistently.
- [ ] `CancellableJobApi` route implementation is API-layer only.
- [ ] API tests cover all status/header branches and pass.


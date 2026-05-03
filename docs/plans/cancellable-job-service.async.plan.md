# Plan: CancellableService#start() asynchronous implementation (Service Layer)

## Goal
Evolve `CancellableService#start()` from synchronous behavior to async fire-and-forget semantics with TDD.

The target behavior matches the async intent already established in:
- `src/test/scala/async/rest/AsyncJobApiSpec.scala` (immediate response + background processing)
- `src/main/scala/async/rest/AsyncJobApi.scala` (`.start` to launch background work)
- `docs/articles/async-job-api.art.md` (red-green-refactor journey to non-blocking flow)

## Context
- Current implementation to evolve: `src/main/scala/async/service/CancellableService.scala`
- Existing test suite: `src/test/scala/async/service/CancellableServiceSpec.scala`
- Worker contract: `src/main/scala/async/service/CancellableWorker.scala`
- Baseline already done: synchronous `start()` calls `prepare()` and waits for `run(jobId)` completion.

TDD cycle for each change:
- Red: add just enough failing test for desired async behavior.
- Green: implement minimal code to pass.
- Refactor: remove duplication and improve readability while keeping tests green.

## Scope

### In scope
- Service-layer behavior of `CancellableService#start()` only.
- Converting `start()` to fire-and-forget launch semantics.
- Service-layer tests proving non-blocking behavior and existing contract stability.

### Out of scope
- API route changes (`CancellableJobApi`, `AsyncJobApi`).
- Other service methods (e.g., cancellation, status).
- Cancellation/status lifecycle mechanics and persistence.
- Full background error-reporting policy (deferred to next iteration).

## Contract Decisions (Service Layer)
1. `start()` calls `worker.prepare()` once.
2. `start()` returns `JobStarted(id, totalCount)` based on `prepare()` output.
3. `worker.run(jobId)` is triggered once in background (fire-and-forget) and is not awaited by `start()`.
4. `start()` completion does not depend on `run()` completion.
5. Background failure propagation/observability is deferred in this iteration.

## Implementation TDD Steps
1. **Red**: add a test proving `start()` returns quickly even when `worker.run(jobId)` is blocked.
2. Keep/adjust existing assertions to still prove:
   - `prepare()` called once,
   - `run(jobId)` called once,
   - returned `JobStarted` contains expected `id` and `totalCount`.
3. **Green**: update `start()` to launch `worker.run(jobId)` via background fiber (`.start`) and return immediately.
4. **Refactor**: extract shared async test setup (for example `Deferred` helpers) and remove duplication.
5. Add/confirm a regression test that ensures data contract stays the same while execution model becomes async.
6. **Refactor**: make service flow explicit and readable: prepare -> launch background run -> return started metadata.

## Definition of Done
- [ ] `CancellableServiceSpec` proves `start()` does not wait for `run()` completion.
- [ ] `CancellableServiceSpec` proves `prepare()` and `run(jobId)` are invoked once.
- [ ] `CancellableServiceSpec` proves returned `JobStarted(id, totalCount)` is correct.
- [ ] `CancellableService#start()` uses fire-and-forget semantics clearly at service layer.
- [ ] Test code is stable (bounded timeout/deferred control) and free of unnecessary duplication.

## Risks / Notes
- Async tests can become flaky without deterministic coordination (`Deferred`, bounded timeout).
- Fire-and-forget can hide runtime failures; a follow-up plan should define failure logging/reporting.

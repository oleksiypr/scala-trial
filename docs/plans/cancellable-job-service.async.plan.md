# Plan: CancellableService#start() asynchronous implementation (Service Layer)

## Goal: Implement the `start` method in `CancellableService` to handle job preparation, and asynchronous start by Fire-And-Forget principles with TDD-driven development.

## Context:
- `src/main/scala/async/service/CancellableService.scala`
- `src/test/scala/async/service/CancellableServiceSpec.scala`
- TDD approach: Red, Green, Refactor cycle to drive implementation:
    - Red: add juts enough test to be red (failing) for the new functionality.
    - Green: implement just enough code to make the test pass.
    - Refactor: eliminate duplication (including test/implementation hardcoded values that were used to make the test green at the first attempt).

## Scope:

### In scope:
- Implementing synchronous implementation  `CancellableService#start()`
- TDD: red, green, refactor cycle to drive implementation.

### Out of scope:
- Concurrency and cancellation mechanics (deferred to a later iteration)
- Asynchronous execution and job management (deferred to a later iteration)
- Relation to `POST /jobs` endpoint (deferred to a later iteration)

## Implementation TDD Steps:

We start with a sequential implementation as per TDD principles:
1. Red test for `CancellableService#start()` that expects a `JobStarted` response with job ID and total count.
2. Green implementation that generates a job ID, estimates total count, and returns hardcoded `JobStarted`.
3. Add red test: verify  `CancellableWorker#prepare()` invocation in `CancellableService#start()` that returns new job ID and estimates job count.
4. Green implementation: implement `CancellableWorker#prepare()` invocation in `CancellableService#start()` that conforms step #1.
5. Refactor: find and remove duplications if any.
6. Add red test: verify `CancellableWorker#run(jobId)` invocation in `CancellableService#start()`.
7. Green implementation: implement `CancellableWorker#run(jobId)` invocation in `CancellableService#start()`, run synchronously (i.e., block until completion).
8. Refactor: find and remove duplications if any.

## Definition of Done:
- [ ] `CancellableService#start()` is implemented with a synchronous flow that prepares and runs a job, returning a `JobStarted` response with job ID and total count.
- [ ] tests in `CancellableServiceSpec` related to `start()` are passing.
- [ ] Implementation is clean and free of unnecessary duplications.
# Plan: CancellableService#start() synchronous implementation (Service Layer)

Goal: Implement the `start` method in `CancellableService` to handle job preparation, and synchronous start with TDD-driven development.


We start with a sequential implementation:
1. Red test for `CancellableService#start()` that expects a `JobStarted` response with job ID and total count.
2. Green implementation that generates a job ID, estimates total count, and returns hardcoded `JobStarted`.
3. Add red test: verify  `CacleebleWorker#prepare()` invocation in `CancellableService#start()` that returns new job ID and estimates job count.
4. Green implementation: implement `CacleebleWorker#prepare()` invocation in `CancellableService#start()` that conforms step #1.
5. Refactor: find and remove duplications if any.
6. Add red test: verify `CacleebleWorker#run(jobId)` invocation in `CancellableService#start()`.
7. Green implementation: implement `CacleebleWorker#run(jobId)` invocation in `CancellableService#start()`, but still run synchronously (i.e., block until completion).
8. Refactor: find and remove duplications if any.





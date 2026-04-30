# CancellableJobApi

The `CancellableJobApi` is an interface that allows you to manage and cancel jobs in a system. It provides methods to cancel jobs, check the status of jobs, and retrieve information about jobs.

## Seq§uence Diagram

```mermaid
sequenceDiagram
    participant Client
    participant API as CancellableJobApi                    
    participant Service as CancellableService
    participant JobRunner as Runner
    Client->>API: POST /jobs 
    API->>Service: prepare
    Service->>Service: generates job ID, estimates count
    Service-->>Runner: run Job(id) (in background)
    Service->>Service: job in `Running` status, keeps track of running in a background jobs
    Service->>API: JobStarted (jobId, total_count)
    API-->>Client: 202 Accepted + headers (jobId, total_count)
    Runner-->>Service: Completed (async)
    
    Client->>API: DELETE /jobs/{jobId}
    API->>Service: cancel(jobId)
    Service->>Runner: cancel gracefully (jobId)
    Runner->>Service: Cancelled
    Service->>Service: Job(id) in `Cancelled` status, keeps track of running in a background jobs
    Service->>API: 204 No Content + headers(jobId) (Job cancelled)
```
   




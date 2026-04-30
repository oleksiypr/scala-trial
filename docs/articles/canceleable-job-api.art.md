# CancellableJobApi

The `CancellableJobApi` is an interface that allows you to manage and cancel jobs in a system. It provides methods to cancel jobs, check the status of jobs, and retrieve information about jobs.

## Sequence Diagram

```mermaid
sequenceDiagram
    participant Client
    participant API as CancellableJobApi                    
    participant Service as CancellableService
    participant Runner as JobRunner
    
    Client->>API: POST /jobs 
    API->>Service: prepare
    Service->>Service: generates job ID, estimates count
    Service-->>Runner: run Job(id) (in background)
    Service->>Service: job in `Running` status, keeps track of running in a background jobs
    Service->>API: JobStarted (jobId, total_count)
    API->>Client: 202 Accepted + headers (jobId, total_count)
    alt Runner completes successfully
        Runner-->>Service: Completed (async) 
        Service->>Service: Job(id) in `Completed` status
    else Runner fails
        Runner-->>Service: Failed (error)
        Service->>Service: Job(id) in `Failed` status
    end
    
    Client->>API: DELETE /jobs/{jobId}
    API->>Service: cancel(jobId)
    Service->>Runner: cancel gracefully (jobId)
    Runner->>Service: Cancelled
    Service->>Service: Job(id) in `Cancelled` status, keeps track of running in a background jobs
    Service->>API: JobCancelled(id, done_count) 
    API->>Client: 204 No Content + headers(jobId, done_count) (Job cancelled)

    Client->>API: HEAD /jobs/{jobId}
    alt job not found
        Service->>API: None
        API->>Client: 404 Not Found
    else
        Service->>API: JobStatus
        API->>Client: headers(jobId, done_count, staus)
    end

    Client->>API: GET /jobs/{jobId}
    API->>Service: get(jobId)
    alt job not found
        Service->>API: None
        API->>Client: 404 Not Found
    else job in progress
        Service->>API: Running(jobId, done_count)
        API->>Client: 202 Accepted + headers(jobId, done_count, running_staus)
    else job cancelled
        Service->>API: Cancelled(jobId, done_count)
        API->>Client: 409 Conflict + headers(jobId, done_count, cancelled_staus)
    else job failed
        Service->>API: Failed(jobId, done_count, failure_reason)
        API->>Client: 409 Conflict + headers(jobId, done_count, failure_reason)
    else job completed
        Service->>API: Completed(jobId, total_count, text_file as a stream)
        API->>Client: 200 OK + body as text file
    end
```
   

For the purpose of this article, we implement a `CancellableJobApi` that allows clients to start a job, cancel a job, and check the status of a job with `HEAD` request. 

The `CancellableService` is responsible for managing the jobs and their statuses, while the `JobRunner` is responsible for executing the jobs in the background. As far as a job is started it is in `Running` status.

When a client starts a job, the API prepares the job and estimates the total count of items to process. The job is then run in the background, and the client receives a response with the job ID and total count.

Assumptions:
- The job can always be started
- The job is a long-running process that can be cancelled gracefully.

# Personal Automation Hub

## Overview
A Spring Boot backend that automates sending scheduled messages to Telegram, Discord and
Email. Jobs are stored in MySQL, each with its own cron expression, message, channel and
(optional) target. Jobs are managed through a REST API, and any change (create, update,
pause, resume, delete) takes effect immediately, without restarting the application.

Every run is retried automatically on transient failures using exponential backoff, and
the outcome of each run (status, attempts, error reason) is saved in an execution log.

New channels can be added by writing a single class, thanks to the Strategy pattern.

## Concepts Covered
- Trigger -> Action model of automation
- Layered architecture: Controller -> Service -> Repository
- REST API design with proper HTTP methods and status codes (201, 204, 400, 404)
- DTOs (Java records) to separate API contracts from database entities
- Bean Validation (`@Valid`, `@NotBlank`, `@Size`, `@Pattern`) and centralized error
  handling with `@RestControllerAdvice`
- Spring Data JPA, `JpaRepository`, derived queries and `@Transactional` deletes
- Dynamic scheduling with `TaskScheduler`, `CronTrigger` and `ThreadPoolTaskScheduler`
- Tracking scheduled tasks with `ScheduledFuture` and a thread-safe `ConcurrentHashMap`
- Rebuilding schedules from the database on startup (`ApplicationReadyEvent`)
- Cron expressions (6 fields in Spring) with explicit timezone (`Asia/Kolkata`)
- Strategy pattern with a `MessageChannel` interface and a registry that Spring fills
  automatically with all implementations
- Calling external APIs (Telegram Bot API, Discord webhooks) with `RestTemplate` and
  connect/read timeouts
- Sending email with Spring Mail (SMTP)
- Transient vs permanent failures, retries with exponential backoff, and a custom
  exception that carries a `retryable` flag
- Execution log entity with an enum status (`EnumType.STRING`)
- At-least-once delivery and why retries can cause duplicate messages
- Externalized configuration: secrets live in environment variables, never in the repository

## What I Learned
- How a bot token, chat id and webhook URL identify the sender and the receiver, and why
  they must be treated as secrets
- Why `@Scheduled` cannot change at runtime and how `TaskScheduler` solves it
- Why a job is re-read from the database when it fires, so edits apply immediately
- Why swallowing exceptions is dangerous, and why errors should be reported, not hidden
- Why a wrong token should never be retried but a network timeout should
- How idempotency trade-offs differ between a reminder bot and a payment system
- How the Strategy pattern keeps code open for extension (add a channel) but closed for
  modification (no changes in the scheduler or executor)
- How to validate configuration at the API boundary (for example, rejecting a job for a
  channel that is not configured) instead of failing silently later
- How to debug Maven, IDE and API issues step by step

## How to Run
1. Prerequisites: Java 17+, Maven, MySQL 8, Git.
2. Create the database: `CREATE DATABASE automation_hub;` (tables are created automatically).
3. Create a Telegram bot with @BotFather, send `/start` to it, and find your chat id.
4. (Optional) Create a Discord webhook: Channel settings -> Integrations -> Webhooks.
5. (Optional) For Email, create a Gmail App Password (requires 2-Step Verification).
6. Set these environment variables:

| Variable | Required | Purpose |
|---|---|---|
| `TELEGRAM_BOT_TOKEN` | Yes | Telegram bot token |
| `TELEGRAM_CHAT_ID` | Yes | Telegram chat to send messages to |
| `DB_USERNAME` | Yes | MySQL username |
| `DB_PASSWORD` | Yes | MySQL password |
| `DISCORD_WEBHOOK_URL` | No | Enables the DISCORD channel |
| `MAIL_USERNAME` | No | Gmail address, enables the EMAIL channel |
| `MAIL_PASSWORD` | No | Gmail App Password, enables the EMAIL channel |

7. Run `AutomationHubApplication`. The API is available at `http://localhost:8080/api/jobs`.
8. Create a job, for example `POST /api/jobs` with the body
   `{"name": "Study reminder", "message": "DSA time!", "channel": "TELEGRAM", "cronExpression": "0 0 21 * * *"}`.
   The cron format has 6 fields: second, minute, hour, day, month, weekday (IST).
   For `EMAIL` jobs, also send `"target": "someone@example.com"`.
9. Retry behaviour can be tuned with `retry.max-attempts` and `retry.initial-delay-ms`
   in `application.properties`.

API endpoints:

| Method | URL | Purpose |
|---|---|---|
| POST | `/api/jobs` | Create a job |
| GET | `/api/jobs` | List all jobs |
| GET | `/api/jobs/{id}` | Get one job |
| PUT | `/api/jobs/{id}` | Update a job |
| DELETE | `/api/jobs/{id}` | Delete a job |
| PATCH | `/api/jobs/{id}/pause` | Pause a job |
| PATCH | `/api/jobs/{id}/resume` | Resume a job |
| GET | `/api/jobs/{id}/logs` | Latest 50 runs of a job |

Note: the API has no authentication yet, so do not expose it publicly.

## Possible Improvements
- Authentication and authorization with Spring Security, then deployment on AWS
- Schedule retries instead of sleeping in the scheduler thread, so threads are never blocked
- Retention policy to delete old execution logs
- More channels such as Slack or SMS, and per-channel message formatting
- Per-job timezone instead of a fixed IST
- Unit and integration tests (for example, testing retry logic with a fake channel)
- Database migrations with Flyway instead of `ddl-auto=update`
- Dockerize the application for easier deployment
# Personal Automation Hub

## Overview
A Spring Boot application that automates sending messages to Telegram on a schedule.
Each job has its own cron expression, stored in MySQL, and is scheduled at runtime
without restarting the application.

This is being built in stages towards a full automation platform with a REST API,
dynamic scheduling, retries, execution logs, multiple channels and deployment on AWS.

Jobs can be managed at runtime through a REST API (create, read, update, delete,
pause and resume), with request validation and clean JSON error responses.

Creating, updating, pausing, resuming or deleting a job through the API immediately
updates its schedule.

## Concepts Covered
- Trigger -> Action model of automation
- Telegram Bot API (`sendMessage`, bot token, chat id)
- Calling external REST APIs with `RestTemplate`
- Externalized configuration with environment variables and `@Value`
- Scheduling basics with `@Scheduled`
- Cron expressions (6 fields in Spring) and timezone handling (`Asia/Kolkata`)
- JPA entities, `JpaRepository` and derived queries (`findByActiveTrue`)
- Hibernate schema generation (`ddl-auto=update`)
- Seeding initial data with `CommandLineRunner`
- Constructor-based dependency injection
- Keeping secrets (bot token, DB password) out of source code
- Layered architecture: Controller -> Service -> Repository
- REST API design with proper HTTP methods and status codes (201, 204, 400, 404)
- DTOs (Java records) to separate API contracts from database entities
- Bean Validation (`@Valid`, `@NotBlank`, `@Size`, `@Pattern`)
- Centralized error handling with `@RestControllerAdvice`
- Testing APIs with Postman
- Dynamic scheduling with `TaskScheduler`, `CronTrigger` and `ThreadPoolTaskScheduler`
- Tracking scheduled tasks with `ScheduledFuture` to cancel and reschedule them
- Thread-safe state with `ConcurrentHashMap`
- Rebuilding schedules from the database on startup with `ApplicationReadyEvent`
- Cron validation with `CronExpression` and a custom exception (HTTP 400)

## What I Learned
- How a bot token and chat id identify the sender and the receiver
- Why secrets belong in environment variables and never in the repository
- How a cron expression defines a schedule, and why the timezone must be explicit
- How moving data from code into a database makes the system configurable at runtime
- How to debug API and Maven/IDE issues step by step
- Why entities are not exposed directly and DTOs are used instead
- How validation and a global exception handler produce clear error messages
- How pause/resume (an `active` flag) is safer than deleting a job
- Why `@Scheduled` cannot change at runtime and how `TaskScheduler` solves it
- Why a job is re-read from the database when it fires, so edits apply immediately
- How to keep in-memory schedules consistent with the database

## How to Run
1. Create a bot with @BotFather on Telegram and copy the bot token.
2. Send `/start` to your bot and find your chat id.
3. Create a MySQL database named `automation_hub`.
4. Set these environment variables: `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID`,
   `DB_USERNAME`, `DB_PASSWORD`.
5. Run `AutomationHubApplication` (Java 17+ and Maven required).
6. A sample job is inserted on first start. Update its `message` in the `jobs` table
   and the next run will send the new text.
7. Manage jobs through the API at `http://localhost:8080/api/jobs`:
   `POST /api/jobs`, `GET /api/jobs`, `GET /api/jobs/{id}`, `PUT /api/jobs/{id}`,
   `DELETE /api/jobs/{id}`, `PATCH /api/jobs/{id}/pause`, `PATCH /api/jobs/{id}/resume`.
   Note: the API has no authentication yet, so do not expose it publicly.
8. Create a job with its own schedule, for example:
  `POST /api/jobs` with
  `{"name": "Study reminder", "message": "DSA time!", "channel": "TELEGRAM", "cronExpression": "0 0 21 * * *"}`
   The cron format has 6 fields: second minute hour day month weekday (IST).

## Possible Improvements
- Retries and an execution log for failed messages (Stage 6)
- More channels such as Email and Slack (Stage 7)
- Authentication and deployment on AWS (Stage 8)
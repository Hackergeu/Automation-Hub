# Personal Automation Hub

## Overview
A Spring Boot application that automates sending messages to Telegram on a schedule.
Messages are stored as jobs in a MySQL database, so the content can be changed
without touching the code. The scheduler reads the active jobs from the database and
sends each one through a Telegram bot at the configured time (IST).

This is being built in stages towards a full automation platform with a REST API,
dynamic scheduling, retries, execution logs, multiple channels and deployment on AWS.

Jobs can be managed at runtime through a REST API (create, read, update, delete,
pause and resume), with request validation and clean JSON error responses.

## Concepts Covered
- Trigger -> Action model of automation
- Telegram Bot API (`sendMessage`, bot token, chat id)
- Calling external REST APIs with `RestTemplate`
- Externalized configuration with environment variables and `@Value`
- Scheduling with `@Scheduled` and `@EnableScheduling`
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

## What I Learned
- How a bot token and chat id identify the sender and the receiver
- Why secrets belong in environment variables and never in the repository
- How a cron expression defines a schedule, and why the timezone must be explicit
- How moving data from code into a database makes the system configurable at runtime
- How to debug API and Maven/IDE issues step by step
- Why entities are not exposed directly and DTOs are used instead
- How validation and a global exception handler produce clear error messages
- How pause/resume (an `active` flag) is safer than deleting a job

## How to Run
1. Create a bot with @BotFather on Telegram and copy the bot token.
2. Send `/start` to your bot and find your chat id.
3. Create a MySQL database named `automation_hub`.
4. Set these environment variables: `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID`,
   `DB_USERNAME`, `DB_PASSWORD`.
5. Optionally change `reminder.cron` in `application.properties` (default: every day at 8 AM).
6. Run `AutomationHubApplication` (Java 17+ and Maven required).
7. A sample job is inserted on first start. Update its `message` in the `jobs` table
   and the next run will send the new text.
8. Manage jobs through the API at `http://localhost:8080/api/jobs`:
   `POST /api/jobs`, `GET /api/jobs`, `GET /api/jobs/{id}`, `PUT /api/jobs/{id}`,
   `DELETE /api/jobs/{id}`, `PATCH /api/jobs/{id}/pause`, `PATCH /api/jobs/{id}/resume`.
   Note: the API has no authentication yet, so do not expose it publicly.

## Possible Improvements
- Per-job cron expressions with dynamic scheduling (Stage 5)
- Retries and an execution log for failed messages (Stage 6)
- More channels such as Email and Slack (Stage 7)
- Authentication and deployment on AWS (Stage 8)
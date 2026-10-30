# Personal Automation Hub

## Overview
A Spring Boot application that automates sending messages through a Telegram bot.
Stage 1 sends a message to my Telegram chat as soon as the application starts.
This is the foundation for a larger automation platform (scheduling, job storage,
retries, multiple channels).

## Concepts Covered
- Trigger -> Action model of automation
- Telegram Bot API (`sendMessage` endpoint, bot token, chat id)
- Calling an external REST API with `RestTemplate`
- Externalized configuration using environment variables and `@Value`
- `CommandLineRunner` to run logic at application startup
- Constructor-based dependency injection with `@Service` and `@Component`
- Keeping secrets out of source code

## What I Learned
- How a bot token and chat id identify the sender and the receiver
- Why secrets belong in environment variables, not in the repository
- How to debug API errors by testing the endpoint in the browser first
- How to resolve Maven/IDE setup issues

## How to Run
1. Create a bot with @BotFather on Telegram and copy the bot token.
2. Send `/start` to your bot, then find your chat id.
3. Set these environment variables:
    - `TELEGRAM_BOT_TOKEN`
    - `TELEGRAM_CHAT_ID`
4. Run `AutomationHubApplication` (Java 17+ and Maven required).
5. You should receive a message in your Telegram chat.

## Possible Improvements
- Send messages on a schedule instead of only at startup (Stage 2)
- Store jobs in MySQL and manage them through a REST API
- Add retries and an execution log for failed messages
- Support more channels such as Email and Slack
- Add authentication and deploy to AWS
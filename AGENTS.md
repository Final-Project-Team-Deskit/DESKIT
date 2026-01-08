# Repository Guidelines

## Project Structure & Module Organization
This repository is a Spring Boot backend with a Vue 3 + Vite frontend.

- `src/main/java/com/deskit/deskit`: backend application source (controllers, services, repositories, entities, config).
- `src/main/resources`: backend resources and SQL seeds (`src/main/resources/sql`).
- `src/test/java/com/deskit/deskit`: backend tests (JUnit 5 / Spring Boot).
- `front/`: frontend app (Vue 3 + TypeScript + Vite).
- `front/public`: static assets (images).

## Build, Test, and Development Commands
Backend (Gradle wrapper):
- `.\gradlew bootRun`: run the Spring Boot app locally.
- `.\gradlew test`: execute JUnit tests.
- `.\gradlew clean build`: clean and build the backend.

Frontend (from `front/`):
- `npm install`: install dependencies.
- `npm run dev`: start Vite dev server.
- `npm run build`: type-check and build production assets.
- `npm run preview`: preview the production build.

## Coding Style & Naming Conventions
- Java: follow standard Spring Boot conventions, 4-space indentation, PascalCase for classes, camelCase for methods/fields.
- TypeScript/Vue: 2-space indentation typical of Vite/Vue defaults; components in `front/src/components` use `PascalCase.vue`.
- Keep package names under `com.deskit.deskit` and mirror domain boundaries (e.g., `account`, `product`, `livechat`).
- No explicit formatter or linter is configured in this repo; keep changes consistent with surrounding code.

## Testing Guidelines
- Backend uses Spring Boot test starter and JUnit 5 (`src/test/java/com/deskit/deskit`).
- Name tests with `*Test.java` and group by feature area (e.g., `product`, `setup`, `order`).
- Run with `.\gradlew test`. There are no frontend tests configured here.

## Commit & Pull Request Guidelines
- Git history shows mixed conventions (merge commits, `FEAT:` prefixes, and non-English messages). Use clear, imperative commit messages and include a scope when helpful (e.g., `feat: add live chat validation`).
- PRs should include a concise summary, testing notes (commands run), and screenshots for UI changes under `front/`.

## Configuration & Services
- Database and Redis are required for many backend features; see `src/main/resources` for SQL seeds.
- `docker-compose.yml` contains a commented MySQL service example; update and enable if you want a local container.

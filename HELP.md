# Question Management Application Design

## Scope

Build a question-management application with:

- React frontend using shadcn/ui-style components for polished forms, tables, dialogs, and drag-and-drop interactions.
- Spring Boot `4.0.7` backend API on Java `21`.
- PostgreSQL local database using `jsonb` for question-type-specific configuration.
- CRUD for questions and order management through drag-and-drop.
- Actuator, metrics, and distributed tracing enabled from the beginning.

Assumption: "sheild component" means `shadcn/ui`. If a different React component library was intended, the frontend layer can be swapped without changing the backend design.

## High-Level Architecture

```text
React App
  ├─ Question Management List
  ├─ Create/Edit Question Screen
  ├─ Drag-and-drop Ordering
  └─ API Client
        │
        ▼
Spring Boot API
  ├─ REST Controllers
  ├─ Validation Layer
  ├─ Service Layer
  ├─ Repository Layer
  ├─ Flyway Migrations
  ├─ Actuator + Metrics
  └─ Micrometer Tracing
        │
        ▼
PostgreSQL
  └─ questions table with jsonb config
```

## Backend Design

### Technology Choices

- Java `21`
- Spring Boot `4.0.7`
- Spring Web MVC
- Spring Data JPA
- PostgreSQL JDBC driver
- Flyway for schema migrations
- Bean Validation for request validation
- Spring Boot Actuator for health, metrics, info, and operational endpoints
- Micrometer Tracing with Zipkin or OpenTelemetry exporter
- Testcontainers for integration tests once implementation begins

Spring Boot documentation currently lists `4.0.7` as a stable version, and its Actuator tracing support is backed by Micrometer Tracing.

### Package Structure

```text
com.example.questionmanagement
  ├─ QuestionManagementApplication
  ├─ question
  │   ├─ api
  │   │   ├─ QuestionController
  │   │   ├─ QuestionRequest
  │   │   ├─ QuestionResponse
  │   │   └─ ReorderQuestionsRequest
  │   ├─ domain
  │   │   ├─ Question
  │   │   ├─ QuestionType
  │   │   └─ QuestionStatus
  │   ├─ service
  │   │   └─ QuestionService
  │   └─ persistence
  │       └─ QuestionRepository
  ├─ common
  │   ├─ error
  │   ├─ validation
  │   └─ web
  └─ config
      ├─ CorsConfig
      ├─ OpenApiConfig
      └─ ObservabilityConfig
```

## Data Model

### Question Entity

Use a relational table for stable searchable fields and `jsonb` for type-specific constraints.

```sql
create table questions (
  id uuid primary key,
  question_text varchar(500) not null,
  description varchar(1000),
  type varchar(30) not null,
  required boolean not null default false,
  display_order integer not null,
  config jsonb not null,
  status varchar(20) not null default 'ACTIVE',
  created_at timestamptz not null,
  updated_at timestamptz not null,
  version bigint not null default 0,
  constraint uq_questions_display_order unique (display_order)
);

create index idx_questions_status_order on questions(status, display_order);
create index idx_questions_config_gin on questions using gin(config);
```

### Supported Question Types

#### Text

```json
{
  "minLength": 1,
  "maxLength": 250,
  "placeholder": "Enter answer",
  "multiline": false,
  "pattern": null
}
```

#### Number

```json
{
  "min": 0,
  "max": 100,
  "step": 1,
  "allowDecimal": false
}
```

#### Dropdown

```json
{
  "options": [
    { "label": "Low", "value": "LOW" },
    { "label": "Medium", "value": "MEDIUM" },
    { "label": "High", "value": "HIGH" }
  ],
  "allowMultiple": false
}
```

#### Document

```json
{
  "allowedTypes": ["pdf", "txt", "docx"],
  "maxSizeMb": 10,
  "minFiles": 0,
  "maxFiles": 1
}
```

### Validation Strategy

- Validate common fields with Bean Validation.
- Validate `config` based on `type` in a dedicated service validator.
- Reject invalid combinations, such as `TEXT` with missing `maxLength`, `NUMBER` with `min > max`, `DROPDOWN` with empty options, or `DOCUMENT` with unsupported file extensions.
- Use optimistic locking through `version`.

## API Design

Base path: `/api/v1/questions`

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/questions` | List questions ordered by `displayOrder` |
| `GET` | `/api/v1/questions/{id}` | Get one question |
| `POST` | `/api/v1/questions` | Create question |
| `PUT` | `/api/v1/questions/{id}` | Update question |
| `DELETE` | `/api/v1/questions/{id}` | Soft delete question |
| `PATCH` | `/api/v1/questions/reorder` | Persist drag-and-drop order |

### Create/Update Request

```json
{
  "questionText": "What is your age?",
  "description": "Age in completed years",
  "type": "NUMBER",
  "required": true,
  "displayOrder": 1,
  "config": {
    "min": 18,
    "max": 99,
    "step": 1,
    "allowDecimal": false
  }
}
```

### Reorder Request

```json
{
  "orderedQuestionIds": [
    "f9739f1f-49d2-47da-a30c-96567f577f35",
    "77efce43-32f4-4268-a324-c5fe075d72d0"
  ]
}
```

The backend updates `display_order` transactionally. To avoid unique constraint conflicts during reorder, it can temporarily shift orders or use a single batch update with safe intermediate values.

## Frontend Design

### Technology Choices

- React with TypeScript
- Vite
- shadcn/ui-style components
- React Hook Form
- Zod validation
- TanStack Query for API state
- dnd-kit for drag-and-drop ordering
- Tailwind CSS

### Screens

#### Question Management Screen

- Table/list of questions ordered by `displayOrder`.
- Columns: drag handle, order, question text, type, required, status, updated date, actions.
- Actions: view/edit/delete.
- Drag-and-drop reorders questions and calls `PATCH /api/v1/questions/reorder`.
- Toast notification for save success/failure.
- Optimistic UI update with rollback on API failure.

#### Create/Edit Question Screen

- Common fields:
  - Question text
  - Description
  - Type
  - Required flag
  - Display order
- Dynamic config section:
  - Text: min length, max length, multiline, placeholder, pattern
  - Number: min, max, step, decimal allowed
  - Dropdown: option rows with label/value, multiple allowed
  - Document: allowed extensions, max size, min/max files
- Form-level validation mirrors backend rules.

## Observability Design

- Enable Actuator endpoints for health, info, metrics, prometheus, and traces where appropriate.
- Use Micrometer Tracing.
- Prefer OpenTelemetry exporter for production readiness; Zipkin can be used locally for quick trace visualization.
- Add correlation IDs in logs so API logs can be connected to traces.
- Include database health indicator through Spring Boot Actuator.

Suggested local management endpoints:

```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=when_authorized
management.tracing.sampling.probability=1.0
```

## Local PostgreSQL Setup

Preferred local setup is Docker so the project stays portable.

Database values:

- Container: `question-management-postgres`
- Database: `question_management`
- User: `qm_user`
- Password: `qm_password`
- Host port: `5432`

Spring datasource target:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/question_management
spring.datasource.username=qm_user
spring.datasource.password=qm_password
```

## Error Handling

- Return consistent problem responses with fields like `timestamp`, `status`, `error`, `message`, `path`, and `traceId`.
- Use `400` for validation errors, `404` for missing questions, `409` for optimistic locking conflicts, and `500` only for unexpected failures.
- Include `traceId` in error responses for support/debugging.

## Security And Best Practices

- Keep API versioned under `/api/v1`.
- Use DTOs instead of exposing JPA entities.
- Use migrations, not Hibernate auto-DDL, for schema ownership.
- Use soft delete with `status` rather than physical deletion.
- Keep JSONB flexible but validate it strictly per question type.
- Add indexes for ordered listing and JSONB search.
- Use CORS configuration scoped to the frontend dev origin.
- Keep secrets outside source code; use environment variables for real deployments.

## Implementation Phases

1. Create backend Spring Boot project and database migration.
2. Implement question CRUD and config validation.
3. Implement reorder endpoint transactionally.
4. Add actuator, metrics, tracing, and structured errors.
5. Create React project with shadcn/ui-style base components.
6. Build question list with drag-and-drop ordering.
7. Build create/edit dynamic form.
8. Add integration tests and frontend validation tests.

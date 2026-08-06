# Backend Folder Structure
## AI Studio for Software Engineering — MVP

| Field | Value |
|---|---|
| **Build** | Maven (or Gradle — Maven shown) |
| **Java** | 21 |
| **Framework** | Spring Boot 3.3+ |

---

## 1. Repository Layout

```
backend/
├── pom.xml
├── Dockerfile
├── .mvn/
├── mvnw / mvnw.cmd
├── src/
│   ├── main/
│   │   ├── java/com/aistudio/
│   │   │   ├── AiStudioApplication.java
│   │   │   ├── api/
│   │   │   ├── application/
│   │   │   ├── domain/
│   │   │   ├── infrastructure/
│   │   │   └── shared/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/
│   │           └── V1__init_schema.sql
│   └── test/
│       ├── java/com/aistudio/
│       └── resources/application-test.yml
└── README.md
```

---

## 2. Package Structure (Clean Architecture)

```
com.aistudio
├── AiStudioApplication.java
│
├── api/                              # Inbound adapters (HTTP)
│   ├── auth/
│   │   ├── AuthController.java
│   │   ├── dto/ RegisterRequest.java, LoginRequest.java, TokenResponse.java, ...
│   ├── profile/
│   │   └── ProfileController.java
│   ├── organization/
│   │   └── OrganizationController.java
│   ├── project/
│   │   ├── ProjectController.java
│   │   └── DashboardController.java
│   ├── requirement/
│   │   └── RequirementController.java
│   ├── task/
│   │   ├── TaskController.java
│   │   └── LabelController.java
│   ├── document/
│   │   └── DocumentController.java
│   ├── context/
│   │   └── ContextAssetController.java
│   ├── ai/
│   │   ├── AssistantController.java
│   │   ├── ConversationController.java
│   │   └── AiActionController.java
│   ├── advice/
│   │   └── GlobalExceptionHandler.java
│   └── openapi/
│       └── OpenApiConfig.java
│
├── application/                      # Use cases + ports
│   ├── auth/
│   │   ├── AuthService.java
│   │   ├── PasswordResetService.java
│   │   └── port/ TokenProviderPort.java, EmailPort.java
│   ├── project/
│   │   ├── ProjectService.java
│   │   ├── DashboardService.java
│   │   └── port/ ProjectRepositoryPort.java
│   ├── requirement/
│   │   ├── RequirementService.java
│   │   └── port/ RequirementRepositoryPort.java
│   ├── task/
│   │   ├── TaskService.java
│   │   └── LabelService.java
│   ├── document/
│   │   └── DocumentService.java
│   ├── security/
│   │   └── ProjectAuthorizationService.java
│   ├── ai/
│   │   ├── AiOrchestrationService.java
│   │   ├── ContextBuilder.java
│   │   ├── ConversationService.java
│   │   └── port/
│   │       ├── AiProviderPort.java
│   │       ├── PromptTemplatePort.java
│   │       └── ConversationRepositoryPort.java
│   └── audit/
│       └── AuditService.java
│
├── domain/
│   ├── user/
│   │   ├── User.java
│   │   └── Role.java (enums)
│   ├── organization/
│   │   ├── Organization.java
│   │   └── Membership.java
│   ├── project/
│   │   ├── Project.java
│   │   └── ProjectMember.java
│   ├── requirement/
│   │   └── Requirement.java
│   ├── task/
│   │   ├── Task.java
│   │   └── Label.java
│   ├── document/
│   │   └── Document.java
│   ├── ai/
│   │   ├── AssistantRole.java
│   │   ├── Conversation.java
│   │   ├── Message.java
│   │   └── ProjectContext.java
│   ├── context/
│   │   └── ContextAsset.java
│   └── common/
│       ├── Priority.java
│       └── DomainException.java
│
├── infrastructure/
│   ├── persistence/
│   │   ├── entity/          # JPA entities (may mirror domain or be separate)
│   │   ├── repository/      # Spring Data JPA
│   │   ├── adapter/         # Implements repository ports
│   │   └── mapper/          # MapStruct entity ↔ domain/DTO
│   ├── security/
│   │   ├── SecurityConfig.java
│   │   ├── JwtService.java
│   │   ├── JwtAuthFilter.java
│   │   └── UserDetailsServiceImpl.java
│   ├── ai/
│   │   ├── MockAiProvider.java
│   │   ├── OpenAiProvider.java
│   │   ├── AnthropicProvider.java
│   │   ├── AiProviderConfig.java
│   │   └── PromptTemplateManager.java
│   ├── mail/
│   │   ├── LoggingEmailAdapter.java
│   │   └── SmtpEmailAdapter.java
│   ├── ratelimit/
│   │   └── RateLimitFilter.java
│   └── config/
│       ├── CorsConfig.java
│       ├── JacksonConfig.java
│       └── AsyncConfig.java
│
└── shared/
    ├── api/ ApiError.java, PageResponse.java
    ├── logging/ RequestIdFilter.java
    └── util/ SlugUtils.java
```

---

## 3. Layer Responsibilities

| Layer | Does | Does not |
|---|---|---|
| **api** | HTTP mapping, validation annotations, auth annotations | Business rules, SQL |
| **application** | Orchestrate use cases, transactions, authz checks | Framework HTTP types |
| **domain** | Invariants, enums, pure domain logic | Spring annotations (prefer POJOs) |
| **infrastructure** | JPA, JWT, LLM SDKs, mail | Controllers calling repos directly |

---

## 4. Key Dependencies (`pom.xml` sketch)

```xml
<!-- Spring Boot starters -->
spring-boot-starter-web
spring-boot-starter-security
spring-boot-starter-data-jpa
spring-boot-starter-validation
spring-boot-starter-actuator
spring-boot-starter-mail

<!-- Persistence / migrations -->
postgresql
flyway-core
flyway-database-postgresql

<!-- Mapping / boilerplate -->
org.mapstruct:mapstruct
org.projectlombok:lombok
lombok-mapstruct-binding

<!-- API docs -->
org.springdoc:springdoc-openapi-starter-webmvc-ui

<!-- JWT -->
io.jsonwebtoken:jjwt-api / impl / jackson

<!-- Tests -->
spring-boot-starter-test
spring-security-test
testcontainers (postgresql)
```

---

## 5. Configuration Sketch

```yaml
# application.yml
spring:
  application:
    name: ai-studio-api
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:aistudio}
    username: ${DB_USER:aistudio}
    password: ${DB_PASSWORD}
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true

aistudio:
  security:
    jwt:
      access-token-ttl: 15m
      refresh-token-ttl: 7d
      secret: ${JWT_SECRET}
  cors:
    allowed-origins: ${CORS_ORIGINS:http://localhost:5173}
  ai:
    provider: ${AI_PROVIDER:mock}   # mock | openai | anthropic
    openai:
      api-key: ${OPENAI_API_KEY:}
      model: ${OPENAI_MODEL:gpt-4o-mini}
    anthropic:
      api-key: ${ANTHROPIC_API_KEY:}
      model: ${ANTHROPIC_MODEL:claude-sonnet-4-20250514}
    context:
      max-requirements: 50
      max-tasks: 100
      max-messages: 20
      max-chars: 48000
  rate-limit:
    ai-per-minute: 30
```

---

## 6. Testing Layout

```
src/test/java/com/aistudio/
├── application/
│   ├── auth/AuthServiceTest.java
│   ├── project/ProjectServiceTest.java
│   └── ai/ContextBuilderTest.java
├── api/
│   ├── auth/AuthControllerIT.java
│   └── project/ProjectControllerIT.java
├── infrastructure/ai/MockAiProviderTest.java
└── support/
    ├── TestSecurityConfig.java
    └── PostgresTestcontainer.java
```

Unit tests mock ports. Integration tests use `@SpringBootTest` + Testcontainers.

---

## 7. Coding Patterns (Backend)

1. One controller per resource aggregate.
2. Services are `@Transactional` at use-case boundaries.
3. MapStruct mappers are interfaces; no manual DTO copy-paste.
4. Throw domain/application exceptions; map in `GlobalExceptionHandler`.
5. Never pass entities outside application without mapping for API responses.
6. AI calls isolated behind `AiProviderPort`.

---

## 8. Build & Run

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
./mvnw test
./mvnw -DskipTests package
docker build -t aistudio-api .
```

---

## 9. Document Control

| Version | Date | Notes |
|---|---|---|
| 1.0 | 2026-08-06 | Clean Architecture package map |

**Previous:** `04-API-SPECIFICATION.md` · **Next:** `06-FRONTEND-STRUCTURE.md`

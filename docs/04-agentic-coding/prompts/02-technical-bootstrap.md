# Technical Bootstrap – Increment 1

**Status:** Reviewed – Accepted

## Objective

Establish the approved minimal Java 21 / Spring Boot 4.1.1 Maven application and verify its application context and managed test dependencies.

## Context

Follow [AGENTS.md](../../../AGENTS.md), [system architecture](../../02-architecture/system-architecture.md), [technology reference](../../02-architecture/technology-stack.md), [coding guidelines](../coding-guidelines.md), [testing strategy](../../05-testing/testing-strategy.md), and the [implementation plan](../../07-implementation/implementation-plan.md). ADR-0002 remains Proposed and unverified.

## Approved Scope

One JAR Maven module with coordinates `com.example:agentic-order-service`, base package `com.example.agenticorderservice`, Web MVC, Validation, a Boot entry point, minimal application configuration, and one context smoke test. Use Boot-managed JUnit 6 and command-local Java 21. Database, transaction, container, and business implementation are excluded.

## Prompt

The following complete user specification is an immutable historical record. Execution notes below may be updated; the prompt block must not change.

```text
# Task: Document and Execute Technical Bootstrap – Increment 1

We are starting Technical Bootstrap – Increment 1 for the Agentic Order Service.

The implementation scope has been approved.

## 1. Document the Prompt Before Implementation

Create:

`docs/04-agentic-coding/prompts/02-technical-bootstrap.md`

Follow the established prompt-playbook format used in:

`docs/04-agentic-coding/prompts/01-environment-assessment.md`

Include:

- Status: Planned
- Objective
- Context
- Approved Scope
- Prompt
- Result
- Lessons Learned
- Interview Talking Points

Record the complete implementation prompt from the approved Technical Bootstrap – Increment 1 specification.

Preserve that prompt as an immutable historical record.

Update `docs/04-agentic-coding/prompts/00-index.md` with a link to the new document.

## 2. Read Authoritative Documentation

Read AGENTS.md and all relevant architecture, technology, coding, testing, and implementation documents before generating code.

Follow all Accepted decisions.

Do not silently resolve Open or Proposed decisions.

## 3. Implement Technical Bootstrap – Increment 1

Approved baseline:

- Java 21
- Spring Boot 4.1.1
- Maven, single module
- Group ID: com.example
- Artifact ID: agentic-order-service
- Base package: com.example.agenticorderservice
- Packaging: JAR
- Spring Web MVC
- Spring Boot Validation
- JUnit 6, using Spring Boot-managed versions

Implementation scope:

1. Create the Maven pom.xml.
2. Create the Spring Boot application entry point.
3. Create minimal application.yml configuration.
4. Add a minimal application-context smoke test.
5. Verify Java 21 compilation and dependency resolution.

Use the existing Java 21 installation. Do not modify the system-wide Java or Maven configuration.

Do not implement:

- Domain entities or repositories
- Controllers or REST endpoints
- Business services
- TransactionTemplate or AmbientTransactionGuard
- Database migrations
- Docker Compose configuration
- PostgreSQL or Testcontainers integration

These belong to later separately approved increments.

## 4. Verification

Run the Maven build and applicable tests with Java 21.

Inspect the resolved dependency tree, including JUnit 6 alignment.

Run `git diff --check`.

Do not modify unrelated files.

## 5. Update the Prompt Playbook

After execution, update:

`docs/04-agentic-coding/prompts/02-technical-bootstrap.md`

Set its status to:

**Executed – Pending Human Review**

Fill in:

- Actual files created or changed
- Build and test results
- Resolved dependency versions
- Problems encountered
- Lessons learned
- Interview talking points

If execution is blocked, mark the document as Blocked and explain the reason.

Do not change the original recorded prompt.

## 6. Completion

Present:

- Implementation summary
- Actual verification results
- Git diff
- Remaining open issues

Do not create a Git commit.

Do not proceed to Increment 2 without explicit human approval.
```

## Result

Executed on 2026-10-13. The implementation passed human code review and was explicitly accepted by the user. No code corrections were required. The reviewed implementation files were `pom.xml`, `AgenticOrderServiceApplication.java`, `application.yml`, and `AgenticOrderServiceApplicationTest.java`. No commit has been authorized or created; Increment 2 has not been started.

### Files Created or Changed

- Created `pom.xml` with the Boot 4.1.1 parent, Java 21 release, Web MVC, Validation, test starter, and executable JAR packaging.
- Created `src/main/java/com/example/agenticorderservice/AgenticOrderServiceApplication.java`.
- Created `src/main/resources/application.yml` with the application name.
- Created `src/test/java/com/example/agenticorderservice/AgenticOrderServiceApplicationTest.java`.
- Created this prompt record before implementation and updated [the index](00-index.md).

Existing README, AI workflow, and environment-assessment edits were preserved. No system-wide Java or Maven configuration was changed.

### Verification Evidence

Commands used the existing Java 21 installation through a command-local environment variable:

```sh
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home /opt/homebrew/bin/mvn -B -ntp verify
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home /opt/homebrew/bin/mvn -B -ntp dependency:tree
git diff --check
git diff --cached --check
```

- Maven `verify`: **BUILD SUCCESS**; one test, zero failures/errors/skips. The mock web application context includes a DispatcherServlet and Jakarta Validation provider. This is not an HTTP endpoint or live-server test.
- Compilation uses `release 21`; the entry-point class has bytecode major version 65. Runtime Java: 21.0.6; Maven: 3.9.9.
- Executable JAR produced at `target/agentic-order-service-0.0.1-SNAPSHOT.jar`; build outputs remain ignored.
- Dependency tree: **BUILD SUCCESS**. Jupiter API/params/engine and Platform commons/engine all resolve to 6.0.3, without JUnit version overrides or JUnit 5 artifacts.
- Local Markdown file links resolve. Staged/unstaged and new-file whitespace checks passed; the immutable prompt block was checked against the recorded specification.
- Persistence, OSIV/JPA configuration, transaction guards, database startup, migration, HTTP business contracts, and concurrency gates remain **Pending** for separately approved increments. ADR-0002 remains Proposed and implementation-unverified.

### Resolved Dependency Versions

These are observed Boot-managed versions, not new independent technology decisions.

| Dependency | Resolved version |
| --- | --- |
| Spring Boot parent, starters, runtime/test modules, packaging plugin | 4.1.1 |
| Spring Framework modules | 7.0.9 |
| JUnit Jupiter and Platform modules | 6.0.3 |
| Jackson databind/core | 3.1.5 |
| Jackson annotations | 2.21 |
| Embedded Tomcat | 11.0.24 |
| Hibernate Validator | 9.1.3.Final |
| Jakarta Validation API | 3.1.1 |
| AssertJ | 3.27.7 |
| Mockito | 5.23.0 |
| Maven Compiler / Surefire plugins | 3.15.0 / 3.5.6 |

### Problems and Remaining Issues

- Initial sandbox build could not write to the Maven cache. Repeating the build with permission resolved this; no dependency or Java baseline was changed.
- The Boot test stack emitted Mockito self-attachment and JVM dynamic-agent warnings. The test passed. Future test-agent configuration needs a separately reviewed change if required; no configuration was added to suppress the warnings.
- The general implementation plan describes a broader skeleton. This explicitly approved increment intentionally stops before its PostgreSQL, OSIV, and transaction infrastructure work; those verification gates remain Pending.
- Open persistence/schema, migration, email syntax/counting, and retry/locking configuration decisions are unchanged and do not block this narrow increment. Human review passed and explicit acceptance was recorded; commit authorization remains pending.

## Lessons Learned

- A command-local JAVA_HOME avoids the existing Maven Java 25 default without changing system configuration.
- Inspecting the resolved graph confirms Boot-managed JUnit 6 alignment; compilation and a context test alone would not establish that alignment.
- A minimal context smoke test verifies MVC and Validation wiring while keeping database and business work outside this increment.

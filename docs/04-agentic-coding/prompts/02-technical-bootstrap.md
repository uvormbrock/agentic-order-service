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

## Post-Review Enhancement – Maven Wrapper

**Enhancement status:** Reviewed – Accepted. The user explicitly accepted the Maven Wrapper enhancement after human review, subject to the final line-ending configuration verified below.

### Rationale and Approved Decision

The user explicitly approved adding the official Apache Maven Wrapper pinned to Maven **3.9.9**, so builds use the same Maven version without changing the system installation. Wrapper scripts were generated by the official plugin release 3.3.4 using the existing Maven installation. The `only-script` distribution avoids committing wrapper binaries. The original implementation prompt above remains immutable and its verification evidence is preserved.

### Generated and Changed Files

- Generated `mvnw` (executable Unix launcher), `mvnw.cmd` (Windows launcher), and `.mvn/wrapper/maven-wrapper.properties`.
- Updated this execution record and the [technology reference](../../02-architecture/technology-stack.md).
- No POM, application source, YAML configuration, test source, or ADR history was changed.

Configuration: `wrapperVersion=3.3.4`, `distributionType=only-script`, and `distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip`. The configured URL is HTTPS on the official Maven Central host and has the exact Maven 3.9.9 artifact path. No repository override was present during verification; no custom repository was added.

### Verification Commands and Results

```sh
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home /opt/homebrew/bin/mvn -B -ntp org.apache.maven.plugins:maven-wrapper-plugin:3.3.4:wrapper -Dmaven=3.9.9 -Dtype=only-script
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home ./mvnw --version
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home ./mvnw -B -ntp verify dependency:tree
git diff --check
git -c core.whitespace=blank-at-eol,blank-at-eof,space-before-tab,cr-at-eol diff --cached --check
```

- Generation: **BUILD SUCCESS**, exactly three generated text files; no wrapper JAR, downloaded Maven archive, or other executable binary was added to the repository.
- Wrapper version command: Apache Maven **3.9.9**, Oracle Java **21.0.6**, runtime at the existing JDK 21 installation. The downloaded Maven resides in the local Maven wrapper cache, outside the repository.
- Wrapper build: **BUILD SUCCESS**, executable JAR packaging completed; one context smoke test passed with zero failures/errors/skips.
- Dependency inspection: Spring Boot **4.1.1**, Spring Framework **7.0.9**, JUnit Jupiter/Platform **6.0.3**, Jackson core/databind **3.1.5**, and Hibernate Validator **9.1.3.Final** remain unchanged. No dependencies or version overrides were added to the POM.
- Final Git checks cover tracked and newly generated files; the Unix launcher is tracked with executable mode. Markdown file links and original prompt preservation were checked.
- Windows launcher execution remains **Pending** because verification ran on macOS; the generated Windows script was inspected, not executed.

### Problems Encountered

The successful smoke test emitted the previously recorded Mockito dynamic-agent warnings. No new build failures occurred and no agent configuration was introduced. The default staged whitespace check flags the officially generated Windows CRLF line endings; a command-local `cr-at-eol` check passes while preserving the generated script unchanged. No repository or global Git setting was changed. Commands required permitted access to the local Maven cache. System-wide JAVA_HOME and Maven installations were unchanged.

### Lessons Learned

- Pinning the Maven distribution through generated wrapper configuration makes the build-tool version explicit while preserving the installed Maven.
- Wrapper release 3.3.4 and downloaded Maven version 3.9.9 are distinct versions.
- Script-only generation keeps launcher/configuration text in Git while Maven binaries remain in the local cache.

### Remaining Gates

Human review of this enhancement passed and explicit acceptance was recorded. Commit authorization remains pending. Database, transaction, and concurrency gates remain Pending; ADR-0002 remains Proposed. Increment 2 has not started.

### Final Git Line-Ending Decision and Verification

The user explicitly approved a minimal root `.gitattributes` file:

```gitattributes
/mvnw text eol=lf
/mvnw.cmd text eol=crlf
```

The generated scripts retain their original working-tree bytes and functional content. Git stores normalized LF text for both scripts; checkout uses LF for `mvnw` and CRLF for `mvnw.cmd`. Explicit `git add --renormalize -- mvnw mvnw.cmd` was required: ordinary staging initially retained the existing CRLF index blob, causing the standard whitespace check to flag carriage returns. After renormalization, standard `git diff --cached --check` and `git diff --check` pass without a whitespace override. This supersedes the earlier temporary check workaround while preserving its historical evidence above.

Final verification on 2026-10-13:

```sh
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home ./mvnw -B -ntp verify
git ls-files --eol mvnw mvnw.cmd
git ls-files --stage mvnw mvnw.cmd
git diff --cached --check
git diff --check
```

- Build: **BUILD SUCCESS** on Java 21.0.6; one smoke test passed, zero failures/errors/skips. Existing Mockito warnings persist.
- Line endings: `mvnw` reports `i/lf w/lf attr/text eol=lf`; `mvnw.cmd` reports `i/lf w/crlf attr/text eol=crlf`.
- Git executable modes: `mvnw` is `100755`; `mvnw.cmd` is `100644`.
- Original prompt and prior execution evidence preserved. No application logic, dependencies, or ADR decisions changed. No commit created; Increment 2 not started.

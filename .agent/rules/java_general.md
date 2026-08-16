---
alwaysApply: false
globs: "**/*.java"
---

# Java General Rules

## Development Workflow

1. When the user explicitly requests Java commands, run Maven and Javac operations serially.
2. After validation is authorized, start with the smallest focused test and expand only as required by risk and the user's request.
3. Do not mix `target/` artifacts, running processes, configuration, or logs between separate checkouts.
4. Do not edit `target/` or generated class/JAR files manually, and do not treat stale build artifacts as evidence for the current source tree.

## Coding Conventions

1. Follow the existing package structure. Do not introduce a second application container, build system, or parallel architecture.
2. Prefer clear immutable designs: declare dependencies and non-changing fields as `final`, and retain mutable state only when it has explicit state-transition semantics.
3. Use Java 25 features such as records, pattern matching, switch expressions, and try-with-resources when they improve clarity. Do not expand the task merely to adopt newer syntax.
4. Make parameter, return-value, collection-mutability, and null semantics explicit. Fail fast on invalid input at public boundaries instead of relying on a later `NullPointerException` to express a business error.
5. Follow existing naming, package boundaries, and formatting. Do not add duplicate `Utils` or `Manager` classes, hidden global state, or shared helpers without clear ownership.
6. Apply `lombok.md` to beans, DTOs, and data carriers. Apply `i18n.md` to comments, logging, and terminology; do not duplicate those rules here.

## Error Handling

1. Do not use empty `catch` blocks, silently return success defaults, discard exceptions, or log only an exception message without preserving the cause.
2. Catch an exception only when the code can recover, translate it into a clear domain error, add necessary context, or perform boundary cleanup. Otherwise, let it propagate.
3. Preserve the original cause when wrapping an exception and add actionable business context. Do not hide the failure type or origin behind a generic `RuntimeException`.
4. Log a failure once at the boundary with the most context. Avoid emitting the same stack trace at every layer when rethrowing.
5. Use try-with-resources for managed resources. Put state restoration and required cleanup in `finally` blocks or container-managed lifecycle callbacks, and do not let cleanup failures mask the primary exception.
6. When catching `InterruptedException`, restore the thread's interrupt status and stop or propagate. Do not catch `Throwable` or swallow `Error` outside an explicit process-level failure boundary.
7. A transaction failure must roll back atomically. Do not publish in-memory success state before the database commit, and keep external side effects within the existing transaction and after-commit boundaries.

## Dependency Injection

1. Prefer Spring-managed services and lifecycle components using `@Component`, `@Service`, `@Configuration`, or the existing stereotype that matches the role.
2. Use constructor injection by default and declare dependencies as `final`. A single constructor does not need `@Autowired`; constructor boilerplate follows `lombok.md`.
3. Do not add field injection, setter injection, runtime lookups from a global application context, or static singleton/service-locator access that hides dependencies.
4. Use `ObjectProvider<T>` or an existing explicit abstraction for optional or lazy dependencies instead of using null to represent missing injection.
5. A Spring-managed provider bridge is allowed only when a legacy static call site cannot yet be migrated. The bridge must define lifecycle cleanup, thread visibility, and test reset behavior, and it must not become the default entry point for new code.
6. Tests should construct the subject directly and provide fakes, stubs, or mocks. Do not relax production constructor or field visibility solely for tests.
7. Do not instantiate injectable services directly inside business methods. Value objects, short-lived domain objects, and objects with explicit factory ownership are exceptions.

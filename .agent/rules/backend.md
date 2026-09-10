---
alwaysApply: false
globs: "src/main/java/**/*.java"
---

# Backend Development Rules / 后端开发规则

## JDK 25 Baseline / JDK 25 基线

1. Backend production code targets JDK 25 as its language and runtime baseline. For new or touched code, prefer the stable JDK 25 syntax and standard APIs whenever they improve clarity, correctness, or resource safety.
2. Prefer `record` and record patterns for immutable DTOs, commands, events, and value objects; use sealed classes or interfaces for genuinely closed domain variants; use pattern matching for `instanceof` and `switch`, switch expressions, text blocks, and other modern syntax when the surrounding code remains easier to understand.
3. Use JDK 25 APIs such as `ScopedValue` only when their lifecycle and thread-boundary semantics fit the existing Spring/Netty design. Do not use them as hidden mutable global state or introduce a new context mechanism without a concrete ownership and cleanup boundary.
4. Do not enable preview features, incubating APIs, or `--enable-preview` merely to use newer syntax. A preview feature requires explicit task scope, a documented compatibility boundary, and focused validation; primitive patterns and other preview-only features are not the default production style.
5. Do not use compact source files or instance `main` methods for Spring components, services, handlers, entities, or other production classes whose explicit type, constructor, annotations, reflection, proxying, or serialization contract matters.
6. Keep records, sealed hierarchies, pattern matching, and flexible constructor bodies behavior-preserving: do not apply them to mutable ORM entities, proxy-sensitive framework types, inheritance-based extension points, or serialization contracts that require a normal class unless the compatibility is verified.
7. Apply modern JDK 25 syntax to the requested backend scope only. Do not perform repository-wide modernization or rewrite stable legacy code solely for style; preserve public protocols, database mappings, thread ownership, transaction boundaries, localized logging, and existing Spring/Netty lifecycle behavior.


---
alwaysApply: false
globs: "**/*.java"
---

# Lombok Rules

## Beans and Data Objects

1. When adding or modifying Java beans, DTOs, configuration objects, commands, events, or other data carriers, prefer Lombok over handwritten getters, setters, constructors, `equals`, `hashCode`, and `toString` methods that contain no business logic.
2. Use `@Data` only when every field may safely participate in getters, setters, `equals`, `hashCode`, and `toString`. When only some generated behavior is appropriate, use targeted annotations such as `@Getter`, `@Setter`, `@EqualsAndHashCode`, or `@ToString` instead of broadening generation for convenience.
3. When construction only assigns fields, prefer `@NoArgsConstructor`, `@RequiredArgsConstructor`, or `@AllArgsConstructor`. Use `@Builder` when it provides clearer creation semantics.
4. Prefer `@Value` or a Java record for immutable data objects. Do not use Lombok to regenerate behavior already provided by a record.

## Usage Boundaries

1. Do not apply `@Data` directly to objects with entity relationships, lazy loading, cyclic references, sensitive fields, or identity based on mutable fields. Use targeted annotations and control generated behavior with `@EqualsAndHashCode.Exclude`, `@ToString.Exclude`, or explicit implementations.
2. Keep explicit getters, setters, or constructors when they perform validation, conversion, caching, event publication, synchronization, lazy initialization, or other side effects. Lombok must not change existing semantics.
3. Before removing handwritten boilerplate, verify that signatures, visibility, annotations, serialization contracts, reflective access, and framework construction requirements exactly match Lombok's generated behavior.

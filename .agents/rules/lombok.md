---
alwaysApply: false
globs: "**/*.java"
---

# Lombok Rules

## Beans and Data Objects

1. When adding or modifying Java beans, DTOs, configuration objects, commands, events, or other data carriers, prefer Lombok over handwritten getters, setters, constructors, `equals`, `hashCode`, and `toString` methods that contain no business logic.
2. Put accessor annotations on the **type** (`@Getter` / `@Setter` / `@Data` on the class), not on individual fields. Use a field level annotation only when that one field genuinely needs different behavior from its siblings, and say why in a short comment.
3. Use `@Data` only when every field may safely participate in getters, setters, `equals`, `hashCode`, and `toString`. When only some generated behavior is appropriate, use targeted annotations such as `@Getter`, `@Setter`, `@EqualsAndHashCode`, or `@ToString` instead of broadening generation for convenience.
4. **Decide `@Data` per class by asking these questions — if any answer is "yes", do not use `@Data`; use `@Getter` plus targeted annotations instead:**
   1. Does any field change after the object is created (counts, levels, colors, state, position, timers)?
   2. Is any instance used as a key or an element of a `HashMap` / `HashSet` / `WeakHashMap` / `ConcurrentHashMap`, or passed to `contains` / `remove` / `indexOf`?
   3. Can the object graph reach back to this object (parent/owner back references, observer collections, instance registries)?
   4. Is the class created by a framework via a specific constructor or factory (JAXB templates, DAO-loaded entities, Spring beans)?
   5. Does any field hold a secret (`password`, `token`, `credential`) that must not reach `toString`?
   Lombok's `@Data` generates field-based `equals` / `hashCode` and a recursive `toString`. On mutable entities that breaks identity based lookups silently, and on cyclic graphs it can overflow the stack. These classes also usually already carry explicit constructors and invariants that `@RequiredArgsConstructor` would bypass.
5. Class-level `@Getter` does not change equality, hashing, or construction, but can still change framework property discovery. For JAXB method-bound attributes, check getter/setter type compatibility: a generated numeric getter can prevent an annotated String setter from binding. Suppress generation for that field when necessary, and verify actual XML deserialization rather than only checking compilation.
6. When construction only assigns fields, prefer `@NoArgsConstructor`, `@RequiredArgsConstructor`, or `@AllArgsConstructor`. Use `@Builder` when it provides clearer creation semantics.
7. Prefer `@Value` or a Java record for immutable data objects. Do not use Lombok to regenerate behavior already provided by a record.

## Usage Boundaries

1. Do not apply `@Data` directly to objects with entity relationships, lazy loading, cyclic references, sensitive fields, or identity based on mutable fields. Use targeted annotations and control generated behavior with `@EqualsAndHashCode.Exclude`, `@ToString.Exclude`, or explicit implementations.
2. Keep explicit getters, setters, or constructors when they perform validation, conversion, caching, event publication, synchronization, lazy initialization, or other side effects. Lombok must not change existing semantics.
3. Before removing handwritten boilerplate, verify that signatures, visibility, annotations, serialization contracts, reflective access, and framework construction requirements exactly match Lombok's generated behavior.
4. Guard tests cannot detect `@Data` by reflection: Lombok annotations use `CLASS` retention, so they never appear in `RuntimeVisibleAnnotations` and `isAnnotationPresent` returns `false`. Inspect source text instead.
5. Be aware of Lombok method collision rules: if a method with the same name and parameter count already exists, Lombok silently skips generation (e.g. overloaded setters such as `setState(int)` vs `setState(CreatureState)`). Do not delete one overload expecting Lombok to regenerate it. See [.agents/memory-bank/patterns/build-and-env.md](../memory-bank/patterns/build-and-env.md).

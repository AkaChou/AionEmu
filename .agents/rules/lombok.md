---
alwaysApply: false
globs: "**/*.java"
---

# Lombok 使用规则 (Lombok Rules)

## Bean 与数据对象 (Beans and Data Objects)

1. 在新增或修改 Java Bean、DTO、配置对象、命令（commands）、事件（events）或其他数据载体时，相比不含业务逻辑的手写 getter、setter、构造器、`equals`、`hashCode` 和 `toString` 方法，优先使用 Lombok。
2. 访问器注解应置于**类型级别**（在类上标注 `@Getter` / `@Setter` / `@Data`），而非各个字段上。仅当某个特定字段确实需要与其同类字段不同的行为时，才使用字段级注解，并在简短注释中说明原因。
3. 仅当所有字段都能安全参与 getter、setter、`equals`、`hashCode` 和 `toString` 时，才使用 `@Data`。若只有部分生成行为合适，应使用针对性的注解（如 `@Getter`、`@Setter`、`@EqualsAndHashCode` 或 `@ToString`），切勿为了图省事而泛用 `@Data`。
4. **通过以下问题逐类评估是否可以使用 `@Data`——若任何一项答案为“是”，严禁使用 `@Data`；应改用 `@Getter` 搭配针对性注解：**
   1. 对象创建后是否有任何字段会发生变化（计数器、等级、颜色、状态、坐标位置、定时器等）？
   2. 该类的实例是否会被用作 `HashMap` / `HashSet` / `WeakHashMap` / `ConcurrentHashMap` 的 key 或元素，或者被传入 `contains` / `remove` / `indexOf`？
   3. 对象引用图是否存在回溯引用（父级/拥有者反向引用、观察者集合、实例注册表）？
   4. 该类是否由框架通过特定构造器或工厂创建（JAXB 模板、DAO 加载的实体、Spring Bean）？
   5. 是否有字段存储敏感信息（`password`、`token`、`credential` 等）且绝不能输出至 `toString`？
   Lombok 的 `@Data` 会基于字段生成 `equals` / `hashCode` 以及递归的 `toString`。在可变实体上这会静默破坏基于标识（identity）的查找；在循环引用图中会导致栈溢出（StackOverflowError）。此外，这类对象通常已具备显式构造器和不变量约束，而 `@RequiredArgsConstructor` 会绕过这些约束。
5. 类级别的 `@Getter` 不会改变等价性、哈希或构造行为，但仍可能改变框架对属性的内省发现机制。对于 JAXB 方法绑定的属性，需检查 getter/setter 的类型兼容性：生成的数值类型 getter 可能会导致标注的 String setter 无法绑定。必要时需抑制该字段的生成，并验证实际的 XML 反序列化结果，而非仅仅检查能否编译通过。
6. 当构造过程仅为字段赋值时，优先使用 `@NoArgsConstructor`、`@RequiredArgsConstructor` 或 `@AllArgsConstructor`。当构建逻辑需要更清晰的创建语义时，使用 `@Builder`。
7. 对于不可变数据对象，优先使用 `@Value` 或 Java record。切勿使用 Lombok 重复生成 record 已经具备的行为。

## 使用边界 (Usage Boundaries)

1. 切勿将 `@Data` 直接应用于具有实体关联、延迟加载（lazy loading）、循环引用、敏感字段或基于可变字段确定对象标识的对象。应使用针对性注解，并通过 `@EqualsAndHashCode.Exclude`、`@ToString.Exclude` 或显式手写实现来控制生成行为。
2. 当 getter、setter 或构造器包含校验、类型转换、缓存、事件发布、同步控制、延迟初始化或其他副作用时，必须保留显式实现。Lombok 绝不能改变既有语义。
3. 在移除手写样板代码前，必须验证方法签名、可见性、注解、序列化契约、反射访问以及框架构造要求是否与 Lombok 的生成行为完全吻合。
4. 守护测试（guard tests）无法通过反射检测到 `@Data`：Lombok 注解使用 `CLASS` 保留策略，因此绝不会出现在 `RuntimeVisibleAnnotations` 中，`isAnnotationPresent` 会返回 `false`。必须通过源码文本进行检查。
5. 留意 Lombok 的方法冲突规则：如果已存在同名且参数数量相同的方法，Lombok 会静默跳过生成（例如重载的 setter，如 `setState(int)` 与 `setState(CreatureState)`）。切勿指望删除其中一个重载后 Lombok 会重新生成它。参见 [.agents/memory-bank/patterns/build-and-env.md](../memory-bank/patterns/build-and-env.md)。

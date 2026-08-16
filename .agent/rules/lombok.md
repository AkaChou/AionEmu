---
alwaysApply: false
globs: "**/*.java"
---

# Lombok 使用规则

## Bean 与数据对象

1. 新增或修改 Java Bean、DTO、配置对象、命令、事件及其他数据载体时，优先使用 Lombok 消除无业务逻辑的 getter、setter、构造器、`equals`、`hashCode` 和 `toString` 样板代码。
2. 所有字段都适合参与 getter、setter、`equals`、`hashCode` 和 `toString` 时使用 `@Data`；只需要部分能力时使用 `@Getter`、`@Setter`、`@EqualsAndHashCode` 或 `@ToString`，不要为方便而扩大生成范围。
3. 构造逻辑仅为字段赋值时，优先使用 `@NoArgsConstructor`、`@RequiredArgsConstructor` 或 `@AllArgsConstructor`；需要清晰创建语义时可使用 `@Builder`。
4. 不可变数据对象优先使用 `@Value` 或 Java record；record 已提供的能力不要再用 Lombok 重复生成。

## 使用边界

1. 实体关系、延迟加载对象、循环引用、敏感字段或以可变字段标识身份的对象，不得直接使用 `@Data`；应改用定向注解，并通过 `@EqualsAndHashCode.Exclude`、`@ToString.Exclude` 或显式实现控制行为。
2. getter、setter 或构造器包含校验、转换、缓存、事件、同步、延迟初始化或其他副作用时保留显式实现，不得用 Lombok 改变现有语义。
3. 删除手写样板代码前，确认方法签名、可见性、注解、序列化约定、反射访问和框架构造要求与 Lombok 生成结果完全一致。

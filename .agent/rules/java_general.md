---
alwaysApply: false
globs: "**/*.java"
---

# Java General Rules

## Development Workflow

1. 用户明确要求运行 Java 命令时，保持 Maven/Javac 串行。
2. 获得验证授权后先运行最小范围的 focused test，再根据风险和用户要求扩大范围。
3. 不要混用不同 checkout 的 `target`、运行进程、配置或日志。
4. 不手工修改 `target/` 或生成的 class/JAR，也不把旧构建产物作为当前源码的验证证据。

## Coding Conventions

1. 遵循现有包结构，不引入第二套应用容器、构建体系或并行架构。
2. 优先使用清晰的不可变设计：依赖和无需变化的字段声明为 `final`，只在确有状态变化语义时保留可变字段。
3. 合理使用 Java 25 已支持的 record、模式匹配、switch 表达式和 try-with-resources；不得仅为使用新语法而扩大改动范围。
4. 参数、返回值、集合可变性和 null 语义必须明确。公共边界对非法输入 fail fast，禁止依赖后续 `NullPointerException` 表达业务错误。
5. 遵循现有命名、包边界和格式，不创建功能重复的 `Utils`、`Manager`、静态全局状态或无明确所有权的公共 helper。
6. Bean、DTO 和数据载体适用 `lombok.md`；注释、日志和术语适用 `i18n.md`，具体约束不在本文件重复定义。

## Error Handling

1. 禁止空 `catch`、静默返回默认成功、丢弃异常或只记录消息而不保留 cause。
2. 只在能够恢复、转换为明确领域错误、补充必要上下文或完成边界清理的位置捕获异常；否则让异常沿调用链传播。
3. 包装异常时保留原始 cause，并提供可定位的业务上下文；不得用笼统的 `RuntimeException` 隐藏失败类型和来源。
4. 同一失败通常只在最有上下文的边界记录一次；记录后继续抛出时避免在每一层重复输出同一堆栈。
5. 资源使用 try-with-resources，状态复原和必要清理放入 `finally` 或受容器管理的生命周期回调；清理失败不得覆盖主要异常。
6. 捕获 `InterruptedException` 时恢复线程中断标志并终止或向上传播；除明确的进程级故障边界外，不捕获 `Throwable` 或吞掉 `Error`。
7. 事务内失败必须整体回滚；数据库提交前不得发布仅内存可见的成功状态，外部副作用应遵守现有事务和 after-commit 边界。

## Dependency Injection

1. 新增服务和生命周期组件优先交由 Spring 管理，使用 `@Component`、`@Service`、`@Configuration` 或语义匹配的现有 stereotype。
2. 默认使用构造器注入并将依赖声明为 `final`；单构造器无需添加 `@Autowired`，构造器样板代码适用 `lombok.md`。
3. 禁止新增字段注入、setter 注入、运行时从全局容器取 Bean，或通过静态 singleton/service locator 隐藏依赖。
4. 可选或延迟依赖使用 `ObjectProvider<T>` 或项目已有的明确抽象，不以 null 表示注入缺失。
5. 只有遗留静态调用点确实无法一次迁移时，才允许 Spring 管理的 provider bridge；bridge 必须有清晰的生命周期清理、线程可见性和测试重置机制，不得成为新代码的默认入口。
6. 测试优先直接构造被测对象并传入 fake、stub 或 mock；不得为了测试而放宽生产构造器和字段可见性。
7. 不在业务方法中直接 `new` 可注入服务；值对象、短生命周期领域对象和明确由工厂拥有的对象除外。

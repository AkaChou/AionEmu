---
alwaysApply: false
globs: "**/*.java"
---

# Java 通用规则 (Java General Rules)

## 开发工作流 (Development Workflow)

1. 当用户明确要求执行 Java 命令时，串行运行 Maven 和 Javac 操作。
2. 获得验证授权后，先从范围最小的专项测试开始，仅在风险需要及用户要求时再逐步扩大验证范围。
3. 切勿在不同的代码检出分支（checkout）之间混用 `../../target` 构建产物、运行中进程、配置或日志。
4. 切勿手动直接编辑 `../../target` 目录或生成的 class/JAR 文件，不可将过期的构建产物作为当前源码树状态的依据。
5. 在排查运行时差异、陈旧行为或环境异常时，请参考 [.agents/memory-bank/patterns/build-and-env.md](../memory-bank/patterns/build-and-env.md)（特别是 IDEA 中 target/classes 的 mtime 检查）。
6. 在高频门面（facade）和热点路径上，避免使用裸露的动态 bean 查找或 `applicationContext.getIfAvailable(...)`；请参考 [.agents/memory-bank/patterns/architecture-runtime.md](../memory-bank/patterns/architecture-runtime.md)。

## 编码规范 (Coding Conventions)

1. 遵循现有的包结构。切勿引入第二个应用容器、构建系统或并行架构。
2. 优先采用清晰的不可变设计：将依赖项和不变字段声明为 `final`，仅在具有明确状态流转语义时保留可变状态。
3. 在有助于提高清晰度时，使用 Java 25 特性（例如 record、模式匹配、switch 表达式以及 try-with-resources）。切勿仅为了采用新语法而扩大任务范围。
4. 明确参数、返回值、集合可变性以及 null 语义。在公共边界处对非法输入及早失败（fail-fast），而不是依赖后续的 `NullPointerException` 来表达业务错误。
5. 遵循既有的命名风格、包边界及 [`formatting.md`](formatting.md)。切勿添加重复的 `Utils` 或 `Manager` 类、隐式全局状态或缺乏清晰归属的共享辅助类。
6. 对 Bean、DTO 和数据载体应用 [`lombok.md`](lombok.md)。对注释、日志和术语应用 [`i18n.md`](i18n.md)；此处不重复这些规则。

## 错误处理 (Error Handling)

1. 严禁使用空的 `catch` 块、静默返回表示成功的默认值、丢弃异常，或仅记录异常消息而不保留原因链（cause）。
2. 仅在代码能够恢复、能将其转换为清晰的领域错误、能补充必要上下文或执行边界清理时才捕获异常。否则应任其向外传播。
3. 包装异常时务必保留原始原因（cause），并补充可操作的业务上下文。切勿用通用的 `RuntimeException` 掩盖失败类型或根因。
4. 在拥有最充分上下文的边界处记录一次失败日志。重新抛出时避免在每一层重复输出相同的堆栈轨迹。
5. 托管资源使用 try-with-resources 管理。将状态恢复和必要清理置于 `finally` 块或容器管理的生命周期回调中，绝不能让清理过程中的失败掩盖主异常。
6. 捕获 `InterruptedException` 时，恢复线程的中断状态并停止执行或向上抛出。在显式的进程级故障边界之外，切勿捕获 `Throwable` 或吞掉 `Error`。
7. 事务失败必须原子回滚。在数据库提交前不得发布内存中的成功状态，并将外部副作用保持在既有的事务与提交后（after-commit）边界内。

## 依赖注入 (Dependency Injection)

1. 优先使用由 Spring 管理的服务和生命周期组件，采用 `@Component`、`@Service`、`@Configuration` 或与职责匹配的既有构造型注解（stereotype）。
2. 默认使用构造器注入，并将依赖字段声明为 `final`。单一构造器无需标注 `@Autowired`；构造器样板代码遵循 `lombok.md`。
3. 严禁使用字段注入、setter 注入、从全局 ApplicationContext 运行时查找，或隐藏依赖关系的静态单例/服务定位器（service-locator）访问方式。
4. 对于可选或延迟依赖项，使用 `ObjectProvider<T>` 或既有的显式抽象，而非使用 null 表示缺少注入。
5. 仅当遗留的静态调用点暂无法迁移时，才允许使用由 Spring 管理的 provider 桥接类。该桥接类必须定义生命周期清理、线程可见性以及测试重置行为，且绝不能成为新代码的默认入口点。
6. 测试应直接构造被测对象并提供假对象（fakes）、存根（stubs）或 mock。切勿仅为了测试而放宽生产代码构造器或字段的访问可见性。
7. 切勿在业务方法内部直接实例化可注入的服务。值对象、短生命周期领域对象以及具有明确工厂归属的对象除外。

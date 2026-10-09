---
alwaysApply: false
globs: "src/main/java/**/*.java"
---

# 后端开发规则 (Backend Development Rules)

## JDK 25 基线 (JDK 25 Baseline)

1. 后端生产代码以 JDK 25 作为语言和运行时基线。对于新增或修改的代码，只要能提升清晰度、正确性或资源安全性，优先使用稳定的 JDK 25 语法与标准 API。
2. 不可变的 DTO、命令（commands）、事件（events）与值对象（value objects）优先使用 `record` 及 record 模式匹配；对真正封闭的领域变体使用 sealed 类或接口；在周边代码可读性更佳的前提下，使用 `instanceof` 与 `switch` 的模式匹配、switch 表达式、文本块（text blocks）等现代语法。
3. 仅当其生命周期和线程边界语义符合现有的 Spring/Netty 设计时，方可使用诸如 `ScopedValue` 等 JDK 25 API。切勿将其用作隐式的可变全局状态，亦不可在没有明确归属和清理边界的情况下引入新的上下文机制。
4. 严禁仅为了使用更新的语法而开启预览特性（preview features）、孵化中 API（incubating APIs）或 `--enable-preview`。启用预览特性需要明确的任务范围、文档化的兼容性边界以及专项验证；原始类型模式匹配（primitive patterns）及其他仅在预览阶段的特性绝非默认的生产代码风格。
5. 对于 Spring 组件、服务、处理器（handler）、实体（entity）或其他显式类型声明、构造器、注解、反射、代理或序列化契约至关重要的生产类，切勿使用紧凑源码文件（compact source files）或实例 `main` 方法。
6. 保持 record、sealed 继承体系、模式匹配和灵活构造器体（flexible constructor bodies）的行为等价性：除非已验证兼容性，否则切勿将它们应用于可变的 ORM 实体、代理敏感的框架类型、基于继承的扩展点，或需要普通类的序列化契约中。
7. 仅将现代 JDK 25 语法应用于明确要求的后端修改范围。切勿仅为了代码风格而进行全仓库范围的现代化改造或重写稳定的既有代码；务必保持既有公共协议、数据库映射、线程所有权、事务边界、本地化日志记录以及 Spring/Netty 的现有生命周期行为不变。

# Build, Environment & Tooling Patterns (构建环境与工具链避坑)

本文档记录 AionEmu 本地开发环境、IDE 运行机制及编译器特定行为的实战经验。

> Pattern IDs: `ENV-001`–`ENV-004`
> card_status: ACTIVE; host-specific observations must retain their environment scope
> scope: IDEA/package runtime artifacts, Maven/JDK/Lombok behavior, and safe bulk text editing
> last_reviewed: 2026-09-14

---

## [ENV-001] 一、IDEA 运行环境与代码生效判定（致命排查陷阱）
<!-- pattern-metadata
status: CONFIRMED
scope: IDEA and packaged runtime classpaths, resources and deployment artifacts
first_seen: unknown
last_verified: 2026-09-14
symptom: 改了源码但运行行为不变、日志与源码不一致、stale class
root_cause: The active process reads a different classpath artifact than the edited source tree
fix_or_guardrail: Identify the launch mode and compare source mtime with target/classes or packaged artifacts
evidence: src/main/java/com/aionemu/gameserver/questEngine/definition/QuestDefinitionDirectoryLoader.java; target/classes; aion/AionEmu.jar; log/console.log
validation: runtime; mtime and classpath inspection before code-level diagnosis
boundaries: Do not mix IDEA target artifacts with packaged deployment evidence
superseded_by: none
first_check: launch command, target/classes, JAR or resource directory and log/console.log
-->

1. **Stale Class 陷阱与运行原理**：
   - 用户日常开发通常直接通过 IntelliJ IDEA 启动 Spring Boot 主类，CWD 为仓库根目录。
   - IDEA 运行时读取的是 `target/classes/` 中的编译字节码与资源副本，**`aion/AionEmu.jar` 与 `aion/data/` 镜像完全不参与该运行模式**（仅 `package.sh` 打包部署路径使用）。
   - 运行时静态数据（含 Quest XML）通过 `QuestDefinitionDirectoryLoader` 从 classpath 加载。
2. **验证改动的唯一可信依据**：
   - 排查“改了代码为何运行无反应/Bug 依然存在”时，**必须首先对比对应 `.class` 或资源文件的修改时间（mtime）是否晚于源文件修改时间**。
   - 若 `target/classes` 未更新，说明 IDEA 增量编译未触发，切勿归咎于代码逻辑错误。
   - 提醒规则：向用户提供改动后，若改动晚于其服务启动时间，必须主动提醒用户在 IDEA 中重新构建并重启。
3. **真实运行日志排查路径**：
   - 真实运行日志输出在 `log/console.log`，可直接 `grep` 任务 ID、`QUEST_AUDIT` 或 `QUEST_RUNTIME`。
   - 注意：`quests.log` 通常为 0 字节，不承载运行时审计日志。

---

## [ENV-002] 二、Maven 与 JDK 编译环境
<!-- pattern-metadata
status: CONFIRMED
scope: Maven annotation processing, JDK compatibility and test baselines
first_seen: unknown
last_verified: 2026-09-14
symptom: Maven 与 standalone javac 结果不一致、Lombok 构造器缺失、JDK 25/26 行为漂移
root_cause: Standalone compiler experiments omit the project annotationProcessorPaths or use a different JDK baseline
fix_or_guardrail: Treat Maven with the configured processor paths as the authoritative compiler validation
evidence: pom.xml; src/test/java/com/aionemu/gameserver/questEngine/runtime/QuestMutationPlannerTest.java
validation: Maven compile or test-compile when authorized; standalone javac is exploratory only
boundaries: Host-specific JDK and Maven paths must not be generalized to every checkout
superseded_by: none
first_check: pom.xml, java version, Maven processor paths and baseline diff
-->

1. **Maven 路径**：
   - 本机 Maven 路径位于 `/opt/homebrew/bin/mvn`（用户终端的全局 `PATH` 可能未包含 `mvn`）。
2. **JDK 目标版本与兼容性**：
   - 项目构建目标为 Java 25，开发环境使用 Java 26 可以稳定完成 `mvn compile` 与 `mvn test-compile`。
3. **全量测试已知环境性失败 (JDK 25 反射 / Objenesis Mock 限制)**：
   - 全量运行 3086+ 单元测试时，约有 113 个既有失败是由于 **JDK 25 反射限制与 Objenesis Mockito 动态代理受限** 引发的环境性失败（如 `NpcMoveControllerPathTest`, `QuestE2eInfrastructureTest` 37 项等），非代码逻辑改坏。
   - 另有部分既有断言漂移，排查测试失败时应对比基线提交 `git diff <base>..HEAD`，确认失败断言涉及的源文件/XML 是否在本轮改动范围内。
4. **Lombok 编译器陷阱（Maven vs Standalone Javac）**：
   - JDK 26 下直接运行 `javac -proc:full -cp lombok.jar` 可能会出现构造器注解静默不生效的假象。
   - 任何涉及注解处理器的实验和验证，**必须以 Maven `annotationProcessorPaths` 配置的构建执行结果为准**。

---

## [ENV-003] 三、Lombok 源码重构规则
<!-- pattern-metadata
status: CONFIRMED
scope: Lombok-generated constructors and accessors in Java source
first_seen: unknown
last_verified: 2026-09-14
symptom: Lombok 方法或构造器消失、重载 setter 冲突、子类 override 编译失败
root_cause: Lombok skips generation when an existing method has the same name and parameter count or field eligibility differs
fix_or_guardrail: Audit overload arity and required final fields before removing hand-written methods
evidence: .agents/rules/lombok.md; src/main/java/com/aionemu/gameserver/controllers/CreatureController.java
validation: static; Maven compile required before accepting a Lombok refactor
boundaries: Type differences do not avoid same-arity collisions; explicit initializers change required-constructor membership
superseded_by: none
first_check: same-name methods, parameter count, final-field initialization and @Override sites
-->

1. **同名同参数个数冲突规则**：
   - 类中若已存在同名且**参数个数相同**的方法（即便入参类型不同），Lombok 将拒绝生成对应方法。
   - **重载 Setter 陷阱**：如存在 `setState(int)` 与 `setState(CreatureState)`，若删去其中一个寄希望于 `@Setter` 自动补充，会导致该方法彻底丢失。重载访问器必须全量保留手写实现。
2. **带参方法非访问器**：
   - 类似 `isCanTeleport(Player)` 这种带有入参的方法不是合法的 JavaBean getter，删除会导致子类 `@Override` 编译失败。
3. **RequiredArgs 字段集合边界**：
   - `@RequiredArgsConstructor` 的参数集合为：**未初始化的 `final` 且非 `static` 字段**。带显式初始值的 final 字段不参与构造器参数。

---

## [ENV-004] 四、代码注释与文本处理安全纪律 (Comment Safety)
<!-- pattern-metadata
status: CONFIRMED
scope: Bulk source text edits, comment localization and lexical structure preservation
first_seen: 2026-08-14
last_verified: 2026-09-14
symptom: 批量注释后代码行丢失、括号错位、词法状态被破坏
root_cause: Broad string replacement or two-stage comment stripping crossed code and comment lexical boundaries
fix_or_guardrail: Use minimal unique anchors, preserve code bytes and scan comments with a single-pass lexer
evidence: .agents/rules/ai-artifacts.md; .agents/rules/formatting.md
validation: diff inspection; lexical/static check; compile only when authorized
boundaries: Comment-only edits still require structural review when strings contain comment-like tokens
superseded_by: none
first_check: git diff added/removed code lines, anchor uniqueness and lexical state transitions
-->

1. **批量编辑安全操作纪律（防止误伤代码）**：
   - 在进行注释汉化或格式治理时，曾发生因 old_string/new_string 缩进不匹配而误删 `if (...)` 或多加闭合括号 `}` 的事故。
   - **纪律要求**：
     - 用不含行首缩进的**最小唯一锚定串**做 `old_string`（如 `spawn(287261, x, y, z, (byte) 0); //Cannon Ball.`）；
     - `new_string` 必须**逐字保留**锚定串中的代码部分，仅修改注释文本；
     - 每次批量编辑后必须执行：
       `git diff -- <files> | grep -E '^[-+]' | grep -vE '(//|/\*|\*/)'`
       验证无任何实际代码行被改动，或直接通过 `mvn -q compile` 进行语法兜底；
     - 包含 `case/if` 块的大段替换，必须在锚定串中包含边界行（如 `}` 或 `break;`），严防括号多写或漏写。
2. **词法解析的单遍状态机原则**：
   - 源码中存在装饰性注释（如 `//\\//\\//***...`）内嵌 `/*` 字符的情况。两阶段正则剥离会导致语法解析错乱，批量处理必须使用单遍词法状态机。

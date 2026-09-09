# 无效代码清理报告（2026-09-09）

## 任务

优化项目，处理无效方法、类、字段，保留重要内容。仅针对 Java 代码。

## 分析方法

自研静态分析（`scripts/deadcode/analyze_deadcode.py`）：

- 语料：`src/main/java` + `src/test/java` + 资源文本（XML/properties/yml，覆盖 AI 名、效果类名等字符串引用）
- 类级：简单名全语料零引用 → 候选；排除 4 个 `CompiledScriptLoader` 动态加载包（`gameserver.ai`、`commands.admin/player`、`world.zone.scripts`、`instance.handlers.scripts`）、框架注解类、含 main 的类、同名歧义类
- 成员级：private 方法/字段在文件内（注释剥离后）仅声明处出现 → 候选；排除带注解成员、Lombok 类级注解的字段、`serialVersionUID` 等
- 复核：`aion/` 部署目录二次校验；按“名称是否还出现在注释中”分桶

## 删除结果（用户已确认范围 A+B 并授权编译验证）

| 类别 | 数量 | 说明 |
|---|---|---|
| 零引用类（删除文件） | 46 | 含 4 个级联死亡类（DAO 实现、SM 包等）；另有 49 个同类由并行进程先行删除，合计 95 |
| 未使用 private 方法 | 124 | 多为副本脚本遗留 `sendMsg`/`deleteNpc`/`stopInstanceTask` 等 |
| 未使用 private 字段 | 54 | 含未赋值/未读取的 SQL 常量、任务句柄等 |
| 连带清理 | 孤立 import、被注释禁用的调用点（桶 B） | 随同方法一并移除 |

编辑文件约 120 个；`SchemaGen`（XSD 生成开发工具）与 11 个 JAXB 数据契约类**保留**。

## 关键事故与教训

1. **分析器注释剥离缺陷**：装饰性行注释 `//\\//\\//***...` 内含 `/*` 序列，两阶段剥离（先块后行）会把它误判为块注释起点，吞掉其后真实代码，导致“在使用”被误判为“未使用”。
   - 影响：`LinkgateFoundryInstance` 的 `sendMessage`/`linkgateTask` 误报；删除脚本的括号平衡守卫阻止了该文件写入，未造成实际破坏。
   - 修复：`analyze_deadcode.py` 改为单遍状态机剥离（行注释优先）。
2. **并行修改**：任务期间另一进程在同工作区执行了约 1900 文件的样式优化并先行删除了 49 个类（与本报告清单一致）。应对：放弃陈旧分析结果，全量重跑后再删除。
3. **验证兜底**：`verify_deletions.py` 以正确的注释/字符串掩码复查所有删除点，确认零悬空引用。

## 验证

- `mvn compile`：通过（仅 Lombok Unsafe 警告）
- `mvn test-compile`：通过（888 个测试文件）
- 最终重扫：未使用 private 方法 0、字段 0、零引用类仅剩保留项

## 事故与修复（SPI，当日后续）

**症状**：清理后服务器启动即崩——`ServiceConfigurationError: DAOClassProvider: Provider GameDAOClassProvider not found`。

**根因**：`META-INF/services/com.aionemu.commons.database.dao.DAOClassProvider` 以 Java SPI（ServiceLoader）按全限定名注册了 `GameDAOClassProvider`/`LoginDAOClassProvider`。该注册文件**无扩展名**，未被分析语料（按扩展名过滤）覆盖，两个 Provider 被误判为零引用类删除；其注册的 `PlayerAtreianBestiaryDAO`/`PlayerTransfoDAO` 随后级联误删。ServiceLoader 失败是运行时错误，编译无法发现。

**处置**：
1. 从 git 恢复 4 个被误删的类（2 Provider + 2 DAO）。
2. 应用户要求**移除 SPI**：单 Spring Boot 应用同包部署全部服务后，SPI 解耦已无意义。`DAOManager.init()` 改为 `init(DAOClassProvider)` 显式传参；两个启动桥接点（`LoginStartupRuntimeBridge`、`GameUtilityServicesRuntimeBridge`）直接 `new` 各自 Provider；删除 `META-INF/services/` 注册文件。依赖关系由编译器保证，此类错误今后会在编译期暴露。
3. `mvn compile` 复验通过。
4. 分析器修复：资源语料纳入 META-INF/ 下全部文件（`is_corpus_resource`）。

**保留结论更新**：`GameDAOClassProvider`、`LoginDAOClassProvider`、`PlayerAtreianBestiaryDAO`、`PlayerTransfoDAO` 为**有效代码**，不属于死代码。

## 产物

- `scripts/deadcode/analyze_deadcode.py` — 候选分析（已修复注释处理 + META-INF 语料）
- `scripts/deadcode/refine_candidates.py` — 分桶复核
- `scripts/deadcode/delete_deadcode.py` — 安全删除（含守卫）
- `scripts/deadcode/verify_deletions.py` — 悬空引用验证
- `scripts/deadcode/deletion_log.txt` — 完整删除日志

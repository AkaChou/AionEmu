# Static Data & JAXB Patterns (静态数据与 JAXB 模式)

本文档记录 AionEmu 静态模板数据、XML 映射及动态反射加载的底层事实。

> Pattern IDs: `SDJ-001`–`SDJ-002`
> card_status: ACTIVE; verify source and runtime evidence before treating a claim as universal
> scope: static data loaders, XML/JAXB entities, and dynamically loaded server classes
> last_reviewed: 2026-09-14

---

## [SDJ-001] 一、动态反射加载边界（严禁按死代码删除）
<!-- pattern-metadata
status: CONFIRMED
scope: Dynamic script loading, AI classes, command handlers and data-text mappings
first_seen: 2026-09-09
last_verified: 2026-09-14
symptom: 静态搜索无引用却删除后启动失败、AI 或技能 XML 无法加载
root_cause: Runtime discovers classes through package scanning, reflection and data attributes rather than static Java calls
fix_or_guardrail: Preserve dynamic package trees and search aion data text before rename or deletion
evidence: src/main/java/com/aionemu/commons/scripting/CompiledScriptLoader.java; src/main/resources/aion/data/static_data/npcs/
validation: static; runtime loader logs when the affected package or data is exercised
boundaries: Package allowlists do not prove every class is used; validate the specific loader and data version
superseded_by: none
first_check: CompiledScriptLoader, @AIName and data-text references
-->

1. **`CompiledScriptLoader.load()` 4 大动态反射包树**：
   以下 4 个包内的类由脚本加载器通过包名动态扫描与反射加载，**永远不会在代码中出现直接静态引用**。在执行死代码扫描与清理时，必须白名单保留整个包树：
   - `com.aionemu.gameserver.ai`
   - `com.aionemu.gameserver.commands.admin` / `player`
   - `com.aionemu.gameserver.world.zone.scripts`
   - `com.aionemu.gameserver.instance.handlers.scripts`

2. **数据文本引用与映射规则**：
   - **技能效果类**：`skillengine/effect/*Effect.java` 依简单类名由技能 XML 动态反射创建。
   - **AI 控制器类**：由 NPC XML 模板中的 `@AIName` 属性值隐式关联。
   - **代码影响面分析规范**：任何涉及此类实体的重构、重命名或安全删除，必须将 `src/main/resources/aion/` 下的数据文本纳入全文关联搜索范围。

3. **已知数据缺口容错提示 (非代码 Bug)**：
   - 启动日志中出现的约 316 条 NPC 技能槽 unresolved 警告，属于 `NpcSkillDefinitionLoader` 的 `sourceOrphanCount` 正常兜底逻辑（`NpcSkillTemplate.unresolved`），反映的是历史上静态 XML 数据的轻微孤立条目，而非代码运行故障。

---

## [SDJ-002] 二、JAXB 实体绑定与 final 字段限制
<!-- pattern-metadata
status: CONFIRMED
scope: JAXB-bound entity fields and JDK 25 or newer reflection behavior
first_seen: unknown
last_verified: 2026-09-14
symptom: JAXB 反射警告、final field 写入失败、XML 属性反序列化后值未生效
root_cause: JAXB needs to write instance fields while newer JDKs restrict reflective mutation of final fields
fix_or_guardrail: Remove final only from XML-injected instance fields and retain static final or safe XmlTransient caches
evidence: src/main/java/com/aionemu/gameserver/model/templates/item/ItemTemplate.java; src/main/java/com/aionemu/gameserver/skillengine/model/SkillLearnTemplate.java
validation: static; focused binding test or runtime loader evidence required per entity
boundaries: Do not remove static final constants or final fields proven to be excluded from JAXB binding
superseded_by: none
first_check: JAXB annotations, field declarations and runtime binding warnings
-->

1. **实例字段严禁声明为 `final`**：
   - **现象**：启动时 JVM（JDK 25+）抛出 final 字段反射改写警告（`Illegal reflective access to final field...`），或反序列化值未生效。
   - **根因**：JAXB 反射机制在反序列化 XML 填充属性时，需要对已声明的字段进行反射写入。JDK 25+ 对运行时修改实例 final 字段施加了最严苛的安全拦截。
   - **代码库规范（永久定论）**：
     - JAXB 绑定的实体模板类（包括 `ItemTemplate`, `BombTemplate`, `TeleporterTemplate`, `TelelocationTemplate`, `HotspotlocationTemplate`, `KiskStatsTemplate`, `SkillLearnTemplate` 等 34+ 个核心类），所有由 XML 反序列化注入的**实例字段严禁声明为 `final`**。
     - 必须保留真实的 `static final` 常量（常量不会被 JAXB 反射修改）。
     - `@XmlTransient` 标记的内部缓存字段（如只读 Map）不受 JAXB 反射影响，可安全保持 final。
   - **编译与设计安全性**：
     - 移除实体实例字段的 `final` 绝对不会破坏编译安全，不影响 definite assignment 语义、Lombok 生成访问器，亦不影响 switch 模式匹配。

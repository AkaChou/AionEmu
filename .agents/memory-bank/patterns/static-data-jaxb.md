# Static Data & JAXB Patterns (静态数据与 JAXB 模式)

本文档记录 AionEmu 静态模板数据、XML 映射及动态反射加载的底层事实。

> Pattern IDs: `SDJ-001`–`SDJ-004`
> card_status: ACTIVE; verify source and runtime evidence before treating a claim as universal
> scope: static data loaders, XML/JAXB entities, and dynamically loaded server classes
> last_reviewed: 2026-09-20

---

## [SDJ-001] 一、动态反射加载边界（严禁按死代码删除）
<!-- pattern-metadata
status: CONFIRMED
scope: Dynamic script loading, AI classes, command handlers and data-text mappings
first_seen: 2026-09-09
last_verified: 2026-09-21
symptom: 静态搜索无引用却删除后启动失败、AI 或技能 XML 无法加载、命令别名静默失效
root_cause: Runtime discovers classes through package scanning, reflection and data attributes rather than static Java calls
fix_or_guardrail: Preserve dynamic package trees and search aion data text before rename or deletion; verify command aliases in administration/commands.properties against super("alias") declarations in both directions
evidence: src/main/java/com/aionemu/commons/scripting/CompiledScriptLoader.java; src/main/resources/aion/data/static_data/npcs/; src/test/java/com/aionemu/gameserver/commands/CommandAliasRegistryTest.java; .agents/summary/command-alias-registry/2026-09-21-dropinfo-removal-and-command-alias-gate.md
validation: static; runtime loader logs when the affected package or data is exercised; client acceptance 2026-09-21 for the restored //dropinfo (CommandAliasRegistryTest still pending Maven run)
boundaries: Package allowlists do not prove every class is used; validate the specific loader and data version
superseded_by: none
first_check: CompiledScriptLoader, @AIName, data-text references and administration/commands.properties aliases vs command super("alias") declarations (both directions)
-->

1. **`CompiledScriptLoader.load()` 4 大动态反射包树**：
   以下 4 个包内的类由脚本加载器通过包名动态扫描与反射加载，**永远不会在代码中出现直接静态引用**。在执行死代码扫描与清理时，必须白名单保留整个包树：
   - `com.aionemu.gameserver.ai`
   - `com.aionemu.gameserver.commands.admin` / `player`
   - `com.aionemu.gameserver.world.zone.scripts`
   - `com.aionemu.gameserver.instance.handlers.scripts`

   **事故记录（2026-09-18，`212e00ef4`）**：`//dropinfo`（`commands/admin/DropInfo.java`）与 `//instance_manager`（`commands/admin/InstanceEngineManager.java`）被当作“零引用死代码”删除，运行期命令静默失效；别名 `dropinfo`（`commands.properties:181`）与 `instance_manager`（`:37`）从未变动，客户端帮助 `HTML/commands.xhtml` 也仍在文档化它们。守卫：`CommandAliasRegistryTest` 双向核对配置别名与 `super("alias")` 声明，任一侧缺失即失败。

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

---

## [SDJ-003] 三、英吉斯温实际世界 ID 与镜像服静态数据边界
<!-- pattern-metadata
status: CONFIRMED
scope: Inggison player-facing teleport, portal, instance-exit, return-item, quest, zone, weather, event, siege and AI static data
first_seen: 2026-09-14
last_verified: 2026-09-14
symptom: 英吉斯温地图驻地、门户、副本出口或任务错误进入 210130000，或运行数据再次把 210130000 当作玩家目标
root_cause: Retail master-server data retained 210130000 for Inggison while the live world is 210050000; hotspot and portal templates consumed the master ID directly
fix_or_guardrail: Player-facing Inggison targets must use 210050000; keep 210130000 only as a legacy map definition and compatibility sentinel, migrate paired zones/assets, and normalize TeleportService2 plus HotspotTeleportService
evidence: commit a7da0ad67; src/main/java/com/aionemu/gameserver/services/teleport/TeleportService2.java; src/main/java/com/aionemu/gameserver/services/teleport/HotspotTeleportService.java; src/main/resources/aion/data/static_data/portals/portal_loc.xml; src/main/resources/aion/data/static_data/quest_definition/quests/10034.xml; src/main/resources/aion/data/static_data/zones/zones_quest.xml
validation: static; client
boundaries: 210130000 map/zone/spawn assets remain inert legacy definitions; Gelkmaros mirror 220140000 is not folded into 210050000
superseded_by: none
first_check: hotspot_location.xml mapid, portal_loc.xml world_id, TeleportService2.resolveInggisonWorldId and quest world-id/zone names
-->

1. **实际世界不变量**：玩家可见的英吉斯温世界 ID 固定为 `210050000`；`210130000` 只允许存在于旧镜像服地图定义、其 zone/spawn 资源和兼容性归一代码中。
2. **迁移面**：热点、门户坐标、副本出口、回城物品、剧情传送、任务世界/区域条件、风轨、天气、活动、攻城和 AI 区域必须同时迁移；只改热点会把传送入口和任务判定拆到两个世界。
3. **运行时护栏**：`TeleportService2` 和 `HotspotTeleportService` 对英吉斯温镜像服目标做最终归一；数据回退或漏改时仍应落到 `210050000`。

---

## [SDJ-004] 四、JAXB 集合属性缺省即 null（允许零子元素的表必须初始化列表）
<!-- pattern-metadata
status: CONFIRMED
scope: JAXB 绑定的模板实体集合字段，以及允许 0 个子元素的静态数据表（如 weather_table.xml 的 map/table）
first_seen: 2026-09-20
last_verified: 2026-09-20
symptom: 某张静态数据表被改成"零子元素"后在启动期抛 NullPointerException；实体 getter 返回 null 而不是空集合
root_cause: JAXB 只为实际出现的子元素写入集合字段，XML 中一个子元素都没有时不会创建空 List，字段保持声明时的值（未初始化即 null）
fix_or_guardrail: 允许零子元素的 List/Collection 字段必须在声明处初始化为 new ArrayList<>()；不要把"XML 至少有一个元素"当契约，以 XSD 的 minOccurs 为准
evidence: src/main/java/com/aionemu/gameserver/model/templates/world/WeatherTable.java:32; src/main/resources/aion/data/static_data/weather_table.xsd; .agents/summary/weather-theobomos/diagnosis-sandrain.md
validation: focused-test；探针用真实 JAXB 反序列化 + 真实服务私有方法反射调用验证（有元素时照常填充，无元素时得到空列表而非 null）
boundaries: 只适用于允许零子元素的集合字段；语义上必须非空的表应改数据或让校验器报错，而不是加静默兜底
superseded_by: none
first_check: 目标字段是否声明为 List/Collection、XSD 与数据是否允许 0 个子元素、getter 是否可能返回 null
-->

1. **JAXB 不会为缺省的元素集合创建空 List**：XML 里没有对应子元素时，`List` 字段保持声明时的值；未初始化的字段就是 `null`，随后
   `for (X x : table.getXs())` 抛 NPE（实测栈：`Cannot invoke "java.util.List.iterator()" because the return value of
   "...getZoneData()" is null`）。
2. **契约以 XSD 为准**：`weather_table.xsd` 的 `table` 是 `minOccurs="0"`，`<map id="210060000" zone_count="1" weather_count="0"/>`
   是合法数据，模型必须能承受空集合，否则"把某图天气表清空"这种纯数据操作会让服务端启动失败。
3. **护栏**：允许零子元素的集合字段一律在声明处 `= new ArrayList<>()`；有子元素时 JAXB 会往这个列表追加
   （已回归验证：Poeta 的 7 条 weather 条目照常解析），初始化不改变正常数据的语义。

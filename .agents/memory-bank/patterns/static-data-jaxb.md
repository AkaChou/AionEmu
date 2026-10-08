# Static Data & JAXB Patterns (静态数据与 JAXB 模式)


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

本文档记录 AionEmu 静态模板数据、XML 映射及动态反射加载的底层事实。

> Pattern IDs: `SDJ-001`–`SDJ-005`
> card_status: ACTIVE; verify source and runtime evidence before treating a claim as universal
> scope: static data loaders, XML/JAXB entities, and dynamically loaded server classes
> last_reviewed: 2026-09-23

---

## [SDJ-001] 一、动态反射加载边界（严禁按死代码删除）
<!-- pattern-metadata
status: CONFIRMED
scope: Dynamic script loading, AI classes, command handlers and data-text mappings
first_seen: 2026-09-09
last_verified: 2026-09-22
symptom: 静态搜索无引用却删除后启动失败、AI 或技能 XML 无法加载、命令别名静默失效
root_cause: Runtime discovers classes through package scanning, reflection and data attributes rather than static Java calls
fix_or_guardrail: Preserve dynamic package trees and search aion data text before rename or deletion; verify command aliases in administration/commands.properties against super("alias") declarations in both directions
evidence: src/main/java/com/aionemu/commons/scripting/CompiledScriptLoader.java; src/main/resources/aion/data/static_data/npcs/; src/test/java/com/aionemu/gameserver/commands/CommandAliasRegistryTest.java; .agents/summary/command-alias-registry/2026-09-21-dropinfo-removal-and-command-alias-gate.md
validation: static; `mvn -Dtest="com.aionemu.gameserver.commands.CommandAliasRegistryTest" test` passed 2026-09-22 (4/4) covering the reflective admin/player alias registry (`//movetonpc` etc.); runtime loader logs when the affected package or data is exercised; client acceptance 2026-09-21 for the restored //dropinfo
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
evidence: commit a7da0ad67; src/main/java/com/aionemu/gameserver/services/teleport/TeleportService2.java; src/main/java/com/aionemu/gameserver/services/teleport/HotspotTeleportService.java; src/main/resources/aion/data/static_data/portals/portal_loc.xml; quest/retail/retail-xml-retention.xml 的 quest 10034 行（XML已退役并删除，见git历史）; src/main/resources/aion/data/static_data/zones/zones_quest.xml
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

---

## [SDJ-005] 五、数据里的字符串形式数字（前导零）—— 自写绑定器必须分辨字面量
<!-- pattern-metadata
status: CONFIRMED
scope: 自写 JSONL/文本绑定器解析静态模板数值字段（JAXB 路径不受影响）
first_seen: 2026-09-23
last_verified: 2026-09-23
symptom: 自写绑定器把 `"questid":"04450"` 读成 0，8 处静默错误；只有逐字段比对才暴露
root_cause: 物品模板数据里存在以字符串书写的数字（带前导零，全量 8 处）；生成器的安全转换规则不容前导零、故意保留其为字符串，而绑定器的"裸数字快路径"直接按字符累加，遇到引号立即停止并返回 0
fix_or_guardrail: 数值快路径前先判断值是否以 `"` 开头，是则交回字符串转换路径；转换规则本身保持"不容前导零"（否则丢零）
evidence: .agents/summary/item-format-migration/2026-09-23-plain-jsonl-probe.zh-CN.md §8.2; .agents/summary/item-format-migration/gen_plain_jsonl.py; .agents/summary/item-format-migration/JsonlItemProbe.java; src/main/java/com/aionemu/gameserver/model/templates/item/actions/QuestStartAction.java:30
validation: 11 分片逐字段比对 11,925,674 个字段；修复前 mismatched=11（含 8 处 questid），修复后回到 3（均为重复 id 假阳性）
boundaries: 仅适用于自写绑定器；JAXB 通过 Integer.parseInt 正确处理前导零（"04450" → 4450），现有 XML 路径不受影响
superseded_by: none
first_check: 目标字段是数值类型但值以 `"` 开头；数据里是否存在前导零数字（`grep -oE '"[a-z_]+":"0[0-9]+"'`）
-->

1. **数据里存在以字符串书写的数字**：`"questid":"04450"`（带前导零，全量 8 处）。这**不是数据缺陷**——
   `questid` 在 `QuestStartAction` 里是 `int`，`Integer.parseInt("04450")` = 4450，JAXB 一直处理正确。
2. **生成器刻意保留其为字符串**：安全转换规则 `^-?(0|[1-9]\d{0,14})$` 不容前导零，匹配失败即保留原字符串，
   以免 `"04450"` → `4450` 静默丢零。规则本身是对的。
3. **自写绑定器的快路径必须分辨字面量**：`readInt()` 见到 `"` 会立即停止累加并返回 0，
   于是这 8 处被**静默读成 0 而非报错**——与 SDJ-004 同类，都属于"不报错但结果错"。
4. **这条边界只在自写绑定器上成立**：JAXB 走 `Integer.parseInt`，本就不受前导零影响。
   因此格式迁移期间，**XML 与 JSONL 两条路径都必须留在逐字段比对的覆盖范围内**。

---

## [SDJ-006] 六、传送门主服/活服孪生双写、落点朝向编码与玩家级硬门禁的落点（instancereq 空转）
<!-- pattern-metadata
status: CONFIRMED
scope: 传送门静态数据（portal_loc.xml/portal_template2.xml）与 PortalService/PortalAI2 门禁面：主服/活服孪生 NPC、真端落点与 h 编码、portal_req 与硬门禁的分工、热更缓存
first_seen: 2026-10-08
last_verified: 2026-10-08
symptom: 点击传送门 NPC 零响应；数据里配了 portal_req（quest_req/min_level）却完全不生效（管理员、普通玩家都不拦）；主服世界落点把玩家送进惰性镜像
root_cause: ① 58Server 数据为主服血统：同一功能面常成对出现 `_M` 主服孪生 NPC（spawn 于惰性 [Master Server] 图），既有配置只挂在主服孪生上、活服 NPC 漏配 ⇒ getPortalDialog/getPortalUse 返回 null ⇒ 点击静默（Silentera 入口 730256/730260 与出口 730270/730271 两次同型命中）；② portal_req 的全部检查位于 `accessLevel < AdminConfig.INSTANCE_REQ` 分支内，且本部署 `gameserver.administration.instancereq = 0` ⇒ 所有 portal_req 空转（并非只旁路管理员）；③ PortalAI2（ai=portal）的 portalUse 在 handleSpawned 缓存，数据热更后旧 NPC 实例仍持旧值
fix_or_guardrail: 活服孪生必须补配（目标 loc 对齐 `_M` 孪生）；玩家级故事门禁必须落**代码硬门禁**（`isXxxEntryWorld/meetsXxxEntryRequirement` + 在 port() 权限块之前检查并发 1300690——Kahrun/Balaurea 成式，不受权限旁路），数据 quest_req 仅作 instancereq>0 部署的双保险；真端落点/朝向：`Map/Worlds/<dir>/world.xml` direct_portals 为权威，`h = dir(度)/3`（MathUtil.convertDegreeToHeading）；世界 ID 以 `id/worldid.xml` 区分活/主服（600010000 Underpass 活 / 600110000 主服；210050000 / 220070000 活）
evidence: commit 735d1c117; src/main/java/com/aionemu/gameserver/services/teleport/TeleportService2.java（isSilenteraCorridorEntryWorld/meetsSilenteraCorridorEntryRequirement）; services/teleport/PortalService.java（port() 硬门禁）; ai/QuestItemNpcAI2.java（失败交互三态）; static_data/portals/portal_loc.xml:333-334; portal_template2.xml（730256/730260/730270/730271）; .agents/summary/silentera-underpass-live-gates/README.zh-CN.md
validation: focused-test（IDEA MCP 2026-10-08 用户授权）SilenteraCorridorEntryRequirementTest 4/4、QuestItemNpcAI2Test 3/3、KahrunEntryRequirementTest 4/4、BalaureaTeleporterQuestRequirementTest 6/6 = 17/17；实机（用户逐轮驱动：冷重启 + //reload portal + //reload_spawn Silentera）
boundaries: instancereq=0 为本部署选择（portal_req 整体空转是现状，不得据 XML 有配置推断门禁生效）；回廊出口的龙界 10031 门禁不做豁免（用户裁定"要求 10031、和真端一致"）；Gelkmaros 侧 22014xx 落点仍在 220140000（主服）世界（19 条遗留，未迁移）；真端 ScriptDLL 的门禁条件表静态不可取证（通用 IAIScriptNpcImp 实现）
superseded_by: none
first_check: `grep npc_id 目标NPC` 于 portal_template2.xml（连同活服孪生）；`grep instancereq src/main/resources/aion/config/administration/admin.properties`（=0 ⇒ portal_req 空转）；ai=portal 的门改动后需 `//reload_spawn <图>` 或重启（ai=portal_dialog 每击实时查）
-->

1. **主服/活服孪生双写**：传送门 NPC 常成对（例：入口 730256 `LF4_UnderPass_In` ↔ 主服 731824 `LF4_UnderPass_M_In`；出口 730270 ↔ 731860；三族相同功能面同理）。既有配置常只挂 `_M` 孪生——而 `_M` 孪生的 spawn 在惰性 [Master Server] 图里、玩家不可达 ⇒ 活服侧点击静默。排查顺序：先 `grep npc_id` 看配置挂在哪个孪生、再 `grep` spawn 文件确认哪个孪生活在玩家可达世界。
2. **玩家级硬门禁必须落代码**：`portal_req`（含 quest_req/min_level）整体在 `accessLevel < INSTANCE_REQ` 分支内，且本部署 `instancereq = 0` ⇒ 数据门禁对所有玩家空转。故事级门禁照 Kahrun/Balaurea 硬门禁成式在 `port()` 权限块之前硬检查（Silentera 入口：仅本族「回廊进军准备」10035/20035 COMPLETE 放行，失败发 1300690）；XML 的 quest_req 保留为 `instancereq>0` 部署的双保险。
3. **落点与朝向的真端编码**：落点坐标誊自真端 world.xml 的 direct_portals（`From_..._To_..._S1/S2` 与 `Op_Risen_coord` 复活点互证）；`portal_loc.h`（signed byte）= 真端 `dir`(度)/3（`MathUtil.convertDegreeToHeading`，`RetailDirectPortalEngine` 同式，dir>127 也是除法而非补码）；世界以 `id/worldid.xml` 判别名与主/活（Underpass=600010000 活、Underpass_M=600110000 主服）。
4. **热更与缓存**：`PortalAI2`(ai=portal) 的 `portalUse` 在 `handleSpawned()` 一次性缓存 ⇒ `//reload portal` 后必须 `//reload_spawn <图>`（对应世界名如 Silentera）或重启才生效；`PortalDialogAI2`(ai=portal_dialog) 每次点击实时查 `PORTAL2_DATA` ⇒ `//reload portal` 即可。zone 注册等结构数据与代码变更仍需冷重启。
5. **任务物件失败交互三态**（QuestItemNpcAI2）：引擎全未认领时——采集物零包（QE-137）；关联任务（onTalkEvent）全部 COMPLETE/REWARD ⇒ 静默（物件已用完）；其余（未接/进行中）⇒ 1300690 提醒。三态由 `failedInteractionReply(dialogNpc, collectObject, relatedQuestsFinished)` 分类、单测锁定。

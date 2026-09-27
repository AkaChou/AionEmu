# P0c-5：SimpleSerialHunt `talk_npc1` 简报步骤接线（真端字段补齐 + 退役悬空测试清理）

- 日期：2026-09-24
- 归属：真端任务驱动替换目标（P0c 线）
- 触发：P0c-4 的 T3 **clean** 全树实测（`gates/T3-p0c4-clean.log`，1952 例 / 22F / 13E）暴露
  35 处退役遗留；其中 14 处属 SimpleSerialHunt 族（13918×7、30600×6、30610×1），1 处属
  SimpleCollectItem（3734 直读退役 XML）。

## 1. 结论

1. 真端 `Quest_SimpleSerialHunt.xml` 的 **`talk_npc1`** 字段此前从未被 AionEmu 解析/消费，导致
   30600/30610 退役 XML 后丢失"接取 → 见简报 NPC → 清标志位 → 开计数"的整段流程；DLL/客户端证据一致，
   属**真端字段缺口**，本次补齐。
2. 串行合成器节点改用规范名 `started/briefed/k1..kN`（原为网格名 `a0b0c0…`，语义是链式但名字误导），
   并新增 `SECTION_5` 简报标志位（0/1，位宽 2 @30..31，避免越过 32 位 quest_vars）。
3. 旧 XML 的 EnterWorld 旧档修复边按 P3 既有裁定**不迁移**（13918 var0=2..5、30600/30610 step-2、
   旧领奖投影补齐）；测试改为反向锁定"不得再引入迁移边"。
4. `metadata.kills` 零售元数据为空且**生产代码零消费**，击杀面权威改为阶梯的 `KillNpc` 边；漂移登记在案。
5. 3734 直读退役 XML 的测试重指生产零售视图；串行冻结指纹基线按规范名重算（仅 30600/30610 变化）。

## 2. 真端证据

| 证据 | 内容 |
|---|---|
| 真端表 `Map/XML/Quest_SimpleSerialHunt.xml` | `<id id="30600"><acquired_npc_name>Hejitor</acquired_npc_name><talk_npc1>Linocus</talk_npc1>…`；30610 = Astella/Aluna；全族 16 行中 3 行带 `talk_npc1`（另 9622 Rebecca_2） |
| NPC 名索引 | `Linocus → 800324`、`Aluna → 800326`（唯一解析，无歧义） |
| 客户端 `quest_q30600.html` | 同时存在 `select1`（接取页，按钮 `HACTION_QUEST_ACCEPT_SIMPLE`/`REFUSE_SIMPLE`）与 `select2`，且 `select2` 的唯一按钮是 **`HACTION_SETPRO1`** |
| 客户端 `quest_monster.csv` | 30600/30610 击杀行门控 `Progress(SECTION_0<1; SECTION_5==0)` —— 标志位未清时击杀行不可见 |
| 客户端 `quest_summary` | 行 0 = "和 Linocus 对话"，行 1/2 = Named/Boss 击杀，行 3 = 向 Hejitor 报告 |

## 3. 代码改动

| 文件 | 变化 |
|---|---|
| `retail/RetailSimpleHuntTable.java` | `Entry` 增列 `talkNpc`（真端 `talk_npc1`）+ 6 参兼容构造；`parseEntry` 读该字段 |
| `retail/RetailSimpleSerialHuntTable.java` | 解析 `talk_npc1` 进 `Entry.talkNpc` |
| `retail/RetailQuestDriver.java` | `compileSimpleSerialHunt` 解析 `talk_npc1` 并传 `compileSerialChain(plan, stages, metadata, briefingNpcIds, briefingNpcName)` |
| `retail/RetailSimpleHuntDefinitionCompiler.java` | 5 参 `compileSerialChain` 重载（3 参保留）；`RETAIL_TALK_NPC_UNRESOLVED`/`RETAIL_TALK_NPC_AMBIGUOUS` 稳定拒绝码；`started/briefed/k1..kN` 规范节点；`SECTION_5` 位宽 2 @30..31；`talk_npc1` 的 `QUEST_SELECT→SELECT2` 与 `SETPRO1→briefed` 路由；`retail.debugCompilationFailure` 例外诊断开关 |
| 测试：`ChainEliteLadderContractTest` | 重指 `ProductionQuestDefinitions`；两条旧档修复用例改为"零售形状不得再引入修复边"；击杀面改锁 `KillNpc` 边 |
| 测试：`CounterChainBriefingStageContractTest` | 重指生产视图；30600/30610 旧档迁移期望改为"无迁移"（24112 仍锁 XML 自愈） |
| 测试：`RewardNpcOwnershipContractTest` / `Quest3734DragonArmsChestTest` | 直读退役 XML 的加载器改走 `ProductionQuestDefinitions` |
| 测试：`RetailSimpleSerialHuntGateTest` | 夹具对齐生产（解析并传 `talk_npc1`）；`inspect` 期望改为"段数 + 简报位/节点/路由"不变量 |
| 数据：`retail-simple-serial-hunt-ir-fingerprints.tsv` | 重算冻结指纹（仅 30600/30610：nodes 6→7、transitions 33→35） |
| 工具：`affected_quest_tests.py` | T1 固定清单新增 `RetailSimpleSerialHuntGateTest`（5 例 / 1.5s） |

## 4. 漂移裁定（真端优先）

1. **旧档迁移面移除**：13918 step 档 var0=2..5、23918 网格档、30600/30610 step-2 档与旧领奖投影
   不再有 EnterWorld 修复边；存量进度按新阶梯重读。零进度损失需另做一次性 DB 归一化（未做，登记为运维可选项）。
2. **metadata.kills 为空**：`RetailQuestMetadataCompiler` 传 `List.of()`，生产零消费；测试改锁击杀边。
3. **节点名变化**：规范名替代网格名属内部标识变化（指纹基线已重算），不影响存档投影。

## 5. 验证

| 档 | 选择器 | 结果 | 日志 |
|---|---|---|---|
| 聚焦契约 | ChainElite + CounterChainBriefing + RewardNpcOwnership + 3734 + SerialGate + HuntTable | **28 例 / 0F / 0E / SUCCESS** | `gates/p0c5-serial-contract-focus5.log` |
| 串行门禁复跑（指纹基线更新后） | 上述前 5 类 | **25 例 / 0F / 0E / SUCCESS** | `gates/p0c5-serial-gate2.log` |
| T1 | 9 个固定门禁（含新串行门禁） | **37 例 / 0F / 0E / SUCCESS / 23s** | `gates/T1-115232.log` |
| T2 | T1 ∪ 13918/23918/30600/30610/3734 命中类 | **81 例 / 0F / 0E / SUCCESS / 22s** | `gates/T2-115302.log` |
| T3 clean | 整个 questEngine 测试树（forkCount=2） | 见 §5.1 | `gates/T3-p0c5-clean.log` |

### 5.1 T3 clean 对账

命令：`mvn -o -B clean test -Dtest='com.aionemu.gameserver.questEngine.**' -DfailIfNoTests=false -DforkCount=2`
（`gates/T3-p0c5-clean.log`）。

结果：**1952 例 / 8F / 13E / 1 skipped**（P0c-4 clean 基线为 1952 例 / 22F / 13E）。

与 `T3-p0c4-clean.log`（35 个不同失败行）逐字 `comm` 对账：

| 分类 | 数量 | 内容 |
|---|---:|---|
| **本切片已修复** | **15** | ChainEliteLadder ×7、CounterChainBriefing ×6、RewardNpcOwnership ×1（30610）、Quest3734 ×1 |
| 仍失败（非本切片） | 19 | SimpleUseItem 退役批（zcode 在飞）：1309(+LegacyTemplateMirror)、1514×5、1197、3914、1644×2、1561×2、11036×2、80008×3、2578、2718/1718 标题奖励 |
| 新增（非本切片） | 1 | `QuestDraupnirNpcVariantContractTest.liveNpcVariantsPreserveQuestDropContracts`：该测试文件**正被并发会话改写**（工作树 ` M`，已把 `load()` 迁到 `ProductionQuestDefinitions`），失败点是 SimpleHunt 网格族退役后真端 metadata 缺 base→live 变体 drop（`236924`），属其 metadata/drop 接力面，与本切片路径（串行族 + 3734）无交集 |

结论：本切片 15 处归零、零新增自因失败；剩余 20 处全部归属并发 SimpleUseItem / Draupnir 在飞切片。

## 6. 未落地 / 后续

| 项 | 数量 | 说明 |
|---|---|---|
| **P0c-6（下一刀）** SimpleHunt 网格族 `talk_npc1` | 47（其中 23 行 XML_RETENTION、24 行宇宙外/未登记） | 这 23 行的保留原因均为 `SEMANTIC_GAP:DIALOG_ROUTE`（phase5-3 旧登记），**当前可编译但 IR 在对话框轴与旧 XML 不等价——缺口正是缺失的简报步骤**；`Entry.talkNpc` 已就绪，只需在网格合成器 `build(...)` 接线（SECTION_5 + started/briefed + QUEST_SELECT/SETPRO1），随后重算等价分类/指纹并逐行裁定退役 |
| 9622（串行族 `talk_npc1=Rebecca_2`） | 1 | 仍保留 XML（未退役），接线后可评估 |
| 旧档 DB 归一化 | 可选 | 若要求存量玩家零进度损失，需一次性 SQL/脚本把旧 step/grid 档重写为新阶梯投影 |

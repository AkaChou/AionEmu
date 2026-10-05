# 存量红重锚批：实机修正批语义漂移 + 退役后原生面收口（2026-10-05）

## 缘起

「存量欠账清理」续批。承接 A：Playbook checker 14 项悬空引用（PATTERNS/CASES 重锚，已完成，checker 绿）。
承接 B：验证重锚引用的存活性时，跑出四个红类——深入归因发现它们**不是** IR 退役病（QE-146 已覆盖），
而是**实机修正批之后的语义漂移**（测试冻结在旧行为）与**P9 语义解冻**（证据冻结过期）。本批逐类重锚。

## 归因方法（重锚前三步取证，本批新增口径）

1. **先查该红何时引入**：`git log -- <测试文件>` 拿最后改动提交（测试冻结点），再对 P3/P5 报告
   （`.agents/summary/quest-engine-native/p3|p5/`）里的**期绿基线**——本批四类的期绿读数 = 早绿后红；
2. **再定位改语义的提交与批文档**：git log 主代码文件（DataDrivenNativeRuntime / SimpleTalkHandler /
   NativeNpcNameResolver…）→ 找到 10-04/10-05 的实机修正批（99684c70d/36d42b775/062257818/f60f97633/
   6a888b801 等）与其主题文档（`.agents/summary/quest-1131-relay-close-dialog/` 等）；
3. **再取现语义的绿参考**：家族门测试（`*NativeFamilyGateTest`）是编码现语义的活基准；
   真端函数级证据（FUN_180cabb10 / FUN_180caf150 / 0x5d8）为最高权威。

## 逐类裁定

| 类 | 红数 | 病根 | 处置 |
|---|---|---|---|
| `EarlyElyosQuestRegressionTest` | 8（含 1 旧登记） | 10-04/10-05 实机批语义漂移：中继推进/接取尾 = 真端 0x5d8 关窗零发页（f60f97633）；cab520 两支接取均发物（6a888b801 期）；两步报告（31 只发契约确认页，1009 推进） | 7 处重锚：`assertOnlyDialogPage(1352/1003)` → `assertCloseDialog`；1002 补 give 断言；1561 宝箱改两步（31→2375 契约页、1009→REWARD+窗）；1137 随 P4 采集族改原生面断言（`SimpleCollectItemHandler` 访问器 + 1137b/1137a 物品事实） |
| `ChainEliteLadderContractTest` | 7（全类） | 13918/23918 是 SimpleSerialHunt 原生行（retention RETAIL_TABLE）；类仍读 ProductionQuestDefinitions（步 f 后无零售合成视图） | 整类重锚原生面：表行五阶段（count 各 1）→ `resolveMonsterIds` 命中五精英；e2e 击杀阶梯（`onKill(Player,Npc)` + `DataDrivenProgress`，宽度 SIX 槽码、末杀收口）；QE-052 owner 分离 + 行 5 NPC 报告面（onDialog 31）；固定奖励列（`NativeQuestXmlTable` 真端 quest.xml + `RetailItemNameIndex.resolve`，锁 c44c50bd0 丢第三条 ITEM） |
| `Quest26802ClientDialogAlignmentTest` | 2（全类） | 26802 是 DD 行（DD_AREA_HUNT_GRID）；P7 步 f 曾「手术删 DD 方法、余下方法保留原红/绿态」——两条法留待收录 | 整类重锚 DD 面：owns/routes + 表行 acquireKind=enterarea + 无 Talk 接取面；击杀网格两组（8 图书管理员×30 组 1 / 6 首领×2 组 2）；e2e 组计数（counter<target 守卫、超杀零写、全组达标收口进 REWARD 清槽、REWARD 击杀零响应） |
| `QuestMinionTutorialRetailAlignmentTest` | 1 | **证据解冻**：旧断言 `rewardNpc(28808)==null`（NPC_Housing_FOBJ_01 未唯一解析冻结）；P9（91eaef381）NPC_ 前缀归一化解到 Housing_FOBJ_01 对象模板 730534——冻结已废止 | 重锚 `assertEquals(730534, ...)` + 解冻依据注释 |

## 关键边界（重锚纪律）

- **不得锁「在飞/未裁定」状态**：26802 的报告面（DD 交付对象 #2）现只对 Talk 接取行注册，且该面正由并行
  会话的在飞改动（19671 中继步行扩展，工作区未提交）演进——本批只锁行数据事实（reward_npc_name），
  **不锁报告面有无**，避免与在飞批冲突。
- **原 IR 名称保留**：全部方法名未动（Playbook `TestClass#method` 引用不断；`COUNTER_SOURCE_
  PROJECTION_NO_LOCK` 等行引用的 `killEdgesAdvanceOnlyTheFirstUnfinishedStage` 现语义下仍是该行的有效证明）。
- **并行会话绕行**：提交只带本批自有文件；`DataDrivenNativeRuntime.java`、`DataDrivenNativeRuntimeGateTest.java`、
  `.agents/summary/quest-19671-relay-close-claim/` 属并行在飞改动，不 staging。

## 验证（IDEA MCP）

- `build_project`（filesToRebuild 强制重编）：零 problem。
- 四类全绿：`EarlyElyosQuestRegressionTest`（19 例，testFailed=0）、`ChainEliteLadderContractTest` 7/7、
  `Quest26802ClientDialogAlignmentTest` 2/2、`QuestMinionTutorialRetailAlignmentTest` 1/1；
  加既有 `Quest14123ZoneSpawnTest` 5/5。
- Playbook checker：`PLAYBOOK_PATTERNS=72 REPRESENTATIVE_COMMITS=60 REPRESENTATIVE_TESTS=74 DETAILED_CASES=49` 绿。
- 纯测试面（零生产代码变更——对 HEAD 而言；测试对工作区含并行在飞主代码仍绿）。

## 工具坑（工作流记录）

IDEA 对外部工具写入的文件存在 VFS/增量编译滞后：Edit 落盘后直接 `build_project` 可能**不重编**该文件
（本批 26802 首跑即吃陈旧类文件）。口径：外改后以 `build_project(filesToRebuild=[...])` 逐文件强制重编，
或核 `target/test-classes/**.class` 时间戳 > 源码时间戳。

## 欠账（不在本批）

1. 其余存量红未盘点：P8 口径 73 红类清单未随仓（gate 产物已按口径清理），无全树 runner ⇒ 无法一次枚举；
   本批只收净手边可归因面（4 类 + Playbook 引用面）。
2. DD 交付对象 #2 面是否应扩到 EnterArea/Hunt 接取行（26802 报告面）——由并行 19671 批裁定。
3. `deliveryWindowPage` 等 EarlyElyos 旧 helper 若有残留死代码，随下次触碰清理。

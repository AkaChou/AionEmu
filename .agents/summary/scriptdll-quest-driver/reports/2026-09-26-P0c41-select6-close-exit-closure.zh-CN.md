# P0c-41 select6 关闭出口闭包：契约门 fatal 归零（1932/3092）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

- 日期：2026-09-26
- 切片：P0c-41（承 P0c-40 登记的 `REPORT_ALIAS_SELECT6_FROM_REWARD`）
- 台账：`../GOAL-retail-driver-progress.zh-CN.md`
- 探针（已归档，不入测试树）：`../P0c41Select6CloseExitProbeTest.java.txt`

## 1. 结论（TL;DR）

**契约门最后一轴归零**：P0c-40 收口后残留的 2 条致命（1932/3092 的 `reward` 态 CHECK 失败页
「结束对话」按钮无路由）已闭合，全目录契约审计 **fatal 2 → 0**，`EVIDENCE_REQUIRED` 桶**清空**。

1. **根因是同一交付段的两个锚点**：`B NPC_REPORT` 块（`reportFlowChain`）把 select5 页 +
   CHECK 按钮对 + **关闭出口**一起锚在块 `source`（阶梯行的 `s{K}`）；而链式登记表的**显式 `R`
   记录**把同一个 CHECK 对锚在块 `target`（`reward`）。"显式路由覆盖块路由"只在
   `(source, npc, dialogId)` 同键时生效，键不同 ⇒ 两个锚点各留一套：`reward` 态多出一对
   失败页下发路由（39 / 20002 的 priority=1 分支），却没有任何 FINISH_DIALOG(1008) 关闭路由。
2. **修的是客户端合同，不是某个任务**：客户端 `select6` 页（2716）**唯一**按钮是
   `HACTION_FINISH_DIALOG`(1008)「结束对话」（`quest-dialog-action-details.csv`）——所以
   **任何下发该页的节点都必须同节点、同 NPC 登记 1008**。按此规则在 `buildChain` 加一个
   闭包后处理（与文件里既有的 `SELECT2_CONTINUE`、`SELECT5_CHECK` 两个后处理同址同形），
   只认客户端出口登记表在册的 `SELECT6` 行，补出的路由与同任务既有关闭出口**逐字同形**。
3. **改动面 = 缺陷面（无溢出）**：闭包只闭合 4 个键（1932/3092 × {`reward`+39, `reward`+20002}），
   链式冻结指纹 set 对拍 **changed = {1932, 3092} / added = 0 / removed = 0**；契约审计
   `PAGE_ACTION_MATCHED` **+2**（正是两条新关闭路由被匹配），无新增未达页、无新增致命。
4. **未采取的两条路（记录理由）**：①生成器侧把交付段 R 记录锚点从 `reward` 迁到 `s{K}`——
   会改登记表事实源，且 priority=1 失败行 `target=reward` 的"失败不推进"语义需逐行重写，
   风险面远大于缺口面；②只给 1932/3092 打点补路由——非通用规则，掩盖上述客户端合同。

## 2. 根因（证据链）

### 2.1 症状（审计行，修复前）

```
EVIDENCE_REQUIRED | 2716 | action=1008 | path=reward + NPC 203893 + 20002 -> reward + page 2716
  reason=visible client action has no route; client does not prove its response page or state side effect
```

同一页在另一锚点上是好的：`PAGE_ACTION_MATCHED | 2716 | action=1008 | path=s1 + NPC 203893 + 39 -> s1 + page 2716`
——**同一客户端页在两套锚点上的致命性不同**，只跑一侧会得出相反结论（与 QE-066 同源）。

### 2.2 IR 事实（`P0c41Select6CloseExitProbeTest#dumpRows`，修复前）

`reward` 节点上（NPC 203893，1932；3092 同形，NPC 798191）：

| 路由（source→target） | 事件 | priority | 条件 | after-commit |
|---|---|---|---|---|
| reward→reward | TalkToNpc 203893 / 39 (CHECK) | 0 | `HasItem(182206008,1)` | 扣物 + 开领奖窗(5) |
| reward→reward | TalkToNpc 203893 / 20002 (CHECK_SIMPLE) | 0 | 同上 | 同上 |
| reward→reward | TalkToNpc 203893 / **39** | **1** | 无 | **ShowQuestDialog(2716)** |
| reward→reward | TalkToNpc 203893 / **20002** | **1** | 无 | **ShowQuestDialog(2716)** |
| **（缺）** | **TalkToNpc 203893 / 1008** | — | — | — |

`s1` 节点上有 `TalkToNpc 203893 / 1008 -> ShowQuestSelectionDialog(SELECT_QUEST)`（块自己的关闭出口），
`started`/`unaccepted` 态各有一份（接取流规范展开）。

### 2.3 客户端证据（强二证据）

| 证据 | 内容 |
|---|---|
| `docs/quest/client-dialog-mapping/quest-dialog-action-details.csv` | page `2375`(select5) 可见按钮 = {`39`「拿出水晶腰带」}；page **`2716`(select6) 可见按钮 = {`1008`「结束对话」}** |
| `quest_client_dialog_exits.tsv` | `1932`/`3092` = `SELECT1_1 SELECT6 SELECT2_CONTINUE SELECT5_CHECK`（select6 页在册） |
| 真端表 `Quest_SimpleTalk.xml` | `1932`: `Tersites/Asclepius/Tersites` + `item_check=1`；`3092`: `Tityus/Vison/Tityus` + `item_check=1`（模板表**没有**页链/页级列 ⇒ 页级合同只能由客户端登记提供） |

### 2.4 锚点为什么错位（登记表生成器侧）

`build_quest_client_talk_chain_steps.py::synthesize_canonical` 对交付段用**字面** `reward` 作
source/target 发射 R 记录（`R(reward, 'QUEST_SELECT', 'reward', 'reward', ...)` 与其后的 CHECK
按钮对），而 `apply_talk_ladder` 的阶梯迁移规则只搬 `r[5]=='started'` 的行 ⇒ 交付段 R 记录留在
`reward`，可 `B NPC_REPORT` 块的 source 已迁到 `s{K}`——块的关闭出口跟着块 source 走，于是
**下发失败页的锚点（reward）与关闭出口的锚点（s1）分家**。平铺行（K=0）不吃这条分家：24123 的
CHECK 对锚在 `started`，其 select6 失败页由接取流的 `started` 态关闭出口覆盖（审计已
`PAGE_ACTION_MATCHED`）。

### 2.5 爆炸半径（为什么只有 2 行致命）

全目录扫描（探针）：**SELECT6 pushers = 1519** 条路由下发 2716 页，其中 **89** 条所在节点没有
同节点同 NPC 的 1008 关闭路由，**79** 条所在任务在客户端出口登记表里**有** SELECT6（其余 10 条
无客户端页证据，不动）。按 `(quest, source, npc, dialogId)` 去重后为 **69 个键 / 64 个任务**——
但其中**只有 2 个任务属 SimpleTalk 链式登记族**（1932/3092），其余 62 个属 DataDriven /
SimpleCollectItem / CombineTask / 旧 Java 形态族（节点名如 `s5`、`equipped96`、`reward30`、
`instance95`），**审计不可达故非致命**（契约门 fatal 全表 = 0）。本片只按客户端证据闭合链式族，
62 行原样登记（见 §6）。

## 3. 落地物

| 物件 | 路径 | 说明 |
|---|---|---|
| 编译器补丁 | `src/main/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleTalkDefinitionCompiler.java` | `buildChain` 新增 select6 关闭出口闭包后处理；两个私有助手 `closeKey` / `pushesSelect6` |
| 冻结指纹（双副本） | `src/test/resources/quest/retail-simple-talk-chain-ir-fingerprints.tsv` + `target/test-classes/...` | 1932/3092 两行改值（第 48/122 行），**285 行布局逐字节保留**，md5 `6a4b09d6f24074d523fffb737b518cc4` |
| 重冻脚本 | `../p0c41_refreeze_fingerprints.py` | set 对拍断言（changed 必须 == {1932,3092}、added/removed 为空）+ 原值漂移守卫 + whipsaw 守卫；`--apply` 写双副本 |
| 重冻前快照 | `../p0c41-pre-refreeze-fingerprints.tsv` | 逐行可复核的"改前"证据 |
| 指纹 dump | `../p0c41-fingerprint-dump.tsv` | `-Dretail.talkChain.fingerprintOut=` 产出的重算全表（285 行） |
| 爆炸半径表 | `../p0c41-select6-close-exit-blast.tsv` | 修复前/后探针对拍：69 → 65 键、4 键闭合、溢出 0、fatal 2 → 0 |
| 裁定表 | `../p0c41-select6-closure-decisions.tsv` | 逐键裁定（4 键）+ 两条未采取路线的理由 |
| 阻塞登记 | `../p0c41-blocked-rows.tsv` | 新轴 `SELECT6_CLOSE_EXIT_OTHER_FAMILIES`（62 行）+ 承接的既有轴 |
| 探针归档 | `../P0c41Select6CloseExitProbeTest.java.txt` | 树内 `.java` 与 `.class` 均已删除 |

## 4. 证据（命令 → 结果）

| 证据 | 命令 | 结果 |
|---|---|---|
| 修复前基线 | `mvn -o -B test -Dtest=P0c41Select6CloseExitProbeTest` | `statuses={CLIENT_PAGE_UNREACHED=1372, EVIDENCE_REQUIRED=2, PAGE_ACTION_MATCHED=106756, TERMINAL_PAGE_REACHED=22702}`；`FATAL count=2`（1932/3092，page 2716，action 1008）；`SELECT6 pushers=1519 missing close exit=89 missing WITH select6 exit=79` |
| 修复后 | 同上 | `statuses={CLIENT_PAGE_UNREACHED=1372, PAGE_ACTION_MATCHED=106758, TERMINAL_PAGE_REACHED=22702}`（**EVIDENCE_REQUIRED 桶消失**）；`FATAL count=0`；`missing close exit=85 missing WITH select6 exit=75` |
| 闭包集合 | `python3 -B ../p0c41_select6_blast.py <before.log> <after.log>` | `closed=4 introduced=0 chain_closed=['1932','3092'] other_closed=[]` → **OK: closure set == chain rows, no overflow** |
| 指纹 set 对拍 | 重算 dump vs 冻结表 | `frozen=285 fresh=285 changed=2 added=[] removed=[]`，`changed ids: ['1932','3092']` |
| 指纹重冻 | `python3 -B ../p0c41_refreeze_fingerprints.py /tmp/p0c41-fp.tsv --apply` | `added=[] removed=[] changed=['1932','3092']` → 双副本 md5 一致；`diff` 仅两行（48/122）改值 |
| 链门（验证模式） | `mvn -o -B test -Dtest=RetailSimpleTalkChainGateTest` | `Tests run: 2, Failures: 0`（重冻前必红，重冻后绿） |
| 退役恒等式 | `python3 -B ../verify_retirement.py` | `catalog=1335 directory=1335 retired=4889 sum=6224 — OK`（catalog/retired 相对 P0c-40 收口值 1341/4883 各偏 6，系并发车道（P0c-49/DataDriven）在此期间新采纳行所致；本片未改任何清单/目录/XML 行——本片改动仅编译器 + 指纹表） |

## 5. 门禁

| 门 | 命令 | 结果 |
|---|---|---|
| 链门 | `run_quest_gates.sh T1`（含 `RetailSimpleTalkChainGateTest`） | 见下表 T1 |
| 契约门 | T1 内含 `QuestClientContractGateTest`（基线 `quest-client-contract-baseline.tsv` 为空 ⇒ 任何致命即新引入） | **fatal 2 → 0**（修复前后对比见 §4 第 1–2 行） |
| T1 净树 | `run_quest_gates.sh T1` | 见 §5.1 |
| T2（1932/3092） | `run_quest_gates.sh T2 1932 3092` | 见 §5.2 |

### 5.1 T1（18 个固定全局门）

命令：`.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T1`（日志 `gates/T1-145930.log`，371 s）。
**63 例 3F**（基线 `gates/T1-143553.log` 63 例 **4F**）——**差值恰好是契约门，零新增**。

| 门类 | 结果 |
|---|---|
| `QuestClientContractGateTest`（**本片目标**，空基线 ⇒ 任何致命即新引入） | **1/1 绿**（基线为 1F） |
| `RetailSimpleTalkChainGateTest` / `RetailSimpleTalkGateTest` | 2/2、3/3 绿 |
| `QuestDefinitionCatalogManifestTest` / `RetailOwnershipGateTest` / `QuestProductionStartupGateTest` / `ProductionCatalogWhitelistVerificationTest` | 10/10、4/4、2/2、1/1 绿 |
| `RetailQuestCatalogTest` / `RetailQuestDriverOverlayTest` / `RetailSystemGrantDispatchTest` / `RetailNonIrAxisGateTest` / `RetailSimpleItemPlayGateTest` / `QuestItemPlayGrantGateTest` / `RetailSimpleSerialHuntGateTest` / `QuestInteractionObjectContractGateTest` | 2/2、5/5、7/7、4/4、1/1、1/1、5/5、2/2 绿 |
| `RetailSimpleHuntFamilyGateTest` | 1/1 绿（337.4 s） |
| `RetailDataDrivenGateTest` | 6 例 **2F**：`driftVersusShellsIsRegistered`（20035 登记/实际拒绝码不一致）、`frozenFingerprintsCoverExactlyTheRetiredQuests`（15042/16821/16823/26821/26823 指纹漂移）— **与基线 `T1-143553` 同两条** |
| `RetailSimpleCollectItemGateTest` | 6 例 **1F**：`frozenFingerprintsCoverExactlyTheRetiredQuests`（frozen=155 / retired=175）— **与基线同一条** |

三条残留失败的归属证据：①三者与 13:55 / 14:13 / 14:35 的三轮基线**逐条同名**（`T1-134855` / `T1-141318` / `T1-143553`）；
②三者全部落在本片**未触碰**的文件上（本片改动仅 `RetailSimpleTalkDefinitionCompiler` 与 SimpleTalk 链式指纹表，
不含 DataDriven / CollectItem 的编译路径、冻结表、漂移登记）；③并发 P0c-49 车道在同一时段运行 T1/T2（`gates/p0c49-*.log`，
15:00–15:01）。

### 5.2 T2 归因（1932 / 3092）

命令：`.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T2 1932 3092`（日志 `gates/T2-150614.log`，407 s）。
选择器覆盖 23 个类（18 个 T1 固定门 + 命中本片两行的 `MissionItemConsumptionBatchRegressionTest` /
`QuestDialogOrderAuditTest` / `QuestItemSourceContractGateTest` / 探针类）。
**89 例 4F、0E**，四条失败**全部为既有/并发**，本片自因**零新增**：

| 失败 | 归属 | 证据 |
|---|---|---|
| `QuestItemSourceContractGateTest.repairedQuestsRequireTheirOwnRetailCollectItems`（quest **4966**：期望 `[182207136, 182400001]` 实得 `[182400001]`） | **CHRONIC**（既有债） | 与**改动前最后一次全量 T3**（`gates/T3-130322.log:2548`，13:03:22）**逐字相同**（含期望/实得集合）；且 4966 不在本片改动面（本片只给 1932/3092 增一条 `reward` 态 `FINISH_DIALOG`，不涉及物品需求） |
| `RetailDataDrivenGateTest.driftVersusShellsIsRegistered`（20035 拒绝码不一致） | 并发 DataDriven 车道在飞 | T1 基线（13:55/14:13/14:35）同名同条 |
| `RetailDataDrivenGateTest.frozenFingerprintsCoverExactlyTheRetiredQuests`（15042/16821/16823/26821/26823） | 同上 | 同上 |
| `RetailSimpleCollectItemGateTest.frozenFingerprintsCoverExactlyTheRetiredQuests`（frozen=155 / retired=175） | 并发采集族在飞 | 同上 |

绿项（与本片同轴、必须全绿）：探针 `P0c41Select6CloseExitProbeTest` **2/2**、
`QuestClientContractGateTest` **1/1**、`QuestDialogOrderAuditTest` **17/17**、
`RetailSimpleTalkChainGateTest` **2/2**、`MissionItemConsumptionBatchRegressionTest` **4/4**、
`QuestItemSourceContractGateTest` 2/3（唯一红为上述 4966 既有债）。

## 6. PENDING（未验证项）

1. **运行时与客户端目检**（未启服，按项目约束不启动服务器）：1932（NPC 203893 Tersites）与
   3092（NPC 798191 Tityus）在 **REWARD 态**缺物品时按 39 / 20002 → 应看到 select6 失败页，
   其「结束对话」按钮应能关窗。本片只证 IR/契约层闭合，未做运行时目检。
2. **62 行其他族的同形状**（`SELECT6_CLOSE_EXIT_OTHER_FAMILIES`）：审计不可达 ⇒ 非致命；
   是否真实可达需按族单独取证（各族族门的审计可达性），不得套用本片规则批量改。
3. **并发车道数字漂移**：`verify_retirement` 的 catalog/retired 计数在并发车道（P0c-49、
   DataDriven）在飞时逐分钟变化；本片以"本片未触碰这些文件"为准，恒等式以各自切片收口值为准。

## 7. 命令索引（可复现）

```bash
# 1) 证据：全目录 select6 缺口的爆炸半径 + 契约致命列表
mvn -o -B test -Dtest=P0c41Select6CloseExitProbeTest
# 2) IR / 客户端页 / 审计逐行 dump（1932、3092）
mvn -o -B test -Dtest=P0c41Select6CloseExitProbeTest#dumpRows
# 3) 冻结指纹重算 + set 对拍 + 重冻
mvn -o -B test -Dtest=RetailSimpleTalkChainGateTest -Dretail.talkChain.fingerprintOut=/tmp/p0c41-fp.tsv
python3 -B .agents/summary/scriptdll-quest-driver/p0c41_refreeze_fingerprints.py /tmp/p0c41-fp.tsv --apply
mvn -o -B test -Dtest=RetailSimpleTalkChainGateTest
# 4) 闭包集合断言表
python3 -B .agents/summary/scriptdll-quest-driver/p0c41_select6_blast.py <before.log> <after.log>
# 5) 门禁（T1 净树 / T2 本片 2 行）
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T1
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T2 1932 3092
# 6) 退役恒等式
python3 -B .agents/summary/scriptdll-quest-driver/verify_retirement.py
```

## 8. 下一步

1. `ACCEPT_PAGE_NOT_EMITTED`（客户端有 `select1(1011)` 页但真端 IR 无任何转换下发该页，48 行）。
2. 剩余 `ADOPTION_BLOCKED`：`1324/2428`（`NOT_IN_CANONICAL_SET`）、deferred `11001/21138/30055/30202/30302`、
   cutscene `18806/28806`。
3. `24123`（`XML_NPC_AXIS`：`204345 Rikesh` vs 真端 `DF2_NPC_Ananta → 204387`；reward 投影 1 vs 末行 2）。
4. `3914/80310/80311` 的 `START_DIALOG_ROUTE_GAP`；REWARD 投影通道 16 行裁定。
5. 本片新登记轴：`SELECT6_CLOSE_EXIT_OTHER_FAMILIES`（62 行，按族取证后再动）。

## 并发车道注记

本片执行期间并行存在 **P0c-49 车道**（其门禁日志 `gates/p0c49-*.log`、`gates/T2-150108.log`）与
**DataDriven / 采集族车道**（在飞行 15673/25673/80846/80847 等）。两者与本片共用工作树与 `target/`，本片
的门禁 T1/T2 与其运行时段重叠；§5 的判定按"逐条与改动前全量基线对拍"给出，不把并发面记为本片自因。

**同标签不同车道**：`.agents/summary/scriptdll-quest-driver/p0c41-sensory-area-scan.tsv` 与
`p0c41_sensory_area_scan.py` 是**另一车道**的 P0c-41（EnterArea 区名桶的感官区 NPC 证据扫描，
IDInfinity 判例批量化），与本片的 select6 关闭出口闭包无关；本片物件一律带 `p0c41-select6-` 前缀或
`p0c41_*fingerprint*` / `p0c41_select6_blast.py` / `p0c41_refreeze_fingerprints.py` 名称。

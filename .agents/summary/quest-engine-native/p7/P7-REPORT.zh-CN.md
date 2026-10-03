# P7 报告：native 附近任务提示轴（真端 opcode 127 / `SM_NEARBY_QUESTS`）

> **门禁产物清理提示（2026-10-03）**：本文件引用的门禁运行产物（`gates/*.log`、`*-red-classes.tsv`、`*-delta.tsv` 等）已按「只记录重要的过程内容、不记录门禁」口径清理，不再随仓保留；关键读数已内联于正文，复现请重跑对应聚焦套件，清理说明见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

日期：2026-10-01　批次：跨族共享客户端面（计划 §10.3-#18）　基线：P6 切换批 `8d7893d37`

## 1. 本批要解决的问题

P1–P6 已切换的全部 native 行在**附近任务提示**面被判「不可接取」：

- `PlayerController.updateNearbyQuests` 逐行调 `QuestService.getLevelRequirement`（走 typed 目录 `questCatalog().findMetadata`，native 行返回 `999`）与 `QuestService.checkStartConditions`（走 typed 模板 `questsData.getQuestById`，native 行模板为 `null` ⇒ 恒 false）；
- 结果：`SM_NEARBY_QUESTS`（客户端 `S_UPDATE_ZONE_QUEST`）里 **native 行一条都不会出现**，而该面正是客户端「附近可接任务」提示的数据源。

## 2. 真端事实基线（本批首次坐实，全部来自 `<真端根>/server58-source/`）

### 2.1 发包者与网络形态

- 发包者 = `MainServer_Server64/classes/Account/User.cpp` 的 `User::_UpdateQuestAcquireCondition`（`FUN_140d4aec0`，`..\..\Shared\Quest.cpp:0x4b7`）。
- 包头字节：`[size:2] [encodedOpcode:2 = 0x0181] [staticCode:1 = 0x56] [~encodedOpcode:2 = 0xfe7e]`。
  真端混淆式 `(op + 0xD5) ^ 0xD5` 反解 `0x0181` ⇒ **`0x7F` = 127**，与 `ServerPacketsOpcodes` 中 `SM_NEARBY_QUESTS = 0x7f` 同号同名；`staticCode 0x56` 与 `Crypt.staticServerPacketCode` 一致 ⇒ 确为**客户端包**（同源码里 `staticCode = 0xac` 的兄弟包是服务器间包）。
- 载荷：`[段计数:1][条目数:2（末段为负数）]` + 每条 **4 字节**：`CanAcquireQuest == 2` ⇒ `questId`；`== 1` ⇒ `questId | 0x20000`；`0` ⇒ 不入包。

### 2.2 判定三值（`Quest::CanAcquireQuest`，`MainServer_Server64/classes/Quest/Quest.cpp:0x1812`）

清单形态调用为 `CanAcquireQuest(quest, out, user, param_4 = 0, param_5 = 0, param_6 = 0, param_7 = 0)`（**不短路、不提示**），返回：

| 返回值 | 含义 | 本批落地 |
|---|---|---|
| `2` | 全部轴通过 | 平条目（`flag = 0`） |
| `1` | **仅**等级轴不达，且 `minlevel_permitted <= level + 1` | `0x20000` 软标记条目（`flag = 1`） |
| `0` | 其余一切失败（种族 / 职业 / 性别 / 头衔 / 军衔 / 重复上限 / 前置 / 等级差 > 1 / 超 `maxlevel`） | 不入列表 |

关键差异（与 NPC 对话形态对比）：NPC 问询形态（`param_5 = 1`）在等级轴上**立即返回 0**；清单形态继续判定其余轴，因此软结论 `1` 只在「其余轴全通过 + 恰好差 1 级」出现。本批据此把 port 的轴检查拆为 `levelVerdict` + `eligibilityVerdict`，清单面判定不复用会短路的 `evaluateNpcAcquire` 顺序。

`Quest::CanAcquireQuest` 各处返回 0 的现场日志串（`invalid race/minlevel/maxlevel/gameclass/finishedcount/leadingquest/restriction quest/title/gender/abyssrank`）与本批轴顺序一一对应。

### 2.3 清单来源

- 真端：`World::GetAcquirableQuestList`（`World.cpp:0x3cbe`）读世界内「可接取任务集合」，该集合由 `World::CheckAcquirableQuestFromNewNpc` / `...FromRemoveNpc` 在 NPC 生成/消失时按 NPC 任务表维护。
- 本服对照：`WorldMapInstance.addQuestIds(Npc)`（对象入图时按 `QuestNpc#getOnQuestStart()` 并入，出图回滚）⇒ **同源同形**，本批不改清单来源。

### 2.4 `finished_quest_condN` 的取值形态（顺带坐实，见 §4）

真端该列写的是**目标行的 `quest.xml <name>`**，可选带 `:n` 奖励档后缀：10035 行中 9461 行为 `Q<id>`，**574 行（CombineTask 全族，与表行数一致）为符号**（如 `ws_q5015` = 行 5015）。`<name>` 列在全表唯一（0 重复）。

## 3. 实现（4 个生产文件 + 2 个门禁类）

| 文件 | 改动 |
|---|---|
| `questEngine/tablelane/NativeQuestStartPort.java` | 新增 `ZoneVerdict{ACQUIRABLE, LEVEL_SOON, OMITTED}` + `zoneVerdict(Player, int)`；把 `evaluateNpcAcquire` 的轴拆为 `levelVerdict` / `eligibilityVerdict` / `repeatVerdict`（**NPC 面顺序与结论逐字不变**）；`prerequisiteId` 增加真端 `<name>` 符号解析（`Q<digits>` → id 快路径，其余回落名称索引） |
| `questEngine/tablelane/NativeQuestXmlTable.java` | 装载期建立 `<name> → questId` 唯一索引（重复名 fail-fast）+ `findByName` / `namedRowCount` |
| `questEngine/QuestEngine.java` | owner 面新增 `nativeZoneVerdict`（非 owner ⇒ `OMITTED`）与 `nativeAcquireAllowed`（真端 `== 2` 硬判定） |
| `services/QuestService.java` | ① `checkStartConditions` 对 native owner 分流到 `nativeAcquireAllowed`（修复势力任务列表/传送门面同病）；② 新增 `nearbyQuestFlags(Player, Collection<Integer>)`：native owner 走 `nativeZoneVerdict`（平条目 `0` / 软标记 `1` / 不入列表 `null`），typed 车道**逐字保留**原判定（等级差 ≤ 2 且 typed 起始条件通过），返回按 ID 升序（真端集合为升序 `std::set`） |
| `controllers/PlayerController.java` | `updateNearbyQuests` 改为「清单来源不变（`WorldMapInstance#getQuestIds`）+ 判定委托 `QuestService.nearbyQuestFlags`」，删除就地 typed 判定与 `questId <= 0xFFFF` 闸门（该闸门对 native 行会跳过等级轴） |
| `network/aion/serverpackets/SM_NEARBY_QUESTS.java` | 条目编码改为真端四字节形：`writeD(flag > 0 ? questId | 0x20000 : questId)`。对 id ≤ 0xFFFF 与旧字节**完全一致**；对 **703 行 id > 0xFFFF 的真端表行**（SimpleTalk 526 / SimpleHunt 142 / SimpleUseItem 33 / SimpleItemPlay 2，如 `80010`）修复旧写法 `writeH(id)+writeH(2)` 截断成假 ID 的缺陷 |

## 4. 顺带坐实的缺陷与修复（同一轴，非扩大范围）

1. **`finished_quest_condN` 符号值抛异常**：14 行（1951–1956 / 2939–2944 / 19051 / 29051）引用 CombineTask 符号（`ws_q5015` … `me_q6556`），旧实现对非 `Q<digits>` 直接抛 `NATIVE_TABLE_PARSE_FAILED`。改 `checkStartConditions` 分流后该异常会落在**玩家附近任务刷新**路径上，属本批引入的可见风险 ⇒ 必须同批修：按真端 `<name>` 索引解析（14/14 命中，无硬编码）。
2. **`minlevel_permitted = 999` 的不可接取行**：已切换族的路由集（本批实测 **5938 行**）里 **1822 行**（如 2732）带该值，真端语义为「等级轴不可达」（非「无限制」）⇒ 清单面恒 `0`，本批以扫描断言冻结（不得落成软标记）。

## 5. 门禁与证据

新增门（2 类 10 例，全绿）：

- `NativeNearbyQuestAxisGateTest`（7 例）：真端三值分档（9/8/7 级）；超 `maxlevel` 硬 0；软结论的前提（种族 `pc_dark` / 性别 `female` / 前置未完成 ⇒ 硬 0，其余轴通过才软 1）；状态轴（进行中 0 / 已完成未达上限可再接 / 达上限 0）；**全族扫描**（路由集 5938 行：软结论 ⇔ 「除等级外全通过 + 恰好差 1 级」；实测软档 1568 行、`minlevel=999` 硬 0 1822 行、非等级轴失败硬 0 2484 行；覆盖 id > 0xFFFF 行）；owner 分流（裸引擎下 native 行按真端入列、非 native 行不因本批改判）；控制器接线（`updateNearbyQuests` → 包内容 = 真端判定）。
- `SMNearbyQuestsPacketTest`（3 例）：平条目 / 软标记条目 / `80010`（id > 0xFFFF）两种条目的真端四字节字节面。

复跑证据：

| 门 | 结果 | 日志 |
|---|---|---|
| 族门 + tablelane + 本批 2 类 | **118/118 绿**（基线 108 + 本批 10） | `gates/2026-10-01-focused-run-p7-familylane.log` |
| 聚焦套件 `-Dtest=*Quest*Test,*Retail*Test` | **1699 例 / 162F+142E / 108 类红**；对 P6 基线 **ADDED 0 / REMOVED 0**，仅新增本批 2 个全绿类 | `gates/2026-10-01-focused-run-p7.log`、`-red-classes.tsv`、`-delta.tsv` |
| 主源码编译 | `mvn -o -q compile` 通过 | — |

## 6. 残余与未闭环

1. **typed 车道（未迁移族）不产生软标记条目**：本批只把 native owner 分流到真端三值；typed 车道保留原「等级差 ≤ 2 且起始条件通过」判定（字节面无变化）。随各族迁移自动收敛，P8 收口时 typed 分支应随目录一起退场。
2. **清单来源仍是「本图实例」**：真端 `World::GetAcquirableQuestList` 以世界为单位，本服 `WorldMapInstance#getQuestIds` 为同形近似；两者在副本/分层世界下的差异未实测（不影响本批语义）。
3. **未坐实轴**：`CanAcquireQuest` 还含物品/装备/制作/阵营等轴，native 端口与 P3 交接口径一致地未消费这些列；本轴与 NPC 接取面同源（同一判定函数），不会出现「能接但不在清单」或反向的不一致。
4. **客户端验收 `PENDING_CLIENT`**：代表行（5000 / 80010 / 1963）在真实客户端上的附近任务提示表现未跑（与 P6 的代表任务验收合并执行）。

## 7. 结论

native 行的附近任务提示轴已按真端 opcode 127 的三值语义接线，且 `checkStartConditions` 的 owner 分流一并修复了势力任务列表与传送门面；同批修掉了符号前置异常与 > 0xFFFF 条目的包编码截断。族门/tablane 118/118、聚焦套件对基线零新增红类，满足「按真端、不许发明、同批自检」的批门要求。

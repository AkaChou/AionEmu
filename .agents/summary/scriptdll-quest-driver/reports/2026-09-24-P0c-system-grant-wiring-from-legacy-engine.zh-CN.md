# P0c 系统发放接线证据：阵营日常的"客户端接受手势"不需要抓包（旧引擎已有全链路）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 背景：M5-b3x 把类别哨兵行编译成 `QuestEvent.SystemGrant` 边后，台账把"分发接线"记为
> **阻塞**（"阵营日常的客户端接受手势（`SM_QUEST_ACTION action=6` 后的回包）需实机抓包"）。
> 本切片用**仓库内代码证据**推翻该阻塞：该链路在旧引擎里端到端存在，且新引擎已复用一半。

## 1. 结论

1. `action=6` 提示包的**构造与包体**都在本仓（不需要抓包）；
2. 客户端"接受"走的是**既有任务接取通道**（`CM_DIALOG_SELECT → QuestService.startQuest`），
   不是阵营专用包；
3. **"无客户端手势也能发放"在生产里已经跑着**：`RetailAreaEngine` 进入 `quest_area` 时直接
   `QuestService.startQuest(...)`；
4. 客户端任务书证据（M5-b2c §4.9）显示哨兵任务的委托书页**只有 `HACTION_FINISH_DIALOG`（"把委托书收起来"）**，
   没有 ACCEPT/REFUSE → 真端语义是**服务端直接发放 + 通知**，玩家不需要"接受"手势。

## 2. 证据（`file:line`）

| 环节 | 位置 | 事实 |
|---|---|---|
| 选任务 + 发提示 | `model/gameobjects/player/npcFaction/NpcFactions.java:265-309` | `sendDailyQuest()` 用 `questCatalog()` + `metadata.npcFactionId()` 取候选，按 `DataManager.NPC_FACTIONS_QUEST_DATA.isActiveOn(id, today)` 过滤**星期位**，选中后 `faction.setQuestId(questId)` → `sendPacket(new SM_QUEST_ACTION(questId, true))`（= action 6） |
| 提示包体 | `network/aion/serverpackets/SM_QUEST_ACTION.java:115-120, 162-165` | action=6 构造器 + `writeImpl case 6 → writeH(0x01); writeH(0x0);` |
| 提示触发时机 | `services/player/PlayerEnterWorldService.java:626`、`questEngine/QuestEngine.java:2184`、`NpcFactions.java:177,238` | 进世界 / 引擎侧 / 入会与放弃后 |
| 客户端接取落点 | `network/aion/clientpackets/CM_DIALOG_SELECT.java:185` | `QuestService.startQuest(env)` |
| 提交后翻阵营状态（新引擎） | `questEngine/runtime/QuestMutationPlanner.java:220-232` → `TypedQuestAfterCommitPort.java:300` → `PlayerQuestEffectPort.java:104-115` | NONE→START 时追加 `AfterCommitAction.StartNpcFactionQuest`，端口校验 `faction.getQuestId()==snapshot.questId()` 后调 `NpcFactions.startQuest(npcFactionId)` |
| 旧引擎等价钩子 | `services/QuestService.java:648-650` | `template.getNpcFactionId()!=0 && !isTimeBased()` → `player.getNpcFactions().startQuest(template)` |
| **无手势发放已在生产** | `ai/RetailAreaEngine.java:122-126`（调用链 `:80 → :101 onPlayerMoved`） | 进入 `quest_area` → `QuestService.startQuest(new QuestEnv(null, player, questId, 0))` |
| 客户端语义 | `quest_q35007.html` 的 `ask_quest_accept` 页 | `<Selects>` 只有 `<Act href="HACTION_FINISH_DIALOG">把委托书收起来</Act>`，无 ACCEPT/REFUSE |

补充事实：`quest_data.xml` 中 `35007` 带 `npcfaction_id="2"` → `QuestService.startQuest` 的阵营前置
（`QuestService.java:606-610`：`faction.isActive() && faction.getQuestId()==questId`）在
`sendDailyQuest()` 刚赋值后天然满足。

## 3. 因此真正的缺口（不是协议）

1. **`QuestEvent.SystemGrant` 没有消费者**：全仓仅 2 处引用（`QuestEvent.java` 定义 +
   `RetailSimpleCollectItemDefinitionCompiler` 产出）→ 175 个已退役的 CollectItem 哨兵任务**只编译不发放**；
2. **顺序约束**：`NpcFactions.startQuest(npcFactionId)` 只翻阵营状态、**不插任务状态**；
   插任务状态的是 `QuestService.startQuest(env)` → 必须先 `startQuest(env)`、再由 after-commit 翻状态；
3. **待实机确认（非阻塞）**：客户端收到 action=6 后是否还需一次点击。按第 4 条证据判断**不需要**。

## 4. 接线（本切片已实施）

### 4.1 新增分发器

`questEngine/retail/RetailSystemGrantDispatcher.java`（新文件）：

| 方法 | 语义 |
|---|---|
| `isSystemGranted(catalog, questId)` | 该任务定义是否含 `QuestEvent.SystemGrant` 边（按事件类型索引查 `transitionsFor("SYSTEM_GRANT")`） |
| `grantIfSystemGranted(player, questId)` | 含该边 **且** 玩家该任务状态为 `NONE`/无记录时，`QuestService.startQuest(new QuestEnv(null, player, questId, 0))` |

### 4.2 接线点

`NpcFactions.sendDailyQuest()` 在 `sendPacket(new SM_QUEST_ACTION(questId, true))` 之后一行
（`NpcFactions.java:310-312`）调用 `grantIfSystemGranted(owner, questId)`：
- 非哨兵任务（XML 驱动或有接取路由）**不含 `SystemGrant` 边 → 行为完全不变**；
- 哨兵任务在"服务端分配轮换任务"的同一时刻直接进入 `START`，与 `RetailAreaEngine` 同款语义。

### 4.3 门禁

新增 `src/test/java/.../retail/RetailSystemGrantDispatchTest.java`（3 例，已加入 T1 固定清单）：

1. **哨兵行必须可发放**：真端 `Quest_SimpleCollectItem.xml` 的 `_faction_` 行，凡在**生产宇宙内**
   （`catalog.findExecutable` 命中）者必须带 `SystemGrant` 边 → 实测 **40/40 通过**；
2. **普通 NPC 接取行不得被系统发放**：`1103`（Mires 对话接取）断言为 false；
3. **未知/非法 ID 安全返回 false**：`999999` / `0` / `null` catalog。

### 4.4 门禁暴露的一处口径错误（已勘误）

`_faction_` 在真端表有 **43 行**，其中 **3 行**（`39611`/`47112`/`49611`）**不在本服宇宙内**
（`quest_definition_catalog.xml`、`quests/`、`git HEAD`、`retail-xml-retention.tsv`、漂移登记表全无），
即 `no-contract-row`。首版门禁把它们当作"必须有定义"导致 FAIL；已改为**只在宇宙内行上断言**，
并在断言里显式记录该口径。M5-b2c 报告 §1 中"这 3 行旧 XML 写了接取 NPC"的表述**已勘误**。

## 5. 未验证 / 边界

- **未在实机客户端**验证客户端收到 `SM_QUEST_ACTION(action=6)` + `addQuest` 后的 UI 表现（需用户执行）；
- `sendDailyQuest()` 在进世界时也会跑：若玩家任务列表已满，`startQuest` 会返回 false 并可能发 1300622 提示
  —— 属既有行为的自然延伸，未额外处理；
- 本切片只接线 `_faction_`（阵营日常）这一类；`_challengetask_`（87 行，数据 87/87 齐）与
  `_area_`（走既有 `RetailAreaEngine`）的推广见下一步。

## 5.1 实测证据

```text
T1（8 个固定门禁，含新增 RetailSystemGrantDispatchTest）：
  Tests run: 28, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS / 31 s
  log: .agents/summary/scriptdll-quest-driver/gates/T1-091132.log

首次暴露：RetailSystemGrantDispatchTest.everyFactionSentinelRowCarriesSystemGrantEdge
  → 哨兵行缺 SystemGrant 边: [39611=<no-definition>, 47112=<no-definition>, 49611=<no-definition>]
  → 判明为「宇宙外 3 行」，修正门禁口径后 40/40 绿。

T3（questEngine 全树，forkCount=2）：
  Tests run: 1955, Failures: 5, Errors: 0, Skipped: 1 — 419 s
  log: .agents/summary/scriptdll-quest-driver/gates/T3-091251.log

  **这 5 个失败与本切片无关**（改动前就在）：对照 zcode 同日的 `gates/T3-085854.log`
  （1952 例 / 5F，先于本切片）→ 失败方法集合**逐字相同**，全部是 SimpleHunt 冻结 IR 指纹
  「真端侧 IR 漂移（冻结基线需重算）」（1102/1112/1133/… 20 个任务）+ 1102 生命周期计数 35→36，
  根因是并发的 SimpleHunt 串行链路切片正在改 `RetailSimpleHuntDefinitionCompiler`（该文件非本切片改动）。
  例数 1952 → 1955 的 **+3 全部是本切片新增且通过**的门禁：`diff` 证据见下表。

  ```text
  diff <(zcode 09:05 失败方法) <(本切片 09:19 失败方法)  →  IDENTICAL（零新增失败）
  ```
```

## 6. 复现命令

```bash
cd /Users/mc/IdeaProjects/AionEmu-test
grep -rn "SystemGrant" src/main/java                     # 接线前后引用点变化
grep -n "sendDailyQuest" -A 45 src/main/java/com/aionemu/gameserver/model/gameobjects/player/npcFaction/NpcFactions.java
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T1
```

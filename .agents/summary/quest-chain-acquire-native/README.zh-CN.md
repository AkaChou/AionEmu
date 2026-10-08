# DD 链式接取（`acquire=none`）发放面修复（2026-10-08）

> 主题：DataDriven `category_acquire_=none` 任务在 native 车道「已路由但不可接取」的整类缺口。
> 起点 = 用户提问「10033 任务怎么接取」；本轮实现整类修复（27 个可路由后继），含 10033/10034/10035。

## 1. 现象与三层根因

- **现象**：10033（ELYOS，Lv52+，MISSION）无任何接取入口：无接取 NPC、升级/登入不发放、前序 10032 完成后也不发放。
- **① 真端表**：DD 行 `category_acquire_ = none`（无接取键）⇒ `DataDrivenNativeRuntime.acquirePlan` 走
  `default -> AcquirePlan.NONE`，`installInterest` 兴趣面 switch `default -> {}` ⇒ **零接取注册**。
- **② 退役 XML/handler**：原发放边为 `unaccepted→started` on `<level-up/>+<start-eligible/>` 与 on `<zone-mission-end/>`；
  退役 handler（如 `_10033Petrified_Subside.java`，删除批 `2c4a0de78`，2026-08-05）为
  `registerOnLevelUp + defaultOnLvlUpEvent(env, 10032, true)`。XML 于 2026-09-29 （`c947f0373`）退役，
  发放边随之丢失。
- **③ 现存自动通路不覆盖**：typed `LevelUp` 广播 / `QuestDependencyIndex` 只索引 XML 定义；zone-mission-end 广播源
  （原 `10031.xml` 的 `broadcast-zone-mission-end 10032~10035`）随 10031 退役消失；`QuestEngine.onEnterZoneMissionEnd`
  无生产调用方（休眠）。

## 2. 证据域与数据资源

| 证据轴 | 条数 | 落点 |
|---|---|---|
| 真端 `quest.xml finished_quest_condN` | 26 | `source=retail-finished-cond` |
| 退役定义 XML 的 `<prerequisites>` / `finished` 条件 | 2 | `source=retired-xml`（10035/20035） |
| 退役 Java handler 的 `defaultOnLvlUpEvent(env, <前序>, true)` | 6 | `source=retired-handler`（10032/10033/10034/20032/20033/20034） |

- 资源：`src/main/resources/aion/data/static_data/quest/retail/quest_chain_acquire_edges.xml`（+ 同名 `.xsd`），**生成物，禁止手改**。
- 生成器：`build_quest_chain_acquire_edges.py`（`--print` 复核 / `--check` 复算 / 默认重写）；读 DD 表 + retention +
  真端 `quest.xml` + `git show <commit>^:<path>`（两代 handler 路径与两代 XML 路径）。
- 运行期注册面 = 资源 ∩「owned ∧ routed ∧ acquire=none ∧ 非冻结」= **27 行**；
  被过滤 7 行 = XML_RETENTION（10032/10112/10527/20035/20112/20527，属 typed 车道）+ 冻结行 20032（`ACTION_UNFACED`）。

## 3. 实现面（文件 → 行为）

| 文件 | 变更 |
|---|---|
| `tablelane/ChainAcquireEdges.java`（新） | 资源装载 + 形状 fail-closed（重复 successor/自环/空前序/未知 source ⇒ `QUEST_CHAIN_ACQUIRE_TABLE_MALFORMED`）；镜像 `NativeInstanceEntryPort` 装载风格（`instance()` + 包内 `load(InputStream)` 测试缝） |
| `tablelane/DataDrivenNativeRuntime.java` | `create(...)` 末位参数 `ChainAcquireEdges` + 过滤/索引；新增 `onQuestCompleted` / `recheckChainAcquires` / `walkChainAcquires` / `predecessorsComplete` / `blockingRefusal`；`onLevelReached` 尾部追加全表重走；视图 `chainAcquireSuccessors/Edges/ByPredecessor` |
| `tablelane/NativeQuestStartPort.java` | 新方法 `unfinishedConditionsPass(player, questId)`（引用行必须**未** COMPLETE；缺行/解析失败 fail-closed）。`start()/grant()` 判定面**刻意不变** |
| `questEngine/QuestEngine.java` | `onQuestStateChanged`（typed 完成同步唯一出口）追加 best-effort `onQuestCompleted`；方法同时服务非完成同步 ⇒ 运行期以「当前确为 COMPLETE」自守卫 |

**发放行走**：全部前序 `COMPLETE`（跨车道只认状态）→ 后继 `unfinished_quest_cond` 全清 → `NativeQuestStartPort.start(...)`
（等级/种族/职业/性别/限制位/finished 前置/重复上限全轴；`commit()` 自带 `SM_QUEST_ACTION` add + zone/nearby 刷新，
等价 `<sync-quest-state mode="VISIBILITY_REFRESH"/>`）。冻结/未路由行进不了注册面；行走零写、幂等
（`ALREADY_RUNNING`/`REPEAT_LIMIT` 静默，其余阻塞类记 `quest` logger 的 `log.quest_trace.acquire_refused`）。

**触发覆盖**：

| 完成来源 | 路径 | 覆盖面 |
|---|---|---|
| native DD 完成（领奖口） | `QuestService.setFinishingState` → `onLvlUp` → `onLevelReached` 重走 | DD 前置（10031/10033/10034…） |
| typed 车道完成 | `PlayerQuestStateSyncPort` COMPLETION → `onQuestStateChanged` → `onQuestCompleted` | XML 前置（10032/20031/10110/10112/20525…） |
| `//quest set … COMPLETE` / 真实升级 / 登入 | `onLvlUp`/`onLoggedIn` → `onLevelReached` 重走 | 等级/unfinished 补发 + 老存档兜底 |

**为什么不动 `QuestService`**：`setFinishingState:435 → onLvlUp → onLevelReached` 已把 native 完成带进运行时，
再插一次只会造成双走查；依赖关系写在 `onLevelReached` 的注释里防重构误删（门禁 ⑰ 反向锁定）。

## 4. 门禁

- `DataDrivenNativeRuntimeGateTest` ⑫–⑰：注册面 27 行冻结（owned/routed/none/非冻结/无自环）；与真端 `finished_quest_cond`
  逐行对拍（26 条）+ 退役证据 5 行的精确前序表；冻结前置死边闭包 = 恰 {20033,20034}（20032 冻结直杀 20033；
  20034 经 20033 下游不可达）；发放行为四负例
  （前置未完 / unfinished 命中 / 重复触发幂等 / 完成态）+ 正例（`SM_QUEST_ACTION` add 包断言）；等级轴（51 不发、52 补发）；
  触发链锁定（`onLvlUp` → 重走 → 生产单例注册面含 10033）。冻结行排除表补链式面断言。
- `QuestChainAcquireResourceGateTest`（新）：资源 34 行良构 + 证据桶冻结（26/2/6）；后继集合 = 证据推导集
  （独立重解析，多一行/少一行即红）；过滤集 = 6 XML_RETENTION ∪ {20032}；前序 ∈ retention 宇宙；
  5 个 unfinished 行的词法 = `Q<id>` 且在册。
- `NativeQuestStartPortTest#unfinishedConditionsGateOnlyTheChainFace`：unfinished 方向正反例 + 共享口 `start()` 不判该轴（锁定）。
- `RetailTableSchemaGateTest`：表清单加 `quest_chain_acquire_edges`（20 对同名 xml/xsd，两钉子同规）。
- `QuestProductionStartupGateTest`：发放面不塌空断言加链式面。

## 5. 口径决策

1. **整类修复**（27 个可路由后继）而非只修 10033：同族缺口同修（rules：系统性修复优先）。
2. **unfinished 仅链式面强制**：真端列 + 退役 XML `start-conditions` 一致；其它 native 接取面口径不动。
3. 资源承载全 34 行、运行期过滤：未来某行翻 owner（如 10032 转 native）时资源无需重生成，注册面自动跟上。

## 6. 残留（明确不做）

1. **93 行** switch 集 `none` 行（136 行中的其余）仓库内无任何链路证据 ⇒ fail-closed 不发放；
   缺输入 = 客户端解包产物（本机 `<客户端解包根>` 不存在）/ 真端链路表（`<真端根>/Map/XML` 未见）。
2. **20032 冻结**（`ACTION_UNFACED`：creation 2 落点别名真端 idelim/world.xml 内在缺失）⇒ 亚斯莫 20033/20034
   注册为文档化死边（门禁 ⑭ 锁死；解冻 = 补落点别名 + 重生成资源 + 断言翻转）。
3. `20031.xml` 仍广播 `20032 20033 20034 20035`，其中 3 个是 native 无 typed owner 行 ⇒ 每次 20031 领奖记
   AFTER_COMMIT 警告（同类于 2026-09-15 修复）。建议后续单独小改：目标收敛为 `20035`。本批未动。
4. `unfinished_quest_cond` 仅链式面强制；其它 native 接取面（Talk/ItemPlay/EnterWorld/EnterArea/LevelUp 与七族）口径不变（已知差异）。
5. `zone-mission-end` 广播半面不实现（完成即发放已覆盖；`onEnterZoneMissionEnd` 保持休眠）。
6. 未覆盖完成路径：`ClassChangeService`/`GiveStigma` 直写 COMPLETE（登录重走兜底）。
7. 链式发放行不注册 `onQuestStart`（无接取 NPC）⇒ 附近任务提示/标记面缺席；验收时观察客户端任务书表现。
8. `RetailLedgerXml` 类注释「八张自造台账」计数陈旧（现 9 + 本批 1，属批前遗产）——登记，未动。

## 7. 验收状态

- **实现完成**（静态面：IDE 检查 0 error；生成器 `--check` 一致）。
- **单测已通过**（2026-10-08 用户授权，经 IDEA MCP 运行，失败数全 0）：
  `QuestChainAcquireResourceGateTest` 5/5、`DataDrivenNativeRuntimeGateTest` 42/42（含 ⑫–⑰）、
  `NativeQuestStartPortTest` 11/11、`RetailTableSchemaGateTest` 2/2、`QuestProductionStartupGateTest` 2/2、
  `RetailOwnershipGateTest` 5/5；另跑相邻回归 `QuestEngineOpenDoorReplayOrderTest` 1/1、
  `DialogServiceQuestDialogTest` 8/8。
- **首轮三红 → 测试建模修正**（实现未动）：① ⑭ 死边须取闭包（20033 = 直接冻结前置；20034 经 20033 下游不可达）；
  ② ⑮「完成态」须按生产口径建档 `completeCount ≥ 1`（`NativeQuestStartPort.repeatVerdict` =
  `finishedcount < max_repeat_count`；裸 `add(COMPLETE, 0)` 是"预算未用"的可重接口径，非缺陷）；
  ③ ⑯ 等级补发须先把等级字段落地再调 `onLevelReached`（等级轴读模型等级，不读通知参数；生产顺序亦然）。
  夹具新增 `NativeTalkFixture.levelUp(player, level)`。
- **实机 PENDING**：服务端需用户重启；走查清单：Lv52+ ELYOS 完成 10032 ⇒ 10033 入列；10033→10034→10035（54 级）；
  10025/14062 进行中不发放；无 10032 不发放；老存档升级/重登补发。
- **Playbook 决策（quest-repair 规则 13/14）**：本批属新 Pattern（QE-163），但 Playbook 更新只跟随**已验收**的代表案例
  ——待单测 + 实机验收完成后，按「先修复提交、后 Playbook/CASES 提交（引用修复哈希）」两步走，并跑
  `python3 .agents/summary/quest/check_quest_repair_playbook.py`。当前不更新 Playbook。

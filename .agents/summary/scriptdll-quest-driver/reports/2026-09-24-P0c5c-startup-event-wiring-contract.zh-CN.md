# P0c-5c：启动期"事件接线合同"归一 + 真端目录全路径启动门禁（`SYSTEM_GRANT` 未接线修复）

- 日期：2026-09-24
- 归属：真端任务驱动替换目标（P0c 线；承接 P0c-5b 的启动阻断修复）
- 触发：用户实机**第二次**启动失败——
  `IllegalStateException: typed production event is not wired into QuestEngine: SYSTEM_GRANT`
  （`QuestEngine.prepareProductionDefinitions:1993` ← `QuestEngine.load:2141`）。

## 1. 结论

1. **根因**：`prepareProductionDefinitions` 里有一道**内联 instanceof 白名单**（"哪些 typed event 已有投递路径"）。
   P0c 切片引入 `QuestEvent.SystemGrant`（阵营日常 / 区域 / 挑战系统发放行）+ `RetailSystemGrantDispatcher`
   （`NpcFactions` 发放点），但白名单**没同步**，于是真端 overlay 目录一带 `SystemGrant` 边就把服务端挡在启动门外。
2. **为什么测试没抓到（同一教训的第二例）**：白名单在 `ProductionCatalogWhitelistVerificationTest` 里还有**第三份拷贝**，
   而该测试只跑 **XML-owned** 目录（catalog），从不跑 overlay 目录；两份清单都漏了 `SYSTEM_GRANT`，于是双双全绿。
3. **更深的缺口**：**没有任何测试跑过完整的 `prepareProductionDefinitions`**。它此前只能靠
   `Config.dataFile("./data/static_data/quest_definition")` + `DataManager.NPC_DATA` 启动，测试无法注入。
   P0c-5b（14120 缺 `talk_npc1`）与本例（`SYSTEM_GRANT` 未接线）都是这个缺口的产物。
4. **修复**：① 把事件接线清单抽成唯一来源 `QuestProductionEventWiring`（启动路径与测试共用）；
   ② `QuestEngine` 增加**可注入 NPC AI 索引**的重载，使门禁能跑完整启动路径；
   ③ 新增常驻门禁 `QuestProductionStartupGateTest`：对**真端 overlay 后的生产目录**跑完整
   `prepareProductionDefinitions`（交互对象合同 + 事件接线合同 + item-play 索引 + NPC 路由注册），并加"目录必须真的含
   `SystemGrant` 边"的回归锚，防止合同空跑。

## 2. 证据

| 证据 | 内容 |
|---|---|
| 启动日志 | `typed production event is not wired into QuestEngine: SYSTEM_GRANT` @ `prepareProductionDefinitions:1993` |
| 引擎侧清单 | `QuestEngine` 内联链列了 34 个事件类型，**无** `QuestEvent.SystemGrant` |
| 测试侧清单 | `ProductionCatalogWhitelistVerificationTest` 同一份清单的第三份拷贝（同样无 `SystemGrant`） |
| 真端语义 | `QuestEvent.SystemGrant` 由 `RetailSystemGrantDispatcher` 显式分发（`NpcFactions:312` 阵营日常轮换；区域/挑战同源），**不经过事件端口** —— 属于"服务侧投递"，与端口投递事件等价放行 |
| 驱动侧产出 | SimpleTalk（P0c-2）、SimpleHunt（P0c-3/4/6）、SimpleCollectItem（M5-b3x/P1a/P1b）的系统发放行都只产出一条 `SystemGrant` 边 |

## 3. 代码改动

| 文件 | 变化 |
|---|---|
| **新增** `questEngine/runtime/QuestProductionEventWiring.java` | 事件接线清单唯一来源：`isWired(QuestEvent)` / `validateDefinition(CompiledQuestDefinition)` / `validate(Iterable)`，文案与启动期一致；`SystemGrant` 以"服务侧投递"入清单并注明分发点 |
| `questEngine/QuestEngine.java` | 内联 instanceof 链改为 `QuestProductionEventWiring.validateDefinition(...)`；新增 `prepareProductionDefinitions(QuestCatalog, IntFunction<String>)` 可注入重载（生产路径固定 `QuestEngine::productionNpcAi`），无参重载保持原语义 |
| `ProductionCatalogWhitelistVerificationTest.java` | 第三份清单删除，改用 `QuestProductionEventWiring.isWired(...)`（XML-owned 目录继续逐条聚合违规） |
| **新增** `questEngine/QuestProductionStartupGateTest.java` | ①完整启动路径门禁：`engine.prepareProductionDefinitions(真端 overlay 目录, 测试 AI 索引)` 必须不抛；②回归锚：被校验目录必须含 `SystemGrant` 边（`checked > 5000`） |
| 工具 `affected_quest_tests.py` | T1 固定清单新增 `QuestProductionStartupGateTest` |

## 4. 验证

| 档 | 选择器 | 结果 | 日志 |
|---|---|---|---|
| 聚焦（新门禁 + 引擎合成 + XML 白名单） | `QuestProductionStartupGateTest,QuestEngineRuntimeCompositionTest,ProductionCatalogWhitelistVerificationTest` | **16 例 / 0F / 0E / SUCCESS** | `gates/p0c5c-startup1.log` |
| T1 clean（固定全局门禁，11 类） | `affected_quest_tests.py` 的 T1 清单 | **42 例 / 0F / 0E / SUCCESS** | `gates/T1-p0c5c-clean.log` |
| T2（启动面 + 14120/14150 面） | 上述 + `QuestEngineRuntimeCompositionTest` + `QuestProductionStartupGateTest` | **72 例 / 0F / 0E / SUCCESS** | `gates/T2-p0c5c.log` |
| T3 clean（全树） | `com.aionemu.gameserver.questEngine.**` | **1962 例 / 9F / 16E / 1 skipped / 13:53**；相对 P0c-5b 最终版（1958 / 8F / 13E）**新增 4 条失败，全部属并发会话在飞切片**（见下），本切片自身 **0 新失败** | `gates/T3-p0c5c-final-clean.log` |

**T3 新增 4 条失败的归因（逐条有证据，非本切片）：**

| 失败 | 证据 | 归属 |
|---|---|---|
| `P0c8RetentionDiffProbeTest.dumpRetainedRowDiffAxes:154 missing -Dp0c8.out` | 该测试要求系统属性 `-Dp0c8.out`（在飞探针） | 并发会话（本次 T3 新增测试类 `retail/P0c8RetentionDiffProbeTest`、`retail/ZZProbeDDTest`） |
| `Quest1346ClientDialogAlignmentTest.followsTheRetailReportOwnerFromTheFinalKillNode` | `NoSuchFile src/main/resources/.../quests/1346.xml`；该 XML 已被退役（`retail-xml-retention.tsv` = `1346 RETAIL_TABLE OK`），测试仍直读 XML 路径 | 并发会话（同一批退役 1347/1376） |
| `Quest1347ClientDialogAlignmentTest.reportsAtTheLegacyEndNpcInsteadOfTheStartNpc` | 同上（`1347.xml` 已退役） | 并发会话 |
| `Quest1376ClientDialogAlignmentTest.followsTheRetailReportOwnerFromTheFinalKillNode` | 同上（`1376.xml` 已退役） | 并发会话 |

测试类增量核对：`Running ...` 类集 diff = **+3 类** = 本切片 `QuestProductionStartupGateTest`（2 例）
+ 并发会话 `retail/P0c8RetentionDiffProbeTest`、`retail/ZZProbeDDTest`（各 1 例）；无消失类。例数 1958 → 1962 由此解释。

## 5. 未验证 / 边界 / 下一步

1. **仍未重启实测**（进程生命周期由用户管理）：门禁证明"真端目录可以走完整启动准备"，但**建议用户重启一次**确认
   启动日志不再出现 `Can't initialize typed quest engine`。
2. **AI 索引口径**：门禁注入 `QuestInteractionObjectTestData.npcAiResolver`（与生产 `DataManager.NPC_DATA` 同口径：
   只区分 `quest_use_item`）。若 `DataManager.NPC_DATA` 本身加载失败（静态数据问题），门禁不覆盖。
3. **目录来源差异**：生产 `prepareProductionDefinitions()` 无参版本读 `Config.dataFile("./data/static_data/quest_definition")`，
   门禁读 classpath 视图（`ProductionQuestDefinitions`）。两者内容一致性沿用既有口径（部署时 resource 与运行目录同源）。
4. **P0c-5b 与本切片是同一模式的两例**：启动期合同（交互对象 / 事件接线）此前都无门禁。现已**两道都进 T1**；
   后续再往 `prepareProductionDefinitions` 加校验时，必须同批给门禁，否则同类问题会以第三种形态再现。
5. 队列不变：**P0c-6 余量**（SimpleHunt `SEMANTIC_GAP:DIALOG_ROUTE`）→ **M3 SimpleTalk** → **P5 DataDriven**。

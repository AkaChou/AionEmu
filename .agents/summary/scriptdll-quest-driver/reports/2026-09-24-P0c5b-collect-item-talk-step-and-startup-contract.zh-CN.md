# P0c-5b：SimpleCollectItem `talk_npc1` 简报步骤接线（服务端启动阻断修复 + 启动期合同门禁）

- 日期：2026-09-24
- 归属：真端任务驱动替换目标（P0c 线；修的是 M5-b/P1b 批次遗留的真实回归）
- 触发：用户提供的服务端启动日志——
  `IllegalStateException: quest 14120 quest_use_item catalog drop npc 700157 item 182215478 collecting step 1
  has no matching START ACTION_ITEM_USE eligibility route`（`QuestInteractionObjectValidator.validateCatalogDrops:89`
  ← `QuestEngine.prepareProductionDefinitions:1942` ← `QuestEngine.load:2141`）。

## 1. 结论

1. **根因**：14120/14150 属 SimpleCollectItem 退役批（`RETAIL_TABLE`）。真端行声明中间 NPC
   （`talk_npc1` = `Tree_Move_Demro` / `Ibelia`）与掉落生效步（`quest.xml` 的 `collect_progress=1`），
   但合成器此前**没有消费 `talk_npc1`**：采集对象与交付路由全部挂在 `started`（`var0=0`），
   而 drops 的 `collectingStep=1` → 启动期校验器找不到同 `var0` 的 `ACTION_ITEM_USE` 路由 → 直接抛异常。
2. **影响面**：全量探针（6191 个可执行定义逐个跑 `QuestInteractionObjectValidator`）显示**只此 14120 一例**
   失败；14150 同样缺步但在对象 AI（`LF3_FOBJ_Q14150`）路径上未触发该校验器，属同因不同表现。
3. **修复**：补真端字段（表 + 解析）→ 合成器新增采集行节点 `v{collect_progress}`，并**按客户端简报页链登记表**
   （`quest_client_briefing_chains.tsv`）接线 `started --QUEST_SELECT→ 入口页 --各按钮跳页→ 末按钮(SETPRO)→ v1`；
   采集对象 `TalkToNpc/CanAct`、报告页 `QUEST_SELECT`、交付检查对（39/20002）、报告 NPC 关窗出口全部改挂采集行。
4. **方法论缺口**：`QuestInteractionObjectValidator` 是**启动期合同**（`QuestEngine.prepareProductionDefinitions` 调用），
   此前**没有任何测试覆盖** —— T3 全树 1952 例全绿的同时服务端起不来。本次新增常驻门禁把它纳入 T1。
5. **反假绿**：族门禁夹具此前调用不带简报步骤的重载，测的是**与生产不同的形状**（所以第一次修完 T1 仍绿——
   其实是对的，但夹具本身永远看不到该步骤）。现在把该重载提升为唯一生产入口（内部解析 `talk_npc1`），
   删除便利重载，并新增"夹具指纹 == 生产 overlay 指纹"的逐行对拍断言。
6. **客户端契约**：第一版硬编码 `QUEST_SELECT→SELECT2` + `SETPRO1→v1` 会丢链上的 `SELECT2_1` 跳页，
   `QuestClientContractGateTest` 报 2 例 `BUTTON_WITHOUT_ROUTE`；改为消费客户端页链登记后归零
   （基线表为空表，即零容忍）。

## 2. 真端 / 客户端证据

| 证据 | 内容 |
|---|---|
| 真端表 `Quest_SimpleCollectItem.xml` | 262 行中 5 行带 `talk_npc1`（9620 Lostes / 14150 Ibelia / 14120 Tree_Move_Demro / 9655 Kinesos / 9656 Kinesos）；宇宙内仅 14120、14150 被驱动 |
| 真端表多中间 NPC | 9620（`talk_npc1..3`）、9656（`talk_npc1..2`）——均**不在本服宇宙内**，仍留 XML |
| 真端 `quest.xml` 元数据 | `<id>14120</id> … <collect_progress>1</collect_progress>`、`<drop_monster_1>LF2_Cherubim_Basket`、`<drop_item_1>quest_14120a`；=`RetailQuestMetadataCompiler` 的 `metadata.drops().collectingStep()` |
| NPC 名索引 | `Tree_Move_Demro → 730020`、`Ibelia → 204582`（唯一解析） |
| 客户端页链登记 `quest_client_briefing_chains.tsv` | `14120  1352  1353:1353 10000:0`；`14150  1352  1353:1353 1438:1438 10000:0`（入口页 + 按钮跳页 + 终结点） |
| 客户端动作 id | `HACTION_SELECT2_1=1353`、`HACTION_SELECT2_1_1=1438`、`HACTION_SETPRO1=10000`（`QuestDialogAction` 同号） |
| 退役前 XML（git HEAD `14120.xml`） | `started --QUEST_SELECT(730020)--> SELECT2`、`started --SELECT2_1(730020)--> SELECT2_1`、`started --SETPRO1(730020)--> v1`，对象/交付全在 `v1` —— 与本次合成形状一致 |

## 3. 代码改动

| 文件 | 变化 |
|---|---|
| `retail/RetailSimpleCollectItemTable.java` | `Entry` 增列 `talkNpc`（真端 `talk_npc1`）+ 6 参兼容构造；`parseEntry` 读该列 |
| `retail/RetailSimpleCollectItemDefinitionCompiler.java` | 收口为**唯一生产入口** `compile(entry, npcIndex, metadata, exits, summaryRows, clientRewardNpcs, briefingChains)`（删除 3/4/5 参便利重载与旧 6 参重载）；内部 `BriefingStep` 解析 `talk_npc1` + 客户端页链；新增稳定码 `RETAIL_TALK_NPC_UNRESOLVED` / `RETAIL_TALK_NPC_AMBIGUOUS` / `RETAIL_BRIEFING_CHAIN_UNREGISTERED` / `RETAIL_TALK_NPC_WITHOUT_COLLECT_STEP`；`build` 新增 `v{collect_progress}` 节点与 `briefingFlow(...)`（入口页 + 每跳 + 终跳清标志位/关窗推进入采集行）；对象/报告/交付路由改挂 `collectSource` |
| `retail/RetailQuestDriver.java` | `compileSimpleCollectItem` 改调唯一入口并传 `clientBriefingChains`（本地重复解析删除） |
| 测试新增 `runtime/QuestInteractionObjectContractGateTest.java` | ①**全量启动期合同**：`ProductionQuestDefinitions` 的每个可执行定义逐个跑 `QuestInteractionObjectValidator`（含 NPC AI 解析，聚合失败上报，要求 `checked>5000` 且 0 失败）；②锁 14120/14150 中间步骤形状（`SETPRO1→v1` + `ACTION_ITEM_USE` 落在 `var0=1`） |
| 测试 `retail/RetailSimpleCollectItemGateTest.java` | 夹具改走唯一入口并加载页链；新增 `fixtureMatchesTheProductionDriver`（178 行中 `RETAIL_TABLE` 的 175 行逐行指纹对拍）；`inspect` 新增"简报链逐跳已接线"与 `v{n}` 节点投影检查 |
| 数据 `retail-simple-collect-item-ir-fingerprints.tsv` | 用 `-Dretail.collect.fpOut=` 生成器重算（**仅 14120/14150 两行**变化，见 §5） |
| 工具 `affected_quest_tests.py` | T1 固定清单新增 `QuestInteractionObjectContractGateTest` |

## 4. 漂移裁定（真端优先）

1. **14120/14150 形状变化**：`started` 单行 → `started --简报链--> v1`；采集对象/交付路由迁移到 `v1`。
   判据是真端 `talk_npc1` + `collect_progress`（不是"与旧 XML 等价"），且旧 XML 形状相同（§2 末行）。
2. **对象/交付挂载点变化**：`started`(var0=0) → `v{step}`(var0=1) 属真端语义要求；旧 XML 亦如此。
3. **漂移登记表**：`-Dretail.collect.equivOut=` 导出与在仓 `retail-simple-collect-item-drift.tsv` **逐字节一致**
   （退役行按冻结登记、非退役行现算），无需重算。
4. **客户端链条不得再硬编码**：`QUEST_SELECT→SELECT2` 只是 14120 链的第一跳；接链必须走登记表，
   否则第 2 跳按钮无路由（本次已由契约门禁抓到并按登记表修正）。

## 5. 对拍结果（冻结指纹）

| quest | P1b 基线（退役时） | 首版（硬编码两跳） | 终版（客户端页链） |
|---|---|---|---|
| 14120 | `4cca0e88…` 4 节点 / 28 边 | `f2b73102…` 5 / 30 | `f8d4740f…` **5 / 31** |
| 14150 | `0f7f0637…` 4 节点 / 36 边 | `bef15c1c…` 5 / 38 | `514a79bf…` **5 / 40** |

（其余 173 行指纹不变；文件为生成物，重算命令 `-Dretail.collect.fpOut=`，禁手改。）

## 6. 验证

| 档 | 选择器 | 结果 | 日志 |
|---|---|---|---|
| 聚焦（启动合同 + 族 + 客户端契约） | `QuestInteractionObjectContractGateTest,RetailSimpleCollectItemGateTest,QuestClientContractGateTest` | **9 例 / 0F / 0E / SUCCESS** | `gates/p0c5b-focus6.log` |
| T1 clean（固定全局门禁，最终版本） | `affected_quest_tests.py` 的 T1 清单（10 类） | **40 例 / 0F / 0E / SUCCESS** | `gates/T1-p0c5b3-clean.log`（首轮 `T1-p0c5b2-clean.log` 同结果） |
| T2（命中 14120/14150 的测试类，最终版本） | T1 + `QuestDialogOrderAuditTest`（11 类） | **57 例 / 0F / 0E / SUCCESS** | `gates/T2-p0c5b3-14120-14150.log` |
| T3 clean（全树，唯一"零新增失败"证据，最终版本） | `com.aionemu.gameserver.questEngine.**` | **1958 例 / 8F / 13E / 1 skipped / 8:02**；失败集合与基线 `T3-p0c5-clean.log`（1952 例 / 8F / 13E）**逐条 diff 完全相同（21 条）** → 零新增失败 | `gates/T3-p0c5b-final-clean.log` |
| 退役对账 | `python3 -B .agents/summary/scriptdll-quest-driver/verify_retirement.py` | `catalog=3448 directory=3448 retired=2776 sum=6224` / **OK：目录一致，无悬空生产引用** | 直接执行输出 |
| 全量启动期合同探针 | 生产视图 6191 个可执行定义 | **0 失败**（`PRODUCTION_INTERACTION_OBJECT_FAILURES=0`） | 门禁内 + `ProductionCatalogWhitelistVerificationTest` |

## 7. 未验证 / 边界 / 下一步

1. **未重启服务端实测**：本切片只证明启动期合同（`prepareProductionDefinitions`）满足与 IR 形状正确；
   实机"服务端能起 + 14120 可玩通"需用户重启验证（进程生命周期由用户管理，本线程不启停服务）。
2. **多中间 NPC 形态未驱动**：真端表另有 `talk_npc2/3` 行（9620/9656），均不在本服宇宙内，仍留 XML。
3. **`metadata.drops` 的 base→live 变体闭包**仍未展开（Draupnir 面，另一会话在改），与本切片正交。
4. **P0c-6 队列**（SimpleHunt 网格族 `talk_npc1` 余量 453 行 `SEMANTIC_GAP:DIALOG_ROUTE`）不受本次影响，
   但**接线口径应统一到 `RetailClientBriefingChains`**（网格族已在用），避免再出现"硬编码一跳"的返工。
5. T3 clean 与 `gates/T3-p0c5-clean.log`（1952 例 / 8F / 13E）逐条对账结果：**失败集合逐条相同（21 条：8F + 13E），零新增失败**；
   总例数 1952 → **1958**（+3 本切片：合同门禁 2 + 族门禁 1；+3 属 P0c-6 切片，其测试晚于 P0c-5 基线日志）。
   21 条既有失败中无一条与本族/14120/14150 相关（`EarlyElyosQuestRegressionTest`、`QuestMultistepChainContractTest`、
   `QuestDraupnirNpcVariantContractTest` 等为并发会话在改的族）。
6. **实机复验口径**：新增的启动期合同门禁覆盖 `QuestEngine.prepareProductionDefinitions` 的全部 6191 个可执行定义，
   因此"服务端起不来"这一类（合同不满足）不会再静默通过；但**仍建议用户重启一次**确认启动日志无 `Can't initialize typed quest engine`。

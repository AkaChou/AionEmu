# 80787 无法接取 / 对话 html load fail：接取入口页客户端契约修复


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 状态：代码落地完成 + 隔离副本全树验证通过（见 §6）。
> 报障：任务 80787「[活动] 撑住，新手！这是圆滚滚的礼物！」NPC 看不到、无法接取；接取时客户端 `对话 html load fail`。

## 1. 两个独立缺陷

| # | 现象 | 根因 | 处置 |
|---|---|---|---|
| 1 | NPC 不刷（看不到） | `src/main/resources/aion/config/main/events.properties:82` `gameserver.event.service.enable = false`（来自 `74b37ea0d chore(config): disable event service and crazy daeva`，2026-09-22）。闸门在 `GameEventRuntimeGateway`（`EventService.checkEvents()` → `EventTemplate.Start()` 才刷怪）。80787 家族 NPC（833671-833674 等 16 点）唯一刷出来源是 `events_config.xml` 的 `Homeward Bound Event`（`<maintainable>80787;...;80794</maintainable>`），静态 spawns 目录零命中 | 工作区该开关已是 `true`（未提交）；**需重启/`//reload events` 才生效**（服务器生命周期由用户管理） |
| 2 | 接取时 `对话 html load fail` | 迁移后合成器把未接态接取边硬编码成真端接取窗页 4（`RetailSimpleHuntDefinitionCompiler.canonicalAcceptFlow`，953-990 行），但 5.8 客户端按任务页 HTML 解析该页：80787 家族任务页只有 `select_none`(4762)，**没有** `ask_quest_accept`(4) ⇒ 客户端加载不到页 4 | 本次修复：`RetailClientAcceptEntryPage` + `RetailQuestDriver.compile` overlay 修复层，见 §2 |

迁移前 XML 见证（`git show 4ede058c0^:.../quests/80787.xml`）：未接态 `QUEST_SELECT(31)` 发 `SHOW_QUEST_PAGE(SELECT_NONE)`（4762），`ASK_QUEST_ACCEPT(1007)` → 页 4；重建后的规范形把 1007 中转删了，同时把页 4 挪到了 31 边上 —— 页 4 在客户端只能由 1007 那条链到达，直接下发即 load fail。

## 2. 修复规则（客户端页契约）

客户端页索引：`src/main/resources/aion/definitions/quest_dialog/client_dialog_contract.tsv`（由 5.8 客户端任务页 HTML 生成；80787 的 `select_none` 页在 HTML 里声明 `HACTION_QUEST_ACCEPT_SIMPLE`=20000 / `HACTION_QUEST_REFUSE_SIMPLE`=20001）。

未接态第一屏取页优先级：**客户端声明 4 → 4；否则 4762(select_none)；否则 1011(select1)；都没有 → 保持合成器页 4**。

对拍证据（`.agents/summary/quest-80787-event-npc-missing/crosscheck_accept_entry.py` 输出
`crosscheck-accept-entry.txt`）：用 153 条有迁移前 XML 见证的 ADOPTED 行对拍 —— `[4,4762,1011]` = agree 153 / disagree 0；`[4,1011,4762]` = 137/16；1044 条无 XML 见证（不动其规则，仅按客户端页契约改页）。

改动面（`.agents/summary/quest-80787-event-npc-missing/accept-page-vs-client.txt`）：
- 真端 owner 行 4983（`retail-xml-retention.tsv` owner=RETAIL_TABLE）；
- 客户端任务页缺页 4 = 2477，其中**可依客户端页修复 = 1872**（声明 4762 或 1011）；
- 其余 605 行任务页连 4/4762/1011 都没有（583 行客户端页索引完全缺登记，多为 5000-6581 段）⇒ 无据可改，保持合成器页并进冻结缺口表。

## 3. 代码改动

| 文件 | 作用 |
|---|---|
| `src/main/java/.../questEngine/retail/RetailClientAcceptEntryPage.java`（新） | 规则 + 修复：`entryPage(questId, contract)`；`repair(compiled, contract)` 只改**接取入口边**（未接态 NONE 投影，或重复任务的再开局别名 = COMPLETE 投影 + StartEligible）且事件为 `QUEST_SELECT(31)`/`UseItem`、afterCommit 含 `ShowQuestDialog(4)` 的边：把页换成客户端声明页，其余条件/动作/目标/顺序原样保留 |
| `src/main/java/.../questEngine/retail/RetailQuestDriver.java` | `compile(int)` = 原 family 分派（改名 `compileByFamily`）+ 入口页修复；`definition(int)` 增加契约快照比对，热重载换了契约实例就清逐任务缓存重算页 |

再开局别名（重复任务）：迁移前 XML 见证 15478（`complete` 边发 `SELECT_NONE_1` 4763）、15205（`complete` 边发 `SELECT1` 1011）—— 再开局同样走信页阶梯，故别名边一并按入口页修复（否则接了第一次、重复接取仍 load fail）。

## 4. 门禁与测试改动

- 新门禁 `RetailClientAcceptEntryPageTest`：① 规则逐 id 落页（80787→4762、80324→4、未登记→4）；② 生产视图里**每个真端 owner 行**的接取入口边下发的页都必须在客户端任务页里；缺口写进冻结表 `src/test/resources/quest/retail-accept-entry-page-gaps.tsv`（新增/消失都判红，重生成 `-Dretail.acceptEntryPage.gapOut=<path>`）。
- 新断言夹具 `src/test/java/.../definition/ClientAcceptEntryPageAssertions.java`：把对齐测试里硬编码的页 4 换成"客户端声明页"，并校验该页确有客户端声明。
- 逐任务对齐测试改断言（原硬编码页 4 → 契约页）：1919、18950、19637、21320、25512、28950、26800、2929、30515、16922、18800、18802、51009、50009、13902、23902、26922、28800、28802、13305、2150、2151、17160/17161/27160/27161（`QuestLegacyMonsterHuntProductionFlowTest`）、`GrowthQuestDialogPageAlignmentTest`（19671/19683/29671 + Abbey 击杀行）、`Quest80787To80794RetailAlignmentTest`。
- 黑盒 e2e `RetailQuestContractTest` 不需改：它按客户端页表走可见动作，`select_none(4762)`/`select1(1011)` 上声明了 `HACTION_QUEST_ACCEPT_SIMPLE`(20000)，与定义里的 20000 提交边对齐。

## 5. 证据文件（本目录）

- `accept-page-vs-client.txt`、`audit_accept_page_vs_client.py`：ADOPTED 面缺页审计。
- `crosscheck_accept_entry.py` / `crosscheck-accept-entry.txt`：规则 × 迁移前 XML 对拍（153/0）。
- `audit_active_events.py`、`report_expired_events.py`、`expired_events_zh.py`、`expired-events-zh.txt`、`events-chs-candidates.txt`、`map_events_to_chs.py`、`search_chs_event_strings.py`：活动名单审计与中文名映射（71 个过期活动 / 当前窗口内 10 个长期活动）。
- 客户端页权威证据：`<客户端解包根>/data_unpacked/Dialogs/80000-84999/quest_q80787.html`（`select_none` 页含 20000/20001 按钮）。

## 6. 验证结果与未验证项

**已验证（2026-09-28，授权后执行）**

1. 编译：`mvn -o ... test` 主编译 4768 文件 + 测试编译 1149 文件通过（唯一编译修复：`Quest80787To80794RetailAlignmentTest` 补 import、门禁 lambda 捕获 final 变量）。
2. 冻结缺口表：`-Dretail.acceptEntryPage.gapOut` 重生成 → **583 行**，与"客户端页索引完全缺登记的真端 owner 行"集合逐 id 相同（差值 0），已落 `src/test/resources/quest/retail-accept-entry-page-gaps.tsv`。
3. 聚焦门（真实工作区）：`RetailClientAcceptEntryPageTest` 2/2、`RetailSimpleCollectItemGateTest` 6/6、`RetailQuestDriverOverlayTest` 5/5、`RetailTsvManifestGateTest` 3/3、`Quest80787To80794RetailAlignmentTest` 2/2、`QuestClientContractGateTest` 1/1、`RetailQuestContractTest`（黑盒 e2e）1/1 → **20/20 绿**。
4. 广域（隔离副本，避开并行会话 Maven 对同一 `target/` 的写入）：`mvn -o -Dtest='com.aionemu.gameserver.questEngine.**.*Test' test` → 2020 例、红集 **129 条**，与并行会话已登记基线 `gates/p4d-T3-invocation-reds.txt` **逐条相同（extra=0 / missing=0）**。
5. 过程中确证并修复的唯一新增红：`RetailSimpleCollectItemGateTest.fixtureMatchesTheProductionDriver`（夹具 vs 生产指纹对照）——按该门禁语义在夹具侧套用同一 overlay 层后归零。

**未验证（PENDING）**

1. 服务器重启 / `//reload events`（用户管理）后的真端与真客户端验收：80787 家族任务可接取、页面正常；重复任务族（如 15476/15478）再开局无 load fail。
2. 活动服务开关（缺陷 1）：工作区 `events.properties:82` 已是 `true`（未提交），需重启或 `//reload events` 生效；71 个过期活动的裁剪口径未提交。
3. Playbook/CASES 未更新：按 quest-repair 规则 11/13，待客户端验收（`CLIENT_ACCEPTED`）后再决定是否登记代表案例。

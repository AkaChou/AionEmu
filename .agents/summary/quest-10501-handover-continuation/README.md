# 任务 10501「被毁的遗迹」上交证物后必须重新对话（2026-09-19）

- 状态: **实现完成、静态验证通过 / 客户端验收通过（CLIENT_ACCEPTED，2026-09-19）** —— 用户 2026-09-19 确认
  “客户端验证成功”（整任务，未限定分支或步骤）；聚焦测试、客户端契约门禁与生产目录门禁见第八节与第十节 10.3；
  验收记录见 `../quest-acceptance/10501-2026-09-19-client-accepted.md`。
- 交付批次: 修复提交 `75312dcdc`（42 个 quest XML + 5 个测试类）；Playbook 案例 8.39、本文档与验收记录随
  第二个文档提交落地（按规则 14/16 两次本地提交，不 push）。
- 范围: `src/main/resources/aion/data/static_data/quest_definition/quests/10501.xml`、
  `src/test/java/com/aionemu/gameserver/questEngine/definition/Quest10501HandoverContinuationTest.java`；
  同形批处理另含 41 个任务 XML 与 3 个测试类（见第十节 10.2）。

## 一、报告与复现

- 玩家侧: 接过龙族证物放置器（182215598 使用三次，var0 0→6）后与 NPC 804700 对话，
  在「拿出证物」页（3057）点提交，服务端扣除龙族证物 182215599、任务进入 REWARD（`状态=4 步数=7`），
  但对话停在「递过证物」页，点击后没有任何下一步；再次与 804700 对话才收到 `下发页=10002`，
  再由 1009 打开奖励窗完成。用户原话：“点交出证物后没有进行下一步，重新对话才将任务完成”。
- 2026-09-19 09:45:01–09:45:12 实机 trace（用户提供）:
  - `CM_DIALOG_SELECT 上一页=3057 动作=39` → `SM_QUEST_ACTION 10501 状态=4 步数=7` → `SM_DIALOG_WINDOW 下发页=10000`
  - 随后没有 `CM_DIALOG_SELECT ... 动作=1008`；3 秒后才出现 `下发页=10002`（重新打开对话）
  - `动作=1009` → `下发页=5`（奖励窗） → `动作=8` → `状态=5` 完成。

## 二、客户端证据

`docs/quest/client-dialog-mapping/quest-dialog-action-details.csv` 中任务 10501 自身 HTML 的按钮：

| 页 | 常量 | 动作 | 中文按钮 |
| --- | --- | --- | --- |
| 10000 | `HTML_PAGE_CHECK_USER_ITEM_OK` | `HACTION_FINISH_DIALOG(1008)` | 递过证物。 |
| 10001 | `HTML_PAGE_CHECK_USER_ITEM_FAIL` | `HACTION_FINISH_DIALOG(1008)` | 结束对话。 |
| 10002 | `HTML_PAGE_DEFAULT_SUCCESS` | `HACTION_SELECT_QUEST_REWARD(1009)` | 报告之前发生的事情。 |
| 5 | `HTML_PAGE_SHOW_SELECT_QUEST_REWARD_WINDOW1` | 无按钮（服务端奖励窗） | — |

结论: 客户端 `HACTION_FINISH_DIALOG` 按钮是**本地关闭**（点击后只关闭窗口，
服务端收到的是不带任务动作的 `CM_CLOSE_DIALOG`），因此任何“下发 10000 后等客户端回传 1008”的服务端
续接都不可能成功；10000 页只能作为对话终点。任务 10501 的成功链必须由服务端把玩家带到 10002。

## 三、根因

XML 的 `s6 -> reward`（`39` + `var0=6` + `has-item 182215599`）成功分支在状态同步后下发
`CHECK_USER_ITEM_OK(10000)`，把对话停在只有本地关闭按钮的页面上；任务此时已经进入 REWARD，
玩家必须重新对话才能由 REWARD 态入口页（10002）继续领奖。旧 handler
（`origin/history` `_10501Research_The_Ruins.java`：`checkQuestItems(env, 6, 7, true, 10000, 10001)` +
REWARD 态 `sendQuestEndDialog`）同样是“确认页后重新对话”，但客户端已把该确认页做成纯本地关闭，
所以迁移后表现为“按钮无效 + 必须再对话”。

## 四、修复

`s6 -> reward` 成功分支的 after-commit 由 `CHECK_USER_ITEM_OK` 改为 `DEFAULT_SUCCESS(10002)`：

```xml
<after-commit>
  <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
  <dialog type="SHOW_QUEST_PAGE" page="DEFAULT_SUCCESS"/>
</after-commit>
```

- 保持 `STATE_SYNC_BEFORE_DEPENDENT_PAGE` 顺序（先状态同步，再下发依赖页）。
- 物品不足分支不变: `s6` + `39` + `var0=6` → `CHECK_USER_ITEM_FAIL(10001)`；
  该页关闭按钮的落点 `s6 -> s6 FINISH_DIALOG(1008) -> SHOW_SELECTION_PAGE(SELECT_QUEST)` 保留不动。
- REWARD 态入口（`reward` + `USE_OBJECT` → 10002）与 `1009 → 5` 奖励窗路由不变，
  上交成功后落的页与 REWARD 态入口页**完全一致**，因此不再需要重新对话。
- `f07723711` 曾为 10000 页补过 `reward -> reward FINISH_DIALOG(1008) -> close-dialog` 兼容路由；
  该路由现在不再由客户端页面触发，保留作为兼容关闭（如需清理应单独评估）。

## 五、回归测试

`Quest10501HandoverContinuationTest`（新增，未执行）锁定：

1. `s6 -> reward` 的 conditions（`var0=6`、`has-item 182215599`）、actions（remove-item + `var0=7`）、
   after-commit 顺序与 `DEFAULT_SUCCESS` 页；
2. 任务 10501 全文不再下发 `CHECK_USER_ITEM_OK(10000)`；
3. 物品不足分支仍下发 `CHECK_USER_ITEM_FAIL(10001)`，且 `s6` 的 1008 关闭落点仍是 `SELECT_QUEST`；
4. 上交成功所落的页 = REWARD 态入口页，`1009` 打开奖励窗 1；
5. 六条可选奖励分支: 三条固定奖励（EXP 22916836、186000231×25、186000237×60）+ 本条可选奖励
   + `CompleteQuest(0)`，after-commit 为 `RefreshPlayerStats → SyncQuestState(COMPLETION) →
   ShowQuestSelectionDialog(SELECT_QUEST)`；
6. 元数据合同: `<items>` = 182215599、`<work-items>` = 182215598、掉落 `236251 → 182215599`（100%、组内每人、collecting-step=6）。

## 六、同类清单（家族审计）

### 6.1 客户端层面（372 个任务）

`audit_finish_page_family.py` + `client-finish-page-family.csv` 枚举全部下发 `CHECK_USER_ITEM_OK(10000)`
的任务，并按任务自身客户端 HTML 中该页按钮的动作分类：

| 分类 | 数量 | 含义 |
| --- | --- | --- |
| `SELF_CONTAINED_REWARD_ACTION` | 238 | 10000 页按钮为 1009，页面自身可继续（含 8.25 代表任务 15010/16942/18745） |
| `CLIENT_LOCAL_CLOSE` | 96 | 10000 页按钮为 1008（本地关闭）= 本次 10501 的死端形态（含 10032、10504、1636、2372 等） |
| `STORY_PAGE_TURN` | 38 | 10000 页按钮为故事翻页动作（1693/2034/1352…），需要先补链根 |

### 6.2 编译 IR 层面（122 个任务）

`HandoverFixPlan.java` 用编译后的 IR 复算（原始输出见 `family-ir-probe-before-fix.txt`）：
客户端 10000 页只有 1008 按钮的任务共 **122** 个，其中 96 个在 XML 里确实下发 10000，
另 26 个从未下发（既有 `CLIENT_PAGE_UNREACHED` 记录）。这 96 个再按“目标节点是否已存在同一 NPC
的对话续接页”分类：

| 分类 | 数量 | 处置 |
| --- | --- | --- |
| 同 NPC 有续接页（`AUTO`） | 41 个任务 / 42 条分支 | 本批直接下发续接页，清单见 `handover-fix-plan.tsv` |
| 无同 NPC 续接页（`REVIEW`） | 55 个任务 | 该 NPC 对话在此结束（下一步在别的 NPC/区域/道具），确认页保持终端页 |

- 40 个任务的续接页是奖励窗 `5`（`SHOW_SELECT_QUEST_REWARD_WINDOW1`）；10504 是 `10002`（`DEFAULT_SUCCESS`，与 10501 同形）。
- 与 Playbook 8.25（`CHECK_CONFIRMATION_PAGE_CONTRACT`）的关系：8.25 的代表任务 2372 属于本批 `AUTO`
  集合，本批把它改为“交付后直接进奖励窗”；1636 属于 `REVIEW` 集合，保持原样。
  该取舍已按用户“同样问题一起改”的指示执行，客户端验收后需回到 Playbook 重新登记 8.25 的复用边界。

## 七、审计影响（已实测）

2026-09-19 用 `QuestDialogOrderAudit` 打印任务 10501 的审计行（原始输出见
`quest-10501-audit-rows-after-fix.txt`，探针在 `/tmp` 编译运行，未写入仓库）：

- `page=10000` → `CLIENT_PAGE_UNREACHED`（10000 不再被下发；该类记录在门禁中非致命，
  10501 原本已有 1013/1695/1779/1864 等同类记录）；
- `s6 + 804700 + 39 -> reward + page 10002` → `PAGE_ACTION_MATCHED`（按钮 1009 有路由）；
- `reward + 804700 + -1 -> reward + page 10002` → `PAGE_ACTION_MATCHED`（REWARD 态入口不变）；
- `s6 + 804700 + 39 -> s6 + page 10001` → `PAGE_ACTION_MATCHED`（失败页 1008 关闭落点仍在）；
- `reward + 804700 + 1009 -> terminal page 5` → `TERMINAL_PAGE_REACHED`。

修复前（`quest-order-audit.csv` 2026-09-12 快照）10501 的 10000 页曾记录
“visible client action has no route”（奖励态 1008 无路由）的 `EVIDENCE_REQUIRED` 行；该行随 10000 页不再下发消失。

`QuestClientContractGateTest` 的致命指纹只有 `PAGE_NOT_IN_TASK_HTML` 与 `BUTTON_WITHOUT_ROUTE`；
在 `-Dquest.client.contract.failOnStaleBaseline=true` 严格模式下门禁通过，说明既没有新增致命缺口，
也没有需要清理的基线残留（`src/test/resources/quest/quest-client-contract-baseline.tsv` 当前为空，未改动）。

## 八、执行结果与剩余验证

已执行（2026-09-19，均在仓库根目录；日志见本目录）：

```bash
mvn -B -Dtest=Quest10501HandoverContinuationTest test
# 2/2 通过（mvn-quest-10501-focused-2026-09-19.log）

mvn -B -Dquest.client.contract.failOnStaleBaseline=true \
  -Dtest='QuestClientContractGateTest,QuestDialogOrderAuditTest,QuestPageButtonAuditTest,QuestItemSourceContractGateTest,AcceptAndConfirmationEntryContractTest' test
# 25/25 通过（mvn-quest-client-gates-2026-09-19.log）

mvn -B -Dtest='ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest' test
# 11/11 通过；PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0
# （mvn-production-catalog-2026-09-19.log）
```

另外执行了只读校验：`xmllint --noout [--schema quest_definition.xsd] quests/10501.xml` 通过。

执行状态（2026-09-19 收尾）：

1. Aion 5.8 客户端复测：**已由用户确认通过**（“客户端验证成功”），见第十一节与验收记录。
2. 全量 `mvn -B test`：已执行（3473 用例 / 0 failures / 2 errors / 2 skipped，两条 error 属并行任务，见 10.3）。
3. 同形批处理已完成（见第十节）：41 个可续接任务改为直接下发续接页，55 个无同 NPC 续接页的任务保持终端确认页。

客户端复测预期序列（Aion 5.8 客户端；用户已确认通过，服务端 trace 未回流为稳定附件）:

```text
CM_DIALOG_SELECT 上一页=3057 动作=39
SM_QUEST_ACTION 10501 状态=4 步数=7
SM_DIALOG_WINDOW questId=10501 下发页=10002      # 不再出现 10000
CM_DIALOG_SELECT 上一页=10002 动作=1009
SM_DIALOG_WINDOW questId=10501 下发页=5
CM_DIALOG_SELECT 上一页=5 动作=8/9/...（或 23）
SM_QUEST_ACTION 10501 状态=5；10502 状态=3
```

## 八·补、文档与记忆库边界（客户端验收后更新）

- Playbook: 客户端验收完成（2026-09-19）后新增案例 8.39（Pattern `HANDOVER_CHECK_PAGE_LOCAL_CLOSE_CONTINUATION`，
  代表提交 `75312dcdc`），并更新 8.25 `CHECK_CONFIRMATION_PAGE_CONTRACT` 的复用边界（ok 页只有
  `HACTION_FINISH_DIALOG(1008)` 时不属于 8.25，必须走 8.39 的直接续接合同）。
- 交叉引用修复: 并行任务把 `Quest14123ZoneSpawnTest#spawnsPeddlerOnBothAcceptanceRoutes` 重命名为
  `spawnsPeddlerOnDialogAcceptance`，使已提交的 `PRE_KILL_QUEST_NPC_ZONE_SPAWN_AND_REENTRY` 指纹引用失效；
  本批次按最小改动更新该引用，使 `check_quest_repair_playbook.py` 恢复绿（该案例正文的语义更新归其所属任务）。
- Memory Bank: 已新增 `QE-041`（`.agents/memory-bank/patterns/quest-engine.md`：`HACTION_FINISH_DIALOG(1008)`
  是客户端本地关闭，不能承载服务端续接），并已运行 `sync_memory_bank.py` 与 `check_memory_bank.py`。
  该目录同时载有并行任务的 `QE-039`/`QE-040` 条目，文档提交时整文件落地以保持路由与症状索引一致。

## 九、剩余风险

- 客户端 1008 行为结论来自用户实机 trace（没有 `CM_DIALOG_SELECT 1008`）+ 10501 页面证据；
  如果后续在真实客户端观察到 1008 会被回传，应重新评估 8.25 家族与本节结论。
- 该修复使玩家不再看到「递过证物」确认页（上交动作在「拿出证物」页已完成）。
  若验收要求保留确认页文本，则需服务端在 `CM_CLOSE_DIALOG` 后主动续接奖励页（引擎级改动，未做）。

## 十、同形批处理（2026-09-19，按用户指示“同样问题一起改”）

### 10.1 判定规则（可复现）

1. 任务的客户端 HTML 中 `check_user_item_ok(10000)` 页只有 `HACTION_FINISH_DIALOG(1008)`（本地关闭，不回传任务动作）；
2. 该任务某条 transition 的 after-commit 下发了页面 10000；
3. 该 transition 的目标节点已存在“同一 NPC + `USE_OBJECT(-1)` 打开对话”的入口路由，显示页 P；
→ 该 transition 直接下发 P（玩家重新对话本来就会落到 P），不再停在下发 10000 的对话上。

工具（均在仓库内，可复跑）:

- `HandoverFixPlan.java`：只读探针，输出计划 TSV `PLAN_ROWS/REVIEW_ROWS`；
- `apply_handover_fix.py <plan.tsv>`：按 (source, target, npc, action) 精确定位 transition、只替换 after-commit 里的页属性，逐文件 `xmllint --noout` 校验，可重复执行（幂等）。

结果: 计划 42 行 / 41 个文件 → 应用 `APPLIED=42 FILES=41`；修复后重算 `PLAN_ROWS=0 REVIEW_ROWS=122`。

### 10.2 修改文件

41 个任务 XML（每处仅一行 `page` 属性）:
`2372, 10504, 13968, 15689, 15690, 15691, 16838(两处), 18742, 18975, 18976, 18977, 18978, 19010, 19016,
19022, 19028, 19034, 21027, 23968, 25689, 25690, 25691, 26838, 28742, 28975, 28976, 28977, 28978, 29010,
29016, 29022, 29028, 29034, 80723, 80795, 80849, 80850, 80851, 80852, 80886, 80958`
（10501 本身已在第四节修复，不在批处理计划内）。

测试:

- 新增 `QuestHandoverContinuationAuditTest`：全库合同——`CLIENT_LOCAL_CLOSE` 家族在“同一 NPC 有续接页”时
  不得再下发 10000，并正向锁定本批 42 条已接线分支（含 source/target/npc/页），家族下界 `family>=100`；
- 新增测试工具 `HandoverContinuationContract`（`closesLocally` / `sameNpcEntryPages` / `handOverSuccessPage`），
  供审计与家族测试共用；
- 更新既有家族断言以反映新合同：`ItemCollectingDialogProtocolAlignmentTest`（受影响任务的成功页改为按合同计算）、
  `QuestRetailCollectionRoleAlignmentTest`（25690 成功页 10000 → 奖励窗 5）。

### 10.3 验证结果（2026-09-19）

- 聚焦: `Quest10501HandoverContinuationTest` 2/2、`QuestHandoverContinuationAuditTest` 2/2、
  `ItemCollectingDialogProtocolAlignmentTest` 6/6、`QuestRetailCollectionRoleAlignmentTest` 12/12（合计 22/22）。
- 门禁批（`-Dquest.client.contract.failOnStaleBaseline=true`）最终 47/47 通过，含 `QuestClientContractGateTest`、
  `QuestDialogOrderAuditTest`、`QuestPageButtonAuditTest`、`QuestItemSourceContractGateTest`、
  `AcceptAndConfirmationEntryContractTest`，以及本批新增/更新的 4 个聚焦类
  （日志 `mvn-handover-family-final-gates-2026-09-19.log`）。
- 生产目录: `PRODUCTION_COMPILE_OK=6189 / PRODUCTION_COMPILE_FAILURES=0 /
  PRODUCTION_INTERACTION_OBJECT_FAILURES=0 / PRODUCTION_WHITELIST_VIOLATIONS=0`，
  `QuestDefinitionCatalogManifestTest` 10/10（日志 `mvn-production-catalog-family-2026-09-19.log`）。
- 全量 `mvn -B test`: 3473 用例 / 0 failures / 2 errors / 2 skipped。
  两条 error（`QuestMonsterProgressContractAuditTest`、`QuestLegacyMonsterHuntProductionFlowTest` 的
  `QuestTransition.sourceNode()==null` NPE）来自并行工作区的 `25060.xml`（新增
  `<transition target="reward">` 缺少 `source`），与本批 41 个任务无关，未改动
  （日志 `mvn-full-suite-2026-09-19b.log`）。
- 客户端复测：用户 2026-09-19 确认“客户端验证成功”，按规则 4 视为整任务验收通过（见第十一节）。

## 十一、客户端验收（2026-09-19，CLIENT_ACCEPTED）

- 用户 2026-09-19 回复“客户端验证成功”，按任务修复规则 4 视为任务 10501 的完整游玩验收（未限定分支或步骤）。
- 验收记录: `.agents/summary/quest-acceptance/10501-2026-09-19-client-accepted.md`（附件均为 `not captured`，未要求用户重复游玩）。
- 同批 41 个任务未逐个实机复验，按同一合同静态锁定与门禁覆盖；55 个无同 NPC 续接页的任务保持确认页终端形态。

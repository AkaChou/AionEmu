# 接取发放（accept-actions）在 typed XML 迁移中丢失：15545/25545 报障 + 全库同类批量修复

```text
report quest: 15545（ELYOS 66+ SIGNIFICANT，NPC 835514）“[Learn Minion] Precious Ally Minion”
symptom: 使用「下级宠物精灵契约书」召唤宠物精灵后，任务没有往下（停留在接取步）
status: IMPLEMENTATION_COMPLETE + 门禁全绿（focus/catalog/client-contract）/ ACCEPTANCE_PENDING（仅剩客户端实机复验）
changed: 15545.xml、25545.xml、2266.xml、3085.xml、28808.xml、QuestMinionTutorialRetailAlignmentTest
working tree: dirty；10525/10529/15546/20525/20529/255xx/spawn-z/startup-perf 等并行改动严格保留未动
```

## 1. 玩家症状与追踪路径（15545）

1. 玩家在 835514（Leona）接取 15545，任务 `START / var0=0`。
2. 使用背包中的「下级宠物精灵契约书」→ 客户端 `CM_MINIONS(action=0)` → `MinionService#addMinion` 抽中并登记守护灵，
   动画与 `SM_MINIONS(1)` 正常（玩家看到“召唤成功”）。
3. 任务仍停在 `START`，客户端任务栏无变化，835514 也没有领奖对话。

## 2. 证据链

| # | 证据 | 结论 |
| --- | --- | --- |
| E1 | 旧 handler `_15545A_New_Friend_For_A_Archdaeva.java:42`（`7e9f0316c^`）在 `ACCEPT_QUEST_SIMPLE` 分支 `giveQuestItem(env, 190080010, 1)`；镜像 `_25545A_New_Comrade_For_A_Archdaeva.java:42` 对应 `190080011` | 接取时必须由服务端发放本任务专属契约书 |
| E2 | 当前 `quests/15545.xml`、`quests/25545.xml` 的 `NPC_START(835514/835515)` 只有 `start-page`，全文件无 `give-item` | 迁移丢失接取发放 |
| E3 | `MinionService#checkQuest`（`MinionService.java:323,334`）只对 `item.getItemId()==190080010`（ELYOS）/`190080011`（ASMODIANS）且 `status==START` 执行 `setQuestVar(1)`+`setStatus(REWARD)` | 用其它同名契约书召唤守护灵不会推进任务 → 正是报障现象 |
| E4 | Aion 5.8 客户端数据 `58Server/Map/XML/quest.xml`：15545 `quest_work_item1 = quest_15545a 1`、25545 `quest_25545a 1`；`quest_data.xml:30748-30753` 同声明 | 工作物品声明正确，缺的是“发放”这半边合同 |
| E5 | 同族教学 19900/29900（`quests/19900.xml:52,67`、`quests/29900.xml:52,67`）在接取路由上 `<give-item item-id="190080020/190080021" count="1"/>` | 本仓库既有同类合同：教学任务工作物品在接取时直接发放 |
| E6 | 全资源检索：`190080010`/`190080011` 仅出现在两条任务定义、`quest_data.xml`、`disassembly_items.xml`、物品模板；`188058502`（任务用下级宠物精灵契约书包袱，拆解可得 190080010/190080011）除拆解表外无任何产出源 | 当前目录既不发契约书也不发包袱 → 新存档上任务不可完成 |
| E7 | `cac3d7608` 同批修复只补 `reward + QUEST_SELECT(31) -> DEFAULT_SUCCESS` 领奖态入口与 reward 投影 | 报障发生在更早的接取/使用阶段，两者互补 |

## 3. 根因（共享合同）

`7e9f0316c`（1500 个 Java handler → typed XML）只迁移了状态与页面，没有迁移旧 handler 的**接取副作用** `giveQuestItem(...)`：
typed 引擎的 `<work-items>` 只负责“声明 + 完成时回收”，接取发放必须显式写成
`<dialog type="NPC_START"><accept-actions><give-item/></accept-actions>`（展开为 `QUEST_ACCEPT_1(1002)` 与
`QUEST_ACCEPT_SIMPLE(20000)` 两条 `unaccepted -> started` 路由，条件 `start-eligible`）。

受影响任务的工作物品在目录里没有任何其它产出源，于是：玩家拿不到任务指定道具 → 交出/使用页无从触发 → 任务无法推进。
15545 因为 `MinionService` 用“物品 id + status”作双键，症状表现为“召唤成功但任务不动”。

## 4. 修复

| 文件 | 变更 |
| --- | --- |
| `quests/15545.xml` | `NPC_START(835514, unaccepted -> started)` 增加 `<accept-actions><give-item item-id="190080010" count="1"/></accept-actions>` |
| `quests/25545.xml` | 天族镜像：`NPC_START(835515)` 发放 `190080011` |
| `quests/2266.xml` | `NPC_START(203558)` 发放 `182203244`（doc_quest_2266a，旧 handler `_2266A_Trustworthy_Messenger` 的 ACCEPT_QUEST 分支） |
| `quests/3085.xml` | `NPC_START(798144)` 发放 `182208048`（doc_quest_3085a，旧 handler `_3085TheRiddlePoem`） |
| `quests/28808.xml` | `NPC_START(830392)` 发放 `182213216`（quest_28808a，旧 handler `_28808OpenSaysMe`） |
| `QuestMinionTutorialRetailAlignmentTest.java` | 新增 `archDaevaMinionChainGrantsItsOwnContractOnAccept`、`legacyAcceptItemGrantsSurviveTheTypedMigration`，共用 `assertAcceptGrant(questId, npcId, workItemId, expectedRewards)`：锁定工作物品声明、奖励物品（按任务）、两条接取路由的 `StartEligible` 条件与 `GiveItem` 动作 |

**为什么不发包袱 188058502**：retail 走「NPC 发包袱 → 玩家拆解（`disassembly_items.xml`：188058502 → 190080010/190080011）→ 使用契约书」。
本仓库旧 handler 直接发契约书，19900/29900 同族任务也直接发工作物品；直接发契约书不依赖拆解选盒链路
（`DisassemblyAction` → `CM_SELECT_ITEM`），且完成时 `QuestMutationPlanner.appendCompletionWorkItemCleanup` 会按
`<work-items>` 自动回收。retail 包袱形式留作可选 1:1 复刻议题（若要做，需连剥壳 UI 一起验收）。

## 5. 验证状态

- 静态（已做）：`xmllint --noout --schema quest_definition.xsd quests/{15545,25545,2266,3085,28808}.xml` → 5 个文件全部 validates。
- 审计复核（已做）：重跑同类审计脚本后，18 个“旧 handler 接取发放”任务中只剩 2 个未覆盖（4542、18808，见 §7）。
- 编译与门禁（2026-09-21 用户授权 Maven，全部 BUILD SUCCESS；日志在本目录，`*.log` 被 .gitignore 排除未入库）：

```text
mvn -B -Dtest=QuestMinionTutorialRetailAlignmentTest,QuestMinionTutorialProductionFlowTest,MinionServiceTest,ExternalRewardAdvanceReentryContractTest test
  -> Tests run: 27, Failures: 0, Errors: 0, Skipped: 0   BUILD SUCCESS   (mvn-focused-tests.log)
     QuestMinionTutorialRetailAlignmentTest 3/3（含本批新增 2 个方法）、QuestMinionTutorialProductionFlowTest 3/3、
     ExternalRewardAdvanceReentryContractTest 2/2、MinionServiceTest 19/19

mvn -B -Dtest=ProductionCatalogWhitelistVerificationTest,QuestDefinitionDirectoryLoaderTest,QuestDefinitionCatalogManifestTest test
  -> Tests run: 13, Failures: 0, Errors: 0, Skipped: 0   BUILD SUCCESS   (mvn-catalog-gates.log)
  -> PRODUCTION_COMPILE_OK=6189 / PRODUCTION_COMPILE_FAILURES=0 /
     PRODUCTION_INTERACTION_OBJECT_FAILURES=0 / PRODUCTION_WHITELIST_VIOLATIONS=0

mvn -B -Dquest.client.contract.failOnStaleBaseline=true -Dtest=QuestClientContractGateTest,QuestDialogOrderAuditTest,QuestStepDialogTerminationTest test
  -> Tests run: 19, Failures: 0, Errors: 0, Skipped: 0   BUILD SUCCESS   (mvn-client-contract-gates.log)
```

- 相邻门禁（`mvn -B -Dtest=QuestWorkItemMigrationCoverageTest,Quest2110WorkItemRegressionTest test`，mvn-workitem-gates.log）：
  `Quest2110WorkItemRegressionTest` 通过，`QuestWorkItemMigrationCoverageTest` 报 **10526/20526**（`legacy=[164002347/164002348] declared=[182216074/182216086]`）。
  这两个文件在当前工作区**未修改**（HEAD 原状），不在本批改动范围内，属既存目录不一致，按并行 10525/10529/20525/20529 批次处理；本批 5 个任务未出现在失败清单中。
- 实机（待用户）：新角色接取 15545/25545，确认背包出现 `quest_15545a`/`quest_25545a` → 使用后任务进入 REWARD → 835514/835515 领奖；
  2266/3085/28808 是否同步复验到交出/完成页由用户决定。

## 6. 存量卡住存档

任务 `cannot-giveup`，已接取且 `START/var0=0` 的角色不会自动补发：需 GM `//add 190080010 1`（阿斯莫德 `//add 190080011 1`）后重新使用契约书。
未加 `enter-world` 补发自愈边：没有“未持有才发放”的原子条件时会在每次登录重复发放，风险高于收益。

## 7. 全库同类审计

- 脚本：`.agents/summary/quest-15545-minion-accept-grant/audit_legacy_accept_item_grant.py`（只读；从 `7e9f0316c^` 抽 `giveQuestItem`，用“最近 dialog 分支（case/QuestDialog）是否 ACCEPT”判定接取上下文，避免把后续步骤误判为接取发放——1198/2498/11123/11143/25052 这类邻近误命中已人工排除）
- 结果 TSV：`.agents/summary/quest-15545-minion-accept-grant/legacy-accept-item-grant-audit.tsv`
- 统计：接取发放任务 18 个；修复前 7 个完全没有 `give-item`，修复后剩 2 个：

| quest | 接取发放物品 | 当前定义 | 处置 |
| --- | --- | --- | --- |
| 15545 / 25545 | 190080010 / 190080011 | 无 | 本批已修 |
| 2266 | 182203244 | 无 | 本批已修 |
| 3085 | 182208048 | 无 | 本批已修 |
| 28808 | 182213216 | 无 | 本批已修 |
| 4542 | 182215327（+ 逐步 182215328 / 182215330） | 无任何 give/remove（只有 182215329 收集物 remove） | 待批：旧 handler 是「接取给 a → 204743 换 b → 204768 交 b + 播影片 → 204808 收 3 个收集物 → 给 d → 交 d」的多步链，需独立逐步重建与验收 |
| 18808 | 188051194（wrap_q_matter_enchant_40a） | 该 id 被写成**奖励物品**，工作物品改为 182213215（quest_18808a） | 证据冲突：旧 handler 接取发的是染色材料包装，客户端 `quest_18808a` 却是箱钥匙，需人工裁定后再动 |

## 8. 风险与未验证项

- 编译门禁未跑：`accept-actions` 展开（两条接取路由 + `StartEligible` + `SyncQuestState(VISIBILITY_REFRESH)`）尚未经真实编译器验证，`xmllint` 只验证 schema。
- 未抓包/未实机验证 `QUEST_ACCEPT_SIMPLE(20000)` 触发链路与接取页展示顺序。
- 有意保留的偏差：2266/3085 旧 handler 在交出页 `removeQuestItem`，本批只补“发放”，物品改由 `QuestMutationPlanner.appendCompletionWorkItemCleanup` 在 COMPLETE 时回收（REWARD→COMPLETE 之间短暂留存）。若要 1:1，需在这两条 `started -> reward` 路由上补 `remove-item count="ALL"`（用 ALL 才能容忍存量无物品存档），待门禁通过后再评估。
- 15545 重复接取时是否叠加契约书未验证（物品 `max_stack_count=100`；任务 `cannot-giveup`、`max_repeat_count=1`；旧 handler 有 `count==0` 守卫，typed 引擎 accept-actions 无等价条件）。
- 审计仅覆盖 Java handler 的 `giveQuestItem`；用其它 API（`ItemService.addItem`）发放的旧行为可能未纳入。
- 2266/3085/28808 的链上 NPC（如 203655/203654、203830、730534 所在链）同样以 `<dialog type="NPC_START">` 建模，会批量生成 `unaccepted -> started` 接取路由；本轮只给旧 `addOnQuestStart` owner 加发放（15545/25545 只有一个 owner，不受此影响）。从链上 NPC 接取是否应被禁止属另一类「接取 owner」合同，需要单独审计后再定，未在本批修改。

## 9. Playbook / Memory Bank

- Playbook：本条与 `WORK_ITEM_DECLARATION_LOST_ON_MIGRATION`（case 8.35，清理侧）互补，属发放侧新合同；但当前仅有静态证据、门禁未跑，按规则**暂不登记** representative case（拟名 `ACCEPT_TIME_WORK_ITEM_GRANT_LOST_ON_MIGRATION`），待 focused + catalog 门禁与实机验收后再评估。
- Memory Bank：未新增跨域 invariant，本轮不更新 `patterns/quest-engine.md`，不写 `[Memory Bank Auto-Updated]`。

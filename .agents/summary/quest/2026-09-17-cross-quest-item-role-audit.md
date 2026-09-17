# 跨任务道具角色错配审计（QE-031）

日期：2026-09-17　范围：全部 6,222 个任务定义（6,186 可执行）

## 1. 症状与根因

症状：收集任务的怪物掉落完全正常、背包里也确实有道具，**交付对话却始终提示物品不足**；或收集进度条始终不刷新、任务停在收集阶段无法进入下一步。

根因（同一批迁移缺陷的两个面）：

1. **交付条件引用了邻居任务的道具**（按邻居复制"道具块"，道具 ID 连数量一起偏移）：
   | 任务 | 掉落/自有的道具 | 交付条件误写成 | 真端 collect_item / check_item |
   |---|---|---|---|
   | 15010 | 182215664(quest_15010a)x5、182215665(quest_15010b)x3 | 182215666(quest_15011a)x7 | quest_15010a 5 + quest_15010b 3 |
   | 15012 | 182215667(quest_15012a)x5 | 182215668(quest_15013a)x5 | quest_15012a 5 |
   | 15043 | 182215677(quest_15043a)x7 | 182215678(quest_15044a)x5 | quest_15043a 7 |
   | 15070 | 182215682(quest_15070a)x10 | 182215683(quest_15071a)x1 | quest_15070a 10 |
   | 51021 | 182215182(quest_51017a)x3 | 182215183(quest_51018a)x3 | quest_51017a 3 |

   道具开发名来自 `item_template@name_desc`；`182215668 = quest_15013a`（属于 15013）是判定关键证据。

2. **`collect-item` 事件监听了邻居任务的道具，且 count 误用掉落行数**：`QuestEngine` 在道具入包后广播 `QuestEvent.CollectItem(itemId, inventoryCount)`，事件用于重新下发任务状态（`PACKET_ONLY`）。监听错的道具 → 事件永不触发 → 客户端收集进度不刷新。
   - 28836：监听 `182213210(quest_28835a) count=5`（该任务恰好 5 条掉落行）→ 改为 `182213207(quest_28836a) count=50`（真端 collect/check 均为 50）
   - 28838：监听 `182213214(quest_41257b) count=8`（恰好 8 条掉落行）→ 改为 `182213208(quest_28838a) count=50`
   - 全库同类事件 147 处，其余 143 处均监听本任务道具，count 与交付数量一致（44/48 有 report 的样例逐一核对）

## 2. 门禁（新增）

`src/test/java/com/aionemu/gameserver/questEngine/definition/QuestItemSourceContractGateTest.java`

- **I1 全库不变量**：`collect-item` 事件只能监听本任务声明（`items`/`inventory-items`/`work-items`）、掉落、发放或上报的道具；违规即失败，无豁免通道。修复后 0 违规。
- **I2 回归锁定**：上面 7 个任务的交付集合必须等于真端 collect/check 道具（含数量与移除动作白名单）。

## 3. 复算

```bash
python3 .agents/summary/quest/item-producer-scan/generate_item_role_baseline.py   # 真端 collect/check 名称 -> ID，输出 test resource 基线
python3 .agents/summary/quest/item-producer-scan/audit_cross_quest_item_roles.py  # 回归 + I1 + 后续轴
```

## 4. 后续待评审轴（本次未纳入门禁）

真端声明了 collect/check 道具、但在我方交付集合（has-item / remove-item / collect-item / npc-item-report）中完全看不到的任务 **89 行** → `.agents/summary/quest/item-producer-scan/item-role-gaps.tsv`。

其中形如 `1932 → 182206008(quest_1932a)`、`2232 → 182203224(quest_2232a)` 的是**本任务自己的道具没被任何交付条件引用**，与"玩家可零进度领奖"同源，优先级最高；其余多为外部来源（采集物、商店材料）或奖励型任务，需要逐条判定后才可收口为门禁。

## 5. 边界

- 跨任务道具交接链是**真端设计**，不得一律判错：如 13904 交付 13903 的道具、80795 使用 80723 的道具、50048 消耗 50047 的奖励（50047 的 reward 即 186000401）。
- `item_template` 缺少 `name_desc` 的道具不参与名称映射判定。
- 验证：`mvn test -Dtest=QuestItemSourceContractGateTest,QuestDropContractGateTest,QuestMovieAndDialogLoopRegressionTest,QuestDefinitionDirectoryLoaderTest,CompletedQuestPrerequisiteRegressionTest,QuestClientContractGateTest,ProductionCatalogWhitelistVerificationTest` → Tests run: 33, Failures: 0, Errors: 0；PRODUCTION_COMPILE_OK=6186、FAILURES=0、WHITELIST_VIOLATIONS=0。未做真机客户端验收。

## 6. 追加批次（零进度交付类，同日）

对 89 行待评审轴逐条定性（`item-role-gaps.tsv` 增加 verdict 列）：

| 定性 | 条数 | 处置 |
|---|---|---|
| OWN_COLLECT_NO_TURNIN_GATE_SINGLE_EDGE / MULTI_EDGE | 35（本批修 7） | 任务掉落并声明自家道具、却没有任何交付校验 → 玩家可零进度领奖、道具永不消耗 |
| EXTERNAL_SOURCE_ITEM | 48 | 副本钥匙/材料/活动/商城道具，非交付缺陷 |
| OWN_COLLECT_NO_DROP | 22 | 自家道具但本任务不掉落，需确认交接链 |
| UNMAPPED_NAME | 11 | 名称未映射，需人工核对 |
| OTHER_QUEST_ITEM | 4 | 他任务道具（交接链或错配） |

**本批修复（7）**：1932 / 3547 / 14121 / 14201 / 24121 / 24152 / 24242 —— 四重证据（唯一进入 reward 的交付边、任务自行掉落该道具、`<items>` 数量与真端 collect_item 一致、其中 3 个真端 COLLECT_ITEM 注释 NPC 与交付边 NPC 吻合）后，在交付边补回
`has-item item-id=X count=N` + `remove-item item-id=X count=N`。

**隔离验证说明**：主工作树当时被并行协作者的 `LegionService`/`LegionMembers` 在途重构阻塞（主源码不可编译），按仓库 worktree 纪律改用 `/tmp` 临时 worktree（仅复制本批 8 个文件）验证，验证后已 `git worktree remove --force` + `prune`，无残留。

**真机验收清单**：`2026-09-17-collect-turnin-acceptance-checklist.md`。

## 7. 第三批（多 NPC 变体，同日）

`2232 / 2239 / 2289 / 3013 / 3088 / 4542`：同一任务由多个 NPC 交付（2~5 个变体），**17 条进入 reward 的交付边全部没有任何条件** → 任一入口都可零进度领奖。
六者均为单一自家道具、`<items>` 数量与真端 collect_item 完全一致、且任务自行掉落该道具，故对每条交付边统一补 `has-item` + `remove-item`。

修复后待评审轴由 89 行降为 **76 行**：48 外部来源 / 29 同类缺交付校验（剩余部分为无 reward 入边或已存在其它道具校验，需逐条确认）/ 22 本任务无掉落 / 11 名称待映射 / 4 他任务道具。

**验证**：`mvn test` 同套门禁 33 项全绿（主工作树恢复可编译后复跑），PRODUCTION_COMPILE_OK=6186、FAILURES=0、WHITELIST_VIOLATIONS=0。

## 8. 第四批：NPC_REPORT 简写展开的无条件交付路由（QE-032）

**机制**：`<dialog type="NPC_REPORT">` 由 `QuestXmlBlockExpander.expandNpcReport` 展开为两条边——
`QUEST_SELECT`（展示页，source→source）与 `SELECT_QUEST_REWARD`（source→target/reward，**conditions 与 actions 全空**）；
只有显式声明同 `(source, npc, SELECT_QUEST_REWARD)` 路由时，简写那条才会被 `explicitDialogRoutes` 过滤掉。

**范围与处置**：全库 66 个任务命中"声明并掉落收集道具、却存在无条件交付分支"：

| 分类 | 数量 | 处置 |
|---|---|---|
| 真端 `collect_item` 名称与数量逐条相等的简写任务 | 51（79 条路由） | **已修**：每条路由补 `priority=0` gated 显式路由（has-item + remove-item）+ `priority=1` 未集齐回落页（CHECK_USER_ITEM_FAIL） |
| 仅 `reward→reward` 简写 | 13（25013/25022/25050/25062/25073/25080/25081/25094/25306/25526/25532/25535/25538） | **保持无条件**：这是"重新打开奖励窗"，道具在首次交付时已扣除，加校验会锁死领奖 |
| 真端道具名未映射 | 2（3217/4217） | 留待人工评审 → `item-handin-route-gaps.tsv` |

**修复形态（每条简写分支三条路由）**：① `priority=0` gated 交付路由（has-item + remove-item + 奖励窗页）；② `priority=1` 未集齐回落路由（`CHECK_USER_ITEM_FAIL`＝页 10001）；③ `FINISH_DIALOG`(1008) 关闭路由——失败页上客户端渲染的关闭按钮必须有落点，否则 `QuestClientContractGateTest` 报 `BUTTON_WITHOUT_ROUTE`（首轮实测命中 16942/26942/26977/30210/30213 共 6 条，补 79 条关闭路由后全绿）。

**已知同类缺口基线**：门禁按编译后模型还发现 **30 个任务 / 42 条**既有的无条件交付分支（多为**显式** `SELECT_QUEST_REWARD` 路由，非简写类），未在本批处理，已逐条写入 `src/test/resources/quest/quest-item-handin-baseline.tsv`（按 `quest-client-contract-baseline.tsv` 惯例，新增即失败、已修复项默认容忍），并在审计脚本中单列。

**静态验证**：51 个文件 XSD 校验 51/51 通过；注入路由的 target 节点全部存在；重复路由检查确认相关"重复"均为 HEAD 中既有的 `has-item` / `expected=false` 成对结构与奖励阶段对话对，非本批引入。

**新增门禁**：`QuestItemSourceContractGateTest#everyRewardEntryBranchVerifiesTheQuestsOwnCollectedItems`——只要任务同时声明并掉落某收集道具，其每个 `(源节点, NPC)` 交付分支都必须有一条覆盖**该任务全部收集道具**的 gated 路由（奖励阶段重开路由除外）。修复前 66 个任务命中，修复后仅剩 3217/4217。

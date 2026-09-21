# 任务 10522 领奖态“只有结束对话”与同型引擎外 REWARD 任务修复

- 日期：2026-09-20
- 范围：`10522`/`20522`（创造力的使用）、`15542`/`25542`（融合）、`15545`/`25545`（守护灵契约）、
  `30211`/`30213`/`30311`/`30313`（贝什蒙德神殿符文宝珠）
- 状态：**10522 CLIENT_ACCEPTED（2026-09-21 用户实机确认）；2026-09-21 授权 Maven 聚焦测试、生产目录门禁与客户端契约门禁全绿；同批其余 9 个同因任务客户端待复测**

## 现象

玩家截图：任务 `10522`（天族 · MISSION · 不可共享 · 不可放弃，前置 `10521`）

```text
Status: REWARD
Vars: 0 0 0 0 0
Complete count: 0
```

步骤「和代理人维达对话 / 使用创造力」，与 NPC `806075`（`LF6_Weatha_E`，客户端名「代理人维达」）对话时
只有通用「结束对话」，没有本任务的完成对话（`select_success(10002)`），因此无法打开奖励窗口 5 领奖。

## 根因

### 1. 迁移遗漏领奖态入口页（直接症状）

Aion 5.8 客户端 `quest_q10522.html` 只有两个页面：

- `select_success`(`10002`)：唯一按钮 `HACTION_SELECT_QUEST_REWARD`(`1009`)「说要听听。」；
- `select_quest_reward1`(奖励窗口 5)：奖励文案页。

旧 handler `origin/history:.../quest/handlers/archdaeva/_10522Using_Essence.java` 的对话合同是：

```java
if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
    if (env.getDialog() == QuestDialog.START_DIALOG)      return sendQuestDialog(env, 10002);
    else if (env.getDialog() == QuestDialog.SELECT_REWARD) return sendQuestDialog(env, 5);
    else return sendQuestEndDialog(env);
}
```

即 `START_DIALOG(31) -> 10002`、`SELECT_REWARD(1009) -> 5`。迁移后 typed 定义只保留了
`started -> reward (SELECT_QUEST_REWARD) -> 奖励窗口`，**没有注册 `reward + QUEST_SELECT(31)` 入口页**，
而 NPC 列表里点任务行发送的正是 `QUEST_SELECT(31)`；引擎找不到该状态的 31 路由，`DialogService`
回落到通用结束对话。同族已正常工作并有相同入口页的任务：`10521`/`20521`
（`reward -> reward` + `QUEST_SELECT(31)` -> `DEFAULT_SUCCESS`）、`10526`/`20525`。

### 2. 引擎外写入方与 reward 投影错位（同型隐藏缺陷）

这些任务的 `REWARD` 不是由 `SELECT_QUEST_REWARD` 事务写入的，而是由引擎外代码直接
`setStatus(QuestStatus.REWARD)`：

| 任务 | 写入方 | 写入的 packed step |
|---|---|---|
| `10522`/`20522` | `CM_CREATIVITY_POINTS#checkQuestCompletion` | 不写（保持 `started` 的 `var0=0`） |
| `15542`/`25542` | `CoalescenceService#updateQuestsOnCoalescenceComplete` | 不写（`var0=0`） |
| `15545`/`25545` | `MinionService#checkQuest` | `setQuestVar(1)` |
| `30211`/`30213`/`30311`/`30313` | `RiftOrbAI2#forQuest` | 不写（`var0=0`） |

typed 引擎按 (status, packed step) 匹配路由（`QuestMutationPlanner#matchesSourceNode`：source 节点投影的
每个变量都必须等于解包后的实际 packed 变量），所以：

- `10522`/`20522` 的 `reward` 投影写成 `var0=1`，写入方却留下 `var0=0` → `REWARD` 状态匹配不到任何
  `reward` 节点路由，玩家的 `Vars: 0 0 0 0 0` 正是这一形态；
- `15545`/`25545` 的 `reward` 投影写成 `var0=0`，写入方却写 `var0=1` → 同样错位；
- 10 个任务全部缺少领奖态入口页，因此即使步数对齐也无法下发 `10002`。

### 3. 同族全库审计（以全部类似任务角度修复）

只读审计脚本 `.agents/summary/quest-10522-reward-reentry/audit_external_reward_advance.py` 扫描
questEngine/commands/gmhandler 之外的 `setStatus(QuestStatus.REWARD)` 写入方，核对写入步数、`reward`
投影、完成 NPC 与领奖态入口页。修复前基线
`.agents/summary/quest-10522-reward-reentry/external-reward-advance-before-fix.tsv`：

```text
10522 projection=1 MISMATCH ENTRY-PAGE-MISSING recovery=none
15542 projection=0 aligned  ENTRY-PAGE-MISSING recovery=none
15545 projection=0 MISMATCH ENTRY-PAGE-MISSING recovery=none
20522 projection=1 MISMATCH ENTRY-PAGE-MISSING recovery=none
25542 projection=0 aligned  ENTRY-PAGE-MISSING recovery=none
25545 projection=0 MISMATCH ENTRY-PAGE-MISSING recovery=none
30211/30213/30311/30313 aligned ENTRY-PAGE-MISSING recovery=none
```

修复后 `.agents/summary/quest-10522-reward-reentry/external-reward-advance.tsv`：全部 `aligned` +
`entry-page-ok`，`10522`/`20522` 带 `recovery=[1]`，`15545`/`25545` 带 `recovery=[0]`。该文件已复制为
测试资源 `src/test/resources/quest/external-reward-advance-baseline.tsv`。

## 修复

### XML（10 个任务）

- `10522.xml`/`20522.xml`：
  - `reward` 投影 `var0: 1 -> 0`（对齐 `CM_CREATIVITY_POINTS` 不写步数的行为）；
  - 删除 `started -> reward (SELECT_QUEST_REWARD)` 的 `set-variable var0=1`（打包步数由目标投影决定）；
  - 新增 `reward -> reward`：`TALK_TO_NPC` `806075`/`806079` + `QUEST_SELECT(31)` -> `SHOW_QUEST_PAGE
    DEFAULT_SUCCESS(10002)`（无 conditions、无 transaction actions、无 priority）；
  - 新增无 source 的 `enter-world` 自愈边：`status-is REWARD` + `var0 == 1` -> `reward`，
    仅 `LEVEL_AND_VISIBILITY_REFRESH`，用于纠正旧定义（`var0=1`）已落盘的错位存档。
- `15545.xml`/`25545.xml`：`reward` 投影 `var0: 0 -> 1`（对齐 `MinionService` 的 `setQuestVar(1)`）；
  新增 `835514`/`835515` 的领奖态入口页；新增 `status-is REWARD` + `var0 == 0` 的 `enter-world` 自愈边。
- `15542.xml`/`25542.xml`：新增 `806074`/`806078` 的领奖态入口页（步数本就对齐，无需自愈边）。
- `30211.xml`/`30213.xml`/`30311.xml`/`30313.xml`：为各自完成 NPC（`798941`、`798926`、`730275`、
  `799322`、`799225`）新增领奖态入口页（步数本就对齐，无需自愈边）。

入口页仍复用既有 `npc-complete` 预览路由完成 `1009 -> 奖励窗口 5`（`preview actions="USE_OBJECT
SELECT_QUEST_REWARD"` / `30313` 的 `reward -> reward actions="USE_OBJECT SELECT_QUEST_REWARD"`），
没有新增奖励发放路径。

### 测试与审计

- 更新既有测试（同型镜像与新合同适配，非新增缺陷）：
  - `Quest10522AutoStartDialogTest` / `Quest20522AutoStartDialogTest`：锁定 `reward var0=0`、
    `started -> reward` 不写 `var0`、领奖态入口页、`enter-world` 自愈边（条件顺序、无 transactions、
    after-commit 序列）；两个任务互为阵营镜像，必须同步。
  - `Quest30313RetailAlignmentTest`：原断言要求 `reward -> reward` 全部下发奖励窗口 5，现按真实合同拆成
    「reward 态路由只能属于 799322/799225」「799225 保留奖励窗口 5 预览」「QUEST_SELECT(31) 入口页下发
    select_success(10002)」三条，未放宽原有护栏。
- 新增 `ExternalRewardAdvanceReentryContractTest`：读取基线 TSV 驱动，
  - 断言 `reward` 投影 == 写入方步数；
  - 断言任何 `started -> reward` 事务不得 `set-variable var0`；
  - 断言每个完成 NPC 都有 `reward + QUEST_SELECT(31)` -> `DEFAULT_SUCCESS`（无 conditions/actions/priority）；
  - 断言 `enter-world` 自愈边的条件列表与基线 `staleRewardSteps` 顺序一致，响应固定；
  - 断言基线中的写入方文件仍存在且仍推进 `REWARD`；
  - 代表任务 `10522`：入口页下发 `10002`、`reward` 态 `1009` 仍打开奖励窗口 5。
- 新增只读静态校验器 `.agents/summary/quest-10522-reward-reentry/verify_external_reward_reentry_contract.py`
  （无构建授权时先做 XML 层等价检查）。

## 已执行验证（静态）

```text
python3 .agents/summary/quest-10522-reward-reentry/verify_external_reward_reentry_contract.py
  -> OK 10522/15542/15545/20522/25542/25545/30211/30213/30311/30313
  -> static contract verified for 10 quests

python3 -c 'minidom.parse(...)'  # 10 个改动 XML
  -> xml-ok 10/10

git diff --check -- <改动路径>
  -> OK
```

客户端页面证据（`docs/quest/client-dialog-mapping/quest-dialog-action-details.csv`）：10 个任务都存在
`select_success(10002)` 且唯一按钮为 `HACTION_SELECT_QUEST_REWARD(1009)`，因此入口页不会引入
「页面不在任务 HTML 中」的客户端契约门禁指纹。

## 已执行验证（2026-09-21 用户授权 Maven）

原始日志（被 `.gitignore` 的 `*.log` 排除、未入库）保存在本目录。

```text
mvn -B -Dtest=Quest10522AutoStartDialogTest,Quest20522AutoStartDialogTest,ExternalRewardAdvanceReentryContractTest,
        Quest30311RetailAlignmentTest,Quest30313RetailAlignmentTest,Quest10520ClientDialogAlignmentTest,
        BroadcastZoneMissionEndDefinitionTest,MinionServiceTest,LegacyRewardStepProjectionRegressionTest test
  -> Tests run: 40, Failures: 0, Errors: 0, Skipped: 0   BUILD SUCCESS        (mvn-focused-reentry-tests.log)

mvn -B -Dtest=ProductionCatalogWhitelistVerificationTest,QuestDefinitionDirectoryLoaderTest,QuestDefinitionCatalogManifestTest test
  -> Tests run: 13, Failures: 0, Errors: 0, Skipped: 0   BUILD SUCCESS        (mvn-catalog-gates.log)
  -> PRODUCTION_COMPILE_OK=6189 / PRODUCTION_COMPILE_FAILURES=0 /
     PRODUCTION_INTERACTION_OBJECT_FAILURES=0 / PRODUCTION_WHITELIST_VIOLATIONS=0

mvn -B -Dquest.client.contract.failOnStaleBaseline=true
       -Dtest=QuestClientContractGateTest,QuestDialogOrderAuditTest,QuestStepDialogTerminationTest test
  -> Tests run: 19, Failures: 0, Errors: 0, Skipped: 0   BUILD SUCCESS        (mvn-client-contract-gates.log)

mvn -B -Dtest='com.aionemu.gameserver.questEngine.definition.*Test' test
  -> Tests run: 980, Failures: 4, Errors: 3, Skipped: 1                     (mvn-definition-package.log)
```

首轮编译暴露测试自身的 lambda 捕获错误
（`ExternalRewardAdvanceReentryContractTest` 在 `assertEquals(..., () -> "..." + line)` 中捕获了非 final 的
循环变量），已修正为快照局部变量后全绿；这是测试代码问题，不是 XML 合同问题。

宽口径 `definition` 包运行的 4 failures + 3 errors **全部落在本任务未改动的任务/测试上**，属工作区既存欠账：

| 失败 | 涉及任务 | 判定依据 |
|---|---|---|
| `QuestArchDaevaPromotionDefinitionTest`（2 例，期望 `reward var0=6`，实为 5） | `10520`/`20520` | 两个 XML 在 `git status` 中未改动；当前提交内容本身投影为 5，与测试期望不符 |
| `QuestKillCounterRetailGateTest`（15101 用了 var0+var1） | `15101` | XML 未改动，含 2 条无 source 恢复边，属既有单计数合同欠账 |
| `QuestWorkItemMigrationCoverageTest`（10526/20526 的 work-items 缺 legacy 道具） | `10526`/`20526` | XML 未改动，与本次入口页/投影修改无关 |
| `Quest25512ClientDialogAlignmentTest`（2 例 NPE：`sourceNode()` 为 null） | `25512` | XML 未改动，HEAD 版即含无 source `enter-world` 恢复边，属测试端假设欠账 |
| `QuestMovieAndDialogLoopRegressionTest`（15301/25301 接取推进，NoSuchElement） | `15301`/`25301` | XML 未改动；对应 `15301` 领奖/对话链未收口问题（见 `.agents/memory-bank/activeContext.md`） |

同一包内的全库门禁（`QuestMovieAndDialogLoopRegressionTest` 的多档奖励/预览契约、`QuestClientContractGateTest`、
`QuestDialogOrderAuditTest`、`ProductionCatalogWhitelistVerificationTest`）均通过，说明本次 10 个任务新增的
`reward + QUEST_SELECT(31) -> DEFAULT_SUCCESS` 入口页没有引入新指纹。

未启动/停止/重启服务端；未做真实客户端复验。

## 客户端复测路径

1. 重建资源并重启后，用 `REWARD` 存档的 10522 角色与代理人维达（`806075`）对话：
   - 点击任务行（客户端动作 31）应下发 `select_success(10002)`；
   - 点击「说要听听。」发送 `SELECT_QUEST_REWARD(1009)`，应打开奖励窗口 5；
   - 选择奖励后进入 `COMPLETE`。
2. `Vars` 为 `0 0 0 0 0` 的旧存档修复后直接匹配 `reward` 节点，无需自愈；若存档是旧定义下
   经 XML 路线写入的 `var0=1`，则由 `enter-world` 自愈边归零后同样可领奖。
3. 同族按 `20522`（806079）、`15542`/`25542`（806074/806078）、`15545`/`25545`（835514/835515）、
   `30211`/`30213`/`30311`/`30313`（798941/798926/730275/799322/799225）逐条复测领奖。
4. 复测后如确认，再按 Playbook 规则评估是否新增代表案例（当前为未验收实现，禁止提前登记）。

## 客户端验收（2026-09-21）

- 用户回复：「客户端验收通过，提交」→ 报障任务 `10522` 记 `CLIENT_ACCEPTED`（REWARD 态点任务行 → `select_success(10002)` →
  「说要听听。」`SELECT_QUEST_REWARD(1009)` → 奖励窗口 5 → `COMPLETE`）。
- 权威范围只覆盖 10522；同批 `20522`/`15542`/`25542`/`15545`/`25545`/`30211`/`30213`/`30311`/`30313`
  仍为 `PENDING`，未逐一实机复验，由 `ExternalRewardAdvanceReentryContractTest` + 基线 TSV 锁定。
- 结构化验收记录：`.agents/summary/quest-acceptance/10522-2026-09-21-client-accepted.md`。

## 边界与风险

- 本模式与 `LEGACY_REWARD_ENTRY_KEEPS_PACKED_STEP`（QE-045）同源但不同触发面：QE-045 面向旧 handler 迁移，
  本任务面向**引擎外直接写 REWARD** 的客户端包/服务/AI，并额外要求领奖态入口页；审计口径也不同
  （`audit_external_reward_advance.py` 而非 legacy reward projection 审计）。
- `enter-world` 自愈边只在校验登录/切图时触发，在线且不重登的错位存档不会立即纠正；
  但本任务玩家的 `var0=0` 存档修复后本就匹配 `reward` 节点，不受此限制。
- 审计脚本按方法体正则扫描写入方；若写入方被重构为非方法体直写或改走 typed 引擎，需重跑脚本刷新基线，
  `ExternalRewardAdvanceReentryContractTest` 会因任务清单变化直接失败。
- 未跑 Maven，因此编译期歧义（`AMBIGUOUS_TRANSITION`）、生产目录编译与客户端契约门禁尚未在本机确认。

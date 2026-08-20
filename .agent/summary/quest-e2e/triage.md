# quest-e2e 全量审计分诊

## 统计口径

`396,787` 是全量 production catalog 在不同状态、事件、条件分支和协议模式下生成的 **transition 场景记录数**，不是任务数量，也不是已确认正确的任务数量。

最终审计结果为：

| 状态 | transition 数 | 含义 |
|---|---:|---|
| `PASS` | 333,121 | 当前场景匹配到预期分支，事务、状态和已支持的 after-commit/协议不变量未发现确定性错误 |
| `AFTER_COMMIT_FAILURE` | 0 | targetless 事件快照修正后已清零；此前 106 条是 fixture 误报 |
| `PAGE_NOT_IN_CLIENT` | 0 | 任务 14047 的 11 条 action-id-as-page 错误已改为明确关闭窗口 |
| `CLICK_NO_RESPONSE` | 0 | 原 57 条交互物 AI 外部响应已从点击错误改为明确的 `RUNTIME_REQUIRED` 边界 |
| `NO_MATCH` | 269 | 场景没有匹配 transition，尚不能直接判定 XML 错误 |
| `AMBIGUOUS_ROUTE` | 157 | 最小条件投影命中了 sibling transition，需结合条件和事件事实逐条分诊 |
| `TRANSACTION_FAILURE` | 0 | production planner 已支持装备起始条件，已确认的 commit 前失败清零 |
| `RUNTIME_REQUIRED` | 59,265 | 59,208 条缺少可用静态世界出生证据，另有 57 条由 `QuestItemNpcAI2` 完成交互物掉落响应 |
| `EVIDENCE_REQUIRED` | 3,975 | 当前 IR 不足以独立证明零售行为，需要旧 handler、客户端或运行时证据 |

`PASS` 只表示本审计覆盖的不变量通过，不等于已经证明任务与零售端到端完全等价。此前的 `342,838 PASS` 是审计器能力和分类修正前的中间结果，不再作为最终基线。

## 已修复的确定性错误

### NO_MATCH：任务 2878–2887 的 20 条接取记录

修改前报告中，任务 2878–2887 的 `QUEST_ACCEPT_1` 和 `QUEST_ACCEPT_SIMPLE` 各有一条
`unaccepted -> started` 场景无法匹配，共 20 条 `NO_MATCH`。这些记录已经由三类独立证据确认是
确定性迁移错误，而不是 fixture 缺少运行时事实：

- 当前 XML 同时要求上一军衔任务已完成，又把同一前序任务放入 `noacquired` 起始条件；正式条件求值中，
  已完成任务不满足 `noacquired`，两组条件互相否定。
- `origin/history` 的任务 2877–2887 handler 都在进入 `PRIMUM_LANDING_400010000` 时调用
  `QuestService.startQuest(env)`，没有 NPC 接受按钮；击杀完成后才允许对应 NPC 报告和领奖。
- Aion 5.8 客户端 Q2877–Q2887 的 `SELECT_NONE(4762)` 只有
  `FINISH_DIALOG(1008)`“履行委托”，奖励报告页 `DEFAULT_SUCCESS(10002)` 才提供
  `SELECT_QUEST_REWARD(1009)`；客户端不存在这批 XML 生成的 NPC 接受链。

当前实现已删除 11 个任务互相否定的 `noacquired` 条件和未接取 NPC 接受/拒绝路由，恢复唯一的
`ENTER_ZONE(PRIMUM_LANDING_400010000) + start-eligible` 自动接取；提交后只执行
`VISIBILITY_REFRESH`。军衔击杀链、报告 NPC 和奖励保持各任务的正式合同，并补回
`REWARD + QUEST_SELECT -> DEFAULT_SUCCESS` 报告页。

Q2877 没有出现在这 20 条 `NO_MATCH` 中，但旧 handler 证明它与后续任务共享相同的区域自动接取
owner；删除它的 NPC 接受链和跨任务 `noacquired` 网，是恢复整条 2877–2887 连续任务合同的一部分。
专项 `Quest2877To2887UrgentOrdersFlowTest` 锁定 11 个任务的前置集合、区域入口、三段军衔击杀、
NPC/page/action、奖励和完整 after-commit 顺序。

状态：`PENDING`（自动化门禁已通过，尚缺人工客户端/运行时验收）。专项测试 11/11、
`QuestDefinitionCatalogManifestTest` 6/6、production whitelist 门禁通过；全量 E2E 已重新生成
`396,787` 条记录，`PASS=333,121`、`NO_MATCH=269`。相较修改前，2878–2887 的 20 条接取
`NO_MATCH` 已清除；Q2877–Q2887 的 264 条当前 transition 均为 `PASS`。这不替代真实客户端
对区域自动接取、军衔击杀和奖励领取的运行时验收，因此不新增代表案例，也不标记为 `CLIENT_ACCEPTED`。

#### 同型对称链：任务 1877–1887

按相同 owner 指纹反查 `origin/history` 后，Elyos 阵营的任务 1877–1887 也存在同源迁移错误：

- 11 个旧 handler 都在进入 `TEMINON_LANDING_400010000` 时调用 `QuestService.startQuest(env)`，
  NPC 只在任务已经进入 `REWARD` 后显示 `DEFAULT_SUCCESS(10002)` 并打开奖励窗口。
- 生产 `quest_data.xml` 要求 Q1877 完成 Q1876，Q1878–Q1887 依次同时完成 Q1876 与上一军衔任务；
  当前任务 XML 却只在 NPC 接受 transition 内检查 Q1876，丢失了连续前置 owner。
- Aion 5.8 客户端 Q1877–Q1887 的 `SELECT_NONE(4762)` 同样只有
  `FINISH_DIALOG(1008)`“履行委托”，`DEFAULT_SUCCESS(10002)` 才提供
  `SELECT_QUEST_REWARD(1009)`；不存在当前 XML 注册的 NPC 接受链。

当前实现已为 Q1877–Q1887 补回连续前置集合，将唯一的 `unaccepted -> started` 入口恢复为
`ENTER_ZONE(TEMINON_LANDING_400010000) + start-eligible`，提交后只执行 `VISIBILITY_REFRESH`；
未接取阶段的 NPC 接受/拒绝路由已移除，`REWARD + QUEST_SELECT -> DEFAULT_SUCCESS` 报告页已补回。
原 `Quest2877To2887UrgentOrdersFlowTest` 已扩展为 22 个双阵营动态合同。

状态：`PENDING`。11 份 XML 的结构化解析、前置集合、唯一自动接取 owner 和奖励报告页静态断言已通过；
扩展后的专项 JUnit、production catalog、whitelist 与全量 E2E 尚未重跑，因为本轮没有构建授权。
真实客户端仍需分别验证 Teminon 区域自动接取、三次军衔击杀、报告页与奖励完成。

本轮 owner 盘点也核对了 `origin/history` 中全部 22 个 `registerOnKillRanked` handler；除这两条
区域自动接取链外，没有第三组同型旧入口。Q3735–Q3745/Q4735–Q4745 的 XML 虽使用
`kill-ranked`，旧 handler 明确注册 `addOnQuestStart` 的 NPC 接取合同，属于不同模式，保留不动。

### NO_MATCH：任务 26802/30603/30613 的 15 条计数和报告记录

修改前，Q26802 有 5 条、Q30603/Q30613 各有 5 条 `NO_MATCH`。这些记录都来自共享
`started/START` 节点把实时计数字段固定投影为零：`QuestMutationPlanner.matchesSourceNode` 会先精确匹配
source 投影，再检查 transition 条件，因此第一次击杀改变变量后，后续击杀、完成分支和报告动作都不再可达。

独立证据确认这不是审计 fixture 误报：

- `origin/history` 的 Q26802 Java handler 允许图书管理员和元素首领两组计数按任意顺序推进，并在最后一次
  击杀后立即进入 `REWARD`；当前 transition 的完成条件和动作已表达该合同，错误只在 source 投影锁死计数。
- `origin/history` 的 Q30603/Q30613 `monster_hunt` 各声明四个独立 6 位计数器，目标分别是
  `var0=9,var1=9,var2=1,var3=1`；旧 `MonsterHunt` 每次只递增命中的维度，不要求依次完成。
- Aion 5.8 客户端 Q30603/Q30613 的 `SELECT2(1352)` 都只有
  `SELECT_QUEST_REWARD(1009)`，旧正式模板同样在四个计数全部完成后才允许报告并显示奖励页 `5`。

当前实现将 Q26802 的 `started` 节点改为只约束 `START` 状态，保留原有两组计数的最后击杀完成分支；
Q30603/Q30613 恢复四个独立 6 位字段，移除错误的串行前置和第三次吉守护者击杀，将报告条件恢复为四个
计数全部完成。NPC `243852` 作为已确认的同模板替代目标保留，不回退为单一旧模板 ID。

`QuestCounterSourceProjectionProductionFlowTest` 通过正式 dispatcher 连续执行 Q26802 的 `30+2` 两种顺序，
以及 Q30603/Q30613 的 `9+9+1+1` 乱序击杀；专项测试 4/4、production catalog/whitelist 均通过。
最终全量 E2E 中 Q26802 为 `214/214 PASS`，Q30603/Q30613 各为 `41/41 PASS`，15 条 `NO_MATCH`
全部清除。状态：`PENDING`（自动化门禁完成，尚未进行真实客户端击杀和报告验收）。

Q49715 也存在相同 source 投影和差一完成问题，但属于用户明确暂缓的 `600060000`（丹纳利亚）任务，
本轮不修改，继续保留 6 条 `NO_MATCH` 及静态世界 `RUNTIME_REQUIRED` 记录。

协议模式曾确认 6 个任务的 8 条 transition 在页面包之前同步状态，顺序相反会让客户端用旧状态加载新页面。现已统一为：

```text
commit
-> SM_QUEST_ACTION
-> SM_DIALOG_WINDOW
```

| 任务 | transition 数 | 修复范围 |
|---:|---:|---|
| 1573 | 1 | `v2 -> reward` |
| 1607 | 1 | `z4 -> reward` |
| 2392 | 3 | 三个选择物品分支进入 `r1/r2/r3` |
| 2533 | 1 | `v1 -> reward` |
| 10032 | 1 | `s7 -> s7` 消耗任务物品后的成功页 |
| 24153 | 1 | `started -> reward` |

对应 XML 只调整 `sync-quest-state` 与页面动作的 after-commit 顺序，没有修改状态图、条件、物品、奖励或页面 ID。最终全量审计中：

```text
INVALID_DIALOG_PACKET=0
INVALID_PACKET_ORDER=0
STATE_CHANGED_WITHOUT_RESPONSE=0
```

### TRANSACTION_FAILURE：任务 9550/9553 的 4 条记录降为 PASS

两个任务的两种正式接取 action 原先都在 PLAN 阶段失败：

| 任务 | action/dialogId | source -> target | 失败证据 |
|---:|---:|---|---|
| 9550 | `QUEST_ACCEPT_1/1002`、`QUEST_ACCEPT_SIMPLE/20000` | `unaccepted -> started` | PLAN 阶段抛出 `unsupported start condition type: equipped` |
| 9553 | `QUEST_ACCEPT_1/1002`、`QUEST_ACCEPT_SIMPLE/20000` | `unaccepted -> started` | PLAN 阶段抛出 `unsupported start condition type: equipped` |

两份 XML 的 metadata 都声明：

```xml
<condition type="equipped" quest-id="125040015"/>
```

实时 `PlayerQuestStartEligibilityPort` 和 snapshot 条件求值器原本已支持该条件，但
`QuestMutationPlanner` 将 metadata 起始条件转换为 typed condition 时漏掉了 `equipped`。planner 现将它转换为
`QuestCondition.EquippedItem`，继续复用已有的装备事实语义：目标物品已装备时允许接取，未装备或装备事实未捕获时
fail closed；未知 metadata 类型仍抛出异常。

9550/9553 XML 保持不变。聚焦回归与最终全量审计确认四条接取 route 均为
`PASS + EXPECTED_TRANSITION_MATCHED`，状态进入 `START`，`TRANSACTION_FAILURE` 清零。

## 已排除的误报

### AFTER_COMMIT_FAILURE：106 条降为 PASS

旧报告中的 106 条失败都发生在事务 commit 后：非对话事件执行 `SHOW_QUEST_PAGE` 或 `SHOW_SELECTION_PAGE` 时，E2E fixture 没有把事件标记为 targetless，真实 `PlayerQuestDialogPort` 因而按普通 NPC 对话错误地要求 interaction objectId。

这与生产路径不一致：

- `PlayerQuestEventPort` 只有 `TalkToNpc` 携带权威对话所有者，其余事件通过 `withTargetlessDialog()` 明确使用 objectId 0。
- 旧 handler 的 `sendDialogPacket` 在 `QuestEnv.visibleObject == null` 时同样发送 objectId 0；`defaultOnLvlUpEvent` 和 `defaultOnZoneMissionEndEvent` 本身只启动/同步任务，不构造 NPC 对话目标。
- E2E fixture 现已按该协议边界构造快照，NPC 对话仍保留权威 objectId 校验。

修正前的误报分布：

| 事件和动作 | 原 transition 数 |
|---|---:|
| `LEVEL_UP + SHOW_QUEST_PAGE` | 56 |
| `ZONE_MISSION_END + SHOW_QUEST_PAGE` | 33 |
| `KILL_NPC + SHOW_SELECTION_PAGE` | 14 |
| `KILL_NPC + SHOW_QUEST_PAGE` | 3 |

修正后的全量协议不变量：

```text
AFTER_COMMIT_FAILURE=0
INVALID_INTERACTION_OBJECT=0
INVALID_DIALOG_PACKET=0
INVALID_PACKET_ORDER=0
STATE_CHANGED_WITHOUT_RESPONSE=0
```

因此原列出的 61 个任务不再属于错误队列，也没有批量修改这些任务的 XML。

## 本轮已修复

### 任务 14047：电影循环、不可达页面和重复佩托

真实客户端复现确认 NPC 802052 的 action 2376 播放电影 421 后仍返回原页面并形成循环；同一坐标还存在任务 NPC 802052 和普通 NPC 204653 两只同名佩托。Aion 5.8 客户端合同为 `SELECT5(2375) -> SELECT5_1(2376) -> SETPRO11(10010)`，而原 XML 只播放电影，没有发送 2376 页面。

当前修复在电影 421 后发送 2376 页面，恢复 798154、204574、802051 的客户端子页面 action 路由，并按旧 handler 顺序先同步已提交状态再关闭、飞行或播放电影。错误阶段点击 802051/802052 时不再把 action ID 10009/10010 当页面发送，而是明确关闭窗口；重叠且无任务引用的 204653 静态出生点已删除。`PAGE_NOT_IN_CLIENT` 已从 11 清零。用户已使用包含该修复的服务端完成真实客户端验收：802052 的 2375 -> 2376 -> 10010 页面链、飞行、唯一佩托和奖励流程均可继续。

客户端崩溃恢复新增 `ENTER_WORLD` 任务合同：`s4(var0=4) -> s3(var0=3)`、`s5(var0=5) -> s3(var0=3)`，仅发送 `PACKET_ONLY` 状态同步；`s6`、奖励和完成阶段不回退。恢复只放在重登入口，避免 `LOG_OUT` 成功回退后又在 `ENTER_WORLD` 二次回退。此前专用测试、catalog/whitelist 和全量 E2E 已通过；两条新增 transition 都是 `EXPECTED_TRANSITION_MATCHED/PASS`。用户已在真实客户端完成断线/重登恢复验收，确认崩溃后回到可重新触发飞行的 `s3`。

随后真实客户端确认最后击杀“背叛者伊卡罗尼斯”不会推进。副本静态出生的是第一形态 `233877`，其模板 AI `betrayer_icaronix` 应在 75% 生命值时生成最终击杀形态 `214599`；旧 handler 和原任务合同也只监听 `214599`。问题由两个改动共同造成：零售空 pattern 绕过了该自定义 AI，同时候选 XML 为迁就静态出生点错误地改为监听 `233877`。当前修复保留 `betrayer_icaronix` 脚本 AI，并恢复 `s5(var0=5) + KILL_NPC(214599) -> s6(var0=6)`，提交后先同步任务状态再播放电影 422。

这次复现也证明此前任务 14047 的 `58/58 PASS` 只验证了“由当前 XML 自己生成事件后能命中当前 XML”的自洽性，没有覆盖 `233877 -> 214599` 的实际 AI 形态切换。用户随后已在真实客户端完成击杀验收，确认 `214599` 出现、击杀后播放电影 422 并推进任务；新增聚焦测试、catalog/whitelist 和全量 E2E 已补跑通过，当前 Q14047 仍为 58/58 PASS。

## 任务 26800：永恒之塔入口对象与迁移状态链

真实客户端复现为使用 `731711`“发光的永恒之塔碎片”读条后没有进入永恒之塔。该对象属于任务
`20527`：其生产定义只接收 `USE_OBJECT/ACTION_ITEM_USE` 并推进任务事件，不执行传送，也不属于任务
`26800`。任务 26800 的正式世界入口是 portal NPC `806082`，`dialog=104 -> loc=2201200 -> world=220120000`；
到达永恒之塔并完成 `806233` 对话后，再使用 `806029`，通过
`dialog=10000 -> loc=3015400 -> world=301540000` 进入知识书库副本。

静态核对同时发现当前 `26800.xml` 存在独立迁移错误：原定义把进度压缩为 `var0=0/1`，丢失旧 handler
的 `0 -> 1 -> 2 -> REWARD(3)` 阶段；缺少客户端可见的 `4763` 接取动作、永恒之塔区域推进和
`806233 + SET_SUCCEED(10255)` 路由；电影 932 未限定在 `var0=2` 的知识书库区域入口；并错误地让
`806079`、`806233`、`806149` 三个 NPC 都能报告和领奖。

当前工作区已恢复显式阶段、客户端页面/action、区域推进和电影顺序，并将最终报告/领奖 owner 限定为
`806149`。`Quest26800ClientDialogAlignmentTest` 锁定完整状态、事务动作、after-commit 顺序、错误对象归属及
两段 portal/world 映射。专项测试 `4/4`、生产 catalog `6/6` 和 whitelist `1/1` 已通过；生产 catalog
共编译 `6200` 条任务，失败 `0`、whitelist 违规 `0`。尚未执行真实客户端复测，当前状态为
`IMPLEMENTATION_COMPLETE / PENDING_CLIENT_ACCEPTANCE`。

## 尚不能判为任务错误

### CLICK_NO_RESPONSE 已清零

当前报告不再把 dispatcher 之外的交互物 AI 收尾误分类为普通点击无响应。原 57 条记录均明确保留为
`RUNTIME_REQUIRED`，不能据此批量修改任务 XML。

### RUNTIME_REQUIRED：世界可达性与交互物 AI 边界

本轮新增静态世界 oracle 后，`RUNTIME_REQUIRED=59,265` 拆分为 `STATIC_WORLD=59,208` 和交互物 AI 收尾 `57` 条。
`STATIC_WORLD` 只表示 `TalkToNpc` 没有已加载地图上的常驻 `<spot>`，且任务定义没有 `SpawnNpc/SpawnNpcRandom`；它是运行时/静态数据队列，不是可以直接修改 XML 的错误结论。

任务 49713 共 73 条编译 transition：7 条仍通过（6 条 `FAST`、1 条 `CM_USE_ITEM`），其余 66 条对话 transition 因 800936、800937、800938 没有静态出生点而标记为 `RUNTIME_REQUIRED`，每个 NPC 各 22 条。当前 `world_maps.xml` 也没有丹纳利亚 `600060000`，因此不能通过猜坐标或固定传送把它们伪装成 PASS。

地图 `600060000`（丹纳利亚）相关任务按用户决定暂时跳过：继续保持
`STATIC_WORLD/RUNTIME_REQUIRED`，不补地图、不猜 NPC 坐标、不将这些记录升级为确定性 XML 错误。
只有后续取得该地图、出生点或真实运行时证据后才恢复分诊。

其中剩余 57 条记录都是无事务动作、无 after-commit 的交互物路由；任务 metadata 声明了对应掉落，正式运行时由
`QuestItemNpcAI2` 在 dispatcher 返回后打开掉落列表。批量 fixture 尚未执行这段 AI 收尾，因此保留
`RUNTIME_REQUIRED`，不把它们判为任务错误或 PASS。

原 1,766 条按以下顺序重新执行并归因：

| 原 fixture 缺口 | 原 transition 数 | 当前结论 |
|---|---:|---|
| NPC faction start/completion | 1,093 | 接入真实 `PlayerQuestEffectPort` 生命周期后清零；1,039 条转 PASS，其余保持原证据/路由分类 |
| class/effect/message/emotion ports | 40 | 接入真实端口和轻量玩家协议状态后全部转 PASS |
| 条件和事件事实 | 633 | 装备、权限、DP、完成次数、制作、PvP、NPC HP 等确定性事实已接入；不再标记为运行时缺口 |

最后 74 条旧 `RUNTIME_REQUIRED` 的具体归因：

- 任务 1929 的 23 条 source-less 全局 transition 现在按 `status-is` 和变量条件选择源投影，全部 PASS。
- 任务 1929 另有 22 条带固定 source 的恢复 transition，补齐 `unequip-item` 装备事实后由 NO_MATCH 转 PASS。
- 47 条 compact counter 在 `required=1` 时生成严格低于字段最小值的 continuing 分支；正式 dispatcher 必然命中 completion sibling，透明归为 `AMBIGUOUS_ROUTE`，不是缺少运行时事实。
- 任务 9550/9553 的 4 条记录在 planner 支持 `equipped` 后全部转为 `PASS`。

本轮 `UNSUPPORTED_SCENARIO_FACTS=59,208` 与 `STATIC_WORLD=59,208` 对齐，表示这些 transition 在缺少静态世界事实时没有进入内存 dispatcher；剩余 57 条才来自上述
`QuestItemNpcAI2` 外部收尾边界。

`NO_MATCH`、`AMBIGUOUS_ROUTE` 和 `EVIDENCE_REQUIRED` 同样保留为证据队列，不因报告清零目标而修改任务 XML。

## 后续处理顺序

1. 保留 6 个任务、8 条 transition 的协议顺序回归和全量核心门禁。
2. 任务 14047 的客户端流程、聚焦测试、catalog/whitelist 和全量 E2E 均已通过，并已按“修复提交 -> Playbook 提交”完成交付。
3. 调查 `NO_MATCH`、`AMBIGUOUS_ROUTE`、`RUNTIME_REQUIRED` 和 `EVIDENCE_REQUIRED`，只把具有独立证据的记录升级为错误队列。

## 验证基线

最新聚焦门禁、catalog/whitelist 和全量审计结果：

```text
Quest49713RetailFlowAlignmentTest
Quest26802ClientDialogAlignmentTest
QuestCounterSourceProjectionProductionFlowTest
QuestDialogOrderAuditTest
QuestE2eInfrastructureTest
QuestDefinitionCatalogManifestTest
ProductionCatalogWhitelistVerificationTest

PRODUCTION_COMPILE_OK=6200
PRODUCTION_COMPILE_FAILURES=0
PRODUCTION_WHITELIST_VIOLATIONS=0

FULL_E2E_ROWS=396787
FULL_E2E_PASS=333121
AFTER_COMMIT_FAILURE=0
PAGE_NOT_IN_CLIENT=0
RUNTIME_REQUIRED=59265
STATIC_WORLD=59208
EVIDENCE_REQUIRED=3975
NO_MATCH=269
AMBIGUOUS_ROUTE=157
TRANSACTION_FAILURE=0
CM_USE_ITEM=90
Q49713=7/73 PASS (FAST=6, CM_USE_ITEM=1; 66 STATIC_WORLD/RUNTIME_REQUIRED)
Q26802=214/214 PASS
Q30603=41/41 PASS
Q30613=41/41 PASS
```

`QuestE2eInfrastructureTest` 的过期 1920 自动弹页断言已经修正：任务 10110 的
`LEVEL_UP`/`ZONE_MISSION_END` 锁定 `QUEST_ACTION -> DIALOG_WINDOW` 及无目标
`objectId=0`；任务 1920 单独锁定进入 `START`、仅发送 `QUEST_ACTION` 且不发送
`DIALOG_WINDOW`。通用 E2E 门禁当前为 40/40 通过。依赖任务专用 XML 的包顺序专项合同和依赖
`equipped` planner 修复的专项合同不混入通用 E2E 门禁。

最终产物：

```text
target/quest-e2e/quest-e2e-report.jsonl
target/quest-e2e/quest-e2e-summary.csv
.agent/summary/quest-e2e/summary.md
.agent/summary/quest-e2e/triage.md
```

## 秒杀边界补充

任务 14047 的副本入口形态 `233877` 在一次攻击中直接归零时，不会经过原有的
`hpPercentage <= 75` 检查；`handleDied()` 现在使用同一个幂等生成门，兜底生成最终
击杀形态 `214599`。正常降至 75% 或更低时仍由阈值路径生成，两个路径最多生成一次。

新增 `Betrayer_IcaronixAI2Test` 锁定该合同。该测试不启动副本或全局服务，仅记录生成
模板 ID；用户已完成真实客户端和重新打包服务端的击杀验收。

真实客户端随后确认 `214599` 被击杀后能播放电影 422 并推进任务，证明 75% 阶段生成
的是任务监听的最终形态；该战斗 NPC 没有头顶任务标记不影响击杀事件归属。最终在
NPC `278500` 领取奖励时，服务端错误发送了 Q14047 不存在的 `DEFAULT_SUCCESS(10002)`，
客户端显示 `load fail! Quest_Q14047.html (HtmlPageId 10002)`。旧 handler 的
`sendQuestEndDialog(USE_OBJECT)` 会直接发送奖励窗口 page `5`，因此当前 XML 已恢复为
`SHOW_SELECT_QUEST_REWARD_WINDOW1`，并由 `Quest14047ClientDialogAlignmentTest` 锁定。用户随后完成客户端端到端验收，确认 278500 对话不再出现 `HtmlPageId 10002` 的 load fail，并能正常进入奖励选择和完成流程。

### 任务 14047 验收与 Playbook 门禁

客户端验收状态已完成，证据来源为用户在真实客户端上的端到端实测，本摘要不声称由 agent 独立启动客户端复现。索引快照的聚焦测试、catalog/whitelist 和当前工作区全量 E2E 均已完成，整体修复门禁为 `ACCEPTED_NEW_PATTERN`。

最终判定确认两个新模式：飞行专属不可达阶段需要显式可重入恢复合同；血量阈值变身需要以同一幂等门覆盖直接秒杀。修复提交为 `8b058d4b4de747d12df9e9af63617619d5eefcf5`，Playbook 代表案例提交为 `bf39d61b00b8fb5edc4df036579ae4abf41a9c9d`；两段飞行传送、GM 寻找传送和副本崩溃回退合同的补充提交为 `e7b01e93e30a2564a610e07d737cfbbe85f56131`。

## 状态同步先于页面的协议顺序修复

1573、1607、2392、2533、10032、24153 的目标 transition 只调整 `after-commit` 顺序：
`sync-quest-state` 必须先于 `SHOW_QUEST_PAGE`，source、target、条件、priority、事务动作和页面 ID
保持不变。`QuestPacketOrderRegressionTest` 既锁定编译 IR，也通过真实
`CM_DIALOG_SELECT -> QuestEngine -> QuestProductionDispatcher -> SM_QUEST_ACTION/SM_DIALOG_WINDOW`
回环验证 objectId、questId 和包顺序。

专项测试 7/7、通用 E2E infrastructure 37/37 通过；生产 catalog 6200 条编译成功、失败 0、
白名单违规 0。Aion 5.8 客户端资源确认相关页面/action 存在，但本批没有逐任务执行人工客户端点击。
修复提交为 `6a77337dbfd8cfec60f9daeb1125e51b976d56a3`，Playbook 提交为
`3f9d41b`。

## 装备物品起始条件生产门禁

9550/9553 的 metadata 都要求装备 `125040015`。planner 现将 `equipped` 起始条件转换为
`QuestCondition.EquippedItem`；已装备时允许接取，明确未装备或装备事实未知时 fail closed。
独立 `QuestEquippedStartProductionFlowTest` 使用 Aion 5.8 客户端页面/action 证据，并让
`QUEST_ACCEPT_1` 经过真实 `CM_DIALOG_SELECT -> QuestEngine -> production dispatcher -> coordinator`
回环，锁定状态、objectId、questId、页面与包顺序。

`QuestMutationPlannerTest,QuestEquippedStartProductionFlowTest` 共 22 个测试通过；E2E infrastructure、
包顺序、catalog 和 whitelist 核心门禁通过，生产 catalog 6200 条编译成功、失败 0、白名单违规 0。
修复提交为 `cc7aabea521af6b27bab129dc4be5ed63c0f3e07`，Playbook 提交为
`47077bdf3`。

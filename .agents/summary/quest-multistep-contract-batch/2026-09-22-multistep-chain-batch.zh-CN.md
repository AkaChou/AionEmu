# 任务书逐行对话链批量修复（13 个任务）— 2026-09-22

任务 1192 的「三步交付链被压成一个进行中状态」不是孤例。本批把同型任务按客户端任务书逐行重建，
使 wiki 的每一步都能拿到各自的状态与独立的 GM 命令，并补上门禁与回归测试。

## 1. 缺陷族

客户端 `data_unpacked/Dialogs/QUEST_Q<id>.html` 的 `quest_summary` 逐行列出 n 个步骤（末行领奖），
每行各自有一条以 `HACTION_SETPRO<i>` / `HACTION_SELECT_QUEST_REWARD` 收口的对话链；
服务端迁移时把整条链压成单个 `started(var0=0)`：

- 所有步骤 NPC 共用 `HACTION_SETPRO1`，且直接 `started -> reward`；
- 客户端第 2..n 行的动作（`SETPRO2..`）在 XML 里没有路由（`audit_unrouted_progress_actions.py` 候选）；
- 玩家跟第 1 个 NPC 说完话即可领奖，中间步骤永远不可达；
- wiki 侧表现为每一步的 GM 命令都退化成 `//quest set <id> START 0`。

## 2. 修复合同（与已提交的 1192 一致）

每个客户端行占一个 `var0`：

| 行 | 状态 | var0 | 进入方式 |
| --- | --- | --- | --- |
| 0 | `started` (START) | 0 | 接取 |
| i (1..n-2) | `s<i>` (START) | i | 第 i 行客户端 NPC 的 `SETPRO{i}` |
| n-1 | `s<n-1>` (START) | n-1 | 第 n-1 行客户端 NPC 的 `SETPRO{n-1}` |
| n-1（领奖行） | `reward` (REWARD) | n-1 | 领奖行 NPC 自己的 `SELECT_QUEST_REWARD` |
| 完成 | `complete` (COMPLETE) | — | 领奖行 NPC 的奖励窗口动作 |

契约要点：

- 每行一个 START 状态 → `//quest set <id> START <行号>` 可以精确跳到该行（wiki 每步一条独立命令）；
- 每步入口页取自该行客户端链首页（`SELECT2`→`SELECT3`→…），不再所有步骤都下发 `SELECT2`；
- 只有领奖行 NPC 拥有 `reward` 行路由与 `npc-complete`；
- 交付条件与物品回收（`has-item` / `remove-item`）从旧路由原样搬到领奖行的提交分支；
- 旧存档自愈边保留：`enter-world + REWARD + var0=0 → var0=n-1`。

## 3. 本批修复的 13 个任务

| 任务 | 行数 | 步骤 NPC（按行） | 领奖 NPC | 领奖页 |
| --- | --- | --- | --- | --- |
| 1183 | 3 | 730013 SELECT2, 730014 SELECT3 | 730012 | SELECT5 |
| 1319 | 9 | 203923, 203910, 203906, 203915, 203907, 798050, 798049, 205240 | 203908 | SELECT10 |
| 1483 | 3 | 203940 SELECT2, 203944 SELECT3 | 798127 | SELECT5 |
| 1514 | 3 | 204582 SELECT2, 204505 SELECT3 | 203831 | SELECT5 |
| 1721 | 3 | 278503 SELECT2, 278502 SELECT3 | 278518 | SELECT5 |
| 1724 | 3 | 278591 SELECT2, 278599 SELECT3 | 278594 | SELECT5 |
| 2449 | 2 | 798115 SELECT1 | 798080 | DEFAULT_SUCCESS |
| 2646 | 4 | 204777, 204700, 204702 | 204817 | SELECT5 |
| 2692 | 4 | 204108, 279027, 279029 | 212164 | SELECT5 |
| 2767 | 3 | 279026, 279061 | 279004 | SELECT5 |
| 3966 | 4 | 203994, 204030, 204568 | 798391 | SELECT5 |
| 3968 | 4 | 798176, 204528, 203927 | 798390 | SELECT5 |
| 4501 | 3 | 204340, 204348 | 204728 | SELECT5 |

三方证据（客户端 quest_summary 行 / 客户端对话链 / retail 步骤）由 `dump_chain_evidence.py` 导出；
逐任务计划见 `batch-plan.tsv`。

## 4. 生成器两版：v1 的内容丢失事故（必须记住）

`apply_multistep_chains.py`（v1）整文件重渲染，**丢掉了块内子元素与非本行内容**：

- 2449：`npc-complete` 的 4 个 `<choice>` 被丢弃（多档可选奖励消失），且写出客户端不存在的
  `page="SELECT_SUCCESS"`（枚举里没有该页，整个任务无法编译）；
- 3966 / 3968：`has-item 182206121/182206123` 条件与配套 `remove-item` 被删（交付物品不再校验、不再回收）；
- 1483 / 1721 / 1724：其它入口 NPC 的 `QUEST_SELECT` 路由与 `give-item` 被删；
- 1183 等：旧路由的 `close-dialog`、重复 `preview` 等被静默删除。

`apply_multistep_chains_v2.py` 改为**块级保留**：只删除四类块（步骤 NPC 的 SETPRO 路由、步骤/领奖 NPC 的
入口与翻页路由、领奖 NPC 的提交路由（条件/动作原样搬运）、非领奖 NPC 的 completion 与重复提交路由），
其余块保留原始文本；节点块在已满足「每行一个状态」时原样保留。

安全闸门：

- 页面名 / 动作名必须命中 `QuestDialogPage` / `QuestDialogAction` 枚举，否则整任务 SKIP；
- 领奖行分支的页面必须在本任务客户端页面索引（`docs/quest/client-dialog-mapping/quest-dialog-pages.csv`）中，
  否则丢弃该分支并打印告警——3966/3968 旧定义里的 `SELECT6(2716)` 正是跨任务残留页，已按证据丢弃。

## 5. 验证证据（分层）

| 层 | 命令 / 脚本 | 结果 |
| --- | --- | --- |
| 结构 | `xmllint --noout --schema quest_definition.xsd <13 个 xml>` | 13/13 通过 |
| 编译 | `QuestDefinitionXmlCompiler.compile`（探针逐个编译） | 13/13 OK |
| 回归测试 | `mvn -B -q test -Dtest=QuestMultistepChainContractTest,Quest1192StepChainContractTest,QuestClientContractGateTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest` | 全绿（新增 5 个测试方法锁定合同） |
| 全目录编译 | `ProductionCatalogWhitelistVerificationTest` | `PRODUCTION_COMPILE_OK=6189`，失败 0，白名单违规 0 |
| 客户端契约门禁 | `QuestClientContractGateTest` | 未新增指纹（基线 0 缺陷） |
| 无路由推进动作 | `audit_unrouted_progress_actions.py` | 80 → 67（本批 13 个全部出列） |
| 行 ↔ 状态对齐 | `audit_reward_row_vs_client_steps.py` | ROW_ALIGNED 2599→2628，ROW_BEHIND 246→217，ROW_STATE_ALIGNED 2381→2405，MISSING_LAST_ROW 121→102 |
| 块级保留审计 | `audit_step_chain_preservation.py <13 个 id>` | 13/13 OK（物品/条件不消失、唯一 completion、页面动作均登记） |

`QuestEngine` 包全量测试（1649 个用例）另有 15 failures + 5 errors，**与本批无关**：
把 13 个 XML 临时还原到 HEAD 后重跑，同样 20 个用例失败（涉及 20529/21114/28808/3057/15101/25608/25060/
10526/20526/25512 等其它任务与其它并行改动）。本批未引入新失败。

## 6. 仍未自动重建的候选（20 个）

`1371, 1430, 1479, 1582, 1643, 1922, 1938, 2223, 2239, 2289, 2307, 2372, 2513, 2600, 2947, 2962, 3050,
3088, 4542, 4942`（另有 4712 的 NPC 与 retail 对不上）。

按优先级：

1. B 类「页面已下发但按钮无路由」：1430（`select1_1_1 → SETPRO1`）、1643（`select1_1 → SETPRO1`）、
   2513、2962、4542（2449 本批已修）；这些只需要补一条推进路由，风险最低；
   **→ 已完成（见 `2026-09-22-b-class-unrouted-buttons.zh-CN.md`）：1430/2513/2962 随 `cd7ce6447` 入库，1643/4542 本轮修复。**
2. 客户端链数量 ≠ 行数-1（链不完整或存在子状态分支）：需要先按客户端页面确认真实层级；
3. retail 无步骤（`report_to_many` / 无条目）：步骤 NPC 只能从客户端行文本反推，需要人工核对 NPC 名字。

## 7. 状态

- 生产 XML：13 个文件已改，**未提交**（工作树另有其它并行改动的 66 个任务 XML，提交时必须按精确路径 stage）；
- 新增测试：`src/test/java/com/aionemu/gameserver/questEngine/definition/QuestMultistepChainContractTest.java`；
- 客户端实机验收：**PENDING**（静态/编译/测试已通过，尚未进客户端跑 13 条链）。

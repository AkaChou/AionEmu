# 1192 同型候选复核（全库） / Candidate sweep for the 1192-style defects

- 日期：2026-09-21 23:5x
- 仓库：`/Users/mc/IdeaProjects/AionEmu-test`（HEAD `8623f4406`，复核时工作树含其它并行任务的未提交改动）
- 关联：本目录 `2026-09-21-1192-step-chain-repair.zh-CN.md` 第 5 节；memory-bank `QE-004` / `QE-005` / `QE-051`
- 验收状态：**STATIC_AUDIT_DONE**（脚本复核 + 抽样 XML/客户端比对）；**PENDING_CLIENT**（未实机）

## 1. 结论

**候选没有被修复。** 当前 HEAD 重跑两套审计后，唯一从候选清单消失的任务是 **1192 本身**（本任务已修）：

| 口径 | 报告首次生成（18:32） | 当前 HEAD（23:5x） |
| --- | --- | --- |
| 客户端推进动作在 XML 中无路由 | 83 | **80**（修正审计解析后，83 − 3 个假阳性） |
| 与「行/状态缺行」交叉的候选 | 67 | **37** |
| 其中与 1192 同型（多行 + 状态缺口 + 动作无路由） | 31 | **29** |
| 行/状态已被今日「领奖行批次」收口、但动作仍无路由 | 0 | **29**（旧 67 中全部 29 个，最后改动均为 `7a7d27809`） |
| 噪声/非本类 | — | **8** |

- 80 = 72 仍缺路由 + 8 噪声；72 = 29「行状态未收口」+ 43「行状态已收口但动作仍无路由」。
- 今日其它 agent 的领奖行批次（`7a7d27809` / `bd6c9e224` / `daa1dd5e4`）**只补了行状态与领奖投影**（例：1319 的 diff 只有 nodes + enter-world 自愈边），没有补任何客户端推进动作路由。

## 2. 审计工具修正（本轮）

`audit_unrouted_progress_actions.py` 原先只解析 `action="X"`，漏掉 `actions="X Y"` 多动作写法。
两者都会展开为真实 `talk(...)` 路由（`QuestXmlBlockExpander`：dialog 的 `actions=` 与 `npc-complete` 的
`<preview actions="..."/>`）。补上后候选 83 → 80，去除 3 个假阳性：

| 任务 | 写法 | 说明 |
| --- | --- | --- |
| 4338 | `<dialog type="TALK_TO_NPC" npc-id="790020" actions="SETPRO3 SELECT3"/>` | s2 → s3 的推进路由本来就存在 |
| 2443 | `<dialog type="TALK_TO_NPC" npc-id="204403" actions="SETPRO1 QUEST_ACCEPT_1"/>` | 接取/推进同边 |
| 2002 | `<preview actions="SELECT_QUEST_REWARD SETPRO8"/>` | 领奖页 select8 的重开预览动作，reward 状态下有路由 |

新增 `candidate_sweep.py`：把无路由清单与行/状态审计交叉，并额外判定「动作所在客户端页是否被 XML 下发」，
用于区分「按钮点下去没反应」与「页面根本没下发（错页/跳页）」。

## 3. 当前 72 个「仍缺路由」的分类

- **A. 页面从未下发（66 个）**：XML 从不 `SHOW_QUEST_PAGE` 该动作所在页 → 该步骤的客户端对话页在服务端流程里不存在。
  1192 修复前即此形态（`select3`/`select3_1` 从未下发）。代表作：1183、1319、1483、1540、2278、2917。
- **B. 页面已下发但按钮无路由（6 个）**：1430、1643、2449、2513、2962、4542 —— 服务端会把这些页送到客户端，
  但按钮动作在 XML 里没有任何路由（点击无响应/硬卡死嫌疑，优先级最高）。
- **C. 噪声（8 个）**：9554–9557（客户端仅 1 行）；1000、2000（XML 无任何 `SHOW_QUEST_PAGE`，过场/自动任务）；
  1489、1908（XML 只有元数据的 stub，没有 nodes/transitions）。

## 4. 抽样证据（XML ↔ 客户端）

| 任务 | 客户端（quest_summary 行 / 按钮链） | XML 现状 | 判定 |
| --- | --- | --- | --- |
| 1183 | 3 行；select1→SELECT1_1；select2/select2_1→SETPRO1；select3/select3_1→SETPRO2 | 只有 started/reward 两个状态；730012/730013/730014 三个 NPC 各自 `SETPRO1 → reward` 直达领奖；无 SELECT3_1/SETPRO2 路由 | 与 1192 修复前同型（跳过第 2 步 + 提前领奖） |
| 1319 | 9 行；select2..select9 各一条链（SETPRO1..SETPRO8），select10→SELECT_QUEST_REWARD | 行状态已由 `7a7d27809` 补齐（var0=0..8）；动作只有 SELECT2_1/SETPRO1；select3..select9 页从未下发 | 行/状态已收口，对话链仍错页（第 2 步起复用第 1 步文本） |
| 1483 | 3 行；第 2 步是 select3/select3_1→SETPRO2 | 第 2 步仍复用 SELECT2/SELECT2_1/SETPRO1 且 target=reward；203940/203944 都能领奖 | 同上 |
| 1900 | 5 行；第 5 步 select6→select6_1→SELECT6_1_1 | 前 4 步已按页/动作路由（SELECT2_1..SELECT5_1 + SETPRO1..4）；缺 SELECT6_1/SELECT6_1_1 | 尾部一步缺失 |
| 4937 | 8 行；第 7 步 select7→SELECT7_1→select7_1 | 已引用 SELECT7_1/SELECT7_2 页，但 select7 页与 SELECT7_1 动作无路由 | 末步跳页 |
| 1430 | select1_1_1 的按钮是 SETPRO1 | XML 已下发 SELECT1_1_1，但全文无 SETPRO1 路由 | 按钮点击无响应（B 类） |

## 5. 与生产门禁的关系（为什么门禁全绿但仍有候选）

- `QuestClientContractGateTest` 的 TSV 基线当前只有注释（`BUTTON_WITHOUT_ROUTE=0`、`PAGE_NOT_IN_TASK_HTML=0`），
  `QuestPrematureRewardRouteAudit` 也要求 0 违规。
- 但门禁的致命集合只覆盖「**IR 已下发**页面中的按钮缺路由」；「客户端有动作的页面**从未被 IR 下发**」
  只记为 `CLIENT_PAGE_UNREACHED`（`EVIDENCE_REQUIRED`，不计失败）。2026-09-14 生成的
  `docs/quest/client-dialog-mapping/quest-order-audit.csv` 里该状态 4489 行、`EVIDENCE_REQUIRED` 7319 行。
- 因此：门禁全绿 ≠ 候选已修；1192 正是门禁没拦住、由实机暴露的缺陷。A 类 66 个要判真伪，必须像 1192
  那样逐个核对客户端 `dialog_id`/按钮链（`docs/quest/client-dialog-mapping/quest-dialog-action-details.csv`）与出场 NPC，
  不能按动作名机械补路由。

## 6. 明细清单

- 全量：`candidate-sweep.tsv`（列：`class` / `row_status` / `missing_actions` / `missing_action_pages` /
  `shown_but_unrouted` / `page_never_shown` / `xml_states` / `last_commit` / `reason`）。
- 29 个同型（行状态未收口 + 仍缺路由）：
  `1183 1371 1479 1514 1582 1643 1721 1724 1922 1938 2223 2239 2289 2307 2372 2600 2646 2692 2767 2947 2962 3050 3088 3966 3968 4501 4542 4712 4942`
- 43 个行状态已收口但动作仍无路由：
  `1111 1319 1322 1354 1394 1414 1430 1472 1483 1484 1540 1574 1900 1937 1989 2278 2279 2428 2449 2480 2513 2515 2523 2538 2901 2902 2903 2904 2912 2913 2917 2920 2964 3093 3209 3218 3970 3973 4052 4218 4905 4906 4937`
- 14 个本次新进入清单（旧交叉未列出，行状态本来就对齐）：
  `1111 1354 1394 1414 1989 2901 2902 2903 2904 2920 2964 3209 3218 4218`
- 6 个 B 类（页面已下发、按钮无路由）：`1430 1643 2449 2513 2962 4542`
- 8 个噪声：`9554 9555 9556 9557`（单行）、`1000 2000`（无对话页）、`1489 1908`（stub）

## 7. 建议

1. **先修 B 类 6 个 + 同型 29 个**：合同同 1192 / QE-004 / QE-005（显式中间状态 + 每步用客户端可见 action/page +
   唯一 reward owner + 旧存档自愈边），需要用户授权后按任务逐个核对客户端 dialog_id 再分批修改并补聚焦测试。
2. A 类其余 43 个先做「客户端页面是否属于该任务链」的证据判定（生产门禁口径为 `EVIDENCE_REQUIRED`），
   判定为真缺陷后再修；不要机械补路由。
3. 客户端实机验收仍为 `PENDING_CLIENT`。

## 8. 产物与命令

- `python3 .agents/summary/quest-1192-step-chain/audit_unrouted_progress_actions.py` → `unrouted-progress-actions.tsv`（80）
- `python3 .agents/summary/quest-1192-step-chain/audit_reward_row_vs_client_steps.py` → `audit-output.tsv` / `audit-run.log`
- `python3 .agents/summary/quest-1192-step-chain/candidate_sweep.py` → `candidate-sweep.tsv`

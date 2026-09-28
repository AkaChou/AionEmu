# P2a 报告：`dialog_exits` 行级可达性普查 v2（只读 · 2026-09-28）

> 授权：用户「开始」；范围 = 只读普查（零形状、零 Maven、红集不解冻）。
> 产物：本报告 + `census-dialog-exits-v2.tsv`（3938 行逐行矩阵）+ `census_dialog_exits_v2.py`（可重放）。
> 上游：P2 立项书 `../2026-09-28-p2-dialog-exits-shrink-charter.zh-CN.md`。

## 0. 一句话结论

**104 个阶梯 token 全部可删**（SELECT1_1 × 98 + SELECT1_1_1 × 6，分布在 98 行），
其余 **339 个 token 保留**（体级四 token 325 + SELECT_NONE_1 14），**判不了 = 0**。
每个删候选都有客户端证据指针（`quest-dialog-pages.csv` active/exact 行 + 源文件 sha256 前 8 位）。

## 1. 方法（与批 0 R1 的差异）

1. **读取点守卫**：脚本断言 `requires(` 仍是 12 处 / 3 文件（SimpleTalk 10、DD 定义 1、DD 采集 1），
   任何漂移即 fail-fast。
2. **新编译路径重算**：阶梯 token 的唯一读取点 `RetailSimpleTalkDefinitionCompiler:1212/1214`
   位于 `else` 分支，进入条件（逐位复刻）：
   `has NPC_START blocks ∧ !systemGrant ∧ ¬all(acceptSourcesWithinSegment)`；
   其中 `acceptSourcesWithinSegment` 复刻自编译器（`block.extra` 首段 × `servesFinishDialog` 的 R 记录匹配）。
3. **客户端证据列（新增）**：按 `build_quest_client_dialog_exits.py` 同一映射
   （`quest-dialog-pages.csv` / `quest-dialog-action-details.csv`，active/exact）反查每个 token 的
   页/按钮证据与源文件 sha256；**无证据的 token 一律判"留"**（保守，不因"看起来没人用"而删）。

## 2. 读取点核对（12 处逐点）

| 读取点 | token | v2 裁定 |
|---|---|---|
| SimpleTalk `:1212/:1214` | SELECT1_1 / SELECT1_1_1 | **不可达**：当前数据 256 个有块行 `all(within)=true` ⇒ 走 canonical 段，else 分支 0 次进入 |
| SimpleTalk `:1411` | SELECT2_CONTINUE | 可达：buildChain 体级无条件读 |
| SimpleTalk `:1438/:1439` | SELECT5_CHECK / _SIMPLE | 可达：体级 |
| SimpleTalk `:1465` | SELECT6 | 可达：体级 |
| SimpleTalk `:1701/:1702/:1727/:1733` | SELECT5_CHECK* / SELECT6 | 可达（reportFlowChain，buildChain:1323 调用；体级已覆盖同 token） |
| DD 定义 `:163` | SELECT_NONE_1 | 可达（DD 行唯一读点） |
| DD 采集 `:64` | SELECT_NONE_1 | 可达（采集行唯一读点） |

## 3. 三态裁定统计

| 裁定 | token 数 | 明细 |
|---|---|---|
| **删** | **104** | SELECT1_1 98 + SELECT1_1_1 6（98 行；全部 = `talk-ladder-dead`） |
| **留** | **339** | SELECT2_CONTINUE 289 + SELECT_NONE_1 14 + SELECT6 13 + SELECT5_CHECK 12 + SELECT5_CHECK_SIMPLE 11 |
| 判不了 | 0 | —— |

行分类：talk-ladder-dead 256 / talk-single-step 1459 / dd-collect 310 / talk-no-start-block 37 /
talk-system-grant 14 / family-- 388 / family-None 1232 / SimpleHunt 202 / SimpleUseItem 38 / SimpleItemPlay 2。

## 4. 代表性证据（删候选）

| quest | NPC_START 块 | `acceptSourcesWithinSegment` | 客户端证据（删 token） |
|---|---|---|---|
| 1131 | npc=203097, extra 首段 `unaccepted started` | true（无段外源） | `pages:select1_1@QUEST_Q1131.html:41a724ff` |
| 1152 | npc=203132, extra 首段 `unaccepted started` | true | `pages:select1_1@QUEST_Q1152.html`（sha 见表） |
| 1156 | npc=203128, extra 首段 `unaccepted` | true | `pages:select1_1@QUEST_Q1156.html`（sha 见表） |

## 5. 保守边界（本普查不声称的部分）

1. `precheck` 拒绝行未单独建模（沿用 R1 口径：不确定即保留）；
2. 结论绑定**当日生产数据快照**（`quest_client_talk_chain_steps.tsv`）：若新增块使
   `acceptSourcesWithinSegment=false`，阶梯 token 会重新变活 ⇒ P2b 执行前必须重跑本脚本，
   且要求输出 `delete_candidates=104` 且无 `talk-ladder-live`；
3. 客户端证据为 active/exact 行；若生成器输入 CSV 变化，须先重跑生成器再重跑普查。

## 6. P2b 建议（执行状态见 `../2026-09-28-p2b-dialog-exits-shrink-ledger.zh-CN.md`）

- 删除 104 个阶梯 token（行级快照 `retired-tsv/quest_client_dialog_exits.tsv.rows-<date>`）；
- 判据：前后逐行指纹逐字节相同 + 家族门 + 清单门 3/3 + T1/T3 红集 sha 恒等
  （`3b92439da8…` / `5e3acdb9dcaf…`）+ manifest 行/计数不变（不删表）；
- 生成器停写移交 ② 维持（`build_quest_client_dialog_exits.py` 不感知缩表）。

## 7. 纪律回执

只读取证：未改任何生产文件、未跑 Maven、未动红集；产物落 `phase3-provenance/`；未 commit/push。

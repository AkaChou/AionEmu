# P0c-10：SimpleTalk 族收口对账（2223 行三分区 + 三桶零差集 + M3-d 降级复核 + 族级分歧表）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

- 日期：2026-09-25
- 归属：真端任务驱动替换目标（P0c 线，v3 提示词 §4 P0c-10）
- 触发：P0c-9 把 SimpleHunt 推到族口径闭环后，SimpleTalk 需要同格式的族级对账——
  585 个稳定拒绝码分类收敛、83 行 M3-d 降级复核、9 个保留码的"可否归零/归零路径"全景表。
  本切片为**对账/收敛切片**（无退役、无生产代码改动）：SimpleTalk 数据面已天然闭环，
  首跑即收口态全绿。

## 1. 结论

1. **族分区闭环**：2223 行 = **1538 RETAIL_TABLE**（XML 已删，M3-b 1498 + P0c-2 40）+
   **685 XML_RETENTION**，owner 分区封闭、漂移表 `retail-simple-talk-drift.tsv` 覆盖全部 2223 行
   （逐行分类登记，静默漂移由 `RetailSimpleTalkGateTest.driftVersusLegacyXmlIsRegistered` 守）。
2. **三桶互斥零差集**：XML_RETENTION 685 = **编译器拒绝 585**（漂移表 `REJECTED:*`，6 个稳定码）+
   **M3-d 降级 83**（`m3d-downgraded-quests.tsv`，code 统一 `CLIENT_ROUTE`）+
   **报告变体 17**（`CLIENT_REPORT_VARIANT`，源自 `m3b_simpletalk_drift.py` 的客户端
   `HACTION_SELECT4/5` 变体按钮清单）。
3. **族级分歧表（9 码全景）**：
   | 码 | 行数 | 可否归零 | 路径 |
   |---|---:|---|---|
   | `RETAIL_TALK_CHAIN` | 322 | 可（未排期） | 客户端页链登记面已具备（`quest_client_talk_pages`/`handin_pages`）；需 SimpleTalk 链式合成扩展（P5-3 wave B 同语义先例 `RETAIL_TALK_CHAIN_DEFERRED`） |
   | `RETAIL_TALK_ITEM` | 162 | 需评估 | 物品轴在真端 SimpleTalk 表无列；需客户端物品词汇/ScriptDLL 证据（P0c-11 面候选） |
   | `CLIENT_ROUTE`（M3-d） | 83 | 可（未排期） | 逐任务客户端合同暂缓；P5-2 交付五页词汇（`handin_pages` + ok 本地关闭规则）为同语义先例，可按词汇覆盖逐任务归零 |
   | `RETAIL_ACQUIRE_NPC_SENTINEL_NO_GRANT_PATH` | 60 | 可（未排期） | CHALLENGE_TASK 发放接线（M5-b2c 三类哨兵；同 SimpleHunt 84 行） |
   | `RETAIL_ACQUIRE_NPC_UNRESOLVED` | 19 | 可（需证据） | NPC 名→id 解析证据 |
   | `CLIENT_REPORT_VARIANT` | 17 | 不可归零（当前） | 客户端报告页变体按钮在真端路由集无对应动作（类同 SimpleHunt `CLIENT_BUTTON_UNWIRED`） |
   | `RETAIL_TALK_CUTSCENE` | 12 | 需评估 | 过场/影片轴族表无列（movie-page-turn 为 XML 编写块）；需客户端影片登记证据 |
   | `RETAIL_REWARD_NPC_UNRESOLVED` | 6 | 可（需证据） | 交付 NPC 名解析证据 |
   | `RETAIL_REWARD_NPC_FACTION_COMPOSITE` | 4 | 不可归零（当前） | 复合势力名客户端无登记；按客户端契约保留 XML |
   归零潜力合计：**484 行有明确归零路径**（322+83+60+19），18 行需证据/评估，19 行当前不可归零（17+4-2 变体/复合与名字类有重叠归因）。
4. **83 行 M3-d 降级复核（逐行留证 `p0c10-m3d-recheck.tsv`）**：全部满足
   "XML 在磁盘 + catalog 有条目 + 清单 reason=`SEMANTIC_GAP:CLIENT_ROUTE` + 漂移表分类在案
   （63 `DIFF:TRANSITION_SET` + 10 `DIFF:NODE_PROJECTION` + 10 `EQUIVALENT`）+ 证据=per-quest-client-contract"。
   其中 10 行漂移分类为 `EQUIVALENT`——IR 等价但因客户端路由怪癖保留，是后续按 P5-2 词汇规则逐任务归零的
   首选工作面（与 P0c-9 的 11102/28313 同性质：编译可行、客户端契约定去留）。
5. **验收**：`RetailSimpleTalkGateTest` 3 例 0F；`verify_retirement.py` =
   `catalog=2314 directory=2314 retired=3910 sum=6224 — OK`；T1 50 例中仅并发 DataDriven 批既有 4F
   （P0c-9 报告 §4.1 已归因），本切片零新增。

## 2. 机器证据（可重跑）

| 证据 | 内容 |
|---|---|
| `python3 -B p0c10_simpletalk_family_closure.py` | 收口不变量全绿（分区/三桶/漂移覆盖/磁盘/catalog/码一致性）；输出 `p0c10-family-reconciliation.tsv`（2223 行逐行）+ `p0c10-family-divergence-table.tsv`（9 码） |
| `p0c10-m3d-recheck.tsv` | 83 行 M3-d 逐行复核（磁盘/catalog/漂移分类/证据列） |
| `RetailSimpleTalkGateTest` | 3 例 0F（`gates/T1-p0c10-talkgate.log`） |
| `verify_retirement.py` | `catalog=2314 directory=2314 retired=3910 sum=6224 — OK` |

## 3. 未验证 / 阻塞 / 下一步

- **未验证**：分歧表的"可归零/需评估"判定是路径级结论，未做逐行编译验证（如 RETAIL_TALK_CHAIN 322 行
  的链式合成可行性需专门切片）；83 行 M3-d 未逐行对照 P5-2 词汇覆盖（归零执行属后续切片）。
- **阻塞**：无。
- **下一步**（v3 队列）：**P0c-11 非 IR 轴系统化**（封顶/等级、前置两表达、旧存档归一化三处合并登记 + 门禁）；
  随后 P1 SimpleItemPlay（15 行全族流程）。RETAIL_TALK_CHAIN 322 行的链式合成可作为独立切片排期
  （归零路径见分歧表）。

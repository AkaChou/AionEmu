# P5a：DataDriven 1508 形状普查基线（表入仓 + 77 种组合 + 分批路线）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 口径：真端优先。DataDriven 是最后的大族：真端 `data_driven_quest.xml`（2526 行，∩本服 1508）
> 本质是一台"任务小引擎"——接取方式 × 进度步骤链（每步 = 名单 + 计数）自由组合。

## 1 交付

- **真端表入仓**：`quest_retail/data_driven_quest.xml`（UTF-16 → UTF-8，2526 行，1.4 MB）。
- **形状普查基线**：`p5-datadriven-shape-census.tsv`（1508 行：quest_id / acquire / 步骤序列 / 步数；
  `p3b-useitem-family.txt` 同款宇宙清单 `p5-datadriven-family.txt`）。
- 结构解析：行 = `category_acquire_`（接取方式）+ `value0_acquire_`（接取参数）+ `reward_npc_name`
  + `progress_info/data` 步骤链（`category_progress_` + `value0_progress_` = "名单 计数"，分号分隔多步）。

## 2 证据（1508 行普查，77 种组合）

**接取方式分布**：Talk 2044（表全量）/ EnterArea 233 / none 136 / `_faction_` 52 / LevelUpLogIn 24 /
ItemPlay 21 / EnterWorld 15 / LevelUp 1。

**进度步骤类型分布**（含大小写变体归一）：Hunt 1392 / Talk 692 / CollectItem 564 / PVP 395 /
EnterArea 207 / ItemPlay 77 / EnterWorld 68 / TalkFOBJ 33。

**前 8 种组合覆盖 1199/1508（79%）**：
| 组合 | 行数 |
|---|---:|
| Talk + hunt 单步 | 552 |
| Talk + collectitem 单步 | 285 |
| EnterArea + pvp | 93 |
| Talk + 无步骤（纯对话） | 77 |
| Talk + pvp | 74 |
| none + hunt | 50 |
| EnterArea + hunt | 32 |
| Talk + 多步 talk 链（3+ 步） | 31 |
| 其余 69 种组合 | 309 |

**本服 XML 现状抽检**：宇宙内 1508 行全部有现役 XML；样例 1870（单步 hunt）= 4 节点
（unaccepted/started/reward/complete）+ NPC_START + npc-complete——与 SimpleTalk/Hunt 的规范形同族。

## 3 分批路线（每批走固定流水线：合成器 → 家族门禁 → 漂移/裁定 → 退役）

- **P5-1**：Talk 接取 + 单步 hunt（552 行）——接取/报告/完成复用既有规范形，进度 = 击杀链；
- **P5-2**：Talk + 单步 collectitem（285 行）——采集链（对照 collect 族口径）；
- **P5-3**：Talk + 纯对话 77 + 多步 talk 链（~90 行）；
- **P5-4**：EnterArea/pvp 系（93+74+36+…≈250 行，需 PVP 步骤语义——Phase 3 对账表标记 `PVP:3`
  未对账，需 ScriptDLL64 侧补证）；
- **P5-5**：其余变体（ItemPlay/EnterWorld/LevelUp/_faction_/none 接取等）。

## 4 对拍结果

- 表 ∩ 生产宇宙 = **1508**（与 goal 口径一致）；宇宙内 1508 行全部有现役 XML（退役前置条件就绪）。
- 步骤类型字典与 Phase 3 预研（`datadriven-progress-schema.tsv`、loader 串表）一致。

## 5 未验证 / 阻塞

- PVP 步骤语义未还原（Phase 3 对账表 1872-1881 等批量 `PVP:3` 未对账）→ P5-4 前置。
- `Talk` value1..n 的扩展参数语义（QUEST_1817B 3 / Movie 30 / 相对坐标等）未逐类还原 → P5-3 前置。
- T3 全树信号：并行会话（P0c 扩展）在途重构期间不稳定（见 P3b 报告 §5），DataDriven 各批的
  全树对账随其后补跑。

## 6 下一步

P5-1 探针已完成：`RetailDataDrivenTable`（新，解析 acquire + Hunt 步段）+ hunt 编译器复用实测
= **456/552（82.6%）一次通过**；拒绝 95（MONSTER_UNRESOLVED 49 / ACQUIRE_NPC_UNRESOLVED 41 /
SENTINEL 4 / COUNTER_6BIT 1）。P5-1 剩余：门禁 + 驱动接线 + 客户端门控（quest_monster.csv）核对
+ 退役 456 壳 XML + 拒绝码逐类（下一轮）。复现：`python3 -B p5-datadriven-shape-census.py`。

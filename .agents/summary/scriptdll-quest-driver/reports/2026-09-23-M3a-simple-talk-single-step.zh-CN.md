# M3-a：SimpleTalk 单步形态合成器与现状冻结


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

- 日期：2026-09-23
- 切片：M3 第一批（SimpleTalk 2223 行的单步形态）
- 前置：`2026-09-23-M2c2-npc-resolution-and-reject-classification.zh-CN.md`
- 一句话：单步合成器已落地并**逐条分类全族 2223 行**；其中 **86 个已证与 XML 的 IR 等价**，
  主拦路石是 **REWARD 节点的 `var0` 投影（1006 例）**——该值**无法由真端表/quest.xml/注册点推导**，
  需先闭环"它由哪个数据源决定"，否则这 1006 个不能立等价。

## 1. 交付

| 交付物 | 位置 | 说明 |
|---|---|---|
| 真端表入仓 | `src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleTalk.xml` | UTF-16+DTD → UTF-8，3152 行 |
| 表加载器 | `retail/RetailSimpleTalkTable.java` | 接取/报告 NPC、`talk_npc1..3`、give/remove/item_check、con_quest、cutscene |
| 单步合成器 | `retail/RetailSimpleTalkDefinitionCompiler.java` | 与 `npc-start` / `npc-report`(SELECT5) / `npc-complete` 三块同构；4 节点 + 6 位 var0 |
| 家族等价门禁 | `test/.../RetailSimpleTalkEquivalenceGateTest.java` | 家族规模冻结 2223 + 逐条分类 + 现状冻结比 + 等价下限 |
| 现状冻结表 | `src/test/resources/quest/retail-simple-talk-pending.tsv` | 2223 行（quest_id, classification） |
| 形状勘察 | `.agents/summary/.../m3_simple_talk_shapes.py` + `retail-simple-talk-shapes.tsv` | 70 种形状的分布基线 |

## 2. 实测分类（2223 行，`-Dretail.talk.equivOut`）

| 分类 | 数量 | 含义 |
|---|---:|---|
| `EQUIVALENT` | **86** | 真端合成定义与 XML 的 IR 完全一致 |
| `DIFF:NODE_PROJECTION` | 1006 | 仅节点投影差异：XML 的 REWARD 节点 `var0=1`，合成器给 `0` |
| `DIFF:TRANSITION_SET` | 506 | 路由多重集差异（XML 常用显式 `TALK_TO_NPC` 接取链，而非规范 `NPC_START`） |
| `REJECTED:RETAIL_TALK_CHAIN` | 308 | 行带 `talk_npcN` 对话链（`FUN_180cabb10`，后续切片） |
| `REJECTED:RETAIL_TALK_ITEM` | 162 | 行带 give/remove 物品轴 |
| `REJECTED:RETAIL_ACQUIRE_NPC_SENTINEL` | 118 | 接取名是类别哨兵（同 M2-c2 结论） |
| `REJECTED:RETAIL_ACQUIRE_NPC_UNRESOLVED` | 19 | 接取名在真端注册表不存在 |
| `REJECTED:RETAIL_TALK_CUTSCENE` | 12 | 行带过场 |
| `REJECTED:RETAIL_REWARD_NPC_UNRESOLVED` | 6 | 报告 NPC 名不存在 |

## 3. 关键发现：REWARD 的 `var0` 投影不可由现有真端数据推导

对这 1006 个任务做了单特征预测力测试（样本 1723 个单步行，XML 侧 `var0∈{0,1}` 分布 1125/598）：

| 候选特征 | 预测准确率 |
|---|---:|
| `item_check` | 0.653 |
| `con_quest` | 0.653 |
| 接取 NPC == 报告 NPC | 0.653 |
| XML 是否用规范 `NPC_START` 块 | 0.653 |
| `category1`（quest.xml） | 0.746 |
| 任务号 ≥ 80000 | 0.757 |
| 14 个 quest.xml 字段（`f_mission`、`max_repeat_count`、`target_type`、`drop_monster_1`…） | 全部 ≤ 0.68 |
| 注册点 `(helper, params)` | 每任务唯一 → 过拟合，无判别力 |

结论：**该位不是真端表/元数据/注册点的函数**，它是历史 handler 的逐任务写法（客户端摘要契约）。
下一步必须先闭环"哪个数据源决定它"，不能靠猜。

**待验证假设**：该位 = 该任务客户端 `quest_summary` 是否含 SECTION_0 行（报告行固定 SECTION_5，见附录 §6 第 10 条）。
验证路径：用 `/Users/mc/PycharmProjects/unpak/` 的 `QUEST_Q<id>.html` 与 `retail-simple-talk-pending.tsv` 的
`NODE_PROJECTION` 子集做交叉表；若一一对应，则该位是客户端契约，可由 HTML 驱动（或直接成为合成器参数）。

## 4. 门禁

| 门禁 | 断言 | 结果 |
|---|---|---|
| `RetailSimpleTalkEquivalenceGateTest.familyScopeIsFrozen` | 真端表 ∩ catalog == 2223 | 绿 |
| `...synthesizedDefinitionsMatchXmlIr` | 逐条分类与 `retail-simple-talk-pending.tsv` 完全一致 + 等价数 ≥ 86 | 绿 |
| 聚焦门禁（29 类选择器） | 无回归 | 见 `gates/m3a-gate-run1.log` |

冻结表的纪律：**基线是进行中状态，不是终局缺口登记**——任何新增分歧、任何静默消失都必须显式重算基线，
等价下限逐次提高；不允许"改小样本"或"放宽口径"来换绿。

## 5. 未验证 / 下一步

- 未验证：`var0` 来源假设（§3）、对话链形态（308）、物品轴（162）、过场（12）、服务端重启、客户端实机。
- 下一步（按价值排序）：
  1. 闭环 `var0` 投影来源（客户端 `quest_summary` 交叉表）；若成立，`NODE_PROJECTION` 1006 个有望直接转等价；
  2. 对齐 `TRANSITION_SET` 506 个的接取链写法（显式 `TALK_TO_NPC` vs 规范 `NPC_START`）；
  3. 实现 `FUN_180cabb10` 对话链形态（308 + 部分 chain 任务）；
  4. 物品轴 `give/remove` 与 `item_check`（162）；
  5. 参考 M2 流程：等价集合 → 冻结指纹 → 退役 XML + catalog 收敛 + 保留清单 owner 从 `FAMILY_PENDING` 转 `RETAIL_TABLE`。

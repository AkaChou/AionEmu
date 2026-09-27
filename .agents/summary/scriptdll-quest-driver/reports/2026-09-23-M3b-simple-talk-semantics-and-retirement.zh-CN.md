# M3-b：SimpleTalk 真端语义收口、领奖投影客户端派生、XML 退役与全树归因

> **口径修正（M3-c，2026-09-23 后续切片）**：本报告的退役动作其后改为**删除**——旧 XML 不再迁移到
> `src/test/resources/quest/retired/`（该目录与 1867 个冻结副本已删除），退役事实只留保留清单
> `owner=RETAIL_TABLE`，历史内容由 git 承担；相关门禁/测试统一改走生产视图与冻结指纹。
> 见 `2026-09-23-M3c-retired-xml-in-git-history.zh-CN.md`。

- 日期：2026-09-23
- 切片：M3 第二批（SimpleTalk 2223 行的语义判据 + 1581 个 XML 退役 + 全树对账）
- 前置：`2026-09-23-M3a-simple-talk-single-step.zh-CN.md`
- 一句话：口径按用户指令改为**真端优先**——XML 自身带历史错误，不再作为准入门槛；SimpleTalk 可驱动
  **1598** 行、退役 **1581** 个 XML（catalog 5938 → 4357），领奖投影改为**客户端任务书末行**派生，
  全树 1931 例与干净 HEAD 基线逐方法一致（**零新增失败**）。

## 1. 口径转向（本轮最重要的一句话）

用户指令（2026-09-23）：

> 现在的任务 xml 有很多错误，其实无法用来审计，如果改造完就删掉 xml 就行，按照真端的方式先迅速推进替代，
> 只有无法替代的 xml 才留下。

落地：**真端语义 + 客户端契约**通过即可退役 XML；与历史 XML 的 IR 等价降级为**漂移登记**（发现静默漂移 /
追溯历史缺陷），不再是退役门槛。判据证据见 §3。

## 2. 交付

| 交付物 | 位置 | 说明 |
|---|---|---|
| 真端表入仓 | `quest_retail/Quest_SimpleTalk.xml` | UTF-16+DTD → UTF-8（20032 行） |
| 客户端对话出口登记 | `quest_retail/quest_client_dialog_exits.tsv`（2806 行） | 真端表没有页链列；由 `build_quest_client_dialog_exits.py` 从仓库内客户端映射烘焙 |
| 客户端任务书行数登记 | `quest_retail/quest_client_summary_rows.tsv`（8935 行） | **本轮新增**；由 `build_quest_client_summary_rows.py` 从客户端 HTML 烘焙 |
| 行数登记视图 | `retail/RetailClientSummaryRows.java` | `rows(id)` / `lastRowIndex(id)` |
| 单步合成器 | `retail/RetailSimpleTalkDefinitionCompiler.java` | 接取 / 报告 / 交付检查 / 完成四块；领奖投影 = 客户端末行 |
| 家族门禁 | `test/.../retail/RetailSimpleTalkGateTest.java` | 三断言：家族规模冻结 / 真端语义不变量 / 漂移登记同步 |
| 漂移登记 | `src/test/resources/quest/retail-simple-talk-drift.tsv`（2223 行） | 与历史 XML 的逐任务分类 |
| 客户端变体登记 | `.agents/summary/.../retail-simple-talk-client-variant.tsv`（17 行） | 真端表无法表达的客户端交付按钮 → 保留 XML |
| 退役脚本与证据 | `m3b_retire_simple_talk_xml.py`、`m3b-retired-simple-talk-evidence.tsv` | 1581 个 XML `git mv` → `src/test/resources/quest/retired/` |
| 对账脚本 | `verify_retirement.py` | `catalog=4357 directory=4357 retired=1867 sum=6224 — OK`（无悬空生产引用） |
| 测试适配脚本 | `m3b_repoint_retired_fixture_tests.py`、`m3b_repoint_classpath_readers.py` | 生产优先 → 退役冻结副本回落 |

## 3. 判据证据：客户端任务书交叉表（`m3b-client-summary-cross-tab.txt`）

把 2223 行漂移分类与客户端 `Dialogs/**/quest_q<id>.html` 的 `quest_summary` 行数做交叉表：

| 漂移分类 | 客户端行数 | 任务数 | 含义 |
|---|---|---:|---|
| `DIFF:NODE_PROJECTION` | 1 | **1002** | XML 把领奖态投影写成 `var0=1`——**任务书没有第 1 行** |
| `DIFF:NODE_PROJECTION` | 2 | 4 | 80316/80317/80322/80323（交付两段式）；XML 的 1 是对的，合成器当时给 0 |
| `DIFF:TRANSITION_SET` | 1 | 549 | 路由多重集差异（接取块写法），投影一致 |
| `EQUIVALENT` | 1 | 43 | 完全一致 |
| `REJECTED:RETAIL_TALK_CHAIN` | 2 / 3 / 4 | 308 | 对话链（`talk_npcN`），已知未实现形态 |
| `REJECTED:RETAIL_TALK_ITEM` / 其他 | 1 | 316 | 物品轴、过场、接取名哨兵等 |

判据来自仓库既有的已确认结论（memory-bank `QE-051`）：**`reward` 节点 `var0` 投影 = 客户端任务书领奖行
（默认末行）；客户端把行号 n 映射到 visible 槽位 3n**；批 10（retail 单步族 184 个 `reward var0=1`）、
批 51（3938/4942 末两行）都是同一条判据的既有范例。

结论：**1002 条 XML 的领奖投影指向不存在的行**——这直接证明"XML 不能当审计金标准"。

## 4. 新一轮修正：领奖投影改为客户端派生（本轮新增）

| 项 | 修正前 | 修正后 |
|---|---|---|
| 合成器 `reward` 投影 | 常量 0（"单一步长 0 投影"） | `客户端任务书行数 - 1`（末行行号） |
| 门禁不变量 | 四个节点 `var0 == 0` | 领奖节点 `var0 == lastRowIndex(id)`，其余 0；且**每个可驱动任务必须在行数登记表里有记录** |
| 漂移登记 | 1006 `NODE_PROJECTION` / 549 `TRANSITION_SET` | **1002** `NODE_PROJECTION` / **553** `TRANSITION_SET`（4 条两行任务的投影归位） |

实现：`quest_client_summary_rows.tsv`（8931 个任务，来自客户端 HTML 的 `<step>` 计数）→
`RetailClientSummaryRows` → `RetailSimpleTalkDefinitionCompiler` → `RetailQuestDriver`。
门禁对"登记表缺行"直接报错，避免静默回落到 0。

## 5. 退役与对账

- `git mv` 1581 个 SimpleTalk XML → `src/test/resources/quest/retired/`（证据 `m3b-retired-simple-talk-evidence.tsv`）
- catalog：5938 → **4357**；`verify_retirement.py`：`catalog=4357 directory=4357 retired=1867 sum=6224 — OK`
- `docs/QUEST_CATALOG.zh-CN.md` 的链接改指冻结副本（`dangling=0`）
- 保留清单（`retail-xml-retention.tsv`，6224 行）分布：

| owner / reason | 数量 |
|---|---:|
| `RETAIL_TABLE/OK`（SimpleHunt 286 + SimpleTalk 1581） | **1867** |
| `XML_RETENTION/FAMILY_PENDING`（DataDriven 1508 / CombineTask 574 / CollectItem 178 / UseItem 104 / ItemPlay 15 / SerialHunt 10） | 2389 |
| `XML_RETENTION/SEMANTIC_GAP` | 1298 |
| `XML_RETENTION/SCRIPTED` | 494 |
| `XML_RETENTION/NO_TABLE` | 176 |

SimpleTalk 侧 642 个未迁移任务全部有稳定原因码（`RETAIL_TALK_CHAIN` 308 /
`RETAIL_TALK_ITEM` 162 / `RETAIL_ACQUIRE_NPC_SENTINEL` 118 / `RETAIL_ACQUIRE_NPC_UNRESOLVED` 19 /
`CLIENT_REPORT_VARIANT` 17 / `RETAIL_TALK_CUTSCENE` 12 / `RETAIL_REWARD_NPC_UNRESOLVED` 6）。

## 6. 测试适配（两类，共 51 个类）

| 类型 | 数量 | 做法 |
|---|---:|---|
| 按生产路径直读 `quest_definition/quests/<id>.xml` | 13 | 改 `QuestXmlFixtures.open(id)`（生产优先 → 冻结副本回落） |
| 按 classpath 直读 `/aion/data/.../quests/<id>.xml` | 37 | 改 `QuestXmlFixtures.openResource(path)`（新增 helper，按路径末段取任务号） |
| 审计"生产目录/生产 catalog" | 4 | 改生产视图 `RetailQuestDriver.overlay(QuestDefinitionDirectoryLoader.compile(...))` |
| `QuestRetailClassGateTest` 职业轴 | 1 | 口径按 owner 分流：真端驱动任务按真端展开口径，XML 保留任务仍按客户端窄口径；登记分歧从 `retail-metadata-divergences.tsv` 读取 |
| `QuestClientContractGateTest` | 1 | `MAX_REPORTED_FINGERPRINTS` 回退 400 → 30（仅影响失败信息截断） |

`QuestRetailClassGateTest` 的口径分流依据：`retail-metadata-divergences.tsv` 已把 136 个任务的
`classes` 轴登记为 `RETAIL_PRIORITY`（真端 `class_permitted` 的 base token 在转职后展开为两条进阶线），
因此真端驱动任务的期望集合按展开口径核对。

## 7. 门禁与全树对账

| 门禁 | 范围 | 结果 | 证据 |
|---|---|---|---|
| 聚焦门禁 | 30 个类 / 123 例 | **全绿** | `gates/m3b-gate-run4.log` |
| SimpleTalk 家族门禁 | 2223 行 + 3 断言 | **全绿** | `gates/m3b-talk-gate-run2.log` |
| questEngine 全树 | 467 个类 / **1931 例** | **28F+7E = 35，与干净 HEAD 基线逐方法一致** | `gates/m3b-questengine-run4.log` |
| 干净 HEAD 基线 | 24 个类 / 112 例 | 28F+7E = 35（同上，`only-now = 0`、`only-baseline = 0`） | `gates/head-baseline-2026-09-23.log` |

**基线的可信度说明（重要更正）**：M3-b 中途的两次全树运行（`m3b-questengine-run1/2.log`）读到的是
`target/classes` 里**陈旧的已退役 XML 副本**，因此当时"零新增失败"的结论不成立。本轮先让资源重新拷贝
（classpath 与源树一致）再跑，暴露出 52 个"classpath 直读退役 XML"的类；全部按 §6 适配后，全树失败集合
与干净 HEAD 基线（`/private/tmp/aionemu-head-1`，6224 个 XML、无 retired 目录）**逐方法相同**。

## 8. 未验证 / 风险

- **未验证**：服务端重启、客户端实机抽检（用户执行）。本轮所有结论都停在静态 + 单测层。
- **风险**：领奖投影改用客户端派生后，4 个两行任务（80316/80317/80322/80323）在真机上的任务书行需要抽检；
  1002 个单行任务按 QE-051 判据应为 0，实机抽检应看到任务停在唯一一行而不是空白。
- 职业轴（`class_permitted` 展开口径）与客户端窄口径的分歧**只做登记**，未收口。

## 9. 下一步（按价值排序）

1. **CombineTask 574**：真端 `Quest_CombineTask.xml` 已定位（5576 行 / 与客户端 `combine_task.xml` 同形）；
   下一步照 M3-a/M3-b 流程：表入仓 → 形状勘察 → `FUN_180caac10` 语义落地（结束进度 / 刷新 / 写 8 分量 /
   发产物）→ 家族门禁 → 漂移登记 → XML 退役。
2. SimpleCollectItem 178 / SimpleUseItem 104 / SimpleItemPlay 15 / SimpleSerialHunt 10（同族小批次）。
3. DataDriven 1508（最大族，需要 `data_driven_quest.xml` 语义）。

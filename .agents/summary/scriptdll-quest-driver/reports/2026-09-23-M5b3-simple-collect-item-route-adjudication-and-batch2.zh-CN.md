# M5-b3 SimpleCollectItem ROUTE/OTHER 轴逐任务定性 + 批次 2 退役（57 个 XML）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

- 日期：2026-09-23
- 前置：M5-b2（批次 1，29 个 XML 退役）留下 **60 个"可驱动但带 `ROUTE/OTHER` 轴"的任务**，
  当时的登记口径是"未定性 → 保留 XML"（`unreviewed-axes=ROUTE`）。
- 本轮口径（用户已多次确认）：**真端表 + 客户端契约优先，XML 只作对照物**；真端无法表达的才保留 XML 降级。

## 1. 方法（把 60 行差异收敛成 9 类形状）

1. 复算门禁的逐任务差异行（`retail-simple-collect-item-diff-lines.tsv`，89 个可驱动行）；
2. 用**门禁自身的 `axisOf` 口径**归轴，只保留 `ROUTE`（结构转换）与 `OTHER`（非转换行）两类，
   排除 `REWARD_ROW`/`VAR0_FIELD`/`DIALOG_*` 噪声；
3. 对每行做形状归一（源状态 / 事件 / 目标状态 / 条件 / 动作 / after-commit），按形状聚合任务；
4. 逐形状找证据（真端表、客户端模板索引、客户端任务书行数、引擎注册语义），出裁定。

工具与产物（可复算）：
`.agents/summary/scriptdll-quest-driver/{m5b3_route_axis_breakdown.py → m5b3-route-axis-breakdown.tsv,
m5b3_build_route_decisions.py → m5b3-collect-route-decisions.tsv}`

## 2. 形状与裁定（60 行全覆盖）

| # | 形状（差异行） | 任务数 | 证据 | 裁定 |
|---|---|---:|---|---|
| 1 | 真端独有 `T START>TalkToNpc[npcId=对象, dialogId=null]>START`（无条件/动作/after-commit）；XML 完全缺该路由 | 54 | 家族 **29 个已退役 XML + 本批 6 个**都显式声明了同形对象 TALK 路由；`QuestEngine.installProductionDefinitions` 只有 `TalkToNpc` 会 `registerQuestNpc`（对象才能成为客户端任务目标）；`QuestInteractionObjectValidator` 明确接受"TALK 路由 **或** drop 元数据"两种形态 | **ADOPT_RETAIL**（`OBJECT_TALK_ROUTE_MISSING`） |
| 2 | 同上，但 XML 路由多一个 `CloseDialog` after-commit | 1（1136） | 无客户端页证据；与 M5b2 已放行的 `DIALOG_1008` 同口径（家族默认不做额外关窗） | **ADOPT_RETAIL**（`OBJECT_TALK_AFTER_COMMIT`） |
| 3 | XML 行阶梯 `START/1 + REWARD/2 + COMPLETE/2`；真端 `REWARD/1` | 1（14150） | 客户端任务书 **2 行**（末行 = 1）→ 真端 REWARD/1 命中 QE-051，XML 的 REWARD/2 越界 | **ADOPT_RETAIL**（`JOURNAL_ROW_OVERFLOW`） |
| 4 | XML 把交付（`dialogId=39/1009/20002` + has-item + remove-item）同时挂在**采集对象**上；真端只挂报告 NPC | 1（2527） | 客户端模板索引 `legacy-quest-dialog-template-index.csv:823`：`start/end_npc=204811`、`report_page=2375`；`client-lifecycle-alignment.csv:892` 已把对象路由判为 `EVIDENCE_REQUIRED: current NPC is absent from the legacy template contract` | **ADOPT_RETAIL**（`OBJECT_HANDIN_DEAD_ROUTE`） |
| 5 | 真端把 `dialogId=31 → ShowQuestDialog(2375)` 挂在 **reward NPC** 上，XML 挂在**接取 NPC/其它 NPC** 上 | 3（21464/30052/30152） | 客户端任务书 NPC 名 = 真端 `reward/talk` 名（客户端分别只出现 `STR_DIC_N_CaspaGhost_01` / `Gellius` / `Gelastra`）→ XML 解析到了同名族的其它 NPC | **ADOPT_RETAIL**（`NPC_ID_RESOLUTION`） |
| 6 | XML 独有 `T START>CollectItem[itemId]>START`（进度刷新路由）；1137 另有接取 `GiveItem`（work item 182200512） | 2（1137/28503） | 真端 `quest.xml` 确有 `quest_work_item1=quest_1137a`，但引擎没有 work-item 自动发放（`workItems()` 在生产代码 0 引用），`CollectItem` 进度路由也不在合成器语义内 | **KEEP_XML**（`WORK_ITEM_GRANT_AND_COLLECT_ROUTE` / `COLLECT_PROGRESS_ROUTE`） |
| 7 | XML 把交付挂在采集对象；真端 `reward_npc_name=DF1A_Anmuring_E(=832822)`；客户端任务书交付对象写作 `Daike(203629)` | 1（2237） | 三方不一致（真端表 / 客户端任务书 / XML），无法单侧裁决 | **KEEP_XML**（`REPORT_NPC_DIVERGENCE`，待复核） |

裁定文件：`.agents/summary/scriptdll-quest-driver/m5b3-collect-route-decisions.tsv`
（60 行 = **57 `ADOPT_RETAIL`** + **3 `KEEP_XML`**）。

## 3. 放行批次 2（57 个 XML 退役）

链路（全部可重跑）：

1. `build_retention_list.py` 接入裁定表：`ADOPT_RETAIL → RETAIL_TABLE/OK`，`KEEP_XML → XML_RETENTION/SEMANTIC_GAP:<basis>`；
2. `m5b3_retire_collect_item_xml.py`（沿用 M5-b2 脚本口径）：删除生产 XML + 从 `quest_definition_catalog.xml` 收敛；
3. 冻结 IR 指纹重算（`-Dretail.collect.fpOut=`），29 行旧指纹**逐字节不变**，新增 57 行；
4. `verify_retirement.py` / `refresh_catalog_doc_links.py --apply`。

| 指标 | 批次 1 后 | 批次 2 后 |
|---|---:|---:|
| 生产 XML（catalog） | 3837 | **3780** |
| 目录文件 | 3837 | **3780** |
| 退役（清单 owner=RETAIL_TABLE） | 2387 | **2444** |
| `retail-xml-retention.tsv` 中 XML_RETENTION | 3837 | **3780** |
| SimpleCollectItem 真端拥有 | 29 | **86** |

`verify_retirement.py`：`catalog=3780 directory=3780 retired=2444 sum=6224` → **OK（无悬空生产引用）**；
`retired=2444 replaced=57 dangling=0`。
退役证据（逐任务 sha256）：`m5b3-retired-collect-item-evidence.tsv`。

## 4. 门禁与回归

| 命令 | 结果 |
|---|---|
| `mvn -o test -Dtest='RetailSimpleCollectItemGateTest,RetailOwnershipGateTest,RetailQuestCatalogTest,RetailQuestDriverOverlayTest,QuestClientContractGateTest,ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest'` | **25/25 绿**（BUILD SUCCESS） |
| `RetailSimpleCollectItemGateTest#frozenFingerprintsCoverExactlyTheRetiredQuests` | 冻结集合 29 → **86**，旧 29 行指纹未变（无静默漂移） |
| `mvn -o test -Dtest='com.aionemu.gameserver.questEngine.**'` | **1947 例 / 0F / 0E / 1 skipped / BUILD SUCCESS**（`gates/m5b3-final-questengine.log`） |

### 4b. 以真端/客户端为准修正的测试（XML 口径残留）

- `LegacyTemplateMirrorRouteRegressionTest#itemCollectingMirrorsUseTheClientOwnedReportAndTurnInProtocol`
  的 2527 镜像从 `npcId=700328`（采集对象，来自旧 XML）改为 **`204811`**（客户端模板索引里的报告 NPC）。
  首次全树跑出的唯一失败（`quest 2527 page route count`）即由此产生；改后该用例 10/10 绿，
  全树随之 0 失败。测试内已加中英双语注释说明依据。

## 5. 结论

- 60 行 `ROUTE/OTHER` 漂移**全部定性**：其中 57 行判定为"XML 历史错误/缺项 → 按真端放行"，
  3 行判定为"真端表无法表达 → 保留 XML 降级"（1137 / 2237 / 28503）。
- 最大的一类（54 行）根因单一：**旧 XML 漏掉了采集对象自身的 TALK 路由**，
  而家族内 29 个已退役任务与 6 个已判定任务都声明了它 —— 属于 xml 侧批量缺项，不是真端差异。
- 本族迁移进度：**86 / 178 由真端驱动并已退役 XML**；剩余 92 行 = 89 稳定码拒绝（40 哨兵 + 25 可选奖励 + 24 多物品）+ 3 个真端缺口。

## 6. 未验证 / 阻塞

- **未跑全仓 `mvn -o test`**（本轮只跑 `questEngine` 全树 1947 例）。
- 无真机 5.8 客户端在线验收：对象 TALK 路由与 reward NPC 变更只做了静态 + 门禁级验证。
- 2237 的三方 NPC 分歧（真端 `DF1A_Anmuring_E` / 客户端 `Daike` / XML 对象）需要客户端字符串表或真端 spawn 复核后才能放行。

## 7. 下一步

1. M5-c：`SimpleUseItem`(104) / `SimpleItemPlay`(15) / `SimpleSerialHunt`(10) 按同一流程推进。
2. 补齐 1137/28503 的真端表达（work-item 发放 + CollectItem 进度路由）后再退役。
3. M6-pre：`DataDriven`(1508 行) 落地。

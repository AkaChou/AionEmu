# P5-2：DataDriven Talk+CollectItem 客户端交付词汇落驱动（collect 批收口）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

- 日期：2026-09-24
- 切片：P5-2（DataDriven 1508 行宇宙的 collectitem 批；接续 P5-1 Talk+hunt 428 行）
- 状态：**完成（636 采纳全链绿；collect 自身零回归；剩余 T2 失败全部归属 P5-1 hunt 轨道与并发会话车道，见 §6）**

## 1. 交付

### 1.1 生产代码
- `RetailHandinDialogFlowCompiler`（新）：交付型对话流合成器，客户端任务书五页词汇
  （select_none / select1 / check_user_item_ok / check_user_item_fail / select_success）逐页逐按钮接线：
  接取流 = 族规范形（入口页来自客户端首屏）+ started 态任务书页路由 + 39/20002 双变体交付检查对
  （成功对带 HasItem 守卫 + RemoveItem → reward；失败对 priority=1 → 失败页）+ reward 态报告页
  （QUEST_SELECT → select_success）+ completeFlow（采集族口径，容忍零奖励组）+ 掉落箱
  `ACTION_ITEM_USE` 自回路路由（只发真端 `ai=quest_use_item` 的掉落 NPC）+ QE-051 进入世界领奖行修复边。
- **ok 页本地关闭续接规则**（本切片核心语义，与交接审计合同逐字镜像）：`check_user_item_ok` 页可见按钮只有
  FINISH_DIALOG(1008)（客户端本地关闭，不回传任务动作）且同一 NPC 在 reward 节点有 USE_OBJECT 续接入口
  （completeFlow 的领奖窗路由）时，交付成功分支直接开领奖窗 `SHOW_SELECT_QUEST_REWARD_WINDOW1(5)`——
  显示 ok 页会形成死端；否则照常显示 ok 页。生产信号 = 登记表 `ok_local_close` 列（客户端动作明细烘焙）。
- `RetailClientHandinPages`：`Pages` 记录增加第六分量 `okLocalClose`；装载器解析 7 列 TSV。
- `RetailDataDrivenCollectCompiler.buildSimple`：handin 登记命中 → 交付流；否则采集规范形（不变）。
- `RetailDataDrivenDefinitionCompiler`：13 参全注册表（summaryRows/handinPages/entryPages/interactionObjects），
  稳定拒绝码 `RETAIL_HANDIN_VOCABULARY_UNSUPPORTED`（例外登记且无交付页的行）。
- `RetailQuestDriver`：装载 4 张新登记表并传入 DD 编译（生产全路径）。
- `RetailDataDrivenTable.Entry`：无进度行（`!hasProgress`）不再误判 hunt/collect（78 行误路由修正）。

### 1.2 派生登记表（`quest_retail/`，可再生成）
- `quest_client_handin_pages.tsv`：**285 行**（六页 + `ok_local_close` 布尔列；65 行 true）。
- `quest_client_entry_pages.tsv`：2449 行（P5-1 回归修复：接取入口页 select_none 家族）。
- `quest_client_handin_exceptions.tsv`：7313 行（页面集超模板 + 人工暂缓清单）。
- `quest_use_item_npcs.tsv`：804 个 `quest_use_item` NPC（启动期掉落箱合同）。

### 1.3 测试
- `ItemCollectingDialogProtocolAlignmentTest`：`compile()` 从直读 src 路径 XML 改为
  `ProductionQuestDefinitions`（生产视图 = XML 目录 + 真端 overlay）——退役任务照常受协议对拍约束。
- `QuestHandoverContinuationAuditTest`：目录改 `ProductionQuestDefinitions.catalog()`——
  REPAIRED 正向锁与死端审计覆盖真端驱动任务（语义已由 25002 基线失败证明必要）。
- `QuestClientContractGateTest`：诊断参数 `MAX_REPORTED_FINGERPRINTS` 还原 30。
- `RetailDataDrivenGateTest`：`ACCEPTED_FLOOR = FROZEN_RETIRED_SIZE = 636`；编译按任务 memoize（1575s → ~2s）。

## 2. 证据

| 证据 | 位置 |
|---|---|
| 三轴对拍（285 行 collect：编译 vs 退役前 XML） | `p5-2-dd-collect-three-axis.tsv` |
| ok 页本地关闭普查（317 行 × 按钮 × 旧 XML 成功页交叉表） | `p52b-ok-page-close-census.tsv`（生成器 `p52b_ok_page_close_census.py`） |
| 暂缓清单（32 行，10 类机器可读原因） | `p52-handin-deferred-quests.tsv` |
| 暂缓执行器（恢复 XML + 目录按序回插 + 登记，幂等） | `p52b_defer_handin_quests.py` |
| 退役一致性（catalog 2422 / directory 2422 / retired 3802 / sum 6224 + 悬空引用 0） | `verify_retirement.py` 输出 |
| T1 47/47 绿 | `gates/T1-224112.log` |
| T2 收口跑（95 例 / 4F / 18E，全部归属见 §6） | `gates/T2-224143.log` |
| 冻结 IR 指纹 636 行 | `src/test/resources/quest/retail-data-driven-ir-fingerprints.tsv` |
| 漂移登记（ADOPTED 636 + 7 类拒绝码 = 1508） | `src/test/resources/quest/retail-data-driven-drift.tsv` |

## 3. 结论

- **DataDriven 1508 行：636 已驱动**（P5-1 hunt 428 + P5-2 collect 208）+ 872 稳定码暂缓，全链机器可核。
- 交付型任务的权威 = **客户端任务书页词汇**（不是家族规范形）：五页齐备且页面集=标准模板的行走交付流；
  每个客户端按钮都有路由（客户端契约门禁断言），页号只来自客户端。
- QE-051 领奖行投影覆盖 DD 全部采纳行（客户端 quest_summary 末行行号；旧 XML 的 0/1 投影是历史误差轴）。
- ok 页本地关闭规则的旁证链：(a) 退役前 XML 的 repaired 形状（13968 等 34 行直接开领奖窗）；
  (b) `HandoverContinuationContract.handOverSuccessPage` 的动态期望（同函数语义）；
  (c) 交接审计 REPAIRED 正向锁 42 分支；三者与登记表 `ok_local_close` 判定一致。

## 4. 对拍结果

- ok 页按钮类型 × 旧 XML 成功页交叉表（235 个当时采纳行）：
  `HAS_1009→10000` 196（与合成器一致）＋ `LOCAL_CLOSE→5` 28（规则命中）＋ 例外 11（见 §5 暂缓）。
- 交付流形状与 `assertItemCheck`/`handOverSuccessPage` 动态合同逐项对拍：
  ItemCollecting 132 行清单在**生产视图**下除暂缓行外全绿（含 13968 等真端驱动行）。
- 三轴对拍（节点集合/转换多重集/进度布局）按行留存于 evidence TSV，形状分歧行走裁定（axes 列）。

## 5. 暂缓（32 行，宁可诚实保留 XML，不虚报 RETAIL_TABLE）

| 原因码 | 行 | 任务 |
|---|---:|---|
| ACCEPT_GRANT_UNEXPRESSED | 10 | 19010/19016/19022/19028/19034/29010/29016/29022/29028/29034（制作大师接取即发放图纸；真端表与元数据均不承载该发放，合成会静默丢失） |
| TURN_IN_NPC_CLIENT_SPLIT | 5 | 18977/18978/28977/28978/25012/25085/25092 中采纳的 5 行（客户端交付 NPC ≠ 真端表接取 NPC，`endNpcs` 硬编码分歧清单） |
| LEGACY_WINDOW_SHORTCUT_LOCK | 7 | 80745/80748/80785/80786/80975/80976/80977（ok 页有 1009 按钮，但锁定形状直接开领奖窗；两条断言互相矛盾，锁优先） |
| START_END_NPC_SPLIT | 4 | 80870/80871/80872/80874（luna：起点/交付 NPC 分离，真端表单一接取 NPC 无法表达） |
| AUTO_STARTED_NO_TALK_ACCEPT | 2 | 80945/80946（自动接取，无对话接取流） |
| HANDOVER_WINDOW_LOCK / MULTI_STAGE_HANDOVER_LOCK | 2 | 80886 / 16838（k1 分支锁定） |
| JOURNAL_VISIBLE_SLOT_ROW / UNPORTED_PREREQUISITE_FAIL_OPEN / INTERACTION_OBJECT_USE_OBJECT_FLOW | 之前批次 | 15002/15010/15070/15514、1870/2869/2870、25013/25062/25080/25081/25526/25532/25535/25538 |

## 6. 未验证 / 归属（T2 剩余 4F + 18E，全部非本批引入）

| 失败 | 归属证据 |
|---|---|
| `AlignedMirrorRewardRowContractTest` mirror 25002 expected 1 was 10 | **基线先例**：`gates/T3-p0c8b-clean2.log:1470` P5 之前同方法同断言同值失败（旧 XML 本就是 10=击杀数）；根因 = hunt 网格领奖投影（满格击杀计数）≠ XML 时代行号合同，属 P5-1 hunt 设计轨道 |
| `JournalRewardRowRepairContractTest` quest 50126 expected 1 was 0 | 同上：50126 = `DD_TALK_HUNT_GRID`（P5-1 批），修复边语义为网格值而非行号 |
| `QuestLegacyMonsterHuntProductionFlowTest` 2F + 18E | 25090/25093 等均为 P5-1 hunt 行；台账阻塞表已登记「并发会话 P5-1 428 行退役 clean T3 暴露 36 个失败方法」，归 P5-1×P4 hunt 形状对账（跨会话协调项）；叠加并发会话今晚新增 79 个 hunt 退役的同类冲突 |

P5-2 批自身相关类全部绿：ItemCollecting 6/6、Handover 2/2、ClientContract、Startup、InteractionObject、
Ownership、CatalogManifest、Catalog、DriverOverlay、DD 门禁 5/5。

## 7. 阻塞

- 并发会话今晚又退役 79 个任务（catalog 2469→2400 方向移动）；本切片数字以 `verify_retirement.py`
  实时为准，报告中已注明本批自身贡献（collect 208 采纳）。
- T3 clean 全树复跑待并发会话收口后统一对账（既有协调项）。

## 8. 下一步

1. **P5-3**：纯对话/多步 talk 行（约 90 行）——`RETAIL_STEP_UNSUPPORTED` 桶拆解。
2. **P5-4**：EnterArea/pvp 系（约 250 行，需 ScriptDLL64 PVP 步语义）。
3. **P5-5**：变体接取（ItemPlay/EnterWorld/LevelUp/_faction_/none）。
4. P5-1 hunt 形状与 LegacyMonsterHunt/AlignedMirror/JournalRewardRow 三个合同的对账（与并发会话协调）。

# ZCODE GOAL 提示词 v2：AionEmu 任务系统改为「真端文件驱动」（终局，自包含）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> **用法**：整份粘贴给 zcode 作为 goal 模式的唯一目标说明；若环境支持 `create_goal`，把 §1 的
> objective 原文用于创建**一个** goal，其余章节作为执行手册。
> 仓库：`<仓库根>`（Aion 5.8 社区服务端，Java 25 + Maven + Spring Boot，单模块）。
> 深度资料：同目录 `2026-09-23-zcode-appendix.zh-CN.md`（**其 §1 工作区状态已过期，以本文件 §3 为准**）。
> 台账（唯一进度源）：`.agents/summary/scriptdll-quest-driver/GOAL-retail-driver-progress.zh-CN.md`。
> 报告索引：`.agents/summary/scriptdll-quest-driver/reports/INDEX.zh-CN.md`。
> 版本：v2（2026-09-23，含 M5-c / M5-b2b / M5-b2c 最新结论）。**v2 取代 v1**（`2026-09-23-zcode-prompt.zh-CN.md`）。

---

## 0. 你接手的是什么

AionEmu 当前用 6224 个 `quest-definition` XML 描述任务。**真端（58Server）根本不这么做**：真端用
"NPC 服务器里的任务对象 + 模板表 + per-quest 脚本"驱动，任务对象在 `+0xa8` 持一张 102 槽事件处理器表
向宿主 NPC 订阅事件。本任务 = 把 AionEmu 换成**真端这套文件驱动的做法**，XML 只作为"真端无法表达"的降级。

**用户已定的口径（不可推翻）**：

1. **真端是基准，XML 不是金标准**。现有 XML 有大量历史错误（已实证：漏路由、错 NPC、错行投影）。
2. **优先速度**：能由真端文件驱动的就迅速替换并删除 XML；只有真端确实无法表达的才保留 XML。
3. **退役 = 删除**（旧 XML 已在 git 历史里，**不要**迁移到 `src/test/resources` 做冻结副本）。
4. 判定产出是**逐任务的漂移判定**（真端对 / XML 错 / 真端无法表达），不是"合成 IR 必须逐字等于 XML"。

---

## 1. goal objective（原文，勿改）

```
把 AionEmu 任务系统改为真端文件驱动：5554 个任务（SimpleHunt 942 / SimpleTalk 2223 / CombineTask 574 /
CollectItem 178 / UseItem 104 / ItemPlay 15 / SerialHunt 10 / DataDriven 1508）的定义完全来自真端模板表
+ 真端 quest.xml 元数据 + ScriptDLL64 反编译语义，不再读 quest-definition XML；仅保留 670 个任务
（494 个真端 per-quest 脚本 + 176 个真端无覆盖）继续走 XML；生产装载链支持真端优先、XML 降级且开关可回退；
删除已迁移任务的 XML 并从 quest_definition_catalog.xml 移除对应条目（6224 → 670）；建立并跑通五类长期门禁
（归属 / 家族等价 / 调度逐帧 / 客户端契约 / 降级清单）；产出终局覆盖率报告与保留清单。
全程只改工作区：不做任何 git 提交、推送、打 tag 或新建分支；不启动/停止/重启服务端。
```

---

## 2. 终局不变量（任何实现都必须满足）

1. **单一运行时**：不得新增第二套任务运行时/存储/调度；必须复用
   `CompiledQuestDefinition → QuestEventIndex → QuestProductionDispatcher → QuestState/QuestVars`。
2. **单一 owner**：每个任务的定义来源唯一（真端表 **或** XML），有机器可读归属表
   （`src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv`），运行时不得双源打架。
3. **可回退**：开关 `aion.quest.retailDriver` 关闭后行为与改造前完全一致（同一份 XML、同一条装载链）。
4. **可证等价**：被迁移的任务必须能证明"真端驱动 == 原 XML"（IR 等价 + 家族门禁 + 冻结指纹），
   不能证明的进保留清单。**不许改 XML 去凑真端**。
5. **客户端契约不变**：`QUEST_Q*.html` 的 `quest_summary` 行 / `SECTION_n` 计数 / `quest_vars` 打包 /
   协议包 / START-REWARD-COMPLETE 迁移在真端定义下保持一致（不一致时按真端为准并留证）。

---

## 3. 起点状态快照（2026-09-23 实测，开工先复核一次）

**Git**：`HEAD = 96bd541e0`；工作区约 2700+ 条未提交改动（多个并发会话），**只改本任务需要的文件、用显式路径**。

**目录对账**：`catalog = 3780`，已退役 = `2444`，合计 `6224`；`verify_retirement.py` 输出
`catalog=3780 directory=3780 retired=2444 sum=6224 — OK`，裸悬空引用 0。

**家族进度（台账为准）**：

| 族 | 生产任务数 | 真端表 | 已可驱动 | XML 已删 | 状态 |
|---|---:|---:|---:|---:|---|
| 元数据层 | 6224 | 6217 可映射 | 全量 | — | M1 完成（`RetailMetadataEquivalenceGateTest` 绿） |
| SimpleHunt | 942 | 1865 行 | **286** | 286 | M2 完成（上限 286/942；178 拒绝落稳定码） |
| SimpleTalk | 2223 | 3152 行 | **1598** | 1498（另 83 行 M3-d 降级回 XML） | M3-b 完成 |
| CombineTask | 574 | 574 行 | **574** | 574 | M4-b 完成（全族 IR 逐行等价） |
| SimpleCollectItem | 178 | 262 行 | **89** | 86 | M5-b3 完成；剩 92 行（89 稳定码 + 3 真端缺口） |
| SimpleUseItem | 104 | 160 行 | 0 | 0 | 未开始（表已入仓） |
| SimpleItemPlay | 15 | 43 行 | 0 | 0 | 未开始（表已入仓） |
| SimpleSerialHunt | 10 | 16 行 | 0 | 0 | 未开始（表已入仓） |
| DataDriven | 1508 | `data_driven_quest.xml` | 0 | 0 | 未开始（最后做） |
| 保留 XML | 670 计划内（当前实际 3780） | — | — | — | 清单已固化：FAMILY_PENDING + SEMANTIC_GAP + SCRIPTED 494 + NO_TABLE 176 |

**回归三档（已落地，实测）**：

```bash
cd <仓库根>
# T1：改完立刻跑，~25 s（固定 7 个全局门禁，25 例）——最近一次 gates/T1-233759.log 全绿
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T1
# T2：T1 + 按任务 ID 反查命中的测试类（0.23 s 扫 1092 类）
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T2 <本切片任务ID...>
# T3：切片收口跑一次，全树（1947 例 / 0F / 0E / 1 skipped / 362 s）
QUEST_FORK_COUNT=2 .agents/summary/scriptdll-quest-driver/run_quest_gates.sh T3
# 只要选择器不跑测试
python3 -B .agents/summary/scriptdll-quest-driver/affected_quest_tests.py --json <ids...>
```

> 并行**只允许** `-DforkCount=N`（多 JVM 分片）；**禁止** `-Dparallel=classes`（同 JVM 共享静态状态）。
> T1 清单唯一来源是 `affected_quest_tests.py#T1_GATE_CLASSES`，不要另抄一份。

**关键路径**：

| 用途 | 路径 |
|---|---|
| 真端数据（UTF-16 + 内部 DTD） | `<真端根>/Map/XML/`（`quest.xml`、`Quest_*.xml`、`data_driven_quest.xml`、`npcfactions_quest.xml`、`challenge_task.xml`、`npcs.xml`） |
| 真端二进制 | `<真端根>/MainServer/{ScriptDLL64.dll,Server64.exe}` |
| 反编译源码 | `<真端根>/server58-source/{MainServer_ScriptDLL64,MainServer_Server64}/`（`fun/`、`classes/`、`symbols.tsv`）；巨文件 `MainServer_ScriptDLL64/ScriptDLL64.c`（271 万行 / 53 MB，latin-1，grep 加 `-m`） |
| 客户端解包（UTF-8） | `<客户端解包根>/data_unpacked/Dialogs/**/quest_q<id>.html`；`Quest_unpacked/{quest.xml,quest_monster.csv,quest_script_monster.csv}` |
| 本仓真端数据 | `src/main/resources/aion/data/static_data/quest_retail/`（各族表 + `quest.xml` + `retail-xml-retention.tsv` + `quest_client_dialog_exits.tsv` + `quest_client_summary_rows.tsv`） |
| 证据与工具 | `.agents/summary/scriptdll-quest-driver/`（**中间产物只能放这里**，禁止 `scripts/`、禁止 `.agent/`） |

**已有可复用工具（优先复用，不要重写）**：

- 回归：`run_quest_gates.sh`（T1/T2/T3）、`affected_quest_tests.py`（受影响测试反查）、`verify_retirement.py`、
  `refresh_catalog_doc_links.py`。
- 真端反向工程：`re_tool_extract.py`（抽函数体）、`re_vtable_scan.py`（虚表/槽位普查）、
  `m5b2_vtable_scan.py`、`m5b2_region_xref.py`。
- 事件码与形状：`m5b2b_handler_slot_scan.py`（任务→(NPC/物件,事件码,处理器,玩家虚槽) 全量普查
  `m5b2b-quest-event-census.tsv`，80102 行/7043 对象）、`m5b2b_event_family_crosstab.py`、
  `m5b2b_triplet_start_correlation.py`、`m5b2b_client_accept_page_probe.py`、`m5b2b_family_ids.py`、
  `m5b2b_family_accept_crosstab.py`、`m5b2b_faction_grant_coverage.py`。
- 覆盖/对账：`retail_family_coverage.py`、`reconcile_simple_hunt.py`、`reconcile_talk_and_datadriven.py`、
  `build_retention_list.py`、`m2e_retire_migrated_xml.py`、`m3b_retire_simple_talk_xml.py` 等各族退役脚本。

**已知既有失败（与本改造无关，不要去"修"）**：`com.aionemu.gameserver.ai.RetailPatternAI2Test`
（`RetailPatternAI2:893` NPE）。`questEngine` 全树当前是 **0 failure / 0 error**。

---

## 4. 下一步队列（按优先级，做完一项立刻做下一项）

### P0（立刻做）M5-b3x：把"系统发放"形状落进 SimpleCollectItem 驱动

已证（M5-b2c）：`REJECTED:RETAIL_ACQUIRE_NPC_SENTINEL` 的 40 行**不是解析缺口**，而是真端**类别哨兵**：

- 真端族表 `acquired_npc_name = _faction_`（43 行）**与"客户端只有 `ask_quest_accept`、没有 `select1`"零差集**；
- 真端 `Map/XML/npcfactions_quest.xml` 给出 (阵营名, 星期位)：其中 **28 行每天发放 → 应驱动**，
  **12 行全 0 = 真端不发放**（AionEmu 现有 XML 却给了接取 NPC 路由 = 跑了真端不该跑的任务）；
- AionEmu **已有**这三套子系统，不需要新造任务形状：`NpcFactionQuestData`
  （`_faction_`，各族 268/270 覆盖）、`ChallengeData`/`challenge_tasks.xml`（`_challengetask_`，SimpleHunt 87/87）、
  `RetailAreaEngine`+世界 `quest_area`（`_area_`）。

施工要点：族表识别三类哨兵 → `RetailSimpleCollectItemDefinitionCompiler` 对哨兵行**不生成接取半边**
（`acceptFlow`/`setproRoute`/`reportNpcExit` 的接取部分），只保留 report（`reward_npc_name`）+ 采集对象 + 领奖 →
metadata 透出 `npc-faction-id`（真端 `quest.xml` **自带 `<npcfaction_name>`**，`npcfactions_quest.xml` 只取星期位；
AionEmu `npc_factions.xml id=2` 与真端 `npcfactions.xml id=2` 已核四元对齐）→ 重算漂移登记 + 家族门禁 →
退役这 28 行 XML → 12 行按真端保持不发放并单独留证。

### P1 SimpleCollectItem 收口（92 行 → 归零或全部有裁决）

- 25 行"可选奖励"（`<choice>` 选择型）+ 24 行"多物品"：逐条判 `ADOPT_RETAIL` / `KEEP_XML`，落稳定码。
- 3 个真端缺口：1137（work-item 发放）、28503（CollectItem 进度路由）、2237（真端/客户端/XML 三方 NPC 分歧）。
- 收口判据：`retail-simple-collect-item-drift.tsv` 178 行全部有裁决，`RetailSimpleCollectItemGateTest` 绿，
  catalog 收敛，`verify_retirement.py` OK。

### P2 `_area_` 数据缺口（独立切片）

SimpleHunt 21 行 `_area_`/`_Area_`，生产 `definitions/compact/ai/ai-areas.xml` 只命中 **3/21**。
需按真端世界文件（`Map/Worlds/*/world_*.xml` 的 `quest_area`，10 GB，UTF-16）重算该表并加门禁。
（§4.9 的 4 个"triplet 却 ask_quest_accept"反例 13912/13913/23912/23913 正是这一类。）

### P3 三个小族：SimpleUseItem(104) → SimpleItemPlay(15) → SimpleSerialHunt(10)

- **SimpleUseItem**：真端表 160 行，**全族 160/160 无 `select1`**（用物品接取）→ 天然是"自动接取"形状，
  与 P0 的哨兵形状同源，先复用 P0 的模型再做本族。
- **SimpleItemPlay**：43 行中 41 行 `triplet & select1`（主形状），2 行 `_faction_`。
- **SimpleSerialHunt**：16 行全部主形状（`triplet & select1`），最小、适合验证新流程。

### P4 大族余量：SimpleHunt 656 行 / SimpleTalk 625 稳定拒绝 + 83 降级

- SimpleHunt：`_faction_` 127（125 覆盖，**89 全 0 不发放**）、`_challengetask_` 87/87 可用、
  `_area_` 21 需 P2；其余为等价缺口，逐类裁定。
- SimpleTalk：`_faction_` 98（44 全 0）；625 个稳定拒绝码分类收敛。

### P5 DataDriven 1508（最后做，最大风险）

真端 `data_driven_quest.xml`（含 52 行 `_faction_`）+ `QuestProgressExtraInfo_*` / `ExtraAction` 类别语义
（已从 `ScriptDLL64` 恢复，见 `scriptdll64-quest-strings.txt`、`scriptdll64-quest-xrefs.txt`）。

### P6 终局收口

五类长期门禁（归属/家族等价/调度逐帧/客户端契约/降级清单）全部实现并绿 →
目录收敛（catalog 与 `quests/` 目录一致）→ 客户端抽检每族 ≥3 → 终局覆盖率报告与保留清单 →
`verify_retirement.py` + 全仓悬空引用 grep 证据。

---

## 5. 每族固定流程（写死，不要每次重新发明）

1. 真端表入仓（UTF-16 → UTF-8，**不要覆盖回 UTF-16**）；
2. 表加载类（允许内部 DTD、禁外部实体访问）；
3. 合成器（表行 → 定义 IR）；
4. **真端语义门禁**（不要求逐字等于 XML）；
5. 漂移登记 TSV（逐任务 + 稳定码，**禁止手改**，用 `-Dretail.*.equivOut=<path>` 重算）；
6. 客户端契约门禁（`QUEST_Q*.html` 出口 / `quest_summary` 行 / 领奖页）；
7. 退役脚本 `--dry-run` → 正式**删除** XML（不迁移 test）+ 从 `quest_definition_catalog.xml` 移除条目；
8. `retail-xml-retention.tsv` 里该族的 owner 从 `FAMILY_PENDING` 转 `RETAIL_TABLE`；
9. 聚焦门禁 + `verify_retirement.py` + **T3 全树一次**并与干净基线逐方法对账（only-now 必须为 0）。

**允许删除 XML 的充分条件（五条同时满足）**：元数据对拍一致（真端优先分歧另列证）/ 进度事件与 DLL 语义
及客户端守卫一致 / 合成 IR 与退役前 XML IR 等价（或按真端语义修正并留证）/ 家族门禁绿 /
不在保留清单且无未闭环口径冲突。任一条不满足 → 进保留清单，**不要改 XML 去凑**。

---

## 6. 执行纪律与硬约束（违反即失败）

**连续执行，中间不要停**：

- 每轮循环：读台账「当前切片/下一步/阻塞」→ 取**最小可推进切片** → 实现 → 跑门禁 → 更新台账（DoD/家族进度/
  证据索引/变更日志）→ **立刻开始下一片**，不要等确认。
- **不要主动提交**：`git commit|push|tag|stash`、新建分支或 worktree **一律不做**（用户明说提交时才做）。
  到终局也不提交，交付物留在工作区。
- **不要** `git checkout` / `git reset --hard` / `git clean`（工作区有并发会话的 2700+ 未提交改动）。
- **不要启动/停止/重启服务端进程**。
- **Maven 已授权（离线）**：`mvn -o test -Dtest=...`，范围限 `com.aionemu.gameserver.questEngine.*` 及
  本任务新增的 quest 相关门禁；全量 `mvn test`、`mvn package/install`、改 `pom.xml`、预计 >10 分钟的构建
  **需先请授权**（请求授权的同时继续做不受影响的部分）。
- **不要动并发文件**：`quests/10528.xml`、`quests/20528.xml`、`.agents/memory-bank/*`、`.agents/summary/index.jsonl`、
  `.agents/summary/quest-10527-reward-row/audit_reward_row_vs_client_steps.py`、
  `src/test/java/**/definition/ArchdaevaRewardRowContractTest.java`、`.agents/summary/idea-unused-false-positive/`、
  `.agents/summary/quest-20528-reward-row/`。
- 规范：Java 用 Tab 缩进、行宽 ≤120、注释中英双语；XML/Java 格式遵循 `.agents/rules/*`；
  **中间脚本只放** `.agents/summary/scriptdll-quest-driver/`。
- 读文件用 `Read`（或 `sed -n`）；**不要**用 python 读文件内容（python 只用于聚合/扫描/生成）。
- `rm` 被拒时用 `python3 -c "from pathlib import Path; Path('<p>').unlink()"`。
- shell 命令按仓库 `AGENTS.md`（`@<Codex 本地配置目录>/RTK.md`）用 `rtk` 前缀（rtk 0.45.0 已装）；
  若某条命令 rtk 报错，原样重跑一次（去掉 rtk）即可，不要卡住。
- 需要用户才能做的事（重启服务端、实机客户端验收、提交）→ 记「待用户执行 / 未验证」后**继续其它工作**。

**允许停的只有三种情况**：① 用户明确要求停或改方向；② 需要用户才能做的外部动作（记录后继续别的）；
③ 确实已无可推进项（直接给终局报告 + 未验证清单）。**禁止**用"要不要继续？""下一步建议…"结束回复。

---

## 7. 作废结论清单（**防止重复推导错误**，必须遵守）

| 作废/不成立的结论 | 现行正确结论 | 证据 |
|---|---|---|
| "槽 `+0x1b8` 是采集进度接口" | `IUserImp` 槽 55 是**客户端演出通知槽**；进度读写走 `+0xd0/+0xe8/+0xf0/+0xf8/+0x100` | `m5b2b_slot55_owner_scan.py` + `Server64.c:2248860` |
| "事件码 `0x37` = 合成专用补槽" | **作废**（首版普查漏 `&LAB_*` 处理器，52117 → 80102 行） | `m5b2b_handler_slot_scan.py` 修正版 |
| "事件码 `0x26` = 交付/报告" | **作废**，改判为生命周期三元组 `{0x1c,0x1d,0x26}` 成员 | `m5b2b-event-family-crosstab.tsv` |
| "`0x35` = 采集进度事件" | **不成立**，`0x1e`+`0x35` 是成对通知槽（采集族仅 13%，非采集族更多） | 同上 |
| "`REJECTED:RETAIL_ACQUIRE_NPC_SENTINEL` 是解析缺口" | 是**另一类任务形状**（类别哨兵 `_faction_` 等 → 系统发放） | 报告 M5-b2b §4.8/§4.9、M5-b2c |
| "144 个类别哨兵不可由真端文件推导" | 只对"**当成 NPC 推导**"成立；三类哨兵都有真端发放系统 | M5-b2c |
| 接取页探针首版数字（如 SimpleSerialHunt 16 vs 10） | 首版把"无契约行 `?`"当真实 NPC；已改三态 `sentinel/real/no-contract` | `m5b2b_client_accept_page_probe.py` |

**当前有效的事件码语义（零反例口径）**：`0x11`=击杀（击杀两族 100%，其它族 0%）/ `0x33`=合成（574/574）/
`0x36/0x37/0x38`=采集族签名（同现 99.9%）/ `0x1c/0x1d/0x26`=生命周期三元组 / `0x1e`+`0x35`=成对通知槽 /
`0x32` 近乎全员（读 `+0xe8`）/ `0x3`=NPC/物件交互。
**形状不变量**：`ask_quest_accept`（委托书 + `HACTION_FINISH_DIALOG`）⇒ 无三元组 443/453 = 97.8%；
`triplet ⇒ select1` 4689/4693 = 99.9%。**哨兵 `start_npc_ids=0` 单独不能判形状**。

---

## 8. 汇报 = 落盘到文档（每完成一个稳定切片都要写）

**规矩：文档才是记录，对话只给摘要 + 文档路径。**

1. 新建 `reports/<YYYY-MM-DD>-<切片>-<主题>.zh-CN.md`，含固定段：
   `## 1 交付`（新增/修改文件→一句话作用，标出可复用资产）/ `## 2 证据`（命令 + 通过数/扫描 N/M）/
   `## 3 结论`（可删 XML 数 + 判据；不可删逐条原因）/ `## 4 对拍结果`（元数据/进度事件/IR/调度/客户端各一项）/
   `## 5 未验证`（+原因）/ `## 6 阻塞与决策项`（附"不停等的处理方式"）/ `## 7 下一步`（最小切片 + 复现命令）。
2. `reports/INDEX.zh-CN.md` 顶部追加一行（日期/切片/文档/一句话结论）。
3. 更新台账 `GOAL-retail-driver-progress.zh-CN.md`：DoD 勾选、家族进度表、当前切片/下一步、阻塞、证据索引
   （命令→结果）、变更日志（每轮一行 + 报告链接）。
4. 对话回复只给：本切片结论 + 证据数字 + 报告路径 + **已继续推进的下一个切片**。

---

## 9. 完成判据（DoD，全绿才允许把 goal 置为 complete）

- [ ] 5554 个任务各有机器可读 owner 记录，且全部由真端文件驱动（不读 XML）
- [ ] 670 个任务在保留清单中，逐条有原因与证据；无未归类任务（5554 + 670 = 6224）
- [ ] 生产接线可用：开关关闭 = 改造前行为，开启 = 按 owner 表加载
- [ ] 被迁移 XML 已从 `quest/definitions/quests/` 删除，`quest_definition_catalog.xml` 已收敛
- [ ] 五类门禁（归属/家族等价/调度逐帧/客户端契约/降级清单）实现且长期绿
- [ ] 客户端抽检：每族 ≥3 个任务，`quest_summary` 行与 `SECTION_n` 计数一致
- [ ] 终局报告（覆盖率、保留清单、口径分歧、未验证项）落盘
- [ ] 全仓无对被删 XML 的悬空引用（`grep` 证据）

**开工第一件事**（按顺序）：读台账与 `reports/INDEX.zh-CN.md` 最新三篇 → 跑一次 T1 复核基线 →
从 §4 的 P0 开始切片，每片落盘后立刻继续。

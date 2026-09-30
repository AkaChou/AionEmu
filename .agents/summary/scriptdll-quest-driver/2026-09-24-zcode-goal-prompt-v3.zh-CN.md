# ZCODE GOAL 提示词 v3：AionEmu 任务系统改为「真端文件驱动」（终局，自包含）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> **用法**：整份粘贴给 zcode 作为 goal 模式的唯一目标说明；若环境支持 `create_goal`，把 §1 的 objective
> 原文用于创建**一个** goal，其余章节作为执行手册。
> 仓库：`<仓库根>`（Aion 5.8 社区服务端，Java 25 + Maven + Spring Boot，单模块）。
> 深度资料：同目录 `2026-09-23-zcode-appendix.zh-CN.md`（**其 §1 工作区状态已过期，以本文件 §3 为准**）。
> 台账（唯一进度源）：`.agents/summary/scriptdll-quest-driver/GOAL-retail-driver-progress.zh-CN.md`。
> 报告索引：`.agents/summary/scriptdll-quest-driver/reports/INDEX.zh-CN.md`。
> 版本：**v3（2026-09-24 夜，含 P0c-8a/8b/8c 缺口逐行裁定结论与 clean 副本 T3 纪律）**。
> **v3 取代 v2**（`2026-09-23-zcode-goal-prompt-v2.zh-CN.md`）与 v1（`2026-09-23-zcode-prompt.zh-CN.md`）。

---

## 0. 你接手的是什么

AionEmu 用 6224 个 `quest-definition` XML 描述任务；**真端（58Server）不这么做**：真端用"NPC 服务器里的任务对象
+ 模板表 + per-quest 脚本"驱动，任务对象在 `+0xa8` 持一张 102 槽事件处理器表向宿主 NPC 订阅事件。
本任务 = 把 AionEmu 换成**真端这套文件驱动的做法**，XML 只作为"真端无法表达"的降级。

**用户已定的口径（不可推翻）**：

1. **真端文件是形状权威**，XML 不是金标准（XML 有大量历史错误：漏路由、错 NPC、错行投影、错接取链）。
2. **客户端解包数据是强二证据**（`quest_q*.html` 出口、`quest_monster.csv` 的 `SECTION` 门控、按钮常量集）。
3. **遗留 XML 只是对照**，每个差异必须归为"真端对、XML 错"或"真端缺口 → 保留 XML"；**不要求零进度损失**。
4. **退役 = 删除**（内容留在 git 历史；**不要**迁移到 `src/test/resources` 做冻结副本）。
5. 判定产出是**逐任务的漂移裁定**（`ADOPT_RETAIL` / `KEEP_XML:<稳定码>`），不是"合成 IR 必须逐字等于 XML"。

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

1. **单一运行时**：不得新增第二套任务运行时/存储/调度；复用
   `CompiledQuestDefinition → QuestEventIndex → QuestProductionDispatcher → QuestState/QuestVars`。
2. **单一 owner**：每个任务的定义来源唯一（真端表 **或** XML），机器可读归属表 =
   `src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv`（**三处副本必须 sha256 一致**）。
3. **可回退**：开关 `aion.quest.retailDriver` 关闭后行为与改造前一致。
4. **可证等价或已裁定**：迁移行要么能证 IR 等价（家族门禁 + 冻结指纹），要么有**逐行裁定记录**
   （`ADOPT_RETAIL` 带机制证据；`KEEP_XML` 带稳定码）。**不许改 XML 去凑真端**。
5. **客户端契约不变**：`QUEST_Q*.html` 的 `quest_summary` 行 / `SECTION_n` 计数 / `quest_vars` 打包 /
   协议包 / START-REWARD-COMPLETE 迁移在真端定义下保持一致（不一致时按真端为准并留证）。

---

## 3. 起点状态快照（2026-09-24 22:35 实测，开工先复核一次）

**Git**：分支 `quest`，`HEAD = 6a7bd55af`；工作区约 3800+ 条未提交改动（**多个并发会话**），
**只改本任务需要的文件、用显式路径**。

**目录对账**：`python3 -B .agents/summary/scriptdll-quest-driver/verify_retirement.py` →
`catalog=2400 directory=2400 retired=3824 sum=6224 — OK`（目录一致、无悬空生产引用）。
另核对：磁盘 `quests/*.xml` = catalog = 保留清单 `XML_RETENTION` 行数（当前都是 **2400**，零差集）。

**家族进度（台账为准，2026-09-24 收口状态）**：

| 族 | 生产任务数 | 已可驱动 / 可合成 | XML 已删 | 状态 |
|---|---:|---:|---:|---|
| 元数据层 | 6224 | 6217 可映射 | — | M1 完成（`RetailMetadataEquivalenceGateTest` 绿） |
| SimpleHunt | 942 | **834 可合成**（286 XML 等价证明 + **481 裁定行** + 其余族内可合成） | **767** | **P0c-8c 完成**：42 行真端表缺口（4 稳定码）+ 108 行稳定拒绝码 + 2 行驱动覆盖缺口 |
| SimpleTalk | 2223 | 1638 可驱动 | 1538（另 83 行 M3-d 降级回 XML） | M3-b + P0c-2 完成 |
| CombineTask | 574 | 574 / 574 | 574 | M4-b 完成（全族 IR 逐行等价） |
| SimpleCollectItem | 178 | 178 / 178 | 175（3 真端缺口留 XML） | P0c-5b 完成 |
| SimpleUseItem | 104 | 102 / 104 | 102（2 缺口 30720/30723） | P3b 完成 |
| SimpleItemPlay | 15 | 0 | 0 | **未开始**（真端表 43 行已入仓） |
| SimpleSerialHunt | 10 | 10 / 10 | 10 | P3 完成 |
| DataDriven | 1508 | 428（**并发会话 P5-1 在飞**） | 428 | 并行批，勿碰其活文件 |
| 保留 XML | 670 计划内（当前实际 **2400**） | — | — | 清单固化：`FAMILY_PENDING` + `SEMANTIC_GAP` + `SCRIPTED` 494 + `NO_TABLE` 176 |

**回归三档（实测；T3 有特殊纪律，见下）**：

```bash
cd <仓库根>
# T1：改完立刻跑，秒级（12 个固定全局门禁类，清单唯一来源 affected_quest_tests.py#T1_GATE_CLASSES）
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T1
# T2：T1 + 按任务 ID 反查命中的测试类
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T2 <本切片任务ID...>
# 只要选择器不跑测试
python3 -B .agents/summary/scriptdll-quest-driver/affected_quest_tests.py --json <ids...>
```

**T3 必须在仓库外隔离副本跑**（并发会话会清空主工作树的 `target/`，且非 clean 树会因
`target/classes` 残留已删 XML 而**假绿**）：

```bash
rsync -a --delete --exclude '/target' --exclude '/.git' --exclude '/aion' --exclude '/patch' \
      --exclude '/log' --exclude '/.codegraph' <仓库根>/ /private/tmp/aion-<切片>/
cd /private/tmp/aion-<切片>
mvn -o -B test -Dtest='com.aionemu.gameserver.questEngine.**,!com.aionemu.gameserver.questEngine.retail.RetailDataDrivenGateTest' \
    -DfailIfNoTests=false -DforkCount=2 > <仓库根>/.agents/summary/scriptdll-quest-driver/gates/T3-<切片>-clean.log 2>&1
# 对账：失败方法集与最新基线逐行 comm（归一化到 `com.aionemu...` 开头的方法行）
comm -23 <(grep -oE 'com\.aionemu[^ ]+$' gates/T3-<切片>-clean.log | sort -u) \
         <(grep -oE 'com\.aionemu[^ ]+$' gates/T3-p0c8c-clean2.log | sort -u)
# 用毕删除副本，不留产物
rm -rf /private/tmp/aion-<切片>
```

- **T3 基线 = `gates/T3-p0c8c-clean2.log`：1961 例 / 30F / 69E / 1 skipped**。
  其中**长期基线失败**为止；另有 **8 个失败属并发 DataDriven 批**（`ItemCollectingDialogProtocolAlignmentTest` ×2、
  `Quest80875RetailAlignmentTest`、`ReportRowRewardProjectionContractTest` ×4）——因该批删了 80745/80945/80798/19672/80875
  的 XML 而测试仍直读文件；**不要去修别人的批**，只在自己的报告里点名归因。
- 并行**只允许** `-DforkCount=N`；**禁止** `-Dparallel=classes`（同 JVM 共享静态状态）。
- `RetailDataDrivenGateTest` 是并发批的在飞门禁（单次 >13 分钟），T3 选择器按上面排除。

**关键路径**：

| 用途 | 路径 |
|---|---|
| 真端数据（UTF-16 + 内部 DTD） | `<真端根>/Map/XML/`（`quest.xml`、`Quest_*.xml`、`data_driven_quest.xml`、`npcfactions_quest.xml`、`challenge_task.xml`、`npcs.xml`） |
| 真端二进制 | `<真端根>/MainServer/{ScriptDLL64.dll,Server64.exe}` |
| 反编译源码 | `<真端根>/server58-source/{MainServer_ScriptDLL64,MainServer_Server64}/`（巨文件 `ScriptDLL64.c` 271 万行 / latin-1，grep 加 `-m`） |
| 客户端解包（UTF-8） | `<客户端解包根>/data_unpacked/Dialogs/**/quest_q<id>.html`；`Quest_unpacked/{quest.xml,quest_monster.csv,quest_script_monster.csv}` |
| 本仓真端数据 | `src/main/resources/aion/data/static_data/quest_retail/`（各族表 + `quest.xml` + `retail-xml-retention.tsv` + `quest_client_*.tsv`） |
| 证据与工具 | `.agents/summary/scriptdll-quest-driver/`（**中间产物只能放这里**，禁止 `scripts/`、禁止 `.agent/`） |

**可复用工具（优先复用，不要重写）**：

- 回归/对账：`run_quest_gates.sh`、`affected_quest_tests.py`、`verify_retirement.py`、`build_retention_list.py`、
  `refresh_catalog_doc_links.py`。
- **P0c 缺口裁定机**（本线最值钱的资产，跨族可复用）：
  `p0c8c_gap_decisions.py`（分级放宽多重集配对 + 机制解释器 + 可达性 + 客户端动作 id ⇒ `ADOPT`/`KEEP:<码>`）、
  `p0c8c_retire_gap_rows.py`（断言 0 UNRESOLVED 才落地：写裁定表 → 追加生产裁定表 → `Path.unlink()` 删 XML → 重写缺口表）。
- 各族退役脚本：`m2e_retire_migrated_xml.py`、`m3b_retire_simple_talk_xml.py`、`m4b_retire_combine_task_xml.py`、
  `m5b2/m5b3/p1b/p3b/p5_retire_*` 等；漂移普查 `retail_family_coverage.py`、`reconcile_*.py`。
- 真端反向工程：`re_tool_extract.py`、`m5b2_vtable_scan.py`、`m5b2_region_xref.py`、`m5b2b_*` 系列。

---

## 4. 下一步队列（按优先级，做完一项立刻做下一项）

### P0c-9（立刻做）SimpleHunt 族收口对账

目标：把 SimpleHunt 从"可合成 + 缺口登记"推进到**族口径闭环**，产出族级分歧表（每行都有稳定码与证据）。

1. **42 行真端表缺口复核**（`simplehunt-dialog-route-gaps.txt`：`KILL_COVERAGE_LOSS` 28 / `CLIENT_BUTTON_UNWIRED` 11 /
   `CLASS_SELECTABLE_REWARD` 2 / `XML_EXTRA_REWARD` 1）：
   - `CLASS_SELECTABLE_REWARD`（11102/28313）**是唯一可技术归零的码**：真端 `quest.xml` 用
     `<class>_selectable_reward` 区块表达职业可选奖励，落地进 `RetailQuestMetadataCompiler` /
     `RetailSimpleHuntDefinitionCompiler` 后这 2 行可转 `ADOPT_RETAIL`（顺带惠及 SimpleTalk/CollectItem 的同类行）。
     落地后必须重算冻结指纹并跑族门禁 + T3。
   - `CLIENT_BUTTON_UNWIRED`（1470/1548/30222/80751/80753/80763/80764/80765/80768/80769/80770）：逐行判定客户端按钮
     （select5 的 `20002` 检查、报告页 `1009`）在真端路由集里是否真有等价物（如 `NPC_REPORT` 的另一种 confirm id 表达）；
     有则转 ADOPT，无则把"真端缺口"写进保留清单证据列。
   - `KILL_COVERAGE_LOSS` 28 行：逐行核对"客户端点名且生产刷怪可达"的怪是否可由真端 `monster*` 列之外的
     真端源（`quest_monster.csv` 的 SECTION 映射 / `npcs.xml` 刷怪）补齐；不能则保持 KEEP 并补可达性证据。
2. **108 行稳定拒绝码**（`retail-simplehunt-compiler-rejects.tsv`）× **2 行驱动覆盖缺口**（`QUEST_SPAWN_UNEXPRESSED`）
   × **83 行 M3-d 降级（SimpleTalk）**：产出族口径分歧表（码 → 行数 → 是否可归零 → 归零路径）。
3. 收口判据：`RetailSimpleHuntFamilyGateTest`（942 行族不变量）绿 + `verify_retirement.py` OK + T3 clean 对账零自因新增。

### P0c-10 SimpleTalk 收口（2223 行）

585 个稳定拒绝码分类收敛（322 对话链 / 162 物品轴 / 60 挑战哨兵无入口 / 19+6 名字未解 / 12 过场 / 4 复合名）+
83 行 `m3d-downgraded-quests.tsv` 降级复核；同族复用 P0c-9 的分歧表格式。

### P0c-11 非 IR 轴系统化（防回归）

P0c-8c 证明**采纳真端会连带改动 IR 之外的轴**，目前散落三处，须合并成一张登记表并加门禁：
① 元数据封顶/等级（`src/test/resources/quest/quest-start-metadata-retail-cap-exceptions.tsv`）；
② 前置条件两种等价表达（`start-conditions(finished)` ↔ `prerequisites`）；
③ 旧存档归一化（`.agents/summary/scriptdll-quest-driver/p0c6-legacy-save-normalization.tsv`）。

### P1 SimpleItemPlay（15 行，真端表 43 行已入仓）

41 行主形状 `triplet & select1` + 2 行 `_faction_`；按 §5 固定流程做全族（最小族，适合验证流程是否已自动化）。

### P2 DataDriven（1508，最大风险，优先让并发批推进）

本线只做**对账与协同**：读 `p5-datadriven-decisions.tsv` 与 `retail-data-driven-drift.tsv`，
把其未收口的测试面（直读退役 XML）登记进报告归因，**不碰**其活文件。

### P3 终局收口

五类长期门禁（归属 / 家族等价 / 调度逐帧 / 客户端契约 / 降级清单）全绿 → 目录收敛
（catalog = `quests/` 目录 = 归属表）→ 客户端抽检每族 ≥3 → 终局覆盖率报告与保留清单 →
`verify_retirement.py` + 全仓悬空引用 `grep` 证据。

---

## 5. 每族固定流程（写死，不要每次重新发明）

1. 真端表入仓（UTF-16 → UTF-8，**不要覆盖回 UTF-16**）；
2. 表加载类（允许内部 DTD、禁外部实体访问）；
3. 合成器（表行 → 定义 IR）；
4. **真端语义门禁**（不要求逐字等于 XML）；
5. 漂移登记 TSV（逐任务 + 稳定码；**禁止手改**，用 `-Dretail.*.equivOut=<path>` / `-Dretail.*.fingerprintOut=<path>` 重算）；
6. 客户端契约门禁（`QUEST_Q*.html` 出口 / `quest_summary` 行 / 领奖页 / 按钮常量集）；
7. 缺口逐行裁定（复用 P0c 判据机）→ `--dry-run` → 正式**删除** XML（不迁移 test）+ 从 `quest_definition_catalog.xml` 移除条目；
8. 重算 `retail-xml-retention.tsv`（三处副本 sha256 必须一致）；
9. 聚焦门禁 + `verify_retirement.py` + **T3 clean 副本一次**并与基线逐方法 `comm` 对账（only-now 必须为 0 或全部可归因）。

**允许删除 XML 的充分条件（五条同时满足）**：元数据对拍一致（真端优先分歧另列证）/ 进度事件与 DLL 语义及客户端守卫一致 /
合成 IR 与退役前 XML IR 等价（或已逐行裁定并带机制证据）/ 家族门禁绿 / 不在保留清单且无未闭环口径冲突。
任一条不满足 → 进保留清单，**不要改 XML 去凑**。

---

## 6. 执行纪律与硬约束（违反即失败）

**连续执行，中间不要停**：

- 每轮循环：读台账「当前切片/下一步/阻塞」→ 取**最小可推进切片** → 实现 → 跑门禁 → 更新台账
  （DoD / 家族进度 / 当前切片 / 证据索引 / 变更日志）→ **立刻开始下一片**，不要等确认。
- **不要主动提交**：`git commit|push|tag|stash`、新建分支或 worktree **一律不做**（用户明说提交时才做）。
- **不要** `git checkout` / `git reset --hard` / `git clean`（工作区有并发会话的 3800+ 未提交改动）。
- **不要启动/停止/重启服务端进程**（用户管理其生命周期）。
- **Maven 已授权（离线）**：`mvn -o -B test -Dtest=...`，范围限 `com.aionemu.gameserver.questEngine.*` 及本任务
  quest 相关门禁；全量 `mvn test`（除 T3 clean 副本）、`mvn package/install`、改 `pom.xml`、预计 >10 分钟的构建需先请授权。
- **T3 绝不在主工作树跑**（会假绿 + 撞并发构建）；一律 rsync 到仓库外副本，用毕 `rm -rf` 清理。
- **临时 JUnit 探针**：源码快照留在 `.agents/summary/scriptdll-quest-driver/<name>.java.txt`，探针本体必须用
  Python `Path.unlink()` 删除（`rm -f` 可能被权限拦），**收尾时确认仓库内零探针残留**。
- **不要动并发文件**（2026-09-24 实测在飞）：
  `.agents/memory-bank/*`、`.agents/summary/index.jsonl`、`quests/10528.xml`、`quests/20528.xml`、
  `src/main/java/**/questEngine/retail/RetailSimpleUseItem*.java`、`RetailClientUseItemReport.java`、
  `src/main/java/**/questEngine/retail/RetailDataDriven*.java`、`src/test/java/**/RetailSimpleUseItemGateTest.java`、
  `JavaHandlerFamilyDefinitionTest.java`、`QuestDraupnirNpcVariantContractTest.java`、
  `RetailDataDrivenGateTest.java`、`.agents/summary/scriptdll-quest-driver/p5*.py|p5*.tsv`。
- 规范：Java 用 **Tab** 缩进、行宽 ≤120、注释**中英双语**；XML/Java 格式遵循 `.agents/rules/*`；
  **中间脚本只放** `.agents/summary/scriptdll-quest-driver/`（禁止 `scripts/`、禁止 `.agent/`）。
- 需要用户才能做的事（重启服务端、实机客户端验收、提交）→ 记「待用户执行 / 未验证」后**继续其它工作**。

**允许停的只有三种情况**：① 用户明确要求停或改方向；② 需要用户才能做的外部动作（记录后继续别的）；
③ 确实已无可推进项（给终局报告 + 未验证清单）。**禁止**用"要不要继续？""下一步建议…"结束回复。

---

## 7. 作废结论清单 + P0c 判例库（**防止重复推导错误**，必须遵守）

### 7.1 作废/不成立（历史）

| 作废/不成立的结论 | 现行正确结论 | 证据 |
|---|---|---|
| "槽 `+0x1b8` 是采集进度接口" | `IUserImp` 槽 55 是**客户端演出通知槽**；进度读写走 `+0xd0/+0xe8/+0xf0/+0xf8/+0x100` | `m5b2b_slot55_owner_scan.py` + `Server64.c:2248860` |
| "事件码 `0x37` = 合成专用补槽" | **作废**（首版普查漏 `&LAB_*` 处理器，52117 → 80102 行） | `m5b2b_handler_slot_scan.py` 修正版 |
| "事件码 `0x26` = 交付/报告" | **作废**，改判为生命周期三元组 `{0x1c,0x1d,0x26}` 成员 | `m5b2b-event-family-crosstab.tsv` |
| "`0x35` = 采集进度事件" | **不成立**，`0x1e`+`0x35` 是成对通知槽（采集族仅 13%） | 同上 |
| "`REJECTED:RETAIL_ACQUIRE_NPC_SENTINEL` 是解析缺口" | 是**另一类任务形状**（类别哨兵 `_faction_` 等 → 系统发放） | 报告 M5-b2b §4.8/§4.9、M5-b2c |
| "144 个类别哨兵不可由真端文件推导" | 只对"**当成 NPC 推导**"成立；三类哨兵都有真端发放系统 | M5-b2c |
| 接取页探针首版数字（如 SimpleSerialHunt 16 vs 10） | 首版把"无契约行 `?`"当真实 NPC；已改三态 `sentinel/real/no-contract` | `m5b2b_client_accept_page_probe.py` |
| "IR 等价 ⇒ 采纳无副作用" | **作废**：采纳会连带改动非 IR 轴（封顶清单 / 旧存档自愈边 / 前置表达） | P0c-8c §7（`catalog`/`cap-exceptions`/`p0c6-legacy-save-normalization.tsv`） |
| "家族门禁的保留码随便起名" | **作废**：`SEMANTIC_GAP:RETAIL_*` 在门禁/拒绝表里**专指族编译器拒绝码**；真端表覆盖缺口必须用**不带前缀**的码 | P0c-8c（`RETAIL_KILL_COVERAGE_LOSS` → `KILL_COVERAGE_LOSS`） |

**当前有效的事件码语义（零反例口径）**：`0x11`=击杀 / `0x33`=合成（574/574）/ `0x36/0x37/0x38`=采集族签名 /
`0x1c/0x1d/0x26`=生命周期三元组 / `0x1e`+`0x35`=成对通知槽 / `0x32` 近乎全员（读 `+0xe8`）/ `0x3`=NPC/物件交互。
**形状不变量**：`ask_quest_accept` ⇒ 无三元组 97.8%；`triplet ⇒ select1` 99.9%；哨兵 `start_npc_ids=0` 单独不能判形状。

### 7.2 P0c 判例库（**跨族复用**）

1. **缺口裁定 = 分级放宽的多重集配对**：`exact → order（奖励排序）→ blind（同包状态名互换）→ npc（接取 NPC 漂移）
   → dialog（报告页占位 id / 客户端报告页登记）→ shape（状态级节点 + 计数记账剥离 + 同步模式归一 + 奖励排序）`，
   每级只放宽一个轴；配不上的再进**机制解释器**（`KILL_VARIANT` / `EXTRA_KILL_COUNTER_LEGACY` / `PREMATURE` /
   `BRIEFING_PAGE_LEGACY` / `CLOSE_DIALOG_LEGACY` / `ACCEPT_FLOW_LEGACY` / `RETAIL_EXTRA_CLIENT_ROUTE` / `GRID` / `LEGACY_NODE`），
   仍落不进任何类别 ⇒ `UNRESOLVED` ⇒ **禁止落地**（脚本硬断言）。
2. **KEEP 的四个稳定理由**：真端编译集缺客户端点名且**生产刷怪可达**的怪（`KILL_COVERAGE_LOSS`）/
   客户端按钮在真端路由集无对应动作（`CLIENT_BUTTON_UNWIRED`）/ 真端区块编译器未落地（`CLASS_SELECTABLE_REWARD`）/
   旧 XML 多出客户端点名的奖励物品（`XML_EXTRA_REWARD`）。
3. **可达性判据**：只有"生产 `spawns/**`（active map，含实例）+ 任务自身 `<spawn>`"里能刷出来的变体才可观测；
   不可达变体的丢失**不需要**保留。
4. **测试夹具纪律**：退役行的测试**一律改生产视图**
   （`ProductionQuestDefinitions.definition(id)`），并用"按 (状态, 打包投影) 定位"（`startStepsByPack` 之类）
   取代写死旧标签（`started`/`k1`/`ready`）与旧计数记账；真端网格步进的条件/动作通常全空（计数即节点打包值）。
5. **正向锁定采纳差异**：把"真端确实没有的那条 legacy 路由/自愈边"写成**否定式断言**（如
   `noneMatch(... reward --31--> reward ...)`），使差异成为长期契约而不是注释。
6. **门禁类别必须同步登记**：新增保留码要同时加进对应家族门禁的类别白名单，并保持"缺口在真端表 ⇒ 行必须仍能合成"的不变量。

---

## 8. 汇报 = 落盘到文档（每完成一个稳定切片都要写）

**规矩：文档才是记录，对话只给摘要 + 文档路径。**

1. 新建 `reports/<YYYY-MM-DD>-<切片>-<主题>.zh-CN.md`，固定段：
   `## 1 交付`（新增/修改文件 → 一句话作用，标出可复用资产）/ `## 2 机器证据`（命令 + 通过数/扫描 N/M）/
   `## 3 代码·数据改动` / `## 4 对拍结果`（元数据 / 进度事件 / IR / 调度 / 客户端各一项）/
   `## 5 门禁`（T1 / T2 / **T3 clean 副本** 三行 + 失败方法集 comm 对账）/
   `## 6 未验证 / 阻塞 / 下一步`。
2. `reports/INDEX.zh-CN.md` 顶部追加一行（日期 / 切片 / 文档 / 一句话结论）。
3. 更新台账 `GOAL-retail-driver-progress.zh-CN.md`：DoD 勾选、家族进度表、当前切片 / 下一步、阻塞、
   证据索引（命令 → 结果）、变更日志（每轮一行 + 报告链接）。
4. 对话回复只给：本切片结论 + 证据数字 + 报告路径 + **已继续推进的下一个切片**。

---

## 9. 完成判据（DoD，全绿才允许把 goal 置为 complete）

- [ ] 5554 个任务各有机器可读 owner 记录，且全部由真端文件驱动（不读 XML）
- [ ] 670 个任务在保留清单中，逐条有原因与证据；无未归类任务（5554 + 670 = 6224）
- [ ] 生产接线可用：开关关闭 = 改造前行为，开启 = 按 owner 表加载
- [ ] 被迁移 XML 已从 `quest_definition/quests/` 删除，`quest_definition_catalog.xml` 已收敛（catalog = 目录 = 归属表）
- [ ] 五类门禁（归属 / 家族等价 / 调度逐帧 / 客户端契约 / 降级清单）实现且长期绿
- [ ] 客户端抽检：每族 ≥3 个任务，`quest_summary` 行与 `SECTION_n` 计数一致
- [ ] 终局报告（覆盖率、保留清单、口径分歧、未验证项）落盘
- [ ] 全仓无对被删 XML 的悬空引用（`grep` 证据）

**开工第一件事（按顺序）**：读台账与 `reports/INDEX.zh-CN.md` 最新三篇
（`2026-09-24-P0c8c-…` → `2026-09-24-P0c8b-…` → `2026-09-24-P0c8a-…`）→
跑一次 `verify_retirement.py` + `run_quest_gates.sh T1` 复核基线 →
从 §4 的 **P0c-9** 开始切片，每片落盘后立刻继续。

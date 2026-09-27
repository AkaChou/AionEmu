# W5 续片 + W6 取证包（四路只读取证，2026-09-27）

> 车道：`quest-native-dispatch`；用途：**W5 续片**（`talk_pages` / `briefing_chains` / `dialog_exits` 退役与缩表）
> 与 **W6 清理**的**可执行前置**。取证方式：四个只读子代理（禁 Maven、禁改文件），结论经主执行体复核。
> 纪律：本文只记录事实与前置，**不含任何已执行的改动**（除 W5-g1/g2 已收口的两张表）。

> **【2026-09-27 22:40 执行现状】** 本包是 W5-g1..g4 **执行前**的只读取证（事实部分实测有效）；两张表**均已退役**。
> 两条预判经实测**修正**：① §1 的"9 行"实为 **6 行**（`80827/80828/80832` 在 `:102` 更早的门就被拦下，
> 是 retention 派生滞后造成的假象），且是**真解锁**（非"拒绝码漂移"）；② §2/§6 的"briefing 是唯一会解锁新受理行"
> **反了**——briefing 门今日**零拒绝**，真正的解锁面是 `talk_pages`（6 行）。终态与范式见两份 W5-g 台账与 GOAL §0.5。

## 1. `quest_client_talk_pages.tsv`（1340 行）：**值已死、门仍活**（**已退役：W5-g3**）

| 项 | 事实（file:line） |
|---|---|
| 值读取点（全部 3 处） | 均在 `RetailDataDrivenDefinitionCompiler.java`：`:174` 受理门（唯一活语义）、`:462` `orElseThrow()` → `pages.entryPage()/completionPage()`、装载穿线（driver `:91/507-508/552/786`，非读值） |
| `:462` 零效果证明 | `RetailDataDrivenTalkCompiler.java:48-52` 的 `entryPage` **不下传不读**；`completionPage` 转发到 `assemble`（`:110-146`）后函数体内**零出现**（奖励窗页取 `:133`、完成流取 `:143`） |
| 受理门判据 | 登记缺失即 `RETAIL_TALK_VOCABULARY_UNSUPPORTED` |
| 今天被该门拒绝的行 | **恰 9 行**：`25200 80814 80823 80824 80827 80828 80830 80832 80833`，在 `retail-xml-retention.tsv` 与 `quest/retail-data-driven-drift.tsv` 双处登记为 `SEMANTIC_GAP:RETAIL_TALK_VOCABULARY_UNSUPPORTED` |
| **fail-open 陷阱（必须同片改）** | `RetailQuestDriver.java:793-796` 把 `RuntimeException` 兜成 `RUNTIME_FAILURE` ⇒ **"只删门、不改 `:462`"会静默漂移拒绝码**（编译过、审查看不出，只有 reason 断言门以误导性消息失败）；反之"只改 `:462` 默认页号、不改门"是真 fail-open |
| 退役前置 | ① 门与 `:462` **同一片**改：门换成 canonical 判据、`:462` 的 `orElseThrow` 与取页一并删除；② 重算 9 行的 drift/retention 登记（预期是**拒绝码漂移**而非真解锁——它们登记为"编译器缺口"）；③ 断言门重锚；④ 三步退役（表/清单/计数）〔**执行结果**：实为 **6 行真解锁**；判据=任务书行；1 受理 + 5 换码；②的预期被实测否决〕 |

## 2. `quest_client_briefing_chains.tsv`（3225 行）：**活受理门**（canonical 行同样吃它）

| 项 | 事实（file:line） |
|---|---|
| 唯一值读取点 | `RetailSimpleHuntDefinitionCompiler.java:197-206`（`chain(...)`/`hops()`） |
| 门的执行位置 | `requireBriefing(...)`（`:185`）在 `compile(...)` **早段无条件执行**；`canonical` 形参已在 W5-g1 删除 ⇒ **canonical 行同样过门** |
| 表载荷退化 | 实际只剩**布尔谓词**（"存在 select2 简报链且末跳为 SETPRO"）；`entry_page` 列**全仓零读**，`hops` 仅看末跳 |
| 换门的方向 | **只新增、不失去受理**；但 47（编译器注释称 `talk_npc1` 行数）vs 23（裁定表覆盖行数）的**差集必须现算** |
| 风险（中） | 只删表不改门 = fail-closed（安全）；**换门才是 fail-open 面**——会丢掉"客户端确有该链"的唯一证据源 ⇒ 任务可能卡死在客户端 `SECTION_5==0` 击杀门 |
| 周边现状 | retention 942 行 `SimpleHunt` 中 785 行已 `reason=OK`；`RETAIL_BRIEFING_*` 拒绝码在 retention/decisions 中**零命中** |
| 退役前置 | ① 先算 47−23 差集并逐行判"真解锁 vs 拒绝码漂移"；② 门换 canonical 判据（简报 NPC 唯一 + 目标投影可表达"见中间人才开计数"）；③ retention/家族门同步；④ 三步退役 〔**执行结果**：零拒绝 ⇒ 退役零受理变化；不变量迁构建期常设门，②的"换门"未取〕 |

## 3. `quest_client_dialog_exits.tsv`：**无整旗标死亡**，只能按行缩表

| 项 | 事实（file:line） |
|---|---|
| 旗标 | 7 个（`RetailClientDialogExits.java:30-42`），类无第二访问器 |
| 调用点 | 全仓 `requires(` **12 处 / 3 文件**；测试**零调用** |
| 死活 | **无整旗标死亡**——7 个旗标各有至少一个 canonical 可达调用点；死亡面只在**行级**（singleStep 行 / 非 systemGrant 链行 / 已 canonical 交付段行） |
| 窄活旗标 | `SELECT1_1`、`SELECT1_1_1` 仅在 **systemGrant ∧ 有 NPC_START 块的链行**可达（`RetailSimpleTalkDefinitionCompiler.java:1189/1212`） |
| 缩表最小动作 | **按行 × 旗标做可达性过滤删 token**（不删旗标）；判据 = 缩表前后**家族门定义快照逐键相同** |
| 盯表门禁 | `RetailTsvManifestGateTest`（`:29/:48`）+ manifest 第 27 行 + 7 个家族 GateTest 的 `load()` 点 |

## 4. W6 清理（逐条核实）

| # | 项 | 实测结论 |
|---|---|---|
| 1 | `RetailSimpleHuntIrCompiler` 零生产引用 | **成立**（源码侧仅自身 + 唯一测试 + `RetailSimpleHuntDefinitionCompiler.java:40,49` 两处 javadoc 提及）；删除面 = 删两类 + 改这两行 javadoc；**3 个测试资源因有第二消费者须保留**；**不在 T1 门禁注册面** |
| 2 | `EarlyElyosQuestRegressionTest` 3 条在册红 | **成立**（最新 `gates/T3-205507.log` 该类 3 Errors；红集与四份历史红集逐字恒等）；期望形状：`:67`（1131 `started→shugo` 交付边）、`:252`（1561 `CanAct` 自环门）、`:311`（1691 `spoken-to-diana→returned-to-sneaker`） |
| 3 | `FailurePage` 命名 | 4 处载体（XML `failure-page` 属性 / TSV `failurePage` token / 布尔 `hasFailurePage` / 测试方法名）；**仅 `RetailSimpleTalkMigrationReviewContractTest.java:48` 名实相反**；另有**一名指两页**的歧义（SELECT6=2716 vs CHECK_USER_ITEM_FAIL=10001） |
| 4 | `retail-quest-ai-name-groups-rejected.tsv` | 22 行（18 数据行）、**零生产消费者**；生成器 `p0c52_quest_ai_name_groups.py:313`（写在 `if emit:` 内）；**缺口 = 表头与清单 note 都只写 basename、无路径/车道** |
| 5 | `quest_use_item_npcs.tsv` | **808 行非空表**且有多消费者（`RetailQuestDriver.java:88`、`RetailQuestUseItemNpcs.java`、三个编译器消费点）⇒ GOAL §3 W5 行的"空表"表述**过期**（本文据实更正） |
| 6 | `report_pages` 生成器停写（W5-g1 移交） | **仍会写回**退役路径（`build_quest_client_report_pages.py:34-35` + `:67`，无 emit 开关）⇒ 属 `scriptdll-quest-driver` 车道，本车道**只登记不移交改脚本** |

## 5. 两处**过期基数**更正（取证附带发现）

| 位置 | 过期表述 | 实测 |
|---|---|---|
| `2026-09-27-tsv-retirement-candidates.zh-CN.md:152` | `EXPECTED_TSV_COUNT` 当前 **27** | 取证时为 **24**；**2026-09-27 22:40 现值 = 22**（g3/g4 各 −1） |
| `src/test/resources/quest/retail-data-driven-drift.tsv:13` 注释 | 计数 **6**（取证代理报"实测 9"） | **复核否决该条**：该文件内 `RETAIL_TALK_VOCABULARY_UNSUPPORTED` 实测 **6** 行 = 注释 ✅；9 是**另一个口径**（`retail-xml-retention.tsv` 里同码行数）⇒ 注释正确，无需改动（代理口径混淆，已更正） |

## 6. 执行序建议（按风险从低到高）

1. **W6-①（零形状、零受理变化）**：删 `RetailSimpleHuntIrCompiler` + 其测试 + 修 2 行 javadoc；T1/T2/T3 复核 + SimpleHunt 家族门。
2. **W6-③/④（文档级）**：`FailurePage` 命名澄清（仅测试方法名 + 歧义登记）；`rejected.tsv` 表头/清单 note 补路径与车道（生成器停写仍移交兄弟车道）。
3. **W5-g3（形状片）**：`talk_pages` 门 + `:462` 同片改（先算 9 行 drift 走向）。
4. **W5-g4（形状片，唯一会解锁新受理行）**：`briefing_chains` 门改写（先算 47−23 差集）。〔**勘误：唯一解锁面是 W5-g3 `talk_pages`（6 行）；W5-g4 实测零拒绝、零受理变化**〕
5. **W5-g5（行级缩表）**：`dialog_exits` 按行 × 旗标可达性删 token（判据 = 家族门定义快照恒等）。

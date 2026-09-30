# 批 P4e 执行台账：talk+collect 混合链构建期生成（P4e-1）+ enterarea 区名登记门（P4e-2）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 授权：用户对 P4e 三点的「同意」——① P4e-1 按 20 行冻结源 + Java/Maven 构建期生成 + 计数 17→16；
> ② P4e-2 保留解析表 + 新增登记门；③ 冻结源路径 `aion/definitions/quest_dialog/`。
> 上游立项：`2026-09-28-p4e-mixed-registry-recon.zh-CN.md`（含复现度实测）；
> 判据实测：`gates/p4e-static-equivalence.txt`。

## 0. 执行偏差（1 处，按 P4d 先例声明）

冻结源落地为 **`talk_collect_frozen_facts.csv`**（批准文本写的是 `.tsv`）。原因：
`RetailTsvManifestGateTest` 的冻结面**同时覆盖 `src/main/resources/aion/definitions/quest_dialog/*.tsv`**——
若用 `.tsv`，新源必须登记进 manifest 并把 `EXPECTED_TSV_COUNT` 加回 17，**净收益归零**；
P4d 的入仓源（`definitions/quest_monster/quest_monster.csv`）同族即 CSV。改判后冻结 TSV 数保持 **17→16**。

## 1. P4e-1：`quest_client_talk_collect_chain_pages.tsv` 改构建期生成

- **冻结源**（20 行 + 注释，列 `kind,quest_id,key,value,provenance`）：
  `src/main/resources/aion/definitions/quest_dialog/talk_collect_frozen_facts.csv`
  - `exclude` ×7（9639, 13956, 15322, 16839, 23956, 25322, 26839）← 退役 `quest_client_talk_pages.tsv`
    的 1340 行注册表里**只有这 7 个 id 影响本表**（DD 混合集 148 中 TALK-only 7 / CHAIN 25 / HANDIN 0）；
  - `cutscene` ×11（quest, 尾页 id, movie id）← 外部 `data_unpacked/Dialogs/<source_file>` 的 `<CutScene id>`；
  - `withheld` ×2（20035, 20501）← `p0c48_add_pvp_collect_stage_rows.py:WITHHELD`（TALK_HUNT_CHAIN_DEFERRED 桶）。
- **生成器**：`src/main/generator/java/com/aionemu/tools/questgen/QuestTalkCollectChainPagesGenerator.java`
  （414 行，逐行移植 254 行 Python 分析脚本：eligible 规则、`select{i}` 按钮图 BFS 页梯、阶段类别与真端
  stepCategories 逐位对齐、collect 段 39/20002 + ok/fail 结果页、梯尾过场、exclude/withheld 过滤）。
- **Maven**：antrun 3.2.0 新增执行 `generate-retail-talk-collect-chain-pages`（绑 `generate-resources`），
  与 P4d 的 monster 执行同链；输出 `target/generated-resources/aion/data/static_data/quest_retail/`。
- **退役同轴**：删源码树表 + manifest 42→41 行 + `EXPECTED_TSV_COUNT` 17→16。
- **复现探针**（分析面，只读）：`tools/p4e_talk_collect_repro_probe.py`
  ① Python 分析生成器（TALK 接回退役快照）输出 − WITHHELD == 退役前手工表（逐字节）；
  ② 构建期生成物 vs 手工表 = **只差第 4 行生成器署名**，86/86 数据行逐字节相同。

## 2. P4e-2：`quest_enterarea_zone_resolution.tsv` 保留 + 新增登记门

- 表**保留**（声明式注册表，别名→登记名是遗留语义映射，不可重算；146 行 / 101 个不同目标区名）。
- 新增常设门 `src/test/java/com/aionemu/gameserver/questEngine/retail/RetailEnterAreaZoneRegistrationGateTest.java`
  （2 例）：①解析表结构（三列必填、非空）；②**每个目标区名必须已在 zones 目录登记**
  （`zones_quest.xml` + 逐地图 `zones_<mapId>.xml`，覆盖 15 个只登记在逐地图文件的区名）。
  另设登记名下界 4000 断言，防目录缺失/解析失效导致**真空通过**。
- **负向验证**：源表临时追加 `NO_SUCH_ZONE_99999` ⇒ 门变红并报出该区名；恢复后源表 sha256 不变
  （`7e21f008…`）且门回绿。
- 生成侧纪律（不再由脚本直改 `zones_quest.xml`）在 P4 立项书 §6.5 已登记，本轮无需改码。

## 3. 验收判据与实测（详见 `gates/p4e-static-equivalence.txt`）

| # | 判据 | 结果 |
|---|---|---|
| A | 生成物 vs 退役前手工表 | ✅ 旧表 `6b3faba5…`（90 行）、生成物 `2b379d23…`（90 行）；差异行 = **[4]**（生成器署名），86/86 数据行 + 其余 3 行注释逐字节相同 |
| B | 构建管线落位 | ✅ `generate-resources` → `target/generated-resources`；`process-resources` → `target/classes/aion/...`（同 sha） |
| C | 冻结事实探针 | ✅ `exclude 7 / cutscene 11 / withheld 2`；①逐字节相同 ②差异行 `[4]` |
| D | IR 指纹恒等 | ✅ DD 1220 行 / 链 288 行与 P4d 基线 `cmp` 逐字节相同 |
| E | 聚焦门 | ✅ 清单门 3/3、SimpleTalk 链 8/8、EventShard 3/3、HuntClientCount 3/3、SerialHunt 5/5；`RetailDataDrivenGateTest` 6 例 1 红（在册 20035，与 P4b/P4c/P4d 同一条） |
| F | P4e-2 新门 | ✅ 2/2 绿；负向注入必红（已实测） |
| G | T1 生产门（88 例 / 395s） | ⚠️ 2 红：①在册 20035（基线同）②`RetailSimpleCollectItemGateTest.fixtureMatchesTheProductionDriver` = **并行车道 WIP**（13:4x 他们把 fixture 侧改为套用 `RetailClientAcceptEntryPage.repair` 对照，生产 overlay 尚未同步 ⇒ 18 例分叉）；本批**未改任何生产 Java**，红集见 `gates/p4e-T1-reds.txt` |

## 4. 纪律回执

- 未启动/停止/重启服务；未创建 worktree；未 push；未纳入并行车道的 80787/ClientDialogAlignment 改动。
- 本批改动面：1 删表 + 1 源 CSV + 1 Java 生成器 + pom 1 个执行 + manifest/计数 + 2 个门禁测试。
- 主树门禁可用（并行车道 80787 编译错误已自愈）；T1 第 2 红已归因并留证。

## 5. 遗留

- **P4f**：`quest_client_talk_chain_steps.tsv`（编译器 IR）的替代 IR 设计，单独立项。
- 冻结事实一旦漂移（DD 步骤类别、客户端 CSV、过场 HTML 变化），探针以「行数 + 逐字节」双口径报警；
  重新冻结必须人工核对 `p0c48:WITHHELD` 与 `TALK_HUNT_CHAIN_DEFERRED` 桶是否仍成立。

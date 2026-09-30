# 批 P4e 只读立项（2026-09-28）：混合链页梯表 + enterarea 区名解析表


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 授权：用户「开始」（承接 P4d 提交后的「下一步」清单）。
> 性质：**只读评估 + 决策点**（已获用户「同意」并执行——执行回执与判据见
> `2026-09-28-p4e-execution.zh-CN.md`；本文件保留立项时的实测与方案原文）。
> 上游：`2026-09-28-p4-projection-layer-architecture-charter.zh-CN.md` §2 / §6.5。
> 实测证据：`phase3-provenance/p4e-repro-measurements.txt`（复现度与登记度实测）。

## 0. 一句话结论

两张表**性质完全不同**，不能同片处理：

- `quest_client_talk_collect_chain_pages.tsv`（86 数据行）= **可完全重放的派生投影**。
  实测：把退役快照接回被退役的输入位后重跑生成器，输出去掉 2 个 WITHHELD 行后与生产表
  **逐字节相同**；非仓内依赖面只有 11 条过场事实 + 7 个排除 id ⇒ 走 P4d 同型
  「源入仓 + Java/Maven 构建期生成」，冻结源仅 **20 行**。
- `quest_enterarea_zone_resolution.tsv`（146 数据行）= **声明式注册表**（遗留语义映射，
  不可由 alias 规范化推导，命中率 50/146），且**无仓内生成源**（`.agents` 快照只有 49 行）
  ⇒ 建议**保留** + 新增「目标区名必须已登记」fail-closed 门（实测 101/101 已登记）。

## 1. P4e-1：`quest_client_talk_collect_chain_pages.tsv`（可构建期生成）

### 1.1 复现度实测（关键结论）

- 生成器 254 行：`.agents/summary/scriptdll-quest-driver/build_quest_client_talk_collect_chain_pages.py`。
- 仓内输入：`data_driven_quest.xml`、`docs/quest/client-dialog-mapping/quest-dialog-pages.csv`
  与 `quest-dialog-action-details.csv`、`quest_client_handin_pages.tsv`、`quest_client_talk_chain_pages.tsv`。
- **断裂输入**：`quest_client_talk_pages.tsv` 已于 2026-09-27 退役（W5-g3），生成器直接 `read_text` 会
  `FileNotFoundError`；把快照 `retired-tsv/quest_client_talk_pages.tsv.retired-20260927` 接回后重跑，
  得到 88 行 vs 生产 86 行。
- **逐字节判据**：生成物去掉 WITHHELD 两行（20035 / 20501）后 == 生产表（92 vs 90 行，含 4 行表头注释）✅

### 1.2 三处非仓内 / 已退役输入的最小冻结面（合计 20 行）

| kind | 条数 | 内容 | 出处 |
|---|---|---|---|
| `exclude` | 7 | 9639, 13956, 15322, 16839, 23956, 25322, 26839 | 退役 `quest_client_talk_pages.tsv`（1340 行注册表里只有这 7 个 id 影响本表；DD 混合集 148 中 TALK-only 7、CHAIN 25、HANDIN 0） |
| `cutscene` | 11 | (quest, 尾页 id, movie id) | 外部 `data_unpacked/Dialogs/<source_file>` 的 `<CutScene id>`（如 16942 → 页 1012 → movie 899） |
| `withheld` | 2 | 20035, 20501 | `p0c48_add_pvp_collect_stage_rows.py:WITHHELD`（TALK_HUNT_CHAIN_DEFERRED 桶，刻意不带入） |

11 条过场事实全表：

```
15306	4081	994	 15316	4081	994	 15604	1694	1000	 15605	1353	1002
15613	1353	875	 16942	1012	899	 25306	4081	866	 25316	4081	866
25602	1694	872	 25604	1012	874	 26942	1012	900
```

### 1.3 拟议源文件形态（待裁）

`src/main/resources/aion/definitions/quest_dialog/talk_collect_frozen_facts.tsv`
（20 数据行 + 注释；列 `kind / quest_id / key / value / provenance`）——
**记录出处、不钉 sha**（沿用 P4d 裁定：外部源会持续修订）。

### 1.4 执行方案（批准后执行，P4d 同型）

1. 冻结源入仓（20 行，带 provenance 列）；
2. Java 生成器 `src/main/generator/java/com/aionemu/tools/questgen/QuestTalkCollectChainPagesGenerator.java`
   （移植 Python 语义：DD 步骤类别与 eligible 规则、`select{i}` 按钮图 BFS 页梯、阶段类别与
   stepCategories **逐位对齐**、collect 段 39/20002 + ok/fail 结果页、过场附加、exclude/withheld 过滤）；
3. Maven：并入 P4d 已建的 `maven-antrun-plugin` `generate-resources` 链，输出
   `target/generated-resources/aion/data/static_data/quest_retail/`；
4. 同轴退役：删源码树表 + manifest 删 1 行 + `EXPECTED_TSV_COUNT` 17→16；
5. 验收判据：旧表与生成物**逐字节相同**、DD/链指纹恒等、T1/T3 红集恒等、
   聚焦门（含 `RetailDataDrivenGateTest`）1 红（在册 20035）。

## 2. P4e-2：`quest_enterarea_zone_resolution.tsv`（建议保留 + 加门）

- 146 行 / 101 个不同目标区名，**101/101 已在 zones 目录登记**（131 条在 `zones_quest.xml`，
  另 15 条只在逐地图 `zones_*.xml`，如 `FALLOW_RUINS_210100000`、`KROTAN_REFUGE_400010000`）⇒ 无死边。
- 别名→登记名是**遗留语义映射**（`LF6_SensoryArea_Q10527a` → `FALLOW_RUINS_210100000`），
  规范化约定命中率仅 50/146 ⇒ 不可重算、不可推导。
- 无仓内生成源：`.agents/summary/scriptdll-quest-driver/quest_enterarea_zone_resolution.tsv` 仅 49 行 ⊂ 生产 146 行。
- 建议处置：**保留为声明式注册表**，另加 fail-closed 门
  「表内每个目标区名必须已在 zones 目录登记」（防未来删区 / 改名导致 EA 步静默死边）；
  生成侧纪律（不再由脚本直改 `zones_quest.xml`）在 P4 立项书 §6.5 已登记，本轮无需改码。

## 3. 需要裁决的三点

1. **P4e-1 是否按 §1.4 执行**（20 行冻结源入仓 + Java/Maven 构建期生成 + 计数 17→16）？
2. **P4e-2 是否按「保留 + 新增登记门」处理**（不删表）？
3. **冻结源放置路径**：`src/main/resources/aion/definitions/quest_dialog/`（与 P4d 的
   `aion/definitions/quest_monster/` 同族）是否可以？

## 4. 纪律回执

- 本轮**只读**：未改生产数据/代码，未跑 Maven，未提交；仅在
  `.agents/summary/quest-native-dispatch/` 落立项与实测证据。
- 复现探针 `tools/p4e_talk_collect_repro_probe.py` 需外部 `data_unpacked/Dialogs` 与退役快照，
  属分析面只读工具（不参与生产构建）。

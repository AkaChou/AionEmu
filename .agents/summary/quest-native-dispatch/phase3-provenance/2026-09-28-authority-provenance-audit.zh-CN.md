# Phase 3 权威血缘审计：21 张在册 TSV 的"真端驱动"成色（只读 · 2026-09-28）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 授权：用户「当前的多余 tsv 导致任务不是和真端一样驱动的」→「开始」。
> 范围：只读取证（零形状、零 Maven、红集不解冻）；对象 = manifest 在册 21 张
> （P1 已退役的 `retail-quest-ai-name-groups-rejected.tsv` 不在册，仅作背景）。
> 产物：`authority-provenance.tsv`（21 行 × 9 列矩阵）+ 本报告。

## 0. 一句话结论

**运行时主驱动确实是真端表**：`RetailQuestDriver.load()` 直接加载 9 张真端解包表
（quest.xml 11.3MB / data_driven_quest.xml / Quest_SimpleTalk / SimpleHunt / SimpleUseItem /
SimpleItemPlay / SimpleSerialHunt / CombineTask / SimpleCollectItem），retention 账标定
**6224 = 4983 真端表驱动 + 1241 保留 XML**。TSV 是三类辅助层：**客户端合同**（真端服务端表
本就不含页面/按钮/行数列）、**派生索引/转写**（真端表的投影）、**审计/契约账**（不驱动行为）。
本次审计**未发现任何一张表注入"三源之外"的行为**（三源 = 真端表 ∪ 客户端证据 ∪ 旧存档兼容）；
但发现 **3 张表再生成通道缺失/不符**、**1 张表门禁覆盖待说明**——这是"血缘缺口"，不是"行为偏离"。

## 1. 方法

1. manifest 21 行 × 文件头来源/生成器声明；
2. 全树 grep 生成器（`.agents/**/*.py|sh`）与写入路径核对（含 `OUT` 常量实读）；
3. `phase3-recon/census-readers.tsv` 消费方矩阵（main/test 直达引用）；
4. 外部输入存在性（客户端解包 / docs 客户端映射）；
5. 逐表判"是否含真端表没有的内容"（adds_beyond_retail）并给裁决。

## 2. 总表（21 张 → 四类裁定）

| 裁定 | 张数 | 表 |
|---|---|---|
| 必须保留（权威源 + 可复核来源） | **17** | 见下矩阵 |
| 必须保留但需缩表 | **1** | `quest_client_dialog_exits.tsv`（P2：行级可达性普查 v2） |
| 内容保留、**再生成通道需补** | **3** | `quest_client_use_item_report.tsv`（G1）、`quest_name_string_ids.tsv`（G2）、`quest_client_kill_targets.tsv`（G3） |
| 必须保留、**门禁覆盖需说明** | **1** | `quest_legacy_heal_rows.tsv`（G4，census 口径 test=0） |
| 无证据注入（退役候选） | **0** | —— |

> 口径说明：G1–G3 是"不可重算"缺口（表内容本身有来源声明与消费证据），不是"凭空注入"；
> 四类互斥计数：17 + 1 + 3 + 1 = 22，其中 `dialog_exits` 同时属"必须保留"与"需缩表"，
> 故唯一行 21 张。

### 2.1 逐表矩阵（节选；全量见 `authority-provenance.tsv`）

| 表 | role | 权威 | 生成器 | 是否补真端表没有的 | 裁定 |
|---|---|---|---|---|---|
| quest_client_dialog_exits | client-registry | 客户端映射 CSV（已入库 17 文件） | build_quest_client_dialog_exits.py | **是**：补真端表没有的续页路由 | 保留 + P2 缩表 |
| quest_client_handin_pages/_exceptions | client-registry | 客户端五页集合 | build_quest_client_handin_pages.py | 是（页面列） | 保留 |
| quest_client_hunt_progress_rows / hunt_stages | retail-table | 真端 quest_monster.csv | build_quest_hunt_progress_rows_tsv.py / p3_client_hunt_stages.py | 否（投影） | 保留 |
| quest_client_kill_targets | retail-table | 真端 quest_monster.csv + npc_template + 刷怪自检 | 声明脚本实为**测试夹具**写入器 | 否（投影） | 保留；补再生成通道（G3） |
| quest_client_kill_targets_stages | retail-table | 真端 quest_monster.csv 变体 | generate_stage_kill_targets.py | 否 | 保留 |
| quest_client_reward_npcs / summary_rows / talk_chain_pages | client-registry | 客户端任务书（行数/页梯/交付 NPC） | m5b3x / build_quest_client_summary_rows / build_quest_client_talk_chain_pages | 是（客户端列） | 保留 |
| quest_client_talk_chain_steps | retail-table | 真端表 + 客户端背书 + XML 转写（词汇 fail-closed） | build_quest_client_talk_chain_steps.py | 否 | 保留 |
| quest_client_talk_collect_chain_pages | retail-table | 真端 stepCategories + 客户端页梯 | build_quest_client_talk_collect_chain_pages.py | 部分（页梯客户端；类别序列真端对齐） | 保留 |
| quest_client_use_item_report | client-registry | 客户端 select5 按钮 | **声明脚本不在树内** | 是（客户端列） | 保留；补再生成通道（G1） |
| quest_enterarea_zone_resolution | retail-table | 真端 questscript_area + 遗留 enter-zone | p0c42/p0c48 注册脚本 | 否（别名解析） | 保留 |
| quest_name_string_ids | name-index | 客户端 client_strings_quest.xml + quest_data.xml 回退 | **无脚本** | 是（字符串 id 索引） | 保留；补再生成通道（G2） |
| quest_use_item_npcs | server-registry | npc_template_*.xml | build_quest_use_item_npcs.py | 否 | 保留 |
| retail-quest-ai-name-groups | name-index | 客户端 npc 块 + 服务端模板 + 词典（fail-closed） | p0c52_quest_ai_name_groups.py | 是（组名→成员展开通道） | 保留 |
| retail-xml-retention | retention | 手工裁定账（6224 owner） | build_retention_list.py（滞后移交 ③） | 不适用 | 保留 |
| quest_legacy_heal_rows | heal | **旧存档兼容（非真端）** | 手工（设计如此） | 是（非真端通道） | 保留；门禁说明（G4） |
| client_dialog_contract | contract | 客户端 CSV（源 sha256 双钉） | generate_quest_dialog_contract.py | 不适用（编译期） | 保留 |
| movie_continuation_exceptions | contract | 手工例外账（需证据评审） | 手工（设计如此） | 不适用（编译期） | 保留 |

## 3. 血缘缺口（G1–G4，只登记不改）

- **G1 `quest_client_use_item_report.tsv`（已冻结）**：表头声明生成器 `p3b_client_use_item_report.py`，
  全树 0 命中（106 行表不可重算）。处置 = **内容 sha256 冻结**（`provenance-pins.tsv` G1 + 校验器），
  补生成器登记为后续项。
- **G2 `quest_name_string_ids.tsv`（已冻结）**：10164 行，无任何写入脚本（全树 .py 0 命中）。
  处置 = 表内容 sha256 + 主来源 `client_strings_quest.xml`（客户端解包）sha256 双冻结
  （`provenance-pins.tsv` G2 + 校验器；外部源缺失时校验器 SKIP 告警）。
- **G3 `quest_client_kill_targets.tsv`（已补通道）**：声明生成器只写测试夹具
  `src/test/resources/quest/iluma-norsvold-kill-target-contract.tsv`；生产表与该夹具
  **整文件逐字节相同**（sha256 均为 `250ff5ee…`）。处置 = 新增 fail-closed 提升脚本
  `regenerate_kill_targets_production.py --check/--apply`（fixture → production，对照 pin 校验）。
- **G4 `quest_legacy_heal_rows.tsv`（已解除）**：16 行旧存档通道，手工维护（设计如此）。census 口径
  `test=0` 是「文件名直引」的局限：表头已声明与测试侧 `quest/retail-legacy-save-normalization.tsv`
  **互斥**（本表只收链通道覆盖不到的 armour 方向），测试侧链条由 `p0c11_build_non_ir_registry.py`
  → 统一登记表 → `RetailNonIrAxisGateTest` 常设门守护。处置 = 书面口径 + **机检互斥不变量**（§7）。

## 4. 对"是不是和真端一样驱动"的回答

1. **状态机语义**（何时推进/完成/发奖）：来自 9 张真端表（4983 行）或保留 XML（1241 行）；
   TSV 不含状态机，不覆盖真端语义。
2. **客户端可见合同**（页面 id、按钮、行数、交付对象）：真端服务端表没有这些列，
   由客户端证据落库（8 张 client-registry + 2 张 name-index 的客户端侧）；
   这正是"以真端和客户端为主"的落地形态，不是"多余"。
3. **唯一显式"补真端表没有的"是 `dialog_exits`**：它补的是对话续页路由，合法性完全落在
   客户端证据；P2 缩表时应把"每行有客户端出口证据"作为判据之一。
4. **`quest_legacy_heal_rows` 是非真端通道**（旧存档兼容）：按既定规则保留，但不应被当作
   真端语义证据。
5. 若目标是"运行时直接读解包文件、取消 TSV 中间层"，那是**架构变更**（失去 fail-closed
   生成器 + 冻结快照 + 门禁对拍），需单独立项评估；本审计不支持"TSV 让任务偏离真端"的说法。

## 5. 建议动作（按优先级）

1. ~~**G1–G3**~~：**已完成**——G1/G2 内容与来源 sha256 冻结（校验器）；G3 补 fail-closed 提升通道。
2. ~~**G4 门禁说明**~~：**已完成**——书面口径 + 机检互斥不变量（校验器）。
3. **P2 立项**：`2026-09-28-p2-dialog-exits-shrink-charter.zh-CN.md`（P2a 只读普查 v2 → P2b 缩表）。
4. 不新增任何 TSV（manifest 政策：页码类只退役不新增）。

## 7. 冻结/通道产物与验证输出（2026-09-28）

- `provenance-pins.tsv`（G1–G4 四行：内容 sha256 / 源 sha256 / 再生成通道 / 不变量）。
- `check_provenance_pins.py`：`PROVENANCE_PINS checks=8 skipped=0 failed=0` → `PROVENANCE_PINS_OK`。
- `regenerate_kill_targets_production.py`：`G3_CHECK_OK (fixture == production, pinned sha match)`。
- P2 立项书：`../2026-09-28-p2-dialog-exits-shrink-charter.zh-CN.md`。

## 6. 纪律回执

只读取证：未改任何生产文件、未跑 Maven、未动红集；产物落
`.agents/summary/quest-native-dispatch/phase3-provenance/`；未 commit/push。

# P4 立项（只读评估）：投影层退场——8 张 retail-table/server-registry TSV 的构建期化

> 授权：用户「继续」（承接「辅助表是必要的吗」的结论）。
> 性质：**只读评估**；不改生产数据/代码、不跑 Maven、不 commit/push。
> 上游：Phase 3 权威血缘审计（`phase3-provenance/2026-09-28-authority-provenance-audit.zh-CN.md`）
> + P2a/P2b `dialog_exits` 缩表台账。

## 0. 一句话结论

这 8 张表不是“行为冗余”，而是**三种不同东西混在同一目录**：
① 仓内可重算的静态投影；② 外部客户端解包数据的唯一仓内快照；③ 编译器 IR。
因此 P4 的前置不是“再删 8 张”，而是先决定 **源能否在仓内重放**。
当前唯一可立即直接消除的低风险对象是 `quest_use_item_npcs.tsv`（纯 `npc_template_*.xml` 派生，
且 `QuestInteractionObjectValidator`/`QuestEngine` 已有 `aiNameByTemplate` 同源查询通道）；
4 张 monster 系表的生成器依赖外部 `quest_monster.csv`（仓内没有完整等价源），
`quest_client_talk_chain_steps.tsv` 则是 XML 退役后的编译器 IR，**不应作为首批退场对象**。

## 1. 范围与非目标

**范围（8 张）**：Phase 3 裁定为 `retail-table`（7 张）+ `server-registry`（1 张）的投影/注册表：

`quest_client_hunt_progress_rows`、`quest_client_hunt_stages`、`quest_client_kill_targets`、
`quest_client_kill_targets_stages`、`quest_client_talk_chain_steps`、
`quest_client_talk_collect_chain_pages`、`quest_enterarea_zone_resolution`、`quest_use_item_npcs`。

**非目标**：
- 客户端合同类（`dialog_exits`、`handin_pages/_exceptions`、`reward_npcs`、`summary_rows`、
  `talk_chain_pages`、`use_item_report`）——真端表没有对应列，按 Phase 3 结论保留；
- name-index（`name_string_ids`、`retail-quest-ai-name-groups`）；
- contract（`client_dialog_contract`、`movie_continuation_exceptions`）；
- heal（`quest_legacy_heal_rows`）与 retention（`retail-xml-retention`）审计账。

## 2. 候选矩阵（只读评估，2026-09-28）

| 表 | role | 权威/源 | 生成器状态 | 当前加载点 | P4 模式 | 优先级 |
|---|---|---|---|---|---|---|
| `quest_use_item_npcs.tsv` | server-registry | 仓内 `src/main/resources/.../npcs/npc_template_*.xml` 的 `ai="quest_use_item"` | `build_quest_use_item_npcs.py`（OK，但写回 `src/main/resources`） | `RetailQuestUseItemNpcs` + `RetailQuestDriver`；`QuestInteractionObjectValidator` 已有 `aiNameByTemplate`（`DataManager.NPC_DATA` → `NpcTemplate.getAi()`） | **M3 直接源（优先）**：把 registry 换成 `IntPredicate` / AI 查询函数，整表退场；M1 构建期生成为 fallback | **P0 试点** |
| `quest_client_kill_targets.tsv` | retail-table | 生产表与 `src/test/resources/quest/iluma-norsvold-kill-target-contract.tsv` 逐字节相同；声明生成器只写测试夹具 | `regenerate_kill_targets_production.py --check/--apply`（G3 通道已补） | `RetailClientKillTargets` + `RetailQuestDriver` | **M4 单源化**：先消掉重复夹具，再决定生产表是否构建期化 | **P1 快赢** |
| `quest_client_talk_collect_chain_pages.tsv` | retail-table（混合客户端页梯） | 仓内 `data_driven_quest.xml` + `docs/quest/client-dialog-mapping/*.csv`；生成器另读外部 `data_unpacked/Dialogs` 做过场交叉核验 | `build_quest_client_talk_collect_chain_pages.py`（OK） | `RetailClientTalkCollectChainPages` + `RetailQuestDriver` | **M2 外部源冻结/替换**：确认过场信息能否用仓内 CSV 替代外部 HTML | **P2** |
| `quest_enterarea_zone_resolution.tsv` | retail-table | `.agents/.../p0c41-sensory-area-scan.tsv` + `zones_quest.xml`；注册脚本同时改写 zone XML | `p0c42_register_sensory_zones.py` / `p0c48_register_pvp_sensory_zones.py`（注册式写入，另写 `target/classes`） | `RetailEnterAreaZoneResolution` + `RetailQuestDriver` | **M3 源声明 + 生成分离**：不要再让构建脚本直接改 `zones_quest.xml` | **P2** |
| `quest_client_kill_targets_stages.tsv` | retail-table | 外部 `Quest_unpacked/quest_monster.csv` + 仓内 `npc_template_*.xml` | `generate_stage_kill_targets.py`（依赖绝对外部路径） | `RetailClientKillTargets` + `RetailQuestDriver` | **M2 先冻结外部源**，再构建期生成 | **P3** |
| `quest_client_hunt_progress_rows.tsv` | retail-table | 外部 `Quest_unpacked/quest_monster.csv`（simpleQuest 行） | `build_quest_hunt_progress_rows_tsv.py`（依赖绝对外部路径） | `RetailClientHuntProgressRows` + `RetailQuestDriver` | 同上；注意仓内 `client-monster-progress-contracts.csv` 只有 853 行，覆盖不了当前 1266 行/965 quest 的全量 | **P3** |
| `quest_client_hunt_stages.tsv` | retail-table | 外部 `Quest_unpacked/quest_monster.csv` + 仓内 `npc_template_*.xml` | `p3_client_hunt_stages.py`（依赖绝对外部路径） | `RetailClientHuntStages` + `RetailSimpleHuntDefinitionCompiler` | 同上；这是 SimpleSerialHunt 的串行阶段合同 | **P3** |
| `quest_client_talk_chain_steps.tsv` | retail-table / 编译器 IR | 真端 SimpleTalk 表 + 仓内客户端 CSV + 退役前 XML 转写（词汇 fail-closed） | `build_quest_client_talk_chain_steps.py`（OK） | `RetailClientTalkChainSteps`（编译器核心输入） | **M5 保留为生成 IR**；不在首批退场，除非先设计替代 IR 源 | **P4/最后** |

## 3. 外部源审计（决定 P4 可行性的关键）

| 源 | 位置 | 规模/哈希 | 仓内状态 | 结论 |
|---|---|---|---|---|
| `quest_monster.csv` | `/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest_monster.csv` | 8499 行 / 2.1MB / sha256 `aaa8da03acfb…` | **不在仓** | 4 张 monster 系表的完整源；不冻结前不能做“离线可重放”的构建期生成 |
| `data_unpacked/Dialogs` | `/Users/mc/PycharmProjects/unpak/data_unpacked/Dialogs` | 21982 文件 / 116MB | **不在仓** | `talk_collect_chain_pages` 生成器的过场交叉核验源；需确认仓内 `quest-order-audit.csv` 是否已足够替代 |
| `client-monster-progress-contracts.csv` | `docs/quest/client-dialog-mapping/` | 853 行 / sha256 `9cab3c82…` | **在仓** | 只覆盖 853 个简单契约，小于当前 `hunt_progress_rows` 的 965 quest；不能单独作为全量源 |

## 4. 迁移模式

- **M1 仓内源构建期生成**：生成器只读仓内文件，输出到 `target/generated-resources`；
  `src/main/resources` 删除原表，manifest 删行、`EXPECTED_TSV_COUNT` 下调；loader 仍从 classpath 读，
  零 Java 行为变化。P0 试点 = `quest_use_item_npcs.tsv`。
- **M2 外部源冻结 + 构建期生成**：先把外部客户端源（或其最小必要投影）入仓并钉 sha256，
  生成器改为仓内相对路径；然后按 M1 处理。适用于 4 张 monster 系表。
- **M3 直接读源/注册分离**：编译器直接读真端源或 zone XML，或把“注册脚本”与“生成脚本”拆开；
  删除的是运行时中间层，风险高于 M1/M2。
- **M4 单源化**：只合并重复快照（生产表 ↔ 测试夹具），不一定减少 manifest 计数；
  `quest_client_kill_targets.tsv` 是当前唯一确定对象。
- **M5 生成 IR 保留**：`quest_client_talk_chain_steps.tsv` 是 XML 退役后编译器实际消费的 IR；
  当前不把它当“可删投影”，先保留并在后续 IR 设计立项中处理。

## 5. 构建管线现实检查

- 当前 `pom.xml` 没有 `exec-maven-plugin` / `build-helper-maven-plugin` / 自定义 `generate-resources`；
  资源根仍是默认 `src/main/resources`。M1/M2 需要新增一个构建期生成绑定或 Java 生成器；
  **P4b 的 M3 直接源路径不需要新增任何 Maven 插件**（复用已加载的 NPC 模板索引）。
- `RetailTsvManifestGateTest` 只冻结
  `src/main/resources/aion/data/static_data/quest_retail/*.tsv` 与
  `src/main/resources/aion/definitions/quest_dialog/*.tsv`。把表移到 `target/generated-resources`
  后，必须同步删 manifest 行、下调 `EXPECTED_TSV_COUNT`，否则门禁会双向红。
- 现有生成器位于 `.agents/summary/scriptdll-quest-driver/`，是分析/证据工具；若升级为生产构建步骤，
  需要决定放 `scripts/`（正式数据生成）还是移植到 Java/Maven 插件。不能把个人绝对路径带进构建。
- 生成产物仍需 fail-closed：生成器必须对源缺失/格式漂移中止，而不是静默输出空表。

## 6. 分批建议

1. **P4a（本文件）**：只读评估，确定源/构建管线/回退边界。
2. **P4b 试点（已执行，见 §10）**：`quest_use_item_npcs.tsv` 直接源消除（M3，优先），
   目标：manifest 21→20；把 `RetailQuestUseItemNpcs` 的布尔注册换成
   `DataManager.NPC_DATA.getNpcTemplate(id).getAi().equals("quest_use_item")` 的同源查询；
   DD/链指纹不变；T1/T3 红集恒等；无需新增 Maven 生成插件。若同源查询触及启动顺序，
   fallback = M1 构建期生成。
3. **P4c 快赢**：`quest_client_kill_targets.tsv` 单源化（M4），
   消除 `src/test/resources/quest/iluma-norsvold-kill-target-contract.tsv` 重复快照；
   如果生产表继续保留，则 manifest 计数不变，只减少一份拷贝。
4. **P4d monster 系批**：先决定是否把 2.1MB `quest_monster.csv` 或其最小投影入仓；
   否则 4 张表继续按“仓内快照”处理，不做构建期生成。
5. **P4e 混合/注册批**：`talk_collect_chain_pages` + `enterarea_zone_resolution`；
   先消外部 Dialogs 依赖与 zone XML 双写。
6. **P4f IR 批**：`talk_chain_steps` 的替代 IR 设计，单独立项，不与前五批混合。

## 7. DoD（每张表迁移时）

- 该表退出冻结 manifest（M1–M3）或重复快照消除（M4），计数与清单同片更新；
- DD（1218 数据行）/链（285 数据行）指纹逐字节相同；
- T1 红集 `3b92439da8…`、T3 基线口径红集 `5e3acdb9…` 与基线恒等；聚焦门绿；
- 离线 Maven 构建不依赖 `/Users/mc/...` 绝对路径；源缺失/漂移必须 fail-closed；
- 客户端合同表不动；不新增 TSV；不启停服务；不创建 worktree。

## 8. 需要用户决定的三件事

1. 是否允许把外部客户端源（至少 `quest_monster.csv` 或其最小必要投影）入仓并钉 sha256？
2. 构建期生成接受 Python 工具链，还是必须移植到 Java/Maven 插件？
3. 目标是减少 **源码树 TSV 数量**（M1/M2），还是减少 **运行时 classpath 文件数**（M3）？

## 9. 纪律回执

只读评估：未改生产数据/代码、未跑 Maven、未动红集；产物仅本立项书与 P4 指针。
`quest` 分支 P2a/P2b 两条提交仍在本地（未 push）。

## 10. P4b 执行回执（2026-09-28 追加）

P4b 试点（`quest_use_item_npcs.tsv`，M3 直接源）**已执行**：

- 读取者 `RetailQuestUseItemNpcs` 改为内存视图（`fromIds`）；源索引 `RetailNpcNameIndex`
  在同批 8 个 `npc_template_*.xml` 流中收集 `ai="quest_use_item"` id 集；驱动与门禁夹具同轴替换。
- 静态复算 **804 = 804（missing 0 / extra 0）**；快照
  `retired-tsv/quest_use_item_npcs.tsv.retired-20260928`，sha256 `2f7b4d22…`。
- DD（1218 数据行）/链（285 数据行）指纹与 P2b 基线逐字节恒等；聚焦门 28 例 1 红（在册 20035）；
  T1 `3b92439da8…`、T3 基线口径 `5e3acdb9…` 红集 sha256 恒等。
- manifest 21→20，`EXPECTED_TSV_COUNT` = 20；未新增 Maven 插件；生成器只读保留 + 停写登记。
- 执行台账：`2026-09-28-p4b-use-item-npcs-retirement.zh-CN.md`；结论固化为记忆库 QE-099。
- §8 三问对 P4b 不构成阻塞（不需外部源入仓、不需构建插件、目标是减少运行时 classpath 文件数）；
  P4c（kill_targets 单源化）与 P4d（monster 系）仍待用户决定。

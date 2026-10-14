# REWARD 进入边刷新合同守卫（typed lane 全库扫除 + ClassChangeService 三件套收口）——客户端验收记录

## 待验收批次（修复提交）

| 提交 | 说明 |
|---|---|
| `0eb4ee9f4` | `fix(quest): guard the REWARD-edge refresh contract and sweep typed-lane XML`——`QuestDefinitionCompiler` 新增「进入 REWARD 的边」刷新同步 fail-closed 守卫（`REWARD_QUEST_REFRESH_REQUIRED`）；20 个生产 typed XML 共 48 处 `PACKET_ONLY` → `LEVEL_AND_VISIBILITY_REFRESH`（另 3 个定义夹具补齐同轴同步）；`ClassChangeService.completeQuest` 补 action3 + `SM_QUEST_COMPLETED_LIST` 三件套与可见性刷新；新建 `ClassChangeServiceCompletionPacketTest` 锁定两条发包序列 |

## 仓库状态

- 记录时 HEAD：`0eb4ee9f4`（工作区干净；本验收记录与 Playbook 随提交 B `docs(quest)` 落盘）。
- 分支：`quest`。

## 客户端与数据出处

- 客户端行为结论沿用前批（REA 逆向，只读 `~/IdeaProjects/aion-ref/`）：0x7f 区域清单只作头顶标记重算触发器，顺序必须「状态包 → 刷新」。
- 任务数据权威：`src/main/resources/aion/data/static_data/quest/` 下 typed 定义与 retail 表。

## 实机上下文

- 服务器：用户自管实机服务端；精灵（天族）角色。
- 抽样方式：三场景抽样（用户选定）——① typed REWARD 边（本批扫除目标）；② ClassChange 三件套（配置门后）；③ 已退役 native lane 边界。

## 预期 vs 实际

| 场景 | 样本 | 预期 | 实际 |
|---|---|---|---|
| ① typed REWARD 边 | 9548（天族 10 级活动收集，NPC 798301，`started → reward` 对话边带刷新同步） | `//quest start 9548` 后按步骤推进；进入 REWARD 时交付 NPC 头顶标记即现；完成后无「任务失败」弹窗 | 用户确认「场景 1 没问题」✓ |
| ② ClassChange 三件套 | 1006/2008（MUSE 转职；`gameserver.simple.secondclass.enable` 后） | 转职指引完成时 action2 → action3 → 已完成列表三件套成对 | 跳过实机（用户裁定「2不做了」）；仅测试层锁定 |
| ③ 已退役 native lane 边界 | 1107（`SimpleUseItem` native 任务；物品 182200501「Broken Axe Handle」→ NPC 203075 Namus 交付） | 使用物品进 START → 对话进 REWARD 标记即现 → 领奖完成三件套 | 用户确认「1107 正常完成」✓ |

## 实机复核证据

- 场景 ①（9548）：typed 对话边进 REWARD 与 80790 批次同轴（`LEVEL_AND_VISIBILITY_REFRESH` 边合同）；用户实机通关无异常。
- 场景 ③（1107）：覆盖「退役 XML → native handler（SimpleUseItem 家族）」边界——该 lane 不经过编译器守卫，其夹具 `simpleuseitem-1107.xml` 已补同轴同步；用户按 GM 流程（`//add 182200501` → 使用 → 与 203075 对话 → 领奖）实机确认正常完成。
- 场景 ②：未实机（默认 false 配置门 + 用户裁定跳过）；包序列由 `ClassChangeServiceCompletionPacketTest`（insert-then-flip 与 existing-row 双序列）锁定。

## 门禁（记录时）

- IDEA MCP（2026-10-14，用户授权）聚焦集 23 类：**22 类全绿**——含 `QuestDefinitionCompilerTest`（守卫正反例）、`QuestProductionStartupGateTest`（全量生产目录过守卫）、`ClassChangeServiceCompletionPacketTest`、`QuestExecutionCoordinatorTest`（扫除后探针补齐）及其余受扫除影响的定义/运行时测试。
- **存量红灯 2 个（非本批引入，已核对）**：`RemainingCapabilityDefinitionTest`（1561）、`ArchivesRewardStepLadderContractTest`（16800）——两任务已由 `4ede058c0` 退役、native handler 接管；`overlayProduction` 不再补条目，报 `missing production quest definition`（与守卫报错 `REWARD_QUEST_REFRESH_REQUIRED` 形态不同，可证非本批守卫触发）。
- 未运行：全量 `mvn` 构建/测试（授权红线，未获授权）。

## 验收状态与残留风险

- 状态：**CLIENT_ACCEPTED**（2026-10-14；场景 ①③ 实机通过，场景 ② 用户裁定跳过）。
- 残留风险 / 未覆盖：
  1. 场景 ②（ClassChange 三件套）未实机——配置门后路径仅测试锁定。
  2. 守卫只约束编译器可见的 typed 目录；retail overlay 后置目录与 native lane（如 1107）不在守卫面，靠夹具/测试层维持同轴。
  3. 1561/16800 两个存量红灯对应测试类的处置（删/重锚）未获批，保持现状。

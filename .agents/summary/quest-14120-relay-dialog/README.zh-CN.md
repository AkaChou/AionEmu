# 任务 14120 中继对话面修复（730020 · 点击即跳步 / 任务书步骤显示为空）

日期：2026-10-07（实机玩家 Kk / 任务 14120 / NPC 730020）
状态：**已验收**（代码与测试完成 + 2026-10-07 用户实机验证成功）

## 现象（用户实机报告）

- 与 NPC 730020（talk_npc1 = Tree_Move_Demro）对话不正确：**点击 NPC 直接推进任务步骤**，对话页全缺。
- **任务书步骤显示为空**。
- QUEST-TRACE 证据：接取后 `SM_QUEST_ACTION 任务=14120 状态=3 步数=0`；与 730020 交互后
  `SM_QUEST_ACTION 任务=14120 状态=3 步数=65536`（无对应 CM_DIALOG_SELECT 页面链）。

## 根因（两层）

1. **中继对话面缺失**：`SimpleCollectItemHandler.talkChainStep` 对任何对话动作（31/26/10000…）
   都立即推进 vars，且只发 `SM_QUEST_ACTION`——不下发该步页、不处理子页动作、不按动作区分。
   与 SimpleTalk/SimpleItemPlay 已验收形状（QE-141：31/26/-1 = 该步页、SETPRO{K} 才推进、
   推进 after-commit = 关窗 0x5d8 零发页、子页动作原样回发）不一致 ⇒ 点击 NPC 即跳步。
2. **步号私有编码污染客户端步骤轴**：中继步写在 vars bit16..17（步号 1 → 65536）。客户端
   任务书按打包整数匹配 steps 行，65536 匹配不到任何步骤 ⇒ 步骤显示为空。

## 权威证据

- 退役 XML 14120（`git show 4ede058c0^:src/main/resources/aion/data/static_data/quest_definition/quests/14120.xml`）：
  - `started → started`：730020 + QUEST_SELECT(31) → `SHOW_QUEST_PAGE SELECT2`（无 var 写）；
  - `started → started`：730020 + SELECT2_1(1353) → `SHOW_QUEST_PAGE SELECT2_1`；
  - `started → v1`：730020 + SETPRO1(10000) → v1（**var0=1**）+ sync PACKET_ONLY + close-dialog。
- 客户端对话合同（`client_dialog_contract.tsv`）：14120 声明 1352/select2、1353/select2_1；
  采集族有 talk 链的行（9620 三步、14150/14120/9655/9656）页 1352/1693/2034 均已声明。
- 同轴参照：SimpleItemPlay 家族注释（真端交付节点 slot 3 #K 与 talk 族 cabb10 同轴）；
  SimpleTalk/SimpleItemPlay 中继实现与族门（QE-141 验收面）。
- 数据行：`Quest_SimpleCollectItem.xml` 有 talk_npc 列的行 = 9620(3 步)、9655(1)、9656(2)、14120(1)、14150(1)。

## 修复

- `SimpleCollectItemHandler`：
  - 新 `onRelayDialog`（与 SimpleTalk/SimpleItemPlay 同形）：31/26/-1 → 该步页
    （pageForStep = 1352/1693/2034，带 questId；未轮到的步零响应）；SETPRO{K}（10000+K−1）→
    `var0 = K` + `SM_QUEST_ACTION` + 关窗；重复/乱序重放 → 关窗兜底；子页动作
    （`isSelectionSubPage` × 契约声明）原样回发。
  - `talkStep` 改读 var0（兼容读旧 bit16..17）；写入一律 var0。
  - 新增 `onEnterWorld` 自愈：旧编码（bit16..17 非零且 var0=0）归一为 var0 并重发状态，
    修复旧存档的任务书步骤空白；`QuestEngine.onEnterWorld` 接线。
- 测试：
  - `SimpleCollectItemNativeFamilyGateTest` +2：`relayChainServesStepPageAndAdvancesOnSetpro`（14120：
    31→页 1352、1353 回发、10000→var0=1+关窗+可采集、重放零步进）、
    `threeStepRelayServesEachStepPageInOrder`（9620：页 1352/1693/2034 逐步、乱序零响应）。
  - `QuestInteractionObjectContractGateTest`：中继步合同改「26 打开步页（不推进）→ 10000 推进」形状。

## 验证

- IDEA MCP（2026-10-07）：`SimpleCollectItemNativeFamilyGateTest` 19/19（含 3 个新用例）、
  `QuestInteractionObjectContractGateTest` 2/2 全绿（exitCode=0）。
- 实机验收通过（2026-10-07 用户确认「实机验证成功」）：冷重启 + 进世界自愈（旧存档 65536→1）后，
  点 730020 → 页 1352 → 翻页 → 1353 → SETPRO1 → 关窗 + 步数=1、任务书步骤恢复；后续采集/交付/
  领奖整链可玩。验收记录：`.agents/summary/quest-acceptance/14120-2026-10-07-client-accepted.md`。

## 遗留（未修，同型风险）

- `SimpleUseItemHandler` 族 90 个 talk_npc 行仍用 bit16..17 私编 + 任意动作推进（同型缺陷，
  待逐行排查与实机裁定）。
- `SimpleCollectItem` 家族 9656（不可路由 TEST 行）的 `give_item1`/`remove_item2` 步物品未消费
  （无实机面，先记录）。

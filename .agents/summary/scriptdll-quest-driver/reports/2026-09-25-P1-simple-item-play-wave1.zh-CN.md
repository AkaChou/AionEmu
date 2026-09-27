# P1 wave-1：SimpleItemPlay 族合成器 + 全族门禁（6 行 IR 等价，9 行稳定码登记）

- 日期：2026-09-25
- 切片：P1 SimpleItemPlay wave-1（族合成器 + 门禁；**无退役、无生产接线**）
- 权威口径：真端模板表 `Quest_SimpleItemPlay.xml` 为形状权威（不可推翻教条 ①）；等价对拍以退役前
  生产 XML 为 IR 逐字基准（教条 ③：每个差异要么"真端对、XML 错"，要么"真端缺口→保留 XML"）。

## 1. 交付

1. `src/main/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleItemPlayTable.java`
   ——真端表只读视图（DTD 内部子集容忍解析，43 行；13 字段原始行 record，
   解析延迟到合成器：`acquired_npc_name / talk_npc1 / talk_npc2 / give_item / give_item1 /
   give_item2 / remove_item1 / remove_item2 / use_item_name / reward_npc_name / con_quest / cutsceneid1`）。
2. `src/main/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleItemPlayDefinitionCompiler.java`
   ——规范形合成器（13704 展开口径）：
   - npc-start 规范展开（QUEST_SELECT→SELECT1 / ASK_QUEST_ACCEPT / ACCEPT_1+ACCEPT_SIMPLE 带
     `StartEligible` + 接取动作 `GiveItem(演出道具, 数量)` / REFUSE×3 / FINISH_DIALOG×2 源）；
   - 物品演出推进：`UseItem(itemId,0)` 在 started（var0=0）→ REWARD，动作 `SetVariable(var0,1)`，
     after-commit `LEVEL_AND_VISIBILITY_REFRESH`；
   - 交付：reward NPC `USE_OBJECT`→SELECT5；`SELECT_QUEST_REWARD`(1009) `RemoveItem(演出道具)` +
     开奖励窗；
   - npc-complete：确认段 8..23 全段固定奖励 + `CompleteQuest(0)`，after =
     `[RefreshPlayerStats, SyncQuestState COMPLETION, ShowQuestSelectionDialog(10)]`；
   - **表值符号带数量后缀**（如 `ITEM_QUEST_13704A 1`）：解析取空白分隔首段为符号，
     尾段数量如实兑现到 GiveItem/RemoveItem（本表全量后缀均为 1）；
   - 稳定拒绝码（precheck 按序首中即拒）：`RETAIL_METADATA_UNRESOLVED` / `RETAIL_ACQUIRE_NPC_*` /
     `RETAIL_REWARD_NPC_*` / `RETAIL_ADVANCE_UNEXPRESSED` / `RETAIL_USE_ITEM_UNRESOLVED` /
     `RETAIL_TALK_CHAIN` / `RETAIL_CON_QUEST` / `RETAIL_CUTSCENE` / `RETAIL_ITEM_SWAP` /
     `COMPILATION_FAILED`。
3. `src/test/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleItemPlayGateTest.java`
   ——全族门禁（4 不变量，见 §2），排查开关 `-Dretail.itemPlay.equivOut=<path>`。
4. T1 固定清单扩至 **16 类**（`affected_quest_tests.py` += `RetailSimpleItemPlayGateTest`）。
5. 逐行证据 `.agents/summary/scriptdll-quest-driver/p1-itemplay-family-gate.tsv`
   （43 行：6 EQUIVALENT + 9 稳定码 + 28 NO_PRODUCTION）。

## 2. 门禁不变量（RetailSimpleItemPlayGateTest，1 例 4 断言）

| # | 不变量 | 内容 |
|---|---|---|
| 0 | 形状快照 + 族闭合 | 真端表恰 43 行；与本服生产相交（源树存在生产 XML，不信 target/classes 残留）恰 **15 行**，每行恰落"等价/拒绝"两桶之一 |
| 1 | wave-1 IR 等价 | 13704/13708/19048/23704/23708/29048 合成定义与生产 XML 在归一化 IR 层逐字等价（复用 `RetailSimpleHuntEquivalenceGateTest.irEquivalenceProblem` 口径），指纹留证 |
| 2 | 稳定码白名单 | 其余 9 行逐一命中登记码：`RETAIL_ACQUIRE_NPC_UNRESOLVED` {18828, 28828, 50048} + `RETAIL_ACQUIRE_NPC_SENTINEL` {39713, 49713}（`_faction_` 势力哨兵）+ `RETAIL_TALK_CHAIN` {18213, 28213} + `RETAIL_ADVANCE_UNEXPRESSED` {80255, 80256}（行无 use_item_name，推进事件不在族表） |
| 3 | 新增/消失必先改登记 | 白名单为精确匹配（多码/少码/换码都失败），28 行真端独有开发/测试任务（9623 等，无生产 XML）显式不参与 |

## 3. 对拍与验收

- **等价对拍**：6 行 wave-1 全部 EQUIVALENT（首轮等价，仅修复"符号+数量后缀"解析后即达成）；
  指纹（SHA-256，`p1-itemplay-family-gate.tsv`）：13704 `cca08e25…`、13708 `47a38027…`、
  19048 `30274765…`、23704 `8e29c70e…`、23708 `acbac73d…`、29048 `ef020953…`。
- **族门禁**：1 例绿。
- **T1 全量**：`run_quest_gates.sh T1` → **59 例 / 0F / 0E**（`gates/T1-015406.log`，361s）。
- **教条 ⑥ 复核**：9 行拒绝码全部来自真端表列形状本身（名字/哨兵/链列/缺 use_item 列），
  未以 XML 或旧 handler 为准补形状；wave-1 6 行以真端表列独立合成后与 XML 等价，即"真端表可完整表达"的证明。

## 4. 未验证 / 阻塞 / 下一步

- **未验证（留 wave-2 后统一做）**：运行时行为（接取发物→使用演出→交付领奖全链路）、
  客户端抽检（`quest_summary` 行 / `SECTION_n` 计数）。
- **阻塞**：`RetailQuestDriver` 与 catalog/manifest 正由并发 DataDriven 批占用（no-touch 清单），
  wave-2 接线与退役必须排在其收敛后。
- **下一步（P1 wave-2）**：`RetailQuestDriver` ItemPlay 分支（wave-1 6 行 owner 转 `RETAIL_TABLE`）→
  6 行 XML 退役 + catalog/manifest 同步 → `verify_retirement` → T3 clean 副本。
  9 行暂缓行保留 XML，稳定码与 `retail-xml-retention.tsv` 对齐登记在 wave-2 一并落表。
- 候选独立切片：RETAIL_TALK_CHAIN 322 行链式合成（SimpleTalk）、M3-d 10 行 EQUIVALENT 归零。

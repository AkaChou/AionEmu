# 2026-09-17 任务掉落收集步数不可达死锁专项排查与根治总结

## 1. 背景与排查

在自主排查任务系统更深层潜在死锁缺陷时，我们结合真端客户端解包（`Quest_unpacked/quest.xml` 中的 `collect_progress`）以及服务端掉落判定逻辑：
```java
// QuestService.java
if (drop.collectingStep() != 0) {
    if (drop.collectingStep() != qs.getQuestVarById(0)) {
        return false;
    }
}
```
展开了全服任务 `<drops>` 配置数学排查。

排查发现：
当任务 XML 的 `<drops>` 中 `collecting-step` 被配置为非 0 值时，若该数值在当前任务的所有节点（`nodes`）声明的 `var0` 取值集合中根本不存在，玩家在游戏中的任务变量 `var0` 无论如何都不可能匹配该步数。在击杀目标怪或采集物体时，`QuestService.isQuestDrop` 恒定返回 `false`，导致**任务道具掉落率物理归零（0%），玩家无论击杀多少次都无法获得任何任务道具，任务彻底卡死**。

---

## 2. 致命掉落死锁清单与根治修复

通过全库 6,222 个任务自动化扫描，精确锁定了 4 个存在完全不可达掉落步数的致命任务，并完成全局修复：

1. **任务 2372 (Immortal Love / 永恒的爱)**：
   - **所属**：魔族 / 重要任务；
   - **病灶**：4 条掉落（NPC 212463, 212464 掉落 182204187，NPC 212452, 212453 掉落 182204188）误配置为 `collecting-step="2"`；但任务节点仅有 `started` (var0=0) 与 `reward` (var0=1)，玩家打怪时 var0 恒为 0，因为 `2 != 0`，掉落率直接为 0%，永远无法掉落；
   - **修复**：将 4 条掉落全部修正为 `collecting-step="0"`（允许在 started 进行阶段正常掉落）。

2. **任务 4907 ([Spy/Group] Lepharists in Elysea / 潜入极乐世界的雷帕尔团)**：
   - **所属**：魔族 / 间谍组队任务；
   - **病灶**：4 条掉落（NPC 212191 掉落 182207079，NPC 212211 掉落 182207080，NPC 214527, 214528 掉落 182207081）误配置为 `collecting-step="1"`；但任务节点仅有 `started` (var0=0) 与 `reward` (var0=1，领奖态)，玩家击杀怪物时处于 started (var0=0)，因为 `1 != 0`，掉落率直接为 0%，永远无法获得任务道具；
   - **修复**：将 4 条掉落全部修正为 `collecting-step="0"`。

3. **任务 24202 (Let Me In, Let Me In / 让我进去)**：
   - **所属**：魔族 / 普通任务；
   - **病灶**：2 条掉落（NPC 214437, 214438 掉落 182215465）误配置为 `collecting-step="2"`；但任务节点只有 `started` (var0=0) 与 `reward` (var0=1)，因为 `2 != 0`，掉落率直接为 0%；
   - **修复**：将 2 条掉落全部修正为 `collecting-step="0"`。

4. **任务 24203 (Secret of Brusthonin Contamination / 布鲁斯特豪宁污染的秘密)**：
   - **所属**：魔族 / 普通任务；
   - **病灶**：1 条掉落（NPC 213360 掉落 182215466）误配置为 `collecting-step="2"`；但任务节点只有 `started` (var0=0) 与 `reward` (var0=1)，因为 `2 != 0`，掉落率直接为 0%；
   - **修复**：将该掉落修正为 `collecting-step="0"`。

---

## 3. 全局自动化回归门禁

在 `src/test/java/com/aionemu/gameserver/questEngine/definition/QuestMovieAndDialogLoopRegressionTest.java` 中新增两大自动化门禁：
1. **`fatalImpossibleDropStepsAreEliminated()`**：
   - 遍历全服所有生产可执行任务的所有掉落声明；
   - 强制断言：任何非 0 的 `collectingStep()` 必须存在于对应任务 `nodes` 声明的有效 `var0` 取值集合中；
   - 全库扫描结果：**0 违规，不可达步数彻底归零**！
2. **`quests2372And4907And24202And24203DropsStepCorrected()`**：
   - 专项断言 2372, 4907, 24202, 24203 掉落步数全部修正为可达的 0。

---

## 4. 架构沉淀

新增架构模式 `[QE-025]` 二十三、任务掉落收集步数有效性与死锁防护 (QUEST_DROP_COLLECTING_STEP_VALIDITY) 至：
- `.agents/memory-bank/patterns/quest-engine.md`
- `.agents/memory-bank/systemPatterns.md`
- `.agents/memory-bank/symptom-index.md`

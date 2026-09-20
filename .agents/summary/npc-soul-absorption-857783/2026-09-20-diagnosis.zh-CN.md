# NPC 857783 灵魂吸收长期控制诊断（2026-09-20）

## 现象

- NPC 857783 对玩家施加客户端名为“灵魂吸收”的技能后，玩家无法施法、无法移动，只能普攻。

## 结论

根因不是客户端/静态技能数据损坏，而是 `RetailPatternAI2` 继承了 `AggressiveNpcAI2` 的通用随机技能选择路径：

1. NPC 857783 的 retail pattern `IDEternity_Q_Sado_Fi_02` 只显式使用技能索引 `0,1,2,4,5`。
2. 该 NPC 的紧凑技能组 `NS_AC21D85D9F10FA7F` 共有 9 个技能，索引 `6` 是 `22868 IDEternity_01_Fighter_Drain_01`，即“灵魂吸收”。
3. `RetailPatternAI2` 未覆盖 `chooseAttackIntention()`，战斗 tick 会走 `AggressiveNpcAI2.chooseAttackIntention() -> SkillAttackManager.chooseNextSkill() -> NpcSkillList.getRandomSkill()`，从整组技能随机施放，包括 pattern 从未引用的 `22868`。
4. `22868` 的四个效果均为 24 小时：`SpellATK`、`Root`、`Silence`、`Bind`；`Silence` 禁止施法，`Root`/`Bind` 禁止移动，因此只剩普攻可用。

## 关键证据

| 证据 | 位置 |
|---|---|
| NPC 857783 绑定技能组和 AI | `src/main/resources/aion/definitions/compact/ai/npc-ai-parts/npc-ai_834041_885645.xml:5088` |
| 技能组包含 9 个技能，索引 6 为 22868 | `src/main/resources/aion/definitions/compact/skills/npc-skills.xml:7285` |
| pattern 只使用索引 0、1、2、4、5 | `src/main/resources/aion/definitions/compact/ai/npcaipatterns_ideternity_q_psh.xml:32` |
| 22868 的 Root/Silence/Bind 均为 86400000ms | `src/main/resources/aion/definitions/compact/skills/skill_templates_part_029.xml:6576` |
| RetailPatternAI2 继承 AggressiveNpcAI2 | `src/main/java/com/aionemu/gameserver/ai/RetailPatternAI2.java:88` |
| 通用攻击分支随机选择 NPC 技能 | `src/main/java/com/aionemu/gameserver/ai/AggressiveNpcAI2.java:7518` |
| 随机技能会遍历完整技能组 | `src/main/java/com/aionemu/gameserver/model/skill/NpcSkillList.java:99` |
| 效果时长读取第一个非零 duration2 | `src/main/java/com/aionemu/gameserver/skillengine/model/Effect.java:1329` |

## 真端脚本意图

- `IDEternity_01_Erath_Named` 等真端 Boss pattern 在指定 HP 阶段显式使用 `SKILLI_INDEX_6`，并同时 `spawn_on_target` 生成 `BIDEternity_01_Boss_Drain_Dispel`；该 dispel NPC 只带 `22870`，用来解除灵魂吸收。
- `IDEternity_Q_Sado_Fi_02` 没有索引 6，也没有生成 drain-dispel NPC。因此 857783 本不应拥有这条控制链。

## 立即解卡

- 用 GM 选中该角色后执行 `//dispel`，该命令调用 `EffectController.removeAllEffects()`。
- 不要只依赖重登：`PlayerEffectsDAO` 会保存剩余时间大于 28 秒的效果，24 小时灵魂吸收会被持久化并在重新登录时恢复。

## 修复实现

- 已在 `RetailPatternAI2` 中覆盖 `chooseAttackIntention()`：保留最高仇恨目标校验和切目标/结束战斗语义，但默认只返回 `SIMPLE_ATTACK`。
- 所有技能施放继续由 retail pattern 动作负责，包括 `use_skill` 的索引技能和 `SKILLI_ANY_SKILL`；禁止通用随机技能绕过 pattern。
- 已在 `RetailPatternAI2Test` 增加 `patternDrivenNpcsDoNotRandomlyCastUnscriptedSkills`：用 22868 构造一个通用随机技能会命中的技能列表，保证 pattern AI 仍返回 `SIMPLE_ATTACK`。

## 验证边界

- 本文件只记录静态源码、静态数据和客户端 `skills.pak` 内 `client_skills.xml` 的诊断证据。
- 已修改生产代码 `RetailPatternAI2.java` 并新增聚焦测试；`git diff --check` 通过。
- 未执行 Maven 构建或测试；未重启服务端。待授权后应执行：
  `mvn -B test -Dtest='RetailPatternAI2Test'`。

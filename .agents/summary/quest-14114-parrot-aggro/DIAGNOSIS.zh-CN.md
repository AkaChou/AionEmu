# 14114 鹦鹉变身被 NPC 攻击 —— 诊断笔记 / Diagnosis

日期：2026-10-07　任务：14114（+ 对称任务 24154）　技能：8197（Polymorph_Parrot）/ 267（Q_Polymorph_Lehpar）

状态：已结案 —— 裁定 B（保持真端，不补 `neutral_to_npc`），无代码/数据变更。

## 现象

14114 `SETPRO1` 施加 8197「카일리니 변신 / Polymorph_Parrot」后，玩家在沃特伦船难雷帕尔营地一带被 NPC 攻击。
用户预期：变身期间不应被攻击。

## 机制链（本服）

1. 任务 14114 在 Klitie（203183）处 `SETPRO1` → `definitions/quests/14114.xml:153-163` 应用 `skill-id=8197`；`SETPRO2` 时移除。
2. 8197 是 `polymorph` 效果：`compact/skills/skill_templates_part_010.xml:21410`。
3. 变身**不改部落**：`TransformEffect.applyTransform()` 只设 model/panel/item，不调 `TransformModel.setTribe()`（全仓唯一 setTribe 调用点是 `Creature.java:147` 的 NPC 出生模板路径）。
4. 因此玩家仍是 `TribeClass.PC`；营地 NPC 全是 `ai="aggressive"` 的 LEHPAR 怪：
   - 210073 LehparBoatswain_20_An（LV20）、210074 LehparCrew_19_An（LV19）、210075 LehparCaptain_21_An（LV21，任务掉落怪），tribe=LEHPAR。
5. 唯一能挡住仇恨的开关是 `neutral_to_npc="true"`：`PolymorphEffect.startEffect` → `setAdminNeutral(1)` → `ai2/handler/AggroEventHandler.java:47` 跳过仇恨。
   当前 8197 的 compact 模板**没有**该属性 → 玩家照常被仇恨。

## 证据：属性在静态数据迁移中丢失

- 迁移前（`git show 01f4ec0bb^:src/main/resources/aion/data/static_data/skills/skill_templates.xml`）：

  ```xml
  <polymorph model="210360" type="NONE" duration2="300000" effectid="175" e="1" basiclvl="100" neutral_to_npc="true"/>
  ```

  同类伪装技能 267（Q_Polymorph_Lehpar，用于 24154）同样带 `neutral_to_npc="true"`。
- 提交 01f4ec0bb（skills 家族切到 compact 形态，删 `static_data/skills/skill_templates.xml`，新增 37 个 compact 文件）之后，两个 polymorph 的该属性都不复存在。

## 与真端 5.8 的对拍（`<真端根>/Map/XML/skill_base.xml`）

对拍脚本：`map-neutral-flag.py`（本目录）。

- `neutral_to_npc` ⟺ 真端 `effectN_reserved14 == 1`：全量 **284 处一致 / 0 处不一致**。
- Deform 全表 123 行：只有 3 个技能 `reserved14=1`（21605 / 21920 / 22749），正是 compact 里 3 个 `neutral_to_npc="true"` 的行。
- **Polymorph 行从不出现 `reserved14=1`**（32 行该字段是武器名，如 `Dh_sword`）；8197 的 effect1 只有 reserved7=1 / reserved8=None / reserved9=Parrot_ex / reserved16=175。
- 结论：compact 数据对真端是忠实映射；**真端 5.8 技能表里 8197 没有中立位**。旧数据里的 `neutral_to_npc="true"` 来自更早版本（Aion-Unique 时代）的数据。

## 裁定（2026-10-07，用户）

**选 B —— 保持真端权威，不改数据**：不恢复 8197（及 267）的 `neutral_to_npc`；船难雷帕尔营地（怪 LV19-21、aggressive）按设计当作危险潜入区。结论已沉淀为记忆库卡片 `QE-156`（POLYMORPH_NEUTRAL_FLAG_RETAIL_RESERVED14）。

- A. 按旧版行为补回数据：给 8197（以及 267）的 polymorph 加 `neutral_to_npc="true"`，恢复"伪装期间不被仇恨"。**（未采纳）**
- B. 坚持"真端权威"：不改，视船难雷帕尔营地为危险潜入区（怪 LV19-21、aggressive）。**（已采纳）**

## 相关数据（供核查）

- 感知区：`zones/zones_quest.xml:28` LF1A_SENSORY_AREA_Q14114_210030000（球心 2096.18/612.73/104.52，r=10）。
- 偷听目标 NPC：`Parrot_Talking`（700029，ai=noaction），点内还有 206008（LF1a_SensoryArea_Q1023_SPG）。
- 客户端页契约：`definitions/quest_dialog/client_dialog_contract.tsv`（14114 行 20563-20572）。

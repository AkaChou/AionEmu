# 885101 深渊搜查兵「飞行被发现」诊断（2026-10-09）

## 现象
实机 Reshanta(400010000)：玩家飞行、与 885101（Ab1_Mission_Eresh_Ra_65_Ae）相距约 28m（3D≈30m），
每次移动都触发客户端音效；服务端 AI 日志同步打印
`Creature event CREATURE_MOVED: 100001`（AbstractAI.onCreatureEvent，AbstractAI.java:322-330；
100001 为玩家模板 ID，非对象 ID）。

## 结论：真端设计行为，非缺陷
885101 真端 AI pattern 为 `Eresukigal_Ranger_KJS`（npc-ai-parts/npc-ai_834041_885645.xml:10425），
是专职**反飞行哨兵**：对感知范围内的飞行玩家，无论距离，直接释放远程攻击技能。

### 证据链
1. **事件派发**：MovementNotifyTask.java:94-118（500ms 周期）向已知列表 NPC 广播
   CREATURE_MOVED（Reshanta 限流 200 个/次，无距离门控，范围=knownlist 视距 ~95m）。
2. **双路处理**：AggressiveNpcAI2.handleCreatureMoved →
   - CreatureEventHandler.onCreatureMoved → checkAggro（CreatureEventHandler.java:67-102）：
     885101 真端索敌 sensory_range=**8m**（+体型补偿 ≈9.8m），28m 不在范围内 ⇒ **不开怪、不喊话**；
   - RetailPatternAI2.handleCreatureMoved（RetailPatternAI2.java:938-946）→
     `runEvent("on_see_user_move", null, creature)`。
3. **真端 pattern 规则**（npcaipatterns_ab1_new_kjs.xml:83，Eresukigal_Ranger_KJS）：
   - `on_see_user` / `on_see_user_move` 条件均为 `<is_user_flying><user>USERI_SEEN</user></is_user_flying>`，
     **pattern 内无任何距离条件**；
   - 动作 = `use_skill(SKILLI_INDEX_2 → OBJI_SEEN)` + 战斗计时器 BTIMERI_10(1000ms)。
   - `is_user_flying` 实现：RetailPatternAI2.matches case 1590-1593 → `player.isFlying()`。
4. **技能索引**：885101 技能组 NS_76589DCA64854C53（npc-skills.xml:5028），
   SKILLI_INDEX_2 = **Ere_ATK_Ra_03 = 22695「날개 동결/冻结之翼」**
   （skill_templates_part_029.xml:1026）：PHYSICAL/ATTACK、instant、
   retail 标志 `f i="84" v="Air"`（空中目标限定）——专门打飞行目标。
   组内其余：20226 buff、22694 召唤、22696 范围箭、22697 眩晕箭、22698 强击。
5. **施法**：RetailPatternAI2.useSkill（2380-2412）→ `NpcController.useSkill(22695)`，
   客户端播放施法/受击音效。玩家每移动一次 ⇒ 一次日志 + 一次音效，与实机观察 1:1 吻合。

### 关键区分
- `sensory_range=8` 只约束**地面普通索敌**（checkAggro：aggro+喊话）；
- **反飞行反应**由真端 pattern 驱动，条件仅 `is_user_flying`，作用距离 = knownlist 视距（~95m）。
  真端 5.8 深渊 Ereshkigal 巡逻队即以此机制狙击飞行玩家。

## 规避方式（真端同款）
- 落地行走：`is_user_flying` 不成立 ⇒ 无反应（sensory 8m 也不会索敌）；
- 飞离 knownlist 视距（>95m）。

## 后续观察点（如需）
- 若实机发现 28m 处被该技能命中不掉血，再查 NpcController.useSkill 的射程校验与
  22695 目标范围（本诊断未涉及命中判定，只定性「发现+音效」链路）。
- 模板 npc_template_834290_885645.xml:77257（attack_range=37，远程狙击手定位吻合）。

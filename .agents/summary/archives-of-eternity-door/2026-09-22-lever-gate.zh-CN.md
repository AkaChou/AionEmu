# 301540000 永恒档案库：3rd Lever 击杀后护盾门不消失

## 现象

- 玩家报告：击杀 NPC 703014（`IDEternity_01_3rd_Lever_Magic`）与 703013（`IDEternity_01_3rd_Lever_Physical`）后，门没有开启。
- 运行日志（`log/console.log`）确认 2026-09-21/22 多次进入 301540000；`log/adminaudit.log` 2026-09-22 22:15:17 有玩家在 `IDEternity_01_3rd_Lever_Magic` 处 `//gps`，与报障时间吻合。

## 机制链路（零售数据）

1. `spawns/Instances/301540000_Archives_Of_Eternity.xml`
   - 703013 / 703014（3rd Lever，物理/魔法）各 3 个 spot；
   - 703019（`IDEternity_01_3rd_Shield_Remove`）3 个 spot，与 Lever spot 3D 距离约 4.9–5.7m。
2. `definitions/compact/ai/npcaipatterns_ideternity_kgw.xml`
   - `IDEternity_01_3rd_Lever_Physical/Magic` 的 `on_die` 为 `broadcast_message` `message_type=1003`、`range_as_meter=8`、`param_obj=OBJI_SELF`。
3. `definitions/compact/ai/npcaipatterns_ideternity_ssh.xml`
   - `IDEternity_01_3rd_Shield_Remove` 的 `on_message` 监听 1003，命中后 `despawn_self`（附带 `STR_IDEternity_01_Shield_Remove`=1403211 系统消息）。
4. `RetailPatternAI2.broadcastMessage` 只投递给实例内 8m 范围内的 `RetailPatternAI2` NPC；Shield_Remove 即接收者，despawn 后客户端护盾门消失。

## 根因

- `npc_template_286321_800030.xml` 中 703009–703016 的 `ai="noaction"`，技能数据 `definitions/compact/skills/npc-skills.xml` 只给 703017–703020 分配了技能组，703009–703016 无任何技能。
- Lever 范式含 `on_wake_up` 的 `use_skill SKILLI_INDEX_0`（起身抵抗 buff）。`RetailPatternAI2.supports(pattern, npc)` 的预校验要求所有技能动作都能在 NPC 技能表中解析，否则返回 false。
- 但运行时 `useSkill(...)` 对 `skill == null` 已安全返回 -1（静默跳过）。
- 结果：AI2Engine 因缺失可选 buff 技能把整条范式判为不支持，回退 `noaction`，`on_die` 的 1003 广播永不执行，703019 不 despawn，门不开。

## 修复

- `RetailPatternAI2.supports(Pattern, Npc)` 增加事件上下文；`on_wake_up` 中目标为 `OBJI_SELF` 的 `use_skill` 在技能缺失时不再否决整条范式（与运行时跳过语义一致）。
- 新增回归：`RetailPatternAI2Test#supportsArchivesOfEternityLeversWithoutServerSideWakeUpSkills`，加载真实零售数据并断言 703009–703016 的 `on_die` 广播保留、范式仍被接受。

## 验证状态

- 已完成：静态数据/源码链路审计。
- 待执行（本轮未授权构建）：
  - `mvn -B test -Dtest='RetailPatternAI2Test,ArchivesOfEternityMechanicsTest,AI2EngineRetailSelectionTest'`
  - 服务端重启后进入 301540000，击杀 3rd Lever 观察 703019 despawn 与护盾门消失（需用户管理进程生命周期）。

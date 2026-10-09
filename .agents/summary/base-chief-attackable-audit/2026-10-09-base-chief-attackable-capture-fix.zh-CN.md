# 基地占领关键 NPC（CHIEF/SLAYER）可攻击性修复 — 2026-10-09

## 现象（实机报障）

天族玩家在欧比斯 (400010000) base id 60「焰毁前哨 Flameruin Outpost」（坐标 2084.9/506.7/2897.5）击杀基地全部可见 NPC，基地归属仍为魔族，占领永不触发。

## 根因

1. 基地易主**只由 CHIEF（指挥官）死亡驱动**：`Base.spawnBoss()`（`services/base/Base.java:387-401`）只给 `handler="CHIEF"` 的 NPC 挂 `BaseBossDeathListener`；`onBeforeDie`（`BaseBossDeathListener.java:47-85`）取仇恨最高伤害者种族 → `setRace` → `BaseService.capture()` 重刷 NPC + 广播 `SM_FLAG_INFO` + 写库。杀普通守卫（`handlerType == null`）设计上就不易主。
2. `Npc.isAttackableNpc()`（`model/gameobjects/Npc.java:405-407`）仅认 `NpcType.ATTACKABLE`；被标 `npc_type="NON_ATTACKABLE"` 的 NPC 玩家无法攻击。
3. Encom 数据包系统性错标：深渊前哨 12 个龙族指挥官（`Ab1_v*_Eresh/Vitra_Fi_Boss_65_Ae`）为 `ATTACKABLE`（故打龙族前哨可占领），而 24 个天/魔族指挥官（`Ab1_v*_Li/Da_Fi_Boss_65_Ae`，883076-883214）全部 `NON_ATTACKABLE` → 天/魔互占闭环永久失效。

## 修复

全库审计 `spawns/Bases/*.xml` 的 CHIEF/SLAYER spawn（487 条），凡模板 `npc_type != ATTACKABLE` 一律修复：

- **4 个 `npc_template_*.xml`，266 行**（本轮 242 id + 深渊先修 24 id），仅改 `npc_type="NON_ATTACKABLE" → "ATTACKABLE"`，其余字段零漂移（diff 剥离该字段后 532 行完全成对）。
- 覆盖 11 图：Eltnen/Heiron/Morheim/Beluslan（冒险家基地 DF2/DF3 系）、Panesterra Belus/Aspida/Atanatos/Disillon（GAb1 系）、Katalam、Kaldor、Levinshor（LF2/LDF4/LDF5 系）。
- 安全性：242 个 id 经全 `spawns/` 反查仅被 `spawns/Bases/` 引用，零复用误伤。

审计脚本：`audit_base_chief_attackable.py`（本目录）；修复后 487/487 全部 ATTACKABLE，violations.tsv 归零。

## 验收

- 实机（2026-10-09）：重启服务端后击杀深渊前哨指挥官，基地成功易主并广播征服消息——**验证通过**（用户确认）。
- 机制备注：指挥官非开场刷新，`delayedSpawn`（Base.java:379-385）在基地启动/易主后 5~10 分钟刷出；复测需等待。

## 遗留边界（未修，逻辑层）

`onBeforeDie` 中若袭击军 SLAYER（NPC）抢到最高伤害：基地不易主但指挥官已死，且仅 `capture→stop→start` 会重刷指挥官 → 基地短暂进入无 boss 状态。真端袭击由真实玩家主导，此模拟边界影响很小；如需修复建议在 boss 死亡后补 delayed respawn 调度。

# 任务 15000 客户端验收记录（维修工具使用区补入）

quest: 15000「[4.8 LF5 필드 퀘스트] [성장] 오드 영역 확장」（天族，Cygnea 4.8；接取/交付 NPC = LF5_Milliard_E 804874「Millard」；任务物品 = Lost Backpack 182215661 / Artificial Aether Generator Repair Tool 182215662）。

user acceptance confirmation: 用户 2026-10-08 原话「实机验证成功」（对本次修复目标「在人工以太生成器处使用维修工具」的确认），未限定分支或步骤 ⇒ 视为整任务游玩验收；同条消息另授权本地提交与同类问题排查修复。

server launch mode: IDEA（常驻 Spring Boot 进程，classpath=target/classes）；服务端重启由用户管理（zones_quest.xml 为启动期静态数据，本次修复经冷重启生效）。

repository commit: 随本批次提交（`fix(quest): 15000 维修工具使用区补入——LF5_ITEMUSEAREA_Q15000 采信真端球值`）；诊断与全库审计见 `.agents/summary/quest-15000-itemusearea/`。

working tree: dirty；并行会话的 npc-walker / geo(path) 主题保留未暂存，未纳入本任务提交。

Aion 5.8 client/data provenance: 真端几何 `<真端根>/Map/XML/Subzones/source_sphere.csv:95696`（`usearea_lf5_itemusearea_q15000,itemUseArea,lf5,0,2890.31,826.17,706.21,28.50`）；短名映射 `<真端根>/Map/XML/Subzones/WorldId.xml`（LF5 → 210070000）；真端任务表 `quest/retail/quest.xml` 15000 行（`quest_work_item1=quest_15000b 1`、`collect_item1=quest_15000a 1`、`drop_monster_1=LF5_A_Bookie_55_An LF5_A_Bookie_56_An`、`drop_prob_1=70`、`check_item1_1=quest_15000a 1`）；DD 表 `data_driven_quest.xml:6029-6050`（CollectItem → Talk → ItemPlay）。本轮未新采集客户端包 SHA-256。

npc template/object: 804874（`LF5_Milliard_E`，`spawns/Npcs/210070000_Cygnea.xml:5758`，约 2868.09/776.90/569.16）；任务物品 182215661（a）/ 182215662（b）；运行时 objectId not captured。

map/instance: Cygnea，world 210070000；运行时 world/instance ID not captured。

steps:

1. 与 804874 接取 15000；击杀 `LF5_A_Bookie_55_An`/`LF5_A_Bookie_56_An` 掉落 `quest_15000a`（Lost Backpack，70%）。
2. 与 804874 对话交付 → 取得维修工具 `quest_15000b`（182215662）。
3. 至生成器处（球心 2890.31/826.17/706.21，r=28.50）右击维修工具：**修复前**每次 `CM_USE_ITEM` 恒定两条 `ZoneName - 缺少区域：LF5_ITEMUSEAREA_Q15000` 告警且使用被拒（1300143）；**修复后**使用放行，DD `ItemPlay` 步推进。
4. 与 804874 对话领奖，任务 REWARD → COMPLETE。
5. 未覆盖：球区边缘（≈28.5m）站位逐点；使用中途死亡/重登；同族其他任务。

source state/status/vars: ItemPlay 步（`START`）→ 使用维修工具成功推进 → `REWARD` → `COMPLETE`。运行时逐字状态/变量 not captured。

action/page/button: 右击维修工具 = `CM_USE_ITEM`（itemObj，修复前日志 151893）→ `PlayerRestrictions#canUseItem`（usearea 校验：`hasAreaRestriction()` + `getUseArea()` 各触发一次 `ZoneName.get`，缺区时两条「缺少区域」告警 → 回退 `NONE` → `isInsideZone(NONE)` 恒 false → 1300143 拦截）→ 修复后通过 → `QuestEngine#onItemUseEvent` → DD 车道 `onItemUsed(itemId=182215662)` 推进 ItemPlay 步。

expected response: 在区内使用维修工具被放行，ItemPlay 步推进；无「缺少区域」告警。

actual response: 用户确认「实机验证成功」（2026-10-08）；逐步 trace/截图 not captured。

startup health: not captured（服务端由用户重启管理）；离线静态：`xmllint --noout --schema zones.xsd zones_quest.xml` 通过（2026-10-08）。

runtime logs: 修复前 4 次 `CM_USE_ITEM` + 「缺少区域」告警（用户提供，12:31:45–12:32:18，见 `.agents/summary/quest-15000-itemusearea/DIAGNOSIS.zh-CN.md` §1）；修复后 not captured。

protocol trace: not captured（修复前后）。

screenshots/recordings and SHA-256: not captured。

acceptance status: ACCEPTED_EXISTING_PATTERN

matched Pattern: `QUEST_ITEM_USE_ZONE_AND_AMBUSH_CONTRACT`（QE-043）；matched fields = player-visible symptom（到达任务地点使用任务道具被拒 / 1300143）、root cause（item_template `usearea` 未在 `zones_*.xml` 注册）、repair layer（静态数据 `zones_quest.xml`）、repair contract（补入区、几何采信真端 `source_sphere.csv`、`zone_type=ITEM_USE`）；differing fields = 本例为**从未注册**（非已注册区几何错位）、无袭击怪与 `npc-complete` 分支；representative commit = `c2f772ea9`（13403 四区）、`de2b8bab1`（30721/30771）；representative test = `MultiCellSensoryZoneRegistrationTest`（含 zones_quest.xml 的 XSD 校验面；本任务未新增专属测试）。

remaining risks: ① 本区未单独新增聚焦测试（DD ItemPlay 面由既有门覆盖），静态面已核对既有门无总量断言；② 逐字 trace/截图 not captured；③ 球区边缘站位与 3D Z 窗口未逐点复测；④ 使用中途死亡/重登未复测；⑤ 同类批量修复已于 2026-10-08 完成 4 区（门禁 11/11：ItemUseAreaZoneRegistrationTest 4/4 + MultiCellSensoryZoneRegistrationTest 5/5 + QuestProductionStartupGateTest 2/2；批量区实机待验收），全库剩余 46 个 GAP 的分类与理由见 `.agents/summary/quest-15000-itemusearea/DIAGNOSIS.zh-CN.md` §7。

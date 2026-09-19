# 龙脊深渊任务副本双生守护者重复刷出（237228 / 237229 各 2 个）

- 日期：2026-09-19
- 地图：301520000（IDSeal_Q，任务版盘龙巢穴）
- 状态：已修复（删除 2 条非真端残留出生块）；聚焦测试待授权运行。
- 关联：同族判据见 `.agents/summary/spawn-duplicate-spots/README.md`（非真端残留 spot 去重）、`AIM-007`（同批次的奥里萨死亡兜底）。

## 现象

客户端实测：任务版盘龙巢穴的双生守护者 `237228`（Lava Protector）与 `237229`（Heatvent Protector）**各自刷出 2 个**（每侧 2 个，共 4 个）。

## 加载归属 / 根因

`src/main/resources/aion/data/static_data/spawns/Instances/301520000_Drakenspire_Depths.xml` 里同一 `npc_id` 有 **两条独立 `<spawn>` 块**：

| 来源 | 237228 | 237229 | 属性 |
| --- | --- | --- | --- |
| 手写 legacy 块（本次删除） | 545.58734 / 212.11098 / 1681.8224 | 545.7349 / 152.29825 / 1681.8224 | 无 `resolve_z` |
| 真端出生面（保留） | 531.088501 / 212.438065 / 1683.411621 | 530.858398 / 151.868103 / 1683.411621 | `initial_delay="1" random_walk="2" resolve_z="true"` |

`SpawnGroup2` 对每个 `<spawn>` 块实例化实体，因此两条块 = 2 个 NPC。legacy 坐标 (545,212/152) 来自**非任务副本** 301390000 的 Lv2 形态（`236225` Fountless Lava Protector / `236226` Fountless Heatvent Protector）布局，属于跨地图复制残留。

补充影响：`WorldMapInstance#getNpc(int)` 只返回第一个命中，`DrakenspireDepthsQInstance#onDie` case 237228/237229 的“击杀一侧后删除另一侧”只能删掉一份，重复副本会残留到副本结束。

## 真端取证

外部真端数据（`58Server/Map/Worlds/IDSeal_Q`）中同名 NPC 均**只有 1 条 count=1 的出生记录**，位置与保留块完全一致：

- `IDSeal_Q_Twin_P_N_65_Ah`（=237228）：`count=1`、`initial_spawn_count=1`，pos `531.088501 / 212.438065 / 1683.411621`。
- `IDSeal_Q_Twin_M_N_65_Ah`（=237229）：`count=1`、`initial_spawn_count=1`，pos `530.858398 / 151.868103 / 1683.411621`。

同一份真端数据中 (545.58734 / 545.7349) 属于 236225/236226（Lv2 形态），而真端 301390000 与 301520000 的静态出生表都**不含**这两个 Lv2 NPC——即 545 位置不是这两张图的双生守护者出生点。

## 处置

- 删除 `301520000_Drakenspire_Depths.xml` 中 237228 / 237229 的两条 legacy `<spawn>` 块（含其注释），保留真端出生面那两条。
- 新增闸门 `src/test/java/com/aionemu/gameserver/dataholders/DrakenspireDepthsQTwinSpawnSurfaceTest.java`：
  - 237228 / 237229 各自必须只有 1 条 `<spawn>` 块、1 个 `<spot>`，且坐标必须等于真端出生面；
  - 545.58734 / 545.7349 两个 legacy 坐标不得再出现在该文件中。

## 同族待定（本次未改，已取证）

同一份出生表/兄弟地图上还有同形状的非真端残留，需要单独决定（都涉及可见或不可见场景对象，未擅自删除）：

- `301520000`：`731580`（IDSeal_WaveDoor）仍有 2 条块——真端 IDSeal_Q 只有 1 条（809.731140 / 598.521606）；多出的那条在 592.412 / 181.304，该点在真端属于 `702827`（IDSeal_Q_FOBJ_AreaDoor），本地也同时存在 702827。
- `301390000`：`236223`（IDSeal_B_KeyMob_SN_65_Ah）本地 2 个点、真端 1 个（762.575378 / 250.841385），多出的 legacy 点在 772.6448 / 263.3362。
- `301390000`：`236126` 本地 6 个点、真端 4 个；`236128` 本地 6 个点中 2 个（458.43515 / 450.35242）不在真端出生面内（总点数恰好相等，属“位置错、数量对”）。

## 验证

- 静态：XML 解析通过；237228 / 237229 各 1 块 1 点且坐标等于真端出生面；`git diff --check` 通过。
- 聚焦测试（待授权，未执行）：
  `mvn -B test -Dtest='DrakenspireDepthsQTwinSpawnSurfaceTest,DrakenspireDepthsQTwinSceneTest,DrakenspireDepthsQOrissanSceneTest,ImmortalOrissanAI2Test,ThresholdTransformDeathFallbackGateTest'`
- 客户端复测：重建/重启新字节码后进入 301520000，双生守护者应各只有 1 个；击杀一侧后另一侧被正确删除、场景继续推进。

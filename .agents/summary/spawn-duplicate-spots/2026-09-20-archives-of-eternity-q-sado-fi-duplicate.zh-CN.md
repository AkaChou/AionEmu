# 永恒档案库任务副本惩罚者麦孜莱姆重复刷出（857783）

- 日期：2026-09-20
- 地图：301570000（IDEternity_Q，任务版永恒档案库）
- 任务：10521 / 20521（击杀 857783 后进入下一步）
- 状态：静态修复完成，聚焦测试与客户端复验待执行。
- 关联：同族判据见 `.agents/summary/spawn-duplicate-spots/README.md` 与 Memory Bank `IR-007`。
- 同族扩展：`2026-09-20-independent-legacy-block-audit.zh-CN.md`。

## 现象

客户端进入 301570000 后，`857783`（惩罚者麦孜莱姆 / Crystalized Shardgolem）刷出 2 个。任务只要求击杀 1 个。

## 根因

`src/main/resources/aion/data/static_data/spawns/Instances/301570000_Archives_Of_Eternity.xml` 中同一 `npc_id` 有两条独立 `<spawn>` 块：

| 来源 | 坐标 | 属性 |
| --- | --- | --- |
| 手写 legacy 块（本次删除） | 552.93384 / 351.93954 / 468.8791 | 无 `resolve_z`，`h=21` |
| 真端出生面（保留） | 544.977173 / 339.557068 / 469.500000 | `initial_delay="1" random_walk="2" resolve_z="true"` |

块级历史：

- legacy 块在初始导入 `911440146` 即存在。
- `a5e274fd0`（"synchronize retail NPC spawn surfaces"）追加了真端块，但没有删除 legacy 块。

`SpawnGroup2` 对每个 `<spot>` 实例化一个实体，因此两条块各生成 1 个模板 857783，客户端表现为 2 个。

## 真端取证

`58Server/Map/Worlds/IDEternity_Q/world_N.xml` 与 `world.xml` 中 `BIDEternity_Q_Sado_Fi_N_65_An_01`：

- `count=1`、`initial_spawn_count=1`；
- 位置为 `544.977173 / 339.557068 / 469.500000`；
- 与该地图保留块完全一致。

同地图本地结构审计结果：33 条 `<spawn>` 块、32 个不同 `npc_id`；唯一重复块为 `857783`。按真端同名记录计数核对后，该 NPC 本地 2 条而真端 1 条，其余差异不属于重复块形态。

## 处置

- 删除 `301570000_Archives_Of_Eternity.xml` 中 `857783` 的 legacy 块，保留真端出生面块。
- 新增 `ArchivesOfEternityQSadoFiSpawnSurfaceTest`：
  - 857783 必须只有 1 条 `<spawn>` 块、1 个 `<spot>`；
  - 坐标必须等于真端出生面；
  - legacy x 坐标不得再出现。

## 验证

- 静态：目标 XML 解析通过；`git diff --check` 通过（工作区既有无关文档换行警告除外）。
- 聚焦测试（待授权，未执行）：
  `mvn -B test -Dtest='ArchivesOfEternityQSadoFiSpawnSurfaceTest'`
- 客户端复验（待执行）：重建并重启服务端后进入 301570000，857783 应只出现 1 个，任务 10521/20521 击杀后可继续推进。

## 涉及文件

- `src/main/resources/aion/data/static_data/spawns/Instances/301570000_Archives_Of_Eternity.xml`
- `src/test/java/com/aionemu/gameserver/dataholders/ArchivesOfEternityQSadoFiSpawnSurfaceTest.java`

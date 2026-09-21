# 2026-09-19 Reshanta 400010000 空中刷点（252537 mind reaper 等）排查

## 现象
玩家进入 Reshanta（400010000）看到 252537「mind reaper」悬在空中，周围多个同类怪也在空中。
`//geo z` 输出：`curZ=1468.7838 geoZ=1460.0012 terrainZ=NaN pathGround=null spawnZ=1468.7838`
（无地形图 → terrainZ=NaN；PATH 投影因垂直容差 0.7m 未命中 → pathGround=null）

## 结论
**不是投影回归，是零售数据的空中刷点（Aerial_Spawn=TRUE），我方按原样保留。**

1. 零售 `58Server/Map/Worlds/ab1/world_N.xml` 中该点属于 `Ab1_1131_N_SpecterRe_25_1`：
   `Aerial_Spawn="TRUE"`，`move_area_points` 的 `<bottom>1468.662598</bottom> / <top>1488.662598</top>`
   是一块 20m 高的飞行活动盒，`<checksurfacez>` = top；刷点 `<z>1468.783813</z>` ≈ bottom。
   同类还有 `Ab1_1131_N_SpecterRe_25_2/25_4`、`Ab1_1131_M_SpecterRe_25_1`（10~20m 盒）。
2. 我方 `static_data/spawns/Npcs/400010000_Reshanta.xml` 的 252537 共 15 个点，其中 9 个由零售同步而来，
   写作 `fly="1" resolve_z="true"`，z 与零售完全一致；其余 6 个是旧的手工点（落在地面）。
3. 运行时对 `fly` 刷点**主动跳过贴地投影**：
   - `SpawnEngine.projectedSpawnZ`：`spawn.canFly()` → 直接返回 `spawn.getZ()`（SpawnEngine.java:320）
   - `SpawnSurfaceResolver.resolve`：`!isResolveZ() || canFly()` → 直接返回 `spawn.getZ()`（SpawnSurfaceResolver.java:24）
4. 全量审计（`.agents/summary/spawn-z-audit/audit_aerial_spawns.py`）：Reshanta 共 39 个 `fly="1"` 刷点，
   其下方客户端 PATH 可行走地面比刷点低 **5.31~32.78m（中位 11.03m）**；37/39 能查到地面节点，2 个（884011/884014 龙角）无节点。
   `//geo z` 的 geoZ=1460.0012 与离线 PATH 地面 1460.00 一致，交叉验证了审计脚本。

## 判断
- 这些怪"浮在空中"是零售设计（幽灵/收割者系 + 龙角等飞行单位），与地面刷点的投影修复互不影响。
- 新加的 geo 兜底 WARN 不会打在它们身上：`fly` 分支在 geo 兜底之前就返回了。
- 若客户端表现看起来不对（例如没有飞行动作、像站姿卡在空中），那属于客户端 NPC 侧信息，需要另查，而不是改 Z 投影。

## 复核方法
```
python3 .agents/summary/spawn-z-audit/audit_aerial_spawns.py   # fly 点清单 + 相对 PATH 地面的悬空高度
```
游戏内：对这些怪 `//geo z` 应始终 `curZ == spawnZ`（若出现 curZ 逐渐下降，说明别处有贴地逻辑在拉它）。

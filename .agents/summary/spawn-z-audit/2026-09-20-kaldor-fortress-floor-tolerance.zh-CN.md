# 2026-09-20 Kaldor 600090000 要塞地面刷点高度（804471/802432）

## 现象

地图 600090000 Kaldor 的 NPC 804471 Habitus 与 802432 Vanst 出现在地下。运行期 `//geo z`
（804471）显示：

```text
curZ=197.77827 geoZ=199.75026 terrainZ=197.77827 pathGround=null spawnZ=200.89
```

XML 中同簇刷点还包含 802431、802358；它们都在同一块要塞地面网格
`levels/common/tribe/rune/buildings/fortress/bu_ru_fortress_ground_01a.cgf` 上：

| NPC | 作者 Z | 地形 Z | 最高物理面 Z | 作者 Z - 物理面 |
|---|---:|---:|---:|---:|
| 802431 | 200.580 | 198.000 | 199.75027 | 0.830 |
| 802358 | 200.580 | 197.470 | 199.75026 | 0.830 |
| 804471 | 200.890 | 197.778 | 199.75026 | 1.140 |
| 802432 | 201.000 | 197.874 | 199.75024 | 1.250 |

## 根因

`SpawnEngine.projectedSpawnZ` 的顺序是 PATH 可行走地面 → 地形高度 → geo 碰撞面 → 地形兜底。
旧实现把地形和碰撞面共用 `AUTHORED_SURFACE_DELTA = 1.0f`：

- 802431/802358 与网格面差 0.83m，旧 1m 容差覆盖，因此此前没有报障；
- 804471/802432 与网格面差 1.14/1.25m，刚好越过旧容差；
- 地形高度图 197.778/197.874 比要塞地面低约 2m，旧实现因此返回地形，NPC 被压到地面网格下方。

这不是单个 NPC 的数据错误，而是同一网格面上 0.83～1.25m 的作者 Z 偏移簇触发了容差边界。

## 修复

拆分两个概念，保持兜底顺序不变：

- `AUTHORED_TERRAIN_DELTA = 1.0f`：地形与作者 Z 贴合才采用地形；
- `AUTHORED_SURFACE_DELTA = 2.0f`：碰撞面与作者 Z 贴合才采用碰撞面；
- 两者都不贴合才回退地形。

没有给 804471/802432 单独加 `resolve_z`/`fly`，也没有改 XML 刷点。

## 离线复核

用既有审计脚本重新计算 47 个 world 的“作者 Z > 地形 1m 且 PATH 未命中”候选点，并取
运行期 `GeoMap.getZ` 实际会返回的最高物理面：

- 1342 个候选中，674 个存在物理碰撞面，668 个没有物理面（运行期仍返回地形，不变）；
- 674 个有物理面的候选中：
  - `|物理面 - 作者 Z| <= 1m`：333 点（旧行为已覆盖）；
  - `1m < |物理面 - 作者 Z| <= 2m`：196 点（新 2m 容差覆盖；其中 194 点的物理面高于地形，运行期结果会改变）；
  - `> 2m`：145 点（仍回退地形，悬空/异常点护栏不变）。

审计口径说明：旧提交统计的 346 点允许“任意一个较低物理面在 1m 内”，而运行期
`geoGround` 读取的是向下投射遇到的第一张（最高）物理面；本复核按运行期口径统计为 333 点。

证据文件：

- `.agents/summary/spawn-z-audit/audit_surface_delta.py`（复现脚本；运行后在本地生成
  `audit_surface_delta_full.txt`）
- `.agents/summary/inggison-somation-rock-z/geo_surface_probe.py`（单点碰撞面探测）

## 验证状态

- static / 离线复现：已通过（Kaldor 两个点的物理面分别为 199.75026、199.75024）。
- focused-test：已通过（`mvn -B -Dtest=SpawnEnginePathProjectionTest test`：4 例 0 失败 0 错误）。
- 附带修正：`Quest10520ClientDialogAlignmentTest` 的 `BitField.max()` 改为 `maxValue()`；
  该测试运行通过（8 例 0 失败 0 错误），完整 testCompile 不再被该行阻断。
- 客户端实机验收：通过（2026-09-20 用户确认 804471/802432 站位正常，没有问题）。

## 边界

- `> 2m` 的碰撞面仍不信任，保留“作者 Z 悬空时回退地形”的既有语义。
- `resolve_z` 的 `SpawnSurfaceResolver`（geo 优先）保持不变。
- 审计使用 PHYSICAL 碰撞面；其他碰撞意图不在本次覆盖范围。

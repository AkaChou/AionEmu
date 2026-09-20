# 2026-09-20 805334 LF4_Somation_E 出生在巨石下方（210050000 Inggison）

## 现象
玩家截图 `//geo z`（目标 NPC 805334，Inggison 210050000）：

```
[Npc] curZ=473.55777 geoZ=473.58194 terrainZ=473.55777 pathGround=null spawnZ=489.7741
```

* 数据作者 Z（`210050000_Inggison.xml` 唯一刷点）：`x=2157.0432 y=277.79797 z=489.7741 h=83`
* 运行期 Z 被压到 **473.55777**，比作者 Z 低 **16.22m**；NPC 站在巨石脚下的地面，而不是石头上。

## 根因（已复现，非推测）
`SpawnEngine.projectedSpawnZ`（`src/main/java/com/aionemu/gameserver/spawnengine/SpawnEngine.java`）对“可移动、非飞行”刷点按
**PATH 可行走地面 → 地形高度 → 保留作者 Z / geo 面** 的顺序兜底：

1. PATH 投影失败（无 0.7m 容差内的节点）→ `pathGround=null`；
2. 于是直接采用**地形高度图**；而高度图只描述地表，**不含岩石/建筑/桥面等道具网格**；
3. 该点在 xml 里的高度正是石头顶面 → 被压到地表。

离线取证（本目录脚本，只读）：

| 证据 | 数值 | 结论 |
|---|---|---|
| `probe_2157_277.py` 复现 `Terrain.getPathHeight(2157.0432, 277.79797)`（float32） | `473.55777` | 与运行期 `curZ` **完全一致**，证明走了地形兜底 |
| `geo_surface_probe.py` 复现 `GeoMap.getZ`（PHYSICAL 碰撞面 + 放置物变换） | `z=489.77418` | 巨石网格面 = 作者 Z（`489.7741`，误差 8e-5） |
| 巨石网格名 | `levels/common/light/natural/rocks/etc/na_l_dark_rockgnbig_02a.cgf`，loc `(2154.292, 268.411, 485.906)` | 地面之上的独立道具网格，地形高度图不含它 |
| 该点第二层碰撞面 | `z=484.19073`（同一巨石的另一三角形） | 说明该网格完整存在于服务端 geo |
| 真端 5.8 客户端/服务端静态表 `58Server/Map/XML/Subzones/client_world_lf4.xml` 的 `npc_info` 805334 | `(2157.043213, 277.797974, 491.812439)` | 真端摆在石头上（比石头面高 2.04m ≈ 模型 height 2.0），进一步确认“不是地面 NPC” |

## 同族审计（不能只修这一个点）
`audit_rocks_vs_spawns.py`（离线复现 PATH 投影 + geo 下投）扫描 `spawns/Npcs/*.xml` 中所有有地形的 world：

* 作者 Z 高于地形 > 1m 且 PATH 未命中：**1342** 点（47 个 world）
* 其中脚下存在与作者 Z 贴合（≤1m）的碰撞网格面、**当前会被地形兜底压到下层地面**：**346** 点
* 分布（前 8）：`600040000` 116、`210100000` 61、`600050000` 32、`220140000` 22、`210050000` 21、`210020000` 16、`220080000` 16、`120080000` 11

Inggison 21 点样例：巨石（805334）、房屋地基（209038）、攻城塔（215730/216732）、烹饪罐（216793/216794）、岩菌（217038）、蛋（217095）、神庙台阶（281621）、纪念碑（700545）、龙族建筑构件（700747/700748）、蘑菇（700916/700917）等 —— 均为“作者把对象摆在道具网格面上”。

## 修复
`SpawnEngine.projectedSpawnZ`：PATH 失败后先看地形，但**当作者 Z 与地形差异超过 `AUTHORED_SURFACE_DELTA`（1.0m）时，再查 geo 碰撞面；若碰撞面与作者 Z 贴合（≤1m），采用该碰撞面**，否则维持原有地形兜底。
无地形图 world（`TERRAIN_DISABLED_MAPS`/缺 PNG）的既有 `keepsAuthoredZ → geo 兜底 → 作者 Z` 分支完全不变；`resolve_z` 走 `SpawnSurfaceResolver`（geo 优先）同样不变。
为控制启动开销，地形与作者 Z 贴合（≤1m）的常规刷点**不会**触发 geo 查询。

聚焦测试：`src/test/java/com/aionemu/gameserver/spawnengine/SpawnEnginePathProjectionTest.java`
* 新增 `prefersCollisionSurfaceMatchingAuthoredZOverTerrainFallback`（用 805334 真实数值：`489.77418` vs 地形 `473.55777`；不贴合时仍落回地形；地形贴合时不触发 geo 查询）
* 原 `fallsBackToGeoSurfaceWhenPathAndTerrainAreUnavailable` 的“地形优先于 geo”断言按新语义改写（geo 不贴合作者 Z 时地形仍然优先）

## 验证状态
* 静态/离线：**已完成**（上表 + 审计脚本）。
* 客户端实机：**通过（2026-09-20 用户确认）** —— 重启后 805334 站在巨石上，不再下沉到石头下方。
* 聚焦测试：**通过**：`mvn -B -Dtest=SpawnEnginePathProjectionTest test`（2026-09-20 21:05，4 例 0 失败 0 错误，BUILD SUCCESS）。

## 复现命令
```
python3 .agents/summary/inggison-somation-rock-z/probe_2157_277.py            # 地形/ PATH / 附近同族刷点
python3 .agents/summary/inggison-somation-rock-z/geo_surface_probe.py         # 单点 geo 碰撞面清单
python3 .agents/summary/inggison-somation-rock-z/audit_rocks_vs_spawns.py 210050000   # 单 world 同族审计
python3 .agents/summary/inggison-somation-rock-z/audit_rocks_vs_spawns.py 110010000 ... # 全量（见 audit_full_output.txt）
```

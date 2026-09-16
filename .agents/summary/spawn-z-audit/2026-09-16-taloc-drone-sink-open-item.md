# 待排查（暂停）：Taloc 300190000 Sheluk Drone 偶发"入地"

- 记录时间：2026-09-16 19:16（用户实机）
- 状态：**暂缓排查**——用户 19:2x 反馈"现在又不入地了，可能是偶发现象，等之后发现再排查"
- 归属：本次刷点 Z 兜底改造（`SpawnEngine.projectedSpawnZ`）的观察项，**但已排除是本次改动直接造成**

## 现场数据（`//geo z` / `//info`）

```
Name: sheluk drone   Id: 216139   ObjectId: 151523   EntityId: 0
Map ID: 300190000 (Taloc's Hollow)   NpcType: ATTACKABLE   TemplateType: MONSTER   AI: retail_pattern
X: 119.22767  Y: 476.3189  Z: 1136.4071
curZ=1136.4071  geoZ=1137.8994  terrainZ=NaN  pathGround=null  spawnZ=1137.8958
```

## 已完成的离线核对

- 该 spawn 点在 `Instances/300190000_Taloc's_Hollow.xml`（npc 216139 第 2 个 spot）：
  `x=119.29971 y=476.04306 z=1137.8958 random_walk=3`，**无 `resolve_z`**。
- 用 `PathData` 离线复现（`.agents/summary/spawn-z-audit/audit_path_projection.py`）在该刷新点查询：
  PATH 表面 = **1137.8900**，与作者 z 差 0.0058m → **0.7m 容差内，PATH 投影成功**。
  ⇒ 生成时走的是 PATH 分支，**不会进入新增的 geo 兜底**；生成高度 ≈ 1137.89，与作者 z 一致。
- `pathGround=null` 出现在 `curZ=1136.4071` 的查询带里（PATH 表面距它 1.48m > 0.7 容差），是"已经下沉"的结果而非原因。
- 结论：**下沉发生在生成之后**，与本次 spawn Z 兜底无关；当前不可复现。

## 下次复现时的最小取证

1. 刚进副本立刻对同一只 NPC 执行 `//geo z` 并截图（记录初始 curZ）。
2. 10–20 秒后对同一只 NPC 再执行一次 `//geo z`：
   - 初始 ≈1137.9、之后变 1136.4 → 生成后移动/AI 造成，查 retail_pattern 的移动目标 z、`NpcMoveController` 贴地修正、Taloc 动态碰撞/动态区域。
   - 初始即 1136.4 → 回到 spawn 路径，查实例 `instanceId` 与 geo 查询是否一致。
3. 同时记录：该怪是否处于移动状态（走/停）、附近是否有动态区域（Taloc 2F 风箱/抬升地面）处于激活状态。

## 相关已交付改动（未提交）

- `SpawnEngine.projectedSpawnZ`：PATH → terrain → geo → XML z（仅可移动、非飞行 NPC；`NON_ATTACKABLE/UNKNOWN` 摆放对象保留作者 Z）
- 偏差 > 10m 打 WARN（I18n `log.4e874ec198eb`，按 world+npc 去重、上限 2048 条）
- 该 WARN 已确认并修掉一类副作用：Reshanta 要塞护盾/以太场发生器被拉低 15–38m
- 验证：`mvn -q -Dtest=SpawnEnginePathProjectionTest,LocalizedLogCallsTest,I18nTest test` → 3/3 + 1/1 + 7/7 通过

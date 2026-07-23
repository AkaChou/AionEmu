# Retail Pattern 公共动作消费者矩阵

## 口径

- `动作数`：12,798 个唯一 Pattern 中该动作的原始出现次数。
- `Pattern 数`：至少包含一次该动作的唯一 Pattern 数。
- `副本绑定`：139 张副本地图的静态/条件出生中，按 `world:NPC:Pattern:action` 去重后的可达绑定数。
- 当前共 1,771 个去重副本绑定，由闭包测试锁定集合哈希。
- 可达不等于接管；具体 NPC 仍必须通过 `RetailPatternAI2.supports(pattern, npc)` 的技能、区域、路径、名称和下游消费者闭包。

## 矩阵

| 动作 | 动作数 | Pattern 数 | 副本绑定 | 唯一消费者与拒绝边界 |
| --- | ---: | ---: | ---: | --- |
| `activate_skillarea` | 552 | 114 | 33 | `RetailPatternAI2.activateSkillArea`；要求当前 world 的 `areaid` 多边形、NPC 技能槽和技能模板完整，`SKILLI_NONE` 继续拒绝。 |
| `enable_area` | 574 | 189 | 45 | `RetailAreaEngine`；仅接管完整的 `RESURRECT`、`QUESTSCRIPT`、`GROUPCTRL`、`LIMIT_NOPARK`、`LIMIT_NORECALL` 前缀及其复活点、任务模板或 GroupCtrl 下游。 |
| `on_off_windpath` | 54 | 25 | 2 | `RetailWindstreamEngine`；要求 NPC 所在 world 存在对应 `groupid`，状态按实例隔离并在进图时同步。 |
| `on_off_moving_collision` | 85 | 38 | 17 | `RetailDynamicAreaEngine`；要求 `type + sunzoneid` 命中 288 个真端动态区域之一，WindBox/Jump 分别使用客户端类型 `0/2`。 |
| `change_world_scene_status` | 101 | 92 | 89 | 当前唯一消费者是 `300300000/300320000` 的 Crucible Handler 阶段链；复用同一 Pattern 的其他 world 明确回退，避免重复推进和奖励。 |
| `spawn` | 16,357 | 3,772 | 1,185 | `RetailPatternAI2` 动态出生与持久化恢复；要求 NPC 名称、出生类型、坐标或 waypoint 完整。 |
| `spawn_on_target` | 895 | 488 | 211 | `RetailPatternAI2` 目标点出生；要求目标对象、距离和 NPC 名称完整。 |
| `spawn_on_target_by_attacker_indicator` | 306 | 64 | 55 | `RetailPatternAI2` 攻击者选择与目标点出生；要求攻击者指示器和 NPC 名称完整。 |
| `spawn_on_multi_target` | 324 | 84 | 59 | `RetailPatternAI2` 多目标出生；要求数量、目标集合和 NPC 名称完整。 |
| `despawn_by_nameid` | 849 | 171 | 75 | `RetailPatternAI2` 按真端 NPC 名称删除本实例对象；不对不存在名称做相似名猜测。 |

## 明确拒绝

- 全量 Pattern 的刷新名称闭包仍有 14 个唯一 NPC 名称不存在，对应 24 条 `Pattern:名称` 缺口；副本可达范围只有 2 条，继续保留旧所有者。
- `change_world_scene_status` 只在 Crucible 两图有完整 Handler 消费者；结构可解析不能替代地图阶段和奖励所有权。
- 区域、风道和移动碰撞动作必须命中当前 NPC 所在 world 的完整定义；同名、同 ID 出现在其他 world 不构成证据。
- 本矩阵不新增运行时分支，只锁定现有公共消费者和拒绝边界。

## 自动验证

- `RetailAiSkillSlotClosureTest.locksAreaSceneAndSpawnConsumerMatrix`
- `RetailAiSkillSlotClosureTest.preservesKnownRetailRuntimeDataGaps`
- `RetailPatternAI2Test`
- `RetailAreaEngineTest`
- `RetailDynamicAreaEngineTest`
- `RetailWindstreamEngineTest`

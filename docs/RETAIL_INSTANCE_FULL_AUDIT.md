# 真端副本完整审计

这份文档记录副本迁移证据和未闭环项。“已盘点”不等于“已对齐”；只有真端 world、AI Pattern、条件出生、掉落和运行时测试都通过，才标记为已对齐。

## 总览

`coverage.xml` 当前登记 139 个生产区域：93 个 Handler、21 个 RETAIL_AI_QUEST、4 个 EVENT、7 个 MATCHMAKER、8 个 TOURNAMENT、2 个 DATA_ONLY、2 个 HOUSING，另有 2 个非生产排除项。

## 已完成窄批次

| 批次 | 区域 | 真端所有权证据 | 服务端边界 |
| --- | --- | --- | --- |
| Harmony | `300450000`、`300570000`、`301100000` | world spawn、AI Pattern、waypoint、条件变量 | 保留匹配、计分和结算 |
| Drakenseer | `301620000` | `IDF6_Dragon/world_N.xml`、`NpcAIPatterns_IDF6_Dragon_SSH.xml`、`npc_drops_part_009.xml` | Handler 清为空类 |
| 下层储藏室 | `300050000`、`300060000`、`300070000`、`300080000`、`300090000` | 条件出生、静态出生、掉落组和 Pattern | 删除手写宝箱出生与旧掉落注入 |
| Draupnir（本批） | `320080000` | `iddf3_dragon/world_N.xml`、`NpcAIPatterns_IDDF3_dragon_SP_YDY.xml`、compact `npc_drops` | 条件阶段接管出生；删除错误 Abbey 箱子和自定义掉落 |

## Draupnir 证据

- 条件世界由 4 条扩为 18 条，包含 `master_mode`、`lastboss`、`lastboss_t`、`iddf3_dragon_t_waveend` 等 8 个变量，覆盖真端 16 个 `condition_info` 区域及两组页级副官出生。
- 条件槽包含普通/特殊 Boss、Akhal、三类效果对象、波次控制和四名副官；真端 NPC/Pattern 映射可在 `npc-ai.xml` 与 `npcaipatterns_iddf3_dragon_sp_ydy.xml` 找到。
- 静态出生移除 11 个与条件槽重复的 NPC 组，避免同一阶段静态和条件双重出生。
- 删除 Handler 中 `702658`、`702659` Abbey 箱子及其自定义掉落；这两个 NPC 属于 Adma 机制，真端 Draupnir world 不引用它们。Boss 其余掉落由 compact `npc_drops` 提供。
- 专项测试锁定变量/条件/槽数量、关键 NPC、重复静态出生和错误 Handler 残留。

## 未闭环

- 139 图的 JVM 重启三阶段（准备、战斗、结算）还需逐图验收。
- 客户端副本协议与真端抓包尚未形成证据闭环。
- 真端基础倍率与私服倍率仍需分层，尤其是掉落、任务、经验、AP/GP。
- 其余 Handler 的线程池、手写出生和 `onDropRegistered` 需按真端 ownership 逐图处理，禁止全局机械删除。
- AI 技能槽、特殊 Portal、跨世界条件变量和 GEO/PATH 压测仍有缺口。

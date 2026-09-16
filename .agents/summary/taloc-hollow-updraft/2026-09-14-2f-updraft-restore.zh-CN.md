# Taloc's Hollow 2F 上升气流（地面喷出气流）修复证据

- 时间：2026-09-14
- 地图：`300190000`（客户端 level 名 `idelim`）
- 报告：打碎虫卵后地面没有气流特效（可正常展开翅膀飞行，属表现缺失）
- 验收：2026-09-16 用户实机确认通过（打破破裂巨虫卵后卵位置升起地面气流）

## 真端链路（静态证据）

| 环节 | 证据位置 |
| --- | --- |
| 破裂巨虫卵 NPC `700739`（`cracked huge insect egg`） | `aion/definitions/compact/ai/npc-ai.xml`：`ai="Elim_WindEventB"` |
| 打破后开气流 | `npcaipatterns_idelim_osy.xml` 的 `Elim_WindEventB.on_die`：`set_condition_spawn_variable IDElim_2F_Wind=1` + `on_off_moving_collision MOVING_COLLISION_WINDBOX sunzoneid=100 TRUE` + `send_system_msg STR_MSG_IDELIM_WIND_INFO` |
| 气流视觉实体 | `condition-spawns.xml` 条件 `3085`（`IDElim_2F_Wind == 1`）刷出 NPC `281817`（`BIDElim_Windbox_NoshowNPC`）于 `653.900757, 839.13562, 1305`，`entity_id="9"` |
| 地面移动碰撞数据 | `dynamic-areas.xml`：`300190000 / MOVING_COLLISION_WINDBOX / id=100 / IDElim_WindBox` |
| 系统信息 | `ai-strings.xml`：`STR_MSG_IDELIM_WIND_INFO = 1400477`（旧硬编码实现使用的同一条） |

方向验证：条件 `3085` 是全库中唯一刷出 `281817` 的位置；`281817` 的客户端模型为
`objects/npc/level_object/windbox/windbox_01a.cgf` + `windbox_steam_01a.cgf` / `windbox_stream_01a.cgf`
（见 `aion/geo/models.mesh`），即"地面喷出气流"。

## 回归来源

- `5ccb10261`（`feat(instance): align Taloc's Hollow retail flow`）删除了 `TalocsHollowInstance.onDie`
  里 `case 700739` 的兜底：原先直接 `sp(281817, ...)` 并广播 `1400477`，改为完全依赖真端 pattern。
- 实机所用产物 `target/classes/.../TalocsHollowInstance.class`（当日 19:35）中已无 `281817`/`1400477`
  常量，说明玩家测试的构建里气流只有真端 pattern 一条路径。
- `Elim_WindEventB` 是否被 `AI2Engine.selectNpcAi` 选中取决于 `RetailPatternAI2.supports` 的完整门禁；
  该 NPC 模板 AI 名为 `noaction`，门禁失败时会静默回落到 `NoAction`，日志无报错，因此无法仅凭日志判定。
  静态核对（事件/动作/条件白名单、条件变量、动态区域、字符串、刷出 NPC 名）均通过，但缺少实机证据。

## 本次修改

`TalocsHollowInstance.onDie` 增加 `case 700739`：卵被打破时以幂等方式驱动真端执行器

- `RetailConditionSpawnEngine.setVariable(instance, "IDElim_2F_Wind", 1, 0)`
- `RetailDynamicAreaEngine.setEnabled(instance, "MOVING_COLLISION_WINDBOX", 100, true)`

两处调用都与真端 pattern 的副作用一致且幂等（条件已激活时不重复刷怪），因此 pattern 正常接管时
不产生重复实体，pattern 未接管时气流视觉与碰撞仍会开启。

## 验收结果

- **2026-09-16 实机验收通过（用户确认）**：打破破裂巨虫卵后，卵位置地面升起气流，可骑乘气流垂直上升。
- 提交：修复代码由 `5830ece07` 落库（`TalocsHollowInstance.java:220-228`）；本证据文档随 `0823653a7` 落库。
- 未按 A/B 隔离取证：没有单独验证 `Elim_WindEventB` pattern 当时是否已被 `AI2Engine.selectNpcAi` 接管，因此“适配器是本次唯一生效路径”未经隔离证明；可复用的结论与边界已提炼为 `IR-010`（`.agents/memory-bank/patterns/instance-runtime.md`）。

## 验收边界（历史记录）

- 修复当时未执行 Maven 构建、未重启服务端、未做真实客户端验证（按项目规则由用户控制构建与生命周期）；上述实机验收由用户在 `5830ece07` 之后的构建上完成。
- 当时列为下一步、且已被本次验收覆盖的客户端侧疑点：`SM_WINDSTREAM_ANNOUNCE` 的 moving-collision 类型字节
  （`RetailDynamicAreaEngine.packetType`：WINDBOX=0 / JUMP=2）与 sunzone 100 的初始开关（`dynamic-areas.xml` 中该区域为 `always_enabled="true"`）——验收通过说明气流视觉与托起碰撞均可由现有链路产生。

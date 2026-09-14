# 塔洛克的巢穴副本书

## 基本信息

- 副本：塔洛克的巢穴（Taloc's Hollow）
- 地图 ID：`300190000`
- 对齐目标：Aion 5.8 真端客户端的刷怪条件、移动路径、门状态和 Boss 机制。
- 当前状态：源码和静态数据已按真端数据整理；本记录同时保留服务端为兼容现有生命周期而增加的适配逻辑。

## 入口与门

入口流程不是右键门 NPC 后由服务端把一个固定门物体删除。真端场景使用条件刷出的实体 `entity_id=51`，服务端对应 NPC `700633`，它是客户端场景门的代理/触发实体，不应承担服务端碰撞。

1. 条件 `5001`（`IDElim_1F_StartRush == 1`）刷出 1F Rush 怪物、`215457` 和 `700633(entity_id=51)`。
2. `215457` 使用真端 AI `Elim_Octaside_Door`，被消灭后把 `IDElim_1F_StartRush` 设置为 `2`。
3. 条件组切换后，入口代理实体消失，客户端场景门进入可通行状态。实机验证结果是消灭 `215457` 后可以通过入口。
4. 服务端不再固定刷 `730527` 作为门碰撞体；服务端静态门只负责已登记的实例门状态。门状态变化后会通知路径障碍刷新，避免 AI 继续把已打开的门当作障碍。

Boss 门使用真端 AI pattern 的 `control_door`。真端 pattern 的门 ID 与服务端 `WorldMapInstance` 门表不同，因此在 `300190000` 中映射为：真端门 `1` -> 服务端门 `48`，真端门 `2` -> 服务端门 `7`。实例创建时服务端门 `49` 默认打开。

## 条件刷怪与移动

副本关键 NPC 由 `condition-spawns.xml` 负责，不再和旧的固定副本 XML 重复刷出：

| 条件 | 内容 |
| --- | --- |
| `5001` | 1F Rush、入口 Boss `215457`、入口代理 `700633(entity_id=51)` |
| `5002` | 1F Boss `215467`，使用真端位置和 `idelim_path_clodwormnm_52` 路径 |
| `5003` | 2F Rescue，包含 `799528` 和 `215478` |
| `5004/5005` | Gellmar：普通服 `215482`，特殊服 `246241` |
| `5006/5007` | Queen Mosqua：普通服 `215480`，特殊服 `246240` |
| `5008/5009` | Celestius：普通服 `215488`，特殊服 `246242` |

条件刷怪生成后会启动对应的真端 walker。这样 NPC 不依赖一次移动事件才开始走路，脱离战斗后也能回到真端路径/出生点。特殊服 Boss 的掉落和实例处理也已在 `TalocsHollowInstance` 中补齐。

## 215467 金奎德

- NPC：`215467`
- 服务端 AI 适配器：`kinquid`
- 真端 AI pattern：`Elim_ClodwormNm`
- 真端刷出点：`(236.154297, 708.597900, 1172.648804)`
- 进入战斗时关闭 1F Boss 门；离开战斗或死亡时打开 Boss 门。
- 重新攻击时会清理回位状态并重新进入 pattern，避免 Boss 已回位但不再战斗、计时器也不再工作的情况。

### 护盾技能时间线

| 技能 | 真端条件 | 周期 | 说明 |
| --- | --- | --- | --- |
| `18817` | HP <= 75% | 首次战斗计时器每 5 秒检查并重复 | 荆棘护甲，持续约 10.1 秒；容易被误认为保护膜 |
| `19233` + `19234` | HP <= 70% | 进入战斗后首次约 40 秒，之后每 40 秒 | 物理/魔法保护膜；每个技能施法模板约 1.5 秒，效果模板持续 100 秒 |

`19233/19234` 不是服务端自定义的快速循环，而是 `Elim_ClodwormNm` 的 `BTIMERI_INDEX_2`：`40000` 毫秒首次触发，之后仍按 `40000` 毫秒重置。两个保护膜技能由同一 pattern 连续执行。

## 烟雾机制

烟雾不是 Boss 本体固定生成一个低处 NPC。服务端刷出三个真端喷口位置之一的烟雾标记：

- `282008`：魔法烟雾。
- `282009`：物理烟雾，作为 `282008` 的 50% alternate。
- 三个喷口：
  - `(292.024658, 719.713196, 1174.000000)`
  - `(266.706848, 680.673279, 1174.000000)`
  - `(263.433411, 716.730042, 1174.000000)`
- 两种烟雾在每个喷口按 `5000/10000` 概率选择。
- NPC 模板高度为 `3`，所以烟雾在 Boss 附近半空出现是符合真端表现的，不应把坐标强行压到地面。

两个烟雾 NPC 使用真端 pattern `Elim_SmogEffect`：

- 看到 `lycan`（金奎德的种族）时对目标使用烟雾技能。
- 收到 Boss 的 `6764` 消息时对自身使用烟雾技能。
- 进入战斗后以 3 秒 battle timer 重复烟雾技能。
- 真端 pattern 还会生成并清理对应的视觉子实体。

当前服务端保留了烟雾实体的生命周期适配：实体最长约 20 秒，被攻击时销毁并按普通刷怪生命周期重置；烟雾触发、属性、位置和 Boss 消息仍由真端 pattern 驱动。

## 其他 Boss 与副本流程

- `Celestius` 已改为使用 `RetailPatternAI2`，不再运行旧的自定义冲刺/计时辅助逻辑。
- Gellmar、Queen Mosqua、Celestius 的普通服/特殊服实体由条件组选择，避免同一位置同时存在两套 Boss。
- 1F Rush、2F Rescue、2F Wind、3F Boss、治疗植物等条件变量保留真端命名和切换方式。
- 旧的固定 Boss、入口代理和副本脚本硬编码刷怪已移除，避免条件刷怪与固定刷怪重复。

## 相关实现位置

- 条件和实体：`src/main/resources/aion/definitions/compact/ai/condition-spawns.xml`
- 移动路径：`src/main/resources/aion/definitions/compact/ai/ai-waypoints.xml`
- 真端 pattern：`src/main/resources/aion/definitions/compact/ai/npcaipatterns_idelim_osy.xml`
- 副本固定刷怪和烟雾喷口：`src/main/resources/aion/data/static_data/spawns/Instances/300190000_Taloc's_Hollow.xml`
- Boss/烟雾 AI 适配器：`src/main/java/com/aionemu/gameserver/ai/instance/tallocsHollow/`
- 条件刷怪执行器：`src/main/java/com/aionemu/gameserver/ai/RetailConditionSpawnEngine.java`
- 门状态和实例处理：`src/main/java/com/aionemu/gameserver/ai/RetailPatternAI2.java`、`src/main/java/com/aionemu/gameserver/instance/handlers/scripts/TalocsHollowInstance.java`

## 验收边界

当前已记录的实机现象：烟雾可以消除 `215467` 的保护膜；烟雾在半空喷出；消灭 `215457` 后入口可以通过。此次提交不启动或重启后端，也不执行 Maven 构建；部署后若仍看到旧行为，应先确认服务端加载的是本次静态数据和源码对应的产物，并使用新建副本复测条件刷怪和 Boss 计时器。

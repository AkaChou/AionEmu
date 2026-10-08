# 帕休曼迪尔寺院 Macunbello 真端条件刷对齐（debufflich 阶段链）

## 1. 需求
- 实机反馈（截图）：在贝丝蒙迪尔神殿（Beshmundir Temple，world 300170000）藏身处
  X=982.16 / Y=134.62 / Z=241.77 处「马昆贝罗 boss 不在」，同时收到系统消息
  「黑暗咒术师马昆贝罗离开了藏身处」（msg 1400469）与「某个地方的沉重的门打开了」（msg 1401839）。

## 2. 根因（两层，缺一不可）
1. **真端 pattern 链本身在工作**：Temadaro（216586，ai=`IDCT_Lichkey4`）despawn 时
   spawn DespawnLich（281697）→ 其 `on_wake_up` 显示 1400469 + 广播 6981（50m）+
   despawn_self → Macunbello（216245，ai=`IDCT_Boss_Lichking`）的 pattern `on_message(6981)`
   → `despawn_self`。于是 boss 被当场抹除。
2. **真端的承接机制未落地**：真端 Macunbello 由 `debufflich` 阈值条件刷生成
   （`no_respawn="TRUE"`，死/被抹都永久消失）；我方 spawn 文件里 216245 是**静态单只**
   （无重生）→ 被 6981 抹掉后本实例内永久丢失，表现即「boss 不在」。
3. **门禁联动（IR-010 同族）**：`debufflich` 未在 condition-spawns.xml 声明时，
   写该变量的 12 个 pattern（8 灵魂 + Lichkey1）被 `RetailPatternAI2` 的运行时门禁
   （`RetailConditionSpawnEngine.supports`）整体拦下，静默回落模板 AI——即「计数器永远不增长，
   后续阶段条件永远不触发」。

## 3. 真端机制（world_N.xml territory，已解析）
| 条件（表达式基底） | N 套 NPC | H 套 NPC | 触发器（同阈值） |
|---|---|---|---|
| `debufflich >= 1`  | 216245 @ (980.805,134.412,244.5) | 216164 @ (980.742,134.419,245) | — |
| `debufflich >= 12` | 216736 @ (980.797,134.350,245.5) | 216733 @ (981.009,134.386,246) | 281696（weakness1）|
| `debufflich >= 24` | 216737 @ (980.793,134.141,246.5) | 216734 @ (981.002,134.411,247) | 281759（weakness2）|
| `debufflich >= 36` | 216738 @ (980.822,134.295,247.5) | 216735 @ (981.015,134.373,248) | 281760（weakness3）|

- **驱动**：8 个灵魂模板（216206-216213，H 页 36 spot）死亡各 +1；Lichkey1（216583）死亡再 +1
  → 上限 37，36 档可达。两处写入均为 pattern 的 `on_die` → `set_condition_spawn_variable(debufflich, +1)`
  （逐模板核对见 §5）。
- **阶段推进语义**：`RetailConditionSpawnEngine` **无补刷**——条件转真时刷一次（含 `initial_delay`），
  阶段推进靠 debufflich 越过下一阈值触发**新条件的首次激活**；被 6981 抹除/被击杀的变体不重生
  （真端 `NPCMaker_Respawn` 对 `no_respawn` 直接跳过，反编译验证）。
- **时序不变量**：触发器 `initial_delay=1`（+1 抖动）必须**先于**阶段变体 `initial_delay=10`（+1）落地，
  否则新刷出的变体会被触发器 `on_wake_up` 的 6981 广播（50m 半径）当场清掉。真端数据天然满足
  （1-2s < 10-11s），本次转写保留。
- `SpecialServer_Cond`（SP_ 系列）普通服恒 0，**故意不声明**（无 pattern 写入 → 表达式里恒 0），
  仅在表达式里保留真端字面。

## 4. 改动（4 个文件）
1. `src/main/resources/aion/definitions/compact/ai/condition-spawns.xml`（world 300170000）：
   - 新增变量声明 `debufflich`、`bt_page`（`bt_page` 是工程页变量：0 = 普通页/N 套，当前实机默认；
     1 = 困难页/H 套，未来难度页入口）；
   - 新增 11 条条件（id 5010-5020）：DebuffLich 触发器 3（`despawn_at_other="true"`，delay 1）
     + N 套变体 4（`bt_page == 0`，delay 10）+ H 套变体 4（`bt_page == 1`，delay 10），
     坐标/朝向/延迟逐一取真端 territory（对照表见 `condition-rows.tsv`，附 source 行号）。
2. `src/main/resources/aion/data/static_data/spawns/Instances/300170000_Beshmundir_Temple.xml`：
   - 删 216245 / 281696 的静态 spawn 块（改条件刷，原位留注释指向条件 id）；
   - 216288（N 侧灵魂）标 `spawn_page="1"`（修复页 0 泄漏，与并行去重会话同口径）；
   - 补齐 7 个 H 侧灵魂 spot（全精度真端坐标）→ 8 灵魂共 36 spot（= 真端 H 页 36 点）。
3. `src/main/java/com/aionemu/gameserver/instance/handlers/scripts/BeshmundirTempleInstance.java`：
   - 删 `onInstanceCreate` 对 216245 的 SLEEP / 19046「Soul Starved」自造逻辑；
   - 删 `onDie` 的 216583-216585 摆渡人三分支（`sp(799518-799520)`，与 condition-spawns#2093-2095
     **双刷**；声明 debufflich 会让 216583 也加入双刷，必须删）；
   - 删 `onDie` 的 216206-216213 灵魂计数段（`macunbelloSoul` 7/14/21 → 1400466/47/48 +
     19046-48 切换），消息与阶段推进改由 DebuffLich 触发器 + 阶段变体条件刷承担。
4. `src/test/java/com/aionemu/gameserver/dataholders/loadingutils/RetailAiDefinitionLoaderTest.java`：
   - 条件刷总数 4439 → 4450；新增 BT 断言（变量声明、11 条条件、bt_page 页分流、
     216245 坐标、触发器/变体延迟时序）；
   - 新增 20 个 BT NPC 的 `RetailPatternAI2.supports` 静态断言（门禁解锁后保持 retail_pattern）。

## 5. 验证
### 静态（已完成）
- `xmllint --noout` 两个 XML 通过；`git diff --check` 干净；IDE 检查两文件 0 错误。
- **白名单审计**（`pattern_support_audit.py`，本目录）：14 个 pattern / 20 个 NPC 的事件、
  条件、动作（含非 TARGET_EVENTS 无 OBJI_EVENT_TARGET）全部命中 `RetailPatternAI2` 白名单，0 问题。
  参数级校验未复刻，但 DebuffLich 系列的动作集（broadcast_message + display_system_message +
  despawn_self）是实机已验证的 DespawnLich（281697）的严格子集。
- **变量写入核对**（ElementTree 提取）：216206-216213 八个 on_die 均写 `debufflich +1`；
  216583 写 `lichkey1 +1` **且** `debufflich +1`；216584/216585 写 `lichkey2/3 +1`。
- **数据态**：页 0 重复坐标 0；H 侧灵魂 spot 36/36；216245/281696 静态块已注释化。
### 待办
- **focused-test 通过**（2026-10-08，IDEA MCP）：`RetailAiDefinitionLoaderTest` 6/6 全绿（0 失败），
  含条件总数 4450 与 20 NPC `supports` 断言。注：类内 `rejectsExactMemberAssignedToMultiplePartyTokens`
  无 `@Test` 注解、从未执行（历史遗留，不在本次范围）。
- 实机验收（用户，按序）：进 BT 新实例 → 藏身处不应有 Macunbello（debufflich=0）→ 杀 1 灵魂出
  216245 → 杀到 12 只出 281696 + 「力量正在减弱」→ 216245 消失、216736 出现（阶段推进）→
  杀 Temadaro 回归 1400469/1401839 → 任务 30227/30327 可完成 → 击杀变体不再重生。

## 6. 边界与遗留
- 216245 被 6981 抹除后任务 30227 在本实例内不可完成——与真端 `no_respawn` 语义一致，**不作兜底**。
- H 套变体（bt_page==1）当前不刷；未来实现 BT 难度页时对实例 `setVariable("bt_page", 1)` 即可切换。
- 真端 `move_area_points` 区域巡逻未做（我方无此机制），条件刷用真端固定落点。
- N 侧灵魂（216287-216294）维持 `spawn_page=1` 跳过（既有去重口径）。
- 灵魂 36 点的**实机可达性**（战斗中被 6981 链波及等）待验收确认。

## 7. 工具（保留于本目录）
- `condition-rows.tsv`：11 条条件的真端 territory 转写对照（含 delay、坐标、source 行号）。
- `pattern_support_audit.py <repo_root>`：解析 npcaipatterns，对 BT 相关 NPC 做
  `RetailPatternAI2` 白名单近似核查（事件/条件/动作/事件目标）。

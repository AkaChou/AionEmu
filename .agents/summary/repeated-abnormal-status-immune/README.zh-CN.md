# 真端「重复异常状态递减/免疫」机制移植

> 日期：2026-10-07 ｜ 状态：实现完成 + 定向测试全绿（94 项）｜ 实机验收 **PENDING**（需用户重启服务端后观察 PvP 连续控制）

## 1. 结论摘要

用户问题：部分负面 buff 可刷新、部分不应刷新（如晕厥），需按真端对齐。
排查结论（真端权威）：真端存在「重复异常状态递减链」，**仅覆盖 PARALYZE / SLEEP / FEAR 三种状态**，
**STUN（晕厥）不在表内**；且**仅 PvP（双方玩家）**读取。已按此口径完整移植进 emulator。

## 2. 真端证据（镜像）

- 数据：`<真端根>/Map/XML/repeated_abnormal_status_immune.xml`（UTF-16 + DOCTYPE），3 条：

| 状态 | holding_time1 | holding_time2 | resist_value 1→5 | time_value 1→5 |
|---|---|---|---|---|
| PARALYZE | 1 | 0 | 0,0,200,400,1000 | 100,90,85,80,0 |
| SLEEP | 1 | 0 | 同上 | 同上 |
| FEAR | 1 | **2000** | 同上 | 同上 |

- 消费逻辑（反编译源码 `58Server/server58-source/MainServer_Server64/`）：
  - 加载与校验：`classes/Skill/SkillDB.cpp:4526`（stride 0x1c；resist<1001、time<101、holding_time1<100、holding_time2<100001）
  - 运行时读/写：`fun/fun_053.cpp:7599`（`FUN_14060afc0` 查询）、`:7667`（`FUN_14060b190` 成功施加后步数+1，上限 5）
  - 调用点：`classes/Skill/SkillEffectMgr.cpp:2414-2431`（`SkillEffectMgr::AddSkillEffect` 内）
- 语义：
  - 目标按状态记「累积步数（≤5）」+「上次命中时刻」；
  - 本次施加落在窗口 `lastHit + holding_time1 × 原始时长 + holding_time2`（闭区间）内 → 时长 ×`time_value[N]%`、追加抵抗 +`resist_value[N]`（千分制）；
  - 窗口外 → 步数归零（重开链）；被抵抗不 +1；**noresist 效果不参与**；
  - 读取端条件为「施法者与目标都是玩家」（字段证据：同函数日志 `"Target(%d, isUser:%d)"` 证实 `obj+0x144 == 1` 即玩家）；
  - 记录端（写函数）无 PvP 门 → 任何来源成功施加都记到目标；可观察等价实现 = 「目标为玩家即记录」。
- 步数档位映射（1-based）：第 N 次成功施加后读第 N 档；首次（无记录）不调整。

## 3. emulator 实现落点

| 环节 | 位置 |
|---|---|
| 数据（3 条，数值逐位） | `src/main/resources/aion/data/static_data/repeated_abnormal_status_immune.xml` + `.xsd` |
| 模板 / 持有者 | `model/templates/RepeatedAbnormalStatusImmuneTemplate.java`、`dataholders/RepeatedAbnormalStatusImmuneData.java`（双索引 + fail-fast） |
| 目标追踪 | `model/gameobjects/player/PlayerRepeatedAbnormalStatus.java`（惰性分配；`byte[]` 步数 + `long[]` 时刻，按数据表位置索引）；`Player` 门面 `getRepeatedAbnormalStep` / `recordRepeatedAbnormalHit` |
| 读取/调整（PvP 门） | `skillengine/effect/EffectTemplate.java#applyRepeatedAbnormalStatusImmune`（在 `calculateEffectResistRate` 的 altered-state 分支内；追加抵抗并入 exclusive 位、不参与 resting 折算） |
| 效果标记与时长 | `skillengine/model/Effect.java`：`repeatedImmuneStatus/Step/DurationPercent` + `recordRepeatedAbnormalStatus(Player, now)`；`getEffectsDuration()` 在 PvP 调整后乘 `percent/100` |
| 写入/落账 | `controllers/effect/EffectController.java#addEffect`（`startEffect` 之后；`owner instanceof Player` 且有标记才落账） |
| 注册 | `StaticData` / `DataManager` / `static_data.xml`（根级 import）/ `static_data.xsd`（include）/ 两个 `messages*.properties`（`log.7f4a2c9e1b63`） |

关键设计：**读-写配对**——读取（含 noresist/表外状态过滤）在抵抗判定阶段只对「经 `calculateEffectResistRate` 且命中表内状态」的效果打标记；
落账只认带标记的效果。因此 `BuffSleepEffect`（不走 calculate）、强迫效果、noresist、被动、表外状态全部天然排除；
`PlayerEffectController.addSavedEffect`（登出补登直接 `startEffect(true)`）天然绕开钩子——补登不入账，正是期望语义。

## 4. 工程防护与近似（复用价值高）

- **⚠ `Effect.startEffect` 的 `duration == 0` 陷阱**：`:945` 早退时模板已 start（异常位已设）、
  效果已进 controller 映射，但 **end 任务未排 → 永久异常 + 映射泄漏**。
  凡「把效果时长缩短到可能为 0」的改动都必须钳 `Math.max(1, …)`（本实现 0% 档 → 1ms）。
- 近似 1：窗口用「本模板原始时长」（`duration2 + duration1 × 技能等级`）近似真端 Original Duration；
- 近似 2：追加抵抗放在 resting ×0.3 折算之外（保证 0% 档 +1000 尽可能等于免疫）；
- 边界：同一次施法多追踪模板 → 先到先得（与 `getEffectsDuration` 首模板同口径）；破抗 > 抗性时 0% 档理论上仍可能落地 1ms（客户端等同没控住）。
- 以上近似均写入代码注释，待真端二次取证时单点回调。

## 5. 验证状态

- 新增单测 4 类 35 项 + 回归 6 类 59 项 = **94/94 通过**（IDEA MCP，2026-10-07）：
  - `RepeatedAbnormalStatusImmuneDataTest`（6）数值逐位 / STUN 排除 / 窗口公式 / fail-fast / XSD 校验
  - `PlayerRepeatedAbnormalStatusTest`（9）惰性分配 / 闭区间窗口 / 封顶 / 独立 / clear
  - `RepeatedAbnormalStatusImmuneEffectTest`（16）档位 100/90/85/80/0% 与 0/0/200/400/1000 / 出窗重置 / NPC 只记不调 / noresist·STUN 排除 / 1ms 防护
  - `EffectControllerRepeatedStatusTest`（4）成功 +1 / 被拒不记 / 无标记不记 / NPC owner 无副作用
  - 回归：`EffectControllerTest`（28）、`EffectTemplateRetailFormulaTest`（3）、`XmlDataLoaderTest`（24）、`StaticDataTest`（1）、`LocalizedLogCallsTest`（1）、`LocalizedLogArgumentsTest`（2）
- **实机验收 PENDING**：需重启服务端后由玩家 A 对玩家 B 连放 SLEEP/PARALYZE/FEAR 类技能，
  观察第 2..6 次时长递减 / 抵抗递增 / 最终免控；NPC 来源与 STUN 场景应保持原行为。

## 6. 后续（待验收后评估）

- 实机验收通过后，考虑把「duration==0 陷阱 + 读-写配对钩子」提升为记忆库 pattern（当前无 skill/effect 运行时卡片，需评估新建）。

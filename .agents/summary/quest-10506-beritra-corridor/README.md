# 任务 10506「Mind Over Matter / 进入进攻回廊」进攻回廊传送门阶段错位修复纪要

## 1. 现象与现场证据 (Symptom & Live Evidence)
- **任务编号**：10506（天族 希格尼娅 / Cygnea 主线使命）。
- **用户反馈**：
  进攻回廊 702666 在任务第三步“去找布里特拉进攻回廊”就进入了，现在到第 5 步“通过布里特拉进攻回廊追踪布里特拉”时进不去了。
- **现场实测时序日志**：
  ```
  09-19 20:37:15 INFO  [multiThreadIoEventLoopGroup-3-4] c.a.g.n.a.s.SM_QUEST_ACTION - [QUEST-TRACE][S->C] SM_QUEST_ACTION 任务=10506 状态=3 步数=2
  09-19 20:37:25 INFO  [multiThreadIoEventLoopGroup-3-4] c.a.g.n.a.s.SM_QUEST_ACTION - [QUEST-TRACE][S->C] SM_QUEST_ACTION 任务=10506 状态=3 步数=3
  09-19 20:37:31 INFO  [multiThreadIoEventLoopGroup-3-4] c.a.g.n.a.s.SM_QUEST_ACTION - [QUEST-TRACE][S->C] SM_QUEST_ACTION 任务=10506 状态=3 步数=67
  09-19 20:37:47 INFO  [multiThreadIoEventLoopGroup-3-4] c.a.g.n.a.s.SM_QUEST_ACTION - [QUEST-TRACE][S->C] SM_QUEST_ACTION 任务=10506 状态=3 步数=4
  ```

## 2. 客户端任务链与状态机映射 (Client Steps vs State Machine)
对照 5.8 客户端任务脚本 `quest_q10506.html` 步骤定义：
- **Step 1 (`started`, var0=0)**：在西格尼亚和布仑太 (804709) 对话 -> `SETPRO1` -> `var0=1` (`s1`)
- **Step 2 (`s1`, var0=1)**：在帕特马深海盆地和普诺埃 (804710) 对话 -> `SETPRO2` -> `var0=2` (`s2`)
- **Step 3 (`s2`, var0=2)**：**去找布里特拉进攻回廊**
  - 前往外界回廊所在位置，进入感应区 `LF5_SENSORYAREA_Q10506_210070000` 触发阶段推进 -> `var0=3` (`s3`)
  - **此步骤仅为侦察发现回廊，回廊前有第 48 进攻部队参谋兵把守，玩家尚未消灭守卫，不得提前使用回廊传送！**
- **Step 4 (`s3`, var0=3)**：**消灭第48进攻部队参谋兵 (0/2)**
  - 击杀外界怪物 236259 满 2 只后推进 -> `var0=4, var1=0` (`s4`)
- **Step 5 (`s4`, var0=4)**：**通过布里特拉进攻回廊追踪布里特拉**
  - **此步骤才是正式使用进攻回廊入口 702666 传送进入回廊内部！**
  - 传送至回廊内部 `(2837.0, 2991.0, 680.0)`，向前移动触发感应区 `LF5_SENSORYAREA_Q10506_2_210070000` -> 推进至 `var0=5` (`s5`)
- **Step 6 (`s5`, var0=5)**：**是陷阱！逃出布里特拉进攻回廊，然后和普诺埃对话**
  - 玩家使用回廊内部出口 702667 传送离开回廊返回外界 `(1894.7863, 2455.1982, 336.875)`
  - 与普诺埃 (804710) 对话触发剧情（SELECT6 -> SELECT6_1 -> SETPRO6）-> 推进至 `var0=6` (`s6`) 并召唤被支配的普诺埃 (236263)
- **Step 7 (`s6`, var0=6)**：压制被支配的普诺埃 (0/1) -> 击杀 236263 推进至 `var0=7` (`s7`)
- **Step 8 (`s7`, var0=7)**：和普诺埃对话 (SELECT8 -> SELECT8_1 -> SET_SUCCEED) -> 获得 182215613，推进至 `reward` (`var0=8`)
- **Step 9 (`reward`, var0=8)**：向布仑太 (804709) 报告完成任务并领奖。

## 3. 根本原因深度溯源 (Root Cause Analysis)
1. **历史定义节点绑定错位**：
   - 历史 XML 定义中，回廊入口 702666 和出口 702667 的 `USE_OBJECT` 传送路由以及 `<can-act template-id="..." action-type="ACTION_ITEM_USE"/>` 交互资格，被全部错误配置在 `s2` 节点（即 Step 3: 去找布里特拉进攻回廊）；
   - 这导致在 Step 3 玩家未消灭守卫时即可直接右键 702666 提前传送进回廊；
   - 而真正需要使用回廊进入的 Step 5 (`s4`) 和需要逃出回廊的 Step 6 (`s5`) 上，**完全缺失 702666 / 702667 的 `can-act` 与 `USE_OBJECT` 路由**！
2. **阻断现象**：
   - 在 `QuestItemNpcAI2.handleDialogStart` 中，服务端通过 `questEngine.onCanAct` 校验玩家当前任务节点是否允许 `ACTION_ITEM_USE`；
   - 当玩家处于 Step 5 (`s4`, var0=4) 时，由于 `s4` 无 `can-act` 路由，`actionAllowed` 返回 false 且无其他 talk 路由，直接 `return` 拦截了交互进度条，导致玩家无法点击使用 702666。

## 4. 修复方案 (Fix & Contract Design)
1. **清理 `s2` 抢跑路由**：
   - 移除 `s2` 上的 702666 / 702667 `USE_OBJECT` 传送与 `can-act` 交互资格。确保 Step 3 必须先走到感应区侦察发现回廊，进入 Step 4 击杀守卫。
2. **补全 `s4`（Step 5: 通过布里特拉进攻回廊追踪布里特拉）路由**：
   - 增加 702666 `USE_OBJECT` 传送进回廊 `(2837.0, 2991.0, 680.0, heading 67)`；
   - 增加 702667 `USE_OBJECT` 传送出回廊（容错退出）；
   - 增加 702666 / 702667 的 `ACTION_ITEM_USE` `can-act` 交互资格。
3. **补全 `s5`（Step 6: 是陷阱！逃出布里特拉进攻回廊，然后和普诺埃对话）路由**：
   - 增加 702667 `USE_OBJECT` 传送出回廊 `(1894.7863, 2455.1982, 336.875, heading 109)`；
   - 增加 702666 `USE_OBJECT` 传送进回廊（容错重返）；
   - 增加 702666 / 702667 的 `ACTION_ITEM_USE` `can-act` 交互资格。
4. **双端文件同步**：
   - 同步修改 `src/main/resources/aion/data/static_data/quest_definition/quests/10506.xml` 与 `aion/data/static_data/quest_definition/quests/10506.xml`。
5. **单元测试回归锁定**：
   - 在 `Quest10506ClientDialogAlignmentTest` 中增加 `corridorPortalsOnlyAvailableInStep5AndStep6` 测试，断言 `s2` 无回廊交互/传送，`s4` 与 `s5` 具备完整的交互与传送路由。

## 5. 本次修改清单 (Changeset Summary)
1. `src/main/resources/aion/data/static_data/quest_definition/quests/10506.xml`
2. `aion/data/static_data/quest_definition/quests/10506.xml`
3. `src/test/java/com/aionemu/gameserver/questEngine/definition/Quest10506ClientDialogAlignmentTest.java`
4. `.agents/summary/quest-10506-beritra-corridor/README.md`
5. `.agents/summary/quest-10506-beritra-corridor/verify_10506_contract.py`

## 6. 验证状态 (Validation Status)
- [x] Python XML Schema 校验（`quest_definition.xsd`）：通过。
- [x] Python 静态状态机与路由连通性断言（`verify_10506_contract.py`）：全部通过。
- [x] 单元测试 `Quest10506ClientDialogAlignmentTest`：通过 (2 tests, 0 failures)。
- [x] 复合主线阶段变量门禁 `QuestCollectProgressAlignmentGateTest`：通过 (6 tests, 0 failures)。
- [x] 全服交互物启动门禁 `QuestInteractionObjectCatalogTest`：通过 (7 tests, 0 failures)。
- [ ] 客户端实机端到端全流程验证（待玩家实机游玩验证）。

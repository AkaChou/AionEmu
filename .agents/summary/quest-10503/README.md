# 任务 10503「提亚马特结界守护者 / Guard Down, Secrets Out」步骤 1 杀怪计数污染与步骤 2 交付对话修复纪要

## 1. 现象与现场证据 (Symptom & Live Evidence)
- **任务编号**：10503（天族 Cygnea / 希哥尼亚 主线使命）。
- **用户反馈**：
  1. 做到“在龙地外庭院和尤碧亚对话”这一步，找到尤碧亚 804705，无法对话（下发通用第 10 页）。
  2. 获取到道具依然不能对话。
  3. 实机执行 `//quest set 10503 START 2` 以后可以对话！
- **现场实测时序日志**：
  ```
  09-19 19:33:52 INFO  [multiThreadIoEventLoopGroup-3-4] c.a.g.n.a.s.SM_QUEST_ACTION - [QUEST-TRACE][S->C] SM_QUEST_ACTION 任务=10503 状态=3 步数=65
  09-19 19:33:58 INFO  [multiThreadIoEventLoopGroup-3-4] c.a.g.n.a.s.SM_QUEST_ACTION - [QUEST-TRACE][S->C] SM_QUEST_ACTION 任务=10503 状态=3 步数=129
  09-19 19:34:00 INFO  [multiThreadIoEventLoopGroup-3-4] c.a.g.n.a.s.SM_QUEST_ACTION - [QUEST-TRACE][S->C] SM_QUEST_ACTION 任务=10503 状态=3 步数=130
  09-19 19:34:13 INFO  [multiThreadIoEventLoopGroup-3-4] c.a.g.n.a.s.SM_DIALOG_WINDOW - [QUEST-TRACE][S->C] SM_DIALOG_WINDOW 玩家=Ww targetObj=0 questId=0 下发页=0
  ```

## 2. 根本原因深度溯源 (Root Cause Analysis)
1. **步数计算与高位残留**：
   - 变量布局：
     ```xml
     <bit-field name="var0" offset="0" width="4" min="0" max="7" persistence="PERSISTENT" scope="LOCAL"/>
     <bit-field name="var1" offset="6" width="2" min="0" max="2" persistence="PERSISTENT" scope="LOCAL"/>
     ```
   - 步骤 1 (s1) 杀 3 只怪阶段：
     - 击杀第 1 只：`var0=1, var1=1` -> `(1 << 6) | 1 = 65`。
     - 击杀第 2 只：`var0=1, var1=2` -> `(2 << 6) | 1 = 129`。
     - 击杀第 3 只（推进转移）：原 XML 在 `s1 -> s2` 中仅执行了 `<set-variable field="var0" value="2"/>`，**未将 `var1` 清零**！
     - 结果：`var0=2, var1=2` -> 打包整型步数 `(2 << 6) | 2 = 130`！
2. **客户端对话判定契约脱节**：
   - Aion 客户端对该任务的 `collect_progress` 声明为 2，客户端严格基于当前任务整型步数 `step == 2`（即纯净的 `var0=2, var1=0`）判定是否进入收集交付步骤。
   - 当服务端下发 `步数=130` 时，客户端判定该任务不在步骤 2，NPC 尤碧亚（804705）头顶不亮任务图标，点击只下发通用第 10 页（任务列表）。
   - 当用户在游戏内执行 `//quest set 10503 START 2` 时，服务端执行 `qs.setQuestVar(2)`，将整型直接覆写为 2（彻底消除了 `var1` 的高位脏数据，变为 `var0=2, var1=0`），服务端下发 `SM_QUEST_ACTION 任务=10503 状态=3 步数=2`。客户端收到纯净的 `2` 后，立即判定命中 `step == 2`，尤碧亚头顶恢复任务标记，对白成功恢复！
3. **交互物同步状态契约**：
   - 采集物 702670 交互完成触发 `USE_OBJECT` 后，原 XML 只有 `<close-dialog/>`，未发送 `<sync-quest-state mode="PACKET_ONLY"/>` 同步包。

## 3. 为什么之前做击杀任务收口的时候没有发现？(Post-Mortem: Why Missed During Kill Task Audit?)
1. **测试用例场景盲区（单一猎杀回访 vs 复合型多阶段任务）**：
   - 之前的击杀任务收口（如 `QuestMonsterProgressContractAuditTest` 覆盖的 11110、1548、15001、15101 等）主要解决的是**单一猎杀回访任务**：
     - 单一猎杀任务流程：接任务 -> 杀怪（如 10 只） -> 杀满后进入“汇报步 (Report Step)” -> 回 NPC 领奖。
     - 核心契约：客户端追踪栏在汇报步**依然需要读取 `SECTION_1` 维持 10/10 的饱和显示**，严禁将计数清零（如果清零，客户端会变成 0/10）。
     - 当时全库收口的所有测试关注点全部集中在“**防止击杀计数在转入汇报步时被提前冲掉**”上。
   - 而 10503 这类任务属于**复合型多阶段任务 (Multi-Stage Hybrid Missions)**：
     - 流程：步骤 1 杀怪 -> **步骤 2 全新收集交付** -> 步骤 3 解读 -> 步骤 4 使用道具……
     - 契约：步骤 2 根本不使用怪兽计数器，客户端声明了 `collect_progress=2`，要求整型步数必须干净为 2。
     - 盲区：因为单一猎杀要求“保留饱和计数”，而复合任务要求“进入非计数步必须清零局部计数”，两者设计目标相反，之前的测试没有把复合任务跨阶段步数纯度纳入断言。
2. **状态机投影 vs 位段底层打包的语义脱节**：
   - XML 编译器校验状态机节点时，只检查 `s2` 节点的投影声明 `<var name="var0" value="2"/>` 是否满足，只要 actions 包含了 `set-variable var0=2`，状态机校验即判定成功；
   - 但底层 `QuestVars` 的位段打包是累积叠加的，编译器过去没有检查未被声明的子字段是否被隐式残留。

## 4. 如何避免下次发生？(Long-term Guardrails)
1. **规范分流（Architecture Patterns Clarification）**：
   - **单一猎杀汇报任务**：进入汇报步保留 `SECTION_1` 饱和计数供客户端展示完成状态。
   - **复合型多阶段任务**：当主阶段 `var0` 推进到后续不相关阶段（收集步、交互步、道具使用步、感应区步）时，**必须显式重置前置阶段使用的所有局部计数器（如 `<set-variable field="var1" value="0"/>`）**。
2. **全服契约门禁锁定 (Automated Gate Test)**：
   - 在 `QuestCollectProgressAlignmentGateTest` 中新增 `multiStageMissionsResetCounterVarsOnEnteringNonCounterStages` 门禁，遍历所有复合型使命任务，强制断言：凡条件判断过 `var1` 的跨阶段转移，若目标阶段不使用 `var1`，actions 必须包含 `set-variable var1=0`，否则构建报错。
3. **上线自愈兜底 (Enter-World Auto-Heal Contract)**：
   - 复合任务各阶段标配 `enter-world` 自愈路由：玩家上线时若处于非计数阶段但检测到 `var1>=1`，即刻静默清零并下发 `PACKET_ONLY` 同步，彻底保障存量脏数据无法阻断客户端交互。

## 5. 本次修改清单 (Changeset Summary)
1. **XML 任务文件（双端同步 src/ 与 aion/）**：
   - `10503.xml`: `s1 -> s2` 增加 `var1=0`，702670 增加 `PACKET_ONLY` 同步，增加 `s2 -> s2` 存量自愈。
   - `10504.xml` / `20504.xml`: 击杀转移清零 `var1=0`，增加自愈与 702671 同步包。
   - `10506.xml`: 击杀转移清零 `var1=0` 及自愈。
   - `10507.xml`: 击杀转移清零 `var1=0, var2=0` 及自愈。
   - `10527.xml` / `20527.xml`: 击杀转移清零 `var1=0` 及自愈。
   - `10528.xml` / `20528.xml`: 击杀转移清零 `var1=0, var2=0` 及自愈。
2. **测试用例与门禁**：
   - `Quest10503ClientDialogAlignmentTest`: 校验 10503 完整契约。
   - `Quest10504ClientDialogAlignmentTest`: 校验 10504 完整契约。
   - `QuestCollectProgressAlignmentGateTest`: 全族群 9 个复合主线清零门禁锁定。

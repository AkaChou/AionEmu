# 任务 10506「Mind Over Matter / 进入进攻回廊」客户端验收记录（ACCEPTED）

```text
quest: 10506（天族 希格尼娅 / Cygnea 主线使命）
user acceptance confirmation: 用户 2026-09-19 回复“验证成功，排查类似问题修复，提交”，确认实机游玩验证成功
server launch mode: 用户管理的服务端实例（本轮未启动、停止或重启服务端）
repository commit: 随本交付批次提交落地
working tree: dirty；严格隔离并保留并行的未暂存/未追踪修改，仅提交本次任务相关的 XML、测试与验收沉淀文档
Aion 5.8 client/data provenance: Aion 5.8 客户端解包数据 Dialogs/10000_19999/quest_q10506.html、docs/quest/client-dialog-mapping/quest-dialog-action-details.csv:712-720
npc template/object: 布仑太 804709；普诺埃 804710；布里特拉进攻回廊入口 702666；布里特拉进攻回廊出口 702667；第48进攻部队参谋兵 236259；被支配的普诺埃 236263
map/instance: Cygnea (希哥尼亚 / 210070000)

steps:
1. 步骤 3 (s2: 去找布里特拉进攻回廊)：
   - 靠近回廊入口触发外界感应区 LF5_SENSORYAREA_Q10506_210070000，步骤自动推进至第 4 步 (s3)；
   - 修复前：s2 错误挂载 702666 USE_OBJECT 与 can-act，导致玩家在未消灭守卫时提前传送进回廊内部；
   - 修复后：s2 移除传送与 can-act 交互，严禁抢跑。
2. 步骤 4 (s3: 消灭第48进攻部队参谋兵 0/2)：
   - 击杀回廊外的 2 只参谋兵 (236259)，满 2 只后推进至第 5 步 (s4, var0=4, var1=0)。
3. 步骤 5 (s4: 通过布里特拉进攻回廊追踪布里特拉)：
   - 修复前：s4 缺失 702666 的 can-act 与 USE_OBJECT，交互被 QuestItemNpcAI2 拦截，无法点击进入回廊；
   - 修复后：右键点击布里特拉进攻回廊入口 (702666) 顺利交互，传送进入回廊内部 (2837.0, 2991.0, 680.0, heading 67)；
   - 回廊内部向前移动触发内部感应区 LF5_SENSORYAREA_Q10506_2_210070000，步骤推进至第 6 步 (s5)。
4. 步骤 6 (s5: 是陷阱！逃出布里特拉进攻回廊，然后和普诺埃对话)：
   - 修复前：s5 缺失 702667 的 can-act 与 USE_OBJECT，无法交互出口逃出回廊；
   - 修复后：右键点击回廊出口 (702667) 传送离开回廊返回外界 (1894.7863, 2455.1982, 336.875, heading 109)，随后与普诺埃 (804710) 对话推进至第 7 步 (s6) 击杀被支配的普诺埃。

source state/status/vars: s2 -> s3 -> s4 -> s5 -> s6
action/page/button: 702666 USE_OBJECT -> teleport (2837.0, 2991.0, 680.0); 702667 USE_OBJECT -> teleport (1894.7863, 2455.1982, 336.875); can-act on s4/s5
expected response: 第 3 步禁止提前传送；第 5 步允许使用 702666 传送进入回廊；第 6 步允许使用 702667 传送离开回廊
actual response: 用户实机游玩验证通过，确认“验证成功，排查类似问题修复，提交”

acceptance status: ACCEPTED
matched Pattern: STATE_GATED_ACTION_ITEM_USE_FLOW (交互物状态门禁与分段推进)
remaining risks: 10506 实机验证通过；全服 2400+ 任务 XML 扫描验证 can-act 与 USE_OBJECT 成对阶段一致性，全服交互物门禁 QuestInteractionObjectCatalogTest (7/7 PASS) 与契约测试 Quest10506ClientDialogAlignmentTest (2/2 PASS) 全绿。
```

## 证据引用
- 根因排查与复盘分析：`.agents/summary/quest-10506-beritra-corridor/README.md`
- 静态契约校验脚本：`.agents/summary/quest-10506-beritra-corridor/verify_10506_contract.py`
- 单元契约测试：`Quest10506ClientDialogAlignmentTest` (2/2 PASS)
- 复合使命变量门禁：`QuestCollectProgressAlignmentGateTest` (6/6 PASS)
- 全服交互物启动门禁：`QuestInteractionObjectCatalogTest` (7/7 PASS)

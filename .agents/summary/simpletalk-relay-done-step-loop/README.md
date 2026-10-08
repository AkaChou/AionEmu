# SimpleTalk 已完成中继步回放死循环（13700 / 13800，2026-10-08 实机）

## 现象

实机 QUEST-TRACE（玩家 Kk，NPC 802350 = LDF4_Advance_Elger_E）：

```
页10(questId=0) → 31 → 1352 → 1353 → 10000 → 关窗(targetObj=0,questId=0,页0)
（7 秒后）页10 → 31 → 1352 → 1353 → 10000 → 关窗   ← 无限循环，任务无法交付
13800 同症状（用户复现报告）
```

## 根因

`SimpleTalkHandler` 中继分支（`questEngine/tablelane/SimpleTalkHandler.java`）对任务行打开
（31/26）只处理两种态：

- 未轮到（`vars < step-1`）→ 静默 `return false`；
- 当前步（`vars == step-1`）→ 发步页。

**已完成的步（`vars >= step`）落入「发步页并 return true」**——把后面的报告分支（31 →
reportConfirmPage → REWARD）永久遮蔽。13700/13800 的末位中继与交付恰好是同一名字节点：

- 13700：talk_npc1 = reward_npc = LDF4_Advance_Elger_E（802350），单步；
- 13800：talk_npc2 = reward_npc = LDF5_Fortress_Alphion_E（802431），两步。

于是步完成后 31 恒回步页（1352/1693），10000/10001 重放只走「关窗兜底」，玩家永远到不了
报告页 select5(2375) → 无法交付 → 死循环。

## 真端证据（真端权威口径）

- 退役 XML（git 4ede058c0^，`quest_definition/quests/13700.xml`）：
  `NPC_REPORT npc-id=802350 source=started target=reward page=SELECT5` —— 802350 上 31 的
  目标就是 SELECT5 报告页，不是步页回放。
- 退役 XML 13800：REWARD 态 `QUEST_SELECT → SELECT5@802431`；SETPRO2 步进后节点越过 Alphion
  的步页。
- 客户端页契约（`quest_dialog/client_dialog_contract.tsv`）：13700/13800 均声明 select5=2375；
  reportConfirmPage 的缺陷 S 规则（跳过被中继占用的 SELECT2）同样指向 2375。
- 库内先例：`SimpleUseItemHandler` 对同形 bug（1559：talk_npc1 = reward_npc）已按
  「中继已走完让位交付面」修复并有实机验收 —— 本次是把 SimpleTalk 对齐到该先例。

## 修复

| 文件 | 改动 |
|---|---|
| `SimpleTalkHandler.java` | 中继 31/26 分支：`vars >= step → continue` 落穿（后续步/报告分支/默认处理） |
| `SimpleCollectItemHandler.java` | 同形分支：`talkStep(state) != step - 1 → return false`（潜在缺陷，现存 5 个有链行无同节点行，预防性同修） |
| `SimpleItemPlayHandler.java` | 同 Talk 形：`vars >= step → continue` |
| `SimpleTalkRelayDoneStepFallthroughTest.java`（新增） | 13700 全链 + 13800 中链/末位中继回归：修复前 31 恒回步页（死循环），修复后 31 → 2375 → 1009 → REWARD + 奖励窗页 5 |

UseItem（1559 先例）本就正确；Combine/SerialHunt 无中继步页面，不受影响。

## 验证状态

- ✅ 2026-10-08 经 IDEA MCP 授权运行，9 个测试类全绿（exit 0，零失败）：
  `SimpleTalkRelayDoneStepFallthroughTest`（新增回归 2/2）、`Quest1913ProductionFlowTest`、
  `Quest1192StepChainContractTest`、`Quest1553ClientDialogAlignmentTest`、
  `Quest19004RetailAlignmentTest`、`SimpleCollectItemNativeFamilyGateTest`、
  `SimpleUseItemNativeFamilyGateTest`、`QuestRepeatLifecycleTest`、`Quest1112ProductionFlowTest`。
  （首跑 13800 用例 NPE 于生产背包端口 → 换 `RecordingInventory` 后通过；次跑吃旧字节码行号
  未漂移 → 重跑即绿，IDEA MCP 已知现象。）
- PENDING：实机复测（服务端为 IDEA 常驻进程，改动需重启后验证；复测前核对进程启动时间晚于编译时间）。

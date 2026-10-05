# Client Action State & Interaction Sync (客户端动作状态与交互同步)

本文档记录客户端本地「动作状态」对交互的封锁机制，以及服务端对客户端收尾/取消请求的应答义务。
客户端不在仓库内，结论来自 5.8 客户端 `bin64/Game.dll` 的反编译证据与实机复现。

> Pattern IDs: `CAS-001`
> card_status: ACTIVE; 客户端结论绑定 5.8 客户端 bin64/Game.dll（ImageBase 0x10000000）与玩家物品使用路径
> scope: 客户端本地动作状态（「使用物品」族）的解除通道、本地拒绝提示（UI 串 901564）与 CM_USE_ITEM 收尾应答契约
> last_reviewed: 2026-10-05

---

## [CAS-001] 客户端动作状态只能被服务端收尾包解除——收尾请求不得静默丢包
<!-- pattern-metadata
status: CONFIRMED
scope: 玩家「使用物品」动作状态（客户端 `[player+0xC20]` ∈ {0x416, 0x41f, 0x420, 0x443} 且 `[player+0xC94]`≠0）与 CM_USE_ITEM 收尾/取消请求的服务端应答；守护灵契约书等由客户端窗口驱动的物品使用
first_seen: 2026-10-05
last_verified: 2026-10-05
symptom: 客户端提示「使用物品时无法进行的动作」（UI 串 901564 STR_CANNOT_DO_WHILE_USING_ITEM）后点击 NPC/交互无反应，移动角色才恢复；典型复现为守护灵契约书完成契约→关闭宠物精灵窗口→不移动即卡；服务端日志无异常，也没有对应的拒绝消息
root_cause: 「使用物品时无法进行的动作」是客户端本地判定：客户端仅在 `[player+0xC94]`≠0 且 `[player+0xC20]` ∈ {0x416, 0x41f, 0x420, 0x443}（「使用物品」动作族）时显示 UI 串 901564，本服不发送该串。该动作状态由服务端 SM_ITEM_USAGE_ANIMATION（opcode 0xB7）的结束帧解除（result=1 成功 / 3 取消），否则要等角色移动由客户端本地中止。客户端在关闭契约窗口时会为契约书再发一次 CM_USE_ITEM 作为该次使用的收尾请求；但契约完成后契约书已消耗（item==null）且该模板本无 <actions>，CM_USE_ITEM 在这些分支静默 return，客户端等不到收尾帧而卡在动作状态。同源 AL-Game 在 item==null 时走 cancelUseItem()，真端 C_USE_ITEM(objId=0) 亦路由到 User::CancelUseItem——本仓库现代化改造时丢掉了这条取消分支；且 Creature.usingItem 此前只写不读。
fix_or_guardrail: CM_USE_ITEM 对「解析不到物品」与「契约物品（MinionService.isMinionContract）」两类收尾请求统一调用 respondCanceledUse：ITEM_USE 任务在飞时不发包（该任务自身的结束动画会收尾），否则以服务端记录的 usingItem 为准广播 SM_ITEM_USAGE_ANIMATION(playerObjId, itemObjId, itemId, 0, 3, 0) 并 cancelUseItem() 清除使用中状态。护栏：客户端本地动作状态的收尾/取消请求必须有应答；为物品动作新增分支前先判定该请求是否是客户端为解除动作状态而发的收尾包，禁止在物品解析失败处直接 return。
evidence: src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_USE_ITEM.java:106（item==null 与契约物品的收尾应答；:256 为 respondCanceledUse）; src/main/java/com/aionemu/gameserver/services/toypet/MinionService.java:273; src/main/java/com/aionemu/gameserver/network/aion/serverpackets/SM_ITEM_USAGE_ANIMATION.java:83; src/main/java/com/aionemu/gameserver/controllers/PlayerController.java:906; .agents/summary/minion-contract-item-state/2026-10-05-minion-contract-stuck-item-state.zh-CN.md（客户端 901564 判定点、动作状态字段读写与真端对照）
validation: runtime；2026-10-05 用户实机：契约完成→关闭宠物精灵窗口→不移动点击 NPC 正常（修复前同步骤稳定复现）; client；客户端 Game.dll 反编译证得 901564 的 6 处判定条件与 `[player+0xC20]`/`[player+0xC94]` 读写; static；IDE 检查 CM_USE_ITEM.java 无诊断错误；未跑构建/测试
boundaries: 客户端结论绑定 5.8 客户端 bin64/Game.dll（ImageBase 0x10000000）；施法、采集等其它动作族未逐一验证；取消动画只广播给发起者及其可见者；usingItem 仅在取消路径清除（成功完成时不清，isUsingItem()/getUsingItemId() 目前无调用者），需要精确追踪「使用中物品」时须另行对齐真端 User::OnUseItemTimerExpired；真端 MinionService 侧收尾顺序差异未改动（保留为边界观察）
superseded_by: none
first_check: CM_USE_ITEM 中所有静默 return 分支（item==null / itemActions==null / actions 为空）是否覆盖客户端收尾包；Creature.usingItem 的写入与清除点；客户端聊天日志（developer.properties 的 show.packetnames.inchat.enable）中 CM_USE_ITEM 与提示的先后
keywords: 使用物品时无法进行的动作、901564、STR_CANNOT_DO_WHILE_USING_ITEM、卡动作状态、移动才恢复、CM_USE_ITEM、关闭契约窗口、取消动画、SM_ITEM_USAGE_ANIMATION、契约书、宠物精灵、守护灵
-->

**规则**：客户端进入「使用物品」等本地动作状态后，该状态只能由**服务端收尾包**
（`SM_ITEM_USAGE_ANIMATION` result=1 成功 / 3 取消）或**角色移动**在客户端本地解除。
客户端为解除该状态而发来的收尾/取消请求（`CM_USE_ITEM`）**必须有应答**——解析不到物品、
物品无动作等情形都要回取消动画，静默丢包会让玩家卡在状态里，直到移动前所有交互都被本地拒绝。

- **客户端判定（901564 的 6 处引用条件一致）**：

  ```c
  // FUN_10040e20 / FUN_10067e80 / FUN_102d1b10 / FUN_10364620 / FUN_103c7060 / FUN_1040cd70
  if (*(char *)(player + 0xc94) != '\0') {
      if (*(int *)(player + 0xc20) == 0x416) { show(901564); return; }   // 0x41f / 0x420 / 0x443 同族
  }
  ```

  `[player+0xC20]` 由 `FUN_1073d280` 写入、动作脚本跑完经 `FUN_106d1120 → FUN_10560d90` 清 0；
  提示文本 901564 来自客户端 `L10N data.pak → Strings/client_strings_ui.xml`，本服务端不发它。

- **本服修复点**：`CM_USE_ITEM` 的 `item == null` 与 `MinionService.isMinionContract(item)` 两个分支
  统一走 `respondCanceledUse(...)`（`CM_USE_ITEM.java:106` / `:256`）：有 `ITEM_USE` 任务在飞时不发包
  （任务自身的结束动画会收尾）；否则广播 result=3 的取消动画并 `cancelUseItem()` 清状态。
  契约本身仍由 `CM_MINIONS(action=0)` 驱动，收尾请求只做取消应答，不重复触发契约。

- **对齐依据**：AL-Game（同源 2019 实现）`CM_USE_ITEM` 的 `item == null → cancelUseItem(); onMove(); return;`
  分支；真端 `C_USE_ITEM` 核心（`Server64.c FUN_14054ab10`）在 objId 解析不到时走 `User::CancelUseItem`
  （`FUN_14054e8a0`），`S_ITEM_USAGE_ANIMATION`（opcode 0xB7）result 语义一致。

- **反向排查**：若玩家报「提示 X 后不能交互」，先确认该提示是否为客户端本地串（而非服务端消息），
  再查当前处于哪个客户端动作状态、对应收尾包是否被服务端静默丢弃。同族契约见 [CV-001]
  （客户端血条只认 SM_ATTACK_STATUS 的百分比）——都是「客户端只认特定包/字段」的同步契约。

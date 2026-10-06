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
scope: 玩家「使用物品」动作状态（客户端 `[player+0xC20]` ∈ {0x416, 0x41f, 0x420, 0x443} 且 `[player+0xC94]`≠0）的收尾契约与 CM_USE_ITEM 收尾/取消请求的服务端应答；守护灵契约书等由客户端窗口驱动的物品使用
first_seen: 2026-10-05
last_verified: 2026-10-06
symptom: 客户端提示「使用物品时无法进行的动作」（UI 串 901564 STR_CANNOT_DO_WHILE_USING_ITEM）后点击 NPC/交互无反应，移动角色才恢复；典型复现为守护灵契约书契约完成后（关窗与否都一样）不移动即卡；服务端日志无异常，也没有对应的拒绝消息
root_cause: 「使用物品时无法进行的动作」是客户端本地门禁：`[player+0xC94]`≠0 且 `[player+0xC20]` ∈ {0x416, 0x41f, 0x420, 0x443} 时显示 UI 串 901564。该动作族的收尾规则（客户端 `FUN_10733220(player, itemId, p3, end, time, p6)`，字段映射由函数体自证；0xB7 字面解析函数未定位，全链 vtable 间接分发）：起始帧 end=0 播放 0x416 并把 time(ms) 写入 `[player+0xC38]` 动作计时器，到期由动作系统（`FUN_106d1120 → FUN_10560d90`）清 `[player+0xC20]` 自动解卡；**end=1/2 落进通用 else 分支 → 经 `(+0x4e8)(player, 0x416, 1, 1, …)` 强制重播 0x416 且不重新计时 → 动作永不到期，永久卡住**——服务端在计时到点时补发 end=1 成功帧，恰好把刚要自然结束的动作重启；只有 end=3 会 play+stop 该动作 + 播 -1 空动作 + 清 `[player+0xC38]`。契约书（190080005…）在客户端无专属处理器（`FUN_10733220` 的 itemId 范围分支只覆盖 190.1M–190.2M / 166xxx / 165xxx / 167xxx / 168xxx / 140xxx）→ 走通用路径；收尾端不查背包（无 itemObjId 参数、无 null 早退）。另：客户端对「解析不到物品」的收尾请求（关窗时补发）没有本地回退，只能等服务端应答帧——服务端静默丢包时玩家只能靠移动解卡。
fix_or_guardrail: 契约/物品使用收尾一律发 `SM_ITEM_USAGE_ANIMATION(…, time=0, end=3)`（`MinionService.broadcastContractUseEnd`、`CM_USE_ITEM.respondCanceledUse`），成功与失败路径都一样；**禁止对「使用物品」族动作发 end=1/end=2 收尾帧**（客户端会重播动作导致永久卡死）；起始帧保持 end=0 + time=使用时长（客户端计时兜底）；ITEM_USE 任务在飞时不重复发包；客户端的收尾/取消请求必须有应答，禁止在物品解析失败处直接 return。
evidence: src/main/java/com/aionemu/gameserver/services/toypet/MinionService.java:272（broadcastContractUseEnd：成功/失败统一 end=3）; src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_USE_ITEM.java:106（:256 respondCanceledUse）; src/main/java/com/aionemu/gameserver/network/aion/serverpackets/SM_ITEM_USAGE_ANIMATION.java:83; src/main/java/com/aionemu/gameserver/controllers/PlayerController.java:906; .agents/summary/minion-contract-item-state/2026-10-05-minion-contract-stuck-item-state.zh-CN.md; .agents/summary/minion-contract-item-state/2026-10-06-contract-completion-motion-not-cleared.zh-CN.md（客户端 FUN_10733220 证据链与真端对照）
validation: client；客户端 Game.dll 反编译证得 `FUN_10733220` 的 end 语义（end=3 唯一停止 / end=1-2 重播）、901564 六处判定条件与 `[player+0xC20]`/`[player+0xC94]` 读写; runtime；2026-10-05 用户实机：关窗补发的收尾请求（end=3 应答）可解除；2026-10-06 用户实机复测通过：契约完成后（不移动、不关窗）点 NPC 正常; static；IDE 检查无诊断错误；未跑构建/测试
boundaries: 客户端结论绑定 5.8 客户端 bin64/Game.dll（ImageBase 0x10000000）；施法/采集等其它动作族未逐一验证；end=3 分支有守卫 `[player+0xC94]==0 || [player+0xC20]!=0x1e`——当前动作是 0x1e 时整帧被忽略（契约流程走 0x416 不受影响）；end=3 幂等（重复发只多播一次空动作）；`QuestEngine.scheduleTypedItemPlay` 同形状（start(time) → time 毫秒后发 end=1）未验证、未改动；真端 MinionService 收尾顺序差异已被「统一 end=3」结论取代
superseded_by: none
first_check: 契约/物品使用路径是否发了 end=1 或 end=2 收尾帧（应为 end=3）；CM_USE_ITEM 中所有静默 return 分支（item==null / itemActions==null / actions 为空）是否覆盖客户端收尾包；客户端聊天日志（developer.properties 的 show.packetnames.inchat.enable）中 CM_USE_ITEM 与提示的先后
keywords: 使用物品时无法进行的动作、901564、STR_CANNOT_DO_WHILE_USING_ITEM、卡动作状态、移动才恢复、CM_USE_ITEM、关闭契约窗口、取消动画、SM_ITEM_USAGE_ANIMATION、end=1 重播动作、end=3 停止、契约书、宠物精灵、守护灵
-->

**规则**：客户端「使用物品」族动作（0x416/0x41f/0x420/0x443）由**起始帧的 `time` 计时**兜底
自然结束（到期动作系统清 `[player+0xC20]`）；服务端要收尾**只能用 `end=3`**（play+stop + 清计时）。
**`end=1`/`end=2` 不是收尾**——客户端会经 `(+0x4e8)(player, 0x416, 1, 1, …)` 重播该动作且不重新计时，
动作永不到期，玩家在移动前所有交互都被本地拒绝（901564）。客户端为解除该状态而发来的收尾/取消
请求（`CM_USE_ITEM`）**必须有应答**：客户端本地没有任何回退，静默丢包同样会卡到移动为止。

- **客户端判定（901564 的 6 处引用条件一致）**：

  ```c
  // FUN_10040e20 / FUN_10067e80 / FUN_102d1b10 / FUN_10364620 / FUN_103c7060 / FUN_1040cd70
  if (*(char *)(player + 0xc94) != '\0') {
      if (*(int *)(player + 0xc20) == 0x416) { show(901564); return; }   // 0x41f / 0x420 / 0x443 同族
  }
  ```

  `[player+0xC20]` 由 `FUN_1073d280` 写入、动作脚本跑完经 `FUN_106d1120 → FUN_10560d90` 清 0；
  提示文本 901564 来自客户端 `L10N data.pak → Strings/client_strings_ui.xml`，本服务端不发它。

- **本服落点**：`MinionService.broadcastContractUseEnd(...)`（`MinionService.java:272`）——契约使用
  的成功与全部失败路径统一发 `end=3`；`CM_USE_ITEM.respondCanceledUse(...)`（`CM_USE_ITEM.java:256`）
  ——关窗补发的收尾请求同样用 `end=3`，有 `ITEM_USE` 任务在飞时不发包（任务自身会收尾）。
  契约本身仍由 `CM_MINIONS(action=0)` 驱动，收尾请求只做应答，不重复触发契约。

- **对齐依据**：真端 `FamiliarContractScroll::OnUseItemTimerExpired`（`Server64.c FUN_140a29d20`）
  = `User::ContractFamiliar`（加守护灵并下发）→ `User::UseItem_Post`（扣物品）→ **最后**
  `World::BroadcastUseItem`（`FUN_1406944a0`）；AL-Game 的 `item == null → cancelUseItem()` 分支与真端
  `C_USE_ITEM(objId=0) → User::CancelUseItem` 说明「解析不到物品的收尾请求按取消应答」的语义。

- **反向排查**：若玩家报「提示 X 后不能交互」，先确认该提示是否为客户端本地串（而非服务端消息），
  再查当前处于哪个客户端动作状态、对应收尾包是否被服务端静默丢弃。同族契约见 [CV-001]
  （客户端血条只认 SM_ATTACK_STATUS 的百分比）——都是「客户端只认特定包/字段」的同步契约。

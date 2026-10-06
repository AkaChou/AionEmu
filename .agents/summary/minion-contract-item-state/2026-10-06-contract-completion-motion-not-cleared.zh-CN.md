# 契约完成后仍卡「使用物品」动作：成功帧 end=1 在客户端重播动作（第三轮，已修复并客户端实机验证通过）

日期：2026-10-06。关联：`2026-10-05-minion-contract-stuck-item-state.zh-CN.md`（第一轮：关窗收尾请求无应答）。

状态：**已修复**，2026-10-06 用户客户端实机验证通过（契约完成 → 不移动、不关窗 → 点 NPC 正常）。

## 1. 现象

> 「打开宠物精灵窗口，使用契约书，契约完成后，获取到精灵宠物，这时候不关闭契约窗口，
> 点击 npc，会提示『使用物品时无法进行的动作』」（不移动、不关窗时必须能交互）

聊天时间线（用户实机）：使用中提示 ×1 → `获得了宠物精灵塞伊兰。` → 完成后提示 ×1（仍卡）。

## 2. 根因（客户端 Game.dll 只读逆向，VA；Ghidra 工程 GameClient64）

**S_USE_ITEM（0xB7）的「使用结果」处理函数 `FUN_10733220(player, itemId, p3, end, time, p6)`**
（字段映射由函数体自证：param_2=itemId、param_4=end、param_5=time(ushort)；0xB7 字面解析函数未定位——
全链 vtable 间接分发，不影响结论）：

```c
if (param_4 == 3) {                       // end=3 = 停止（唯一）
  if ([player+0xC94]==0 || [player+0xC20]!=0x1e) {
    play(0x416); stop(0x416);             // (**(+0x668))/(**(+0x670))
    FUN_107308a0(player);                 // (+0x4e8)(player, -1, 1, 1, …) 播 -1 空动作 → [0xC20] 归零
  }
  player[0x187] = 0;                      // 清 [player+0xC38]（动作计时）
} else {                                  // end=0/1/2 同分支
  if ([player+0x2E8] < 9 && (… || motion==0x416)) 
    (+0x4e8)(player, 0x416, 1, 1, …);     // ★ 强制【重播】0x416，且不更新 [0xC38]
  if (end ∈ {0, 0x13, 0x17}) {            // 只有这些 end 才写计时
    [player+0xC2C] = p3; [player+0xC38] = time; [player+0xC3C] = tick;
  }
}
```

- **起始帧（end=0）**：播放 0x416 并把 `time` 写入 `[player+0xC38]`（默认 1500ms）——客户端动作计时器；
  到期由动作系统 `FUN_106d1120 → FUN_10560d90` 清 `[player+0xC20]=0`（自动解卡路径）。
- **结束帧 end=1/2**：落进 else 分支 → **重播 0x416 且不重新计时** → 动作永不到期 ⇒ **永久卡住**
  （交互 gate：`[player+0xC94]!=0 && [player+0xC20]∈{0x416,0x41f,0x420,0x443}` → UI 串 901564）。
  本 bug 时序正是：start(1500ms) 计时到点时服务端发来 end=1 → 重播 → 计时失效。
- **end=3**：play+stop 0x416 + 播 -1 空动作 + 清计时 → 解卡（与第一轮实机「关窗取消帧能解除」一致）。
- **契约书段无特判**：契约书 id（190080005…190080021、190089999）不匹配 `FUN_10733220` 的任何
  itemId 范围分支（范围以补码加法编码：`param_2 + 0xf4ab4de0 < 100000` 即 [190100000,190200000)；
  另有 166xxx/165xxx/167xxx/168xxx/140xxx 段）→ **走通用路径**，等价于任意普通物品。
- **接收端不查背包**：`FUN_10733220` 只接 itemId（模板 id），无 itemObjId、无 null 早退 ⇒
  服务器先扣物品再发收尾帧不影响生效（顺序非本因）。

## 3. 修复（`MinionService.addMinion`，已实施；2026-10-06 客户端实机验证通过）

新增 `broadcastContractUseEnd(player, itemObjId, itemId)`：契约使用的收尾一律发 **end=3**（time=0）；
**成功路径不再发 end=1**（下发 `SM_MINIONS(1,…)` 之后发停止帧），失败/中止路径（数量上限拒绝、
扣除失败、创建失败回滚、玩家中止）由 end=2 一并改为 end=3（原来同样会重播动作、同样会卡）。
起始帧保持 `(…, 1500, 0)` 不变。
回归断言：`MinionServiceTest.endsTheContractUseWithTheClientStopFrameOnly`（源码级：成功后必有收尾、
类内不得再出现 `end=1`/`end=2` 帧）；测试未运行（按规则需授权）。

**同根因隐患（未动）**：`QuestEngine.scheduleTypedItemPlay` 形状相同（start(time) → time 毫秒后发 end=1），
按同一客户端逻辑可能同样卡；但任务物品部分落在客户端特殊物品段（另有分支），需单独验证再决定。

## 4. 证据来源

- 客户端逆向（子代理，只读 `-noanalysis -readOnly`）：`FUN_10733220` / `FUN_107308a0` / `FUN_10560d90` /
  `FUN_10732580`(0x1c) / `FUN_10732c70`(0x1e) / `FUN_1077eb10`(清 0xC94)；`FindFieldWrites 0xC20/0xC94`。
- 真端对照：`FamiliarContractScroll::OnUseItemTimerExpired`（140a29d20）= 加守护灵并下发 →
  `User::UseItem_Post`（14054cc90）→ **最后** `World::BroadcastUseItem`（1406944a0，result=1/2）；
  计时器 `User::AddUseItemTimer`（14054e010）。
- 工具：`.agents/summary/minion-contract-item-state/tools/`（FindScalarXrefs / DecompileAt /
  FindFieldWrites / FindCallers，只读）。

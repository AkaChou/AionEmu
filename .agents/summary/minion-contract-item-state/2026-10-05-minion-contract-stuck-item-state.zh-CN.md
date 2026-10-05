# 守护灵契约后「使用物品时无法进行的动作」：关窗收尾请求无应答卡住客户端动作状态（已修复，客户端实机验证通过）

状态：**已修复**（2026-10-05）。修复文件 `src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_USE_ITEM.java`；
可复用结论已提炼为 memory-bank 模式 `CAS-001`（`.agents/memory-bank/patterns/client-action-state.md`）。

## 1. 报障

使用守护灵契约书完成契约 → 关闭宠物精灵界面 → **不移动**角色点击 NPC → 客户端提示
「使用物品时无法进行的动作」。移动后可恢复。

## 2. 现象确认（用户实机聊天日志，当时已开启封包名显示）

```text
0x01D1 : CM_MINIONS
获得了宠物精灵哈梅伦。
0x00EC : CM_USE_ITEM
使用物品时无法进行的动作。   ×2
```

用户确认：该 `CM_USE_ITEM` 是**关闭宠物精灵契约窗口**时发出的（不是点 NPC、也不是点物品）。

## 3. 根因（已确认）

1. **该提示是客户端本地判定，不是服务端消息。**
   - 提示文本 = 客户端 UI 字符串 **901564 `STR_CANNOT_DO_WHILE_USING_ITEM`**
     （客户端 `L10N/CHS/Data/data.pak → Strings/client_strings_ui.xml`，zip 可直接读取）。
   - 本仓库服务端没有任何地方发送 901564（全库 grep：`901564` / `0xdc1bc` / `STR_CANNOT_DO_WHILE` 均无生产引用）；
     `CM_SHOW_DIALOG` / `NpcController.onDialogRequest` 也没有「使用物品中」拦截。

2. **客户端判定位置（bin64/Game.dll，Ghidra 工程 `GameClient64`，地址为 VA）。**
   全库仅 6 处引用 901564，条件一致：

   ```c
   // FUN_10040e20 / FUN_10067e80 / FUN_102d1b10 / FUN_10364620 / FUN_103c7060 / FUN_1040cd70
   if (*(char *)(player + 0xc94) != '\0') {
       if (*(int *)(player + 0xc20) == 0x416) { show(901564); return; }   // 0x41f / 0x420 / 0x443 同族
   }
   ```

   `[player+0xC20]` 是客户端「当前动作/动作脚本」状态 id（`FUN_1073d280` 写入、动作脚本跑完
   `FUN_106d1120 → FUN_10560d90` 清 0）。0x416/0x41f/0x420/0x443 为「使用物品」族动作。
   ⇒ **契约之后客户端的「使用物品」动作一直没结束**，移动才会中止它。

3. **契约状态由服务端收尾包解除，而本服在收尾请求上静默丢包——这就是卡的成因。**
   - 客户端在**关闭契约窗口**时会为契约书再发一次 `CM_USE_ITEM`，作为该次使用的收尾请求。
   - 契约完成后契约书已消耗（解析为 `item == null`），且该物品模板本无 `<actions>`
     （`itemActions == null`）；本服 `CM_USE_ITEM` 在这两处**静默 return**，客户端等不到
     `SM_ITEM_USAGE_ANIMATION` 收尾帧（result=1 成功 / 3 取消），「使用物品」动作状态永不结束。
   - 同源 AL-Game（2019 构建）在 `item == null` 时走 `cancelUseItem()`；真端 `C_USE_ITEM` 核心
     在 objId 解析不到时也路由到 `User::CancelUseItem`（`Server64.c`）。本仓库现代化改造时丢掉了这条取消分支。
   - `Creature.usingItem` 此前**只写不读**（唯一清点在 `PlayerController.cancelUseItem`），成功完成时不清。

4. **已排除**：服务端动画包本身与 AL-Game `writeImpl` 逐字节一致、opcode 0xB7 与真端一致、
   失败/取消 result 语义一致；契约时 `CM_USE_ITEM` 与 `CM_MINIONS` 的顺序差异不是根因
   （真端 `MinionService` 侧顺序差异保留为边界观察，未改动）。

## 4. 修复（已实施）

`CM_USE_ITEM` 对两类收尾请求统一应答取消动画：

```java
if (item == null) {                       // 解析不到物品（契约书已消耗等）
    respondCanceledUse(player, uniqueItemId, 0);
    return;
}
if (MinionService.isMinionContract(item)) { // 契约书的收尾请求；契约本身由 CM_MINIONS(action=0) 驱动
    respondCanceledUse(player, item.getObjectId(), item.getItemTemplate().getTemplateId());
    return;
}
```

`respondCanceledUse(...)`：若 `ITEM_USE` 任务在飞（真正使用/契约进行中）则不发包——该任务自身的
结束动画会收尾；否则以服务端记录的 `usingItem` 为准广播
`SM_ITEM_USAGE_ANIMATION(playerObjId, itemObjId, itemId, 0, 3, 0)` 并 `cancelUseItem()` 清除使用中状态。

即：**客户端本地动作状态的收尾/取消请求必须有应答，不允许静默丢弃。**
未采用的备选（记录备查）：A. `MinionService.addMinion` 收尾改为真端顺序（加守护灵→扣物品→最后播结束动画）
并成功时清 `usingItem`；B. 契约物品改走通用「使用物品」管线——本症状下均非必需，未实施。

## 5. 验证

- **客户端实机（2026-10-05，用户）**：契约完成 → 关闭宠物精灵窗口 → 不移动点击 NPC 正常，提示消失。✅
- static：IDE 检查 `CM_USE_ITEM.java` 无诊断错误。
- 未跑构建/测试（按 AGENTS.md 未获授权）。
- 诊断期间临时开启的封包日志（`src/main/resources/aion/config/administration/developer.properties`
  三处：`showpackets.enable` / `show.packetnames.inchat.enable` / `filter.packets.inchat`）**已全部回退默认**
  （`git diff` 为空）。

## 6. 工具与证据

- 真端零售证据：反编译 `Server64.c`（`<真端根>/server58/MainServer_Server64/`）中
  `FUN_14054ab10`（C_USE_ITEM 核心，objId=0 → `User::CancelUseItem`）、`FUN_14054e8a0`（`User::CancelUseItem`）。
- Ghidra 工程（只读 `-readOnly`）：`<真端根>/server58/ghidra-projects/GameClient64`
  （客户端 bin64/Game.dll，ImageBase 0x10000000，VA = 文件偏移 + 0x10000000）。
- 本轮脚本（只读）：`tools/`（`FindScalarXrefs` / `DecompileAt` / `FindFieldWrites` / `FindCallers`）；
  运行：`analyzeHeadless <ghidra-projects> GameClient64 -process Game.dll -noanalysis -readOnly -scriptPath <tools> -postScript <脚本> <参数>`。
- 客户端 UI 串来源：客户端 `L10N/CHS/Data/data.pak → Strings/client_strings_ui.xml`（zip 可直接读取）。
- 旧版同源实现对照：AL-Game `CM_USE_ITEM`（`item == null → cancelUseItem(); onMove(); return;`）。

# 极乐世界传送等待问题处理方案

> 状态：仅记录方案，未实施
>
> 日期：2026-09-14
>
> 范围：天族极乐世界传送员；魔族对应传送员的处理方式相同

## 1. 现象与结论

刚进入极乐世界后，通过空间传送师打开原版传送地图并选择其他区域时，客户端会先等待十几秒，并显示：

```xml
<id>1401400</id>
<name>STR_MSG_HOUSING_WAIT_TO_TELEPORT</name>
<body>%0秒后，将进行空间移动。</body>
```

结论：

- `1401400` 是客户端本地字符串，不是服务端设置的传送冷却配置。字符串名中的 `HOUSING` 只是客户端资源命名，不能据此判断服务端使用了 Housing 逻辑。
- 原版传送地图的等待判断发生在客户端提交 `CM_TELEPORT_SELECT` 之前。服务端无法通过修改费用、任务条件或 `TeleportService2` 来取消这段本地等待。
- 如果必须保留原版传送地图界面并只删除等待，需要修改对应版本的客户端 `Game.dll`。
- 如果不修改客户端，可以改为服务端对话/自定义菜单选择目的地，再直接调用现有传送服务；代价是绕过原版地图界面。

## 2. 当前服务端传送链路

已确认的代码路径如下：

```text
NPC 对话 dialogId=44
    -> DialogService
    -> TeleportService2.showMap(...)
    -> SM_TELEPORT_MAP
    -> 客户端选择目的地
    -> CM_TELEPORT_SELECT
    -> TeleportService2.teleport(...)
```

相关位置：

- `src/main/java/com/aionemu/gameserver/services/DialogService.java:673-719`：通用飞行/传送对话，默认调用 `TeleportService2.showMap`。
- `src/main/java/com/aionemu/gameserver/services/teleport/TeleportService2.java:694-707`：检查飞行状态和敌对 NPC 后发送 `SM_TELEPORT_MAP`。
- `src/main/java/com/aionemu/gameserver/network/aion/serverpackets/SM_TELEPORT_MAP.java:49-75`：写入目标 NPC、传送模板，并按配置写入目的地列表。
- `src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_TELEPORT_SELECT.java:36-56`：读取 `targetObjectId`、`locId`，确认 NPC 可见且在交互范围内后调用传送服务。
- `src/main/java/com/aionemu/gameserver/services/teleport/TeleportService2.java:111-181`：执行阵营、路线、任务、基纳和最终位置校验，然后完成普通传送或飞行传送。

天族极乐世界传送员当前模板为 `npc_ids="203726"`、`teleportId="1"`，路线定义在：
`src/main/resources/aion/data/static_data/npc_teleporter.xml:6-20`。

魔族对应模板为 `npc_ids="204191"`、`teleportId="50"`，位于同文件 `:161-175`。

## 3. 为什么服务端配置不能直接去掉等待

当前 5.8 客户端的反汇编证据显示，传送地图处理函数会把内部目的地计数与 `0x0A`（十个目的地）比较：

```text
cmp dword ptr [rbx + 0x564], 0x0A
jb  <正常继续分支>
...
mov edx, 0x156238       ; 字符串 ID 1401400
```

计数达到十个时会进入客户端本地计时/提示分支。这个分支在 `CM_TELEPORT_SELECT` 发出之前执行，所以以下修改不能解决等待本身：

- 修改 `npc_teleporter.xml` 的价格、任务或目的地坐标。
- 修改 `teleport_location.xml`。
- 修改服务端 `STR_MSG_HOUSING_WAIT_TO_TELEPORT`。仓库服务端未找到该客户端字符串 ID；即使把客户端提示文字隐藏，也不会取消计时逻辑。
- 只修改 `CM_TELEPORT_SELECT` 或 `TeleportService2.teleport`。客户端未提交选择时，这些代码不会被调用。

## 4. 方案 A：不改客户端，改用服务端对话菜单

适用条件：可以接受不显示原版传送地图，优先保持所有服务端规则有效。

后续实施步骤：

1. 在 `DialogService` 的通用传送分支中，为 `npcId=203726` 增加专用入口；魔族如需同步处理，再加入 `npcId=204191`。
2. 不再为这两个 NPC 调用 `TeleportService2.showMap`，改为发送一个专用对话页面或目的地菜单。具体页面 ID 和选项值需要结合当前客户端 HTML/对话资源确认。
3. 玩家选择目的地后，取得该 NPC 的 `TeleporterTemplate`，调用已有服务：

   ```java
   TeleporterTemplate template =
       DataManager.TELEPORTER_DATA.getTeleporterTemplateByNpcId(npc.getNpcId());
   TeleportService2.teleport(
       template, selectedLocId, player, npc, TeleportAnimation.JUMP_ANIMATION);
   ```

4. `selectedLocId` 必须来自该 NPC 的模板，不接受客户端任意传入的地点 ID。这样可以继续复用现有的阵营、路线存在性、任务、基纳和地图位置校验。
5. 不要直接用 `TeleportService2.teleportTo` 替代上述调用，否则会绕过传送费用和路线任务条件。

优点：不需要给玩家分发修改版客户端，等待逻辑不会再被原版地图界面触发。

代价：不再使用原版传送地图；需要设计对话页面、选项映射和页面关闭行为。

## 5. 方案 B：保留原版地图界面，修改客户端

适用条件：必须保留原版传送地图和目的地选择体验。

当前客户端定位到的文件：

```text
/Users/mc/IdeaProjects/5.8客户端/bin64/Game.dll
```

针对当前这份 5.8 x64 客户端，已记录的候选修改如下。它们只适用于字节布局完全一致的客户端版本，不能直接套用到其他补丁版本：

| 文件偏移 | 原始字节 | 候选字节 | 作用 |
|---|---|---|---|
| `0x1A2C3A` | `0F 82 0E 01 00 00` | `E9 0F 01 00 00 90` | 将“目的地数小于十”的条件跳转改为无条件跳转，绕过等待分支 |
| `0x7B3C78` | `75 1F` | `EB 1F` | 绕过另一处本地计时初始化条件分支 |

正式实施时必须遵循：

1. 先复制 `Game.dll` 备份，记录 SHA-256，并确认目标偏移处仍是表中的原始字节。
2. 只对匹配当前版本的文件执行修改；字节不匹配时停止，不能盲改偏移。
3. 修改后重算 PE CheckSum，并重新检查两处反汇编结果。
4. 用未进入极乐世界和刚进入极乐世界两种状态测试所有目的地，确认没有 `1401400` 提示和十几秒等待。
5. 确认其他传送员、任务限制、基纳扣除和传送结果没有回归。

风险：所有玩家都需要使用同一修改版客户端；客户端更新、完整性校验或安全软件可能覆盖或拒绝修改。该方案暂不执行。

## 6. 后续验收标准

无论最终采用哪种方案，都应验证：

- 刚进入极乐世界后立即与 `203726` 对话，可以选择并执行每个有效目的地。
- 不再出现客户端字符串 `STR_MSG_HOUSING_WAIT_TO_TELEPORT`（ID `1401400`）。
- `loc_id=3` 的任务 `1006`、`loc_id=313` 的任务 `10031` 第 3 步、`loc_id=444` 的任务 `10520` 第 4 步等原有条件仍然生效。
- 基纳不足时仍拒绝传送，且费用计算保持现有逻辑。
- 魔族 `204191`、其他地区传送员和非相关对话行为不受影响。
- 记录一次真实客户端操作或抓包，确认最终走到了服务端传送调用；静态代码检查不能替代这个验收。

本次仅保存本文档；未修改仓库中的 Java、XML、客户端资源或客户端二进制，也未执行构建和启动服务。

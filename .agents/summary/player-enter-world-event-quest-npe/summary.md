# 玩家进出世界 NPE 故障排查与系统性修复

## 1. 故障现象与日志回放

### 现象 1：进世界时活动任务 NPE（导致登入世界中断）
```text
09-30 12:42:03 ERROR [pool-4-thread-26] GAMECONNECTION_LOG - 进入世界 129957 时出错
java.lang.NullPointerException: Cannot invoke "java.lang.Integer.intValue()" because the return value of "com.aionemu.gameserver.model.templates.QuestTemplate.getMinlevelPermitted()" is null
	at com.aionemu.gameserver.services.QuestService.startEventQuest(QuestService.java:749)
	at com.aionemu.gameserver.services.EventService.StartOrMaintainQuests(EventService.java:184)
	at com.aionemu.gameserver.services.EventService.onPlayerLogin(EventService.java:129)
	at com.aionemu.gameserver.services.player.PlayerEnterWorldService.enterWorld(PlayerEnterWorldService.java:680)
```

### 现象 2：登出世界时术古扫荡 NPE（级联故障）
```text
09-30 12:43:17 ERROR [PacketProcessor:0] c.a.g.network.aion.AionClientPacket - 处理客户端（cc）消息 [C] 0xCE CM_QUIT 时出错
java.lang.NullPointerException: Cannot invoke "com.aionemu.gameserver.model.gameobjects.player.PlayerSweep.setShugoSweepByObjId(int)" because the return value of "com.aionemu.gameserver.model.gameobjects.player.Player.getPlayerShugoSweep()" is null
	at com.aionemu.gameserver.services.events.ShugoSweepService.onLogout(ShugoSweepService.java:91)
	at com.aionemu.gameserver.services.player.PlayerLeaveWorldService.startLeaveWorld(PlayerLeaveWorldService.java:108)
```

---

## 2. 根因剖析与完整因果链

### (1) 根因：`QuestTemplate.getMinlevelPermitted()` 返回 `null` 引发自动拆箱 NPE
1. **真端合法数据**：真端 `quest.xml` 中活动任务（如 80834/80835 王冠收集家，配置于 `events_config.xml` 的 `Prestige Pack Event` 长期活动中）的 `<minlevel_permitted>` 为 `0`，表示无最低等级限制。
2. **元数据映射瑕疵**：`QuestTemplate.fromMetadata` 中历史代码使用了 `t.minlevelPermitted = m.minLevel() == 0 ? null : m.minLevel();`，导致 `minLevel == 0` 的任务被赋予 `null`；同时，JAXB 反序列化未配置 `minlevel_permitted` 属性的 XML 任务时该字段也为 `null`。
3. **Getter 缺乏兜底**：`QuestTemplate` 类仅依靠 Lombok `@Getter` 生成 `public Integer getMinlevelPermitted()`，未如 `maxRepeatCount`、`countRecoverLimitedQuest` 那样提供默认值防护。
4. **调用处拆箱崩溃**：`QuestService.startEventQuest`（及 `QuestService.java:488, 623`、`QuestsData.java:92`）将 `template.getMinlevelPermitted()` 参与基本类型 `<` 运算，Java 自动调用 `.intValue()` 拆箱，遇到 `null` 立即抛出 NPE。

### (2) 级联故障：登入中断引发登出 NPE
1. `PlayerEnterWorldService.enterWorld` 在第 680 行执行 `eventService().onPlayerLogin(player)` 时抛出 NPE 中断。
2. 导致第 686 行的 `shugoSweepService().onLogin(player)` 从未被执行，玩家的 `PlayerSweep` 棋盘对象为 `null`。
3. 客户端因登录异常或退出发送 `CM_QUIT`，进入 `PlayerLeaveWorldService.startLeaveWorld`。
4. `ShugoSweepService.onLogout(player)` 中无脑调用 `player.getPlayerShugoSweep().setShugoSweepByObjId(...)`，因引用为 `null` 再次发生 NPE，导致后续的关键登出清理（组队解绑、军团仓储更新、状态清理）被截断。

---

## 3. 系统性修复内容

1. **`QuestTemplate.java` 契约强化**：
   - 显式实现 `public int getMinlevelPermitted()`，当底层字段为 `null` 时默认返回 `0`（与 `maxRepeatCount` 的防护模式保持一致），彻底阻断拆箱 NPE；
   - 修正 `QuestTemplate.fromMetadata`，如实保留 `m.minLevel()`，不再将 0 级转为 `null`。
2. **`QuestService.java` 防御性加固**：
   - `startEventQuest` 增加 `template == null` 判空防护；
   - 提取 `int minLevel = template.getMinlevelPermitted()` 保证绝对安全。
3. **`ShugoSweepService.java` 登出防护**：
   - `onLogout` 增加 `player == null || sweep == null` 判空，防止登入未成功或未开启棋盘的玩家在登出时引发二次崩溃；
   - 移除类头多余的 `@author` 注释。
4. **新增单元测试**：
   - `QuestTemplateMinLevelTest.java`：验证默认 null 时返回 0、0 级元数据及显式等级正确映射；
   - `ShugoSweepServiceTest.java`：验证 null 玩家或 null sweep 登出时不抛异常。

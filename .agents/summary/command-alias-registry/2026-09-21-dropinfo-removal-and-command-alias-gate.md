# `//dropinfo` 失效事故与命令别名一致性门禁（2026-09-21）

## 一、现象与时间线

- 玩家（`Ww`）输入 `//dropinfo` 查看当前目标/指定 NPC 掉落：客户端无任何回显，命令静默失效。
- `log/adminaudit.log:280`：`2026-09-16 21:51:11,983 玩家=Ww，目标=vudu crestlich，命令=dropinfo` —— 该命令当时可用且有 GM 审计记录。
- 删除提交 `212e00ef4`（2026-09-18 00:41，`chore(admin): remove unreferenced DropInfo and InstanceEngineManager`）之后，审计日志中再无 `dropinfo` 记录。

## 二、根因

1. `212e00ef4` 按"全库零引用"删除两个类：
   - `src/main/java/com/aionemu/gameserver/commands/admin/DropInfo.java`（构造器 `super("dropinfo")`，`AdminCommand`）；
   - `src/main/java/com/aionemu/gameserver/commands/admin/InstanceEngineManager.java`（构造器 `super("instance_manager")`，`AdminCommand`）。
2. 命令注册是**反射路径**，静态引用必然为零，与"死代码"无关：
   `ChatProcessor#init`（`src/main/java/com/aionemu/gameserver/utils/chathandlers/ChatProcessor.java:112`）→
   `CompiledScriptLoader.load("com.aionemu.gameserver.commands.admin", "com.aionemu.gameserver.commands.player")`（同文件 `:120`）→
   `ChatCommandsLoader#postLoad`（`src/main/java/com/aionemu/gameserver/utils/chathandlers/ChatCommandsLoader.java:36-42`）→
   `ChatProcessor#registerCommand`（同文件 `:132`）。
3. 判定缺陷 A（动态包树边界收窄）：`SDJ-001` 列出的 4 大动态反射包树包含 `commands.admin` / `commands.player`，但提交信息原文只写了"非动态反射 **AI** 包树"，`systemPatterns.md:16` 的一行铁律也只点名 `gameserver.ai`，于是这两个类被当成普通类处理。
4. 判定缺陷 B（复查语料缺别名）：复查按类名（`DropInfo` / `InstanceEngineManager`）而非别名（`dropinfo` / `instance_manager`）检索，而别名一直在配置里：
   `src/main/resources/aion/config/administration/commands.properties:181` `dropinfo = 0`、
   同文件 `:37` `instance_manager = 3`。"无配置字符串引用"与事实相反。
5. 判定缺陷 C（语义误判）：提交信息把二者分别描述为"未接线的查询结果载体""无注册入口的实例引擎管理器"，未识别出它们就是命令处理类。
6. 为什么验证没拦住：编译期看不见反射注册；测试无别名一致性门禁（此前唯一涉及 `commands.properties` 的用例 `GameServerTest:49` 只断言配置文件路径）；该批次记录"未执行的全量验证"中明确写了启动/客户端验收未执行。

## 三、影响面核对（修复前，别名 ↔ 命令类全量比对）

- `commands.properties` 配置别名 188 个；两个命令包 `super("alias")` 声明 186 个。
- 悬空别名恰好 2 个：`dropinfo`、`instance_manager`（反方向为空，无"已声明未配置"与重名别名）。

## 四、修复

1. 从删除提交的父提交恢复两个命令类（`212e00ef4^`），别名与访问等级配置无需改动。
2. 新增守卫 `src/test/java/com/aionemu/gameserver/commands/CommandAliasRegistryTest.java`（三个用例）：
   - `everyConfiguredAliasHasACommandClass`：配置别名必须都有实现类（本次事故方向）；
   - `everyCommandClassAliasIsConfigured`：实现类别名必须都在配置里（`registerCommand` 会静默丢弃未配置项）；
   - `everyCommandClassDeclaresExactlyOneAlias`：每个命令源文件必须且只能声明一个 `super("alias")`，保证解析覆盖率并防守重名。

## 五、验证状态

- static：别名 ↔ 命令类双向比对通过（188 / 188，零悬空）；`git diff --check` 通过。
- focused-test：`mvn -B -Dtest=CommandAliasRegistryTest test` —— **待授权，未执行**。
- runtime/client：未做（服务器生命周期由用户掌控），需真实客户端敲 `//dropinfo` 验收。

## 六、遗留观察（本轮未改）

- `.cmd_drop` 路径：`src/main/java/com/aionemu/gameserver/commands/player/cmd_drop.java:84-85` 在"带 NPC ID 参数且当前无目标"时执行 `Npc npc = (Npc) player.getTarget();` 会 NPE；且它打印 `drop.getChance()`（原始概率），与 `//dropinfo` 的 `DropGroup#getAdjustedChance`（含掉落倍率修正）语义不同。
- 客户端帮助 `src/main/resources/aion/data/static_data/HTML/commands.xhtml:96` 与 `commands.en.xhtml:95` 一直文档化 `//dropinfo [Target]`，恢复后重新与实现一致。

## 七、同类误删横向审计（2026-09-21，只读静态核对）

审计口径：**凡是"注册键写在反射注册表/数据/配置里、类本身零静态引用"的机制，都算同一失效家族**。

### 7.1 命令域（`com.aionemu.gameserver.commands.admin` / `player`）

- 历史删除事件仅 2 起：
  - `212e00ef4`（2026-09-18）：`DropInfo`、`InstanceEngineManager` —— 误删，本轮恢复；
  - `3e40f5116`（2026-07-12）：`MarryDel`、`PowerUp`、`cmd_answer`、`cmd_divorce`、`cmd_marry`、`wedding/cometome`、`wedding/missyou` —— **有意下线婚礼系统**，同一提交同时删除 `WeddingService`/`WeddingDAO`/`WeddingsConfig`/`WeddingCommand`/`weddings.properties`/婚礼 HTML 与 `commands.properties` 中的相关别名，属完整功能下线，非误删；全库现无残留引用。
- 修复后一致性：配置 188 别名 ↔ 命令类 `super("alias")` 188 个，双向零差异、零重名。
- 客户端帮助 `HTML/commands.xhtml` / `commands.en.xhtml` 里 83 个 `//xxx` 引用中，唯一无实现的是 `//skill [Skill Id] [Skill Level]`；但 `git log -S 'super("skill")'` 在该仓库全历史为空 → 属上游帮助文本残留（本仓库从未实现），不是删除事故。

### 7.2 AI 树（`com.aionemu.gameserver.ai`，`AI2Engine:74` 加载）

- 注册键：类上的 `@AIName("...")`；消费方：NPC 模板 `ai="..."`（`AI2Engine.validateScripts()` 启动期比对并告警）。
- 静态核对：`ai/` 下 804 个 `@AIName` 值 vs `npc_template_*.xml` 中 789 个不同 `ai=` 值 → **0 悬空**。
- 历史删除（`ai/instance` 194、`ai/siege` 5、`ai/walkers` 1、`ai/event` 1）均出自 2026-07 的零售化改造（"convert ... to alias portals" / "consume retail stage callback graph" 等），无 NPC 数据仍引用其 AI 名。

### 7.3 实例处理类（`instance.handlers.scripts`，`InstanceEngine:61` 加载）

- 注册键：`@InstanceID(n)`；历史删除 26 个文件。
- 静态核对：从删除提交的父版本取出 `@InstanceID`，与当前 115 个 handler 的 id 集合比较，并检查 `world_maps.xml` 是否仍有该世界 → **0 个"handler 被删且世界地图仍在"的缺口**。

### 7.4 区域脚本（`world.zone.scripts`，`ZoneService:192` 加载）

- 注册键：`@ZoneNameAnnotation("名字 名字")` → `ZoneName.get()` 查注册表，未知名回退 NONE 并告警。
- 历史删除：**0 个文件**；3 个被标注的区域名（`PRIMUM_FORTRESS`、`TERMINON_LANDING`、`CORE_400010000`）均在 `static_data/zones/zone_abyss_shields.xml` 中存在。

### 7.5 其他反射注册面

- `Class.forName` 全库仅 3 处：`DatabaseFactory`（驱动名来自配置）、`ClassTransformer`（配置值转类）、`CompiledScriptLoader`（包扫描）。当前无任何配置字段使用 `Class<?>` 转型，故无配置指向已删类的悬空。
- `src/main/resources/META-INF/services/` 已不存在（SPI 机制已下线）→ 无 SPI 悬空类。
- `com.aionemu.gameserver.quest` 已无源码且不在 4 个加载包内 → 任务域不存在同类"handler 静默消失"路径。

### 7.6 门禁覆盖边界

- 自动门禁目前只覆盖命令域（`CommandAliasRegistryTest`）。
- AI 名称、实例 id、区域名三类目前依赖：启动期告警日志（`AI2Engine.validateScripts`、`InstanceEngine` 注册日志、`ZoneName.get` 告警）+ 本节的静态核对脚本口径；如需长期防回归，可照命令域模式各补一条同类门禁。

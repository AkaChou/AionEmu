# 真端副本完整审计

本文记录本轮按真端 world、AI Pattern、条件出生、静态出生和结算数据核对的结果。

## Smoldering Fire Temple（302000000）

### 已完成

- 真端普通/Master 条件出生、Pattern、奖励门控、结算恢复和静态去重已完成，并在独立窗口提交。

## Dark Poeta（300040000）

### 真端证据

- `/Users/mc/IdeaProjects/58Server/Map/Worlds/idlf1/world_N.xml` 为 UTF-16LE 真端来源；普通创建 ID `39/66/122/179` 使用 `spawn_page=1`，SP/Master 创建 ID `1001/1002` 使用 `spawn_page=2`。
- 真端条件出生共有 73 条、20 个条件变量；现有生成数据漏掉来源编号 `#3/#4/#5/#18/#19/#28`，并遗漏 `vanq`、`aboss_die`、`sboss_die`、`SpecialServer_Cond`。
- `IDLF1_Temp_01_Sp`、`IDLF1_Temp_08_Sp`、`IDLF1_Temp_09_Sp`、`IDLF1_Vanq_A_Sp` Pattern 已存在并分别推进缺失变量；`SpecialServer_Cond` 按出生页面在 Handler 初始化。
- `206478` 的三条真端出生包含同一九点 sensory polygon；现有条件加载器和 Pattern AI 数据结构已支持该字段，不需要新增 dynamic area。

### 已完成

- 补入 6 条真端条件出生，使用 `10457..10462`，保留页面、位置、初始延迟、重生延迟、战斗状态反出生和 sensory polygon。
- 规范化真端 #5/#18 的多余右括号，避免表达式解析失败。
- Dark Poeta Handler 按 `spawn_page=2` 写入 `specialserver_cond=1`，普通页面写入 `0`，使新建和恢复路径一致。
- 保留现有 Handler 的 runtime 状态、条件引擎、Pattern、分数、掉落和结算 ownership；未删除无明确真端替代的逻辑。

### 验证范围

- `DarkPoetaRetailMigrationTest` 锁定 20 个变量、73 条条件、来源编号、页面、NPC 数量、sensory polygon、表达式规范化和页面初始化。
- 运行条件表达式解析、条件出生、Pattern AI、Handler 恢复和 XML schema 专项测试；GM 实测和线上副本压测不在本窗口范围内。

## Steel Rake（300100000）

### 真端证据

- `/Users/mc/IdeaProjects/58Server/Map/Worlds/idshulackship/world_N.xml` 为 UTF-16LE 真端来源；唯一条件为 `IDSHULACKSHIP_PH_KILL == 1`，生成 `215069` 并使用真端巡逻路径。
- 恢复源码 `server58-source/MainServer_Server64/fun/fun_055.cpp` 证明 `214968` 死亡时写入 `IDSHULACKSHIP_PH_KILL=1`。
- 真端无条件出生包含 13 个随机池和 2 个固定 5.8 Named；旧 Handler 的六等分 Special Delivery、Shugo 二选一和旧版 `215064/215065` 均不符合真端数据。
- compact `npc_drops` 已覆盖 Steel Rake 钥匙、Boss 和宝箱掉落；`215081` 的 `188051416` 为 50.06%，不存在旧 Handler 强制注入的 `188053787`。
- `IDSShip_KK` Pattern 写入 `Lever_ver30`，因此该变量继续由条件出生世界声明。

### 已完成

- 新增 `9785..9799` 共 15 条 Steel Rake 条件出生：1 条击杀条件、13 个真端随机池和 1 组固定 Named。
- 恢复 Brownie/Shadowstalker 的 16 点真端巡逻路径，并按真端概率、坐标和 Party 关系生成酒馆老板、Largimark、Special Delivery、宝箱、Calydon 与 Shugo 随机池。
- 删除静态 `215069` 及随机池重叠出生点，避免开场提前出生和重复出生。
- Handler 仅保留 `214968` 的条件变量写入与退出逻辑；移除旧随机出生、重复门/宝箱出生及自定义掉落注入。

### 验证范围

- `SteelRakeRetailMigrationTest` 锁定变量、15 条条件、真端概率/坐标、waypoint、固定 Named、静态去重、Pattern 和掉落 ownership。
- 条件出生与 waypoint XML 已通过 schema；Loader、Condition Engine 和 Steel Rake 专项测试通过。GM 实测和线上副本压测不在本窗口范围内。

## Draupnir Cave（320080000）

### 真端证据

- `iddf3_dragon/world_N.xml`、`NpcAIPatterns_IDDF3_dragon_SP_YDY.xml` 和 compact `npc_drops` 共同接管阶段出生、AI 与掉落。
- 条件世界由 4 条扩为 18 条，包含 `master_mode`、`lastboss`、`lastboss_t`、`iddf3_dragon_t_waveend` 等 8 个变量，覆盖 16 个真端 `condition_info` 区域及两组页级副官出生。
- `702658/702659` 属于 Adma 机制，真端 Draupnir world 不引用它们。

### 已完成

- 条件槽覆盖普通/特殊 Boss、Akhal、三类效果对象、波次控制和四名副官。
- 静态出生移除 11 个与条件槽重复的 NPC 组，避免阶段出生重复。
- 删除 Handler 中错误的 Abbey 箱子与自定义掉落；其余 Boss 掉落由 compact `npc_drops` 提供。
- 专项测试锁定变量、条件、关键 NPC、静态去重和错误 Handler 残留。

## 未闭环

- 其余生产副本仍需按同样 ownership 证据逐图处理；单图完成不代表全部区域完成。
- 客户端协议、基础倍率和 GEO/PATH 压测不在本批次范围内。

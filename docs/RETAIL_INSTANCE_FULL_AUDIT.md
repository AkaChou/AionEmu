# 真端副本完整审计

本文记录本轮按真端 world、AI Pattern、条件出生、静态出生和结算数据核对的结果。

## Smoldering Fire Temple（302000000）

### 真端证据

- `58Server/Map/Worlds/IDDF2_Dflame_Event/world_N.xml` 定义了 11 个条件变量、672 个条件出生；普通模式使用
  `spawn_page=1`，Master 模式使用 `spawn_page=2`，两个 Talking Mirror 条件没有页面限制。
- 真端普通模板为 `244xxx/834058`，Master 模板为 `245xxx/834212`；同坐标条件必须按页面保留各自模板。
- 真端 Pattern 推进三个阶段、Boss 房间四波和最终 Boss 所需变量；`world_timeattack.xml` 各排名均以
  `IDDF2_Dflame_Event_Reward` 作为结算条件。
- 真端条件数据的 `initial_spawn_time`、`spawn_time`、walker 和 `despawn_at_attack_state` 分别对应初始延迟、
  重生时间、移动路径和战斗状态反出生，不能由 Handler 的固定计数或串行出生替代。

### 已完成

- 将 672 个真端条件出生写入 `condition-spawns.xml`，保留页面、条件表达式、初始延迟、重生、路径和反出生语义；
  非奖励条件增加 `IDDF2_Dflame_Event_Reward == 0`，使完成态统一撤销阶段出生及其延迟/重生任务。
- Handler 初始化 `RetailConditionSpawnEngine`，完成时设置 `IDDF2_Dflame_Event_Reward=1`。
- 结算清理按 `RETAIL_AI_DATA` 的 NPC score ownership 删除普通与 Master 全部计分出生，避免只清理 `244xxx`。
- 门 8 与最终结算同时识别普通 `244095/244100` 和 Master `245198/245203`；恢复结算按 NPC ID 的击杀记录，
  不再依赖已经失效的旧手工出生坐标。
- 删除错误的四 Boss 串行出生、12 个 `244093` 解锁条件，以及 `834066/834067/834068` 手工出生。
- 删除已由条件引擎接管的静态阶段怪、宝箱、治疗塔和出口；保留真端非条件静态 Boss、药箱和 GM 变身工具。
- 宝箱掉落限定为普通 `834058` 和 Master `834212`。

### 验证范围

- `SmolderingFireTempleRetailMigrationTest` 锁定变量/条件/NPC 数量、页面与模板映射、关键阶段表达式、延迟、
  重生、walker、Handler ownership 和静态出生去重。
- 运行条件出生/Handler 恢复/Pattern AI 专项测试、两个 XML schema 和 `git diff --check`。

## 未闭环

- 其余生产副本仍需按同样 ownership 证据逐图处理；单图完成不代表全部区域完成。
- 客户端协议、基础倍率和 GEO/PATH 压测不在本批次范围内。

# 真端副本完整审计

本文记录本轮按真端 world、AI Pattern、条件出生、静态出生和掉落数据核对的结果。

## Adma Stronghold（320130000）

### 真端证据

- `58Server/Map/Worlds/iddf2a_adma/world_N.xml` 提供 `adma_t_boss`、`iddf3_dragon_fx3` 两个变量和 9 个条件区域。
- `npcaipatterns_master_4id_jsm.xml` 中的 `Adma_T_Control_01/02/05` 与 `Adma_T_Named_01..06` 负责 Pot 随机出生、阶段变量、阶段消息、辅助出生和亡魂控制。
- `npc-ai.xml` 将 `237239..237245`、`856574`、`856575` 绑定到上述真端 Pattern。
- `npc_drops_part_013.xml` 已包含 Adma 首领钥匙、装备包和常规掉落；Handler 注入属于重复或私服自定义逻辑。

### 已完成

- 将真端 9 条条件出生写入 `condition-spawns.xml`，共 2 个变量、9 个条件、9 个槽。
- 删除静态出生中与条件阶段重复的 `237242`、`237243`。
- 用静态出生表达真端 `Common_F_Adma_T_Pot_SP`：默认 `856575`，`237245` 以 `select_prob=2500` 保留 25% 分支。
- 用真端坐标和 `entity_id=66` 加入 `730176` 出口出生。
- 删除 Handler 的掉落注册、Pot 定时器、Lannok/Gutorum/死亡收割者/出口手工流程，以及错误的 Abbey 箱子逻辑。
- 删除无真端坐标对应的旧 Handler 手工附加箱子出生；真端公共箱子 territory 仍列入后续普通出生同步。

### 验证范围

- 专项 `AdmaStrongholdRetailMigrationTest` 锁定条件数量、关键 NPC、随机 Pot、出口和 Handler 残留。
- 运行条件出生 XSD、Adma 静态出生 XSD、专项 Maven 测试及既有迁移测试。

## 未闭环

- 其余生产副本仍需按同样 ownership 证据逐图处理；单图完成不代表 139 个区域全部完成。
- 客户端协议、基础倍率和 GEO/PATH 压测不在本批次范围内。

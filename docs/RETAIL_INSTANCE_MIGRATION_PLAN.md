# 真端副本全套迁移技术方案

> 状态：实施中（2026-07-20）。静态数据、次数冷却、动态实例基础、Portal/Luna 统一准入、结算账本、时间攻击、无限塔、battleground、arena PvP、tournament 和 Luna 奖励已接管；自动匹配与 handler 状态恢复仍在实施。
> 目标：以 58Server 5.8 真端副本数据和行为为权威，完整替换 AionEmu 当前副本创建、进入、冷却、次数、持久化、匹配、评分与奖励机制。
> 执行策略：单轨替换，不做新旧兼容。新机制接管一条运行路径时，旧逻辑和旧数据必须在同一实施批次清理。

## 1. 结论

当前副本与真端不一致的根因不是缺少少量 handler，而是当前实现只有“内存地图实例 + 手写 handler”，缺少真端统一的动态世界模型。

真端副本全套至少包括：

1. 真端副本创建、限制、冷却、匹配、评分和奖励静态数据；
2. 动态副本实体、成员资格、有效期、空本时间和重进资格；
3. 阶段、条件变量、门、主要目标、积分和定时器状态持久化；
4. 统一的进入判断、收费、次数消费和失败补偿；
5. 完整匹配状态机、开放时段、职业配额、阵营侧和补位；
6. 数据驱动的时间攻击、战场、竞技场、无限塔和 Luna 结算；
7. JVM 重启后的实例恢复与奖励幂等。

现有 `WorldMapInstance`、`InstanceService` 的底层地图操作、`SpawnEngine`、Retail AI 和 handler 特殊编排继续复用。不会重写整个世界系统，也不会引入通用工作流 DSL、事件溯源或可插拔框架。

## 2. 已确认实施决策

### 2.1 单轨运行

- 生产运行路径只能存在一套副本规则。
- 不设置 `LEGACY`、`HYBRID`、`RETAIL` 运行模式。
- 不保留旧机制配置开关。
- 不做新旧数据库双读、双写或后台对账。
- 不在新定义缺失时回退旧 XML、旧枚举、旧 handler 数值或旧冷却表。
- 关键定义缺失、ID 无法映射或跨表引用不完整时，启动直接失败。

### 2.2 替换即清理

每个实施批次必须同时包含：

1. 新机制；
2. 数据转换或数据库迁移；
3. 最小完整验证；
4. 被替代旧调用、旧模型、旧 XML、旧 DAO 和旧测试的删除；
5. 本文档的状态、证据和剩余工作更新。

只新增新逻辑、不删除已被替代旧逻辑的批次，不算完成。

### 2.3 开发期边界

开发期间允许存在尚未接入运行路径的候选转换器、数据文件和新核心类，但一旦新代码进入正式调用链，旧调用链必须在同一批次删除。开发期并存不等于运行时兼容。

### 2.4 发布和故障处理

- 发布使用维护窗口，不支持在线热切换。
- 数据库结构变更前必须生成可恢复备份。
- 故障处理方式是回滚整个发布版本和数据库备份，不是在运行时切回旧逻辑。
- 代码、静态数据和数据库迁移视为一个不可拆分发布单元。

## 3. 完成标准

满足以下全部条件后，才能称为“真端副本全套完成”：

- [x] 139 个有效副本地图全部完成数据和行为审计；
- [x] 18 个当前无专用 handler 的地图已逐图确认 Quest/Retail AI、Housing、Event 或纯数据所有权；
- [ ] 所有副本入口统一经过一个进入决策和提交入口；
- [ ] 真端基础次数、同步冷却、累计次数和购买次数全部生效；
- [ ] 动态副本可在 JVM 重启后恢复；
- [ ] 阶段、门、主要目标、条件变量、积分和截止时间可恢复；
- [ ] 结算和扣费在崩溃重试后不重复、不丢失；
- [ ] 自动匹配规则不再依赖 `AutoGroupType` 巨型枚举和地图类型 switch；
- [ ] 时间攻击、战场、竞技场、无限塔和 Luna 奖励来自真端表；
- [ ] 客户端副本信息、次数、匹配、阶段和积分包与 5.8 协议一致；
- [ ] 被替代的旧 XML、DAO、模型、枚举、硬编码和测试全部删除；
- [ ] 真端基础值与服务器自定义倍率分离；真端模式不使用私服修正值冒充基础值；
- [ ] 本文档所有阶段状态、验证证据和清理项均更新完成。

## 4. 当前系统盘点

### 4.1 地图和 handler 覆盖

当前 `world_maps.xml` 中有 139 个有效副本地图：

- 121 个地图有专用 handler；
- 18 个地图没有专用 handler；
- handler 注解共有 123 个唯一地图 ID；
- `300260000`、`301632000` 有 handler，但不在当前有效副本地图集合中。

没有专用 handler 的 18 个地图：

```text
301340000
310020000 310030000 310040000 310060000
310070000 310080000 310120000
320010000 320030000 320040000 320050000
320070000 320090000 320140000
600080000 720010000 730010000
```

无 handler 时 `InstanceEngine` 返回 `GeneralInstanceHandler` 空实现。地图可能存在基础刷怪，但没有阶段、门、计时、积分和结算编排。

### 4.2 当前创建和生命周期

当前 `InstanceService.getNextAvailableInstance(...)`：

1. 从 `WorldMap` 分配内存 `instanceId`；
2. 创建 `WorldMapInstance`；
3. 加入地图实例集合；
4. 固定加载 spawn page `0`；
5. 调用 handler 的 `onInstanceCreate`；
6. 启动空本检查。

存在的问题：

- 没有永久 `instanceUid`；
- 没有动态实例数据库记录；
- 没有实例恢复入口；
- 创建、成员登记、收费和传送不是一个原子业务过程；
- spawn page 不是来自 `instance_creation`；
- 实例有效期、绑定期、空本期和重进期没有统一语义。

### 4.3 当前内存状态

`WorldMapInstance` 保存：

- 地图对象；
- 地图玩家；
- 注册对象 ID；
- 队伍、联盟和 League；
- handler；
- 空实例销毁 `Future`。

Retail AI 还使用多个静态 `Map<WorldMapInstance, State>` 保存：

- 条件变量和条件刷怪；
- 动态区域启停状态及到期任务；
- Windstream 状态；
- 复活、任务和限制区域状态；
- NPC Party 和组队控制状态。

这些状态在进程退出后全部丢失。

### 4.4 handler 状态规模

当前 handler 脚本中：

- 71 个文件声明了 `Future` 或同类任务字段；
- 95 个文件直接创建定时任务；
- 约有 1,052 个调度调用点；
- 约有 265 个 `Future` 字段声明。

这些对象不能直接序列化。完整恢复必须迁移为“业务状态 + 绝对截止时间”，不能保存 Java 对象图。

### 4.5 当前冷却和次数

当前本地 `instance_cooltimes.xml` 有 110 条简化定义。数据库 `portal_cooldowns` 只有：

```text
player_id
world_id
reuse_time
entry_count
```

无法表达：

- `coolt_sync_id` 共享次数；
- 普通与 F2P 冷却表；
- 次数累计；
- 每日累计重置；
- 额外次数；
- 购买次数和阶梯价格；
- 组件物品；
- Luna 或其他货币消费；
- 购买上限和购买步数。

### 4.6 当前自动匹配

当前匹配主要由：

- `AutoGroupType` 巨型枚举；
- `auto_group.xml` 简化属性；
- `AutoGroupService` 内地图类型 switch；
- Dredgion、Kamar、Ophidan、Idgel 等专用 Service；
- 内存搜索者和自动实例集合；

共同组成。

未完整接入的真端能力包括：

- 每周开放时段；
- 职业最小/最大配额；
- 阵营侧；
- 年龄或等待容差；
- shuffle 条件；
- 迟到进入和补位；
- 队列公平序号；
- 完整状态变化和取消原因。

### 4.7 当前评分和奖励

当前有 43 类左右的奖励模型或 handler 奖励实现，奖励物品、时间阈值、分数和出生点大量硬编码。

`InstanceReward` 本身只保存内存列表、阶段类型、地图 ID 和实例 ID，没有持久化、结算批次和幂等键。

## 5. 真端数据源

### 5.1 基础表和 China 覆盖

| 表 | 基础表条数 | China 有效覆盖条数 | 当前接入状态 |
| --- | ---: | ---: | --- |
| `instance_creation.xml` | 380 | 无区域覆盖 | 已接管，378 条有效定义 |
| `instance_restrict.xml` | 149 | 无区域覆盖 | 已接管 |
| `instance_cooltime.xml` | 151 | 145 | 已接管 China 145 条 |
| `instance_cooltime2.xml` | 286 | 272 | 已接管 China 272 条 |
| `instance_pool.xml` | 4 | 无 | 缺失 |
| `instance_scaling.xml` | 2 | 无 | 部分被本地逻辑代替 |
| `instant_dungeon_define.xml` | 291 | 无 | 缺失 |
| `instance_bonusattr.xml` | 18 | 无 | 已有简化接入 |
| `instant_dungeon_battleground.xml` | 9 | 无 | 已接管 |
| `instant_dungeon_idarenapvp.xml` | 40 | 无 | 已接管 |
| `instant_dungeon_tournament.xml` | 5 | 无 | 已接管 |
| `world_timeattack.xml` | 19 | 13 | 已接管 |
| `world_timeattack2.xml` | 2 | 无 | 缺失 |
| `infinity_indun_reward.xml` | 40 | 无 | 已接管 |
| `matchmaker.xml` | 158 | 158 | 已加载，完整状态机实施中 |
| `team_match_maker.xml` | 1 | 1 | 已接管，含 Team Match 登记、准备窗口、迟到补位和恢复 |
| `npc_scores.xml` | 2,835 | 无 | 已生成并部分接入 |
| `luna_indun.xml` | 2 | 2 | 已接管，creation/开放时段/价格次数闭包 |

### 5.2 权威顺序

目标为中国区 5.8 服务端，数据裁决顺序：

1. 基础真端 XML；
2. `Map/XML/China` 同名区域覆盖；
3. 5.8 固定客户端协议和显示数据；
4. 恢复的 58Server 服务端流程；
5. 当前 AionEmu 仅用于识别已有兼容修复，不作为真端数值来源。

### 5.3 转换原则

原始表为 UTF-16，且包含特殊 DTD。运行时不直接加载原始文件。

统一复用：

```text
/Users/mc/PycharmProjects/aion_drop/staticdata_converter
```

转换器负责：

- 基础表和 China 覆盖选择；
- UTF-16 XML 解析；
- 世界名称、NPC、物品、技能、区域 Alias 的 ID 映射；
- 跨表外键验证；
- XSD 验证；
- 来源哈希和覆盖报告；
- 确定性输出；
- 只生成候选文件，不直接修改 AionEmu。

## 6. 目标架构

```text
58Server 基础 XML + China 覆盖
                │
                ▼
staticdata_converter 离线转换、映射和校验
                │
                ▼
definitions/compact/instance/
  ├── definitions.xml
  ├── limits.xml
  ├── matchmaking.xml
  ├── rewards.xml
  └── manifest.xml
                │
                ▼
RetailInstanceData
                │
       ┌────────┼────────┐
       ▼        ▼        ▼
Instance     Dynamic   Instance
Admission    Instance  Settlement
Service      Manager   Service
       │        │        │
       └────────┼────────┘
                ▼
WorldMapInstance / SpawnEngine / Retail AI / InstanceHandler
```

不新增：

- 通用规则 DSL；
- 通用事件总线；
- 事件溯源；
- 只有一个实现的接口或工厂；
- 运行时原始真端 XML 解析；
- 新旧兼容层。

## 7. 静态数据模型

### 7.1 `RetailInstanceDefinition`

每个定义至少包含：

```text
creationId
worldName
worldId
clientInstanceId
instanceType
spawnPage
ownerScope
insideGroup
keepGroup
difficulty
deathExpPenalty
deathApPenalty
bonusAttrId
scalingId
startAliases
resurrectAliases
exitAliases
restriction
cooldownRule
matchmakerIds
rewardProfileId
```

### 7.2 ID 规则

以下 ID 必须使用不同字段和不同 Java 类型语义，禁止继续统称 `instanceId`：

| 字段 | 含义 |
| --- | --- |
| `creationId` | `instance_creation.id` |
| `worldId` | 地图 ID |
| `clientInstanceId` | 客户端显示和协议副本 ID |
| `cooltimeTableId` | `instance_cooltime2.id` |
| `f2pCooltimeTableId` | F2P 冷却表 ID |
| `cooltimeSyncId` | 多个副本共享次数的同步组 |
| `matchmakerId` | 匹配定义 ID |
| `runtimeInstanceId` | 当前 `WorldMap` 下的频道号 |
| `instanceUid` | 数据库永久动态实例 ID |

### 7.3 转换门禁

生成器必须拒绝：

- 重复 ID；
- 重复名称；
- worldName 无法映射 worldId；
- worldId 不存在或不是副本地图；
- 冷却表、F2P 冷却表或同步组缺失；
- 匹配定义引用不存在的实例；
- 奖励引用不存在的物品、NPC 或技能；
- 起点、出口或复活 Alias 缺少坐标；
- 匹配人数与实例人数互相冲突；
- 未解释的关键枚举值。

输出 manifest 至少记录：

```text
source path
source SHA-256
region
converter version
record count
override count
resolved reference count
rejected record count
rejection reason
```

## 8. 动态实例模型

### 8.1 永久实例标识

新增数据库自增 `instanceUid`。当前 `runtimeInstanceId` 只用于地图频道和客户端传送，不能作为数据库主键。

恢复时：

1. 读取活动 `instanceUid`；
2. 重新建立 `WorldMapInstance`；
3. 优先恢复原 `runtimeInstanceId`；
4. 发生占用时分配新频道并更新映射；
5. 玩家成员资格始终按 `instanceUid` 关联。

### 8.2 公共运行状态

`InstanceRuntimeState` 保存：

```text
instanceUid
definition
phase
spawnPage
conditionVariables
flags
activeSpawnKeys
completedSpawnKeys
doorStates
dynamicAreaStates
objectiveStates
scores
deadlines
randomSelections
settlementState
stateVersion
```

不保存：

- `Player`、`Npc`、`VisibleObject` Java 对象；
- 运行时 objectId；
- `Future`；
- handler 整体对象；
- 线程、锁、回调或 lambda；
- 可从静态数据重新构建的完整刷怪列表。

### 8.3 稳定对象键

优先使用真端 `entityId`。没有 `entityId` 时由转换器生成：

```text
worldName:spawnPage:groupKey:memberIndex
```

handler 动态对象必须提供业务键：

```text
stage3.final_boss
left_room.reward_chest
wave5.guard.2
```

禁止用运行时 objectId 持久化。`npcId + 坐标` 只能作为诊断信息，不能作为恢复主键。

### 8.4 定时器

持久化：

```text
deadlineKey
deadlineAt
payloadKey
completed
```

恢复规则：

- 未到期：按剩余时间重新调度；
- 已到期且未完成：立即执行一次；
- 已完成：不再执行；
- 回调必须幂等；
- 同一 `deadlineKey` 只能有一个有效任务。

不保存 `Future`。旧 handler 的任务字段必须迁移为 deadline key。

## 9. 数据库设计

### 9.1 `dynamic_instances`

```text
instance_uid         BIGINT AUTO_INCREMENT PRIMARY KEY
world_id             INT NOT NULL
creation_id          INT NOT NULL
client_instance_id   INT NOT NULL
runtime_instance_id  INT NOT NULL
owner_type           TINYINT NOT NULL
owner_id             INT NOT NULL
difficulty           TINYINT NOT NULL
status               TINYINT NOT NULL
spawn_page           TINYINT NOT NULL
created_at           BIGINT NOT NULL
active_until         BIGINT NOT NULL
empty_until          BIGINT NOT NULL
destroy_at           BIGINT NOT NULL
state_version        INT NOT NULL
state_json           JSON NOT NULL
updated_at           BIGINT NOT NULL
```

`state_json` 保存稀疏公共状态。当前实例规模不需要拆成多张键值表。

### 9.2 `dynamic_instance_members`

```text
instance_uid
player_id
team_id_at_entry
side
permitted
joined_at
left_at
reentry_until
exit_world_id
exit_alias
reward_status
PRIMARY KEY(instance_uid, player_id)
```

成员资格不依赖玩家当前是否仍在原队伍。退队、换队长和掉线后是否可重进，只读取该表和副本定义。

### 9.3 `player_instance_limits`

```text
player_id
limit_key
reset_at
base_used
bonus_used
purchased_count
purchase_step
updated_at
PRIMARY KEY(player_id, limit_key)
```

`limit_key` 通常等于 `cooltimeSyncId`。多个 worldId 共享同一次数时只保存一行。

### 9.4 `instance_reward_ledger`

```text
instance_uid
player_id
reward_key
status
payload_hash
created_at
completed_at
UNIQUE(instance_uid, player_id, reward_key)
```

该表用于保证结算最多执行一次。

### 9.5 数据库迁移原则

- 新表完成并通过离线迁移验证后，在维护窗口一次性切换；
- 将有效 `portal_cooldowns` 转换为 `player_instance_limits`；
- 同步组内多条旧记录发生冲突时，按最严格的剩余冷却和最高已用次数合并，并输出审计报告；
- 切换成功后删除 `portal_cooldowns`、对应 DAO、模型和加载保存流程；
- 不保留双写或旧表只读兜底。

## 10. 核心服务

### 10.1 `RetailInstanceData`

职责：

- 加载 `definitions/compact/instance`；
- 建立 creationId、worldId、clientInstanceId、cooltimeSyncId 和 matchmakerId 索引；
- 启动时执行跨表验证；
- 向进入、实例、匹配和结算服务提供不可变定义。

替换完成后删除当前 `InstanceCooltimeData` 及其旧 JAXB 模型。

### 10.2 `DynamicInstanceManager`

职责：

- 创建、恢复和销毁动态实例；
- 分配 `instanceUid`；
- 调用现有 `WorldMapInstanceFactory` 和 `SpawnEngine`；
- 管理成员资格、有效期、空本期和重进期；
- 保存状态快照；
- 重建截止时间任务；
- 在服务器接受登录前恢复活动副本。

`InstanceService` 收缩为底层地图实例操作，不再拥有进入规则、冷却规则和持久化业务。

### 10.3 `InstanceAdmissionService`

所有副本入口必须统一调用：

- Portal；
- AutoGroup；
- Luna；
- 任务传送；
- 直接传送门；
- 重登恢复；
- GM 明确指定副本入口。

预检结果：

```text
allowed
failureReason
definition
existingInstanceUid
ownerScope
requiredCosts
limitMutation
memberReservations
reentry
```

执行顺序：

1. 对全部成员执行无副作用预检；
2. 查找可重进实例；
3. 预留实例和容量；
4. 扣除物品、基纳、Luna 和次数；
5. 持久化成员资格；
6. 传送；
7. 失败时释放预留并执行补偿。

重进不重复收费、不重复扣次数。

替换后删除：

- `PortalService` 中副本规则和地图 switch；
- `LunaShopService` 中副本 ID、固定价格和直接创建实例逻辑；
- `AutoGroupService` 中重复的等级和冷却判断；
- 其他绕过统一入口直接创建副本的调用。

### 10.4 `InstanceLimitService`

解释：

- Daily；
- Weekly；
- Relative；
- `coolt_sync_id`；
- 普通/F2P 表；
- `maxcount`；
- 次数累计；
- 每日累计重置；
- 购买次数；
- 阶梯价格；
- 最大购买次数；
- 组件物品或 Luna 消费。

China 规则使用显式 `Asia/Shanghai`，禁止依赖 JVM 默认时区。

### 10.5 `InstanceSettlementService`

只支持真端已有明确类型：

```text
TIME_ATTACK
BATTLEGROUND
ARENA_RANK
INFINITY_FLOOR
LUNA
```

统一输入事件：

```text
NpcKilled
PlayerKilled
PlayerDied
ObjectiveCompleted
StageChanged
TimeExpired
InstanceFinished
```

结算流程：

1. 根据真端表计算结果；
2. 生成不可变奖励计划；
3. 写入 `instance_reward_ledger`；
4. 发放物品、经验、AP、GP 和基纳；
5. 标记完成；
6. 重启后仅重试未完成记录。

## 11. 匹配系统

### 11.1 数据模型

`RetailMatchDefinition`：

```text
matchmakerId
worldId
openSchedule
minLevel
maxLevel
minUsers
maxUsers
matchSides
raceFree
registerModes
classMinimums
classMaximums
ageTolerance
ageRequisite
shuffleSize
shuffleTimeout
lateEntry
readyTimeout
```

### 11.2 状态机

```text
REGISTERED
  -> FORMING
  -> READY_CHECK
  -> RESERVED
  -> INSTANCE_CREATED
  -> ENTERING
  -> ACTIVE
  -> FINISHED | CANCELLED
```

必须覆盖：

- 登记期间成员变化；
- 队长更换；
- 职业配额；
- 双侧人数平衡；
- 开放时段；
- 快速进入；
- 迟到进入和补位；
- 匹配超时；
- 并发重复匹配；
- 公平排队序号；
- 取消和惩罚原因。

### 11.3 删除范围

新匹配进入正式调用链时，同批删除：

- `AutoGroupType` 枚举；
- `auto_group.xml`、XSD、dataholder；
- 专用地图类型可用性 switch；
- 重复的 Service 冷却判断；
- 只为旧枚举服务的测试和帮助方法。

具体战场的特殊分队、出生和结算编排可以保留在数据驱动核心之上的小型 handler 中。

## 12. handler 边界

### 12.1 必须数据驱动

- 进入等级、人数、种族和导师限制；
- 任务和物品限制；
- spawn page；
- 起点、复活点和出口；
- 实例有效期、空本时间和重进时间；
- 冷却、次数和购买次数；
- 条件变量和条件刷怪；
- 动态区域和 NPC Party；
- 匹配开放时间和职业配额；
- 标准计分和奖励。

### 12.2 handler 仅保留

- 真端数据无法表达的剧情分支；
- 过场动画联动；
- 复杂 Boss 演出；
- 载具、特殊变身和独有机关；
- 特殊任务副作用；
- 有客户端或真端证据的特殊 UI 行为。

### 12.3 handler 状态迁移

旧 handler 中以下字段必须迁移：

- 阶段整数和布尔标记 -> `InstanceRuntimeState`；
- `Future` -> deadline key 和 deadlineAt；
- NPC objectId -> stable spawn key；
- 门状态 -> `doorStates`；
- 分数 -> 公共 score state；
- 奖励是否发放 -> reward ledger；
- 玩家资格 -> `dynamic_instance_members`。

迁移完成后删除 handler 中对应字段和调度代码。禁止保留“新状态为主、旧字段兜底”。

## 13. 重启恢复

启动顺序：

1. 加载并验证真端副本静态数据；
2. 初始化世界地图；
3. 读取未过期 `dynamic_instances`；
4. 创建对应 `WorldMapInstance`；
5. 加载静态 spawn page；
6. 应用完成目标、动态刷怪、门和区域状态；
7. 恢复 handler 特殊状态；
8. 重建 deadlines；
9. 恢复成员资格和重进索引；
10. 完成后才允许玩家登录。

恢复失败：

- 单个实例状态不合法时标记为损坏并停止启动；
- 不静默销毁实例；
- 不将玩家送回出口掩盖恢复错误；
- 由维护人员修正或恢复数据库备份后重新启动。

## 14. 协议验证

已确认的 5.8 battleground 动态块长度：

- type 5 奖励块：130 字节；
- type 6 全场状态块：2,343 字节，双侧各 96 个 12 字节槽位；
- type 7 队伍表：6,629 字节，单侧 96 个 69 字节玩家槽位；
- type 11 阵营积分更新：18 字节。

`301700000` 已按 `World_BattleGround_SendWorldInfo`、`World_BattleGround_BroadcastWorldInfo`、
`World_BattleGround_BroadcastScoreUpdated` 和 `World_BattleGround_RewardUser` 的恢复结果实现上述固定长度，
没有复制现有旧战场分支的缩减槽位。

需要确认：

- `SM_INSTANCE_INFO` 中当前固定零值的真实含义；
- 额外次数和购买次数同步；
- 冷却同步组显示；
- Luna 副本状态和收费响应；
- 完整自动匹配状态；
- 迟到进入和补位状态；
- 战场、竞技场和时间攻击阶段包；
- 重进和实例绑定信息。

验证来源：

1. 5.8 客户端包读取；
2. 当前 opcode 表；
3. Game.dll 反汇编；
4. 58Server 恢复包路径；
5. 真端运行日志或抓包；
6. 客户端实际 UI 行为。

未确认字段不实现猜测值。协议不明确属于实施阻断，不允许以固定零值完成验收。

## 15. 旧逻辑和旧数据清理清单

以下内容在相应新机制接管时删除，而不是保留到项目末尾：

### 15.1 静态数据

- [x] `data/static_data/instance_cooltimes/`；
- [x] `data/static_data/auto_group/`；
- [ ] 被真端定义替代的 `instance_bonusattr` 简化数据；
- [ ] 与新定义重复的本地副本倍率、人数和冷却配置；
- [ ] `static_data.xml` 和 `static_data.xsd` 中对应 import/include；
- [ ] 运行目录中的旧副本 XML 副本。

### 15.2 Java 模型和加载器

- [x] `InstanceCooltime`；
- [x] `InstanceCooltimeData`；
- [x] `AutoGroupType`；
- [x] `AutoGroupData` 及旧 XML 模型；
- [x] `PortalCooldownItem`；
- [x] `PortalCooldownList`；
- [x] `PortalCooldownsDAO`；
- [x] 只为旧模型服务的 DataManager 字段和 loader。

### 15.3 运行逻辑

- [x] `PortalService` 中副本进入判断、收费、注册和地图 switch；
- [ ] `AutoGroupService` 中专用副本可用性和冷却 switch；
- [x] Luna 固定副本 ID、价格、免费标记和直接创建逻辑；
- [ ] `InstanceService` 中不属于底层地图操作的进入和次数逻辑；
- [ ] Retail AI 的静态 `Map<WorldMapInstance, State>`；
- [ ] handler 中已由公共状态接管的阶段、计时、积分和奖励字段；
- [ ] 各战场 Service 重复的 `hasCoolDown` 和开放判断；
- [ ] 被真端奖励表替代的奖励常量和类。

### 15.4 数据库

- [x] `portal_cooldowns` 表；启动迁移完成后立即删除；
- [x] 对应外键、DAO 注册、登录加载和登出保存逻辑；
- [x] Luna 旧免费次数存储中被新 limit 模型替代的字段；`player_luna_shop` 仅保留工艺宝箱状态；
- [ ] 被 reward ledger 替代的临时奖励标记。

### 15.5 测试

- [ ] 删除只验证旧 XML 和旧枚举的测试；
- [ ] 删除旧 DAO 测试；
- [ ] 删除旧 handler 硬编码数值测试；
- [ ] 新增统一数据闭包、恢复、扣费、匹配和结算测试。

## 16. 实施阶段与严格依赖

### 阶段 0：证据和版本冻结

目标：确定输入、ID、协议和代表性副本基线。

任务：

- [x] 冻结基础 XML 和 China 覆盖 SHA-256；
- [x] 冻结 5.8 客户端版本和启动参数；
- [x] 输出副本跨表 ID 映射报告；
- [x] 确认 clientInstanceId 和 cooltimeSyncId 协议含义；
- [x] 为代表性副本建立当前服务端行为基线；
- [x] 建立当前 `SM_INSTANCE_INFO` 字段级协议基线。

验收：

- 所有目标表来源明确；
- 代表性副本关键 ID 零歧义；
- 未确认协议字段有明确阻断记录；
- manifest 可重复生成。

### 阶段 1：静态数据转换和加载

依赖：阶段 0。

任务：

- [x] 增加真端副本生成器；
- [x] 生成 definitions、limits、matchmaking、rewards、coverage 和 manifest；
- [x] 增加 XSD；
- [x] 增加跨表校验；
- [x] 新增 `RetailInstanceData` 和 loader；
- [x] 将次数、冷却、创建、匹配静态读取调用切到新数据；
- [x] 删除旧 `instance_cooltimes`、`auto_group` 数据和模型。

验收：

- China 有效表条数和覆盖关系正确；
- 所有生产副本 worldName 可映射；
- 关键引用零缺失；
- 相同输入生成结果字节稳定；
- AionEmu 完整启动不再加载旧副本静态表。

### 阶段 2：动态实例和状态持久化

依赖：阶段 1。

任务：

- [x] 新建四张数据库表；
- [x] 实现 `DynamicInstanceManager` 基础生命周期；
- [x] 增加 `instanceUid`；
- [x] 实现创建、公共状态保存、实例恢复和销毁；
- [x] 实现稳定对象键；
- [x] 实现 deadline 调度；
- [ ] 将 Retail AI 实例状态迁入公共状态；
- [ ] 删除对应静态状态 Map 和旧空本任务所有权逻辑。

验收：

- 普通队伍本在创建、首 Boss、开门和完成阶段重启均可恢复；
- 已过期任务只执行一次；
- objectId 变化不影响状态应用；
- 不存在持久化 `Future` 或 Java 对象引用。

### 阶段 3：统一进入、冷却和次数

依赖：阶段 2。

任务：

- [x] 实现 `InstanceAdmissionService` 基础事务与补偿；
- [x] 实现 `InstanceLimitService`；
- [x] Portal、Quest、Luna、普通匹配、Team Match 和 Tournament 生产进入路径统一接入；
- [x] 离线迁移并删除 `portal_cooldowns`；
- [x] 实现同步组、F2P、累计和购买次数；
- [x] Portal 和 Luna 创建、扣费、次数及传送失败补偿；
- [x] 删除旧 PortalCooldown 模型、DAO、运行路径和 Portal 旧进入分支；旧表由数据库迁移删除。

验收：

- 创建失败不扣费；
- 重进不重复扣次数；
- 同步组多个地图共用次数；
- Daily、Weekly、Relative 在 `Asia/Shanghai` 下正确；
- 队伍成员预检失败时全队均不扣费；
- 数据库只剩新 limit 模型。

### 阶段 4：handler 状态迁移

依赖：阶段 2、3。

任务：

- [ ] 将剩余 39 个含任务字段的生产 handler 迁移到 deadline；
- [ ] 将阶段、门、动态对象和积分迁入公共状态；
- [ ] 为特殊对象补 stable key；
- [ ] 删除已迁移字段和调度代码；
- [x] 对 18 个无 handler 地图确认数据驱动覆盖；
- [x] 完成 Sulfur、Carpus、Hamate、Treasure Island、Danuar 三变体、Adma、Padmarashka、Cradle 和 Transidium 的首批恢复迁移；
- [ ] 删除只提供重复通用逻辑的 handler。

验收：

- 生产 handler 不再持有需要跨重启保存的 `Future`；
- 生产 handler 不再保存关键 NPC objectId；
- 通用阶段、积分和奖励不再由 handler 私有字段维护；
- 139 个地图都有行为闭包报告。

### 阶段 5：积分和奖励

依赖：阶段 2、4。

任务：

- [x] 接入 `world_timeattack`；
- [x] 接入 battleground；9 条真端行覆盖 8 个 handler，含 IDRun；
- [x] 接入 arena PvP；40 条真端行覆盖 8 张地图和全部 spawn page；
- [x] 接入 tournament；5 条定义、6 个 matchmaker、十张 Lobby/Stage 地图和轮次结算已接管；
- [x] 接入 infinity reward；
- [x] 接入 Luna 奖励；两张 Luna 地图使用独立幂等结算键；
- [x] 实现 reward ledger；在线结算先落 PENDING，离线玩家登录重试；
- [ ] 删除对应奖励类、常量和 handler 结算逻辑。

验收：

- 崩溃重试不重复奖励；
- 真端分数阈值、时间阈值和奖励一致；
- 玩家离线后可完成待结算；
- 不再存在被真端表覆盖的硬编码奖励数值。

### 阶段 6：完整匹配

依赖：阶段 1、2、3、5。

任务：

- [x] 加载 158 条 matchmaker 和 1 条 `team_match_maker`；
- [x] 实现登记、选人、准备、进入、活动、补位、退出、取消和恢复状态机；
- [x] 接入开放时段、职业配额、阵营侧、shuffle 池扩展、准备超时和迟到补位；
- [x] 自动匹配创建、成员预留、次数消费、取消补偿和传送均接入统一准入；
- [x] 接入主动取消、准备超时、队伍变化和惩罚区分；
- [x] 删除 `AutoGroupType`、旧 auto_group 数据和 `MatchDefinition.getAutoInstance()` 专用 switch，适配器类名由真端定义生成。

验收：

- Dredgion、战场、竞技场和普通自动组队均使用同一匹配核心；
- 开放时段和职业配额来自真端表；
- 并发请求不会重复创建实例；
- 迟到进入和补位符合协议；
- 运行时不再引用 `AutoGroupType`。

### 阶段 7：全量闭包和发布

依赖：阶段 0～6 全部完成。

任务：

- [x] 完成 139 个地图的静态和行为闭包报告；
- [ ] 完成代表性副本在线验收；
- [ ] 完成三类 JVM 崩溃恢复测试；
- [ ] 完成数据库离线迁移演练；
- [ ] 完成协议抓包对比；
- [ ] 扫描旧类、旧 XML、旧表和旧调用残留；
- [ ] 维护窗口执行整体发布；
- [ ] 更新本文档最终状态和证据。

验收：

- 完成标准全部勾选；
- 旧逻辑和旧数据残留为零；
- 启动、运行和停服通过；
- 数据库迁移后无旧表读取；
- 代表性副本端到端通过。

## 17. 代表性验收矩阵

| 类型 | 代表副本 | 地图 ID | 核心验证 |
| --- | --- | ---: | --- |
| 普通队伍本 | 贝希姆斯神殿 | 300170000 | 队伍资格、Boss、门、重进和恢复 |
| 单人剧情本 | 克罗梅德试炼 | 300230000 | 个人所有权、剧情阶段、出口 |
| 时间评分本 | 黑暗普埃塔 | 300040000 | 计时、等级评分、奖励和恢复 |
| PvP 战场 | 巴拉纳特德雷得奇安 | 300110000 | 双侧、积分、掉线和结算 |
| 自动匹配本 | 被吞噬的奥菲丹桥 | 301210000 | 队列、职业配额、补位和迟到进入 |
| 无限层数本 | 永恒试炼 | 301560000 | 楼层、重启和层数奖励 |
| Luna 副本 | 污染地下通道 | 301630000 | 免费次数、Luna 扣除、阶段和奖励 |

每个代表副本至少执行：

- [ ] 正常通关；
- [ ] 中途退出和重进；
- [ ] 队伍成员变化；
- [ ] 队长变化；
- [ ] 玩家掉线；
- [ ] 准备阶段 JVM 重启；
- [ ] 战斗阶段 JVM 重启；
- [ ] 结算阶段 JVM 重启；
- [ ] 冷却重置；
- [ ] 重复进入请求；
- [ ] 创建失败；
- [ ] 扣费后传送失败；
- [ ] 奖励发放过程中崩溃。

## 18. 风险和阻断

| 优先级 | 风险 | 解决方式 |
| --- | --- | --- |
| P0 | creationId、worldId、clientId、cooltimeId 混用 | 转换期强类型映射，关键引用失败即停止 |
| P0 | handler 的 Future 和任意字段无法恢复 | 迁移为公共状态、stable key 和绝对 deadline |
| P0 | 动态 NPC 没有稳定标识 | 真端 entityId 优先，生成稳定 spawn key |
| P0 | 协议字段含义未知 | 客户端、Game.dll、真端包和运行行为交叉验证 |
| P1 | 扣费成功但实例创建或传送失败 | 统一预检、预留、提交和补偿 |
| P1 | 奖励重复或丢失 | 唯一 reward ledger 和幂等结算 |
| P1 | 115 个 handler 中存在大量私服硬编码 | 逐图闭包报告，通用逻辑删除，特殊编排保留 |
| P1 | 无兼容切换增加发布风险 | 维护窗口、全量演练、整包回滚和数据库备份 |
| P2 | 真端恢复源码参数语义不完整 | XML、客户端、日志和黑盒行为共同裁决 |

## 19. 实施进度

### 19.1 总进度

| 阶段 | 状态 | 完成度 | 最后更新 | 证据 |
| --- | --- | ---: | --- | --- |
| 方案和现状审计 | 完成 | 100% | 2026-07-19 | 本文档 |
| 阶段 0：证据和版本冻结 | 完成 | 100% | 2026-07-19 | `manifest.xml`、`coverage.xml`、生成器 `--check` |
| 阶段 1：静态数据转换和加载 | 完成 | 100% | 2026-07-19 | 6 个生成 XML、统一 XSD、`RetailInstanceDataTest`、旧静态模型删除 |
| 阶段 2：动态实例和状态持久化 | 进行中 | 70% | 2026-07-19 | 四张表、`instanceUid`、公共状态、稳定对象键、deadline、创建/恢复/销毁、成员资格 |
| 阶段 3：统一进入、冷却和次数 | 进行中 | 95% | 2026-07-19 | 真端次数/冷却/购买次数、生产进入路径统一准入与失败补偿、旧 DAO/模型删除 |
| 阶段 4：handler 状态迁移 | 进行中 | 39% | 2026-07-20 | 139 图行为闭包；32 个 handler 移除私有关键任务，剩余 39 个含 `Future` 文件 |
| 阶段 5：积分和奖励 | 进行中 | 95% | 2026-07-19 | reward ledger、timeattack、infinity、battleground、IDRun、arena PvP、tournament、Luna |
| 阶段 6：完整匹配 | 进行中 | 95% | 2026-07-19 | 158+1 条定义、数据化适配器、阵营/职业/shuffle、动态实例、统一准入、超时/补位/惩罚、Team Match 协议与恢复 |
| 阶段 7：全量闭包和发布 | 进行中 | 10% | 2026-07-19 | 139 图静态与行为闭包报告已完成 |

### 19.2 更新规则

每次实施必须更新：

1. 总进度表；
2. 对应阶段任务勾选；
3. 实际变更文件；
4. 删除的旧逻辑和旧数据；
5. 执行的测试和结果；
6. 新发现的阻断；
7. 下一步唯一工作项；
8. 下方实施日志。

代码已经变更但本文档未同步更新时，该实施批次不算完成。

### 19.3 下一步

唯一下一步：迁移生产 handler 的 `Future`、阶段、门、关键对象 ID 和截止时间到公共可恢复状态，并同步删除旧字段与调度逻辑。

## 20. 实施日志

### 2026-07-19

- 完成当前副本创建、生命周期、handler、Retail AI、冷却、匹配、评分和数据库盘点。
- 当前有效副本集合更新为 139 张，121 张有专用 handler，18 张无专用 handler；另有 `300260000`、`301632000` 两个非活动 handler 注解。
- 确认真端 instance creation/restrict/cooltime/cooltime2/matchmaker/reward 数据规模。
- 确认真端 `WorldDb`、`DynamicWorldManager`、`DynamicWorld`、`MatchMaker` 和购买次数能力。
- 确认复用现有 `staticdata_converter`、`WorldMapInstance`、`SpawnEngine` 和 Retail AI，不重写世界系统。
- 按项目决策改为单轨替换：不做兼容、双读、双写、旧逻辑回退或长期并行运行。
- 明确每个替换批次必须同步删除旧逻辑和旧数据，并持续更新本文档和进度。
- 客户端权威路径更新为 `/Users/mc/IdeaProjects/5.8客户端`，冻结 `Aion.bin`、`Game.dll`、`system.cfg` 和两个启动脚本的 SHA-256 与启动参数。
- 新增 `scripts/generate_retail_instance_data.py`，生成 `definitions.xml`、`limits.xml`、`matchmaking.xml`、`rewards.xml`、`coverage.xml` 和 `manifest.xml`。
- 真端 `instance_creation.xml` 的 `id=83` 存在三条完全相同记录；生成时保留一条有效记录并在 manifest 记录 2 条源重复，冲突重复仍直接失败。
- 139 张活动实例地图闭包为 134 张标准 `instance_creation` 地图和 5 张特殊系统地图；特殊地图均写入明确所有权原因，不回退旧副本定义。
- 确认客户端副本列表 ID 为 `instance_cooltime.id`；`coolt_sync_id` 是玩家共享次数与冷却持久化键，两者禁止混用。
- 新增统一 XSD、`RetailInstanceData` 加载器和闭包测试；生成器确定性检查、6 个 XML XSD 校验及 `RetailInstanceDataTest` 已通过。
- 真端 `matchmaker.xml` 158 条定义已接入，生成 260 条 NPC 映射和 101 条客户端字符串映射；旧 `auto_group.xml`、XSD、模型和巨型枚举数据已删除。
- 真端次数、共享同步键、F2P、累计、购买次数已接入，旧 `instance_cooltimes.xml`、`portal_cooldowns` DAO/模型和运行路径已删除。
- 新增四张副本表、动态实例 `instanceUid`、成员资格、公共运行状态、创建/恢复/销毁和空本回收基础。
- Portal 已切到统一准入基础流程，创建、扣费、次数消费和失败补偿由 `InstanceAdmissionService` 管理。
- 自动匹配已改为真端数据驱动的开放时段、等级、人数、职业配额、creation/spawn page 和动态实例创建；阵营侧、shuffle、补位与统一成员准入仍在实施。
- 当前验证：`mvn -q -DskipTests compile` 通过；`InstanceLimitServiceTest`、`InstanceRuntimeStateTest`、`RetailInstanceDataTest` 通过。
- 完成 `301700000` IDRun 真端闭包：修复错误的进入初始化，新增 60 秒准备、780 秒比赛、五阶段全场到达顺序计分、玩家阶段 bit mask、Light/Dark 条件变量、断线参与率和 60 秒结算退出。
- 新增统一 `idrun_sensory_score` AI，836199～836203 在玩家进入 20 米范围时登记阶段；836347/836348 接入 2 秒 `useitem` 交互，精确消费 185000320/185000319、给予 188058577/188058576，并递增 `idrun_treasure_despawn`。
- IDRun 胜/平/负奖励完全读取 `instant_dungeon_battleground`：钥匙 6/4/3、AP 1600/320/320 + 参与率积分占比 bonus 6400、GP 50/10/10；在线与离线奖励共用幂等账本。
- IDRun `SM_INSTANCE_SCORE` 按真端统一 battleground 动态块实现 type 5/6/7/11 固定长度，type 6/7 使用 96 槽而非旧分支的缩减槽位。
- 本批次新增 `TreasureIslandRewardTest` 和 IDRun 真端奖励测试；`mvn -q -DskipTests compile`、`TreasureIslandRewardTest,InstanceSettlementServiceTest,TransactionSafetyTest,LocalizedLogCallsTest`、生成器 `--check` 均通过。
- 完成 `instant_dungeon_idarenapvp` 40 条定义接管，严格使用 `world_id + spawn_page` 选行，覆盖 `300350000`、`300360000`、`300420000`、`300430000`、`300450000`、`300550000`、`300570000`、`301100000`。
- 根据真端 `World_IDARENAPvP_ProcessNextStage` 修正阶段语义：比赛固定 3 轮；`stage_count=4/6` 是可无重复抽取的场景池大小，不是 4/6 轮。
- 根据 `World_IDARENAPvP_OnWaitTimeExpired`、`OnPlayTimeExpired`、`OnUpdatePCKillScore`、`DecreasePCScore`、`ScoreModByRank` 和 `RewardUser` 接入 130 秒等待、每轮真端时限、初始/最低分、击杀/死亡分、第三轮排名倍率、最高分/分差提前结束、离线时长奖励分和 60 秒结算退出。
- Arena 奖励统一实现为基础 + 积分占比 + 排名权重 + 排名专属物品；专属物品受真端最少参与人数约束，训练场零奖励行不再发放旧私服奖励。
- Harmony 改为队伍积分和排名、玩家独立 reward ledger：队伍分数包含每名参与者的在线时长奖励分，同队玩家获得同一队伍公式结果，并分别使用 Harmony 奖励倍率完成在线或离线幂等结算。
- 删除个人 Arena 和 Harmony 子类的硬编码 `reward()`、Harmony 固定 `1000/150`、固定 `120/180/10` 秒、`getGloryRewardRate()` 误用、直接 AP/GP/物品发奖及离场删除奖励记录逻辑。
- `SM_INSTANCE_SCORE` 的 Arena 奖励统一为真端 76 字节块；个人 Arena 主体保持 `12 × 92 + 76 + 24` 字节结构，Harmony 在相同 76 字节奖励块后保留队伍状态字段。
- 删除 `PvPArenaPlayerReward` 中 Crucible/Courage/Infinity、opportunity、gloryTicket、medal、lifeSerum 等旧专用字段和 `canRewardOpportunityToken()`。
- 新增 `PvPArenaPlayerRewardTest`、`PvPArenaMigrationTest`，扩展 `InstanceSettlementServiceTest` 覆盖 40 行完整性、严格 spawn page、Discipline/Chaos/Glory/Harmony 公式、训练场零奖励、人口门槛、最低分、离线时长和提前结束条件。
- 本批次验证通过：`mvn -q -Dtest=InstanceSettlementServiceTest,PvPArenaPlayerRewardTest,PvPArenaMigrationTest,TransactionSafetyTest,LocalizedLogCallsTest test`；`python3 scripts/generate_retail_instance_data.py --check`。
- 完成 tournament 单轨替换：5 条真端定义、6 个 matchmaker、五组 Lobby/Stage、真端轮次阈值与奖励接管；删除 Hall/Arena of Tenacity 旧 Service、handler、reward/player reward、位置和自动匹配模型，以及 HOT 配置、玩家字段、命令和旧测试。
- Luna 生成闭包新增 `luna_cost.xml`，两条 `luna_indun` 严格映射 creation 322/323、world、spawn page、起点和价格 45/47；`RetailInstanceData` 启动校验 dungeon/creation/world/price/schedule 全闭包。
- Luna 入场改为真端单轨：每日一次免费 + 一次 20 Luna、每周三 09:00 重置的一次免费 + 五次 20 Luna；免费次数也计数，成功后递增，重进不收费，传送/扣费失败回退实例、成员、Luna 和次数。
- 删除 Luna 固定地图、固定坐标、固定 20 Luna、直接创建和 `free_under/free_munition`；`player_luna_shop` 仅保留每日工艺宝箱 `free_chest`，副本次数进入 `player_instance_limits` 独立负键命名空间。
- 两张 Luna handler 改用 `lunaPlan/settleLuna`，与普通时间攻击分离幂等键；真端等级价格增幅公式已实现，中国区当前 `price_ratio=0`。
- Luna 批次验证通过：生成器生成与 `--check`、`mvn -q -DskipTests compile`、`RetailInstanceDataTest,LunaInstanceServiceTest,InstanceLimitServiceTest,LunaShopServiceTest,InstanceSettlementServiceTest,TransactionSafetyTest,LocalizedLogCallsTest`。
- 完成 `team_match_maker.xml` 单轨接管：按真端 302350000/creation 362、每侧最低 12、总上限 96、两阵营、草稿 900 秒和最大 20 活跃实例进入统一匹配核心。
- Team Match 接入 `CM/SM_FIND_GROUP` 0x12/0x14/0x16/0x17/0x18/0x19：实例创建后开启准备窗口，成员变化刷新快照，进入或取消后关闭，重登恢复未完成窗口；匹配成功后删除公共招募登记，不保留旧列表状态。
- 匹配核心已接入阵营固定侧、原队伍原子选取、职业上限、年龄放宽、shuffle 候选池扩展、准备超时、迟到补位、主动取消、队伍变化取消和惩罚区分；Team Match 保留原队伍/联盟，普通自动匹配按原协议拆队。
- `RetailMatchSession` 升级到不兼容版本 2，持久化等级、原队伍、准备登记 ID、进入/在线状态和补位截止时间；旧版本会话不兼容读取，动态实例恢复后重新建立匹配适配器与准备计时。
- 完成 139 图行为闭包：108 张 `HANDLER`、13 张 `RETAIL_AI_QUEST`、8 张 `TOURNAMENT`、4 张 `EVENT`、2 张 `HOUSING`、2 张 `DATA_ONLY`、2 张 `EXCLUDED_NON_PRODUCTION`、0 张 `MATCHMAKER`；每张图均生成唯一 `behavior` 与 `behavior_source`。
- 行为闭包生成修改 `scripts/generate_retail_instance_data.py`、`coverage.xml`、`manifest.xml`、`RetailInstanceData` 和 `RetailInstanceDataTest`；加载器会校验 139 条、合法分类、来源非空和 manifest 分类计数一致。
- 本批次验证通过：`mvn -q -DskipTests compile`；`RetailMatchSessionTest,RetailMatchPlannerTest,FindGroupServiceTest,FindGroupProtocolTest,MatchDefinitionTest,InstanceAdmissionRoutingTest,LocalizedLogCallsTest`；生成器生成与 `--check`；`RetailInstanceDataTest,FindGroupProtocolTest,LocalizedLogCallsTest`。
- 完成匹配适配器数据化：生成数据写入适配器类名，`MatchDefinition.getAutoInstance()` 删除地图/类别专用 switch；159 条非 Tournament/Team Match 定义由测试闭包。
- 完成 Danuar Reliquary、Lucky Danuar Reliquary、Infernal Danuar Reliquary 的击杀计数、死亡 Idean、炸弹 deadline、完成、过期、出口和 Boss 阶段恢复。
- 完成 Adma Stronghold 的可疑罐、Lannok/Reaper 链和完成出口恢复；完成 Padmarashka Cave 的九段警告、两小时驱逐、守护者/卵计数和动画状态恢复。
- 完成 Cradle of Eternity 与 Transidium Annex 的首阶段 deadline、阵营、关键计数、门状态和完成对象恢复；含 `Future` 的生产 handler 文件由 62 降至 58。
- 新增 `InstanceHandlerRecoveryMigrationTest`；`mvn -q -Dtest=InstanceHandlerRecoveryMigrationTest,InstanceDeadlineSchedulerTest test` 和 `mvn -q -DskipTests compile` 通过。
- 完成 Theobomos Lab 的随机宝箱选择、元素封印石、Silikor 守卫计数、Triroan/Ifrit 延迟阶段和完成出口恢复；剩余含 `Future` 的生产 handler 文件降至 57。
- `mvn -q -Dtest=InstanceHandlerRecoveryMigrationTest,InstanceDeadlineSchedulerTest test` 再次通过。
- 完成 Draupnir Cave 的阵营、副官/Charger 计数、Bakarma 奖励箱、Akhal 延迟阶段和 Abyss Gate 两波 deadline 恢复；剩余含 `Future` 的生产 handler 文件降至 56。
- `mvn -q -Dtest=InstanceHandlerRecoveryMigrationTest,InstanceDeadlineSchedulerTest test` 通过。
- 完成 Crucible Challenge 第二奖励阶段的阶段、击杀/刷新/奖励计数和递归 deadline 恢复，删除固定频率私有 `Future`；剩余含 `Future` 的生产 handler 文件降至 55。
- `mvn -q -Dtest=InstanceHandlerRecoveryMigrationTest,InstanceDeadlineSchedulerTest test` 通过。
- 完成 Linkgate Foundry 真端 20 分钟绝对截止时间、15/10/5/3/1 分钟警告、除 Belsagos 外实验室怪物到期清理、完成出口和重启恢复；删除私有 `Future`、线程池直调和固定索引清怪，剩余含 `Future` 的生产 handler 文件降至 54。
- Linkgate 批次验证通过：`mvn -q -DskipTests compile`、`InstanceHandlerRecoveryMigrationTest,InstanceDeadlineSchedulerTest` 和生成器 `--check`。
- 完成 Drakenseer's Lair 真端 10 分钟绝对截止时间、1 分钟启动提示、最后 1 分钟警告、三座护盾导管计数、Akhal 护盾解除、超时退出和完成出口恢复；删除私有任务列表与直接线程池调度，剩余含 `Future` 的生产 handler 文件降至 53。
- Drakenseer 批次验证通过：`mvn -q -DskipTests compile`、`InstanceHandlerRecoveryMigrationTest,InstanceDeadlineSchedulerTest` 和生成器 `--check`。
- 完成 Right Wing Chamber 15 分钟宝箱截止时间、15 分 10 秒退出截止时间、剩余倒计时、134 个古代宝箱和两段消失消息恢复；删除按玩家重复创建的任务、两个私有 `Future` 和固定索引清理，剩余含 `Future` 的生产 handler 文件降至 52。
- Right Wing 批次验证通过：`mvn -q -DskipTests compile`、`InstanceHandlerRecoveryMigrationTest,InstanceDeadlineSchedulerTest` 和生成器 `--check`。
- 清理 18 份副本刷怪资源中的旧重生语义：阶段/一次性 NPC 删除 `respawn_time`，handler 或真端 AI 动态生成的重复 NPC 从静态 spawn 删除；不保留静态重生兼容路径。
- 修复真端 NPC 点位迁移的副本重生语义：`generate_retail_npc_spawns.py` 不再把副本内 `npc_type="ATTACKABLE"` 的 `spawn_time` 写成 AionEmu `respawn_time`，非攻击型机关仍保留原刷新配置。
- 使用修正后的迁移器重生成钢铁钩爪号、德拉乌尼尔洞穴、阿德玛城寨、阿图拉姆、活动阿图拉姆和黑暗普埃塔，删除 159 个攻击型组的 `respawn_time`，点位数量不变且二次生成字节一致。
- 删除 18 个已由副本 handler 动态创建和控制的 Boss 静态 spawn，避免初始双刷以及死亡后被静态刷新器重新创建。
- 新增迁移器回归测试，覆盖副本攻击型怪不重生、非攻击型机关保留刷新和新增攻击型组不写重生时间。
- 完成 Left Wing Chamber 的 12 阶段、总计 60 分钟宝箱消失链恢复：保留 12 个真端点位和每阶段 5 分钟倒计时，持久化当前阶段与下一绝对截止时间，重建剩余宝箱并追赶停服期间已过期阶段；删除 12 个布尔计时字段、私有 `Future`、直接线程池调度和无效门缓存，剩余含 `Future` 的生产 handler 文件降至 51。
- Left Wing 批次验证通过：`mvn -q -DskipTests compile`、`InstanceHandlerRecoveryMigrationTest,InstanceDeadlineSchedulerTest` 和生成器 `--check`。
- 完成 The Hexway 的 12 阶段、总计 60 分钟宝箱消失链恢复：保留 Captain Jarka 的 6 选 1 仓库钥匙掉落、12 个宝箱点位和每阶段 5 分钟倒计时，持久化当前阶段与下一绝对截止时间；删除 12 个布尔计时字段、私有 `Future`、直接线程池调度和无效门缓存，剩余含 `Future` 的生产 handler 文件降至 50。
- The Hexway 批次验证通过：`mvn -q -DskipTests compile`、`InstanceHandlerRecoveryMigrationTest,InstanceDeadlineSchedulerTest` 和生成器 `--check`。
- 完成 Lower Udas Temple 的 12 阶段、总计 60 分钟宝箱消失链恢复：保留 6 个普通和 6 个高级宝箱点位、Boss/宝箱/玩家专属掉落以及离本钥匙清理，持久化当前阶段、下一绝对截止时间和 Debilkarim 死亡后的停止状态；停止后重启仍重建剩余宝箱但不继续消失，删除 12 个布尔计时字段、私有 `Future` 和直接线程池调度，剩余含 `Future` 的生产 handler 文件降至 49。
- Lower Udas 批次验证通过：`mvn -q -DskipTests compile`、`InstanceHandlerRecoveryMigrationTest,InstanceDeadlineSchedulerTest` 和生成器 `--check`。
- 合并 Kysis、Krotan、Miren 三个同构深渊宝物库 handler：共享恢复型编排保留三套司库/Boss/钥匙/神器/门/区域差异，持久化随机选择、死亡、四段屏障删除 deadline、12 阶段宝箱和 Boss 停止状态；重启不再重新抽取或复活已死亡动态 Boss，并删除三份私有 `Future`、36 个布尔计时字段和重复线程池逻辑，剩余含 `Future` 的生产 handler 文件降至 46。
- 深渊宝物库批次通过 `mvn -q -DskipTests compile` 和 `InstanceHandlerRecoveryMigrationTest,InstanceDeadlineSchedulerTest`；生成器临时目录重生成后除工作树 `limits.xml` 的外部末尾换行外全部字节一致，正式 `--check` 被该单一非语义差异阻断，本批未修改或纳入该文件。
- 完成 Sealed Argent Manor 真端 60 秒准备、15 分钟挑战和 Boss 死亡后 3 秒结算的绝对 deadline 恢复；持久化积分、击杀数、排名、门 14、随机元素抗性、职业 Zadra 选择及死亡、随机 Boss 掉落、已消耗 Drained Hetgolem、一次性计分怪死亡和玩家结算标记，重启不会重抽、重复刷 Boss、重复耗材、复活已清理计分怪或重复发奖。
- Sealed Argent Manor 删除三个私有任务容器、直接线程池调度、内存开始时间、门缓存和失效销毁字段；积分改用已生成的真端 `npc-scores.xml` 数值，奖励继续由幂等 `InstanceSettlementService` 接管，剩余含 `Future` 的生产 handler 文件降至 45。
- Sealed Argent Manor 批次验证通过：`mvn -q -DskipTests compile`、`mvn -q -Dtest=InstanceHandlerRecoveryMigrationTest,InstanceDeadlineSchedulerTest test`；正式生成器 `--check` 仍仅被工作树 `limits.xml` 的外部末尾换行阻断，本批不修改或提交该文件。
- 完成 Smoldering Fire Temple 真端时间攻击恢复：删除旧 60 秒准备和 600 秒挑战硬编码，统一读取 `world_timeattack` 的 100 秒准备与 480 秒挑战；Sealed Argent Manor 同步改为读取同一真端时间字段，避免后续 handler 再复制计时常量。
- Smoldering Fire Temple 持久化每个稳定点位的击杀事件、积分、击杀数、排名、门 2/8、12 只 Vengeful Obscura 解锁、Temple Guardian 至 Enraged Kromede 四段动态 Boss、三处阶段传送对象、随机宝箱掉落和玩家结算标记；重启不会重复计分、漏掉 Boss 阶段、重复刷传送对象或重复发奖。
- Smoldering Fire Temple 计分改用真端 `npc-scores.xml`，修正旧代码对 244091/244092/244093 的错误积分；离本清理改为删除实际掉落的 `162002085` 至 `162002090`，不再用错误的 `162002031` 至 `162002036` 物品 ID。删除三个私有任务容器、直接线程池调度、内存计数/开始时间、门缓存和失效销毁字段，剩余含 `Future` 的生产 handler 文件降至 44。
- Smoldering Fire Temple 批次验证通过：`mvn -q -DskipTests compile`、`mvn -q -Dtest=InstanceHandlerRecoveryMigrationTest,InstanceDeadlineSchedulerTest,InstanceSettlementServiceTest test`；正式生成器 `--check` 仍仅被工作树 `limits.xml` 的外部末尾换行阻断，本批不修改或提交该文件。
- 完成 Divine Tower L/D（`310160000`、`320160000`）真端单轨替换：以 `IDAb1_Heroes_L/world_N.xml`、`IDAb1_Heroes_D/world_N.xml` 和 `NpcAIPatterns_IDAb1_Heroes_JSM.xml` 为权威，Retail AI/条件刷新接管四段防守波次、四道墙、女巫移动、Boss 生成与战斗、限时特效和出口，掉落回归真端数据；两个 380 行私服 handler 缩减为仅保留 `@InstanceID`，旧波次计数、手写点位、线程任务、错误掉落和出口逻辑全部删除。
- Divine Tower 条件刷新转换支持 `1st_door` 至 `4th_door` 等数字开头变量、`life`、固定 `respawn_time` 和空条件组隔离；全量重生成 4,991 条条件、6,204 个槽位、916 个受支持变量及 35 个无条件感知刷新。运行时新增 life/重生绝对截止时间、一次性死亡标记和重复死亡幂等保护，JVM 恢复不会复活已死亡对象、重置 life 或重复调度重生。
- Divine Tower 静态刷怪按真端重新生成：补入初始控制 NPC `248401` 和 `248458` 至 `248464`，删除已由条件刷新生成的 `248025`、`248437`、`248404` 至 `248407`，并移除副本攻击型基础怪的旧 `respawn_time`；`806731`/`806732` 出口由 `boss_die == 1` 条件生成，不保留静态或 handler 兼容路径。剩余含 `Future` 的生产 handler 文件降至 42。
- Divine Tower 批次验证通过：转换器 9 项测试、`mvn -q -DskipTests compile`、`RetailAiDefinitionLoaderTest,RetailConditionSpawnPartyLoaderTest,RetailConditionSpawnEngineTest,RetailPatternAI2Test,InstanceHandlerRecoveryMigrationTest,InstanceDeadlineSchedulerTest`、`condition-spawns.xsd` 校验和 `git diff --check`；正式副本生成器 `--check` 仍只报告无关工作树 `limits.xml` 的末尾换行差异，本批不修改或提交该文件。

### 2026-07-20

- 完成 Abyssal Splinter 与 Unstable Abyssal Splinter（`300220000`、`300600000`）真端单轨替换：以 `idabre_core/world_N.xml`、`idabre_core_02/world_N.xml`、两套真端 AI Pattern 和 `instance_creation.xml` 为权威，Retail AI/条件刷新接管 Boss 技能、阶段消息、神器、门、最终 Boss、宝箱和完成出口。
- 条件刷新转换器支持 `spawn_time_ex`，运行时按 `spawn_time + Rnd(0, spawn_time_ex)` 生成随机重生绝对截止时间并持久化；恢复后复用原 deadline，不重新随机。多 `condition_info` 仅对显式 `--alternative-world` 拆分，`--base` 复用旧 `(worldId, expression, source)` ID 并只替换选定世界，避免未审计地图启用和状态键漂移。
- 使用 `--base` 与两个 `--alternative-world` 正式重生成得到 5,017 条条件、6,230 个槽位、926 个变量；原 4,991 个 ID 全部保留，新条件使用 4,992 至 5,017，生成文件与项目 `condition-spawns.xml` SHA-256 一致。
- 两个约 400 行旧 handler 删除手写 Boss/门/宝箱/掉落、线程任务、错误 `19283` Abyssal Blessing 和静态出口兼容逻辑，只保留离本/登出删除真端副本钥匙 `185000104`；Unstable handler 复用同一清理逻辑。掉落表已含该钥匙，不再由 handler 重复注册。
- 删除两张图静态完成出口，并移除由真端条件/AI 控制的一次性攻击怪旧 `respawn_time`。剩余含 `Future` 的生产 handler 文件降至 40。
- Abyssal Splinter 批次验证通过：转换器 9 项测试、正式窄重生成字节比对、`mvn -q -DskipTests compile`、`RetailAiDefinitionLoaderTest,RetailConditionSpawnPartyLoaderTest,RetailConditionSpawnEngineTest,RetailPatternAI2Test,InstanceHandlerRecoveryMigrationTest,InstanceDeadlineSchedulerTest`、`condition-spawns.xsd` 校验和相关文件 `git diff --check`。
- 完成 Taloc's Hollow（`300190000`）真端单轨替换：以 `idelim/world_N.xml`、`NpcAIPatterns_IDElim_OSY.xml`、`instance_creation.xml` 和恢复后的 `Elim_HealtoPC01/02` 真端脚本为权威，Retail AI/条件刷新接管 Boss 技能、消息、电影 435/437、门、阶段对象、风柱和巨型治疗植物生成。
- 旧 503 行 handler 缩减为 83 行：删除手工 Boss 死亡编排、门/对象缓存、条件生成、私服掉落与奖励、任务道具无条件发放、HTML 提示、线程任务和 `Future`；只保留无真端替代来源的 434/438/463/464 电影触发、离本力量石/变身/召唤物清理，以及治疗植物交互。治疗植物不再直接改 HP/MP，改由 NPC 施放真端技能 `19229`（HP 20000、MP 10000）和 `19230`（HP 30000、180 秒持续恢复）。剩余含 `Future` 的生产 handler 文件降至 39。
- 任务 `10032`/`20032` 的入口恢复成对发放真端 work item，使用批量背包预检；传送失败时回滚两件任务物品，成功后才推进 `2 -> 3`。handler 不再清理任务 work item，继续由任务死亡、登出和离图流程负责。
- 删除与 `IDElim_3F_Heal_Plant_Giant == 50` 条件刷新重复的静态 `700941`，并移除一次性巨型虫卵 `700738` 的旧 `respawn_time`。434/438/463/464 在真端 AI/XML 和恢复 DLL 中均未发现替代触发，客户端 `idelim` 关卡为当前工具链无法解包的专用 PAK，因此按证据边界保留最小入口/区域触发，不保留关联私服消息或编排。
- Taloc's Hollow 批次验证通过：`mvn -q -DskipTests compile`、`mvn -q -Dtest=InstanceHandlerRecoveryMigrationTest,TalocsHollowQuestMigrationTest,RetailAiDefinitionLoaderTest,RetailConditionSpawnEngineTest,RetailPatternAI2Test test` 和相关文件 `git diff --check`。

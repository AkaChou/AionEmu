# 真端副本全套迁移技术方案

> 状态：实施中（2026-07-20）。静态数据、次数冷却、动态实例基础、Portal/Luna 统一准入、结算账本、时间攻击、Infinity Shard、无限塔、battleground、arena PvP、tournament 和 Luna 奖励已接管；自动匹配与 handler 状态恢复仍在实施。
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

`WorldMapInstance` 当前保存：

- 地图对象；
- 地图玩家；
- 注册对象 ID；
- 队伍、联盟和 League；
- handler；
- `InstanceRuntimeState` 持久机制状态；
- 按实例持有的不可序列化瞬态对象和任务；
- 旧空实例销毁 `Future`。

Retail AI 的实例状态迁移已经完成：

- 条件变量、条件刷怪、区域启停和 Windstream 状态写入 `InstanceRuntimeState`；
- 区域玩家集合、条件出生对象、组队控制器和任务对象由 `WorldMapInstance` 瞬态容器持有；
- `DynamicInstanceManager` 绑定状态变更落库，并在恢复实例时先解码状态再初始化 Retail AI；
- `InstanceService.destroyInstance()` 统一清理 Retail AI、deadline 和对象注册表。

当前 `src/main/java/com/aionemu/gameserver/ai` 中已无 `static Map<WorldMapInstance, State>`；历史上 10 个实例静态 Map 和 2 个全局玩家状态表由 `22146d6e3` 删除。剩余旧空实例销毁任务所有权属于生命周期收口项，不再与 Retail AI 状态迁移混记。

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
| `instance_bonusattr.xml` | 18 | 无 | 已接管，18 条真端 Buff 定义 |
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
- [x] 被真端定义替代的 `instance_bonusattr` 简化数据、JAXB 模型和主静态表引用；
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
- [x] Retail AI 的静态 `Map<WorldMapInstance, State>`；
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
- [x] 生成 definitions、limits、matchmaking、bonus-attributes、rewards、coverage 和 manifest；
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
- [x] 将 Retail AI 实例状态迁入公共状态；
- [x] 删除对应静态状态 Map；
- [ ] 删除旧空本任务所有权逻辑。

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

- [ ] 将剩余 30 个含 `Future` 的生产 handler 迁移到 deadline 或真端 Retail AI；
- [ ] 将阶段、门、动态对象和积分迁入公共状态；
- [ ] 为特殊对象补 stable key；
- [ ] 删除已迁移字段和调度代码；
- [x] 对 18 个无 handler 地图确认数据驱动覆盖；
- [x] 完成 Sulfur、Carpus、Hamate、Treasure Island、Danuar 三变体、Adma、Padmarashka、Cradle 和 Transidium 的首批恢复迁移；
- [x] 完成 Infinity Shard（`300800000`）真端 Retail AI/条件出生接管，删除旧 handler、Hyperion/Ide Resonator Java AI、手工护盾/出口/奖励和私服掉落；
- [x] 完成 Nochsana 与 Alquimia 的真端掉落收口，删除重复钥匙、私服奖励和无业务价值 handler；
- [x] 完成奥德矿脉（`301690000`）真端种族任务链，条件出生接管初始敌人、裂隙、任务对象、最终 Boss 和友方 NPC；
- [x] 完成西奥博莫斯试验室（`301610000`）真端 Boss/门/出口/掉落接管，handler 仅保留真端数据未表达的钥匙宝箱生成；
- [x] 完成因德拉图要塞（`310090000`）真端 AI/掉落收口，删除私服烙印包 handler 和已被 Retail Pattern 接管的旧 Java AI；
- [x] 完成暗影法庭（`320120000`）真端任务/钥匙掉落收口，删除重复掉落与无权威来源电影 handler；
- [x] 完成奥德遗传实验室（`310050000`）真端钥匙掉落收口，handler 仅保留离本钥匙清理；
- [x] 完成圣所地下城（`301580000`，真端 `IDF6_OP`）种族条件出生迁移：`703092` 的真端 Pattern 写入 `IDF6_RACE_L/D`，条件出生接管 `806076/806080` 和 `806189/806190`，补入真端静态控制 NPC/火焰效果，删除旧 handler 的首个进入玩家手刷逻辑；
- [x] 完成阿祖图兰要塞（`310100000`，真端 `IDLF3_Castle_Lehpar`）私服任务 Buff 清理：真端世界无技能区域，技能 `274` 仅有任务技能定义且无副本调用，删除旧 handler 的全本强制施放和离本清理逻辑；
- [x] 完成黑暗普埃塔（`300040000`）普通服 page 1 的真端积分、评级、Boss 选择和可恢复生命周期：67 条条件、16 个变量、`npc-scores.xml`、`instant_dungeon_define.xml` 与公共 deadline 接管流程，handler 仅保留真端生成数据未表达的电影、门和 Marabata 控制器交互；
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
- [x] 接入黑暗普埃塔真端 NPC/采集积分、严格时间评级和等级 Boss 规则；最终奖励由评级 Boss、条件奖励箱和真端掉落闭环，不走 handler 直发；
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
| 阶段 1：静态数据转换和加载 | 完成 | 100% | 2026-07-20 | 7 个生成 XML、统一 XSD、`RetailInstanceDataTest`、旧静态模型删除；真端 18 条副本 Buff 已接管 |
| 阶段 2：动态实例和状态持久化 | 进行中 | 78% | 2026-07-20 | 四张表、`instanceUid`、公共状态、稳定对象键、deadline、创建/恢复/销毁、成员资格；Retail AI 实例状态已下沉，Dark Poeta 与 The Eternal Bastion 的积分、评级、奖励和 deadline 已接入恢复路径 |
| 阶段 3：统一进入、冷却和次数 | 进行中 | 95% | 2026-07-19 | 真端次数/冷却/购买次数、生产进入路径统一准入与失败补偿、旧 DAO/模型删除 |
| 阶段 4：handler 状态迁移 | 进行中 | 70% | 2026-07-20 | 139 图行为闭包；Infinity Shard、Haramel、Adma、Alquimia、Aether Mine、Indratu、Shadow Court、Steel Rake Cabin、Divine Tower L/D、Sanctuary Dungeon、Azoturan Fortress、Karamatis、Ataxiar 等旧 handler 已删除，Nochsana、Theobomos Test Chamber、Aetherogenetics Lab 已收缩为真端数据未表达的最小交互，Kromede 的 Boss 选择、掉落、剧情出生、宝库感知、任务电影和尸体技能职责已回归真端链，Raksang Ruins、Drakenspire 普通版/任务版、Fallen Poeta、Dark Poeta 与 The Eternal Bastion 的主要流程已由真端条件出生、Retail Pattern、公共状态及 deadline 接管；当前为 93 张 `HANDLER`、21 张 `RETAIL_AI_QUEST`、7 张 `MATCHMAKER`，剩余 6 个含 `Future`、32 个直接使用 `GameThreadPoolServices` 的生产 handler |
| 阶段 5：积分和奖励 | 进行中 | 97% | 2026-07-20 | reward ledger、timeattack、infinity、battleground、IDRun、arena PvP、tournament、Luna、Dark Poeta 与 The Eternal Bastion 真端积分/评级/条件奖励箱；Eternal Bastion 仍保留 802185 的证据边界奖励 hook |
| 阶段 6：完整匹配 | 进行中 | 96% | 2026-07-20 | 158+1 条定义、数据化适配器、阵营/职业/shuffle、动态实例、统一准入、超时/补位/惩罚、Team Match 协议与恢复、登录时统一同步 18 个 HUD 入口开闭状态 |
| 阶段 7：全量闭包和发布 | 进行中 | 18% | 2026-07-20 | 139 图静态与行为闭包报告、条件表达式逐条解析、全量 1753 项自动测试基线已完成；本轮修复生成 XML 非法条件并更新 6000 条条件加载基线 |

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

唯一下一步：继续迁移仍含私有阶段、门、关键对象 ID 或线程池调度的生产 handler，并在每张图完成时同步删除旧字段、旧静态出生和旧掉落逻辑。

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
- 完成 Divine Tower L/D（`310160000`、`320160000`）真端单轨替换：以 `IDAb1_Heroes_L/world_N.xml`、`IDAb1_Heroes_D/world_N.xml` 和 `NpcAIPatterns_IDAb1_Heroes_JSM.xml` 为权威，Retail AI/条件刷新接管四段防守波次、四道墙、女巫移动、Boss 生成与战斗、限时特效和出口，掉落回归真端数据；删除两个 380 行私服 handler 及迁移后遗留的空注册壳，旧波次计数、手写点位、线程任务、错误掉落和出口逻辑全部删除。
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
- 完成 Beshmundir Temple（`300170000`）真端单轨替换：以 `idcatacombs/world_N.xml`、`NpcAIPatterns_IDCatacombs_hue.xml`、`NpcAIPatterns_IDCatacombsHardNamed_hue.xml` 及真端 NPC/技能/掉落/任务数据为权威，Retail AI 与条件刷新接管普通/困难 Boss、门、消息、技能、召唤、电影、阶段对象和掉落。
- 条件刷新新增显式 `spawn_page` 范围，普通服只读变量 `SpecialServer_Cond` 固定按 `0` 求值；所有 5,080 条条件均显式写入页码，其中既有条件为 `0..255`，贝斯蒙迪尔 67 条条件保留真端 page 1/2。普通与困难阿巴纳分别由 `IDCT_SpecterN_Spawn`、`IDCT_SpecterH_Spawn` 在 10 座纪念碑后生成，不再沿用旧 handler 的错误 15 座计数。
- 为会写条件变量的页码初始 NPC 生成生产者刷新：page 1 包含 15 个 `216739`、`216287..216294` 与 `216587..216589`，page 2 包含 15 个 `216740`、`216206..216213` 与 `216583..216585`；马昆贝洛 `debufflich` 阶段链、船夫钥匙链和难度专属初始 Boss 均按真端页码运行。孤立但真实写入的 `DOORWALL_SPAWN` 保留为可持久化变量，不放宽被拒绝条件链。
- 删除所有与条件刷新重复的旧静态点；`216586` 因真端没有页码初始条件继续保留。旧 635 行 handler 缩减为 61 行，只保留任务油召唤、焚化炉钥匙交互和离本/登出清理 `185000091..185000096`；删除私服掉落、错误钥匙映射、Boss 死亡编排、错误技能链、电影/门兼容、线程任务和 `Future`。剩余含 `Future` 的生产 handler 文件降至 38。
- Beshmundir Temple 批次验证通过：转换器 16 项测试和窄重生成 SHA-256 幂等检查；`mvn -q -DskipTests compile`；`RetailConditionSpawnEngineTest,RetailConditionSpawnPartyLoaderTest,RetailAiDefinitionLoaderTest,RetailPatternAI2Test,BeshmundirTempleInstanceTest,InstanceHandlerRecoveryMigrationTest`；`condition-spawns.xml` 对新版 XSD 校验及相关文件 `git diff --check`。
- 完成 Kumuki Cave（`302330000`）真端单轨替换：以 `IDEvent_Solo/world_N.xml`、`NpcAIPatterns_IDEvent_Solo_JSM.xml`、`NpcAIPatterns_IDEvent_Solo_YDY.xml` 及真端 NPC/技能/掉落数据为权威，Retail AI 与条件刷新接管两套等级页、Porgus 营救、15 分钟计时链、Boss/召唤、机关、电影 951、消息、变身、奖励箱和完成出口。
- 条件刷新保持 10 个变量、111 条真端条件与 3 条页级初始托管条件，共 114 条、366 个槽位：page 1、page 1-2、page 2 分别为 113、29、113 个初始演员；补齐两套 8 只 Porgus、三处人参、四个机关、伪装物和电影/计时观察器。库穆基真端路径由 9 条补齐为 40 条，所有条件 NPC 的 `walker` 均有对应路径。
- 删除与真端页级条件托管重叠的 22 个旧静态 NPC 组，旧静态点与条件托管 NPC 已无交集；保留真端 Pattern 数据无法表达的 `703424` 铁栅钥匙交互，以及真端钥匙箱 `246294`。钥匙只由 `246294` 按真端掉落 `185000295`，删除旧 handler 对 `246327`、`246328`、`246381`、`246377`、`246379` 的私服掉落注册。
- 旧 639 行 handler 缩减为 45 行：删除手工 Porgus/Boss/奖励箱/出口/机关生成、门映射、电影、消息、倒计时、死亡编排、私有任务列表和 `Future`；只保留 `703424` 消耗 `185000295` 后删门、缺钥匙提示，以及离本/登出清理 4 件副本道具和 4 个副本效果。剩余含 `Future` 的生产 handler 文件降至 37。
- Kumuki Cave 批次验证通过：转换器条件/路径 20 项测试；`mvn -q -DskipTests compile`；`RetailConditionSpawnEngineTest,RetailConditionSpawnPartyLoaderTest,RetailAiDefinitionLoaderTest,RetailPatternAI2Test,KumukiCaveInstanceTest,InstanceHandlerRecoveryMigrationTest` 共 97 项测试；条件与路径 XSD、静态出生 XML、窄重生成 SHA-256 `06081c0c1e29ba1589d3fa5a6aacba074a0ce2330469b85f2aefe6e618cf758e` 字节幂等及 `git diff --check` 均通过。

- 完成 IDSweep 宝库（`301400000`）与皇帝特里鲁内克保险箱（`301590000`）真端单轨迁移：以 `58Server/Map/Worlds/IDSweep`、`IDSweep_02`、`world_timeattack.xml`、真端 AI Pattern 和 `5.8客户端` 对应静态数据为权威。两个旧 handler 合并为 `ShugoVaultTimeAttackInstance`，统一实现 100 秒准备、480 秒挑战、门开启、绝对截止时间、最终 Boss 死亡后 3 秒结算、JVM 重启恢复、积分/击杀/排名和玩家奖励幂等；时间、NPC 分数和奖励计划均从真端数据加载，不再保留旧 `Future`、内存开始时间、手写 Boss 链、私服掉落注册、种族灵魂刷出或旧消息时间线。击杀幂等按真端生命周期分流：无重生的一次性 NPC 使用 `NPC ID + 坐标` 稳定键，可重生条件点使用对象代次键，避免重启重复计分又保留合法重生计分。
- IDSweep 条件闭包重新生成：转换器将由副本核心写入的 `IDSweep_Reward`、`IDSweep_Reward_S` 纳入只读变量，输出 `5715` 条条件、`7286` 个槽位、`958` 个受支持变量；其中 `301400000` 为 `221` 条、`301590000` 为 `410` 条。闭包包含 `1STAGE_2START`、`1STAGE_START`、`2STAGE_ING`、`3STAGE_START`、`4STAGE_PHASE`、`4STAGE_ELITE`、保险箱 `SpecialServer_Cond == 0/1` Live/Master 分支、页级 AI 演员、奖励箱、出口和 `832932` 管家条件对象。奖励完成时通过 `RetailConditionSpawnEngine.setVariable` 持久化真端变量：宝库写 `IDSweep_Reward`，保险箱 S 级写 `IDSweep_Reward_S`，A-F 级写 `IDSweep_Reward`；不恢复旧 handler 手工刷奖励 NPC。
- 清理旧资源：删除保险箱静态出生文件中的全部 `235xxx` 战斗出生，仅保留真端静态入口、宝箱和 NPC 点；条件刷新接管动态战斗和结算对象。未对宝库按 NPC ID 整组删除，因其静态 `235xxx` 同时包含无条件常驻和条件波次，避免误删真端常驻点。
- IDSweep 批次验证通过：转换器 `test_generate_retail_condition_spawns.py` 19 项；`mvn -q -DskipTests compile`；`InstanceHandlerRecoveryMigrationTest,InstanceDeadlineSchedulerTest,InstanceSettlementServiceTest,RetailAiDefinitionLoaderTest,RetailConditionSpawnEngineTest,RetailConditionSpawnPartyLoaderTest,RetailPatternAI2Test`；`condition-spawns.xsd` 和保险箱静态 XML 校验；`git diff --check`。当前剩余风险仅为 `SpecialServer_Cond` 的服务器环境值：真端由动态世界创建时注入，AionEmu 普通服条件引擎缺省为 `0`，因此默认运行 Live 分支；两套分支数据均已保留，若部署 Master 数据源需在实例创建入口接入同一环境变量。

- 完成乌达斯神殿上层（`300150000`）真端单轨替换：以 `IDTemple_Up/world_N.xml`、`NpcAIPatterns_IDTemple_hue.xml` 和真端 NPC/掉落数据为权威，Retail AI/条件刷新接管 Live/Master 初始 NPC、钥匙怪、三段钥匙死亡链、门控、出口、传送对象和条件 Boss；删除旧 handler 的随机手写出生、私服掉落、死亡消息、门控制和线程池调度，仅保留离本/掉线清理 `185000083..185000085`。
- 乌达斯上层条件闭包使用 `--base --world 300150000 --alternative-world 300150000` 定向生成，保留原条件 ID `1738`，补入 `5716..5728`，共 `14` 条条件、`14` 个槽位、`3` 个副本变量；`SpecialServer_Cond` 的 Live/Master 分支、`FanaticElNBoss` 和 `Teleporter_Spawn` 均由真端表达式驱动，生成文件与窄重生成字节一致。
- 静态出生按真端清理：删除条件托管的 `215782`、`215783`、`215793`、`730217`、`700706`、`730272`，补回 `215787` 的两点 `pool=1` 随机组和 `215788` 固定点，并校正 `215789`/`215790` 的真端点位；不保留旧 handler 兼容出生。
- 乌达斯上层批次验证通过：`mvn -q -DskipTests compile`；`InstanceHandlerRecoveryMigrationTest`、`RetailAiDefinitionLoaderTest`、`RetailConditionSpawnEngineTest`、`RetailConditionSpawnPartyLoaderTest`、`RetailPatternAI2Test` 共 96 项；`condition-spawns.xsd`、静态出生 XML、窄重生成字节比对和 `git diff --check` 均通过。
- 完成阿德玛城堡陷落（`301600000`）真端单轨替换：以 `IDF6_Adma/world_N.xml`、`NpcAIPatterns_IDF6_Adma_Lap_SSH.xml`、真端 NPC/AI/掉落数据为权威，条件刷新接管召唤石、控制 NPC、最终 Boss 和出口，Retail Pattern AI 接管两扇门；删除旧 `AdmaFallInstance` 的私服掉落、门控制、手刷出口/宝箱和线程池消息。闭包补入 AI 写入但原文件遗漏的 `Sub_Boss_Die` 变量，正式结果为 6 条条件、4 个副本状态变量。
- 删除与 `boss_summon == 2` 条件刷新重复且坐标错误的静态 `220427`，保留真端静态入口、普通怪、两个交互对象和 `248974` 消息控制 NPC；不保留无真端出生来源的旧宝箱兼容路径。该图不再注册专用 handler，回落公共 `GeneralInstanceHandler`。
- 阿德玛批次验证通过：`mvn -q -DskipTests compile`；`InstanceHandlerRecoveryMigrationTest`、`RetailAiDefinitionLoaderTest`、`RetailConditionSpawnEngineTest`、`RetailConditionSpawnPartyLoaderTest`、`RetailPatternAI2Test`；`condition-spawns.xsd`、静态出生 XML 和相关文件 `git diff --check` 均通过。生产 handler 直接使用 `GameThreadPoolServices` 的文件降至 64 个，含 `Future` 的文件仍为 35 个。
- 完成哈拉梅尔（`300200000`）真端单轨替换：以 `idnovice/world_N.xml`、`NpcAIPatterns_LDF4_PJW.xml` 和真端 NPC/AI/掉落数据为权威，`IDNovice_Hameroon` Retail Pattern AI 接管召唤、职业宝箱、457 电影、出口和死亡清理；删除旧 handler 的 HTML、随机掉落、成长药、手刷宝箱/出口、重复钥匙掉落和线程池消息。
- 哈拉梅尔没有真端条件刷新缺口，静态出生保留 `216922` 等常驻对象，不新增宝箱/出口兼容出生；删除只验证旧 `sendMovie` 私有列表的 `HaramelInstanceTest`。生产 handler 直接使用 `GameThreadPoolServices` 的文件降至 63 个，含 `Future` 的文件仍为 35 个。
- 哈拉梅尔批次验证通过：`mvn -q -DskipTests compile`；`InstanceHandlerRecoveryMigrationTest`、`RetailAiDefinitionLoaderTest`、`RetailConditionSpawnEngineTest`、`RetailPatternAI2Test`；静态出生 XML、相关 AI XML 和 `git diff --check` 均通过。
- 完成 Infinity Shard（`300800000`）真端单轨替换：以 `idruneweapon/world_N.xml`、`npcaipatterns_idruneweapon_kmj.xml`、真端条件出生、NPC/技能/掉落数据为权威，Hyperion 的充能、护盾、阶段召唤、失败处理、出口和奖励箱全部由 Retail AI 与 `cSetCharge`、`cSetFastCharge`、`cSetIdPortal`、`cProtection01..04`、`cSetVritra2/4/6/10` 条件出生接管。
- 删除 `InfinityShardInstance`、`HyperionAI2`、`IdeResonatorAI2` 三条旧 Java 路径，清除手工 `284437` 护盾、护盾死亡计数、`730842`/`802184` 手工生成、Hyperion/奖励箱私服掉落和线程池系统消息；静态出生不含旧 `284437`，出口由 `cSetIdPortal == 1` 条件出生生成。
- 重新生成 `coverage.xml`/`manifest.xml`，300800000 归类为真端 `MATCHMAKER`（`matchmaker.xml:324`），同时消除已删除 Haramel/Adma handler 的陈旧 coverage 引用；大小写差异经加载器验证为不影响绑定，未引入无行为收益的名称改动。
- Infinity Shard 批次验证通过：`mvn -q -Dtest=RetailAiDefinitionLoaderTest,RetailPatternAI2Test,InstanceHandlerRecoveryMigrationTest test`；Pattern 231073 支持校验、18 条条件出生、关键变量/出口/护盾断言及旧文件删除断言均通过。
- 完成诺克萨纳训练营（`300030000`）真端单轨收缩：以 `idab1_minicastle/world_N.xml`、`NpcAIPatterns_IDMini_01.xml`、真端 NPC/技能/掉落数据为权威，静态出生、Boss/门/出口和 NPC AI 继续由真端数据接管。
- 删除 `NochsanaTrainingCampInstance` 的私服支援包 `188053787`、活动宝箱 `188051138` 注入、旧死亡/手刷出口注释和无效辅助方法；保留当前 Retail AI 运行时无法直接派发的神器 `700437` 物品交互，使用真端 NPC 技能 `276`，不保留兼容掉落或重复出口生成。
- Nochsana 批次验证通过：`mvn -q -Dtest=InstanceHandlerRecoveryMigrationTest test`、`mvn -q -DskipTests compile`、副本静态出生 XML 与 `git diff --check`。
- 完成阿尔奎米亚研究所（`320110000`）真端单轨替换：现有 `AlquimiaResearchCenterInstance` 只重复注册 `214027 -> 185000006`、`214034 -> 185000007` 两把已在真端 `npc_drops` 中以 100% 定义的钥匙，并向 `214028` 注入真端掉落表不存在的私服烙印包 `188053787`，因此完整删除该 handler。
- 320110000 的静态刷怪、NPC Pattern、钥匙、出口和掉落继续由真端数据负责；覆盖重新生成为 `MATCHMAKER`，来源 `matchmaker.xml:323,407`。行为闭包调整为 104 张 `HANDLER`、3 张 `MATCHMAKER`，总数仍为 139。
- 完成奥德矿脉任务副本（`301690000`，真端 `IDF6_Q`）单轨替换：以 `IDF6_Q/world_N.xml`、`LF6_G_Din_02_Enter_Attack_74` 和任务 `10529`/`20529` 为权威，静态补入真端控制 NPC `244145`；玩家进入其感知范围后由 Retail Pattern AI 按天族/魔族写入 `f6_mission_start = 1/2`，电影结束后经 `CM_PLAY_MOVIE_END -> RetailPatternAI2.onQuitCutscene` 写入 `f6_mission_spawn = 1/2`、清除初始阶段并删除控制 NPC。
- 使用条件出生转换器定向生成 `5729..5770` 共 42 条条件、42 个槽位和 2 个变量，接管 `244111..244113`、`244127..244129`、`703317`、`703325`、`731709`、`731715`、`806293`、`806294`、`806298`、`806299` 等种族分支对象；不保留旧 handler 的 16 个手工点位、重复死亡删除或传送裂隙生成。
- 删除 `AetherMineQInstance`；任务脚本删除 30 秒 `GameThreadPoolServices` 延迟和 `QuestService.addNewSpawn` 手刷 Boss/友方 NPC，电影包改为携带 `244145` 的对象 ID，使客户端电影结束事件回到真端 Pattern 链。覆盖重新生成为 103 张 `HANDLER`、15 张 `RETAIL_AI_QUEST`，139 张图总数不变；生产 handler 仍有 35 个声明 `Future`、62 个直接使用 `GameThreadPoolServices`。
- 奥德矿脉批次验证通过：条件转换器 19 项测试；`mvn -q -DskipTests compile`；`InstanceHandlerRecoveryMigrationTest`、`RetailAiDefinitionLoaderTest`、`RetailConditionSpawnEngineTest`、`RetailPatternAI2Test`；正式副本生成器 `--check`；条件出生和静态出生 XSD 校验；定向重生成 SHA-256 `93a019b6b923358453f099cfcac7af8cdda134d75cd1244e69bdc5bd51d6c023` 字节一致及 `git diff --check`。
- 完成西奥博莫斯试验室（`301610000`，真端 `IDF6_Lap`）真端单轨收缩：`248975` 感知玩家后写 `boss_summon = 1`，四个 `248969` 召唤石把 `boss_summon_check` 累加到 4，`248972` 再写 `boss_summon = 2` 生成 `220426`；Boss 死亡由 `IDF6_Lap_Boss` 写 `End_Boss_Die = 1` 并通过条件出生生成 `806206` 出口。6 条既有真端条件和三个变量完整保留。
- 删除静态常驻的 `220426` 和旧 `Desecrated_IfritAI2`，由真端 Pattern 接管 Boss 召唤、技能、元素生物、消息、脱战清理、狂暴和死亡变量；子 Boss 门由 `IDF6_Lap_SubBoss_01/02` 的 `control_door` 接管，不再手开旧门 ID 或延迟广播私服消息。
- `TheobomosTestChamberInstance` 从 183 行缩减为 15 行，只保留 Boss 死亡生成钥匙宝箱 `806221`：该宝箱不在 `IDF6_Lap/world_N.xml`，但 `chest_templates.xml` 的 `185000264` 钥匙约束、真端 `IDF6_LAP_ARMOR_LOOK_R_69A` 掉落及两套独立 5.8 服务端实现均确认这一职责。删除 `onDropRegistered`，钥匙回归真端 40% 概率，Boss/宝箱奖励回归真端 common drop groups，移除烙印包、随机装备箱等私服注入；生产 handler 直接使用 `GameThreadPoolServices` 的文件降至 61，含 `Future` 的文件仍为 35。
- 西奥博莫斯试验室批次验证通过：`mvn -q -DskipTests compile`；`InstanceHandlerRecoveryMigrationTest`、`RetailAiDefinitionLoaderTest`、`RetailPatternAI2Test`、`RetailConditionSpawnEngineTest`；正式副本生成器 `--check`；条件出生和静态出生 XSD 校验及 `git diff --check`。
- 完成因德拉图要塞（`310090000`，真端 `IDLF3_Castle_Indratoo`）真端单轨替换：静态出生继续保留 Boss `214159`、门和传送对象，Boss 使用真端 `DrGuard_PhA_L48` Pattern 与生成的 common drop groups；删除仅向每名玩家注入私服烙印包 `188053787` 的 `IndratuFortressInstance`，并删除已被 Retail Pattern 接管的旧 `Brigadier_IndratuAI2`。
- 因德拉图要塞不再注册专用 handler，覆盖重新生成为 `MATCHMAKER`，来源 `matchmaker.xml:315,404`；行为闭包调整为 102 张 `HANDLER`、4 张 `MATCHMAKER`，139 张图总数不变。生产 handler 的 `Future` 和 `GameThreadPoolServices` 数量不变。
- 因德拉图要塞批次验证通过：`mvn -q -Dtest=InstanceHandlerRecoveryMigrationTest,RetailAiDefinitionLoaderTest,RetailPatternAI2Test test`、`mvn -q -DskipTests compile`、正式副本生成器 `--check`、静态出生/coverage/manifest XML 解析及 `git diff --check`。
- 完成暗影法庭（`320120000`，真端 `IDDC1_Arena_3F`）真端单轨替换：`214347`、`214349`、`214351`、`214353`、`214357`、`214360`、`214531` 的七把钥匙均已由生成的真端掉落表以 100% 提供；静态出生保留所有钥匙怪与出口 `700369`，任务 `_24046The_Shadow_Calls` 负责进入、退出和任务推进。
- 删除重复注册七把钥匙的 `ShadowCourtInstance`；其 423 电影在真端世界、NPC Pattern 和任务数据中均无触发来源，两套独立 5.8 实现也不包含该逻辑，因此不保留旧私服电影路径。覆盖重新生成为 `RETAIL_AI_QUEST`，来源 `_24046The_Shadow_Calls.java`；行为闭包调整为 101 张 `HANDLER`、16 张 `RETAIL_AI_QUEST`，139 张图总数不变。
- 暗影法庭批次验证通过：`mvn -q -Dtest=InstanceHandlerRecoveryMigrationTest,RetailInstanceDataTest test`、`mvn -q -DskipTests compile`、正式副本生成器 `--check`、静态出生/coverage/manifest XML 解析及 `git diff --check`。
- 完成奥德遗传实验室（`310050000`，真端 `IDLF3LP`）真端单轨收缩：钥匙来源恢复为 `212341 -> 185000001`、`212175 -> 185000002`、`212196 -> 185000003`、`212193 -> 185000004`、`212342 -> 185000005`，均使用生成的真端 100% 掉落；删除旧 handler 对 `212193` 的错误随机钥匙 3/4、对 `212202` 的错误钥匙 5 注入，以及 `212211 -> 188053787` 私服烙印包。
- `AetherogeneticsLabInstance` 从 142 行缩减为 29 行，只保留玩家离本或登出时清理五把副本钥匙；删除未使用门字段、空死亡分支、随机数、掉落注册和无调用消息方法，不保留兼容掉落。
- 奥德遗传实验室批次验证通过：`mvn -q -Dtest=InstanceHandlerRecoveryMigrationTest test`、`mvn -q -DskipTests compile`、正式副本生成器 `--check`、静态出生/真端掉落 XML 解析及 `git diff --check`。
- 完成钢铁钩号船舱（`300460000`，真端 `IDShulackShip_Solo`）旧 handler 单轨替换：以 `world_N.xml`、`NpcAIPatterns_IDShulackShip_KJS.xml`、真端 NPC/任务/掉落和正式静态 walker 为权威，条件出生新增 `lever_ver30` 世界及酒馆四组互斥 Party；两处点位按 `45% 219032 + 219003`、`5% 219039 + 219003` 对称交换，总概率严格为 100%。静态出生没有 `219032/219039`，不产生重复 Boss。
- 删除 `SteelRakeCabineInstance` 和船舱版 `AnikikiAI2`：移除手工随机 Boss、`219040` 错误任务掉落、`219033/215489/700553/700554` 私服掉落、错误 `730766` 出口、重复死亡删除和线程池技能注入。任务 `3203/4203` 的 `182209084/182209099` 继续由真端定义的 `219037` 100% 掉落负责；`219033`、`215489`、`700553`、`700554` 继续使用生成的真端掉落。
- `219033`、`219040`、`701386`、`701387` 分别由 `IDSShip_KK`、`IDSlk_Extra1`、`IDSShip_LeverA`、`IDSShip_LeverB` 接管；`219040` 使用正式静态路径 `IDShip_FShulackWiBreeder_42_Ae_Path`，删除仅供旧 AI 使用的 `3004600001` walker。`730766` 在真端属于 `IDShulackShip_02` 且本图无出生，旧分支随手刷出口删除后不可达；`730199` 的真端 Pattern 源文件当前缺失，因此保留现有独立门内传送交互，不把它计作本次兼容层。
- 覆盖重新生成为 100 张 `HANDLER`、17 张 `RETAIL_AI_QUEST`，139 张图总数不变；生产 handler 仍有 35 个声明 `Future`、61 个直接使用 `GameThreadPoolServices`。钢铁钩号船舱批次验证通过：转换器 21 项测试；`RetailPatternAI2Test` 在合并正式静态 walker 后确认四个关键对象可选择真端 Pattern；`InstanceHandlerRecoveryMigrationTest` 校验四组概率、点位和旧路径删除；条件/静态 walker XML、正式副本生成器 `--check`、编译及 `git diff --check`。
- 清理 Divine Tower L/D 迁移后遗留的两个空 handler：运行时无专用注册时本就回落公共 `GeneralInstanceHandler`，删除空类不改变副本行为。覆盖重新生成为 98 张 `HANDLER`、6 张 `MATCHMAKER`，两张图分别以 `matchmaker.xml:419`、`matchmaker.xml:421` 为行为入口；139 张图总数不变，生产 handler 的 `Future` 和直接线程池计数不变。
- 完成 Sanctuary Dungeon（`301580000`，真端 `IDF6_OP`）真端单轨替换：`703092` 使用 `LF6_F2_Din_04_Enter_Attack_67` 感知玩家阵营并设置 `IDF6_RACE_L/D`，四条条件出生生成 `806076/806080` 和 `806189/806190`，条件 ID 为 `5772..5775`；静态补入 `703092` 与 `806118`，坐标、重生和随机行走参数来自 `world_N.xml`。
- 删除 `SanctuaryDungeonInstance` 的首个进入玩家阵营字段、手工刷怪和出口刷出逻辑，不保留兼容分支。覆盖重新生成为 `97 HANDLER`、`18 RETAIL_AI_QUEST`、`6 MATCHMAKER`；生产 handler 的 `Future` 和直接线程池计数保持 `35/61`。
- Sanctuary 批次验证通过：条件转换器定向生成、`condition-spawns.xsd`、静态出生 XML、`InstanceHandlerRecoveryMigrationTest`、`RetailAiDefinitionLoaderTest`、`RetailConditionSpawnEngineTest`、`RetailPatternAI2Test`、`mvn -q -DskipTests compile`、副本生成器 `--check` 和 `git diff --check`。
- 完成阿祖图兰要塞（`310100000`，真端 `IDLF3_Castle_Lehpar`）单轨清理：真端 `world_N.xml` 的 `<activate_skill_areas>` 为空；真端 XML、恢复源码和本地任务数据中，`Q_Azoturan_Buff` 除技能 `274` 定义/字符串外没有施放入口。旧 `AzoturanFortressInstance` 会在任意玩家进入大区时向全本玩家强制施放该任务技能，属于无真端来源的私服增益。
- 删除 `AzoturanFortressInstance` 的区域监听、全本强制 Buff 和离本/登出清理，不保留兼容逻辑。静态出生、NPC Pattern、掉落和出入口不变；覆盖回到真端匹配入口 `matchmaker.xml:334,405`，行为闭包更新为 `96 HANDLER`、`18 RETAIL_AI_QUEST`、`7 MATCHMAKER`，生产 handler 的 `Future` 和直接线程池计数保持 `35/61`。
- 阿祖图兰要塞批次验证：`InstanceHandlerRecoveryMigrationTest` 断言旧 handler 删除及真端匹配归类；正式副本生成器重生成和 `--check`、`mvn -q -DskipTests compile`、相关 XML 解析及 `git diff --check`。
- 完成卡拉马提斯 A（`310010000`，真端 `idabprol1`）真端任务施法迁移：恢复源码 `FUN_180f70b20` 与任务注册表 `0x3ee/99` 证明 Hagen/Belpartan 保护技能只在飞升任务状态 `99` 的 NPC 对话中对当前玩家施放；删除区域监听时向全本玩家强制施放 `281` 的 `KaramatisInstance`，不保留兼容入口。`_1006Ascension` 在原有飞行传送前使用真端技能 `281`，静态出生、任务阶段和副本出口不变。
- 卡拉马提斯 A 批次验证：`InstanceHandlerRecoveryMigrationTest`、正式副本生成器及 `--check`、覆盖/manifest XML 解析、`mvn -q -DskipTests compile` 和 `git diff --check`；行为闭包更新为 `95 HANDLER`、`19 RETAIL_AI_QUEST`、`7 MATCHMAKER`。
- 完成阿塔夏 C（`320020000`，真端 `idabprod2`）真端任务施法迁移：恢复源码 `FUN_180f70a70` 与任务注册表 `0x7d8/99` 证明 Hagen 保护技能只在飞升任务状态 `99` 的 NPC 对话中对当前玩家施放；删除区域监听时向全本玩家强制施放 `257` 的 `AtaxiarInstance`，不保留兼容入口。`_2008Ascension` 在原有飞行传送前使用真端技能 `257`，静态出生、任务阶段和副本出口不变。
- 阿塔夏 C 批次验证：`InstanceHandlerRecoveryMigrationTest`、正式副本生成器及 `--check`、覆盖/manifest XML 解析、`mvn -q -DskipTests compile` 和 `git diff --check`；行为闭包更新为 `94 HANDLER`、`20 RETAIL_AI_QUEST`、`7 MATCHMAKER`。
- 完成克罗梅德试炼（`300230000`）私服群体 Buff 清理：真端恢复源码 `0x48AA/0x6FBA` 的电影 `454` 回调只推进任务并传送，没有技能 `19288`；真端世界没有对应激活区域，独立 5.8 实现也只有旧私服分支注入该 Buff。删除 `KromedesTrialInstance.rageOfKromede()`、`19288` 离本清理及其技能依赖，不改电影 `454/462`、真端掉落、NPC AI、阶段出生和任务逻辑，不保留兼容入口。
- 克罗梅德试炼批次验证：`InstanceHandlerRecoveryMigrationTest`、`mvn -q -DskipTests compile` 和 `git diff --check`；行为闭包仍为 `94 HANDLER`、`20 RETAIL_AI_QUEST`、`7 MATCHMAKER`，总计 `139` 张有效副本不变。
- 完成克罗梅德试炼掉落单轨化：删除 `KromedesTrialInstance.onDropRegistered()`，`216967/216968/216980/216981` 四把钥匙继续使用已生成的真端 100% 掉落；按 China `npcs_monsters.xml` 覆盖补入遗漏的 `216999 -> 185000101` 100% 掉落。`217005/217006` 回归真端 common drop groups，不再强制注入 `188052826`、`185000102`、`188053787`、随从契约或 50 个 Minium；职业宝箱也不再由 handler 手写 Corrupt Judge 装备。
- 当前掉落生成器仍只读取基础 NPC 数据，未套用 China 覆盖；本批正式数据已闭环，但重新生成会丢失 `216999 -> 185000101`。该缺口列为独立生成链治理项，不保留 Java 兼容掉落入口。
- 删除克罗梅德试炼四处私服职业宝箱：`211861` 的真端模板归属 `LF2`，`212333/212335/212338` 归属 `LF2/LF3`，四者均不在 `idcromede/world_N.xml`，其正式掉落也是 LF2/LF3 通用宝箱数据。删除 `spawnClassTreasure()` 及 `216981/216982/216999/217000` 死亡分支中的四处手刷，不保留职业分派或兼容出生；`217004/217001` 剧情 NPC 出生暂按独立证据批次继续核对。
- 删除克罗梅德试炼最终 Boss 私服经验药直发：`188900010` 是限制 40–49 级使用的商城 20% 经验药，不在 `217005/217006` 的 China 真端掉落、不在 `18602/28602/19675/29675` 真端任务奖励，独立 5.8 Kromede handler 也无此发放。删除 `ItemService.addItem()` 和无用 import，不保留替代奖励；电影 `455` 继续作为独立职责核对。
- 克罗梅德试炼三只受伤剧情 NPC 回归 Retail Pattern：`Cromede_Torture/Wife/Assijudge` 死亡或低血量分支生成 `282112/282113/282114`，其 `Cromede_*_Spawn` Pattern 再以真端坐标生成 `217004/217001/217003` 并广播 `6403/6404`。删除 handler 对 `216982/217000/217002` 的重复死亡分支和错误坐标手刷，不保留 Java 兼容出生。
- 克罗梅德试炼最终 Boss 回归真端消息链：恢复 `world_N.xml` 的静态 Angry Judge `217006`（`668.567871, 774.373657, 216.88036`）；剧情救援 Pattern 广播 `6404` 后由 `Cromede_Named_Angry` 原地生成 `217005` 并自删。删除 `onInstanceCreate()` 的私服二选一随机手刷、错误坐标以及无用 `Rnd/doors` 状态，不保留 Java Boss 选择兼容逻辑。
- 克罗梅德试炼宝库提示回归真端静态感知链：恢复源码 `FUN_180ca5db0` 证明提示由 `206163 / IDCromede_SensoryArea_BossDoor` 向进入感知区的当前玩家发送 `STR_QUEST_SAY_IDCromede_004 / 1111370`，与 `216999 / Cromede_Relic3_Noshow` 的死亡职责无关。补入 `world_N.xml` 的四点多边形、高度 `213.045425..227.045425`、真端出生 Z `219.712234` 和单播用户消息动作；删除 handler 的全本广播、`216999` 死亡分支及即时删除 `164000141`，该道具只在离本统一清理，不保留兼容入口。
- 宝库感知批次验证通过：`RetailPatternAI2Test`、`RetailAiDefinitionLoaderTest`、`KromedesTrialInstanceTest` 共 87 项测试；生产定义装载确认 `206163` Pattern 可执行、字符串和区域可解析，另通过 `git diff --check`。
- 完成克罗梅德试炼任务电影单轨替换：恢复源码 `FUN_180f961e0/FUN_180f96390` 证明 `730308 / IDCromede_FOBJ_Q18603` 在任务 `18602/28602` 阶段 1、交互 `0x2711` 且持有 `185000109` 时关闭对话、推进阶段 2、消耗钥匙、播放 `454`，并在 `653,774,216` 生成 `282089`；AionEmu 电影包不携带 Alias，因此在电影结束后无动画传送到 `IDCromede_Alias_02` 的 `687.631104,675.972412,201.040802,h=90`，不再在电影回调推进阶段。
- 删除 `Maga_Potion_Temple_VaultAI2` 的绕任务私服传送，把 `730308` 模板切到 `quest_use_item` 并注册到两份任务；恢复 `700939` 的独立 5.8 点位 `656.92,585.74,199.04`。真端 `world_N.xml` 未列出该尸体对象，但恢复源码 `FUN_180fa4fe0/FUN_180fa50a0` 明确绑定其任务行为，当前无新的运行阻断。
- 尸体 `700939` 的 `0x2712` 分支改为施放真端技能 `19288` 后推进 `2 -> 3`，删除私服系统消息 `1111307`；最终击杀只注册真端 `217005`，阶段 3 时由任务播放 `455` 并进入 REWARD，完整删除 handler 的 `217005/217006` 完成分支和区域无条件 `454`，不保留 `217006` 兼容完成路径。
- 本批实际变更为两份任务脚本、`KromedesTrialInstance`、`730308` NPC 模板、Kromede 静态出生、旧 AI 删除及两份 Kromede 回归测试；`KromedesTrialInstanceTest,KromedesTrialQuestMigrationTest,RetailPatternAI2Test,RetailAiDefinitionLoaderTest,InstanceHandlerRecoveryMigrationTest` 共 110 项测试、`mvn -q -DskipTests compile`、实例生成器 `--check`、NPC/出生 XML 解析、残留扫描和 `git diff --check` 均通过。唯一下一项仍是继续迁移剩余私有状态和线程池 handler。
- 完成 Raksang Ruins 单轨替换：`300610000` 的 108 条条件和 21 个运行变量接管三路波次、Boss、开门及阵营出生；旧 handler 从 839 行缩减为 16 行，删除波次计数、六组 `Future`、直接线程池调度、门缓存、硬编码出生和私服掉落，不保留兼容分支。真端条件出生与 Retail Pattern 未生成出口 `730445`，因此仅保留 `236306` 死亡后生成出口的最小职责。
- Raksang 集成提交 `40db7a5cc`；`RaksangRuinsRetailMigrationTest,RetailPatternAI2Test,RetailAiDefinitionLoaderTest` 通过，生产 handler 中含 `Future` 的文件降至 34 个，直接使用 `GameThreadPoolServices` 的文件降至 60 个。
- 完成 Drakenspire Depths 双图单轨替换：`301390000` 的 63 条条件、10 个变量和 `301520000` 的 35 条条件、6 个变量接管出生、波次、双生 Boss、门、电影和结束流程；删除两份 handler 中 3189 行硬编码编排、私服掉落、对象/门缓存、电影和线程任务。普通版仅保留标准退出；任务版仅保留标准退出以及离本/登出时删除钥匙 `185000219`、效果 `22778/22779`，三项清理尚无生成数据替代证据，不扩展为兼容流程。
- Drakenspire 集成提交 `4c1587197`；`DrakenspireDepthsRetailMigrationTest,RetailPatternAI2Test,RetailAiDefinitionLoaderTest` 通过，生产 handler 中含 `Future` 的文件降至 32 个，直接使用 `GameThreadPoolServices` 的文件降至 58 个。
- 完成 Fallen Poeta 单轨替换：`301660000` 的 97 条条件和 54 个变量接管 Anuhart 追击、铁栅/火海阶段、阵营出生、Boss、宝箱和出口；旧 handler 删除 788 行阶段字段、`Future`/任务容器、直接线程池调度、硬编码出生、电影和私服掉落。陷阱补给箱 `833862` 已由真端掉落数据提供 `164002346 x2`；handler 仅保留该临时道具及炮台效果 `21805/21806` 的离本/登出清理，清理职责尚无生成数据替代证据。
- Fallen Poeta 集成提交 `3e2d1f699`；`FallenPoetaRetailMigrationTest,RetailPatternAI2Test,RetailAiDefinitionLoaderTest` 通过，生产 handler 中含 `Future` 的文件降至 31 个，直接使用 `GameThreadPoolServices` 的文件降至 57 个。
- 完成 Dark Poeta 掉落单轨替换：删除 `DarkPoetaInstance.onDropRegistered` 的商城道具、活动宝箱和按玩家注入掉落共 150 行；普通 Boss/宝箱由生成的真端 NPC 掉落接管。S/A 级 Boss `237372/237373` 的真端 Pattern 在死亡时设置 `svanq_die/avanq_die`，条件出生据此生成奖励箱 `856605/856606`，最终奖励挂在奖励箱掉落而非 Boss 本体，不保留直注兼容。
- Dark Poeta 掉落集成提交 `707d7f84a`；增强 `DarkPoetaRetailMigrationTest` 覆盖 Boss 死亡变量、评级奖励箱和奖励箱掉落，`DarkPoetaRetailMigrationTest,RetailPatternAI2Test,RetailAiDefinitionLoaderTest` 通过。
- 完成 Dark Poeta 真端数据扩展：`300040000` 从 5 条条件/3 个变量扩展为 67 条条件/16 个变量，纳入 291 条 `instant_dungeon_define.xml` 定义和 `Objects.xml` 采集物 ID；`401111 -> 200`、`401112 -> 50` 由真端 `score_gather_IDLF1_*` 行提供。普通服固定使用 `SpecialServer_Cond=0` 和 spawn page 1，不混用 SP 阈值、NPC 或流程；数据提交为 `5d525affe`。
- 完成 Dark Poeta 真端结算规则：`InstanceSettlementService` 从生成数据读取 120 秒准备、14400 秒时限和 600 秒离场时间，评级同时满足最低积分与严格 `< TIME_MAXIMUM`；S 级最高进入玩家等级 `< 55` 写 `GRADE=1`，`>= 55` 写 `GRADE=6`，显示评级仍为 S。最终奖励由 `GRADE` 选择评级 Boss，再由 `BOSS_KILL`、`svanq_die/avanq_die` 生成出口/奖励箱并走真端掉落，不调用通用 timeattack 直发；公共规则提交为 `392ece755`。
- 完成 Dark Poeta handler 单轨替换：NPC 分数统一读取 `RETAIL_AI_DATA.getNpcScore()`，击杀/采集事件、总分、击杀数、采集数、最高进入等级、评级、电影单次标记和条件变量写入 `InstanceRuntimeState`；准备、到期、结算、离场及 Marabata 控制器改为绝对 deadline，重启恢复后不重复积分、评级或电影。删除硬编码积分/阈值、`Future`、直接线程池、计时字段、等级 Boss/Anuhart/出口/宝箱/阵营 NPC 手工出生及旧心脏最终形态流程；handler 提交为 `e4442a5b1`。
- Dark Poeta 基础出生缺口已关闭：`idlf1/world_N.xml` 证明 `215429` 和 `215430` 在 page 1 各有两个独立 `no_respawn` 点位，静态出生按 `difficult_id=1` 写入全部四点，删除旧 handler 的二选一随机手刷和恢复状态。电影 `426/427`、门 `33`、Marabata 控制器 30 秒重生仍无条件出生或 Retail Pattern 的等价接管证据，因此保留为可恢复的最小交互职责，不作为兼容分支。
- Dark Poeta 收口验证通过：`InstanceSettlementServiceTest,DarkPoetaInstanceTest,DarkPoetaRetailMigrationTest,InstanceHandlerRecoveryMigrationTest` 共 39 项测试，`mvn -q -DskipTests compile` 和 `git diff --check` 通过；仅有 Lombok 调用 JDK `Unsafe` 的弃用警告。生产 handler 残留更新为 30 个含 `Future`、56 个直接使用 `GameThreadPoolServices`。
- 全量测试首次收口发现两项确定性失败：Dark Poeta 条件出生由 5775 净增 62 条至 5837 后完整加载基线未同步，以及 `8edbaa541` 的统一匹配改造删除旧 Dredgion 登录调用后未在新服务保留非活动等级档位的入口关闭包。`9987d9096` 同步加载基线，并由 `RetailMatchmakingService` 按真端定义统一发送 18 个非 Tournament HUD 入口的 open/close 状态，不恢复旧 `DredgionService2` 双轨调用；`RetailAiDefinitionLoaderTest,DredgionService2Test,MatchDefinitionTest` 已通过。
- 全量收口复验通过：`mvn -q test` 共 1753 项测试、0 失败、0 错误、1 跳过；`python3 scripts/generate_retail_instance_data.py --check` 确认 139 张有效地图、134 张标准图和 5 张特殊图生成结果幂等；`mvn -q -DskipTests compile`、Dark Poeta 三份关键 XML 校验和 `git diff --check` 通过。测试日志中的中断、损坏 GZIP、路径 worker 失败及启动失败均为对应测试主动覆盖的异常分支。
- 完成 `instance_bonusattr.xml` 真端单轨接管：正式副本生成器新增第 7 份输出 `bonus-attributes.xml`，将 18 个 Buff 的属性名、加值/百分比和值完整映射到 `StatEnum`/`Func`；`RetailInstanceData` 启动校验定义数和 manifest 闭包，`InstanceBuff` 直接读取统一数据。删除旧 `InstanceBuffData`、两份 JAXB 模型、Encom 简化 XML/XSD、主静态表 import/include 和旧日志键，不保留兼容加载；真端 `7/8` 号 Buff 的 `PVP_DEFEND_RATIO` 与 `ABNORMAL_RESISTANCE_ALL` 均恢复为 `9999`。数据提交为 `5a13c84f1`。
- Retail AI 公共状态复审确认 `22146d6e3` 已删除 10 个 `Map<WorldMapInstance,...>` 和 2 个全局玩家状态表；持久机制状态进入 `InstanceRuntimeState`，不可序列化对象进入 `WorldMapInstance` 瞬态容器，销毁路径统一清理。新增 `RetailAiInstanceStateMigrationTest` 防止六个引擎重新引入实例静态 Map，并将旧空实例任务所有权保留为独立未完成项。
- 修复全图启动时的真端条件表达式阻断：解析器不再把 `24_middle`、`1141_out` 等数字开头变量截断为整数，并按真端 `ab1/world_N.xml` 的实际数据容许仅在表达式末尾缺失闭合括号。`RetailConditionSpawnEngineTest` 新增原始表达式回归并逐条解析生成文件全部 5837 条条件；修复提交为 `ca16a89e3`。
- 本批验证通过：正式副本生成器生成与 `--check`，`bonus-attributes.xml`/`manifest.xml` XSD 校验，`RetailInstanceDataTest,PvPArenaPlayerRewardTest,TreasureIslandRewardTest,DataManagerTest` 共 10 项测试，`RetailConditionSpawnEngineTest` 全部测试，`mvn -q -DskipTests compile` 和 `git diff --check`。

- 完成 The Eternal Bastion（`300540000`）状态迁移：准备窗口、1800 秒战斗期限、完成/失败评级、离场 deadline、积分、击杀数和玩家奖励标记统一写入 `InstanceRuntimeState`，重启后由 `restoreDeadline()`/`restoreScore()` 恢复；奖励统一经 `InstanceSettlementService.timeAttackPlan()` 与 `settleTimeAttack()` 幂等结算。`RetailPatternAI2` 的 `give_score` 负责 `score_apply_type=3`，handler `onDie` 对已接管类型不再重复计分；炮台物品和离本效果清理保留为真端未表达的交互职责。
- 重新生成 `300540000` 条件出生：保留 `Race`、`Wave_Z1`、`castle_gate_02_Bomb`、`timewave_down` 和终局 Boss/门条件，共 302 条条件、302 个槽位、43 个变量；修正生成结果中两条 `Race == 2(Race == 2)` 非法表达式为 `(Race == 2)`，全量条件表达式逐条解析通过，加载基线更新为 6000 条。
- 修复条件生成器的无条件 Retail AI 变量生产者闭包：无条件生产者作为锚点，避免无效 `fire*` 变量依赖传播而错误删掉 `Race/Wave_Z1`；`aion_drop` 提交 `3620311`，生成器回归测试 24 项通过。
- The Eternal Bastion 仍保留 `802185` 的四项机会奖励 hook，原因是当前仓库没有中国区 5.8 真端掉落行证据；Ashunatal 的 `243816` 条件出生及 `243807/243816/243817/243818` 全局积分、Crucible/Shugo 条件闭包、Bastion 终局出生、Dredgion 最终 Rank 奖励和 Iron Wall/Stonespear/Evergale 的部分真端证据仍列为未闭环项，不以硬编码替代证据。
- 本批验证：`mvn -q -Dtest=TheEternalBastionMigrationTest,RetailConditionSpawnEngineTest test`、条件加载器全量解析、`mvn -q -DskipTests compile` 和 `git diff --check`；仅保留 Lombok 调用 JDK `Unsafe` 的弃用警告。

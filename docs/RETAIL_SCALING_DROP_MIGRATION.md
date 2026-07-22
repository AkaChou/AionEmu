# 真端 ScalingDrop 迁移评估与实施方案

> 状态：已实现首版（2026-07-22）。
> 范围：中国区 5.8 `ScalingDrop`，包含离线转换、运行时单轨路由和外层掉落倍率加成。
> 结论：按真端的“替代普通 NPC 掉落”语义单轨接管，不能把 ScalingDrop 结果追加到当前 `npc_drops`。

## 1. 结论

技术上可以迁入，所需真端数据、NPC 引用和本地物品映射均已具备。

但不能把两个真端 XML 直接复制进 AionEmu，也不能只把物品平铺到现有掉落组：

1. 真端在掉落生成入口选择普通 `NPC_GetDropItemsList(...)` 或 `ScalingDrop`，不是两条路径相加；
2. ScalingDrop 是两级随机：先独立判定掉落集合，再在集合内按权重只选一项；
3. 外层概率使用千分制，内层权重使用万分制；
4. 外层还受玩家等级、职业、种族和掉落倍率影响；
5. 当前数据已把部分 ScalingDrop 物品平铺进 `npc_drops`，直接新增运行路径会重复掉落。

推荐方案是：离线转换真端三类关系，生成按 `npc_id` 索引的紧凑 ScalingDrop 定义；运行时在 `DropRegistrationService` 中对有定义的 NPC 替代普通 `NpcDrop` 计算。Quest、活动、全局掉落和副本 handler 仍按现有顺序处理。

## 2. 权威证据

### 2.1 真端数据

权威数据位于：

```text
/Users/mc/IdeaProjects/58Server/Map/XML/monster_scaling_drop.xml
/Users/mc/IdeaProjects/58Server/Map/XML/monster_scaling_drop_setList.xml
/Users/mc/IdeaProjects/58Server/Map/XML/China/npcs.xml
/Users/mc/IdeaProjects/58Server/Map/XML/China/npcs_npcs.xml
/Users/mc/IdeaProjects/58Server/Map/XML/China/npcs_monsters.xml
```

其中：

- `npcs*.xml`：NPC 通过 `<scaling_drop>` 引用 ScalingDrop 名称；
- `monster_scaling_drop.xml`：定义外层集合、适用等级、职业、种族和外层概率；
- `monster_scaling_drop_setList.xml`：定义集合内物品、数量和权重。

中国区主表实测：

| 数据 | 数量 | 说明 |
| --- | ---: | --- |
| ScalingDrop 定义 | 22 | 5 个测试定义，17 个生产定义 |
| ScalingDropSet 定义 | 26 | 3 个测试定义，23 个生产定义 |
| 中国区 NPC 引用 | 22 | 全部能映射到当前 `NpcTemplate` |
| 生产物品行 | 25 | 24 个唯一物品名，全部能映射到当前物品 ID |

`Special01` 区域文件只有测试和早期活动定义，生产迁移应使用基础表加 China NPC 引用，不应拿 `Special01` 覆盖中国区主数据。

### 2.2 真端执行逻辑

恢复代码位于：

```text
/Users/mc/IdeaProjects/58Server/server58-source/NPCServer_NPCSvr64/classes/Misc/ScalingDropInfo.cpp
/Users/mc/IdeaProjects/58Server/server58-source/NPCServer_NPCSvr64/classes/Misc/ScalingDropSet.cpp
/Users/mc/IdeaProjects/58Server/server58-source/NPCServer_NPCSvr64/fun/fun_026.cpp
/Users/mc/IdeaProjects/58Server/server58-source/NPCServer_NPCSvr64/fun/fun_007.cpp
```

已恢复的语义：

1. 玩家等级、职业、种族必须同时匹配外层规则；
2. 外层为独立判定，随机范围 `1..1000`，`rate=1000` 表示 100%；
3. 每个命中的外层规则执行一次对应 ScalingDropSet；
4. 内层随机范围 `1..10000`，按累计权重选中最多一个物品；
5. 内层权重总和不足 10000 时，剩余区间表示不产出物品；
6. 多个外层规则彼此独立，因此同一 NPC 可以同时产生多个物品；
7. 调用者按能力分支在普通掉落与 ScalingDrop 之间二选一。

最后一点决定迁移边界：ScalingDrop 不是普通 NPC 掉落的附加层，而是这些 NPC 的替代掉落来源。

### 2.3 尚未完全命名的真端倍率参数

恢复代码已经证明外层概率会经过倍率、等级条件、上限和服务器全局修正，但以下全局量仍是反编译地址，尚未恢复业务名称：

```text
DAT_14056cc04
DAT_14056cc08
DAT_14056cc0c
DAT_1417817c8
DAT_1417817cc
```

因此可以实现结构和基础概率完全一致，但在这些常量映射完成前，不能宣称“所有倍率配置下 100% 真端一致”。第一版应明确使用现有 `DropModifiers` 只修正外层概率一次，并把这项差异留作验收门禁，而不是猜常量含义。

## 3. 当前 AionEmu 状态

当前运行链路：

```text
DropRegistrationService.registerDrop(...)
  -> npc.getNpcDrop()
  -> NpcDrop.dropCalculator(...)
  -> DropGroup.dropCalculator(...)
  -> Quest / Event / Global / InstanceHandler / AI
```

当前没有：

- NPC 到 ScalingDrop 的引用；
- ScalingDrop/ScalingDropSet 数据模型和加载器；
- 等级、职业、种族的 ScalingDrop 过滤；
- “先外层判定、再内层权重选择”的两级计算；
- ScalingDrop 替代普通 NPC 掉落的路由。

当前可复用：

- `DropRegistrationService` 的掉落登记、所有权和索引；
- `DropItem` 的数量、拾取和分配流程；
- `DropModifiers` 的本服倍率与等级差修正入口；
- `NpcDropData` 的目录加载、重载和失败即停模式；
- `//reload drop` 的运维入口；
- 当前物品模板和全部 24 个生产物品名映射。

不需要新增掉落服务、接口、工厂、事件总线或配置模式。

## 4. 当前数据覆盖与缺口

当前 `npc_drops` 已平铺了部分与 ScalingDrop 相同的物品。这些不是完整实现，只是普通 `items_info`/生成数据与 ScalingDrop 的局部重合。

按 ScalingDrop 分量审计，22 个引用 NPC 中：

- 15 个在默认倍率下已有相同或空结果；
- 7 个缺失或语义不等价；
- 迁入运行时 ScalingDrop 前，22 个都必须切换为单轨所有权，不能保留普通 NPC 掉落并追加。

关键缺口：

| NPC | 真端 ScalingDrop | 当前状态 | 结论 |
| --- | --- | --- | --- |
| `246327` | 普通门钥匙 1 个，100% | 无 NPC 掉落定义 | 缺失 |
| `246328` | 奖励门钥匙 1 个，100% | 无 NPC 掉落定义 | 缺失 |
| `246381` | 变身活动物品 1 个，100% | 无 NPC 掉落定义 | 缺失 |
| `835730` | 高级宝物包 100% + 充能碎片 20% + 高级魔石包 100% | 宝物包与碎片被放在同一组选一；魔石变为普通公共组 | 结构错误 |
| `835731` | 低级宝物包 100% + 低级魔石包 100% | 只有宝物包和普通公共组 | 物品形态不一致 |
| `835732` | 魔法球 1 个，100% | 只有 0.1% 活动杂物 | 缺失 |
| `835733` | 勋章包 100% + 烙印包 100% + 进化石 12 个 100% + 深渊包装备组 20% | 只有 0.1% 活动杂物 | 主要奖励全部缺失 |

`835733` 的 20% 深渊包装备组不是三次独立判定。真端在组命中后按 `10% / 50% / 40%` 权重只选一个，即总体概率分别为：

```text
武器包 2%
防具包 10%
首饰包 8%
无该组物品 80%
```

## 5. 为什么不能直接复用普通 DropGroup

现有 `DropGroup` 每组只生成一次随机数，并用候选概率阈值选择最接近的一项。它可以在默认倍率下用累计阈值模拟一个 ScalingDropSet，但不能完整替代真端语义：

1. 当前模型没有玩家等级和职业过滤；
2. 多个外层集合必须各自独立判定，不能合并为一个 `common_0/common_1`；
3. 掉落倍率应只作用于外层集合概率，内层权重比例保持不变；
4. 当外层概率被倍率推到上限时，直接缩放每个累计阈值会改变内层分布；
5. 当前普通掉落和 ScalingDrop 在真端是替代关系。

因此，只修改 XML 可以修复默认 1 倍下的当前中国区数据，但不能称为完整 ScalingDrop 迁移。

## 6. 推荐目标设计

### 6.1 离线转换，不在运行时解析原始真端 XML

在以下源数据仓库扩展现有生成器：

```text
/Users/mc/PycharmProjects/aion_drop/staticdata_converter/
```

转换输入：

```text
China/npcs*.xml
monster_scaling_drop.xml
monster_scaling_drop_setList.xml
Items.xml / 本地物品名称映射
```

转换时完成：

1. 以 China NPC 引用为入口，只输出实际使用的生产定义；
2. 将 ScalingDrop 名称解析为定义；
3. 将 set 名称解析为 ScalingDropSet；
4. 将物品名解析为 AionEmu `item_id`；
5. 将职业、种族和等级范围转换为本地枚举/位掩码；
6. 保留原始 `outer_rate`、`item_weight` 和数量，不提前相乘；
7. 生成确定性 UTF-8 紧凑文件和来源清单；
8. 任一引用、物品或枚举无法解析时生成失败。

建议只生成一个运行时文件，避免在 Java 中继续维护两级名称表：

```text
definitions/compact/npc_drops/scaling_drops.xml
```

示意结构：

```xml
<scaling_drops>
  <npc id="835733" source="scaledrop_IDAbRe_Core_03_Witch_Boss_Ae">
    <set rate="1000" min_level="1" max_level="75">
      <item id="188058117" count="1" weight="10000"/>
    </set>
    <set rate="1000" min_level="1" max_level="75">
      <item id="188058118" count="1" weight="10000"/>
    </set>
    <set rate="1000" min_level="1" max_level="75">
      <item id="190200000" count="12" weight="10000"/>
    </set>
    <set rate="200" min_level="1" max_level="75">
      <item id="188058130" count="1" weight="1000"/>
      <item id="188058131" count="1" weight="5000"/>
      <item id="188058132" count="1" weight="4000"/>
    </set>
  </npc>
</scaling_drops>
```

### 6.2 最小运行时模型

只需要一个按 NPC ID 索引的数据模型，可放入现有 `NpcDropData` 所有权下：

```text
ScalingDropData
  npcId -> List<ScalingDropSet>

ScalingDropSet
  outerRate       0..1000
  minLevel/maxLevel
  classMask
  raceMask
  List<ScalingDropItem>

ScalingDropItem
  itemId
  count
  weight          0..10000
```

不需要保留真端的字符串名称查找，也不需要让 `NpcTemplate` 增加 `scalingDropName`；转换器已经能把 NPC 引用解析为最终 `npc_id`。

### 6.3 运行路由

在 `DropRegistrationService.registerDrop(...)` 中只替换普通 NPC 掉落这一段：

```text
if npcId has ScalingDrop:
    execute ScalingDrop
else:
    execute current NpcDrop

continue existing Quest / Event / Global / handler / AI flow
```

禁止：

- 先执行 `NpcDrop` 再追加 ScalingDrop；
- 增加 `legacy/hybrid/retail` 配置开关；
- 定义缺失时悄悄回退普通掉落；
- 在 AI 或副本 handler 中为 7 个缺口 NPC 手写奖励。

有 `<scaling_drop>` 引用但定义无效时应在加载阶段失败，而不是在 NPC 死亡时静默无掉落。

### 6.4 计算顺序

每个外层集合按以下顺序执行：

1. 检查玩家等级范围；
2. 检查玩家职业；
3. 检查玩家种族；
4. 将 `outerRate / 10f` 转为百分比；
5. 使用现有 `DropModifiers` 对外层概率修正一次；
6. 外层未命中则跳过该集合；
7. 外层命中后，在 `1..10000` 内按累计 `weight` 选最多一个物品；
8. 使用现有 `DropItem` 注册数量和拾取归属。

内层权重禁止再次经过掉落倍率或等级差修正。

第一版使用 `DropModifiers` 是本服兼容选择；恢复并命名真端倍率常量后，再决定是否替换为真端专用外层公式。

## 7. 实施阶段

### 阶段 A：转换器与审计报告（已完成）

目标：先证明生成数据闭包，不接运行路径。

- 解析 17 个生产 ScalingDrop、23 个生产 set；
- 解析 22 个中国区 NPC 引用；
- 解析 25 个生产物品行和 24 个唯一物品；
- 输出 `scaling_drops.xml`；
- 输出引用、物品、过滤条件和来源哈希报告；
- 生成器二次运行必须字节一致。

### 阶段 B：数据加载与验证（已完成）

目标：加载紧凑定义，不改变掉落结果。

- `NpcDropData` 同目录加载 `scaling_drops.xml`；
- 建立不可变 NPC ID 索引；
- 校验重复 NPC、空 set、概率范围、权重范围和物品存在性；
- `//reload drop` 同时原子替换普通和 ScalingDrop 数据。

### 阶段 C：单轨接管（已完成）

目标：22 个引用 NPC 改由 ScalingDrop 计算。

- 在 `DropRegistrationService` 路由普通掉落或 ScalingDrop；
- 保持 Quest、活动、全局掉落和 handler 顺序；
- 不修改拾取、分配、宠物/守护星自动拾取；
- 删除或停止生成这 22 个 NPC 中仅用于普通回退的重复平铺掉落。

### 阶段 D：真端倍率闭包（后续门禁）

目标：从“结构正确”提升到“全部倍率配置真端一致”。

- 恢复 `param_4`、`param_5` 和五个全局量的业务含义；
- 对照真端倍率、等级差、软上限 700、硬上限 1000 和全局修正；
- 有证据后再替换第一版 `DropModifiers` 适配，不猜测常量。

## 8. 测试与验收

### 8.1 转换器测试

至少覆盖：

- 名称和 ID 全部可解析；
- China NPC 引用闭包为 22；
- 生产定义为 17，生产 set 为 23；
- 生产物品行为 25，唯一物品为 24；
- `rate=0` 保留且不产出物品；
- `apply_level`、`apply_class`、`apply_race` 能被解析；
- 第二次生成无 diff。

### 8.2 Java 单元测试

最小测试集：

1. 等级、职业、种族任一不匹配时 set 不执行；
2. 多个外层 set 独立命中并能产出多个物品；
3. 一个内层 set 最多选择一个物品；
4. 权重和不足 10000 时允许空结果；
5. 倍率只作用外层，不改变内层权重比例；
6. 有 ScalingDrop 的 NPC 不再执行普通 `NpcDrop`；
7. 无 ScalingDrop 的 NPC 保持当前行为。

必须使用两个真实数据夹具：

- `246327`：验证缺失门钥匙恢复；
- `835733`：验证三个必掉集合和一个 20% 加权集合。

### 8.3 数据验收

- `xmllint`/XSD 验证通过；
- 22 个 NPC 均存在本地模板；
- 24 个生产物品均存在本地模板；
- 无未知职业、种族或等级范围；
- 无重复 NPC 所有权；
- `//dropinfo` 能区分普通掉落和 ScalingDrop 来源；
- 运行时重载后索引和 XML 属于同一版本，不允许部分替换。

### 8.4 行为验收

- `246327/246328/246381/835732/835733` 缺失奖励恢复；
- `835730` 的两个 100% set 与一个 20% set 可以同时产出；
- `835733` 的三个必掉 set 不互斥；
- `835733` 的深渊三选一不会一次掉出多件；
- 当前 15 个已平铺引用不会重复掉落；
- Quest、活动、全局和副本 handler 掉落不回归。

## 9. 风险与门禁

| 风险 | 影响 | 门禁 |
| --- | --- | --- |
| 把 ScalingDrop 追加到普通掉落 | 15 个已覆盖 NPC 重复奖励 | 路由必须二选一 |
| 把多个外层 set 合并 | 必掉物互斥，`835730/835733` 结果错误 | 每个外层 set 独立滚动 |
| 对内层权重再次套倍率 | 加权分布改变 | 倍率只作用外层 |
| 直接加载原始 UTF-16/DTD | 运行时复杂且区域选择不清 | 离线转 UTF-8 紧凑定义 |
| 用普通公共组代替包装物 | 物品身份、开启行为和客户端表现变化 | 保留真端 wrapper item ID |
| 猜测反编译全局常量 | 高倍率和等级差行为错误 | 阶段 D 证据门禁 |
| 单独手改 AionEmu XML | 下次生成被覆盖 | 修复必须落在 `aion_drop` 生成源 |

## 10. 完成标准

以下全部满足后，才能称为 ScalingDrop 迁移完成：

- [ ] 17 个生产定义、23 个生产 set、22 个 NPC 引用全部由转换器生成；
- [ ] 25 个生产物品行全部解析，24 个唯一物品全部存在；
- [ ] 22 个引用 NPC 使用 ScalingDrop 单轨接管普通 NPC 掉落；
- [ ] 外层过滤、独立概率和内层加权选择都有测试；
- [ ] 当前重复平铺数据已清理或停止生成；
- [ ] `246327` 和 `835733` 真实夹具测试通过；
- [ ] 重载、`//dropinfo` 和运行时数据版本一致；
- [ ] 真端倍率常量完成命名和行为对照，或明确记录第一版仅使用本服 `DropModifiers` 的兼容差异；
- [ ] 生成器、紧凑数据、Java 路由和测试作为一个发布单元验证。

## 11. 当前实现边界

已修改：

- `NpcDropData` 同目录加载 `scaling_drops.xml`；
- `DropRegistrationService` 按 NPC ID 在普通掉落与 ScalingDrop 之间二选一；
- 外层使用 `DropModifiers`，内层权重不套倍率；
- `aion_drop/staticdata_converter/generate_scaling_drops.py` 生成 UTF-8 紧凑数据。

仍待真端证据闭包：恢复反编译全局倍率常量的业务名称和全部高倍率边界行为。

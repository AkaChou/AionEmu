# 静态数据迁移计划

## 目标

`src/main/resources/aion/definitions` 只保存已经完整接入运行时并通过聚焦验证的真端转换数据。不能可靠替换现有功能的 compact 数据不迁入 `main`，继续由旧 JAXB 静态数据提供。

## 目录

- `definitions/compact/world.xml`、`id-mappings.xml`：风道及地图名称映射的直接运行来源。
- `definitions/compact/npc-skills.xml`：NPC 技能的直接运行来源。
- `definitions/compact/pets-rides.xml`：宠物自动使用药剂与商店价格倍率的直接运行来源；其余宠物子表按兼容性逐项处理。
- `definitions/schemas/`：上述运行 XML 对应的最小 XSD 集合。
- `cache/`：派生的 JAXB 合并缓存，不是权威数据源。

## 迁移顺序

1. 只将运行时已经直接读取的真端转换 XML 迁入 `main`。
2. 风道已直接使用 `compact/world.xml` 与 `id-mappings.xml`，不再保留旧风道表。
3. NPC 技能已直接使用 `compact/npc-skills.xml`，不再保留旧 NPC 技能表。
4. 宠物药剂和商店表已直接使用 `compact/pets-rides.xml`，不再保留旧表。
5. 其余领域只有在完整映射并通过聚焦验证后，才把对应真端 XML 加入 `definitions`。

## 验证记录

已记录且对应代码未变化的验证，提交前不重复执行。

| 提交 | 范围 | 验证 | 结果 |
|---|---|---|---|
| `1bd0be1d` | 直接运行 definitions 数据 | 4 份运行 XML 逐份 XSD 校验、文件范围审计 | 2026-07-13 通过；仅保留 4 个运行 XML 与 4 个 XSD |
| `0ccafb5e` | world.xml 风道 | `AionServicePathsTest`、`WindstreamDefinitionLoaderTest`、`GameServerTest`、`XmlDataLoaderTest` 聚焦方法 | 2026-07-13 通过；未启动项目 |
| `c93eaa20` | NPC 技能 | `NpcSkillDefinitionLoaderTest`、`PriestAI2Test`、`XmlDataLoaderTest` 聚焦方法、`GameServerTest` | 2026-07-13 通过；59058 个 NPC 分配可加载，未启动项目 |
| `108e259e` | 宠物药剂与商店 | `XmlDataLoaderTest` 宠物药剂与商店加载/主 XSD、`GameServerTest` | 2026-07-13 通过；33 条药剂规则和 5 条商店倍率直接从 compact 数据加载，未启动项目 |

## 待实现或无法可靠映射

- 紧凑数据包使用字段字典和面向源文件的结构；不能匹配现有 JAXB 模型的领域，需要最小的显式加载器或替代数据容器。
- `world.xml` 风道已经实现：141 个风道组引用 111 条 WIF 轨迹；客户端值 `405001` 可正确归一为风道组 `405`；进入和移动位置按真端的 45/50 单位阈值校验。旧地图 ID `600040000`、`600050000` 继续复用 `Tiamat_Down`、`LDF5a` 客户端地图，此映射已由 `aion-geo` 验证。
- `fly_path.xml` 不包含初始启用状态；动态风道 301、302 继续保持初始关闭，由现有 NPC AI 开启。
- `compact/npc-skills.xml` 已直接替换旧 NPC 技能表：10743 个共享组展开为 59058 个 NPC 分配。663 个唯一技能节点只有真端名称而没有可解析 ID；加载器保留对应 NPC 分配但跳过不可执行技能，不猜测 ID。
- `compact/pets-rides.xml` 的 `toypet_doping.xml` 有 33 条规则，其中现有 31 条在饮料、食物和卷轴字段上逐项一致；`toypet_merchant.xml` 的 5 条价格倍率与现有表全部一致。启动通过 `PetDefinitionLoader` 直接流式解释这两张新表，旧 `pet_doping.xml`、`pet_merchand.xml` 及其 XSD 已删除。
- 字段行为若无法从 58Server 真端、`aion-server`、转换器或 5.8 客户端证明，必须先记录在此处，才能移除该领域的兼容数据源。

## 大型生成文件

`main` 不保存未接入运行时的全量转换输出、gzip、manifest 或 catalog；仅保留上面列出的 4 个运行 XML，均小于 100 MB。

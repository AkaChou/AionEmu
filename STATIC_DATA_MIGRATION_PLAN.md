# 静态数据迁移计划

## 目标

`src/main/resources/aion/definitions` 是唯一的新静态定义根目录，内容为完整的 `converted_staticdata_v2` 输出。先保持现有 AionEmu 功能，再逐领域使用这些定义替换 JAXB 旧数据。掉落继续使用当前已验证的数据，仅迁移目录，不重新生成。

## 目录

- `definitions/compact/`：领域加载器使用的正式 XML 数据包。
- `definitions/schemas/`：转换器输出的 XSD。
- `definitions/manifest.json`、`catalog.json`、校验与修复报告：记录数据来源和转换结果。
- `definitions/gzip/`：与 XML 等价的压缩副本；超大领域可直接使用。
- `definitions/npc_drops/`：当前 AionEmu 掉落数据的权威目录；数据由旧目录原样迁移，不重新生成。
- `definitions/items/item/`：当前 AionEmu 物品运行模板的兼容目录；启动和热重载均从这里加载。
- `definitions/skills/`：当前 AionEmu 技能运行模板的兼容目录；启动和热重载均从这里加载。
- `cache/`：派生的 JAXB 合并缓存，不是权威数据源。

## 迁移顺序

1. 将转换器完整输出导入 `definitions` 并核对清单。
2. 按领域保持现有玩法并逐步替换：世界与风道、掉落、物品、技能、NPC 与 NPC 技能、任务与配方、传送门与副本，最后处理其余数据容器。
3. 已将当前 `npc_drops` 原样迁移到 `definitions/npc_drops`，启动加载、热重载、掉落导出和测试均使用新路径。
4. 新 `compact/items.xml` 暂不能无损生成现有运行模型，已先把现有物品模板迁入 `definitions/items/item`，保持装备、动作和属性语义。
5. 新 `compact/skills.xml` 暂不能无损生成现有运行模型，已先把现有技能模板迁入 `definitions/skills`，保持效果、动作、条件和 motion 语义。
6. NPC 技能已直接使用 `compact/npc-skills.xml`，不再保留旧 `npc_skills.xml`。
7. 每个领域的新定义加载器和聚焦测试通过后，才删除对应的旧 `static_data` 导入。
8. 所有 `DataManager` 数据容器都有已验证的新来源后，再移除最后的 JAXB 合并缓存假设。

## 验证记录

已记录且对应代码未变化的验证，提交前不重复执行。

| 提交 | 范围 | 验证 | 结果 |
|---|---|---|---|
| `3f4636a8` | definitions 数据导入 | 转换器完整单测、全量生成、往返/XSD 报告、100 MB 文件审计 | 2026-07-12 通过；所有文件均小于 100 MB |
| `279beedb` | world.xml 风道 | `mvn -q -Dtest=AionServicePathsTest,WindstreamDefinitionLoaderTest,GameServerTest,XmlDataLoaderTest#staticDataSectionCountUsesTopLevelXmlElements test` | 2026-07-12 通过；未启动项目 |
| `bc40031a` | NPC 掉落 | `NpcDropDataTest`、`NpcTemplateDropLoadingTest`、`GameServerTest` 及 `XmlDataLoaderTest` 掉落相关方法 | 2026-07-12 通过；全部现有分片可急切加载，未启动项目 |
| `cc01713a` | 物品模板 | `XmlDataLoaderTest` 物品相关方法、`DataManagerTest`、`AwakeningScrollSkillDataTest`、`DyeActionTest`、`GameServerTest`、`AionServicePathsTest` | 2026-07-12 通过；128629 个模板可加载，未启动项目 |
| `ec392399` | 技能模板 | `XmlDataLoaderTest` 技能相关方法、`AwakeningScrollSkillDataTest`、`MinionTransformSkillDataTest`、`HotReloadDataTest`、`SkillTemplateTest` | 2026-07-12 通过；14480 个模板及冷却组可加载，未启动项目 |
| 待提交 | NPC 技能 | `NpcSkillDefinitionLoaderTest`、`PriestAI2Test`、`XmlDataLoaderTest` 静态 XSD/分区方法、`GameServerTest` | 2026-07-12 通过；59058 个 NPC 分配可加载，未启动项目 |

## 待实现或无法可靠映射

- 紧凑数据包使用字段字典和面向源文件的结构；不能匹配现有 JAXB 模型的领域，需要最小的显式加载器或替代数据容器。
- `world.xml` 风道已经实现：141 个风道组引用 111 条 WIF 轨迹；客户端值 `405001` 可正确归一为风道组 `405`；进入和移动位置按真端的 45/50 单位阈值校验。旧地图 ID `600040000`、`600050000` 继续复用 `Tiamat_Down`、`LDF5a` 客户端地图，此映射已由 `aion-geo` 验证。
- `fly_path.xml` 不包含初始启用状态；动态风道 301、302 继续保持初始关闭，由现有 NPC AI 开启。
- NPC 掉落继续使用现有 26 个正式分片、公共掉落组和旧表备份；`static_data.xml` 不再合并掉落，启动与热重载统一通过 `XmlDataLoader.loadNpcDropData()` 从 `definitions/npc_drops` 加载。
- `compact/items.xml` 有 128380 条客户端源记录和 307 个字段；现有运行数据有 128629 个唯一模板，并额外依赖派生掩码、分类、装备槽、说明 ID、嵌套武器属性、修饰器和道具动作。当前参考代码不足以证明这些派生关系全部可逆，因此暂不直接替换；后续只有在转换器能生成完整 `ItemTemplate` 并通过行为覆盖审计后，才删除兼容模板。
- `compact/skills.xml` 有 14494 条客户端源记录，现有运行数据有 14480 个模板，并额外包含嵌套属性、起始/使用/装备条件、效果、动作、周期动作和 motion。当前参考代码不足以证明所有字段到运行类的映射，因此暂不直接替换；启动与热重载统一通过 `XmlDataLoader.loadSkillData()` 从兼容目录加载。
- `compact/npc-skills.xml` 已直接替换旧 NPC 技能表：10743 个共享组展开为 59058 个 NPC 分配。663 个唯一技能节点只有真端名称而没有可解析 ID；加载器保留对应 NPC 分配但跳过不可执行技能，不猜测 ID。
- 字段行为若无法从 58Server 真端、`aion-server`、转换器或 5.8 客户端证明，必须先记录在此处，才能移除该领域的兼容数据源。

## 大型生成文件

转换器以 95 MiB 为上限按完整 XML 成员自动分片。当前普通动画包和地区动画对象包各生成 3 个分片，最大文件分别为 99488612、99317630 字节；全输出没有超过 100 MB 的文件。

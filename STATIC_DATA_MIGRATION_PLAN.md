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
- `definitions/items/sets/`：当前 AionEmu 物品套装部件与奖励属性的兼容目录。
- `definitions/items/random_bonuses/`：当前 AionEmu 背包洗练与伊甸石随机属性组的兼容目录。
- `definitions/items/groups/`：当前 AionEmu 奖励池、制作材料与宠物食物分组的兼容目录。
- `definitions/items/assembly/`：当前 AionEmu 组合道具产物、部件与数量的兼容目录。
- `definitions/items/multi_return/`：当前 AionEmu 多目的地回城卷轴地图列表的兼容目录。
- `definitions/items/disassembly/`：当前 AionEmu 物品拆解分组、概率与奖励的兼容目录。
- `definitions/items/enchant/`：当前 AionEmu 物品强化与授权属性表的兼容目录。
- `definitions/items/upgrade/`：当前 AionEmu 物品净化升级产物、条件与成本的兼容目录。
- `definitions/items/custom_set/`：当前 AionEmu 新物品初始强化套装的兼容目录。
- `definitions/items/skill_enhance/`：当前 AionEmu 按职业选择的新物品技能强化池兼容目录。
- `definitions/skills/`：当前 AionEmu 技能运行模板的兼容目录；启动和热重载均从这里加载。
- `definitions/npcs/`：当前 AionEmu NPC 运行模板的兼容目录；主 JAXB 缓存不再合并 NPC 模板。
- `definitions/npcs/factions/`：当前 AionEmu NPC 阵营、等级与加入 NPC 绑定的兼容目录。
- `definitions/npcs/assembled/`：当前 AionEmu 深渊战舰航线与组装部件的兼容目录。
- `definitions/quests/`：当前 AionEmu 任务主数据与服务器任务脚本的兼容目录。
- `definitions/recipes/`：当前 AionEmu 制作配方的兼容目录。
- `definitions/commerce/npc_shops/`：当前 AionEmu 商品页、NPC 商店、回购与收购映射的兼容目录。
- `definitions/portals/`：当前 AionEmu 固定传送坐标、NPC 传送路径和卷轴传送配置的兼容目录。
- `definitions/instances/`：当前 AionEmu 副本冷却、增益属性、出口和自动组队配置的兼容目录。
- `definitions/locations/`：当前 AionEmu 动态裂隙、副本裂隙和普通裂隙地点索引的兼容目录。
- `definitions/player/storage/`：当前 AionEmu 背包与角色仓库扩展 NPC、等级和价格的兼容目录。
- `definitions/player/guides/`：当前 AionEmu 等级引导、HTML 问卷和奖励配置的兼容目录。
- `definitions/world/`：当前 AionEmu 地图、复活坐标和常规传送网络等服务器世界配置的兼容目录。
- `definitions/world/movement/`：当前 AionEmu 飞行环与跨地图道路定义的兼容目录。
- `definitions/world/resources/`：当前 AionEmu 宝箱钥匙与采集物材料定义的兼容目录。
- `cache/`：派生的 JAXB 合并缓存，不是权威数据源。

## 迁移顺序

1. 将转换器完整输出导入 `definitions` 并核对清单。
2. 按领域保持现有玩法并逐步替换：世界与风道、掉落、物品、技能、NPC 与 NPC 技能、任务与配方、传送门与副本，最后处理其余数据容器。
3. 已将当前 `npc_drops` 原样迁移到 `definitions/npc_drops`，启动加载、热重载、掉落导出和测试均使用新路径。
4. 新 `compact/items.xml` 暂不能无损生成现有运行模型，已先把现有物品模板迁入 `definitions/items/item`，保持装备、动作和属性语义。
5. 新 `compact/skills.xml` 暂不能无损生成现有运行模型，已先把现有技能模板迁入 `definitions/skills`，保持效果、动作、条件和 motion 语义。
6. NPC 技能已直接使用 `compact/npc-skills.xml`，不再保留旧 `npc_skills.xml`。
7. 新 `compact/npcs.xml` 暂不能无损生成现有运行模型，已先把现有 NPC 模板迁入 `definitions/npcs`。
8. 新 `compact/quests.xml` 暂不能替代服务器任务模型，已先迁移任务主数据与脚本；挑战任务仍作为独立领域保留。
9. 新 `compact/recipes.xml` 暂不能直接形成运行配方，已先迁移现有配方模板。
10. `compact/world.xml` 的 `direct_portal.xml` 是动态直通门模型，不能替代固定传送配置；已先迁移现有传送门数据。
11. 新副本数据与当前冷却次数、增益数值不完全相同，且不含服务器出口坐标；已先迁移现有副本基础配置。
12. 新包没有现有三类裂隙服务使用的地点 ID 索引；已先迁移现有裂隙配置。
13. 新匹配器数据不能无损形成 NPC 入口和服务器提示字段；已先迁移现有自动组队配置。
14. 新副本创建表只有复活点别名，没有当前服务使用的坐标；已先迁移世界与副本复活起点。
15. 新世界 ID 表缺少地图几何和运行权限字段；已先迁移现有世界地图模板。
16. 新机场/航线数据与现有路径存在地图、坐标和服务器扩展差异；已先迁移常规传送网络。
17. 新包没有飞行环三点坐标、半径和道路出口定义；已先迁移现有世界移动配置。
18. 新包没有背包与角色仓库扩展价格表；已先迁移现有玩家存储扩展配置。
19. 新包没有宝箱钥匙或完整采集物产出表；已先迁移现有世界资源配置。
20. 新商店数据存在缺页、内容差异和无法解析的商品引用；已先迁移现有 NPC 商店配置。
21. 新套装表存在缺失套装、部件引用和属性映射歧义；已先迁移现有物品套装配置。
22. definitions 物品模板已直接表达唯一的交易限制清理规则；已删除冗余清理表和启动后处理。
23. 新随机属性表会改变概率、属性数量和值，且存在非唯一符号转换；已先迁移现有随机属性配置。
24. 新包没有服务器奖励池与宠物食物分组；已先迁移现有物品组配置。
25. 新组合道具表存在无法解析的物品名称和运行数量差异；已先迁移现有组合配方。
26. 新多目的地回城表会丢失服务器兼容地图；已先迁移现有目的地列表。
27. 新拆解数据需要合并缺失引用、自定义覆盖与禁用规则；已先迁移现有拆解表。
28. 新强化与授权表不能直接保持服务器模板 ID 和属性函数语义；已先迁移现有强化表。
29. 新物品升级表会改变大量产物、材料和费用；已先迁移现有净化升级表。
30. 新自定义套装表会改变当前物品引用的初始强化值；已先迁移现有套装。
31. 新物品技能强化表包含当前模型未表达的概率并新增未验证 ID；已先迁移现有职业技能池。
32. 新 NPC 阵营表缺少服务器加入 NPC 绑定且等级规则不同；已先迁移现有阵营表。
33. 新包没有深渊战舰的组装部件和封包实体映射；已先迁移现有组装 NPC 表。
34. 新包没有服务器等级引导问卷与奖励表；已先迁移现有 guide 配置。
35. 每个领域的新定义加载器和聚焦测试通过后，才删除对应的旧 `static_data` 导入。
36. 所有 `DataManager` 数据容器都有已验证的新来源后，再移除最后的 JAXB 合并缓存假设。

## 验证记录

已记录且对应代码未变化的验证，提交前不重复执行。

| 提交 | 范围 | 验证 | 结果 |
|---|---|---|---|
| `3f4636a8` | definitions 数据导入 | 转换器完整单测、全量生成、往返/XSD 报告、100 MB 文件审计 | 2026-07-12 通过；所有文件均小于 100 MB |
| `279beedb` | world.xml 风道 | `mvn -q -Dtest=AionServicePathsTest,WindstreamDefinitionLoaderTest,GameServerTest,XmlDataLoaderTest#staticDataSectionCountUsesTopLevelXmlElements test` | 2026-07-12 通过；未启动项目 |
| `bc40031a` | NPC 掉落 | `NpcDropDataTest`、`NpcTemplateDropLoadingTest`、`GameServerTest` 及 `XmlDataLoaderTest` 掉落相关方法 | 2026-07-12 通过；全部现有分片可急切加载，未启动项目 |
| `cc01713a` | 物品模板 | `XmlDataLoaderTest` 物品相关方法、`DataManagerTest`、`AwakeningScrollSkillDataTest`、`DyeActionTest`、`GameServerTest`、`AionServicePathsTest` | 2026-07-12 通过；128629 个模板可加载，未启动项目 |
| `ec392399` | 技能模板 | `XmlDataLoaderTest` 技能相关方法、`AwakeningScrollSkillDataTest`、`MinionTransformSkillDataTest`、`HotReloadDataTest`、`SkillTemplateTest` | 2026-07-12 通过；14480 个模板及冷却组可加载，未启动项目 |
| `67c4d33a` | NPC 技能 | `NpcSkillDefinitionLoaderTest`、`PriestAI2Test`、`XmlDataLoaderTest` 静态 XSD/分区方法、`GameServerTest` | 2026-07-12 通过；59058 个 NPC 分配可加载，未启动项目 |
| `a68baad0` | NPC 模板 | `XmlDataLoaderTest` NPC 相关方法、`NochsanaFortressGateTemplateTest`、`NpcTemplateDropLoadingTest`、`GameServerTest` | 2026-07-12 通过；87961 个唯一模板可加载，未启动项目 |
| `34594cd9` | 任务主数据与脚本 | `XmlDataLoaderTest` 任务/XSD/分区相关方法、`GameServerTest` | 2026-07-12 通过；6424 个任务和 3813 个脚本处理器可加载，未启动项目 |
| `c130304f` | 制作配方 | `XmlDataLoaderTest` 配方/XSD/分区相关方法、`GameServerTest` | 2026-07-12 通过；14540 个配方可加载，未启动项目 |
| `244a2c34` | 传送门 | `XmlDataLoaderTest` 传送门加载/主 XSD 方法、`GameServerTest`、两份传送门 XML 的 XSD 校验 | 2026-07-12 通过；548 个固定坐标和 826 个传送配置可加载，未启动项目 |
| `0bcbcd95` | 副本基础配置 | `XmlDataLoaderTest` 副本加载/主 XSD 方法、`DataholderLookupIndexTest` 出口覆盖方法、`GameServerTest`、三份 XML 的 XSD 校验 | 2026-07-12 通过；110 个冷却、18 个增益和 242 个出口可加载，未启动项目 |
| `dae2de3a` | 裂隙地点 | `XmlDataLoaderTest` 裂隙加载/主 XSD/分区统计方法、两类裂隙模型测试、`GameServerTest`、三份 XML 的 XSD 校验 | 2026-07-12 通过；6 个动态、9 个副本和 80 个普通裂隙地点可加载，未启动项目 |
| `afa2149d` | 自动组队 | `XmlDataLoaderTest` 自动组队加载/主 XSD/分区统计方法、`AutoGroupServiceTest`、`GameServerTest`、自动组队 XML 的 XSD 校验 | 2026-07-12 通过；130 个匹配掩码及 NPC 入口映射可加载，未启动项目 |
| `4a27b2e1` | 复活起点 | `XmlDataLoaderTest` 复活点加载/主 XSD/分区统计方法、`GameServerTest`、两份 XML 的 XSD 校验 | 2026-07-12 通过；26 条世界起点和 92 条副本起点可加载，未启动项目 |
| `4e2986da` | 世界地图模板 | `XmlDataLoaderTest` 地图加载/主 XSD/分区统计方法、`GameServerTest`、地图 XML 的 XSD 校验 | 2026-07-12 通过；185 个地图模板及关键几何/能力字段可加载，未启动项目 |
| `f7cdc7c9` | 常规传送网络 | `XmlDataLoaderTest` 传送加载方法、主 XSD/分区统计方法、`GameServerTest`、四份 XML 的 XSD 校验 | 2026-07-12 通过；139 个绑定点、363 个传送模板、357 个地点和 316 条飞行路径可加载，未启动项目 |
| `ae8a4360` | 飞行环与道路 | `XmlDataLoaderTest` 世界移动加载/主 XSD/分区统计方法、`GameServerTest`、两份 XML 的 XSD 校验 | 2026-07-12 通过；72 个飞行环和 8 条道路可加载，未启动项目 |
| `005d2e4a` | 背包与仓库扩展 | `XmlDataLoaderTest` 存储扩展加载/主 XSD/分区统计方法、`GameServerTest`、两份 XML 的 XSD 校验 | 2026-07-12 通过；11 个背包扩展 NPC、268 行仓库配置形成 267 个唯一 NPC 模板，未启动项目 |
| `41d0342c` | 宝箱与采集物 | `XmlDataLoaderTest` 世界资源加载/主 XSD/分区统计方法、`GameServerTest`、两份 XML 的 XSD 校验 | 2026-07-12 通过；359 行宝箱配置形成 358 个唯一模板，761 个采集物模板可加载，未启动项目 |
| `d0edaaad` | NPC 商店 | `XmlDataLoaderTest` 商店加载/主 XSD/分区统计方法、`LimitedTradeNpcTest`、`GameServerTest`、两份 XML 的 XSD 校验 | 2026-07-12 通过；3898 个商品页、2461 个普通商店、189 个回购和 256 个收购 NPC 映射可加载，未启动项目 |
| `f7a8ea48` | 物品套装 | `XmlDataLoaderTest` 套装加载/主 XSD/分区统计方法、`GameServerTest`、套装 XML 的 XSD 校验 | 2026-07-12 通过；672 个套装及部件反向索引可加载，未启动项目 |
| `2aa12ea3` | 物品限制清理 | `XmlDataLoaderTest` 物品加载/主 XSD/分区统计方法、`DataManagerTest` | 2026-07-12 通过；物品 `100000001` 无后处理仍可交易，冗余容器与规则已删除，未启动项目 |
| `2ab52d85` | 物品随机属性 | `XmlDataLoaderTest` 随机属性加载/主 XSD/分区统计方法、`GameServerTest`、随机属性 XML 的 XSD 校验 | 2026-07-12 通过；659 个唯一背包/抛光随机组及现有概率可加载，未启动项目 |
| `c21d45ba` | 物品组 | `XmlDataLoaderTest` 物品组加载/主 XSD/分区统计方法、`GameServerTest`、物品组 XML 的 XSD 校验 | 2026-07-12 通过；4994 条运行奖励、794 条宠物食物缓存及类型查询可加载，未启动项目 |
| `1a22d17a` | 组合道具 | `XmlDataLoaderTest` 组合道具加载/主 XSD、`GameServerTest`、组合道具 XML 的 XSD 校验 | 2026-07-12 通过；173 条运行配方、关键部件列表及数量可加载，未启动项目 |
| `f4a5842c` | 多目的地回城 | `XmlDataLoaderTest` 多目的地加载/主 XSD、`GameServerTest`、多目的地 XML 的 XSD 校验 | 2026-07-12 通过；4 组目的地及 Inggison/Gelkmaros 兼容世界 ID 可加载，未启动项目 |
| `70b1a3f1` | 物品拆解 | `XmlDataLoaderTest` 拆解加载/主 XSD、`GameServerTest`、拆解 XML 的 XSD 校验 | 2026-07-12 通过；8861 个分组及代表性等级过滤、奖励 ID 可加载，未启动项目 |
| `80faac2a` | 物品强化与授权 | `XmlDataLoaderTest` 强化加载/主 XSD、`GameServerTest`、强化 XML 的 XSD 校验 | 2026-07-12 通过；189 张模板及 ENCHANT/AUTHORIZE 类型索引可加载，未启动项目 |
| `25902510` | 物品净化升级 | `XmlDataLoaderTest` 升级加载/主 XSD、`GameServerTest`、升级 XML 的 XSD 校验 | 2026-07-12 通过；3897 个基础物品、代表性结果、材料与 AP 成本可加载，未启动项目 |
| `3af5e65c` | 物品初始强化套装 | `XmlDataLoaderTest` 套装加载/主 XSD、`GameServerTest`、套装 XML 的 XSD 校验 | 2026-07-12 通过；90 组及关键引用的现有强化值可加载，未启动项目 |
| `db687168` | 物品技能强化池 | `XmlDataLoaderTest` 技能池加载/主 XSD、`ItemServiceSkillEnhanceTest`、`GameServerTest`、技能池 XML 的 XSD 校验 | 2026-07-12 通过；389 个 ID、职业专属与通用回退技能池可加载，未启动项目 |
| `57abf839` | NPC 阵营 | `XmlDataLoaderTest` 阵营加载/主 XSD、`GameServerTest`、阵营 XML 的 XSD 校验 | 2026-07-12 通过；22 行阵营、NPC 反向索引及重复 ID 后项覆盖可加载，未启动项目 |
| `bb43d030` | 组装 NPC | `XmlDataLoaderTest` 组装模板加载/主 XSD、`AssembledNpcTest`、`GameServerTest`、组装 XML 的 XSD 校验 | 2026-07-12 通过；2 条航线及 65 个部件可加载，未启动项目 |
| 待提交 | 等级引导问卷 | `XmlDataLoaderTest` 引导加载/主 XSD、`GameServerTest`、引导 XML 的 XSD 校验 | 2026-07-12 通过；代表性 HTML、奖励选择数和奖励列表可加载，未启动项目 |

## 待实现或无法可靠映射

- 紧凑数据包使用字段字典和面向源文件的结构；不能匹配现有 JAXB 模型的领域，需要最小的显式加载器或替代数据容器。
- `world.xml` 风道已经实现：141 个风道组引用 111 条 WIF 轨迹；客户端值 `405001` 可正确归一为风道组 `405`；进入和移动位置按真端的 45/50 单位阈值校验。旧地图 ID `600040000`、`600050000` 继续复用 `Tiamat_Down`、`LDF5a` 客户端地图，此映射已由 `aion-geo` 验证。
- `fly_path.xml` 不包含初始启用状态；动态风道 301、302 继续保持初始关闭，由现有 NPC AI 开启。
- NPC 掉落继续使用现有 26 个正式分片、公共掉落组和旧表备份；`static_data.xml` 不再合并掉落，启动与热重载统一通过 `XmlDataLoader.loadNpcDropData()` 从 `definitions/npc_drops` 加载。
- `compact/items.xml` 有 128380 条客户端源记录和 307 个字段；现有运行数据有 128629 个唯一模板，并额外依赖派生掩码、分类、装备槽、说明 ID、嵌套武器属性、修饰器和道具动作。当前参考代码不足以证明这些派生关系全部可逆，因此暂不直接替换；后续只有在转换器能生成完整 `ItemTemplate` 并通过行为覆盖审计后，才删除兼容模板。
- `compact/skills.xml` 有 14494 条客户端源记录，现有运行数据有 14480 个模板，并额外包含嵌套属性、起始/使用/装备条件、效果、动作、周期动作和 motion。当前参考代码不足以证明所有字段到运行类的映射，因此暂不直接替换；启动与热重载统一通过 `XmlDataLoader.loadSkillData()` 从兼容目录加载。
- `compact/npc-skills.xml` 已直接替换旧 NPC 技能表：10743 个共享组展开为 59058 个 NPC 分配。663 个唯一技能节点只有真端名称而没有可解析 ID；加载器保留对应 NPC 分配但跳过不可执行技能，不猜测 ID。
- `compact/npcs.xml` 有 87734 条客户端源记录，现有兼容 XML 有 87970 行、87961 个唯一模板，并额外依赖派生名称 ID、AI、装备、统计、边界、交互和运行枚举。当前参考代码不足以证明完整映射，启动统一通过 `XmlDataLoader.loadNpcData()` 从兼容目录加载。
- `compact/quests.xml` 是客户端 `quest.xml` 与奖励、采集、狩猎、对话、物品使用等关联文件的源文档包，不能表达服务器 `QuestTemplate` 派生字段和 3813 个 XML 脚本处理器。启动与热重载统一通过 `XmlDataLoader.loadQuestData()`、`loadQuestScripts()` 从兼容目录加载。
- `compact/recipes.xml` 保存客户端配方名称引用，现有运行模板已解析为技能 ID、物品 ID、名称 ID、组件和组合产物。当前转换器未提供这些引用的完整运行映射，因此启动统一通过 `XmlDataLoader.loadRecipeData()` 从兼容目录加载。
- `compact/world.xml` 中的 `direct_portal.xml` 是 218 条动态跨地图直通门配置，真端按地图生成组、时间表、开放时长和使用次数创建 NPC；现有 `PortalLocData`、`Portal2Data` 则索引 548 个固定坐标以及 826 个 NPC 对话、卷轴和副本路径配置。两者运行语义不同，不能互相替代；启动与 `//reload portal` 统一通过 `XmlDataLoader` 从 `definitions/portals` 加载兼容数据。
- `compact/instances.xml` 的冷却表必须结合世界 ID 表和 `instance_cooltime2.xml` 才能形成运行模板，但其行为值与当前配置不同，例如 Indratu Fortress 的 `maxcount` 为 2、当前运行值为 5。增益表也存在有意调整，buff 7/8 的客户端 `Pvpdefendratio +9999`、`arAll +9999` 在当前运行数据中为 `+900`、`+1000`；新包同时没有服务器按阵营设置的副本出口坐标。为避免玩法回归，启动统一通过 `XmlDataLoader` 从 `definitions/instances` 加载 110 个冷却配置、18 个增益模板和 242 个出口，待这些差异有明确迁移规则后再直接解释 compact 数据。
- 三类现有裂隙配置只保存服务使用的地点 ID 与普通裂隙所属世界；具体刷新点由刷怪数据和服务代码解释。`compact/world.xml` 的 `direct_portal.xml` 是另一套真端动态直通门定义，`compact/instances.xml` 也没有这 6 个动态裂隙、9 个副本裂隙和 80 个普通裂隙地点索引，不能可靠替代；启动统一通过 `XmlDataLoader` 从 `definitions/locations` 加载兼容数据。
- `compact/instances.xml` 的 `matchmaker.xml` 有等级、队伍规模、职业配额、登记方式和开放时段，但没有当前自动组队入口依赖的 NPC ID；实例字段仍是客户端名称，`name_id`、`title_id` 也不能由单条记录直接得到。当前 130 个服务器匹配掩码还被多个服务和封包直接引用，因此启动统一通过 `XmlDataLoader.loadAutoGroupData()` 从 `definitions/instances/auto_group` 加载兼容配置，待 NPC/文本/世界映射均可证明后再直接解释新匹配器数据。
- `compact/instances.xml` 的 `instance_creation.xml` 只给出 `start_point_alias_*`、`resurrect_point_alias_*` 等客户端别名，没有三维坐标；`compact/world.xml` 也不包含当前按世界、阵营和等级选择的复活表。启动统一通过 `XmlDataLoader` 从 `definitions/world/revive_start_points` 加载 26 条世界起点和 92 条副本起点，待客户端 Level 别名可可靠解析到坐标后再直接替换。
- `compact/id-mappings.xml` 的 `id/worldid.xml` 有 233 个客户端世界 ID 和分流、限制、PVE 比率等字段，但没有当前地图模板依赖的 `world_size`、水面/死亡高度、区域能力 flags、实例标记、世界类型、AI 追击范围和可读名称。上述字段直接服务于 Geo/Nav、区域、飞行、召回、PVP 和实例创建，不能猜测；启动统一通过 `XmlDataLoader.loadWorldMapsData()` 从 `definitions/world/maps` 加载现有 185 个模板。
- `compact/world.xml` 的 `airline.xml`、`airports.xml`、`fly_path.xml` 能表达大量客户端机场和航线，但机场坐标仍是 Level 别名，缺少绑定价格与 NPC 映射；现有 316 条飞行路径中有 97 条与同 ID 新路径存在坐标或地图差异，419–423 五条服务器路径在新数据中不存在，且主服继续把旧 Inggison/Gelkmaros 路径映射到 `210130000/220140000`。为保持传送落点和航线，启动统一通过 `XmlDataLoader` 从 `definitions/world/transport` 加载 139 个唯一绑定点、363 个唯一传送模板、357 个地点和 316 条飞行路径。
- 新源目录没有飞行环或跨地图道路定义；`AnimationMarkers` 中名称带 `fly`、`road` 的文件只是客户端动画标记，不能表达现有飞行环的中心/左右三点、半径，也不能表达道路出口地图与坐标。启动统一通过 `XmlDataLoader` 从 `definitions/world/movement` 加载原样迁移的 72 个飞行环和 8 条道路。
- 新源目录没有背包或角色仓库扩展的 NPC、等级和价格表；名称带 `cube` 的文件是动画/预设，`toypet_warehouse.xml` 是玩具宠物仓库，都不能驱动现有扩展服务。启动统一通过 `XmlDataLoader` 从 `definitions/player/storage` 加载 11 个背包扩展 NPC，以及 268 行仓库配置按 NPC ID 形成的 267 个唯一模板；重复 NPC `263516` 保持现有后项覆盖语义。
- 新源目录中名称带 `chest`、`gather` 的数据仅有动画标记、任务采集条件和采集配方经验，缺少宝箱钥匙、采集物模板 ID、技能等级、采集次数以及普通/额外材料概率，不能驱动现有交互与奖励逻辑。启动统一通过 `XmlDataLoader` 从 `definitions/world/resources` 加载 359 行宝箱配置形成的 358 个唯一模板，以及 761 个采集物模板；重复宝箱 NPC `700477` 保持现有后项覆盖语义。
- 新商店数据分散在 `compact/commerce-events.xml`、`item-relations.xml`、`npcs.xml` 和 `items.xml`：普通商品页与现有同 ID 页面仅 3019/3245 个顺序完全一致，226 个页面内容变化、11 个旧页面缺失，并有 314 个商品名称无法解析到新 Item 表；回购与收购又分别依赖 `trade_in_list.xml`、`purchase_list.xml` 和 NPC 多类交易字段。为避免商店缺货或客户端交易校验回归，启动统一通过 `XmlDataLoader` 从 `definitions/commerce/npc_shops` 加载现有 3898 个商品页、2461 个普通商店、189 个回购和 256 个收购 NPC 映射，待引用覆盖与行为差异全部可证明后再直接解释 compact 数据。
- `compact/item-relations.xml` 的 `setitem.xml` 有 721 个套装，现有运行表有 672 个，两者仅 640 个 ID 相交；新表缺少 32 个现有套装，并新增 81 个套装，其中 42 个部件名称无法解析到新 Item 表。共同套装仍有部件差异，奖励字符串还需要 `attackdelay`、`boosthate` 的符号转换，且 `silence_arp`、`paralyze_arp` 在现有数据中分别出现抗性与抗性穿透两种解释，不能按名称唯一映射。启动统一通过 `XmlDataLoader.loadItemSetData()` 从 `definitions/items/sets` 加载现有 672 个套装，待所有部件和属性映射可证明后再直接解释新表。
- `compact/item-relations.xml` 的 `item_random_option.xml` 与 `polish_bonus_setlist.xml` 共提供 659 个随机属性键，数量与现有唯一键一致，但按现有属性转换规则只有 482 组能完全还原；113 个概率值不同，部分抛光组的属性数量和值也不同，`attackdelay` 与 `parry` 还出现非唯一符号转换。为保持物品洗练、随机装备属性和伊甸石结果，启动统一通过 `XmlDataLoader.loadItemRandomBonusData()` 从 `definitions/items/random_bonuses` 加载现有 659 个唯一随机组，待概率变更和全部属性转换有明确规则后再直接解释新表。
- 新 definitions 清单没有 `item_group` 等价文档；现有表是服务器为制作材料、魔石、勋章、食物、药品、矿石、采集、强化、首领奖励和 21 类宠物食物维护的运行分组，无法从单个 Item 字段恢复。启动统一通过 `XmlDataLoader.loadItemGroupsData()` 从 `definitions/items/groups` 加载 4994 条运行奖励和 794 条宠物食物缓存。
- `compact/item-relations.xml` 的 `assembly_items.xml` 有 202 条组合道具，按新 Item 名称只能唯一解析 175 条产物，24 条配方仍含无法解析的部件。与现有 173 条运行配方相交的 171 条中，5 条会改变部件数量或暴击产物，另有 1 条各部件数量不同，当前运行模型无法无损表达。为保持现有组合动作，启动统一通过 `XmlDataLoader.loadAssemblyItemsData()` 从 `definitions/items/assembly` 加载现有 173 条配方，待物品引用和逐部件数量模型完整后再直接解释新表。
- `compact/item-relations.xml` 的 `item_multi_return.xml` 有 9 组真端目的地，但当前物品动作只引用 6/7 两组；新表各含 10 个目的地，现有运行表各含 12 个。真端的 Inggison/Gelkmaros 世界 ID `210050000/220070000` 在当前服务器与 Geo 数据中兼容为 `210130000/220140000`，现有表还保留 Cygnea、Iluma、Enshar、Norsvold，而当前传送服务不识别新表的两个原始世界 ID。启动统一通过 `XmlDataLoader.loadMultiReturnItemData()` 从 `definitions/items/multi_return` 加载现有 4 组目的地，待世界别名、PortalLoc 坐标和客户端选项一并迁移后再直接解释新表。
- `compact/item-relations.xml` 的拆解数据来自 `disassembly_item.xml`、`disassembly_item_customize.xml` 和 `disassembly_item_setlist.xml` 三张关联表。基础表 8889 条中 8879 条能映射到新 Item，但比现有运行表多 18 个 ID；15 个集合名称引用缺失，3 个奖励名称无法解析，还必须合并 1206 条自定义覆盖。现有表另保留 672 个禁用奖励标记、范围数量以及等级/职业/种族条件。为避免拆解概率或奖励变化，启动统一通过 `XmlDataLoader.loadDisassemblyItemSetsData()` 从 `definitions/items/disassembly` 加载现有 8861 个分组，待三表引用和修复规则能完整往返后再直接解释新表。
- `compact/item-relations.xml` 的 `item_enchanttable.xml` 有 131 张原始强化表，`item_authorizetable.xml` 有 264 张授权表；现有运行数据使用 `10000–10222` 的服务器模板 ID，只保留 40 张 `ENCHANT` 和 149 张 `AUTHORIZE`，并把原始属性符号转换为 `StatFunction`。新表还包含无限强化和当前模型未表达的随机字段，不能按 ID 或字段直接替换。启动统一通过 `XmlDataLoader.loadItemEnchantData()` 从 `definitions/items/enchant` 加载现有 189 张表，待物品 `tempering_table_id`、类型划分和属性转换全部可证明后再解释新表。
- `compact/item-relations.xml` 的 `item_upgrade.xml` 有 3898 个基础物品和 4493 个可解析结果；现有运行表保留 3897 个基础物品、4066 个结果。共同基础项只有 3017 个在当前模型支持的产物、检查强化/授权等级、材料、金币和 AP 字段上完全一致，880 个存在结果数量或成本差异；新表的结果强化变化、NPC 限制等字段也未被当前模型表达。为保持净化升级行为，启动统一通过 `XmlDataLoader.loadItemUpgradeData()` 从 `definitions/items/upgrade` 加载现有数据，待全部差异和附加字段有运行规则后再直接解释新表。
- `compact/item-relations.xml` 的 `itemcustomset.xml` 有 104 组，现有 90 组全部存在，但只有 80 组名称和初始强化值完全一致；10 组数值变化，其中 7 组仍被当前物品模板引用。新表还提供 `custom_option_slot_1..6`，当前运行模型未解释这些选项。为避免新建物品强化等级变化，启动统一通过 `XmlDataLoader.loadItemCustomSetData()` 从 `definitions/items/custom_set` 加载现有 90 组，待自定义选项和变化规则实现后再直接解释新表。
- `compact/item-relations.xml` 的 `item_skill_enhance.xml` 有 457 个 ID 和 1486 条技能组引用；现有运行表只实现其中 389 个 ID，并按职业展开为 556 条数值技能规则。新表另有每级强化概率和每个技能的权重，当前物品服务使用等概率技能 ID 列表，无法直接表达这些概率；68 个新增 ID 也未被当前兼容物品模板验证。启动统一通过 `XmlDataLoader.loadItemSkillEnhanceData()` 从 `definitions/items/skill_enhance` 加载现有规则，待技能组到数值 ID、权重和强化等级算法完整实现后再解释新表。
- `compact/npc-relations.xml` 的 `npcfactions.xml` 有 18 条真端阵营，但不包含现有加入/退出流程所需的 NPC ID 或数值名称 ID；当前运行表用 22 行绑定 22 个 NPC，并形成 14 个有效阵营。字段行为也不同，例如真端 Army_Li/Army_Da 最低等级为 45、当前为 40，导师阵营在新表中出现 `minlevel=999/maxlevel=1`，不能直接用于当前等级校验。启动统一通过 `XmlDataLoader.loadNpcFactionsData()` 从 `definitions/npcs/factions` 加载现有阵营，待 NPC 名称引用、文本 ID 和等级语义全部映射后再直接解释新表。
- 新 definitions 清单没有组装 NPC 等价文档；`fly_path.xml`、`airports.xml` 和 `AnimationMarkers/*carrier*` 只能提供航线或客户端动画标记，不能形成 `SM_NPC_ASSEMBLER` 所需的 route、存活时间以及 65 个 `npcId/entityId` 部件映射。启动统一通过 `XmlDataLoader.loadAssembledNpcsData()` 从 `definitions/npcs/assembled` 加载现有 2 条深渊战舰模板。
- 新 definitions 清单没有服务器等级引导问卷等价文档；名称带 `guide` 的输入仅是动画标记和角色外观预设，不能表达当前 50 个按等级、种族、职业选择的 HTML 文案、问卷按钮及 470 个奖励项。启动统一通过 `XmlDataLoader.loadGuideData()` 从 `definitions/player/guides` 加载现有配置。
- 字段行为若无法从 58Server 真端、`aion-server`、转换器或 5.8 客户端证明，必须先记录在此处，才能移除该领域的兼容数据源。

## 大型生成文件

转换器以 95 MiB 为上限按完整 XML 成员自动分片。当前普通动画包和地区动画对象包各生成 3 个分片，最大文件分别为 99488612、99317630 字节；全输出没有超过 100 MB 的文件。

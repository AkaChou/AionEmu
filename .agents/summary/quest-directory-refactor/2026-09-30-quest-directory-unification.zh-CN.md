# 任务资源目录统一收敛与路径重定向报告

## 一、 背景与根因

在此次整改前，`src/main/resources/aion/data/static_data/` 下的任务相关资源割裂在 3 个同级目录和 1 个根文件中：
1. `quest_data/`（旧 AionEmu 任务属性表，`quest_data.xml` 3.2MB）；
2. `quest_definition/`（XML 状态机定义，`quest_definition_catalog.xml` 与 808 个 XML）；
3. `quest_retail/`（官方 5.8 客户端真端解包模板表与元数据表 `quest.xml` 11.2MB）；
4. `quest_random_rewards.xml`（散落在 `static_data` 根目录）。

存在严重的问题：
- **目录层级割裂**：任务模块占据了 static_data 下多个顶层命名空间；
- **元数据重复冗余**：`quest_data.xml` 与 `quest_retail/quest.xml` 的任务元数据（ID、等级、职业、奖励、掉落、任务道具）近乎 100% 重叠；
- **清单双重维护**：`quest_definition_catalog.xml` 与 `retail-xml-retention.tsv` 同时记录任务接管状态。

## 二、 统一收拢后的目录架构

全量任务静态资源统一归拢入 `src/main/resources/aion/data/static_data/quest/`：

```text
src/main/resources/aion/data/static_data/quest/
├── retail/                  # 官方真端驱动核心数据（最终主干）
│   ├── quest.xml            # 真端全量任务元数据表（10,035 任务）
│   ├── Quest_SimpleHunt.xml
│   ├── Quest_SimpleTalk.xml
│   ├── Quest_SimpleUseItem.xml
│   ├── Quest_SimpleCollectItem.xml
│   ├── Quest_SimpleItemPlay.xml
│   ├── Quest_SimpleSerialHunt.xml
│   ├── Quest_CombineTask.xml
│   ├── data_driven_quest.xml
│   ├── retail-xml-retention.tsv
│   ├── quest_name_string_ids.tsv
│   ├── quest_legacy_heal_rows.tsv
│   └── quest-retail-tsv-manifest.tsv
├── definitions/             # XML 状态机定义（逐步退役收敛）
│   ├── quest_definition_catalog.xml
│   ├── quest_definition_catalog.xsd
│   ├── quest_definition.xsd
│   └── quests/              # 808 个保留 XML
└── legacy/                  # 传统 AionEmu 兼容过渡层
    ├── quest_data.xml       # 传统任务属性表（待后续由 quest.xml 完全替代）
    ├── quest_data.xsd
    ├── challenge_tasks.xml  # 挑战任务
    ├── challenge_tasks.xsd
    ├── quest_random_rewards.xml # 随机奖励表
    └── quest_random_rewards.xsd
```

## 三、 代码与配置重定向

1. **`static_data.xml` 与 `static_data.xsd`**：
   - 引用路径平滑切换为 `quest/legacy/quest_data.xml`、`quest/legacy/challenge_tasks.xml`、`quest/legacy/quest_random_rewards.xml`；
   - XSD schemaLocation 引用相应更新。
2. **`quest_data.xsd` 与 `challenge_tasks.xsd`**：
   - 由于目录深入一层，相对引用更新为 `../../global_types.xsd` 等。
3. **`quest_definition_catalog.xml`**：
   - 全量 808 个 XML 资源的 `resource` 属性更新为 `aion/data/static_data/quest/definitions/quests/<id>.xml`。
4. **服务端核心代码常量**：
   - `QuestEngine.java`、`QuestDefinitionCatalogManifest.java`、`QuestDefinitionDirectoryLoader.java`、`QuestDefinitionXmlCompiler.java`、`RetailQuestDriver.java`、`RetailQuestWorkItems.java`、`Reload.java` 及全部 `RetailClient*.java` 类的加载路径同步对齐。
5. **测试用例路径常量**：
   - 全量 218 个测试类中的路径常量全部对齐新结构。

## 四、 验证结果

1. **IDEA 全工程 Rebuild**：`isSuccess: true`，零编译错误；
2. **脱机 ClassLoader 全量生产目录验证**：6,224/6,224 全量任务正常加载，0 missing，0 wrongOwner；
3. **XML/XSD 静态校验**：
   - `static_data.xsd` 自身编译干净；
   - `static_data.xml` validates against `static_data.xsd` PASS；
   - `quest_data.xml` validates against `quest_data.xsd` PASS；
   - `challenge_tasks.xml` validates against `challenge_tasks.xsd` PASS；
4. **核心门禁测试验证**：
   - `ProductionCatalogWhitelistVerificationTest` PASS；
   - `RetailQuestDriverOverlayTest`（5 项测试）100% PASS；
   - `RetailTsvManifestGateTest`（3 项测试）100% PASS；
   - `QuestDefinitionCatalogManifestTest`（11 项测试）100% PASS；
   - 10 个家族门禁（37 项测试）100% PASS；
   - 总计 62+ 项关键门禁测试 100% 全绿通过。

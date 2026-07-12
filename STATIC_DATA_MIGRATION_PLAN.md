# 静态数据迁移计划

## 目标

`src/main/resources/aion/definitions` 是唯一的新静态定义根目录，内容为完整的 `converted_staticdata_v2` 输出。先保持现有 AionEmu 功能，再逐领域使用这些定义替换 JAXB 旧数据。掉落继续使用当前已验证的数据，仅迁移目录，不重新生成。

## 目录

- `definitions/compact/`：领域加载器使用的正式 XML 数据包。
- `definitions/schemas/`：转换器输出的 XSD。
- `definitions/manifest.json`、`catalog.json`、校验与修复报告：记录数据来源和转换结果。
- `definitions/gzip/`：与 XML 等价的压缩副本；超大领域可直接使用。
- `definitions/npc_drops/`：当前 AionEmu 掉落数据的迁移目标。
- `cache/`：派生的 JAXB 合并缓存，不是权威数据源。

## 迁移顺序

1. 将转换器完整输出导入 `definitions` 并核对清单。
2. 按领域保持现有玩法并逐步替换：世界与风道、掉落、物品、技能、NPC 与 NPC 技能、任务与配方、传送门与副本，最后处理其余数据容器。
3. 将当前 `npc_drops` 原样迁移到 `definitions/npc_drops`，同步修改启动加载、热重载和测试路径。
4. 每个领域的新定义加载器和聚焦测试通过后，才删除对应的旧 `static_data` 导入。
5. 所有 `DataManager` 数据容器都有已验证的新来源后，再移除最后的 JAXB 合并缓存假设。

## 验证记录

已记录且对应代码未变化的验证，提交前不重复执行。

| 提交 | 范围 | 验证 | 结果 |
|---|---|---|---|
| `3f4636a8` | definitions 数据导入 | 转换器完整单测、全量生成、往返/XSD 报告、100 MB 文件审计 | 2026-07-12 通过；所有文件均小于 100 MB |
| 待提交 | world.xml 风道 | `mvn -q -Dtest=AionServicePathsTest,WindstreamDefinitionLoaderTest,GameServerTest,XmlDataLoaderTest#staticDataSectionCountUsesTopLevelXmlElements test` | 2026-07-12 通过；未启动项目 |

## 待实现或无法可靠映射

- 紧凑数据包使用字段字典和面向源文件的结构；不能匹配现有 JAXB 模型的领域，需要最小的显式加载器或替代数据容器。
- `world.xml` 风道已经实现：141 个风道组引用 111 条 WIF 轨迹；客户端值 `405001` 可正确归一为风道组 `405`；进入和移动位置按真端的 45/50 单位阈值校验。旧地图 ID `600040000`、`600050000` 继续复用 `Tiamat_Down`、`LDF5a` 客户端地图，此映射已由 `aion-geo` 验证。
- `fly_path.xml` 不包含初始启用状态；动态风道 301、302 继续保持初始关闭，由现有 NPC AI 开启。
- 字段行为若无法从 58Server 真端、`aion-server`、转换器或 5.8 客户端证明，必须先记录在此处，才能移除该领域的兼容数据源。

## 大型生成文件

转换器以 95 MiB 为上限按完整 XML 成员自动分片。当前普通动画包和地区动画对象包各生成 3 个分片，最大文件分别为 99488612、99317630 字节；全输出没有超过 100 MB 的文件。

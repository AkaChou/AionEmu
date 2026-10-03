# 证据表迁出生产资源树 + legacy 退役探针否决（2026-10-03，资源精简批；用户指令「删除能删的」）

## 1. 已执行：两份 BYTE_IDENTICAL 证据表迁至测试资源树（生产行为零变更）

`git mv` 至 `src/test/resources/aion/data/static_data/quest/retail/`（**同 classpath 相对路径**
`aion/data/static_data/quest/retail/…`，provenance 门 `TABLE_ROOT + repo_path` 解析与
`table-source-provenance.tsv` 全部 13 行零改动）：

| 文件 | 体积 | 迁出理由 |
|---|---|---|
| `challenge_task.xml` | 218,118 B | main 零读者（唯一消费 = provenance 门 hash 校验 + presence 门 99xxx 段对拍，均测试侧 classpath 读） |
| `quest_random_rewards.xml` | 4,110,550 B | 同上；真端原始转储（UTF-16LE BOM），运行时消费的是 `quest/legacy/quest_random_rewards.xml` 适配副本 |

效果：生产部署（JAR/package.sh 资源树）瘦 **~4.3MB** 非运行时数据；`src/main` = 运行时数据、
`src/test` = 证据数据的边界收紧。门禁 = `TableSourceProvenanceGateTest` 2/2 +
`RetailDataDrivenClientPresenceGateTest` 4/4 绿（零代码/零 TSV 改动）。

## 2. 探针否决：`legacy/` 4 文件不可直接退役（前提推翻登记）

上一轮对话「legacy/ 是唯一可删空间（切线后删）」的前提被本批探针**否决**——两份 retail 副本是
**真端原始转储**，与现有 JAXB 绑定不兼容：

| 障碍 | rewards（`QuestRandomRewardsData`） | challenge（`ChallengeData`，StaticData `@XmlElement("challenge_tasks")`） |
|---|---|---|
| 编码 | retail = UTF-16LE(BOM) + DOCTYPE 大实体表；legacy = UTF-8 | 同左 |
| 行模型 | 行元素同名（`quest_random_reward`）但 retail 多 `__comment__` 列 | **根本不同**：legacy `<task id type race min_level…>` 属性形 + `<quest repeat_count score>`；retail `<challenge_task>` 子元素形（`level_min`/`quest_list/data`）⇒ 直接切 import = 绑定读零行（空表） |
| 值域 | **retail `<item>` = 物品名字符串**（`potion_hp_mp_30a`），legacy = 数字 id ⇒ 需物品名→id 解析轴；**prob 尺度不同**（legacy 700000 形 vs retail 25000 形）⇒ 需概率语义取证 | `name_id`（数字）vs `desc`（STR_ 键）同轴问题 |

唯一有利事实：**legacy 329 行 = retail 817 行的纯子集**（0 缺 id / 0 name 漂移）⇒ 未来
「真值转换批」方向成立：离线转换工具（物品名解析 + prob 归一 + challenge 行适配）产出
绑定兼容形 → 切 `static_data.xml:84/:141` 两处 import + `RetailQuestDriver.RANDOM_REWARDS`
+ `static_data.xsd` include 链 → legacy/ 整目录退役。**登记为工程批（非用户资产）**，
前置 = 物品名解析轴（P3 `RetailItemNameIndex` 同轴）。

## 3. 随行登记的现状事实

- 运行时今天消费的 rewards = **legacy 329 行陈旧副本**（`static_data.xml:84` import +
  `RetailQuestDriver.RANDOM_REWARDS`）；生产转换约定已容忍缺组
  （`RetailQuestMetadataCompiler:651`「随机组缺失时按生产转换约定跳过」）。
- 全目录孤儿扫描零结果：`static_data/quest` 771 文件（definitions 743 目录扫描装载 /
  retail 21 运行时与门禁 / legacy 4 运行时 + schema 链）**无一零读者**。

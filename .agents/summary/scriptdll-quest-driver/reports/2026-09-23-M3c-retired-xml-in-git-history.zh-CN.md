# M3-c：退役 XML 只留 git 历史（取消测试作用域冻结副本）

- 日期：2026-09-23
- 切片：M3 收口（去 fixture 口径落地 + 退役任务测试适配补齐）
- 承上：`2026-09-23-M3b-simple-talk-semantics-and-retirement.zh-CN.md`（SimpleTalk 语义收口与首次退役）
- 一句话：按用户指令改为 **旧 XML 不再进 test**——退役 = 删除生产 XML + catalog 收敛，历史内容由 git 承担，
  仓库内只保留机器可读的保留清单（`owner=RETAIL_TABLE`）；所有读退役 XML 的测试改走**生产视图**。

## 1. 口径变更（本轮最重要的一句话）

用户指令（2026-09-23）：

> 旧 xml 已经在 git 历史中，所以没必要迁移到 test。

落地：
- 退役动作 = **删除** `src/main/resources/.../quest_definition/quests/<id>.xml`（不再 `git mv` 到 `src/test/resources/quest/retired/`）；
- 退役事实的唯一仓库内记录 = `retail-xml-retention.tsv` 的 `owner=RETAIL_TABLE` 行（6224 行全量台账）；
- 历史内容回溯方式 = git（`git log --follow -- src/main/resources/aion/data/static_data/quest_definition/quests/<id>.xml`）；
- `src/test/resources/quest/retired/`（1867 个冻结副本）**已删除**，且 `verify_retirement.py` 新增"该目录不得存在"检查。

## 2. 交付

| 交付物 | 位置 | 说明 |
|---|---|---|
| 生产视图 | `src/test/java/.../definition/ProductionQuestDefinitions.java` | XML 目录 + `RetailQuestDriver.overlay`，惰性编译 + 缓存；`catalog()` / `definition(id)` / `entry(id)` |
| 退役集合读取 | `src/test/java/.../definition/RetiredQuestIds.java` | 直接读保留清单 `owner=RETAIL_TABLE`（不再读目录） |
| 生产 XML 装载 | `src/test/java/.../definition/QuestXmlFixtures.java` | 只读生产 XML；缺失即失败并提示改用生产视图；新增 `productionXmlPresent(id)` 看源树 |
| 删除 | `src/test/resources/quest/retired/`（1867 个 XML）、`RetiredQuestDefinitions.java` | 历史由 git 承担 |
| 退役脚本 | `m2e_retire_migrated_xml.py` / `m3b_retire_simple_talk_xml.py` | 改为**删除**（幂等：`git cat-file -e HEAD:<path>` 判已退役）；证据列改 `git-history:<path>` |
| 保留清单生成 | `build_retention_list.py` | 宇宙改 `catalog ∪ 既有清单`（不再读 fixture 目录）；退役标记改 `retired-xml-in-git-history` |
| 退役收口校验 | `verify_retirement.py` | 退役集合读清单；新增"不得存在测试作用域副本目录"检查 |
| 目录文档 | `docs/QUEST_CATALOG.zh-CN.md` + `refresh_catalog_doc_links.py` | 1867 行链接改为「已退役（真端驱动；XML 见 git 历史）」标注，**不生成任何链接** |
| 写作指南 | `docs/quest/WRITING_GUIDE(.zh-CN).md` | 退役任务的例子改指 git 历史回溯（不再指向冻结副本） |
| 历史工具适配 | `audit_removed_spawned_ids.py` / `restore_equivalence_ids.py` / `build_retail_contract_tsv.py` / `build_item_selectable_contract_tsv.py` | 退役 XML 一律从 `git show HEAD:<path>` 读取或按"原样删除"推理 |

## 3. 测试适配（去 fixture 后补齐）

| 类型 | 类 | 做法 |
|---|---|---|
| 生产视图取定义 | `Quest1112ProductionFlowTest`、`MonsterHunt1102DefinitionTest`、`MonsterHuntFamilyDefinitionTest`、`JavaHandlerFamilyDefinitionTest`、`BlankJournalSlotBoundaryContractTest`、`AlignedMirrorRewardRowContractTest`、`QuestDraupnirNpcVariantContractTest`、`ReportTo1101DefinitionTest` | `definition(id)` → `ProductionQuestDefinitions.definition(id)` |
| 文档型门禁改 IR 断言 | `QuestSimpleHuntRetailContractTest` | 退役任务改 `checkRetiredRow`：槽位字段几何 + 可计数上限 = 真端 `countN` + 覆盖真端怪集合 |
| 文档型门禁改 IR 断言 | `QuestRewardItemGateTest` | 退役任务改 `parseRetailProduction`：固定道具 = 档位 1 的 ITEM；可选项 = 领奖确认分支多发放的道具；ext = 元数据最后一轮追加 |
| 冻结证据型门禁 | `RetailSimpleTalkGateTest`、`RetailSimpleHuntIrCompilerTest`、`QuestSimpleHuntRetailContractTest`、`RetailOwnershipGateTest` | 退役任务不可重算 → 登记行 / 冻结 IR 指纹即证据；仍由 XML 拥有的任务照旧现算对拍 |
| 宇宙断言 | `RetailMetadataEquivalenceGateTest` | `XML 目录 + 退役集合 = 6224`；XML↔真端对拍只覆盖仍由 XML 拥有的任务 |
| 节点标签 | `MonsterHunt1102DefinitionTest` | 真端合成器规范形 `a0..a3`（0..3 次击杀）替代 XML 时代标签 |

## 4. 本轮修复的语义问题（真端驱动侧）

| 问题 | 影响 | 修法 |
|---|---|---|
| 掉落目标未按同名族闭包展开 | 退役任务（如 2631）的掉落只绑真端 id，本服实刷别名不掉任务物品 | `RetailQuestMetadataCompiler` 对 `drop_monster` 解析结果套 `withDisplayNameVariants` |
| 元数据门禁 `drops` 轴口径 | 闭包后两侧不同形 | `dropsKey` 两侧同口径闭包归一（集合语义，去重） |
| ext 槽位合同漏项 | 快照生成器只取 `reward_item_ext_1`，漏 `_2.._N` | `build_item_selectable_contract_tsv.py` 改 `^reward_item_ext_\d+$`；重算后 10 行基线补全（行数仍 3992） |
| 领奖投影 | XML 时代 1101 `reward var0=1` 指向不存在的行 | 生产视图 = 客户端任务书末行行号（1101/1102 → 0）；测试同步 |
| 完成路由粒度 | 1230 XML 只声明 choice 1/2 | 真端合成器发放全段 16 条；差异已在 `retail-simple-talk-drift.tsv` 登记（`1230 DIFF:TRANSITION_SET`） |

## 4b. 降级批次（M3-d 前哨）：真端合成无法表达的任务回到 XML

全树对账（`gates/m3b-nofixture-questengine-run1.log`）暴露 **38 条退役引发的新失败**，
全部落在"逐任务客户端契约测试 vs 真端合成定义"上。根因样本（1141，逐条取证）：

- 客户端 `Dialogs/QUEST_Q1141.html`：select1(1011) → ask_accept(4) → accept_1(1003) → **select5(2375)**
  → select_quest_reward1(5) → quest_complete(1008)；
- 退役前 XML：报告环节由**酒桶对象触发**（`USE_OBJECT` → page 2375）；
- 真端合成定义：报告环节是 `QUEST_SELECT`（dialog 31）路由 —— 真端模板表**没有"对象触发的报告"列**，
  无法表达该形态；
- 结论：属于用户口径里的"**真端无法解决 → 保留 XML 降级**"。

落地（工具 `m3d_downgrade_to_xml.py`，可重跑）：

| 批次 | 任务数 | 代表家族 | 触发证据 |
|---|---:|---|---|
| 1 逐任务客户端契约 | 20 | 1141（酒桶对象报告）、1351、2110、2150/23902/28800/28802/3103/4913/18802、30312/30314/30315、2654、1526、26930、80290/80294、1117/1353 | `gates/m3d-downgrade-batch1-run1.log` |
| 2 事件/电影节 | 16 | 11003、80016/80018、80028/80029/80031/80032、80034–80039、18505/18509/28509 | `gates/m3d-downgrade-run2.log`（类适配后的 302 例复跑日志，31F+7E，暴露本批） |
| 3 报告型/收集型家族 | 47 | 80356/80365、80268–80686 报告族（25）、18618–18627 + 28618–28627 收集族（20） | `gates/m3d-downgrade-batch3-run1.log` |
| **合计** | **83** | catalog 4357 → **4440**；RETAIL_TABLE 1867 → **1784** | |

判据（可审计）：**逐任务客户端契约测试**是该任务客户端可见行为的证据；真端合成无法满足它 → 该任务退回
XML 所有权（`owner=XML_RETENTION`、`reason=SEMANTIC_GAP:CLIENT_ROUTE`、`evidence=downgraded:*`），
XML 由 `git show HEAD:<path>` 复原、catalog 行按 id 顺序插回、目录文档链接同步恢复。

复跑结果：

| 证据 | 结果 |
|---|---|
| 降级批次三组聚焦复跑 | batch1 76 例（2 条外族）→ 第二轮族级复跑 **12/12 绿**（`m3d-downgrade-batch3-run1.log`） |
| 退役收口 | `catalog=4440 directory=4440 retired=1784 sum=6224 — OK` |
| 聚焦门禁（30 类，降级后） | **123/123 绿**（`gates/m3d-focus-run1.log`） |
| **questEngine 全树** | **1933 例 / 28F+7E（35）**，与干净 HEAD 基线**逐方法一致**（only-now=0、only-baseline=0；`gates/m3d-questengine-run1.log`） |

## 5. 验证证据

| 证据 | 结果 |
|---|---|
| 聚焦门禁（30 类，`gates/m3b-nofixture-run7.log`） | **123/123 绿** |
| 退役收口（`verify_retirement.py`，M3-d 降级后） | `catalog=4440 directory=4440 retired=1784 sum=6224 — OK`（无悬空生产引用；无冻结副本目录） |
| 保留清单重算（`build_retention_list.py`，M3-d 降级后） | 与在库清单**零漂移**（6224 行；RETAIL_TABLE 1784 / FAMILY_PENDING 2389 / SEMANTIC_GAP 1381 / SCRIPTED 494 / NO_TABLE 176） |
| 目录文档（`refresh_catalog_doc_links.py`，M3-d 降级后） | `retired=1784 replaced=0 dangling=0`（无链接残留） |
| 退役类适配后复跑（`gates/m3c-restore-run1.log`） | 302 例 / 59F+13E（退役引发的新失败收敛到 38 条） |
| 降级 + 全树复跑（`gates/m3d-questengine-run1.log`） | **1933 例 / 28F+7E**，与干净 HEAD 基线逐方法一致（only-now=0） |

## 6. 未验证

- 服务端重启与实机客户端抽检（需用户执行）；
- 全量 `mvn test` / `mvn package`（未授权）。

## 7. 全树对账（最终）

- 命令：`mvn -o test -Dtest=<467 类 questEngine 选择器>`（`gates/m3d-questengine-run1.log`）
- 结果：**1933 例 / 28F+7E = 35**，与干净 HEAD 基线（`gates/head-baseline-2026-09-23.log` +
  `gates/head-baseline-failures.txt`，35 条）**逐方法一致**：only-now=0、only-baseline=0
  （参数化用例 `QuestRetailCollectionRoleAlignmentTest(CollectionCase)[2,5,6,7,8,9,10]` 同集合）。
- 🔴 仍然**未做**：服务端重启与实机客户端抽检（需用户执行）。

## 8. 收尾批次（用户口径再确认 + 索引清理 + 降级登记持久化）

用户本轮再次明确口径：**"旧 xml 已经在 git 历史中，所以没必要迁移到 test。"**
据此把上一轮的"删除生产 XML"从工作区约定推进到 **git 索引级事实**，并补上降级批次的持久化登记。

### 8a. 索引清理（迁移痕迹归零）

- 早期 `git mv` 把 1867 个生产 XML 暂存成 `src/test/resources/quest/retired/<id>.xml` 重命名条目，
  工作区副本已删、索引仍带 `R`（提交会把冻结副本重新带回仓库）。
- 处理：`git rm -r --cached src/test/resources/quest/retired` → 索引只剩 1867 条 `D`（纯删除），
  **仓库内不保留任何测试作用域副本**；退役历史统一走 `git log --follow -- <原路径>`。
- M3-d 降级复原的 **83 个**生产 XML 原本处于"索引已删 + 工作区未跟踪"的危险状态（提交会连带删除），
  已用 `git add` 重新纳入索引，`git diff --cached HEAD` 对 1141.xml 等为空 = 与原提交逐字节一致。

### 8b. 降级登记持久化（消除"重算即虚报"的坑）

- 新增 `.agents/summary/scriptdll-quest-driver/m3d-downgraded-quests.tsv`（83 行：`quest_id code note`）。
- `m3d_downgrade_to_xml.py` 新增 `register_downgrade()`（幂等 upsert，`--dry-run` 不落盘）。
- `build_retention_list.py` 新增最高优先级分支 `downgrade_registry()`：命中登记 → `XML_RETENTION` /
  `SEMANTIC_GAP:<code>` / `evidence=downgraded:<note>`，**不再**把这些行按"真端表族 + 驱动已实现"算回
  `RETAIL_TABLE`（否则会虚报"已由真端驱动"）。
- 复算证据：重跑生成器后三份清单（`.agents` / `src/test/resources` / `src/main/resources`）
  与在库版本 **diff = 0**，分布 `RETAIL_TABLE 1784 / SEMANTIC_GAP 1381 / FAMILY_PENDING 2389 /
  SCRIPTED 494 / NO_TABLE 176`。

### 8c. 仓库整洁

- 删除本轮遗留探针 `src/test/java/com/aionemu/gameserver/questEngine/definition/ZZProbe1141Test.java`；
  `probe/` 下日志保留为降级取证。
- 清理改写脚本留下的未使用 import：**64 个文件 / 110 条**（7 种：`InputStream` 60、`Objects` 24、
  `QuestDefinitionXmlCompiler` 8、`QuestXmlFixtures` 7、`Files` 4、`Path` 4、`assertNotNull` 3），
  复扫剩余 0 条。

### 8d. 本轮验证

| 证据 | 结果 |
|---|---|
| `mvn -o -q test-compile` | 通过（仅 Lombok `Unsafe` 弃用告警） |
| `python3 -B .agents/summary/scriptdll-quest-driver/verify_retirement.py` | `catalog=4440 directory=4440 retired=1784 sum=6224` + `OK: 目录一致，无悬空生产引用` |
| `python3 -B .../build_retention_list.py` + 三份清单 diff | diff = 0（零漂移） |
| 未使用 import 复扫 | 0 |

**状态**：全部变更仍在工作区，**未提交**（等待用户授权）。下一步进入 **M4-b CombineTask**（574 行 → 合成器 + 家族门禁 + 退役）。

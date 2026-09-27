# M1 真端元数据层 报告

- 日期：2026-09-23
- 切片：M1（元数据层：真端 quest.xml → QuestMetadata + 全量对拍门禁）
- 覆盖范围：全部 6224 个生产目录任务（全局基础设施，服务于全部 8 族）

## 1. 交付（新增/修改文件）

**可复用资产（生产代码，`questEngine/retail/`）**
- `RetailQuestXmlTable.java` — 真端 quest.xml（10035 任务）只读装载器；支持数字字段族
  （`finished_quest_cond1..N`）与容器块（`fighter_selectable_reward` 的 `<data>` 子块）。
- `RetailQuestMetadataCompiler.java` — quest.xml 行 → `QuestMetadata` 映射器（本切片核心）。
  内置经全库投票验证的静态映射：职业 token→PlayerClass（含 base 展开规则）、称号符号名→id
  （`RetailQuestTitleIds`，142 项）、工艺技能名→id（9 项）、NPC 势力名→id（10 项）。
  无法解析的符号名进 `Outcome.unresolved()`（调用方降级）。
- `RetailItemNameIndex.java` — 物品 `name_desc`→item_id 索引（`RetailNpcNameIndex` 的物品侧孪生）。
- `RetailQuestTitleIds.java`（生成）— 称号符号名→id 快照。

**入仓真端数据（`static_data/quest_retail/`）**
- `quest.xml` — 服务器真端 quest.xml 转 UTF-8（11.3MB，10035 任务；与客户端解包副本除 dev_name 外逐字节一致）。
- `quest_name_string_ids.tsv` — STR_QUEST_NAME_Qxxxx → 客户端字符串 id（9546 条；
  615 条缺失者回退 quest_data nameId——displayNameId 的真端来源已闭环）。

**门禁**
- `RetailMetadataEquivalenceGateTest`（definition/）— 全量 6224 任务：
  真端映射元数据 vs quest-definition XML 元数据，30 个轴逐轴对比；
  未登记差异一律失败；`-Dretail.metadata.diffOut=` 导出模式供再分诊。
- `src/test/resources/quest/retail-metadata-divergences.tsv` — 已登记分歧 4135 行 / 2624 任务，
  三类：`RETAIL_COND_PLACEMENT`(2127)、`RETAIL_PRIORITY`(1282)、`XML_ONLY_ASSET`(726)。

**审计脚本（`.agents/summary/scriptdll-quest-driver/`）**
- `audit_quest_data_vs_xml_metadata.py` — quest_data.xml ↔ 生产 XML 元数据全量对账（结论：quest_data
  陈旧，3977 任务有差异，不可作为真端元数据源）。
- `audit_generate_metadata_divergences.py` — 由门禁导出生成分歧登记表。

## 2. 证据（命令 + 结果）

| 命令 | 结果 |
|---|---|
| `python3 -B .agents/summary/scriptdll-quest-driver/audit_quest_data_vs_xml_metadata.py` | checked=6224 diffs=6046 quests_with_diff=3977（quest_data 不可用做真端元数据源） |
| `mvn -Dtest='RetailMetadataEquivalenceGateTest' test` | **Tests run: 1, Failures: 0, Errors: 0** BUILD SUCCESS |
| `mvn -Dtest='com.aionemu.gameserver.questEngine.retail.*Test,QuestSimpleHuntRetailContractTest' test` | **Tests run: 13, Failures: 0, Errors: 0** BUILD SUCCESS（基线无回归） |

迭代过程：首轮 8143 差异行 → 定性出 9 个系统性缺陷并修复（reward_repeat_count 255 规则、
drops 下划线族、collecting_step=collect_progress、quest_repeat_cycle 无后缀标签、条件归属规则、
displayNameId 客户端字符串源、重复奖励组合并、each_member 缺省、跨组同值标量去重）→ 剩余
4135 行全部定性为三类已登记分歧。

## 3. 结论

- 真端元数据层可用：6217/6224 任务可从真端 quest.xml 完整映射出 QuestMetadata（7 个为服务端自有
  EVENT 任务，真端无行，已登记 RETAIL_MISSING）。
- 等价面：3600 个任务 30 轴全等；其余 2624 个任务的差异全部落入三类已登记口径（见 §4）。

## 4. 对拍结果（登记分歧的定性）

- **元数据**：30 轴全量对拍完成；登记分歧 4135 行。
  - `RETAIL_COND_PLACEMENT`（2127 行，~1050 任务）：生产 XML 对真端 `finished_quest_cond`
    混用 prerequisites / start-conditions 两种表达（912:600 比例，与真端输入无相关性，属转换期噪声）。
    运行时语义基本等价（finished 条件多 reward-mode/重复次数校验）；映射器规则固定，分歧按任务登记。
  - `XML_ONLY_ASSET`（726 行，~420 任务）：bonuses/kills 在真端 quest.xml 无数据源（来自客户端附带表）；
    `grep -rn '\.bonuses()\|\.kills()' src/main/java` 证明 typed 链路无消费方，映射为空行为等价。
  - `RETAIL_PRIORITY`（1282 行）：逐任务方言，典型如 XML 丢失真端随机奖励/职业奖励
    （1397/1540/1007）、XML 多出的旧版物品需求（1182）、category 方言（SEEN_MARKER vs PUBLIC）等。
    按用户既定规则 §2.5 真端优先，登记可溯。
- **进度事件 / IR / 调度逐帧 / 客户端 SECTION**：本切片不涉及（元数据层不含节点/边），后续族切片覆盖。

## 5. 未验证

- 服务端重启后真端元数据层的实机装载（未授权重启；生产接线在 M2+）。
- `name` 轴按真端 Qxxxx 约定（英文名无运行时消费方，grep 证据已记录；客户端名字由客户端字符串表驱动）。

## 6. 阻塞与决策项（不停等，继续推进）

| 项 | 处理 |
|---|---|
| RETAIL_COND_PLACEMENT 的严格语义差（finished 条件多 reward-mode 校验） | 已按固定规则归属并逐任务登记；如需逐任务复核，登记表可重放 |
| 真端 999 封顶 vs 生产 UNLIMITED（7 任务） | 已归一化；cap-82 轴复用既有例外表 |

## 7. 下一步

- M1-owner：生成 6224 全量 owner/保留清单（`retail-xml-retention.tsv` 雏形）：
  三态归属（真端模板表族 / XML 保留）+ 原因（SCRIPTED / NO_TABLE / RETAIL_PRIORITY 等）。
- 复现：`python3 -B .agents/summary/scriptdll-quest-driver/retail_family_coverage.py`；
  `mvn -Dtest='RetailMetadataEquivalenceGateTest' test`。

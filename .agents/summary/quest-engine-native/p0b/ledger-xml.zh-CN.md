# 台账 XML 化批（2026-10-03，用户指令「`src/main/resources/aion/data/static_data/quest` 下的 tsv 改造成 xml」→ 范围裁定「quest/retail 下的真端没有的 tsv」；零行为变更）

范围 = `quest/retail/` 下 8 个**自造 TSV 台账**（7 张运行时装载 + 1 张 retention 清单 + 其 test 侧副本），
改造成同目录同名 XML + 同名 XSD，全部消费方随批迁移。执行前置条件 = 并行工作流（P9/P10，commit
`91eaef381`）落定；转换基线 = 落定后的工作树。

## 1. 清点与冻结（先行，执行中不得改名）

命名与行模型冻结表（行 = 行元素；列 = 列子元素；多值列 = 逗号串单元素文本；2 空格缩进；LF/UTF-8；
TSV `#` 注释逐行映射为 XML 注释，涉及「列/格式」表述的行同批改写）：

| # | 文件（stem，.xml/.xsd 同名） | 根 | 行 | 列（按数据列序） | 数据行 | 消费面 |
|---|---|---|---|---|---|---|
| 1 | quest_client_handin_npc_sets | quest_client_handin_npc_sets | handin_npc_set | quest_id, npc_ids | 215 | RetailClientHandinNpcSets + 合成器 |
| 2 | quest_legacy_heal_rows | quest_legacy_heal_rows | heal_row | quest_id, stale_row, reward_row, evidence | 2 | RetailLegacySaveHealRows |
| 3 | quest_name_string_ids | quest_name_string_ids | name_string_id | key, string_id | 10161 | RetailQuestDriver.nameIds + 2 测试 |
| 4 | retail-instance-entry-points | retail_instance_entry_points | entry_point | creation_id, world_id, alias, x, y, z, heading, resolved, source | 3 | NativeInstanceEntryPort |
| 5 | retail-npc-name-aliases | retail_npc_name_aliases | npc_name_alias | name, npc_ids | 178 | RetailNpcNameIndex + NativeNpcNameResolver |
| 6 | retail-quest-ai-name-groups | retail_quest_ai_name_groups | quest_ai_name_group | quest_ai_name, member_name_descs | 60 | 同上 |
| 7 | retail-quest-string-ids | retail_quest_string_ids | quest_string_id | key, string_id, body（可缺=旧格式行） | 7 | RetailStringIds |
| 8 | retail-xml-retention | retail_xml_retention | quest | quest_id, owner, family, reason, evidence | 6224 | RetailQuestDriver 启动校验 + RetiredQuestIds + DataDrivenNativeRuntime + 17 测试（test 副本） |

冻结的 XSD 口径要点（生成器 = 扩展后的 `generate_retail_xsd.py`；观察全域 ∪ 装载器 fail-closed）：
行键 `xs:unique`（8 表键全部实测唯一）；`key` 形 `STR_QUEST_NAME_Q[0-9]+`；`string_id` int；
逗号串 `\d+(,\d+)+`（handin/alias 钉 ≥2）；retention `owner`/`family` 枚举与
`reason` 模式 `(OK|SCRIPTED|NO_TABLE|ADJUDICATED:[A-Za-z0-9_]+)`；`resolved` xs:boolean。
不可表达面（头注释注明 + 装载器兜底）：列表去重、`owner=RETAIL_TABLE ⇔ reason=OK` 共现、
`family=- ⇒ XML_RETENTION`、`quest_name_string_ids.string_id 不唯一`（反直觉，禁加 unique）。

## 2. 执行（逐文件 sha256 前→后 + git 快照）

- **转换基线**：`HEAD = c60824347`（`91eaef381` P9 落定后；`3e476908b`/`c60824347` 两提交对 8 路径
  零 diff，`git show --stat` 复核）。转换器开工时校验 8 个 TSV 工作树干净，并钉 `git rev-parse HEAD`。
- **前→后 sha256**：逐表 `before_sha256`（`git show HEAD:<tsv>` 原始字节）与 `after_sha256`
  （新 XML 原始字节）记录于 `p0b/ledger-xml/ledger-equivalence.tsv`（8 行，含行数）。定稿时复核：
  工作树 8 个 XML 与 test 副本的当前 sha256 与记录逐字节一致（证据未被后续改动污染）。
- **改动面**：8 个 main TSV `git rm`（已暂存删除）；8 个 `*.xml` + 8 个 `*.xsd` 新增；
  `src/test/resources/quest/retail-xml-retention.xml` 新增（与 main 副本**同字节**，
  sha256 `1e3342e3…` 双方一致）。
- 转换器 `convert_ledgers_to_xml.py`：读工作树、`--force` 防呆、注释文本硬校验（`--`/尾 `-`/`<!`）、
  文本转义 `& < >`（非 CDATA）、C0 控制字符白名单、2 空格/LF/UTF-8/末行换行。

## 3. 等价性验证（规范行逐行全等 + 注释保留 + 双副本字节相等）

`verify_ledger_equivalence.py`（before = `git show HEAD:<tsv>`，after = XML 解析，按表重建**规范行元组**
逐行比较 + 注释逐条核对 + 双副本字节相等）。定稿复核重跑结果：

```
self-test OK (value mutation / row-count mutation / comment checks all red as expected)
OK   quest_client_handin_npc_sets: rows=215 verdict=IDENTICAL
OK   quest_legacy_heal_rows: rows=2 verdict=IDENTICAL
OK   quest_name_string_ids: rows=10161 verdict=IDENTICAL
OK   retail-instance-entry-points: rows=3 verdict=IDENTICAL
OK   retail-npc-name-aliases: rows=178 verdict=IDENTICAL
OK   retail-quest-ai-name-groups: rows=60 verdict=IDENTICAL
OK   retail-quest-string-ids: rows=7 verdict=IDENTICAL
OK   retail-xml-retention: rows=6224 verdict=IDENTICAL
```

自测以「值突变 / 行数突变 / 注释缺失」三类注入验证对拍器会红（防空转假绿）。转义往返（retention
evidence 列含 `>` 的行）由 after 侧显式反转义覆盖。

## 4. XSD 落地（8 张 schema + 校验）

- `generate_retail_xsd.py` CONFIGS 10→18（+8 台账项；小能力：`string_patterns`/`decimal_tags`/
  `nonblank_tags`/`optional_tags`/`key_type`/`batch` 注记）；既有 10 张 XSD 重生成**字节不变**。
- 新 8 张 XSD 落同目录（19 = 18 门禁表 + `HtmlPages.xsd` 既存散件）。
- 全部 8 对 `xsd_validate.java`（JDK Xerces）校验 `OK`；模式整值锚定行为（`x1,2y` 不满足
  `\d+(,\d+)+`）在 Xerces 与 libxml2 双实现实测确认，故 `handin/alias` 的 ≥2 钉用整串 pattern 表达。

## 5. 承重面（门禁）

- `RetailTableSchemaGateTest.TABLES` 十→十八（+8 台账 stem）；既有「真资源×同目录 XSD 校验」与
  「无 DOCTYPE + 仅五预定义实体」两用例自动覆盖新表；javadoc 十表→十八表。聚焦套件内绿。
- `RetailOwnershipGateTest` 新增 `retentionCopiesStayIdentical`：`/quest/retail-xml-retention.xml`
  与主副本 `assertArrayEquals`，失败消息带两侧 sha256（此前零守卫、漂移已实际发生过一次）。
- `RetailEnterAreaZoneRegistrationGateTest` 的文件系统直读（依赖 CWD）改为 classpath 读主副本。

## 6. 连带同步

- **main 解析点**（11 处，语义/错误码逐条保留）：RetailClientHandinNpcSets、RetailLegacySaveHealRows、
  RetailStringIds、RetailQuestAiNameGroups、RetailNpcNameIndex（别名+组表）、RetailQuestDriver
  （nameIds + verifyProductionCoverage + parse 委托）、NativeInstanceEntryPort、NativeNpcNameResolver
  （别名+组表）、DataDrivenNativeRuntime（retentionSwitchSet）、RetiredQuestIds、RetailNpcNameAliases
  （资源常量）；注释面 RetailQuestMetadataCompiler、SimpleCollectItemHandler 等随批改字。
  新助手 `RetailLedgerXml`（secure parse：disallow-doctype-decl + ACCESS_EXTERNAL_DTD/SCHEMA=""）。
- **test**：新夹具 `RetailLedgerRows` + 12 个直接解析者迁移（含去未用 import/helper）。
- **生产者/读者脚本**（14 个）：`build_retention_list.py`、`p0c52_quest_ai_name_groups.py --emit`、
  `dd-unresolved-name-probe.py`（--emit 改插入式 XML 追加）、7 个 p7/p3/p10 读者、`convert_*`/
  `verify_*` 两新工具。
- **收口扫描**：8 个 stem 的 `.tsv` 引用在 `src/` 全域零残留（历史性表述行以「原…退役」留档）。
- **计划与记忆库**：迁移计划 §5.2 两行（handin / ai-name-groups）+ §5.4 四行（retention / name_string_ids /
  aliases / legacy-heal）随批注明 XML 化 + 追加第五十三版；memory-bank 引用改指（6 文件 184 处文本
  替换 + 元数据字段 162 处改含目录限定路径 `quest/retail/retail-xml-retention.xml`——裸文件名在
  XML 化后入扩展名域，与 test 副本双双命中后缀搜索即判 missing path，实测 158 处结构红灯一次修；
  QE-062 同步扩写该变体）→ `sync_memory_bank.py` + `verify_memory_bank.py` **三步全绿**（STEPS=3）。

## 7. 边界与后续

### 7.1 聚焦验证（授权：用户「一次授权全批」）

```
mvn -q test -Dtest='RetailTableSchemaGateTest,RetailOwnershipGateTest,RetailQuestDriverOverlayTest,\
RetailMetadataEquivalenceGateTest,QuestRetailClassGateTest,ItemPlayFamilyRowInventoryGateTest,\
RetailDataDrivenClientPresenceGateTest,RetailNonIrAxisGateTest,RetailEnterAreaZoneRegistrationGateTest,\
DataDrivenQuestTableGateTest,DataDrivenNativeContractGateTest,DataDrivenNativeRuntimeGateTest,\
DataDrivenEnterAreaPortGateTest,RetailQuestAiNameGroupGateTest,NativeNpcNameResolverTest,\
RetailSayBubbleGateTest,RetailRewardWindowRouteTest,UseItemFamilyRowInventoryGateTest,\
QuestSimpleHuntRetailContractTest' -DfailIfNoTests=false
```

结果：**92 用例 / 88 绿 / 3F+1E 全部为批前既存红**（本批零新增红灯）：

| 失败用例 | 根因（批前已存在） | 判据 |
|---|---|---|
| `RetailNonIrAxisGateTest.cappedQuestsAreNeverRetailDriven` | 26 个 DataDriven 封顶行 P5D 起 owner=RETAIL_TABLE，封顶台账未随表同步退役 | 2026-10-01《聚焦套件红线归因》§4-D 已登记；HEAD 逐行 grep 与转换后一致 |
| `RetailNonIrAxisGateTest.saveHealQuests…`（16900） | owner=RETAIL_TABLE 但定义已退役（"missing production quest definition N"） | 同文档 §4-C 原文判据 |
| `ItemPlayFamilyRowInventoryGateTest.evidenceFacesStayFrozen` | 冻结集写于 P5D `fa010d3f8`；P9 `91eaef381` 组表 33→60 + 成员集语义 ⇒ 5 个名由未解析转解析 | pre/post-P9 组表 TSV `git show` 双向 grep；P9 未触碰该门类 |
| `RetailQuestAiNameGroupGateTest.everyDeclaredGroup…`（jabsuroong） | P9 新增组名与 pre-P9 既存别名行（TSV line 9）撞通道互斥闸 | 两侧数据 `git show` 验证；P9 未触碰该门类 |

四红归属各自批（AI 组表/P9 面 2 条、DataDriven 家族门禁债 2 条），按「门禁按家族在切换批内逐批
重锚」口径清偿，不在本格式批内改判。

**HEAD 隔离基线实证**（用户授权；仓外临时 worktree 检 HEAD `c60824347`，只跑 3 个红灯类，
`mvn -q test -Dtest='RetailNonIrAxisGateTest,ItemPlayFamilyRowInventoryGateTest,RetailQuestAiNameGroupGateTest'`
→ Tests run: 14, Failures: 3, Errors: 1）：**同 4 红原样复现**——26 封顶任务清单逐字相同
（35052…46548）、`missing production quest definition 16900` 同错、ItemPlay 冻结面同 6 名对
`[_faction_]`。唯一表述差异：AiNameGroup 门的首个违例组基线为 `HousingManager_Li`、本树为
`NPC_event_svs_jabsuroong`——该门经 `RetailQuestAiNameGroups.defaultGroups()`（`Map.copyOf`，
HEAD 原版同款）迭代，**顺序按 JVM 运行随机化**，两侧违例集合一致而首个命中随机；本批对该
方法的转换逐语义等价（含 `Map.copyOf` 保留）。实证后 worktree 已 `remove --force` + `prune`。

### 7.2 推迟的历史性一次性脚本（记录不转换）

以下脚本引用旧 `.tsv` 路径，均为已完成批次的一次性取证/生产者（重跑会 fail-fast 或写向废弃路径，
不会静默复活运行时 TSV），随本批留档不再维护：`p3/tools/step5_anchor_evidence.py`、
`p0a/tools/owner_identity.py`、`p5d/tools/itemplay-{longtail,shape-audit,activation}-probe.py`、
`quest-area-grant/probe/run_flip_probe.sh`、`quest-acquire-npc-set/build_quest_client_accept_npc_sets.py`
（路径为该表迁入 `quest/retail/` 前的旧址）、`scriptdll-quest-driver/p0c52_retire_guard_group_rows.py`。

## 8. 证据索引

| 证据 | 位置 |
|---|---|
| 等价性对拍（before/after sha256 + 判定） | `p0b/ledger-xml/ledger-equivalence.tsv` |
| 转换器 / 对拍器 / XSD 生成器 / 校验器 | `p0b/tools/{convert_ledgers_to_xml,verify_ledger_equivalence,generate_retail_xsd}.py`、`p0b/tools/xsd_validate.java` |
| 8 张 XSD | `quest/retail/<stem>.xsd` |
| 承重门 | `RetailTableSchemaGateTest`（18 表）、`RetailOwnershipGateTest.retentionCopiesStayIdentical` |
| 红归因原始登记 | `quest-engine-native/gates/2026-10-01-red-attribution.zh-CN.md` §4-C/§4-D |

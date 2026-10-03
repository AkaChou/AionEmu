# retail 十表 DOCTYPE 剥离 + XSD 落地批（2026-10-03，用户指令「xml 没有 xsd 的添加 xsd，有 DOCTYPE 去掉」；零行为变更）

## 1. 清点（先行）

`src/main/resources/aion/data/static_data/quest/` 全树 754 个 XML 的清点结论：

| 面 | 事实 |
|---|---|
| 已有 schema 链（零改动） | `definitions/quest_definition_catalog.xml`（catalog xsd，`QuestDefinitionCatalogManifest` 装载）、`definitions/quests/*.xml` ×740（`QuestDefinitionXmlCompiler` 以 `quest_definition.xsd` 校验装载）、`legacy/` ×2（xsd 同目录）、`retail/HtmlPages.xml`（`HtmlPages.xsd`，前一批） |
| 有 DOCTYPE 无 XSD（本批十张） | `Quest_SimpleHunt / Quest_SimpleSerialHunt / Quest_SimpleTalk / Quest_SimpleCollectItem / Quest_SimpleUseItem / Quest_SimpleItemPlay / Quest_CombineTask / quest.xml / data_driven_quest.xml / npcfactions_quest.xml` |

## 2. 实体语义实证（剥离安全边界，运行时同款解析器配置）

探针 `tools/xml_dom_probe.java`（FEATURE_SECURE_PROCESSING + 外部 DTD/schema 拒绝 + 实体展开开，
与 `NativeQuestTableLoader/NativeQuestXmlTable/RetailQuestXmlTable/DataDrivenQuestTable` 完全同配置）：

- **预定义五实体不受自名声明影响**：DOCTYPE 里 `<!ENTITY quot "quot">` 等声明被解析器按规范忽略，
  `&quot;` → `"`、`&apos;` → `'`、`&gt;/&lt;/&amp;` → 标准字符（实测 dev_name 行逐字符确认）；
- **非预定义实体按声明文本展开为字面量**：`<!ENTITY hellip "hellip">` ⇒ `&hellip;` 当前解析 =
  6 字符字面量 `hellip`（**不是 U+2026**）——`Quest_SimpleTalk.xml` ×1、`Quest_SimpleUseItem.xml` ×1
  （均在 `dev_name`，如 `[이벤트] 요정으로부터&hellip;` → `...으로부터hellip`）；
- 正文实体引用全集（去注释/去 DOCTYPE 实测）：仅 `amp/lt/gt/apos/quot/hellip` 六种；
  其余八表零引用。

**剥离动作**：DOCTYPE 块整体删除 + `&hellip;` → 字面量 `hellip`（= 当前解析结果，逐字符不变）。
**明确不做**：不把 `hellip` "修复" 为 `…`（那是语义变更，属后续批决策，且这两个字段是 dev_name 备注列）。

## 3. 执行（`tools/strip_retail_doctype.py`，幂等）

| 文件 | sha256 前 → 后 | 处理 |
|---|---|---|
| Quest_CombineTask / SimpleCollectItem / SimpleHunt / SimpleItemPlay / SimpleSerialHunt / data_driven_quest / quest | 见脚本输出 | DOCTYPE 块（~100 行自名实体）+ 随行换行删除，其余字节不动 |
| Quest_SimpleTalk / Quest_SimpleUseItem | 同上 | 同上 + `&hellip;` → 字面 `hellip` |
| npcfactions_quest | f1b63f6a → 491152f6 | **用户裁定转码**：UTF-16LE(BOM)+CRLF → UTF-8+LF、声明 `UTF-16`→`UTF-8`、删 DOCTYPE（正文零实体引用，227676 → 111166 B） |

## 4. 等价性验证（DOM 级，逐元素）

变换前后各以运行时同款解析器配置全文转储（探针 `dump` 模式：每元素 路径/属性/文本，
Unicode 转义可 diff）：**10/10 逐行全等**（含 npcfactions 转码前后）。清单：
`dom-equivalence.tsv`（before/after dump sha256 + 元素数 + `IDENTICAL`；before = HEAD 版可随时由
`git show` 复现，原始 dump 属中间产物不驻留）。

## 5. XSD 落地（十张，同目录同名，`tools/generate_retail_xsd.py` 生成）

生成口径（逐条写入各 XSD 头注释）：

1. **required 行子元素 = 观察全域（出现率 100%）∪ 装载器 fail-closed 必填**
   （如 talk/collect/use-item/item-play 双 NPC、combine 的 task_npc/combineskill/recipe_name/product）；
2. **`xs:unique` 行键**：行 id（装载器对重复 fail-closed）；`quest.xsd` 追加 `name` 唯一
   （`NativeQuestXmlTable` 对非空重名 fail-closed）；
3. **int 定型 = 生产 int 解析点清单**：七张简单表 = `NativeQuestTableLoader`（con_quest / cutsceneid1 /
   cs1_haction / countN / count_* / combine_skillpoint≥0）；quest.xml = `RetailQuestMetadataCompiler`
   `integer()` 全量 42 项（含 drop_prob_N / reward_*N 族）；DD 表 `<id>`；npcfactions 行键属性；
4. **枚举/模式 = fail-closed 语义域**：`item_check`/`party_drop` 出现即恒 `"1"`（观察形，装载器按
   `=="1"` 判定）；npcfactions 星期位 `{0,1}`（全 0 = 真端本征不轮换）；DD `category_progress_` =
   8 kind 的**大小写不敏感字母类模式**（镜像装载端 lower 归一，实测数据混用 Hunt/Collectitem/Pvp 等）；
5. **行模型 = `xs:all`**（装载器按标签名读取、与顺序无关；实测无行内重复子标签）；
   **容器递归**：quest.xml 11 组 `*_selectable_reward`（1..N `<data>`，每 data 恰 1 件职业物品）、
   DD `progress_info`（1..N 步，`category_progress_` + `value0_progress_` 必填）；
6. **XSD 1.0 表达不了的面由装载器兜底并在头注释注明**：monsterN-无-countN 共现（hunt fail-closed）、
   DD 列号合法性（payload/extra 列集校验）。

校验：真资源 **10/10 通过**（`tools/xsd_validate.java`，JDK SchemaFactory 同门测试路径）。

## 6. 承重面（门禁）

新增 `RetailTableSchemaGateTest`（tablelane）：

1. 十张真资源对同目录 XSD 全文校验——schema 形漂移即红；
2. 无 DOCTYPE 钉子（剥离批不得回退）+ **正文实体引用只允许预定义五实体**（`&hellip;` 型陷阱
   在无 DTD 下是未定义实体炸解析，此钉直接编码该教训）。

## 7. 连带同步

- Java 注释/javadoc ×6：`NativeQuestTableLoader`（类头「byte 级一致」→ 精简形+同名 xsd；解析器注释）、
  `NativeQuestXmlTable`、`RetailQuestXmlTable`、`RetailSimpleHuntTable`、`DataDrivenQuestTable`
  （全部改为「副本已无 DOCTYPE；内部子集能力保留为解析器能力面 + 负例测试依赖；外部 DTD/实体拒绝为纵深防御」）；
- 对齐门测试 ×2（`SimpleTalkRowAlignmentGateTest` / `SimpleCollectItemRowAlignmentGateTest`）：
  删「从文件自带 DOCTYPE 提取实体表」死机制（`ENTITY` 正则 + 提取循环），改预定义五实体表
  ——替换顺序 quot→amp→apos→lt→gt（复刻原 LinkedHashMap 插入序 = 解析器单层展开语义，
  如 `&amp;quot;` 只展开一层）；
- 未动：`DataDrivenNativeContractGateTest` 的 DTD 剔除（纵深防御空操作，无行为引用）、
  `retail-xml-retention.tsv`（quest id 轴，与 DOCTYPE 无关）、`DataDrivenQuestTable` 解析器配置
  （`disallow-doctype-decl=false` 保留 = 能力面）。

## 8. 边界与后续

- 本轮只做两件事（XSD 落地 + DOCTYPE 剥离），**未做重排版**；HtmlPages 式的手工重排版为独立轴；
- `hellip` 字面量为现行为；若后续要改 U+2026 = 语义变更批（须对拍）；
- 原始 dump 属中间产物（规则 `ai-artifacts.md` §6），已蒸馏为 `dom-equivalence.tsv` 后移除；
  before 侧可由 `git show HEAD:<path>` + 探针随时复现。

## 8.1 聚焦验证（用户授权后执行，2026-10-03）

```bash
mvn -q test -Dtest='RetailTableSchemaGateTest,NativeQuestTableLoaderTest,NativeQuestXmlTableTest,RetailSimpleHuntTableTest,DataDrivenQuestTableGateTest,DataDrivenNativeContractGateTest,SimpleTalkRowAlignmentGateTest,SimpleCollectItemRowAlignmentGateTest,CombineTaskRowAlignmentGateTest,UseItemFamilyRowAlignmentGateTest,SimpleHuntRetailContractTest,HtmlPagesRegistryTest' -DfailIfNoTests=false
```

**结果 = 12/12 类全绿（63 例，0 失败）**：RetailTableSchemaGateTest 2/2（新门）、NativeQuestTableLoaderTest 12/12、
NativeQuestXmlTableTest 5/5、RetailSimpleHuntTableTest 3/3、DataDrivenQuestTableGateTest 4/4、
DataDrivenNativeContractGateTest 7/7、SimpleTalkRowAlignmentGateTest 5/5、SimpleCollectItemRowAlignmentGateTest 7/7、
CombineTaskRowAlignmentGateTest 4/4、UseItemFamilyRowAlignmentGateTest 7/7、QuestSimpleHuntRetailContractTest 1/1（补跑）、
HtmlPagesRegistryTest 6/6。运行日志为一次性产物（复现命令即上方；按沉淀口径「门禁运行产物不入库」不提交）。

## 9. 证据索引

| 证据 | 位置 |
|---|---|
| 剥离工具（含 sha256 前后对照输出） | `tools/strip_retail_doctype.py` |
| DOM 等价清单（10 表） | `retail-doctype-xsd/dom-equivalence.tsv` |
| XSD 生成器（口径 = 各表配置注释） | `tools/generate_retail_xsd.py` |
| 真资源校验探针 | `tools/xsd_validate.java` |
| 转储/实体语义探针 | `tools/xml_dom_probe.java` |

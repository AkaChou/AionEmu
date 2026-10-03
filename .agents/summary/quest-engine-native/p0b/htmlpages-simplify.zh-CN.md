# HtmlPages.xml 精简批（2026-10-03，用户指令「简化这个文件，不改变功能」；零行为变更）

## 1. 消费面审计（先行——决定简化安全边界）

| 面 | 事实 |
|---|---|
| 本仓运行时 | `HtmlPagesRegistry`（tablelane）：启动期 `QuestEngine:2418 ensureLoaded()` 装载 5904 行字典；`SimpleHuntHandler`/`SimpleCollectItemHandler` 构造注入的 `pagesRegistry` 字段**只赋值从未读取**；`find/require/idByHtmlPagename` 全 main **零调用**——注册表 = fail-closed 词汇基座（查询面留给后续对话批次） |
| 真端 | NPCServer `NPCSvr64.c:547866` / MainServer `Server64.c:2355117` 各装载一份双向字典（id↔符号名）；运行期只解析名字→页号（甚至多内联常量），对话页正文在客户端 |
| 结论 | **行集一行不能动**（字典 = fail-closed 判定域，裁剪即语义变更）；**编码/排版域可自由简化**（DOM 解析不关心字节形态） |

## 2. 执行：字符域转码（零语义变更）

- UTF-16LE(BOM) → **UTF-8**（内容纯 ASCII，字节精确可预期）；声明 `encoding="UTF-16"` → `encoding="utf-8"`；
- 元素间空白塌缩：`\r\n\t*` → `\n`（含 DOCTYPE 内部声明间空白——DTD 实体子集逐字保留，展开语义不变；
  塌缩只命中 `>…<` 之间的空白，永不触及元素内文本）；
- **1,591,444 → 718,568 B（−54.8%）**；新 sha256 `dd7a29f7…`（源 `91ea9a0f…` 不变，溯源源列原样保留）。

## 3. 等价性验证

1. **字符级**：变换后文本 ≡ 「旧文本按同一规则变换的期望」逐字符全等（断言通过）——任何 XML 解析器
   对两份输入产出相同 DOM；
2. **行级**：5904 行 `(id, name, htmlpagename)` 三元组新旧逐条全等（断言通过）；
3. **门禁**：`HtmlPagesRegistryTest` 5/5（真资源解析 + 重复 id/页名/未知页负例）、
   `TableSourceProvenanceGateTest` 2/2、消费门 `NativeQuestRewardClaimGateTest` 12/12 +
   `Quest14053LevelUpDialogTest` 2/2。零 Java 行为变更，不触发聚焦套件全量。

## 4. 连带同步

- `table-source-provenance.tsv` 首行：`BYTE_IDENTICAL` → `CONVERTED_UTF8_WHITESPACE_NORMALIZED
  (simplify batch char-semantic-equal…)`，encoding `UTF-16LE(BOM)` → `UTF-8`，repo_sha256 更新；
- `TableSourceProvenanceGateTest.p0bByteIdenticalTablesArePresent`：HtmlPages.xml 移出 byte 级清单
  （余 3 张 = challenge_task / quest_random_rewards / npcfactions_quest），**并钉死该行必须保持
  CONVERTED 形**——回退 BYTE_IDENTICAL 即与盘上 UTF-8 副本矛盾，门会红；
- `HtmlPagesRegistry` javadoc：数据源描述同步为简化形。

## 5. 二段：用户精简 + HtmlPages.xsd（2026-10-03 同日，零行为变更）

**用户手改**（未提交工作区）：移除 DOCTYPE/内部实体子集（"文档"）+ 重排版（2 空格缩进、LF），
新 sha256 `43ec7f7e…`（806,991 B）。语义复验：**5904 行 (id,name,htmlpagename) 三元组与上一形
逐条全等**；DOCTYPE 移除安全性 = 文件内**零实体引用残留**（无未定义实体炸解析风险）。

**新增 `HtmlPages.xsd`**（同目录，镜像注册表 fail-closed 规则）：
- `id` = 非负 int + `xs:unique`（5904 唯一）；`name` = 必填非空；`htmlpagename` = 可选、
  出现即非空 + `xs:unique`（5869 唯一；缺席行不入判定 = 字段缺席空节点集的规范行为）；
- 承重面 = `HtmlPagesRegistryTest.realResourceValidatesAgainstTheHtmlPagesXsd`（真资源全文
  schema 校验，schema 形漂移即红）——运行时解析路径不变（零行为变更）；
- 连带同步：注册表 javadoc 与解析器注释（DOCTYPE 已移除，外部 DTD 拒绝转为纵深防御表述）、
  溯源行 repo_sha256 更新 + transformation 注明 DOCTYPE 移除/重排版/xsd；
- 门禁 = HtmlPagesRegistryTest 6/6 + TableSourceProvenanceGateTest 2/2。
- 提交 `31bfa636e`（一段）→ 本二段提交；两段均为 commit 级 pathspec（零混入）。

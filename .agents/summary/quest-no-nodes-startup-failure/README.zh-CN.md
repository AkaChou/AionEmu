# 15548/25548 目录 EXECUTABLE 零节点 → 启动 NO_NODES 崩溃（2026-09-26）

## 现象

用户实机启动日志（2026-09-26 01:11:57）：

```
com.aionemu.gameserver.GameServerError: Can't initialize typed quest engine.
Caused by: com.aionemu.gameserver.questEngine.definition.QuestCompilationException:
    NO_NODES: executable definition has no nodes
	at QuestDefinitionCompiler.compile(QuestDefinitionCompiler.java:34)
	at QuestDefinitionXmlCompiler.compile(QuestDefinitionXmlCompiler.java:68/64)
	at QuestDefinitionCatalogManifest.lambda$compileEntries$0(...:250)
```

错误信息不含任务 id（可诊断性缺口），定位靠本目录 `scan_no_nodes.py` 重放编译判定。

## 根因

工作区未提交改动把 `quest_definition_catalog.xml` 里 15548/25548 两个条目从
`METADATA_ONLY` 翻成 `EXECUTABLE`（并行 DataDriven 车道在飞标记，报告登记"20h 未动、
阻塞全部 catalog 级测试"），但两个 XML（各 14 行）只有 `<metadata>`，没有 `<nodes>`。
`QuestDefinitionCompiler.compile` 对节点为空的可执行定义直接 `fail(NO_NODES)`；
`RetailQuestDriver.overlayProduction` 在 manifest 编译**之后**运行，救不了这一步。
`target/classes` 已带翻 转 后 的 catalog，实机启动即命中。

## 为什么 EXECUTABLE 对这两行本来就非法

生产 retention 清单（权威）早已裁定：

```
src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv
15548	XML_RETENTION	DataDriven	SEMANTIC_GAP:RETAIL_ACQUIRE_GRANT_UNSUPPORTED	retail-data-driven-drift.tsv
25548	XML_RETENTION	DataDriven	SEMANTIC_GAP:RETAIL_ACQUIRE_GRANT_UNSUPPORTED	retail-data-driven-drift.tsv
```

即真端表无法表达这两行的接取发放路径，DataDriven 采纳被 **REJECTED**，登记保留
XML。挂 `EXECUTABLE` 既违反 retention 分类，也没有节点定义支撑。

## 修复（最小、可逆）

1. `quest_definition_catalog.xml` 553/917 两行 `mode="EXECUTABLE"` → `mode="METADATA_ONLY"`
   （回退到 HEAD 一致态；该态经实机验证可启动）。
2. 同步单文件到 `target/classes/aion/data/static_data/quest_definition/`，
   防止陈旧 classpath 在下次构建前继续崩。

## 复现 / 门禁循环

```bash
python3 .agents/summary/quest-no-nodes-startup-failure/scan_no_nodes.py [quest_definition_dir]
```

遍历 catalog 全部 `mode="EXECUTABLE"` 条目，断言其 XML `nodes/node` ≥ 1（与 Java
`parseNodes` 同口径：`<nodes>` 缺失或空都算 0）。退出码 1 = 红 offenders 清单，
0 = 绿。修复前 RED（15548、25548 两条），修复后 GREEN。

## 未验证 / 待授权

按 AGENTS.md 规则 1 未跑 Maven。被本崩溃阻塞、修复后应复绿的 catalog 级门禁
（报告中点名的）：

```bash
mvn test -Dtest='ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest'
```

若 DataDriven 通道日后补齐 acquire-grant 语义，15548/25548 应回"恢复→采纳→退役"
闭环（删 XML + 删条目），而不是长期挂 EXECUTABLE（p52b 先例）。

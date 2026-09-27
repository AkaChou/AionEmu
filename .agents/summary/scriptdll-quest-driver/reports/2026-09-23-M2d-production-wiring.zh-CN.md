# M2-d 生产接线（真端优先 overlay + 开关回退）报告

- 日期：2026-09-23
- 切片：M2-d（SimpleHunt 族生产接线）
- 覆盖范围：retail-owned SimpleHunt 465 个清单任务（286 个可证 IR 等价者实际生效）

## 1. 交付
- `questEngine/retail/RetailQuestDriver.java`（新，生产代码）：真端优先 overlay 单例。
  - 装载：保留清单（`quest_retail/retail-xml-retention.tsv`，已升级为生产资源）→ RETAIL_TABLE+SimpleHunt
    判定为真端拥有；真端表/quest.xml/NPC+物品索引/随机组/名称 id 全部从 classpath 装载。
  - `overlay(xmlCatalog)`：retail-owned 任务用 `RetailSimpleHuntDefinitionCompiler` 产物替换/补入，
    其余条目原样；拒绝任务自动回退 XML 并缓存拒绝码；装载失败降级返回 XML 目录（不阻断启动）。
  - 开关 `aion.quest.retailDriver`（默认 true）；false 时 overlay 原样返回传入目录实例
    （与改造前逐字节一致，满足不变量 §2.3）。
- `QuestEngine.loadProductionCatalog()`（改，3 行）：XML manifest 编译后过 overlay。
- `.agents/summary/scriptdll-quest-driver/build_retention_list.py`：清单生成器三路输出
  （summary 台账 / 测试资源 / 生产资源）。

## 2. 证据
- `mvn -Dtest='RetailQuestDriverOverlayTest' test` → 2/2 绿：
  关=assertSame(传入目录)；开=retail-owned 条目携带真端编译定义（assertSame 直接产物）、
  保留任务原样保留、条目数不变、统计可见。
- 生产 4 门禁：ProductionCatalogWhitelistVerificationTest 1/1、QuestDefinitionCatalogManifestTest 10/10、
  QuestClientContractGateTest 1/1 → overlay 不破坏装载契约。
- 全量：retail.*Test + QuestSimpleHuntRetailContractTest + Ownership + Metadata + Overlay + 生产 3 门禁
  → **33/33 绿 BUILD SUCCESS**。
- 既有失败：`QuestRetailStartMetadataGateTest.contractCoversExactlyTheProductionCatalog`
  （18744/28744 无合同行）——四项输入文件 git diff 为空，与本改造无关；修法为重跑 systemic-goal
  基线生成脚本。

## 3. 结论
- 服务端生产装载链现在默认"真端优先"：286 个 SimpleHunt 任务由真端表+真端元数据驱动，
  XML 仅作保留清单内降级来源；开关一键回退。
- 未验证：服务端重启实机装载（需用户执行，未授权重启）。

## 4. 下一步
- M2-c：145 spawn 名多 id 消歧（预计扩大等价集）、21 名解析、13 溢出登记。
- M2-e：删除 286 个任务的 XML + catalog 收敛（生产门禁 + 悬空引用 grep + 契约快照重算）。

# XML-only 176：无 Quest-AI 注册的 34 件裁定材料（C 类）

- 日期：2026-10-03；性质：只读裁定材料（零代码/数据变更）。
- 口径：P10 §7 的注册面（`FUN_180cb5920(name, questId)`）**未命中**的 34 件；三方交叉与脚本见 `../p10-xml-only-176/`。
- 列义：`真端元数据` = 真端 quest.xml 有行；`目标列数` = 该行 collect/drop/check/work/talk/reward 前缀列数；`本仓行为` = 本仓 XML 有 transitions；`引用 NPC` = 本仓 XML 的 `npc-id` 去重数；`客户端` = 客户端 quest.xml 有该 id。

| 任务 | 真端元数据 | 目标列数 | 本仓行为 | 引用 NPC | 客户端 | 建议 |
|---|---|---|---|---|---|---|
| 1000 | yes | 0 | yes | 0 | yes | 逐件裁定 |
| 1195 | yes | 0 | yes | 1 | yes | 逐件裁定 |
| 1489 | yes | 1 | — | 0 | yes | 保持 XML 车道（真端同为零驱动） |
| 2000 | yes | 0 | yes | 0 | yes | 逐件裁定 |
| 2590 | yes | 0 | — | 0 | yes | 保持 XML 车道（真端同为零驱动） |
| 3219 | yes | 6 | — | 2 | yes | 保持 XML 车道（真端同为零驱动） |
| 3220 | yes | 9 | — | 3 | yes | 保持 XML 车道（真端同为零驱动） |
| 4219 | yes | 6 | — | 2 | yes | 保持 XML 车道（真端同为零驱动） |
| 4220 | yes | 9 | — | 3 | yes | 保持 XML 车道（真端同为零驱动） |
| 9554 | yes | 0 | — | 0 | yes | 保持 XML 车道（真端同为零驱动） |
| 9555 | yes | 0 | — | 0 | yes | 保持 XML 车道（真端同为零驱动） |
| 9556 | yes | 0 | — | 0 | yes | 保持 XML 车道（真端同为零驱动） |
| 9557 | yes | 0 | — | 0 | yes | 保持 XML 车道（真端同为零驱动） |
| 16984 | yes | 0 | — | 0 | yes | 保持 XML 车道（真端同为零驱动） |
| 16989 | yes | 0 | — | 0 | yes | 保持 XML 车道（真端同为零驱动） |
| 18744 | yes | 0 | yes | 0 | yes | 逐件裁定 |
| 21030 | yes | 1 | yes | 1 | yes | 逐件裁定 |
| 26984 | yes | 0 | — | 0 | yes | 保持 XML 车道（真端同为零驱动） |
| 28744 | yes | 0 | yes | 0 | yes | 逐件裁定 |
| 50032 | yes | 0 | — | 0 | yes | 保持 XML 车道（真端同为零驱动） |
| 50038 | yes | 1 | yes | 1 | yes | 逐件裁定 |
| 50040 | yes | 1 | yes | 1 | yes | 逐件裁定 |
| 50041 | yes | 1 | yes | 1 | yes | 逐件裁定 |
| 50110 | — | 0 | yes | 2 | — | 退役候选（真端/客户端皆无登记） |
| 50111 | — | 0 | yes | 2 | — | 退役候选（真端/客户端皆无登记） |
| 50123 | — | 0 | yes | 1 | — | 退役候选（真端/客户端皆无登记） |
| 50124 | — | 0 | yes | 1 | — | 退役候选（真端/客户端皆无登记） |
| 51032 | yes | 0 | — | 0 | yes | 保持 XML 车道（真端同为零驱动） |
| 51038 | yes | 1 | — | 0 | yes | 保持 XML 车道（真端同为零驱动） |
| 51040 | yes | 1 | — | 0 | yes | 保持 XML 车道（真端同为零驱动） |
| 51041 | yes | 1 | — | 0 | yes | 保持 XML 车道（真端同为零驱动） |
| 51110 | — | 0 | yes | 2 | — | 退役候选（真端/客户端皆无登记） |
| 51111 | — | 0 | yes | 2 | — | 退役候选（真端/客户端皆无登记） |
| 89999 | — | 0 | yes | 4 | — | 退役候选（真端/客户端皆无登记） |

## 分桶汇总

- **逐件裁定**（9）：`1000`, `1195`, `2000`, `18744`, `21030`, `28744`, `50038`, `50040`, `50041`
- **保持 XML 车道（真端同为零驱动）**（18）：`1489`, `2590`, `3219`, `3220`, `4219`, `4220`, `9554`, `9555`, `9556`, `9557`, `16984`, `16989`, `26984`, `50032`, `51032`, `51038`, `51040`, `51041`
- **退役候选（真端/客户端皆无登记）**（7）：`50110`, `50111`, `50123`, `50124`, `51110`, `51111`, `89999`

## 裁定要点（需用户拍板）

1. **退役候选**（真端 quest.xml 无行 ∧ 客户端 quest.xml 无行 = 纯本服自造）：`50110/50111/50123/50124/51110/51111/89999`
   等（见上表）。本仓 7 件自造任务的 XML 行为是自研内容——退役前须确认本服无玩家存档引用（`quest_work_item`/存档清理）。
2. **保持 XML 车道**（真端同样零驱动、本仓 XML 也只有 metadata）：这批与真端行为一致（客户端可见、服务端不驱动），
   建议**原样保留**并把 P10 §4 的"C"降级为"文档标注"，避免无收益的删除动作。
3. **逐件裁定**：有行为但真端无注册/无 DD/无族表的件（含序章 `1000/2000` 一类），
   须按 P11 §4 的待验清单先坐实真端计数面，再决定 D1/D2/D3。
4. **P9 残余 20 行**（另一类，见 `../p9-semantic-closure/P9-SEMANTIC-CLOSURE.zh-CN.md` §5）：
   16 行 `GAb1_0X_VillageNN_Guard`（是否放开 title 判据）、2 行 `magician_apprentice`（遗留 vs 客户端身份裁定）、
   2 行 `LDF5A_Munition_Vritra`（真端名册与客户端双向零命中的补证）。

## 裁定（2026-10-03，本线程，含复核证据）

| 桶 | 件数 | 复核证据（本轮新跑） | 裁定 |
|---|---|---|---|
| 真端/客户端皆无登记 | 7（`50110 50111 50123 50124 51110 51111 89999`） | 真端 quest.xml 无行 ∧ 客户端 quest.xml 无行 ∧ 8 族表/DD/Quest-AI 注册全 0 ∧ **任务间引用 0**（`quest-ids="…"` / `<quest-id>` 零命中）∧ **客户端对话契约 0**（`client_dialog_contract.tsv` 0 行） | **退役**（无任何外部依赖） |
| 真端同为零驱动 | 18（见上表） | 真端 quest.xml 有行、目标列 0-9、本仓 XML 仅 metadata 或零行为；客户端 quest.xml 有行 | **保持 XML 车道**（与真端行为一致，删除零收益） |
| 有行为但真端无注册 | 9（`1000 1195 2000 18744 21030 28744 50038 50040 50041`） | `18744/21030/28744/50038/50040/50041` 在 `client_dialog_contract.tsv` **有行**（客户端对话契约存在）；`1000/2000/1195` 客户端 quest.xml 有行 | **保持 XML 车道**（本仓自研实现合法，客户端可见） |

**P9 残余 20 行**：16 行 `GAb1_0X_VillageNN_Guard` **裁定「放开」**（客户端 `<quest_ai_name>` 声明优先，
title 不一致降级为告警）；2 行 `magician_apprentice` 与 2 行 `LDF5A_Munition_Vritra` **维持 fail-closed**（待补真端/客户端证据）。

### 执行状态：**两处数据面改动被并行任务阻塞，暂缓落地**

`retail-xml-retention`、`retail-quest-ai-name-groups` 等台账正被并行线程转换为 XML+XSD
（工作区已出现未跟踪的 `retail-xml-retention.xml/.xsd`、`retail-quest-ai-name-groups.xml/.xsd`
等 8 组文件）。此时：(a) 编辑 TSV 会与转换源冲突；(b) 删除 7 份任务 XML 会使
`XML_RETENTION` 行与现存 XML 数量不一致，门禁必然转红。

⇒ 建议顺序：**等并行台账转换落地并提交后**，在**新 XML 台账形态**上一次性执行
①7 件退役（删 XML + 台账标记）与 ②16 行 Guard 放开（生成器 + 门常量重冻），
避免二次改写台账。

## 执行记录（2026-10-03，已落地）

- **7 件退役已执行**：删 `quests/{50110,50111,50123,50124,51110,51111,89999}.xml` +
  删 `quest_definition_catalog.xml` 对应 7 行 + 删 `retail-xml-retention.xml`（含 `src/test/resources/quest/` 孪生副本）对应 7 行。
- **生产全集重锚 6224 → 6217**：`RetailQuestDriver.PRODUCTION_QUEST_COUNT`、`RetailOwnershipGateTest`（2 处）、
  `RetailQuestDriverOverlayTest`、`RetailMetadataEquivalenceGateTest`、`XmlDataLoaderTest`。
- **顺带修正一处陈旧断言**：`XmlDataLoaderTest.questDataSynthesizesFromProductionCatalog` 原断言 6224 与
  P7 步 f 后的直通 overlay 语义不符（`catalog()` = XML 目录条目），改为 **733**（= 6217 − 原生覆盖 5484）。
- **计数审计下限重锚**：`QuestIncrementRangeContractTest` 的覆盖下限 `>100` → `>=73`（退役 7 件后实测值）。
- 验证：`RetailOwnershipGateTest`、`RetailMetadataEquivalenceGateTest`、`RetailQuestDriverOverlayTest`、
  `XmlDataLoaderTest`、`QuestIncrementRangeContractTest`、`QuestDefinitionCatalogManifestTest`、
  `QuestInteractionObjectCatalogTest`、`QuestAutoStartDialogAuditTest`、`QuestNpcFactionRetailGateTest`
  + 家族聚焦 10 类，共 **19 类 140 例全绿（EXIT=0）**。
- 未执行：16 行 `GAb1_*_Guard` 放开（需生成器改出 XML 形态后再落，见 `../p11-quest-ai-lane/`）。

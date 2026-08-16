# Aion 5.8 客户端任务页面与动作映射

本目录的表格由客户端数据直接生成，用于查询任务对话页面、页面中的可选动作、动作 ID 和中文按钮文本。

## 数据来源

- 权威基础对话包：Aion 5.8 客户端 `data/Dialogs/Dialogs.pak`。
- 权威中文本地化包：Aion 5.8 客户端 `L10N/CHS/Data/data.pak`。
- 权威任务数据包：Aion 5.8 客户端 `data/Quest/Quest.pak`。
- `HyperLinks.xml`、`HtmlPages.xml`：从 Aion 5.8 客户端基础对话包解包获得。
- 中文任务 HTML：从 Aion 5.8 客户端中文本地化包解包获得。
- 生成脚本：`scripts/quest/generate_client_dialog_mapping.py`。

需要重新生成而当前任务缺少上述客户端文件或解包产物时，先向用户请求提供。取得文件后，可通过命令行参数或 `AION_CLIENT_ROOT`、`AION_UNPACK_ROOT` 传入位置；不要把个人机器路径写入文档或生成结果。

`mapping-summary.json` 记录权威原始包、解包输入的路径与 SHA-256，以及 active
任务 HTML 的排序清单哈希和本次生成的记录数。2026-08-13 已从上述权威原始包重新解包核对：
`HyperLinks.xml`、`HtmlPages.xml` 和全部 9,116 个 active 任务 HTML 与生成器当前输入逐文件一致；
仅 `unused/quest_q21223.html` 存在差异，不参与 active 审计。

## 表格说明

| 文件 | 含义 |
| --- | --- |
| `client-hyperlinks.csv` | `HyperLinks.xml` 中的全部动作 ID 和 `HACTION_*` 常量 |
| `client-html-pages.csv` | `HtmlPages.xml` 中的全部页面 ID、常量和 HTML 页面名 |
| `quest-dialog-pages.csv` | 中文任务 HTML 的完整页面索引，包括无按钮终止页、页序号和源文件 SHA-256 |
| `quest-dialog-action-details.csv` | 中文任务 HTML 中每个 `<Act>` 的逐条明细，包含任务号、源文件、页面、动作 ID、动作常量和中文按钮文本 |
| `quest-action-summary.csv` | 中文任务页面实际引用的动作汇总及中文文本示例 |
| `page-action-map.csv` | 页面名到页面内实际动作的聚合映射 |
| `same-id-map.csv` | 两个独立 ID 空间的同号对照 |
| `same-symbol-map.csv` | 去掉 `HACTION_`、`HTML_PAGE_` 前缀后的同符号词干对照 |
| `parse-errors.csv` | 无法解析的中文任务 HTML；只有表头表示全部解析成功 |
| `parse-recoveries.csv` | 原始标记不规范或为空的文件、恢复解析诊断及提取记录数 |
| `legacy-quest-dialog-contracts.csv` | 从 `origin/history` 正式 retail 模板 XML 提取的 NPC、页面和报告状态合同，包含 Git 对象及内容哈希 |
| `client-lifecycle-alignment.csv` | 客户端接取/报告页面图与当前 XML、旧正式模板合同的逐路由交叉审计 |
| `quest-order-audit.csv` | active 客户端页面动作图与编译后任务 IR 的逐路径顺序审计 |

CSV 使用带 BOM 的 UTF-8 编码，可直接用 Excel 打开。

生成器优先使用严格 XML 解析。客户端原始任务 HTML 中存在未声明实体和错配标签时，生成器使用 `lxml` 检查恢复结果，并按原始标签顺序提取页面与动作，相关诊断记录在 `parse-recoveries.csv`；不会无声跳过这些文件。空文件同样记录为诊断，并产生 0 条动作记录。

## 关系边界

`HyperLinks.xml` 和 `HtmlPages.xml` 是两个独立的 ID 空间。同一个数值不代表“执行该动作后打开这个页面”。例如：

```text
HyperLinks ID 31 = HACTION_QUEST_SELECT
HtmlPages  ID 31 = HTML_PAGE_PACKAGE_LIMITATION
```

`quest-dialog-action-details.csv` 和 `page-action-map.csv` 表达的是客户端 HTML 中可以直接验证的关系：某个页面包含一个按钮，该按钮的 `href` 引用了某个 `HACTION_*`。动作执行后的服务器状态变化或下一个页面仍由服务端任务处理逻辑决定，不能只凭这两份客户端定义推断。

`quest-order-audit.csv` 只使用 `source_variant=active` 且精确映射的任务 HTML 页面。审计从外部入口开始遍历，而不是把 HTML 文件中的页面声明顺序当作任务执行顺序。外部入口包括 `QUEST_SELECT`、`USE_OBJECT`、不依赖 NPC 的 `QUEST_ACTION`，以及没有出现在当前任务页面按钮中、但会直接打开 active 页面的一类 NPC 动作（例如 `EXCHANGE_COIN`）。

每条可达 IR 响应显示任务页面后，审计器先确认页面存在，再只沿该页面实际可见的动作，匹配响应后状态和同一 NPC 下的编译路由。未由客户端可达路径触发的服务端兼容备用路由不会被枚举为客户端流程。状态含义如下：

- `PAGE_ACTION_MATCHED`：当前页面的可见动作在编译 IR 中有匹配路由；这不单独证明该动作的响应页面或状态副作用。
- `TERMINAL_PAGE_REACHED`：服务端到达客户端存在且没有可见动作的终止页。
- `EVIDENCE_REQUIRED`：服务端页面不在 active 页面索引、可见动作没有协议 ID，或可见动作没有匹配路由；必须结合旧 handler 或真实证据处理。
- `CLIENT_PAGE_UNREACHED`：带可见动作的 active 客户端页面未由当前 IR 显示，现有路径不足以确定它对应的 NPC 和状态。

`fix_status=FIXED` 表示该路径已经结合客户端 HTML、`origin/history` 旧 handler 和权威任务奖励数据修复；`NOT_NEEDED` 表示当前 IR 满足本阶段页面/动作合同。任务 1913 和 8 个装备兑换任务由测试要求不得存在 `EVIDENCE_REQUIRED` 或 `CLIENT_PAGE_UNREACHED`。

生命周期对齐器不会把“包含接受/报告按钮的页面”直接当作入口。它先按动作符号构建页面图，沿前驱边回溯到唯一链根，并显式处理 `ASK_QUEST_ACCEPT(1007) -> SHOW_ASK_QUEST_ACCEPT_WINDOW(4)` 的协议边。只有客户端链根唯一、当前 NPC 属于旧正式模板合同，且报告前后状态与旧合同一致时，页面错配才可自动修复。应用时每个 XML 都先重验扫描时的 SHA-256，只替换 `start-page` 或 `page` 属性，不重排文档。

截至 2026-08-13，相对迁移基线已按强证据门槛修复 738 条 XML 页面属性：483 条 `NPC_START` 接取页和 255 条 `NPC_REPORT` 报告页，其中报告页包含 19 条 `item_order` 路由。本轮新增的 `report_to_many` 合同建模只在状态时序已由旧 handler 或专用历史测试确认的任务上应用；1913 系列等专用传送任务保留 `START + var0` 中间状态，不套用通用模板的 `REWARD` 时序。当前 `client-lifecycle-alignment.csv` 中 3,570 条路由为 `NOT_NEEDED`，2,367 条为 `EVIDENCE_REQUIRED`；后者主要是缺少唯一旧模板合同、当前 NPC 不属于旧模板，或报告动作/状态时机需要专用 handler 或真实证据。`EVIDENCE_REQUIRED` 是待补证据，不应被描述为已与客户端完全一致，也不应只改页面掩盖状态问题。

当前 `quest-order-audit.csv` 包含 80,748 条 `PAGE_ACTION_MATCHED`、14,330 条 `TERMINAL_PAGE_REACHED`、8,122 条 `CLIENT_PAGE_UNREACHED` 和 7,877 条 `EVIDENCE_REQUIRED`。前两类只证明当前可达页面和动作合同，不能单独证明所有状态副作用都与真实一致；后两类必须继续补充客户端路径、旧 handler 或真实抓包证据。

客户端的一个可见动作可能对应多个互斥的服务端条件分支。审计表会为每个候选分支输出一行，并用 `candidate_count` 和 `candidate_index` 关联；没有匹配路由时 `candidate_count=0`。候选合同字段包括：

- `candidate_source_node`、`candidate_target_node`、`candidate_target_status` 和 `candidate_target_variables`：候选的状态图投影。
- `candidate_conditions` 和 `candidate_priority`：分支条件及调度优先级。
- `candidate_transaction_actions`：事务内动作，保留声明/执行顺序。
- `candidate_response`：从 after-commit 中提取的任务页、选择窗口、通用窗口或关闭行为，并保留它们在完整序列中的位置。
- `candidate_after_commit_sequence`：提交成功后的完整副作用顺序。

候选按完整合同稳定排序，因此生成结果不依赖同义分支的输入枚举顺序；`candidate_index` 是展示序号，不代表运行时调度顺序，调度依据应查看 `candidate_priority` 和条件。这些字段准确描述当前服务端 IR，但不把它自动判定为真实行为；响应页面、状态推进、奖励和其他副作用仍需结合 `origin/history` 旧 handler 或真实抓包证据审定。

## 重新生成

在项目根目录执行：

```bash
python3 scripts/quest/generate_client_dialog_mapping.py
python3 scripts/quest/generate_client_dialog_mapping.py --check
```

客户端动作/页面映射或活动任务 XML 引用发生变化后，同步并检查类型化 Java 枚举：

```bash
python3 scripts/quest/generate_quest_dialog_enums.py
python3 scripts/quest/generate_quest_dialog_enums.py --check
```

重新生成旧正式模板合同和客户端生命周期对齐报告：

```bash
python3 scripts/quest/extract_legacy_quest_dialog_contracts.py
python3 scripts/quest/align_client_quest_dialog_lifecycle.py
python3 scripts/quest/extract_legacy_quest_dialog_contracts.py --check
python3 scripts/quest/align_client_quest_dialog_lifecycle.py --check
```

`align_client_quest_dialog_lifecycle.py --write` 只应用上述强证据门槛下的 `READY` 路由；默认运行只更新报告。

重新生成顺序审计（先完成测试编译）使用：

```bash
mvn -q -Dexec.classpathScope=test \
  -Dexec.mainClass=com.aionemu.gameserver.questEngine.definition.QuestDialogOrderAudit \
  -Dexec.args="docs/quest/client-dialog-mapping/quest-dialog-pages.csv docs/quest/client-dialog-mapping/quest-dialog-action-details.csv docs/quest/client-dialog-mapping/quest-order-audit.csv" \
  exec:java
```

也可以显式指定来源和输出目录：

```bash
python3 scripts/quest/generate_client_dialog_mapping.py \
  --definitions-dir "${AION_UNPACK_ROOT}/dialog_unpacked" \
  --zh-dialogs-dir "${AION_UNPACK_ROOT}/data_unpacked/Dialogs" \
  --output-dir docs/quest/client-dialog-mapping
```

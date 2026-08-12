# Aion 5.8 客户端任务页面与动作映射

本目录的表格由客户端数据直接生成，用于查询任务对话页面、页面中的可选动作、动作 ID 和中文按钮文本。

## 数据来源

- `HyperLinks.xml`、`HtmlPages.xml`：从 `/Users/mc/IdeaProjects/5.8客户端/data/Dialogs/Dialogs.pak` 解包到 `/Users/mc/PycharmProjects/unpak/dialog_unpacked`。
- 中文任务 HTML：`/Users/mc/PycharmProjects/unpak/data_unpacked/Dialogs`。
- 生成脚本：`scripts/generate_client_dialog_mapping.py`。

`mapping-summary.json` 记录源文件路径、SHA-256 和本次生成的记录数。

## 表格说明

| 文件 | 含义 |
| --- | --- |
| `client-hyperlinks.csv` | `HyperLinks.xml` 中的全部动作 ID 和 `HACTION_*` 常量 |
| `client-html-pages.csv` | `HtmlPages.xml` 中的全部页面 ID、常量和 HTML 页面名 |
| `quest-dialog-action-details.csv` | 中文任务 HTML 中每个 `<Act>` 的逐条明细，包含任务号、源文件、页面、动作 ID、动作常量和中文按钮文本 |
| `quest-action-summary.csv` | 中文任务页面实际引用的动作汇总及中文文本示例 |
| `page-action-map.csv` | 页面名到页面内实际动作的聚合映射 |
| `same-id-map.csv` | 两个独立 ID 空间的同号对照 |
| `same-symbol-map.csv` | 去掉 `HACTION_`、`HTML_PAGE_` 前缀后的同符号词干对照 |
| `parse-errors.csv` | 无法解析的中文任务 HTML；只有表头表示全部解析成功 |
| `parse-recoveries.csv` | 原始标记不规范或为空的文件、恢复解析诊断及提取记录数 |
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

`quest-order-audit.csv` 只使用 `source_variant=active` 的任务 HTML。每条 IR 响应显示任务页面后，审计器检查该页面的可见动作在响应后的状态和同一 NPC 下是否存在编译路由。状态含义如下：

- `VERIFIED`：当前页面动作在编译 IR 中有匹配路由。
- `UNRESOLVED`：客户端证明动作可见，但客户端不能单独证明响应页面或状态副作用；不得仅凭 `same-symbol-map.csv` 自动修复。
- `UNREACHED`：active 客户端页面未由当前 IR 显示，现有路径不足以确定它对应的 NPC 和状态。

`fix_status=FIXED` 表示该路径已经结合客户端 HTML、`origin/history` 旧 handler 和权威任务奖励数据修复；`NOT_NEEDED` 表示当前 IR 原本就满足客户端页面动作合同。任务 1913 和 8 个装备兑换任务由测试要求所有已进入 IR 的 active 页面路径均为 `VERIFIED`。

## 重新生成

在项目根目录执行：

```bash
rtk python3 scripts/generate_client_dialog_mapping.py
```

重新生成顺序审计（先完成测试编译）使用：

```bash
rtk mvn -q -Dexec.classpathScope=test \
  -Dexec.mainClass=com.aionemu.gameserver.questEngine.definition.QuestDialogOrderAudit \
  -Dexec.args="docs/quest/client-dialog-mapping/quest-dialog-action-details.csv docs/quest/client-dialog-mapping/quest-order-audit.csv" \
  exec:java
```

也可以显式指定来源和输出目录：

```bash
rtk python3 scripts/generate_client_dialog_mapping.py \
  --definitions-dir /Users/mc/PycharmProjects/unpak/dialog_unpacked \
  --zh-dialogs-dir /Users/mc/PycharmProjects/unpak/data_unpacked/Dialogs \
  --output-dir docs/quest/client-dialog-mapping
```

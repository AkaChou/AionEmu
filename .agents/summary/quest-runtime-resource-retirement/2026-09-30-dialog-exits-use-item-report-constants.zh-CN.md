# 客户端契约 TSV 常量化退役纪要（第一片）

## 范围

只处理 `src/main/resources/quest` 中两张已缩到实质小集合的表，不触碰 130 任务审计、NPC 别名和其他并行未提交工作：

- `quest_client_dialog_exits.tsv`
- `quest_client_use_item_report.tsv`

## 变更

1. `RetailClientDialogExits` 不再读取 TSV，直接持有 14 个 `SELECT_NONE_1` 任务 ID。
2. `RetailClientUseItemReport` 不再读取 TSV，直接持有 5 个 CHECK 任务及交付物品 ID；未登记任务继续走 `REWARD` 默认。
3. 删除生产与测试资源中的对应 4 个文件：
   - `src/main/resources/quest/quest_client_dialog_exits.tsv`
   - `src/main/resources/quest/quest_client_use_item_report.tsv`
   - `src/test/resources/quest/quest_client_dialog_exits.tsv`
   - `src/test/resources/quest/quest_client_use_item_report.tsv`
4. `src/main/resources/quest` 从 12 张表缩减到 10 张。
5. 更新 `RetailSimpleCollectItemGateTest` 的过期注释，不再指向已删除的 `SELECT1_1/SELECT6` 登记口径。

## 防回退

已有 `RetailClientContractConstantsGateTest` 覆盖 14 个对话出口和 5 个 CHECK 模式，防止常量视图被误改成空集合后再次复现生产启动缺资源问题。

## 验证状态

- 静态检查：`git diff --check` 无输出；IntelliJ 错误级检查未发现新增错误。
- Maven / 测试：**PENDING**（按仓库规则未获授权，未执行）。
- 建议聚焦门禁：`mvn -Dtest=RetailClientContractConstantsGateTest,RetailQuestDriverOverlayTest,RetailSimpleUseItemGateTest,RetailSimpleCollectItemGateTest test`
- 生产目录门禁：**PENDING**。
- 真实客户端验收：**PENDING**。

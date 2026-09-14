# 任务 3734 客户端验收记录

quest: 3734

user acceptance confirmation: 用户于 2026-08-28 在对话中明确回复“验收完成，提交”，确认范围为完整任务验收，未限定单一分支或步骤。

server launch mode: not captured

repository commit: `b942cecbe` (`fix(quest): open quest 3734 dragon arms chest`)

working tree: dirty；任务 3734 XML 与专项测试包含在上述修复提交中。工作树中其余 1311、14024、3732、SkillTemplate 等文件属于其他进行中工作，不属于本验收记录。

Aion 5.8 client/data provenance: Aion 5.8 客户端；交互物/NPC 与任务页面证据取自仓库内 `npc_template_286321_800030.xml`、`chest_templates.xml`、`quest-dialog-pages.csv` 与 `quest-dialog-action-details.csv`，本次未重新采集客户端包哈希。

npc template/object: 龙族武器保管箱 template ID 700415；运行时 object ID not captured。

map/instance: `300030000_Nochsana_Training_Camp.xml` 中的任务生成数据；运行时 instance ID not captured。

steps:

1. 前置：任务 3704 未完成且未获得；任务 3734 经 NPC 800518 接取后进入 `START var0=0`。
2. 使用龙族武器保管箱 700415；修复前无响应，修复后可完成交互。
3. 完成任务后续交付与奖励流程。

source state/status/vars: 交互门禁 `START var0=0 -> START var0=0`；`npc-item-report` 集齐 4 个 `182202180` 后进 `REWARD var0=1`，完成 `COMPLETE var0=0`。

action/page/button: `ACTION_ITEM_USE(template=700415)`；任务交付按钮为 `CHECK_USER_HAS_QUEST_ITEM(39)`，奖励预览动作为 `USE_OBJECT SELECT_QUEST_REWARD`。

expected response: `QuestItemNpcAI2` 通过 700415 的 `ACTION_ITEM_USE` 资格路由启动使用流程；使用完成后解析任务 owner，注册 3734 掉落并展示可拾取武器。

actual response: 用户确认修复后客户端验收完成；保管箱可开启，任务可正常完成。

startup health: 修复 XML 通过 `xmllint --noout --schema quest_definition.xsd`；任务专用测试与生产 catalog/白名单门禁未运行（本会话未获构建授权，命令已列于交付说明）；用户未报告任务引擎初始化或编译错误。

runtime logs: not captured

protocol trace: not captured

screenshots/recordings and SHA-256: not captured

acceptance status: ACCEPTED_EXISTING_PATTERN

matched Pattern: `STATE_GATED_ACTION_ITEM_USE_FLOW`；匹配字段为交互物仅在 `started` 状态开放 `can-act/ACTION_ITEM_USE`、无 unaccepted 路由、无事务动作和 after-commit 副作用。差异是本任务为 100% 交互掉落，不经过该对象页链或 work item 转移；`QuestItemNpcAI2` 在使用完成后通过同一 CanAct 路由解析 owner 并注册掉落。代表提交 `c02c8722e`；代表测试 `EarlyElyosQuestRegressionTest#stolenVillageSealUsesTheItemStackOnlyAfterAcceptance`。

remaining risks: 任务专用测试与生产 catalog/白名单门禁未在本会话运行；运行时 object ID、启动日志、packet 顺序未捕获；4734 复用 700415，但本次仅验收并修复 3734，未扩展到阿斯莫德任务。

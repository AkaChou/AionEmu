# 任务 14120 客户端验收记录

quest: 14120「The Bucket List / 森林真正的主人」

user acceptance confirmation: 用户原话“验证通过 提交，加入任务书，排查类似问题修复”；2026-09-10；未限定分支或步骤，按规则视为整个任务客户端流程验收完成。

server launch mode: not captured（服务端由用户管理）

repository commit: `dd38d87868be7729f419c934da5229ffc5a4139f`（`fix(quest): repair 14120 and 14150 reward dialogs`）

working tree: dirty；本次提交只包含 14120/14150 任务 XML 与专项测试；数据库、Kromede 副本、对话运行时、1192/18602、SQL、客户端资料提取、JFR 和其他临时测试属于工作区其他改动，均保留。

Aion 5.8 client/data provenance: Aion 5.8 客户端；仓库证据为 `docs/quest/client-dialog-mapping/quest-dialog-pages.csv`、`quest-dialog-action-details.csv`、`legacy-quest-dialog-contracts.csv`；对应 `10000_19999/quest_q14120.html`，source SHA-256 `502f63a2aa2809e6493b00b2186e8066a71df2a7ecb7afbf1fe11104cefa3031`，本次未重新采集客户端包哈希。

npc template/object: 接取 NPC template 203932；中间 NPC template 730020；最终交付/领奖 NPC template 730019；runtime object ID not captured；交互对象来源为任务 XML、旧任务合同与 Aion 5.8 客户端页面/action 映射。

map/instance: 开放世界；world/instance ID not captured；入口与重入上下文 not captured。

steps:

1. 满足 ELYOS、等级 24+、任务 1300 与 1032 均未完成且未接取的条件；击杀 700157 获取任务物品 182215478。
2. 与 203932 对话，沿 `QUEST_SELECT(31) -> SELECT1(1011)` 及接受链接取任务；与 730020 沿 `QUEST_SELECT(31) -> SELECT2(1352) -> SELECT2_1(1353) -> SETPRO1(10000)` 完成交接。
3. 与 730019 对话进入 `SELECT5(2375)`，点击“拿出克鲁比安的干净水罐”提交 182215478，进入奖励窗口并选择奖励完成任务。

source state/status/vars: `unaccepted / NONE / var0=0` -> `started / START / var0=0` -> `v1 / START / var0=1` -> `reward / REWARD / var0=1` -> `complete / COMPLETE / var0=1`。

action/page/button: `203932 + QUEST_SELECT(31) -> SELECT1(1011)`；`730020 + QUEST_SELECT(31) -> SELECT2(1352)`；`SETPRO1(10000)` 进入 `v1`；`730019 + QUEST_SELECT(31) -> SELECT5(2375)`；客户端按钮发送 `CHECK_USER_HAS_QUEST_ITEM_SIMPLE(20002)`，事务内移除 182215478，提交后按 `LEVEL_AND_VISIBILITY_REFRESH -> SHOW_SELECT_QUEST_REWARD_WINDOW1(5)`；奖励选择使用 `SELECT_QUEST_REWARD(1009)` 及对应奖励动作完成任务。

expected response: 730019 的客户端入口页和按钮动作可命中；成功交付同一次交互内移除任务物品、进入 `REWARD`，先同步已提交任务状态再显示奖励窗口 page 5；奖励选择后发放奖励并完成任务。

actual response: 用户确认“验证通过”；按规则记录为完整客户端流程通过。未捕获独立 packet、运行日志或稳定截图附件。

startup health: not captured；本次未启动、停止或重启服务端，未运行 Maven；用户未报告 typed quest engine 初始化失败或 `QuestCompilationException`。

runtime logs: not captured；无稳定时间窗口、角色、NPC object、任务或 world 日志附件。

protocol trace: not captured；无稳定的 objectId、questId、page 或 action 包序列附件。

screenshots/recordings and SHA-256: not captured；对话中的临时缓存图片路径不可作为长期验收附件。

acceptance status: ACCEPTED_EXISTING_PATTERN

matched Pattern: `COLLECT_TURN_IN_DIALOG_CHAIN_MISMATCH`；匹配“收集交付 load fail/页面链未闭合”的形状，14120 的任务专属差异是客户端使用 `CHECK_USER_HAS_QUEST_ITEM_SIMPLE(20002)`，而不是标准 `CHECK_USER_HAS_QUEST_ITEM(39)`。代表案例为 14015，代表提交 `e6f4f12cf`，代表测试 `Quest14015ClientDialogAlignmentTest#collectDialogChainUsesOnlyClientOwnedPages`；本任务不重复新增 Pattern 或案例正文。

similar issue audit: 针对 1465 条旧合同为完整 `item_collecting` 且奖励页为 5 的生产定义进行静态扫描；修复 14120 后仅发现 14150 同样存在显式物品检查成功路由缺少奖励页，已在同一修复提交中补齐 `LEVEL_AND_VISIBILITY_REFRESH -> page 5`，并修正其奖励状态直接 `USE_OBJECT` 的页面合同。其余 `SELECT6`/`DEFAULT_SUCCESS` 直接入口保留，因为旧 XML 与客户端证据将其定义为中间/回落页，不能按 page 5 全局替换；14150 尚未获得本次用户单独客户端验收确认。

remaining risks: 14150 仅完成源码、客户端映射和旧合同对齐，未单独客户端/runtime 验收；本次未运行 Maven focused/catalog/whitelist 门禁；未捕获启动日志、运行日志、协议 trace、稳定截图、runtime object ID、重连/重登/死亡/重复领取路径；服务端重新加载后的启动健康仍由用户环境负责。

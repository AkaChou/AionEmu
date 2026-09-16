# 任务 19637「Onboard for One / 特别任务1」客户端验收记录

quest: 19637「Onboard for One」（中文名「特别任务1」），天族，50 级可接，NPC 798926 起始与报告；覆盖整条任务（接取 -> 10 杀与脏数据自愈 -> 凯西内尔报告 -> 6 选 1 装备奖励 -> 完成），未限定单一分支或步骤。

user acceptance confirmation: 用户原话「客户端验证通过」（2026-09-16）；未限定分支或步骤，按验收规则视为整条任务客户端验收完成。

server launch mode: not captured（服务端由用户管理；本会话未启动、停止或重启）

repository commit: `4b61f761ee7c05ea7647ff8dc780cf51fcea383a`

working tree: dirty；工作区含未提交的 RetailPatternAI2、GameEventServices 并行优化改动及 .zcode/，均与本任务无关，保留未提交。

Aion 5.8 client/data provenance: Aion 5.8 客户端解包证据 `quest_monster.csv`（`19637,Progress(SECTION_0==0; SECTION_1<10)`）、`quest_q19637.html`（sha256 `6dc00fabf0619b3295937805013b1af0a10b75dfa7650510c129ce5103425436`）与 `quest-dialog-pages.csv`；旧端 Handler 证据 `_19637Onboard_For_One.java`（Commit `911440146`）。本次未重新采集客户端包哈希。

npc template/object:
- 接取与报告 NPC：798926「凯西内尔主神代理人 Cainus」，英吉斯温 `210050000`；运行时 objectId 68387。
- 击杀目标：215500, 215501, 215502, 215503（包含 215502 绿地斯科拉姆）。

map/instance: world `210050000`（英吉斯温），非副本；运行实例 ID not captured。

steps:
1. 与 798926 对话：下发 4762（SELECT_NONE）-> 点 20000（QUEST_ACCEPT_SIMPLE）接取任务，状态进入 START，var0=0, var1=0。
2. 击杀目标怪物 10 只：0..8 杀通过 priority 1 路线步进递增 var1 并重置 var0 为 0（支持历史脏数据自动纠偏），第 10 杀通过 priority 0 路线设置 var0=1, var1=10，直接进入 REWARD 状态。
3. 回到 798926 报告：NPC 下发 DEFAULT_SUCCESS(10002) -> 点 SELECT_QUEST_REWARD(1009) 弹出奖励窗口 5。
4. 选择 6 选 1 装备奖励完成：发放固定经验值 6937236 与所选装备，任务置 COMPLETE 并清理工作状态。

source state/status/vars: `unaccepted`(var0=0, var1=0) -> `started`(var0=0, var1=0..9) -> `reward`(var0=1, var1=10) -> `complete`(var0=0, var1=0)。

action/page/button: 接取 4762 -> 20000；报告 10002 -> 1009 -> page 5 -> 8..13。

expected response:
- 接取：NONE -> START，下发 4762，点击 20000 后状态置 START，var0=0, var1=0，关窗并刷新可见性。
- 击杀推进：var1 精确 +1，var0 保持 0，客户端任务面板显示消灭怪物 (var1/10)。
- 第 10 杀：var0 设为 1，var1 设为 10，客户端第一步完成并提示找凯西内尔对话，同时服务端直接进入 REWARD 并触发 LEVEL_AND_VISIBILITY_REFRESH，凯西内尔头上出现黄色问号。
- 报告领奖：NPC 下发 DEFAULT_SUCCESS(10002)，点击 SELECT_QUEST_REWARD(1009) 打开奖励选择窗口 5，选择装备后发放经验与装备，任务置 COMPLETE。

actual response: 用户确认客户端验证通过。

startup health: not captured（服务端由用户管理；单测 `Quest19637ClientDialogAlignmentTest` 与全量生产目录门禁 `ProductionCatalogWhitelistVerificationTest` 通过，6193 任务 0 失败 0 违规）。

runtime logs: not captured

protocol trace:
- 接取日志：
  - `CM_DIALOG_SELECT 玩家=Ww npcId=798926 targetObj=68387 questId=19637 上一页=10 动作=31`
  - `SM_DIALOG_WINDOW 玩家=Ww targetObj=68387 questId=19637 下发页=4762`
  - `CM_DIALOG_SELECT 玩家=Ww npcId=798926 targetObj=68387 questId=19637 上一页=4762 动作=20000`
  - `SM_QUEST_ACTION 任务=19637 状态=3 步数=0`

screenshots/recordings and SHA-256:
- 截图 1789562796682.png：证明排查前故障状态（`Status: START, Vars: 1 0 0 0 0 0`，由于旧 counter-grid 错误绑定 var0 导致 1 杀即触发 step 0 完成）。

acceptance status: ACCEPTED_EXISTING_PATTERN

matched Pattern: `COUNTER_SOURCE_PROJECTION_NO_LOCK`（Pattern 58）与 `MULTI_COUNTER_FINAL_EVENT_ENTERS_REWARD`（Pattern 60 / QE-018）。本次复用了已有的阶段变量与击杀计数字段隔离规范，不引入新的未知 Pattern，因此不修改 `PATTERNS.zh-CN.md`。代表回归：`Quest19637ClientDialogAlignmentTest#restoresClientStepAndKillCounterSeparationAndDialogContract` 与 `Quest19637ClientDialogAlignmentTest#killChainAdvancesVar1UpToTenAndEntersRewardOnTenthKill`。

remaining risks: 仅覆盖天族 19637 特别任务 1；同系列 19638~19642 结构类似，后续如有反馈可按同模式跟进；未抓取最终完成抓包包体。

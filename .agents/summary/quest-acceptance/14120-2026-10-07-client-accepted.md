# 14120 客户端验收记录（2026-10-07）

quest: 14120「The Bucket List / 森林真正的主人」（ELYOS；接取 NPC 203932 / 中继 NPC 730020 / 交付 NPC 730019）
user acceptance confirmation: 用户明确回复「实机验证成功，提交」（2026-10-07）；按规则视为整任务游玩验收，未限定分支或步骤
server launch mode: IDEA（常驻进程；用户冷重启后加载含修复的构建——本修复为结构变更，JRebel 热更不覆盖）
repository commit: `d12e4236e87bc06d13ca2c1fcaa4e825e7c1e3e3`（修复提交 `fix(quest): 14120 中继对话面补齐…`；验收时工作区含该提交与并行未提交改动）
working tree: dirty（含并行任务改动；本修复已随 `d12e4236e` 落库，验收记录与 Playbook 见后续提交）
Aion 5.8 client/data provenance: Aion 5.8 客户端（用户实机）；客户端对话页契约 `src/main/resources/aion/definitions/quest_dialog/client_dialog_contract.tsv`（14120 声明 4/1003/1004/1011/1012/1352/1353/2375）；退役 14120 任务 XML（git 历史，XML 已退役并删除）
npc template/object: 接取 203932（Phomona）、中继 730020（Tree_Move_Demro，`npc_template_286321_800030.xml`）、交付 730019（Tree_NoMove_Lodas）；中继 NPC 的 runtime object ID not captured（诊断期 trace 的 targetObj=47427 为 203932 实例）
map/instance: 普通世界（LF2 / Eltnen 森林区）；无实例

steps:
1. 前置：14120 已接取（接取链 203932：`31 -> 1011(select1) -> 1012(select1_1) -> 1007(ask) -> 4 -> 1002 -> 1003`）；修复前该角色存档中继步为 bit16 私编（`vars=65536`）
2. 冷重启服务端后登录：`ENTER_WORLD` 自愈把 `vars=65536` 归一为 `var0=1`（旧存档修复路径本次一并实机覆盖）
3. 与中继 NPC 730020 对话：任务行（31）→ 页 1352（select2）；翻页动作 1353（select2_1）→ 页 1353；确认 SETPRO1（10000）→ 关窗 + `SM_QUEST_ACTION 状态=3 步数=1`
4. 采集 LF2_Cherubim_Basket（700157）取得 quest_14120a；与 730019 交付（31 → select5=2375；交付检查按钮 20002 → 扣除 182215478 + REWARD + 奖励窗页 5）；选择奖励完成任务

source state/status/vars: `START/var0=0`（修复前私编 `65536`）→ 自愈归一 `START/var0=1`（中继完成）→ 采集 → `REWARD/var0=1` → `COMPLETE`
action/page/button: 730020：`31 -> 1352`、`1353 -> 1353`（原样回发）、`10000 -> 关窗 + 步数=1`；730019：`31 -> 2375(select5)`、`20002(CHECK_USER_HAS_QUEST_ITEM_SIMPLE) -> 扣物 + REWARD + 页 5`、领奖 `8..22/23 -> COMPLETE + 页 10`
expected response: 中继链按「步页 → 子页回发 → SETPRO1 关窗 + 步号 var0」执行；任务书步骤文本随步号恢复；采集/交付/领奖可玩
actual response: 用户实机确认「实机验证成功」（整链；点击中继 NPC 不再跳步、任务书步骤不再空白；修复前 trace 的 `步数=65536` 不再出现）

startup health: 用户冷重启加载修复构建后正常游玩；启动日志未采集（`not captured`）；未报告 typed quest engine 初始化异常
runtime logs: 诊断期 trace（修复前：`SM_QUEST_ACTION 任务=14120 状态=3 步数=65536`，见 `.agents/summary/quest-14120-relay-dialog/README.zh-CN.md`）；验收时刻日志 `not captured`
protocol trace: 验收时 `not captured`；修复前诊断 trace 见主题目录
screenshots/recordings and SHA-256: `not captured`

acceptance status: ACCEPTED_NEW_PATTERN
matched Pattern: `NATIVE_RELAY_STEP_VAR0_AXIS`（新）；representative commit `d12e4236e`；`SimpleCollectItemNativeFamilyGateTest#relayChainServesStepPageAndAdvancesOnSetpro`
remaining risks: `SimpleUseItem` 族 90 个 talk_npc 行为同型私编（bit16..17 + 任意动作推进），未修、未实机；14150（同形 1 步中继）未单独实机复测；9620 三步中继链仅单测覆盖（乱序/重看）；死亡/重登/放弃重接路径未逐条实机

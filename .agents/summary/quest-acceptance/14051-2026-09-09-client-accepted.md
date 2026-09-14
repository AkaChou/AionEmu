# 任务 14051 客户端验收记录

quest: 14051「Root of the Problem / 采集根标本」

user acceptance confirmation: 用户原话“客户端验证完成 添加任务书，排查类似问题”；2026-09-09；结合当前任务上下文，未限定分支或步骤，按规则视为整个任务客户端流程验收完成。

server launch mode: not captured（服务端由用户管理）

repository commit: `b87c2ab0c`（`fix(quest): repair 14051 collection turn-in flow`）；相似问题排查修复提交 `b526ebf`（`fix(quest): consume legacy collection items on turn in`）

working tree: dirty；14051 客户端验证对应的 XML 与专项测试已包含在 `b87c2ab0c`；27540、28511 的相似问题修复和批量回归断言已包含在 `b526ebf`，尚未进行客户端验证；18600、18602 及其他临时文件属于工作区其他任务，均保留。

Aion 5.8 client/data provenance: Aion 5.8 客户端；仓库证据为 `../../../docs/quest/client-dialog-mapping/quest-dialog-pages.csv`、`quest-dialog-action-details.csv` 和 `quest-order-audit.csv`，对应 `10000_19999/quest_q14051.html`，source SHA-256 `43b734e84896cb268ef836daeec137033bf5fa6469c3201e6dca0b6dfa18510c`；本次未重新采集客户端包哈希。

npc template/object: 初始 NPC template 204500；收集交付 NPC template 204549；后续 NPC template 730026；最终领奖 NPC template 730024；runtime object ID not captured；交互对象来源为任务 XML、旧 handler 与 Aion 5.8 客户端页面/action 合同。

map/instance: world/instance ID not captured；入口与重入上下文 not captured。

steps:

1. 满足任务 14050 已完成、1063 未完成且未接取，以及 ELYOS、等级 36 等接取条件后，任务进入 `START var0=0`。
2. 与 NPC 204500 对话，`QUEST_SELECT(31) -> SELECT1(1011)`；点击结束推进 `SETPRO1(10000)`，进入 `s1 var0=1`。
3. 与 NPC 204549 对话，依次经过 `SELECT2(1352)`、`SELECT2_1(1353)`、`SELECT2_1_1(1354)`，点击结束发送 `SETPRO2(10001)`，进入 `s2 var0=2`。
4. 收集 182215337、182215338 各 3 个；在 NPC 204549 的 `SELECT3(1693)` 点击“拿出标本”，发送 `CHECK_USER_HAS_QUEST_ITEM(39)`，成功后扣除两类根样本、给予 182215339，并显示 `CHECK_USER_ITEM_OK(10000)`。
5. 前往 NPC 730026，`QUEST_SELECT(31) -> SELECT4(2034)`；点击“拿出标本”进入 `SELECT4_1(2035)`，结束对话发送 `SETPRO4(10003)`，消耗 182215339 后进入领奖状态。
6. 与 NPC 730024 打开奖励页并选择奖励完成任务；登出/登录、重连、重启、死亡、重复领取：not captured。

source state/status/vars: `unaccepted / NONE / var0=0`；自动接取后 `started / START / var0=0`；204500 对话后 `s1 / START / var0=1`；204549 介绍链完成后 `s2 / START / var0=2`；交付成功后 `s3 / START / var0=3`；730026 交接后 `reward / REWARD / var0=3`；最终领奖后 `complete / COMPLETE / var0=3`。

action/page/button: `QUEST_SELECT(31) -> SELECT1(1011)`；`SETPRO1(10000) -> s1`；`QUEST_SELECT(31) -> SELECT2(1352)`；`SELECT2_1(1353) -> SELECT2_1`；`SELECT2_1_1(1354) -> SELECT2_1_1`；`SETPRO2(10001) -> s2`；`QUEST_SELECT(31) -> SELECT3(1693)`；`CHECK_USER_HAS_QUEST_ITEM(39) -> s3 + remove 182215337/182215338 + give 182215339 + CHECK_USER_ITEM_OK(10000)`；`QUEST_SELECT(31) -> SELECT4(2034)`；`SELECT4_1(2035) -> SELECT4_1`；`SETPRO4(10003) -> reward`；`SELECT_QUEST_REWARD(1009) -> page 5`。

expected response: 204549 的客户端介绍链必须先完整翻页，再由 `var0=2` 的 action 39 进入交付分支；成功事务内扣除两类根样本并给予合成标本，提交后按 `PACKET_ONLY sync -> CHECK_USER_ITEM_OK` 响应；后续 730026 接管 `var0=3` 对话并最终进入奖励页。

actual response: 用户确认“客户端验证完成”；按该确认记录为完整客户端流程已通过。未捕获独立 packet、运行日志或稳定截图附件。

startup health: not captured；未启动或重启服务端，用户未报告 typed quest engine 初始化失败或 `QuestCompilationException`。

runtime logs: not captured；无稳定时间窗口、角色、NPC object、任务、世界或实例日志附件。

protocol trace: not captured；无稳定的 objectId、questId、page 或 action 包序列附件。

screenshots/recordings and SHA-256: not captured；用户未提供需要长期保存的截图或录屏附件。

acceptance status: ACCEPTED_EXISTING_PATTERN

matched Pattern: `COLLECT_TURN_IN_DIALOG_CHAIN_MISMATCH` 与 `COLLECT_ITEM_TURNIN_REMOVAL_MISSING`；前者匹配客户端 1352 -> 1353 -> 1354、1693 -> 39、2034 -> 2035 的页面/action 链与 XML source/NPC owner 修正，代表提交 `e6f4f12cf`、代表测试 `Quest14015ClientDialogAlignmentTest#collectDialogChainUsesOnlyClientOwnedPages`；后者匹配 action 39 成功分支对普通收集物显式 `remove-item`，代表提交 `bd782024d`、代表测试 `Quest14023ClientDialogAlignmentTest#verifiesFullDialogAndItemTurnInContract`。14051 不新增重复 Playbook/Pattern 条目。

similar issue audit: 当前 6238 个生产任务 XML 中，静态扫描发现 26 个 action 39 成功分支没有在同一 transition 扣除全部检查物；结合 XML 后续路径与 legacy 合同，24 个属于后续阶段再扣除的有意流程。27540 的 182216160 与 28511 的 182212022 在当前 XML 中全局没有任何扣除，且 legacy `collect_items` 与 `collectItemCheck(..., true)` 明确要求交付检查成功时扣除；已在 `b526ebf` 补充 XML 和批量回归断言，尚未获得客户端验收，因此不计入本次 ACCEPTED 状态。其余 client page/action `EVIDENCE_REQUIRED` 告警主要缺少唯一 owner 或属于领域积木/兼容路由，未凭静态结果批量修改。

remaining risks: 未捕获运行日志、协议 trace、稳定截图、运行时 object ID、重连/重登/死亡/重复领取路径；`b526ebf` 中 27540、28511 的修复仍需专项编译/catalog/whitelist 门禁和客户端流程验证；客户端顺序审计 CSV 仍保留 14051 修复前快照，需在获准构建后重新生成以清除旧的 `EVIDENCE_REQUIRED` 记录。

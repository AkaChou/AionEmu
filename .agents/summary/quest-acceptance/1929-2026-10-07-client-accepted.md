# 1929 客户端验收记录（2026-10-07）

quest: 1929「A Sliver Of Darkness / 暗黑碎片」（Elyos 烙印之石教学链；对话 NPC 205111 Ecus；起始 NPC 203164）
user acceptance confirmation: 用户明确回复「实机验证成功，提交」（2026-10-07）；整任务验收，未限定分支或步骤
server launch mode: IDEA（常驻进程；用户重启后加载含修复的构建）
repository commit: `b632ee3b58957ed2072fc0d6abeb0995ecda7bcd`（验收时工作区含该修复的未提交版本；repair 已按本记录落库）
working tree: dirty during acceptance（修复与 summary/memory-bank 待提交）；repair 落 `b632ee3b5`
Aion 5.8 client/data provenance: Aion 5.8 客户端（用户实机）；客户端对话页契约 `src/main/resources/aion/definitions/quest_dialog/client_dialog_contract.tsv`（1929 声明 select5/select5_1/select5_1_1/select5_2/select6…，不含 select5_3）；退役定义 `quest_definition/quests/1929.xml`（git 历史）与孪生 2900 真端 `reward_extend_stigma1`
npc template/object: 205111（Ecus；教学对话与结晶发放）；203164（起始 NPC）；对话对象运行时 object ID not captured
map/instance: 天族序章普通世界 + 教学实例/飞行段（定义节点 `instance93` → `flight94`，含 `teleport-player-next-available-instance world-id=310070000`）；运行时 instance ID not captured

steps:
1. 前置：Elyos 角色（24 级，进阶职业已转），1929 未完成；从起始 NPC 接取 → 教学实例/飞行段（93/94）→ 过场动画结束（步数 98）
2. 与 205111（Ecus）首次对话：点任务行 → select5 → select5_1 → select5_1_1，最后只有一个「结束对话」按钮
3. 点击「结束对话」：发放本职业烙印之石一次，直接打开烙印之石窗口；再与 205111 对话只出示装备引导页，重复点击不再发放
4. 在烙印窗口把结晶放入凹槽（修复前客户端提示「没有空余的烙印之石凹槽」、无法安装的缺陷已消失）
5. 装备后任务继续推进到 select6 及后续报告/领奖链路

source state/status/vars: 1929 `START`，步数 98（发放前后保持 98）；槽位存储 `advanced_stigma_slot_size` 由 0 变为 2（24 级 + 教学资格）
action/page/button: 任务列表 `CM_DIALOG_SELECT 动作=31`；select5_2 页按钮（`HACTION_SELECT5_3`）；烙印窗口页 1
expected response: 发放一次（`give-item` + `sync-quest-state PACKET_ONLY` + 打开烙印窗口页 1）；再次对话命中持有量门控（priority 0）直接出示 select5_2；槽位在下发前重发（`SM_CUBE_UPDATE` + 1402942）
actual response: 用户实机确认「实机验证成功」——对话只有一个结束按钮、发放一次、凹槽开启可安装、任务可继续（与预期一致）
startup health: 用户重启加载修复构建后正常；无 typed quest engine 初始化异常、无 `QuestCompilationException`/`AMBIGUOUS_TRANSITION` 报告
runtime logs: 验收时刻日志未采集（`not captured`）；此前各轮 `log/quests.log`、`log/console.log`（含 `[STIGMA-TRACE]`/`[QUEST-TRACE]`）见 `.agents/summary/quest-1929-stigma-dialog/DIAGNOSIS.zh-CN.md` 第 5–9 轮窗口
protocol trace: 验收时 `not captured`；第 8/9 轮窗口见 DIAGNOSIS（`SM_QUEST_ACTION`/`SM_CUBE_UPDATE`/`SM_DIALOG_WINDOW` 包序）
screenshots/recordings and SHA-256: `not captured`

acceptance status: ACCEPTED_NEW_PATTERN
matched Pattern: `STIGMA_TUTORIAL_STEP_COUPLED_SOCKETS`、`STIGMA_SLOT_ENTITLEMENT_DATA_DRIVEN`、`STATELESS_GRANT_LOOP_REGRANTS_ITEM`（均新）；representative commit `b632ee3b5`；`Quest1929RetailAlignmentTest#grantsTheStigmaOnceAndKeepsTheInstallStep`
remaining risks: 孪生任务 2900（Asmodian）与 30217/30317（55 级烙印组队任务）只同步了 `extend-stigma-slots` 元数据，未实机复测；魔族 205110/205111 分支与 30217/30317 的槽位档位（6/7 格）未实机验证；装备后 select6 → 报告/领奖链路本次未逐页记录；客户端凹槽展开态来自「重登/登录包序」的结论只在本机 5.8 客户端成立，未做多客户端版本对照

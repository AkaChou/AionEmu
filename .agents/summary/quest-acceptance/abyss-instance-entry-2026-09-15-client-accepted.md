# 欧比斯通路副本入场门禁客户端验收记录

quest: 关联欧比斯入场任务链终点 1044（天族）/ 2042（魔族）；验收对象为「出口或副本内传送门通往欧比斯」的副本入场门禁，代表副本为黑暗普埃塔 Dark Poeta（300040000）。

user acceptance confirmation: 用户原话“验证完成 提交”；2026-09-15；未限定单一分支或步骤，按验收规则视为该副本入场拦截与放行行为客户端验收通过。

server launch mode: not captured（服务端由用户管理；本会话未启动、停止或重启）

repository commit: `d2676efb77c3e81e2742f03a4d5b351752d873c7`（`fix(teleport): gate abyss-link instances behind abyss entry quest`）；本验收记录在其后的文档提交中。

working tree: dirty；本次修复提交只包含 `PortalService.java`、`TeleportService2.java`、`portal_template2.xml` 与 `AbyssInstanceEntryRequirementTest.java`。工作区中其它协作者的 AI、QuestEngine、Memory Bank 改动及暂存文件与本任务无关，均保留未提交、未提交进本任务。

Aion 5.8 client/data provenance: Aion 5.8 客户端；系统提示依据客户端字符串 `STR_MSG_CANNOT_TELEPORT_TO_ABYSS`(1390152，“必须完成欧比斯入场任务。”)；静态数据依据 `portal_template2.xml`、`portal_loc.xml`、`instance_exit.xml`、`npcs/npc_template_286321_800030.xml`；本次未重新采集客户端包哈希。

npc template/object:
- 副本内欧比斯出口 NPC：731666「Dark Poeta Abyss Gate 4.9」（由 `DarkPoetaInstance` 在击杀首领后生成），其 portal 指向 loc 4000108。
- 黑暗普埃塔入口 NPC：因特尔蒂卡「超越之遗物」730186（天族）、贝鲁斯兰「超越之遗物」730185（魔族）、雷山塔之心「陈旧遗物」805296、竞技场大厅 804677/804682、向导传送 835261/835266（dialog 10003）。
- runtime object ID not captured。

map/instance: 黑暗普埃塔 world `300040000`；副本内出口 loc `4000108` 属于欧比斯 world `400010000`；入口 loc 3000400。运行实例 ID not captured。

steps:
1. 使用未完成欧比斯入场任务链（天族 1044 / 魔族 2042 未 COMPLETE）的角色，在因特尔蒂卡或贝鲁斯兰与「超越之遗物」NPC 对话，选择进入黑暗普埃塔。
2. 使用未完成任务的账号（含具备副本任务跳过权限的账号）通过竞技场大厅 / 向导传送 NPC 的 dialog 10003 选择黑暗普埃塔。
3. 角色完成欧比斯入场任务链后重复上述入口，确认可正常进入。

source state/status/vars:
- 未达标：1044 / 2042 为 NONE、START 或 REWARD（未 COMPLETE）。
- 达标：1044 / 2042 为 COMPLETE。

action/page/button: 门户对话 `dialog 10000`（730185/730186/805296/832753/832754）与 `dialog 10003`（804677/804682/835261/835266）选择黑暗普埃塔入口。

expected response:
- 未达标：拒绝进入副本并提示 `STR_MSG_CANNOT_TELEPORT_TO_ABYSS`（“必须完成欧比斯入场任务。”）；该硬门禁不受管理员 `INSTANCE_REQ` 与会员 `INSTANCES_QUEST_REQ` 跳过权限影响，也不消耗副本冷却。
- 已达标：正常进入黑暗普埃塔。
- 副作用兜底：即使未达标角色已处于此类副本内，离开副本时也不会进入欧比斯，而是回绑定点。

actual response: 用户确认“验证完成”；未完成欧比斯入场任务时进入黑暗普埃塔被拦截并给出提示，完成后可正常进入。

startup health: not captured；服务端由用户管理，本会话未启动、停止或重启，未收到 typed quest engine 初始化失败、`QuestCompilationException`、`AMBIGUOUS_TRANSITION` 或生产 catalog 编译失败的报告。

runtime logs: not captured。

protocol trace: not captured。

screenshots/recordings and SHA-256: not captured。

acceptance status: ACCEPTED_NEW_PATTERN

matched Pattern: `ABYSS_LINKED_INSTANCE_ENTRY_GATE`；匹配字段为副本可通过配置出口或副本内传送门直达欧比斯、入场必须校验欧比斯入场任务、门禁必须独立于副本任务跳过权限、未达标者离开副本时回绑定点而非进入欧比斯；差异字段为黑暗普埃塔的配置出口是因特尔蒂卡/贝鲁斯兰，欧比斯通路来自副本内 NPC 731666，因此需要额外的登记集合而非仅依赖 `instance_exit.xml`。代表提交 `d2676efb77c3e81e2742f03a4d5b351752d873c7`；代表测试 `AbyssInstanceEntryRequirementTest#darkPoetaEntryRequiresTheRacialAbyssEntryQuest`、`#darkPoetaAbyssGateLeadsIntoTheAbyss`、`#instancesWhoseExitLeadsIntoTheAbyssAreDetectedFromExitData`。

remaining risks: not captured；`//goto` 等 GM 指令仍按管理员通道直接进入，不在本门禁范围；本次未捕获运行日志、协议抓包与截图附件；未逐一覆盖每个入口 NPC 的客户端对话分支，由静态数据测试锁定 14 条入口路径的前置配置。

# 任务 19638「特别任务2」交付 NPC 洛塔斯(799022) 无交付对话——诊断与修复

日期：2026-10-08　|　分支：`quest`　|　状态：**实现完成，待编译/测试授权与实机验收（PENDING）**

## 一、现象与实机证据

玩家 Kk 报障（任务书指引与 799022 对话，但点击 799022 只有「进入卡斯帕内部」选项，19638 无法继续）。
`log/quests.log` 2026-10-08 证据链：

```text
00:14:22,853 CM_DIALOG_SELECT 玩家=Kk npcId=798926 targetObj=74194 questId=19638 上一页=10 动作=31   # 凯西内尔处接取
00:14:23,986 SM_QUEST_ACTION 任务=19638 状态=3 步数=0
00:34:58,539 SM_QUEST_ACTION 任务=19638 状态=3 步数=64      # 杀怪计数（var1<<6）
   … 128 / 192 / 256 / 320 / 384 / 448 / 512 / 576 …
00:34:58,550 SM_QUEST_ACTION 任务=19638 状态=4 步数=1        # 第 10 杀 → REWARD（待交付）
00:35:50,821 SM_DIALOG_WINDOW 玩家=Kk targetObj=74865 questId=0 下发页=1011   # ← 右键洛塔斯(799022, obj 74865)
00:35:54,799 SM_DIALOG_WINDOW 玩家=Kk targetObj=74865 questId=0 下发页=1011
00:36:31,146 SM_DIALOG_WINDOW 玩家=Kk targetObj=74865 questId=0 下发页=1011   # 三次全落传送门页，零 C->S
```

对照（同一 NPC、同一玩家）：00:14:56 玩家带 10032（s2 对话步在此 NPC）右键时下发的是 **页 10**，
客户端随即渲染出任务行（`npcId=799022 … 上一页=10 动作=31 questId=10032`，随后 1693/1694/10002 链路完整）。

## 二、机制链：开门页由谁决定

1. 服务端**不下发选项列表**，只下发「页面 id + questId」（`SM_DIALOG_WINDOW`，`network/aion/serverpackets/SM_DIALOG_WINDOW.java:52-99`）；选项由 5.8 客户端按页面渲染。
2. 799022 是真端 **Taloc's Hollow 天族入口**：`portal_template2.xml:2185`（`loc_id=3001900`，`dialog=10000`）；NPC 模板 `name_desc="Elim_DF4_01"`、`ai="portal_dialog"`（`npc_template_286321_800030.xml:71535`）。
3. `portal_dialog` 的开门判定在 `ai/portals/PortalDialogAI2.java:143-291`（`checkDialog`）：
   - 读 `questEngine().getQuestNpc(npcId).getOnTalkEvent()`；表内有 START → 页 10、有 REWARD → **页 5 + questId**（奖励窗）；表空 → 传送门页 `getTeleportDialogId`（默认 **1011**，`dataholders/Portal2Data.java:138-141`）。
   - `Specialize01PortalAI2.checkDialog` 同形同表。
4. 普通 NPC（如 19637 的交付 NPC 凯西内尔 798926）走 `ai2/handler/TalkEventHandler.onTalk` → 引擎开门平面，DD 交付面 `DataDrivenNativeRuntime.dispatchReportDialog`（REWARD + `-1` → 页 5，QE-145/QE-160 已收口），因此**不暴露**本缺口；10032 这类带对话步的任务因路由已进表（`installProductionDefinitions`，`QuestEngine.java:2545-2555`）也不暴露。

## 三、根因

`DataDrivenNativeRuntime.installInterest`（`questEngine/tablelane/DataDrivenNativeRuntime.java:1884-1922`）
只安装 kills / talks / fobjs / **acquireTalks**，**漏装 `reportTalksByNpcId`**。于是 DD 行的交付对象 #2
（`reward_npc_name`，QE-160 面）在**引擎派发面**完整（`dispatchReportDialog` 认领 31/1009/-1），
但在**NPC 注册表面**缺席 → 传送门类 AI 看不到可交任务 → 回落传送门页 1011。

其余六族的 `installInterest` 均注册交付 NPC（约定面）：`SimpleHuntHandler.installInterest` 的
`rewardNpcIdsByQuestId → addOnTalkEvent`、`SimpleCollectItemHandler:683`（`rewardNpc.addOnTalkEvent`）、
`SimpleTalkHandler:515-531`、`SimpleUseItemHandler:410-416`、`SimpleItemPlayHandler:491-505`、
`SimpleSerialHuntHandler:228-244`、`SimpleCombineTaskHandler:343-344`。

引入史（证明是遗漏而非设计）：`reportTalksByNpcId` 由 `ca3f35be8`（2026-10-04，DD_TALK_SIMPLE 零步交付面）引入、
`cc74a3249` 扩至全 Talk 行、QE-160（`526afff8b`，2026-10-07）扩至所有行——三次都只接引擎派发面，未触及 `installInterest`。

## 四、影响面（类）

审计工具：同目录 `audit_portal_delivery_rows.py`（DD 表 `reward_npc_name` × `portal_dialog` NPC 集合求交）。

- 命中（Talk 接取行）：**19638 / 19639 → 799022**（Taloc's Hollow）；**15301-15316 → 805327**（LF5_Dike_E）；**25301-25316 → 805339**（DF5_Skuldun_E）。
- 边界：解析口径与 `NativeNpcNameResolver` 的 fail-closed 一致但不含别名表/名组表（MISSING 行不代表无缺口，PORTAL 命中可靠）；`specialize_portal`（`Specialize01PortalAI2`，19 个 NPC）同读该注册表，修复面同覆盖。

## 五、修复

1. **`DataDrivenNativeRuntime.installInterest` 增补交付面注册**：`reportTalksByNpcId → addOnTalkEvent`
   （**不写 `onQuestStart`**——附近任务提示候选集只属接取面；与 SimpleHunt 的 reward NPC 注册逐字同形）。
2. **门禁**：`DataDrivenNativeRuntimeGateTest#reportTalkInterestsAlsoRegisterTheDeliveryDialogFace`
   ——逐项断言交付面进表 + 799022/19638/19639 类锚点 + 不进 `onQuestStart` + 冻结行不借道。
3. **预期实机行为**：REWARD 态右键 799022 → `SM_DIALOG_WINDOW targetObj=… questId=19638 下发页=5`
   （奖励窗，与 19637 家族合同同形）→ 选择奖励（8..22/23 动作段）→ `NativeReportRewardFlow` 结算 → 回页 10 完成。

未采用方案（留档）：让 `PortalDialogAI2.checkDialog` 先经引擎开门平面（`-1`）——会改变全部 485 个
`portal_dialog` NPC 的开门行为；QE-145 已定「阶段面不认领 `-1`、进行中重放=页 10/页 5」，注册表补装是与六族约定的最小对齐面。

## 六、验收状态

- **静态**：diff 复核完成；IDEA 运行测试时已随模块编译（无编译错误）。
- **聚焦测试（IDEA MCP runner，2026-10-08 已执行，全绿）**：
  - `DataDrivenNativeRuntimeGateTest` **36/36**（含新用例 `reportTalkInterestsAlsoRegisterTheDeliveryDialogFace`，已核对测试日志的 testStarted/testFinished 名单）
  - `Quest19637ClientDialogAlignmentTest` **2/2**（同族交付面合同回归）
  - `PortalDialogAI2Test` **5/5**（传送门页轴回归）
  - `QuestProductionStartupGateTest` **2/2**（生产目录启动门）
  - `ProductionCatalogWhitelistVerificationTest` **1/1**（`PRODUCTION_COMPILE_OK=707 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`）
- **待实机（用户，PENDING_CLIENT）**：重启服务端（本改动是启动期注册面，必须重启生效）后，Kk 在 REWARD 态右键洛塔斯 799022，
  应弹奖励窗（`SM_DIALOG_WINDOW targetObj=… questId=19638 下发页=5`）→ 选择奖励领取 → `NativeReportRewardFlow` 结算 → 回页 10 完成；
  验收记录按 `.agents/summary/quest-acceptance/README.zh-CN.md` 字段补写。
- **边界/风险**：START 态（杀怪中）右键 799022 将由传送门页 1011 变为页 10（列表）——与 19639 接取面现状
  （接取 NPC 同为本 NPC，已在表中）及 QE-145「列表不得被功能页抢占」一致；无任务玩家仍见传送门页 1011，入口功能不受影响。
- **沉淀**：模式卡 `QE-162`（DELIVERY_FACE_MUST_JOIN_THE_NPC_DIALOG_REGISTRY）；Playbook 代表性案例资格判定随客户端验收后复核。

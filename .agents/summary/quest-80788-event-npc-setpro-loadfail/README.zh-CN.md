# 事件 NPC 对话按钮 SETPRO1（动作 10000）被原样回发成任务页 ⇒ `Quest_Q80788.html` load fail

> 状态：实现完成 + **门禁全绿（32/32）+ 实机验证通过**（2026-10-06 用户确认「验证成功」：应援 buff + NPC 施法动作）
> 关联 Pattern：QE-148（本主题：接取面不认领未声明动作 + NPC 对话按钮归 NPC 自身层/AI）、QE-137、QE-141、QE-142（0x5d8=关窗，属任务侧 SETPRO 分派面）、QE-144/QE-145
> 日期：2026-10-06（含当日三轮修订：①以「关窗」止血 load fail → ②用户裁定「应该能获取一个 buff」后改为「不认领 + AI 前置」→ ③用户裁定「没有像 npc 831031 一样有施法动作」后把 AI 收尾改为**零发包**，实机通过）
> 验收记录：`.agents/summary/quest-acceptance/80788-2026-10-06-client-accepted.md`

## 1. 报障与现象

用户报障：`npc 833672`（帕塔 / `event_Mongsil_Return`），点击「获得帕塔的应援」→ 客户端弹
`load fail! Quest_Q80788.html (HtmlPageId 10000) (QuestId 80788)`。

实机日志（`log/quests.log` 2026-10-06 09:39–09:40，玩家 Kk）：

```
[S->C] SM_DIALOG_WINDOW 玩家=Kk targetObj=141234 questId=0 下发页=10        ← 打开 NPC（通用选择页）
[C->S] CM_DIALOG_SELECT 玩家=Kk npcId=833672 targetObj=141234 questId=0 上一页=10 动作=10000
[S->C] SM_DIALOG_WINDOW 玩家=Kk targetObj=141234 questId=80788 下发页=10000  ← 动作被当页回发（load fail）
```

同型重复三次；同一 NPC 家族（833671，`event_Mongsil_Newbie`）在 09:39:15 亦为
`动作=10000 → questId=80787 下发页=10000`。

## 2. 证据链

| # | 证据 | 结论 |
|---|---|---|
| 1 | `<客户端解包根>/data_unpacked/Dialogs/event_branch/event_mongsil_return.html`（及 `_d_`/`newbie` 三个同族变体） | 对话页 `select_quest` 的按钮 = `<Act href="HACTION_SETPRO1">获得帕塔的应援</Act>`；另一页 `select1` = `HACTION_FINISH_DIALOG`。**10000 来自该按钮，不是任务页按钮** |
| 2 | `src/main/resources/aion/definitions/quest_dialog/client_dialog_contract.tsv:40888-40891` | 80787/80788 的客户端契约页集合 = {4762 `select_none`, 10002 `select_success`}，**无 10000**（80789/80790 同族） |
| 3 | `aion/data/static_data/quest/retail/data_driven_quest.xml`（80788 段） | 80788 = `category_acquire_=Talk` @ `event_Mongsil_Return` + `reward_npc_name` 同 NPC；**零步（无 progress_info）** |
| 4 | 退役 XML `git show 4ede058c0~1:src/main/resources/aion/data/static_data/quest_definition/quests/80788.xml` | 833672 上的对话转移只有 `QUEST_SELECT(31)`/`ASK_QUEST_ACCEPT(1007)`/`QUEST_ACCEPT_1(1002)`/`QUEST_ACCEPT_SIMPLE(20000)`/`QUEST_REFUSE_*`/`FINISH_DIALOG`——**没有 SETPRO1 转移**；接取收尾 = close-dialog |
| 5 | 真端 cabb10（`.agents/summary/quest-engine-native/p3-prereqs/simple-talk-codegen.md:39,90`） | `10000/10001/10002 → SetQuestProgress(+0xf0)(quest, 1/2/3) + mgr+0x5d8 + Give/Remove`，**零发页** |
| 6 | 真端写入过滤（`.agents/summary/quest-engine-native/p0a/semantic-matrix-camera-channels.md:74`） | `UserQuestData_SetQuestProgress` 过滤器 3 要求 **status∈{3,4}** 才写 ⇒ 未接取（无记录）**零写** |

## 3. 根因（两层）

**表层**：`DataDrivenNativeRuntime.dispatchAcquireDialog`（真端 `FUN_180c47220` 接取面，只服务未接取
玩家）的 `default` 分支把**任何 ≥1000 动作原样当页回发**并携带 questId——而动作 id 不是页
（80787-80790 族任务页契约只声明 {4762, 10002}）⇒ 客户端按任务 html 找 page 10000 → load fail。

**深层**：这个按钮（`HACTION_SETPRO1`=10000）是 **NPC 自身的服务动作**，不是任务动作：

- 真端：NPC 833672 的 AI 脚本 = `World_event_NPC_support_buffer_01`，NPC 模板自带应援技能
  `world_event_buff_l_return_sheep_live`（11049，`파타의 응원`=Pata 的应援，BUFF）；
- emulator：运行时 AI 名 = `ayas_support` → `Ayas_SupportAI2` **早已实现** 833671→11047 /
  833672→11049 的应援 buff；但其 `onDialogSelect` 形状是「**先问任务引擎**，engine 认领则整个动作
  被吞」——接取面无论回发页还是关窗都返回 true，AI 的 buff 分支**永远不可达**。
  （实机 2026-10-06：修复①之后不再 load fail，但 buff 仍不触发，即此面。）

真端侧该动作的收尾 = **零发页**（无 0x188）；注意 `mgr+0x5d8` 关窗（QE-142/141/143 + 2026-10-05 实机回调）
属 **cabb10 的任务侧 SETPRO 分派面**（questId≠0）——本按钮 questId=0、由 NPC 自身 AI 脚本
（`World_event_NPC_support_buffer_01`）处理，不经过该面，不能把 0x5d8 收尾搬过来（见 §7.1）。
退役 XML 里这些任务**无 SETPRO1 转移**（按钮与任务无关，任务接取仍走 31 → 4762 → 20000 → close）。

## 4. 修复（两侧）

**① 引擎侧** `src/main/java/com/aionemu/gameserver/questEngine/tablelane/DataDrivenNativeRuntime.java`
（`dispatchAcquireDialog` default 分支）：只对客户端契约声明过的页动作（`hasButtonPage`）原样回发；
**未声明的 ≥1000 动作零发页、`continue` 候选循环，循环耗尽返回 false（不认领）**——把动作留给
NPC 自身层（AI）。认领（回发页或关窗）都会劫走 AI 增益面。

**② AI 侧** `src/main/java/com/aionemu/gameserver/ai/event/Ayas_SupportAI2.java`：
把 `dialogId == 10000` 分支**提到任务引擎调用之前**（应援按钮是 NPC 自身动作，不属于任务对话面），
增益后**零发页、零关窗**——与同族可施法 NPC 831031（`Npc_SupportAI2`）严格一致。

> 第③轮修正：曾以「真端 cabb10 的 mgr+0x5d8 = 关窗」为由补发 `SM_DIALOG_WINDOW(getObjectId(), 0)`。
> 复核后确认：`0x5d8` 确为**关窗**（QE-142/141/143 + 2026-10-05 实机回调），但它属**任务侧对话分派面**
> （cabb10，questId≠0 的 SETPRO）；本按钮 questId=0、由 NPC 自身 AI 脚本处理，**不经过该面**——
> 收尾语义跨面搬运即多发一条关窗包。实机（2026-10-06）带关窗包时玩家看不到 NPC 的施法动作，
> 撤销后复测通过（buff + 施法动作），故与同脚本族参照 NPC 831031 的零发包收尾对齐（见 §7.1）。

影响面：80875 族（契约声明 10000/10001/10002 的检查页）合法回发保持；「先问引擎再处理 10000」
的同型事件 AI（DivineBonfire / Code_Red_Nurser / Dapplie / Motlie / LegendaryToyBear / Mighty_Hero /
Npc_Support 等）随①一并恢复可达性（本批仅验证 Ayas 链）。

## 5. 门禁

- `src/test/java/com/aionemu/gameserver/questEngine/tablelane/DataDrivenNativeRuntimeGateTest.java`：
  新用例 `acquireFaceEchoesOnlyClientDeclaredActionPages`——轴 1 未声明 10000（事件族）→
  **不认领且零发页**（`onDialog` 返回 false + 无对话包）+ 任务状态零写；轴 2 声明 ≥1000 页动作的行
  仍原样回发；既有断言更新：`acquireTalkDialogFaceFollowsTheRetailVocabulary` 的 1012 改为契约驱动
  （声明回发 / 未声明零发页不认领）。
- `src/test/java/com/aionemu/gameserver/ai/event/AyasSupportAI2Test.java`：
  新用例 `cheerButtonOutranksTheQuestEngineAndSendsNoDialogPacket`——10000 分支先于
  `questEngine().onDialog` 处理，且源内**不得**出现 `new SM_DIALOG_WINDOW(getObjectId(), 0)`
  （零发页、零关窗 = 与参照 NPC 831031 同形）。

## 6. 验收记录（2026-10-06）

- **实机第 2 轮（用户）**：buff 正确获得 ✅；但**没有** NPC 施法动作（对照 831031）✗ → 第③轮修正。
- **focused-test（IDEA MCP，用户授权）**：第③轮复跑 `DataDrivenNativeRuntimeGateTest` **29/29**
  （含 `acquireFaceEchoesOnlyClientDeclaredActionPages`）、`AyasSupportAI2Test` **3/3**
  （含改写后的 `cheerButtonOutranksTheQuestEngineAndSendsNoDialogPacket`）—— 合计 **32/32，0 失败**。
  （第②轮同批另有 `NativeAcceptEntryAskFlowGateTest` 4/4、`DialogServiceQuestDialogTest` 8/8、
  `CMDialogSelectContextTest` 6/6、`QuestEngineNpcDialogDispatchTest` 6/6，合计 56/56。）
- IDE 侧增量编译（`build_project`，仅两个改动文件）0 问题。
- **实机第 3 轮（用户，2026-10-06）**：重启服务端后复测「验证成功」✅ —— 应援 buff（11047/11049）
  与 **NPC 施法动作**均正常；验收记录见 `.agents/summary/quest-acceptance/80788-2026-10-06-client-accepted.md`。

## 7. 施法动作调查（第③轮）

### 7.1 服务端分包差异（用户对照 831031 后）

两 AI 的增益链在服务端**完全同形**（`getSkill(getOwner(), skillId, 1, player).useWithoutPropSkill()`，
`Skill.startCast()` 对 CAST 类技能无条件广播 `SM_CASTSPELL` + 结果包），唯一分包差异是
Ayas 侧多发一条 `SM_DIALOG_WINDOW(getObjectId(), 0)` 关窗；且「多发关窗」正是第②轮为「止血 load fail」
加进去的（其依据是**任务侧** cabb10 的 0x5d8 关窗语义，跨面搬到了 questId=0 的 NPC 自身动作上）；
参照 NPC 831031（`Npc_SupportAI2`）零发页零关窗。
⇒ 第③轮改为零发包收尾（§4 ②），实机复测通过。

### 7.2 客户端数据对照（排除「NPC 不能播动作」）

| 面 | 831031（有施法动作） | 833672 |
|---|---|---|
| 客户端 npc 表 `client_npcs_npc.xml` | `race_type=PC_Light`、`game_lang=light`、`idle_animation=idle_NPC`、`talk_animation=talk_C`、`ui_race_type=light` | 以上字段**均无**（`appearance_custom=World_Guide_2`、`pc_type=light_f`） |
| 外观预设 `custompreset.pak` | `preset_lfnpc35.xml`（`pc_type=pc_lf`，完整 PC 外观） | `preset_world_guide_2.xml`（同族 `pc_type=pc_lf`）⇒ **模型同为天族女性 PC 模型** |
| 技能客户端数据 `skills.pak/client_skills.xml` | 20950：`motion_name=alterfire` + `cast_fx` + `instant_skill=1` | 11049：`motion_name=buff`（**无** `cast_fx`） |
| 真端 NPC 表镜像 `Map/XML/npcs.xml` | 同客户端（含 `race_type`） | 同客户端 + `no_check_animation=1` |
| NPC 技能表（双方） | `CL_BlessofHealth_G1_NPC` | `world_event_buff_l_return_sheep_live`（**与仓库 `npc-skills.xml` 一致**） |

- `motion_name=buff` 是**玩家技能通用动作**（客户端内 108 个技能使用，如 `PR_RapidRegeneration` 全家、
  `WI_MindsEye` 等），PC 动画集必然含该动作 ⇒ 不是「客户端没有这个动作」。
- `race_type` 也不是动作的必要条件（客户端 639 个 PC 外观 NPC 无该字段，含大量过场 NPC）。
- 结论：客户端侧**看不出**「833672 不能播 buff 动作」的硬约束；分包差异只剩关窗包，故按 §4 ② 对齐。

### 7.3 工具与证据

- 客户端解包（只读）：`<客户端目录>/data/skills/skills.pak` → `client_skills.xml`（8099 条目）、
  `<客户端目录>/data/custompreset/custompreset.pak` → `preset_world_guide_2.xml`、
  `<客户端解包根>/npcs_unpacked/client_npcs_npc.xml`；解包用 `<客户端解包根>/aion_pak.py`（输出到 `/tmp`，未入库）。
- 真端：`<真端根>/Map/XML/npcs.xml`（真端 NPC 表镜像）；脚本 DLL 虚表读取脚本
  `tools/dump_vftable.py`（`World_event_NPC_support_buffer_01` 与同族脚本对象共用
  `IAIScriptNpcImp` 基类虚表，无逐脚本回调体可读——本批未再深入）。

## 8. 边界与悬案

- **施法动作已闭环**：撤销关窗包后实机通过（buff + 动作）⇒ 成因是**多发收尾包**，不是客户端数据；
  833672 缺 `race_type`/`idle_animation`/`talk_animation` **不阻塞**施法动作（§7.2 已排除「无动作可播」）。
  备选路径（本轮未采用、留档）：① 客户端数据补丁（`npcs.pak` 按 CPK-001 补字段）；② 施法者改玩家
  （`getSkill(player, …)`，同 `Conquest_Npc_BuffAI2` 形）。日后其它 NPC 出现「buff 有、动作无」，
  先比收尾包再动客户端数据。
- **同型 AI 面**：「先问引擎再处理 10000」的事件 AI 还有 DivineBonfire / Code_Red_Nurser /
  Dapplie / Motlie / LegendaryToyBear / Mighty_Hero / Npc_Support 等；本批只验证 Ayas 链，
  它们的 buff 可达性依赖同一「接取面不认领」前提（回归面 = DD 门两轴）。
- **DD 进度面**（`dispatchDialog` 的 `其余 ≥1000 原样回发`）未动：只服务已接取且命中当前步的行，
  有 9/28 实机翻页序列背书；同型未声明页若在该面出现属潜在缺口，待样本。
- **无 AI 处理该按钮的 NPC**：落 DialogService 兜底（questId=0 两参回显 / questId≠0 关窗，
  QE-137#7），未取样本。
- 「点应援即接取任务」假设不被证据支持（退役 XML 无 SETPRO1 转移 + 真端 SetQuestProgress 对
  未接取零写）；若后续真端取证成立再议。

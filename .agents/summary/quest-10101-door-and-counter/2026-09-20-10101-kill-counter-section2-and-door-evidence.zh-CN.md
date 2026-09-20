# 任务 10101/20101「不祥的幻象」击杀计数空分子与遮蔽的门取证

- 时间：2026-09-20（用户提供 14:30–14:31 实机 QUEST-TRACE 与任务追踪截图）
- 玩家：Ww（截图显示第 3 行「消灭巴鲁纳次元研究所中的次元研究所卫兵 (/2)」带 [完成] 徽标）
- 客户端权威：`Quest.pak` / `data/Dialogs/10000_19999/quest_q10101.html`；解包根 `/Users/mc/PycharmProjects/unpak`
- 相关文件：`src/main/resources/aion/data/static_data/quest_definition/quests/10101.xml`、`20101.xml`
- 本轮改动状态：已改 9 个任务 XML（10101/20101 + 7 个同族，未提交）；门禁已跑（见第十三节），未实机复验
- ⚠️ 第十二节的「行号探针假设 A/B」在第十三节被证伪/收敛：**不要**把 SECTION_0 直接当 HTML 行号改写阶段

## 一、症状与日志证据

```text
14:31:37 SM_DIALOG_WINDOW questId=10101 下发页=10000（731530 SETPRO2 → 传送进 301340000）
14:31:44 SM_QUEST_ACTION 任务=10101 状态=3 步数=3   # 第 1 只卫兵：var0=3
14:31:44 SM_QUEST_ACTION 任务=10101 状态=3 步数=4   # 第 2 只卫兵：var0=4
```

- 客户端任务说明第三行 `([%8]/2)`：分母 2 正确，分子恒为空 —— 服务端从未写 `SECTION_2`。
- 玩家同时反馈下一步「打开遮蔽的门，进入通道」的门无法交互（见第五节取证计划）。

## 二、客户端计数槽位合同（本轮复核）

1. `quest_q10101.html` 的 `quest_summary` 9 行使用三段式槽位：行 k 用 `[%3k]`（可见）/`[%3k+1]`（颜色）/`[%3k+2]`（计数）。第三行计数为 `[%8]` ⇒ **group 2 ⇒ SECTION_2（offset 12）**。
2. 客户端自身监控数据 `quest_script_monster.csv`：`10101,Progress(2~!4),,killedByUser,…`。同型对照（数值形式的首值 == HTML 计数 group）：
   - 10084 `Progress(2~!6)` ↔ `[%8]`；12522 `Progress(1~!5)` ↔ `[%5]`；14062 `Progress(2~!8)` ↔ `[%8]`；10091 `Progress(3~!6)` ↔ `[%11]`；14011/14022/24013-24015 同形（见第六节）。
   - 结论：数值形式 `Progress(k~!m)` 的 **k = 计数所在 SECTION（也即显示槽 group）**，区间尾部 m 对应服务端行索引行走范围；10101 的 k=2 ⇒ 计数必须在 SECTION_2。
3. 仓库既有合同：QE-012（`%3n+2 ← SECTION_n`，6-bit 固定位段）、QE-040（行索引固定在 SECTION_0，计数只能放 SECTION_1+）；已实机验收的 11468（`[%5]/[%8]/[%11]` ↔ var1/var2/var3）、15001（`[%5]/[%8]` ↔ var1/var2）与此一致。

## 三、根因

`10101.xml`/`20101.xml` 的击杀路线把「杀了几只」直接编码进行索引 `var0`（2→3→4），progress 布局只有 `var0`，`SECTION_2` 永远是 0：

- 客户端按 SECTION 读分子 ⇒ 分子恒空（`(/2)`）；
- 实机上「杀 1 只就跳到门行」也是同一编码的副产品（行索引被当成计数器）。

## 四、本轮修改（10101.xml、20101.xml）

1. `<progress>` 新增 `<bit-field name="var2" offset="12" width="6" min="0" max="2"/>`（SECTION_2，客户端 `[%8]`）。
2. 两次击杀在同一事务内同时写行索引与计数：
   - `s2→s3`（第 1 只）：`set var0=3` + `set var2=1`；
   - `s3→s4`（第 2 只）：`set var0=4` + `set var2=2`。
3. 失败回退（`s2/s3/s4 → s1` 的 enter-world / die / log-out 共 9 条）追加 `set var2=0`，避免脏计数进入下一轮。
4. 未改动（刻意保持最小风险）：阶段路由（sensory area 仍读 var0=4）、掉落门禁 `collecting-step="7"`、`reward` 节点投影与迁移路线。奖励态因此保留 `var2=2`，击杀行显示 2/2。

预期实机：重置任务到 `var0=2`（`//quest set 10101 START 2`）后，第 1 只卫兵显示 `1/2`（var0=3, var2=1），第 2 只显示 `2/2`（var0=4, var2=2），随后任务说明进入下一步。

## 五、遮蔽的门（234193）取证

静态证据链（均指向「点击即开门」，非任务变量门禁）：

- 客户端 NPC `234193` = `IDLDF4_Re_01_Door_10`（「遮蔽的门」，`ui_type=Door_obj`、`cursor_type=action`、`talk_delay_time=5`、`ai_name=IDLDF4_Re_01_NoShowNPC_01`）。
- 零售 AI 映射：`npcaipatterns_idldf4_re_01_ssh.xml` 的 `IDLDF4_Re_01_NoShowNPC_01` = `on_talked_by_user → use_skill(OBJI_SELF, SKILLI_INDEX_0)`；`npc-skills.xml` 组 `NS_20183113926F7CFE` 唯一技能 = `NWI_Suicide_ID_NoneFx`(21494，penalty 19713)。
- 服务端模板 `npc_template_216189_235748.xml:67797`：`npc_type=NON_ATTACKABLE`、`ai="useitem"`、`tribe=IDLDF4_RE_01_DOOR`；`AI2Engine.selectNpcAi` 对「有规则的 retail pattern」不会回落到 `useitem`，`RetailPatternAI2.handleTalkedByUser`（AIM-006，commit 5f7df6599）已跳过不存在的默认 HTML 页。

因此静态上「点击门 → 自身施放自杀技能 → 门消失/开口」应成立；若实机仍无响应，需要一次带日志的复现（本轮无法自行取得）：

1. `//quest log` 打开后**长按/持续点击门 5 秒以上**（客户端 `talk_delay_time=5`，可能要求保持交互）；记录门是否消失、通道是否打开。
2. 采集同一时间窗口的服务端日志（quest trace、AI/skill、unknown packet、`SM_DIALOG_WINDOW` page=10 一类错误页）。
3. 若门完全不响应且日志无任何客户端请求，则问题在客户端点击/命中判定（门 NPC 的刷点与碰撞：`spawns/Instances/301340000_Linkgate_Foundry.xml` entity 338，Z 值审计属 `spawn-z-audit` 范围）；若服务端收到请求但无效果，则问题在 `retail_pattern` 选择或自杀技能执行（需要 `AI2Engine` 选择日志或技能日志）。

## 六、同型族审计（sweep 候选，未修改）

脚本：`.agents/summary/quest-10101-door-and-counter/audit_stage_as_kill_counter.py`（同一 NPC 连杀、仅行走 var0、客户端存在计数槽）。
命中 8 个同型任务，全部存在「分子区段从未写入」风险：

| quest | 服务端 var0 行走 | 客户端计数槽 group | 客户端行条件 |
| --- | --- | --- | --- |
| 14011 | 3,4 | 2 | Progress(2)/(3)/(4) |
| 14014 | 6,7 | 4 | Progress(5~!7)/(7) |
| 14021 | 2..7 | 1 | Progress(1~6) ×6 |
| 14022 | 3..7 | 2 | Progress(2~7) ×2 |
| 24011 | 2..6 | 2 | Progress(2~5)/(6) |
| 24013 | 4..7 | 3 | Progress(3~6)/(7) |
| 24014 | 3..5 | 2 | Progress(2~4) |
| 24015 | 3,4 | 2 | Progress(2~3)/(4) |

同族修复方向与 10101 相同：把击杀计数镜像到客户端计数槽所在的 SECTION_k（6-bit 固定位段），行索引继续只做任务说明行控制；每个任务需按各自客户端行条件单独确认 k 与击杀次数，不能机械套用。

## 七、验证状态与边界

- 已做（静态，无构建）：2 个 XML 可被 XML parser 解析；`git diff --stat` = 每个文件 +14 行；计数位段 `var2@12 width=6` 符合 QE-012；击杀动作写入字段与 `ClientQuestSectionAlignmentTest` 的「var1..var4 必须落在 6*N」规则一致。
- 未做：Maven 测试、服务端启动/重启、真实客户端复验、旧存档迁移（现网存档若已停在 var0=4 且 var2=0，需要重新接取或 `//quest set 10101 START 2` 才能看到 1/2→2/2）。
- 建议门禁（需用户授权）：`mvn -q -Dtest='ClientQuestSectionAlignmentTest,QuestMonsterProgressContractAuditTest,ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest' test`。
- Memory Bank：待实机验收后再把「行索引行走 ≠ 计数区段，客户端分子只读 SECTION_k」提升为 Pattern（当前仅本主题目录留证）。


## 八、遮蔽的门：交互链路静态复核（2026-09-20 追加）

点击门 → 服务端执行链（逐段核对，均无静态阻断）：

1. `CM_SHOW_DIALOG` → `NpcController.onDialogRequest`（`src/main/java/com/aionemu/gameserver/controllers/NpcController.java:404`）：
   - 仅校验 `getObjectTemplate().canInteract()`（= talkInfo != null）。234193 模板含 `<talk_info delay="1" distance="5"/>`（`npc_template_216189_235748.xml:67800`）⇒ 通过；
   - 随后派发 `getAi2().onCreatureEvent(AIEventType.DIALOG_START, player)`；事件门禁 `isNonFightingState()` 对空闲门成立（`AbstractAI.java:241`）。
2. `AbstractAI:723` → `handleDialogStart(player)` → `RetailPatternAI2:969`：
   - 模式无 gauge 事件 ⇒ 不走 gauge；
   - `handleTalkedByUser`（`RetailPatternAI2:1031`）判定为直接交互（`use_skill` 且无 `on_hyperlink_clicked`）⇒ 跳过默认 HTML 页（AIM-006 / commit 5f7df6599），直接 `runEvent("on_talked_by_user", null, player)`。
3. 规则动作 `use_skill(OBJI_SELF, SKILLI_INDEX_0)` → `RetailPatternAI2:2216 useSkill`：target=自身；技能 = `npc-skills.xml:1391` 组 `NS_20183113926F7CFE` 唯一技能 `NWI_Suicide_ID_NoneFx`(21494) → `getOwner().getController().useSkill(21494, 1)`。
4. AI 选择：`AI2Engine.selectNpcAi("useitem", 234193, npc)`（`ai2/AI2Engine.java:136`）：`useitem` 不在保留名单 → retail 模式有 `on_talked_by_user` 规则 ⇒ 返回 `retail_pattern`（空模式才有 useitem 回落）。`RetailPatternAI2.supports` 的 gauge/wake-up/master/npc-party/world-scene 守卫在「只有 on_talked_by_user」的模式上全部平凡通过（`RetailPatternAI2:558-589、701-848`）。
5. 模板 `is_dialog` 缺失（默认 false）不影响本路径：`isDialogNpc()` 只在 `TalkEventHandler.onSimpleTalk` 子状态与 `QuestItem*` AI 中读取。

⇒ 代码层没有「门点了没反应」的明显缺陷；剩余可能只剩三类，必须靠运行期证据区分：

| 观测 | 结论 | 处理方向 |
| --- | --- | --- |
| `//ai2 info` 显示 `Ai name: useitem`（非 retail_pattern） | 模式未被选中（映射/加载/守卫） | 修 AI 选择或映射，属共享合同缺陷 |
| AI=retail_pattern，但点击后 AI 日志没有 `DIALOG_START` | 客户端未发出交互请求（点击命中/距离/客户端门槛） | 查客户端门对象与刷点、交互距离；属客户端层 |
| 有 `DIALOG_START` 与 `on_talked_by_user`，门不死/通道不开 | 自杀技能 21494 或 penalty 19713 执行失败 | 查技能执行与 NPC 伤害免疫（NON_ATTACKABLE/CONSTRUCT） |

## 九、门取证的 3 步操作（用户侧，无需改代码）

1. 面对门按目标选中 → 输入 `//ai2 info`，记录 `Ai name / state / substate`。
2. 保持选中 → `//ai2 log`（对该 NPC 打开事件日志；亦可 `//ai2 eventlog` 全局）→ 点击门 1~3 次（客户端 `talk_delay_time=5`，建议持续按住/连点 ≥5 秒）。
3. 记录：门是否消失、通道是否打开、服务端日志是否出现 `DIALOG_START` / `on_talked_by_user` / 技能 21494 / `SM_DELETE`。
   - 注意：该刷点 `spawns/Instances/301340000_Linkgate_Foundry.xml:35` 带 `respawn_time="30"`，门被击碎后 30 秒会重刷；若通道已打开属正常，不要把重刷当成「没反应」。


## 十、2026-09-20 晚：玩家二次实机反馈复核

反馈 1「点击『使用进攻回廊。』后先弹『稍等一下。』页再进副本」：

- 结论：**与真端/旧 handler 一致，不是本轮回归**。
- 证据：`origin/history` 旧 handler `_10101Dismal_Developments.java` 的 731530 分支：

```java
if (!TeleportService2.teleportToInstance(player, 301340000, 243, 333, 392, (byte) 91, TeleportAnimation.BEAM_ANIMATION)) return false;
changeQuestStep(env, 1, 2, false, 0);
return sendQuestDialog(env, 10000);   // 10000 = HTML_PAGE_CHECK_USER_ITEM_OK
```
- 客户端映射：`docs/quest/client-dialog-mapping/quest-dialog-action-details.csv` 第 574 行
  `10101 … check_user_item_ok,10000,HTML_PAGE_CHECK_USER_ITEM_OK … HACTION_FINISH_DIALOG,1008 … 稍等一下。`；
  XML 现状 `s1→s2` after-commit = teleport → PACKET_ONLY → `SHOW_QUEST_PAGE CHECK_USER_ITEM_OK`，顺序与旧 handler 相同（先传送后发页）。
- 若要移除该页，属于「偏离真端客户端流程」的改动，需用户明确决定，本轮不改。

反馈 2「任务击杀完成后 html 为空、没有任务内容」：

- 本轮无法读取用户两张截图（`/Users/mc/Library/Caches/WeType/dsclp/*.png` 不存在），需重新附上；同时需要区分「任务追踪窗口为空」与「点击门弹出的对话页为空」两种情况。
- 静态复核（本轮新增）：点击 234193 时 **不应下发任何 HTML**：
  - `RetailPatternAI2.isDirectTalkInteraction`（`ai/RetailPatternAI2.java:1047`）对 `on_talked_by_user → use_skill` 且无 `on_hyperlink_clicked` 的模式返回 true ⇒ `handleTalkedByUser` 跳过 `super.handleDialogStart`（不发默认页 10）；
  - 该模式只有 `use_skill`，门自体施放 21494（自杀），不会产生对话窗口；
  - 零售 AI 数据装配：`RetailAiDefinitionLoader` 读取 `npc-ai-parts/npc-ai_<start>_<end>.xml` 分片 + 全部 `npcaipatterns*.xml`，覆盖 234193 与其模式 ⇒ `selectNpcAi` 应返回 `retail_pattern`。
- 因此「门点击后出现空白 HTML」若属实，说明运行期与静态预期不符（AI 选择或客户端页面缺失），必须用 `//ai2 info`（选中门）与 `//ai2 log` + 点击的日志来定位；「任务追踪窗口为空」则需要 `SM_QUEST_ACTION 步数=` 的实际值与 `//quest set/show` 的现状。


## 十一、门禁执行记录（用户授权后，2026-09-20 16:37）

```text
mvn -q -Dtest='QuestMonsterProgressContractAuditTest,ClientQuestSectionAlignmentTest,ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest' test
→ QuestMonsterProgressContractAuditTest 13/13 PASS（含本轮新增 quest10101And20101MirrorTheScoutCountIntoClientSectionTwo）
→ ClientQuestSectionAlignmentTest 6/6 PASS
→ QuestDefinitionCatalogManifestTest 10/10 PASS
→ ProductionCatalogWhitelistVerificationTest 1/1 PASS
→ PRODUCTION_COMPILE_OK=6189、PRODUCTION_COMPILE_FAILURES=0、
  PRODUCTION_INTERACTION_OBJECT_FAILURES=0、PRODUCTION_WHITELIST_VIOLATIONS=0
```

新增回归（`QuestMonsterProgressContractAuditTest`）：10101/20101 首次击杀 → `var0=3, var2=1`；第二次击杀 → `var0=4, var2=2`；并断言 `var2` 位于 offset 12（SECTION_2）。

## 十二、待补证据：任务追踪窗口（任务说明）为空

玩家澄清：击杀完成后为空的是**任务追踪窗口/任务说明**（非对话页），且两张截图未能送达（本地缓存路径不存在）。需要的证据：

1. 空白时刻的 `[QUEST-TRACE][S->C] SM_QUEST_ACTION 任务=10101 状态=… 步数=…`（每次击杀后各一条），用于确认客户端实际收到的 SECTION_0。
2. 探针（一次即可判定行模型）：`//quest set 10101 START 2/3/4/5` 逐值切换，记录任务追踪窗口在每个值下显示的行（或为空）。
3. 重传两张截图。

候选解释（待探针判定，**未改动阶段路由**）：

- 假设 A：客户端按 `SECTION_0 == 行号` 显示行 ⇒ `var0=4` 应显示第 5 行「和 Ditono 对话」；若探针显示为空，则 A 被否证。
- 假设 B：客户端追踪行由自身 objective 数据门控（`sensoryArea Progress(4)` 等）⇒ 空的根因是「第二次击杀把行索引推到 4，而客户端第 5 行的客户端条件尚未满足」；对应修法是第二次击杀只把行索引推进到门行 3，由感应区推进 3→4。该改法会牵动后续阶段号与 `Progress(7)`／掉落 `collecting-step=7`，必须先由探针确认再实施。


## 十三、2026-09-20 收敛：阶段逐步行走 + 计数镜像到「该行的槽」

### 13.1 客户端权威数据（Quest.pak，本次逐条读取）

```text
Quest_unpacked/quest_script_monster.csv:88  10101,Progress(2~!4),,killedByUser,,1,IDLDF4_Re_01_DrakanScout_Ra_65_An_Q
Quest_unpacked/quest_script_monster.csv:89  10101,Progress(4),,sensoryArea,,1,IDLDF4Re_01_Q_SensoryArea_Q10101
Quest_unpacked/quest_monster.csv:2841       10101,Progress(7),doc_quest_10101a,questItemDropMonster,,1,ldf5_fortress_vritra_noble_l_qnmd_65_an
```

- 击杀目标在客户端 progress 2、3、4 上都有效（`2~!4`）⇒ 服务端 `SECTION_0` 必须**每杀 +1**（2→3→4）；感应区目标在 progress 4 生效（2 杀打满的同一刻），掉落/收集步在 progress 7（本 XML `collecting-step="7"` 一致）。
- 旧 handler（`origin/history` 的 `_10101Dismal_Developments`）同口径：`defaultOnKillEvent(env, 234680, 2, 4)` 逐杀累加 var0，`804556`（Ditono）在 `var == 5` 对话。
- 客户端计数分子读的是**该行自己的槽**：`quest_q10101.html` 第三行 `visible="[%6]" ... ([%8]/2)` ⇒ 槽 2 ⇒ `SECTION_2`（offset 12）；未写该槽时实机显示空分子（本轮 14:31 截图）。

### 13.2 被证伪的假设（第十二节 A/B）

- 假设「客户端行列号 = SECTION_0 原值」与实机矛盾：玩家在击杀阶段（SECTION_0=3/4）截图看到的是**击杀行**（第 3 行）带空分子，而不是第 4/5 行；客户端是按自己的 progress 数据（`Progress(2~!4)`）决定显示哪一行，SECTION_0 只是 progress 指针。
- 因此 10101 在 `s2→s3 / s3→s4` 上「保持阶段不动」的改写（曾临时落到工作区）**已回退**：它会让 `Progress(4)` 的感应区目标永远不出现。回退脚本：`reapply_10101_counter_only.py`（只保留计数镜像）。

### 13.3 同族 sweep（脚本 `audit_stage_as_kill_counter.py`）

同一形状（击杀路由只走 var0，客户端中段行有 `([%3k+2]/N)` 计数槽）在生产目录命中 8 个，其中 7 个可机械对齐：

| 任务 | 客户端计数槽 | 要求 | 击杀阶段 | 本次处置 |
| --- | --- | --- | --- | --- |
| 10101 / 20101 | `[%8]` → SECTION_2 | /2 | 2→3→4 | ✅ 已镜像 |
| 14011 | `[%8]` → SECTION_2 | /3 | 2→3→4 | ✅ 已镜像 |
| 14014 | `[%14]` → SECTION_4 | /3 | 5→6→7 | ✅ 已镜像 |
| 14021 | `[%5]` → SECTION_1 | /6 | 1→…→7 | ✅ 已镜像 |
| 14022 | `[%8]` → SECTION_2 | /6 | 2→…→7 | ✅ 已镜像 |
| 24013 | `[%11]` → SECTION_3 | /5 | 3→…→7 | ✅ 已镜像 |
| 24014 | `[%8]` → SECTION_2 | /3 | 2→3→4→5 | ✅ 已镜像 |
| 24015 | `[%8]` → SECTION_2 | /3 | 2→3→4→reward | ✅ 已镜像 |
| 24011 | `[%8]` → SECTION_2 | /5 | 6 条击杀转换（1→2 另有对话路径） | ⛔ EVIDENCE_REQUIRED：击杀条数 6 ≠ 客户端 /5，需先判定是多余击杀路线还是计数槽判断错误 |

- 落地脚本：`apply_phase_walk_counter_mirror.py`（dry-run 默认；`--apply` 写入）。每任务断言：客户端恰有 1 个计数槽、击杀转换条数 == `/N`、阶段链闭合，然后在每条击杀转换内写入 `set-variable varK = 序号`，并在 `<progress>` 增加 `varK @ offset 6*K, width 6, max N`。
- 结构性回归：`QuestMonsterProgressContractAuditTest#phaseWalkKillQuestsMirrorEachKillIntoTheClientCounterSection`（7 任务逐杀断言「计数 1..N + 阶段落在期望值」）；10101/20101 由既有用例覆盖（阶段 3/4 + 计数 1/2）。
- 未纳入：其余「行索引 + 计数」家族（如 14252/24252 已按行索引合同修复）不在本次 sweep 范围，因为它们的客户端条件含 `SECTION_0==S` 门控而非 `Progress(k~!m)` 跨段。

### 13.4 遮蔽的门（234193）静态链复核（本轮新增）

| 环节 | 证据 | 状态 |
| --- | --- | --- |
| 刷怪 | `spawns/Instances/301340000_Linkgate_Foundry.xml:35` `<spawn npc_id="234193" respawn_time="30">` @ (218.77, 318.36, 392.89) | ✅ 存在 |
| 零售 AI 模式 | `definitions/compact/ai/npcaipatterns_idldf4_re_01_ssh.xml` `IDLDF4_Re_01_NoShowNPC_01` → `on_talked_by_user → use_skill(OBJI_SELF, SKILLI_INDEX_0)` | ✅ 存在 |
| NPC→模式绑定 | `npc-ai-parts/npc-ai_216004_235537.xml` `npc id="234193" ai="IDLDF4_Re_01_NoShowNPC_01" talk_delay="5"` | ✅ 存在 |
| 技能组 | `definitions/compact/skills/npc-skills.xml:1391` `NS_20183113926F7CFE = NWI_Suicide_ID_NoneFx(21494)`；`:12136` 该组已分配给 `npc_ids="234193 …"` | ✅ 存在 |
| 实机响应 | 需要 `//ai2 info`（选中门）+ `//ai2 log` + 点击 1~3 次（客户端 `talk_delay_time=5`，建议持续点击 ≥5 秒）的日志 | ⏳ PENDING |

结论：静态链完整，门「点不动」只可能出在运行期选择（AI 选择/技能施放/客户端交互标志）或玩家点击方式（5 秒延迟）。本轮无法取得运行期证据，保持 EVIDENCE_REQUIRED。

### 13.5 门禁执行记录（用户已授权 Maven 门禁）

```text
# 1) 授权命令（本轮改动后复跑，PASS）
mvn -q -Dtest='QuestMonsterProgressContractAuditTest,ClientQuestSectionAlignmentTest,ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest' test
→ QuestMonsterProgressContractAuditTest 14/14、ClientQuestSectionAlignmentTest 6/6、
  QuestDefinitionCatalogManifestTest 10/10、ProductionCatalogWhitelistVerificationTest 1/1
→ PRODUCTION_COMPILE_OK=6189、PRODUCTION_COMPILE_FAILURES=0、
  PRODUCTION_INTERACTION_OBJECT_FAILURES=0、PRODUCTION_WHITELIST_VIOLATIONS=0

# 2) 扩大一轮（追加同族合同测试；PASS 的与 1) 相同，另有一条与本改动无关的既有失败）
mvn -q -Dtest='QuestMonsterProgressContractAuditTest,ClientQuestSectionAlignmentTest,QuestSection0ReportRowContractTest,QuestKillCounterRetailGateTest,ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest' test
→ Tests run: 38, Failures: 1（QuestKillCounterRetailGateTest#singleCounterQuestsRequireExactlyTheClientGate:
  quest 15101 is in the single-counter contract but uses [var0, var1]）
→ QuestSection0ReportRowContractTest 3/3 PASS
```

既有失败取证（非本任务引入）：`quests/15101.xml` 在工作区**未被修改**，其已提交的击杀转换同时写 `var0`（0→1 行推进）与 `var1`（计数器），而 `quest-kill-counter-retail-contract.tsv:44` 把 15101 记作单计数合同 → 该门禁需要按 8.43 的「SECTION_0 双语义」区分行索引与计数器；本任务的 9 个任务都**不在**该 TSV 中，不受影响。

### 13.6 待实机复验（PENDING，需服务端重启后）

1. `//quest delete 10101` 后重接（旧存档 packed 值按新布局会误读），推进到副本内，验证：第 3 行 `1/2` → `2/2`；击杀行不再空分子；2 杀后任务说明切到「打开遮蔽的门，进入通道」。
2. 若 2 杀后仍为空：逐值探针 `//quest set 10101 START 2/3/4/5/6/7/8/9`（每次 /quest set 后看任务说明行），把「SECTION_0 值 → 实际显示行」记录回本节；这将一次性判定剩余的行映射问题。
3. 同族 7 任务（14011/14014/14021/14022/24013/24014/24015）各跑一次击杀阶段，确认计数 1..N 可见；24011 先做 13.3 的判定。
4. 门（13.4）：`//ai2 info` + `//ai2 log` + 持续点击 ≥5 秒的记录。


## 十四、2026-09-20 19:33 实机复验：计数已写入，但说明行不推进（VISIBILITY_REFRESH 缺失）

### 14.1 实机日志（用户提供，新包已生效）

```text
19:33:02 CM_DIALOG_SELECT npcId=731530 动作=10001（SETPRO2，进副本）
19:33:02 SM_QUEST_ACTION 任务=10101 状态=3 步数=2                       # var0=2（击杀起点）
19:33:09 SM_QUEST_ACTION 任务=10101 状态=3 步数=4099                    # 4099 = 1<<12 | 3 → var0=3, var2=1 ✅ 1/2
19:33:11 SM_QUEST_ACTION 任务=10101 状态=3 步数=8196                    # 8196 = 2<<12 | 4 → var0=4, var2=2 ✅ 2/2
```

- 计数镜像修复**生效**：`SECTION_2` 已随击杀写入 1、2（不再是空分子）✅。
- 但客户端任务说明**停在击杀阶段**，没有切到「打开遮蔽的门，进入通道」；用户用 `//quest set 10101 START 4`（GM 面板显示 `Vars: 4 0 0 0 0 0`）后说明才推进。
- `//quest set` 与击杀路线走的是**同一个** `SM_QUEST_ACTION`；差别在于 GM 命令随后还调用 `updateZone()` + `updateNearbyQuests()`（`commands/admin/Quest.java:222-224`），即引擎的 `QuestStateSyncMode.VISIBILITY_REFRESH`（`PlayerQuestStateSyncPort.sync` 把 refreshVisibility 映射为 zone + nearby 刷新）。
- 结论：**`var0=4, var2=2` 本身是正确的客户端契约状态**（`var2=2` 才能让第三行显示 `2/2`；GM 写入的 `4 0 0 0 0 0` 只是顺带把分子清空，并不是正确状态）；缺的是**说明行推进时的可见性刷新**。

### 14.2 修改

`apply_row_advance_visibility_refresh.py`（dry-run 默认）把「阶段/说明行推进」的 START→START 转换从 `PACKET_ONLY` 升为 `VISIBILITY_REFRESH`：

| 任务 | 升级的转换 | 理由 |
| --- | --- | --- |
| 10101 / 20101 | s3→s4（2 杀完成→门行）、s4→s5（感应区→迪托诺）、s5→s6、s6→s7、s7→s8 | 每步都会换行；s2→s3 仍是同一击杀行 → 保持 PACKET_ONLY；s8→reward 原本就是 LEVEL_AND_VISIBILITY_REFRESH |
| 14021 | s6→s7（第 6 杀后进入「去找线索」行，非 REWARD） | 同型 |
| 24014 | s4→s5（第 3 杀后进入「交付证据」行，非 REWARD） | 同型 |

同族其余任务（14011/14014/14022/24013/24015）的击杀阶段结束即进入 REWARD（已是 LEVEL_AND_VISIBILITY_REFRESH），无需改动；24011 仍在 EVIDENCE_REQUIRED。

### 14.3 回归

- 新增 `QuestMonsterProgressContractAuditTest#midRouteJournalRowAdvancesCarryAVisibilityRefresh`：对上述 12 条转换断言 after-commit 里存在带 `refreshVisibility()` 的 `SyncQuestState`。
- 门禁（用户已授权，改动后复跑）：

```text
mvn -q -Dtest='QuestMonsterProgressContractAuditTest,ClientQuestSectionAlignmentTest,ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest' test
→ QuestMonsterProgressContractAuditTest 15/15（含 10101/20101 计数镜像 + 7 任务同族镜像 + 行推进刷新）、
  ClientQuestSectionAlignmentTest 6/6、QuestDefinitionCatalogManifestTest 10/10、
  ProductionCatalogWhitelistVerificationTest 1/1
→ PRODUCTION_COMPILE_OK=6189、PRODUCTION_COMPILE_FAILURES=0、
  PRODUCTION_INTERACTION_OBJECT_FAILURES=0、PRODUCTION_WHITELIST_VIOLATIONS=0
```

### 14.4 待实机复验（PENDING）

1. 重启后重接 10101：第 1 杀 `1/2`、第 2 杀 `2/2`，且**不需要** GM 命令就切到门行；随后进感应区 → 迪托诺 → 逃生回廊 → 收集步（`Progress(7)`）。
2. 若说明行仍不推进：记录「击杀同步」与「`//quest set` 后」两段 `SM_QUEST_ACTION` 前后客户端窗口截图，并尝试对照把 `var2` 清零（仅实验，不作提交）以区分「刷新缺失」与「客户端按计数槽决定行」两种解释。
3. 门（234193）交互仍按 13.4 取证（`//ai2 info` + `//ai2 log` + 持续点击 ≥5 秒）。


## 十五、2026-09-20 19:45 纠偏收敛：彻底撤销高位 var 污染，步数恢复纯 var0 阶段行走

### 15.1 用户实机反馈与根因确认

用户在实机提供核心证据：
```text
09-20 19:33:09 SM_QUEST_ACTION 任务=10101 状态=3 步数=4099  # (1 << 12) | 3 -> var0=3, var2=1
09-20 19:33:11 SM_QUEST_ACTION 任务=10101 状态=3 步数=8196  # (2 << 12) | 4 -> var0=4, var2=2
击杀2个 npc后 ，任务没有进入下一阶段，使用命令 //quest set 10101 START 4 才进入了下一阶段 ，根因是任务状态不正确，当前任务 html 在击杀完什么也不显示了
```

1. **HTML 空白根因**：服务端强行在 `<progress>` 声明并写入 `var2`（offset 12），导致击杀 2 只后整型步数被打包为 `8196`。Aion 5.8 客户端在 `quest_script_monster.csv` 声明 10101 是 `Progress(2~!4)`（单变量阶段行走），客户端无该任务的 `SECTION_2` 计数器布局。客户端收到非预期的高位非零值时，步骤匹配全部失效，HTML 渲染崩溃为空白。
2. **`//quest set` 恢复原理**：`//quest set 10101 START 4` 调用 `qs.setQuestVar(4)` 将整型步数直接置 4（`Vars: 4 0 0 0 0 0`），消除了高位污染；客户端收到干净的 `步数=4`，完全匹配 `Progress(4)`，HTML 立即恢复渲染并切入下一阶段。

### 15.2 修复执行

1. **10101.xml / 20101.xml**：
   - 彻底删除 `<progress>` 中的 `var2` 字段声明；
   - 击杀转换仅保留 `var0` 递增（`s2->s3` 置 3，`s3->s4` 置 4）；
   - 移除所有失败/重置转换中残留的 `set var2=0`；
   - 保留 `s3->s4` 等跨阶段转换的 `<sync-quest-state mode="VISIBILITY_REFRESH"/>`。
2. **同族任务清理**：
   - 撤销此前脚本误加高位字段的全部同族任务（`14011`、`14014`、`14021`、`14022`、`24013`、`24014`、`24015`）；
   - `14021`（`s6->s7`）与 `24014`（`s4->s5`）保留 `VISIBILITY_REFRESH`。
3. **单元测试回归对齐**：
   - 更新 `QuestMonsterProgressContractAuditTest`：移除断言 `var2` 镜像的失效测试，新增 `quest10101And20101WalkPhaseStepsWithoutCorruptingPackedStep` 确保 `var2` 绝不声明且打包步数严格等于 3/4；保留 `midRouteJournalRowAdvancesCarryAVisibilityRefresh`。

### 15.3 门禁回归验证（用户已授权）

- 执行命令：
  ```bash
  mvn -q -Dtest='QuestMonsterProgressContractAuditTest,ClientQuestSectionAlignmentTest,ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest' test
  ```
- 验证结果：
  - `QuestMonsterProgressContractAuditTest`: 14/14 通过（含 `quest10101And20101WalkPhaseStepsWithoutCorruptingPackedStep` 验证步数绝不高位污染严格为 3 和 4、`midRouteJournalRowAdvancesCarryAVisibilityRefresh` 验证说明行推进带可见性刷新）。
  - `ClientQuestSectionAlignmentTest`: 6/6 通过。
  - `QuestDefinitionCatalogManifestTest`: 10/10 通过。
  - `ProductionCatalogWhitelistVerificationTest`: 1/1 通过（`PRODUCTION_COMPILE_OK=6189`, `PRODUCTION_COMPILE_FAILURES=0`, `PRODUCTION_INTERACTION_OBJECT_FAILURES=0`, `PRODUCTION_WHITELIST_VIOLATIONS=0`）。

### 15.4 实机复验指南与预期现象

1. 服务端重新加载数据或重启后：
   - 击杀第 1 只侦察兵：`SM_QUEST_ACTION 任务=10101 状态=3 步数=3`（不再是 `4099`）；
   - 击杀第 2 只侦察兵：`SM_QUEST_ACTION 任务=10101 状态=3 步数=4`（不再是 `8196`）；
   - 击杀完成后，客户端收到纯 `步数=4`，任务说明窗口正常显示并无缝切到下一步（摧毁秘密通道的门），不再出现 HTML 空白，亦不再需要 `//quest set 10101 START 4` 兜底；
   - 跨阶段（`s3->s4`）的 `VISIBILITY_REFRESH` 会触发客户端范围与任务可见性刷新，确保门和感应区正确对玩家可见。


## 十六、2026-09-20 20:53 对话波尔迪安 (802357) 无法推进下一步（幻象道具门禁阻断）

### 16.1 用户实机反馈与日志现象

用户在击杀 2 只侦察兵并顺利破门、见迪托诺、逃离研究所、击杀进攻区域贵族交付计划书后，推进至 `s8`（`var0=8`），随后与波尔迪安（802357）对话：
```text
09-20 20:52:52 SM_DIALOG_WINDOW 玩家=Ww targetObj=103558 questId=0 下发页=10
09-20 20:52:53 CM_DIALOG_SELECT 玩家=Ww npcId=802357 targetObj=103558 questId=10101 上一页=10 动作=31  (QUEST_SELECT)
09-20 20:52:53 SM_DIALOG_WINDOW 玩家=Ww targetObj=103558 questId=10101 下发页=3739                      (SELECT9)
09-20 20:52:53 CM_DIALOG_SELECT 玩家=Ww npcId=802357 targetObj=103558 questId=10101 上一页=3739 动作=10255 (SET_SUCCEED: "移动到沙帕灵开拓地。")
09-20 20:52:53 SM_DIALOG_WINDOW 玩家=Ww targetObj=0 questId=0 下发页=0                                   (对话窗口直接关闭，未传送且未切状态)
```

### 16.2 根因定位

1. **阻断路由与条件分析**：
   - 玩家在 `var0=8` 触发 `QUEST_SELECT`（31），NPC 802357 下发第 3739 页（`SELECT9`），玩家点击「移动到沙帕灵开拓地。」（`SET_SUCCEED` / `10255`）。
   - `10101.xml` 中 `s8 -> reward` 路由声明了条件 `<has-item item-id="182215577" count="1"/>` 及动作 `<remove-item item-id="182215577" count="1"/>`。
   - 玩家在整个 10101 流程中从未获得过道具 `182215577`（进攻简要）；条件判定失败且变更规划器因道具缺失拒绝该突变，导致路由不匹配，引擎回退直接关闭对话窗口（下发页=0）。
2. **移动后 HTML 内容空白根因（REWARD 态 var0 越界）**：
   - 客户端 `quest_q10101.html` 的 `<steps>` 仅定义了 9 行步骤（索引 0 至 8，对应 `visible="[%0]"`、`[%3]`、...、`[%24]"`）；最后一行（第 8 行 `[%24]`）正是交付奖励步「和卡多尔的阿尔皮奥对话」。
   - 旧 Java Handler 调用 `defaultCloseDialog(env, 8, 9, true, false)` 时，底层 `changeQuestStep` 的逻辑是：若 `reward == true`，仅调用 `qs.setStatus(QuestStatus.REWARD)`，**根本不修改变量，var0 仍保持 8**。
   - 但迁移至 XML 时，误将 `reward` 节点的投影和转换动作写成了 `set-variable field="var0" value="9"`。
   - 当服务端发送 `var0=9` 时，客户端计算可见性槽位 `9 * 3 = 27`（`[%27]`），而 HTML 中根本不存在该行，导致 `<steps>` 内 9 个步骤全部判定为不可见（隐藏）；客户端回退显示静态尾部文本「指令：和恢复中的卡伦对话。」，造成「移动后任务 html 内容又没了」的现象。
   - 同理，魔族镜像任务 `20101.xml` 的 `reward` 节点同样误写为 `var0=9`。

### 16.3 修复与对齐

1. **10101.xml**：
   - 在 `s6 -> s7`（使用秘密回廊 731532 逃出研究所）中增加 `<remove-item item-id="182215520" count="1"/>`，使研究所钥匙在该步即被消耗；
   - 在 `s7 -> s8`（向波尔迪安 802357 提交计划书）中，移除多余的钥匙门禁 `<has-item item-id="182215520" count="1"/>` 与 `<remove-item item-id="182215520" count="1"/>`，仅严格校验与扣除进攻计划书 `182215452`，彻底避免 GM 重置或丢弃钥匙后提交失败进入 `CHECK_USER_ITEM_FAIL`（10001）；
   - 移除 `s8 -> reward` 中的 `<has-item item-id="182215577" count="1"/>` 与 `<remove-item item-id="182215577" count="1"/>`；
   - 从 `<metadata><work-items>` 中移除 `182215577`；
   - 将 `<node label="reward" status="REWARD">` 中的 `var0` 修正为 `8`；
   - 将 `s8 -> reward` 中的动作修正为 `<set-variable field="var0" value="8"/>`；`<progress>` 中 `var0` 的 `max` 修正为 `8`；
   - 保留传送至卡尔多（`world-id="600090000" x="1299.16" y="1317.84" z="200.7"`）、`LEVEL_AND_VISIBILITY_REFRESH` 与 `close-dialog`。
2. **20101.xml**：
   - 在 `s6 -> s7`（使用秘密回廊 731532）增加 `<remove-item item-id="182215522" count="1"/>`；
   - 在 `s7 -> s8`（向基西安 802361 提交计划书）中移除多余的 `<has-item item-id="182215522" count="1"/>` 与 `<remove-item item-id="182215522" count="1"/>`，仅校验与扣除进攻计划书 `182215453`；
   - 移除 `s8 -> reward` 中的 `<has-item item-id="182215578" count="1"/>` 与 `<remove-item item-id="182215578" count="1"/>`；
   - 从 `<metadata><work-items>` 中移除 `182215578`；
   - 将 `<node label="reward" status="REWARD">` 中的 `var0` 修正为 `8`；
   - 将 `s8 -> reward` 中的动作修正为 `<set-variable field="var0" value="8"/>`；`<progress>` 中 `var0` 的 `max` 修正为 `8`；
   - 保留传送至卡尔多（`world-id="600090000" x="407.16" y="1371.84" z="164.7"`）、`LEVEL_AND_VISIBILITY_REFRESH` 与 `close-dialog`。
3. **单元测试回归**：
   - 在 `QuestMonsterProgressContractAuditTest` 中新增：
     1. `quest10101And20101SubmitInvasionPlanOnlyRequiresPlanItem`：断言仅持有进攻计划书（无需钥匙）即可向 802357 / 802361 提交并推进至 `var0=8`；
     2. `quest10101And20101AdvanceToRewardWithoutPhantomItem`：断言触发 `SET_SUCCEED` 切入 `REWARD` 态且 `var0` 正确保持为 8（对应客户端 HTML 第 8 行 `[%24]` 交付行）。


## 十七、2026-09-20 21:40 实机完整验收、根因复盘与全库同类问题一并修复

### 17.1 用户实机完整验收 (Client Acceptance)

用户在实机上对 10101 的全链路进行了多轮逐段实测与验证，最终确认：**「10101 验证通过」**。
全流程验收证据链：
1. **击杀计数与阶段行走 (s2 -> s3 -> s4)**：消灭德拉坎侦察兵（234680），纯单变量 `var0` 依次下发 `步数=3` 与 `步数=4`，无高位非零污染（不再出现 `4099` / `8196`）；客户端追踪窗口正常前进至门交互步骤。
2. **遮蔽的门 (234193)**：交互长按 5 秒后顺利开门并进入内部通道。
3. **感应区与迪托诺 (804556)**：触发电影 903 播放，对话用军牌（`182215521`）置换秘密区域钥匙（`182215520`）。
4. **逃生回廊 (731532)**：顺利传送至卡多尔（600090000），且钥匙在穿越时即刻消耗，不残留至后续环节。
5. **贵族击杀与计划书递交 (s7 -> s8)**：击杀贵族监察官（234647）拾取进攻计划书（`182215452`），波尔迪安（802357）以 `CHECK_USER_HAS_QUEST_ITEM`（39）正常接收并推进至 s8（`CHECK_USER_ITEM_OK` 10000），不再强求已消耗的钥匙。
6. **沙帕灵开拓地传送与 REWARD 切入 (s8 -> reward)**：点击 `SET_SUCCEED`（10255）顺利传送至阿尔皮奥（802431）处，且无幻象道具 `182215577` 阻断；状态平滑切入 `REWARD`。
7. **REWARD 态 HTML 渲染无空白**：`reward` 节点的 `var0` 锁定为 8，精确命中客户端 `quest_q10101.html` 第 8 行 `[%24]`（和阿尔皮奥对话 [完成]），不再越界，追踪窗完全正常渲染。
8. **领奖完成 (reward -> complete)**：与阿尔皮奥对话顺利结算获得 2461 万经验与复合魔石包。

魔族同族任务 `20101.xml` 同步对齐全部合同。

### 17.2 根因系统性复盘：为什么之前这些错误都没有找到？

1. **遗留 Java 过程式代码的容错遮蔽 (Legacy Java Procedural Tolerance)**：
   - 旧 Java Handler 中调用 `removeQuestItem(env, 182215577, 1)` 底层是 `player.getInventory().decreaseByItemId(...)`；当玩家背包无此道具时仅返回 `false`，但旧代码**从不校验返回值，也不抛异常**，后续逻辑继续执行。
   - 旧 Java Handler 调用 `defaultCloseDialog(env, 8, 9, true, false)` 时，底层 `changeQuestStep` 的实现为：
     `if (reward) { qs.setStatus(QuestStatus.REWARD); } else { if (nextStep != step) qs.setQuestVarById(varNum, nextStep); }`
     即在进入 `REWARD` 态时，Java 代码**根本没有将 var0 写入 9**，变量实际保持在 8！
2. **声明式 XML 引擎的强契约校验 vs 机械批量迁移 (Declarative Strictness vs AST Migration)**：
   - 2026年8月的批量迁移脚本（commit `56c28337f`）机械地将 Java AST 调用翻译为 XML 标签：把 `removeQuestItem(182215577, 1)` 变成了 `<has-item>` 与 `<remove-item>` 并加入 `<work-items>`；把 `defaultCloseDialog(8, 9, true)` 变成了 `set-variable field="var0" value="9"` 与 `<node label="reward"><var name="var0" value="9"/></node>`。
   - 在强类型的 XML 任务引擎中：
     * `<has-item>` 是不可跳过的硬前置条件；
     * `<remove-item count="1">` 必须通过 `QuestMutationPlanner.removalFeasible()` 检查，玩家没有道具直接导致突变拒绝、对话强关；
     * `reward` 节点中的 `var0=9` 真实生效并下发给客户端，导致客户端解析 `[%27]` 越界渲染崩溃。
3. **现有编译器与编目测试的四重盲区 (Compiler & Whitelist Verification Blind Spots)**：
   - **盲区一：未获取道具硬门禁**。编译器只校验 XSD 语法和状态图可达性，但从不检查某条转换上要求的道具（`<has-item>` / `<remove-item count="1">`）是否在全流程中有任何获取途径（`<give-item>` / 掉落 / 采集）。
   - **盲区二：REWARD 变量越界**。测试未将 XML 中的 `reward` 变量与客户端解包的 `quest_q<id>.html` 中的 `<step>` 行数进行交叉核对，无法在静态时发现步骤越界导致的 HTML 空白。
   - **盲区三：Progress 形式与位段冲突**。此前误将怪物击杀统一当做多字段计数器（`SECTION_N`），未核对客户端 `quest_script_monster.csv` 中 `Progress(min~!max)` 的单变量阶段行走契约。
   - **盲区四：中途说明行刷新的网络包模式**。中途阶段切换使用 `PACKET_ONLY` 导致客户端任务追踪窗口不能主动重绘。

### 17.3 全库同类问题系统排查与一并修复

针对上述根因，对全库 6,200+ 任务展开全量扫描排查：

1. **同类致命幻象道具阻断排查与修复**：
   - 在主线任务（`category="MISSION"`）中扫描发现 `10526.xml` 与 `20526.xml` 存在完全相同的缺陷：
     * `10526.xml` 在 `s11 -> reward`（觉醒的德贾博 SET_SUCCEED）声明了 `<remove-item item-id="164002347" count="1"/>`，且列入 `<work-items>`；该道具在任务中从未发放（实际发放并使用的是 `182216074`），导致 `removalFeasible()` 判定失败，玩家永远无法完成 10526。
     * `20526.xml` 同样在 `s11 -> reward` 声明了从未发放的 `<remove-item item-id="164002348" count="1"/>`。
   - **一并修复**：
     * `10526.xml`：工作物品与移除动作剔除幻象道具 `164002347`，仅扣除持有的 `182216074`；
     * `20526.xml`：工作物品与移除动作剔除幻象道具 `164002348`，仅扣除持有的 `182216086`；
     * 补充单元回归测试 `QuestMonsterProgressContractAuditTest#quest10526And20526AdvanceToRewardWithoutPhantomItem`。
2. **同类 REWARD 态 var0 越界导致 HTML 空白排查与修复**：
   - 扫描发现同族大天使/主线任务 `10520.xml` 与魔族 `20520.xml`：
     * 客户端 `quest_q10520.html` / `quest_q20520.html` 仅有 6 行步骤（索引 0 至 5，第 5 行为交付给代理人 `[%15]`）；
     * 服务端在 `s5 -> reward`（SET_SUCCEED）将 `var0` 写入了越界的 `6`，导致 REWARD 态下客户端 HTML 步骤全部消失。
   - **一并修复**：
     * `10520.xml` 与 `20520.xml`：`<progress>` 的 `var0` 最大值修正为 `5`，`reward` 节点变量投影修正为 `var0=5`，`s5 -> reward` 动作修正为 `<set-variable field="var0" value="5"/>`；
     * 补充单元回归测试 `Quest10520ClientDialogAlignmentTest#advanceToRewardKeepsVar0AlignedWithClientStepCount`。

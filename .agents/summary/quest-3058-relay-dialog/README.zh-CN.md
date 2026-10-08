# 任务 3058 中继对话面修复（798189 · 与 Oileus 对话后任务书步骤消失）+ 同型面全量排查

日期：2026-10-08（实机玩家报障 / 任务 3058 马波路之石 / NPC 798189 Oileus）
状态：**已验收**（2026-10-08 用户实机验证成功；修复提交 `f84393452`、沉淀 `98ba6fc69`；验收记录 [3058-2026-10-08-client-accepted.md](../quest-acceptance/3058-2026-10-08-client-accepted.md)）

## 现象（用户实机报告）

- 使用 ITEM_QUEST_3058A（182208041）接取后，与 npc798189（Oileus，talk_npc1）对话 →
  **任务列表（任务书）步骤整块空白**。
- 预期 trace 形态（与 14120 同型）：接取后 `SM_QUEST_ACTION 任务=3058 状态=3 步数=0`；
  与 798189 交互后 `步数=65536`（bit16..17 私编），客户端按打包整数匹配 steps 行落空。

## 根因（两层，与 14120 完全同型；QE-141 boundaries 点名的未修残留）

1. **步号私有编码污染客户端步骤轴**：`SimpleUseItemHandler.talkChainStep` 把中继步写
   vars bit16..17（step 1 → 65536）。客户端任务书按打包整数匹配 steps 行 ⇒ 65536 无匹配
   ⇒ 步骤显示为空（10101:8196 / 14120:65536 / 本任务同机制）。
2. **任意动作一律推进**：不区分「任务行打开 / 翻页 / 推进」，与中继 NPC 对话即推进
   （步页 1352/1353/1693/1694 全缺，点击即跳步）。

## 权威证据

- 真端 SimpleUseItem 表行 3058：`use_item_name=ITEM_QUEST_3058A`、`talk_npc1=Oileus`、
  `talk_npc2=Lavirintos`、`remove_item2=ITEM_QUEST_3058A 1`、`reward_npc_name=Siraus`。
- 静态数据解析（与退役 XML 逐一对上）：Oileus→**798189**、Lavirintos→**203701**、
  Siraus→**798213**（各唯一；798189 即用户报障 NPC）。
- 退役 XML 3058（`git show 4ede058c0^:.../quest_definition/quests/3058.xml`）：
  started+798189 `SETPRO1`→v1（**var0=1**）；v1+203701 `SETPRO2`→v2（**var0=2**，remove 3058A）；
  v2+798213 → reward；每步 after-commit = sync + close-dialog。
- 客户端任务书三行（`QUEST_Q3058.html`）：
  `[%0]`→Oileus / `[%3]`→Lavirintos / `[%6]`→Siraus（槽位 = 3×行号）；
  对话契约声明 1352/1353（select2/select2_1）、1693/1694（select3/select3_1）、2375（select5）。
- 同型母本：14120 修复 `d12e4236e`（SimpleCollectItem 族，实机验收通过）。

## 修复（提交 f84393452）

- `SimpleUseItemHandler`：
  - `talkChainStep` → `onRelayDialog`（与 SimpleTalk/SimpleItemPlay/SimpleCollectItem 同形）：
    31/26/-1 → 该步页（pageForStep = 1352/1693/2034，带 questId；未轮到的步零响应；
    **链满让位交付面**——1559 的 talk_npc1 = reward_npc，链满后点任务行必须给报告页 2375）；
    SETPRO{K}（10000+K−1）→ 该步 give/remove + `var0 = K` + `SM_QUEST_ACTION` + 关窗
    （真端 cabb10 0x5d8、零发页；顺序 = 状态→包→物品→关窗，与已验收母本同序）；
    重复/乱序重放 → 关窗兜底；子页动作（`isSelectionSubPage` × 契约声明）原样回发。
  - `relayStep` 改读 var0（兼容读旧 bit16..17）；写入一律 var0。
  - 新增 `onEnterWorld` 自愈：旧编码（bit16..17 非零且 var0=0）归一为 var0 并重发状态；
    `QuestEngine.onEnterWorld` 接线（用户已落盘存档 65536 → 1）。
- 测试 `SimpleUseItemNativeFamilyGateTest`：
  - 主链用例改写为 QE-141 形态（`relayChainServesStepPagesAndAdvancesThroughSetproIntoVar0`：
    31→页/子页回发/10000→var0=1→…→第 3 步换物 + **链满让位**断言）；
  - 新增 `quest3058RelayWritesVar0AndRemovesItsItemAtStepTwo`（实机回归：31→1352→1353→
    SETPRO1→var0=1；（203701）31→1693→1694→SETPRO2→var0=2 + 移除 3058A）；
  - 新增 `enterWorldNormalizesTheLegacyStepEncoding`（3058 的 65536→1，干净行不动）。

## 验证

- IDEA MCP（2026-10-08）77 例全绿：`SimpleUseItemNativeFamilyGateTest` 14/14、
  `SimpleCollectItemNativeFamilyGateTest` 19/19、`SimpleTalkNativeFamilyGateTest` 17/17、
  `SimpleItemPlayNativeFamilyGateTest` 16/16、`UseItemFamilyRowAlignmentGateTest` 7/7、
  `QuestProductionStartupGateTest` 2/2、`QuestInteractionObjectContractGateTest` 2/2；
  `git diff --check` 干净；IDE lint 无编译错误。
- 实机验收通过（2026-10-08 用户确认「实机验证成功」）：整链按复测口径验收——① 旧档进世界
  自愈（任务书步骤恢复，`步数=1`）；② 与 Oileus：31→页 1352→1353→「结束对话」→ `步数=1` +
  关窗 + 任务书亮行 1；③ 与 Lavirintos：1693→1694→SETPRO2→`步数=2` + 移除 3058A；④ 向 Siraus
  报告领奖。逐步 trace/截图 not captured；验收记录见
  [3058-2026-10-08-client-accepted.md](../quest-acceptance/3058-2026-10-08-client-accepted.md)。

## 同型面全量排查（2026-10-08，随本次修复）

判据（`questEngine` 全库）：① 给客户端的 step 值**不得含客户端未声明的位**（高危：任何
bit6+ 未声明位 → 任务书步骤匹配落空 = 步骤空白）；② 步号写 **var0（低 6 位）**；
③ 中继推进须有 SETPRO{K} 动作门（禁「任意动作推进」）。

扫描面：全部 `getQuestVars().setVar/setVarById` 写入点 + 22 个 `new SM_QUEST_ACTION(...)` 发包点
（9 个文件）。

| 面 | 结论 |
|---|---|
| SimpleTalk 中继链 | ✓ 写连续 step；自愈写 relayCount；动作门已有 |
| SimpleItemPlay 中继链 | ✓ 写连续值 + SETPRO 动作门 + 子页回发 + 自愈（QE-141 形态） |
| SimpleCollectItem 中继链 | ✓ 已修（`d12e4236e`，14120 实机验收） |
| **SimpleUseItem 中继链** | **本次修复**（bit16 私编 → var0 + 动作门 + 链满让位 + 自愈） |
| SimpleHunt / SimpleSerialHunt 相机计数 | ✓ `ProgressCamera` → `RawQuestVarsCodec`（6/10 位槽、守卫位 fail-closed） |
| DataDriven 车道 | ✓ `DataDrivenProgress`：步号 bits0-5、组槽 bits6+、`guardClear` 校验、REWARD 发 0 |
| SimpleCombineTask | ✓ 交付只翻状态，vars 原样（var0 恒 0） |
| NativeQuestStartPort | ✓ `setVar(0)` 清零 |

**结论：bit16..17 私编面与「任意动作推进」面已全部清零**（历史仅 SimpleCollectItem 与
SimpleUseItem 两处，均已收口）。

### 待实机观察候选（登记，不改代码）

`SimpleSerialHuntHandler` 简报位下发：接取带简报的行（全表仅 **9622/30600/30610** 三行）时
置 `vars=0x40000000` 并下发 `SM_QUEST_ACTION(step=0x40000000)`；向简报 NPC 对话后清 0。

- 依据链：bit30 偏移来自真端证据（真端相机代码显式 `vars < 0x40000000` 检查——
  `.agents/summary/quest-engine-native/p0a/semantic-matrix-camera-channels.md`；
  `RawQuestVarsCodec.GUARD_BITS`），但「简报语义 = CONVENTION_ONLY」（P1 spike）；P0a/P1
  raw-vars 实测样本 high_bit 计数为 0（未见真端样本带 bit30）。
- 风险：若客户端对 bit30 无特判（与 65536 同型），简报窗口（接取后→找简报 NPC 前）任务书
  步骤会空白。30600 任务书行 0 = 「和 Linocus 对话」（槽位 `[%0]`）——该行是否在简报前显示
  即判定点。
- 不改的理由：bit30 是真端协议守卫区（真端代码证据），「简报前渲染」的真端行为未知；
  贸然剥离位可能偏离真端。
- 观察口径：实机接取 30600（德雷得奇安事件，简报 NPC = Linocus 800324）或 9622/30610，
  接取后立即看任务书步骤栏是否显示「和 Linocus 对话」；与简报 NPC 对话清位后是否恢复正常
  步骤。若确认空白，再按证据修复（候选：接取置位只落服务端存档、下发前按真端实证口径处理）。

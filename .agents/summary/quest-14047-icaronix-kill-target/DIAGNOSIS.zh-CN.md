# 任务 14047 击杀目标错位：本服听 214599，真端注册在 233877（2026-10-07）

## 现象

实机（Elyos 任务 14047《Chaining Memories》第 5 步，副本 Azoturan Fortress 310100000）：
玩家击杀副本里实际刷出的 Boss **233877**（客户端名「背叛者伊卡罗尼斯」，任务飞行路径落点）
后任务不推进。`log/quests.log` 2026-10-07 23:33:47（步数=5）→ 23:35:02（GM `reset instance`
后回落到步数=3）之间无任何 14047 出包，也无击杀痕迹（服务端无击杀日志，无法回放是否出过第二形态）。

## 真端证据（权威）

1. **脚本注册面**：`<真端根>/server58/MainServer_ScriptDLL64/ScriptDLL64.c:1887148`
   `FUN_180cb5920(&DAT_186385e60, L"IDLF3_Castle_Lehpar_LehparIcaronixQ_45_Q_Ae", 0x36df)`；
   0x36df = 14047。全文件 41 处 0x36df 的 NPC 名只有 7 个
   （Boreas / Gaix / IDLF3_Castle_Lehpar_Acestes_E / **IDLF3_Castle_Lehpar_LehparIcaronixQ_45_Q_Ae** /
   IDLF3_Castle_Lehpar_Peitho_E / Juditio / Kalendros），**无 214599、无 214598**。
   本仓同源证据表 `src/main/resources/aion/data/static_data/quest/retail/retail-quest-ai-registrations.xml:1802-1805`
   （生成器 `.agents/summary/quest-engine-native/p11-quest-ai-lane/emit_quest_ai_registrations.py`）同形。
   另：`ScriptDLL64.c:1081721`（0xdca=3530）、`:1081733`（0x11ae=4526）把
   `IDLF3CL_LehparIcaronixQ_45_Ah`（=214598）注册给 **3530/4526**。
2. **真端 AI pattern**（`<真端根>/Map/XML/NpcAIPatterns.xml`）：
   - `D2_FnA`（233877 的 pattern）`<event_handlers>` **为空** —— 城堡 Boss 无任何变身/生成。
   - `ND2_AhC_1`（214598 的 pattern）才有变身：`on_battle_timer`（HP<75%）→ 生成
     `IDLF3CL_TestResultIcaronixQ_45_Ah`（=214599）+ `despawn_self`；`on_die` 同形生成。
   - `NLehpar_BhB`（214599 的 pattern）`on_killed_by_user`：despawn 召唤物、说
     `STR_CHAT_IDLF3Lephar_TestResultReal_50_Ah_AIPattern_2`、生成史莱姆。
3. **SimpleHunt 合同快照**：`src/test/resources/quest/quest-simple-hunt-retail-contract.tsv:554,658`
   —— 3530/4526 的目标 id 组就是 `214598 214599`（独立佐证 214599 属于那两条计数链）。
4. **副本世界数据**（`<真端根>/Map/Worlds/idlf3_castle_lehpar/world.xml`）：
   `IDLF3_Castle_Lehpar_LehparIcaronixQ_45_Q_Ae`(233877) 在 (478.81, 431.13, 1062.98)；
   `Named_N_IDLF3CL_LehparIcaronixQ_45_Ah`(214598) 在 (460.70, 440.07, 993.94)（另一层）。
   任务的 `flight-teleport 72001` 终点 (474.19, 429.22, 1062) ≈ 233877 的刷点。
5. 客户端名：233877/214598 = 背叛者伊卡罗尼斯；214599 = 背叛的伊卡罗尼斯（不同名）。

## 本服偏差点

- `quest/definitions/quests/14047.xml` 的 `s5→s6` 沿用 Aion-Unique 时代的 `kill-npc 214599`
  （退役 XML 同值，属「实现与退役 XML 一致、但与真端不一致」情形）。
- 为让 214599 可达，`npc_template_216189_235748.xml:65296` 给 **233877** 也挂了
  `ai="betrayer_icaronix"`（`Betrayer_IcaronixAI2`：75%/死亡生成 214599）。
  真端里该 AI 语义（`ND2_AhC_1`）属于 214598 —— `npc_template_200000_216188.xml:76972`
  已正确挂载，本服 233877 是重复/越界挂载。
- 后果：只有杀掉 AI 造出的第二形态 214599 才会推进；直接击杀任务指向的 233877 不推进。
  8 月 17 日那次验收（`8b058d4b4`）走的就是这条人造路径。

## 修复（2026-10-07，真端口径）

1. `quest/definitions/quests/14047.xml`：`s5→s6` 击杀目标改为 **233877**（附真端证据注释）。
2. `npc_template_216189_235748.xml`：233877 的 `ai` 改为 `aggressive`（真端 `D2_FnA` 空 pattern；
   否则 75% 自删不产生击杀事件）。
3. `Betrayer_IcaronixAI2` 只服务 214598（类注释已标明归属）。
4. 测试重锚：`Quest14047ClientDialogAlignmentTest`（击杀目标常量 + 出生点断言：233877 静态 1 只、
   214599 静态 0 只、定义不得再监听 214599）、`QuestMovieAndDialogLoopRegressionTest`
   （`quest14047OnlyPlaysMovieOnActualKillStage` 的 id 改为 233877）。

## 验证

IDEA MCP（2026-10-07），全部 exit 0：

```
Quest14047ClientDialogAlignmentTest               7/7
QuestMovieAndDialogLoopRegressionTest            15/15
ThresholdTransformDeathFallbackGateTest           2/2
Betrayer_IcaronixAI2Test                          2/2
AI2EngineRetailSelectionTest                      3/3
QuestAiDialogBindingGateTest                      2/2
QuestDefinitionDirectoryLoaderTest                2/2（全量目录编译 + 无死胡同 BFS）
QuestKillCounterRetailGateTest                    3/3
QuestKillItemRewardEntryDialogGateTest           13/13
ProductionCatalogWhitelistVerificationTest        1/1（PRODUCTION_COMPILE_OK=707 / 0）
```

实机复测：**ACCEPTED**（2026-10-08 用户重启加载修复构建后实机复测并确认「实机验收成功」；验收记录
`.agents/summary/quest-acceptance/14047-2026-10-08-client-accepted.md`）。复测预期：飞抵城堡 Boss →
击杀 233877 → 发 状态=3 步数=6 + 电影 422，且不再出现第二形态。

## 未决 / 后续

- 214598→214599 链属任务 3530/4526（SimpleHunt 计数）本片未实机核对；如需可专项验。
- 服务端无击杀/动态生成日志，本次无法从日志证明当时是否出过 AI 第二形态；本修复后该路径不再存在。

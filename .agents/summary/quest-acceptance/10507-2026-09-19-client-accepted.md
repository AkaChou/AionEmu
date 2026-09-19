# 任务 10507「A Big Plot to Foil / 支援派遣队」客户端验收记录（ACCEPTED）

```text
quest: 10507「A Big Plot to Foil / 支援派遣队」（ELYOS，min-level 64，category MISSION，前置 10500/10501/10502/10503/10504/10506）
user acceptance confirmation: 用户 2026-09-19 先回复「15006 10507 验证完成」，随后明确回复「10507 验证成功」；
  10507 未限定分支或步骤，按项目规则视为整任务游玩验收。前一条消息中的「15006」在 Aion 5.8 客户端 `quest.xml` 里是
  minlevel/maxlevel=999 的占位条目、生产目录无对应定义，按上下文判读为 10506 的笔误
  （10506 验收记录见 `.agents/summary/quest-acceptance/10506-2026-09-19-client-accepted.md`）；本次用户已单独明确 10507，本条不再依赖该判读
server launch mode: 用户管理的服务端实例（本轮未启动、停止或重启服务端）
repository commit: 7d5bb5317（修复提交：10507 s6->s7 两条击杀收口 transition 补 `var1=0`/`var2=0`，并新增 s7 两条 `enter-world` 自愈清零路由）；本验收记录在其后的文档提交中
working tree: 记录时本任务源码路径已提交；工作区其余为并行任务产物（`.agents/summary/spawn-z-audit/` 等），不属本任务范围
Aion 5.8 client/data provenance: Aion 5.8 客户端解包数据 Quest_unpacked/quest.xml（id=10507，minlevel 64，无 collect_progress 声明）与
  Quest_unpacked/quest_monster.csv:2864-2865（`Progress(SECTION_0==6; SECTION_1<5)` 对应 236264/236265 系怪物模板，
  `Progress(SECTION_0==6; SECTION_2<3)` 对应 702668）；仓库内对白映射 docs/quest/client-dialog-mapping/（SELECT1..SELECT6 系）
npc template/object: 对话/领奖 804711（布仑太系）、804712、804713、804714、804715；击杀目标 236264/236265（var1 计数）、702668（var2 计数）；感应区 LF5_SENSORYAREA_Q10507_210070000；影片 993；runtime object ID 本轮未采集
map/instance: Cygnea（希哥尼亚 / world 210070000）

steps:
1. 依次与 804711/804712/804713/804714/804711/804715 对话（SELECT1..SETPRO6），把任务推进到 s6（var0=6）。
2. s6 双计数阶段：击杀 236264/236265 累加 var1、击杀 702668 累加 var2，两组计数可任意顺序推进（每次推进下发 `PACKET_ONLY`）。
3. 两组计数达标后的 priority 0 击杀收口进入 s7：修复后 actions 为 `set var0=7` + `set var1=0` + `set var2=0`。
   修复前只写 `var0=7`，残留计数会把打包整型步数污染（例：var1=4、var2=3 时为 `7|(4<<6)|(3<<12)=12551` 而非纯净 7）。
4. 进入感应区 LF5_SENSORYAREA_Q10507_210070000 触发影片 993，`movie-end` 推进 `var0=8` 进入 REWARD。
5. 与 804711 对话领取奖励并完成（npc-complete，SELECTION_DIALOG；固定奖励位 0/7/8 + 6 档选择奖励）。
6. 旧存档自愈：处于 s7 且带 var1>=1 或 var2>=1 残留时，`enter-world` 路由静默清零并下发 `PACKET_ONLY`。

source state/status/vars: s6（var0=6，var1/var2 计数中）-> s7（var0=7，var1=0，var2=0）-> reward（var0=8）
action/page/button: 804711/804712/804713/804714/804711/804715 的 SETPRO1..SETPRO6；kill 236264/236265 与 702668 的 s6 计数/收口；enter-zone 感应区 -> play-movie 993 -> movie-end；804711 SELECT_QUEST_REWARD
expected response: 双计数收口后客户端整型步数保持纯净 7（无 var1/var2 高位残留），s7 感应区与影片 993 正常触发，随后进入 REWARD 并正常领奖完成
actual response: 用户实机游玩确认「10507 验证成功」（随后单独再次明确）；未提供抓包、截图或日志，技术产物记为 not captured

startup health: not captured（服务端由用户管理；本轮未采集启动日志，用户亦未报告 typed quest engine 初始化失败、QuestCompilationException、AMBIGUOUS_TRANSITION 或 production catalog 编译失败）
runtime logs: not captured
protocol trace: not captured
screenshots/recordings and SHA-256: not captured

acceptance status: ACCEPTED
matched Pattern: QE-044 的「跨阶段计数残留污染打包整型步数」臂（MULTI_STAGE_COUNTER_LEAK）；
  matched fields：非计数阶段（s7）的打包整型步数必须纯净、计数在阶段收口处显式清零、旧存档 enter-world 自愈；
  differing fields：10507 无客户端 `collect_progress` 声明，客户端门控是击杀计数（SECTION_1/SECTION_2）与感应区/影片步骤而非交付对白，
  修复前症状是 s7 影片/推进门控不触发而不是 NPC 下发通用第 10 页；
  representative commit 7d5bb5317，representative test QuestCollectProgressAlignmentGateTest#multiStageMissionsResetCounterVarsOnEnteringNonCounterStages
remaining risks:
- 同批 20504、10527/20527、10528/20528、10530/20530、1373 仍未逐任务实机验收（静态门禁与各自契约测试已锁定）。
- 10507 的旧存档自愈（s7 带 var1/var2 残留）未单独构造脏存档实测，仅由契约测试与条件互斥证明。
- 用户早前消息中的「15006」按上下文判读为 10506 笔误；10507 已由用户单独明确验收，若「15006」实际指其他任务仍需用户补正后另行记录。
```

## 证据引用
- 同批根因与修复纪要：`.agents/summary/quest-collect-progress-alignment/README.md`、`.agents/summary/quest-10503/README.md`（10507 清零项）
- 兄弟任务实机验收：`.agents/summary/quest-acceptance/10503-2026-09-19-client-accepted.md`、`.agents/summary/quest-acceptance/10504-2026-09-19-client-accepted.md`、`.agents/summary/quest-acceptance/10506-2026-09-19-client-accepted.md`
- 契约门禁：`QuestCollectProgressAlignmentGateTest#multiStageMissionsResetCounterVarsOnEnteringNonCounterStages`（hybrid 家族 10503/10504/10506/10507/10527/10528/20504/20527/20528）
- 生产 Catalog 门禁：`QuestDefinitionCatalogManifestTest` (10/10 PASS)、`QuestItemSourceContractGateTest` (3/3 PASS)

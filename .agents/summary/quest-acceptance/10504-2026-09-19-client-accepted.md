# 任务 10504「Confiscate the Slate / 没收石板」客户端验收记录（ACCEPTED）

```text
quest: 10504「Confiscate the Slate / 没收石板」（ELYOS，min-level 60，category MISSION，start-conditions = finished 10503）
user acceptance confirmation: 用户 2026-09-19 回复「10504 验证成功」；未限定分支或步骤，按项目规则视为该任务的完整游玩验收
server launch mode: 用户管理的服务端实例（本轮未启动、停止或重启服务端）
repository commit: 7d5bb5317（修复提交：10504.xml 计数清零/采集步放宽/s2-s3 双向交互许可/s3 交付路由 + Quest10504ClientDialogAlignmentTest + QuestCollectProgressAlignmentGateTest）；本验收记录在其后的文档提交中
working tree: 记录时本任务源码路径已提交；工作区其余为并行任务产物（.agents/summary/spawn-z-audit/ 等），不属本任务范围
Aion 5.8 client/data provenance: Aion 5.8 客户端解包数据 Dialogs/10000_19999/quest_q10504.html
  （SHA-256 ab4f8e037a956774cbe08a694098dd93f522fe3d5ae09811d0d0c16d8f897142）与客户端 quest.xml 的 collect_progress=3；
  仓库内映射证据 docs/quest/client-dialog-mapping/quest-dialog-action-details.csv:675-679
  （SELECT1 1011 -> SELECT1_1 1012 -> SELECT4 2034 -> check_user_item_ok 10000 -> check_user_item_fail 10001）
npc template/object: 交付 NPC 阿斯特拉佩 804706；石板交互物 702671；前置击杀 236253/236254（s1 自环）；精英怪 236255（s2->s3）；工作物品 182215606、交付物 182215607；runtime object ID 本轮未采集
map/instance: Cygnea（希哥尼亚 / world 210070000，与 10503 同区；本轮未另采 world ID 证据）

steps:
1. 完成 10503 后接取 10504；与阿斯特拉佩 804706 对话（SELECT1 1011 / SELECT1_1 1012 / SETPRO1 10000）拿到工作物品 182215606，进入 s1（var0=1）。
2. 击杀 236253/236254 直到 var1>=4：每次击杀 var1 自环 +1 并下发 PACKET_ONLY；收口时 priority 0 进入 s2 并显式 set var1=0
   （修复前只写 var0=2，var1 残留 4，4<<6|2 使客户端整型步数变 258）。
3. 击杀精英怪 236255：s2->s3（var0=3）。
4. 与石板 702671 交互（USE_OBJECT）取得 182215607：drop collecting-step=0，s2 与 s3 都保留 can-act/USE_OBJECT
   （修复前写死 collecting-step=2 且只开 s2，先杀精英进 s3 就永久无法采集）；交互后 PACKET_ONLY + close-dialog。
5. 持 182215607 与 804706 对话：客户端 collect_progress=3 命中 s3 的 CHECK_USER_HAS_QUEST_ITEM(39)
   （var0==3 + HasItem(182215607,1)，priority 0）→ 事务内扣道具 + var0=4 → REWARD + DEFAULT_SUCCESS(10000)，随后 SELECT_QUEST_REWARD 打开奖励窗并可完成。
   步骤 3/4 顺序可交换（修复后 s2/s3 双向可采集），两种顺序都不得卡死。

source state/status/vars: s1（var0=1，var1 0->4）-> s2（var0=2，var1=0）-> s3（var0=3，var1=0）-> reward（var0=4）
action/page/button: 804706 SELECT1(1011)/SELECT1_1(1012)/SETPRO1(10000)；kill 236253/236254 -> s1 自环；kill 236255 -> s3；702671 USE_OBJECT -> PACKET_ONLY + close-dialog；804706 CHECK_USER_HAS_QUEST_ITEM(39) -> check_user_item_ok(10000) -> SELECT_QUEST_REWARD(1009)
expected response: 交付步整型步数纯净为 3（无 var1 高位残留），NPC 头顶亮任务标记，交付成功后同一次交互进入 REWARD 并打开奖励窗；石板在 s2/s3 均可采集
actual response: 用户实机游玩确认「10504 验证成功」；未提供抓包、截图或日志，技术产物记为 not captured

startup health: not captured（服务端由用户管理；本轮未采集启动日志，用户亦未报告 typed quest engine 初始化失败、QuestCompilationException、AMBIGUOUS_TRANSITION 或 production catalog 编译失败）
runtime logs: not captured
protocol trace: not captured
screenshots/recordings and SHA-256: not captured

acceptance status: ACCEPTED
matched Pattern: QE-044（COLLECT_PROGRESS_PREMATURE_ADVANCE_DIALOG_DROPPED / MULTI_STAGE_COUNTER_LEAK）；
  matched fields：collect_progress=3 的交付点必须落在 var0==3、跨阶段计数清零保证打包步数纯净、采集步在 s2/s3 双向开放；
  differing fields：10504 比 10503 多一条「s2/s3 双阶段可采集」的死锁防护（模式 C 采集死锁），10503 的抢跑点在击杀收口本身；
  representative commit 7d5bb5317，representative test Quest10504ClientDialogAlignmentTest#slateCollectionAndHandoverContract
remaining risks:
- 同批同因的 20504、10506、10507、10527/20527、10528/20528、10530/20530、1373 已由 QuestCollectProgressAlignmentGateTest 与各自契约测试锁定，但未逐任务实机验收。
- 旧存档自愈（enter-world 清零残留 var1）未单独构造脏存档实测，仅由契约测试与条件互斥证明。
```

## 证据引用
- 根因溯源与同批修复纪要：`.agents/summary/quest-collect-progress-alignment/README.md`
- 兄弟任务实机验收：`.agents/summary/quest-acceptance/10503-2026-09-19-client-accepted.md`
- 契约测试：`Quest10504ClientDialogAlignmentTest#slateCollectionAndHandoverContract`、`QuestCollectProgressAlignmentGateTest#quest10504Alignment`、`#multiStageMissionsResetCounterVarsOnEnteringNonCounterStages`
- 生产 Catalog 门禁：`QuestDefinitionCatalogManifestTest` (10/10 PASS)、`QuestItemSourceContractGateTest` (3/3 PASS)

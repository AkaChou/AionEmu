# 任务排查与系统性修复记录（2026-09-17）

## 1. 称号前置自指死锁（Bootstrap Deadlock）彻底排查与解除
- **排查范围**：全服 6,222 个任务 XML 与真端客户端 。
- **排查发现**：
  - 真端客户端中实际仅 29 个任务具有 title 前置要求（均为军衔或特殊资格）。
  - 服务端仅 3 个任务配置了 title-id：10521（天族阿斯特拉 65 级主线“回忆的永恒”）、20521（魔族阿斯特拉 65 级主线“找回的命运”）、2511（魔族“出人头地的野心”）。
  - 这 3 个任务全部将自身通关发放的称号（306“记忆的继承者”、75“暗夜之印”）错误配为了接取前置，导致新升 65 级玩家无法接取主线。
- **修复与守卫**：
  - 移除 10521、20521、2511 中的 title-id 约束。
  - 在 QuestRewardTitlePrerequisiteAuditTest 中增加 noQuestRequiresItsOwnRewardTitle() 全量门禁测试，严防未来任何任务误将自身奖励称号配为前置。

## 2. 道具使用任务拒绝动作补齐（BUTTON_WITHOUT_ROUTE 销项）
- **涉及任务**：1197, 1198, 80008, 80009。
- **病灶**：使用道具弹出 page 4 接受窗口时，点击拒绝按钮客户端发送 action 1008（HACTION_FINISH_DIALOG）。原 XML 仅声明了 QUEST_REFUSE_1 (1003)，缺失 FINISH_DIALOG。
- **修复**：将拒绝动作扩充为 actions="QUEST_REFUSE_1 FINISH_DIALOG"，支持客户端全部关闭/拒绝行为。

## 3. 接取后 page 1003 按钮动作闭环路由补齐
- **涉及任务**：2289, 2367, 2411, 2443, 2448, 2922, 3088, 3936, 3937, 4940, 4941（共 11 个任务、18 条缺失路由）。
- **病灶**：在 unaccepted -> started 接取后下发 page 1003 (QUEST_ACCEPT_1)，客户端展示的后续按钮在 started 状态下无对应 NPC 路由，导致对话卡死或客户端契约违规。
- **修复**：
  - 2289：NPC 203616 点击 SETPRO1 (10000) 结束对话 (close-dialog)。
  - 2367、2411、2448、2922：发任务的主 NPC 补齐 SELECT1_1、SELECT1_2 分支展示页以及 SETPRO10、SETPRO20 分支记录与关闭路由。
  - 2443：NPC 204403 补齐 SETPRO1 / QUEST_ACCEPT_1（准备好了）以及 SELECT1_1_1（还没准备好）闭环。
  - 3088：NPC 798202 补齐 SELECT1_1 / SELECT1_2 / SELECT1_3 果汁选项及 SELECT1 重新考虑、SETPRO1/3/7 提交果汁路由。
  - 3936、3937：NPC 203710 / 203708 扩展 actions="QUEST_SELECT SELECT1" 响应 1003 上的“继续听”。
  - 4940、4941：NPC 204050 / 204060 补齐 SELECT1（继续听/点头）与 SETPRO1（结束对话）路由。

## 4. 客户端契约基线文件完全清零
- src/test/resources/quest/quest-client-contract-baseline.tsv 中历史残留的 22 条违规全部清零，客户端契约门禁达到 0 缺陷纯净状态。

## 5. 多目标击杀任务契约缺失与变量脱节排查修复（15324, 50091, 50092）
- **病灶分析**：
  - 客户端契约 client-monster-progress-contracts.csv 要求多杀怪任务将步骤保留在 SECTION_0 (var0)，将实时杀怪计数器放在 SECTION_1 (var1)。
  - 全量审计发现 15324（天族 65 级龙界守护）、50091（46-50 级库木奇藏身处活动）、50092（51+ 级库木奇藏身处活动）在客户端要求击杀（分别要求 20 只与 15 只），但服务端却漏配了杀怪流转逻辑，导致接取后直接可向 NPC 领奖或杀怪不涨计数。
- **修复方案**：
  - 15324：全面对标魔族对应任务 25324，构建完整的 3 阶段守护击杀结构（希哥尼亚 20 只 -> 哥尔哈 20 只 -> 卡多尔 20 只 -> 玛琪纳 805331 汇报领奖），严格分离 var0（步骤 0..3）与 var1（击杀计数 0..20）。
  - 50091 & 50092：构建库木奇藏身处 15 只人参怪（246293）击杀流转，分离 var0（步骤）与 var1（击杀计数 0..15），杀满后向天族 835680 / 魔族 835681 领奖。
  - 在 QuestMonsterProgressContractAuditTest 中新增 repairedMultiKillQuestsDeclareSeparateKillCounters 门禁，锁死变量分离契约。

## 6. 全服状态图死胡同（无下一步）与击杀后报告源错位彻底根治（1640, 2569, 16900-16903, 24151）
- **全服拓扑分析发现**：
  - 基于全量图遍历算法对 6,186 个生产执行任务进行全图 BFS 连通性分析，发现了 7 个存在可达死胡同（Reachable Dead End）的严重孤例任务：
    - 16900, 16901, 16902, 16903：击杀怪物后状态正常流转至 k1，但 NPC_REPORT 的 source 却误配为初始状态 started，导致玩家击杀完成后向目标 NPC 报告时 NPC 完全无响应、无法领奖；
    - 24151：通过 kill-chain 击杀 5 只怪物后状态正常流转至 k5，但 NPC_REPORT 的 source 同样误配为 started，击杀完成后无法领奖；
    - 1640：reward 状态节点无任何流出转移，若玩家进入 reward 态则无法安装部件完成任务；
    - 2569：虚设了无任何流出边的 s2 节点，且存在跳转至 s2 的 SETPRO2 动作，导致进入 s2 后永久死锁。
- **全局修复与长效门禁**：
  - 将 16900-16903 的 NPC_REPORT source 修正为 k1，24151 的 NPC_REPORT source 修正为 k5；
  - 为 1640 补齐 reward 下的安装部件与完成路由；为 2569 消除 s2 死胡同并支持提交领奖；
  - 在 QuestDefinitionDirectoryLoaderTest 中新增全量静态拓扑门禁 executableQuestsHaveNoReachableDeadEndNodes，每次测试对全服所有可执行任务进行状态图 BFS 遍历，确保不存在任何除 COMPLETE 以外的无出边死胡同节点。

## 7. 前置依赖拓扑闭环与自指死锁环根治（18992 自依赖解除）
- **排查发现**：
  - 全库前置依赖图 DFS 环路扫描发现：任务 18992（天族 66 级重大副本任务“又一块碎片，又一场战斗”）的 start-conditions 中配置了 condition type="finished" quest-id="18992"，导致任务要求必须先完成自己才能接取自己，形成永久不可接取死锁环；
  - 比对真端客户端 quest.xml 与魔族对称任务 28992，确认 18992 本无任何前置任务要求。
- **修复与防御**：
  - 移除 18992.xml 中误配的自指 start-conditions；
  - 在 CompletedQuestPrerequisiteRegressionTest 中新增全库前置拓扑有向图 DFS 环路与自依赖门禁 noQuestRequiresItsOwnRewardTitle，彻底杜绝任何任务自指依赖或多任务相互前置死锁。

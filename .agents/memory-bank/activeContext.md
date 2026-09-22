# Active Context (当前攻坚上下文)

记录当前正在推进的任务与未解决的问题焦点。跨 Agent 接力时，先读此文件了解当前状态。

> last_updated: 2026-09-21
> status: ACTIVE
> scope: current checkout only
> owner: shared agents
> expires: focus changes, work is closed, or the recorded branch/worktree is replaced

---

## 当前攻坚焦点

- **主分支 / 工作分支**：`quest`
- **当前核心关注域**：
  - Quest 任务流转、状态机与 NPC 交互（Taloc's Hollow 副本实例脚本、Condition Spawns、Retail AI 判定）。
  - **已沉淀**：跟随 NPC 寻路轨迹（Breadcrumb trail）与护送 AI 选型优化已修复并提炼为 `AIM-001`–`AIM-003`（见 [patterns/ai-movement.md](patterns/ai-movement.md)），证据留在 `docs/movement/escort-follow-movement-repair.md`。不再作为未完成焦点。
  - **已沉淀**：Taloc's Hollow 卵的脱战抖动与孵化物过早消失已修复并提炼为 `AIM-004`（不可移动 NPC 不因够不着放弃目标）与 `IR-009`（pattern 子对象 `live_time` 与生成者状态重置解耦）；聚焦测试 95 例 + 客户端实机验证均通过，证据留在 `.agents/summary/talocs-hollow-mosqua-egg/2026-09-16-egg-disengage-and-summon-live-time.zh-CN.md`。不再作为未完成焦点。
  - **已验收**：任务 10032 击杀 Celestius 后卡斯帕的幻影 799503 不出现 → `RetailPatternAI2` 增加“死亡/消失事件链子对象不随生成者状态重置删除”护栏（`IR-011`）+ 实例层按真端动作幂等补刷（`RetailPatternAI2#spawnRetailActionNpc`，IR-010 的同类适配器）；2026-09-16 客户端实机复验通过（幻影现身、10032 正常完成），证据在 `.agents/summary/quest-10032/2026-09-16-celestius-death-spawn-caspa-ghost.zh-CN.md`。
  - **已验收**：任务 15300/25300 步骤 7「消灭盘龙巢穴的奥里萨」在爆发/一击致死时跳过阈值变身（237230 不生成 237231），已给 `immortalOrissanAI2` 加死亡兜底并批量补齐同族 8 个阈值变身 AI（死亡路径与阈值路径共用同一个 `*Once()` 闸门），Q 实例奥里萨死亡场景改为对实际生成的爆破手（209711/209776）喊话；聚焦测试 11 例全绿，不变量沉淀为 `AIM-007`。**2026-09-19/20 用户客户端确认 15300 全程顺利完成（含领奖）**，证据在 `.agents/summary/quest-15300-orissan/2026-09-19-immortal-orissan-death-fallback.zh-CN.md`。
  - **新焦点（待客户端观测/分批处置）**：领奖 packed step 投影全库审计（`QE-045`）发现 490 个旧 handler 领奖入口里 106 个 `MISMATCH_PROJECTION`、32 个 `MISSING_RECOVERY_EDGE` 尚未处置；本轮只修已验收的 15300/25300。下一步先用一次客户端观测确定「reward 投影 = 旧 handler pre-step」与「= 客户端 quest_summary 报告行」两条读数的取舍，再按批（优先 154xx redemption_landing 家族与 15301/25301 姊妹链）推进；脚本与清单在 `.agents/summary/quest-reward-projection-audit/2026-09-20-legacy-reward-projection-audit.zh-CN.md`。
  - **待一次客户端观测（QE-045 × QE-051 正面冲突，批次 13 已挂账）**：13965/23965（enter-zone 置 REWARD）与 15674/25674（CHECK_COLLECTED_ITEMS 置 REWARD）的客户端 quest_summary 都是 2 行、行 1 = 与领奖 NPC (835217/835220/806114/806116) 对话，按 QE-051 应有行 1 状态，但按 QE-045 必须保持 `reward var0=0` + `REWARD/var0=1` 恢复边（f6aff952a 基线，LegacyRewardStepProjectionRegressionTest 20 例硬锁）。取舍只需一次实机观测：**在 REWARD 态（可开奖励窗口时）任务书高亮的是行 0 还是行 1**；显示行 1 → 按客户端证据重定这 4 个的 QE-045 基线（XML + 锁测试 + 审计 QE045_LOCKED 同改）；显示行 0 → 保留现状并把“末行不可达”记为零售原样。观测前禁止批量翻动 20 个锁任务。
  - **待客户端验收（引擎外 REWARD，`QE-046`）**：任务 10522 领奖态「对话只有结束对话」的直接根因是迁移漏掉 `reward + QUEST_SELECT(31) -> DEFAULT_SUCCESS(10002)` 领奖态入口页，叠加引擎外写入方（`CM_CREATIVITY_POINTS`/`CoalescenceService`/`MinionService`/`RiftOrbAI2`）留下的 packed step 与 `reward` 投影错位。已按全库审计口径修复 10 个同型任务（10522/20522、15542/25542、15545/25545、30211/30213/30311/30313），并用 `ExternalRewardAdvanceReentryContractTest` + `src/test/resources/quest/external-reward-advance-baseline.tsv` 锁定；2026-09-21 授权 Maven 已完成：回归批 40 例、生产目录门禁 13 例（PRODUCTION_COMPILE_OK=6189/FAILURES=0）、客户端契约门禁 19 例（含 failOnStaleBaseline=true）全绿，宽口径 definition 包 980 例的 7 个失败均落在未改动任务上（10520/20520、15101、10526/20526、25512、15301/25301）。**客户端实机复测待用户执行**（需重建资源并重启），命令、审计脚本与复测路径见 `.agents/summary/quest-10522-reward-reentry/2026-09-20-external-reward-advance-reentry.zh-CN.md`。

  - **待客户端验收（计数越界，`QE-047`，已修复）**：10525 报障 `QUEST_AUDIT ... value out of range for progress field: var1`（实测 packed 步数 130/258/386 = var1 8/16/24，第四次点击 +8 越界）根因是四位证人证言自环无上限守卫，且 `var1` 紧凑放在 `offset=4` 污染客户端 `SECTION_0` 行索引；同族同因的 10529/20529 s8 boss 击杀（第二次击杀即越界）一并修复：10525/20525 改为 `var1 offset=6 width=4 max=15` + `variable-below 14/13/11/7` + 进入世界自愈边，10529/20529 补 s7→s8 清零，并把 s8 boss 击杀改为一次推进且同事务清零 var1。新增 `Quest10525TestimonyCounterContractTest`/`Quest10529BossKillCounterContractTest`/全服门禁 `QuestIncrementRangeContractTest`（全库 6222 定义 / 1012 条增量：EXACT_COUNTER_UNBOUNDED 6→0）。**2026-09-21 授权 Maven 已完成：聚焦测试 5/5 全绿；生产目录/客户端契约门禁全绿（PRODUCTION_COMPILE_OK=6189，FAILURES=0，INTERACTION_OBJECT_FAILURES=0，WHITELIST_VIOLATIONS=0）**，客户端实机复测未做；证据在 `.agents/summary/quest-10525-testimony-counter/2026-09-21-10525-testimony-counter-overflow.zh-CN.md`。

  - **待客户端验收（10525 s4/s5 报告链，`QE-004`，已修复）**：13:37 实机 `CM_DIALOG_SELECT npcId=806134 questId=10525 动作=31` 被回退成 `questId=0/下发页=10`；10525 缺 `s4 QUEST_SELECT -> SELECT5(2375)` 与 `s5 SELECT6(2716) -> SELECT6(2716)`，魔族镜像 20525 两条都已有。已补齐 10525 并给 `QuestDialog31RegressionTest` 增加双阵营 turn-in 路由断言；XML/XSD/IDE/diff 静态检查通过，2026-09-21 授权 Maven 六类测试全绿（退出码 0），客户端复测未做。证据在 `.agents/summary/quest-10525-report-dialog31/2026-09-21-10525-s4-s5-report-dialog31.zh-CN.md`。

  - **待客户端验收（击杀目标变体族，`QE-048`，已修复）**：15546《[每日]雷欧娜的委托》击杀不计数（客户端四个 `[%n]/4` 不动）的根因是任务只登记了世界中**零刷新**的 base 模板（240475/240483/240495/240497），而 Iluma 实际刷的是同族 `T_` 变体（241656/241657/241664/241665/241676/241677/241678/241679），`QuestEngine.onKill` 的 owner 索引因此永远拿不到该任务；已把 15546/25546 的每个计数器族改为「base + `T_` 变体」集合并同步 `<metadata><kills>`，再按同一客户端契约扫描修复 43 个同族任务（HEAD 42 个硬缺陷 → 当前工作区 0 命中）。新增门禁 `QuestIlumaNorsvoldKillTargetCoverageTest` + `src/test/resources/quest/iluma-norsvold-kill-target-contract.tsv`（45 任务评审快照），`QuestA03ShardRetailAlignmentTest` 相应改为「零售 progress_info 名单 ⊆ 目标 = 客户端契约快照」。**2026-09-21 授权 Maven：定向 40/40 全绿（PRODUCTION_COMPILE_OK=6189，FAILURES=0，WHITELIST_VIOLATIONS=0）；全量 questEngine 1555 run / 5 failures / 6 errors，11 项已逐条归因且均非本改动引入**；客户端实机复测未做，证据在 `.agents/summary/quest-15546-kill-progress/2026-09-21-iluma-norsvold-kill-target-variants.zh-CN.md`。 2026-09-21 追加（`QE-050`）：用户实测"某族打满 4/4 后继续击杀，进度不涨但客户端仍提示任务更新"。根因是收口自环只有下界（`variable-at-least 3 -> set 4`）在饱和后仍命中，planner 产出与当前 packed 完全相同的计划，`requiresStatePersistence` 不写库但 after-commit 的 `sync-quest-state PACKET_ONLY` 照发，客户端渲染成一次任务更新。已加引擎护栏（`QuestExecutionCoordinator.withoutRedundantStateSync`：状态未变化且无必需动作时丢弃多余状态同步）并把 15546/25546 计数改为精确边界（累加 `variable-below 4`、越界自愈 `variable-at-least 5 -> set 4`）；聚焦 88/88 全绿，全量 questEngine 1561 run / 5F / 6E 与修复前同一批既有失败，饱和自环全库审计 26 -> 18（余 18 条仅提交空事务、不再发通知，待逐任务收紧）。**客户端复测点：打满后多杀不应再出现"任务更新"，四族仍应逐次 1/2/3/4 推进**。

  - **已收口（性能线）**：D 项"每实体预制容器"懒物化共 8 个切片（12.15–12.23）已全部提交并复测验收（12.25）：
    存活堆 **−133 MB / −387 万对象**（且复测轮生物多 2.2%）、单位 CPU 样本分配 **−34%**、
    `Buffer.checkIndex` 7.5%→0%、`Arrays.fill` 4.5%→0%、容器/锁相关 CPU 帧→0，窗口内 0 异常；不变量沉淀为 `AR-010`。
    **寻路不再改动**（硬约束：不降路径质量）：A\* 的 87.5 MB/300s 属预热性常驻增长（`SearchWorkspace` CPU 帧仅 0.4%，
    `PathData$MapData$Node` 池高水位 64.9 万→105.8 万），缩池只会把常驻换成下次重新分配；唯一零质量候选是
    "手写开放集比较器（同全序）"，评估仅 2–5% 总 CPU，本轮判定不做。
  - **性能线复开条件**（满足任一才再开工，且必须用同一套 JFR 口径采样，不要继续扫直方图）：① 目标在线人数下出现卡顿/
    TPS 低于基线；② 单次 GC 停顿 > 200 ms 或 GC 频率上升；③ 进程 RSS > 目标机器物理内存 70%；④ 直方图出现新的
    "每实体预制容器"回归。复开首批候选：手写开放集比较器 → `collideWith` 调用点 → `WaterVolumeStore.find`（仍 5.9→8.6 MB/300s）
    → `KnownList.knownObjectsSnapshot`。证据与复现命令见 `.agents/summary/architecture-performance-refactor/2026-09-15-gameplay-jfr-hotspots.md` 12.26。
  - **容量口径基线**：世界全刷 + 0 玩家 ≈ **2.74 GB** 存活堆，叠加 12.98 万 NPC + 1 玩家 ≈ **2.82 GB**；16 GB 堆按 G1 live ≤75%
    给玩家的净余量 ≈ 9 GB（每人 1.5–5 MB **尚未实测**，需多做号差分）。真正的天花板是 CPU：**1 名玩家**时寻路+geo 已占 CPU 采样 ≈65%。
    部署到 16 GB 机器不要 `-Xmx16g`（用 `-Xmx10g -Xms4g`），并显式设 `-XX:MaxDirectMemorySize=2g`（默认 = Xmx，Netty 直接内存不计入堆直方图）。

---

## 正在进行中的注意事项 (Next Steps & Gotchas)

1. **工作区未提交改动防御**：
   - 工作区存在并行任务与副本相关修改（如 `CelestiusAI2.java`, `KinquidAI2.java`, `RetailPatternAI2.java` 等）。
   - 动手修改代码前务必先执行 `git status`，确认当前改动范围，避免覆盖未提交的成果。
   - `git add -A <dir>` 会连同目录内无关任务的改动一并暂存（曾把 Taloc's Hollow 等并行修改带入提交暂存区）；提交前用 `git diff --cached --name-only` 核对范围，或按文件清单精确 `git add`。
   - 已提交：`3fc71b693` 移除 137 个非真端残留 spot（33 个文件），并沉淀 `IR-007`；判定清单与脚本在 `.agents/summary/spawn-duplicate-spots/`。
2. **任务排查标准流程**：
   - 检查任务 XML 节点时，对照 `docs/quest/` 维护文档及旧 Handler 行为。
3. **P0–P2 架构改造：已全量验证并本地提交（本轮收尾）**：
   - 记录见 `.agents/summary/architecture-performance-refactor/2026-09-15-p0-p2-implementation.md`；跨域不变量已沉淀为 `AR-004`（Netty 读缓冲 limit、直连 initialized/onDisconnect、writeData 后禁止 flip + 小端载荷、交付切接收方 ServiceContext）。
   - 端到端验收通过（18:47 实例，真实客户端）：客户端登录 → 账号认证（内嵌直连）→ 进入世界，窗口内 0 ERROR/WARN；聊天服与登录服握手、登录服心跳线程均正常。
   - 全量 `mvn test`：`3247` 例，`13` 失败（全部经 HEAD `2f0752248` 干净副本基线复现，属既有 quest/AI 数据与审计闸门欠账），本次改造引入的 7 例已修复并沉淀 `AR-005`。
   - 收尾清理已完成：`MapRegion.getObjectsSnapshot()` 删除、`PacketProcessor` 去掉 `LinkedList` 强转。
   - **既有 13 例失败仍未处理**（quest 1722/1367/3935/80805/10032、SETPRO 领奖审计、Retail AI 定义计数 134/133、WorldScoped waypoint 3206/3207、windstream 兼容映射、Theobomos 编队），如需修复应另开任务。
4. **泰奥博莫斯（210060000）红/黄天空：根因已收敛，待实机验收**：
   - 结论：客户端 `lf2a` 的 `WeatherSystem` 只有 `SandRain` / `SandRain_Before`，二者 Sky 都指向
     `LF2A.Weather.LF2A_Rain`，且没有 remain/after 档；服务端下发 code 0 只能停止沙尘粒子，
     客户端不会把天空从乌云退回 `TimeEnv/Daylight`。Poeta/Inggison 有 after 档，所以能恢复。
   - 已处置：`weather_table.xml` 中 210060000 **整条移除**（不再注册天气表、不再下发 `SM_WEATHER`），
     客户端加载时保持 `TimeEnv/Daylight` 晴天；`//weather THEOBOMOS 1` 预期提示
     `Region has no weather defined`。`WeatherTable.zoneData` 的空列表初始化继续保留（`SDJ-004`）。
   - 已排除：贝里特拉入侵（`beritra.enable=false` 后 `Id 13 is invalid`）；`lf2a` 天气 SkySkyDome 为空，
     当前触发路径不是客户端 skydome 库。
   - 待验收：重启服务端 + 完全重启客户端进图。若仍红黄，再改客户端
     `Levels/lf2a/Level.pak`（优先散文件 `Levels/lf2a/mission_mission0.xml`），不要先回封标准 zip。
   - 2026-09-20 12:47 追加：服务端已确认无 210060000 天气表、无 `SM_WEATHER`，实机仍不晴，
     触发点已定位到客户端。已生成待验证补丁
     `.agents/summary/weather-theobomos/client-patch-20260920-1249/`（含标准 zip 回封 `Level.pak`
     与散文件 `unpacked/mission_mission0.xml`）；实测时优先散文件，回封 pak 需客户端接受度验证。
   - 2026-09-20 13:58 追加：用户替换第一版 pak 后天空已正常，但地面/雾仍像黄昏。原因是
     `invade_direct_portal` / `WorldRaid` 两个 cutscene TimeEnv 选项仍以
     `time_name="Daylight"/"Night"`、`zonename=""` 与正常时段竞争；已把两者改名为 `*_disabled`、
     `time_name` 同名、`zonename="__disabled__"`，对应 TimeofDayGroup 同步改名。最新补丁
     `Level.pak` MD5 `f72b44b54f0989b9235519a4f3d21571`，XML MD5
     `53839e5962aea7df4e9f6357267dfcd5`，待实机确认地面/雾恢复晴天。
   - 证据：`.agents/summary/weather-theobomos/diagnosis-sandrain.md`、
     探针 `.agents/summary/weather-theobomos/probe/TheobomosWeatherProbe.jsh`。

## 交接规则 (Handoff Rules)

- 本文件只记录当前未完成事项，不承载已经验证的长期模式。
- 焦点变化时更新 `last_updated`、`status`、阻塞原因和下一检查点；完成后标记 `DONE` 或删除已解决条目。
- 分支名、未提交路径和运行环境属于当前 checkout 的上下文，不能直接升级为跨 checkout 的永久规则。
  - **已沉淀**：任务引擎启动阶段针对 `1722.xml`（同 NPC 同阶段无优先级重叠转换）与 `3940.xml`（`<counter>` 导致 source 节点省略声明退化为 `START:0` 投影碰撞）崩溃已彻底排查修复并提炼为 `QE-014`（见 [patterns/quest-engine.md](patterns/quest-engine.md)），全量 6193 条 EXECUTABLE 任务独立编译通过。
  - **分批复测中（领奖行全库审计，`QE-051`）**：10527 报障“使用道具 182216075 后任务书停在上一行”的根因是迁移把 legacy `useQuestItem(env, item, 14, 15, true)` 的 `to` 丢掉、`reward` 节点投影就地把 var0 抄成 `from`；判据扩为“客户端 quest_summary 的每一行都必须能找到 var0==行号的 START/REWARD 状态”。批次 1-6 累计修复 349 个任务：批次 1（9 组镜像单侧缺陷）、批次 2（120 个双侧缺末行镜像对）、批次 3（202 个末行 NPC 命中领奖路线的单侧任务）、批次 4（10529/20529 报告行拆分 + 10530 reward=9 + 3090 SECTION_0 位段隔离 + 全库 varN@6N 门禁）、批次 5（15550/25550 三段对话链与 8 个感应区坐骑任务的行推进恢复）、批次 6（18208/18209/28208/28209 竞技场两阶段：按客户端 quest_script 的 SECTION_0 声明重建 var0 行号 + var1/var2 计数、reward=2、4 条旧存档收敛边）。**用户授权 Maven：12 个测试类 63 例全绿；`PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`**；全库审计（`audit_reward_row_vs_client_steps.py`）批次 5+6 后 `ROW_ALIGNED 2556 -> 2570`、`ROW_STATE_ALIGNED 2338 -> 2352`、`MISSING_LAST_ROW 159 -> 149`；SECTION_0 约束审计（`check_section0_requirements.py`）`UNREACHABLE 5 -> 3`（余 1922、2947、14054）。**客户端实机复测未做（PENDING_CLIENT）**；剩余 `MISSING_LAST_ROW` 149 个多数缺 legacy 行推进证据，禁止按“末行 NPC 命中”机械批量推进。证据与脚本在 `.agents/summary/quest-10527-reward-row/`（报告、audit/check 脚本、批次 5/6 重放脚本、ArenaPhaseRowContractTest / SensoryAreaRideRowContractTest 等合同测试）。
  - **批次 7 完成（2026-09-21，领奖行合同第一批“报告/交付型 2~3 行”）**：成长任务族 18 个（19672..19694 / 29672..29694，reward 投影 0->1 + enter-world 自愈边）、3210（3 行，reward 0->2 + var0=0/1 两条自愈边；镜像 4210 已是 2）、18036/28036（新增 `s1(START,var0=1)` 节点并把 `started(SETPRO1)` 与两条 `var0>=1` 报告事务改指 s1，修掉“交出物品后无事务可匹配”的卡死，reward 0->1 + 自愈边）。**用户授权 Maven：13 个测试类 68 例全绿**；全库审计 `ROW_ALIGNED 2570 -> 2591`、`ROW_BEHIND 275 -> 254`、`ROW_STATE_ALIGNED 2352 -> 2372`、`MISSING_LAST_ROW 149 -> 129`，逐行 diff 仅这 21 个任务变化；`xmllint` 21 文件 validates；IDE lint 0 警告；`git diff --check` 干净。**客户端实机复测仍未做（PENDING_CLIENT）**。
  - **批次 8 完成（2026-09-21，领奖行合同 × 引擎外写入方求交，`QE-046`）**：10522/20522（`CM_CREATIVITY_POINTS#checkQuestCompletion`）、15542/25542（`CoalescenceService#updateQuestsOnCoalescenceComplete`）、30211/30213/30311/30313（`RiftOrbAI2#forQuest`）共 8 个任务——写入方在 `setStatus(REWARD)` 前补 `qs.setQuestVarById(0, 1)`（领奖行），reward 投影 `0 -> 1`，旧自愈边由 `var0==1` 改为 `var0==0`（无 actions，仅 `LEVEL_AND_VISIBILITY_REFRESH`），并重刷 `src/test/resources/quest/external-reward-advance-baseline.tsv`（10 任务 writer step=1 / projection=1 / recovery=[0]）。**用户授权 Maven：17 个测试类 84 例全绿（含 Quest10522AutoStartDialogTest 更新后的 reward=1 投影与 `var0==0` 自愈边断言）；`PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`**；全库审计 `ROW_ALIGNED 2591 -> 2599`、`ROW_BEHIND 254 -> 246`、`ROW_STATE_ALIGNED 2372 -> 2380`、`ROW_WITHOUT_STATE 575 -> 567`、`MISSING_LAST_ROW 129 -> 121`，逐行 diff 仅这 8 个任务变化；`xmllint` 8 文件 validates；IDE lint 0 警告；`git diff --check` 干净。**客户端实机复测仍未做（PENDING_CLIENT）**。
  - **批次 8 附带收口（2026-09-21）**：批次 1-7 的 375 个任务新增无 source `enter-world` 自愈边后，用「引用了改过的任务 id + 按 sourceNode 过滤」筛出的 43 个定向回归类里 36 例失败——21 个测试类 helper 因 `sourceNode().equals(...)` NPE（已统一改为 `Objects.equals`）、7 个任务的 reward 行期望按客户端行数更新（1553=2、1988/2988=3、3082=3、14026/24026=5、14051=4、15550/25550=2、24030=9），`QuestClientContractGateTest` 的 28208/28209 新指纹（客户端 task 末行 Anja=205321 缺 1009 完成路线）已补 `<npc-complete npc-id="205321">`；另修掉 98b34418a 遗留的 `QuestMovieAndDialogLoopRegressionTest` 节点改名断言（started → s0）。**回归收口后 302 例全绿**。
  - **批次 9（2026-09-21，领奖行合同续作）**：28932 的 `started` 由自闭合节点改为只声明行号 `var0=0`（与天族镜像 18932 同形；不声明 var1 计数器，保住 `var1>=1` 的满计数恢复对话），新增 `Quest28932RewardRowContractTest`（3 条合同），审计 `ROW_STATE_ALIGNED 2380 -> 2381`、`ROW_WITHOUT_STATE 567 -> 566`、`INTERIOR_GAP 267 -> 266`（逐行 diff 仅 28932）；30203/30303 经 `origin/history:_30203GroupHalttheCeremony` 与客户端 `Progress(SECTION_0<1)…(SECTION_3<1)` 核实为“var0 是击杀标志位”的审计误报族，记入审计脚本 `VAR0_FLAG_EXCEPTIONS`；**21 例聚焦测试全绿**；客户端实机复测仍为 PENDING_CLIENT。
    - **批次 10 完成（2026-09-22，领奖行合同：retail 单步族 + 镜像单侧）**：retail `zz_retail_simple_quests.xml` 的 249 个单步任务（start + 1 step，客户端 2 行）里 184 个早已 `reward var0=1`；落后的 18 个按族内模板统一收口——A 组 12 个末行完全没有 START/REWARD 状态（1527/1528/1725/2135/2247/2266/3087/4020/21455/26838/80735/80736）、B 组 5 个末行有 var0=1 中间态但 reward 仍停在行 0（1963/1964/16838/16977/18035）、C 组 29002（镜像 19002 已对齐，且 `_29002ExpertAethertappersTest` 在 204099 的 STEP_TO_1 写 `setQuestVarById(0,1)`、204257 领奖不改 var0）——全部 reward 投影 0→1 + 无 source `enter-world` 自愈边（`REWARD && var0==0 → 1`）。**用户授权 Maven：11 个测试类 38 例全绿（含新增 `RetailSingleStepRewardRowContractTest` 4 例）；`PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`**（主工作树当时被并行任务的 `PlayerCommonData.java`/`CM_HOTSPOT_TELEPORT.java` 语法错误态占住，按项目规则改用临时 worktree 验证并已移除）；全库审计 `ROW_ALIGNED 2599→2617`、`ROW_BEHIND 246→228`、`ROW_STATE_ALIGNED 2381→2394`、`ROW_WITHOUT_STATE 566→553`、`ALIGNED 2381→2394`、`MISSING_LAST_ROW 121→108`，逐行 diff 仅这 18 个任务；`xmllint` 18/18 validates。**客户端实机复测仍未做（PENDING_CLIENT）**。脚本 `apply_batch10_retail_single_step_rows.py`（`--check` 幂等）+ 证据 `batch10-evidence.tsv`。
    - **批次 11 完成（2026-09-22，领奖行合同：25608 多阶段 ENTER_AREA 行对齐）**：客户端 `quest_q25608.html` 去重后 7 个可见槽位，retail 7 步为 TALK 806177 → TALK 806197 → ENTER_AREA 206534 → HUNT 241235 x10 → TALK 806197 → ENTER_AREA 206542 → COLLECT_ITEM 805964；旧链把 HUNT/Mumu/交付行错放在 var0=2/3/4、reward 停在 5，行 2/5 无状态。修复：新增 step2/step5 两个 enter-zone、HUNT→step3、Mumu SELECT5→step4、交付行归 step6（与 reward 同为 var0=6、status 不同）、reward 5→6 + REWARD/var0<6 的 enter-world/QUEST_SELECT 自愈边、drop collecting-step 0→6；从本机客户端 `Levels/DF6/Level.pak` 解包 `mission_mission0.xml` 的触发点坐标在 zones_quest.xml 新增两个 sensory zone；新增 `Quest25608RetailSevenStepAlignmentTest`（6 条合同）。主工作树被并行任务 13 个 XML 的 `AMBIGUOUS_TRANSITION` 阻塞，按规则改用临时 worktree（HEAD 79775ca10 + 本批 3 个改动）验证：**28 例全绿 / PRODUCTION_COMPILE_OK=6189 / FAILURES=0**，worktree 已移除；单任务审计 ROW_BEHIND/MISSING_LAST_ROW → ROW_ALIGNED（reward_var0 5→6、visible 0..6）；`xmllint` quest_definition.xsd + zones.xsd 均 validates；IDE lint 0 warning。**客户端实机复测仍未做（PENDING_CLIENT）**。
  - **批次 12 完成（2026-09-22，领奖行 + 领奖 NPC 归属核实族）**：客户端 quest_summary 行内命名 NPC 是领奖 NPC 的验收口径；19064/29064 新增 s1(START,var0=1)、行 0 交给 Undin/Darfen(798450/798452)、领奖/completion 由起始 NPC 203701/204053 改到 Jucleas/Balder(203752/204075)、reward 0→1 + `var0=0/2` 双自愈边；21455 领奖/completion 799404→Unset(799244)、删 SETPRO2 死路由、换物品并入 SETPRO1；30614 reward 0→1 + `var0=0` 自愈边 + 领奖态入口页 `reward + QUEST_SELECT(31) -> DEFAULT_SUCCESS(10002)`（必须唯一且位于 npc-complete 之后，首版误插进 completion 块内部会让 XSD 报错）。**30614 归属修订**：首轮按 legacy 单源留在 Aluna(800326) 属误判（retail contract 与 legacy template 同源于 `terath_dredgion.xml`），已按客户端行 1「向 Astella 报告」+ quest_complete「阿斯泰拉说必须…」+ 同族 30610/30611/30612/30613 命名规则 4/4 + 2026-08-06 f737cfef1 客户端/真端交叉审计回滚为 Astella(800327)；10a2e7e57 据 legacy 单源改错，登记在 `report-npc-mismatch.csv` 第 64 行。门禁 `RewardNpcOwnershipContractTest`（6 条合同）；单任务审计 4/4 `ROW_ALIGNED / ALIGNED / ROW_STATE_ALIGNED`，全库 `last_row_npc_matches_quest` False 523→519、`MISSING_LAST_ROW 102→99`；Maven **PENDING_MAVEN**、客户端实机 **PENDING_CLIENT**。脚本 `apply_batch12_reward_npc_ownership.py`（`--check` 4/4 幂等）、证据 `batch12-evidence.tsv`、报告 §十六。
  - **批次 13 完成（2026-09-22，5.8“两行、末行是与领奖 NPC 的对话”族，6 个任务）**：客户端 quest_summary 恰好 2 行且行 1 点名领奖/完成 NPC 的 1926（Latri 203894，承 `_1926Secret_Library_Access`）、2938（Izwin 204267，承 `_2938Secret_Library_Access`）、39003（DF2a_Nevma_G_LHM 800504）、49003（dromik 800505）、80989/80990（IDRUN_Entrance_guide 836196）统一 reward 投影 0→1 + 无 source `REWARD/var0=0 → set var0=1`（LEVEL_AND_VISIBILITY_REFRESH）自愈边；1926/2938 迁移期已有的 `REWARD/var0=1` 入口边保留（条件互斥，无 AMBIGUOUS_TRANSITION）。**同形的 QE-045 锁任务 13965/23965/15674/25674 被本批打回不改**（见焦点区的待观测项），与批次 2“排除 QE-045 锁任务”同口径。门禁 `RewardRowTwoRowTalkFamilyContractTest`（6 条合同：投影/owner/自愈边唯一且 planner 可收敛/迁移期入口边按任务保留/无事务写非领奖行 var0/锁任务保持 0）；单任务审计 6/6 `ROW_ALIGNED / ALIGNED / ROW_STATE_ALIGNED`（visible 0 → 0 1），全库 `ROW_ALIGNED 2633→2639`、`ROW_BEHIND 211→205`、`MISSING_LAST_ROW 99→93`；`xmllint` XSD 6/6 validates、IDEA lint 0 problem；Maven **PENDING_MAVEN**、客户端实机 **PENDING_CLIENT**。脚本 `apply_batch13_two_row_talk_rows.py`（`--check` 6/6 幂等，含 `QE045_DEFERRED` 常量）、证据 `batch13-evidence.tsv`、报告 §十七。
  - **批次 14 完成（2026-09-22，事件族“两行、末行是与领奖 NPC 的对话”，4 个任务）**：80255/80256（活动烟花族漏网项——行 1 是字面中文名“和帕尔图/布巴纳对话”没有 `STR_DIC_N_` 键，批次 1-7 的“末行 NPC 键”筛选漏掉；同族 80257-80260 已对齐；归属由客户端表 831163=event_Parutoo、831164=event_Boobanah + npc-complete owner 核对）与 80601/80606（德雷得奇安事件链头本；legacy `_80601Fight_Of_The_Navigators`/`_80606The_Good_News_And_Bad` 击杀分支 `setQuestVarById(0, 1)` 后才置 REWARD，typed 击杀事务保留 `set-variable var0=1` 但 reward 投影仍是 0，经无 actions 的 `NPC_REPORT` 进入领奖态的存档匹配不到 reward 路由）统一 reward 投影 0→1 + 无 source `REWARD/var0=0 → set var0=1` 自愈边。边界：80602..80605/80607..80610 的 var0=3/4/5/9 是阶段计数（STATES_BEYOND_ROWS）不动；50008/51008（var0 是 0→2 投递计数 + 客户端表缺名）与 1123/1466/2484/2842/4712 （非 NPC 行内键 / 缺 reward 投影 / owner 不唯一 / 击杀自环）仍需单独取证。门禁 `RewardRowEventTwoRowContractTest`（6 条合同）；单任务审计 4/4 `ROW_ALIGNED / ALIGNED / ROW_STATE_ALIGNED`，全库 `ROW_ALIGNED 2639→2643`、`ROW_BEHIND 205→201`、`MISSING_LAST_ROW 93→89`；Maven 原授权命令重跑 29 例全绿（PRODUCTION_COMPILE_OK=6189/FAILURES=0），追加授权后 8 个测试类 **35 例全绿**（含新增门禁 6/6）、客户端 **PENDING_CLIENT**。脚本 `apply_batch14_event_two_row_rows.py`（--check 4/4 幂等）、证据 `batch14-evidence.tsv`、报告 §十八。
  - **批次 15 完成（2026-09-22，残余两行族 1123/2484）**：1123 的行 1 用客户端 actor 名键 `STR_DIC_LA12`（不在 `STR_DIC_N_*` 表里 → 审计 `last_row_npc_matches_quest=False`），用同族 1006/1122/1124/30507 的 completion owner 全部 = 790001 = **Pernos** 交叉解键，与自身 start/completion owner 自洽；进入 REWARD 的唯一路由是 LF1 感应区 enter-zone + play-movie 11（不写 var0）。2484 的 legacy `_2484OurManInElysea` 在烽火对象 700267 处 `setQuestVarById(0, 1)`、随后在 Hippolyta(203331) 处置 REWARD，typed 三条 NPC_REPORT 都无 var0 动作靠投影补足。两者统一 reward 投影 0→1 + 无 source `REWARD/var0=0 → set 1` 自愈边。挂账：1466（`Quest1466ClientDialogAlignmentTest` 硬锁 reward 无投影 + 报告路由写 var0=2 + headless journey，需客户端观测才能重定基线）、4712（行 1 向 Henir 报告 vs completion owner 是 279042/798327/798330，属归属核实题）、2842（var0 是 0..39 击杀计数，行 1 槽位 `[%15]`）、50008/51008（投递计数 + 客户端表缺名）。门禁 `RewardRowResidualTwoRowContractTest`（6 条合同）；单任务审计 2/2 `ROW_ALIGNED / ALIGNED / ROW_STATE_ALIGNED`，全库 `ROW_ALIGNED 2643→2645`、`ROW_BEHIND 201→199`、`MISSING_LAST_ROW 89→87`；Maven 已授权 8 类重跑 **35 例全绿**（PRODUCTION_COMPILE_OK=6189/FAILURES=0），追加授权后 9 个测试类 **41 例全绿**（含新增门禁 6/6）、客户端 **PENDING_CLIENT**。脚本 `apply_batch15_residual_two_row_rows.py`（--check 2/2 幂等）、证据 `batch15-evidence.tsv`、报告 §十九。**“两行 + 末行对话”族至此 12 个（13/14/15 批 = 6+4+2）收口完毕。**

  - **批次 16 完成（2026-09-22，领奖 owner 收敛 4712/2484）**：4712 的行 1 = 向 `STR_DIC_N_Henir` 报告，
    用同族 4713/4714/4715/4716（行内都写 `STR_DIC_N_Henir` 且 completion owner 全部 279042）交叉解键
    → Henir = 279042；legacy `_4712Escape_From_The_Dredgion` 在囚犯 798327/798330 处
    `defaultCloseDialog(env, 0, 1, true, false)`（写 var0=1 并置 REWARD、囚犯随即 onDelete），REWARD 态只在
    279042 处发送 10002/结束对话 → reward 投影 0->1 + 无 source 自愈边，删除囚犯的 `npc-complete`、
    保留其 `NPC_REPORT -> reward` 入口。2484 按 legacy 归属（只在 203331 Hippolyta 处 `setStatus(REWARD)`）
    删除 204407/700267 的 `npc-complete`、保留三条报告路由。新增模式卡 **QE-052**（领奖 owner 必须等于
    客户端领奖行 NPC）。门禁 `RewardOwnerTrimContractTest`（7 例）；单任务审计 4712 `ROW_BEHIND -> ROW_ALIGNED`，
    全库 `ROW_ALIGNED 2645->2646`、`ROW_BEHIND 199->198`、`ROW_STATE_ALIGNED 2423->2424`、
    `MISSING_LAST_ROW 87->86`；xmllint 2/2 validates、IDEA lint 0、`git diff --check` 干净；
    Maven 已授权 10 个测试类 **48 例全绿**（含新增门禁 7/7，PRODUCTION_COMPILE_OK=6189 / FAILURES=0）、客户端 **PENDING_CLIENT**。脚本 `apply_batch16_reward_owner_trim.py`
    （--check 2/2）、证据 `batch16-evidence.tsv`、报告 §二十。
  - **批次 16 边界（下一批前必读）**：28208/28209 的 Inggness(205320)/Anja(205321) 双领奖 owner 待客户端取证
    （禁止先收敛）；26838/16838 的 `Jarik01/Ostia01` vs `Jarik02/Ostia02` 01/02 变体分歧仍未收敛；
    删除 completion owner 必须以保留 `NPC_REPORT -> reward` 入口为前提（删多留少会让行 0 无法推进），
    且 `npc-complete` 与领奖态入口页必须落在同一 owner 上。
  - **批次 17 完成（2026-09-22，圣灵守护者武器事件族 80290/80291/80294/80295）**：先做族级扫描
    （新工具 `.agents/summary/quest-10527-reward-row/audit_legacy_reward_entry_steps.py` → `legacy-reward-entry-scan.tsv`，
    扫描迁移前 651 个 handler 任务的 `useQuestItem`/`defaultCloseDialog`/`checkQuestItems`/`checkQuestItemsSimple`/
    `changeQuestStep`）——“nextStep 被迁移丢弃”的完整集合 = 15300/25300（QE-045 基线，测试锁定）+ 80291/80295。
    80291/80295（客户端 2 行）legacy `checkQuestItems(env, 0, 1, true, 5, 0)` → 投影 `0->1` + 自愈边；
    80290/80294（客户端仅 1 行、原投影 1 属 `STATE_OUT_OF_RANGE`）投影 `1->0` + 自愈边；owner 唯一
    （831384 = event_Zephyrin / 831387 = event_Lilyolin）。门禁 `DurableDaevanionWeaponRewardRowContractTest`
    （7 例）；单任务审计 4/4 `ROW_ALIGNED / ROW_STATE_ALIGNED`，全库 `ROW_ALIGNED 2646->2650`、
    `ROW_BEHIND 198->196`、`ROW_AHEAD 2591->2589`、`ROW_STATE_ALIGNED 2424->2428`、
    `ROW_WITHOUT_STATE 523->521`、`STATE_OUT_OF_RANGE 2448->2446`、`MISSING_LAST_ROW 86->84`；
    族级扫描复核 `LEGACY_NEXTSTEP_DROPPED` 为空；xmllint 4/4 validates、IDEA lint 0、`git diff --check` 干净；
    Maven 已授权 12 个测试类 **58 例全绿**（含本批门禁 7/7，PRODUCTION_COMPILE_OK=6189 / FAILURES=0）、客户端 **PENDING_CLIENT**。
    脚本 `apply_batch17_daevanion_durable_weapon.py`
    （--check 4/4）、证据 `batch17-evidence.tsv`、报告 §二十一。
  - **批次 17 边界（下一批前必读）**：`STEP_EQUALS_NEXTSTEP` 的 392 个任务（2600/1920/2945 等
    `defaultCloseDialog(env, s, s, true, ...)`）与 QE-045 基线（15300/25300 的 reward=13）不得按客户端末行机械推进；
    剩余 `MISSING_LAST_ROW 84` 多为这类任务或多阶段/计数族，必须逐族取客户端观测或 legacy 证据。
  - **批次 18 完成（2026-09-22，镜像单侧投影落后族 11110/14201/16974/17160/17161/17526）**：族级判据——同形镜像对
    （q / q±10000）客户端 quest_summary 行数相同、末行 NPC 都能在任务内对上，但只有一侧 reward 投影等于领奖行；
    **已对齐的那一侧即参照基线**（11110/21110=1、14201/24201=2、16974/26974=1、17160/27160=1、17161/27161=1、17526/27526=1）。
    6 个任务本就有完整 0..N-1 行状态（rows_without_state 空、row_state 已 ROW_STATE_ALIGNED），修复 = reward 投影 + 无 source
    `REWARD/var0=0 -> 镜像行` enter-world 自愈边（LEVEL_AND_VISIBILITY_REFRESH）。门禁 `MirrorRewardProjectionLagContractTest`（5 例）；
    单任务审计 6/6 `ROW_BEHIND -> ROW_ALIGNED`（recovery=True）；全库 `ROW_ALIGNED 2650->2656`、`ROW_BEHIND 196->190`
    （row_state 计数与 `MISSING_LAST_ROW 84` 不变）；xmllint 6/6 validates、IDEA lint 0、`git diff --check` 干净；
    Maven 已授权 15 个测试类 **92 例全绿**（含本批门禁 5/5，PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / WHITELIST_VIOLATIONS=0）、
    客户端 **PENDING_CLIENT**。脚本 `apply_batch18_mirror_projection_lag.py`（--check 6/6）、证据 `batch18-evidence.tsv`、报告 §二十二。
  - **批次 18 边界（下一批前必读）**：镜像两侧投影比较只比行号 `var0`——镜像侧可另有计数槽（27160/27161 带 `var1=10`）；
    16837/16986/16988 属同形但被 `QuestPrematureRewardRouteExclusionTest` 的 `RewardCase.reward={var0:0}` 锁定
    （完成报告后 packed var0 保持 0），未取到客户端观测前**禁止按镜像推进**；本批只覆盖“有完整状态、仅投影落后”形态，
    下一批（批次 19）转向需要补中间状态阶梯的 B 组：15000/15670（rows=4，reward 0→3）、23809（0→3）、23918（1→5）、
    24046（6→7，镜像行状态本身有缺口），不能只改投影。
  - **批次 19 完成（2026-09-22，单步塌陷对 15000 / 15670）**：族级判据——同形镜像客户端行数相同、末行 NPC 都能对上，
    镜像那侧已有完整 0..N-1 阶梯（25000/25670 均 4 行 reward=3），本侧被迁移塌陷成“单步交接直接置 REWARD”、只剩行 0
    （ROW_BEHIND / MISSING_TAIL_ROWS / ROW_WITHOUT_STATE）。修复按客户端对话页 `<Act href="HACTION_*">` 按钮链补回行 1/行 2：
    15000 交背囊 182215661 → SELECT2(1352) → SELECT2_1(1353) → SETPRO2 接过工作物品 182215662 → use-item → 领奖行 3；
    15670 赫梅洛斯 806093（行 0/行 1）→ 第五处痕迹 731793（行 2）→ 伊利西亚 806114（行 3），4 处痕迹 703434-703437 的
    can-act 自环与 drops collecting-step 随行号 0 → 1 移动；两者都补 `REWARD/var0=0 -> 3` 的 enter-world 自愈边
    （塌陷定义留下的旧存档否则连奖励对话都点不出来）。门禁 `CollapsedSingleStepLadderContractTest`（7 例）；
    单任务审计 2/2 `ROW_BEHIND -> ROW_ALIGNED`；全库 `ROW_ALIGNED 2656->2658`、`ROW_BEHIND 190->188`、
    `ROW_STATE_ALIGNED 2428->2430`、`ROW_WITHOUT_STATE 521->519`；xmllint 2/2 validates、`git diff --check` 干净；
    Maven 已授权 16 个测试类 **99 例全绿**（PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / WHITELIST_VIOLATIONS=0）、
    客户端 **PENDING_CLIENT**。脚本 `apply_batch19_collapsed_single_step_ladder.py`（--check 2/2 幂等）、
    证据 `batch19-evidence.tsv`、报告 §二十三。
  - **批次 19 边界（下一批前必读）**：`npc-complete` 的 `<preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>` 已经展开
    reward 自环（1009 与 USE_OBJECT），**不得再显式声明 SELECT_QUEST_REWARD 的 reward 自环**（AMBIGUOUS_TRANSITION）；
    客户端页按钮必须在服务端有落地页路由（`1352 -> SELECT2`、`1353 -> SELECT2_1`），否则 QuestClientContractGateTest 报
    BUTTON_WITHOUT_ROUTE；剩余塌陷对：`23809/13809`（还需 QE-052 owner 收敛，镜像自己也把三棵树登记成 NPC_START +
    npc-complete）、`23918/13918`（客户端 quest_monster.csv 用 SECTION_0..4 串行门控 5 只精锐兵，而 23918 现为 32 节点
    组合式模型，需先定建模口径）、`24046/14046`（INTERIOR_GAP 缺行 4/7，两侧页动作不同形、14046 侧另有 movie 翻页修复记录）、
    `1000/11000`、`39713/49713`（页动作链不同形，需先定行内 NPC 与 owner）。
  - **批次 21 完成（2026-09-22，精锐兵链式计数族 23918 / 13918）**：族级判据——客户端 quest_summary 6 行
    （行 0..4 各一只特殊精锐兵、行 5 向 Elger(13918=802350)/Helgund(23918=802353) 报告）+ quest_monster 的
    `SECTION_0<1; SECTION_5==0` .. `SECTION_4<1; SECTION_3==1` **链式串行 0/1 计数槽** + 领奖行 NPC 与 owner 对齐；
    本族 `var0` 不是行号，权威口径是 `audit_section0_report_row_closure.py` 的 `COUNTER_CHAIN`（QE-053）。
    改前 23918 是 32 节点 `<counter-grid>` 自由组合（任意顺序击杀与客户端门控冲突 + owner 错挂接取 NPC 802347 +
    `fixed-reward-indices="0 1"` 吞掉 ITEM 169405255×6 + 无 kills），13918 是单槽 `var0=0..5` 步骤号且缺 var4
    （owner 同样错挂 802328）。本批两侧同形重建：`var0..var4` 各占 `SECTION_n = 6n` 的 0/1 槽、9 节点串行阶梯
    （k1..k5，每只怪只推自己那一槽、`PACKET_ONLY`）、owner 收敛到领奖行 NPC、`fixed-reward-indices="0 1 2"` 恢复道具格、
    23918 补 `<kills>` 五条、迁移自愈 6 条（13918 的 `START var0=2..5` 步骤号迁移 + 两侧 `REWARD`/`var0>=2` 与
    `sum(var0..var4)<5` 补齐到全 1）。验证：xmllint 2/2 validates、两侧 `COUNTER_CHAIN_OK`（改前 13918 为
    `COUNTER_CHAIN_GAP`）、`QuestDialogOrderAudit` 无 `CLIENT_PAGE_UNREACHED`、planner 探针乱序/回看无计划、
    全库快照变化（`ROW_ALIGNED 2659->2658`、`ROW_BEHIND 187->188`）全部归因于 13918 由“行号口径误判对齐”转为
    登记例外 `VAR0_FLAG_EXCEPTIONS`；Maven 已授权 21 个测试类 **149 例全绿**（含 `ChainEliteLadderContractTest` 7 例，
    PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / WHITELIST_VIOLATIONS=0）、客户端 **PENDING_CLIENT**。
    脚本 `apply_batch21_chain_elite_ladder.py`（--check 2/2 幂等）、证据 `batch21-evidence.tsv`、报告 §二十五、模式卡 QE-053。
  - **批次 21 边界（下一批前必读）**：本族位宽**必须保留 6 bit**（1-bit 位域无法表示旧 step 存档的 `var0=2..5`，
    `VariableIs` 自愈边会被 `ProgressLayout.pack` 上界校验拒绝）；`var5` 不声明（客户端 `SECTION_5==0` 是报告行门控）；
    不得按 `audit_reward_row_vs_client_steps.py` 的行号口径给本族“修正” reward 投影（已登记例外，权威口径是
    COUNTER_CHAIN）；`npc-complete` 的 preview 已展开 SELECT_QUEST_REWARD，不得再显式声明 reward 自环
    （AMBIGUOUS_TRANSITION）。剩余“单步塌陷/错位”挂账：`24046/14046`（8 行、两侧页动作不同形 + 既有 movie 翻页修复记录）、
    `1000/11000`（4 行）、`39713/49713`（3 行 FACTION 日任、三名可互换报告 NPC）。
  - **批次 10 边界（下一批前必读）**：21455 的领奖 NPC 归属已在批次 12 收口（领奖/completion 799404 → Unset 799244，accept 仍是 799404 Miener）；25608 已在批次 11 收口（实际缺 ENTER_AREA 206534 与 ENTER_AREA 206542 两行；修复为 step2/step5 两个 enter-zone、HUNT→step3、Mumu SELECT5→step4、交付行归 step6、reward 5→6，并从客户端 DF6 Level.pak 的 mission_mission0.xml 触发点注册两个 sensory zone）；10530 第 8 行是与第 9 行共槽的空 `<p>`（镜像 20530 无此行），已在审计脚本登记 `DUPLICATE_VISIBLE_SLOT_BLANK_ROWS`，禁止按行号加一；19008/19014/19020/19026/19032 等“名人考试”族的 reward=1 属 legacy 语义（19057/29057 的 handler 另有 var0=2 的失败分支），不得按“末行行号 2”改；剩余 MISSING_LAST_ROW 95 个（镜像同缺 32、QE-045 锁 10）需逐族 legacy/retail 证据。
- **批次 8 边界（下一批前必读）**：10522/20522/15542/25542/30211/30213/30311/30313 的 QE-046 基线已随写入方一起改到领奖行 1，后续再改这 8 个任务的 reward 投影必须同时改写入方并重刷基线；19064/29064/21455/30614 的领奖 NPC 归属已在批次 12 收口（30614 按客户端行内命名回滚为 Astella 800327，禁止再按 `terath_dredgion.xml` 单源改回 Aluna 800326）；26838（末行 Jarik01=806574，定义 806575）仍需先核实领奖 NPC 身份；28932/30203/30303 需要给无投影的 `started` 节点补 var0；1607/1990/2990/3502/14012/14013/17511/27511 属多阶段/内部缺口（INTERIOR_GAP、MISSING_TAIL_ROWS），必须单独设计阶段推进；脚本 `apply_batch8_external_writer_reward_row.py`（`--check` 幂等）、门禁 `ExternalRewardAdvanceReentryContractTest` 与 `Quest10522AutoStartDialogTest`。
  - **批次 7 边界（下一批前必读）**：10522/20522/15542/25542/30211/30213/30311/30313 的 QE-046 基线（reward 投影必须等于引擎外写入方的 packed step）已由批次 8 一并改到领奖行 1（写入方 + 投影 + 基线 TSV 三处同改）；30614/26838/19064/29064 的末行 NPC 与定义 NPC 不一致，需先核实领奖 NPC 身份；28932/30203/30303 需要给无投影的 `started` 节点补 var0；1607/1990/2990/3502/14012/14013/17511/27511 属多阶段/内部缺口（INTERIOR_GAP、MISSING_TAIL_ROWS），必须单独设计阶段推进。脚本 `apply_batch7_report_row_contract.py`、证据 `batch7-evidence.tsv`、门禁 `ReportRowRewardProjectionContractTest`（5 条合同）。

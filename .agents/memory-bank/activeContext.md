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
  - **批次 8 边界（下一批前必读）**：10522/20522/15542/25542/30211/30213/30311/30313 的 QE-046 基线已随写入方一起改到领奖行 1，后续再改这 8 个任务的 reward 投影必须同时改写入方并重刷基线；30614（末行 Astella=800327，定义 Aluna=800326）、26838（末行 Jarik01=806574，定义 806575）、19064/29064（末行 Jucleas/Balder，定义 Lavirintos/Kvasir）需先核实领奖 NPC 身份；28932/30203/30303 需要给无投影的 `started` 节点补 var0；1607/1990/2990/3502/14012/14013/17511/27511 属多阶段/内部缺口（INTERIOR_GAP、MISSING_TAIL_ROWS），必须单独设计阶段推进；脚本 `apply_batch8_external_writer_reward_row.py`（`--check` 幂等）、门禁 `ExternalRewardAdvanceReentryContractTest` 与 `Quest10522AutoStartDialogTest`。
  - **批次 7 边界（下一批前必读）**：10522/20522/15542/25542/30211/30213/30311/30313 的 QE-046 基线（reward 投影必须等于引擎外写入方的 packed step）已由批次 8 一并改到领奖行 1（写入方 + 投影 + 基线 TSV 三处同改）；30614/26838/19064/29064 的末行 NPC 与定义 NPC 不一致，需先核实领奖 NPC 身份；28932/30203/30303 需要给无投影的 `started` 节点补 var0；1607/1990/2990/3502/14012/14013/17511/27511 属多阶段/内部缺口（INTERIOR_GAP、MISSING_TAIL_ROWS），必须单独设计阶段推进。脚本 `apply_batch7_report_row_contract.py`、证据 `batch7-evidence.tsv`、门禁 `ReportRowRewardProjectionContractTest`（5 条合同）。

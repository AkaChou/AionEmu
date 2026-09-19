# Memory Bank 检索盲检报告（2026-09-19）

## 方法

- 查询来源：Pattern `evidence` 字段引用的 `.agents/summary/**` 原始证据文档，取标题 + 首个症状/结论段落（截断 400 字）。
- 去标识处理：查询中移除 summary 路径、所属 topic 目录名、Pattern ID 与 commit hash，避免与卡片证据路径直接命中。
- 打分：直接调用仓库现有 `search_memory_bank.py --json`，与 Agent 实际使用路径一致。
- 期望集合：一份证据被多个 Pattern 引用时，命中其中任意一个都算成功。
- 低信号过滤：没有「症状/现象/问题/Symptom」章节的文档（纯验收记录、量化记录）不计入命中率，单列。
- 已知偏差：查询文本与元数据同源，命中率偏乐观；本报告用于发现「连原始证据都找不到」的卡片，而不是估计真实用户检索上限。

## 结果

- 有效查询数：23（低信号文档 16 份已剔除）
- Top-1 命中：18/23（78%）
- Top-3 命中：18/23（78%）
- Top-5 命中：19/23（83%）
- MRR：0.791

### Top-3 未命中的查询（需要补元数据的候选）

- 期望 `QE-008`：查询「任务 19055 不可达接取前置审计。任务 19055 的 finished quest-id="19054" 前置已在…」 → QE-039(4.1258), QE-023(3.5832), QE-042(3.4339)
- 期望 `AR-006, AR-008`：查询「游戏内 JFR 热点分析 / Gameplay JFR hotspots。- 发起者 ： NpcMoveControll…」 → QE-018(1.3067), QE-004(1.2644), QE-029(1.1803)
- 期望 `AR-001`：查询「P0–P2 改造后 JFR 量化记录 / JFR after the P0–P2 refactor。日期 / Date:…」 → ENV-005(2.2514), QE-041(1.6074), AR-012(1.6015)
- 期望 `AR-011`：查询「Spring 配置层解耦与碎片调度任务收拢记录（2026-09-18）。根据 Spring 迁移遗留清单（ 2026-0…」 → QE-031(1.9426), QE-030(1.5858), QE-025(1.4919)
- 期望 `QE-007`：查询「任务 2392 可选收集物清理与领奖卡死修复审计。在任务 1922 与 2947 的可选工作物品阻断修复及后续 29 个…」 → QE-031(2.093), QE-020(1.7795), QE-025(1.5863)

### 低信号文档（16 份，未计入命中率）

- `.agents/summary/architecture-performance-refactor/2026-09-15-allocation-and-null-gate.md`（期望 AR-007）
- `.agents/summary/architecture-performance-refactor/2026-09-15-p0-p2-implementation.md`（期望 AR-004, AR-005）
- `.agents/summary/quest-10031-zone-mission-broadcast/2026-09-15-zone-mission-end-broadcast-self-target.zh-CN.md`（期望 QE-015）
- `.agents/summary/quest-1192/2026-09-14-work-item-migration-audit.zh-CN.md`（期望 QE-011）
- `.agents/summary/quest-14045-14046-movie-page-turn/README.md`（期望 QE-013）
- `.agents/summary/quest-acceptance/1006-2026-09-10-client-accepted.md`（期望 QE-006）
- `.agents/summary/quest-acceptance/10501-2026-09-19-client-accepted.md`（期望 QE-041）
- `.agents/summary/quest-acceptance/11468-2026-09-16-final-counter-client-accepted.md`（期望 QE-018）
- `.agents/summary/quest-acceptance/1220-2026-09-16-client-accepted.md`（期望 QE-017）
- `.agents/summary/quest-acceptance/1370-2026-09-13-client-accepted.md`（期望 QE-009）
- `.agents/summary/quest-acceptance/14023-2026-08-28-client-accepted.md`（期望 QE-001）
- `.agents/summary/quest-acceptance/14045-14046-2026-09-14-client-accepted.md`（期望 QE-013）
- `.agents/summary/quest-acceptance/15001-2026-09-19-section0-report-row-client-accepted.md`（期望 QE-040）
- `.agents/summary/quest-e2e/triage.md`（期望 QE-001）
- `.agents/summary/quest-engine-startup-debug/README.md`（期望 QE-014）
- `.agents/summary/taloc-hollow-updraft/2026-09-14-2f-updraft-restore.zh-CN.md`（期望 IR-010）

### 多 Pattern 共用证据

- AR-006、AR-008 共用 `.agents/summary/architecture-performance-refactor/2026-09-15-gameplay-jfr-hotspots.md`：排名 miss
- AR-004、AR-005 共用 `.agents/summary/architecture-performance-refactor/2026-09-15-p0-p2-implementation.md`：排名 1
- AR-012、ENV-005 共用 `.agents/summary/legacy-config-mirror-fix/README.md`：排名 1
- QE-012、QE-016 共用 `.agents/summary/quest-10032/2026-09-15-section0-stage-correction.zh-CN.md`：排名 1
- QE-035、QE-038 共用 `.agents/summary/quest-counter-audit/2026-09-18-counter-kill-and-repeat-gate-alignment.zh-CN.md`：排名 1
- QE-020、QE-021 共用 `.agents/summary/quest-systemic-audit/2026-09-17-systemic-quest-family-audit.zh-CN.md`：排名 1

## 明细

| 查询（截断） | 期望 | 排名 | 命中 |
|---|---|---|---|
| 固定乘坐交互 load fail 修复记录。- NPC 702685 （攻城炮 / Rentus E… | `AIM-006` | 1 | `AIM-006` |
| 启动加速：并行加载、刷怪并行与 Spring 锁消除 / Startup speedup: para… | `AR-001` | 1 | `AR-001` |
| 遗留配置镜像跨服务覆盖修复（database.url 泄漏到游戏服）。- 启动本身成功（login/… | `AR-012, ENV-005` | 1 | `AR-012` |
| 本地化日志参数契约：异常必须回到日志调用（2026-09-19）。批次把 583 处日志调用里被 I… | `AR-013` | 1 | `AR-013` |
| 任务 10032：击杀 Celestius 后卡斯帕的幻影（799503）不出现。玩家在塔洛克空洞（… | `IR-011` | 1 | `IR-011` |
| 任务 11468/21468 塔洛克物品计数客户端 SECTION 对齐。- 实测：使用 （Neit… | `QE-012` | 1 | `QE-012` |
| 任务 10032/20032 真机修正：阶段必须留在 SECTION 0。服务端 步数=64 （当前… | `QE-012, QE-016` | 1 | `QE-012` |
| 任务 11468/21468 三维计数最后一击进入 REWARD。- 实机 GM 信息：任务 114… | `QE-018` | 1 | `QE-018` |
| 任务 19638 重启后无法接取根因排查与修复报告。玩家 Ww （ID: 153807）在完成 19… | `QE-019` | 1 | `QE-019` |
| 全服任务存量缺陷与孤例修复系统性排查报告 (Systemic Quest Family Audit)… | `QE-020, QE-021` | 1 | `QE-020` |
| 2026-09-18 任务 51022 商团货物箱子 ACTION ITEM USE 启动门禁修复。… | `QE-033` | 1 | `QE-033` |
| Quest Systemic Goal 进度台账（GOAL PROGRESS）。 轴 audit 范… | `QE-034` | 1 | `QE-034` |
| 2026-09-18 剩余 10 项 questEngine 失败收口：计数器真端合同与门禁口径。 … | `QE-035, QE-038` | 1 | `QE-038` |
| Enter-Zone 迁移接取 owner 回归修复。- 任务 18300 在 NPC 804699… | `QE-039` | 1 | `QE-039` |
| 任务 15001「Lending Both Hands」双计数未推进 SECTION 0 修复。- … | `QE-040` | 1 | `QE-040` |
| SECTION 0 报告行闭环系统性审计（同类问题全量排查）。本次执行证据（客户端数据为机器本地解包… | `QE-040` | 1 | `QE-040` |
| 任务 10501「被毁的遗迹」上交证物后必须重新对话（2026-09-19）。- 玩家侧: 接过龙族… | `QE-041` | 1 | `QE-041` |
| Quest 30721 接取失败：check item 被误当成 inventory item na… | `QE-042` | 1 | `QE-042` |
| 任务 19055 不可达接取前置审计。任务 19055 的 finished quest-id="1… | `QE-008` | 5 | `QE-008` |
| P0–P2 改造后 JFR 量化记录 / JFR after the P0–P2 refactor。… | `AR-001` | miss | `-` |
| 游戏内 JFR 热点分析 / Gameplay JFR hotspots。- 发起者 ： NpcMo… | `AR-006, AR-008` | miss | `-` |
| Spring 配置层解耦与碎片调度任务收拢记录（2026-09-18）。根据 Spring 迁移遗留… | `AR-011` | miss | `-` |
| 任务 2392 可选收集物清理与领奖卡死修复审计。在任务 1922 与 2947 的可选工作物品阻断… | `QE-007` | miss | `-` |

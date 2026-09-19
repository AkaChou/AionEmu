# Active Context (当前攻坚上下文)

记录当前正在推进的任务与未解决的问题焦点。跨 Agent 接力时，先读此文件了解当前状态。

> last_updated: 2026-09-20
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

## 交接规则 (Handoff Rules)

- 本文件只记录当前未完成事项，不承载已经验证的长期模式。
- 焦点变化时更新 `last_updated`、`status`、阻塞原因和下一检查点；完成后标记 `DONE` 或删除已解决条目。
- 分支名、未提交路径和运行环境属于当前 checkout 的上下文，不能直接升级为跨 checkout 的永久规则。
  - **已沉淀**：任务引擎启动阶段针对 `1722.xml`（同 NPC 同阶段无优先级重叠转换）与 `3940.xml`（`<counter>` 导致 source 节点省略声明退化为 `START:0` 投影碰撞）崩溃已彻底排查修复并提炼为 `QE-014`（见 [patterns/quest-engine.md](patterns/quest-engine.md)），全量 6193 条 EXECUTABLE 任务独立编译通过。

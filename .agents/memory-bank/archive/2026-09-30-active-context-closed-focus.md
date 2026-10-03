# Active Context 已关闭焦点归档（2026-09-30）

> status: ARCHIVED
> scope: AionEmu-test `quest` checkout；2026-09-23 版 `activeContext.md` 中已关闭的焦点条目
> source: `.agents/memory-bank/activeContext.md`（2026-09-23 版，见 git 历史）
> last_verified: 2026-09-30
> replacement: 条目内引用的 Pattern ID（AIM-00x / IR-00x / CPK-003）与 `.agents/summary/**` 主题记录
> read_by_default: no

归档准则见同目录 `README.md`：本文件保留出处与结论，不作为当前规则直接执行。

## 2026-09-30 生成物清理提示

> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

## 已沉淀 / 已验收（AI 移动与任务实例）

  - **已沉淀**：跟随 NPC 寻路轨迹（Breadcrumb trail）与护送 AI 选型优化已修复并提炼为 `AIM-001`–`AIM-003`（见 [patterns/ai-movement.md](../patterns/ai-movement.md)），证据留在 `docs/movement/escort-follow-movement-repair.md`。不再作为未完成焦点。
  - **已沉淀**：Taloc's Hollow 卵的脱战抖动与孵化物过早消失已修复并提炼为 `AIM-004`（不可移动 NPC 不因够不着放弃目标）与 `IR-009`（pattern 子对象 `live_time` 与生成者状态重置解耦）；聚焦测试 95 例 + 客户端实机验证均通过，证据留在 `.agents/summary/talocs-hollow-mosqua-egg/2026-09-16-egg-disengage-and-summon-live-time.zh-CN.md`。不再作为未完成焦点。
  - **已验收**：任务 10032 击杀 Celestius 后卡斯帕的幻影 799503 不出现 → `RetailPatternAI2` 增加“死亡/消失事件链子对象不随生成者状态重置删除”护栏（`IR-011`）+ 实例层按真端动作幂等补刷（`RetailPatternAI2#spawnRetailActionNpc`，IR-010 的同类适配器）；2026-09-16 客户端实机复验通过（幻影现身、10032 正常完成），证据在 `.agents/summary/quest-10032/2026-09-16-celestius-death-spawn-caspa-ghost.zh-CN.md`。
  - **已验收**：任务 15300/25300 步骤 7「消灭盘龙巢穴的奥里萨」在爆发/一击致死时跳过阈值变身（237230 不生成 237231），已给 `immortalOrissanAI2` 加死亡兜底并批量补齐同族 8 个阈值变身 AI（死亡路径与阈值路径共用同一个 `*Once()` 闸门），Q 实例奥里萨死亡场景改为对实际生成的爆破手（209711/209776）喊话；聚焦测试 11 例全绿，不变量沉淀为 `AIM-007`。**2026-09-19/20 用户客户端确认 15300 全程顺利完成（含领奖）**，证据在 `.agents/summary/quest-15300-orissan/2026-09-19-immortal-orissan-death-fallback.zh-CN.md`。

## 已收口（性能线 D 项）

  - **已收口（性能线）**：D 项"每实体预制容器"懒物化共 8 个切片（12.15–12.23）已全部提交并复测验收（12.25）：
    存活堆 **−133 MB / −387 万对象**（且复测轮生物多 2.2%）、单位 CPU 样本分配 **−34%**、
    `Buffer.checkIndex` 7.5%→0%、`Arrays.fill` 4.5%→0%、容器/锁相关 CPU 帧→0，窗口内 0 异常；不变量沉淀为 `AR-010`。
    **寻路不再改动**（硬约束：不降路径质量）：A\* 的 87.5 MB/300s 属预热性常驻增长（`SearchWorkspace` CPU 帧仅 0.4%，
    `PathData$MapData$Node` 池高水位 64.9 万→105.8 万），缩池只会把常驻换成下次重新分配；唯一零质量候选是
    "手写开放集比较器（同全序）"，评估仅 2–5% 总 CPU，本轮判定不做。

## Next Steps #3：P0–P2 架构改造（已全量验证并提交）

3. **P0–P2 架构改造：已全量验证并本地提交（本轮收尾）**：
   - 记录见 `.agents/summary/architecture-performance-refactor/2026-09-15-p0-p2-implementation.md`；跨域不变量已沉淀为 `AR-004`（Netty 读缓冲 limit、直连 initialized/onDisconnect、writeData 后禁止 flip + 小端载荷、交付切接收方 ServiceContext）。
   - 端到端验收通过（18:47 实例，真实客户端）：客户端登录 → 账号认证（内嵌直连）→ 进入世界，窗口内 0 ERROR/WARN；聊天服与登录服握手、登录服心跳线程均正常。
   - 全量 `mvn test`：`3247` 例，`13` 失败（全部经 HEAD `2f0752248` 干净副本基线复现，属既有 quest/AI 数据与审计闸门欠账），本次改造引入的 7 例已修复并沉淀 `AR-005`。
   - 收尾清理已完成：`MapRegion.getObjectsSnapshot()` 删除、`PacketProcessor` 去掉 `LinkedList` 强转。
   - **既有 13 例失败仍未处理**（quest 1722/1367/3935/80805/10032、SETPRO 领奖审计、Retail AI 定义计数 134/133、WorldScoped waypoint 3206/3207、windstream 兼容映射、Theobomos 编队），如需修复应另开任务。

## Next Steps #5：客户端血条数字显示（已实机验收并收口）

5. **客户端血条「数字」显示（Game.dll v3）：已实机验收并收口**：
   - **已交付并确认有效**：`patch/Game.dll`（29,795,000 字节，MD5 `71a146481980f284e1144967037e3ab1`
     ＝ 原版 + VIP + 3 处数字改动，与原版逐字节差 502 处）；2026-09-23 用户实测
     **窗口内血条（目标窗口/组队/基础状态栏）出现数字**，客户端稳定。部署 = 覆盖 `bin64/Game.dll`，
     回滚 = `bin64/Game.dll.bak`。
   - **2026-09-23 补丁目录精简（用户要求「只留最终的」）**：`patch/` 现仅剩
     `Game.dll`（唯一二进制补丁）、`Levels/lf2a/Level.pak`（天空修复）、`L10N/CHS/Data/data.pak`（⛔ 暂不可部署）；
     已删除 `Game.hpnum.dll`、`Game.vip-hpnum.dll`（内容并入 `patch/Game.dll`）、`data.numbers.pak`、
     `data/Npcs/npcs.pak`、`data/ui/game/game.pak`，重建方法保留在 `patch/patch_documentation.md`。
   - **用户已接受该形态为交付**（「显示数字就行了」）；**头顶世界血条的数字不在本轮范围**——它由
     NPC 显示系统手动绘制（`0x108c37df`→`0x108c3ed0`→`0x108c2250`），从不调用数字文本函数
     `0x1097d290`，需**代码注入**才能实现；若日后要做，两条路：①索取 5.8 64 位现成补丁做字节对比；
     ②x64dbg 跟 `Game.dll+8C2250` 取子控件虚表与构造点。
   - **机理**（模式卡 `CPK-003`）：`num_type`(→`+0x8d4`，default/small/micro) + `value_type`(→`+0x364`)
     由 UI 数据属性写入；控件更新函数 `0x108e125d` 读 `num_type` 后调 `0x1097d290` 画数字。
   - **已撤的两个备选包**（2026-09-23 精简时删除，配方留在 `patch/patch_documentation.md`）：
     `data.numbers.pak`（目标窗口数字，基准 = 客户端现用 data.pak、差异条目 = 1，只在「不想动 DLL」时才需要）
     与 `data/Npcs/npcs.pak`（非必需——世界血条默认就有）。
   - **纪律（本轮血的教训，`CPK-001`）**：pak 交付物必须以**客户端现用文件**为基准做单条目替换，
     出货前核对差异条目数 == 1。上一轮误用仓库版（差 60 条目：任务对话 HTML + Strings/npcs/UI）
     整包发出 → 客户端崩溃。仓库版 `patch/L10N/CHS/Data/data.pak` 曾只撤掉「数字」一处而非真正回滚，
     经用户指出后已 **`git checkout` 回滚为提交版本**（95,915,168 / `9e624783…`，23,271 条目）；
     回滚后仍与客户端原版差 **58 个条目**（55 `Dialogs/*` + 2 `Strings/*` + 1 `npcs/npc_mesh_replace.txt`）
     → 标记 ⛔ **仍不可部署**，启用只能逐条目二分定位。客户端现用文件（94,240,820 / `b47db70c…`）
     = 原版 + 勾选框，与客户端原版仅差 1 条目。
   - 证据：`.agents/summary/client-hp-display/2026-09-23-client-hp-display-switch.zh-CN.md`（§12–§16）。


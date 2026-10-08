# zone-mission-end 广播残留无路由目标清理（2026-10-08）

> 主题：实机 10520 领奖触发 `AFTER_COMMIT BroadcastZoneMissionEnd failed` 的根因修复与目录级门禁固化。
> 同类 = QE-015（2026-09-15 10031 自含目标修复）；本批 = 同 Pattern 在 9321e7663 退役批次后的回归清欠。

## 1. 现象

- 实机（玩家 Kk，questId=10520，NPC 806076，reward→reward USE_OBJECT 领奖入口）：
  - `WARN QUEST_RUNTIME - typed 任务 10520 已提交，但有 1 个提交后动作失败`
  - `WARN QUEST_AUDIT - ... 阶段 AFTER_COMMIT 失败，动作=BroadcastZoneMissionEnd`
  - `QuestAfterCommitException: after-commit action BroadcastZoneMissionEnd failed for player 151512 quest 10520`
- 任务本身已提交成功；只有提交后广播动作失败。

## 2. 根因链

1. `TypedQuestAfterCommitPort.execute` → `broadcastPort.broadcastZoneMissionEnd` →
   `QuestProductionDispatcher.dispatchOwners(ZoneMissionEnd)`：逐目标投递，
   `success &= !owners.isEmpty() && !failed` ⇒ **目标缺路由 = 整次广播硬失败**（`QuestProductionDispatcher.java:316-326`）。
2. 9321e7663（2026-09-29）批量物理退役 29 个 DataDriven 遗留 XML（含 **10526/20526/20032/20033/20034**）
   并从 catalog 拔除；这些任务翻成 native 车道后**不再有 typed 定义**，
   而 `QuestEngine` 只为「定义携带 ZoneMissionEnd transition」的任务注册路由（`QuestEngine.java:2655`），
   `RetailQuestDriver`（表驱动）不生成该路由 ⇒ 目标侧路由消失。
3. 但广播源清单没跟上退役批次：
   - `10520.xml` 领奖入口广播 `10521 10522 10525 10526 10527 10528 10529 10530`（10526 无定义）
   - `20520.xml` 同构（20526 无定义）
   - `20031.xml` 4 处广播 `20032 20033 20034 20035`（前 3 个无定义；即链式发放批 README 残留 #3）
4. QE-015 的 2026-09-15 验收只做了一次性人工目录审计（24 处广播 0 违规），**未固化为常驻门禁**，
   所以 9-29 退役引入 6 处违规时零红灯。

## 3. 行为无损论证（为何删目标不丢真端语义）

- 10526 真端接取条件 = `finished_quest_cond=Q10525`（`quest_chain_acquire_edges.xml:107-111`，
  `source=retail-finished-cond`），不是 10520；广播命中的语义 = 「前序已完且自身达标即接取」。
- 2026-10-08 落地的链式发放面（`DataDrivenNativeRuntime.onQuestStateChanged` 尾部
  `recheckChainAcquires` **全表重走** + `onQuestCompleted` 定向走）在**任意任务完成**时按
  「全部前序 COMPLETE + unfinished 条件全清」评估 10526 ⇒ 10520 完成时同样补发，覆盖真端边界场景。
- 已退役/native 行（20032 冻结、20033/20034 文档化死边）同理由链式面承担；README 残留 #5 已定口径
  「zone-mission-end 广播半面不实现」。

## 4. 全目录审计（修复依据）

对 `definitions/quests/*.xml` 全部广播行做「目标存在性 + zone-mission-end 路由」双维扫描，
违规面恰好 3 文件 6 行（无其他污染，`schedule-event-quest-refresh` 目标全干净）：

| 文件:行 | 修复 |
|---|---|
| `10520.xml:270` | 目标剔除 `10526` |
| `20520.xml:269` | 目标剔除 `20526` |
| `20031.xml:395/412/429/445` | 目标收敛为 `20035`（README 残留 #3 的「后续单独小改」落地） |

## 5. 变更清单

- XML：`10520.xml` / `20520.xml` / `20031.xml`（上述 6 行）
- 测试锁定面同步：
  - `Quest10520ClientDialogAlignmentTest`：两条期望数组剔除 10526/20526
  - `Quest20031ZoneMissionBroadcastTest`：`ASMODIAN_FOLLOW_UPS={20035}`；javadoc 与路由核对循环改为
    「全部目标必须拥有路由」（移除已退役目标跳过逻辑）
- 新增门禁：`ProductionBroadcastTargetsGateTest`——遍历生产 catalog 全部可执行定义，
  断言每条 `BroadcastZoneMissionEnd` 动作：①不自含；②目标为 catalog 内可执行 typed 定义；
  ③目标拥有 `ZoneMissionEnd` 路由；④广播动作总量 ≥ 下限（当前实测 14，防清点面塌空壳）。
  QE-015 的一次性人工审计自此固化为常驻断言。

## 6. 验收状态

- **单测已通过**（2026-10-08 用户授权，经 IDEA MCP 运行，失败数全 0，合计 17/17）：
  `ProductionBroadcastTargetsGateTest` 1/1（新门禁）、`Quest10520ClientDialogAlignmentTest` 8/8、
  `Quest20031ZoneMissionBroadcastTest` 1/1、`QuestProductionStartupGateTest` 2/2、
  `RetailOwnershipGateTest` 5/5（IDE 间歇性进程启动失败，重试即过，与本批改动无关）。
- **实机 PENDING**：服务端重启后复测 10520 领奖入口（USE_OBJECT）应无 AFTER_COMMIT 告警；
  10526 由链式面在 10525 完成（或任意完成触发的全表重走）时发放。
- Playbook/Pattern：同 QE-015 Pattern，按 quest-repair 规则 10 不新增案例；
  实机验收通过后再评估是否把「owner 翻转/退役批次必须同步收敛广播清单」补进 QE-015 的 first_check
  并登记新门禁 `ProductionBroadcastTargetsGateTest`。

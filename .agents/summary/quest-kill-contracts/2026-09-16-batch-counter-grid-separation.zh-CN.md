# 全量纯击杀 counter-grid / counter 任务批量治理记录 (94 个任务)

## 1. 治理背景与目标

根据 Aion 5.8 客户端契约 `docs/quest/client-dialog-mapping/client-monster-progress-contracts.csv`：
大量任务在客户端声明了 `Progress(SECTION_0==0; SECTION_1<N)`，明确定义：
- `SECTION_0`（服务端的 `var0`）是任务阶段（Step），阶段 0 为击杀怪兽，阶段 1 为向 NPC 报告；
- `SECTION_1`（服务端的 `var1`）是当前阶段的击杀计数（`0..N`）。

历史迁移由于脚本过度简化，机械地将纯击杀任务写成了单字段 `<counter-grid field="var0" required="N"/>` 或 `<counter field="var0" required="N"/>`。
导致两个致命问题：
1. 击杀 1 只怪使 `var0` 从 0 变成 1，客户端解析到 `SECTION_0=1`，瞬间判定第一步（杀怪）完成，提前切换到下一步；
2. 服务端状态仍停留在 `k1` 中间态，NPC 根本没有对话响应，两头堵死。

---

## 2. 治理范围与策略

本次针对类别 1（纯击杀 `counter-grid` 与 `counter`）全部 **94 个** 任务进行批量重构与契约对齐：
- **特别任务系列**: 19631~19636（因特尔蒂卡特别任务 1~6）、19638~19642（英吉斯温特别任务 2~6）；
- **要塞与深渊突袭系列**: 13840~13880、13920~13946（深渊要塞守卫与突袭击杀日常）；
- **主神殿/副本日常系列**: 42000~42013、42100~42113；
- **露娜/活动日常系列**: 46533, 46534, 50068~50105, 51076~51098。

重构规则（黄金三角标准）：
1. 扩展 `<progress>` 声明 `var0`（步骤）与 `var1`（击杀计数，max=N）；
2. `started` 节点只投影 `START`，不得锁死实时计数字段；`reward` 节点投影 `var0=1, var1=N, status=REWARD`；
3. `priority 1` 自环递增（0..N-1 杀）：动作显式执行 `<set-variable field="var0" value="0"/>`（自动纠正老脏数据）+ `<increment-variable field="var1" delta="1"/>`，提交后 `PACKET_ONLY` 同步；
4. `priority 0` 终结直达（第 N 杀）：动作显式执行 `<set-variable field="var0" value="1"/>` + `<set-variable field="var1" value="N"/>`，目标直达 `reward` 节点，提交后 `LEVEL_AND_VISIBILITY_REFRESH`；
5. 满计数恢复路由：在 `started` 状态下具备针对 `var1 >= N` 的安全报告路由；
6. 完整保留原接取（`NPC_START`）与完成奖励发放（`npc-complete` 或完成 transition）。

---

## 3. 验证与门禁

1. **静态 XSD 模式校验**:
   全量 94 个生成的 XML 经过 `xmllint --schema ...` 逐文件校验，通过率 100%（0 错误）。
2. **全量生产目录编译门禁**:
   运行 `ProductionCatalogWhitelistVerificationTest`：
   `PRODUCTION_COMPILE_OK=6193, PRODUCTION_COMPILE_FAILURES=0, PRODUCTION_WHITELIST_VIOLATIONS=0`。
3. **新增契约审计单测**:
   运行 `QuestMonsterProgressContractAuditTest`，12 个核心特别任务（19631~19642）及全量契约测试全部通过。

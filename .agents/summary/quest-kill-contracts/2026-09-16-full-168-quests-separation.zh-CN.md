# 全量实锤多击杀任务批量治理汇总记录 (累计 168 个任务)

## 1. 治理成果全景

经过两批系统化工具重构与全量门禁检验，全服所有存在多击杀变量错配（客户端要求 `SECTION_1` 计数，但服务端误用 `var0`）的实锤问题任务**已全部 100% 重构完毕并合闸通过**：

- **第一批（94 个任务）**：
  - 类别：单字段 `<counter-grid field="var0">` 与 `<counter field="var0">` 纯击杀任务；
  - 覆盖范围：天族特别任务 19631~19642、深渊要塞突袭 13920~13946、主神殿副本日常 42000~42113 等。
- **第二批（74 个任务）**：
  - 类别：手写 `<increment-variable field="var0"/>`（59 个）与 `<kill-chain>` 链式硬编码（15 个）；
  - 覆盖范围：魔族特别任务 29631~29642、萨尔潘要塞军团日常 35052~35065、卡特拉姆要塞军团日常 45052~45065、提亚马兰塔之眼日常 36532~36536、露娜/军团赏金日常 46531~46548，以及周常消灭水灵 13759~13769 系列等。

累计重构任务总数：**168 个任务**！

---

## 2. 治理核心机制（黄金三角标准）

所有 168 个任务统一对齐 Aion 5.8 客户端物理契约（`Progress(SECTION_0==0; SECTION_1<N)`）：
1. **字段隔离**：
   `var0`（offset=0, width=6）严格作为步骤变量；`var1`（offset=6, width=6, max=N）严格作为击杀计数；
2. **老脏数据自愈**：
   击杀自环（`priority 1`）动作显式执行 `<set-variable field="var0" value="0"/>` + `<increment-variable field="var1" delta="1"/>`，任何已有脏数据（如 `var0=1, var1=0`）只要击杀任意 1 只怪即可立刻在客户端纠偏刷新；
3. **终击直达 reward**：
   第 N 击（`priority 0`）动作设置 `<set-variable field="var0" value="1"/>` + `<set-variable field="var1" value="N"/>`，直达 `reward` 节点并触发 `LEVEL_AND_VISIBILITY_REFRESH`；
4. **满计数安全恢复**：
   在 `started` 状态下增加针对 `var1 >= N` 的对话报告路由，杜绝历史存档卡死；
5. **对话与发奖闭环**：
   `reward` 状态下收到 `QUEST_SELECT(31)` 展示 `DEFAULT_SUCCESS`，点击 `SELECT_QUEST_REWARD` 弹出奖励窗口并完成。

---

## 3. 门禁验证结果

1. **XSD 模式校验**: 全部 168 个生成的 XML 文件经 `xmllint --schema quest_definition.xsd` 校验，100% 通过（0 错误）。
2. **全量生产目录 6,193 门禁**: `ProductionCatalogWhitelistVerificationTest` 编译 6,193 个任务，结果 `PRODUCTION_COMPILE_OK=6193, FAILURES=0, WHITELIST_VIOLATIONS=0`。
3. **新增契约审计单测**: `QuestMonsterProgressContractAuditTest` 严格断言天魔两族全部 24 个特别任务（19631~19642, 29631~29642）与全量契约表，全部绿色通过（耗时仅 0.3 秒）。

# SETPRO 直达奖励根因门禁

- 状态: 实现完成，静态门禁已通过，待真实客户端验收（IMPLEMENTED_PENDING_CLIENT_ACCEPTANCE）
- 日期: 2026-09-15

## 症状

客户端在任务仍处于 `START` 时显示材料/步骤选择页，但服务端把可见的 `SETPRO` 按钮直接映射到 `REWARD`，导致中间步骤、材料条件或后续 NPC 对话被跳过。该形状曾在多个迁移批次中由机械补齐按钮路由引入。

## 证据规则

`QuestPrematureRewardRouteAudit` 只审计高置信形状:

- 生产任务、客户端 active/exact 页面中可见的 `SETPRO` 动作；
- 来源节点投影为 `START`，目标节点投影为 `REWARD`；
- 路由没有条件和事务动作；
- 同一来源节点另有不同目标的 `START` 阶段，或同一 NPC/同一来源/同一奖励节点在同一客户端页上有多个无条件选择。

拥有材料条件/扣除动作的正常直接领奖路由放行；不同奖励节点的显式奖励选择（例如 2911 的 `reward0/reward1`）放行，避免把合法分支当成缺陷。审计结果带任务、来源、NPC、动作、原因和客户端页证据。

## 本次修复

- `1722`: 删除 NPC 278560 的旧 `started -> reward SETPRO3`，保留 NPC 278544 的真实 `s2 -> s3 SETPRO3`。
- `10527`: 删除旧 `s2 -> reward SETPRO3`，保留 `s2 -> s3 SETPRO3`。
- `20527`: 删除旧 `s2 -> reward SETPRO3`，保留 `s2 -> s3 SETPRO3`。
- `20528`: 删除旧 `s6 -> reward SETPRO7`，将真实 `s6 -> s7` 路由对齐为客户端 `SETPRO7`。
- `2332`: 三个材料按钮分别增加 `has-item` 和对应精确扣除，失败时回显选择页；保留当前客户端对齐的公共奖励元数据。
- `1367`: 恢复三组奖励分支、三条材料条件和奖励预览所有者，避免三个按钮共用无条件 `REWARD` 路由。
- 新增全量客户端门禁与专项审计回归样例。

## 静态结果

- 6231 个生产任务 XML 可由 Python XML parser 读取。
- 针对该规则的当前 XML 扫描结果为 0 条高置信缺口。
- `git diff --check` 通过。
- 本轮未执行 Maven/编译/测试；未启动或重启服务；未进行真实客户端验收；不包含推送。

## 后续

授权构建后先运行专项 `QuestPrematureRewardRouteAuditTest` 与 `QuestClientContractGateTest`，再更新生产目录资源并做客户端回归。客户端验收需要分别覆盖材料不足、正确材料按钮、中间阶段按钮和最终奖励 NPC 对话。

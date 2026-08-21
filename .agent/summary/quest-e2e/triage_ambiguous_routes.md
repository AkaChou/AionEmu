# AMBIGUOUS_ROUTE 分诊报告（基于 398e2681d 后刷新的 E2E 报告）

> 生成时间: 2026-08-21
> 报告来源: `target/quest-e2e/quest-e2e-report.jsonl`（08-21 12:26 后再次刷新，PASS=332289）
> 范围: AMBIGUOUS_ROUTE 160 行 / 89 qids

## 关键结论

160 行全部为 **fixture 归因保守标记**，不是任务 XML 缺陷：

- 每行 `observedStatus` 均达到该事件的合法终态，事务 trace 完整（commit + after-commit + close）。
- `ALTERNATE_TRANSITION_MATCHED` 表示期望 transition 与实际匹配 transition 不同但同事件同目标方向；
  抽查全部类别后未发现行为错误或不可达路由。

## 类别分布

| 类别 | 代表 qid | 行数 | 形状 | 性质 |
|---|---|---:|---|---|
| 职业分支奖励边 | 13833/13834/23832/23834 | 64 | `reward->complete` 同事件按 `advanced-class-is` 互斥分支，各职业不同奖励物品 | retail 正确形状；fixture 未预设职业事实，无法归因到唯一分支 |
| 击杀完成边 sibling | 3118/3120 等 | ~49 | `<counter>` 展开的计数边与完成边共享事件；fixture 在边界 pack 值探测时归因到完成边 | 行为正确（第 N 只击杀进 REWARD 正是 retail 语义） |
| 步骤边 sibling | 45010/45017/45018/45024/45026 | ~12 | `SETPRO1` 同时有条件推进边（priority=0, started→v1）与兜底对话边（started→started） | 条件优先匹配是预期协议顺序 |
| 接取动作 sibling | 2290 等 | 8 | `QUEST_ACCEPT_1(1002)` 探测时期望停留 unaccepted，实际匹配接取边进 started | 接取即推进是正确行为 |
| 提交条件边 sibling | 21030/24123 | 4 | `CHECK_USER_HAS_QUEST_ITEM`(39)/20002 提交边与进行中提示边的归因差异 | 集齐物品提交进 REWARD 正确 |
| 其他孤例 | — | 余量 | 同上各类变体 | 同上 |

## 处置结果（2026-08-21 实施）

1. **不修改任务 XML**——无缺陷可修，批量改动反而会破坏职业奖励分支等正确形状。
2. **fixture 归因改进已实施**：`QuestCondition.listsAreMutuallyExclusive/areMutuallyExclusive`
   提取为公共 API（编译器委托同一实现）；`QuestE2eBatchAudit` 对 ALTERNATE 归因增加
   `exclusiveSibling` 判定——同组源 status 下，条件相同（双协议注册）、条件互斥（职业分支）、
   或显式优先级兄弟归入新信息性状态 `EXCLUSIVE_SIBLING`。
3. 刷新后分布：AMBIGUOUS_ROUTE 160 → **4**，EXCLUSIVE_SIBLING=156。
   剩余 4 行为真实待取证项：
   - 19900/29900：`reward↔legacy-reward` 登入摆动边（遗留奖励节点兼容结构），需单任务核对；
   - 80030/80033：`EVENT_QUEST_REFRESH` complete→complete 与 unaccepted→started 的活动刷新归因，
     需单任务核对活动任务机制。
   回归测试：`QuestExclusiveSiblingAttributionTest`。

## 与 Playbook 的关系

- 本批不产生代表案例（无修复）。
- 若后续实施 fixture 归因改进，属共享 runtime/工具层修改，需附生产目录级审计对比（改进前后行数分布）。

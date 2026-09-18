# P6 前置与互斥条件审计：首批修复

## 范围与证据

- 来源：`/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest.xml`，SHA-256 `0edade9f28411d73ffa8e9e84694e908d436852e11d77fb68a85ea2a5352025f`。
- 生产 XML 6,222 个。保留 finished 的 OR 分支、字段内 AND、全局 prerequisites、奖励结局以及公共互斥条件。
- 既有参考：QE-021；`043426b47` 的 2303 分组转换与 1510 取消前置处理。首批互斥族不含多分支 finished，不受其推断边界影响。
- 审计工具：`audit_prerequisite_axis.py`；解析测试：`test_audit_prerequisite_axis.py`；剩余清单：`prerequisite-axis-audit.tsv`。均保留在本主题目录供后续 P6 使用。
- 前次“166 处”未正确拆分逗号、名称与结局，不作为缺陷数。新审计结果为结构候选，不能作为运行时等价证明。
- `1870` 没有自动补 1868：生产缺失正向引用必须先查版本/取消证据；缺失不等于取消。

## 本批修改

- 1648：完成前置从 1636 纠正为客户端 1643。目录原已写 1643，因此不重复修改该行。
- 6 对共 12 个双向互斥任务完整补齐 unfinished + noacquired：18250/18251、28250/28251、18975/18976、18977/18978、28975/28976、28977/28978。
- XML 仅修改 metadata；状态图、动作、事务和 after-commit 顺序未改变。
- 目录仅更新以上 12 行互斥说明；未删除任务、未将缺失前置替换成无条件可接。
- 资格测试覆盖每对两个方向，对方不存在、NONE、LOCKED 可接；START、REWARD、COMPLETE 不可接；1648 另覆盖错误前置、正确前置进行中和完成。

## 数量

| 口径 | 修复前 | 修复后 |
|---|---:|---:|
| 规范化表达一致 | 5962 | 5975 |
| REVIEW_CONTRACT | 222 | 209 |
| REVIEW_ABSENT_REFERENCE | 31 | 31 |
| NO_CLIENT_RECORD | 7 | 7 |

剩余 247 条均为待复核记录，不能称为 247 个真实缺陷。取消前置、使命运行时前置、结局限制和服务器自建任务需逐类核对。此审计尚未覆盖装备条件、完成次数条件及运行时补充门槛。

## 验证

- IDE 单文件编译：PlayerQuestStartEligibilityPortTest 成功。
- `python3 -m unittest discover -s .agents/summary/quest-systemic-goal -p test_audit_prerequisite_axis.py`：6 项通过。
- `mvn -q '-Dtest=PlayerQuestStartEligibilityPortTest#reciprocalRetailQuestFamiliesRejectActiveAndCompletedAlternatives+undeadWarAlertRequires1643RatherThanUnrelated1636,CompletedQuestPrerequisiteRegressionTest,QuestBatchReportNpcAlignmentTest,ProductionCatalogWhitelistVerificationTest' test`：23 项，0 failure/error/skipped。
- `PRODUCTION_COMPILE_OK=6189`，编译失败、交互对象失败、白名单违规均为 0。
- 首轮运行资格测试整类时暴露既有 10521 用例期望 TITLE_MISSING、实际 START_CONDITION_REJECTED；相关生产 XML 和该断言均未被本批修改，待专项复核，不能宣称资格整类全绿。
- 首轮新增 1648 测试曾重复 addQuest 而未替换已有状态；已改为 setStatus，聚焦复测通过。
- 客户端尚未验收，未启动/停止/重启服务器。未创建提交。

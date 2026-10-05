# IR 测试退役清扫批（TEMP-VERIFY 集群，2026-10-05）

## 缘起

用户报障收口后遗留的存量红：`AcceptAndConfirmationEntryContractTest`（HEAD 即红，
`definitions/quests/15010.xml` 不存在 + `verificationView().find(1877)` 空）。排查发现同形红
不止一类——共 7 个带 `TEMP-VERIFY(view)` 标记的类中 **5 类红 + 1 类零 @Test 死壳**，
全部同根因，随本批一并处理。

## 根因（一句话）

`TEMP-VERIFY(view)` 的「宽松生产视图」= `RetailQuestDriver.overlay(XML目录)` 的**旧语义**
（overlay 曾把真端编译产物并进目录）；P7 步 f 原子切换（715a00136，2026-10-02）后 overlay 已是
**直通**（"无 retail 编译产物，保留任务一律维持 XML"），退役任务（retention owner=RETAIL_TABLE）
从此没有任何 IR——凡 find/compile 退役任务 id 的 IR 断言必然红。批内各红类红点：
`missing production quest definition NNNNN` / `NoSuchFileException 15010.xml` / `find(..).orElseThrow` 空。

## 逐类裁定（三式：整删 / skip+守卫 / 活面重锚；先例 P7「手术删 22 类 DD 方法」+ P8 第一刀「both-red 重锚批」）

| 类 | 红主语 | 处置 |
|---|---|---|
| `AcceptAndConfirmationEntryContractTest` | 1877（RETAIL_TABLE/DD_PVP_GRID）、15010（RETAIL_TABLE/DD_HANDIN_CANONICAL） | 1877 法**重锚 native**（保留方法名：`RetiredQuestIds.contains` + `runtime.owns(1877)` + `acquireTalkInterests` 无 1877 + `pvpSteps` 非空）；15010 半**删**（1636 半保留） |
| `QuestItemSourceContractGateTest` | 26 项硬编码审计里 17 项退役（15010/15012/15043/15070/1932/3547/14121/14201/24121/24152/24242/28836/28838/1870/2870/28739/28740） | 统一经 **`assertTurnInItemsOrRetired`**：目录缺行 → 守卫断言确属退役后跳过；XML 保留行（51021/2232/2239/2289/3013/3088/4542/3217/4217）照常断言 |
| `QuestRefactorRepairRegressionTest` | 24155（RETAIL_TABLE） | 该法**删**（1917/XML 法保留）；`load()` 去 overlay 直取 XML 目录 |
| `JournalRewardRowRepairContractTest` | 16900（首红）等 CONTRACTS 内退役行 + RETAIL_DRIVEN 全 9 行 | 三扫描循环加 **`retiredOnly` skip+守卫**；`retailDrivenRowsCarryNoJournalRepairEdge` **重锚**为「名单每行确属退役」（无 IR ⇒ 修复边结构性不存在）；`definition()` 去 overlay |
| `AlignedMirrorRewardRowContractTest` | 18/18 id 全 RETAIL_TABLE（9 组镜像两侧） | **整删文件**（无残余 XML 主语） |
| `RetailSequentialQuestFamilyTest` | 零 @Test（P8 已删唯一测试法，余 helpers 死壳） | **整删文件** |
| `QuestProductionAcceptProtocolRegressionTest` | （绿） | 未动（主语仍 XML） |

## 守护逻辑

- skip 的前提 = `RetiredQuestIds.contains(questId)`（retention 清单 owner=RETAIL_TABLE 为唯一事实源）
  ——**跳过条件本身就是防名单陈旧守卫**：非退役行缺目录即红，不被静默跳过。
- 重锚只断言 native 公共注册面（`DataDrivenNativeRuntime.owns/routes/acquireTalkInterests/pvpSteps()`），
  行为语义仍归 DD/族门承担（行矩阵冻结、区域注册门等）。
- 方法名全部保留（Playbook `TestClass#method` 引用不断）。

## 验证

- IDEA `build_project`：✅ 零 problem。
- IDEA MCP（exitCode 0）：`AcceptAndConfirmationEntryContractTest` 2/2 ✅、
  `QuestRefactorRepairRegressionTest` 1/1 ✅、`JournalRewardRowRepairContractTest` 4/4 ✅、
  `QuestItemSourceContractGateTest` 3/3 ✅。
- Memory Bank：QE-051 证据补注（AlignedMirror 删除）+ QE-146 新卡（三式口径）+ sync/verify 三步绿。

## 边界与欠账（不在本批）

1. **Playbook checker 14 项存量失败**（P7/P8/minion 批删除测试后未刷新引用；本批零新增：
   本批涉及的 TestClass#method 引用全部有效）。逐项重锚/删行属独立欠账。
2. 历史 summary 报告中对被删类的引用不追改（历史留痕）。
3. 更大范围的存量红（P8 报告口径「73 红类」）由其收录批继续清理；本批只收 TEMP-VERIFY 同形集群。
4. 无客户端行为变更（纯测试面），不需要实机复测。

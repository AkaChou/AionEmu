# P0c-10d：SimpleTalk M3-d 复核 10 行 EQUIVALENT 归零（retired 3909 → 3918 实测）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

- 日期：2026-09-25
- 切片：P0c-10d（对账/收敛切片：**无生产代码改动**，只做登记翻转 + 退役落地）
- 判据来源：P0c-10 M3-d 逐行复核（`p0c10-m3d-recheck.tsv`）中漂移分类 = **EQUIVALENT** 的 10 行——
  真端合成定义与退役前生产 XML 在归一化 IR 层逐字等价（P5-2 交付五页词汇规则口径），
  M3-d `CLIENT_ROUTE`（逐任务客户端合同暂缓）降级对这 10 行不再必要（教条 ③：等价即归零）。

## 1. 交付

1. **登记翻转**：`m3d-downgraded-quests.tsv` 83 → **73**（移除 80029/80344/80351/80360/80575/
   80577/80578/80580/80683/80686，留痕头注）。
2. **保留清单重生成**：`build_retention_list.py` → 10 行 `RETAIL_TABLE/SimpleTalk/OK`；
   全量 6224 = RETAIL_TABLE/OK **3918** + SEMANTIC_GAP 1636 + SCRIPTED 494 + NO_TABLE 176
   （总数含并发批同窗口 1 行反向翻转在飞，非本切片）；两副本逐字节一致。
3. **退役落地**：`p0c10d_zero_m3d_equiv_rows.py`（四重前置断言 → `--dry-run` → 落地）：
   `Path.unlink()` 删除 10 个生产 XML；`quest_definition_catalog.xml` 移除 10 条（余 **2306**）。
4. **收口复跑**：`p0c10_simpletalk_family_closure.py` 全部不变量成立——
   三桶 **REJECT 585 + M3D 73 + REPORT_VARIANT 17 = 675** 与 XML_RETENTION 675 零差集；
   SimpleTalk RETAIL_TABLE **1548**（= 2223 − 675）；分歧表 `CLIENT_ROUTE` 83 → 73（其余 8 码不变）。
5. **验收**：**`verify_retirement.py` = `catalog=2306 directory=2306 retired=3918 sum=6224 — OK`**，
   零悬空生产引用。

## 2. 门禁与归账

| 门禁 | 结果 |
|---|---|
| SimpleTalk 族门禁 | `RetailSimpleTalkGateTest` **3/3 绿** |
| T1（16 类） | **58/59**：唯一 1F = `RetailDataDrivenGateTest.frozenFingerprintsCoverExactlyTheRetiredQuests`（`frozen=718 retired=719`，并发批 DD 车道在飞自因——其车道 T1 在 wave-2 时点曾 59/59，此后其新改动使冻结集差一） |
| T2（10 id 命中，111 例） | 仅 2 失败且皆为**既有基线失败**（零关联本切片）：`ReportToManyDialogRouteRegressionTest`（3914 = P3b SimpleUseItem 行，P0c-9 基线即在）+ `QuestFactRequirementsTest`（19637/19638 = DataDriven 行，P0c-9 基线即在）；**我的 10 id 零命中** |
| T3 clean 副本（`/private/tmp/aion-p10d`，用后已删） | **1976 例 / 41F / 35E / 1 skipped**；方法级失败集 **95 → 76**（vs wave-2 时点）：**新增 0、解决 19**；新增消失的失败全在并发批车道（其对齐测试批量修复 + 对话序在飞）；我的 10 id 零命中 |

## 3. 过程记录（共享主树风险第 4 次显式化）

- **classpath 卫生前置化**：本次落地前主动对齐 `target/classes` 与源树——删除本切片 10 个
  （预期残留）+ `25500.xml`（并发批更早退役的残留）；退役切片跑门禁前先对齐应固化为标准步骤。
- **并发构建竞态实锤**：一次 T2 初跑 13 例 ERROR，根因是并发会话的 maven 构建与本文会话
  同时写主树 `target/`——`RetailIrFingerprint.class`（test 源树类）被其增量编译瞬时删除
  （`NoClassDefFoundError`）；`mvn test-compile` 恢复后复跑即绿。两会话共用主树 `target/`
  的固有风险：门禁以复跑为准，终局证据只认 clean 副本 T3。

## 4. 未验证 / 下一步

- **未验证**：10 行运行时行为与客户端抽检（与全族一致，留 P3 终局抽检统一做）。
- **下一步（大切片）**：**RETAIL_TALK_CHAIN 322 行链式合成**——SimpleTalk 族最大归零面；
  客户端页链登记面已具备（`quest_client_talk_pages` / `quest_client_handin_pages`），
  P5-3 wave B 链语义（stage 梯 + SETPRO/SET_SUCCEED 推进 + var0 阶梯值）为同语义先例。
- 候选：M3-d 余 73 行按 P5-2 交付词汇逐任务归零、CHALLENGE_TASK 哨兵 60 行发放接线、
  SimpleItemPlay 9 行 KEEP 波次推进。

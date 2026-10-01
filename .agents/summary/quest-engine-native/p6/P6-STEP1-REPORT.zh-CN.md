# P6 步骤 1 报告：CombineTask 表装载 + 行集可行性冻结

> 主题：计划 §7 P6 批门第 1 条（本族表行清单、来源 hash、NATIVE_READY 行集与未证行集）+ 第 2 条输入。
> 日期：2026-10-01。分支：`quest`。前置：`p5/P5-REPORT.zh-CN.md`、`p3-prereqs/family-table-shapes.md`。
> 口径：只读真端表 + 仓库静态数据；**本步零切换**（旧编译器、旧金标与 dispatch 全部不变，仅新增装载与冻结门）。

---

## 1. 表来源与逐行装载

| 表 | 根元素 | 行数 | 入仓副本 | 真端源 | 内容一致性 |
|---|---|---|---|---|---|
| `Quest_CombineTask.xml` | `quest_combinetasks` | **574** | `src/main/resources/aion/data/static_data/quest/retail/`（UTF-8，`7e994de9…e8ffc`） | `<真端根>/Map/XML/Quest_CombineTask.xml`（UTF-16，`f4af3e79…08c3`） | `table-source-provenance.tsv` 既有登记：`CONVERTED_UTF8_WHITESPACE_NORMALIZED(P0a token-semantic-equal)`，574 行 |

`NativeQuestTableLoader` 新增本族行模型与装载（逐行原文，缺必填列即 fail-closed）：

```java
record CombineTaskRow(int questId, String devName, List<String> taskNpcNames, String combineSkill,
        int skillPoint, String recipeName, String product, List<String> components)
```

- 必填面（缺失即 `NATIVE_TABLE_PARSE_FAILED`）：`task_npc`（逗号分隔，至少一个名字）、`combineskill`、`recipe_name`、`product`；
  `combine_skillpoint` 缺省为 0（与 `RetailQuestMetadataCompiler` 的 `null → 0` 同口径），负值 fail-closed。
- `give_component1..8` 按**位置保留**（真端 helper `lVar4 = 8`），缺位为 `null`；`product` 保留 `符号 [数量]` 原文
  （符号解析走两通道：先原名、未命中再去 `ITEM_` 前缀，与 P3/P5 同规则）。

## 2. 列填充率（复算，574 行）

| 列 | 覆盖 | 说明 |
|---|---|---|
| `dev_name` | 574 | 韩文原文 |
| `task_npc` | 574 | **全部两名**（天/魔各一，如 5000 = `Anteros,Auminus`） |
| `combineskill` | 574 | 制造技能符号（`weaponsmith` 等） |
| `combine_skillpoint` | 574 | 技能点 1..100 档（14 行/档） |
| `recipe_name` | 574 | 配方符号（`r_ws_q5000`） |
| `product` | 574 | 单槽 `符号 数量`（如 `item_ws_q5000 3`） |
| `give_component1` | 574 | 第 1 分量 `符号 数量` |
| `give_component2` | 152 | 第 2 分量 |
| `give_component3..8` | 0 | 真端数据只有 1/2 两槽（helper 仍允许 8 槽） |

## 3. 行集划分（owner × 可行性）

| 表行 | 退役（owner=RETAIL_TABLE） | XML-only | 未登记 | **NATIVE_READY** | fail-closed |
|---|---|---|---|---|---|
| **574** | **574** | 0 | 0 | **574** | **0** |

七轴逐行全通（574/574）：

1. 接取 NPC 名唯一解析（两名各自唯一，`NativeNpcNameResolver`）；
2. `combineskill` 符号可解（`RetailQuestMetadataCompiler.combineSkillId`）；
3. `combine_skillpoint` 与真端 `quest.xml` 一致（缺省 0 口径）；
4. 产物单槽且 `符号 [数量]` 可解（`RetailItemNameIndex`，两通道规则）；
5. 分量非空且全部可解（1..2 槽）；
6. `(skill, product)` 在 `recipe_templates.xml` 中**唯一**命中（`RetailRecipeIndex`）；
7. 真端元数据交叉一致：产物 = `quest.xml` 单条物品需求（id + 数量）、分量 = `quest.xml` 工作物品列表（id + 数量，按表序）。

**该族无 fail-closed 残余**——与 P4/P5 不同，本族 574 行同形，无需保留不可路由行集。

## 4. 复算与证据

- 冻结门：`src/test/java/com/aionemu/gameserver/questEngine/retail/CombineTaskFamilyRowInventoryGateTest.java`
  —— **独立复算**（不复用 handler / 编译器判定），断言 574/574/574 与空原因直方图；
  证据导出：`-Dp6.out=<path>` 输出逐行 verdict。
- 逐行复算产物：`p6/combine-feasibility.tsv`（576 行 = 2 行表头注释 + 574 行数据，全部 `NATIVE_READY`）。
- 装载门：`NativeQuestTableLoaderTest#loadsFullCombineTaskTable`（574 行 + 5000 逐字段 + 形状冻结：
  双 NPC 574/574、分量 1..8 位置保留、第 1 分量 100%、第 2 分量 152 行、第 3 槽起恒空）。

## 5. 与现状强类型编译器的一致性（步骤 2 的输入）

现有 `RetailCombineTaskDefinitionCompiler.precheck` 的判据与本步七轴同构（NPC 唯一 / 技能一致 / 技能点一致 /
产物单槽可解析 / 分量可解析 / 配方唯一 / 元数据产物与分量一致），并额外把「表行与 `quest.xml` 元数据不一致」
细分成稳定拒绝码。因此**本族 574 行在切换到原生车道时不应出现新的不可路由面**；运行期若出现拒绝，
只能来自装载层 fail-closed（本步已把该类风险前置到装载门）。

## 6. 下一步（步骤 2，另批提交）

1. `NativeRecipePort`（学/忘配方）+ `SimpleCombineTaskHandler`（真端 `FUN_180caac10` 参数序：接取发分量
   并学配方 → 交付产物（持有门）→ 回收剩余分量 → `REWARD` → 领奖扣产物、忘配方 → `COMPLETE`；`abandon → 忘配方`）；
2. `QuestEngine` 原生分流 + `installInterest`；`RetailQuestDriver` 切断 `compileCombineTask`；
3. 同批删旧：`RetailCombineTaskTable`、`RetailCombineTaskDefinitionCompiler`、旧金标与相关资源；
4. 跨族旧金标重锚 + 逐行对拍门（`ADDED 0 / REMOVED 0` 口径）；
5. 代表任务真实客户端验收（未跑则本族 `PENDING_CLIENT`）。

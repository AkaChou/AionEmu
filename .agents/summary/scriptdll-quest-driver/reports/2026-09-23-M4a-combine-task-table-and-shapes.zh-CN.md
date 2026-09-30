# M4-a：CombineTask 真端表入仓与全族形状勘察（574/574 可整表复现）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

- 日期：2026-09-23
- 切片：M4 第一批（CombineTask 574 行的数据源与可迁移性测量）
- 前置：`2026-09-23-M3b-simple-talk-semantics-and-retirement.zh-CN.md`
- 一句话：真端 `Quest_CombineTask.xml` 入仓（574 行）后全族三方对账**六项判据 574/574 全部一致**，
  且 **574/574 都是同一形状**（四节点 `var0=0`），CombineTask 具备"整表复现 → 退役 XML"的条件。

## 1. 交付

| 交付物 | 位置 | 说明 |
|---|---|---|
| 真端表入仓 | `quest_retail/Quest_CombineTask.xml` | UTF-16+DTD → UTF-8（574 行 / 210,274 字节），`m4_bake_combine_task_table.py` 可重跑 |
| 形状勘察脚本 | `.agents/summary/scriptdll-quest-driver/m4_combine_task_shapes.py` | 三方对账：真端表 / 生产 XML / 本服索引（npc、item、recipe） |
| 勘察结果 | `retail-combine-task-shapes.tsv`（574 行）+ `m4-combine-task-shapes.txt` | 逐任务 verdict + 六个字段对账 |

## 2. 对账结果（574/574）

| 判据 | 结果 | 说明 |
|---|---|---|
| 真端表有行 | 574/574 | 与 `retail-xml-retention.tsv` 的 `FAMILY_PENDING:CombineTask` 完全一致 |
| 任务 NPC 唯一解析 | 574/574 | `task_npc`（多为 `Anteros,Auminus` 两名）在 `npcs/npc_template_*` 里唯一命中 |
| `combineskill` → skill id | 574/574 | `weaponsmith`→40002 等映射复用 `RetailQuestMetadataCompiler.COMBINE_SKILLS` |
| `combine_skillpoint` | 574/574 | 与 XML `combine-skill-point` 逐值相同 |
| `product` → `metadata/items` | 574/574 | 物品名（如 `item_ws_q5000`）经 item 名索引落到 item_id，数量一致 |
| `give_componentN` → `work-items` | 574/574 | 1..8 个分量逐项一致（id 与数量） |
| `recipe_name` → `learn-recipe` | 574/574 | 用 `(skillid, productid)` 在 `recipe_templates.xml` 反查 recipe 模板 id，与 XML `learn-recipe recipe-id` 相同（全族无歧义） |

## 3. 形状（574/574 同形）

```
<node label="unaccepted" status="NONE">    var0=0
<node label="started"    status="START">   var0=0
<node label="reward"     status="REWARD">  var0=0     ← 与 SimpleTalk 不同：**没有行号投影需求**
<node label="complete"   status="COMPLETE">var0=0
```

- 进度域：单一 `var0`（6 位，0..63），无计数语义（真端 `FUN_180caac10` 的 `0x100` 结束当前进度、
  `0x5d8` 刷新、`0x1c0` 写 8 个分量、`0x1f8`/`0x1d0` 逐个发产物、`0x1f0` 收尾）。
- 因此 **CombineTask 不需要"客户端任务书领奖行"派生**：全族 `reward` 投影就是 0。
  （对照：SimpleTalk 有 1002 个任务需要 0、4 个需要 1。）

## 4. 结论与下一步

结论：**CombineTask 574 行可由真端表整表复现**，可迁移上限 = **574/574**（待合成器落地后由家族门禁固化）。

下一批（M4-b，照 M3-b 流程）：

1. `RetailCombineTaskTable`（id → task_npc / combineskill / point / recipe_name / product / give_componentN）
   + `RetailRecipeIndex`（`(skillid, productid)` → recipe id，索引已验证 574/574 唯一）；
2. `RetailCombineTaskDefinitionCompiler`：NPC_START（`give-item` 分量 + `learn-recipe`）→ 交付路由
   （`has-item` product → REWARD，`remove-item` 分量）→ `npc-complete`；拒绝码留给"NPC 名多解 / recipe 缺项"；
3. 家族门禁 `RetailCombineTaskGateTest`（家族冻结 574 + 真端语义不变量 + 漂移登记）；
4. 退役 574 个 XML（catalog 4357 → 3783）→ retention owner 转 `RETAIL_TABLE`；
5. 聚焦门禁 + questEngine 全树与干净 HEAD 基线对账。

## 5. 未验证 / 风险

- 未验证：服务端重启、客户端实机（用户执行）。
- 风险：`selectable_reward` / 多产物（`product` 带多行）尚未在真端表侧出现；若出现需在 M4-b 的拒绝码里显式登记。

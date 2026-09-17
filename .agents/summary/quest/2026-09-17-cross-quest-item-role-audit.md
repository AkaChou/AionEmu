# 跨任务道具角色错配审计（QE-031）

日期：2026-09-17　范围：全部 6,222 个任务定义（6,186 可执行）

## 1. 症状与根因

症状：收集任务的怪物掉落完全正常、背包里也确实有道具，**交付对话却始终提示物品不足**；或收集进度条始终不刷新、任务停在收集阶段无法进入下一步。

根因（同一批迁移缺陷的两个面）：

1. **交付条件引用了邻居任务的道具**（按邻居复制"道具块"，道具 ID 连数量一起偏移）：
   | 任务 | 掉落/自有的道具 | 交付条件误写成 | 真端 collect_item / check_item |
   |---|---|---|---|
   | 15010 | 182215664(quest_15010a)x5、182215665(quest_15010b)x3 | 182215666(quest_15011a)x7 | quest_15010a 5 + quest_15010b 3 |
   | 15012 | 182215667(quest_15012a)x5 | 182215668(quest_15013a)x5 | quest_15012a 5 |
   | 15043 | 182215677(quest_15043a)x7 | 182215678(quest_15044a)x5 | quest_15043a 7 |
   | 15070 | 182215682(quest_15070a)x10 | 182215683(quest_15071a)x1 | quest_15070a 10 |
   | 51021 | 182215182(quest_51017a)x3 | 182215183(quest_51018a)x3 | quest_51017a 3 |

   道具开发名来自 `item_template@name_desc`；`182215668 = quest_15013a`（属于 15013）是判定关键证据。

2. **`collect-item` 事件监听了邻居任务的道具，且 count 误用掉落行数**：`QuestEngine` 在道具入包后广播 `QuestEvent.CollectItem(itemId, inventoryCount)`，事件用于重新下发任务状态（`PACKET_ONLY`）。监听错的道具 → 事件永不触发 → 客户端收集进度不刷新。
   - 28836：监听 `182213210(quest_28835a) count=5`（该任务恰好 5 条掉落行）→ 改为 `182213207(quest_28836a) count=50`（真端 collect/check 均为 50）
   - 28838：监听 `182213214(quest_41257b) count=8`（恰好 8 条掉落行）→ 改为 `182213208(quest_28838a) count=50`
   - 全库同类事件 147 处，其余 143 处均监听本任务道具，count 与交付数量一致（44/48 有 report 的样例逐一核对）

## 2. 门禁（新增）

`src/test/java/com/aionemu/gameserver/questEngine/definition/QuestItemSourceContractGateTest.java`

- **I1 全库不变量**：`collect-item` 事件只能监听本任务声明（`items`/`inventory-items`/`work-items`）、掉落、发放或上报的道具；违规即失败，无豁免通道。修复后 0 违规。
- **I2 回归锁定**：上面 7 个任务的交付集合必须等于真端 collect/check 道具（含数量与移除动作白名单）。

## 3. 复算

```bash
python3 .agents/summary/quest/item-producer-scan/generate_item_role_baseline.py   # 真端 collect/check 名称 -> ID，输出 test resource 基线
python3 .agents/summary/quest/item-producer-scan/audit_cross_quest_item_roles.py  # 回归 + I1 + 后续轴
```

## 4. 后续待评审轴（本次未纳入门禁）

真端声明了 collect/check 道具、但在我方交付集合（has-item / remove-item / collect-item / npc-item-report）中完全看不到的任务 **89 行** → `.agents/summary/quest/item-producer-scan/item-role-gaps.tsv`。

其中形如 `1932 → 182206008(quest_1932a)`、`2232 → 182203224(quest_2232a)` 的是**本任务自己的道具没被任何交付条件引用**，与"玩家可零进度领奖"同源，优先级最高；其余多为外部来源（采集物、商店材料）或奖励型任务，需要逐条判定后才可收口为门禁。

## 5. 边界

- 跨任务道具交接链是**真端设计**，不得一律判错：如 13904 交付 13903 的道具、80795 使用 80723 的道具、50048 消耗 50047 的奖励（50047 的 reward 即 186000401）。
- `item_template` 缺少 `name_desc` 的道具不参与名称映射判定。
- 验证：`mvn test -Dtest=QuestItemSourceContractGateTest,QuestDropContractGateTest,QuestMovieAndDialogLoopRegressionTest,QuestDefinitionDirectoryLoaderTest,CompletedQuestPrerequisiteRegressionTest,QuestClientContractGateTest,ProductionCatalogWhitelistVerificationTest` → Tests run: 33, Failures: 0, Errors: 0；PRODUCTION_COMPILE_OK=6186、FAILURES=0、WHITELIST_VIOLATIONS=0。未做真机客户端验收。

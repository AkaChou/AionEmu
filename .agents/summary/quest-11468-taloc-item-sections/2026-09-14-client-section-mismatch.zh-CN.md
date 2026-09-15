# 任务 11468/21468 塔洛克物品计数客户端 SECTION 对齐

## 触发与症状

- 实测：使用 `164000139`（Neith's Sleepstone，技能 9834）后任务 11468 变为 `START step=256`；
  再使用 `164000137`（Shishir's Powerstone，技能 9832）后变为 `START step=257`。
- 客户端任务窗口只剩奖励，`任务说明` 为空，看不出三种物品的 `x/10`、`x/5`、`x/3` 进度。

## 权威证据链

1. 客户端任务脚本 `Quest_unpacked/quest_script_monster.csv:311-316`：
   `11468` 使用 `SECTION_1` 统计魔法石、`SECTION_2` 统计保护石、`SECTION_3` 统计声响之珠，
   并同时要求 `SECTION_0==0`。`21468` 在 `:845-850` 相同。
2. 客户端摘要 `Dialogs/10000_19999/quest_q11468.html` 的 `quest_summary` 也按三组计数占位符显示
   `x/10`、`x/5`、`x/3`。
3. 旧处理器 `origin/history` 的 `_11468WithFriendsLikeThese` 使用
   `getQuestVarById(1/2/3)` 与 `setQuestVarById(1/2/3)`。`QuestVars` 的六个槽位是 6-bit，
   因此对应 packed 值位段 `SECTION_1=6..11`、`SECTION_2=12..17`、`SECTION_3=18..23`。
4. 当前 XML 曾把 `var1/var2/var3` 紧凑放在 `offset 0/4/8`。于是第一次使用魔法石就把
   `SECTION_0` 置为 1，破坏客户端 `SECTION_0==0` 的进行中条件；声响之珠计数也落在错误的
   `SECTION_1`，而不是 `SECTION_3`。

## 修改

- `11468.xml`、`21468.xml`：`var1/var2/var3` 改为标准 6-bit 位段
  `offset=6/12/18, width=6`。
- `ClientQuestSectionAlignmentTest` 新增回归：依次触发技能 `9832/9833/9834` 后，
  `SECTION_0` 必须保持 0，`SECTION_1/2/3` 必须各为 1。

## 验证边界

- 已完成：`git diff --check`；IDE 对修改的 XML 与测试文件检查无 error。
- 未执行：Maven 测试、服务端重启、真实客户端复测。
- 数据注意：测试角色当前 DB 中已保存的旧 packed 值（如 `257`）会按新布局误读为
  `SECTION_1=4`。复测前应放弃/重置该任务变量后重新接取；不要直接把旧值当新布局继续推进。

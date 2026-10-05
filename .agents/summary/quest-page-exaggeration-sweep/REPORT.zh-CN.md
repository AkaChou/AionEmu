# XML 车道「发页」全量普查 Report（2026-10-05）

> 方法：QE-142（真端 ScriptDLL 反编译取证）。扫描面：`quests/*.xml` 全部 915 个
> after-commit 含 `SHOW_SELECTION_PAGE` 的行；工具：`find_handlers.py` / `branch_report.py` /
> `class_c.py` / `fd_check.py`（本目录）。

## 一、SETPROn（推进类）40 行 / 20 任务 —— 已修复 ✅

**真端铁律（逐任务函数体核实）**：六步 SETPROn + FINISH_DIALOG（1002 基准）与全部同类
处理器，推进分支 = `SetProgress + 0x5d8 刷新`，**零发页**；页只在「打开/回显/检查结果」分支出现。

| 任务 | 行 | 真端函数 | 真端行为 |
|---|---|---|---|
| 1007 ×6 | SETPRO3→reward10..60 | FUN_180fa57d0 | 0x100(quest,10/20/30/40/50/60) 按职阶分档，零页 |
| 2009 ×6 | SETPRO3→reward10..60 | FUN_180fa2900 | 同上（职阶档 10..60） |
| 1922 | SETPRO12→s4 | FUN_180f6fcd0 | 0xf0(quest,4)，零页 |
| 1990 | SETPRO2/SETPRO3 | 通用口 / FUN_180fc9fb0 | SetProgress+刷新，零页 |
| 2001/2004/2006 | SETPRO1、SETPRO3 | FUN_180f74830/FUN_180fa2d70、FUN_180fa4310/FUN_180fc0f00、FUN_180f81d30 | 零页 |
| 2114 ×2 | SETPRO1/SETPRO2 | FUN_180f40160 | **发真端页 0x548/0x69d（SELECT2/SELECT3）——非页 10，已替换** |
| 2122 | SETPRO1 | FUN_180f75120 | 0xf0+0x1d0+刷新，零页 |
| 2221/3721/4914 | SETPRO1 | 无自定义处理器（全通用口） | 零页 |
| 2223 | SETPRO1 | FUN_180f80030 | 0x410(发物)+0xf0+刷新，零页 |
| 2900 ×5 | SETPRO1..4/SETPRO10 | FUN_180f7a340/180f9a830/180fabcb0/180fb3d40/180f8d630 | 零页（SETPRO10=0x100 完成+传送） |
| 2990 | SETPRO2/SETPRO3 | 通用口 / FUN_180fca140 | 零页 |
| 3090 | SETPRO1 | FUN_180fe3940 | 0xf0(3)+0x410+刷新，零页 |
| 3940/4944 | SETPRO3 | FUN_180fca740/FUN_180fca3e0 | 零页 |
| 14054 | SETPRO6 | FUN_180fce410/FUN_180fce860 | 0x100+刷新，零页 |
| 21114 | SETPRO2/SETPRO4 | FUN_180f9a4f0（发物+通用口）/通用口 | 零页 |

修改：38 行删页（每行附函数级注释）+ 2114 两行改为 SHOW_QUEST_PAGE SELECT2/SELECT3。
测试对齐：Quest21114PoisonedFungiRetailFlowTest ×2、MigratedQuestRepairDefinitionTest（3090 行）。
验证：生产目录全量编译绿（707/0）+ 8 个相关测试类全绿。

## 二、FINISH_DIALOG（1008）409 行 / 295 任务 —— 真端零页，实机休眠（未改）

- 真端：0x3f0 → 仅 0x5d8 刷新（通用口 FUN_180caf150 + 4 处自定义分支 +
  抽样 10/10 任务的 0x3f0 全走通用口）——与本仓 `RetailDialogIntentClassifier`
  的 `FINISH_DIALOG → LOCAL_CLOSE`（本地关窗）判定一致。
- 实机：`log/quests.log` 全量 **0 次**客户端上行 `动作=1008`（两周+）→ 该 409 行为休眠行，
  现行为（回页 10）从未被触发。
- 结论：协议上与真端不符但零实机影响；建议后续批次对齐（改删页），不阻塞本批。

## 三、完成/领奖收尾族（TALK_TO_NPC 146 + SELECTED_QUEST_REWARD* ~301）—— 保持

`reward→complete` 收尾回选择页 10 = 完成对话返回列表的零售 UX（quests.log 中 367 次
`questId=0 下发页=10` 全部为完成收尾，实机验证过）；与本批「推进类」缺陷不同机制，不动。

## 四、kill-npc / use-item 行 9 条 —— 下批展开

## 五、真端新知识（通用处理器族）

- `FUN_180caf150`：通用动作口。SETPRO 族（10000–10254）带**防跳步校验**
  （`action-9999 != step+1 → "attempting to Jump Progress" 拒绝`），通过后 SetProgress+仅 0x5d8；
  0x3f0(1008)→仅刷新；0x3ea(1002) 且 status<2 → 发页 0x3eb(1003)；0x3f1(1009)/0x3ef(1007)→完成/特殊口。
- `FUN_180caf460`：通用按步页池——unaccepted→0x129a(4762)；START 第 n 族页：
  1011/1352/1693/2034/2375/2716/3057/3398/3739/4080/6500/6841/7182/7523/7864；REWARD→0x2712(10002)。
- `FUN_180caf740`/`FUN_180caf350`/`FUN_180caf3c0`：打开发当前步页 / 通用口包装。
- vtable 补充：`0x2d0`=传送（world+x/y/z+heading），`0x410(_,hash,1,_)`=发物（名称哈希），
  `0x1b0`=完成结算，`0x1d0`=标记位，`0x1d8`=影片/演出，`0x1e0`=分档演出。

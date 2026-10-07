# 击杀/用物推进 REWARD 的翻译夸大发页：11 行全量收口（报障样本 3031 完成击杀弹 load fail）

- 日期：2026-10-07
- 范围：`3031`（报障样本，天族）/ `3056` / `2001` / `11006` / `11031` / `11032` / `11033`，全目录共 11 行
- 状态：**实现完成 + 聚焦测试全绿（IDEA MCP，用户授权）；客户端实机复测 PENDING_CLIENT**
- 关联前序：QE-143（`.agents/summary/quest-page-exaggeration-sweep/REPORT.zh-CN.md` 第四节「kill-npc / use-item 行 9 条 —— 下批展开」本批收口）、QE-142（真端取证法）、QE-137（对话窗下发面契约）

## 一、现象

用户报障：任务 3031《海盗团的赏金》（`Wanted: Pirates`，ELYOS，46+，类别 IMPORTANT，接取 NPC 对话 730144，
完成方式 = 击杀）**击杀完成后弹出一个 load fail 对话**。

「击杀完成」即最后一只任务怪（214219/214220/214222/214223）把两组计数打满、任务由 `started` 进入 `reward` 的那一次击杀。

## 二、根因

`3031.xml` 的 4 条 `started → reward`（priority 0，击杀完成边）`after-commit` 里带着
`<dialog type="SHOW_SELECTION_PAGE" page="SELECT_QUEST"/>`（页 10）：

```xml
<after-commit>
  <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
  <dialog type="SHOW_SELECTION_PAGE" page="SELECT_QUEST"/>   <!-- 翻译夸大，已删 -->
</after-commit>
```

击杀事件没有对话对象：`QuestEngine.onKill(...)` 以 `interactionObjectId=0`（`QuestDispatchContract.BROADCAST`）
派发，`PlayerQuestDialogPort.showSelectionDialog` 在 targetless 形态下下发
`SM_DIALOG_WINDOW(0, 10)`——客户端按对象 0 打开对话窗、加载不到对应 html，即显示 **load fail**
（与 QE-137 的「无对话对象下发页即 load fail」同机制；页 10 是 NPC 级通用选择页，不是可脱离对话对象渲染的任务页）。

## 三、真端取证（ScriptDLL64.c 反编译源，逐函数读体）

| 任务 | 真端函数 | 行为 | 页面 |
|---|---|---|---|
| 3031（0xbd7） | `FUN_180ed93d0` / `FUN_180ed9410`（L2432154/L2432165）→ 通用计数口 `FUN_180caa850`（L2138785） | 击杀只做位域累加 `0xf0` + 计数通报 `0x120` | **零发页** |
| 3056（0xbf0） | `FUN_180ed9330`（L2432134） | START 且计数达标 → `0x100` 推进 + `0x110` 计数通报 | **零发页** |
| 2001（0x7d1） | `FUN_180edded0`（L2434861） | 击杀按变量阶梯 3→..→8，末击 `0x100` 收尾 + `0x110` 通报 | **零发页** |
| 11006（0x2afe） | `FUN_180f03f20`（L2462746，注册 `FUN_180cb2eb0(...,0x2afe,5,0xadc40f1,...)`） | START/step==2 → `0x100` 推进 + `0x2f8` 通告 | **零发页** |
| 11031（0x2b17） | `FUN_180f00ac0`（L2460484，0xadc4104） | START/step==2 → `0x270` + `0x100` 推进 + `0x2b8` 通告 | **零发页** |
| 11032（0x2b18） | `FUN_180f00b30`（L2460503，0xadc4106） | 同上 | **零发页** |
| 11033（0x2b19） | `FUN_180f00ba0`（L2460522，0xadc4108） | 同上 | **零发页** |

击杀目标名链（真端 `Map/XML/npcs_monsters.xml` ↔ 注册表 `FUN_180cb2ac0(...,0x11,<handler>)`）：

- 214219 `LF2A_TesinonSeamanM_46_An`、214220 `LF2A_TesinonSeamanM_47_An` → `FUN_180ed93d0`（计 15 进 var1）；
- 214222 `LF2A_TesinonSeamanR_46_An`、214223 `LF2A_TesinonSeamanR_47_An` → `FUN_180ed9410`（计 12 进 var2）；
- 214578 `LF2A_SpectreFxQ_50_An` → `FUN_180ed9330`（3056）；210368 `SpriggD_3_n` / 210369 `SpriggD_4_n` → `FUN_180edded0`（2001）。

计数口径与 XML 逐位对齐（`FUN_180caa850(0xbd7,_,_,0,1,0xf,2)` = 上限 15 → var1；`(...,0xc,3)` = 上限 12 → var2），
说明这些边就是同一批击杀路由；真端在完成击杀上**只发状态/计数包、不发页**，页 10 / 奖励窗 5 系旧 XML 翻译夸大。

## 四、全目录类扫描（`quests/*.xml`，915 个）

「事件非 `dialog` 且目标节点 = REWARD，after-commit 含发页」共 11 行：

| 任务 | 行 | 事件 | 原尾随页 |
|---|---|---|---|
| 3031 | ×4 | `kill-npc` 214219/214220/214222/214223 | `SHOW_SELECTION_PAGE SELECT_QUEST`(10) |
| 3056 | ×1 | `kill-npc` 214578 | `SHOW_SELECTION_PAGE SELECT_QUEST`(10) |
| 2001 | ×2 | `kill-npc` 210369 / 210368 | `SHOW_QUEST_PAGE SHOW_SELECT_QUEST_REWARD_WINDOW1`(5) |
| 11006 | ×1 | `use-item` 182206705 | `SHOW_SELECTION_PAGE SELECT_QUEST`(10) |
| 11031/11032/11033 | ×1 each | `use-item` 182206724/26/28 | `SHOW_SELECTION_PAGE SELECT_QUEST`(10) |

修复后复扫：同类行 **0**。未纳入本批（保留原状、边界见下）：8 行 `use-item → SHOW_ASK_QUEST_ACCEPT_WINDOW`
（1114/11216/1607/1670/2122/2136/2343/4914，`unaccepted → unaccepted` 的「用物触发接取面」，页 4 在客户端契约集合内，
非 REWARD 入口）；「完成/领奖收尾回页 10」族（`COMPLETION` sync + 页 10，实机验证过，QE-143 边界①）。

## 五、修复

数据（11 行删页，保留 sync，逐行附真端函数级双语注释）：

- `quests/3031.xml` ×4、`quests/3056.xml` ×1、`quests/2001.xml` ×2、`quests/11006.xml` ×1、
  `quests/11031.xml` ×1、`quests/11032.xml` ×1、`quests/11033.xml` ×1

测试：

- 更新 `Quest11006ClientDialogAlignmentTest`（第二瓶水进 REWARD 的 after-commit 断言改为仅
  `SyncQuestState(LEVEL_AND_VISIBILITY_REFRESH)`，注释附 `FUN_180f03f20` 零发页取证）。
- 新增 `QuestKillItemRewardEntryDialogGateTest`：
  - 11 条取证样本边的 after-commit = 仅 sync、无对话页动作；
  - 3031 完成击杀的 dispatch 仿真仍进入 REWARD（var1 14→15 / var2 12 保持，证明删页未动状态推进）；
  - 生产目录全量门禁：`KillNpc`/`KillNpcSet`/`UseItem`/`ItemPlay` 进入 REWARD 的边不得带对话页。

## 六、测试执行记录（IDEA MCP，2026-10-07，用户授权）

| 测试类 | 结果 |
|---|---|
| `QuestKillItemRewardEntryDialogGateTest`（新增） | 13/13 绿（2 方法 + 11 条取证样本；含生产目录全量编译 `QuestDefinitionDirectoryLoader.compile`，1.0 s） |
| `Quest11006ClientDialogAlignmentTest`（更新） | 1/1 绿 |
| `RewardRowProjectionRegressionTest`（3056/11031/11032/11033 领奖投影面回归） | 168/168 绿 |
| `QuestKillCounterRetailGateTest` | 3/3 绿 |
| `QuestUseItemRewardCleanupGateTest` | 1/1 绿 |

未执行：Maven 命令行构建（规则 1）；服务端进程未触碰。

## 七、边界与风险

1. 本批只删「非对话事件进入 REWARD」上的发页；对话面（TALK_TO_NPC / npc-complete）的页一律不动。
2. 真端 `0x100` 收尾仍由本仓 `sync-quest-state LEVEL_AND_VISIBILITY_REFRESH` 表达（QE-040 口径），未改。
3. 实机验收口径：3031 完成最后一击后**不再弹窗**、任务书正常推进到「向代理人报告」；3056/2001/11006/11031-11033
   同型（3056 杀怪完成、2001 击杀阶梯最后一击（SpriggD 系列，真端变量阶梯 3..8）、11006 装第二瓶水、11031-11033 用物）。

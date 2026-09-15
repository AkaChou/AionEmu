# 任务 10032「Help in the Hollow」卡住 / 使用道具后计数异常 取证

- 时间：2026-09-14（服务器会话 20:37 / 21:24 / 21:47 三次启动）
- 玩家：Kk（objectId 151528），副本 300190000（idelim）
- 相关：`quests/10032.xml`、`CM_USE_ITEM`、`Skill.useSkill`、`QuestMutationPlanner`、`PlayerQuestInventoryPort`

## 一、实机证据（log/console.log）

| 时间 | 事件 | 现象 |
| --- | --- | --- |
| 21:32:40 | CM_USE_ITEM 182215619 | SM_QUEST_ACTION 10032 状态=3 **步数=21** |
| 21:33:04 | CM_USE_ITEM 182215618 | 无任务包（var0 已是 s5，fruit 无对应 transition） |
| 21:33:07 | CM_USE_ITEM 182215619 | SM_QUEST_ACTION 10032 状态=3 **步数=37** |
| 21:33:59 | GM `moveto 110010000`（离开副本） | **没有任何 10032 同步包**（s5→s2 回退边未执行） |
| 21:34:07 | GM `reset instance` | 同上 |
| 21:38:16 | GM `quest show 10032` | 服务端仍为 START，vars=[37,0,0,0,0,0]（= 客户端截图） |
| 21:51:23 | CM_USE_ITEM 182215619（objId 152158） | 步数=**53** |

步数解码（本任务 XML 的 progress 布局 `var0@0 w4`、`var1@4 w5`）：
`步数 = var0 + var1*16` → 21=(var0=5,var1=1)、37=(5,2)、53=(5,3)：服务端每次使用 **+1**，与
5.8 客户端 `data_driven_quest.xml` 的 `ItemPlay QUEST_10032B, 20` 合同一致。

## 二、DB 查询（只读，al_server_gs）

- `player_quests`：10032 = START，quest_vars 37 → 53（21:51 后），complete_count=0。
- `inventory`：151528 名下 **不存在** 182215618/182215619/182215620/182215621（任何 location/任何 owner 均无）。
- 对照：182206001（模板 activate_count=1000）DB activation_count=1000 且使用后仍在；
  182212218、186000015 的 activation_count=0 也同样未被使用消耗。

## 三、已定位的问题

### 3.1 打包布局与全库/同系列不一致（"瞬间完成 20"的直接来源）

- `10032.xml`：`var0 offset=0 width=4`（阶段），`var1 offset=4 width=5`（使用次数）。
  → 阶段值 5 占用了最低 4 位：用 **1** 次道具步数就是 21（≥20），用 2 次 37。
  任何"低位字段=次数"的读取方（客户端任务栏 / 工具）都会在第一次使用后显示 20 已完成，
  而服务端才计数到 1。
- 对比 `10031.xml`（同系列、客户端已验收）：`var0@0 w4`、**`var1@6 w4`**、`var2@12 w4`。
- 全库主流约定同为 6 位步进：`var0 width=6` 3873 处、`var1 offset=6` 数百处。
- 结论：10032 的 `var1 offset=4` 与"阶段占低位"的组合需按客户端声明复核（真端 Items 数据里
  道具字段已核对一致，问题在任务变量布局）。

### 3.2 回退边在缺物品时被静默丢弃

- s4/s5/s6 → s2 的三类事件（die / log-out / enter-world 且 world≠300190000）动作均为
  `remove-item 182215618 count=1` + `remove-item 182215619 count=1` + `set-variable var0=2`。
- `QuestMutationPlanner.removalFeasible` 与 `PlayerQuestInventoryPort.preflight` 判定
  `remaining < count` 时直接 `Optional.empty()` → 整条 transition 被丢弃（无异常、无日志、无包）。
- 与日志一致：21:33:59 离开副本后没有任何回退同步；玩家此后无法回到 s2（s5 只能在副本内推进）。
- 既有规范（QE-007 / playbook）：回收清理语义应使用 `count="ALL"`（`removeAll()` 对
  `removalFeasible` 短路为 true）。

## 四、待确认

1. 图 1 未读取（`WeType/dsclp/1789393075785.png` 不存在），若是客户端任务栏截图请重发；
   "20 已完成"是客户端任务栏显示还是直接读日志步数，需要一句话确认。
2. 两件任务道具（182215618/182215619）当前不在背包；其模板与真端 `Items.xml` 一致
   （`activation_count=1000`、`use_delay_type_id=61`/`use_delay=10000`、`area_to_use=IDElim_ItemUse`、
   activation skill 4 级），设计上可重复使用，需确认是被哪条 remove-item 路径清掉或人工删除。

## 五、验证边界

- 未修改任何生产文件；未执行 Maven、未重启服务、未做客户端复验。
- 上述日志/DB/代码结论为静态+实机日志+只读 DB 证据；任务 XML 修改后仍需
  quest-specific 编译测试、生产目录门禁与真机复验。

## 六、客户端任务栏截图（2026-09-14 追加证据）

客户端任务说明「51级卡斯帕中出现的问题」第 6 行：

```
使用卡斯帕的眼泪，净化卡斯帕内部(20/20)
```

- 该行计数显示为 **20/20（饱和）**，而同一时刻服务端 `quest_vars=53`
  （= var0 阶段 5 + var1 次数 3 × 16，服务端实际只用了 3 次）。
- 结论：客户端读取的"计数字段"落在我们打包的最低位上——`步数 = 阶段 + 次数×16`，
  用 1 次就已经是 21（≥20），所以客户端在第一下之后就显示 20/20；
  服务端却还在 1…20 地累加。这就是"使用道具后瞬间就完成了 20"的直接来源。
- 对照：同族实现里 10112 / 20112 使用 6 位步进（`var1 offset=6`），
  10032 与 20032（天/魔双胞胎任务）用的是自造的 `var1 offset=4 width=5`。
- 待验证的判别数据：**尚未使用眼泪时（var1=0、步数=5）该行显示的是 `0/20` 还是 `5/20`**——
  前者说明客户端只读 var1（则需把次数放到客户端实际读取的字段），后者说明客户端读的低位包含阶段号。
- 修复方向：让客户端读取的字段里**只放使用次数**（次数在低位、阶段移到高位，如 `var1 offset=6`），
  同步改节点投影/自环计数/判定，并把 s4/s5/s6→s2 的清理改成 `count="ALL"`。
  引擎侧 `collecting-step` 比较的是节点投影的 `var0`（`QuestInteractionObjectValidator`），
  改字段后需一并复核该掉落过滤。

> **2026-09-15 修正**：本节把阶段搬到 var1 的结论已被真机否定；阶段必须留在 SECTION_0（var0），见 [2026-09-15-section0-stage-correction.zh-CN.md](2026-09-15-section0-stage-correction.zh-CN.md)。

## 七、2026-09-14 实施（用户授权"开始+构建授权"）

改动文件：

- `quests/10032.xml`、`quests/20032.xml`
  - `progress` 改为 `var0 offset=0 width=6 max=20`（当前阶段计数，客户端任务说明读取的槽位）
    + `var1 offset=6 width=6 max=8`（阶段索引 = 客户端任务说明行索引）。
  - 各节点投影只声明阶段 `var1`（避免 `matchesSourceNode` 用计数参与源节点匹配）。
  - 使用果实 `s4→s5`：`var0=0`、`var1=5`。
  - 使用眼泪 `s5→s5`（priority 1，`var0<19`）：`increment var0`；`s5→s6`（priority 0，`var0>=19`）：
    `var0=20` + `var1=6`（第 20 次使用同时把计数补满，客户端显示 20/20 并进入击杀行）。
  - `s4/s5/s6 → s2`（死亡/下线/离开副本）与 `s7 → reward` 的清理：`remove-item count="ALL"`，
    回退边追加 `var0=0`（阶段回到 2）。上交心脏仍是精确 `count="1"`（QE-007 边界）。
  - 掉落门禁 `collecting-step` 6 → 20：运行时 `QuestService.isQuestDrop` 比较的是
    `getQuestVarById(0)`（6 位槽 0），旧值 6 在 `var1=19` 时永远不可能命中（旧布局下心脏根本不会掉）。
- 新增 `Quest10032ItemPlayClientCounterProductionFlowTest`：锁定客户端可读低位槽=真实次数、
  阶段推进、缺道具时的回退、掉落门禁、上交心脏精确消耗。

## 八、验证（2026-09-14 22:2x）

在独立 worktree（HEAD + 仅本次改动）执行：

| 门禁 | 结果 |
| --- | --- |
| `Quest10032ItemPlayClientCounterProductionFlowTest` | PASS（10032 + 20032 各 1 条动态用例） |
| `ProductionCatalogWhitelistVerificationTest` | PASS（6193/6193 编译；3934 的失败来自其他会话未提交改动） |
| `QuestPageButtonAuditTest` | PASS（2/2） |
| `QuestClientContractGateTest` | 23 条 `BUTTON_WITHOUT_ROUTE`（1197/1198/2289/2367/2411/2443/2448/2922/3088/3936/3937/3938/4940/4941/80008/80009）；**回滚本次改动后计数完全相同**，证明与 10032/20032 无关 |

`target/classes/.../10032.xml` 已随构建同步为新布局。

## 九、交付边界与待办

- 未提交、未重启服务、未做真机验证。
- **旧存档语义已变**：现网 `player_quests.quest_vars=53` 这类旧打包值按新布局会读成
  `var0=53, var1=0`，测试角色需要 `//quest delete 10032`（重新接取）或按新语义 `//quest set`。
- 待真机确认：任务说明中「使用卡斯帕的眼泪，净化卡斯帕内部」应随使用 1/20→19/20，第 20 次后显示
  20/20 并切到「消灭混沌之特伦克」；离开副本后应回到「和罗特亚斯对话」，且不再因缺物品卡住。
- 后续可审计：其他「道具使用计数放在 var1」的任务（如 `15042.xml`）是否存在同类客户端槽位问题。

# quest 1929「暗黑碎片 · 烙印之石」重复发放与对话整链重放诊断（2026-10-07）

## 玩家症状（用户实机，NPC 205111 Ecus，目标对象 167331）

- 一次对话里出现两个「结束对话。」按钮：先 select5_1_1 的，再 select5_2 的；点第一个仍会进入下一页。
- **每次**和 205111 对话都会多送一个烙印之石（用户职业 = 海流爆裂 140000004 系），整条
  select5 → select5_1 → select5_1_1 → select5_2 链重放。
- 期望：第一次对话点「结束对话」发一次烙印任务道具；之后未装备时再对话只推进对话/任务状态，
  直接出示第二个「结束对话」页（装备引导页），不再发道具。

## 实机 trace（log/quests.log，2026-10-07 17:00:53–17:01:04）

```
[S->C] SM_DIALOG_WINDOW questId=0    下发页=10      ← 点 NPC（-1）无路由 ⇒ 兜底任务列表页 10
[C->S] CM_DIALOG_SELECT questId=1929 上一页=10   动作=31    ← 点任务行（QUEST_SELECT）
[S->C] questId=1929 下发页=2375  ← SELECT5
[C->S] 上一页=2375 动作=2376    [S->C] 下发页=2376  ← SELECT5_1
[C->S] 上一页=2376 动作=2377    [S->C] 下发页=2377  ← SELECT5_1_1
[C->S] 上一页=2377 动作=2461    [S->C] 下发页=2461  ← SELECT5_2（装备引导页）
[C->S] 上一页=2461 动作=2546    [S->C] 下发页=1     ← SELECT5_3：发道具 + 开烙印窗；不推进 ⇒ 循环
```

旧 XML 的 11 条 `action="SELECT5_3"` 分支 = `give-item` + `show-dialog-window 1`，**不推进 `step`**，
且是 `spawned98 → spawned98` 自环；因此任务行打开永远回到 select5（整链重放），每次点 2546 都再发一份。

## 证据链

| 证据 | 结论 |
|---|---|
| 客户端 `Dialog` 包 `QUEST_Q1929.html`（`data_unpacked/Dialogs/`） | select5_1_1 的按钮 `HACTION_SELECT5_2`（文案「结束对话。」）→ 客户端页 select5_2；select5_2 文案「请您打开窗口，装备我给您的结晶」= 装备引导页，其按钮 `HACTION_SELECT5_3`（「结束对话。」）。道具在进入 select5_2 之前必须已在背包（文案「我给您的」） |
| `HtmlPages.xml` / `HyperLinks.xml`（真端） | SELECT5=2375、SELECT5_1=2376、SELECT5_1_1=2377、SELECT5_2=2461、SELECT5_3=2546、SELECT6=2716；页面 id 与 HACTION id 同号 |
| `client_dialog_contract.tsv`（本库冻结客户端契约） | 1929 声明 select5/select5_1/select5_1_1/select5_2/select6…；**不声明 select5_3** ⇒ 服务端不可能下发 2546 页（旧写法改发 dialog 1 正确，但发放时机错位） |
| 孪生任务 2900（Asmodian，同 stigma 家族） | legacy `SELECT_ACTION_3058` = `giveQuestItem` + `step 96→99` + `sendQuestDialog(3058)`（教学页）；var 99 时同动作重发教学页；教学页按钮 `STEP_TO_7` → `SM_DIALOG_WINDOW(objId, 1)`（烙印窗）。现行 2900.xml 同形（`movie96 → equipped99`） |
| 旧 AionEmu `QuestHandler.giveQuestItem`（`8994fc8fc^`） | `existentItemCount >= itemCount` 时不发放并提示 `STR_CAN_NOT_GET_LORE_ITEM` ⇒ 迁移到 XML 的 `give-item`（无条件 add）丢失了 lore 幂等语义 |
| 引擎事实捕获 | `QuestFactRequirements` 对 `QuestCondition.HasItem` 捕获背包事实；`QuestCondition.areMutuallyExclusive` 已认定 HasItem expected 翻转对与 AdvancedClassIs 同类互斥 ⇒ 同事件多分支可数据化判定无歧义 |

## 修复（生产 XML：`quests/1929.xml`）

1. 新增节点 `granted95`（step 95，START）=「已发放、未装备」，并纳入既有失败守卫
   （die / enter-world 分支的 `variable-at-least 93` + `variable-below 99` 仍覆盖 95，
   死亡/离开实例照旧移除烙印并失败）。
2. `spawned98` 的 11 条 `SELECT5_3` 发放分支 → **22 条 `SELECT5_2` 分支**：
   - 未持有（`has-item … expected="false"`）⇒ `give-item` + `set-variable step=95`；
   - 已持有（`has-item …`）⇒ 仅 `set-variable step=95`（lore 语义，不再重复发放；同时修复旧缺陷留下的残留背包状态）；
   - after-commit 一律 `sync-quest-state(PACKET_ONLY)` → `close-dialog`（真端「推进分支带 0x5d8 关窗」口径，QE-143）；
     发放分支不再自行展示 select5_2 页，否则首轮对话会出现第二个「结束对话」按钮（用户 17:30 复测反馈）。
3. `granted95` 路由：`QUEST_SELECT`/`USE_OBJECT`/`SELECT5_2` → `SHOW_QUEST_PAGE SELECT5_2`（再次对话展示第二个
   「结束对话」页 = 装备引导页，绝不再发放）；`SELECT5_3`（引导页按钮）→ `show-dialog-window 1`（烙印窗）；
   `equip-item 140000001..4` → `step=96` + `sync(PACKET_ONLY)` + `close-dialog`。
4. 删除 `spawned98` 上被分支覆盖的无条件 `SELECT5_2 → SELECT5_2` 页导航；`spawned98` 保留 equip-item ×4
   （旧缺陷残留：先装备再对话也能推进到 equipped96，避免死锁）。

## 第二缺陷：烙印槽位不开启（2026-10-07 17:30–17:32 复测反馈）

症状：无法安装烙印之石，提示「没有空余的烙印之石凹槽」；客户端烙印窗无槽位。

两层根因：

1. **服务端资格判定被写死**：`StigmaService.getPossibleStigmaCount` 旧实现把资格绑在
   `quest 1929/2900 + vars==98` 上；本次修复把发放步推进到 95 后条件恒假 ⇒ 判定为「无资格」。
   （旧 Elyos 分支在发放前 step 98 即开槽，Asmodian 在 99，本身就是硬编码步数的历史遗留。）
2. **客户端从未收到槽位数**：登录只推送 DB 字段 `AdvancedStigmaSlotSize`（默认 0）；资格变化
   （接取/推进/完成/失败）也没有任何推送，客户端必须重登才可能刷新。

修复（数据驱动，去掉全部任务 ID/步数/结晶硬编码）：

- 数据面：定义 XML `<metadata>` 新增 `extend-stigma-slots="true"`（XSD 属性 + XML 编译器 + `QuestMetadata.extendStigmaSlots`）；
  真端侧 `reward_extend_stigma1` 由 `RetailQuestMetadataCompiler` 同列解析（本仓 `quest/retail/quest.xml` 恰有 16 行带该字段，
  与 8 天族 + 8 魔族烙印任务一一对应）。已标记的 4 个在库任务：1929、2900、30217、30317。
- `StigmaService.hasStigmaSlotEntitlement`：遍历玩家任务状态，任一被数据标记的任务处于
  START/REWARD/COMPLETE ⇒ 具备资格；槽位数仍按 20/30/40/45/50/55 档位表（纯函数 `stigmaSlotCount`）。
- 推送：`refreshStigmaSlots`＝`max(DB 会员/GM 解锁值, 资格计算值)`，在**登录**（`PlayerEnterWorldService`）、
  **升级**（`PlayerController.upgradePlayer`）、**任务状态提交后**（`PlayerQuestStateSyncPort.sync` 末尾，
  仅当该任务元数据声明 `extendStigmaSlots` 时调用）三处触发。

## 第三轮：客户端仍显示「没有空余的烙印之石凹槽」（2026-10-07 17:56–17:58 复测）

实机 trace（`log/console.log`）确认对话与发放已正确：205110 重接 → step 94 → 98 → 玩家点 2461 →
`SM_QUEST_ACTION 状态=3 步数=95`（发放，after-commit 只 sync + 关窗）→ 点 2546（引导页按钮）→
`show-dialog-window 1`（烙印窗）。随后玩家在窗口里放结晶，客户端提示「没有空余的烙印之石凹槽」。

证据链（全部为仓内/客户端解包产物，非猜测）：

- 该提示 = 客户端字符串 **1300408 `STR_STIGMA_SLOT_IS_NOT_OPENED`**（韩文「빈 스티그마 슬롯이 없습니다」）；
  本服务端**从不发送**该 id（全库 grep 仅 SM_SYSTEM_MESSAGE 定义处），装备失败路径是静默返回 null
  （`Equipment.equipItem` → `notifyEquipAction` 返回 false → `return null`，无系统消息）⇒ **拒绝发生在客户端本地**，
  客户端自己认为「打开的凹槽数 = 0」。
- 客户端窗口（`<客户端解包根>/data_unpacked/UI/game/player_info_dialog.xml` 的 `tabpage_stigma`）共
  **7 个槽**：`stigma_stone_normal1..3`（普通）、`high1..2`（상급/ENHANCED1）、`highest1`（최상급/ENHANCED2）、
  `extend1`（EXTEND，100×100 大槽）——与服务端 `getPossibleStigmaCount` 的 1..7（含 `STIGMA_SPECIAL`）逐位对应。
- 服务端从来只发 `SM_CUBE_UPDATE.stigmaSlots(存储字段)`，且 **`STR_MSG_STIGMA_OPEN_SLOT_BY_QUEST`（1402942，
  「스티그마 슬롯이 확장되었습니다」）全库零发送**；等级 20/45/55 只发过 NORMAL/ENHANCED1/ENHANCED2 通知。
  GM 解锁路径（`GiveStigma.unlock`）恰恰是「**持久化**槽位数（DB `advenced_stigma_slot_size`）+ 提示重登」，
  说明客户端认这个存储值/登录下发的值 ⇒ 资格计算值此前从未落库，重登后又回到 0。

本轮修复：

1. `StigmaService.refreshStigmaSlots`：计算值高于存储值时**按 GM 解锁同语义持久化**（`setAdvancedStigmaSlotSize`），
   并在数值首次抬升、且资格来自任务数据时补发真端 1402942「任务开启凹槽」通知（幂等：只在抬升那一次发）。
   客户端此时同时拿到「槽位数」与「开启通知」，登录路径也不再是 0。
2. 卸下路径（`notifyUnequipAction`）同样改为下发达标值，不再回落存储字段。
3. `StigmaService.onStigmaSlotQuestCommitted`（同步口对「元数据声明扩展烙印槽」的任务调用）：重算推送 +
   **每次提交都补发 1402942**——登录早期发的通知可能落空，玩家在世界内与任务交互时补发才可靠；
   重复通知只是提示文本，不会造成服务端放行（装备仍由 `getPossibleStigmaCount` 校验）。

复测判据（客观）：重登后 `players.advenced_stigma_slot_size`（验收角色，24 级 Elyos）应从 0 变为 **2**
（24 级 + 1929 进行中）；若仍为 0，则说明资格/目录侧未生效，需补日志定位；若为 2 而客户端仍拒绝，
则客户端另有门槛（届时以「窗口内 7 槽的开启外观」作为二次判据）。

## 第四轮：凹槽开启后「发放完又关闭」（2026-10-07 18:19 复测）

实机结果：**过场动画后（step 98）凹槽确实开启了**（服务端判据已成立：DB `advenced_stigma_slot_size`
已被写成 **2** ✓），但**结束对话拿到结晶后（step 95）凹槽又显示为关闭**，引导页要求安装时无法安装。

服务端在三个时刻发出的包完全相同（槽位数 2 + 1402942 通知；grant 时多一个 close-dialog 0/0），
故差异只能来自客户端对**其它事件**的反应（物品入包 / close-dialog / 窗口重开）。为把判据落到包级，
补了 **STIGMA-TRACE**（与 QUEST-TRACE 同一个 logback `quest` 出口、同一开关）：

- `log.stigma_trace.slot_push`：每次下发槽位数时打印 玩家/等级/存储/计算/是否有资格/下发值；
- `log.stigma_trace.slot_opened`：正式开启时（数值抬升）打印通知对（1402942 + 1402933）；
- `log.stigma_trace.slot_quest_notify`：被标记任务每次提交时补发的通知。

同时按「按类型开启」口径补发真端 **1402933（普通槽扩展）**，与 1402942 成对发送。

下一轮判据（决定性）：冷重启后**先只看不动作**——登录后立刻看烙印页凹槽开合；
再走动画/对话流程，逐点对照 STIGMA-TRACE 时间戳。若登录即开 ⇒ 槽位数是判据、关闭另有原因
（close-dialog / 入包事件）；若登录关、任务提交后开 ⇒ 通知是判据，需按事件重发。

## 第五轮：定性结论 = 关窗包抹掉客户端凹槽态（2026-10-07 18:26–18:28 STIGMA-TRACE 对照）

同一会话内三个提交点，服务端发出的内容完全一致（槽位数 2 + 1402942），**唯一差别是关窗包**：

| 时刻 | 提交 | 服务端包 | 玩家观察 |
|---|---|---|---|
| 18:28:18 | 动画结束 → step 98 | 槽位 2 + 1402942（**无关窗**） | 凹槽**开启** |
| 18:27:17 | 203164 接取 → step 93 | 槽位 2 + 1402942 + `SM_DIALOG_WINDOW(0,0)` | 关闭 |
| 18:28:50 | 领结晶 → step 95 | 槽位 2 + 1402942 + `SM_DIALOG_WINDOW(0,0)` | 凹槽**关闭** |

⇒ 客户端在收到 `SM_DIALOG_WINDOW(0, 0)` 时会重置烙印窗口的展开渲染，
**槽位状态必须是最后到达的协议事实**。修复（纯数据，无引擎改动）：
`1929.xml` 中 36 处 `after-commit` 的 `sync-quest-state → close-dialog` 反转为
`close-dialog → sync-quest-state`（脚本 `reorder_close_before_sync.py close-first`），
使同步携带的「槽位数 + 任务开启凹槽通知」落在关窗之后。

门禁：`Quest1929RetailAlignmentTest` 7/7（after-commit 断言已按新序更新）、
`QuestStepDialogTerminationTest` 1/1。

> **第六轮已证伪并回退此假设**（见下）：关窗在前/在后结果一致，顺序不是判据。

## 第六轮：顺序假设证伪 → 改为「打开烙印窗口前重发槽位数」（2026-10-07 18:33–18:35 复测）

实机复测（`log/console.log` 18:35:20–18:35:27，验收角色，NPC 205111）：

```
18:35:24 [C->S] CM_DIALOG_SELECT 上一页=2377 动作=2461          ← 点「结束对话」
18:35:24 [S->C] SM_DIALOG_WINDOW targetObj=0   下发页=0          ← 关窗（新顺序：关窗在前）
18:35:24 [S->C] SM_CUBE_UPDATE  下发=2
18:35:24 [S->C] SM_QUEST_ACTION 1929 步数=95
18:35:24 [S->C] SM_SYSTEM_MESSAGE 1402942
18:35:26 [S->C] SM_DIALOG_WINDOW targetObj=156571 下发页=2461    ← 再次对话：装备引导页
18:35:27 [S->C] SM_DIALOG_WINDOW targetObj=156571 下发页=1       ← 打开烙印窗口（DialogPage.STIGMA）
```

结果：**关窗在前、槽位包在后，凹槽依然关闭** ⇒ 第 5 轮的「关窗顺序」假设被证伪。
（`PlayerController`/`QuestExecutionCoordinator` 侧确认关窗与同步的包序确已按新顺序下发，
`target/classes` 中 XML 与源一致，故本次复测确实跑在反转后的数据上。）

修正后的模型（同时满足三轮观察）：

| 观察 | 事实 | 模型解释 |
|---|---|---|
| 98（过场动画后） | 无关窗 ⇒ 凹槽**开** | 登录/升级/任务提交下发的槽位数进入客户端缓存并渲染 |
| 首次点「结束对话」→ 95 | 关窗 + 槽位(2) + 通知（顺序无关） | 关窗包触发的**槽位缓存重置晚于同一批包生效**，批内后续的槽位包（值未变化）救不回来 |
| 再对话 → 页 1（烙印窗） | 无任何槽位下发 ⇒ 凹槽仍**关** | 窗口内的凹槽渲染读取的就是被重置后的客户端缓存 |

⇒ 修复方向：**槽位数必须在「打开烙印窗口」这一交互里重新下发**（与包序无关、与关窗无关）。
实现（数据无关、非任务特例，两个开窗入口各一处）：

- `PlayerQuestDialogPort`（任务引擎侧，1929/2900 的 `show-dialog-window dialog-id="1"` 路由）：
  新增 `stigmaSlotRefresh`（`Consumer<Player>`，生产默认 `StigmaService::refreshStigmaSlots`），
  在 `showDialog`/`showSelectionDialog`/`showDialogWindow` 发送 `DialogPage.STIGMA`（页 1）**之前**调用，
  保证槽位是窗口渲染前最后到达的协议事实。
- `DialogService`（烙印名人/Stigma Master 的 `OPEN_STIGMA_WINDOW` 通用入口）：
  抽出 `openStigmaWindow(player, targetObjectId)`，先重发槽位数再发页 1（两个种族分支复用）。
- 追加（同一轮 19:03 实机反馈后）：**被标记任务的每一页对话都重发槽位**。19:03 复测显示 98（无关窗）时
  客户端还剩 **1 格**可用、领结晶（关窗批）后 **0 格** ⇒ 缓存被关窗清零与「窗口读缓存」两条互相印证；
  但玩家从「再次对话」到「点开烙印窗」之间仍会看到 0 格。故把条件扩展为
  `dialogId == DialogPage.STIGMA.id() || declaresStigmaSlotExtension(questId)`（任务数据门控，
  非任务 ID 白名单）：1929 的 2375/2376/2377/2461/页 1 每一页下发前都重发槽位，
  玩家再次对话的那一刻槽位就回来了。任务引擎未由 Spring 提供（单元测试/未启动进程）时该判定按
  「未声明」处理（`declaresStigmaSlotExtensionBestEffort`），派生推送绝不影响对话交付。
  重发口统一为 `StigmaService.reannounceStigmaSlots(player, dialogId)`：普通页只重发槽位数；
  **页 1（烙印窗口）额外补发真端 1402942「任务开启凹槽」通知**——第 3 轮已确认客户端对
  「开启通知」与「槽位数」分别处理凹槽展开，窗口正是玩家核对凹槽来源的界面，其余页面不发以免刷提示。

回退：第 5 轮对 `1929.xml` 的 36 处顺序反转用 `reorder_close_before_sync.py sync-first` 还原为仓库既有约定
（`sync-quest-state → close-dialog`，QE-143），`Quest1929RetailAlignmentTest` 三处 after-commit 断言同步回退。

门禁（IDEA MCP 运行点，均 exit 0）：`PlayerQuestDialogPortTest` 14/14（新增
`stigmaWindowReannouncesSlotsBeforeTheWindowPacket`——断言重发发生在窗口包**之前**、
`questDeclaringTheStigmaSlotExtensionReannouncesSlotsOnItsDialogPages`、
`otherDialogPagesDoNotReannounceSlots`）、`Quest1929RetailAlignmentTest` 7/7、
`QuestRawDialogAfterCommitTest` 2/2、`DialogServiceTest` 3/3、
`QuestMinionTutorialProductionFlowTest` 3/3（回归：未标记任务不产生额外槽位包）。

### 残留（第八轮后已由「发放分支不关窗」取代）

第 6 轮的「每一次交互都重发槽位」保留（开窗前重发仍是对的），但它已被证明无法恢复被关窗清掉的
客户端展开态；恢复只有两条路：**进入世界（登录）时的包序**，或**从一开始就不关窗**（第八轮采用的正是后者）。
若冷重启 + 重登后、按引导页打开烙印窗口仍显示 0 槽，则说明客户端槽位态的判据不在本服务端下发的任何包内，
需按客户端侧继续取证（下一步判据：重登瞬间的槽位显示，即登录推送是否被客户端采纳）。

## 第七轮：开窗前重发已生效，但客户端槽位态恢复不了（2026-10-07 19:11–19:12 复测）

实机日志（`log/quests.log`）证明第 6 轮的重发**确实按预期下发**：

```
19:12:09 [STIGMA-TRACE] SM_CUBE_UPDATE 下发=2     ← 2461 引导页前重发
19:12:09 [QUEST-TRACE]  SM_DIALOG_WINDOW 下发页=2461
19:12:11 [STIGMA-TRACE] SM_CUBE_UPDATE 下发=2     ← 烙印窗口（页 1）前重发
19:12:11 [QUEST-TRACE]  SM_DIALOG_WINDOW 下发页=1
```

结果：**槽位仍未恢复**。结合第 5/7 轮的分布（98＝无关窗⇒1 格；领取结晶＝有关窗⇒0 格且此后不可逆），
结论收敛为：**客户端的烙印槽位展开态不是由 `SM_CUBE_UPDATE` 在会话内重建的**；能重建它的只有
**进入世界（登录）时**的包序（与 AEMU `.givestigma unlock` 的「Try Relog Now」一致），
而**服务端关窗包会不可逆地清掉它**——这正是发放分支（教学链上唯一带关窗的交互）独有的动作。

## 第八轮：发放分支改为「不关窗，直接打开烙印窗口」（真端/退役 XML 形状）

- `1929.xml`：22 条发放分支（spawned98 → granted95）的 after-commit 由
  `sync-quest-state(PACKET_ONLY) + close-dialog` 改为 `sync-quest-state(PACKET_ONLY) + show-dialog-window(dialog-id="1")`
  （脚本 `rewrite_grant_after_commit.py`）。真端/退役 XML 的发放分支本就是 `give-item + show-dialog-window(1)`：
  交付结晶与打开烙印窗口是同一个点击，教学链从不关窗。
- 打开窗口需要对话对象 ID，而该按钮在客户端**不带对象 ID**（快照 targetless，日志 `targetObj=0`）：
  `PlayerQuestDialogPort.resolveObjectId` 在 targetless 且目标页是 `DialogPage.STIGMA` 时，回落到
  玩家正在进行的任务对话授权（`Player.getNpcQuestDialogObjectIdForQuest(questId)`，由任务列表点击登记，
  同一 NPC + 同一任务）——这是服务端自己的权威记录，不是用 templateId/target 猜测；
  真端 `Npc::OnHyperlinkAction_OpenStigmaWindow` 在发页 1 之前同样先登记对话对象（`FUN_14019a6a0`，
  见 `58Server/server58-source/MainServer_Server64/classes/NPC/Npc.cpp:12279`）。
- 首轮对话因此仍只有**一个**「结束对话」按钮：点它 ⇒ 发放结晶 + 推进 95 + 直接进入烙印窗口（无第二页、无关窗）。
  再次对话（未装备）仍走 `granted95` 的引导页路由。

门禁：`PlayerQuestDialogPortTest` 16/16（新增 `targetlessStigmaWindowUsesTheRememberedDialogPeer`、
`targetlessStigmaWindowStaysUnboundWithoutADialogAuthorization`）、`Quest1929RetailAlignmentTest` 7/7、
`QuestStepDialogTerminationTest` 1/1。

## 第九轮：定性 = 教学步数 98↔95，发放不再推进步数（2026-10-07 19:26 复测）

19:26 复测（`log/quests.log`）证明第 8 轮形状本身已生效——发放那次**没有关窗**，烙印窗口按对话对象打开：

```
19:26:40 [C->S] CM_DIALOG_SELECT 上一页=2377 动作=2461
19:26:40 [S->C] SM_QUEST_ACTION 1929 步数=95
19:26:40 [STIGMA-TRACE] SM_CUBE_UPDATE 下发=2 ×2（提交口 + 开窗前重发）+ 1402942
19:26:40 [S->C] SM_DIALOG_WINDOW targetObj=151597 questId=0 下发页=1   ← 烙印窗口，且无 下发页=0
```

结果：**凹槽仍然关闭** ⇒ 第 6/8 轮的「关窗包」假设被证伪。与 98 时刻（无关窗、同批 cube+notify，
凹槽开启，用户 19:24–19:25 两次复现）对比，唯一的差别只剩 **`SM_QUEST_ACTION 步数=95`**：

| 时刻 | 步数 | 同批包 | 凹槽 |
|---|---|---|---|
| 过场动画结束 | **98** | quest-action + cube + notify | **开启** |
| 领取结晶（任意版本） | **95** | quest-action(95) + cube + notify（± 关窗、± 重发） | **关闭且不可恢复** |
| 再对话 / 打开窗口（95） | 95 | cube + notify + 页 1 | 仍关闭 |

用户口径「以前 xml 的时候不会有这个问题」与之完全一致：退役 XML/真端在发放时**不推进步数**
（`spawned98` 98 → 装备后 `equipped96` 96），自然没有 95 这个状态。

### 修复：恢复「步数保持 98」的真端形状（脚本 `restore_step98_grant_flow.py`）

1. 删除 `granted95` 节点与其全部路由（该节点是第 1 轮为门控重放自造的）；
2. 22 条发放分支回到 `spawned98` 自环：交付分支 = `give-item` + sync + 打开烙印窗口；
   已持有分支 = 只开窗口（无状态变化，按不变量不发同步）；两者都不再 `set-variable step=95`；
3. 入口按 **lore 道具持有量**门控（`priority="0"` 优先于剧情回退页的 `priority="1"`，同 4217 的既有写法）：
   已持有结晶 ⇒ 直接出示装备引导页 `SELECT5_2`；未持有 ⇒ 原剧情页 `SELECT5`；
4. 引导页按钮 `SELECT5_3` ⇒ 打开烙印窗口（页 1）；装备分支仍 `step 96 + sync + close`（真端形状）。

门禁（IDEA MCP，均 exit 0）：`Quest1929RetailAlignmentTest` 7/7（重写为
`grantsTheStigmaOnceAndKeepsTheInstallStep` / `routesHeldStoneTalksStraightToTheGuidePageAndTheWindow`：
断言无 `granted95`、`spawned98` 步数=98、发放分支无 set-variable、持有量门控优先级、装备分支形状）、
`QuestDefinitionCatalogManifestTest` 10/10（含 707 全量生产目录编译 + 无歧义校验）、
`PlayerQuestDialogPortTest` 16/16。

### 残留判据

若重登后在 98 状态下（走完过场动画、**不领取**、直接打开烙印窗口）凹槽仍关闭，则「步数 98 开启」这条
也要作废，下一步需取客户端侧证据（重登瞬间的凹槽显示 = 登录包序是否被采纳）。

## 本主题保留的证据与脚本（稳定名，供复核）

| 文件 | 作用 |
|---|---|
| `DIAGNOSIS.zh-CN.md` | 本诊断全文（十轮实机排除 + 修复形状 + 机械普查 + 验收） |
| `retired-1929.xml` | 退役定义（git 历史 `quest_definition/quests/1929.xml` 副本，旧「整数步」形状的真端对齐基准） |
| `legacy-_1929A_Sliver_Of_Darkness.java`、`legacy-2900.java` | 旧 AionEmu handler（`8994fc8fc^` 前后）与 2900 迁移前 handler 副本，lore 幂等语义与页面链的证据 |
| `apply_1929_grant_once.py` | 第 1 轮转换脚本（granted95 形状；已被终版取代，保留作过程留痕） |
| `rewrite_grant_after_commit.py` | 第 8 轮脚本（发放分支 after-commit 改开窗；终版仍沿用其结论） |
| `reorder_close_before_sync.py` | 第 5/6 轮脚本（after-commit 顺序翻转/回退，`close-first\|sync-first`；结论已证伪，保留作反例留痕） |
| `restore_step98_grant_flow.py` | **终版转换脚本**：删除 granted95、发放回到 `spawned98` 自环（不推进步数）、入口持有量门控 |
| `.agents/summary/quest-acceptance/1929-2026-10-07-client-accepted.md` | 客户端验收记录（ACCEPTED_NEW_PATTERN） |

## 机械普查（同类形状，未修，留待裁定）

生产目录中「对话动作 → 自环 → give-item → 无状态推进」形状共 **26 条 / 18 个任务**：
10530、11216、1170、14052、18511、19000、19002、20530、21080、2221、2223、24022、24030、24114、24154、
28511、29008、3200。多数带 `variable-is` 门（发放已由变量门控）或属交互物 `USE_OBJECT` 语义，
需逐件对照客户端页面/真端表裁定，不得套用本任务的规则批量改。

## 验证状态

- 已完成（无需构建）：XML well-formed + 结构断言（22 分支 / granted95 节点 / 路由 / after-commit 顺序）、
  页面全部落在客户端契约 `client_dialog_contract.tsv` 内、`git diff --check` 通过、IDE 静态检查 0 error。
- 门禁（用户 2026-10-07 授权，IDEA MCP 运行点，均 exit 0）：
  - `Quest1929RetailAlignmentTest`：7/7 通过（终版含新增 `grantsTheStigmaOnceAndKeepsTheInstallStep`、
    `routesHeldStoneTalksStraightToTheGuidePageAndTheWindow`、`plansTheStigmaGrantOnlyWhileTheStoneIsAbsent`）；
  - `QuestDefinitionCatalogManifestTest`：10/10（生产目录整体编译 + 契约断言）；
  - `ProductionCatalogWhitelistVerificationTest`：`PRODUCTION_COMPILE_OK=707`、`FAILURES=0`、
    `INTERACTION_OBJECT_FAILURES=0`、`WHITELIST_VIOLATIONS=0`；
  - `QuestProductionStartupGateTest`：2/2；`QuestStepDialogTerminationTest`：1/1；
  - 第二轮（槽位修复）新增/变更：`StigmaServiceSlotCountTest` 6/6、`StigmaSlotQuestFlagTest` 3/3
    （断言全库只有这 4 个定义声明 `extend-stigma-slots`）、`PlayerQuestStateSyncPortTest` 3/3、
    `QuestMinionTutorialProductionFlowTest` 3/3、`Quest80487ProductionFlowTest` 3/3、
    `QuestDialogLoopBreakerProductionFlowTest`（e2e）2/2。
- **存量红灯（与本次改动无关，证据已核）**：`QuestMetadataFieldMappingTest`（expected 740 / actual 733）、
  `QuestRetailStartMetadataGateTest.contractCoversExactlyTheProductionCatalog`（16800+ 等契约行无生产定义）、
  `Quest1112ProductionFlowTest`（1112 已退役：`quests/1112.xml` 在 HEAD 即不存在）。
  三者同源于「真端表驱动迁移」后目录收缩/退役行缺定义；本次未增删任何目录条目
  （`quest_definition_catalog.xml` 未改动，且目录整体编译 707/0 失败）。
- 客户端验收：**ACCEPTED**（2026-10-07 晚，用户实机整链复测后明确回复「实机验证成功，提交」）——
  详见下节第十轮；验收记录 `.agents/summary/quest-acceptance/1929-2026-10-07-client-accepted.md`。

## 第十轮：实机验收通过（2026-10-07）

用户实机复测后明确回复「实机验证成功，提交」，整链验收通过：

1. 首次对话只有 **一个**「结束对话」按钮；点击后只发放 **一次** 结晶并直接进入烙印窗口
   （不推进步数、不关窗、不再弹出第二个按钮、不再整链重放）；
2. 再次对话（未装备）只出示装备引导页，按钮打开烙印窗口，不再发放；
3. 步数保持 98 ⇒ 客户端烙印凹槽开启（2 格），结晶可安装，装备后任务按 select6 继续推进。

repair 提交 `b632ee3b5`；Pattern 资格：**ACCEPTED_NEW_PATTERN**（见 Playbook 三个 Pattern 与记忆库 QE-158）。

## 未捕获项

- 本次运行的 object/instance ID、抓包、截图 not captured；真端 ScriptDLL 无 1929 任务脚本（mission 类
  对话机不在 ScriptDLL 的 Simple* 类里），故以客户端页图 + 孪生任务 2900 的 legacy/XML 契约 + 旧 handler
  lore 语义为证据；用户实机结论为准。

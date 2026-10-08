# 帕休曼迪尔寺院走廊雕像阵补写（真端 select_prob 权重还原）+ 216297 无敌疑云定案

## 1. 需求

- 实机报障：「npc 216297 有个无敌 buff，且不消失」；随后用户质疑：「216297 的确不是雕像，能够攻击，
  排查真端，是不是这个 npc 和无敌技能是模拟端模拟出来的」。
- `//movetonpc 216295` 报 `未找到 NPC 生成配置：216295`——真身雕像在我方零刷点。
- 用户裁决：**真端什么样就什么样**（每组按权重随机 1 只）；顶楼孤 216297 点位（真端无）**删除**。

## 2. 216297「无敌不消失」定案：真端行为，非模拟端自造

三层真端原始数据逐层核对（UTF-16 解码）：

| 层 | 文件 | 事实 |
|---|---|---|
| 服务端 monster 表 | `<真端根>/Map/XML/npcs_monsters.xml` | 216297 技能表 8 项：0 号 `BNFI_Invincible_Statue`（无敌）、7 号 `NEL_DispelInvincible_Self`（解除）；`unattackable=0`（可选中攻击）；`move_speed walk/run=0`（站桩） |
| 原始 AI pattern | `<真端根>/Map/XML/NpcAIPatterns_TeCa_JM.xml` | `IDCT_StDrakanFi_Fake` = on_wake_up 自施 SKILLI_INDEX_0 + 感知/被击 5 事件全部显式 `do_nothing`（不索敌、被打不反击） |
| 客户端模板 | `<真端根>/Map/XML/npcs.xml` | mesh `DrakanFighterHigh_Statue`（雕像外观）、三速全 0 |

**真端设计**：Fake = 永久无敌站桩装饰；玩家可选中、可挥刀（unattackable=0），但伤害被无敌吸收，
它永远不会动/还手。我方转写逐项一致（速度 0、血量 40200、技能组、retail_pattern 装配）——**不是 bug**。

真身（IDCT_StDrakanFi/Sc）解无敌路径：①收到 `IDCT_StatueNPC` 广播的消息 7000（30m）；②同伴被攻击联动
（on_see_friend_attacking/attacked，flag 条件）→ 自施 18129 解除无敌进入战斗。

## 3. 真正缺口：走廊 10 组雕像阵整体缺失

真端 `SPG_C_StDrakan_55_Ae_1..10`（no_respawn）：左列 x≈1187.2-1187.4、右列 x≈1243.3-1243.4，
y = 509.4/539.4/569.5/599.7/629.8（每 30m 一组），左列 dir=0、右列 dir=180，z=251。
每组 **5 个 npc 条目**（N 页 4 候选 + H 页 1 候选）：

| 条目 | 概率（select_prob） |
|---|---|
| N 真身 Fi `IDCatacombsN_StDrakanFi_55_Ae`（216295） | 3000 |
| N 真身 Sc `IDCatacombsN_StDrakanSc_55_Ae`（216296） | 3000 |
| N 假 Fi `IDCatacombsN_StDrakanFiFake_55_Ae`（216297） | 2000 |
| N 假 Sc `IDCatacombsN_StDrakanScFake_55_Ae`（216298） | 2000 |
| H 真身 Sc `IDCatacombsH_StDrakanSc_55_Ae`（216215） | 5000（spawn_page 2） |

N 页权重和恰为 10000 → "每组按权重随机 1 只"；真端每次进本独立抽取。

## 4. 改动

1. `condition-spawns.xml`（world 300170000）：新增 **#5023~#5032**（10 组）。每组恒真表达式
   `1 >= 1`（Expression 原生支持数字字面量；未声明变量回退 0 亦有 Ahbana 批次先例）+ 1 group +
   1 slot × **4 个 `<party>`**（probability 3000/3000/2000/2000——loader 的 party=ConditionSpawnChoice，
   slot 内多 party 即"权重 N 选 1"，`select()` 要求权重和恰为 10000 否则抛 IllegalStateException）。
   坐标/朝向逐组取真端；z=251 + 引擎强制 resolve_z 贴地。条件总数 4452 → **4462**。
2. `300170000_Beshmundir_Temple.xml`：删除孤 216297 点位 (1447.94, 1388.14, 302.295)（真端无此出处，
   旧 emu 遗留），留注释指向 #5023-#5032。
3. `RetailAiDefinitionLoaderTest`：计数 4462；新增雕像阵断言（10 条 / 恒真表达式 / 5023 的 4 choice
   权重和 10000 / 首末 choice 成员 id 与坐标朝向 / initial_delay=1）。
4. `RetailPatternAI2Test`：`btConditionChainNpcsKeepRetailSupportWithProductionData` 清单 19 → 23
   （+216295/216296/216297/216298，带真实 owner 跑 supports）。

## 5. 验证

- 静态：`xmllint` 两数据文件良构；IDE 两测试文件 0 错误；`git diff --check` 干净。
- focused-test（2026-10-08，IDEA MCP）：`RetailAiDefinitionLoaderTest` 类级 **6/6 全绿**；
  `RetailPatternAI2Test#btConditionChainNpcsKeepRetailSupportWithProductionData` **通过**（23 NPC supports 全过）。
- 实机（待用户，需重启）：走廊每组 1 只——真身（约 60% 概率）打它一下看无敌解除（自施 18129）并反击；
  假雕像（约 40%）永久无敌站桩（真端行为，勿再报障）。`//movetonpc 216295` 应可传送（条件刷生成后按存活 NPC 传送）。

## 6. 边界与遗留

- **H 页真身 Sc(216215) 5000 权重未落地**：`select()` 权重和必须=10000，单 choice 5000 会抛异常；
  且 H 套按既有 bt_page==1 口径预留（无入口）。将来补 H 页时需把"50% 不出"语义另行建模。
- **真端 StatueNPC 机关未核查落地**：消息 7000 的发送者 `IDCT_StatueNPC`（30m）在我方 BT 的刷点与装配
  未验证——若实机打真身不解除无敌，沿此方向查。
- **本日验收通过的摆渡人链**（#2093-2095，三岛船夫 + Macunbello #5013）已由埋点日志全链实锤
  （见 console.log 16:40:51-16:41:57 段）；此前"船夫不出现"根因：①玩家杀的是灵魂（走 debufflich 链）
  ②soulwatcher 落水（见下）③一次击杀后 90s 内 `//reset instance` 清掉了刚生成的船夫。
- **遗留三问题（未修，待立项）**：
  1. NPC 追击跨水 z 崩坏（soulwatcher 从码头甲板 221.31 追进纯水区后 z→216.94 卡船底）——移动引擎追击路径问题；
  2. `//reset instance` NPE：实例销毁时 Temadaro on_despawn 仍向已销毁实例刷 DespawnLich(281697)；
  3. 广播消息遍历不跳尸体：broadcast_message 的 doOnAllNpcs 对已死 NPC 执行 on_message → despawn_self
     二次删除 NPE（WorldMapInstance 访问器捕获，仅打断当次广播）。

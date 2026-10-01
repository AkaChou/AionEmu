# SimpleTalk codegen 流水线还原（P3 前置）

> **批次**：SimpleTalk 纯对话任务家族（P3 切换批前置考古，后台 Explore agent，只读）。
> **方法**：抽样 7 任务（14275/14270/24270 单段，1471/1691/2641 三段链，1469 带物品），quest id→hex 全部
> python 程序化换算（14275=0x37c3、14270=0x37be、24270=0x5ece、1471=0x5bf、1691=0x69b、2641=0xa51、
> 1469=0x5bd、1131=0x46b）；定点扫描（grep id hex）+ 全文函数体阅读 + **capstone 反汇编补 IDA 未分析函数**
> （LAB_ thunk 族）双轨验证。
> **证据缩写**：`SC` = /Users/mc/IdeaProjects/58Server/server58/MainServer_ScriptDLL64/ScriptDLL64.c（行号）；
> `f731` = server58-source/MainServer_ScriptDLL64/fun/fun_731.cpp（行号）；`ASM` = 本轮 capstone 对
> ScriptDLL64.dll（image base 0x180000000）的反汇编输出；`XML` = 本仓 Quest_SimpleTalk.xml；
> `HP` = 本仓 HtmlPages.xml（UTF-16，`<htmlpage><id>N</id><name>` 形）。

## 0. 总架构（一句话版）

每个 SimpleTalk 任务在 **CRT 动态初始化数组**（.rdata，VA≈0x1811c87c0 起，file off 0x11c6fc0 起，ASM 实证
指针连续排列）里有一组静态初始化函数：`b5920`（NPC名→questId 注册节点）+ `b3070`（步进描述表）+ 若干
`cb2ac0/cb2ad0`（把**每任务生成的 thunk 函数**写进节点的 102 槽回调表）；运行期由通用分派器
（`cab520` 接取侧 / `cabb10` 对话-报告侧）消费这些 thunk。**页 id 全是通用 select 页字面量，不是每任务页**；
任务差异只体现在 questId、步进号、con_quest 下一环 id、物品表、movie 触发这几个 thunk 立即数上。

## 1. talk 专属 helper 分类表（函数/签名/语义/证据）

**写入器（codegen 调用面）**

| 函数 | 签名 | 语义 | 证据 |
|---|---|---|---|
| `FUN_180cb5920` | (out node, wchar* npcName, questId) | 构造 IOneQuestScriptNpc 节点（0xc80B）：vtable=`IOneQuestScriptNpc::vftable`@0x18123d3b8（仅 1 项 dtor，ASM）；名字 wchar@+6；**102 个回调槽 @+0xa8+i*8（清零）+ 102 个步进值 dword @+0x3d8+i*4（初始化 0xffffffff）**；尾插全局 map `DAT_1847204c8`（key=questId，multimap） | SC:2146365-2146437；ASM 0x180cb5934 `lea rax,[rip+..]→18123d3b8` |
| `FUN_180cb2ac0` | (dummyRec, node, slot, cb, local) | 槽写入：`node+0xa8+slot*8 = cb` | f731/SC:2143901-2143911 |
| `FUN_180cb2ab0` | 同上 | 第二张表 `node+0x404+slot*8`（本批样本未用） | SC:2143890 |
| `FUN_180cb2ad0` | (dummyRec, node, kind=3, stepIdx, cb, local) | **talk 链步进追加器**：从 node+0x1e0（即槽 0x27）找空槽至槽 0x31，写 cb@+0xa8+i*8、stepIdx@+0x3d8+i*4 | SC:2143913-2143935 |
| `FUN_180cb3070` | (dummyRec, node, questId, kind u8, value u32, extra u32) | 步进描述数组：+0x570 计数（max 0x14）、+0x574=questId（首条）、+0x578+i*6={kind,value,pad}、+0x5f0+i*4=extra | f731:6477-6500 |

**运行期分派器（thunk 的目标，全任务共享）**

| 函数 | 地址/行 | 语义（逐动作） |
|---|---|---|
| `FUN_180cab520` | f731:1669（SC:2139262） | **接取侧页动作分发**（thunk 形态 `cab520(questId, mgr, evt, giveItemId, giveCnt)`）。evt+0x30→player、evt+0x28→状态(0/10 才继续)、evt+0x38→当前链接 id：1002→`+0xd8`=SetQuestAcquired(questId) 成功后发页 1003；1003→页 1004；1007→mgr+0x1a0（拒绝流，页 1007=quest_refuse_4）；1012/1013→原样发页；**20000→SetQuestAcquired + mgr+0x5d8 状态包 + give_item(mgr+0x410)**；20001→player+0x2a8(0x1560e9,0x1e)+0x5d8 |
| `FUN_180caf7c0` | f731:4306 | cab520 姊妹版（+0xd8 调用带 param_4 标志 0/0x40000000），SimpleHunt 等家族用 |
| `FUN_180cabb10` | f731:1894（SC:2139499） | **对话/报告侧分发**（thunk 形态 `cabb10(questId, mgr, evt, finalStep, giveIds*, giveCnts*, remIds*, remCnts*, 0)`，数组各 3 槽×4B）。发页：`finalStep==iVar1(evt+0x18)`→页 2375(select5)；iVar1 0/1/2→1352/1693/2034；21..24→2376/2461/2546/2631。动作：**1009(0x3f1) 报告→phase==表值则 mgr+0x1c8 奖励窗，否则 +0x100 SetQuestSuccess+发页(idx=phase 映射)；10000/10001/10002→SetQuestProgress(+0xf0)(questId, 1/2/3)+0x5d8+GiveItem(+0x410)+RemoveItem(+0x1d0)；10020-10023→SetQuestProgress(21..24)+跳下一条 select 页**；20002/20004/20005 特殊分支 |
| `FUN_180cafa40` | f731:4410 | cabb10 姊妹版（进度写带 0/0x80000000/0xc0000000 标志位，页组同 1352/1693/2034/2375），非 talk 主用 |
| `FUN_180cab820` | f731:1777 | **cutscene/页 idx 选择**：phase {0xb,0x15→0; 0xc,0x16→1; 0xd,0x17→2; 0xe,0x18→3}→`mgr+0x1b0(player, questId, idx)` |
| `FUN_180caabb0` | f731:1339 | 发固定页（param_4）via mgr+0x188；talk 用 param_4=**0x3f3=1011(select1)** |
| `FUN_180ca9fb0` | f731:939 | mgr+0x5d8（quest 状态包刷新） |
| `FUN_180caad20` | f731:1393 | GetQuestState(+0xd0)→{state,vars}；**vars==param_4 → +0x100 SetQuestSuccess + 发页**（phase 门样板） |
| `FUN_180caa0b0` | f731:986 | phase 门发页：vars==param_4→2716(select6)；11/21→3057(select7)、12/22→3398(select8)、13/23→3739(select9)、14/24→4080(select10) |
| `FUN_180caac10` | f731:1355 | 完成流：+0xd0 读态→+0x100 SetQuestSuccess→+0x5d8→mgr+0x1c0(...,8,0)→条件 RemoveItem 循环（quest 5000 族 thunk FUN_180cbc620 用，SC:2146384） |

**每任务生成的 thunk（读不到函数体的 LAB_ 已用 capstone 还原）**

| 槽 | thunk（14275 例） | 等价体 | 证据 |
|---|---|---|---|
| 0x1c（接取 NPC） | `LAB_180de0d70` | `jmp caabb0(0x37c3, ·, ·, 0x3f3)` → 发页 1011 select1 | ASM@0x180de0d70；注册 SC:1543293 |
| 0x1d（接取 NPC） | `LAB_180cb5a70` | `jmp ca9fb0`（状态包刷新，全任务共用） | ASM@0x180cb5a70；注册 SC:1488744 |
| 0x26（接取 NPC） | `FUN_180e1eb80` | `cab520(0x37c3,·,·,0,0)` | SC:2336776-2336781 |
| 0x27..（链步进，b2ad0） | `FUN_180e54bf0` | `cabb10(0x37c3,·,·,0,&give_ids,&give_cnt,&rem_ids,&rem_cnt,0)` | SC:2355819-2355824 |
| 0x32（交付 NPC） | `LAB_180e2f110` | `jmp cab820(0x37c3,·,·,finalStep)`（14275/24270/14270 finalStep=0；1471=3；1938=2） | ASM@0x180e2f110/0x180e34910；注册 SC:1648450 |
| 0x35（交付 NPC） | `FUN_180ea8bc0` | `player+0x1b8 PlayMovie(·, questId=0x37c3, …)`（evt getter 0x18/0x38/0x48/0x40） | SC:2401143-2401158 |
| 0x1e（交付 NPC） | `FUN_180df8df0` | `mgr+0x1a8(player, 0x5bd)` = **接续下一任务（con_quest=1469）接取窗** | SC:2307619-2307631；注册 SC:1580104 |

结论：**"talk 专属函数"不是 2 个而是每任务 5-7 个注册 + 4-7 个 thunk**（家族普查的"约 2 个"需修正）；
其中真正每任务唯一的代码只有：cab520/cabb10/cab820/df8df0 型 thunk 的**立即数**
（questId/finalStep/con_quest/物品表）。

## 2. 注册面与槽位（含 0x37/0x38 验证）

一句话结论：**retail 表 codegen 用的槽是 0x1c/0x1d/0x1e/0x26/0x27-0x31/0x32/0x35（接取 NPC：
0x1c/0x1d/0x26；交付 NPC：0x1e/0x32/0x35/b2ad0 步进）；0x37/0x38 不属于 retail codegen，而是
event_quest.xml 运行时系统的默认槽**。两套共用同一张 map（DAT_1847204c8）+ 同一节点布局
（+0xa8 槽表/+0x3d8 步进值）。

证据：
- 14275（0x37c3）全部注册位（grep 全文仅此 10 处）：0x1d SC:1488744、0x1c SC:1543293、0x1e SC:1580104、
  0x26 SC:1629098、0x32 SC:1648450、b2ad0(3,0) SC:1689608、0x35 SC:1739481、b3070
  SC:1751900/1812268/1830088 —— **无 0x37/0x38**。14270/24270/1471 同（1471 另有 b2ad0(3,1/2/3)
  SC:1726671/1729973/1731585）。
- 0x37/0x38 归属：默认注册 stub `FUN_181074640(node)→cb2ac0(node,0x38,FUN_1810748f0)`、
  `FUN_1810746a0→0x37→FUN_181074b30→caad20(完成门)`（SC:2689575-2689615），被 **event_quest.xml 加载器**
  调用：工厂按 `type` 字符串分发（`L"talk"`=DAT_181390b58→SimpleTalkQuest ctor `FUN_18106bcf0`，
  SC:2684747-2684753；hunt/collect/use_item/item_play/serial_hunt 同段）；行解析器 `FUN_18106eb70`
  （acquired_npc_name→rec+0x28，SC:2685463+）；每 NPC 节点装配 `FUN_18106dfe0`（SC:2685226）。
  第二默认组 `FUN_1810722d0(0x38→FUN_181072430→caa0b0 phase 页 2716/3057/3398/3739/4080)`、
  `FUN_181072300(0x37→caadb0)`（SC:2687668-2687720）用于 collect 族。
- **静态注册实证**：14275 的 6 个初始化函数指针（0x180903880=b5920、0x1809326c0=0x1d、0x180a8ac90=b3070、
  0x180a0d740、0x180a3eec0=b2ad0、0x180a7c300=0x35）在 .rdata **连续**落在 file off 0x11c6fc0-0x11c7018
  （VA 0x1811c87c0-0x1811c8820），ASM 逐 qword 命中 —— CRT 动态初始化数组，即"离线 codegen、静态注册"
  的二进制铁证。

## 3. talk 链（talk_npc1/2/3）phase 门机制

一句话结论：**phase 写入 = cabb10 的链接动作（10000/10001/10002 → SetQuestProgress(+0xf0)(questId,1/2/3)；
10020-10023 → 21..24）；phase 读取 = evt+0x18（cabb10 用它与注册的 finalStep 比对选页、cab820/caa0b0/caadb0
用它映射页 idx 或完成门）**；每个 talk NPC 的节点用 b2ad0 绑定"步进号→cabb10 thunk"，b3070 在同节点写
`{kind=3,value=步进号}` 描述表。

证据：
- 1471（0x5bf，3 段链）步进表与回调一一对应：b3070 kind3 value=0/1/2/3
  （SC:1759952/1783261/1786055/1787419）↔ b2ad0(3,0/1/2/3)（SC:1699124/1726671/1729973/1731585）；
  step0/1/2 在 Likasas/Shugo_c16/Shugo_LC1_26 节点，step3（报告步）回 Likasas。4 个 cabb10 thunk 的
  finalStep 均烘焙为 3（SC:2364685/2389449/2391828/2392993）。
- 页阶梯（cabb10 内 switch，f731:1915-1945）：select2(1352)→动作 10000→进度 1→select3(1693)→10001→2→
  select4(2034)→10002→3→finalStep 命中→select5(2375)→1009 报告。21..24 分支（页 2376/2461/2546/2631）
  对应动作 10020-10023（f731:2037-2052），与 caa0b0/caadb0/cab820 里 `{0xb..0xe, 0x15..0x18}` 的 vars
  值域吻合（f731:1002-1022、1426-1449、1798-1816）。
- 完成门样板：caad20（f731:1393）`vars==param_4 → SetQuestSuccess(+0x100)+发页`；1009 报告分支
  cabb10 f731:1954-1981。
- **未还原**：evt+0x18 的生产者（哪个系统把 quest vars 装进事件）与 node+0x3d8 步进值的运行期读取者不在
  本 DLL 反编译内（见 §7）。

## 4. 报告/领奖流 + 页 id 对拍

一句话结论：**talk 报告/领奖是 cabb10 的 1009 链接 → 命中 finalStep 时 `mgr+0x1c8`（奖励/完成窗，形同
afa40 同型）；页 id 全部是 HtmlPages.xml 里的通用 select 页字面量，硬编码在共享分派器里**。

对拍（页 id → HP 内名称，python 解码 UTF-16 全部命中）：

| 页 id | HP 名称 | 用途（代码位置） |
|---|---|---|
| 1011 | HTML_PAGE_SELECT1 | 0x1c 接取页（caabb0 r9d=0x3f3，ASM） |
| 1002/1003/1004/1007 | quest_1_2 / quest_accept_1 / quest_refuse_1 / quest_refuse_4 | 接取流（cab520 f731:1688-1709） |
| 1012/1013 | select1_1 / select1_1_1 | cab520 0x3f4/0x3f5 |
| 1352/1693/2034 | select2/3/4 | 链中继页（cabb10 iVar1=0/1/2） |
| 2375 | select5 | 报告页（finalStep==iVar1） |
| 2376/2461/2546/2631 | select5_1..5_4 | 10020-10023 动作落点 |
| 2716/3057/3398/3739/4080 | select6..select10 | 事件表系统 phase 页（caa0b0） |

`20000/20001/20002/20004/20005` 不在 HtmlPages.xml（python 校验 MISSING）——是客户端发回的链接/动作 id，
非页。

## 5. 接取流

一句话结论：**talk 接取 = 交付/接取 NPC 节点槽 0x26 → cab520；链接 1002 或 20000 触发 `+0xd8`
SetQuestAcquired（返回 0 即失败返回），20000 路径随后发状态包并发 acquisition 物品**。

证据：cab520 f731:1692-1701（1002→+0xd8→页 1003）与 f731:1716-1737（20000→+0xd8→+0x5d8→
`param_5>=1 则 mgr+0x410(param_4,param_5)` 给物品）；接取物品实证：1131（0x46b）
`cab520(0x46b,·,·,0xadc28ba=182200506, 1)` SC:2321854 ↔ XML `give_item ITEM_QUEST_1131A 1`（XML:357）。
`+0xd8` 定名沿用 P7 IUserImp 结论；af7c0 同型调用见 f731:4331-4343。

## 6. 表列 ↔ 生成函数参数对拍（4 任务实证）

**14275（0x37c3）** `XML:1276-1281`：dev_name/acquired=Telemachus/reward=Hagne/con_quest=1469

| XML 列 | codegen 参数 | 证据 |
|---|---|---|
| acquired_npc_name=Telemachus | b5920(node@DAT_1857915f0,L"Telemachus",0x37c3)；其上挂 0x1c/0x1d/0x26 | SC:1451308；1488744/1543293/1629098 |
| reward_npc_name=Hagne | b5920(node@DAT_185791c30,L"Hagne",0x37c3)；其上挂 0x1e/0x32/0x35/b2ad0(3,0) | SC:1421872；1580104/1648450/1739481/1689608 |
| （无物品列） | cabb10 thunk 4 个 DAT（0x185e8b398/3a8/3b8/3c8）均在 .data 虚拟段未初始化区（ASM 校验超 raw 范围→运行期为 0）= 无 give/remove | SC:2355823 |
| con_quest=1469 | 0x1e thunk 立即数 0x5bd | SC:2307629 |
| （无 talk_npc） | b3070：kind0/-1@接取、kind3/0@交付、kind4/-1@交付 | SC:1812268/1751900/1830088 |

**1469（0x5bd）** `XML`：acquired=Hagne/talk_npc1=TreasureGuardianQ_36_Ae/**give_item1 ITEM_QUEST_1469A 1**/
reward=Hagne/con_quest=1470

| XML 列 | codegen 参数 | 证据 |
|---|---|---|
| give_item1=ITEM_QUEST_1469A 1 | 两个 cabb10 thunk（talk 步 + 报告步）giveIds=[**182201386**,0,0,0]、giveCnts=[1,0,0,0]（ASM 读 .data） | SC:2374739(DAT_18470f568/578)、2385975(DAT_18470f548/558) |
| con_quest=1470 | 0x1e thunk 立即数 0x5be | SC:2307645（FUN_180df8e20） |

**1131（0x46b）**（佐证 remove_item1）`XML:355-365`：give_item=ITEM_QUEST_1131A、
give_item1=ITEM_DOC_QUEST_1131B、remove_item1=ITEM_QUEST_1131A
- cab520 接取给 182200506(=1131A)（SC:2321854）；cabb10 remIds=[182200506]、giveIds=[182200507(=1131B)]
  （ASM：DAT_18470f0a8/f0b8/f0c8/f0d8 及第二套 f0e8-f118）↔ SC:2371081/2388429 ——
  give_item1/remove_item1 与槽位逐字节吻合。

**1471（0x5bf）**：链结构对拍见 §3（talk_npc1/2/3 → 三个 b2ad0 步进 + reward 复用步 3；con_quest=1938 →
0x1e 立即数 0x792，SC:2309581；0x32 邻座 thunk `cab820(0x792,·,·,2)` ASM@0x180e34930）。

## 7. EVIDENCE_MISSING 与补全路径

1. **槽回调的运行期调用者 + node+0x3d8 步进值读取者**：全文仅写者（f731:6244/SC:2143933）；读侧 `0x3d8`
   命中均为无关 vtable 调用。判定：事件装配/槽调用胶水不在 ScriptDLL64 反编译可见面（或 IDA 未还原常量）。
   补全路径：`/Users/mc/IdeaProjects/58Server/MainServer/` 主服务端其它模块（搜 IOneQuestScriptNpc 事件
   glue、或对 0x3d8 做全二进制 imm/riprel 六向扫描）。
2. **evt+0x18 getter 的填充源**（quest vars 原值 vs 换算步号）：位于 mgr 对象类（param_2 vtable
   +0x18/0x28/0x30/0x38/0x40/0x48 getter 实现），本轮未追。补全：从 mgr+0x188（发页）vtable 反推类，
   定位其构造与 getter。
3. **LAB_ thunk 与 atexit 清理函数 IDA 未生成函数体**：本轮已 capstone 逐个还原
   （0x180de0d70/0x180dd3db0/0x180ddc950/0x180ddcf90/0x180e2f110/0x180e2be30/0x180e33a50/0x180e34910/
   0x180cb5a70）；清理函数（LAB_18111bf20 等，SC:1451309）未还原（推测为 map 摘除，未实证）。
4. **20001 的 `player+0x2a8(0x1560e9,0x1e)`**：+0x2a8 槽语义未定名（0x1560e9=1441001，疑似 cutscene/特效
   id）。补全：沿 IUserImp vtable +0x2a8 实现函数。
5. **mgr 侧 vtable 定名**（+0x188 发页/+0x1a0 拒绝/+0x1a8 接取窗/+0x1b0/+0x1c0/+0x1c8 奖励窗/+0x410 给物/
   +0x5d8 状态包）为**用法推断**，未逐个打开实现体；如需定名按同法追 vtable 即可。
6. Quest_SimpleTalk.xml **无 emotion/emotion1 列**（grep 空）——背景中 `emotion1→+0x148` 属其它表 schema，
   P3 批不适用。

## 8. 对 P3 切换批的设计输入（主会话归纳）

1. **handler 形状**：native SimpleTalkHandler = 表驱动的通用页流（共享 select 页阶梯 1011→1002/1003/1004→
   1352/1693/2034→2375 + 动作词汇 1002/10000-10002/10020-10023/1009），每任务差异只有表行字段
   （NPC 对、链长、con_quest、物品表、movie）——**页 id 与动作码是全局常量（协议级），不是任务硬编码**，
   与 G3 SPIKE 的动作词汇双侧互证。
2. **页对拍 PASS**：代码用到的全部页 id 在入仓 HtmlPages.xml 有名；20000+/10000+ 是动作 id 非页。
3. **0x37/0x38 归属澄清**：retail codegen 用 0x1c/0x1d/0x1e/0x26/0x27-0x31/0x32/0x35；
   0x37/0x38 = event_quest.xml 事件任务子系默认槽（P2 报告 §4 的对象装载器槽位归属活动子系，与 §6.5 结论闭环）。
4. **物品 id 解码实证**：表列的 `ITEM_QUEST_xxx` 名形 ↔ codegen 数值形（182200506=1131A）逐字节吻合
   ⇒ P3 需要物品名→id 解析（对应 plan §6.7 的 RetailItemNameIndex KEEP/ADAPT 轴 + NativeNpcNameResolver 同法）。
5. **con_quest 链**：0x1e thunk 的立即数 = 下一环任务 id ⇒ con_quest 不只是接取前置门，还是**链式接取窗**
   （完成即弹下一环接取），P3 handler 须双语义都建模。

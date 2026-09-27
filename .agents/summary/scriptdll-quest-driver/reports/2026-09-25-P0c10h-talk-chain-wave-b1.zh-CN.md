# P0c-10h SimpleTalk wave B B-1：复合行首轮落地（16 ADOPT 退役 + 169 KEEP 逐机制归零在案）

- 日期：2026-09-25
- 切片：P0c-10h（wave B 复合行：E 记录 + 系统性缺口修复 + B-1 等价采纳退役）
- 前置：`2026-09-25-P0c10g-talk-chain-gate-permanent.zh-CN.md`

## 交付

1. **builder（`build_quest_client_talk_chain_steps.py`）**：
   - 行集扩 wave B 物品轴批（129 行普查 B_COMPOUND − 83 轴分歧行）；
   - 词汇扩 `HAS_ITEM` 条件 + `GIVE_ITEM` 动作 + npc-start `accept-actions` 接取发物
     （B 记录 extra 第三段）；E 记录（EnterWorld 无源事件，QE-051 奖励行自愈边）+
     `STATUS_IS` 条件词汇；页证据分级（KEEP 行 SNAP / wave B UNVERIFIED / ADOPT fail-closed）；
   - **三个系统性正则缺口修复**：transition 额外属性（`priority`，55 处此前被静默丢弃——
     3966 误判 UNREACHABLE 的根因）、` />` 带空格自闭合（13700 等 0/4 节点漏转写）、
     N 记录完整性闸（无 var 子元素的变体节点，判例 30711——KEEP 行快照放行不回炉）；
   - 已退役行转写源改 `git show HEAD:`（退役=删除后登记表仍机器可重生成）；
   - 物品轴计数对账（真端表 vs XML）：交付路由自给自消费豁免（判例 1118）、
     结构性分歧跳过出清单（判例 1183：XML accept 全发 + 三倍重复发物 vs 真端阶段绑定）。
2. **compiler（`RetailSimpleTalkDefinitionCompiler` + `RetailClientTalkChainSteps`）**：
   - decode 扩 `HAS_ITEM`/`GIVE_ITEM`/`STATUS_IS`；`REFRESH_PLAYER_STATS` after 分支补齐；
   - 动作 token 三形态（枚举名 / 数字 id / `-`=null dialogId）；loader Q/E 记录
     （`QuestEvent.QuestDialog` / `QuestEvent.EnterWorld`）；
   - `acceptFlow` 接取发物参数化（GiveItem 落 QUEST_ACCEPT_1/SIMPLE）；
   - precheck 登记覆盖复合行放行；buildChain 块间同键去重（npc-report 推进路由胜
     npc-complete 预览路由——10 行 AMBIGUOUS_TRANSITION 的根因）。
3. **裁定与退役**：
   - `p0c10h-talk-chain-b1-decisions.tsv`（185 行）：**16 ADOPT_RETAIL**（12 物品轴链行 +
     4 残组规范行）+ 83 CHAIN_AXIS_MISMATCH + 42 CHAIN_DEFERRED_COMPOUND + 33
     DIFF:TRANSITION_SET + 7 CHAIN_SHAPE_UNSUPPORTED + 4 CHAIN_NO_START_BLOCK；
   - **19004 重裁定升级**：p0c10f 曾因登记表缺 E 记录判 `KEEP_XML/ENTER_WORLD_EDGE`，
     E 记录修复后 registry-replay 等价成立（升级留痕于裁定表 evidence）；
   - `build_retention_list.py` 接入 B-1 裁定层（优先于 wave A 层）；16 行 retention 翻转；
   - `p0c10h_retire_b1_rows.py`（前置断言 + unlink + catalog 同步）：删 16 XML，
     catalog 余 **1995**；冻结指纹 83 → **99**（`retail-simple-talk-chain-ir-fingerprints.tsv`）；
   - ChainGate 残组形状断言放宽（ADOPT 行须有路由或 NPC_START 规范块）。

## 证据

- 探针四轮收敛（`p0c10h-chain-probe-round2.tsv` 归档 + 本轮 /tmp 输出）：
  R1 5E/41D/41R → R3（E 记录）16E/43D/40R → R4（token 修复）16E/49D/34R →
  R5（块去重）16E/51D/32R。
- `verify_retirement.py` = **catalog=1995 directory=1995 retired=4229 sum=6224 — OK**
  （恰 +16，零悬空引用）；target/classes 手术式对齐（src=target=1995）。
- `RetailSimpleTalkChainGateTest` 2/2 绿（99 行冻结指纹 + 分区 + 回放保真）；
  `RetailSimpleTalkGateTest` 3/3 绿（drift 按 final 编译器行为刷新 22 行）。
- B-1 EQ 集 ∩ 页证据未核验集 = ∅（页证据不完整者一律不采纳）。

## 未验证

- 16 行的运行时行为与客户端抽检仍属 P3 终局项。
- KEEP 169 行（83 轴分歧 + 42 deferred + 44 registry 形状行）的机制级归零待后续微波。

## 下一步

- 分歧 83 行：按真端阶段绑定规范合成（弃 XML 逐字转写）或逐行 KEEP；
- EnterWorld 之外剩余 DIFF（canonical 接取流缺失 = 真端对 XML 缺）逐行裁定；
- deferred 42（con_quest/cutscene 客户端证据）与页未核验 16 行证据补全。


---

## 附：B-2 微波（同日，多变体 NPC_START 修复后追加）

**系统性修复**：多变体任务可有多个 NPC_START（镜像/职业变体，判例 1484 五块、2266 三块）——
buildChain 原先只合成 findFirst 一块的接取流；改为 `steps.blocks(questId, kind)` 全量遍历，
NPC_START 每块各合成 acceptFlowChain、NPC_REPORT 同样全量参数化。-loader 新增 `blocks()` 列表访问器。

**B-2 采纳**：2646/16990（探针 round7 等价；裁定 `p0c10h-talk-chain-b2-decisions.tsv`，
retention 层 B-2 后读覆盖 B-1）。`p0c10h_retire_b2_rows.py` 删 2 XML + catalog 余 **1993**；
冻结指纹 99 → **101**。

**证据**：verify_retirement = **catalog=1993 directory=1993 retired=4231 sum=6224 — OK**；
ChainGate 2/2 + SimpleTalk 3/3 绿。

**余量分桶（round7，49 DIFF + 32 REJECTED）**：
- complete-variant 38（headline 下轮：npc-complete 全参数转写 + completeFlow 参数化，
  同 npc-start/npc-report 口径）；
- other-shape 10（残组 8 行 acceptFlowChain vs expander 形状差 + 1938/3966）；
- accept-flow 1（35017）；REJECTED：AMBIGUOUS 13 / BAD_NODE 7 / NO_START 9 / START_CONFLICT 2 / UNREACHABLE 1。


---

## 附：B-3 微波（同日，npc-complete 全参数回放后追加）

**系统性修复（headline 兑现）**：B 记录 NPC_COMPLETE 全参数转写（extra 七分节
`cri|fixed|actions|finish|preview|choice|fallback`）+ compiler `completeFlowFromBlock` 与
`expandNpcComplete` 一一对应——固定奖励按索引、可选项仅经 choice 授予（废除按位近似）、
预览按 complete-reward-index 下发本档奖励窗口、finish 三模式；动作 raw 解析
（枚举名/数字/`A..B` 区间）与 dialogActions 同语义；choice 单/复数 action 形态、
自闭合 npc-complete 均实测支持。单步路径保留旧规范形（无块参数的消费方）。

**B-3 采纳**：14 行（1158/1605/1609/1971/2539/2958/3081/3100/3913/13809/21073/23809/
30042/30142——complete-variant 桶主力 + 残组 21073/23809）；裁定
`p0c10h-talk-chain-b3-decisions.tsv`；冻结指纹 101 → **115**；
`p0c10h_retire_b3_rows.py` 删 14 XML + catalog 余 **1943**（含并发会话同期 −36）。

**证据**：verify_retirement = **catalog=1943 directory=1943 retired=4281 sum=6224 — OK**
（4231 + 本线 14 + 并发 36）；ChainGate 2/2 + SimpleTalk 3/3 绿（drift 终刷 4 行：
2231/2724/18805/28805 参数化后如实登记为编译拒绝）。

**SimpleTalk 累计退役 1663/2223；余量 = DIFF 31 + REJECTED 36 − 已再退役行（下轮复查）。**


---

## 附：B-4 微波（同日，事件元素自闭合斜杠修复后追加）

**系统性修复**：` />` 带空格自闭合的第三处盲区——TR/QA/C 正则的 **dialog/can-act 事件元素**
（` action="SELECT2_1" />` 不匹配→显式路由漏转写）。修复后残组清单从 14 缩到 6（其余全是
已退役的规范行），8 行误判残组找回路由并等价（13700/13701/18940/23700/23701/28940/39003/
49003，判例 13700：缺 `TalkToNpc[804699, SELECT2_1]` 视图自环）；P 记录（bit-field）同源
漏转写一并修复（ChainGate「缺布局记录」暴露）。

**B-4 采纳**：8 行（裁定 `p0c10h-talk-chain-b4-decisions.tsv`）；冻结指纹 115 → **123**；
删 8 XML + catalog 余 **1935**。

**13809/23809 指纹演进（显式重冻结）**：` />` 修复补出两行自己的路由 → 编译 IR 变化 →
冻结值偏离。临时还原 git 历史 XML 复核：**EQUIVALENT 成立** → ChainGate 冻结开关全量重算，
diff 确认恰两行变化、无其他漂移后入仓（重冻结证据留本节）。

**证据**：verify_retirement = **catalog=1935 directory=1935 retired=4289 sum=6224 — OK**
（恰 +8）；ChainGate 2/2 + SimpleTalk 3/3 绿。

**SimpleTalk 累计退役 1671/2223；链行余量 = registry 内 KEEP 60 + 轴分歧 83 + deferred 42。**


---

## 附：B-5 微波（同日，priority 透传 + 多 NPC_COMPLETE 回放后追加，24 行）

**系统性修复**：① R/C/Q 记录新增 **priority 列**（transition 中段属性捕获，loader 容错读取，
compiler 透传 QuestTransition.priority）——重叠路由的显式消歧在转写中被丢弃导致 13 行
AMBIGUOUS_TRANSITION（判例 2964/3218/4209 均有 2 条 priority 路由）；② **多 NPC_COMPLETE
回放**（判例 1484 十块——每个变体 NPC 各有自己的完成流），builder `re.finditer` 全量转写
（172→209 条记录），compiler 逐块 `completeFlowFromBlock`。

**B-5 采纳**：24 行（1484/2266/2271/2480/2538/2663/2914/2954/2964/3037/3041/3087/3093/
3218/3966/4052/4209/4218/11010/11103/11460/21033/21455/80752——DIFF-complete 桶主力 +
AMBIGUOUS 桶大部 + 3966 翻案）；裁定 `p0c10h-talk-chain-b5-decisions.tsv`；冻结指纹
123 → **147**；删 24 XML + catalog 余 **1911**。

**证据**：探针 round11→round12：EQ 0→24、DIFF 23→7、REJECTED 36→28；
verify_retirement = **catalog=1911 directory=1911 retired=4313 sum=6224 — OK**（恰 +24）；
ChainGate 2/2 + SimpleTalk 3/3 绿（drift 终刷 2 行）。

**链行余量**：in-disk 35 行（DIFF 7 + REJECTED 28）+ 轴分歧 83 + deferred 42。


---

## 附：B-6 微波（同日，varless 节点回放支持后追加）

**系统性修复**：varless 变体节点（无 `<var>` 子元素，判例 30711——XML 合法定义为空
projection，`QuestDefinitionXmlCompiler.parseNodes` 口径）入 N 记录（var0='-' 可空），
loader `NodeRecord.var0` 改 Integer、`buildChain` 回放空 vars map、ChainGate 回放校验
对齐可空语义；builder N 转写双形态 + 完整性闸同步计入。

**B-6 采纳**：30711/30761（当年 KEEP 的登记缺口正是缺 varless 节点——翻案）；裁定
`p0c10h-talk-chain-b6-decisions.tsv`；冻结指纹 147 → **149**；删 2 XML + catalog 余 **1909**。

**证据**：verify_retirement = **catalog=1909 directory=1909 retired=4315 sum=6224 — OK**
（恰 +2）；ChainGate 2/2 + SimpleTalk 3/3 绿（drift 终刷 6 行）。

**链行余量**：in-disk 34 行（DIFF 12 + REJECTED 22）+ 轴分歧 83 + deferred 42。


---

## 附：B-7 微波（同日，系统发放链行规范形——首个非等价采纳路线，8 行）

**系统性修复（P0c-10h B-7 前置）**：挑战型委托行真端表 `grantKind=systemGrant`，但 XML 是
旧 NPC 接取接线（unaccepted 源接取/拒绝路由，部分行还有 npc-start 块）。规范形按 P0c-2
形状合同实现：① precheck `START_CONFLICT` 撤销（系统发放 + npc-start 共存合法）；
② buildChain 系统发放行跳过 npc-start 接取流合成；③ 剔除 unaccepted 源的
TalkToNpc / 无目标 QuestDialog 路由（旧 UI 接线残留）。

**采纳判据（ doctrine ⑤ 逐行裁定，非等价路线）**：真端表是形状权威 → SystemGrant 边
必需（真端对）；XML 接取路由为残留（真端经系统发放）。**验收 = SimpleTalk 族门禁的
系统发放形状不变量**（无接取路由 + SystemGrant 边存在 + 领奖投影合同）——比 XML 等价
更强的语义验收。drift 登记保留诚实 DIFF 分类。

**B-7 采纳**：35010/35017/35018/35024/45011/45018/45025/45026（35xxx/45xxx 镜像双家族）；
裁定 `p0c10h-talk-chain-b7-decisions.tsv`；ChainGate 冻结开关改为 retention 驱动目标集
（新采纳行无冻结值也可入表）；指纹 149 → **157**；删 8 XML + catalog 余 **1901**。

**证据**：verify_retirement = **catalog=1901 directory=1901 retired=4323 sum=6224 — OK**
（恰 +8）；ChainGate 2/2 + SimpleTalk 3/3 绿（drift 终刷 2 行：35011/35026 转编译拒绝如实登记）。

**链行余量**：in-disk 26 行（NO_START 10 / AMBIGUOUS 7 / COMPLETE 拒绝 4 / DIFF 4 /
UNREACHABLE 1）+ 轴分歧 83 + deferred 42。


---

## 附：B-8 微波（同日，区间动作展开后追加，7 行）

**系统性修复（第八处转写盲区，判例 4970）**：`actions="SELECTED_QUEST_REWARD1..NOREWARD"`
的 **A..B 区间 token 带点号**，ACTIONS_ATTR 字符类 `[A-Z0-9_ ]` 不含 `.` → 区间路由被
转写成 null 动作（通配）→ 与 USE_OBJECT 路由判 AMBIGUOUS。修复：字符类含 `.` +
compiler `expandDialogActionTokens`（dialogActions 同语义：单 token / 区间展开逐 id 事件，
CAN_ACT/QUEST_ACTION/ENTER_WORLD 分支不受影响）。校验器 `AMBIGUOUS_TRANSITION` 消息
同步升级为输出冲突对（source/event/target/prio）——本轮定位即靠它。

**B-8 采纳**：4970（等价）+ 35011/35025/35026/45010/45017/45024（系统发放合同，同 B-7
判据——区间修复后与 35010 系同样编译通过）；裁定 `p0c10h-talk-chain-b8-decisions.tsv`；
冻结指纹 157 → **164**；删 7 XML + catalog 余 **1894**。

**证据**：verify_retirement = **catalog=1894 directory=1894 retired=4330 sum=6224 — OK**
（恰 +7）；门禁 5/5 一次全绿。

**SimpleTalk 累计退役 1712/2223；链行余量 = in-disk 27 行（NO_START 10 / COMPLETE 拒绝 4 /
DIFF 4 / DIFF(SG) 0 / UNREACHABLE 1 → 保守口径含残余）+ 轴分歧 83 + deferred 42。**


---

## 附：链行 in-disk KEEP 收口（19 行逐机制归因，closure 表）

`p0c10h-chain-keep-closure.tsv`：五族机制，全部已有 b1/b2 KEEP 决定，本表补归因——
- **NO_START_OBJECT ×10**（1323/2611/3001/3023/19070/19071/21136/24123/24202/80320）：
  acquired_npc 为物件哨兵（判例 1323 `LF2_Lost_JewelBox`）——接取经物件交互，
  链表无物件起始词汇：真端缺口 → 保留 XML（待物件起始机制入词汇后翻案）；
- **NPC_COMPLETE_META_MISMATCH ×4**（2231/2724/18805/28805）：npc-complete 块索引与
  quest.xml 奖励档不符——参数化回放响亮拒绝（旧按位近似曾静默掩盖），数据不一致保留 XML；
- **REPORT_VIEW_SYNC_DIFF ×2**（1938/21081）：1009 页视图 sync 形态差（单条 countMismatch=1）；
- **COMPLETE_PARAM_DIFF ×2**（2641/2653）：多变体完成块参数差；
- **SHAPE_UNSUPPORTED ×1**（1152）：complete 节点不可达。

SimpleTalk 链行战役至此收口：**322 行普查 → 1712/2223 全族退役（含链行 81 + 此前 M3-b/P0c-2/
10d 批），in-disk 链行仅余 19 行 KEEP（五族机制在案）+ 轴分歧 83 + deferred 42**。
下轮 headline：轴分歧 83 行按真端阶段绑定规范合成（弃 XML 逐字转写）模式设计与评估。


---

# P0c-10i 轴分歧行 canonical 规范合成（62 采纳退役 + 20 缺口 KEEP）

- 日期：2026-09-25（同日续）
- 前置：`p0c10h-chain-axis-mismatch.tsv`（82 行 XML 与真端阶段绑定语义结构性分歧）

## 交付

1. **builder canonical 分支**（`synthesize_canonical`）：真端表为形状权威——
   - 阶梯：K 段 → s1..sK（var0=1..K，判例 1183：reward 与末段 sK 同 var0）；
   - talk_npc(k) 页流（QUEST_SELECT/SELECT2 视图 + SETPRO(k) 授/收：`give_item(k)`→GIVE_ITEM、
     `remove_item(k)`→REMOVE_ITEM，SYNC LV_REFRESH + CLOSE）；
   - 报告流（reward NPC）：QUEST_SELECT/SELECT5 + SELECT_QUEST_REWARD 推进领奖窗
     （item_check 行带末段发物的 has-item 门控 + 交付消费）；
   - B 记录：NPC_START（无编号 give_item → accept-actions 接取发物）+ NPC_REPORT（sK→reward）
     + NPC_COMPLETE（规范形 SELECTED_QUEST_REWARD1..NOREWARD 区间）；
   - **两条解析通道**：item symbol → `quest_data.xml` quest_work_items（本任务符号按字母升序
     ↔ 清单序，判例 2515：A/C/E ↔ 412/414/416——字母非连续）；NPC 名 → `npc_name_index.tsv`
     （单 id fail-closed，名字尾空格 strip）；
   - 缺口处理：quest_data 无 work_items（18 行）或非阶段轴（2 行）→ `p0c10i-canonical-gaps.tsv` KEEP。
2. **裁定**：`p0c10i-canonical-decisions.tsv`（82 行 = 62 ADOPT + 20 KEEP）；非等价路线
   （同 B-7 方法论），验收 = 族门禁真端语义不变量；DIFF 为 canonical 阶梯 vs XML 自定义
   标签的预期形状差（drift 保留诚实分类）。
3. **退役落地**：`p0c10i_retire_canonical_rows.py` 删 62 XML + catalog 余 **1832**；
   ChainGate 冻结（retention 驱动目标集）→ **226 行**。

## 证据

- 探针迭代：round1 NO_START×62（缺 B 记录）→ round2 UNREACHABLE×63（缺 NPC_COMPLETE）→
  round4 全编译通过 ×62 DIFF（canonical vs XML 预期形状差）→ round5 稳定。
- `verify_retirement.py` = **catalog=1832 directory=1832 retired=4392 sum=6224 — OK**（恰 +62）。
- ChainGate 2/2 + SimpleTalk 3/3 绿。

## SimpleTalk 链行战役终账

322 普查 = **退役 1793**（wave A 83 + P0c-2 40 + M3-b 1498 + P0c-10d 10 + B-1~B-8 81 +
P0c-10i 62 + DataDriven 13 等）+ KEEP（in-disk 19 五族机制 + 轴缺口 20 + deferred 42 +
页未核验 16 中的链行部分）。全库 **4392/5554**。


---

# P0c-10j deferred 42 行证据补全裁定（25 采纳退役 + 17 KEEP）

- 日期：2026-09-25（同日续）
- 前置：`p0c10h-chain-deferred-compound.tsv`（42 行 con_quest/cutscene deferred）

## 判据（本轮核心成果：con_quest 语义闭环证明）

**con_quest 的接取前置由后继任务的真端主表 `finished_quest_cond` 列承载**，运行时链路：
`finished_quest_cond` →（RetailQuestMetadataCompiler）prerequisites/start-conditions →
（QuestDependencyIndex）依赖图 →（QuestProductionDispatcher）状态变化广播 →
（PlayerQuestStartEligibilityPort.prerequisitesMet）接取门槛强制。
**链行 IR 无需也无法承载**——判例 1131→1132：1132 主表 `finished_quest_cond1>Q1131` 在案。
前驱的 con_quest 列与后继的 cond 列是同一事实的两面，此前 deferred 是保守搁置而非真缺口。

**裁定**（`p0c10j-deferred-decisions.tsv`，42 行）：
- **25 ADOPT**（`CON_QUEST_METADATA_CLOSED`）：con_quest 后继主表 cond 闭环验证通过
  （35/36）+ canonical 通道可用（合成入登记表 → 指纹 → 退役）；
- **17 KEEP**：11001（`CON_QUEST_SUCCESSOR_COND_MISSING`——后继 11070 真端主表缺 cond，
  镜像 21070 有 Q21069 对照可证数据缺口）+ 10（`CANONICAL_INPUT_GAP`——work_items/符号
  通道缺失，19064 符号 `ITEM_REC_L_ME_DEVANION_MASTER_01A` 非 quest_data 命名域）+
  6 cutscene-only（`CUTSCENE_EVIDENCE_PENDING`——PlayMovie 词汇在册但 cutsceneid1→movie id
  客户端证据未建）。
- 流程教训：首次 ADOPT 集 35 行含 10 行 canonical 通道缺失行——retire 前置断言
  （`冻结指纹缺行`）拦截，降级 KEEP 后重跑。

## 证据

- canonical 批扩展：87 行合成（62 mismatch + 25 deferred ADOPT）；登记表重生成；
  ChainGate 冻结 **251 行**；25 XML 删除 + catalog 余 **1809**。
- `verify_retirement.py` = **catalog=1809 directory=1809 retired=4415 sum=6224 — OK**
  （4392 + 25 − 并发 −2；恒等式成立）。
- ChainGate 2/2 + SimpleTalk 3/3 绿。

## SimpleTalk 链行战役累计

322 普查 → 退役 **1818**（含本批 25）+ in-disk KEEP（19 五族机制 + 11001 + 10 通道缺口 +
6 cutscene 待证据 + 页未核验 16 中的链行）。全库 **4415/5554**。


---

# P0c-10k cutscene 证据补建裁定（4 采纳退役 + 2 KEEP）

- 日期：2026-09-25（同日续）
- 前置：P0c-10j 留下的 6 行 `CUTSCENE_EVIDENCE_PENDING`

## 证据链补建（本轮成果）

- **movie id** = 真端表 `cutsceneid1` 内容（1422→100、2421→132、3006→361、3020→363、
  13800→827、23800→828）——直接在真端表，无需外部登记；
- **触发动作** = 真端表 `cs1_haction`，值域为 `QuestDialogAction` 续页/接取动作 id
  （1422:1353=SELECT2_1、3006:1694=SELECT3_1、3020:1007=ASK_QUEST_ACCEPT）；
  客户端 CSV 页链交叉印证（1422: select2 页按钮 1353 → select2_1 → 推进）；
- **页编号语义澄清**：阶段页续页动作统一 SELECT2_1（编号跟随客户端 select2 页而非阶段序——
  1422 的 talk_npc1 阶段用 SELECT2_1，判例修正一次错位假设）；
- **老 XML 从未实现过场**（1422 退役前 XML 无 play-movie 元素）——真端轴、XML 缺口，
  合成是补真端语义而非等价复刻。

## 落地

- builder：`encode_after` += `play-movie` 词汇；canonical 合成 MOVIE 边（ASK_QUEST_ACCEPT
  分支 + SELECT2_1 阶段分支）；compiler `decodeAfters` += `MOVIE:<id>:<type>` →
  `AfterCommitAction.PlayMovie(id, CUTSCENE)`；
- 裁定：`p0c10j-deferred-decisions.tsv` 升级 4 行为 ADOPT（`CUTSCENE_HACTION_EVIDENCED`）；
  13800/23800 KEEP（talk_npc1 = ZoneTeleport 传送门物件，无 haction——过场由传送触发，
  触发机制未定）；
- 删 4 XML + catalog 余 **1805**；指纹 **255**；
  **verify_retirement = 1805/1805/4419 sum=6224 — OK**（恰 +4）；门禁 5/5 绿。


---

# P0c-10l SimpleTalk 族终收口 + T1 全量复跑（对账切片）

- 日期：2026-09-25（同日续）

## 族终账（零未归类）

SimpleTalk 真端表 ∩ 生产宇宙 = **2223**：
- **RETAIL_TABLE 1803**（已退役，合成定义驱动）；
- **XML_RETENTION 420**（保留 XML）：33 行链行微波机制归因（closure/gap 表）+
  387 行 M3-b 批既有 KEEP（三桶 REJECT/M3D/VARIANT 零差集口径，早有登记）；
- 未归类 **0**。

## T1 全量复跑（62 例）

本 lane 门禁全绿：ChainGate 2/2（指纹 255 行受控演进：SELECT2_1 视图路由无条件补全 →
89 行 canonical 指纹更新，diff 确认全部为 canonical 合成行后入仓）+ SimpleTalk 3/3 +
其余 10 类绿。**3F+6E 全部归并行车道**：
- `ProductionCatalogWhitelistVerificationTest`（15548/25548 NO_NODES）与
  `QuestDefinitionCatalogManifestTest`（6E）：门禁文件 M 状态（并行在改）；
- `RetailDataDrivenGateTest` 1F：门禁文件 ?? 状态（并行新增未收口）；
- `QuestClientContractGateTest` 1F（count=204 BUTTON_WITHOUT_ROUTE）：**并行会话把
  客户端合同门禁切到 overlay 视图**（退役任务首次纳入按钮审计）后暴露的 canonical 形状
  vs 客户端按钮图对齐缺口。本 lane 已修 80 处（284→204，canonical 补 SELECT2_1 视图路由，
  与 10k movie 同形状）；剩余 204 处涉及审计页链语义深水区（QUEST_SELECT 在 select2 页的
  双页导航、CHECK_USER_ITEM_OK 页链），且门禁文件本身 M 中——归 lane owner 收口。

## verify_retirement

**catalog=1803 directory=1803 retired=4421 sum=6224 — OK**（4419 + 并发 +2；恒等式成立）。
全库 **4421/5554**（79.5%）。

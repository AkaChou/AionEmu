# P0c-55 阶段腿逐阶段重建：9 个多阶段任务的页面/推进动作按真端与客户端复原（SimpleTalk 链车道 / 续片 32）

日期：2026-09-26　车道：SimpleTalk 链登记表（`quest_client_talk_chain_steps.tsv`）　编码：`P0c-55`（编号取全局最大值；唯一键 = lane + 续片号）

## 1. 缺口形状

P0c-54 按「`SETPRO<k>` 行的 owner == 真端 `talk_npc<k>`」硬轴 fail-closed 剔除了 9 个任务，登记为 `STAGE_PAGE_ADVANCE_NPC_DRIFT_PENDING`（1323/1394/1484/2480/2538/3093/4052/11010/11103）。本片先做逐阶段普查定形，结论是**一个共同的塌缩签名**：

XML 期把这 9 个任务的**阶段腿参数**压成了第一阶段的值——HEAD 版 XML 里每条推进 <transition> 都写 `action="SETPRO1"`、每个阶段都下发 `SELECT2`/`SELECT2_1`（原注释写着「对话链按客户端按钮图重建」：重建模板没按阶段参数化）：

| 轴 | 真端/客户端声明 | 登记表现状（pre） |
|---|---|---|
| 阶段 NPC | K 个 `talk_npc<k>`（K=1..3） | owner 正确（腿与页行都挂在 `talk_npc<k>` 上） |
| 推进动作 | 阶段 k = `SETPRO{k}`（客户端页 id `10000+k-1`） | **每阶段都是 `SETPRO1`**（K≥2 的腿把 var0 设回 1 ⇒ 后续阶段状态机不可达） |
| 阶段页 | 阶段 k = `SELECT{k+1}` → 按钮 → `SELECT{k+1}_1` | **每阶段都下发 `SELECT2`/`SELECT2_1`**（客户端阶段页未下发） |
| 领奖页 | `SELECT5`（按钮 `HACTION_SELECT_QUEST_REWARD`）在 `reward_npc_name` 上 | 6 缺失 / 1 塌缩（写成 `SELECT2`）/ 1 已正确 / 越界阶段行散布 |

机器可见后果（`QuestDialogOrderAudit`，本片前后）：**34 行 `CLIENT_PAGE_UNREACHED`**——8 个任务的阶段 2/3 页（`1693/1694`、`2034/2035`）与领奖页 `2375` 在编译产物 IR 里**从未被下发**（1323 的领奖页本就正确，缺页 0）。另有越界行：接取 NPC（1394@204041）与领奖 NPC（1323@203939、1394@203941）上也挂着同一组阶段页/推进动作。

## 2. 裁决口径（fail-closed 轴）

裁定表 `p0c55-stage-leg-decisions.tsv`（9 行；生成器逐条校验，任一轴不成立即中止）：

1. **真端轴**：`talk_npc1..K` 连续无缺 + 名字唯一解析 + `reward_npc_name` 唯一解析 + `collect_progress` **不存在**（本族全是赠礼/传话链，无收集段；有收集段即 fail，交回阶梯轴）；
2. **客户端轴**：任务书行数 == K+1（QE-051 行契约；行 0..K-1 逐行点名 `talk_npc<k>`、行 K 点名 `reward_npc_name`）+ 每阶段两页链 `SELECT{k+1} → 按钮 → SELECT{k+1}_1(带 HACTION_SETPRO{k})` 成立 + 领奖页 `SELECT5` 唯一按钮常量 == `HACTION_SELECT_QUEST_REWARD` + 领奖页与阶段页不同名；
3. **登记表轴（塌缩签名）**：腿按 **owner 锚定**逐跳相接——阶段 k 的腿 = `owner == talk_npc<k>` 且 `src == 前一跳 dst` 的 `SETPRO` 行（首跳 `src == started`），且末跳 dst 节点的 packed 值 == K。`talk1 == talk3`（2538）由链位消歧；
4. **页行分派**：owner 在该腿源状态上的页行按**动作类**分派——页名动作行（`SELECT\d+(_\d+)?`）= 续页行（动作+页 → `SELECT{k+1}_1`），其余（`QUEST_SELECT`/`USE_OBJECT`）= 入口页行（页 → `SELECT{k+1}`）。1394 的 `actions="USE_OBJECT QUEST_SELECT"` 双入口行合法保留（同一页的两条交互路线）；
5. **领奖行三形互斥**：①已正确（1323）②塌缩（领奖 NPC 的 `QUEST_SELECT` 行下发阶段页 ⇒ **改写其页**，1394）③缺（在 packed K 的 `REWARD` 节点上**插入**一条，其余 7 个）；
6. **越界剪除**：阶段页被「非 (talk_npc<k>, 腿源状态)」的行下发、或 `SETPRO` 行既不在腿链上也不是领奖窗行（`SHOW_SELECT_QUEST_REWARD_WINDOW*`）⇒ 剪除；**接取页行（`SELECT1*`）一律不动**（接取侧扩散属 `XML_ONLY_ACCEPT_ROLE_SPREAD_PENDING`，与编译器 `acceptFlow` 交互，另轴）；
7. **收尾两守卫**：①逐阶段页的 owner 集必须 == `{talk_npc<k>}`（领奖页 == `{reward NPC}`）；②越界行被剪后该任务的阶段/领奖页仍逐页被下发（无页丢失）。

## 3. 五源取证（以 1484 为例，其余同形）

| 源 | 证据 |
|---|---|
| 真端 `Quest_SimpleTalk.xml` | `acquired=Shugo_LF2_14` / `talk_npc1=Anasya` / `talk_npc2=Telamone` / `talk_npc3=Sandinas` / `give_item1..3` / `reward_npc_name=Shugo_LF2_13`；**无** `collect_progress`/`item_check` |
| 客户端 HTML（`QUEST_Q1484.html`） | `select2`→`HACTION_SELECT2_1`→`select2_1`→**`HACTION_SETPRO1`**；`select3`…`SETPRO2`；`select4`…`SETPRO3`；`select5`→`HACTION_SELECT_QUEST_REWARD`（按钮文案「拿出东西。」）|
| 客户端任务书 `quest_summary` | 4 行 = `Anasya` / `Telamone` / `Sandinas` / `Shugo_LF2_13` ⇒ 行 k ↔ 阶段 k、行 K = 领奖行（与节点 packed 值一致：`stage1/1`、`stage2/2`、`reward/3`）|
| 客户端页索引 CSV | 页号共号：`select3`=1693、其按钮=1694=`select3_1` 页号、`select3_1` 按钮=10001=`HACTION_SETPRO2`；`select5`=2375/按钮 1009 |
| 登记表 + IR | 腿 owner = 204045/204048/204011（= talk1/2/3）+ `GIVE_ITEM:182201403/182201404/182201405`（= `give_item1..3`）；但页/动作全是阶段 1 的值，且 798126 上只有无按钮反馈页 `DEFAULT_SUCCESS` ⇒ `2375` 未达 |

`talk_npc` 与任务书行的**逐行**对应是本片的判据基石（1323 行 0=Lodas/行 1=Justachys、11010 行 2=物件 `Supply_Box`、2538 行 2=Verdandi 回访 = `talk_npc3 == talk_npc1` 全部逐字命中）。

## 4. 落地

生成器新增**阶段腿重建通道** `rebuild_stage_legs`（`build_quest_client_talk_chain_steps.py`，与阶梯/接取入口改写同址、最后执行）：

* 装载期：新增 `p0c55-stage-leg-decisions.tsv` 装载（与**阶梯表**同任务即 fail；与**接取入口表**允许共存——1323 同时受两表裁定，两域页/动作不相交，注释留痕）；
* 改写面（本片 9 任务）：动作名 `SETPRO{k}` 9 处、入口页 `SELECT{k+1}` 14 处、续页 8 处（动作+页）、领奖行 1 改写 + 7 插入；剪除 7 条越界行（1323:3 / 1394:4）；
* 效果（爆炸半径）：`CHANGED [1323,1394,1484,2480,2538,3093,4052,11010,11103] / ADDED [] / REMOVED []`，登记表 5076→ 同 5052 行（**行数不变**：9 任务共 +8 插 −7 剪 —— 与净行数一致），`5bf47fded704aaa73b838dc257f3687a`（419803 字节）；
* 保真：生成器重跑与登记表**逐字节相同**，23 张裁定表零漂移 ⇒ 修复可由生成器复现（生成物纪律）。

**IR 逐行对拍**（9 任务）：`-49/+49` 行，全部落在三类靶面——阶段 2/3 页路由（`dialogId 1352/1353 → 1693/1694/2034/2035`）、阶段 2/3 腿（`10000 → 10001/10002`，阶段 1 保持 `10000`）、领奖页（`dialogId=31 → 2375`）；物品轴（`GiveItem/RemoveItem`）与领奖窗行逐字保留。

**审计前后对比**：9 任务未达/待证 **34 → 0**；全表未达/待证 **1348 → 1314**（净 −34，`ONLY_POST = 0` ⇒ 零新增未达）；9 任务审计总行数 11/20/37/23/30/36/32/36/26 → 10/14/34/22/27/33/29/33/25（剪掉的越界行同时消失）。

**冻结指纹**：外科重冻 9 值（其余 276 行逐字节不变），新增/删除空 ⇒ 改动面 == 缺陷面；`c9657153…` → **`aaf2b9c0efdd0879f139dfab3fe4a31f`**（主/副本双写）。

## 5. 门禁

| 门 | 命令 | 读数 |
|---|---|---|
| 净树（本片 3 类） | `-Dtest='RetailSimpleTalkChainGateTest,QuestDialogOrderAuditTest,QuestClientContractGateTest'` | **20/20 绿** |
| 保真 | `python3 -B p0c42_builder_fidelity_check.py` | `FIDELITY_OK` 419803 字节 + **23 张**裁定表零漂移 |
| 普查复算 | `python3 -B p0c55_stage_leg_census.py` | pre `{OK 6, REPORT_OK 1, 缺陷 24}` → post **`{OK 22, REPORT_OK 9}`** |
| T1 | `run_quest_gates.sh T1`（`gates/T1-220318.log`） | **75 例 1F**：`RetailDataDrivenGateTest.driftVersusShellsIsRegistered`（**在册 lane `20035`**，与本片零交集；本片 9 个 id 零命中） |
| T2 | `run_quest_gates.sh T2 1323 1394 1484 2480 2538 3093 4052 11010 11103`（`gates/T2-221103.log`） | **93 例 4F**，四条全为**既存**（见 §6）|
| T3 | `QUEST_FORK_COUNT=2 run_quest_gates.sh T3`（`gates/T3-221828.log`，22:18:28→22:24:37） | **2013 例 / 117F / 22E / 1S**，失败身份集对装入前两基线**逐条相同**（ADDED 0 / REMOVED 0）；见 §6 |

## 6. T3 归因

T3（`-Dtest=com.aionemu.gameserver.questEngine.**`，`forkCount=2`）一次收口跑（`gates/T3-221828.log`，22:18:28→22:24:37，总耗时 06:08），基线取**装入前**最近两次全树读数 `gates/T3-212101.log`（21:28）与 `gates/T3-213222.log`（21:38）——两次逐项同值（**2013 例 / 117F / 22E / 1S**），故身份集对拍可用。

**读数与身份集差值（机械）**：T3 后态 `2013 例 / 117F / 22E / 1S`（BUILD FAILURE 由既存失败引起，属 T3 的定义内态——T3 只作「零新增失败」证据），与两条基线的失败**身份集完全相同**：三份日志各 139 条失败行 / **107 个唯一身份**（`类.方法`），`only_in_base = []`、`only_in_post = []`（对两条基线各算一次）⇒ **零新增身份、零消失身份**。身份提取已核完整（`[ERROR]   ` 前缀行 139 条全部被正则命中，无未匹配行）。

* T2 的四条失败 T3 基线里**全部既存**（按测试方法名对拍）：`RetailDataDrivenGateTest.driftVersusShellsIsRegistered`（20035 漂移登记，lane）、`QuestStartItemDefinitionRegressionTest.startItemNpcsExposeTheDialogRoutesUsedAfterReading`（1197 NPC 700004）、`JournalRewardRowRepairContractTest.persistedRewardRowsAreRepairedOnEnterWorld`（15613）、`LegacyTemplateMirrorRouteRegressionTest.legacyTemplateCloseControlsDoNotChangeQuestState`（1131）——四条点名的任务 id 均**不在**本片改动面（`CHANGED` 只有 9 个 id）。
* 本片 9 个 id 在 T2/T3 的失败条目中**零命中**；本片只有两个文件被改（登记表 + 冻结指纹），且冻结指纹的变化行恰为这 9 行（§4）。
* **并发 lane 的重要说明（诚实口径）**：同窗口内 DataDriven 车道也在改树（其自身 T3 为 22:17 起跑的 `gates/T3-221706.log`，2015 例 —— 比本片基线多 2 例 = 其新增测试类；该日志**不属本片**，勿引用）。因此本片 T3 覆盖的是两车道合并后的树；「零新增身份」既覆盖本片改动，也覆盖该窗口内车道改动，不能单独归因给本片。**本片自因面的锐化证据**是：改动面只有 2 个文件、`CHANGED` 恰 9 个 id、`only_in_*` 为空、本片 9 个 id 在全部失败条目零命中（§4 + 本节第 1/2 条）。
* T1/T2 复核（同一身份提取口径）：T1 后态 `75 例 1F` = 唯一的 `RetailDataDrivenGateTest.driftVersusShellsIsRegistered`（lane `20035`，行号随车道改测试文件从 `:423` 移到 `:430`，消息逐字相同）；T2 后态 `93 例 4F` 的四条身份**全部**在装入前 T3 基线身份集内（即上述四条）⇒ **两条门禁均零新增**。

## 7. 残留与登记

| 项 | 类型 | 处理 |
|---|---|---|
| `XML_ONLY_ACCEPT_ROLE_SPREAD_PENDING`（34 项 / 25 任务） | 另轴（接取侧） | **未动**：`SELECT1/SELECT1_1` 被铺到非接取 NPC（2538/3093/11010/11103 各含），与编译器 `acceptFlow` 的页下发交互，判据须另立；本片剪除面**只含阶段/领奖页**，接取行零改动 |
| `ACCEPT_ENTRY_PAGE_WRONG_PENDING`（7 条） | 接取入口轴 | 未动（P0c-42 域） |
| `GATE_VS_ITEM_CHECK_PENDING`（21033/21455） | 阶段轴邻域 | 未动：其 `SETPRO` 编号/节点与「无 s1、`SETPRO2` 落在 `started`」另形，不属本片塌缩签名（本片 9 任务的腿链末跳 dst packed == K 逐条成立） |
| 35017/45010/45017/45024/45026、19004 审计残留 | 既存 | 本片前后逐字相同（`AUDIT_UNREACHED_LANE_PREEXISTING`） |
| 9 任务运行时/客户端目检 | **PENDING（需授权启服）** | 静态判据已闭环（普查全绿 + 审计 34→0 + IR 逐行 + 指纹 9 值）；未执行命令见 §8 |

## 8. 复现命令

```
python3 -B .agents/summary/scriptdll-quest-driver/p0c55_stage_leg_census.py           # 普查（pre 缺陷 24 → post 0）
python3 -B .agents/summary/scriptdll-quest-driver/p0c43_dryrun_diff.py \
    --expect 1323,1394,1484,2480,2538,3093,4052,11010,11103                        # 干跑：变化面 + 23 表零漂移
python3 -B .agents/summary/scriptdll-quest-driver/p0c42_builder_fidelity_check.py     # 保真：逐字节相同
mvn -o -B surefire:test -Dtest=RetailSimpleTalkChainGateTest \
    -Dretail.talkChain.fingerprintOut=<dump.tsv>                                       # 指纹 dump（重冻用）
python3 -B .agents/summary/scriptdll-quest-driver/p0c55_refreeze_fingerprints.py <dump.tsv> [--apply]
# 审计/IR 探针（源码归档 .agents/summary/scriptdll-quest-driver/P0c55*ProbeTest.java.txt，用后即删）：
mvn -o -B test -Dtest=P0c55StageLegProbeTest,P0c55AuditProbeTest \
    -Dp0c55.questIds=1323,1394,1484,2480,2538,3093,4052,11010,11103 \
    -Dp0c55.irOut=<ir.tsv> -Dp0c55.auditOut=<audit.txt>          # 装入后用 surefire:test + 手工同步 target/classes
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T1|T2 <ids...>; QUEST_FORK_COUNT=2 run_quest_gates.sh T3
```

未执行（未授权）：服务器启停与客户端实机目检（AGENTS.md 规则 1：保持 PENDING 并记录未执行的命令）。

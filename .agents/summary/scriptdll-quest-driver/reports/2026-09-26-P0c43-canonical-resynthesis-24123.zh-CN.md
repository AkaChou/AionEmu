# 2026-09-26 续片 26 / P0c-43：报告轴改道 canonical —— 24123 弃 XML 逐字转写（第三种形状裁定通道）

## 1. 结论

台账登记的 `XML_NPC_AXIS`（"遗留 XML 转写在结构轴上与真端分歧"）以判例 **24123** 收口，并给登记表
引入**第三种形状裁定通道：改道（reroute）**——前两种是"阶梯改写"（P0c-36/37）与"接取入口重绑"
（P0c-42），都保留转写骨架只改局部；改道是**整块弃转写、按真端重新合成**。

24123 的两轴分歧（真端 + 客户端双证）：

| 轴 | 遗留 XML 转写形（改道前） | 真端声明 |
|---|---|---|
| **阶段梯** | 零阶段路由：`N reward REWARD 1`，交付侧路由全在 `started`（var0 布局 `P 0 1 0 1` 只容 0/1） | `talk_npc1=DF2_NPC_Ananta`(204387) + `talk_npc2=DF2_LycanPrisoner_Q24123A`(802442) ⇒ **K=2** |
| **报告 NPC** | 报告/完成流（`SELECT_QUEST_REWARD` / `CHECK_USER_HAS_QUEST_ITEM[_SIMPLE]` / `SET_SUCCEED` / `npc-complete`）**全绑接取 NPC 204345=Rikesh** | `reward_npc_name=DF2_NPC_Ananta`(204387) |

客户端 `QUEST_Q24123.html` 的页序正是 K=2 阶梯（`select2`(1352)→`select2_1`(1353, SETPRO1)→
`select3`(1693, SETPRO2)→`select5`(2375, SELECT_QUEST_REWARD)）+ 任务书 3 行（= K+1，QE-051）。
**遗留 XML 完全没有中间两页的转写**，审计因此把 1352/1353/1693 判为 `CLIENT_PAGE_UNREACHED`
（P0c-37 已分区为 `GENUINE_GAP`）⇒ 玩家在 Ananta/LycanPrisoner 处永远看不到对话页、阶段推不动。

**修复效果（修复面 = 缺陷面）**：登记表 24123 块 22 行 → 19 行（`P 0 1 0 1` → `P 0 6 0 63`；
`N reward REWARD 1` → `N s1(1)/s2(2)/reward(2)`；`B NPC_START 204345` + `B NPC_REPORT 204387` +
`B NPC_COMPLETE 204387`；16 条转写 R 行 → 9 条 canonical R 行）；审计 24123 **10 行（3 未达）→ 9 行
（全 `PAGE_ACTION_MATCHED`/`TERMINAL_PAGE_REACHED`）**；链式冻结指纹 `changed={24123}` /
`added=0` / `removed=0`；契约门 fatal 保持 **0**；净树复跑 20/20 全绿。

**第二个交付物仍是耐久性**：改道落进生成器（新裁定表 `p0c43-canonical-resynthesis.tsv` + 加载器
六轴 fail-closed + 转写阶段对该任务 skip），落地判据 = **生成器重跑输出与登记表逐字节相同**
（md5 `322f4d1a99192de960ec52d6c18c16f1`，430735 字节，8 张既有裁定表零漂移）。

**第三个交付物是把登记表的残留面量出来**：改道后登记表内 117 行真端多阶段行里，
**阶梯缺失轴归零**（0 行），**真端报告 NPC 未接线只剩 1 行**（2482），另有 2 行信息性
（35010/35011）。24123 不在残留表。

## 2. 缺口形状（判例 24123）

### 2.1 真端轴（形状权威）

`Quest_SimpleTalk.xml` 24123：`acquired_npc_name=Rikesh`、`talk_npc1=DF2_NPC_Ananta`、
`talk_npc2=DF2_LycanPrisoner_Q24123A`、`reward_npc_name=DF2_NPC_Ananta`；
`npc_name_index.tsv` 唯一解析 Rikesh→204345、DF2_NPC_Ananta→204387、DF2_LycanPrisoner_Q24123A→802442。
即 **接取人 ≠ 报告人**，且报告人同时是第 1 段说话人。

### 2.2 客户端轴（强二级证据）

页序与按钮（`quest-dialog-pages.csv` / `quest-dialog-action-details.csv`）：

| 序 | 页 | 按钮（动作常量） | 改道前审计 |
|---|---|---|---|
| 1 | `select1`(1011) | 20000 `HACTION_QUEST_ACCEPT_SIMPLE`「接受。」/ 20001 `HACTION_QUEST_REFUSE_SIMPLE`「拒绝。」 | 两按钮 `PAGE_ACTION_MATCHED`（靠 `B NPC_START` 的 `startPages=SELECT1` 下发入口页） |
| 2 | `select2`(1352) | 1353（续页） | **`CLIENT_PAGE_UNREACHED`** |
| 3 | `select2_1`(1353) | 10000 `SETPRO1` | **`CLIENT_PAGE_UNREACHED`** |
| 4 | `select3`(1693) | 10001 `SETPRO2` | **`CLIENT_PAGE_UNREACHED`** |
| 5 | `select5`(2375) | 1009 `SELECT_QUEST_REWARD` | `PAGE_ACTION_MATCHED`（但绑在接取 NPC、`started` 态） |
| 6 | 领奖窗(5) | — | `TERMINAL_PAGE_REACHED` ×4 |

任务书 3 行（`quest_client_summary_rows.tsv` = 3）⇒ 末行行号 2 = reward 行 var0（QE-051）。

### 2.3 转写形为何不成立（本片的核心判定）

遗留 XML 是**把多行任务书压成单个 `started(var0=0)`** 的手工转写惯性产物：既没有中间页的转写
（结构缺失，不是页面名错），又把报告流铺在接取 NPC 身上。P0c-19 判例（"全形状铺满一个 NPC =
手工转写漂移，按真端对、XML 错裁"）在这里第二次现形。两条既有通道都修不了它：

- `apply_talk_ladder`（P0c-36 阶梯改写）：只搬 `int(r[3]) == reward_id` 的行、只插阶段节点，
  **不重绑报告 NPC、不动 `P` 布局**；
- `rebind_accept_entrance`（P0c-42 接取入口重绑）：只改接取段（入口页 + 接取窗 + 结果页），
  本行接取段本来是对的。

⇒ 需要**整块按真端重新合成**（`synthesize_canonical` 已具备该形状：K 段 `SETPRO` 阶梯、宽布局、
报告流绑 `reward_npc_name`），本片把"何时允许改道"做成裁定表 + 六轴 fail-closed。

## 3. 改道通道（生成器侧落地）

### 3.1 裁定表 `p0c43-canonical-resynthesis.tsv`

列：`quest_id / code / legacy_report_npc / retail_reward_npc / basis`；当前 code 域只有
`LEGACY_REPORT_FLOW_ON_ACQUIRED`。表头逐轴写明判据与留证（真端轴 + 客户端页序 + 任务书行数）。

### 3.2 六轴 fail-closed（加载器 `canonical_resynthesis`）

| 轴 | 断言 | 失败语义 |
|---|---|---|
| ① code 域 | `code` 必须在登记域内 | 表腐化 |
| ② 漂移形确认 | `legacy_report_npc` 解析唯一且 **== 真端 acquired 的 id** | 不是"报告流全绑接取 NPC"这一形，人工重核 |
| ③ 真端对照 | `reward_npc_name` 唯一解析、**≠ acquired id**、且 == 裁定表值 | 无需改道 / 表与真端不一致 |
| ④ 阶段归属 | 每个 `talk_npcK` 的 id **≠ acquired id** | 阶梯自环（阶段没换人） |
| ⑤ 投影一致 | 客户端任务书行数 **== K+1** | QE-051 投影判据不符 |
| ⑥ 页证齐备 | 客户端页册含 `SELECT{i+2}`（i=0..K-1），且 canonical 的页链走边可定位 | 页证不足 |
| 互斥 | 不得同时出现在阶梯表 / 接取入口表 | 两套裁量冲突 |

加载器另按与既有 canonical 批同口径补齐 `canonical_work_items`，并把该行并入 `canonical_pending`；
转写循环对该行 **skip**（`CANONICAL-OVERRIDE`），避免双份块。

## 4. 落地物

| 文件 | 说明 |
|---|---|
| `src/main/resources/…/quest_client_talk_chain_steps.tsv`（+`target/classes` 副本） | 24123 块 22→19 行；md5 `322f4d1a99192de960ec52d6c18c16f1`（5113→5110 行） |
| `.agents/…/build_quest_client_talk_chain_steps.py` | 新增 `canonical_resynthesis` 加载器（六轴 fail-closed + 互斥闸 + work_items 补齐）+ 转写 skip |
| `.agents/…/p0c43-canonical-resynthesis.tsv` | 改道裁定表（生成器输入，含逐轴判据说明与留证） |
| `.agents/…/p0c43_dryrun_diff.py` | 干跑比对（重定向 `OUT` → 逐任务块对拍，`--expect` 断言变化面；含 8 张裁定表零漂移断言） |
| `.agents/…/p0c43_blast_radius.py` | 爆炸半径（`p0c43-registry-pre.tsv` vs 已安装登记表，逐任务块对拍；落盘 24123 前后块） |
| `.agents/…/p0c43-registry-pre.tsv` | 改道前登记表快照（md5 `badc6ee2…`，便于独立复现爆炸半径） |
| `.agents/…/p0c43-refreeze_fingerprints.py` | 指纹外科重冻（`EXPECTED_CHANGED=("24123",)` + PRE 值守卫 + whipsaw 守卫） |
| `.agents/…/p0c43_residue_census.py` / `p0c43-residue-census.tsv` | 残留普查（投影式判据，见 §6） |
| `.agents/…/p0c43-24123-audit-{pre,post}.tsv` | 24123 审计逐行前后对照（10 行 → 9 行） |
| `.agents/…/p0c43-24123-ir-post.tsv` | 24123 编译后 IR（38 条转换，逐条留证） |
| `.agents/…/p0c43-24123-registry-{pre,post}.tsv` | 24123 登记表块前后对照（22 行 / 19 行） |
| `.agents/…/p0c43-blast-radius.txt` / `p0c43-generator-fidelity.txt` | 爆炸半径与生成器忠实性判据输出 |
| `src/test/resources/quest/retail-simple-talk-chain-ir-fingerprints.tsv`（+target 副本） | 外科重冻 24123 一行（288 行布局逐字节保留；双副本 md5 `3686124be2b45dda60c0fdb07f437258`） |
| `.agents/…/P0c43CanonicalResynthesisProbeTest.java.txt` | 探针源码归档（树内 `.java` 与 `.class` 已删除） |

## 5. 验证

| 门 | 结果 |
|---|---|
| 生成器忠实性 | `FIDELITY_OK: 生成器重跑输出与登记表逐字节相同（430735 字节）`、`REGISTRY_MD5 322f4d1a99192de960ec52d6c18c16f1`、`DECISION_TABLES_STABLE: 8 张裁定表零漂移` |
| 爆炸半径 | 干跑 `CHANGED [24123] / ADDED [] / REMOVED []`；独立复现 `TOTAL_BLOCKS 295 → 295`、`BLAST_OK` |
| 审计（24123） | 10 行（3 × `CLIENT_PAGE_UNREACHED`）→ **9 行全达**（6 × `PAGE_ACTION_MATCHED` + 3 × `TERMINAL_PAGE_REACHED`），转换数 35 → 38 |
| **接取按钮覆盖（本片主要未定风险）** | **已实证关闭**：页 1011 的 20000/20001 两按钮在两态下都 `PAGE_ACTION_MATCHED`；编译 IR 另含 1002（接受→`started` + 接取窗 1003）/1003/1004/1007/1008 路由 ⇒ 编译器 `acceptFlow` 已覆盖 SIMPLE 按钮族，无需 P0c-42 式重绑 |
| 链门（校验轮，无 `-D`） | `RetailSimpleTalkChainGateTest` 2/2 绿；指纹 `changed={24123}`（`ecd63f6f…` → `fd6b6f49…`） |
| 契约门 | `QuestClientContractGateTest` 1/1 绿，fatal 0（空基线） |
| 审计全表 | `QuestDialogOrderAuditTest` 17/17 绿 |
| 净树复跑 | 链门 2 + 审计 17 + 契约门 1 = **20/20**（探针删除后） |
| T1（18 类 63 例） | **3F**（`gates/T1-162409.log`）：DataDriven 指纹 5 条 + DataDriven 20035 漂移登记 + CollectItem 冻结 155/退役 175 —— 与 `T1-143553` 基线**逐字相同**（连消息文本都同），且契约门已从 4F 里消失 ⇒ 零新增 |
| T2（24123，63 例） | **3F**（`gates/T2-163015.log`）：与 T1 **逐条相同**（含消息前缀）⇒ 24123 自身零新增 |
| T3（全树） | 见 §5.1 |

### 5.1 T3 与归因

首次 T3（`gates/T3-163623.log`）在 **14 秒**即 BUILD FAILURE，原因是**并发 lane 的在飞探针**
`P0c52GuardGroupProbeTest.java`（16:36:43 写入；其 `QuestMetadata.nameId()` / `keySet()` 调用与当时主树
不符）——测试树整体编译失败，T3 无法起跑。按约束**不触碰并发 lane 文件**，等其自洽后复跑：

| 轮次 | 日志 | 结果 |
|---|---|---|
| 复跑（本片） | `gates/T3-163754.log` | **2006 例 / 121F+21E+1S**（7:16 min） |
| 基线（本片改动安装前） | `gates/T3-161145.log` | 2006 例 / 121F+21E+1S |

**失败身份集对拍（110 条身份，双向 `comm` 差集）**：`only-now = ∅`、`only-pre = ∅` ⇒ **零新增、零消失**，
无一条失败可归因本片。窗口内并发 lane 的 `P0c52GuardGroupProbeTest` 已可编译且**绿**（其基线时不存在，
故计数 ±0 里含其 +1 例），`RetailTalkChainGateProbeTest` 两轮日志各 5 处（既有失败身份，非本片引入）。
并发 lane 在本片 T3 窗口内仍有活跃写入（其探针 mtime 16:46:07），故本片不把 T3 当作"全树绝对干净"的
证明，只当作**身份级零新增**的证据。

## 6. 残留与下一步（`p0c43-residue-census.tsv`）

普查口径（**投影式，不看节点标签**）：登记表内真端多阶段行 117 行；判据 ①真端 `reward_npc_name`
解析 id 是否在块内接线（`REWARD_NPC_UNWIRED`）②客户端任务书行数 == K+1 且 K>1 而 START/REWARD
节点 var0 投影不足 K（`LADDER_MISSING`）。

| 轴 | 行数 | 明细 |
|---|---|---|
| `LADDER_MISSING` | **0** | 改道后登记表内**无**客户端背书的阶梯缺失；24123 是最后一个 |
| `REWARD_NPC_UNWIRED` | **1** | **2482 Moreinen**：真端 `acquired=Hakon` / `talk_npc1=Sereniti` / `talk_npc2=Neusa` / `reward=Moreinen`(204211)，而块内 NPC 集为 {204210, 204333, 278018, 278020}（204211 完全不接线；交付页 SELECT5 与 `s2→reward` 在 278020 上，阶段当主 3 人 vs 真端 K=2）⇒ 同签名候选，**须按 24123 的取证口径逐行裁定**（客户端页序 + 任务书行数 + 审计），不得批量套用 |
| `REPORT_SIDE_MIXED`（信息性） | 2 | 35010/35011：交付动作同时由 799805 与 799806（=35018 的 reward）持有——同族共享交付 NPC，未见客户端矛盾，仅留证 |

**普查判据的三次过度标记（教训留档）**：本片首版按节点标签 `s\d+` 判"缺阶梯"⇒ 117 行里 88 行是
完整阶梯（1394 用 `v1/v2`、1537 用 `k1..k3`、其他族 `steps1`/`equipped96`/`reward30`/`instance95`）；
第二版把 `QUEST_SELECT` 当报告侧动作 ⇒ 把**每个链上 NPC 的通用对话事件**算进报告绑定，产生
215 行噪声；第三版未计 QE-051（"reward 与末段 sK 同 var0"）⇒ 末段 var0 落在 REWARD 节点的行
（如 4052 的 `stage1(1)/stage2(2)/reward(3)`）被误判。**结论：跨族普查必须投影式（var0 / 节点接线），
且必须先在已知判例上校准（本判据在 24123 改道前块上会触发 `REWARD_NPC_UNWIRED`、改道后不触发）。**

其他承轴（承接前片，未变）：`p0c42-blocked-rows.tsv` 的 `SELECT6_CLOSE_EXIT_OTHER_FAMILIES`（62 行）、
44 行 `XML_RETENTION` 的 1011 未达（QE-066 双侧对拍后再采纳）、25070 的 DataDriven 族形资质、
`RUNTIME_PENDING` 目检清单。

## 7. PENDING（需授权）

- **运行时/客户端目检**（24123）：接取 NPC Rikesh(204345) 处接取 → Ananta(204387) 两页对话
  （select2 续页「继续听。」→ select2_1「结束对话。」）→ LycanPrisoner(802442) select3 →
  回 Ananta 报告 select5 → 领奖窗 → 完成。按约束**未启服**，本轮未执行任何服务器生命周期操作。
- 前片遗留目检清单不变（1323 物件、21460/29070/29071、2458/4905/4906、P0c-39/40/41/42 各行）。

## 8. 命令索引

```
python3 -B .agents/summary/scriptdll-quest-driver/p0c43_dryrun_diff.py --expect 24123     # 干跑（安装前）
python3 -B .agents/summary/scriptdll-quest-driver/p0c43_blast_radius.py                    # 爆炸半径（安装后，独立复现）
python3 -B .agents/summary/scriptdll-quest-driver/p0c42_builder_fidelity_check.py /tmp/p0c43-fidelity.tsv
python3 -B .agents/summary/scriptdll-quest-driver/p0c43_residue_census.py                  # 残留普查
mvn -o -B test -Dtest=RetailSimpleTalkChainGateTest -Dretail.talkChain.fingerprintOut=/tmp/p0c43-fp.tsv
python3 -B .agents/summary/scriptdll-quest-driver/p0c43_refreeze_fingerprints.py /tmp/p0c43-fp.tsv --apply
mvn -o -B test -Dtest='RetailSimpleTalkChainGateTest,QuestClientContractGateTest,QuestDialogOrderAuditTest'
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T1
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T2 24123
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T3
```

## 9. 并发车道注记

本片期间另一车道在 **P0c-52**（奖励组/`QuestMetadata` 轴）工作，其临时探针
`P0c52GuardGroupProbeTest.java` 自 16:36:43 起令测试树无法编译（T3 首跑 14 秒失败），
另有其 `RetailTalkChainGateProbeTest` 在 T3 窗口内报 1F。**本片未触碰该车道的任何文件**；
共享面（登记表 / 指纹 / 审计）与 P0c-52 无交集，T1/T2 的失败身份与基线逐字相同。

**⚠️ 编号撞车（后续会话注意）**：该车道在更早的续片 17 已把自己的感官区注册批记作 **P0c-43**
（其脚本 `p0c43_retire_sensory_area_rows.py`，mtime 11:59:59，**不是本片产物**）；本片的 P0c-43 =
报告轴改道（判例 24123）。本片全部产物用 `p0c43-24123-*` / `p0c43-canonical-resynthesis.tsv` /
`p0c43-residue-census.tsv` / `p0c43-blast-radius.txt` / `p0c43-generator-fidelity.txt` /
`p0c43-registry-pre.tsv` / `p0c43_blast_radius.py` / `p0c43_dryrun_diff.py` /
`p0c43_refreeze_fingerprints.py` / `p0c43_residue_census.py` 十个名字，与该车道文件无重叠。

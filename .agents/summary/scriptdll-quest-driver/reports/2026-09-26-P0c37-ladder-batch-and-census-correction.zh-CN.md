# P0c-37：阶梯余量分批 —— 可达性分区纠正普查口径 + 1479 入阶梯


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 切片窗口：2026-09-26（承接 P0c-36 登记的"余量 39 行缺阶梯"）。
> **本片最重要的产出是口径纠正**：把 P0c-36 的"节点投影轴"普查换成**客户端页可达性对拍**后，
> 39 行里只有 **19 行是真缺口**，另 22 行是普查误报（阶段语义已由 transition 级 var0 旗标承载）。
> 落地面：**1479**（真缺口 + 已驱动行）完成阶梯，其 4 个中间对话页由 `CLIENT_PAGE_UNREACHED`
> 转 `PAGE_ACTION_MATCHED`；**24123** 在裁定过程中暴露出新的 NPC 轴，**本轮不带入**并登记。

## 1. 结论摘要

- **普查口径纠正（本片主产出）**：`p0c36_ladder_gap_census.py` 的判定轴是"任务书上每行都要有对应的
  START/REWARD 节点投影"（节点投影轴），它看不见**把阶段编码在 transition 级**的行——这类行用
  单 `started` 节点 + var0 旗标 + 条件化路由表达阶段，例如 35010：
  `SETPRO1 cond VAR_IS:var0=0 act SET_VAR:var0=1`（推进）、`SELECT_QUEST_REWARD cond VAR_IS:var0=1`（交付）。
  这类行的中间页在编译后 IR 里**本来就可达**。用 `QuestDialogOrderAudit` 对拍后：
  **真缺口 19 行 / 普查误报 22 行**（两侧合计 41 = 普查全集，含已裁定的 24202/80320）。
- **1479 入阶梯**：K=2、`SETPRO`、talk=[203912, 203898]、client 3 行、cp=0（掉落门不限行）、
  reward 投影 2（P0c-34 已裁定，本片复用）。阶梯后节点 `started(0)/s1(1)/s2(2)/reward(2)`，
  交付段（`B NPC_REPORT`）迁到 `s2`；页面 1352/1353/1693/1694 全部转 `PAGE_ACTION_MATCHED`。
- **生成器泛化（对 P0c-36 两行可证无损）**：①建档第二份阶梯裁定表（同任务跨表即 fail）；
  ②掉落轴放宽为 `collect_progress ∈ {0,K}`（源码级证据：`isQuestDrop` 只在 `collectingStep != 0`
  时校验 var0，故 `0` 表示不限行，阶梯不影响掉落）；③`started` var0 兼容无约束值 `-`（归一为 0）；
  ④交付段迁移由"动作白名单"改为"source=`started` 且 npc=交付 NPC 的 R 记录全迁"（白名单会漏迁
  24123 那种 `CHECK_USER_HAS_QUEST_ITEM` 门路由）。**24202/80320 的登记行逐字节不变**（见 §3）。
- **24123 新轴（不带入）**：XML 转写的 NPC 是 **204345 = Rikesh**，而真端 `reward_npc_name =
  DF2_NPC_Ananta` 经 `npc_name_index` 解析为 **204387**；同行的 reward 投影也是 1（客户端末行为 2）。
  两条轴都需要逐行裁定（NPC 归属需客户端信件页/NPC 对话树佐证），按"宁可保留、不虚报"登记，
  已撤回本轮临时加的 reward 覆盖。

## 2. 落地物

| 产物 | 路径 | 说明 |
|---|---|---|
| 可达性分区（探针实测） | `p0c37-gap-partition.tsv` + `p0c37-gap-partition.txt` | 41 行：`GENUINE_GAP` 19 / `CENSUS_OVERFLAG` 22，逐行列出不可达页 |
| 机器裁定（可重放） | `p0c37_ladder_batch_derive.py` → `p0c37-talk-ladder-decisions.tsv` | 三轴机器推导 + 页链 mode 推导；**自校验**：24202→SETPRO/K=2、80320→TALK/K=1 必须复现；读分区表过滤误报行；幂等（既有裁定不清空） |
| 阻塞登记 | `p0c37-blocked-rows.tsv` | `XML_NPC_AXIS`（24123）/ `ADOPTION_BLOCKED`（17 行，登记表无记录）/ `CENSUS_OVERFLAG`（20 行）逐条带证据 |
| 阶梯裁定 | `.agents/summary/scriptdll-quest-driver/p0c37-talk-ladder-decisions.tsv` | 1 行（1479，含 K/推进方式/页链的完整 basis） |
| 登记表 | `src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv`（+ target 同 md5 `cae1950622d7db287d56543abd5f63da`） | 1479 = 6 条阶段 R + B 块迁 `s2` + 节点 `s1/s2` |
| 指纹 | `src/test/resources/quest/retail-simple-talk-chain-ir-fingerprints.tsv`（+ target 同 md5 `173a67f49ae05dfd73861f26477d21f1`，274 → **275** 行） | **只新增 1479 一行**；dumped 全集对拍：added=[1479]、changed=[]、removed=[] |
| 生成器 | `build_quest_client_talk_chain_steps.py` | 四处泛化（见 §1），并保持 fail-closed |
| 探针（源码归档，树内已删） | `P0c37LadderProbeTest.java.txt` + `p0c37-ladder-probe-before/after.txt`；`P0c37GapPartitionProbeTest.java.txt` + `p0c37-gap-partition.txt` | 改动前后对拍（审计逐页判定 + 编译后节点/路由/掉落） |

## 3. 关键证据（可复核）

**① 生成器泛化对 P0c-36 两行无损**（最重要的一条）：改动前后对**登记表**逐行字节比较——

```
q=24202 snapshot=30 current=30 IDENTICAL=True
q=80320 snapshot=25 current=25 IDENTICAL=True
q=24123 identical to snapshot: True   （本轮撤出批量，登记行未动）
```

**② 1479 改动前后（审计 + IR）**

| 页 | 改动前 | 改动后 |
|---|---|---|
| 1352 `select2`（stage1 首页） | `CLIENT_PAGE_UNREACHED` | `PAGE_ACTION_MATCHED` |
| 1353 `select2_1`（stage1 续页，按钮 SETPRO1） | `CLIENT_PAGE_UNREACHED` | `PAGE_ACTION_MATCHED` |
| 1693 `select3`（stage2 首页） | `CLIENT_PAGE_UNREACHED` | `PAGE_ACTION_MATCHED` |
| 1694 `select3_1`（stage2 续页，按钮 SETPRO2） | `CLIENT_PAGE_UNREACHED` | `PAGE_ACTION_MATCHED` |

节点 `started(0)/reward(2)/complete(0)`（3 节点）→ `started(0)/s1(1)/s2(2)/reward(2)/complete(0)`（5 节点）；
`transitions` 30 → 36；掉落 `drops=[]`（cp=0，掉落门不限行 ⇒ 无 QE-061 暴露面）；
交付段迁移 `B NPC_REPORT 203912: started→reward` ⇒ `s2→reward`，接取流（`unaccepted` 源）不动。

**③ 普查误报的判据（以 35010 为例，登记表逐字）**

```
35010 R 12 204560 SETPRO1  started -> started  VAR_IS:var0=0  SET_VAR:var0=1  prio 0
35010 R 16 799805 SELECT_QUEST_REWARD started -> reward VAR_IS:var0=1 prio 0
```

→ 阶段推进与交付条件都由 transition 级 var0 旗标承载，中间页 `PAGE_ACTION_MATCHED`；
若按普查直接套节点阶梯，等于重写一个**已经工作**的形状（并有把 `VAR_IS:var0=0` 条件与
新增 `s1(var0=1)` 冲突的风险），故判定为**不得重写**。

## 4. 门禁结果

| 门 | 命令 | 结果 |
|---|---|---|
| 链指纹门 | `mvn -o -B test -Dtest=RetailSimpleTalkChainGateTest` | **2/2 绿**；第一条红是"`1479`: RETAIL_TABLE 但不在冻结指纹里"（1479 此前只有 B 块、无 R 路由，不进 `questIds()`；阶梯后有了路由 ⇒ 必须冻结）→ dump 对拍 **added=[1479] / changed=[] / removed=[]** → 外科装入双副本后转绿 |
| 家族语义门 | `mvn -o -B test -Dtest=RetailSimpleTalkGateTest` | **3/3 绿** |
| 契约审计 | `mvn -o -B test -Dtest=QuestDialogOrderAuditTest` | **17/17 绿** |
| T2（1479） | `run_quest_gates.sh T2 1479`（`gates/T2-125512.log`） | 64 例 4 红：`QuestClientContractGateTest`（致命集 **44 / 所列 36 行与 P0c-36 基线逐字相同** ⇒ 本片 0 命中）、`RetailDataDrivenGateTest`×2（lane 20035/15042 轴）、`RetailSimpleCollectItemGateTest`（lane frozen 155 vs 175）；**同轮 CatalogManifest 10/10、Ownership 4/4 已转绿**（lane 迁移收口） |
| 退役恒等式 | `python3 -B verify_retirement.py` | `catalog=1351 directory=1351 retired=4873 sum=6224 — OK` |
| 副本一致性 | `md5 -q` | 登记表 `cae19506…`、指纹 `173a67f4…` 均 src↔target 相等 |

## 5. 阻塞登记（`p0c37-blocked-rows.tsv`）

- **24123 · `XML_NPC_AXIS`**：XML 转写 NPC 204345=Rikesh vs 真端 `DF2_NPC_Ananta`→204387；
  reward 投影 1 vs 客户端末行 2。两轴都要客户端证据（信件页/NPC 对话树），本轮不猜。
- **17 行 · `ADOPTION_BLOCKED`**：1324/2428/11001/18806/18807/18809/21070/21138/21460/24120/
  28806/28807/29070/29071/30055/30202/30302 —— 真端行未被合成（登记表无 N 记录，编译器稳定码
  `RETAIL_TALK_CHAIN_COMPOUND`／`NO_START`／`ACQUIRE_NPC_UNRESOLVED` 等），阶梯通道不可用；
  且这些行客户端**确有**中间页不可达 ⇒ 顺序是"先采纳（canonical/compound 通道）→ 再套阶梯"。
- **20 行 · `CENSUS_OVERFLAG`**：中间页已可达（含 5 行 35010 系旗标形、4 行 reward 轴行、其余已覆盖行）。

## 6. 未验证 / 阻塞（PENDING）

- 运行时可授权项（PENDING）：1479 的 `SETPRO1/SETPRO2` 实际点击 → var0 1→2 与任务书行推进，
  需启动服务器 + 客户端抽检；按约束**未启动进程**。
- 17 个 `ADOPTION_BLOCKED` 行的采纳通道（compound 分解 / canonical 换源）未开；24123 的 NPC 轴未裁。
- `p0c36_ladder_gap_census.py` 的判定轴**未改**（保留为候选生成器）；本片把"可达性对拍"作为
  **第二道过滤器**独立留存（`p0c37-gap-partition.tsv`），避免改动历史普查脚本导致口径不可追溯。

## 7. 命令索引

```bash
# 证据与裁定
python3 -B .agents/summary/scriptdll-quest-driver/p0c37_ladder_batch_derive.py
# （分区表由探针产出：mvn -o -B test -Dtest=P0c37GapPartitionProbeTest，源码见 .java.txt 归档）

# 落地
python3 -B .agents/summary/scriptdll-quest-driver/build_quest_client_talk_chain_steps.py
cp src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv \
   target/classes/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv

# 指纹（只能新增/修改被核对的行走 dump 通道）
mvn -o -B test -Dtest=RetailSimpleTalkChainGateTest -Dretail.talkChain.fingerprintOut=/tmp/p0c37-fp.tsv

# 门禁
mvn -o -B test -Dtest=RetailSimpleTalkChainGateTest,RetailSimpleTalkGateTest
mvn -o -B test -Dtest=QuestDialogOrderAuditTest
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T2 1479
python3 -B .agents/summary/scriptdll-quest-driver/verify_retirement.py
```

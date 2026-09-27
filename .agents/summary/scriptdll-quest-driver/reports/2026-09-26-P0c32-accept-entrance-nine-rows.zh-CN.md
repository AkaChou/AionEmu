# P0c-32：接取入口两形放行——链式登记行 9 行采纳 + 1479 投影数据修复

> 日期：2026-09-26 ｜ 切片：P0c-32 ｜ lane：活跃（同窗并发翻转 30 行，互不交叉）｜ 前序：P0c-31

## 裁定核心（编译器通道，非逐行特例）

链式 precheck 原要求规范 `NPC_START` 块，把**逐字接取路由**形状的行误判为残组拒绝：

- **两形判据**：`canonicalStart`（NPC_START 块，块合成接取流）|| `acceptEntrance`（登记表
  R/Q 记录里 `unaccepted→started` 的入口，如 QUEST_ACCEPT_1 / QUEST_ACCEPT_SIMPLE / SETPRO1
  ——`buildChain` 的逐字回放本就生成这些路由，无需块合成）。`NO_ROUTES` 同步放宽为
  "无路由 **且** 无 NPC_START 块"。
- **零入口行维持拒绝**：19070/19071（其登记行确无任何 unaccepted→started 路由——真端表无
  接取流，非本通道可解）；7 行未注册行维持 NO_START（码不变）。
- **1479 数据缺陷（client 权威）**：N 记录 `reward var0=1` 来自 XML 手工值；客户端任务书
  `QUEST_Q1479.html` 实证 **3 行**（末行=回 Memnes 交付）→ 投影应为 **2**（QE-051 0 基末行
  索引）。已修登记表 main+target；gate `acceptedDefinitionsCarryRetailSemantics` 直证
  （修复前红、修复后绿）。

## 落地（本片净变更）

| 件 | 内容 |
|---|---|
| 编译器 | `RetailSimpleTalkDefinitionCompiler` 链式 precheck 两形判据（含 Javadoc 判例） |
| 登记数据 | `quest_client_talk_chain_steps.tsv` 1479 N reward 1→2（main+target） |
| drift 登记 | 8 行 REJECTED:*→DIFF:TRANSITION_SET、1479→DIFF:NODE_PROJECTION（main+target） |
| 翻转 | 9 行 XML_RETENTION→RETAIL_TABLE（`p0c32-accept-entrance-decisions.tsv` + b1）|
| 指纹 | 8 行外科式追加（**非整文件安装**：dump 为 routes-keySet 驱动，整装会丢零路由既有 pin——见判例） |
| 收口 | verify_retirement **catalog=1391 retired=4833 sum=6224 — OK**；SimpleTalk 族门 3/3 + ChainGate 2/2 绿；9 行 lenient 回放探针绿 |

## 判例（勿重推）

1. **指纹 dump 是 routes-keySet 驱动，丢零路由 pin**：`questIds()=routes.keySet()`——dump/
   分区不变量对零路由登记行（1479 是新出现的第一个被采纳者；既有 11069/18210/21073/26990/
   28210 亦属该类）**结构性不可见**。安装指纹必须外科式（diff + 逐行追加），整文件安装会
   丢既有零路由 pin；该 gate 缺口在案（未来硬化方向：questIds 覆盖块记录）。
2. **1479 的 XML 手值不得当转写源**：链式登记 N 记录的老行有 XML 转写成分——采纳时
   client 行数（QE-051）是投影唯一权威；`acceptedDefinitionsCarryRetailSemantics` 是该面的
   机器守卫。
3. **两会话并发读改写共享文件可零损失**：本片翻转与 lane 同窗（manifest 四副本 + catalog
   双副本），whipsaw 守卫（四副本一致基线）+ 逐行唯一性断言 + verify_retirement 事后直证
   → 我方 9 行 + lane 30 行全部在位（retired 4794→4833=+39）；协同判例=守卫在写前 + 收口
   核验在写后。
4. **precheck 放宽的红线**：只放行"有确实接取入口"的行；零入口行码不变——放宽必须消费
   登记数据作判据，不做无条件的块豁免。

## 数据

- SimpleTalk 2223 = **2077 + 145**（退役 +9）；CLIENT_ROUTE 不变（本批为链轴 KEEP 出清——
  原 `SEMANTIC_GAP:RETAIL_TALK_CHAIN*` 桶 55→46）。
- 采纳 61→70（实跑与门禁均已绿，本片即生效口径）。
- 风暴复测 missing=664/wrongOwner=0 持稳（lane 同窗 30 行翻转零增量）。

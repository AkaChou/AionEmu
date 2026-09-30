# P1a：复合奖励引用 20 行 = 客户端 dic 链交付 NPC 集 + 多 NPC 交付流（SimpleCollectItem 收口之一）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 口径：真端优先。接续 M5-b3x（哨兵行 = 系统发放形状），本切片解决唯一剩下的哨兵类拒绝：
> 奖励引用是 `<地图>_<势力名>` 复合名（非 NPC 名）的 20 行。

## 1 交付

**新证据资产（机器生成）**
- `m5b3x_client_reward_npcs.py`：客户端任务书 dic 链解析器——`QUEST_Q<id>.html` 引用
  `[%dic:STR_DIC_E_<token>]`（本任务 id 或共享名，如 `LDF5a_Silverlin_BL`；35022 复用 35021、
  36015/36016 复用 36500、45022 复用 45021）→ 串表正文点名 `STR_DIC_N_<显示名>` → npc `name_desc`
  唯一匹配。产出登记表 `quest_client_reward_npcs.tsv`（入仓资源）+ 证据副本
  `m5b3x-client-reward-npcs-evidence.tsv`（同内容）。
- **对拍结论：20/20 行的客户端交付 NPC 集与现行 XML 的报告 NPC 集逐一对齐、零分歧**
  （含 39709/49709 的三人支部组 800936/800937/800938；XML 的接取半边仍是历史错误，但交付 NPC 选择
  全部获客户端实证）。另确认 `npc_factions.xml` 的 `npcid` 是"入会 NPC"而非交付 NPC（39601 的
  Silverlin_L：入会 800939 vs 交付 800922/800923），不能用作复合名解析源。

**生产代码**
- `RetailClientRewardNpcs`（新）：交付 NPC 集登记表只读视图。
- `RetailSimpleCollectItemDefinitionCompiler`：编译签名加 `clientRewardNpcs`；precheck 对复合名行改为
  "登记表覆盖即可放行（缺失仍按 `RETAIL_REWARD_NPC_FACTION_COMPOSITE` 拒绝）"；build 的交付流
  （QUEST_SELECT→SELECT5 / 39+20002 交付检查 / 完成 8..23 / FINISH_DIALOG 关窗）按 NPC 集逐个展开，
  哨兵行仍为零接取路由 + SystemGrant 边。
- `RetailQuestDriver`：装载登记表并传入编译器。

**清单与登记（机器生成）**
- `retail-simple-collect-item-drift.tsv` 重算：accepted 109 → **129**（拒绝 49 = 25 可选奖励 + 24 多物品）。
- `m5b3x-collect-sentinel-decisions.tsv` 重生成：**40 行裁定**（28 ADOPT_RETAIL + 12 ADOPT_RETAIL_DISABLED，
  后者 = 真端"定义在、星期位全 0 不发放"，上一切片已把生产星期位修正为全 0）。
- `retail-xml-retention.tsv`：RETAIL_TABLE/OK 2464 → **2484**。
- 退役 **20** 个 XML（catalog 3760 → **3740**）；冻结指纹 106 → **126**；门禁下限 109/106 → 129/126。

## 2 证据

- 登记表：`python3 -B m5b3x_client_reward_npcs.py` → `family=178 registered=28 unresolved=150`
  （150 = 107 无客户端 HTML 的普通行 + 43 有 HTML 但 dic 链不解交付 NPC 的行；**20/20 复合行全部登记**，
  另 8 行普通行顺带登记，不参与编译路径）。
- 裁定：`python3 -B m5b3x_build_sentinel_decisions.py` → `decisions=40`（28 + 12）。
- 退役：`migrated=126 moved=20 missing=0`；`verify_retirement.py` →
  `catalog=3740 directory=3740 retired=2484 sum=6224 — OK`；`refresh_catalog_doc_links.py --apply` →
  `replaced=20 dangling=0`。
- 合同台账：`build_retail_contract_tsv.py` 复跑（cap 例外 38 行不变，本批退役行不在封顶清单）。
- 门禁：T1 七类 **25/25 绿**；T2（20 id）**25/25 绿**；T3 全树见 §5。

## 3 结论

- **可删 XML 数 = 20**（35021/35022/36015/36016/39601/39605/39608/39701/39708/39709/45021/45022/46015/
  46016/49601/49605/49608/49701/49708/49709）：元数据对拍一致；交付 NPC 集客户端 dic 链 20/20 实证；
  进度事件与 DLL 语义一致（普查 43/43 无三元组，交付流挂支部 NPC + 采集对象签名）；差异全部落在
  "XML 历史接取半边"（双侧证据判 XML 错）；家族门禁绿；无未闭环口径冲突。
  - 其中 **8 行 GRANT_DAILY**：阵营每天发放，交付/领奖流就绪；
  - **12 行 RETAIL_DISABLED**：真端全 0 不发放（Silverlin/Greenhat 禁用任务）——定义照常合成
    （真端 DLL 侧对象存在），生产星期位已修全 0 → 阵营随机池永不发放、无接取路由永不可接取，
    与真端行为一致；已有进行中存档仍可交付/领奖（"永不可接取"以外无行为差异，M5-b2c §5.4 判据）。
- SimpleCollectItem 哨兵故事线就此**收口**：族内 178 行 = 129 可驱动 + 49 稳定码拒绝
  （25 可选奖励 + 24 多物品，P1b 处理）+ 0 未定性哨兵。

## 4 对拍结果

- **元数据**：20 行 npc-faction-id 直达（真端 quest.xml），全树元数据门禁绿。
- **进度事件（DLL）**：交付 NPC 上 0x3 交互 + 采集对象 0x36/37/38 签名与多 NPC 交付流一致。
- **IR**：漂移 178 行全登记；20 行 DIFF（XML 接取半边轴）→ 裁定 ADOPT；冻结指纹 126 行守等价证据。
- **客户端**：交付 NPC 集 20/20 与现行 XML 对齐（dic 链实证）；QuestClientContractGateTest 绿。

## 5 未验证 / 门禁

- T3 全树（forkCount=2）：**1947 例 / 0F / 0E / 1 skipped / BUILD SUCCESS / 373 s**（`gates/T3-012055.log`，
  唯一 skip = 既有 `QuestDialogMigrationEquivalenceTest`）。
- 实机验收：阵营日常的委托书 UI 与多支部 NPC 交付界面（与 M5-b3x 同一待办：接受手势抓包）。

## 6 阻塞与决策项

| 项 | 处理（不停等） |
|---|---|
| SystemGrant 分发接线（同 M5-b3x §5） | 等实机抓包；不阻塞 |
| P1b：25 可选奖励 + 24 多物品 | 下一切片逐类裁定 |

## 7 下一步

P1b：可选奖励（`<choice>` 选择型 25 行）与多物品（24 行）逐类裁定——复现：
`python3 -B .agents/summary/scriptdll-quest-driver/m5b3x_client_reward_npcs.py` 与
`mvn -o test -Dtest=RetailSimpleCollectItemGateTest`。

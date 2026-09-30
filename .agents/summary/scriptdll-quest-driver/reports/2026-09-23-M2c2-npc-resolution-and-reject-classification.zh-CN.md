# M2-c 批次 2：接取/报告 NPC 解析与 SimpleHunt 拒绝归类


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

- 日期：2026-09-23
- 切片：M2-c 批次 2（上一轮遗留的 179 个拒绝：145 接取名 + 21 怪名 + 13 计数）
- 前置：`2026-09-23-M2e-M2c1-retire-and-variant-equivalence.zh-CN.md`
- 结论一句话：**145 个"接取名解析失败"里 144 个是真端**类别哨兵**（`_challengetask_`/`_faction_`/`_area_`），
  真端任何数据文件都不含其 npc id；剩下 1 个（14242）经 `NPC_` 前缀别名可编译但与 XML 的 IR 不等价。
  因此 SimpleHunt 的可迁移上限为 **286/942**，其余 656 行按稳定拒绝码登记并继续走 XML。**

## 1. 交付

| 交付物 | 位置 | 说明 |
|---|---|---|
| 拒绝登记表 | `src/test/resources/quest/retail-simplehunt-compiler-rejects.tsv` | 178 行（quest_id, code, detail），家族门禁逐条消费 |
| 拒绝登记生成器 | `.agents/summary/scriptdll-quest-driver/m2c2_register_rejects.py` | 由门禁导出的直方图生成登记表 |
| 名字解析补强 | `RetailNpcNameIndex#addStrippedPrefixAliases` | 为 `NPC_xxx` 形式的 `name_desc` 登记 `xxx` 别名（真端任务表省略该前缀） |
| 编译器稳定拒绝码 | `RetailSimpleHuntDefinitionCompiler#precheck` | 6 个码：接取/报告 NPC 的 `_SENTINEL` / `_UNRESOLVED` / `_AMBIGUOUS`、`_EXCEEDS_6BIT`、`_MONSTER_UNRESOLVED` |
| 家族门禁升级 | `RetailSimpleHuntFamilyGateTest` | 覆盖 942 行登记行（原先只覆盖 465），不变量见 §3 |
| retention 生成器修正 | `build_retention_list.py` | 生产宇宙 = catalog ∪ 退役 fixture（冻结 6224，不再随 catalog 收敛缩小）；消费拒绝登记表 |

拒绝码直方图（2026-09-23 实测）：

| 拒绝码 | 数量 | 含义 |
|---|---:|---|
| `RETAIL_ACQUIRE_NPC_SENTINEL` | 154 | 接取名是真端类别哨兵，NPC 只能由类别子系统运行时决定 |
| `RETAIL_COUNTER_EXCEEDS_6BIT` | 13 | 计数 65/80/100/500 > 真端 6 位打包字段上限 63 |
| `RETAIL_MONSTER_UNRESOLVED` | 11 | 击杀槽位的名字是"刷新区 / 世界头目族"标记，不是 NPC 名 |

## 2. 证据（全部可复现）

1. **真端辅助表不含 npc id**（`/Users/mc/IdeaProjects/58Server/Map/XML/`）：
   - `challenge_task.xml`（123 条）：字段 = id/name/desc/type/race/level_min/level_max/quest_list(quest_id,quest_repeat,score)/town_id/贡献者奖励 —— **无 npc 字段**；
   - `npcfactions.xml`（18 条）：id/name/desc/category/minlevel/race —— **无 npc 字段**；
   - `npcfactions_quest.xml`：quest_id → `npcfaction_name` + 星期位 —— **无 npc 字段**。
2. **真端 NPC 注册表 `Map/XML/npcs.xml`（175,468 条 `<npc>`，87,734 条 name→id）**：
   - `_challengetask_` / `_faction_` / `_area_` → **0 命中**（哨兵不是 NPC 名）；
   - `LF4_GuardianOfDivine`（faction 类奖励名）→ 0 命中；`Gardugu` → 0 命中，但 `NPC_Gardugu` → 204004；
   - `Town_Visitorpot_seller_05` → 831209、`Palas` → 799805、`Arena_Geniki_E_LHM` → 800500（这些才是真名，早前已能解析）。
3. **两套名字索引一致性**：把 `Quest_SimpleHunt.xml` 用到的 5234 个不同名字分别喂给"本服 `name_desc` 索引"
   与"真端 `npcs.xml` 索引"，命中 9694 次**完全一致**（0 分歧），517 次两边都没有 → 换索引源不会解锁新任务，
   未解析的名字在真端自己的注册表里也不存在。
4. **类别哨兵不是"一个哨兵对应一个 NPC"**：`_challengetask_` 的 74 个任务在生产 XML 里用 6 个不同 NPC
   （800445/800447/800450/800452/831209/831234）；`_faction_` 的 62 个任务**完全没有 NPC_START**（只有 TALK_TO_NPC）。
5. **town_id 相关性不足以推导**：`challenge_task.xml` 的 `town_id` 与 XML 接取 NPC 一一对应
   （1001–1025 → 831209，2001–2025 → 831234，无 town_id → 800445/800447/800450/800452），
   但真端没有 town_id → npc_id 的表（`housing_town.xml` 亦无），因此仍不可由真端文件推导。
6. **计数溢出是真端口径 vs 生产口径分歧**：真端 `FUN_180cb13b0` 为固定 6 位/计数器；生产 XML 为装下 65/80/100/500
   把字段放宽到 7/9 位（`bit-field width=` 实证：80691→9 位、18504→7 位、1842→7+1 位）。迁移会改变客户端可见进度编码 → 保留 XML。
7. **怪名不可解析者的真实性质**：`IDAbRe_Up_Asteria` / `WorldRaid_DF5` / `LDF5_Fortress_7011_Boss_Da`
   在真端注册表里是族名前缀（`WorldRaid_DF5_1_1H`、`LDF5_Fortress_7011_Boss_Da_1..5`）；
   但按前缀展开得到的集合与生产 XML 实际击杀集**不一致**（13910 只用 `_1`；17018 只用 H 档），不可作为规则。
8. **14242（`Gardugu`）**：别名后编译通过，IR 对拍显示 XML 多一条 `TalkToNpc[204004, dialogId=10000]` 接取路由
   与 2 条 KillNpc 路由（`retail.hunt.equivOut` 证据）→ 按"XML 与真端口径分歧优先保留 XML"登记 `SEMANTIC_GAP:DIALOG_ROUTE`。

## 3. 家族门禁不变量（升级后）

覆盖保留清单里 **family=SimpleHunt 的全部 942 行**：

| 登记状态 | 门禁要求 | 实测 |
|---|---|---:|
| `RETAIL_TABLE/OK` | 必须合成成功 | 286 ✓ |
| `SEMANTIC_GAP:RETAIL_*` | 必须按登记码被拒绝 | 178 ✓ |
| `SEMANTIC_GAP:{DIALOG_ROUTE,KILL_ROUTE_MISMATCH,NODE_*_MISMATCH}` | 必须仍有计划（缺口是 IR 等价不是编译） | 477 ✓ |
| 拒绝登记表 | 每一行都必须落在 942 行族内，且码与实测一致 | 178 ✓ |

合计 286 + 178 + 477 = 941，加 14242（`SEMANTIC_GAP:DIALOG_ROUTE`）= **942** ✓。

## 4. 对拍与门禁结果

| 门禁 | 命令 | 结果 |
|---|---|---|
| 聚焦门禁（118+1） | `mvn -o -Dtest=<29 类选择器> test` | **119/119 绿**（`gates/m2c2-gate-run2.log`） |
| 家族门禁 | `-Dtest=RetailSimpleHuntFamilyGateTest` | 942 行全部符合 §3 不变量（`gates/m2c2-gate-run2.log`、`/tmp/hunt-family7.txt`） |
| 家族等价门禁 | `-Dtest=RetailSimpleHuntEquivalenceGateTest` | accepted=287 equivalent=286（14242 登记为缺口后为 286/286） |
| 归属门禁 | `-Dtest=RetailOwnershipGateTest` | 6224 行全覆盖，SimpleHunt 家族判定一致 |
| questEngine 全树 | `mvn -o -Dtest=<467 类选择器> test` | 28F+7E，与纯净 HEAD 基线逐方法比对 **only-now = 0**（`gates/m2c2-gate-run1.log`） |

## 5. 未验证 / 阻塞

- 服务端重启、客户端实机抽检：仍未执行（需用户执行）。
- 全量 `mvn test` / `mvn package`：未授权，未执行。
- 145 个哨兵任务的**运行时 NPC 绑定**（faction NPC 由种族/区域决定、challenge task 由军团面板决定）属类别子系统，
  本次只证明"真端数据文件无法表达"，未实现该子系统。

## 6. 下一步

- **M3 SimpleTalk（2223 行）**：先做 20 个代表任务语义闭环（`FUN_180cab520` 单步 /
  `FUN_180cabb10` 状态链 0→1→2 / `FUN_180caca90` 报告；2641 = 4 行摘要），再全族。
- M2 收尾项：`displayNameId` 来源未定、XML 比真端多怪的 12 个任务（口径分歧，保留 XML）。

---

## 7. 追加交付（口径修正 + M3 起步）

### 7.1 retention 口径修正：`RETAIL_TABLE` 只给"驱动已实现"的家族

**问题**：`RetailQuestDriver.load()` 只把 `RETAIL_TABLE + SimpleHunt` 当作真端驱动集合，
但 `build_retention_list.py` 之前把**所有**"真端表有行"的任务（SimpleTalk 2223、DataDriven 1508、
CombineTask 574、CollectItem 178、UseItem 104、ItemPlay 15、SerialHunt 10）也标成 `RETAIL_TABLE/OK`
——清单因此**虚报** 4612 个任务"已由真端驱动"。

**修正**：生成器引入 `IMPLEMENTED_FAMILIES = {'SimpleHunt'}`；表有行但驱动未实现的家族改为
`XML_RETENTION / FAMILY_PENDING:<族>`（evidence = `retail-table-present driver=pending`），
`RetailOwnershipGateTest` 的允许原因集合加入 `FAMILY_PENDING`。

修正后的保留清单（6224，可重跑 `build_retention_list.py`）：

| owner / reason | 数量 |
|---|---:|
| RETAIL_TABLE / OK（仅 SimpleHunt，均已证 IR 等价且 XML 已退役） | **286** |
| XML_RETENTION / FAMILY_PENDING（SimpleTalk 2223、DataDriven 1508、CombineTask 574、CollectItem 178、UseItem 104、ItemPlay 15、SerialHunt 10） | 4612 |
| XML_RETENTION / SEMANTIC_GAP | 656 |
| XML_RETENTION / SCRIPTED | 494 |
| XML_RETENTION / NO_TABLE | 176 |

→ 清单现在同时回答两件事：`family` 列 = 真端数据来源，`owner` 列 = 当前是否真的由真端驱动。
门禁：`RetailOwnershipGateTest`、`RetailQuestDriverOverlayTest`、SimpleHunt 家族/等价门禁全绿；
聚焦门禁 **119/119 绿**（`gates/m2c2-gate-run3.log`）。

### 7.2 M3 起步：SimpleTalk 表入仓 + 形状基线

- 真端 `Quest_SimpleTalk.xml`（UTF-16 + 内部 DTD）转 UTF-8 入仓：
  `src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleTalk.xml`（3152 行，709KB）。
- 形状勘察脚本 `m3_simple_talk_shapes.py` → `retail-simple-talk-shapes.tsv`：
  `表 ∩ 生产 catalog = 2223`，70 种形状，头部形状为
  `same/chain0/item_check`（1046）、`diff/chain0`（167）、`same/chain0/check/cond`（161）、
  `diff/chain0/check`（158）、`same/chain0`（116）——**"接取=报告 NPC、无对话链"是最大的一块**。
- 已确认规范形（读 `QuestXmlBlockExpander`）：
  `NPC_START`（`selection-sources`、`start-page=SELECT1`）+ `NPC_REPORT`（page ∈ {SELECT2, SELECT5, DEFAULT_SUCCESS}）
  + `npc-complete`；SimpleTalk 单步任务的报告页是 **SELECT5**（SimpleHunt 是 SELECT2）。
- M3 合成器下一步：先覆盖 `same|diff / chain0 / 无 give·remove·cutscene` 形状（约 1500 个），
  再生产 IR 等价门禁筛出可退役集合；`item_check` 影响元数据/报告守卫、`con_quest` 影响前置，均由元数据层承担。

# P5D 步 3 报告：ItemPlay 激活批（18213/28213 由 XML 保留转真端直驱）

> **门禁产物清理提示（2026-10-03）**：本文件引用的门禁运行产物（`gates/*.log`、`*-red-classes.tsv`、`*-delta.tsv` 等）已按「只记录重要的过程内容、不记录门禁」口径清理，不再随仓保留；关键读数已内联于正文，复现请重跑对应聚焦套件，清理说明见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 主题：把 P5D 步 2「接线 ≠ 激活」的两行**真正激活**——owner 由 `XML_RETENTION` 重裁为 `RETAIL_TABLE`、
> 删除遗留 XML 定义与目录条目，并补上端到端中继用例（步 2 做不到，因为路由集不含中继行）。
> 日期：2026-10-01。分支：`quest`。前置：`p5d/P5D-STEP1-REPORT.zh-CN.md`（真端形态）、
> `p5d/P5D-STEP2-REPORT.zh-CN.md`（中继接线 + 相机闸门 + 自愈）。
> 批门：族门 13/13 + 行集门 5/5；族门 + tablelane **138/138**（步 2 基线 136 → +2 例）；
> 聚焦套件 **1699 / 162F+142E / 108 类**，对步 2 基线 **ADDED 0 / REMOVED 0 / changed triplets 0**。

---

## 1. 激活判据（四源，逐行复算）

| # | 轴 | 判据 | 18213 | 28213 |
|---|---|---|---|---|
| 1 | 真端表行 | 取接/交付 NPC 唯一可解、两步 `talk_npcK` 全可解、步 2 发/扣可解、无 `cutsceneid1`/`item_check` | `Junos → Inggril → Romedon`（交付 = `Romedon`） | `Shinin → Inggness → Kijan`（交付 = `Kijan`） |
| 2 | 真端 `quest.xml` | **可接取形**（`minlevel_permitted = 51`，非停用 `999`）、种族 `pc_light`/`pc_dark`、前置 `finished_quest_cond1 = Q18212/Q28212`，`quest_work_item1/2` 与表内符号同源 | pc_light / 51 / Q18212 | pc_dark / 51 / Q28212 |
| 3 | 客户端页契约 | 入口 `select1`(1011) + 页动作 1007 目标页 `ask_quest_accept`(4) + 两步页 `select2`(1352)/`select3`(1693) 逐页声明，与 native 页序一致 | 4,1003,1004,1011,1352,1353,1693,1694,2375 | 同左 |
| 4 | owner + 目录 | 台账 `RETAIL_TABLE`、XML 不在仓、目录无条目（单一 owner，无双路径） | ✅ | ✅ |

- 复算工具：`p5d/tools/itemplay-activation-probe.py`（四源独立重解析，任何一轴不成立即 `P5D3_PROBLEM` 并返回非 0）。
- 逐行证据：`p5d/p5d-step3-itemplay-activation.tsv`（本批新增；台账 note 引用此文件名）。
- 行集分解随之更新：43 行 = **8 路由 + 7 XML 保留 + 28 不在生产**（步 2 是 6 + 9 + 28）。

---

## 2. 落地

| # | 面 | 变更 |
|---|---|---|
| 1 | owner 台账 | `retail-xml-retention.tsv`（**main + test 双副本**）18213/28213：`XML_RETENTION / ADJUDICATED:RETAIL_TALK_CHAIN` → `RETAIL_TABLE / OK`，note 记 `p5d-step3-itemplay-activation.tsv basis=RETAIL_SLOT_SHAPE+CAMERA_GATE+CLIENT_PAGE_LADDER` |
| 2 | 遗留 XML | 删除 `quests/18213.xml`、`quests/28213.xml`；`quest_definition_catalog.xml` 同步删两条 `<definition>`（目录 740 + XML 740 一致） |
| 3 | 行为面 | **零代码变更**：步 2 的中继链/相机闸门/自愈面在 owner 退役后自动生效（`retired ∧ ¬xmlOnly ∧ 面可解 ⇒ routes`） |
| 4 | 用例 | 族门新增 `activatedRelayRowsRunTheWholeChainEndToEnd`（两行各走：入口页 → 1007 问询窗 → 1002 提交发 A → 中继 1 页 1352 → **闸门拒绝**（步 1 < relayCount） → 中继 2 页 1693 发 B 扣 A → 用 B 推进 REWARD 步 3 → 奖励窗 → 领奖完成） |
| 5 | 用例 | 行集门新增 `activatedRelayRowsCarryTheRetailShapeAcquireAxisAndClientPageLadder`（真端三源 + 客户端页阶梯逐轴对称），并把「长尾行路由面」从「一律不路由」改为「只有两行激活、其余 fail-closed」 |
| 6 | 跨族重锚 | `UseItemFamilyRowAlignmentGateTest` 的 ItemPlay 独立复算由「有长尾 ⇒ 不路由」改为**逐面判定**（接取/交付/演出道具/步发扣/中继名 + 过场与交付门 fail-closed），规模常量 6 → 8；`UseItemFamilyRowInventoryGateTest` 退役/NATIVE_READY 期望 6 → 8 |

**未改动**：`SimpleItemPlayHandler` 与其余生产代码（本批是纯数据 + 门禁批）。

---

## 3. 与 QE-112 在飞切片的隔离

`retail-xml-retention.tsv`（main/test）与 `quest_definition_catalog.xml` 同时在 QE-112 工作区里被改（3122/3123/4122/4123 四行与四条目录项），
本批改动落在这两个文件的**其它行段**（台账 3245/4312 行，目录 393/513 行），全部按 **hunk 级隔离**提交：

- 提交前复核 QE-112 四行改动仍在工作区未暂存，且**未**进本批 commit；
- 本批不触碰 `RetailSimpleHuntDefinitionCompiler` / `RetailDataDrivenDefinitionCompiler` 及其测试（§10.3-#1 阻塞仍在）。

---

## 4. 门态（可复跑）

| 门 | 命令 | 结果 |
|---|---|---|
| 族门（ItemPlay） | `mvn -o test -Dtest='SimpleItemPlayNativeFamilyGateTest'` | **13/13**（步 2 基线 12 → +1 端到端链路） |
| 行集门 | `mvn -o test -Dtest='ItemPlayFamilyRowInventoryGateTest'` | **5/5**（+1 激活证据面） |
| 族门 + tablelane | `mvn -o test '-Dtest=*FamilyGateTest,*RowAlignmentGateTest,*InventoryGateTest,NativeQuestTableLoaderTest,*RewardClaimGateTest,*NativeTalk*Test,NativeNearbyQuestAxisGateTest,SMNearbyQuestsPacketTest,NativeAcceptEntryAskFlowGateTest'` | **138/138**（步 2 基线 136 → +2 例） |
| 聚焦套件 | `mvn -o test '-Dtest=*Quest*Test,*Retail*Test'` | **1699 / 162F+142E / 108 类**，对步 2 基线 **ADDED 0 / REMOVED 0 / changed triplets 0** |
| 目录/归属补跑 | `mvn -o test '-Dtest=ProductionCatalogWhitelistVerificationTest,JavaHandlerFamilyDefinitionTest,XmlDataLoaderTest,LegacyTemplateMirrorRouteRegressionTest,RewardNpcOwnershipContractTest,RewardOwnerTrimContractTest,QuestDefinitionCatalogManifestTest'` | `ProductionCatalogWhitelistVerificationTest` 与 `QuestDefinitionCatalogManifestTest` **绿**；其余 4 类 **65 例 / 4F+9E 既有红**（见 §5） |

证据：`gates/2026-10-01-p5d-step3-family-tablane.log`（`*.log` 被 `.gitignore` 忽略 ⇒ 未入仓）、
`gates/2026-10-01-focused-run-p5d-step3-red-classes.tsv`、`gates/2026-10-01-focused-run-p5d-step3-delta.tsv`
（入仓）。

---

## 5. 范围外红登记（既有，非本批引入）

`catalog-ownership` 补跑里 4 类红全部是**已退役行在 typed 生产视图缺定义**这一类既有缺口
（与 QE-112 在飞合成器切片 / P7 归属），证据：失败里出现的 id（1101..1125、4713、19064、30610、2527、1115、1107、1230、21455、15672）
**全部在 HEAD 就是 `RETAIL_TABLE` 且无 XML**，与本批两行无关；`QuestItemPlayGrantGateTest`（聚焦套件内）
同样是这一类既有红（基线即 1F，本批三元组不变）。

- 归因要点：本批只改了 18213/28213 两条台账行 + 两条目录项 + 两个 XML 文件，不可能把 1000+ 条别族
  行的定义补回去；红类的失败信息里 **零**次出现 `18213/28213/ItemPlay`。
- 本批新增的 2 行会与既有退役行一样进入该缺口（typed 视图不合成 native 行）——这正是 §10.3-#1/#2 与
  P7 的收口对象；native 车道的服务面（接取/中继/领奖）由本批端到端用例与族门覆盖。

---

## 6. 残余与下一步

1. **保持 XML 保留（理由已按真端形态改写口径）**：50048（中继名 `NPC_event_devasday_shugoseller` 静态数据零命中）、
   18828/28828（接取/交付名无解 + `con_quest` 目标 18829/28829 不在本表）、39713/49713（接取哨兵）、
   80255/80256（无相机）。
2. **P7 DataDriven（1467 行）+ §10.3-#1/#2**：仍被 QE-112 在飞切片阻塞（同一批合成器文件），落地后重冻 P0a 基线并收口 typed 视图缺口（§5）。
3. **客户端验收**：ItemPlay 全族仍 `PENDING_CLIENT`（本批新增两条激活行的中继链路，建议随客户端验收一并复测
   18213/28213 与 19048）。

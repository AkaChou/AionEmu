# 物件 owner 同型批次（2026-10-08）：逐任务证据与收口记录

范围：3036 修复报告列出的「物件被登记为任务完成 owner / 发页目标」同型候选 + 逐件复核对齐过程中
发现的同类物件（`ai=quest_use_item` / FIELD_OBJECT 系 / riftorb / shimmering_spring 等），
逐件取证后收口。代表先例 = 3036（已随 `e4fe4409f` 提交 + 客户端验收）。

口径（用户指令）：**逐件取证，不得批量改**；真端权威、退役 XML/legacy handler 仅作参考；
物件不得是 `npc-complete` owner、不得被发页（页 10 以 questId=0 解析物件自身 html → load fail）。

## 1. 逐任务裁定

| quest | 物件 | 裁定 | 依据 |
|---|---|---|---|
| 3036 | 700398 圣物 | 已收口（先例，已提交/验收） | legacy `useQuestObject(0,1,true,false)` 零回页；任务书末行 = Atropos 798155 |
| 1582 | 700196 墓碑 | 收口 | legacy START/var0==0 USE_OBJECT → var0=1 零回页；领奖行 = Nerison 204573；三步阶梯（Trou select2 链 / Nerison select3 链 + SETPRO3/SETPRO4） |
| 2232 | 700061 蜂巢 | 收口（纯采集） | 物件只掉落；领奖 = Gilungk 203613（交付人分歧另案） |
| 2237 | 700145 肥料袋 | 收口（纯采集） | legacy `// loot` 只掉落、零发页；交付 = 832822（retention `ADJUDICATED:REPORT_NPC_DIVERGENCE`，本批不动） |
| 2307 | 700247 汤锅 | 收口 | 物件 USE_OBJECT 零回页推进；Spedor 204336 三支选择置 var0=2；领奖 = Fathir 204378 |
| 2664 | 700324 药缸 | 收口 | legacy 阶梯 var 0..4 零回页、var==4 置 REWARD；领奖 = Dewi 204777；reward 轴 = legacy 落盘 4 |
| 4004 | 700340 土堆 | 收口 | 同 2664；领奖 = Randet 205128；reward 轴 = 4 |
| 4012 | 700342 FOBJ | 收口（纯采集） | 物件只掉落；领奖 = Scarecrow_Virhu 730104；reward 轴 = 0 |
| 28302 | 730375 S_BOX | 收口 | 箱子保留对话页（select2/select6，对照组 legacy 1352/2716 与真端 accept/refuse 页）；SET_SUCCEED 收敛到阶梯末节点 k10；领奖 = Hariken 799530 |
| 28303 | 700980 动力物件 | 收口 + 阶梯回归真端 | 真端 FUN_180ed7990/FUN_180ed7ab0 绝对写 var0=1/2（1+1 步）；物件 var==2 置 REWARD + 销毁、零回页；领奖 = Nineveh 804821；传送球 730390 归 SteamTachysphereAI2（XML 不参与） |
| 21105 | 700812 笛子 | 收口 | 笛子零回页占位；难民 799366 承担 SET_SUCCEED（var0=1 + 移除笛子道具 + 消失）；领奖 = 799276；REWARD/0 → 1 修复边保留（`Contract(21105, 1, 0)`） |
| 30211 | 730275 符文宝珠 | 收口 | 真端注册面挂 riftorb AI（RiftOrbAI2：页 1011 → 按钮 10000 → var0=1 + REWARD，零回页）；legacy 里 730275 的注册与对话块**整体注释禁用**；领奖行 = Pilomenes 798941 |
| 30213 | 730275 | 收口 | 同 30211；领奖行 = Cainus 798926 |
| 30311 | 730275 | 收口 | 同 30211；领奖行 = Herka 799322；与姊妹任务 30313 同形（`disabledPrismInteractionStaysUnroutedAndRewardClaimBelongsTo799225`） |
| 18808/28808 | 730534 | **不动** | 物件合法承担整个任务面且**有对话 html**（select1/select_quest）；任务书该行就是对物件本身的动作 |
| 80707 | 833501 魔法喷泉 | **不动** | 活动任务，物件自身 html（`world_fobj_spring_fountain.html`）声明 select1/select_quest（收尾页 10 可加载）；任务页在 `quest_q80707.html` |
| 80833 | 833661 老虎机物件 | **不动** | retention `ADJUDICATED:RETAIL_TALK_JOURNAL_MISSING`（客户端任务书缺失、领奖投影不可推导，fail-closed 既有裁定） |

塔标（riftorb/宝珠）：`retail-quest-ai-registrations.xml` 把 730275 挂到 riftorb AI；
`RiftOrbAI2#forQuest`（`src/main/java/.../ai/instance/beshmundirTemple/RiftOrbAI2.java`）覆盖
30211/30213/30311/30313，写 var0=1 后置 REWARD——XML 侧不得再复制 NPC 面（否则物件成为第二领取入口）。

## 2. 改动清单（本批工作区）

XML（`src/main/resources/aion/data/static_data/quest/definitions/quests/`）：
1582、2232、2237、2307、2664、4004、4012、21105、28302、28303、30211、30213、30311
（全部通过 `xmllint --schema quest_definition.xsd`）。

测试：
- 新增 `QuestObjectOwnerTrimContractTest`（10 个 TrimCase × 5 条合同：物件不得领奖 / 物件零发页 /
  领奖 owner 集合 / 领奖态步号轴 / 物件路由形状）。
- `JournalRewardRowRepairContractTest`：2664/4004/4012 移出（改由本批合同 + 投影回归锁定）；
  21105 保留 `Contract(21105, 1, 0)`（其 REWARD/0 → 1 修复边随本批补回）。
- `RewardRowProjectionRegressionTest`：追加 `Row(2664, 1, 4, true)`/`Row(4004, 1, 4, true)`/`Row(4012, 1, 0, false)`。
- `Quest30311RetailAlignmentTest`：交付对象集合 {799322, 730275} → {799322}（宝珠归 AI）。
- `QuestStartItemDefinitionRegressionTest`：1582 断言改锚物件零回页 `USE_OBJECT(-1)` 推进。
- `LegacyTemplateMirrorRouteRegressionTest`：2237 断言改锚交付 NPC 832822（报告页 2375 + 20002 双 prio 检查对），
  并新增「物件 700145 无任何 31 路由」守卫。
- `ExternalRewardAdvanceReentryContractTest`：退役行（15545/25545，XML 已随 2026-09-27 真端表驱动迁移删除）
  增加「缺席必须确属退役」守卫跳过——原测试自该迁移起在 15545 行即中止，无法验证本批基线。

基线：`src/test/resources/quest/external-reward-advance-baseline.tsv` 三行 `completionNpcIds` 去掉 730275
（30211 → `798941`；30213 → `798926 798941`；30311 → `799322`）。审计脚本
`.agents/summary/quest-10522-reward-reentry/audit_external_reward_advance.py` 当前无法复现该 TSV
（只找到 8 个任务、漏 MinionService 两行），故本批为外科式更新，脚本漂移单独挂账。

## 3. 门禁结果（IDEA MCP，2026-10-08）

全绿：QuestObjectOwnerTrimContractTest 5/5、ExternalRewardAdvanceReentryContractTest 2/2、
Quest30311RetailAlignmentTest 1/1、Quest30313RetailAlignmentTest 2/2、QuestStartItemDefinitionRegressionTest 1/1、
JournalRewardRowRepairContractTest 4/4、RewardRowProjectionRegressionTest 171/171、Quest3036ClientDialogAlignmentTest 5/5、
QuestItemSourceContractGateTest 3/3、QuestDefinitionCatalogManifestTest 10/10、
ProductionCatalogWhitelistVerificationTest（`PRODUCTION_COMPILE_OK=707 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`）、
QuestDialogMigrationGateTest 4/4、QuestInteractionObjectContractGateTest 2/2、RetailOwnershipGateTest 5/5。

既存红（非本批引入，退役任务无 IR，`missing production quest definition`）：
- `LegacyTemplateMirrorRouteRegressionTest`：1115/15672/16976/2527（本批仅修 2237 锚点）。
- `LegacyKillFlowRepairDefinitionTest`：15304/23702。

## 4. 遗留项 / 未收口

1. **宝珠三任务领奖态步号轴（QE-051 vs QE-056 冲突，待实机 A/B）**：30211/30213/30311 的
   `reward var0=1` 由批次 8 的 QE-046 基线锁住（写入方 RiftOrbAI2 同写 1）；但三任务客户端
   `quest_summary` 均为 2 行 `[%0]/[%3]`、无客户端脚本（`quest_script_monster.csv` 无行），与
   3036/1123 同型——QE-056（2026-10-07 扩展）称此类任务 reward 投影必须停在落盘值、抬到行号会让
   步骤整块空白。需按 QE-056 first_check 做一次实机观测（`//quest set 30211 REWARD 0/1` 对比任务书
   高亮行）；若确认 1 为坏值，则须按批次 8 边界「写入方 + 投影 + 基线三处同改」回滚到 0。
2. 30213 的接取 NPC 798941 在既有形状里带一份与 798926 相同的领取面（legacy REWARD 分支只注册
   798926；客户端交付 NPC 集合为单件）——本批按姊妹任务 30313 先例保留，未收口。
3. 21105 笛子 700812 的 legacy 随机刷怪（在物件实时坐标生成难民）尚无 typed 等价动作，待以物件
   刷点坐标补 `spawn-npc-current-or-default`。
4. 28303 真端 AI 注册面另有 246133/246137/248079 三个击杀 id（Drakan Ninja/Boss 模板）未建模；
   28302 阶梯 10 步 vs legacy 5 步模型的收敛点一致，步数本身待单独取证。
5. 1582 第 4 行 Kirene 为客户端数据遗留行（legacy/真端均无此步）；奖励变体索引（legacy
   `sendQuestEndDialog(env, 0/1)`）未在 completion 上建模。
6. `audit_external_reward_advance.py` 漂移（漏 MinionService 行）与
   `journal` 大小写检索（`gather_evidence.py` 只查大写 `QUEST_Q<id>.html`，本批 30211/30213/30311
   均为小写 `quest_q<id>.html`，需统一为大小写不敏感）。

## 5. 证据文件（本目录）

- `gather_evidence.py` / `scan-object-owners.tsv` / `evidence-<quest>.txt`：全库物件 owner 扫描与逐任务证据。
- `analyze_legacy.py` / `legacy-facts.tsv`：`7e9f0316c^` legacy handler 的逐目标事实（sets_reward/pages/status）。
- `client_facts.py`：客户端 NPC 名称 + 对话 html 大小写不敏感存在性。

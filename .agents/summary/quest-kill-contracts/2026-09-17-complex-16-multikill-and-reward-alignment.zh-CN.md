# 16 个复杂多阶段/多怪物维度击杀任务系统性治理与 REWARD 闭环报告

### 1. 背景与排查成因 (Background)

在完成第一批（94个）、第二批（74个）基础单目标击杀任务重构后，深入调阅 Aion 5.8 客户端全量物理解包数据（`Quest_unpacked/quest_monster.csv` 等 3,655 条 SECTION 契约）进行地毯式交叉比对，发现并彻底治愈了 16 个历史遗留的复杂击杀任务。

这类任务在历史上因以下原因被漏网或仅做局部修补：
1. **步骤非 0 时的多目标击杀（Step > 0 Multi-Kill）**：客户端要求在 `SECTION_0 == 1` 或 `SECTION_0 == 2` 下进行 `SECTION_1 < N` 计数，旧服务端直接用 `var0` 做 kill-chain 或 counter-grid，导致杀怪越界破坏步骤号；
2. **多怪物维度独立计数（Multi-Dimension Monster Counters）**：客户端为怪 A、怪 B 分配了独立的 `SECTION_1`、`SECTION_2` 计数位，旧服务端仅定义了单字段 `var0` 强行串联，导致后续怪物计数在客户端永远为 0；
3. **多计数器未闭环直入 REWARD（落实 [QE-018]）**：最后一组怪物打满后未直入 `reward` 节点并刷新，导致任务停在 `START`、NPC 头顶不亮黄色问号。

---

### 2. 治理清单与架构对齐 (Inventory & Schema Alignment)

| 任务 ID | 任务名称 | 阵营与类型 | 客户端真实契约 | 服务端字段重构 | 核心治理点 |
|---|---|---|---|---|---|
| **13945** | 破坏神圣要塞外城门 | 天族联合要塞 | `SECTION_0==1; SECTION_1<2` | `var0` (step 1), `var1` (count 0..2) | 隔离步骤与城门击杀计数，第 2 杀直入 reward 并刷新 |
| **18994** | Did You See THAT Coming? | 天族遗迹副本 | `SECTION_0==1; SECTION_1<3` + `SECTION_0==2; SECTION_1<1` | `var0` (step 1/2), `var1` (count 0..3) | 阶段 1 打满 3 装置后推进 step 2，杀信使队长直入 reward |
| **28994** | The Legion Knocks | 魔族遗迹副本 | `SECTION_0==1; SECTION_1<3` + `SECTION_0==2; SECTION_1<1` | `var0` (step 1/2), `var1` (count 0..3) | 同上，对齐魔族遗迹副本信使队长击杀 |
| **13705** | 海岸迂回作战 | 天族日常 | `SECTION_0<3` (怪A), `SECTION_1<3` (怪B) | `var0` (0..3), `var1` (0..3) | 双怪并行独立计数，最后完成者直入 reward |
| **25406** | Thwart the Ereshkigal Legion | 魔族军团周常 | `SECTION_0==0; SECTION_1<4` + `SECTION_2<4` | `var0` (step), `var1` (0..4), `var2` (0..4) | 补齐 `var2` 字段，怪 A/B 独立计数，终击直入 reward |
| **25407** | Thwart the Beritra Legion | 魔族军团周常 | `SECTION_0==0; SECTION_1<4` + `SECTION_2<4` | `var0` (step), `var1` (0..4), `var2` (0..4) | 补齐 `var2` 字段，怪 A/B 独立计数，终击直入 reward |
| **25408** | Stop the Beritra Base Troops | 魔族军团周常 | `SECTION_0==0; SECTION_1<4` + `SECTION_2<4` | `var0` (step), `var1` (0..4), `var2` (0..4) | 补齐 `var2` 字段，怪 A/B 独立计数，终击直入 reward |
| **25580** | Stop the Elyos Researchers | 魔族每日入侵 | `SECTION_0==0; SECTION_1<20` + `SECTION_2<20` | `var0` (step), `var1` (0..20), `var2` (0..20) | 补齐 `var2` 字段，20/20 双目标独立累计并安全闭环 |
| **15546** | Leona's Favor | 天族军团日常 | `SECTION_0==0; SECTION_1..4 < 4` (四目标各4) | `var0` (step), `var1..var4` (各 0..4) | 补齐 `var4`，四目标并行独立累计，全部完成直入 reward |
| **25546** | Arund's Favor | 魔族军团日常 | `SECTION_0==0; SECTION_1..4 < 4` (四目标各4) | `var0` (step), `var1..var4` (各 0..4) | 同上，魔族四目标各 4 杀严格隔离与闭环 |
| **17510** | 遗忘龟裂调查 | 天族副本核心 | `SECTION_0==3; SECTION_1<10` + `SECTION_2<1` | `var0` (step 3), `var1` (0..10), `var2` (0..1) | 补齐 `var2`，小怪 10 只与 Boss 1 只全部达成后推进 step 4 |
| **27510** | 遗忘龟裂调查 | 魔族副本核心 | `SECTION_0==3; SECTION_1<10` + `SECTION_2<1` | `var0` (step 3), `var1` (0..10), `var2` (0..1) | 同上，魔族遗忘龟裂双目标严格对齐 |
| **10112** | 后方支援任务 | 天族神圣要塞 | `SECTION_0==3; SECTION_1<5` + `SECTION_2<1` | `var0` (step 3), `var1` (0..5), `var2` (0..1) | 补齐 `var2`，清除 s2 遗留计数，小怪 5 只与 Boss 1 只闭环 |
| **20112** | 进路支援任务 | 魔族神圣要塞 | `SECTION_0==3; SECTION_1<5` + `SECTION_2<1` | `var0` (step 3), `var1` (0..5), `var2` (0..1) | 同上，魔族神圣要塞进路支援双目标严格对齐 |
| **10011** | 需要鲜血浇灌的花朵 | 天族使命大结局 | `SECTION_0==5; SECTION_1<5` + `SECTION_0==6` | `var0` (step), `var1` (0..5) | 规范化 6-bit 位段，s5->s6 自动清零计数器，阻断污染 |
| **20011** | 需要鲜血浇灌的花朵 | 魔族使命大结局 | `SECTION_0==5; SECTION_1<5` + `SECTION_0==6` | `var0` (step), `var1` (0..5) | 同上，魔族使命大结局规范化与计数安全隔离 |

---

### 3. 验证与门禁标准 (Verification)

1. **XSD 模式校验**: 全部 16 个 XML 经 `xmllint --schema quest_definition.xsd` 校验，100% 验证通过。
2. **审计测试门禁扩展**: `QuestMonsterProgressContractAuditTest#complexAndMultiDimensionKillQuestsAlignWithClientSections` 严格锁定这 16 个任务的字段存在性与 6-bit 偏移量，IDE inspections 0 错误 0 警告。
3. **备份保留**: 原始 XML 完整备份于 `.agents/summary/quest-kill-contracts/backups-complex-16/`。

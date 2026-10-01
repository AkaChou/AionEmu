# P0a §4.5：零任务硬编码审计（扫描 + 召回 + 裁定）

> P0a 只读审计产物 · 2026-10-01。逐行清单：`hardcode-audit.tsv`（57 行）；扫描器：`tools/hardcode_scan.py`（src/main/java 全域，6 形态）。

## 1. 召回自检（门禁资格）

计划要求的 6 个已知样本：**6/6 PASS**（RetailQuestMetadataCompiler `questId==1007||2009`、CAKE_EVENTS `Set.of(80008,80009)`、RetailClientDialogExits 任务表、RetailChallengeAcquireAdoptions ADOPTED、RetailNpcNameIndex `byName.put`、buildDefault* 形态类）。其中 buildDefaultEntries 字面样本已不在当前 src/main（P6 形态类覆盖 2 处等价形态）。**扫描器具备门禁资格**（recall-first，人工裁定记录于本文件）。

## 2. 总量与分区

| 区 | 命中 | 处置基调 |
|---|---:|---|
| `questEngine/retail/` | 48 | **HARDCODE_TO_DELETE**：整包按计划 §6.7 随家族切换删除，无需逐行迁移 |
| `questEngine/`（非 retail，XML 车道） | 3 | 2 处 craft 技能词汇表 = GLOBAL_CONSTANT_OK；1 处页 id 形态 = XML 车道冻结范围 |
| 跨包 | 6 | 见下逐行裁定 |

## 3. 跨包逐行裁定（audit 的真正增量）

| 位置 | 内容 | 裁定 | 理由 / 过期条件 |
|---|---|---|---|
| `controllers/PlayerController.java:192` | `questId <= 0xFFFF` | **GLOBAL_CONSTANT_OK** | 协议/数据域范围守卫，非任务事实；真端同构（questId 4 字节键） |
| `geoEngine/GeoWorldLoader.java:60` | MAIN_DISABLED_MAPS（map id 集） | **NOT_QUEST_SCOPE** | 地图 id，与任务无关（扫描器范围误报，登记不处置） |
| `model/templates/quest/XMLStartCondition.java:143` | `questId == 2947 \|\| questId == 1922` | **XML_LANE_FROZEN_SCOPE** | XML 车道引擎层 questId 特例；XML 车道冻结、后续退役单独立案时处置（登记在案，防遗忘） |
| `ai/quests/QuestStartItemNpcAi2.java:28` | `REWARDS_BY_NPC = Map.of(700004→(1197,182200558),…)` | **XML_LANE_FROZEN_SCOPE**（任务奖励事实进 AI 代码） | 服务新手起始任务；归属 XML 车道起始任务轴，退役案处置 |
| `ai/RetailPatternAI2.java:91` | C_SCORE_CONSUMERS（npc id 集 + 301120000 地图） | **P0_WHITELIST**（AI 战场积分消费方，非任务事实） | npc/map id；过期条件 = P7 DD EnterArea/战场轴若引用同 id 集须改为数据表 |
| `questEngine/definition/QuestXmlBlockExpander.java:711` | 页 id 39 的 talk 块回退 | **XML_LANE_FROZEN_SCOPE** | XML 车道内部页流 |

## 4. retail 包内需要特别留存的「知识」（删除 ≠ 丢弃）

1. `RetailNpcNameIndex:371-414` 的 `byName.put` 硬编码（要塞神长/世界 BOSS/多刷怪点 id 集）：这是**真端表名 → 本服模板 id** 的映射知识。owner-identity 矩阵已证明直接名字解析对 4623 行失效（命名惯例漂移），native `NativeNpcNameResolver` 落地时**必须以数据表 + 静态数据证据重建该映射**（带出处），不得原样搬代码（P0 白名单禁止），也不得静默丢失（防 QE-073 身份盲区复发）。
2. `RetailQuestMetadataCompiler:88` COMBINE_SKILLS（工艺技能名→40001-40010）：与 XML 车道两处一致，属**全局工艺技能词汇**——native 复用时按 GLOBAL_CONSTANT_OK 走白名单（证据：真端 CombineTask `combineskill` 字段同词汇；过期条件 = P6 CombineTask 批核验）。
3. `RetailQuestMetadataCompiler:462` `questId==1007||2009`（转职仪式 1 基奖励槽特例）：HARDCODE_TO_DELETE，P1 metadata loader 改读 quest.xml `reward_exp*` 槽位声明，不得白名单。

## 5. 结论

- native 车道新建代码的硬编码门以本扫描器为基线（当前 0 处 native 代码，P1 起每批重跑，exit=0 为过门）。
- retail 包 48 处不逐行处置：整包删除门（§6.7）覆盖；删除门执行时本 TSV 作为删除完备性的对照底稿。

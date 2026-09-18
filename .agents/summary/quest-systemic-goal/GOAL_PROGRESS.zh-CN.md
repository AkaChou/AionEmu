# Quest Systemic Goal 进度台账（GOAL_PROGRESS）

> 目标：以 Aion 5.8 真端解包数据为权威，对 AionEmu 任务系统做长期系统性根因治理。
> 权威外部数据：`/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest.xml`（只读）。
> 本台账记录每个审计轴的：audit count / real defects / false positives / evidence blocked / fixed / commit / tests / next action。

## 快照基线（2026-09-18 全库只读扫描）

- 真端任务 10,035 个；生产任务 6,222 个。
- 审计脚本：`.agents/summary/quest-systemic-goal/audit_start_metadata.py`
- 差异清单：`.agents/summary/quest-systemic-goal/start-metadata-diff.tsv`

### start-metadata 轴量化结论（sentinel 归一后）

| 轴 | audit 范围 | 差异数 | real defect | false positive / 等价表达 | 状态 |
|---|---|---|---|---|---|
| min-level | 真端可接任务（min≠999 占位）6,215 | 9 | 9（生产漂移） | 0 | P1 修复 |
| max-level | 全部 6,222 | 360 | 28（19 生产过宽无上限 + 4 生产 82→真端 65 + 5 族内不对称残留 cap） | 332（329 整族一致 cap=82 有意封顶 + 3 生产 998 等效无上限） | P2 修复+例外 |
| faction | 全部 6,222 | 1 | 0 | 1（15205 服务端按阵营拆分 15205/25205） | 例外记录 |
| gender | 全部 6,222 | 0 | 0 | 0 | 基线锁定 |
| max_repeat_count | 全部 6,222 | 0 | 0 | 0 | 基线锁定 |

### 真端 sentinel 语义（已取证）

- `minlevel_permitted=999`：真端占位任务（不可接取），生产侧自行激活，min 不纳入比对。
- `maxlevel_permitted=0`：字段默认，无上限。
- `maxlevel_permitted=998 / 999`：不可达大值，等效无上限（998 全部出现在 min=66 的 5.8 末期任务上；999 同时用于占位任务）。
- 生产 `max-level=2147483647 / 999 / 998`：同样等效无上限，与真端无上限等价。
- 真端 `race_permitted=pc_light pc_dark` 与生产 `races=PC_ALL` 等价。

## 阶段 P0：Goal 进度台账

- 状态：DONE（本文件）
- 修复+例外清单+门禁 commit：fbfbaba1c
- 产物：本台账 + 全库 start-metadata 审计脚本与差异清单

## 阶段 P1：min-level 真端精确对齐

- audit count：6,215（真端可接任务）
- real defects：9（1648、2231、2641、19000、19001、19002、19003、25407、25408）
- 修复明细（生产 → 真端）：
  - 1648：45 → 42（镜像 2648 生产/真端均 42，互证）
  - 2231：13 → 12
  - 2641：37 → 41（注意：镜像 1641 真端为 42，真端两族本就不同，以真端 2641=41 为准）
  - 19000/19001/19002/19003：40 → 50（镜像 29000~29003 生产/真端均 50，互证）
  - 25407/25408：70 → 68（镜像 15407/15408 生产/真端均 68，互证）
- 门禁：`QuestRetailStartMetadataGateTest` + 基线 `src/test/resources/quest/quest-start-metadata-retail-contract.tsv`
- 目录：`docs/QUEST_CATALOG.zh-CN.md` 同步刷新目标行（含 28649 文档漂移 66+→37+）
- 状态：DONE（见 commit）

## 阶段 P2：max-level 与 faction 全库收敛

- audit count：6,222
- real defects 28，分三类：
  - A. 真端有明确上限，生产无上限（19 条）：4711→50、4712→50、4722→55、15665→67、15666→67、18833→23、18835→26、25665→67、25666→67、29672→55、29673→19、29674→30、29675→40、29676→48、29684→19、29685→29、29686→39、29687→49、29688→65
  - B. 生产 82 过宽，真端 65（4 条）：80621、80622、80643、80644
  - C. 真端无上限，生产残留零散 cap（5 条）：27525（82，同族 17525 无上限）、50074（82，同族 Kumuki 50073=50/50074 无上限）、80945（82）、80946（82，同族 80947 无上限）、80878（75，同族 Luna 80876/80877=75 但真端 80878 明确 0）
- false positive / 等价表达 332 条：
  - 329 条整族一致 `max-level=82`（5000~5040 制作补给族、5100~5581、15300/25300、35052~35065 阵营每日、36542~36548、39022、45052~45065、46537~46548、49022、80602~80610 组队任务等）：服务端系统性版本封顶惯例（整族无一例外=82，与配置全局上限 83 相邻；5.8 玩家 66 级封顶，无玩家可见影响）→ 有意封顶例外清单 `quest-start-metadata-retail-cap-exceptions.tsv`
  - 25533、25566、25640：生产 `max-level=998` 等效无上限，等价表达
  - 15205：faction 服务端按阵营拆分（配对 25205），例外记录
- 状态：DONE（见 commit）

## 阶段 P3：职业权限语义收敛

- audit count：135（真端 class_permitted 为非空真子集的生产任务；另 6065 条真端全集+生产通配等价）
- token 映射：warrior->WARRIOR、fighter->GLADIATOR、knight->TEMPLAR、scout->SCOUT、
  assassin->ASSASSIN、ranger->RANGER、mage->MAGE、wizard->SORCERER、
  elementallist->SPIRIT_MASTER、cleric->PRIEST、priest->CLERIC、chanter->CHANTER、
  engineer->TECHNIST、gunner->GUNSLINGER、rider->AETHERTECH、artist->MUSE、bard->SONGWEAVER
- 死条目规则：min>=10（转职后）时 6 个 base 职业（WARRIOR/SCOUT/MAGE/PRIEST/TECHNIST/MUSE）
  不可达，真端 token 与生产遗留声明在比对前删除；只比较"实际可接受职业集合"。
- real defects 58（已修复）：
  - FIX_ADD 42：导师任务族 3928/3929/4922/4926/4927/4928/4929（生产漏声明 classes，
    其中 4928 只剩死条目 PRIEST 已是无职业可接的死任务）、Kaliga 武器收集族 18618~18627/28618~28627、
    18643~18648/28643~28648、Dark Poeta/守护者英雄分组 80219/80220/80228/80229/
    80316/80317/80322/80323 -> 按真端写入实际职业集合
  - FIX_REPLACE 9：18614/18630/18634/28614/28630/28634 删多余 TEMPLAR（同族 18613 等互证）、
    19074 枪星导师任务删 AETHERTECH（镜像 29074 正确互证）、14031/24031 机甲星使命
    收窄为 [AETHERTECH]（真端两份一致 [engineer rider]+任务名/枪械奖励多源互证）
  - FIX_APPEND 7：1466/1496/1497/1498/2696 补 AETHERTECH（真端全集，生产缺机甲星）、
    11076 补 AETHERTECH（MUSE 条目为死条目表达）
- intentional variant 3：3121/30350（密码之刃=技匠系武器适配）、30237（新枪矛=战士系武器适配）
- EVIDENCE_BLOCKED 7：24050~24054（真端缺 MUSE 疑似笔误 vs 生产通配）、3910、4931
  （真端与生产互有缺失，17 级使命链语义缺仓库内证据；取证方向：客户端任务文本职业说明、
  真端服务器抓包接取行为）
- 门禁：`QuestRetailClassGateTest`（3 例）+ 基线 `quest-class-retail-contract.tsv`（135 行快照）
- 状态：DONE（commit 1a9a80f72）

## 阶段 P4：奖励结算精确对齐

### 第一批：档位 1 数值奖励（EXP/GOLD/AP/GP）——DONE

- audit count：6,215（真端有档位 1 字段的生产任务；真端字段缺失=未配置，生产自建奖励属服务端设计）
- 审计脚本：audit_reward_axis.py（道具 name_desc->id 映射 128,310 项 + 真端 client_items 兜底）
- 语义规则（取证结论）：
  - 真端字段存在（含 0）即权威；字段缺失（RETAIL_UNSET）跳过
  - 生产档位 1 = 平铺 <rewards>；多档任务取第一个 <group>；npc-complete 的
    fixed/choice reward-index 绑定容器内位置，插入必须尾部追加、删除前必须核对索引合同
  - 实际修复：EXP 17 + GOLD 23 + AP 10 + GP 25 + EXP 占位删除 3 = 78 处数值对齐
    （含 2641/2724/1908/2345 等任务；80989/80990 真端 0 但生产 1 点经验占位按
    npc-complete 索引合同保留为例外）
- intentional 例外 20 条：AP 服务端 4 倍版本倍率族（11279~11286、21281~21288、
  18849、18850、28849、28850，生产=真端精确 ×4）
- EVIDENCE_BLOCKED：
  - ITEM_UNMAPPED 956：真端奖励道具名（SCROLL_speed_fly_50A 等 4.x 旧名）无法映射到
    5.8 道具 id；client_items_etc.xml 为专用容器格式不可解析。取证方向：解包真端
    item 表（etc 部分）或以 strings/tooltip 反查
  - TITLE 173：真端 reward_title1 为名称字符串（light_title04），本机无 title 模板表
    可映射数字 id。取证方向：解包真端 title 表
- 第二批（item/selectable 轴）——DONE（2026-09-18 同日）：
  - 审计口径升级：可选奖励的等价发放来源=metadata SELECTABLE 声明 ∪ 显式
    SELECTED_QUEST_REWARD 分支的可变 grant-reward ∪ npc-complete choice 指向项。
    原始 34 条 SELECTABLE_DIFF 中 16 条被证明为等价形态（1941 类 8 分支与真端
    8 件映射逐 id 一致互证）
  - SEL_EXTRA 17 条缩池修复：4017 缩 102000900、3946 缩 152200370、
    30222 族 10 条缩 186000098、1540 缩 110300939、
    1942/19015/30327 删不可达防御分支
  - **更正（档位轴重审）**：1687/2677 实为真端三档任务（档 1 防具 4 件、
    档 2 帽/装备 5-8 件、档 3 装备 4 件），P4-2 按"单档 selectable 口径"把
    档 2/3 道具误判为 extra 缩掉——已回滚到三档平铺单池形态（玩家可选范围
    恢复），记 MULTI_TIER_FLATTENED 例外，待按 QE-026 重建三档 reward-groups
    + 档位窗口合同；18606/50029/51029 删除的 190000016/186000178 实为真端
    reward_item_ext_1 扩展奖励——已回滚（档 1 平铺表达，结算结果一致），
    记 EXT_FLATTENED 例外。教训已固化：缩池前必须先查真端 selectable2/3
    与 ext 字段（audit_tier_axis.py 新增档位轴扫描）
  - SEL_MISSING：2641 补真端 5 件上衣声明（npc-complete 通配自动覆盖）；
    16921/26921 记等价例外（真端唯一 1 项可选=固定发放）
  - ITEM_DIFF 23 条：5 条容器内完全重复声明去重（2303/2367/2411/2448/3088，
    其中 2303 fixed 索引把重复项全列为发放项=完成发 48 瓶 vs 真端 24，重复即
    重复发放型真缺陷）；16 条零散错配修复（1648 coin_05x4、2332/25082 药水
    替换、2585/2611 数量、2962/15230/15231/15232/25230/28915/80333 补真端
    固定道具、18310/18606/50029/51029 删生产多发项）
  - 逐条例外：2392 分支档位形态（8/4/4+2 币三分支与真端单组最大档一致，已验收）、
    2345 双路线组归属 EVIDENCE_BLOCKED（取证方向：旧 handler 2345 路线发放代码）、
    16921/26921 单项可选=固定发放
  - 门禁：`QuestRewardItemGateTest`（2 例，固定道具多重集合 + 可选三来源并集，
    覆盖 3,992 行可映射基线）+ 基线 `quest-item-selectable-retail-contract.tsv`
  - 验证：11 门禁 46 例全绿；PRODUCTION_COMPILE_OK=6189、0 失败、0 白名单违规
  - EVIDENCE_BLOCKED 不变：ITEM_UNMAPPED 956（道具旧名无映射）、TITLE 173（无
    title 模板表）、2345（双路线组归属）
- 门禁：`QuestRewardValueGateTest`（1 例，覆盖全部 6,222 任务含 METADATA_ONLY）+
  基线 `quest-reward-value-retail-contract.tsv`（6,215 行）
- 状态：DONE（数值轴全库 0 未解释差异；门禁测试 46 例全绿、
  PRODUCTION_COMPILE_OK=6189、0 白名单违规）

## 阶段 P5：下一类未根治维度

- 状态：SCANNING（P4 全部子批已完成并提交，审计明细 TSV 已归档）
- P5 方向与既有覆盖映射（2026-09-18 登记）：
  1. 任务放弃/失败/重登后的状态与道具恢复：已有 QE-003（完成路径 work-item 清理）、
     QE-011（work-item 迁移覆盖）、QE-016（实例回退区间）+ QuestWorkItemMigrationCoverageTest
     守护；剩余盲区=非 work-item 任务道具的放弃恢复，待专项扫描
  2. repeatable 重复接取/周期/冷却/奖励幂等：repeat 轴（max_repeat_count 全量）与
     daily/weekly cycles 已由 QuestRetailStartMetadataGateTest 锁定 0 差异；
     **reward_repeat_count 已扫描收敛（2026-09-18）**：真端 265 条字段值全部等于
     max_repeat_count，生产缺省（XML 缺省=maxRepeatCount）全部等价，0 真实差异；
     cooldown-seconds/daily/weekly 无真端解包来源（data_driven_quest.xml 仅含
     category/reward_npc/progress 字段）→ EVIDENCE_BLOCKED（取证方向：真端
     challenge_task 表或服务器侧周期数据）
  3. 事件任务生成/回收：80xxx 活动任务生产自建（SERVER_ONLY 清单 7 条）已入
     start-metadata 门禁例外；生成/回收运行时行为待运行时验证（PENDING 用户侧）
  4. NPC 报告目标与客户端页面所有者一致性：已有 Playbook 多 NPC owner 模式
     （MULTI_NPC_HANDOFF_REWARD_OWNER 等）+ QuestDialogOrderAudit；
  5. 奖励领取路径唯一性/不可重复领取：结构性保证=QE-014 编译器 AMBIGUOUS_TRANSITION
     拦截同 (source,npc,action) 重叠边 + npc-complete 完成合同编译校验
     （CHOICE_REWARD_TYPE/INDEX_OUT_OF_RANGE）+ QE-022 BFS 死胡同门禁；
     P4-2 的 fixed 索引合同修复进一步消除重复发放
  6. 任务状态持久化与快照恢复：已有 QE-012（SECTION 位段）/QE-019（事实需求继承）
     覆盖；快照恢复专项待扫描
  7. 新症状类：维持「先全库归类再修」纪律
- 扫描结果（2026-09-18）：
  - 真端占位任务（minlevel_permitted=999）被生产激活的数量 = **0 条**
    （生产 6,222 任务在真端全部可接，无需激活例外台账；start gate 的
    RETAIL_PLACEHOLDER 跳过逻辑保留为防御性）
  - challenge_task.xml 的 quest_repeat（159 条）属挑战任务系统，与通用任务的
    cooldown/daily/weekly 无关；后者真端来源维持 EVIDENCE_BLOCKED
  - 方向 1（放弃恢复）：生产放弃路径已在引擎层统一清理声明的 work-items
    （QE-003/QE-011）；中途 remove-item 交付的道具离开背包后无恢复需求；
    掉落/give-item 天然可重取——静态层面无新增缺陷，运行时行为验证
    属用户侧 PENDING
- 追加修复（2026-09-18）：QuestWorkItemMigrationCoverageTest 首次纳入本轮门禁
  即暴露 25304 声明缺口（legacy quest_work_items=182215874 未声明，完成后残留
  背包，QE-011 同根因）→ 已补声明，3/3 全绿（commit 32b59d6d6）
- 档位轴扫描（2026-09-18，audit_tier_axis.py 新增）：真端档位 2/3
  （reward_exp2/gold2/abyss_point2/item2_N/selectable2_N 等）与生产
  reward-groups 对照，残留 141 条多档差异整体定性为**多档合同结构改造批次**
  （QE-026 REWARD_GROUP_TIER_FIDELITY 模式应用）：
  - TIER2/3_MISSING_PROD 112 条：真端有实质档位 2/3、生产单组表达
    （1687/2677 已回滚保留三档平铺单池；其余如 1122/1367/1535/1922 待重建）
  - TIER2/3_NUMERIC/ITEM_DIFF 34 条：生产有组但组内容与真端档位不一致
  - EXT_DIFF 40 条：真端 reward_item_ext_1 扩展奖励 vs 生产 extended-rewards
  - 每条重建涉及档位窗口页（QE-026：第 5/6 档为页面 45/46）、choice 组分配、
    complete-reward-index 与预览合同的完整取证，逐任务执行
  - TIER2/3_UNMAPPED 14 条：真端道具旧名不可映射，EVIDENCE_BLOCKED（同
    ITEM_UNMAPPED）
- 第三批：extended（最后一轮追加）轴 —— DONE（2026-09-18 同日）
  - 真端 ext 模型取证：reward_gold_ext + reward_item_ext_1 +
    selectable_reward_item_ext_N + reward_title_ext（title 为名称字符串，
    数字 id 映射 EVIDENCE_BLOCKED）
  - 引擎语义：extended-rewards 由 QuestMutationPlanner.appendFinalRepeatRewards
    在最后一轮重复完成（completeCount==rewardRepeatCount-1）时自动追加发放
  - 修复 81 条：G1 补 gold_ext GOLD 行 27 条（2341/2346/3007/3010/3054/3110/
    3314/3320/3321/3322/3534/4527/11138/11145/11226/11227/11229/11233/11464/
    21076/21079/21237/21238/21240/21246/21464/28606）、G2 建容器补声明 39 条
    （含 2658/2659/2660 的 selectable 对、3119 族移动形态、16977 族、80875/
    80899/80990）、G3 4202/4206/4214/4216 补真端 selectable 件、G4 80040/
    80041/80116 族缩池到真端单件、80990 删 ext 组 1 点 EXP 占位
  - 形态兼容：extended-reward-groups 组 1 = 最后一轮追加奖励（3119 族 9 条
    原生声明即为正确形态，audit 与门禁均已兼容）；18606/50029/51029 从
    EXT_FLATTENED 例外移除（已按真端重建）
  - 门禁：QuestRewardItemGateTest 扩为 3 例（fixed 多重集合 + 可选三来源 +
    extended 全量），基线 TSV 增 retail_extended 列
  - 验证：12 门禁 50 例全绿；PRODUCTION_COMPILE_OK=6189、0 失败、0 白名单违规
  - 剩余 TIER_MISSING/NUMERIC/ITEM_DIFF 136 条维持「多档合同结构改造批次」
    （QE-026 逐任务窗口/choice/组索引取证）
- 下一检查点：P5 静态可扫项已全部收敛或登记；剩余验证依赖
  （a）Aion 5.8 客户端实机复测（用户侧 PENDING）、
  （b）真端 item/title 模板表解包（ITEM_UNMAPPED 956 / TITLE 173 / 2345 双路线）、
  （c）运行时日志与抓包（cooldown/daily/weekly、事件任务生成回收）
- Memory Bank：QE-034（RETAIL_START_METADATA_CONTRACT_ALIGNMENT）已沉淀，
  sync/check 通过，随本台账一起提交

## 验证记录

### 2026-09-18 P1+P2 联合门禁（P1/P2 共享同一基线与门禁，合并交付）

- 命令 1：`mvn -q -Dtest=QuestRetailStartMetadataGateTest test`
  - Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
- 命令 2：`mvn -q -Dtest=QuestRetailStartMetadataGateTest,QuestItemSourceContractGateTest,QuestDropContractGateTest,QuestMovieAndDialogLoopRegressionTest,QuestDefinitionDirectoryLoaderTest,CompletedQuestPrerequisiteRegressionTest,QuestClientContractGateTest,ProductionCatalogWhitelistVerificationTest test`
  - QuestRetailStartMetadataGateTest: 6/0/0/0
  - QuestItemSourceContractGateTest: 3/0/0/0
  - QuestDropContractGateTest: 1/0/0/0
  - QuestMovieAndDialogLoopRegressionTest: 19/0/0/0
  - QuestDefinitionDirectoryLoaderTest: 2/0/0/0
  - CompletedQuestPrerequisiteRegressionTest: 7/0/0/0
  - QuestClientContractGateTest: 1/0/0/0
  - ProductionCatalogWhitelistVerificationTest: 1/0/0/0
- 生产编译：PRODUCTION_COMPILE_OK=6189，PRODUCTION_COMPILE_FAILURES=0，
  PRODUCTION_INTERACTION_OBJECT_FAILURES=0，PRODUCTION_WHITELIST_VIOLATIONS=0
- 未执行验证：Aion 5.8 客户端实机复测（等级门槛变化需客户端登录验证）——PENDING，
  由用户执行；本批改动只影响接取资格数值判定，无状态机/协议/奖励路径变化。

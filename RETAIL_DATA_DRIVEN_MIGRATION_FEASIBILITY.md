# 真端任务与副本全数据驱动迁移：可行性与实施方案

> 分析日期：2026-07-26
> 文档性质：迁移架构与数据契约建议，不代表当前代码已完成迁移
> 真端数据权威源：/Users/mc/IdeaProjects/58Server-new
> 真端源码与反编译语义证据：/Users/mc/IdeaProjects/58Server
> 无损转换方案参考：/Users/mc/PycharmProjects/aion_drop
> AionEmu 运行目标：/Users/mc/IdeaProjects/AionEmu

## 1. 结论

该方向可行，而且比继续逐任务、逐副本修改更接近真端原有架构。

但“先补全数据，再数据驱动”必须准确理解为：

1. 在转换侧无损保存全部真端数据，不因 AionEmu 暂时不用某字段而丢弃。
2. 在生成期建立地区覆盖、引用闭包和类型化中间表示。
3. 在运行侧只加载已经完成引用解析、语义证明和执行器支持的派生数据。
4. 继续复用 AionEmu 已有 Quest、AI、副本、状态、计时和结算服务，不再建立第二套脚本虚拟机。
5. 以“行为能力族”为迁移单位，一次补齐一种通用语义并自动覆盖全部匹配数据，不再为单个任务或地图增加编译分支。
6. 只有触发、条件、状态、进度、完成、奖励、清理和恢复等所有权全部闭合后，才删除原 Java Handler。

推荐的整体流水线是：

~~~text
58Server-new 原始公共数据 + China 覆盖数据
                    │
                    ▼
          无损规范化权威层
     原值、顺序、空值、重复值、来源、SHA
                    │
                    ▼
              全局引用图
 NPC / Item / Skill / World / Area / Path / Portal /
 String / Movie / Faction / ChallengeTask / Party ...
                    │
                    ▼
              类型化中间表示
     Event + Condition + State + Action + Reward
                    │
                    ▼
           AionEmu 运行时派生表
       只包含可执行且引用闭合的有效数据
                    │
                    ▼
 QuestEngine / DataDrivenQuest / RetailPatternAI2 /
 InstanceRuntimeState / InstanceSettlementService ...
~~~

这不是要求把 58Server-new 的全部大 XML 直接放进 GameServer 内存。正确边界是：

- 全数据必须存在于转换和审计层；
- 运行时只消费当前版本实际可达的引用闭包；
- 未知语义必须保留并隔离，不能静默删除，也不能猜测执行。

## 2. 权威来源和覆盖规则

### 2.1 权威顺序

| 优先级 | 来源 | 责任 |
| --- | --- | --- |
| 1 | 58Server-new/Map/XML、58Server-new/Map/Worlds | 真端原始数据、China 地区覆盖、世界与副本资源 |
| 2 | 58Server/server58-source、恢复函数和类 | 解释槽位、事件、状态转换、失败语义和服务调用 |
| 3 | aion_drop/staticdata_converter | 无损规范化、地区覆盖、manifest、往返验证和派生生成方法 |
| 4 | AionEmu definitions、静态 XML、Java Handler | 当前兼容目标、运行时消费者和回退实现 |

AionEmu 已生成 XML 是派生产物，不能反向定义真端字段含义。Java Handler 只能用于：

- 证明当前 AionEmu 还在承担哪些行为；
- 作为 shadow 对比对象；
- 在真端数据或语义尚未闭合时继续回退。

不能因为 Handler 当前这样实现，就把它的私服逻辑写回“真端数据模型”。

### 2.2 公共层与 China 层

58Server-new 同时存在公共文件和 China 同名文件，且内容并不总是相同。例如公共 quest.xml、npcs.xml 与 China/quest.xml、China/npcs.xml 的 SHA-256 不同。因此应采用以下规则：

1. 原始层同时保存 common 和 China 两份记录及各自 SHA-256。
2. China 存在同逻辑路径时，形成有效视图时由 China 完整覆盖 common。
3. China 不存在时回退 common。
4. 文件名匹配忽略大小写，但 manifest 保存原始文件名。
5. 覆盖不是删除：被覆盖的 common 记录仍应能审计和往返恢复。
6. 运行时派生表必须记录其有效来源，不能混合 common 与 China 的同名行。

### 2.3 当前 P0 路径问题（已解决，保留记录）

> 状态更新 2026-07-26：AionEmu 侧路径漂移已解决。
> generate_retail_simple_quests.py 的 DEFAULT_RETAIL 与 generate_retail_instance_data.py 的
> DEFAULT_SOURCE 均已指向 /Users/mc/IdeaProjects/58Server-new/Map/XML；语义根保持
> /Users/mc/IdeaProjects/58Server/server58-source；两个生成器均有 assert_not_legacy_data_root
> 旧根拒绝守卫、来源 manifest + SHA-256 记录和 --check 陈旧检测。
> aion_drop 侧路径漂移也已解决：`convert_staticdata.py`、`generate_scaling_drops.py` 和
> `quest_migration_audit.py` 的数据默认根已切到 58Server-new；转换器说明与设计文档同步更新，
> 正式转换入口拒绝旧 `/Users/mc/IdeaProjects/58Server/Map/XML`。58Server/server58-source 与
> ScriptDLL64.dll 路径仍只承担语义证据，不随数据根迁移。

当时存在以下路径漂移：

- scripts/generate_retail_simple_quests.py 的数据默认仍是 /Users/mc/IdeaProjects/58Server/Map/XML。
- scripts/generate_retail_instance_data.py 的数据默认仍是 /Users/mc/IdeaProjects/58Server/Map/XML。
- aion_drop/staticdata_converter 的说明和部分生成器默认仍指向旧 58Server。
- Quest 的反编译语义路径应继续使用 /Users/mc/IdeaProjects/58Server/server58-source，不应错误切到只承担数据权威的 58Server-new。

因此第一步不是增加字段，而是统一入口参数和 manifest：

- 数据根切到 58Server-new；
- 源码语义根继续指向 58Server；
- 生成产物记录两种来源的逻辑路径和 SHA；
- CI 禁止未声明的旧数据根参与正式生成。

即使抽样文件在两个目录中当前 SHA 相同，也不能依赖这种偶然一致；权威路径必须唯一，否则后续更新会再次产生不可见漂移。

### 2.4 任务侧三层数据契约与门禁（已落地）

为同时保证"随时可玩"和"随时可改"，任务脚本数据采用三层结构，均加载进同一 `quest_scripts` 容器，由 `QuestEngine.selectScriptQuests` 按 patch > retail > legacy 三级优先选择（同级后加载者覆盖）：

| 层 | 位置 | 维护方式 | 说明 |
| --- | --- | --- | --- |
| 生成层 | definitions/compact/quests/scripts/zz_retail_*.xml | 机器产物，只读 | 手改会被门禁测试拦截；重跑生成器必覆盖 |
| 补丁层 | definitions/compact/quests/patches/ | 人工维护 | 条目须 `patch="true"`，按 quest id 整条覆盖生成层 |
| 旧实现 | 其余 scripts/*.xml 与 Java QuestHandler | 逐步退役 | retail/patch 缺席或被禁用时兜底 |

配套机制：

- 止血开关：`gameserver.quest.retail_disabled_ids`（CustomConfig，逗号分隔 quest id）。命中集合的 retail 条目在选择期跳过，自动回退旧 XML 或 Java 处理器；patch 条目不受禁用影响。
- 手改检测：生成 report 记录 `output_sha256`；`RetailQuestRuntimeSmokeTest.generatedOutputMatchesReportHash` 比对产物哈希，任何机器可跑，不依赖真端数据源。
- 所有权对账：`QuestEngineScriptRoutingTest.allXmlQuestDefinitionsTransferToScriptRegistry` 断言全量选择结果中 retail/legacy 条目数与 report `ownership` 一致。
- 补丁层约束：`RetailQuestRuntimeSmokeTest.patchLayerEntriesAreExplicitAndUnique` 强制 patch 标记齐全、同 id 不重复（目录内文件顺序与文件系统相关，重复即不确定覆盖）。
- 生成侧陈旧检测：`generate_retail_simple_quests.py --check`（需本机真端数据源）。

行为修改的三条合法路径：真端数据本身有 → 改生成器规则（惠及整个能力族）；单任务偏离真端 → 补丁层 override；全新私服内容 → 手写 `DataDrivenQuestData` XML（legacy 层）。

### 2.5 引用闭包能力族（任务切片已闭合）

`blocked_reference_resolution` 198 的构成：`_faction_` 102、`_challengetask_` 86、`_area_` 8、其他 2。按族推进：

- **`_faction_`（已闭合）**：真端 simple 表的 `acquired_npc_name = _faction_` 表示任务由 NPC 势力每日池发放（运行时 `NpcFactions.sendDailyQuest` → `QUEST_DATA.getQuestsByNpcFaction`，链路完整）。生成器新增权威源 `npcfactions_quest.xml`（436 条，quest_id→势力+周几），闭包校验 = 任务在真端势力表 ∧ `quest_data.npcfaction_id` 指向已加载势力；通过则 start 置空（`start_npc_ids="0"`，照旧 XML 形态），不通过则以 `blocked_faction_binding` 隔离。
- **`_challengetask_`（已闭合）**：任务由挑战任务列表（军团/城镇，`ChallengeTaskService.buildTaskList`）发放。生成器新增权威源 `challenge_task.xml`（123 组 / 159 任务），闭包集 = 真端挑战表 ∩ AionEmu `challenge_tasks.xml`（两侧已对齐）；simple hunt 84 条与 data_driven PVP 2 条 start 置空，不满足则 `blocked_challenge_binding` 隔离。哨兵解析已泛化为 `resolve_sentinel_starts`。
- **`_area_` 8（已闭合）**：引用图从真端 `Worlds/*/world.xml|world_N.xml` 的 `questscript_area` 反查任务集合并保留源文件 SHA；8 条任务全部解析到 4 个明确区域目标，运行投影继续复用 `ai-areas.xml`，不再以私服 `invasion_world` 反向定义来源。
- **其余 2 条（30720/30723，已闭合）**：`reward_npc_name=magician_apprentice` 按集合引用解析为 804897/804898；`item_order` 扩为 `end_npc_ids` 集合并注册两个交还 NPC，不再强行猜测单值 race 映射。

任务引用切片收官口径：331 条引用全部 resolved（area 8、challenge_task 150、faction 171、npc_quest_alias 2），unresolved、ambiguous、fallback、rejected 均为 0。该口径不外推为副本、AI 和 world 全局引用图完成。

另：原计划"执行器 schema 预留非线性能力位"经审计放弃——真端 `data_driven_quest.xml` 即线性模型，分支/失败边/跨任务状态只存在于 ScriptDLL，预留即猜测；且 XSD 加 optional 属性天然向后兼容，无 breaking 风险。动作槽缺口（delay/timer/message/enter_instance）经交叉验证当前 xml_owned=0，留到对应 Java handler 退役时配对实施。

### 2.6 副本行为数据化可行性审计（2026-07-26，全部实测）

审计对象：`compact/instance/coverage.xml`（140 世界 × 10 维所有权）、真端 `Map/Worlds`（258 目录）、AionEmu 副本运行时与 handler 源码。

**十维所有权全景**（140 世界）：

| 维度 | 已数据化主体 | HANDLER 残留 |
| --- | --- | --- |
| spawn | RETAIL_DATA 102 | 15 |
| ai | RETAIL_PATTERN 98（+SCRIPT_AI 19） | — |
| path | RUNTIME_PATHING 101 + RETAIL_DATA 31 | 4 |
| entry | RETAIL_PORTAL 65 + MATCHMAKER 28 | —（PORTAL_REJECTED 6） |
| reward | RETAIL_DROPS 55 + SETTLEMENT 35 | 14 |
| **stage** | RETAIL_PATTERN 45 | **61** |
| **exit** | INSTANCE_EXIT_DATA 39 | **58** |
| **recovery** | STATELESS 49 + QUEST_STATE 17 | **56** |
| door | RETAIL_DATA 38 | 38（进行中的并行工作线） |
| score | SETTLEMENT/N.A. | 35 |

**behavior=HANDLER 86 世界的维度组合矩阵**：26×全五维（重型 PvPvE/竞技）、12×五维全空（薄 handler，各余一小撮异构杂务：物品清理、宝箱选择、击杀桥接——`behavior_source` 已逐个精确圈定）、10×仅 exit、8×stage+recovery、其余混合。

**真端证据面结论**：`Worlds/*/world.xml` 只含静态资源（direct_portals/npc_spawn/way_point/区域盒），与已数据化维度一一对应；stage/score/recovery 行为只存在于 ScriptDLL——与任务侧结论一致，重型副本行为化 = ScriptDLL Pattern 恢复，成本 O(副本组)。

**批量杠杆排序（下一族 = exit）**：

1. **exit 族（第一批已落地）**：基类 `GeneralInstanceHandler.onExitInstance` 默认空实现；精确解析后 27 个 override 中**13 个纯样板**（单语句 `moveToInstanceExit` 且 exit 数据在位）已删除，基类改为数据守卫默认（有 `INSTANCE_EXIT_DATA` 条目时传出，无条目保持无操作）——行为逐字节等价，且有数据无 override 的世界顺带修复"离开按钮无反应"。剩余 14 个 override 含附加逻辑（战场登记/物品清理），属 stage/score 维度残留，随对应族迁移消亡，**不做化妆式改写**。
2. **score/settlement 族（35）**：`SETTLEMENT_SERVICE` 35 先例 + `npc_scores.xml` 权威表在位。补充实测：score=HANDLER 35 与 reward=SETTLEMENT 35 完全重合（结算已服务化、计分过程仍手写）；分发管线**已在位**（`RetailPatternAI2` 死亡路径 → `InstanceHandler.onRetailNpcScore`，真端 `npc_scores.xml` 2835 条已进 `RETAIL_AI_DATA`）。

   **复审结论（2026-07-26，本族实质已闭合）**：早期"11/35 已对接"是按叶子文件 grep 得到的低估——多数副本 handler 是薄壳，实现在共享基类（`PvPArenaInstance` 5 世界、`HarmonyArenaInstance` 3、`CrucibleInstance` 2 等）。按继承链复查并剔除 `GeneralInstanceHandler` 的 `return false` 空壳后：
   - **18 个世界**已走共享 `onRetailNpcScore` 契约；
   - **11 个世界**已通过私有 helper（`retailScore()` / `RETAIL_AI_DATA.getNpcScore`）取真端分值，仅分发未走契约；
   - 余 6 个中 IdgelDome 只维护 `InstanceScoreType` 相位状态机（属 stage 维度）、ShugoVault/CrucibleSpire 等**根本无 NPC 计分**（标签过度归因，计分在 reward/结算服务）。

   即 **29/35 计分世界的分值已取自真端数据**；`score:HANDLER` 标签衡量的是分发归属而非数据化程度。剩余工作是把私有 helper 路由并入共享契约——纯内部重构，无数据化收益，且需在 680–773 行 PvP handler 中做行为重组（双计风险，无测试保护），按"证据不足不猜测 / 只做 O(能力族) 的活"暂不执行。杠杆因此重排：**副本侧唯一实质缺口为 stage/recovery**。
3. **stage+recovery 主战场（61+56）**：按副本组分批 ScriptDLL Pattern 恢复（RETAIL_PATTERN 45 先例证明管线可行）；recovery 依赖 stage 状态模型，随族推进。

   **stage 真实归属复测（2026-07-26）**：61 个 stage=HANDLER 中 **20 个已委托 `RetailConditionSpawnEngine`**（状态交由真端条件刷怪引擎，handler 只做桥接），41 个为自有状态机。41 个里 PvP 竞技一族（`PvPArenaInstance`/`HarmonyArenaInstance` 共 8 世界）的 stage 是 PvP 回合状态（`InstanceScoreType` + 计时），真端无对应 XML 表述。

   **已识别的具体族：定时宝箱房（5 个结构克隆）**——`SulfurTreeNest`(300060000)/`LeftWingChamber`(300080000)/`RightWingChamber`(300090000)/`CarpusIsleStoreroom`(300050000)/`HamateIsleStoreroom`(300070000) 逐行同构，差异仅 `@InstanceID`、宝箱 ID 数组、状态键前缀；形态为"进入计时区 → 15 分钟 → despawn 宝箱"。
   - 宝箱侧**有真端权威标记**：`npcs.xml` 的 `ai_name = AI_IDREWARD_FobjDropdice`（214804 为 `ABRwd_DespawnBox`），可据此从真端刷怪数据推导 ID，无需手写数组。
   - 计时侧**无表级权威**：`world_timeattack`(19 条，计分型时间攻击)、`aitimeconditions`(4 条，周期型) 均不覆盖；`ABRwd_DespawnBox` 仅在 `MainServer_ScriptDLL64/classes/NPC/IAIScriptNpcImp.cpp` 见到 AI 类注册，取值需 **AI 类级反编译**（不同于任务侧 `fun_NNN.cpp` 的证据流）。
   - 计时权威**穷尽搜索后确认不存在**（2026-07-26 复核，四层权威全查）：`npcs.xml` 宝箱条目只有 `talk_delay_time`（交互延迟）无生存期字段；世界 `world.xml` 的 `limitareas`/`recallareas` 仅布尔标志无时长；`instance_creation`/`instant_dungeon_define`/`instance_restrict`/`instance_cooltime` 命中这 5 个世界但**无任何时间字段**；`world_timeattack`(计分型)、`aitimeconditions`(周期型) 属别的机制。
   - **前一轮"待 AI 类反编译流解锁"的判断已被证伪**：`ABRwd_DespawnBox` 在 `IAIScriptNpcImp.cpp` 仅是名字写入缓冲并注册到**通用 vftable**，无专用实现可恢复——该工具链投入对本族无效，不应据此排期。
   - 处置：现有 900 秒为私服取值，不可反向定义真端语义。该族**永久阻塞于计时权威缺失**（原因码 `blocked_no_timer_authority`），handler 保留兜底；除非出现新证据源（如客户端资源或另一版本数据），否则不数据化。
4. **薄 handler 12 个**：异构杂务，穿插长尾（同 41 gap handler 性质），`behavior_source` 即退役验收单。
5. door 38：并行工作线进行中，避让。

**gap handler 退役 backlog 增强（副作用预筛，2026-07-26）**：audit 的 mechanisms 正则存在盲区（未覆盖 setFlightTeleportId/scheduleRespawn/onDelete 等调用）——对全部 gap handler 源码宽筛后：**35 个纯净可退役**（6 个 Dialog-only + 29 个 Dialog+Kill），**4 个有据搁置**（2443 飞行传送、2493/3712/4712 对话后 despawn——执行器 `delete_action_target` 仅覆盖 ACTION 物件流且无 respawn 调度语义，模型缺口，留待能力位补齐）。已退役：3044、2449（`COMPILED_SIMPLE_TALKS` 证据登记管线，单个 ~20 分钟）；2767、1691（**零成本型**：真端表已有完整定义，删 handler 即由通用族接管）。

**零成本退役的批量试错法**（实测校准）："真端表内有定义"≠ 闭合——17 个候选批删后仅 2 个被接管（2767/1691），其余 15 个被族解析的 unsupported/证据要求挡住并掉入 isolated。正确管线：批删候选 → 一次生成 → 以 **isolated 不变**为硬验收 → 恢复全部失败者。一次生成同时验证整批，但候选须以"删后隔离零增"为唯一放行标准，不得以隔离换退役数。

追加搁置：2513（三选一分支且每站对话页不同 1011/1352/1693——`DataDrivenQuest` step 单 `dialog_id` 无法表达 per-NPC 对话页，模型能力位缺口）。搁置清单合计 5：2443、2493、2513、3712、4712。

**族级杠杆已耗尽（剩余 24 个 gap handler 的结构普查，2026-07-26）**：对全部剩余 gap handler 复现五个族解析器后：

| 分类 | 数量 | 处置 |
| --- | --- | --- |
| 真端 simple/data-driven 表中**无任何条目** | 20 | 只能走 `COMPILED_*` 证据登记（O(1)/个），无族路径 |
| data_driven 表中但含 `value10_progress_`（计时器槽） | 2（13945/23945） | 执行器计时器支持 0/33，能力位缺口 |
| `talk_npc1` 与交还 NPC 不同名 | 2（14200/24155） | 见下 |

`talk_npc1` 不同名的 19 条（10 条在 base）曾被视为"中间站族"候选，实测否证：14200/0x3778 的 Pygmalion 挂**通用进行中掩码 0x40000000**（非 var 门控），交还 NPC Atropos 挂 phase 3 mask=3 + phase 4；AionEmu handler 对该站也只 `setQuestVar(0)`（不推进）——即**信息站而非步骤**。是否推进需逐任务判定，不构成族规则，停止该方向。

结论：gap handler 的批量路径到此为止，剩余为 O(1)/个的证据登记工作或能力位依赖，迁移主线回到副本侧能力族（§2.6 杠杆排序）。

**表外 handler 的形态普查与击杀计数解码阻塞（2026-07-26）**：对 14 个"真端表无条目"的 handler 逐个测绘注册面后，形态高度分散（起始 NPC 1 个、对话站 1–9 个、击杀组 0–4 个、进度变量 1–4 个），无共同形态可建单一新族。其中纯对话链者可直接走 `COMPILED_SIMPLE_TALKS`（1472 已按此退役）；**击杀型（约 13 个）阻塞于计数解码**：
- 已验证可读的相位登记形如 `FUN_180cb3070(&DAT_x,&DAT_y,quest_hex,phase,mask,0)`，任务侧全部退役均据此；
- 击杀进度却有多种调用形态：`FUN_180caa850(quest_hex,...,0,1,0xf,2)`（3031，可读出 15/12 两路并行计数）与虚表调用 `(*plVar+0x110)(plVar,quest_hex,3,3,1,1,0,0,0,0)`（11227，参数语义未知）；
- 后者未建立可靠读法前，任何计数取值都是猜测，故相关 handler 保持隔离（原因码 `blocked_kill_progress_decode`）。

**`+0x110` 解码尝试与负结果（2026-07-26，已执行）**：
1. 样本面：全 fun 目录提取到 **154 个任务**含 `(*plVar+0x110)(plVar, quest_hex, a, b, c, 1, 0,0,0,0)` 调用。
2. **表级 ground truth 结构上不存在**：这 154 个与 `Quest_SimpleHunt.xml`(1859 条含计数) 交集 **0**，与 `data_driven_quest.xml` 的 Hunt 步骤(1188 条)交集也是 **0**——显式进度调用恰好只出现在"表外编译型"任务上，两套机制互斥。
3. 假设 `(current, total, completeFlag)`：由 7 个任务的成对调用强支持（如 0x4e7b `(3,6,0)`+`(6,6,1)`、0x40e `(6,7,0)`+`(7,7,1)`）。
4. **但不变式 `flag==1 ⟺ current==total` 有 18 个真实反例**（如 0x30ea `(1,5,1)`、0x4aa `(1,9,1)`）；且已排除"混入其他调用点"的解释——18 个 hex 全部是 `quest.xml` 中的真实任务 ID。
5. 结论：`(a,b)` 亦可读作 var 推进区间（startVar→endVar），两种读法给出不同数据，**该调用的解码未建立**。

**第三方证据源已找到并验证：客户端 Dialogs（2026-07-26，解除本阻塞）**
`/Users/mc/PycharmProjects/unpak/data_unpacked/Dialogs`（2686 目录/文件，116MB）内每任务一份 `*[Qq]<id>.html`，其中 `<HtmlPage name="quest_summary">` 给出**玩家可见的有序目标清单与显式计数**，格式 `目标[%dic:STR_DIC_M_<retail_npc_name>]([%N]/COUNT)`：
- **交叉验证成立**：3031 客户端摘要为 `(.../15)` 与 `(.../12)`，与 ScriptDLL `FUN_180caa850(0xbd7,...,0xf,2)` / `(...,0xc,3)` 读出的 15 / 12 **精确一致**；即客户端计数与真端脚本互证。
- **覆盖率实测**：22 个剩余 gap handler **全部有 quest_summary**，其中 **14 个含显式计数**（2114 10/10、3031 15/12、3502 五个 1、18208/18209/28208/28209 5+1、14200 3、24155 3、13945/23945 2、3210/4210 1/1、3056 1），其余 8 个为单目标或非击杀式（计数隐含为 1）。
- 摘要同时给出**目标顺序**与 NPC/物件的真端名（`STR_DIC_*_<name>`），可直接映射到 npcs.xml。
- 权威定位：客户端属权威顺序第 3 层（任务存在性权威）的自然延伸——它是玩家可见契约，且此处与第 2 层 ScriptDLL 互证，非私服产物。

因此 `blocked_kill_progress_decode` **不再是有效阻塞理由**：击杀计数改由客户端摘要提供，`+0x110` 无需解码。下一步实施：在生成器中新增客户端摘要解析（目标清单 + 计数 + 名称映射），作为 `COMPILED_*` 族条目的计数来源与自动校验项。

**族级杠杆：`talk_npc1` 冗余判定（一条规则退役 12 个 handler）**——上批 16 个失败候选中 14 个卡在同一字段。审计真端 `Quest_SimpleHunt.xml`：47 条含 `talk_npc1`，其中 **28 条 `talk_npc1 == reward_npc_name` 且无其他附加字段**（纯重复点名交还 NPC），19 条不同名（真中间站）。ScriptDLL 双样本证实（3329/0xd01：起点 Dalanius 挂 phase 0，phase 3×2 与 phase 4 同挂 Zinas 同一 DAT；16900/0x4204：Telemachus phase 0，Castor phase 3×2+4），**同名即无独立中间站**，`monster_hunt` 模板的 start→kill→end 已完整表达。`retail_hunts` 据此加窄口径规则：同名时该字段视为冗余，不同名仍按 unsupported 隔离。收益：java_handlers 813 → **801**（3329/13702/16900-16903/23702/24112/24151/26905/26906/26908），isolated 保持基线 17，生成定义与被删 handler 注册逐字段一致。

## 3. 当前方法为何走入误区

### 3.1 任务迁移正在积累按 ID 编译逻辑

当前正确的运行骨架已经存在：

~~~text
DataDrivenQuestData
        │
        ▼
DataDrivenQuest
        │
        ▼
ScriptRegistry
        │
        ▼
QuestEngine
~~~

ScriptQuest 直接复用现有 XML QuestHandler，没有复制一套对话、击杀、物品和区域运行时，这一方向应保留。

问题主要在生成侧：

- generate_retail_simple_quests.py 当前约 3,192 行；
- 对应生成器测试约 1,305 行；
- 存在约 49 组 COMPILED_* 常量族；
- 许多复杂任务通过“任务 ID + 恢复函数编号 + token”单独编译；
- 每迁移一组任务，都要补常量、判断、测试、报告和所有权例外。

这种方法能保证单个任务证据严格，但无法形成整体迁移能力。根因不是审查太严格，而是“证据规则绑定到任务 ID”，没有先把真端槽位、事件和动作编译成通用操作。

> 状态更新 2026-07-26：生成侧散布点已注册表化。`COMPILED_FAMILIES` 注册表统一驱动
> 各能力族的调用、ID 并集、simple/hunt/data 分桶、通用族账目收回（reclaims）、
> eligible/generated/skipped 统计与 script_sources 清单；`compiled_sink` 按条目 kind
> 与族默认桶分派。新增一族从改 9 处散布点降为 2 处：①写 COMPILED_* 数据与
> compiled_*() 校验函数（推荐参照 compiled_simple_talks 的“证据全在数据表”形态）
> ②在注册表登记一行。“证据规则绑定任务 ID”的根因（人工逆向锚点无法自动推导）
> 仍在，后续按 §5 的通用槽位编译推进；本步先把结构成本降为 O(能力族)。

### 3.2 当前 DataDrivenQuest 只是严格线性子集

当前 DataDrivenQuest 已支持的主要能力包括：

- 启动：TALK、ITEM_PLAY、WORLD_ACTIVE、SENSORY_COMPLETE、部分 ENTER_AREA；
- 步骤：TALK、COLLECT_ITEM、HUNT、ENTER_AREA、ENTER_WORLD、ACTION、ITEM_PLAY、GET_ITEM；
- 副作用：简单物品增删、对话、单影片、传送、目标删除和世界重置。

其数据结构本质上仍是：

~~~text
start -> step[0] -> step[1] -> ... -> reward
~~~

真端任务则包含：

- 多入口；
- 分支和汇合；
- 并行目标；
- 失败边；
- 绝对或相对计时；
- 世界进入、离开和重入；
- 动态刷怪、影片、传送、技能、对象控制；
- 随机、职业、阵营和可选奖励；
- 任务间状态联动。

继续给线性 Step 增加单任务布尔字段，最终仍会回到逐个修补。

### 3.3 当前生成报告已经暴露数据缺口

以下是可行性审计时任务生成报告的初始快照：

| 指标 | 当前值 |
| --- | ---: |
| AionEmu 基础任务 | 6,476 |
| 有可执行所有者 | 6,453 |
| 正式隔离 | 23 |
| 生成 XML | 5,028 |
| 保留旧 XML | 726 |
| 基础 Java Handler | 835 |
| 复杂语义或副作用 Handler | 792 |
| 未解析引用（初始快照） | 198 |
| 不支持字段记录 | 13 |

初始 198 个未解析引用由以下类别组成：

| 引用 | 数量 |
| --- | ---: |
| _faction_ | 102 |
| _challengetask_ | 86 |
| _area_ | 8 |
| npc:magician_apprentice | 2 |

这说明当时的阻塞并不都是“任务太复杂”，其中大量只是引用表没有进入统一闭包。先补阵营、挑战任务和区域引用，比继续手工审查 198 条任务更有效。

> 状态更新 2026-07-27：任务引用图首批已闭合，共 331 条引用全部 resolved，unresolved、ambiguous、fallback、rejected 均为 0；初始 198 个缺口不再是当前阻塞。该结果只代表任务引用切片，不代表副本、AI 和 world 的全局引用图已经完成。

> 状态更新 2026-07-27：统一引用图当前纳入 76,151 条任务、副本、NPC、AI Pattern、路径、门、world 和 instance_creation 引用；68,427 条 resolved，7,645 条 Pattern 缺定义保守 fallback，79 条权威目标缺失显式 rejected，unresolved 和 ambiguous 均为 0。此前 139 条 waypoint ambiguity 已证明是大小写不同的同一 world 被重复扫描，并在输入归一层一次修复。该闭包只覆盖已建模引用族，新引用族仍须先进入统一图再接入运行时。

### 3.4 副本 coverage 是审计结果，不是行为数据

当前已检入 coverage.xml 共 140 个世界：

| 行为分类 | 数量 |
| --- | ---: |
| HANDLER | 86 |
| RETAIL_AI_QUEST | 28 |
| TOURNAMENT | 8 |
| MATCHMAKER | 7 |
| EVENT | 4 |
| EXCLUDED_NON_PRODUCTION | 3 |
| HOUSING | 2 |
| DATA_ONLY | 2 |

仍由 Handler 持有的维度：

| 维度 | Handler 数量 |
| --- | ---: |
| stage | 61 |
| recovery | 56 |
| exit | 58 |
| door | 38 |
| score | 35 |
| spawn | 15 |
| reward | 14 |
| path | 1 |

coverage.xml 能回答“谁负责”，但不能执行：

- 什么事件触发阶段变化；
- 条件变量如何计算；
- 哪个 Spawn Page 被选择；
- 门何时开启；
- 计时如何恢复；
- 分数和奖励如何结算；
- 离图或重启如何清理。

`generate_retail_instance_data.py` 现已从独立真端门引用图、运行时静态门、可达 NPC 和 Pattern 支持派生 door owner，并按 Handler 继承链的真实移动/传送能力纠正无证据的 `path:HANDLER`；其余维度仍从已审计 coverage 保留，运行时目录也尚未检入 generator 可产生的 manifest.xml。因此“生成事实”和“人工审计所有权”已经接通首批维度，但还不是完整十维可重复流水线。

> 状态更新 2026-07-27：`generate_retail_instance_ownership_matrix.py` 及其 JSON 明确降级为 `coverage.xml` 的运行时审计投影，不能作为真端所有权证据。可派生维度必须来自 58Server-new/58Server 的独立源矩阵；无法派生的维度继续保留当前 owner、证据、缺口和删除条件，不再用矩阵一致性代替真端一致性。

### 3.5 根因归纳

当前误区可概括为四点：

1. 从 AionEmu 缺什么开始补，而不是从真端到底有什么开始建模。
2. 把复杂语义绑定到任务 ID 或地图 ID，而不是抽取事件、条件和动作能力族。
3. 先生成运行时小表，导致被丢弃的原始槽位无法再解释。
4. 把“结构能解析”“数据已导入”“运行时已接管”“旧 Handler 可删除”混为同一状态。

## 4. 可行性评估

| 维度 | 结论 | 依据 | 主要约束 |
| --- | --- | --- | --- |
| 原始数据完整性 | 高 | 58Server-new 包含任务、实例、AI、世界、引用和 China 覆盖数据 | 文件量大、存在宽松 XML 和地区覆盖 |
| 语义恢复 | 中高 | 58Server 恢复源码可证明注册、回调、状态和服务调用 | 部分函数仍不可读，不能猜测 |
| 转换能力 | 高 | aion_drop V2 已有无损、SHA、修复、冲突和往返校验 | 默认数据根需要切换 |
| AionEmu 执行能力 | 中高 | Quest、Retail AI、条件刷怪、区域、门户、状态、计时、结算均已有公共实现 | Quest 图、少量动作和引用消费者仍缺 |
| 全自动接管 | 有条件可行 | 能按能力族批量编译并保守回退 | 必须先完成引用闭包与操作语义 |
| 一次性删除全部 Handler | 不可行 | 仍有真端语义缺口和恢复边界 | 只能按行为维度闭合后删除 |

可行性的关键不是建立一个“万能 DSL”，而是把真端已经存在的数据结构无损保存，然后用少量类型化操作连接现有服务。

## 5. “全数据”的准确范围

### 5.1 全量任务数据现状

58Server-new/Map/XML 中与任务直接相关的主要表：

| 表 | 记录数 |
| --- | ---: |
| quest.xml | 10,035 |
| Quest_SimpleHunt.xml | 1,863 |
| Quest_SimpleTalk.xml | 3,152 |
| Quest_SimpleCollectItem.xml | 262 |
| Quest_SimpleUseItem.xml | 160 |
| Quest_SimpleItemPlay.xml | 43 |
| Quest_CombineTask.xml | 574 |
| Quest_SimpleSerialHunt.xml | 16 |
| Quest_SimpleGather.xml | 0 |
| data_driven_quest.xml | 2,492 |
| challenge_task.xml | 123 |
| npcfactions_quest.xml | 436 |
| quest_random_rewards.xml | 817 |
| jobfactions.xml | 4 |

quest.xml 本身约有 235 种标签。只读取 ID、等级、奖励和少量前置条件并不等于读取了任务全数据。

data_driven_quest.xml 中的进度节点，按大小写归一后的主要出现次数：

| 节点 | 次数 |
| --- | ---: |
| Hunt | 1,392 |
| Talk | 692 |
| CollectItem | 564 |
| PVP | 395 |
| EnterArea | 207 |
| ItemPlay | 77 |
| EnterWorld | 68 |
| TalkFOBJ | 33 |

启动类型主要包括：

| 启动类型 | 次数 |
| --- | ---: |
| Talk | 2,044 |
| EnterArea | 233 |
| none | 136 |
| _faction_ | 52 |
| LevelUpLogIn | 24 |
| ItemPlay | 21 |
| EnterWorld | 15 |
| LevelUp | 1 |

value0_progress_ 出现 3,429 次，value1 到 value10 也承载大量数据。已审计任务证明这些槽位会混合承载：

- 影片；
- 世界；
- 传送；
- 绝对坐标和相对刷怪；
- NPC、对象和刷怪页；
- 生命周期；
- 计时参数；
- 状态回退。

因此 value0..value10 不能在导入时直接改名或丢弃。它们必须先作为原始槽位保存，再根据任务类型、节点类型、版本和源码证据编译为类型化字段。

### 5.2 当前副本生成已覆盖的表

generate_retail_instance_data.py 当前读取 17 张主要表：

1. instance_creation.xml
2. instance_restrict.xml
3. instance_cooltime.xml
4. instance_cooltime2.xml
5. matchmaker.xml
6. team_match_maker.xml
7. instant_dungeon_define.xml
8. world_timeattack.xml
9. world_timeattack2.xml
10. infinity_indun_reward.xml
11. instant_dungeon_battleground.xml
12. instant_dungeon_idarenapvp.xml
13. instant_dungeon_tournament.xml
14. luna_indun.xml
15. luna_cost.xml
16. npc_scores.xml
17. instance_bonusattr.xml

这些表主要覆盖准入、冷却、匹配、时间挑战、Luna、锦标赛、计分和增益，尚不足以描述副本内完整行为。

### 5.3 需要统一纳入的副本和世界数据

至少还应纳入：

| 数据 | 当前规模 | 用途 |
| --- | ---: | --- |
| simple_entrance.xml | 152 | 入口、准入和目标世界 |
| direct_portal.xml | common 216 / China 218 | 直达门户和坐标 |
| condspwntimes.xml | 638 | 条件刷怪时间与周期 |
| InAreaObjCtrl.xml | 56 | 区域对象组控 |
| instance_pool.xml | 4 | 副本池和随机选择 |
| instance_scaling.xml | 2 | 人数或等级缩放 |
| world_invasion.xml | 2 | 世界入侵生命周期 |
| Map/Worlds/*/world.xml | 256 | 世界静态对象、出生与区域 |
| world_N.xml | 222 | 条件和普通 Spawn Page |
| world_M.xml | 223 | 额外世界/地图规则 |
| world_N_WayPoint_*.xml | 143 | 具名路径和路线 |

并需要连接以下引用域：

- NPC 模板和 NPC 技能槽；
- Item、掉落和临时物品归属；
- Skill、技能区域和效果；
- AI Pattern、Party 和条件变量；
- World、Area、GroupCtrl 和动态区域；
- Door、Portal、Location Alias 和 Path；
- String、影片和客户端场景；
- Quest、Faction、ChallengeTask 和匹配规则。

### 5.4 AI 数据

当前紧凑 AI 数据约包含：

- 12,799 个 Pattern 记录；
- 68,992 条 Rule；
- 57 类 Event；
- 41 类 Condition；
- 61 类 Action。

现有 RetailPatternAI2 已覆盖绝大多数可达结构。全量数据中仍可见少量当前不执行的操作：

- is_on_time：1 处；
- give_abysspoint：2 处；
- give_money：2 处；
- give_world_score：2 处。

这些记录必须进入无损权威层，但不表示现在就要实现运行时。现有证据显示它们只服务不可达测试 NPC 时，应保持隔离；等真实可达引用出现时再补通用消费者。这正是“全量保存、按闭包执行”的边界。

## 6. 需要补充的公共字段

以下字段属于无损权威层和编译审计层。为避免运行时 XML 膨胀，可通过 manifest 和字典索引复用，不要求每个运行时节点重复写完整来源信息。

### 6.1 来源与版本

| 字段 | 含义 |
| --- | --- |
| dataset_id | 数据集稳定标识，例如 retail-5.8-cn |
| snapshot_id | 一次完整生成快照 ID |
| schema_version | 权威层或 IR 版本 |
| source_root_id | 逻辑根标识，不把本机绝对路径当业务数据 |
| source_file | 原始文件名 |
| source_path | 相对数据根的逻辑路径 |
| source_region | common、China 或其他地区 |
| source_sha256 | 原文件 SHA-256 |
| source_record | 原始记录键或序号 |
| source_line | 可获得时记录原始行号 |
| source_slot | value0..value10 或原始字段名 |
| source_order | 原节点、属性或重复字段顺序 |
| generator_version | 生成器版本或 Git 提交 |

### 6.2 原始值状态

| 字段 | 含义 |
| --- | --- |
| presence | missing、empty、value，三者不能合并 |
| raw_value | 原始文本，保留 1 与 1.0 的区别 |
| normalized_value | 仅供解析使用的确定性规范值 |
| raw_type | attribute、element、text、slot |
| occurrence | 同名重复字段的出现序号 |
| raw_children | 嵌套节点的有序内容 |

### 6.3 身份与引用

| 字段 | 含义 |
| --- | --- |
| stable_id | 当前领域的稳定数值或内容 ID |
| name | 真端原始名称 |
| id | 真端原始数值 ID |
| reference_type | npc、item、skill、world、area、path 等 |
| reference_name | 原始引用文本 |
| resolved_id | 确定解析出的目标 ID |
| resolved_source | 目标来自哪张表和哪一行 |
| resolution_scope | 全局、世界、地图实例、任务或 Pattern |
| resolution_status | resolved、missing、ambiguous、disabled |
| candidate_ids | 歧义时保留全部候选 |

### 6.4 冲突、修复和语义证据

| 字段 | 含义 |
| --- | --- |
| conflicts | 地区覆盖、重复 ID、同名多 ID 或来源不一致 |
| repairs | 宽松 XML 的确定性修复记录 |
| repair_type | 标签大小写、注释、残缺标签、重复属性等 |
| original_digest | 修复前片段摘要 |
| semantic_evidence_path | 58Server 恢复源码的逻辑路径 |
| semantic_evidence_symbol | 类、函数或回调标识 |
| semantic_evidence_sha256 | 语义证据文件 SHA |
| mapping_rule | 使用的通用语义映射规则 |
| confidence | proven、derived、unknown |

unknown 只能进入权威层和隔离报告，不能进入可执行运行时投影。

### 6.5 执行公共字段

| 字段 | 含义 |
| --- | --- |
| scope | player、party、quest、instance、world、npc |
| version | 操作或记录版本 |
| enabled | 地区覆盖和功能开关后的有效状态 |
| event | 触发事件 |
| conditions | 类型化条件列表 |
| actions | 类型化动作列表 |
| state_reads | 操作读取的状态集合 |
| state_writes | 操作写入的状态集合 |
| error_policy | reject、rollback、retry、ignore-if-absent |
| transaction_group | 需要原子执行的动作组 |
| idempotency_key | 奖励、刷怪、计分和结算的稳定幂等键 |
| deadline | 绝对期限或相对持续时间 |
| recovery_policy | 重启、掉线、重入后的恢复方式 |
| cleanup_policy | 完成、失败、离图和销毁时的清理方式 |

### 6.6 字段补充优先级

| 优先级 | 必须先补的字段 | 原因 |
| --- | --- | --- |
| P0 | 来源、地区、SHA、record/slot/order、presence、raw_value、引用状态、冲突和修复 | 没有这些字段就无法证明数据无损，也无法安全重编译 |
| P0 | event、condition、state_reads/state_writes、action、error_policy | 决定一条数据是否可以执行 |
| P0 | resolved_id、resolution_scope、candidate_ids、semantic evidence、confidence | 阻止名称误绑和未知语义接管 |
| P1 | 状态图、失败边、计时、transaction_group、idempotency_key、recovery_policy | 支持复杂任务、副本阶段和异常恢复 |
| P1 | reward group、随机权重、倍率应用点、结算账本 | 防止奖励数值和重复发放错误 |
| P2 | 压缩字典、统计索引、反向查询和可视化字段 | 只影响体积和审计效率，不应阻塞正确性 |

## 7. 任务数据需要补充的字段

### 7.1 身份与基础分类

- quest_id、name、category、type、subtype；
- mission、repeatable、daily、weekly、event、test、disabled；
- min_level、max_level、recommended_level；
- race、class、gender、faction、job_faction；
- world、zone、package、server_condition、地区启用状态；
- 任务显示、追踪、共享和自动接取属性。

### 7.2 接取与前置条件

- 已接、未接、已完成、未完成任务条件；
- all、any、not 组合；
- 任务组和 CombineTask；
- 等级、种族、职业、性别、阵营；
- 物品存在、不存在、数量、装备状态；
- 世界、区域、队伍、军团和副本状态；
- 挑战任务和阵营任务引用；
- 重复次数、周期、冷却、限次恢复；
- 前置失败时的对话和错误结果。

### 7.3 启动事件

- Talk、TalkFOBJ；
- ItemPlay、UseItem、GetItem；
- Hunt、PVP；
- EnterArea、LeaveArea；
- EnterWorld、LeaveWorld；
- LevelUp、Login、LevelUpLogIn；
- MovieEnd、TimerEnd、SkillUse、ObjectAction；
- Faction、ChallengeTask 和系统事件；
- 多启动入口的优先级、去重键和接取方式。

每个启动事件都应保存：

- 事件类型；
- 目标引用；
- 原始对话或动作 ID；
- 条件；
- 状态前置；
- 成功节点；
- 失败结果；
- 是否自动接取。

### 7.4 进度图

完整任务不能只保存 step 数组，应在权威 IR 中表达有向图：

- graph_entry；
- node_id；
- node_type；
- node_state；
- event；
- conditions；
- actions；
- outgoing_edges；
- success_edge；
- failure_edge；
- timeout_edge；
- cancel_edge；
- parallel_group；
- join_policy；
- counter_id、target、amount；
- repeat、loop_guard；
- terminal_status。

运行时第一阶段仍可把单路径图编译为现有 DataDrivenQuest 线性 Step。只有真实数据要求分支、并行或失败边时，才最小扩展 DataDrivenQuest；不能为了未来可能性预建另一套图 VM。

### 7.5 进度节点数据

需要完整覆盖：

- Talk、TalkFOBJ；
- Hunt、SerialHunt、KillInWorld、KillRanked；
- PVP、阵营或排名击杀；
- CollectItem、Gather、WorkOrder；
- ItemPlay、UseItem、GetItem、EquipItem；
- EnterArea、LeaveArea、EnterWorld；
- Action、HAction、功能对象；
- 影片开始和结束；
- NPC 感知、目标丢失、到达目标；
- 计时开始、暂停、到期和回退。

每个节点至少包含目标集合、数量、计数共享方式、世界范围、队伍归属、事件过滤、状态读写和成功/失败边。

### 7.6 value0..value10 原始槽位

每个槽位必须同时保存原始和解析结果：

| 字段 | 含义 |
| --- | --- |
| slot_index | 0..10 |
| slot_name | 原始字段名 |
| presence | missing、empty、value |
| raw_value | 原始文本 |
| source_order | 槽位顺序 |
| parser_key | 任务类型 + 节点类型 + 版本 |
| parsed_type | int、float、bool、enum、reference、coordinate、duration 等 |
| parsed_role | movie、world、npc、spawn_page、timer 等 |
| parsed_value | 类型化值 |
| unit | ms、s、count、degree 等 |
| reference | 解析后的目标 |
| evidence | 源码或数据结构证据 |

任何已出现但没有 parser_key 的非空槽位都必须阻断该任务的运行时接管。

### 7.7 动作和副作用

- 任务状态建立、推进、回退、完成、失败、取消；
- 变量设置、增加、清零和跨任务状态；
- 给物品、扣物品、检查物品和工作物品清理；
- Spawn、Despawn、Spawn Page、相对目标和绝对坐标；
- 传送、实例进入和位置恢复；
- 播放影片、系统消息、对话和 HAction；
- 使用技能、施加或移除效果；
- 启用区域、门、Portal、动态对象和场景状态；
- 启动、取消和恢复计时器；
- 任务完成后的清理和回滚。

物品、奖励、传送、刷怪和状态推进等组合动作需要 transaction_group。不能先扣物品失败后仍推进状态，也不能重放事件时重复发奖励。

### 7.8 奖励

- 固定奖励；
- 可选奖励；
- 随机奖励；
- 职业、种族、性别和阵营奖励；
- 扩展奖励；
- 重复奖励；
- EXP、金币、AP、GP、DP、CP；
- Item、Title、Recipe、Skill；
- 背包扩展和功能解锁；
- 概率、权重、选择数量和互斥组；
- 服务器倍率应用点；
- 奖励账本和幂等键。

随机奖励必须引用 quest_random_rewards.xml 等真端表，不能在生成器中提前随机或只保留一次抽样结果。

### 7.9 失败、清理和恢复

- timeout、death、logout、leave_world、abandon；
- 状态回退节点；
- 临时物品和效果清理；
- 动态 NPC 和对象清理；
- 影片或传送中断；
- 玩家掉线重入；
- 服务重启恢复；
- 重复事件防重；
- 已完成但奖励未发放的补偿。

## 8. 副本数据需要补充的字段

### 8.1 创建、入口和准入

- creation_id、world_id、world_name；
- entrance、simple_entrance、direct_portal；
- 起点别名和坐标；
- 最小/最大等级；
- 最小/最大人数；
- solo、party、alliance、race、class；
- 前置任务、物品、费用和 Luna；
- matchmaker、team_match、tournament；
- 入场次数、冷却、同步冷却和重置周期；
- 开放日历、服务器条件和活动窗口；
- 拒绝原因和客户端提示。

### 8.2 创建页、出生和随机池

- spawn_page；
- 静态 NPC、对象和 Door；
- condition_spawn；
- condspwntimes；
- Party、随机 Party、权重和互斥池；
- instance_pool；
- initial_delay、respawn、lifetime；
- 坐标、heading、move area、waypoint；
- owner、scope、despawn 条件；
- 动态出生稳定键；
- 服务重启后的恢复规则。

### 8.3 AI 和事件

- NPC 到 Pattern 的映射；
- Pattern、Rule、Event、Condition、Action；
- NPC 技能槽和技能模板；
- target selector；
- timer；
- sensory area；
- condition variable；
- NPC Party；
- walker 和 waypoint；
- unsupportedReason 和缺失引用；
- 回退 AI 的所有者和删除条件。

### 8.4 区域、路径、门和门户

- Area、SkillArea、GroupCtrl；
- 感知区、复活区、限制区；
- WindPath、动态碰撞、Jump 和 WindBox；
- Door ID、脚本 ID、初始状态和状态变化；
- direct portal、普通 portal 和 location alias；
- 路径名、节点顺序、spawn page 和世界范围；
- 影片、场景状态和客户端消息；
- 缺 GEO/PATH 时的明确失败策略。

### 8.5 状态和阶段

- 变量定义、类型、默认值和作用域；
- world variable、instance variable、NPC local state；
- 生产者、消费者和写入时机；
- stage、phase、round、wave；
- 条件表达式和优先级；
- 状态迁移图；
- 绝对 deadline、相对 timer；
- complete、fail、abort；
- 重入和重启恢复。

变量只有消费者没有生产者，或 Pattern 写入未声明变量时，必须阻断相关闭包，不能由运行时默认为 0 后继续接管。

### 8.6 计分、排名和结算

- NPC 分值；
- PvE、PvP、采集、区域和对象计分；
- 阵营、队伍和个人分数；
- 幂等事件键；
- 时间奖励和扣分；
- 排名阈值；
- 胜负、平局和超时；
- 基础奖励和服务器倍率；
- 在线、离线和邮件发放；
- 结算状态机；
- 防重复账本；
- 重启重放。

### 8.7 离开、销毁和恢复

- 正常离图；
- 显式退出；
- 登出；
- 掉线重入；
- 踢出和超时；
- 队伍解散；
- 实例空置和销毁；
- 临时物品、效果、召唤物和动态对象清理；
- 状态持久化；
- deadline 恢复；
- 未完成结算补偿。

## 9. 需要补充的引用图

引用解析不应散落在各生成器中。应统一生成有向引用图：

~~~text
source record
  └─ field/slot
      └─ raw reference
          ├─ resolved target
          ├─ ambiguous candidates
          └─ missing target
~~~

至少覆盖以下引用类型：

- quest、challenge_task、combine_task；
- faction、npc_faction、job_faction；
- npc、npc_name、npc_party；
- item、random_reward；
- skill、npc_skill_slot；
- world、zone、area、group_controller；
- spawn_page、condition_spawn、instance_pool；
- path、waypoint、location_alias；
- portal、direct_portal、door；
- string、message、movie、cutscene；
- matchmaker、cooltime、score、reward；
- AI Pattern、condition variable。

每条引用需要记录：

- 来源记录和字段；
- 原始名称或 ID；
- 解析作用域；
- 候选集合；
- 唯一目标；
- 是否可达；
- 是否影响运行时接管；
- 失败原因。

全局数据允许保留 disabled 或测试记录的未解析引用，但任何准备接管的任务、副本或 Pattern，其可达闭包必须是零 unresolved、零 ambiguous。

## 10. 类型化中间表示

### 10.1 为什么需要 IR

原始真端 XML 适合完整保存，不适合直接由 AionEmu 运行：

- 字段名和大小写不统一；
- value 槽位依赖上下文；
- 名称引用需要解析；
- 多表共同描述一个行为；
- 地区覆盖需要先计算；
- 运行时不应解析数百兆宽表。

当前 AionEmu XML 又过早压缩了语义。因此中间需要一个编译期 IR，用于连接两者。

IR 不是新脚本语言，也不直接面向策划手写。它应由生成器确定性产生。

### 10.2 最小操作模型

每个可执行单元只需要五类核心结构：

1. Event：什么时候触发。
2. Condition：当前状态是否允许。
3. Transition：状态从哪里到哪里。
4. Action：调用哪个已有服务做什么。
5. Result：成功、失败、重试、回滚和后续边。

建议的动作通用字段：

| 字段 | 含义 |
| --- | --- |
| op | 稳定的动作类型 |
| args | 类型化参数 |
| target | player、npc、party、instance、world |
| scope | 状态和对象的查找范围 |
| preconditions | 执行前条件 |
| write_set | 状态写入集合 |
| transaction_group | 原子动作组 |
| idempotency_key | 防重键表达式 |
| on_success | 成功边 |
| on_failure | 失败边 |
| error_policy | 拒绝、回滚或重试 |
| recovery_policy | 重启后如何恢复 |

### 10.3 操作目录

操作目录应按真实数据逐步补齐，典型类别为：

- 状态：start、set、increment、reset、complete、fail；
- 物品：check、give、remove、exchange、cleanup；
- 对象：spawn、despawn、select_party、activate_page；
- 移动：teleport、enter_instance、move_path；
- 表现：dialog、message、movie、scene；
- 战斗：use_skill、effect、aggro；
- 世界：door、portal、area、dynamic_collision、world_flag；
- 时间：schedule、cancel_deadline、timeout；
- 计分：add_score、rank、settle；
- 奖励：build_reward、grant、queue_offline；
- 清理：leave、logout、destroy、rollback。

同一个 op 只有在 Quest 和 Instance 的真实语义一致时才共享执行器。不要为了形式统一强行建立一个大基类；可共享现有服务调用，Quest 和 Instance 只提供薄上下文适配。

## 11. 数据驱动运行时如何补充

### 11.1 事件路由

继续复用：

- QuestEngine 的任务事件注册；
- ScriptRegistry 的任务处理器查找；
- RetailPatternAI2 的 NPC Event 回调；
- 实例创建、玩家进入、离开、销毁生命周期；
- 门、区域、对象和计分的现有回调。

生成期把 Event 编译到现有注册 API，不新增全局事件总线。

### 11.2 条件求值

补充一个类型化条件目录即可，按 scope 从现有状态读取：

- Quest：QuestState、玩家状态、库存和任务列表；
- AI：NPC、目标、Pattern 本地变量；
- Instance：InstanceRuntimeState、成员、世界对象；
- World：RetailConditionSpawnEngine 的变量和标志。

条件必须声明输入类型和缺失策略。未知条件不返回 false 后继续，而是使该执行闭包不具备接管资格。

### 11.3 动作执行

动作直接调用已有服务：

| 行为 | 首选现有宿主 |
| --- | --- |
| Quest 注册和状态 | QuestEngine、QuestService、QuestState |
| 任务数据执行 | DataDrivenQuest |
| AI Event/Condition/Action | RetailPatternAI2 |
| 条件刷怪 | RetailConditionSpawnEngine |
| 区域和组控 | RetailAreaEngine、RetailGroupControlEngine |
| 直达门户 | RetailDirectPortalEngine |
| 实例状态 | InstanceRuntimeState |
| 绝对期限 | InstanceDeadlineScheduler |
| 动态实例恢复 | DynamicInstanceManager |
| 计分与结算 | InstanceSettlementService |

缺少动作时应扩展这些共享宿主，而不是为任务 ID 或地图 ID 新建处理器。

### 11.4 状态持久化

- 任务继续以 QuestState 为权威；
- 副本继续以 InstanceRuntimeState 为权威；
- AI 本地状态按已有稳定前缀持久化；
- deadline 使用绝对时间，不能只保存 JVM 内相对定时器；
- 动态 Spawn、计分和奖励写入稳定幂等键；
- 只有真实数据需要新类型时才扩展状态模型。

### 11.5 引用和支持门禁

生成期完成引用解析，运行时只接受数值 ID 和已验证索引。RetailPatternAI2.supports/unsupportedReason 应继续作为硬门禁：

- 不放宽为“能解析 XML 就执行”；
- 不用默认值掩盖技能、路径、区域、变量或消费者缺口；
- 报告必须包含 world、NPC、Pattern、操作和缺失目标；
- 新能力完成后自动减少拒绝集合。

### 11.6 结算和幂等

奖励、计分、动态出生和完成动作都可能因网络重试、重入或 JVM 恢复再次触发。数据必须提供稳定键，例如：

~~~text
quest:<questId>:<playerId>:<completionCycle>:reward
instance:<instanceUid>:<eventType>:<stableObjectKey>
instance:<instanceUid>:<playerId>:settlement:<resultVersion>
~~~

键的具体存储继续复用现有账本和状态，不另建通用工作流数据库。

### 11.7 shadow 和审计

在切换所有权前，数据驱动执行器应支持 audit/shadow：

- 接收同一事件；
- 计算条件结果、状态变化和动作计划；
- 不实际发奖励或修改世界；
- 与当前 Handler 的事件、状态、刷怪、门、计分和奖励结果比较；
- 输出确定性差异。

shadow 通过后再切换 owner，不应直接删除 Handler 后在线试错。

## 12. 从“逐个迁移”改为“能力族迁移”

### 12.1 任务能力族

推荐按以下顺序补齐：

1. 引用族：Faction、ChallengeTask、Area、NPC、Item、Skill、World。
2. 条件族：前置、库存、阵营、世界、重复和冷却。
3. 进度族：TalkFOBJ、PVP、Enter/Leave、Get/Use/PlayItem。
4. 动作族：Movie、Teleport、Spawn、Despawn、Skill、Message。
5. 时间族：Timer、Deadline、Timeout、Reset。
6. 图结构：分支、汇合、并行和失败边。
7. 奖励族：随机、可选、职业、阵营和幂等发放。
8. 恢复族：掉线、离图、重启和回滚。

每完成一个能力族，生成器应自动重新扫描所有任务：

- 新增多少可编译任务；
- 仍有哪些原始字段未映射；
- 仍有哪些引用未解析；
- 哪些 Handler 的行为维度已闭合；
- 输出集合是否确定性变化。

不能再新增 COMPILED_<具体任务> 作为正常路径。确实无法恢复的唯一任务只进入 evidence-gaps 数据，记录缺什么证据，不在其中实现行为。

### 12.2 副本能力族

按 dimension_owners 的行为维度迁移：

1. entry；
2. spawn；
3. ai；
4. path；
5. door；
6. stage；
7. score；
8. reward；
9. exit；
10. recovery。

同一维度的共享根因应一次解决。例如补齐 condition spawn 的变量生产者，应使全部引用同类变量的地图重新通过闭包，而不是只给当前地图的 Handler 增加 setVariable。

### 12.3 人工审查仍然需要，但对象改变

人工审查不再逐条翻译任务代码，而是审查：

- 一条字段映射是否有真端证据；
- 一个操作是否与现有服务语义一致；
- 一个引用解析规则是否唯一；
- 一个能力族的 shadow 差异是否为零；
- 一个所有权维度是否可以切换。

一次审查结果应覆盖所有同形数据。

## 13. 推荐实施阶段

### P0：固定权威源

目标：

- 所有数据生成入口默认切到 58Server-new；
- ScriptDLL 语义入口继续使用 58Server；
- 固定 common + China 覆盖规则；
- 输出 source manifest 和 SHA-256；
- 正式生成禁止隐式使用旧数据根。

验收：

- 每个输入文件来源唯一；
- 同一快照重复生成 manifest 一致；
- 生成报告明确 data root 和 semantic source root；
- AionEmu、aion_drop 不再各自选择不同权威路径。

### P1：建立无损权威层

> 状态更新 2026-07-27：P1 全量验收完成。aion_drop V2 默认且强制使用 58Server-new，
> 不再用 AionEmu 派生技能表反向补全真端引用；两次隔离全量生成均产出 6,311 个文档、
> 284 个 bundle、298 条显式 repairs 和 0 个 conflicts。items/skills/npcs 分别保留
> 128,380/14,494/87,734 行，China/USA/Europe/Japan/Taiwan 五地区层、所有 roundtrip 和 XSD
> 均通过；两轮 manifest、validation、repairs、catalog 的 SHA-256 逐字节一致。

直接复用 aion_drop V2 的既有能力，不新建第三套转换器：

- UTF-16 到 UTF-8；
- 宽松 XML 的确定性修复；
- common/China 双层保留和有效视图；
- 顺序、重复、空值和原始文本保留；
- catalog、manifest、repairs、conflicts；
- 规范化往返校验；
- quests、instances、ai、world 模块化输出。

验收：

- 源记录、属性、元素、顺序、重复和空值往返一致；
- 任何无法确定修复的文件生成失败；
- 不因 AionEmu 未消费字段而删除数据。

### P2：建立全局引用图

> 状态更新 2026-07-27：任务切片已闭合为 331/331 resolved。direct portal 门户切片也已从真端定义、world 端点、AI Pattern 和 DirectPortalMgr 语义生成：218 条定义中 192 条可达、26 条无已知消费者；135 条由 AI Pattern 消费、106 条由 DirectPortalMgr 时段概率消费、49 条重叠。157 条端点与语义闭合的定义已由现有 `RetailDirectPortalEngine` 消费，其中包含 56 条 manager-only 定义；44 条 type 0 门户的普通次数、额外次数、AP 确认/扣费及 `0x117` 使用次数同步包已按 `DirectPortal.cpp` 和 `fun_045.cpp` 批量闭合。其余 35 条仅因端点缺失继续拒绝，特殊字段拒绝已归零。P2 仍未宣告完成，下一步继续扩展副本、AI 和 world 的其他引用族。

> ScriptDLL NPC 对话传送切片已由 `generate_retail_script_transports.py` 从注册表、回调、`npcs.xml`、`WorldId.xml` 和 `Worlds/*/world.xml` 独立生成：150 个注册、142 个唯一回调、114 个传送调用；64 个不是传送，42 个完成事件/静态起点/API/终点路线闭包（80 条端点路线），44 个明确拒绝。生成器同时机械提取全部回调谓词、读取/调用操作并归并为 69 个结构族；42 个闭合路线集中在 11 个族。矩阵仍不解释谓词和非传送调用的业务语义，也不证明运行时消费者选择；因此不会据此自动扩大转换批次。

> AionEmu 门户矩阵只把上述独立证据投影到当前运行时：80 条候选中，7 条已由 `PortalService` 按相同 callback shape、相同 dialog 和相同终点精确承接；10 条因注册闭包不完整拒绝，63 条因没有同事件的运行时先例拒绝。起点对账 45 条匹配、17 条不匹配、18 条缺失，单独记录且不冒充传送语义门禁。

> 统一图状态更新 2026-07-27：当前已建模引用族共 76,151 条，unresolved=0、ambiguous=0；79 条真端目标缺失保持 rejected，7,645 条未定义 Pattern 保持 fallback。P2 的统一图、反向引用和闭合门禁已经建立；后续新增变量、技能槽、门户等引用族时继续扩图，不把“当前图闭合”外推为尚未建模的语义已经存在。

初始 198 个任务引用缺口已经解决，后续继续统一以下引用：

- 名称到 ID；
- 地区和世界作用域；
- 别名；
- 技能槽；
- 变量生产者和消费者；
- 路径、区域、门户和字符串；
- 歧义和缺失报告。

验收：

- 每个可执行闭包零 unresolved、零 ambiguous；
- disabled/test 数据可隔离但必须保留；
- 引用图可从目标反查所有消费者。

### P3：编译类型化 IR

- 保留 value0..value10 原始槽位；
- 由 source shape + event/node type + version 选择 parser；
- 生成 Event、Condition、Transition、Action、Reward；
- 记录通用 mapping_rule 和源码证据；
- 未知非空槽位隔离整条可达闭包。

验收：

- 不以任务 ID 或地图 ID 选择语义；
- IR 重复生成字节稳定；
- 所有运行时字段能反查原始槽位；
- 无静默丢字段。

### P4：补齐现有执行器

- 先生成当前执行器已支持的运行投影；
- 按数据量和可达性补最小缺失动作；
- Quest 继续扩展 DataDrivenQuest；
- AI 继续扩展 RetailPatternAI2；
- Instance 继续扩展已有 Area、Spawn、Portal、State、Deadline、Settlement 服务；
- 不接 JNI，不加载 ScriptDLL64.dll，不新建通用脚本 VM。

验收：

- 每个新增 op 有至少一个真实来源样本；
- 新能力自动覆盖全部同形数据；
- 原有回退集合只减少，不产生新的静默接管。

### P5：生成运行时派生表

运行时数据应是权威 IR 的投影，而不是第二份手工数据：

- Quest XML；
- AI Pattern 和依赖表；
- Instance definitions、limits、matchmaking、rewards；
- condition spawns、areas、portals、paths；
- coverage 和 manifest。

验收：

- 全量重生成后 git diff 可解释；
- --check 通过；
- XSD 通过；
- manifest 与运行时文件一一对应；
- 生成器能复现 dimension_owners 或合并一个小型审计覆盖文件。

人工覆盖文件只允许记录：

- 当前 owner；
- 真端证据；
- 缺口；
- 删除条件。

不能在覆盖文件中编写每张图的行为代码。

### P6：shadow 与能力族切换

- 对同一事件比较旧 Handler 和数据计划；
- 先切单一行为维度；
- 记录差异和拒绝原因；
- 保留明确回退；
- 所有维度闭合后删除 Handler。

### P7：恢复和异常路径

- 任务掉线、放弃、超时；
- 副本重入、空置、销毁；
- JVM 强制终止和恢复；
- deadline 重建；
- 动态 Spawn 去重；
- 计分、掉落和奖励重放；
- 离线奖励。

### P8：全量接管验收

- 任务按能力族和可达闭包完成所有权；
- 140 个 coverage 世界的每个维度都有可验证 owner；
- Java Handler 仅保留有明确真端数据或语义缺口的桥接；
- 每个保留桥接都记录输入、输出和删除条件。

## 14. 验收门禁

### 14.1 数据门禁

- source manifest 完整；
- common/China 覆盖确定；
- SHA-256 可复现；
- 原始字段覆盖率 100%；
- 顺序、重复和空值保留；
- repairs 和 conflicts 非静默；
- 规范化往返通过；
- XSD 通过。

### 14.2 编译门禁

- 非空原始槽位都有 parser 或被隔离；
- 引用闭包完整；
- mapping_rule 不按实体 ID 分支；
- IR 确定性；
- unsupported op 明确；
- 可达闭包没有 unknown confidence。

### 14.3 运行时门禁

- 事件注册完整；
- 条件输入完整；
- 状态迁移合法；
- 动作参数类型和引用完整；
- transaction_group 失败可回滚；
- 幂等键稳定；
- deadline 可恢复；
- supports/unsupportedReason 与生成报告一致。

### 14.4 任务所有权门禁

每个任务至少验证：

- trigger；
- eligibility；
- state/graph；
- progress；
- completion；
- reward；
- cleanup；
- recovery。

任一维度仍由 Java Handler 唯一承担，就不能删除该 Handler。

### 14.5 副本所有权门禁

每张图至少验证：

- entry；
- spawn；
- ai；
- path；
- door；
- stage；
- score；
- reward；
- exit；
- recovery。

coverage.xml 只能在这些维度均有证据后改变 owner。

### 14.6 最终自动验证

- aion_drop 全量或对应模块转换；
- 规范化往返；
- 引用闭包；
- AionEmu 运行时 XML 全量重生成；
- generator --check；
- XSD；
- loader 测试；
- Quest/AI/Instance 能力族测试；
- 重复事件和幂等测试；
- deadline 和 JVM 恢复测试；
- shadow 差异为零；
- 生成 XML、旧 XML、Java Handler 所有权无重叠。

## 15. 风险与处理

| 风险 | 影响 | 处理 |
| --- | --- | --- |
| 把全数据直接加载到运行时 | 启动慢、内存大、语义仍不清楚 | 权威层全量，运行投影按闭包裁剪 |
| value 槽位被提前改名 | 丢失未知语义，无法回溯 | 原始槽位与解析结果并存 |
| common/China 混用 | 地区行为漂移 | 双层保留，单一有效视图 |
| 名称解析取第一个候选 | NPC、区域、路径错绑 | 歧义即隔离 |
| supports 门禁被放宽 | Pattern 静默执行错误 | 继续保守拒绝，补真实数据或消费者 |
| 继续增加单任务编译常量 | 迁移成本线性增长 | 改为 source-shape 和 operation 规则 |
| 建立新 VM 或 JNI | 重复运行时、难调试、跨平台风险 | 复用现有 Java 执行器 |
| coverage 人工编辑不可复现 | 生成后丢所有权信息 | 生成事实 + 小型所有权审计数据合并 |
| 奖励和刷怪事件重放 | 重复奖励、重复 NPC、分数错误 | 稳定幂等键和持久账本 |
| 反编译函数不完整 | 错猜任务语义 | confidence=unknown，保留 Handler |
| 一次性删除 Handler | 隐藏恢复和清理缺口 | 按行为维度切换 |

## 16. 推荐仓库职责

### 58Server-new

- 只读原始数据权威源；
- 不写迁移修复；
- 由 manifest 记录输入 SHA。

### 58Server

- 只读语义证据；
- 提供类、函数、回调、状态和服务调用证明；
- 不作为当前真端数据默认根。

### aion_drop

- 负责无损规范化权威层；
- 负责 common/China 覆盖；
- 负责 catalog、manifest、repairs、conflicts 和往返验证；
- 负责通用引用索引和可复用派生生成；
- 不直接修改 AionEmu 运行文件。

### AionEmu

- 负责把权威数据编译为当前运行时 XML；
- 负责 Quest、AI、Instance 的类型化消费者；
- 负责 supports、所有权、shadow、恢复和运行测试；
- 不复制一份手工“真端全数据”。

## 17. 最小可落地顺序

如果立即开始实施，最短且风险最低的顺序是：

1. 只改数据根和 manifest，固定 58Server-new + China。
2. 用现有 aion_drop V2 重新生成 quests、instances、ai、world 权威模块。
3. 建全局引用图，先消除当前 _faction_、_challengetask_、_area_ 三类批量缺口。
4. 给 value0..value10 建原始槽位表，不改变运行时。
5. 从当前已有 DataDrivenQuest 能力反向定义首批类型化 Event/Condition/Action。
6. 删除生成器中可被通用规则替代的 COMPILED_* 分支。
7. 按真实可达数量补 Movie、Spawn、Timer、EnterInstance 等动作族。
8. 生成 coverage + manifest，合并小型所有权缺口数据。
9. 开启 shadow，按行为维度切换所有权。
10. 全维度闭合后批量删除对应 Handler。

前三步完成前，不建议继续扩大逐任务迁移批次。否则新任务仍会增加同一类技术债。

## 18. 最终判断

从真端全数据出发、再用通用执行器完成任务和副本整体迁移是正确方向，现有项目也已经具备大部分运行基础。

真正需要新增的不是另一套脚本框架，而是：

- 一个以 58Server-new 为唯一数据权威的可复现入口；
- 一套无损、可追溯的全数据层；
- 一个跨任务、副本、AI 和世界的引用图；
- 一套由真实字段和源码证据编译的类型化操作；
- 对现有 Quest、AI、Instance 执行器的少量能力补齐；
- 一套按行为维度切换所有权的严格门禁。

完成这些后，迁移成本将从“每个任务或副本都改代码”，变成“补一种通用语义，自动接管所有同形数据”。这才是能够完成整体迁移、并长期跟随真端数据重生成的路径。

## 19. Java handler 批量退役方法（实测）

### 19.1 核心杠杆：xml_owned_ids 排除链

生成器 `generate()` 第 3560 行：`xml_owned_ids = enabled_ids - java_handler_ids`。即**有 Java handler 的 quest 被主动排除**，生成器不会为其生成 XML。这意味着：删 Java handler 后，生成器会自动从真端脚本数据表（SimpleTalk/SimpleHunt/SimpleUseItem/...）生成 XML，无需逐个写 evidence。

这是从“逐个退役”转向“O(能力族) 批量退役”的关键。

### 19.2 批量退役流程（已验证）

1. **交集分析**：`Java ∩ 真端脚本表`，找出有真端表定义但被 Java 占据的 quest。
2. **形状抽样**：核对真端表形状是否被生成器现有函数支持（allowed 字段集 + 渲染模板）。
3. **批量删 Java**：`git rm` 对应 handler 文件。
4. **重生成**：`python3 scripts/generate_retail_simple_quests.py`，确认无并行 python 进程。
5. **核对**：删了 N 个，total 应 +N、java -N、isolated 不变；偏差必须能归因为“真端形状未被生成器支持”。
6. **恢复失败的**：删了但进 isolated/unsupported 的（形状不支持），`git checkout HEAD --` 恢复其 Java。
7. **门禁**：Routing 7 + Smoke 58 全绿。
8. **提交**：路径限定，不卷入并行工作线。

### 19.3 实测结果（2026-07-27）

| 批次 | 真端表 | 候选 | 成功 | 失败原因 |
|------|--------|------|------|----------|
| SimpleTalk | Quest_SimpleTalk.xml | 59 | 54 | 5 个 ScriptDLL-only 证据不足 |
| SimpleHunt | Quest_SimpleHunt.xml | 4 | 1 | 3 个形状未映射（talk+give_item+hunt 混合） |
| SimpleUseItem | Quest_SimpleUseItem.xml | 11 | 10 | 1 个 ScriptDLL 证据不足（扩展后） |
| SimpleItemPlay | Quest_SimpleItemPlay.xml | 8 | 8 | 0（修复 give_item 字段后全成功） |
| SimpleCollect | Quest_SimpleCollectItem.xml | 2 | 2 | 0（扩展 talk_npc 支持后全成功） |

**净退役 75 个 Java handler（54 SimpleTalk + 1 SimpleHunt + 10 SimpleUseItem + 8 SimpleItemPlay + 2 SimpleCollect），门禁全绿。**

### 19.4 形状映射扩展（本会话完成）

通过扩展生成器形状映射，4 个简单表的 Java handler 批量退役：

- **simple_use_items**：扩展支持 talk_npc3 + give/remove_item*N* 物品流转形状，输出 data_driven 步骤链（start_type=ITEM_PLAY + TALK 步骤链，保留严格顺序语义）。退役 10 个。
- **simple_item_plays**：修复 give_item 字段读取（SimpleItemPlay 表用 `give_item` 无后缀，非 `give_item1`），放宽 remove_item 要求，跳过哨兵 NPC。退役 8 个。
- **simple_collects**：扩展 allowed 支持 talk_npc1/2/3，输出 talks 列表（item_collecting 模板已支持中间 talk NPC）。退役 2 个。

关键经验：**形状映射扩展是 O(形状族) 杠杆**。每个简单表函数的字段集（allowed）+ 输出格式扩展，对应一批 Java handler 自动退役。扩展前必验证：字段在 allowed 里、输出格式与渲染模板匹配、哨兵 NPC 跳过。

### 19.5 下一批杠杆：剩余 SimpleHunt + data_driven 覆盖池

剩余 3 个 SimpleHunt 覆盖的 Java handler（14152/14112/24153）形状是 talk + give_item + monster hunt 混合步骤链，需扩展 retail_hunts 输出 data_driven 步骤链。其中 24153 是并行 5 目标 hunt（每目标独立 var 槽），需 data_driven 支持并行 HUNT 步骤（当前 data_driven HUNT 是顺序的，不支持并行多 var 槽）。这是模型能力差距，需先扩展 data_driven 模型。

### 19.6 最大长尾池：269 个 data_driven 覆盖的 Java handler

790 个 Java handler 中，269 个在真端 `data_driven_quest.xml` 有定义。形状分布（acquire, progress）：
- **120 EnterArea+PVP**（深渊/地图 PVP 击杀任务，最大类）
- 18 none+PVP, 14 EnterArea+Hunt, 12 LevelUpLogIn+ItemPlay 等

`data_driven_pvps` 函数当前只支持 `acquire=Talk + progress=PVP`（119 个已生成）。`acquire=EnterArea + progress=PVP` 的 120 个需要扩展为 `kill_in_world` 带 `invasionWorldId`（进入世界自动开始），但真端块无 world 字段--需从 ScriptDLL 或 quest.xml 区域推导。这是下一阶段的最大批量池。

### 19.7 批量判定要点

- **SimpleTalk 表形状被生成器完整支持**（54/59 成功率 92%）--安全批量池。
- **SimpleUseItem/ItemPlay/Collect 表形状经扩展后全部支持**（20/21 成功率 95%）--形状映射扩展杠杆验证成功。
- **SimpleHunt 混合形状（talk+give_item+hunt）需模型扩展**（1/4）--data_driven 需支持并行 HUNT。
- **删前必验证形状支持**：用 `Java ∩ 真端表` 交集 + 生成器 allowed 字段集核对，避免批量删后大面积隔离。
- **哨兵 NPC（_faction_ 等）必须跳过**：simple_item_plays 等函数需跳过 acquired_npc 以 `_` 开头的记录，让 faction 闭包处理。
- **5% 失败可接受**：恢复失败的 Java，不影响整体进度。


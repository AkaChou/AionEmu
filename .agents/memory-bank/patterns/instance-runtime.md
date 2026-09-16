# Instance & Runtime Patterns (副本与运行时模式)

本文档记录副本特殊逻辑、运行时配置、实例刷怪分组和事件安全方面可跨任务复用的排查结论。代码提交、静态审计和聚焦测试不会自动等同于 Maven、运行时或客户端验收。

> Pattern IDs: `IR-001`–`IR-010`
> card_status: ACTIVE; runtime-sensitive findings retain their validation boundary
> scope: instance handlers, drop/stat registration, GM command loading, walker formations and event services in AionEmu-test
> last_reviewed: 2026-09-16

---

## [IR-001] 一、副本特殊掉落在注册阶段按物品 ID 去重 (INSTANCE_DROP_DEDUPE_AT_REGISTRATION)
<!-- pattern-metadata
status: CONFIRMED
scope: DropRegistrationService and instance-specific onDropRegistered handlers
first_seen: 2026-09-13
last_verified: 2026-09-14
symptom: 副本钥匙或特殊物品实际掉落两份，但基础 NPC 掉落表和任务掉落表看起来各自都只有一份
root_cause: Base NPC drops and quest drops are registered before the instance handler, which then unconditionally appends the same item
fix_or_guardrail: Before adding an instance fallback drop, check existing DropItem entries by dropItem.getDropTemplate().getItemId(); append only when the item ID is absent
evidence: commit e9b4bb129; DropRegistrationService.java:116-175; KromedesTrialInstance.java:194-217,453-459; KromedesTrialInstanceTest
validation: static; focused regression test added but Maven/JUnit not run; runtime/client drop verification pending
boundaries: Deduplicate by item ID only for the intended special-drop contract; do not delete task or base sources before auditing random-boss and quest-state branches
superseded_by: none
first_check: DropRegistrationService.registerDrop, NPC base drop XML, quest XML drops and instance onDropRegistered
-->

- 注册顺序是关键：基础 NPC 掉落、任务 XML 掉落先合并，实例 Handler 后补充。因此“Handler 里看到缺钥匙就追加”不能写成无条件 `add`。
- Kromedes Trial 的修复保留随机 Boss/阵营分支的补钥匙能力，同时保证同一物品 ID 最终只注册一份；类似掉落问题先找重复来源，再决定是否改数量或删数据。

## [IR-002] 二、BOOST_SPELL_ATTACK 不得套用 short 属性上限 (BOOST_SPELL_ATTACK_INTEGER_CAP)
<!-- pattern-metadata
status: CONFIRMED
scope: StatCapUtil, CreatureGameStats modifier evaluation and skills using BOOST_SPELL_ATTACK
first_seen: 2026-09-13
last_verified: 2026-09-14
symptom: 技能 XML 配置百万级即时伤害，但角色处于特定被动修饰状态时最终伤害骤降到约数万
root_cause: The generic modifier cap used Short.MAX_VALUE, truncating BOOST_SPELL_ATTACK value 10000000 before normal magic, resistance and PvE calculations
fix_or_guardrail: Give BOOST_SPELL_ATTACK an Integer.MAX_VALUE upper-cap branch in StatCapUtil; do not alter skill XML or level-penalty formulas without tracing the full damage chain
evidence: commit 9deacf5a89830045092dc2a87da82dc009db895f; StatCapUtil.java:122-165; StatFunctions.java:448-469; CreatureGameStats.java:172-190; StatCapUtilTest#keepsBoostSpellAttackDamageAboveShortRange
validation: static; focused regression test added; Maven/runtime damage verification not run
boundaries: This removes an intermediate-value truncation only; target mdef, resistance, stat_ratio, PvE reduction and AI damage modifiers still apply
superseded_by: none
first_check: CreatureGameStats.getStat, StatCapUtil.getUpperCap, registered BOOST_SPELL_ATTACK modifiers and the final magic-damage chain
-->

- `noresist=true` 只影响命中/抗性判定，不等于跳过伤害减免；先确认 `BOOST_SPELL_ATTACK` 是否经过属性修饰上限，再分析目标属性和等级惩罚。
- 同一技能可能因角色是否注册属性修饰函数而走不同路径；看到数量级差异时，不能只看技能 XML 的 `value`。

## [IR-003] 三、GM 命令必须同时核对类扫描与有效运行时配置 (GM_COMMAND_EFFECTIVE_CONFIG)
<!-- pattern-metadata
status: CONFIRMED
scope: ChatCommandsLoader, ChatProcessor and effective administration/commands.properties for admin commands
first_seen: 2026-09-13
last_verified: 2026-09-14
symptom: 源码和 target/classes 中已有 GM 命令，但客户端输入 //command 后被当作普通聊天文本
root_cause: ChatProcessor rejects the discovered command when the effective runtime commands.properties lacks its alias/access-level entry; source resources may differ from aion/config
fix_or_guardrail: Keep the concise //reset instance interface, enforce self-owned solo-instance scope through InstanceService.destroyInstance, and verify/reload the effective runtime commands.properties before testing
evidence: commit 4d348038db6b036274205744d85f9ca34be1b985; ChatProcessor.java:132-154; CM_CHAT_MESSAGE_PUBLIC.java:40-44; effective aion/config/administration/commands.properties
validation: static; focused unit tests added; source command path succeeded; live runtime registration after reload not confirmed
boundaries: Reset must not destroy group/alliance/league instances or a multiplayer instance merely because the caller is currently alone; ignored local runtime config is not a source commit
superseded_by: none
first_check: launch classpath, target/classes, aion/config/administration/commands.properties, ChatProcessor registration and //reload commands
-->

- “源码存在”不是“当前进程已注册”：IDEA 运行时要同时检查 `target/classes` 与实际读取的 `aion/config`；配置修正后先执行 `//reload commands`，再测试 `//reset instance`。
- 副本重置应复用 `InstanceService.destroyInstance()` 完成清理和离场传送，不要在命令层复制生命周期逻辑。

## [IR-004] 四、巡逻编队按组锚点距离分组，避免坐标漂移拆队 (WALKER_FORMATION_ANCHOR_GROUPING)
<!-- pattern-metadata
status: CONFIRMED
scope: InstanceWalkerFormations, ClusteredNpc and WalkerGroup formation membership
first_seen: 2026-09-13
last_verified: 2026-09-14
symptom: rows 配置正确却出现 WalkerGroup Invalid row sizes，编队成员被拆成多个刷怪组
root_cause: Exact X/Y position hashing treated small retail coordinate synchronization drift as separate groups
fix_or_guardrail: Group candidates by X/Y distance to the current group anchor within WalkerGroupShift.DISTANCE (2m), never by transitive nearest-neighbor chaining
evidence: commit 99f3f4aa2; InstanceWalkerFormations.java; ClusteredNpc.java; 5,343-route audit with 58 improved combinations and no unwanted multi-group merge
validation: static; focused regression coverage added; Maven/runtime formation test not run
boundaries: Compare each candidate with the group anchor, not any member; a row-size warning alone does not prove instance spawn failure
superseded_by: none
first_check: InstanceWalkerFormations.organizeAndSpawn, POSITION_GROUP_DISTANCE, WalkerGroupShift.DISTANCE, rows/member count and actual spawn logs
-->

- 原来的精确坐标 hash 会把约 1.5–1.8 米的零售坐标漂移误判为两组；2 米阈值应与现有 `WalkerGroupShift.DISTANCE` 保持一致。
- 不要改成任意近邻连通分量：链式近邻可能把原本独立的远距离队伍合并；组锚点比较保留了独立队伍边界。

## [IR-005] 五、事件日志与客户端崩溃不能仅凭时间相邻归因 (EVENT_CRASH_CAUSALITY_BOUNDARY)
<!-- pattern-metadata
status: PROVISIONAL
scope: CrazyDaevaService event selection, teleport side effects and PvP reward calls
first_seen: 2026-09-13
last_verified: 2026-09-14
symptom: 活动日志后紧接客户端断线或被报告为崩溃
root_cause: Temporal proximity alone does not prove the selected branch ran; unconditional same-position teleport and semantically wrong PvP reward side effects were unsafe regardless of the reported crash cause
fix_or_guardrail: Verify random value, level gate, selected-branch log and runtime bytecode before assigning causality; remove no-op same-position teleport and wrong doReward call, and reset per-round selection state
evidence: commit 5b6d3dc92d81bc37317af73827e8739331299a51; CrazyDaevaService.java; log/console.log:41226-41232; events.properties:145-159
validation: static; diff check; runtime bytecode/log timing inspection; build, restart and client dump unavailable
boundaries: The original client crash cause remains unconfirmed; do not claim client acceptance from server-side timing alone
superseded_by: none
first_check: startChoose branch conditions, running PID/classpath, selected-player logs, teleport packets and client dump/log
-->

- 先用随机值、最低等级和运行时字节码确认是否真正进入选中分支；未出现选中日志、传送包或活动状态变化时，不能把断线直接归因于该分支。
- 即使根因尚未被客户端证据证明，也应移除同地图同坐标的无意义传送和错误语义的奖励调用，避免无关副作用扩大问题。

## [IR-006] 六、重叠 NPC 先证明加载归属再删除 (DRAUPNIR_SPAWN_OWNERSHIP_UNRESOLVED)
<!-- pattern-metadata
status: PROVISIONAL
scope: 320080000 Draupnir Cave static spawns, condition-spawns and producer-page ownership
first_seen: 2026-09-13
last_verified: 2026-09-14
symptom: 两个 NPC 在相近坐标重复出现，疑似同一训练/生产点被刷出两次
root_cause: Ownership between static-spawn loading and condition-spawn producer pages is unresolved; coordinate overlap alone cannot distinguish intended retail duplicates from duplicate materialization
fix_or_guardrail: Do not delete NPC IDs 213776 or 236925 from either source until loader ownership and producer-page materialization are proven
evidence: DraupnirCave world 320080000; NPC IDs 213776/236925; condition-spawns.xml producer-page-1-1 and producer-page-2-2; static-spawn audit
validation: static investigation only; no code change, test or runtime/client confirmation
boundaries: This is an investigation guardrail, not evidence that either NPC should be removed; inspect loader ownership, conditions and retail/legacy intent first
superseded_by: none
first_check: static spawn loader, RetailConditionSpawnEngine, condition-spawns producer pages and legacy/client NPC evidence
-->

- “坐标相近”只能触发归属审计，不能直接作为删 NPC 的依据；先判断静态加载器和条件刷怪引擎是否都实例化了同一个 producer page。
- 在未解决前保留两个候选 ID 及其来源记录，避免把零售/legacy 的两套页面误合并成一次破坏性数据删除。

## [IR-007] 七、判定 spot 是否新增禁用含 z 的坐标哈希 (SPAWN_SPOT_IDENTITY_EXCLUDES_Z)
<!-- pattern-metadata
status: CONFIRMED
scope: static spawn XML auditing and de-duplication under src/main/resources/aion/data/static_data/spawns
first_seen: 2026-09-14
last_verified: 2026-09-14
symptom: 排查同一 NPC 重复刷出时，按“该点是否为新引入”筛选候选，数量远少于实际，且把重复归因给错误的提交
root_cause: Retail synchronisation re-projects z from terrain, so an existing spot's z changes while x/y stay; a coordinate hash that includes z reports the re-projected spot as newly added and hides the real leftover
fix_or_guardrail: Compare spots by planar x/y only; decide “newly introduced” via block-level history (which commit introduced the retail spot, and whether the parent revision already had a coincident legacy spot in the same npc_id block) instead of hashing (x, y, z)
evidence: commit 3fc71b693; SpawnGroup2.java:104; SpawnSurfaceResolver.java:24; NormalBalaureaSpawnDataTest.java:26; spawns/Npcs/400010000_Reshanta.xml:886
validation: static; focused-test (dataholders and spawnengine suites passed except a pre-existing unrelated failure); runtime/client verification pending
boundaries: The exclusion of z applies to identity and de-duplication decisions only; z still matters for spawn height and resolve_z behaviour. Legacy and retail spots with differing entity_id are different objects and must not be merged on coincidence alone
superseded_by: none
first_check: spot identity comparison code, resolve_z handling in SpawnSurfaceResolver, and per-block git history of the spawn XML
-->

- 真端同步会把已存在的点补上 `resolve_z="true"` 并改 z（地形重投影），x/y 不变。因此同块内两个 spot 的 z 本来就不同，**z 不能进入身份判定**。
- 用块级历史取证代替坐标哈希：找到引入真端点 `R` 的提交，检查其父版本中同一 `npc_id` 块内是否已存在与 `L` 平面重合的 legacy 点。是则该提交在 `L` 旁新增了重合的 `R`，属制造重复。
- 该判据在同一批数据上把候选从 68 修正到 138，并推翻了对引入提交的误判（`a5e274fd0` 并未新增点，只是补 `resolve_z`；重复源自更早的提交）。
- 与 `IR-006` 的关系：IR-006 要求先证明加载归属再删；本条给出可执行的归属判据。两者都禁止仅凭坐标接近直接删除——`entity_id` 不同的重合点是不同对象，必须保留待人工判断。

---

## [IR-008] 八、NPC 死亡链路上的玩家对象可为 null (NPC_DEATH_PLAYER_MAY_BE_NULL)
<!-- pattern-metadata
status: CONFIRMED
scope: 实例脚本 onDie/掉落/播片等以 AggroList 结果作为玩家入参的链路
first_seen: 2026-09-15
last_verified: 2026-09-15
symptom: NPC 死亡时 NPE "Cannot invoke Player.getClientConnection() because \"player\" is null"（如 DarkPoetaInstance.sendMovie → PacketSendUtility）
root_cause: AggroList#getMostPlayerDamage() 在没有玩家伤害（NPC/环境/无 aggro）时返回 null，而实例脚本直接把该值当非空使用
fix_or_guardrail: 消费 getMostPlayerDamage() 的结果必须先判空；实例 sendMovie(Player,int) 必须在标记“已播”之前判空；PacketSendUtility.sendPacket 已容忍 null 玩家（静默跳过）
evidence: src/main/java/com/aionemu/gameserver/controllers/attack/AggroList.java:264; src/main/java/com/aionemu/gameserver/utils/PacketSendUtility.java:191; src/main/java/com/aionemu/gameserver/instance/handlers/scripts/DarkPoetaInstance.java:868; src/test/java/com/aionemu/gameserver/instance/handlers/InstanceMovieNullGuardTest.java; src/test/java/com/aionemu/gameserver/utils/PacketSendUtilityTest.java
validation: focused-test + full-suite; 全量 3262 例仅剩 13 个既有 quest/AI 失败；闸门经“移除守卫即失败”负向验证；运行态验收 2026-09-15 20:58 重启（PacketSendUtility.class 字节码含 ifnull 判空）后进入暗黑波伊塔 300040000，至 21:05 NPE/ERROR 均为 0，任务 3502 由状态 3 推进到状态 4
boundaries: 中央判空只保证不崩，不会补发封包；若语义上需要“播给副本内玩家”，必须另做（当前 sendMovie 仍是“首个有玩家伤害者”语义）
superseded_by: none
first_check: getMostPlayerDamage 调用点、实例脚本 sendMovie/sendPacket(player,…)、PacketSendUtility 的 null 容忍度
-->

- 触发条件很常见：NPC 被另一个 NPC、环境伤害、GM 命令杀死，或对玩家已离线/aggro 已清空时死亡 → `getMostPlayerDamage()` 返回 `null`。
- 危害不止“少发一个包”：异常会**中断 onDie 剩余逻辑**（点位累加、`deleteNpc`、`spawn` 都不会执行），因此必须彻底消除。
- 两层防护：① `PacketSendUtility.sendPacket` 判空（覆盖全部 94 个 `getMostPlayerDamage()` 消费点的崩溃面）；② 25 个实例脚本的 `sendMovie` 在 `movies.add` 之前判空（避免“播给 null 却标记已播”），由 `InstanceMovieNullGuardTest` 闸门守护（要求至少扫到 20 个声明文件，防止包路径变动导致闸门静默失效）。

---

## [IR-009] 九、pattern 临时子对象的 live_time 与生成者状态重置解耦 (PATTERN_SPAWN_LIVE_TIME_SURVIVES_SPAWNER_RESET)
<!-- pattern-metadata
status: CONFIRMED
scope: RetailPatternAI2 的 spawn/spawn_on_target 系列动作创建的临时 NPC，及其在 resetPatternState 中的清理
first_seen: 2026-09-16
last_verified: 2026-09-16
symptom: 卵孵化出的召唤物只短暂出现就消失（真端数据写的是 live_time=18）；同类“带 live_time 的临时召唤物”都如此
root_cause: despawn_self → NpcController#onDespawn → AIEventType.DESPAWNED → handleDespawned → resetPatternState 把所有 spawn_id 登记对象统一 despawnForLifecycle，忽略对象自带的 live_time，刚生成的召唤物在同一调用栈内被删除
fix_or_guardrail: 自带 live_time 的对象在 resetPatternState 中只保留登记（显式 <despawn spawn_id> 仍可清理），由自己的到期任务删除；live_time=0 的标记物/门等继续随重置删除
evidence: src/main/java/com/aionemu/gameserver/ai/RetailPatternAI2.java:2257; src/main/java/com/aionemu/gameserver/ai/RetailPatternAI2.java:2299; src/main/java/com/aionemu/gameserver/ai/RetailPatternAI2.java:2311; src/main/java/com/aionemu/gameserver/ai/RetailPatternAI2.java:2875; src/test/java/com/aionemu/gameserver/ai/RetailPatternAI2Test.java:1105
validation: focused-test（RetailPatternAI2Test 78 例 0 失败，2026-09-16；新增 patternResetKeepsSelfManagedLiveTimeSpawns 断言 live_time 对象不随重置删除、live_time=0 对象仍被删除）；客户端实机验证通过（2026-09-16，用户报告：孵化物正常存活，不再过早消失）
boundaries: 真端只用显式 <despawn spawn_id> 表达“随生成者清理”；本护栏不得让显式 despawn 失效。despawn_at_attack_state=FALSE 的“战斗中延迟删除”语义保持不变
superseded_by: none
first_check: resetPatternState/releaseTrackedSpawns 是否按 live_time 区分释放
-->

- **症状**：Taloc's Hollow 的 mosqua egg（282006）孵化出的 workmanfly（282082）只闪一下/存活极短就消失，而真端 pattern 给它的是 `live_time=18`。
- **根因链**：
  1. `spawn_on_target` 生成 282082 时带 `spawn_id=SPAWN_ID_1`，被登记进 `RetailPatternAI2#spawned`，同时排了自己的 18 秒到期任务；
  2. 同一条动作链末尾的 `despawn_self` 删除卵自身 → `NpcController#onDespawn` 抛 `AIEventType.DESPAWNED` → `handleDespawned()` → `resetPatternState()`；
  3. 原实现对 `spawned` 一律 `despawnForLifecycle`，而该对象 `despawn_at_attack_state=TRUE`、且不在战斗 → 立即 `onDelete()`：召唤物活不到自己的 18 秒。
- **真端依据**：真端只在需要“随生成者清理”时写显式 `<despawn spawn_id>`（同一个文件里 `Elim_NeutflyNm.on_die` 就是这样清理 SPAWN_ID_1/2/3 的），而 `Elim_NeutflyEgg` 对 SPAWN_ID_1 没有任何 despawn——孵化物应当独立活满 `live_time`。
- **修复与边界**：按对象是否自带 `live_time` 区分——自带者只保留登记（显式 despawn 仍有效），由到期任务删除；`live_time=0` 的标记物/门继续随重置删除。`shouldDelayLifecycleDespawn`（战斗中延迟删除）语义未变。

## [IR-010] 十、真端对齐删除硬编码副作用后必须保留幂等实例适配器 (RETAIL_ALIGNMENT_KEEPS_IDEMPOTENT_ADAPTER)
<!-- pattern-metadata
status: CONFIRMED
scope: 实例脚本从硬编码副作用迁移到真端 pattern（AI2Engine 门禁接管 + RetailConditionSpawnEngine / RetailDynamicAreaEngine）
first_seen: 2026-09-14
last_verified: 2026-09-16
symptom: 对齐真端数据后，实例里原本必然出现的特效或托起碰撞整块消失（例：Taloc's Hollow 2F 打破破裂巨虫卵后地面不再升起上升气流，但角色仍可展开翅膀自行飞上去）
root_cause: 实例脚本里原先把副作用写死的兜底（直接 spawn 特效 NPC、广播系统消息）被删除，改为完全依赖真端 pattern；该 NPC 的模板 AI 名不是真端 pattern 名，AI2Engine.selectNpcAi 在 RetailPatternAI2.supports 门禁不通过时会静默回落到模板 AI，副作用整块不执行且日志无报错
fix_or_guardrail: 迁移时在实例生命周期事件里保留幂等适配器，直接驱动真端执行器（RetailConditionSpawnEngine.setVariable 设条件变量、RetailDynamicAreaEngine.setEnabled 开地面移动碰撞），坐标与实体 ID 仍取真端数据；条件已激活时不重复刷怪，因此 pattern 正常接管时不会产生第二份实体
evidence: commit 5830ece07; src/main/java/com/aionemu/gameserver/instance/handlers/scripts/TalocsHollowInstance.java:220; src/main/resources/aion/definitions/compact/ai/condition-spawns.xml:31056; src/main/resources/aion/definitions/compact/ai/dynamic-areas.xml:218; src/main/resources/aion/definitions/compact/ai/npc-ai.xml:59884; .agents/summary/taloc-hollow-updraft/2026-09-14-2f-updraft-restore.zh-CN.md
validation: 实机验收通过（用户 2026-09-16 确认打破卵后地面升起气流）；未按 A/B 隔离 pattern 是否接管，未执行 Maven/JUnit
boundaries: 适配器只允许驱动真端执行器，禁止把真端刷怪坐标复制进实例脚本；同一条件变量/动态区域重复开启必须保持幂等；不改变真端 pattern 自身的动作顺序与清理语义
superseded_by: none
first_check: 对齐真端时被删除的实例脚本副作用（特效实体、条件刷怪、移动碰撞）是否还有幂等替代路径
-->

- **症状**：`5ccb10261` 删除 `TalocsHollowInstance.onDie` 中“打破卵即 spawn 气流 NPC `281817`”的兜底、改为只依赖真端 pattern 之后，游戏内打破卵完全没有气流（视觉与托起碰撞同时缺失），而飞行本身正常。
- **根因链**：真端副作用由 `Elim_WindEventB.on_die` 承担（置 `IDElim_2F_Wind=1` → 条件 3085 刷 `281817`，并开 `MOVING_COLLISION_WINDBOX` sunzone 100）；该 NPC 模板 AI 名是 `noaction`，`RetailPatternAI2.supports` 门禁不通过时 `AI2Engine.selectNpcAi` 静默回落到模板 AI，既无日志也无异常，表现就是“整块特效消失”。
- **护栏**：实例层用条件变量 + 动态区域 API 触发真端链路，与 pattern 的副作用等价且幂等（条件激活后不重复刷怪）；坐标、`entity_id`、sunzone 全部以真端数据为唯一来源。
- **边界**：这条兜底只保证副作用一定发生，不替代 pattern 行为取证；模板 AI 名 ≠ 真端 pattern 名时门禁失败是静默的，判断“pattern 是否接管”必须逐 NPC 核对。

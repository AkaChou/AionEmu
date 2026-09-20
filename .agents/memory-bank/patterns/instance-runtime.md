# Instance & Runtime Patterns (副本与运行时模式)

本文档记录副本特殊逻辑、运行时配置、实例刷怪分组和事件安全方面可跨任务复用的排查结论。代码提交、静态审计和聚焦测试不会自动等同于 Maven、运行时或客户端验收。

> Pattern IDs: `IR-001`–`IR-013`
> card_status: ACTIVE; runtime-sensitive findings retain their validation boundary
> scope: instance handlers, drop/stat registration, GM command loading, walker formations and event services in AionEmu-test
> last_reviewed: 2026-09-20

---

## [IR-001] 一、副本特殊掉落在注册阶段按物品 ID 去重 (INSTANCE_DROP_DEDUPE_AT_REGISTRATION)
<!-- pattern-metadata
status: CONFIRMED
scope: DropRegistrationService and instance-specific onDropRegistered handlers
first_seen: 2026-09-13
last_verified: 2026-09-16
symptom: 副本钥匙或特殊物品实际掉落两份，且点击任意掉落行都提示同一限持物品已拥有
root_cause: Base NPC drops and quest drops are registered before the instance handler, which then unconditionally appends the same item; multiple fallback entries also reuse index 1 although the client returns only the index byte
fix_or_guardrail: Before adding an instance fallback drop, check existing DropItem entries by item ID and append only when absent; after all registration hooks, normalizeDropIndices(droppedItems) must renumber the corpse drop set to unique sequential indices before any loot status/auto-loot packet
evidence: commit e9b4bb129; DropRegistrationService.java:176-179,420-429; TalocsHollowInstance.java:118-173; Treasure_Box_Success_BossAI2.java:133-142; DropRegistrationServiceTest#normalizesDuplicateDropIndicesToUniqueSequentialValues; TalocsHollowInstanceTest
validation: static/IDE checks; mvn -B test -Dtest='DropRegistrationServiceTest,TalocsHollowInstanceTest' passed (10 tests, 2026-09-16); related mvn -B test -Dtest='DropServiceTest,DropDistributionServiceTest,KromedesTrialInstanceTest' passed (14 tests); client loot verification succeeded (2026-09-16)
boundaries: Normalization covers the initial registerDrop path and the direct siege-chest drop set; drops appended after the loot list is released must still allocate a free index. Deduplicate by item ID only for the intended special-drop contract; do not delete task or base sources before auditing random-boss and quest-state branches
superseded_by: none
first_check: DropRegistrationService.registerDrop/normalizeDropIndices, NPC base drop XML, quest XML drops and instance onDropRegistered
-->

- 注册顺序是关键：基础 NPC 掉落、任务 XML 掉落先合并，实例 Handler 后补充。因此“Handler 里看到缺钥匙就追加”不能写成无条件 `add`。
- Kromedes Trial 的修复保留随机 Boss/阵营分支的补钥匙能力，同时保证同一物品 ID 最终只注册一份；类似掉落问题先找重复来源，再决定是否改数量或删数据。
- 客户端 `CM_LOOT_ITEM` 只回传目标对象 ID 和一个字节的索引；同一尸体的 `DropItem.index` 必须唯一。实例 Handler 的固定索引会在 `registerDrop` 释放掉落前由 `normalizeDropIndices` 统一重排；异步追加动态掉落仍需自行分配未占用索引。

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
last_verified: 2026-09-19
symptom: 源码和 target/classes 中已有 GM 命令，但客户端输入 //command 后被当作普通聊天文本
root_cause: ChatProcessor rejects the discovered command when the effective runtime commands.properties lacks its alias/access-level entry; source resources may differ from aion/config
fix_or_guardrail: Keep the concise //reset instance interface, support self-owned solo/solo-entered group instances and current team (group/alliance/league) instances via InstanceService.resetPlayerInstances, and verify/reload the effective runtime commands.properties before testing
evidence: commit 4d348038db6b036274205744d85f9ca34be1b985; ChatProcessor.java:132-154; CM_CHAT_MESSAGE_PUBLIC.java:40-44; effective aion/config/administration/commands.properties
validation: static; focused unit tests in InstanceServiceTest passed (8 tests); source command path succeeded; live runtime registration after reload not confirmed
boundaries: Reset covers player-owned solo instances, solo-entered group instances (no group registered), and instances registered to the player's current group/alliance/league; must never reset other teams' instances; ignored local runtime config is not a source commit
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
last_verified: 2026-09-19
symptom: 同一 NPC 重复刷出；排查时按“该点是否为新引入”筛选候选数量远少于实际，且把重复归因给错误的提交。残留也可能是**另一条独立 `<spawn>` 块**且与真端点相距十几米（例：任务版盘龙巢穴 237228 / 237229 各刷 2 个），同块重合点判据扫不到
root_cause: Retail synchronisation re-projects z from terrain, so an existing spot's z changes while x/y stay; a coordinate hash that includes z reports the re-projected spot as newly added and hides the real leftover. 该同步还会**叠加**真端出生面而不同步删除 legacy 块，于是同一 npc_id 出现两条独立块
fix_or_guardrail: Compare spots by planar x/y only; decide “newly introduced” via block-level history (which commit introduced the retail spot, and whether the parent revision already had a coincident legacy spot in the same npc_id block) instead of hashing (x, y, z). 追加判据：按 npc_id 统计本地块数/点数与真端出生文件（58Server Map/Worlds 下的地图出生数据）对比，真端条数少于本地时，多出的那条即非真端残留，与距离无关
evidence: commit 3fc71b693; SpawnGroup2.java:104; SpawnSurfaceResolver.java:24; NormalBalaureaSpawnDataTest.java:26; spawns/Npcs/400010000_Reshanta.xml:886; src/main/resources/aion/data/static_data/spawns/Instances/301520000_Drakenspire_Depths.xml; src/test/java/com/aionemu/gameserver/dataholders/DrakenspireDepthsQTwinSpawnSurfaceTest.java; .agents/summary/spawn-duplicate-spots/2026-09-19-drakenspire-q-twin-duplicate.zh-CN.md
validation: static（真端 IDSeal_Q 出生面 count=1 取证 + 本地 XML 解析确认 237228/237229 各 1 块 1 点）；focused-test PENDING（DrakenspireDepthsQTwinSpawnSurfaceTest 待授权运行）；既有 2026-09-14 focused-test 记录（dataholders and spawnengine suites passed except a pre-existing unrelated failure）；runtime/client verification pending
boundaries: The exclusion of z applies to identity and de-duplication decisions only; z still matters for spawn height and resolve_z behaviour. Legacy and retail spots with differing entity_id are different objects and must not be merged on coincidence alone. 条数比较只能判“本地多于真端”；真端静态条数为 0 的 NPC 可能是运行期事件生成（如由失败/重生链刷出的形态），不能仅凭条数删除
superseded_by: none
first_check: spot identity comparison code, resolve_z handling in SpawnSurfaceResolver, and per-block git history of the spawn XML
-->

- 真端同步会把已存在的点补上 `resolve_z="true"` 并改 z（地形重投影），x/y 不变。因此同块内两个 spot 的 z 本来就不同，**z 不能进入身份判定**。
- 用块级历史取证代替坐标哈希：找到引入真端点 `R` 的提交，检查其父版本中同一 `npc_id` 块内是否已存在与 `L` 平面重合的 legacy 点。是则该提交在 `L` 旁新增了重合的 `R`，属制造重复。
- 该判据在同一批数据上把候选从 68 修正到 138，并推翻了对引入提交的误判（`a5e274fd0` 并未新增点，只是补 `resolve_z`；重复源自更早的提交）。
- 与 `IR-006` 的关系：IR-006 要求先证明加载归属再删；本条给出可执行的归属判据。两者都禁止仅凭坐标接近直接删除——`entity_id` 不同的重合点是不同对象，必须保留待人工判断。
- **2026-09-19 修订**：残留形态不止“同块内重合点”。`301520000_Drakenspire_Depths.xml` 中 237228/237229 各有一条真端块（531.088501 / 530.858398，`resolve_z="true"`）和一条手写 legacy 块（545.58734 / 545.7349，无属性），两块相距约 15 米，同块判据扫不到，客户端表现为“各刷 2 个”。真端 IDSeal_Q 出生数据里同名 NPC 都只有 1 条 count=1 记录（位置等于保留块），因此新增判据：**按 npc_id 对比本地与真端条数**，本地多出的块即残留（`trigger=客户端实测重复刷出`；`change=删除 2 条 legacy 块 + 新增 DrakenspireDepthsQTwinSpawnSurfaceTest 闸门`；`evidence=上述 301520000 出生表与本条 summary`）。

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
fix_or_guardrail: 迁移时在实例生命周期事件里保留幂等适配器，直接驱动真端执行器（RetailConditionSpawnEngine.setVariable 设条件变量、RetailDynamicAreaEngine.setEnabled 开地面移动碰撞），坐标与实体 ID 仍取真端数据；对象类副作用可用 RetailPatternAI2#spawnRetailActionNpc 按同一份真端 spawn 动作补刷（同实例已存在同模板 NPC 即跳过）；条件已激活/对象已存在时不重复刷怪，因此 pattern 正常接管时不会产生第二份实体
evidence: commit 5830ece07; src/main/java/com/aionemu/gameserver/instance/handlers/scripts/TalocsHollowInstance.java:220; src/main/resources/aion/definitions/compact/ai/condition-spawns.xml:31056; src/main/resources/aion/definitions/compact/ai/dynamic-areas.xml:218; src/main/resources/aion/definitions/compact/ai/npc-ai-parts/npc-ai_270907_286549.xml:6491; .agents/summary/taloc-hollow-updraft/2026-09-14-2f-updraft-restore.zh-CN.md
validation: 实机验收通过（用户 2026-09-16 确认打破卵后地面升起气流）；全量 Maven 测试 3282 例、0 失败、2 跳过（2026-09-16 mvn -B test，含当时工作区并行改动）；未按 A/B 隔离 pattern 是否接管；追加：Celestius 死亡后卡斯帕的幻影 799503 的同类适配器（spawnRetailActionNpc）同日实机验收通过
boundaries: 适配器只允许驱动真端执行器，禁止把真端刷怪坐标复制进实例脚本；同一条件变量/动态区域重复开启必须保持幂等；不改变真端 pattern 自身的动作顺序与清理语义；`RetailPatternAI2#spawnRetailActionNpc` 只按动作里的绝对坐标解析（owner 坐标按 0,0,0 传入），`SPAWN_LOCATION_MY_POINT` 的动作（例：`IDSeal_Q_Oritsa_01.on_die` 刷 237231）无法用它补刷，实例层必须改用生成者/死亡者的实际坐标直接 spawn
superseded_by: none
first_check: 对齐真端时被删除的实例脚本副作用（特效实体、条件刷怪、移动碰撞）是否还有幂等替代路径
-->

- **症状**：`5ccb10261` 删除 `TalocsHollowInstance.onDie` 中“打破卵即 spawn 气流 NPC `281817`”的兜底、改为只依赖真端 pattern 之后，游戏内打破卵完全没有气流（视觉与托起碰撞同时缺失），而飞行本身正常。
- **根因链**：真端副作用由 `Elim_WindEventB.on_die` 承担（置 `IDElim_2F_Wind=1` → 条件 3085 刷 `281817`，并开 `MOVING_COLLISION_WINDBOX` sunzone 100）；该 NPC 模板 AI 名是 `noaction`，`RetailPatternAI2.supports` 门禁不通过时 `AI2Engine.selectNpcAi` 静默回落到模板 AI，既无日志也无异常，表现就是“整块特效消失”。
- **护栏**：实例层用条件变量 + 动态区域 API 触发真端链路，与 pattern 的副作用等价且幂等（条件激活后不重复刷怪）；坐标、`entity_id`、sunzone 全部以真端数据为唯一来源。
- **边界**：这条兜底只保证副作用一定发生，不替代 pattern 行为取证；模板 AI 名 ≠ 真端 pattern 名时门禁失败是静默的，判断“pattern 是否接管”必须逐 NPC 核对。

## [IR-011] 十一、生成者死亡/消失事件链生成的子对象不随生成者状态重置删除 (PATTERN_SPAWN_SURVIVES_SPAWNER_DEATH_RESET)
<!-- pattern-metadata
status: CONFIRMED
scope: RetailPatternAI2 的 spawn/spawn_on_target 系列动作在 on_die / on_killed_by_user / on_killed_by_npc / on_despawn 事件链里创建的子对象及其释放时机
first_seen: 2026-09-16
last_verified: 2026-09-16
symptom: 击杀 Boss 后应当现身的对话 NPC、奖励 NPC 或传送门完全不出现（例：塔洛克空洞击杀 Celestius 后找不到卡斯帕的幻影 799503，任务 10032 无法交付）
root_cause: NpcController 先抛 DIED 触发 on_killed_by_user（spawn 登记进 spawned[SPAWN_ID_n]），再抛 DIED 触发 handleDied → resetPatternState → releaseTrackedSpawns；live_time=0 的子对象不属于 selfManagedSpawns，被 despawnForLifecycle 在同一调用栈内 onDelete，客户端看不到实体
fix_or_guardrail: 新增 SPAWNER_END_EVENTS(on_die/on_killed_by_user/on_killed_by_npc/on_despawn) 与 spawnerEndEventInProgress 标记，spawnAt 用 hasIndependentLifetime(liveTime, spawnerEndEventInProgress) 判断；这类子对象与 live_time 对象一样只保留登记、不随生成者状态重置删除
evidence: src/main/java/com/aionemu/gameserver/ai/RetailPatternAI2.java:176; src/main/java/com/aionemu/gameserver/ai/RetailPatternAI2.java:1040; src/main/java/com/aionemu/gameserver/ai/RetailPatternAI2.java:2271; src/main/java/com/aionemu/gameserver/ai/RetailPatternAI2.java:2315; src/main/java/com/aionemu/gameserver/ai/RetailPatternAI2.java:2343; src/test/java/com/aionemu/gameserver/ai/RetailPatternAI2Test.java:1127; src/main/java/com/aionemu/gameserver/controllers/NpcController.java:244; src/main/resources/aion/definitions/compact/ai/npcaipatterns_idelim_osy.xml:11; src/main/resources/aion/definitions/compact/ai/npc-ai-parts/npc-ai_200000_216003.xml:10453; src/main/resources/aion/definitions/compact/ai/npc-ai-parts/npc-ai_286550_799680.xml:10793; src/main/resources/aion/data/static_data/quest_definition/quests/10032.xml:279; commit 5ccb10261; .agents/summary/quest-10032/2026-09-16-celestius-death-spawn-caspa-ghost.zh-CN.md
validation: 静态取证（死亡事件链顺序、登记与释放判定、799503 无其它生成入口）已完成；聚焦测试 mvn -B test -Dtest='RetailPatternAI2Test' 通过（2026-09-16，79 例 0 失败 0 错误），补刷适配器与 i18n 改动后 mvn -B test -Dtest='RetailPatternAI2Test,LocalizedLogCallsTest' 再次通过（2026-09-16 17:53:26，80 例 0 失败 0 错误，BUILD SUCCESS，工作区含并行改动、非 A/B 隔离）；客户端实机验收通过（2026-09-16 用户报告击杀 Celestius 后幻影现身、10032 正常完成）；另有实例层幂等补刷适配器（spawnRetailActionNpc）兜底 pattern 未接管的情况
boundaries: 显式 <despawn spawn_id> 与 live_time 到期任务语义不变；可逆的 on_leave_attack_state（脱战，90 处 spawn 动作）仍随回位重置释放；这类子对象不再随生成者回位/重生自动回收，清理交给真端显式动作或副本销毁；异步延迟链（技能后接 spawn）在死亡处理中本就会被 resetPatternState 取消，不在本护栏范围内；护栏只影响此前“生成后立刻被同一调用栈删除”的无效 spawn，不会改变已在生效的交互对象
superseded_by: none
first_check: resetPatternState/releaseTrackedSpawns 是否把“生成者生命周期结束事件链里生成的子对象”与“普通战斗期子对象”区分开
-->

- **症状**：击杀 Celestius（215488/246242）后，真端 pattern `Elim_ComadAe.on_killed_by_user` 应当刷出卡斯帕的幻影 799503（`CaspaGhost_01`，`SPAWN_ID_4`、`live_time=0`），但副本里完全没有该 NPC，任务 10032 拿到心脏后无法交付；同类“死亡时刷 NPC/传送门/控制物”的 pattern 共 127 条记录都受影响。
- **根因链**：
  1. `NpcController:244` 先抛 `AIEventType.DIED` 给自身 AI → `handleKilled` → `runEvent("on_killed_by_user")` → `spawn` 生成 799503 并登记进 `spawned[SPAWN_ID_4]`；
  2. `NpcController:245` 紧接着抛 `AIEventType.DIED` 通用事件 → `handleDied()` → `runDeathEvent()` → `resetPatternState()`；
  3. `releaseTrackedSpawns()` 只放过 `selfManagedSpawns`（IR-009 起仅 `live_time>0`），`live_time=0` 的对象走 `despawnForLifecycle()`：`despawn_at_attack_state=FALSE` 且自己不在战斗 → 立即 `onDelete()`，同一次死亡处理里被删除。
- **真端依据**：真端 `<spawn_id>` 只用于显式 `<despawn spawn_id>`；`Elim_ComadAe.on_killed_by_user` 刷出 799503 后**没有**任何对该 spawn_id 的 despawn，说明它必须活到副本结束（IR-009 的同一原则，只是对象没有 `live_time`）。
- **修复与边界**：死亡/消失事件链（`on_die`/`on_killed_by_user`/`on_killed_by_npc`/`on_despawn`）里生成的子对象只保留登记、不随生成者状态重置删除，显式 `<despawn>` 与 `live_time` 语义不变；可逆的脱战事件（`on_leave_attack_state`）仍随重置释放，避免每次脱战泄漏标记物。
- **安全性论证**：这四类事件之后紧跟着 `resetPatternState()`，被标记的子对象此前一定是“生成后立即删除”的无效 spawn；护栏只让它们按真端意图可见，不会改变本已生效的对象。
- **实机闭环**：2026-09-16 击杀 Celestius 后幻影 799503 正常现身、10032 正常完成；由于“终端事件是否送达”仍不可静态判定，实例层同时保留 `RetailPatternAI2#spawnRetailActionNpc` 幂等补刷（见 IR-010），pattern 与适配器不会产生第二份实体。
- **教训**：真端对齐把“实例脚本兜底 spawn”删掉时，必须同时确认真端动作的产物能活过引擎自己的状态重置；`live_time>0` 与 `live_time=0` 两条路径要分开核对。

## [IR-012] 十二、副本销毁后残留的延迟任务必须自行收口 (INSTANCE_DELAYED_TASKS_SELF_TERMINATE_AFTER_TEARDOWN)
<!-- pattern-metadata
status: CONFIRMED
scope: 实例处理器用 GameThreadPoolServices 排定的延迟场景任务（spawn / killNpc / 移动指令），尤其 DrakenspireDepths 301390000 与 301520000
first_seen: 2026-09-19
last_verified: 2026-09-19
symptom: 副本销毁后日志持续刷“生成 NPC 209679/237219/237232/237217 时出错”，异常是 InstanceScaler.onBeforeSpawn → WorldPosition.getWorldMapInstance 的 NullPointerException（部分只记录裸 NPE）
root_cause: 场景延迟任务排期最长 87 秒，远超副本存活时间；任务体只检查玩家与坐标，不检查实例是否已销毁。副本销毁后 WorldMapInstance/mapRegion 已拆除，SpawnEngine.spawnObject 在 InstanceScaler.onBeforeSpawn 处 NPE；getNpcs() 返回的 null 列表还会让 killNpc 迭代时二次 NPE
fix_or_guardrail: 实例处理器覆写 spawn(int,float,float,float,byte)，isInstanceDestroyed 时直接返回 null；所有延迟任务入口（raidSeal / moveToSealForward / killNpc 与直接调用 SpawnEngine 的特效方法）对 null 返回值与 null 列表判空；守护写在统一入口而不是逐个延迟毫秒
evidence: commit e718118c5; src/main/java/com/aionemu/gameserver/instance/handlers/scripts/DrakenspireDepthsQInstance.java; src/main/java/com/aionemu/gameserver/instance/handlers/scripts/DrakenspireDepthsInstance.java; log/error.log:11267; log/error.log:11355; log/error.log:11379; src/test/java/com/aionemu/gameserver/instance/handlers/scripts/DrakenspireDepthsInstanceTeardownGuardTest.java; .agents/summary/quest-15300-orissan/2026-09-19-immortal-orissan-death-fallback.zh-CN.md
validation: focused-test（2026-09-19 mvn -B test -Dtest='DrakenspireDepthsQOrissanSceneTest,DrakenspireDepthsQTwinSceneTest,DrakenspireDepthsQTwinSpawnSurfaceTest,DrakenspireDepthsInstanceTeardownGuardTest,ImmortalOrissanAI2Test,ThresholdTransformDeathFallbackGateTest,Betrayer_IcaronixAI2Test'：17 例 0 失败 0 错误，BUILD SUCCESS）；运行期日志证据来自 2026-09-19 22:37:13/22:39:13/22:39:35；修复后客户端复测待执行
boundaries: 只覆盖实例处理器自身排定的延迟 spawn；AI 定时器、任务线程与引擎内部延时不在本护栏内；判空仅在 isInstanceDestroyed 为真时生效，不影响副本内的正常补刷
superseded_by: none
first_check: 副本销毁后仍在排队的延迟任务入口是否检查 isInstanceDestroyed，spawn 返回值与 getNpcs 列表返回值是否判空
keywords: 生成 NPC 时出错, NullPointerException, mapRegion is null, 副本销毁, instance teardown, isInstanceDestroyed, killNpc null
-->

- **症状**：龙脊深渊剧情副本（301390000）与任务副本（301520000）销毁后仍刷 NPE：`生成 NPC 209679 时出错，世界 301390000`（`Cannot invoke "MapRegion.getParent()" because "this.mapRegion" is null`）与 `生成 NPC 237219/237232/237217 时出错，世界 301520000`；后者只记录裸 `NullPointerException`。
- **根因链**：`237216`（Grave Cavity Rendclaw）等场景在死亡回调里排 25–87 秒后的 spawn 任务；副本超时/重置先执行 `onInstanceDestroy()` 拆掉世界实例，延迟任务随后仍在同一实例 ID 上调用 `SpawnEngine.addNewSingleTimeSpawn` + `spawnObject`，`InstanceScaler.onBeforeSpawn` 读 `position.getWorldMapInstance()` 时拿到 null。
- **护栏**：把守护放进统一入口——覆写 `GeneralInstanceHandler#spawn`，`isInstanceDestroyed` 为真直接返回 null；`raidSeal`/`moveToSealForward`/`killNpc`/直接 `SpawnEngine.spawnObject` 的特效方法全部对 null 判空，`getNpcs()` 的 null 列表不再被迭代。
- **边界**：这不是“副本销毁要取消所有任务”的通用实现，只是让延迟任务在实例拆除后不再触碰世界；如果后续把延迟任务改成可取消的 `Future` 集合，也应保留这层判空。

## [IR-013] 十三、出生 Z 兜底不得压掉作者摆在道具网格面上的站位 (SPAWN_Z_PREFERS_MATCHING_COLLISION_SURFACE)
<!-- pattern-metadata
status: CONFIRMED
scope: worlds with a terrain heightmap 下“可移动、非飞行”刷点的出生 Z 解析（SpawnEngine.projectedSpawnZ）与 geo/地形数据边界
first_seen: 2026-09-20
last_verified: 2026-09-20
symptom: NPC 应站在巨石、建筑、桥面、机关等道具上，实际出现在其下方的地面（例：Inggison 210050000 / 805334 LF4_Somation_E 作者 Z=489.7741 是巨岩顶面，运行期被压到 473.55777，低 16.22m）；`//geo z` 同时出现 `curZ == terrainZ`、`pathGround=null`、`spawnZ` 明显更高
root_cause: SpawnEngine.projectedSpawnZ 的兜底顺序是 PATH 可行走地面 → 地形高度 → 非攻击对象保留作者 Z → geo 面；地形高度图只描述地表、不含岩石/建筑等道具网格，PATH 又因 0.7m 垂直容差未命中，于是“作者 Z 落在碰撞网格上”的正确高度被地形高度覆盖
fix_or_guardrail: PATH 失败后先取地形：地形与作者 Z 贴合（≤1m，AUTHORED_SURFACE_DELTA）时直接采用地形；否则查 geo 碰撞面，贴合作者 Z（≤1m）则采用碰撞面；两者都不贴合才退回地形。无地形 world 的既有 keepsAuthoredZ/geo 兜底分支与 resolve_z 的 SpawnSurfaceResolver（geo 优先）保持不变；不要用“只给报障点加 resolve_z/fly”之类的单点数据补丁替代该顺序修正
evidence: commit 1bbfbc793（本次修复）；src/main/java/com/aionemu/gameserver/spawnengine/SpawnEngine.java:317-390；src/test/java/com/aionemu/gameserver/spawnengine/SpawnEnginePathProjectionTest.java（新增 prefersCollisionSurfaceMatchingAuthoredZOverTerrainFallback）；.agents/summary/inggison-somation-rock-z/2026-09-20-805334-somation-rock-top.zh-CN.md；离线复现 terrain float32=473.55777 等于运行期 curZ；geo 巨岩面 489.77418 等于作者 Z（岩石网格 na_l_dark_rockgnbig_02a）；真端 Inggison 出生表 npc_info 805334 z=491.812439；全量同族审计 47 world / 1342 候选 / 346 点；客户端实机验收（2026-09-20 用户确认）
validation: static + 离线复现完成（含全量同族审计：1342 个“作者 Z 高于地形 >1m 且 PATH 未命中”的刷点中 346 点脚下存在贴合碰撞面，会被地形兜底压到下层地面）；focused-test 未运行（未获授权：mvn -q -Dtest=SpawnEnginePathProjectionTest test）；客户端实机验收通过（2026-09-20 用户确认重启后 805334 站在巨石上）
boundaries: 贴合容差取 1m；地形与作者 Z 差 <1m 的分支不查 geo（启动性能），该区间内“网格面才是真站位面”的偏差不会被修正；不覆盖“作者 Z 低于下方网格面/位于网格内部”与 resolve_z 路线；TERRAIN_DISABLED_MAPS 或缺 PNG 的 world 行为不变；审计按 PHYSICAL 碰撞面与精确 XY 三角形包含复现，未覆盖 geo 其他碰撞意图
superseded_by: none
first_check: SpawnEngine.projectedSpawnZ 的兜底顺序，以及 //geo z 的 curZ / terrainZ / pathGround / spawnZ 四项对比
keywords: NPC 在石头下面, 出生在下方地面, curZ 等于 terrainZ, pathGround=null, spawnZ 明显更高, 作者 Z 被压到地面, rock top, prop mesh collision, 刷点高度, 贴地兜底
-->

- **现象判据**：`//geo z` 同时满足 `curZ ≈ terrainZ`、`pathGround=null`、`spawnZ` 明显高于 `curZ`，基本可以判定“作者 Z 落在网格碰撞面，被地形兜底压到下层地面”。
- **根因**：地形高度图（`geo/<world>.png`）只描述地表，岩石/建筑/桥面/机关等由 `geo/<world>.geo.gz` + `models.mesh` 提供；`PathData` 的 0.7m 垂直容差会判定“站在道具上的刷点”投影失败，随后的地形兜底与真实站位面相差可达十几米。
- **修复契约**：地形与作者 Z 贴合才用地形；不贴合时用“与作者 Z 贴合（≤1m）的 geo 碰撞面”；两者都不贴合才退回地形。这样既修“站在道具上被压到地面”，也保留“作者 Z 悬空（真端数据错误）时压回地形”的既有修复。
- **取证方法**：离线用 `models.mesh` + `geo/<world>.geo.gz` 复现 `GeoMap.getZ`（PHYSICAL 面 + 放置物 loc/rotation/scale），即可在不启动服务端的前提下给出该点全部碰撞面高度；见 `.agents/summary/inggison-somation-rock-z/geo_surface_probe.py`。
- **教训**：任何“贴地/兜底”修复都要区分“地表高度”与“碰撞面高度”，并用同族审计（world 级全量刷点 × geo 面）给出影响面，而不是只修报障的那一个点。

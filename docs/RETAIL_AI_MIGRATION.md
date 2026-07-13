# 真端 AI 迁移记录

本文是 AI 整体改造的轻量留痕，只记录已经确认的语义、落地结果、验证方式和保留回退项。

## 迁移约束

- 数据来源以 `/Users/mc/PycharmProjects/aion_drop/converted_staticdata_v2` 和真端 `58Server/Map` 为准。
- `definitions` 只迁移字段完整、运行语义明确的数据；缺失引用和不完整条件不补默认值。
- XML 生成脚本统一放在 `/Users/mc/PycharmProjects/aion_drop/staticdata_converter`。
- Java 日志统一使用 Lombok `@Slf4j`，不新增 `System.out` 或 `System.err`。
- AI Pattern 及其 AI 运行依赖 XML 统一位于 `definitions/compact/ai/`；技能分类属于技能数据，保留在 `definitions/compact/skills/skill-categories.xml`。
- 运行时技能模板已迁入 `definitions/compact/skills/`，以 `index.xml` 列出的 30 个分片为唯一权威；外部 `converted_staticdata_v3` 只作为生成来源。

## 当前数据

| 数据 | 当前结果 |
| --- | ---: |
| 真端 AI XML | 259 个，其中 242 个 Pattern 文件 |
| NPC 与 AI 映射 | 87,721 条 |
| AI 字符串 | 3,491 条 |
| 消息区域 | 134 个 |
| 动态复活区域 | 18 个完整区域，含 18 个目的 Alias 坐标 |
| 技能区域 | 276 个多边形，265 个世界内唯一 ID |
| 路径 | 3,042 条 |
| Location Alias | 356 个，529 个坐标点 |
| 条件刷新 | 4,430 个条件，5,643 个槽位 |
| 条件 NPC Party | 734 个 Party，4,177 个成员 |
| 静态 NPC Party | 92 个 Party，278 个成员 |
| 感知区域 | 39 个完整区域，覆盖 8 个世界、37 个 NPC ID |
| 动态移动碰撞区域 | 288 个，其中 WindBox 171 个、Jump 117 个 |
| 技能模板 | 14,517 条，30 个分片 |
| NPC 计分 | 2,829 条完整 NPC-ID 映射 |
| 直达传送门 | 30 条完整定义，含 6 条玩家持钥匙开启定义 |
| 限量任务 | 5 条全服名额定义 |
| 可结构执行 Pattern | 12,654 / 12,797（98.88%） |

上述 AI 派生 XML 均由 `staticdata_converter` 独立生成并通过 XSD 校验；字段或依赖不完整的数据继续留在生成器拒绝清单中，不迁入 `definitions`。

## 已确认语义

- `say_to_all` 调用真端普通 NPC 广播入口；`shout_to_all` 使用喊话入口。
- `say_to_all_str` 直接发送 Pattern 中的原始字符串，不经过本地化字符串表。
- `skill_level=0` 使用 NPC 技能表等级；非零值覆盖技能表等级。
- `change_world_scene_status`：高 16 位为 `StageType.type`，低 16 位为 `StageType.id`。
- `teleport_target` 使用当前地图实例内的绝对坐标；玩家目标可带传送特效。
- `on_leave_attack_state` 不是死亡事件。已支持“自身技能 + 自身技能或按 spawn_id 清理”的离战队列，并保留本次事件新排入的技能任务。
- `despawn_at_attack_state=TRUE` 允许子 NPC 在战斗状态中按生命周期删除；`FALSE` 在生命周期到期时等待子 NPC 离开战斗状态。
- `except_specialize` 是与真端 `SpecialSvrType` 相交时跳过刷新的位掩码；本项目普通服模式等同 `SpecialSvrType=0`。
- `add_battle_timer` 和 `set_idle_timer` 保留创建时的事件目标及消息，后续计时器条件可继续解析攻击者和 `USERI_EVENT_TARGET`。
- `goto_alias` 从当前世界的完整 Location Alias 中随机选择坐标，支持步行和跑步，并继续触发到点事件。
- `set_condition_spawn_variable_to_world` 只向目标世界当前全部实例传播；目标世界和变量必须同时存在于完整条件刷新数据中。
- `give_item_by_user_indicator` 的 `item_id` 是物品模板名称；NPC 级校验会拒绝当前物品模板中不存在的奖励。
- `activate_skillarea` 按当前世界和 `areaid` 读取真端 `Worlds/*/world.xml` 多边形；同一 ID 的多个区域作为并集使用。
- 技能区域只替换普通技能的距离/形状选区，敌友、物种、状态、抗性、效果与 AI 技能事件继续走现有技能引擎；`CASTOR` 和 `AREA` 分别按施法者与区域内玩家广播结果。
- `enable_area.op_code=1` 为启用，`0` 为停用；真端按当前世界实例和区域名前缀更新状态。
- `AI_CONTROL_AREA_RESURRECT` 在死亡位置命中启用区域后，按玩家种族/部族过滤，再从区域的 `location_alias` 随机选择复活坐标。
- `give_score` 将目标玩家与当前 NPC 交给主服；无覆盖值时按 NPC 的 `npc_scores.xml` 分值计分。本项目只在副本处理器显式接入时执行。
- `on_off_windpath` 的 `TRUE/FALSE` 分别对应客户端状态 `1/0`；状态按地图实例隔离，NPC 所在地图缺少对应 `groupid` 时不启用真端 AI。
- `on_off_moving_collision` 按当前地图实例和 `type + sunzoneid` 启停区域；WindBox 与 Jump 的客户端类型分别为 `0`、`2`，进入地图时同步当前状态，副本销毁时清理。
- `SKILLI_INDEX_N` 严格按 `npc-skills.xml` 原始槽位读取；无 ID 槽保留空位而不压缩，缺槽时整套 Pattern 回退旧 AI，禁止猜测技能。
- `on_gauge_begin` 在交互进度条开始后触发；移动、攻击、受击或使用技能会取消并触发 `on_gauge_stop`；成功到期先执行 `on_talked_by_user`，再执行 `on_gauge_end`。
- Gauge 时长直接使用真端 NPC `talk_delay_time`（秒）。字段缺失或为 `0` 时不启用该 NPC 的真端 Pattern。
- 真端普通怪物在 `on_wake_up` 后进入 5 秒 WakeUp 状态；进入和离开分别触发 `on_enter_wakeup_state`、`on_leave_wakeup_state`，离开后才进入空闲事件。
- `on_healed_by_user` 在玩家或其召唤物实际提高 NPC 的 HP/MP 后触发，并将该玩家作为事件目标；零治疗、NPC 自疗及 FP/DP 恢复不会触发。
- `on_see_master_spelling`、`on_see_master_spelled`、`on_master_attacked` 使用刷新时绑定的主人；命中主人分支后不再执行普通 friend/see 分支。
- `on_quit_cutscene` 使用客户端回传的目标 NPC object ID 精确投递，不向当前地图其他 NPC 广播。
- `play_cutscene_by_user_indicator` 将发起 NPC object ID 写入影片封包；仅 `CUTSCENE_PLAY_TO_USER` 支持 `teleport_alias`，客户端回传同一影片 ID 后才消费待传送记录并按当前世界 Alias 传送。
- `give_exp` 使用真端 64 位原始经验值直接奖励事件玩家，不叠加任务或狩猎倍率。

## 2026-07-13

- 支持非零 `skill_level`，旧 `bomb/summoner` 结构覆盖从 112 提升到 115。
- 修正离战技能链清理时机，结构覆盖从 115 提升到 119。
- 支持 187 个 `say_to_all_str` 动作。
- 支持全部 208 个 `teleport_target` 动作：玩家目标支持传送特效；后续真端证据确认 NPC 自身传送忽略 `showfx`，统一走普通离场与重新发现链。

## 2026-07-14

- 将 Pattern、NPC 映射、字符串、区域、路径、Alias、条件刷新、直达传送门、NPC 计分及旧式 Bomb/Summon 模板统一迁入 `definitions/compact/ai/`；`skill-categories.xml` 保留在 `definitions/compact/skills/`。
- 旧式 Bomb/Summon 模板改为通过 `Config.definitionFile(...)` 独立加载，`static_data.xml` 不再读取 `data/static_data/ai/`。
- 9 个 `staticdata_converter` 生成器同步使用新目录下的 XSD 相对路径；按真端输入重新生成后与项目 XML 逐字节一致。
- 离战事件中的自身技能继续沿用现有动作队列，技能完成后可执行同一规则中的移动、门、刷新和变量动作；结构覆盖由 12,195 提升到 12,255，净增 60 个 Pattern。
- 支持 54 个 `on_off_windpath` 动作；进入地图时同步当前实例覆盖状态，副本销毁时清理。涉及的 25 个 Pattern 中 21 个完整放行，结构覆盖由 12,255 提升到 12,276。
- 迁入 288 个动态移动碰撞区域，覆盖 171 个 WindBox 和 117 个 Jump；`on_off_moving_collision` 复用 `SM_WINDSTREAM_ANNOUNCE` 发送类型 `0/2`，状态按实例隔离，并支持日常时段、跨午夜时段、`always_enabled`、生命周期到期关闭、进图同步和副本销毁清理。
- `RetailDynamicAreaEngineTest` 覆盖普通时段、跨午夜、常开状态、WindBox/Jump 类型映射、实例隔离及 `clear()`，专项 5 项测试通过。
- 将 `on_end_feared` 映射到现有效果结束回调的 Fear 离开状态位，恢复 7 个 Pattern；结构覆盖由 12,276 提升到 12,283。
- `goto_alias` 到点时触发 `on_arrived_at_point`，恢复 4 个完整 Alias 运行链；5 个相关 Pattern 均可结构识别，覆盖由 12,283 提升到 12,288。仅使用编号 waypoint 的 `IDCT_SumWind` 继续在 NPC 级校验中保留旧 AI。
- `generate_npc_ai_mappings.py` 从真端 sparse NPC 模块生成 `talk_delay`，`npc-ai.xml` 的 87,721 条映射全部带该字段并通过 XSD 校验。
- NPC 技能加载保留 317 个无 ID 原始槽，避免后续技能索引错位；项目内当前 14,517 个技能模板参与 NPC 技能引用闭包校验。
- `XmlDataLoader` 已直接读取 `definitions/compact/skills/index.xml`、`groups.xml` 和 29 个模板分片；旧 `data/static_data/skills/skill_templates.xml` 不再参与运行时加载。
- 当前项目内 `definitions/compact/skills/` 是技能数据唯一运行时来源，包含 14,517 条模板以及 `npc-skills.xml`、`skill-categories.xml`、`charge_skills.xml` 和 `motion_times.xml`；不再等待外部 `converted_staticdata_v3` 文件落地。
- `SkillData` 的冷却组、技能 ID/组索引和独占属性映射均为加载后派生数据，已统一标记为 `@XmlTransient`，避免 JAXB 将 `Map<Integer, Set<String>>` 等运行时容器误当成 XML 契约。
- 重新迁移后的真端模板纠正了测试基准：技能 838 为 `effective_angle=26`、`effective_width=5`，技能 4769 为 `stance_type=2`；聚焦测试按当前权威 XML 验证，不反向修改模板数据。
- 当前重新生成版本已通过完整打包和整服启动验收：以 `-Xms2g -Xmx8g` 在 17 秒内进入“服务器已就绪，可接受连接”，实际加载 14,517 个技能模板、59,058 个 NPC 技能列表、87,721 条 NPC AI 映射、4,430 条条件刷新、92 个静态 NPC Party、288 个动态区域和 3,042 条路径，并通过优雅停服。刚就绪后立即停服会中断一个正在执行的 PATH 任务并记录 `ClosedByInterruptException`，不属于 AI/技能数据加载失败，PATH 关闭日志需单独收口。
- 在线堆统计中共有 99,268 个 `Npc` 实例，其中 80,637 个使用 `RetailPatternAI2`，本次已刷新 NPC 的实际装配率为 81.23%。该数值只代表本次启动时已经刷新的对象，不等同于静态数据覆盖率。
- 上一版曾对 87,721 条 `npc-ai.xml` 映射做互斥闭包审计：66,019 条进入静态运行候选，2,438 条因技能槽回退，19,264 条因 Pattern 或其他依赖回退，严格静态理论上限为 65,151 条；当前结果见下方最新闭包记录。
- 真端技能模板启动验收发现并补齐 `ReturnCoolReduce`、`SprintFpReduce`、`OdellaRecoverIncrease`、`DeathPenaltyReduce` 四类效果的 `EffectType` 与效果保留标记；修复后启动日志中的“缺少效果类型”归零，并新增全量断言保证全部模板效果均可解析出 `EffectType`。
- `generate_retail_ai_strings.py` 补采 `send_system_msg` 引用，从真端 `localization.xml` 新增 112 条完整字符串，AI 字符串由 3,379 增至 3,491；`ai_string_missing` 由 249 归零，恢复 223 条可静态启用的 NPC 映射。
- `generate_retail_condition_spawns.py` 同时采集本地和跨世界条件变量写入；用全部真端世界文件重新生成后为 4,430 条条件、5,643 个槽位、733 个完整变量，其中包含 8 条无条件感知刷新，且与项目 XML 的 SHA-256 一致；剩余跨世界变量受不完整条件闭包阻塞，不补空定义。
- 一次性闭合 `SKILLI_ANY_SKILL`、`on_die + OBJI_KILLER`、`control_door method=0` 和 `on_spelled + OBJI_CASTER` Alias 传送四类真端语义：随机技能复用 `NpcSkillList.getRandomSkill()`，死亡事件沿用控制器传入的实际 killer，门方法 `0` 与现有非 `1` 分支一致关闭，施法者传送复用现有玩家 Alias 链。结构覆盖增加 17 个 Pattern，静态映射结构拒绝由 104 降至 58，恢复 46 条映射。
- `USERI_MASTER` 改为读取当前 AI NPC 的主人，并在 Pattern 装配时校验主人依赖；恢复 3 个 Pattern、8 条映射，可结构执行 Pattern 增至 12,634，静态映射结构拒绝降至 50。
- `play_cutscene_by_user_indicator` 补齐 `USERI_TALKER`、发起 NPC object ID、影片 ID 匹配消费和延迟 Alias 传送；转换器同步采集 `teleport_alias`，从真端世界数据生成光/魔两个 `IDAb1_Heroes_L_Airport_End` 坐标。恢复 2 个 Pattern、2 条映射，可结构执行 Pattern 增至 12,636，静态映射结构拒绝降至 48。
- NPC 自身传送闭合前的最近一次映射闭包审计：87,721 条映射中 66,090 条进入静态运行候选，2,450 条因技能槽回退，19,181 条因 Pattern 或其他依赖回退；严格静态理论上限为 65,222 条（74.35%），14,517 个技能模板下无 NPC assignment 指向无效模板。
- `reset_hatepoints` 已按真端参数区分全量仇恨与临时仇恨：`volatile_hatepoint_only=TRUE` 只清除定时仇恨贡献，`is_except_most_hating=TRUE` 保留当前最高仇恨者；旧定时任务用令牌消费，不会二次扣减。恢复 `IDAbRe_Core_Summon8` 的 3 条映射。
- 修正第 7/8/9/10 阶段通过状态为真端 `407105/508105/609105/810105`，并补入第 9 阶段奖励状态 `609006`；结构拒绝由 45 降至 35，恢复 10 条映射，其中 9 条直接启用、1 条仍因 NPC 技能槽回退。
- `change_world_scene_status` 在副本死亡处理已先写入同一状态时保持幂等，不重复推进或发奖励；只有天界熔炉 `300300000` 与单人挑战 `300320000` 可装配包含该动作的 Pattern，复用同一 Pattern 的野外 NPC 继续回退。
- 已核对 NPC 死亡调用顺序：副本 `onDie` 先完成阶段推进和奖励，随后 AI 执行 `on_die`；因此第 7～10 阶段不在 `onChangeStage` 重复实现，AI 只通过幂等判断确认相同状态。
- 真端 `use_skill`、`use_skill_by_attacker_indicator` 在 `SKILLI_NONE` 时返回动作失败，不静态启用；`attack_most_hating + SKILLI_NONE` 仅切换到最高仇恨目标，可以启用。`activate_skillarea + SKILLI_NONE` 同样继续拒绝。
- 使用自然刷新的 Ragnarok（NPC `216576`、Pattern `DF4_FieldRaid`）验证服务器侧阶段事件链：该 NPC 位于 Gelkmaros 出生数据，`281811` 仅共享 Pattern 和技能组、没有出生点。测试在 80%/60%/40%/20% HP 分别通过真实 `on_battle_timer + BTIMERI_INDEX_0` 条件命中优先级 `10/11/12/15`，设置对应阶段标记，并闭合阶段自身技能 `19207/18675/19207/19208`、等级 `46`、技能模板及最终 `NpcController.useSkill` 调用；该测试不代表客户端自然联动已经验收。
- 2026-07-15 使用 Windows 5.8 客户端完成同一自然刷新 Ragnarok 的实战验收：在线确认 AI 为 `retail_pattern`，80%/60%/40%/20% 四阶段均能使用技能，阶段过程中可见拉拽和寄生虫 `281950` 刷新；服务端同时记录 `19190/19192/19209` 等 46 级技能。Boss 死亡后寄生虫自动清理，阶段战斗链与召唤物生命周期均通过。
- NPC 掉落公共组引用已保留真端 `common_drop_adjustment`，运行时按 `基础 chance * adjustment / 100` 计算后再应用服务器掉落修正；普通组同步支持可选 `drop_group_adjustment`，但当前真端 NPC 数据没有该字段，不生成猜测值。Ragnarok 戒指组基础概率 `0.00946%`、专属倍率 `266400`，运行时基础概率为 `25.20144%`，`//dropinfo` 显示倍率后的概率。
- `generate_npc_ai_mappings.py` 可联合检查 `npc-ai.xml -> Pattern -> SKILLI_INDEX -> npc-skills.xml`，分别报告无 assignment、越界和孤儿槽；按当前 Pattern 与 v3 技能数据重算为 828/1,620/3，三类互斥，共影响 2,451 个 NPC 映射。项目 `npc-skills.xml` 与最新 v3 文件 SHA-256 一致，不是迁移遗漏；v2 反而多 5 个无 ID 孤儿槽。
- 支持全部 20 个 `on_gauge_begin/stop/end` Pattern，并补齐 Gauge 消息中的 `OBJI_TALKER`；结构覆盖由 12,288 提升到 12,311，净增 23 个 Pattern。
- 当前 12 个 Gauge Pattern 可映射到 16 个 `talk_delay > 0` 的 NPC。`702010-702013` 的真端数据没有时长，继续使用旧 AI；4 个 `IDF5_U3_TEMP_*` Pattern 没有 NPC 映射，也不自动启用。
- 支持 WakeUp 状态进入/离开事件，复用现有 5 秒 `NPC_STATE_WAKE_UP` 窗口并在重生时隔离过期任务；结构覆盖由 12,311 提升到 12,322。
- 12 个 WakeUp Pattern 中 10 个映射到 44 个 `MONSTER` NPC。`IDF5_TD_Siege` 仍受 NPC party 事件阻塞，`IDKamar_Britra_As_Hide_Party` 没有 NPC 映射，不自动启用。
- 支持唯一的 `on_healed_by_user` Pattern；瞬时、持续和条件治疗均按实际 HP/MP 增量触发，结构覆盖由 12,322 提升到 12,323。
- Retail `spawn` 生成的子 NPC 继承当前 NPC 为主人，并在 AI 装配前可见；支持 13 个使用主人施法、受法和受击事件的完整 Pattern，结构覆盖由 12,323 提升到 12,336。
- 接入唯一的 `on_quit_cutscene` Pattern，复用 `CM_PLAY_MOVIE_END` 已有的目标 NPC 与影片 ID，结构覆盖由 12,336 提升到 12,337。
- 支持 9 个 `give_exp` 动作，恢复 Luna 分级经验奖励与真端测试 NPC 共 2 个 Pattern；结构覆盖由 12,337 提升到 12,339。
- `has_attack_damage_flag=DODGE` 使用普通攻击 `AttackResult` 和技能 `Effect` 的真实命中状态，不以零伤害猜测；恢复 3 个竞技场 Pattern，结构覆盖由 12,339 提升到 12,342。
- 支持 `system_message_to_all_by_obj_indicator_param`，将对象参数写入系统消息上下文并广播给当前实例在线玩家；转换器同步补入 `STR_IDRUN_STAGE2_NOTICE`，结构覆盖由 12,342 提升到 12,343。
- 物品模板同时按显示名和真端内部 `name_desc` 建索引，接入 `give_item_by_obj_indicator(OBJI_TALKER)`，结构覆盖由 12,343 提升到 12,344。
- 放行计时器事件目标、大数值仇恨百分比、感知区 `USERI_EVENT_MAKER`、Alias 消息/当前目标及受击施法者上下文，结构覆盖由 12,344 提升到 12,372。
- 终止事件仅支持真实数据中的 `use_skill(OBJI_SELF) -> despawn(SPAWN_ID_1)` 两步清理链，不创建死亡后的延迟任务；恢复 7 个 Pattern，结构覆盖提升到 12,379。
- 接入 `on_quest_finished` 和 `is_target_quest_state`；任务状态按 `NONE/LOCKED`、`START/REWARD`、`COMPLETE` 映射，成功结算后只通知结算 NPC。恢复 4 个 Pattern，结构覆盖提升到 12,383。
- 条件刷新生成器按 slot 候选保留真端 NPC Party，生成 734 个 Party、4,177 个成员及稳定 token；相关路径闭包扩展到 3,042 条。当时仅扫描 `world_N.xml`，条件刷新为 4,394 个条件、5,607 个槽位；加入缺失 `world_N.xml` 时回退 `world.xml` 后，5 个世界新增 36 个完整条件/槽位，当前为 4,430/5,643。
- 运行时已按同实例、同显式 Party token、排除发送者接入 4 个 `on_party_mbr_*` 事件和 `broadcast_message_to_party`；恢复 11 个 Pattern，结构覆盖由 12,383 提升到 12,394。
- `open_directportal_by_user` 只迁入字段完整的 82/83/84/94/95/96：校验请求玩家钥匙和 `group_id` 互斥，Portal 创建成功后才消费钥匙，消费失败时关闭刚创建的 Portal；完整定义由 24 条增至 30 条。
- 新增全服持久化限量任务计数、原子接取扣减和封顶恢复；`charge_limitedquest` 读取 5 条真端配置，恢复 3 个 Pattern。连同玩家 Portal，结构覆盖由 12,394 提升到 12,403。
- `generate_retail_ai_strings.py` 和 `generate_retail_skill_categories.py` 均保留为独立转换器，并补充生成与拒绝不完整输入的单元测试。
- `generate_npc_ai_mappings.py`、`generate_retail_ai_areas.py`、`generate_retail_ai_location_aliases.py`、`generate_retail_ai_strings.py`、`generate_retail_ai_waypoints.py`、`generate_retail_condition_spawns.py`、`generate_retail_skill_categories.py` 共 21 项转换器测试通过。2026-07-15 真实重生成得到 87,721 条 NPC AI 映射、134 个消息区域、276 个技能区域、18 个复活区域、231 个任务区域、1 个限制区域、56 个 Group Controller、112 个 Group Control 区域、356 个 Alias（529 个点）、3,491 条 AI 字符串、3,042 条路径、14,457 条技能分类以及 4,430 条条件刷新（5,643 个槽位）；八个项目派生 XML 的 SHA-256 均与当前文件一致。
- 支持 10 个 `say` 动作，按 `USERI_SEEN` 或 `USERI_TALKER` 只向对应玩家发送 NPC 系统消息。
- `generate_retail_ai_strings.py` 已采集 `say.string_id`，重新生成后补入 3 个真实字符串引用。
- 支持 5 个 `send_message` 动作；当前真端数据均为 `OBJI_SELF`，按真端语义延迟 1ms 投递 `on_message`。
- 感知区域进入/离开事件的 `USERI_EVENT_MAKER` 使用事件携带的玩家；`play_cutscene_by_user_indicator` 可按真端 `CUTSCENE_PLAY_TO_ALLIANCE` 向其当前联盟发送影片，无联盟时只发给触发玩家，结构覆盖增至 12,645。
- 真端 MainServer `Npc_RequestTeleport` 已证明忽略 NPC 自身传送的 `showfx` 参数；普通 NPC 始终使用 `RemoveType/PutType=0/0`。运行时复用 KnownList 的正常离场删除和重新发现链，不触发 despawn 生命周期，坐标与 Alias 两类自身传送均支持 `TRUE/FALSE`；恢复 9 个 Pattern、12 个 NPC 映射的结构资格，结构覆盖增至 12,654。
- 条件刷新生命周期测试已贯通 `initialize -> SpawnEngine.spawnObject -> setVariable -> NpcController.onDelete -> despawn/remove`，证明变量切换会按真实 NPC 生命周期删除旧槽位对象。
- NPC Party 测试已贯通 `SpawnTemplate.npcPartyId -> Npc`，并证明同一地图实例内的同 Party NPC 可以发现彼此；现有事件测试继续覆盖 4 类 Party 事件及上下文分发。
- 5.8 封包回归已固定普通 NPC 离场 `SM_DELETE` 的 6 字节载荷，以及 Gauge `SM_USE_OBJECT`、动态碰撞 `SM_WINDSTREAM_ANNOUNCE` 的 13 字节字段顺序和小端序。
- 支持 10 个 `return_to_spawn_point`，复用现有 `RETURNING` 归位状态机。
- 支持 30 个 `reset_queued_actions`，只清动作续链，不清 AI 计时器、变量或刷新物。
- 统一对象条件校验：消息参数、看见目标、攻击者和施法者可按事件上下文执行敌我、生命值和飞行状态判断。
- 修正 `use_skill` 的 `OBJI_SEEN`、`OBJI_ATTACKER`、`OBJI_FRIEND`、`OBJI_KILLER`、`OBJI_TALKER` 目标解析。
- `is_skill_count_left` 按真端字段只要求技能索引；`is_event_skill_category` 覆盖全部已有技能事件。
- 支持 NPC 自身 `teleport_target_alias`；真端忽略该目标的 `showfx`，运行时统一按普通离场与重新发现处理。
- 整体结构覆盖由 10,329 提升到 10,773，净增 444 个 Pattern。
- 承接 `spawn` 的战斗状态离场、空中刷新和普通服专精过滤，整体结构覆盖提升到 11,521，新增 748 个 Pattern。
- 旧 `bomb/summoner` Boss 结构覆盖由 119 提升到 125。
- 战斗和空闲计时器保留事件上下文，补齐计时器中的攻击者飞行、职业等判断。
- 支持 `goto_alias` 及 `NPC_STATE_GOTO_POINT`，复用现有移动控制器和到点事件。
- 支持完整数据范围内的跨世界条件变量、按用户目标发放物品和动态可攻击状态切换。
- 本轮结构覆盖由 11,521 提升到 11,813，净增 292 个 Pattern；其中最近恢复点由 11,746 提升到 11,813，净增 67 个。
- `generate_retail_ai_areas.py` 已扩展为同时生成 134 个消息区域和 276 个完整技能多边形；`idinfinity` 中 11 组同 ID 异形区域原样保留。
- 剩余消息区域缺口只有 `ab1_ship_msg`、`abqup`、`Door_care_msg`、`InvadePortalDest_39_DefGroupOFFArea`、`InvadePortalDest_40_DefGroupOFFArea` 五个唯一名称；在真端 World、转换结果和客户端数据中均只有 AI 引用、没有多边形定义，按“只迁移完整数据”要求继续回退。
- 支持 549 个使用 `SKILLI_INDEX_*` 的 `activate_skillarea` 动作；相关 421 个 NPC 的技能索引完整，涉及的 28 个技能模板全部存在。3 个 `SKILLI_NONE` 动作继续拒绝。
- 整体结构覆盖由 11,813 提升到 11,924，净增 111 个 Pattern。
- `generate_retail_direct_portals.py` 从真端 `direct_portal.xml`、`Worlds/*/world.xml` 和数值 NPC 主表生成 24 条字段与坐标完整、无入侵/额外 AP/强退等特殊语义的直达传送门；其余 106 条 AI 引用继续拒绝。
- 支持 `open_directportal`、`close_directportal` 的入口/出口刷新、持续时间、等级、通行次数、手动与超时关闭；入口复用真端 `700137` 的 3 秒使用进度，出口 `700138` 不可交互。
- 24 条完整定义涉及 30 个动作、25 个 Pattern 和 27 个 NPC；按整个 Pattern 依赖闭包校验，当前只有 3 个 Pattern（5 个 NPC）全部引用完整，可在运行时启用。
- AI 装配由仅替换旧 `bomb/summoner` 改为所有 NPC 均执行完整依赖校验：通过则使用 `retail_pattern`，否则保留原 AI。
- 结构覆盖由 11,924 提升到 12,095，净增 171 个 Pattern；结构可识别不等于运行依赖完整。
- `generate_retail_ai_location_aliases.py` 同时采集 `TargetTeleport`、`teleport_target_alias` 和 Cutscene `teleport_alias`；当前从真端完整生成 356 个 Alias、529 个坐标点。
- `generate_retail_ai_areas.py` 增加 `enable_area` 复活区闭包生成，从真端世界数据迁入多边形、种族、部族、目的 Alias 和坐标完整的 18 个区域。
- 运行时支持实例级复活区启停，并让副本复活优先使用死亡位置命中的真端复活区；实例销毁时清理动态状态。
- 只把已接入真实复活消费者的 `RESURRECT` 计入支持，结构覆盖由 12,095 提升到 12,109，净增 14 个 Pattern；NPC 运行时仍按所在世界检查区域前缀，缺失依赖时保留原 AI。
- `AttributeShapeResurrect_Area13`、`AttributeShapeResurrect_Area14` 在真端世界数据中无定义；`LDF5_Fortress_QuestArea_q23817` 实际不是复活区，相关 Pattern 继续拒绝。
- `generate_retail_npc_scores.py` 将真端 2,829 条 NPC 计分定义与 `npc-ai.xml` 对齐为数值 NPC-ID 主键；6 条采集计分不属于 NPC AI，未迁入。
- `give_score` 首批接入卡马尔战场 12 个 NPC 和奥菲丹战路 3 个 NPC，复用各副本现有组队分摊、阵营总分、个人分数与封包逻辑。
- 结构覆盖仍保持 12,109：计分动作必须经过具体 NPC、分值特殊字段和当前副本消费者校验，未接入世界不计为完成。
- 伊德盖尔地标战 8 个候选 NPC 的旧硬编码分值与真端表冲突，未启用；`score_apply_type` 或 `equalizing_score` 非零的定义也继续拒绝。
- 支持 `on_user_enter_sensory_area` 和 `on_user_leave_sensory_area`。从真端 `world_N.xml` 迁入 35 个常驻、4 个条件刷新的完整 3D 多边形，按世界、NPC ID 和刷新坐标绑定。
- 感知 NPC 生成时检查当前实例玩家，后续复用现有 500ms 移动通知做进入/离开状态跃迁；不受 95 米已知列表限制，也未新增轮询线程。
- 条件刷新由 3,436 个条件、4,616 个槽位提升到 3,448 个条件、4,655 个槽位；结构覆盖由 12,109 提升到 12,195，净增 86 个 Pattern。
- 分页、生命周期、可重生或条件依赖闭包不完整的感知刷新继续拒绝，不进入 `definitions`。

## 保留回退项

以下项目缺少完整真端字段、引用或下游消费者，按“只迁移完整数据”约束继续使用旧 AI，不属于本次迁移未完成项。

- 当前 12,797 个去重 Pattern 中，12,654 个通过通用结构校验（98.88%）；映射级结构拒绝剩余项主要是 `SKILLI_NONE`、缺失数据及测试 Pattern，不按文件逐条猜测放行。
- 通用语义拒绝剩余 16 个 Pattern：9 个真端明确返回失败的 `SKILLI_NONE`，1 个没有竞技场计分消费者的 `USERI_EVENT_MAKER` 计分，1 个空 `pathname` 路径起点，以及 5 个无刷新消费者的测试奖励/时间语义。唯一空路径起点 `IDSeal_Guardian_Chief_02` 仍缺可证明坐标；竞技场 `207101` 虽有完整 100 分定义，但项目没有 `300450000` 的计分处理器，不能让 NPC 先消失后不加分。
- 19,160 条 `missing_pattern` 映射中，19,116 条可回退到已注册旧 AI；真正没有 NPC 模板承载回退链的为 44 条，因此 `missing_pattern` 不是等量的运行失效 NPC。
- NPC 技能槽：全部映射中 2,451 个 NPC 映射缺完整槽（828 个无 assignment、1,620 个索引越界、3 个孤儿槽）；其中 HERO 无缺口，LEGENDARY 有 35 个。项目文件与最新 v3 完全一致，必须补同版本真端 NPC 技能数据，不能从相邻阶段或旧服猜测。
- `SKILLI_ANY_SKILL` 已按真端特殊枚举接入，运行时从当前 NPC assignment 的可用技能中随机选择。真端 `AP_UseSkill` 只将枚举值 `7..26` 映射到技能槽，`SKILLI_NONE=0` 会得到无效索引 `-1` 并返回动作失败，不是成功的 no-op；因此 `use_skill(SKILLI_NONE)` 继续回退，不从模板或相邻槽位猜测。
- `give_score`：已迁移完整 NPC 计分表并接入 15 个 NPC；其余世界需要先接入对应副本计分消费者，特殊阵营/均衡计分还需补语义。
- `set_condition_spawn_variable_to_world`：当前只放行目标世界和变量均已迁入条件刷新数据的 Pattern；`LF3/DF3/LF6` 等缺少完整条件刷新数据的目标继续拒绝。
- 结构通过 Pattern 中的刷新名称缺口实际只有 14 个唯一名称、影响 142 条映射；这些名称在项目 NPC 模板和重新生成的真端 NPC 主表中均不存在，继续回退，不从相似名称猜测 ID。
- 直达传送门剩余 105 个唯一 ID 已一次性审计：73 个依赖当前未实现的特殊语义，32 个缺完整端点坐标，当前没有可在“不猜测、不迁移不完整数据”约束下直接补入的定义。
- 限量任务计数链已完成；9645/9661/9663/13816/23816 虽能从真端恢复任务字段，但当前项目没有这些任务的接取 NPC 刷新，13816/23816 相关 NPC 全部缺失且真端最低等级为 999，单独迁入 Quest 模板仍无法接取或完成，因此不为 AI 覆盖率迁移不可运行任务。
- `is_on_time` 只出现在 `Test_Basic_Monster_AI_JSM_1`；对应 NPC `287240` 以及测试金币、深渊点、世界分数 NPC `287229/287230/287231` 均只有模板、技能和 AI 映射，没有任何刷新消费者，不新增只服务不可达测试 NPC 的时间表或奖励运行链。
- 玩家开启 Portal 已完成钥匙、互斥组和 `invade_type=5` 基础运行链；真端世界级 Portal 类型计数和专用开放通知尚未接入，不影响当前六条 Portal 的创建、消费和通行。
- `enable_area` 已接入 `RESURRECT`、`GROUPCTRL`、`QUESTSCRIPT`、`LIMIT_NOPARK`、`LIMIT_NORECALL`；仍只放行区域定义及下游模板完整的 NPC。
- `give_score` 其余 Pattern 和直达传送门剩余 106 条引用需先补特殊语义或运行时消费者。
- Gauge 已完成数据和运行时接入，服务器启动及 5.8 封包字段回归已通过；本次 Windows 客户端实战验收范围为 Ragnarok 阶段战斗链，不包含 Gauge 专项交互。
- 服务器启动、真实数据加载、在线 AI 装配及 Ragnarok 80%/60%/40%/20% 四次阶段转换均已通过服务端回归和 Windows 5.8 客户端实战验收。

## 验证基线

```bash
mvn -q -DskipTests compile
mvn -q -Dtest=RetailAreaEngineTest,RetailConditionSpawnEngineTest,RetailDirectPortalEngineTest,RetailDynamicAreaEngineTest,RetailPatternAI2Test,RetailWindstreamEngineTest,AI2EngineRetailSelectionTest,RetailAiDefinitionLoaderTest,MPHealInstantEffectTest,SkillSpelledEventTest,CMPlayMovieEndTest test
mvn -q -Dtest=RetailPatternAI2Test,RetailQuestStateTest,QuestServiceRetailEventTest,RetailConditionSpawnEngineTest,RetailNpcPartyTest,RetailNpcPartyEngineTest,RetailConditionSpawnPartyLoaderTest,RetailAiDefinitionLoaderTest test
mvn -q -Dtest='XmlDataLoaderTest#aiDefinitionsLoadDirectlyFromCompactBundle+skillSupportDefinitionsLoadDirectlyFromCompactBundle' test
mvn -q -Dtest=SkillDefinitionLoaderTest,NpcSkillDefinitionLoaderTest,NpcSkillListTest,NpcSkillRuntimeClosureTest test
mvn -q -Dtest=SkillDefinitionLoaderTest,PersistentUtilityEffectTest test
mvn -q -Dtest=SMDeleteTest,SMUseObjectAndWindstreamPacketTest test
mvn -q -Dtest='Retail*Test,*Retail*Test,NpcSkill*Test,AI2EngineRetailSelectionTest,ThinkEventHandlerTest,AttackEventHandlerTest,AttackManagerLeashTest' test
```

扩展共享战斗或数据加载行为后，再运行相关的条件刷新、技能事件、仇恨和效果回归测试。

2026-07-15 在 NPC 自身传送闭合后再次执行完整 Retail AI/NPC Skill 回归与 `mvn -q -DskipTests package`，均通过；加载器实算结构覆盖为 12,654/12,797。新构建以 `-Xms2g -Xmx8g` 完成整服启动，17 秒进入“服务器已就绪，可接受连接”，随后优雅停服且全部线程池归零。运行时加载 14,517 个技能模板、59,058 个 NPC 技能列表、87,721 条 NPC AI 映射、4,430 条条件刷新、92 个静态 NPC Party、288 个动态区域和 3,042 条路径。

按当前验收范围，AI 整体改造的服务端数据、运行时、封包、阶段回归、完整打包及整服启动均已完成；Windows 5.8 客户端已完成 Ragnarok 四阶段技能、拉拽、寄生虫刷新及死亡清理实战验收，Gauge 等其他专项交互仍以服务端回归为准。

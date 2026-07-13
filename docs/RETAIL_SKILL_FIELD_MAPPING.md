# 真端技能字段对应基线

## 1. 范围

本文件只对应真端字段，不实现技能执行逻辑。

运行时输入：

```text
src/main/resources/aion/definitions/compact/skills/index.xml
src/main/resources/aion/definitions/compact/skills/groups.xml
src/main/resources/aion/definitions/compact/skills/skill_templates_part_001.xml ... part_029.xml
```

外部转换目录只负责生成这些文件，不参与服务器运行时读取。

当前运行时 compact bundle 已重新生成并包含 14,517 条技能模板；14,457 是技能分类数量。下文 14,494、14,410 等数量是上一版 V3 字段审计快照，只用于保留当时的对照证据，后续重新完成全字段差异审计后再统一刷新。

上一版字段审计快照包含 14,494 条技能记录和 357 个字段定义：

- 145 个技能级字段；
- 212 个 `effect1`～`effect4` 字段；
- 231 种原始 `effectN_type` 拼写；
- 忽略大小写和下划线后为 174 种效果类型。

对应目标是当前 `SkillTemplate`、`Properties`、`Conditions`、`Actions`、`Motion` 和 `EffectTemplate`。字段被列为“目标明确”只表示知道应进入哪个运行模型，不表示数值转换已经证明正确。

状态说明：

| 状态 | 含义 |
|---|---|
| 已对应 | 当前模型中存在明确目标，且可由同 ID 技能或 JAXB 模型证明 |
| 待转换 | 目标结构明确，但枚举、单位、正负号、默认值或组合规则仍需验证 |
| 待证明 | 当前证据不足，不能猜测或丢弃 |
| 效果专用 | 必须按 `effectN_type` 解释，禁止做全局位置映射 |

## 2. 已对应的技能级字段

| 真端字段 | 当前目标 | 处理 |
|---|---|---|
| `id` | `SkillTemplate.skillId` | 正整数直接映射，必须唯一 |
| `name` | `SkillTemplate.namedesc` / XML `name_desc` | 内部技能键；14,410 个共同技能全部一致 |
| `desc` | 文本资源查询 → `name`、`nameId` | 不能直接写入显示名称 |
| `type` | `SkillTemplate.type` / `skilltype` | 枚举规范化；当前仍有 508 个共同技能值差异 |
| `sub_type` | `SkillTemplate.subType` / `skillsubtype` | 枚举规范化；当前仍有 42 个值差异 |
| `skill_group_name` | `SkillTemplate.skill_group_name` | 字符串映射；当前有 11 个值差异 |
| `delay_id` | `SkillTemplate.delayId` | 整数映射；双方均有值时存在 25 个差异 |
| `activation_attribute` | `SkillTemplate.activationAttribute` | 枚举规范化；当前有 27 个差异 |
| `dispel_category` | `SkillTemplate.dispelCategory` | `Buff/DebuffPhy/DebuffMen/Npc_*` 枚举转换 |
| `required_dispel_level` | `SkillTemplate.reqDispelLevel` | 整数映射；当前有 78 个差异 |
| `target_slot` | `SkillTemplate.targetSlot` | 枚举规范化；当前有 485 个差异 |
| `hostile_type` | `SkillTemplate.hostileType` | 双方均有值的 4,895 个技能完全一致 |
| `cancel_rate` | `SkillTemplate.cancelRate` | 整数映射；当前有 90 个差异 |
| `motion_name` | `Motion.name` | 名称规范化；当前有 79 个差异 |
| `first_target` | `Properties.firstTarget` | 枚举转换；当前有 177 个差异 |
| `first_target_valid_distance` | `Properties.firstTargetRange` | 距离值；当前有 4,639 个差异，必须重点验证 |
| `target_range` | `Properties.targetType` | `OnlyOne/Area/Party` 等枚举转换 |
| `target_species_restriction` | `Properties.targetSpecies` | 物种枚举转换 |
| `target_relation_restriction` | `Properties.targetRelation` | `Enemy/Friend/All` 枚举转换 |
| `add_wpn_range` | `Properties.addWeaponRange` / `awr` | 布尔值转换 |
| `target_maxcount` | `Properties.targetMaxCount` | 整数映射 |
| `revision_distance` | `Properties.revisionDistance` | 整数映射 |
| `target_slot_level` | `SkillTemplate.targetSlotLevel` | 整数映射 |
| `conflict_id` | `SkillTemplate.conflictId` | 整数映射 |
| `toggle_timer` | `SkillTemplate.toggleTimer` | 时间单位待统一 |
| `counter_skill` | `SkillTemplate.counterSkill` | `AttackStatus` 枚举转换 |
| `charge_set_name` | `SkillTemplate.charge_set_name` | 字符串映射并校验 `ChargeSkillData` 外键 |
| `no_remove_at_die` | `SkillTemplate.noRemoveAtDie` | 布尔值转换 |
| `ammo_speed` | `SkillTemplate.ammoSpeed` / XML `ammospeed` | 整数映射；注意字段名不同 |
| `obstacle` | `SkillTemplate.obstacle` / `GeoService.canSeeSkill` | `0..5` 原值映射；普通 LOS 通过后，再按材质 `skill_obstacle` 等级执行技能障碍判定 |
| `motion_play_speed` | `Motion.speed` | 百分比整数 |
| `instant_skill` | `Motion.instantSkill` | 布尔值转换 |
| `apply_heal_boost_bonus` | `SkillTemplate.applyHealBoost` / XML `applyhealboost` | 真端 `0/缺省` 显式生成 `false`，`1` 生成 `true`；旧模板缺省仍保持 `true` |
| `apply_mpheal_boost_bonus` | `SkillTemplate.applyMpHealBoost` / XML `applymphealboost` | 真端 `0/缺省` 显式生成 `false`，`1` 生成 `true`；MP 恢复使用独立的 `MP_HEAL_SKILL_BOOST`，旧模板缺省保持 `true` |
| `cost_charge_weapon`、`cost_charge_armor` | `actions/chargeuse` | 技能成功结束时遍历已装备武器和防具分别扣除充能；每件物品饱和扣到 `0`，输入限制为 `0..4000` |
| `target_valid_status1`、`target_valid_status2`、`target_valid_status3`、`target_valid_status4`、`target_valid_status5` | `Properties.targetStatus` | 按槽位顺序组合，不能去重重排 |
| `self_hide_restriction` | `startconditions/selfhide` | 当前 10 条 `Hide` 要求施法者处于 `HIDE` 状态；其他值拒绝转换 |
| `chain_skill_prob1`、`penalty_active_defend_succ`、`penalty_defend_succ`、`penalty_no_casting_time_succ`、`penalty_time_succ` | 无运行时输出 | 当前真端记录全部为 `0`；转换器验证零值，未来出现非零值时拒绝转换 |

这些字段的目标已经确定，但有差异的字段仍须以真端、当前模板和行为测试判定最终取值。

## 3. 目标结构明确、仍需转换规则的技能级字段

| 字段组 | 真端字段 | 当前目标或转换方向 |
|---|---|---|
| 元素 | `element_type` | `EffectTemplate.element`；需确定是应用到全部攻击效果还是指定效果 |
| DP 消耗 | `cost_dp` | `startconditions/dp` 与 `actions/dpuse` |
| 伊德石条件 | `polish_charge_weapon` | `useconditions/idianchargeweapon` |
| 驱散次数 | `required_dispel_count` | XML `req_dispel_count` / `SkillTemplate.reqDispelCount`，初始化运行时 `Effect.power`；字段缺失时普通技能默认 10、Maintain 默认 30 |
| 目标停止 | `target_stop` | 施法/移动条件，需从当前 `playermove` 行为反推 |
| 移动施法 | `move_casting` | `useconditions/playermove` |
| 加成开关 | `apply_magical_skill_boost_bonus`、`apply_magical_critical` | 伤害计算开关；当前模板已有直接字段，仍需随对应效果族验证完整行为 |
| PVP 修正 | `pvp_remain_time_ratio`、`pvp_damage_ratio` | `pvp_duration`、`pvp_damage`，需确认比例单位 |
| 通用消耗 | `cost_parameter`、`cost_end`、`cost_end_lv`、`cost_start`、`cost_start_lv` | `Actions` 或 `PeriodicActions`；按消耗类型和等级增量组合 |
| 施法时间 | `casting_delay` | 施法时长来源；不能误写成 cooldown |
| 连锁技能 | `chain_category_name`、`chain_category_level`、`chain_skill_prob1`、`chain_skill_prob2`、`prechain_category_name`、`prechain_count`、`prechain_skillname`、`chain_time`、`self_chain_count` | `chain_skill_prob2` -> `chain_skill_prob`；当前 `chain_skill_prob1` 全部为零，非零值拒绝转换；其余字段生成技能链条件及关联数据 |
| 武器条件 | `required_sword`、`required_mace`、`required_dagger`、`required_orb`、`required_book`、`required_2hsword`、`required_polearm`、`required_staff`、`required_bow`、`required_leftweapon`、`required_gun`、`required_cannon`、`required_keyblade`、`required_harp` | `startconditions/weapon`，按真端槽位合并 |
| 载具条件 | `required_ride_robot` | 载具/机器人使用条件 |
| 物品消耗 | `component`、`component_count`、`component_expendable` | `startconditions/item` 与 `actions/itemuse` |
| 状态条件 | `nouse_combat_state` | 战斗状态使用条件 |
| 飞行条件 | `self_flying_restriction`、`target_flying_restriction` | `selfflying`、`targetflying` 等条件 |
| 形态条件 | `allow_use_form_category` | `form` 条件 |
| 持久化 | `no_save_on_logout`、`remain_cooltime_on_login` | 效果/冷却持久化行为 |
| 成功惩罚 | `penalty_skill_succ`、`penalty_type_succ`、`penalty_skill_succ_msg` | `penalty_skill_id` 及施放成功后的动作/消息 |
| Toggle | `toggle_id` | toggle 冲突或分组；不能直接等同 `toggle_timer` |
| 周期消耗 | `cost_time`、`cost_toggle`、`cost_checktime_parameter`、`cost_checktime` | `PeriodicActions` 及 `checktime` |
| 等级时间 | `delay_time_lv` | delay/cooldown 等级增量，单位待证明 |
| 弹药 | `use_arrow_count` | 弹药消耗条件和动作 |
| 姿态 | `change_stance`、`stance2_type` | `SkillTemplate.stance` 及姿态类型 |
| 前置技能 | `pre_cond_skill_group` | `startconditions/skillgroup`，检查施法者是否存在对应 `SKILL_` stack |
| 飞行结束移除 | `remove_at_fly_end` | XSD 与 Java `remove_flyend` 已绑定，实际移除时机仍需逐路径验证 |

这里的目标明确不代表可以立即生成模板。例如 `casting_delay`、`delay_time`、`delay_time_lv` 和 `nonchained_delay_time` 都与时间有关，但不能统一映射到 `cooldown`。

## 4. 尚未编译的 32 个技能级字段身份

以下字段仍需继续查真端 `SkillDB`、5.8 客户端或补齐对应运行机制。当前阶段完整保留原值，不允许静默忽略：

```text
__type_desc__（occurrence 0）
__type_desc__（occurrence 1）
__type_desc__（occurrence 2）
advancement_rate
auto_attack
broadcast_use_message
charging_delay
damage_attenuation
delay_type
exclusive_attribute
exclusive_attribute_tag
hide_decrease_count
is_familiar_skill
max_maintain_count
max_skill_point
motion_mode
nonchained_delay_time
penalty_skill_succ_msg
penalty_type_succ
pre_fx_delay
prechain_skillname
stance2_type
status_fx_slot
status_fx_slot_lv
status_sfx1
system_fire_fx
target_range_opt3
target_stop
toggle_id
ultra_skill
ultra_transfer
use_arrow_count
```

`__type_desc__` 在字段字典中出现三次，必须按 occurrence 区分，不能用普通 `Map<String, String>` 覆盖。

`obstacle` 已按真端碰撞规则编译。`server58-source/NPCServer_NPCSvr64/classes/Skill/Skill.cpp:408` 读取该字段并写入技能对象偏移 `0xA8`；`fun/fun_064.cpp:251` 再把该值复制到 NPC 攻击碰撞参数。`classes/Misc/switchD_1402f15f9.cpp` 以 `materialId * 6 + skillObstacle` 查询比较表，表项为 `0` 时才视为阻挡。材质障碍等级 1～4 的结果如下：

| 技能 `obstacle` | 等级 1 材质 | 等级 2 材质 | 等级 3 材质 | 等级 4 材质 |
|---:|:---:|:---:|:---:|:---:|
| 1 | 阻挡 | 阻挡 | 阻挡 | 阻挡 |
| 2 | 穿透 | 阻挡 | 阻挡 | 阻挡 |
| 3 | 穿透 | 穿透 | 阻挡 | 阻挡 |
| 4 | 穿透 | 穿透 | 穿透 | 阻挡 |
| 5 | 穿透 | 穿透 | 穿透 | 穿透 |

字段缺失时真端构造默认值为 `0`。AionEmu 复用现有 `MaterialTemplate.skill_obstacle` 和 `CollisionIntention.SKILL`，普通实体墙仍先走基础 LOS；技能 3777 的旧 LOS 例外保持不变。

`type_message` 已确认不是 `type` 的别名。真端 `SkillDB` 将 `type` 写入技能对象偏移 `0x14c`，用于命中、抵抗和魔法反击等战斗结算；`type_message` 写入独立偏移 `0x150`，缺失时继承 `type`，并在 `SkillEffectMgr` 中只选择技能结果消息类别。当前 509 条显式记录全部为 `type=Physical`、`type_message=Magical`。AionEmu 通过独立 `type_message` 属性选择失败结果 `DODGE/RESIST`，`skilltype` 继续负责战斗结算，同时移除原枪炮星/机甲星职业硬编码。

`advancement_rate` 和 `max_skill_point` 属于 Gather/Combine 技能表的遗留元数据。真端会把它们解析到模板偏移 `+4/+8`，但恢复源码和 `Server64.exe` 模板表完整引用扫描均未发现运行时读取；AionEmu 的采集/制作升级也只使用自身经验、费用和升阶规则。因此两字段继续无损保留在 `retail_fields`，不接入经验倍率或技能上限。

`cost_charge_weapon` 和 `cost_charge_armor` 已按真端 `Skill::Set` 与 `User_BurnChargeByAction` 对应。字段分别存为无符号 16 位值并在 `4000` 封顶；技能成功结束时，真端按武器和防具类别遍历全部装备逐件扣除，充能不足只扣到 `0`，不阻止技能。AionEmu 通过单个 `<chargeuse weapon="..." armor="..."/>` 动作复用现有 `ChargeInfo`，不替代物品自己的 `burn_attack/burn_defend`。

## 5. 212 个效果字段的对应规则

真端效果字段由 4 个槽位和 59 种后缀组成。不是每个后缀都在每个槽位出现，当前字段字典实际覆盖如下：

| 字段模式 | 槽位 | 当前目标 | 状态 |
|---|---|---|---|
| `effectN_type` | 1～4 | 具体 `EffectTemplate` 子类 | 待转换；231 种拼写先归一为 174 种类型 |
| `effectN_noresist` | 1～4 | `EffectTemplate.noResist` | 已对应 |
| `effectN_remain1`、`effectN_remain2` | 1～4 | `duration1`、`duration2` | 已对应，单位需校验 |
| `effectN_effectid` | 1～4 | `EffectTemplate.effectid` | 已对应 |
| `effectN_basiclv` | 1～4 | `EffectTemplate.basicLvl` | 已对应 |
| `effectN_hop_type`、`hop_a`、`hop_b` | 1～4 | `hopType`、`hopA`、`hopB` | 已对应，枚举需规范化 |
| `effectN_randomtime` | 1～3 | `EffectTemplate.randomTime` | 已对应 |
| `effectN_acc_mod1` | 1、2 | `EffectTemplate.accMod1` | 已对应 |
| `effectN_acc_mod2` | 1～3 | `EffectTemplate.accMod2` | 已对应 |
| `effectN_critical_prob_mod1` | 1～3 | 当前基类无同名字段 | 待证明 |
| `effectN_critical_prob_mod2` | 1～4 | `EffectTemplate.critProbMod2` | 已对应 |
| `effectN_critical_add_dmg_mod2` | 1、3 | `critAddDmg1/critAddDmg2` 候选 | 待证明，不能仅凭后缀选目标 |
| `effectN_target_type` | 1～4 | 效果目标选择策略 | 待转换；当前基类无直接字段 |
| `effectN_checkforchain` | 2～4 | 效果链检查 | 待证明 |
| `effectN_checktime` | 1～4 | 周期/延迟检查时间 | 待证明 |
| `effectN_hidemsg` | 1～4 | 效果消息可见性 | 待证明 |
| `effectN_cond_preeffect` 及 `prob1/prob2` | 1～4 | `preEffect`、`preEffectProb` | 待转换；两个概率不能压成一个值 |
| `effectN_cond_race` 及 `prob1/prob2` | race 仅槽 2，概率槽 2～3 | `conditions/targetrace` 候选 | 待证明 |
| `effectN_cond_status` 及 `prob1/prob2` | 2～4 | 效果 `conditions/subconditions` | 待转换 |
| `effectN_cond_attack_dir` 及 `prob1/prob2` | 2～3 | 前后方伤害条件/修正器 | 待转换 |
| `effectN_reserved1`～`reserved22` | 1～4 | 由具体效果类型决定 | 效果专用；共 88 个字段 |
| `effectN_reserved_cond1` 及 `prob1/prob2` | 1～4 | 具体效果的第一组条件 | 效果专用；共 12 个字段 |
| `effectN_reserved_cond2` 及 `prob1/prob2` | 1～4 | 具体效果的第二组条件 | 效果专用；共 12 个字段 |

以上 59 种后缀模式覆盖字段字典中的 212 个 `effect*` 字段。验收以字段字典索引 `i=0..356` 唯一覆盖为准，不使用容易重复计算的人工分组小计。

最重要的限制：`reserved2` 在 `SkillATK_Instant` 中可以是伤害百分比，在 `StatUp` 中可以是属性变化值；因此只能建立：

```text
(canonical effect type, reserved position) → 当前效果参数
```

禁止建立：

```text
reserved position → 全局统一参数
```

## 6. 首批编译顺序

字段对应完成后的最小实现顺序：

1. 解析并无损保存全部 357 个字段，包括重复 `__type_desc__`。
2. 先编译第 2、3 节中已证明的 113 个技能级字段。
3. 再实现第 3 节的结构字段，每组留下一个真实技能对照测试。
4. 第 4 节的 32 个待证明字段保持发布阻断状态；已证明无运行时消费者的字段保留原值但不阻断发布。
5. 效果层先编译通用元数据，再按 `effectN_type` 分 family 解释 reserved 参数。

在第 4 节待证明字段归零、174 种归一效果类型全部有处理规则之前，不切换运行时数据源。

## 7. 当前 V3 候选进度

技能级字段当前已编译 `113/145`，覆盖率 `77.93%`；剩余 32 个字段继续无损保留在 `retail_fields`。效果类型与实例已分别关闭 `174/174`、`22,494/22,494`，总门槛为 `287/319`（`89.97%`）。

最近一次 V3 输出快照包含 14,494 个技能模板，174 种 canonical effect 和 22,494 条效果实例均已完整生成；按增量开发约定，本次字段修改后暂不全量重建 V3 与正式 compact 快照。

`SpellATK` 已覆盖真端全部 556 条效果（549 个技能）：

- `reserved8` → `delta`，`reserved9` → `value`，每跳基础伤害为 `delta × skillLevel + value`；
- `reserved10` → `element`，`checktime`、`remain1`、`remain2` 分别进入 `checktime`、`duration1`、`duration2`；
- `reserved12` 只按是否非零控制 `mrresist`，143 条启用、413 条关闭；原始 `100/500/2000` 不作为倍率；
- 技能级 `apply_magical_skill_boost_bonus`、`apply_magical_critical` 分别进入 `applymboost`、`applymcrit`；
- `Bleed`、`Poison`、飞行状态及背击条件复用现有 `Conditions`；无法完整表达的组合继续保留在 `retail_fields`，不输出半成品效果。

`Dispel` 已覆盖真端全部 338 条效果：

- 四种模式分布为 `Effect_Type=61`、`Slot_Type=47`、`Effect_ID=228`、`Effect_ID_Range=2`；
- `reserved13/14`、`reserved15/16`、`reserved17/18` 分别编译为最大移除数、驱散等级和驱散强度的等级公式；
- 驱散等级 `100` 保持真端强制语义，驱散强度先扣减目标效果剩余 `power`，耗尽后才计入实际移除上限；
- `Special2`、`SpellATK`、`DeboostHealAmount` 分别映射到 AionEmu 的 `SPEC2`、`SPELLATTACK`、`DEBOOSTHEAL`；无法识别的槽位或效果类型不会输出半成品。

`ProcATK_Instant` 已覆盖真端全部 316 条效果：

- `reserved1/2` → `delta/value`，伤害公式为 `delta × skillLevel + value`，并沿用魔法伤害结算；
- `reserved3` → `checkprotector`，只控制伤害保护者转移，不跳过普通吸收、反射和转治疗护盾；
- `reserved5` → `weaponboost`，非零时覆盖施法者武器伤害倍率；当前唯一非零值为技能 9211 的 `100`；
- `reserved10` → `element`，`acc_mod2`、`critical_prob_mod2`、`noresist` 和通用仇恨字段继续复用效果公共属性；
- `reserved11/12` 在真端 `ProcATK_Instant` 的检查与生效函数中未读取，继续原样保留；技能 8344 的 `Stagger` 兼容映射为技能 8217 子效果。

`HostileUp` 已覆盖真端全部 308 条效果：

- `reserved1/2` → `delta/value`，永久仇恨公式为 `delta × skillLevel + value`；
- `reserved3/4/5` → `timed_delta/timed_value/timed_duration`，限时仇恨与永久仇恨可在同一次效果中同时生效；
- `reserved9` → `split_totem_hate`；仅当来源是 `TOTEM` 侍从时，按真端规则将 99% 仇恨记给图腾、1% 记给主人；
- `reserved10` → `element`，`noresist`、命中、暴击概率和通用仇恨字段继续复用效果公共属性；
- 限时仇恨到期只扣回原 `AggroInfo` 的对应增量，仇恨列表清空或同对象 ID 条目重建后不会误扣新条目。

`Heal` 已覆盖真端全部 249 条效果：

- `reserved8/9` → `delta/value`，每跳治疗量为 `delta × skillLevel + value`；
- `reserved6 == 1` → `percent=true`，按目标最大 HP 的百分比计算；`checktime` 直接作为周期；
- 技能级 `apply_heal_boost_bonus` → `applyhealboost`，98 条启用、151 条关闭；旧 AionEmu 模板缺省仍启用以保持兼容；
- 7 条 `checktime=0` 同时持续时间为 0，沿用现有不创建周期任务的行为；
- `reserved1/2/10/16` 在真端 `SkillEffectHeal` 中未读取，不编译为运行参数，原值继续保留在 `retail_fields`。

`NoReduceSpellATK_Instant` 已覆盖真端全部 236 条效果：

- `reserved1/2` → `delta/value`，固定伤害公式为 `delta × skillLevel + value`，结果最少为 1；
- `reserved6 == 1` 的 23 条效果按目标最大 HP 百分比计算，`reserved3` → `maxdamage` 并限制百分比伤害上限；其余 213 条固定伤害不读取 `reserved3`；
- `reserved10` → `element`，继续复用现有不可减免魔法伤害路径，不应用魔增、知识及元素或魔法防御减伤；
- `critical_prob_mod1/2` 按 `mod1 × skillLevel + mod2` 计算暴击概率，`critical_add_dmg_mod2` 保持暴击附加伤害；真端缺省 `critical_prob_mod2` 显式生成 `0`；
- 全部 236 条效果已按技能 ID 和效果槽位逐条核对 `value`、`delta`、百分比、上限、元素及暴击字段，与真端源数据一致。

`Provoker` 已覆盖真端全部 276 条效果（263 个技能）：

- `reserved17` 按技能名解析为触发技能 ID；224 个不同触发技能的 `activation_attribute` 均为 `PROVOKED`，运行时阻止此类技能继续触发攻击方 `Provoker`，避免递归链；
- `reserved14` 的 `Me/Opponent` 分别指效果持有者自身和本次攻击或受击的对方，触发技能施法者始终是效果持有者；
- `reserved4/5` → `minradius/radius`，按真端规则以包含边界的平方距离判断；`reserved15/16` → `delta/value`，触发技能等级为 `delta × 当前效果技能等级 + value`；
- `reserved_cond1` 的 `EveryHit/PhHit/MaHit/NmlATK/BackATK` 分别控制受击、物理受击、魔法受击、攻击和背击事件，`reserved_cond1_prob1/2` 按千分比等级公式判定；
- `reserved_cond2` → `condrace`，35 条种族条件的两段概率均为真端固定值 `0/1000`；`GChief_Dragon`、`F6_Raid_Boss` 分别规范化为 `GCHIEF_DRAGON`、`F6_RAID_BOSS`；`reserved10` → `element`；
- 全部 276 条效果已按技能 ID 和效果槽位逐条核对触发技能、目标、距离、等级公式、事件及概率、种族和元素，无缺失、额外或字段差异。

`MPHeal_Instant` 已覆盖真端全部 215 条效果（215 个技能）：

- `reserved1/2` → `delta/value`，瞬时 MP 恢复量为 `delta × skillLevel + value`；真端要求两者非负且不能同时为零；
- `reserved6 == 1` 的 21 条效果按目标最大 MP 百分比计算，其余 194 条使用固定恢复量；`reserved10` → `element`；
- 技能级 `apply_mpheal_boost_bonus` → `applymphealboost`，25 条启用、190 条关闭；MP 恢复加成使用新增的独立属性 `MP_HEAL_SKILL_BOOST`，不复用 HP 治疗加成，并按真端限制在基础恢复量的 3 倍以内；
- `reserved11` 在真端 `SkillEffectMpHeal_Instant` 的参数检查和结算函数中未读取，不编译为运行参数，原值继续保留在 `retail_fields`；
- 全部 215 条效果已按技能 ID 和效果槽位逐条核对 `value`、`delta`、百分比、元素及 MP 恢复加成开关，无缺失、额外或字段差异。

`Paralyze` 已覆盖真端全部 181 条效果（181 个技能）：

- 真端注册到通用 `SkillEffectAbnormalState<2,3>`，不是 `Petrification`；`remain1/remain2` → `duration1/duration2`，持续时间按 `duration1 × skillLevel + duration2` 计算；
- `reserved10` → `element`，并继续复用 `noresist`、`acc_mod1/2` 与现有 `PARALYZE_RESISTANCE`、魔法命中抗性路径；
- `reserved1/2` → `delta/value`，真端将 `delta × skillLevel + value` 限制到 100 后作为异常状态附带参数；恢复源码仅在开始、恢复、结束时写入或清零该参数，未发现独立玩法读取点，因此复用基类字段而不新增 Java 子系统；
- 113 条前置效果及其概率继续映射为 `preeffect/preeffect_prob`；3 条 Poison 和 1 条 Sleep 状态条件均由现有 `conditions/abnormal` 完整表达；
- `reserved9/11/12/16` 在该模板中没有专用读取，原值继续保留在 `retail_fields`；全部 181 条效果已按技能 ID 和效果槽位逐条核对，缺失、额外和字段差异均为 0。

`AbsoluteSlow` 已覆盖真端唯一 1 条效果（技能 21559，效果槽 2）：

- 真端把最终攻击间隔限制为不低于 `baseAttackDelay × (reserved1 × skillLevel + reserved2) / 100 + reserved3 × skillLevel + reserved4`；当前数据固定为 `baseAttackDelay × 50%`；
- AionEmu 的 `StatCapUtil` 已在全部攻击速度修正之后强制同一个 `base / 2` 下限，因此无需新增计算器或属性字段；
- 旧模板的 `ATTACK_SPEED REPLACE 5000` 会把 `5000` 写成 bonus 而不是最终攻击间隔，行为不等价，真端候选不再生成该 change；
- `remain1/2`、`effectid`、`noresist`、前置效果和 `reserved10` 元素继续映射到公共效果字段，`reserved1/2/3/4/11/12` 原值保留在 `retail_fields`。

`AbsoluteSnare` 已覆盖真端全部 20 条效果（20 个技能）：

- 真端只对玩家限制最终移动速度上限，公式为 `(reserved1 × skillLevel + reserved2) / 10 m/s`；NPC 不应用该上限；
- AionEmu 的速度单位为真端参数的 100 倍，因此 `reserved1 × 100` → `delta`、`reserved2 × 100` → `value`，并同时限制地面 `SPEED` 与飞行 `FLY_SPEED`；
- 上限修正在其他速度加成后执行，只削减超出上限的部分，不把低于上限的速度强制抬高；
- 旧模板的 `SPEED` / `FLY_SPEED REPLACE` 子节点会覆盖 bonus 而不是最终速度，行为不等价，真端候选不再生成这些 change；
- 全部 20 条效果已按技能 ID 和效果槽位逐条核对，缺失、额外和字段差异均为 0；候选已通过 XSD、JAXB 全量加载及专项行为测试。

`BuffStun` 已覆盖真端全部 2 条效果（技能 19580 槽 2、技能 22718 槽 1）：

- 两条数据均通过 `noresist=1` 表示不做抗性判定，复用现有 `BuffStunEffect` 直接加入成功效果的行为，不新增 Java 模型；
- `remain1/2`、`effectid`、`basiclv`、前置效果和 `reserved10` 元素按公共效果字段生成，技能 19580 的前置效果保持为槽 1；
- 两条的 `reserved11=100` 在真端通用异常状态参数解析中没有专用读取，继续保留在 `retail_fields`，不编译为运行参数；
- 两条输出与真端按技能 ID 和效果槽位核对后，缺失、额外和字段差异均为 0，并已通过无抗性行为测试及 XSD/JAXB 全量加载。

`Petrification` 已覆盖真端全部 2 条效果（技能 16492 槽 1、技能 19862 槽 1）：

- 真端使用独立 `SkillEffectPetrification`、`PETRIFICATION` 异常状态和 `PERIFICATION_RESISTANCE`，不能复用普通 `Paralyze`；AionEmu 新增最小独立效果模型，并将石化加入禁止攻击、禁止移动和强制下坐骑掩码；
- `reserved6=1` 表示百分比模式，`reserved1/reserved2` 编译为 `PHYSICAL_DEFENSE PERCENT` 的 `delta/value`，最终修正为 `reserved1 × skillLevel + reserved2`；其他模式在完整证明前拒绝输出；
- `PERIFICATION_RESISTANCE` 已加入通用 altered-state 路径，因此 Boss、实体、AI 异常免疫和 `ABNORMAL_RESISTANCE_ALL` 与其他控制状态一致生效，同时继续使用独立石化抗性穿透；
- 真端 `EndEffect` 的附加结算分支与 `Deform` 及通用异常状态模板相同，受技能级伤害归属字段控制；两条石化数据均未启用该字段，因此无需增加不可触发的运行逻辑；
- 两条输出与真端按技能 ID 和效果槽位结构化核对后，缺失、额外和字段差异均为 0；物防百分比生命周期、控制掩码、抗性路径以及 XSD/JAXB 全量加载均已通过测试。

`Simple_Root` 已覆盖真端唯一 1 条效果（技能 8219，效果槽 1）：

- 真端 effect factory 索引 `0x95` 注册为 `SkillEffectAbnormalState<24,40>`；开始时设置异常状态 24、抗性类型 40，并调用目标的强制位移处理，结束时清除状态；
- AionEmu 现有 `SimpleRootEffect` 已使用独立 `KNOCKBACK` 状态、`STAGGER_RESISTANCE` 和短距离强制后退，能够表达该真端行为，无需新增效果模型；
- `remain1/remain2`、`effectid`、`noresist`、`reserved10` 元素和 HOP 字段按公共效果字段生成；唯一真端记录为 `duration1=1000`、`effectid=20003`、`noresist=true`、`element=WIND`；
- 该记录设置 `noresist=1`，因此不会进入抗性判定；`reserved11=100` 继续保留在 `retail_fields`，不重复编译为运行参数；输出与真端按技能 ID 和效果槽位核对后，缺失、额外和字段差异均为 0。

`InvulnerableWing` 已覆盖真端唯一 1 条效果（技能 3128，效果槽 2）：

- 真端恢复效果时向玩家开启状态 `0x1e`，结束时清除同一状态；AionEmu 现有 `InvulnerableWingEffect` 同样仅对玩家生效，并在效果应用/结束时开启和清除无敌翼标记；
- 唯一记录只使用公共效果字段，生成 `duration1=0`、`duration2=120000`、`effectid=105372`、`noresist=true` 和 `preeffect=1`，无需新增专用参数、Java 模型或 XSD；
- 前置槽 1 的 `Dispel` 已完整生成，因此槽 2 的前置关系不会引用缺失效果；输出与真端按技能 ID 和效果槽位核对后，缺失、额外和字段差异均为 0。

`Escape` 已覆盖真端唯一 1 条效果（技能 302，效果槽 1）：

- 真端效果在玩家对象上注册 1 ms 的 `0x3d` 定时器，玩家定时器分派将 `0x3d` 明确交给 `User_Escape`；AionEmu 现有 `EscapeEffect` 对应执行副本离开和绑定点回城；
- 唯一记录没有专用运行参数，只生成公共 `e=1` 和 `noresist=true`；`reserved2=0` 与 `acc_mod2=0` 不产生额外语义，原值仍保留在 `retail_fields`；
- 输出与真端按技能 ID 和效果槽位核对后，source=1、output=1、missing=0、extra=0，无需新增 Java 模型、XSD 或 JAXB 注册。

`ReturnPoint` 已覆盖真端唯一 1 条效果（技能 8198，效果槽 1）：

- 真端从施法物品模板读取 world、alias 和多目的地回城信息，解析目标后执行延迟传送；AionEmu 现有 `ReturnPointEffect` 同样要求物品上下文并读取 `returnWorldId` 与 `returnAlias`；
- 唯一记录没有效果专用参数，只生成公共 `e=1`；目标信息属于物品模板而不是技能效果，不能复制进技能 XML；
- 输出与真端按技能 ID 和效果槽位核对后，source=1、output=1、missing=0、extra=0，无需新增 Java 模型、XSD 或 JAXB 注册。

`SwitchHostile` 已覆盖真端全部 2 条效果（技能 3739、11604，均为效果槽 1）：

- 真端 `User::SetPet` 将宠物对象 ID 保存到玩家字段 `0x54a4`，效果生效时把玩家 ID 和宠物 ID 发送给目标 NPC；NPC Server 的 `NPC_SwitchHostileOfTwo` 交换两者仇恨条目并重新计算最高仇恨目标；
- AionEmu 现有 `SwitchHostileEffect` 同样在目标 NPC 的仇恨列表中交换玩家与当前召唤物的仇恨值；两条真端记录均没有效果专用参数，只保留公共 `e=1` 和 `hoptype=SKILLLV`；
- 输出与真端按技能 ID 和效果槽位核对后，source=2、output=2、missing=0、extra=0，无需新增 Java 模型、XSD 或 JAXB 注册。

`SwitchHPMP_Instant` 已覆盖真端全部 2 条效果（技能 1327、3904，均为效果槽 1）：

- 真端将 `reserved1 × skillLevel + reserved2` 作为 HP 交换百分比，将 `reserved3 × skillLevel + reserved4` 作为 MP 交换百分比；输出分别映射为 `delta/value` 和 `mpdelta/mpvalue`；
- AionEmu 现在先按两个百分比计算当前 HP/MP 转移量，再将结果限制到 `1..max`；旧模板未填写四字段时继续按 100%/100% 全量交换，避免候选切换前产生行为回归；
- 两条真端数据均为 `0/100/0/100`；按技能 ID、效果槽位和四个公式字段核对后，source=2、output=2、missing=0、extra=0，并已通过 XSD/JAXB 全量加载。

`AlwaysHit`、`AlwaysNoResist`、`AlwaysBlock`、`AlwaysDodge`、`AlwaysParry`、`AlwaysResist` 已完成真端字段映射（共 5、5、22、8、8、43 条效果）：

- 六族的 `reserved8/9` → `delta/value`，可触发次数为 `delta × skillLevel + value`；旧模板未填写 `delta` 时继续使用原 `value`；
- `reserved13 != 0` → `consume=true`，表示对应攻击或防御判定发生时递减剩余次数；该字段不是优先级；
- `AlwaysHit` 让物理攻击绕过闪避、招架和格挡，`AlwaysNoResist` 让魔法攻击绕过魔抗；防御侧四族继续共用现有攻击状态观察者；
- 真端未发现攻击、防御效果之间的优先级比较或冲突机制，因此不生成 `priority`；
- 本批不重建 V3，字段映射先通过转换器聚焦测试，累计后统一生成候选文件。

`Recall_Instant`、`Spin`、`Pulled`、`HiPass` 已分别覆盖真端全部 3、3、2、2 条效果：

- 四族直接复用 AionEmu 现有召回、旋转、拉拽和高等通过运行时，不新增 Java 效果类；
- `remain1/2`、`effectid`、`noresist`、`basiclvl`、前置效果、元素和 HOP 字段统一由公共效果映射生成；
- `Spin/Pulled` 的 `reserved16` 以及其他未发现独立运行时读取的保留槽继续留在 `retail_fields`，不猜测编译；
- 共 10 条记录已进行内存级源数据核对，本批继续不重建 V3。

`AbsoluteAPPoint_Heal_Instant`、`AbsoluteEXPPoint_Heal_Instant`、`ExtendAuraRange` 已分别覆盖真端全部 3、6、2 条效果：

- 两个绝对点数恢复族的 `reserved2` → `points`，直接复用现有欧比斯恩惠和伯丁之星固定点数恢复；
- 光环范围的 `reserved1/2` → `BOOST_MANTRA_RANGE ADD` 的 `delta/value`，最终增量为 `delta × skillLevel + value`；
- 光环范围 `reserved9=1` 的技能 359 生成 `onfly` 条件，技能 341 不带飞行条件；
- 11 条记录只需转换映射，不新增 Java 逻辑，本批继续不重建 V3。

`DPTransfer`、`BuffSilence`、`NoDeathPenalty`、`NoResurrectPenalty`、`Stagger`、`Stumble`、`Flyoff` 已分别覆盖真端全部 1、5、11、12、17、20、2 条效果：

- DP 转移、增益沉默和两种免惩罚效果只使用公共字段，直接复用现有运行时；
- `Stagger/Stumble` 的 `reserved1/2` → `delta/value`，持续时间、抗性命中、前置效果、元素和 HOP 字段继续使用公共映射；
- `Flyoff` 的 `reserved2` → `value`、`reserved4` → `distance`，当前两条真端数据的等级增量槽均为 0；
- 68 条记录已进行内存级逐条核对，本批继续不重建 V3。

`DispelBuff`、`DispelDebuff`、`DispelDebuffPhysical`、`DispelDebuffMental`、`DispelNpcBuff`、`DispelNPCDebuff` 已分别覆盖真端全部 29、71、24、6、13、2 条效果：

- 六族共用真端公式：`reserved1/2` → `delta/value`，最大驱散数量为 `delta × skillLevel + value`；`reserved15/16` → `dispel_level_delta/dispel_level`，驱散等级同样随技能等级计算；`reserved17/18` → `dpower/power`；
- 普通 `DispelBuff` 同时匹配模板类别 `BUFF` 和 `ALL`；通用 `DispelDebuff` 匹配 `ALL`、物理和精神减益，两个专用 family 仅匹配对应的物理或精神类别；全部分支继续应用驱散等级与强度；
- 真端通用 NPC Debuff 类别 5 同时匹配效果类别 6（NPC 物理减益）和 7（NPC 精神减益），不匹配普通玩家减益；AionEmu 已补齐通用及精神类别，并保留物理、精神两类模板分类；
- 技能 18154 恢复为槽 1 `dispelnpcbuff` 成功后执行槽 2 `dispelnpcdebuff`，不再沿用旧模板把两个效果都写到槽 1 的错误结构；
- 六族按技能 ID、效果槽位、数量、驱散等级和强度公式逐条核对，输出数分别为 29、71、24、6、13、2，全部 missing=0、extra=0，并已通过分类边界测试及 XSD/JAXB 全量加载。

`BuffBind`、`Disease`、`Search`、`DPHeal`、`OneTimeBoostSkillCritical` 已分别覆盖真端全部 12、21、19、8、3 条效果：

- `BuffBind` 和 `Disease` 复用现有异常状态运行时；持续时间、效果 ID、无抗性、前置效果、元素和 HOP 字段使用公共映射，`Disease` 的唯一背击条件生成现有 `conditions/back`；
- `Search` 的 `reserved7=1/2/5/10` 分别映射为 `SEARCH1/2/5/10`，其他值拒绝生成，当前 19 条真端记录均在运行时枚举范围内；
- `DPHeal` 的 `checktime` 直接作为周期，`reserved9` -> `value`，`reserved6=1` -> `percent=true`；当前唯一百分比记录为技能 19259 的每 2 秒恢复最大 DP 的 15%；
- `OneTimeBoostSkillCritical` 的 `reserved4` -> `value`、`reserved7` -> `count`、`reserved6` -> `percent`，三条真端记录分别覆盖 3 次和 1 次技能暴击加成；
- 五族共 63 条记录已按技能 ID、效果槽位和专用字段进行内存级核对，本批继续不重建 V3，也不提前切换正式模板。

`Root`、`Silence`、`Bind`、`NoFly`、`Sleep`、`Fear` 已分别覆盖真端全部 157、131、68、104、72、79 条效果：

- 六族直接复用现有定身、沉默、束缚、禁飞、睡眠和恐惧运行时，持续时间、随机时间、效果 ID、命中、前置效果、元素及 HOP 字段统一使用公共映射；
- `Root/Fear` 的 `reserved2` -> `resistchance`；字段不存在时不输出属性，继续使用运行时默认值 100，显式的 0 则原样保留；
- `Root` 的 `reserved1/3/4/6`、`Fear` 的 `reserved1/15/16`、`NoFly` 的 `reserved1/2/checktime` 等尚未证明为独立运行参数的槽继续保留在 `retail_fields`；
- 六族共 611 条记录已按技能 ID、效果槽位和已编译字段进行内存级核对，本批继续不重建 V3。

`ProcHealInstant`、`ProcMPHealInstant`、`FPHealInstant`、`ProcFPHealInstant`、`DPHealInstant`、`ProcDPHealInstant`、`MPHeal`、`FPHeal` 已分别覆盖真端全部 159、110、32、13、11、11、143、12 条效果：

- 六个瞬时恢复族共用 `reserved1/2 -> delta/value` 和 `reserved6=1 -> percent=true`，最终恢复量为 `delta × skillLevel + value` 或对应最大资源的同值百分比；
- `MPHeal/FPHeal` 与已闭合的 `Heal/DPHeal` 共用周期公式：`reserved8/9 -> delta/value`、`reserved6=1 -> percent=true`，`checktime` 直接作为周期；
- `DPHeal` 同步补出 `reserved8 -> delta`，当前 8 条记录该槽均为 0，不改变现有行为但保持公式完整；
- 八族共 491 条记录已按技能 ID、效果槽位、公式、百分比和周期逐条核对，本批继续不重建 V3。

`MPAttackInstant`、`FPAtkInstant`、`MPAttack`、`FPAtk`、`DelayedFPAtkInstant` 已分别覆盖真端全部 144、72、40、29、11 条效果：

- 两个瞬时扣减族使用 `reserved1/2 -> delta/value`、`reserved6=1 -> percent=true`；两个周期扣减族使用 `reserved8/9 -> delta/value`、`reserved6=1 -> percent=true` 和真端 `checktime`；
- `DelayedFPAtkInstant` 另将 `reserved9 -> delay`，输出到现有 `delayedfpatk_instant` 运行时，当前 11 条均为百分比扣减；
- MP/FP 瞬时和周期运行时改为实际使用 `delta × skillLevel + value`，修复旧模板虽有 `delta` 但结算只读取 `value` 的问题；
- 五族共 296 条记录已按技能 ID、效果槽位、公式、百分比、周期和延迟逐条核对，本批继续不重建 V3。

`Blind`、`Slow` 已分别覆盖真端全部 84、165 条效果：

- `Blind` 的 `reserved1/2 -> delta/value`，闪避概率按 `delta × skillLevel + value` 计算；这使技能 18443 的唯一 `delta=95` 记录不再被旧运行时按 0% 处理；
- `Slow` 的 `reserved1/2` 取反后写入 `ATTACK_SPEED` 的 `delta/value`，`reserved6=1` 使用 `PERCENT`，其余记录使用 `ADD`；
- 两族共 249 条记录已按技能 ID、效果槽位和公式逐条核对，本批继续不重建 V3。

`BoostSkillCastingTime`、`BoostHealEffect`、`OneTimeBoostHealEffect`、`DeboostHealAmount` 已分别覆盖真端全部 88、6、17、65 条效果：

- 施法时间的 `reserved1/2 -> delta/value`，`reserved3=None/Heal/SummonTrap/Attack/SummonHoming/Summon` 分别映射到现有六个 `BOOST_CASTING_TIME_*` 属性，统一使用百分比修正；
- 治疗增强的 `reserved1/2 -> delta/value`，`reserved9=1` 生成飞行条件；一次性治疗增强使用 `reserved3/4 -> delta/value`，两者均修正 `HEAL_SKILL_BOOST`；
- 治疗削弱将 `reserved1/2` 取反后映射到 `HEAL_SKILL_DEBOOST` 的 `delta/value`，保持真端正负方向；
- 四族共 176 条记录已按技能 ID、效果槽位、属性、公式和飞行条件逐条核对，本批继续不重建 V3。

`Bleed`、`Poison` 已分别覆盖真端全部 131、154 条效果：

- 两族共用 `reserved8/9 -> delta/value`，每跳伤害按 `delta × skillLevel + value` 计算，真端 `checktime` 直接作为周期；
- `reserved10` 继续映射元素；2 条 Bleed、1 条 Paralyze、1 条飞行状态条件及 10 条 Poison 背击条件均由现有 `conditions` 完整表达；
- `reserved11/12` 和 Poison 的少量其他保留槽未发现独立运行时读取，继续保留在 `retail_fields`；
- 两族共 285 条记录已按技能 ID、效果槽位、公式、周期、元素和条件逐条核对，本批继续不重建 V3。

`Confuse`、`OpenAerial`、`CloseAerial`、`Fall` 已分别覆盖真端全部 39、34、53、24 条效果：

- 四族直接复用现有混乱、浮空开始、浮空结束和强制落地运行时，不新增 Java 效果类；
- 150 条记录只需编译持续时间、随机时间、效果 ID、无抗性、命中、前置效果、元素及 HOP 等公共效果字段；未发现需要编译的专用保留槽；
- 输出已按技能 ID、效果槽位和公共字段进行内存级逐条核对，本批继续不重建 V3。

`Hide` 已覆盖真端全部 74 条效果：

- `reserved7=1/2/3/5/10/13/20` 分别映射到现有 `HIDE1/2/3/5/10/13/20` 可视状态；其他值拒绝生成；
- `reserved3 -> buffcount`、`reserved4 -> type`，直接复用现有隐身动作次数和受伤取消策略；技能 420 按真端 `buffcount=1` 生成，不沿用旧模板手工写入的 5；
- 真端将移动速度修正为 `clamp(reserved1 × skillLevel + reserved2, 0, 100) - 100` 百分比；当前 74 条的 `reserved1` 均为 0，因此生成 `SPEED PERCENT` 的 `delta=0` 和 `value=reserved2-100` 可完整表达现有数据；
- 74 条记录已按技能 ID、效果槽位、状态、动作字段和速度修正逐条核对，本批继续不重建 V3。

`Sanctuary` 已覆盖真端全部 81 条效果：

- 81 条记录只使用持续时间、效果 ID、无抗性、命中、前置效果、元素及 HOP 等公共效果字段，没有专用运行参数；
- 现有 `SanctuaryEffect` 作为保护标记加入效果控制器，普通驱散、反击驱散和驱散计数均跳过该效果，真端 AI 条件也复用这一标记；
- 输出已按技能 ID、效果槽位和公共字段进行内存级逐条核对，本批继续不重建 V3。

`StatBoost` 已覆盖真端全部 218 条效果：

- `reserved13` 映射属性名，`reserved1/2` 生成 `ADD` 的 `delta/value`，`reserved3/4` 生成 `PERCENT` 的 `delta/value`；同一记录两组均非零时同时保留两种修正；
- 攻击间隔沿用 AionEmu 的反号约定，移动/飞行速度和触发减伤率强制使用百分比；补齐 `arSpin/AGI/VIT/DEX/ProcReduceRate` 五个旧转换表没有的真端属性名；
- `reserved9` 是真端底层属性应用路径选择，AionEmu 的统一属性容器不区分该调用入口，原值继续保留在 `retail_fields`；
- 218 条记录已按技能 ID、效果槽位、属性展开、模式和两组等级公式逐条核对，本批继续不重建 V3。

`WpnDual` 已覆盖真端全部 11 条效果：

- 真端 `reserved1/2` 是双持技能效率的等级公式；当前 11 条的 `reserved1` 均为 0，因此 `reserved2 -> skill_efficiency` 可由现有运行时完整表达；
- `reserved3/4 -> max_damage_delta/max_damage_chance`，完整攻击概率为 `reserved3 x skillLevel + reserved4`；`reserved5/6 -> delta/value`，非完整攻击伤害比例为 `reserved5 x skillLevel + reserved6`；
- `reserved7/8` 是招架比例公式，当前 11 条均未设置；转换器遇到非零的技能效率等级增量或招架公式时拒绝生成，避免未来数据被静默降级；
- 11 条记录已按技能 ID、效果槽位和三组现有运行时可表达的公式逐条核对，本批继续不重建 V3。

`WpnMastery` 已覆盖真端全部 123 条效果：

- `reserved5` 映射 13 种武器类型；剑、锤、匕首、法杖、双手剑、长枪和弓修正物理攻击，法书、宝珠、枪炮、魔炮、弦乐器和启动钥匙修正魔法攻击；
- 真端 `reserved1/2` 是武器伤害倍率公式，生成 `PERCENT delta/value`；`reserved3/4` 是固定伤害加成公式，生成 `ADD delta/value`，最终值都按 `delta x skillLevel + value` 计算；
- AionEmu 的武器专精函数现在保留原始 `change` 类型：倍率继续写入武器固定加成率，固定伤害走普通加法；单手魔法武器不再被旧物理攻击专用分支忽略；
- 123 条记录已按技能 ID、效果槽位、武器类型和两组公式逐条核对，其中第二组非零的 8 条不再沿用旧模板的零值遗漏；本批继续不重建 V3。

`AmrMastery` 已覆盖真端全部 38 条效果：

- 真端 PE 调试字符串及玩家属性布局确认三组依次为物理防御、魔法防御和回避；`reserved5` 映射 `CHAIN/CLOTHES/LEATHER/PLATE/ROBE` 五种防具类型；
- `reserved1/2` 与 `reserved3/4` 分别生成 `PHYSICAL_DEFENSE` 的 `PERCENT/ADD`，`reserved6/7` 与 `reserved8/9` 分别生成 `MAGICAL_RESIST` 的 `PERCENT/ADD`，`reserved10/11` 与 `reserved12/13` 分别生成 `EVASION` 的 `PERCENT/ADD`；
- AionEmu 防具专精包装器现在保留原始函数类型，固定值不再被错误当作百分比；38 条记录只做内存级逐条核对，本批继续不重建 V3。

`SubTypeBoostResist` 已覆盖真端全部 3 条效果：

- 真端 `reserved1/2` 生成 `BOOST_RESIST_DEBUFF ADD` 的 `delta/value`，抗性增量按 `delta x skillLevel + value` 计算；
- 当前 3 条的 `reserved3` 均为 `Debuff`，其他未支持子类型拒绝生成，避免把不同子类型误编译为通用减益抗性；
- 3 条记录已按技能 ID、效果槽位、子类型和公式逐条核对，无需修改现有 Java 运行时，本批继续不重建 V3。

`ResurrectPositional` 已覆盖真端唯一 1 条效果（技能 4004，效果槽 1）：

- 真端 `reserved6=PR_ResurrectDebuff1` 解析为复活处罚技能 ID `8295`，生成现有 `resurrectpos skill_id="8295"`；
- `reserved7=1` 是处罚技能等级参数，当前处罚技能 8295 本身为 1 级，AionEmu 复活流程按该技能模板应用灵魂病，无需扩展 XML 或 Java 模型；
- 唯一记录已核对效果槽、处罚技能、无抗性和 HOP 字段，本批继续不重建 V3。

`AbsoluteStatToPCDebuff`、`CondSkillLauncher`、`DispelBuffCounterATK`、`SummonHouseGate` 已分别覆盖真端全部 1、2、3、3 条效果：

- 绝对属性减益的 `reserved1` 通过真端 `absolute_stat_to_pc.xml` 名称索引解析为 `statsetid`；当前记录 `Stat_BNWI_DeformAbstatdSA_QooQoo -> 28`，继续复用现有绝对属性集合运行时；
- 条件技能触发的 `reserved3` 通过真端技能名称索引解析为 `skill_id`，`reserved4 -> value`，`reserved13 -> HP/MP type`；当前两条分别解析为技能 8930/8448 和阈值 10/50；
- 驱散反击的 `reserved2 -> value`、`reserved8/9 -> hitdelta/hitvalue`、`reserved16 -> dispel_level`、`reserved17/18 -> dpower/power`，元素与 HOP 继续使用公共映射；
- 房屋传送门的 `reserved2 -> time`、`reserved9` 通过真端 NPC 名称索引解析为 `npc_id`，三条记录分别得到 749246、749247、749017；
- 四族共 9 条记录直接复用现有 Java 效果类，只增加转换映射和名称索引；本批继续不重建 V3，也不提前切换正式模板。

`SummonGroupGate` 已覆盖真端全部 4 条效果：

- 与 `SummonHouseGate` 共用 `reserved2 -> time`、`reserved9` 真端 NPC 名称索引到 `npc_id` 的映射，四条记录分别解析为 833207、833208、749017、749083；
- 现有 `SummonGroupGateEffect` 已完整承载生成、归属和定时消失逻辑，无需 Java 或 XSD 改动；本批继续不重建 V3。

`ResurrectBase`、`Rebirth`、`Resurrect` 已分别覆盖真端全部 6、9、9 条效果：

- 三族共用 `reserved6` 真端技能名称到 `skill_id` 的解析，处罚技能等级 `reserved7` 当前 24 条均为 1；
- `Rebirth` 额外将 `reserved2 -> resurrect_percent`，当前百分比范围为 0..95；持续时间、随机时间、效果 ID、无抗性、前置效果、元素和 HOP 使用公共映射；
- 24 条记录直接复用现有 `ResurrectBaseEffect`、`RebirthEffect` 和 `ResurrectEffect`，无需 Java 或 XSD 改动；本批继续不重建 V3。

`ConvertHeal` 已覆盖真端全部 4 条效果：

- `reserved1/2 -> hitdelta/hitvalue`，`reserved7/8 -> delta/value`，`reserved6=1 -> percent=true`，`reserved9 -> hitpercent`，`reserved13 -> HP/MP type`；
- `reserved_cond1` 及其概率字段映射到现有盾观察者的命中类型，四条均为 `EVERYHIT`；现有 `ConvertHealEffect` 已按技能等级使用两组公式；
- 4 条记录无需 Java 或 XSD 改动，本批继续不重建 V3。

`AbsoluteStatToPCBuff` 已覆盖真端全部 83 条效果：

- `reserved1` 通过真端 `absolute_stat_to_pc.xml` 名称索引解析为 `statsetid`，83 条使用的 28 个名称均成功解析；
- `reserved2 -> value`，当前为 0 或 10；持续时间、效果 ID、无抗性、前置效果、元素和 HOP 继续使用公共映射；
- 直接复用现有 `AbsoluteStatToPCBuffEffect` 和绝对属性集合加载器，无需 Java 或 XSD 改动；本批继续不重建 V3。

`RideRobot` 已覆盖真端全部 12 条效果：

- 12 条记录的专用 `reserved1..18` 均为零或空，只生成效果 ID、基础等级、无抗性和 HOP 等公共字段；
- 机甲 ID 继续由现有 `RideRobotEffect` 从主手武器模板读取，不在技能 XML 中重复生成；本批继续不重建 V3。

`APBoost`、`XPBoost`、`BoostDropRate`、`DRBoost`、`BoostHate` 已分别覆盖真端全部 21、41、52、45、18 条效果：

- 五族共用 `reserved1/2 -> delta/value` 等级公式；AP、掉落率和掉落稀有度使用 `ADD`，仇恨使用 `PERCENT`；
- `XPBoost` 将同一百分比公式应用到制造、采集、组队狩猎、狩猎和怪物图鉴五种现有经验属性，全部由 `RewardType` 的实际结算路径消费；
- `DRBoost reserved5/6 -> minlevel/maxlevel`，补齐旧模板遗漏的目标等级范围；当前 45 条中 4 条设置边界，运行时在范围外不应用效果；
- `reserved10` 继续映射元素；五族共 177 条记录只做聚焦转换和逐条内存核对，本批继续不重建 V3。

`Curse` 已覆盖真端全部 34 条效果：

- 真端恢复源码确认 `reserved1/2` 按技能等级计算后取负，同时作用于 `MAXHP/MAXMP`；
- `reserved6=1` 使用 `PERCENT`，否则使用 `ADD`；当前 27 条百分比记录和 7 条固定值记录均可由现有 `CurseEffect` 的属性修正直接承载；
- `reserved10/11/12` 继续使用公共元素和暴击概率字段映射；34 条记录只做聚焦转换和逐条内存核对，本批继续不重建 V3。

`ShieldMastery` 已覆盖真端全部 6 条效果：

- 真端 `reserved1/2` 生成 `BLOCK PERCENT` 的 `delta/value`，盾牌格挡率增量按 `reserved1 x skillLevel + reserved2` 计算；
- 现有 `ShieldMasteryEffect` 仅在装备盾牌时应用该百分比，无需修改 Java 或 XSD；6 条记录只做聚焦转换和逐条内存核对，本批继续不重建 V3。

`Evade` 已覆盖真端全部 19 条效果：

- `reserved1=Effect_Type`，`reserved2..8` 映射待解除的效果类型；`stunlike` 按真端展开为 `STUN/STUMBLE/STAGGER/OPENAERIAL/SPIN`；
- `reserved13/14 -> delta/value` 控制最大解除数，`reserved15/16 -> dispel_level_delta/dispel_level`，真端驱散强度固定为 100；补齐旧模板遗漏的驱散等级 1/2；
- 现有 `EvadeEffect` 已复用完整的 `DispelEffect` 字段和移动逻辑，无需修改 Java 或 XSD；19 条记录只做聚焦转换和逐条内存核对，本批继续不重建 V3。

`SpellATKDrain` 已覆盖真端全部 16 条效果：

- `reserved8/9 -> delta/value`，周期伤害公式为 `delta x skillLevel + value`；`reserved12` 按是否非零控制魔法防御减伤，`checktime` 和 `reserved10` 分别映射周期与元素；
- `reserved14/15`、`reserved16/17` 分别是真端 HP、MP 吸取百分比的等级公式；当前 16 条的两个等级增量均为 0，因此 `reserved15 -> hp_percent`、`reserved17 -> mp_percent` 可由现有 XML 完整表达；
- `reserved2/7/11` 在真端 `SkillEffectSpellAttackDrain` 的检查、抗性与周期结算中未读取，原值继续保留在 `retail_fields`；运行时改为遵守技能级 `applymboost` 和效果级 `mrresist`，不再硬编码开启；
- 16 条记录已按技能 ID、效果槽位、伤害、魔防、HP/MP 吸取、周期和元素逐条核对；转换规则进度为 116/174，本批继续不重建 V3。

`ProcVPHeal_Instant` 已覆盖真端全部 17 条效果：

- `reserved1/2 -> delta/value`，恢复量按 `delta x skillLevel + value` 计算；`reserved6=1` 时按安息能量上限的千分比恢复；
- `reserved3/4` 是恢复上限百分比的等级公式；当前 17 条的 `reserved4` 均为 0，因此 `reserved3 -> value2` 可由现有 XML 完整表达，未来遇到非零增量时拒绝生成；
- `reserved11` 在真端检查、抗性和触发结算中未读取，原值继续保留在 `retail_fields`；现有 `ProcVPHealInstantEffect` 已完整承载恢复和上限逻辑，无需修改 Java 或 XSD；
- 17 条记录已按技能 ID、效果槽位、恢复量、百分比模式和恢复上限逐条核对；转换规则进度为 117/174，本批继续不重建 V3。

`CaseHeal` 已覆盖真端全部 12 条效果：

- `reserved1/2 -> delta/value`，`reserved6=1 -> percent=true`，`reserved10 -> cond_value`，`reserved13 -> HP/MP type`；补齐旧模板中技能 21886 漏掉的百分比治疗标记；
- 真端 `reserved8/9` 是触发概率的等级公式，当前 12 条均为固定 100%，因此现有运行时的必定触发可完整承载；未来遇到非默认概率公式时转换器拒绝生成；`reserved7` 在真端结算路径中未读取并继续保留；
- 运行时治疗加成改为遵守技能级 `applyhealboost`，并使用施法者而非受影响者的治疗属性；12 条记录已逐条核对，转换规则进度为 118/174，本批继续不重建 V3。

`HealCastorOnAttacked` 已覆盖真端全部 9 条效果：

- `reserved1/2 -> delta/value`，恢复量按技能等级计算；`reserved3/4` 是作用范围的等级公式，当前 `reserved3` 全为 0，因此 `reserved4 -> range` 可由现有 XML 完整表达；
- `reserved14` 当前全为 `Castor_Party`，映射为现有 `healcastoronatk type="HP"` 的施法者队伍恢复逻辑；其他目标模式或非零范围增量拒绝生成；`reserved10` 继续映射元素；
- 9 条记录已按技能 ID、效果槽位、恢复量、范围、目标模式和元素逐条核对，无需修改 Java 或 XSD；转换规则进度为 119/174，本批继续不重建 V3。

`BoostSkillCost` 已覆盖真端全部 17 条效果：

- `reserved1/2 -> delta/value`，技能资源消耗修正按 `delta x skillLevel + value` 计算；运行时补齐原先遗漏的等级增量；
- 真端 `reserved5/6` 还可在效果结束时修正 MP，当前 17 条均为不产生变化的 `0/100`；未来遇到其他公式时拒绝生成；
- 17 条记录已按技能 ID、效果槽位、消耗公式和结束 MP 公式逐条核对，无需修改 XSD；转换规则进度为 120/174，本批继续不重建 V3。

`MagicCounterATK` 已覆盖真端全部 15 条效果：

- `reserved1/2 -> delta/value`，反击伤害按目标最大 HP 的 `delta x skillLevel + value` 百分比计算；`reserved5 -> maxdmg` 设置伤害上限，`reserved10 -> element`；
- 运行时补齐原先遗漏的百分比等级增量；`reserved11/12` 在真端参数检查、抗性和反击路径中未读取，继续保留在 `retail_fields`；
- 15 条记录已按技能 ID、效果槽位、伤害百分比、上限和元素逐条核对，无需修改 XSD；转换规则进度为 121/174，本批继续不重建 V3。

`SkillCooltimeReset` 已覆盖真端全部 12 条效果：

- `reserved1=Delay_ID` 时把 `reserved2..8` 作为离散冷却 ID，当前多值记录均连续，可用 `first_cd/second_cd` 无损表达；`Delay_Id_Range` 使用 `reserved2/3` 作为范围，五条阵营战食物效果正确覆盖 `1..1000000`；
- `reserved9=0` 按目标技能原始冷却的百分比缩减，`reserved9=1` 按固定毫秒缩减；`reserved10/11` 是按被缩减技能等级计算的增量/基础值，当前 `reserved10` 全为 0，因此生成 `delta=0/value=reserved11`，未来非零增量在运行时模型能区分目标技能等级前拒绝生成；新增显式 `percent` 字段，同时兼容旧模板以 `delta` 表示百分比的写法；
- 运行时改为只遍历当前存在的冷却记录，避免百万 ID 范围空转，并修复旧整数除法导致 1% 至 99% 不生效的问题；12 条记录按技能 ID、效果槽位、目标冷却、模式和缩减量逐条核对；转换规则进度为 122/174，本批继续不重建 V3。

`WeaponStatUp`、`WeaponStatBoost` 已分别覆盖真端全部 15、20 条效果：

- 两族的 `reserved5` 映射武器条件，当前覆盖弓、双手剑、启动钥匙和全部武器；`reserved13` 映射物理攻击、攻击距离和攻击速度；
- `WeaponStatUp reserved1/2` 是等级增量/基础值，`reserved6` 区分固定值和百分比；攻击距离固定值乘 1000 转为 AionEmu 距离单位，攻击速度百分比取反；当前 15 条只使用第一组属性公式，其他非零公式拒绝生成；
- `WeaponStatBoost reserved1/2` 是固定距离，`reserved3/4` 是百分比距离，20 条记录均只启用其中一组；`reserved9=1` 的飞行弓被动生成 `onfly` 条件，`allwp` 不附加武器限制；两族共 35 条逐条核对，无需修改 Java 或 XSD；转换规则进度为 124/174，本批继续不重建 V3。

`DelayedSpellATK_Instant` 已覆盖真端全部 27 条效果：

- `reserved1/2 -> delta/value`，`reserved6=1 -> mode=PERCENT`，`reserved10 -> element`；
- 真端延迟按 `reserved8 x skillLevel + reserved9` 计算，新增 `delaydelta/delay` 完整保存公式，并保留负值回退 500 ms 的真端行为；
- 27 条记录已按技能 ID、效果槽位、伤害、模式、延迟和元素逐条核对；转换规则进度为 125/174，本批继续不重建 V3。

`TargetChange` 已覆盖真端全部 103 条效果：

- `reserved1=0` 生成 `target_effector=false` 并清空玩家目标，`reserved1=1` 生成 `target_effector=true` 并把目标切换到施法者；其他值拒绝生成；
- 运行时删除技能 ID 白名单与 25% 随机分支，直接执行真端目标类型；当前 100 条切向施法者、3 条清空目标；
- 103 条记录已按技能 ID、效果槽位和目标类型逐条核对；转换规则进度为 126/174，本批继续不重建 V3。

`ReturnHome` 已覆盖真端全部 5 条效果：

- 五条记录的专用 `reserved1..25` 均为空，直接生成现有 `return` 效果；效果位置、无抗性等继续使用公共字段；
- 真端源码只执行回城逻辑，不解析额外参数；无需修改 Java 或 XSD，转换规则进度为 127/174，本批继续不重建 V3。

`PetOrderUseUltraSkill` 已覆盖真端全部 15 条效果：

- `reserved1 -> ultra_skill`，真端以 3 为首个奥义序号；`reserved2=1 -> release=true`，用于宠物完成技能后自动解除；
- `reserved19=1` 的 7 条记录在该效果真端解析和执行路径中均未读取，继续保留在 `retail_fields`，不误映射为解除标记；
- 运行时按 `ultra_skill=3..16` 复用现有 14 组 `pet_skills.xml` 映射，补齐复用奥义 5 的技能 11608 没有直接 `order_skill` 映射的问题，并保留旧模板缺少该属性时按技能 ID 查询的兼容行为；15 条记录只做聚焦转换和逐条内存核对，转换规则进度为 128/174，本批继续不重建 V3。

`PetOrderUnSummon` 已覆盖真端全部 4 条效果：

- 四条记录没有专用 `reserved` 参数，只生成效果位置、持续时间、无抗性和前置效果等公共字段；
- 真端直接强制解除施法者的召唤物，新增运行时效果复用现有召唤解除流程；转换规则进度为 129/174，本批继续不重建 V3。

`BoostSpellAttackEffect` 已覆盖真端全部 47 条效果：

- `reserved1/2 -> BOOST_SPELL_ATTACK PERCENT delta/value`，法术攻击倍率按 `delta x skillLevel + value` 计算；当前 47 条的 `reserved1` 均为 0；
- `reserved9=1` 的两条飞行被动生成 `onfly` 条件；`reserved3/4` 是真端第二组公式，当前全部为 0，未来出现非零数据时在确认对应 AionEmu 属性前拒绝生成；
- 直接复用现有 `BoostSpellAttackEffect` 和法术伤害结算；47 条只做聚焦转换和逐条内存核对，转换规则进度为 130/174，本批继续不重建 V3。

`MPShield` 已覆盖真端全部 14 条效果：

- `reserved1/2 -> hitdelta/hitvalue` 控制单次吸收比例，`reserved6=1 -> percent=true`；`reserved7/8 -> delta/value` 控制护盾总吸收量；
- `reserved3/4 -> mp_delta/mp_value`，每点吸收伤害消耗的 MP 比例按 `delta x skillLevel + value` 计算并按真端上限截到 100%；修复旧模板虽然写入 `mp_value`、Java 却未绑定和使用的问题；
- `reserved_cond1` 及概率继续映射命中类型，当前 14 条均为 `EVERYHIT/1000`；固定 `reserved9=100` 继续保留在 `retail_fields`，未来其他值在证明语义前拒绝生成；14 条只做聚焦转换和逐条内存核对，转换规则进度为 131/174，本批继续不重建 V3。

`Aura`、`Protect`、`Reflector` 已分别覆盖真端全部 37、118、133 条效果：

- `Aura reserved1` 通过真端技能名称索引解析为 `skill_id`，`reserved3/4 -> distance/distance_z`；当前目标关系固定为 `Party/Friend`，并保留施法者包含标记；
- `Protect reserved1/2 -> hitdelta/hitvalue` 表示被保护者减伤公式，`reserved5 -> radius`，`reserved6=1 -> percent=true`，`reserved8/9 -> delta/value` 表示保护者承伤公式；运行时不再把两组公式混用，补齐技能 16756 原模板遗漏的等级增量；
- `Reflector reserved1/2 -> hitdelta/hitvalue`、`reserved4/5 -> minradius/radius`、`reserved7/8 -> delta/value`，并保留命中类型、触发概率、百分比和元素字段；`reserved9=1` 的技能攻击专用标记继续保留在 `retail_fields`，由 `reserved_cond1=skill` 承载现有运行时过滤；
- 三族共 288 条记录只做聚焦转换和逐条内存核对，转换规则进度为 134/174，本批继续不重建 V3。

`TargetTeleport`、`DelayedSkill`、`SkillLauncher` 已分别覆盖真端全部 9、43、79 条效果：

- `TargetTeleport reserved9=0` 使用施法者前方位置，`reserved11 -> distance`；`reserved9=1` 使用 `reserved10` 的地图别名，运行时复用零售 AI 位置别名索引，不再直接跳过三条别名传送；
- `DelayedSkill reserved1` 通过真端技能名称索引解析为 `skill_id`，`reserved2/3` 生成后续技能等级的 `delta/value` 公式，`reserved4=1` 时直接沿用当前技能等级；这补齐旧模板把固定 60/75 级等公式简化成 `value=1` 的丢失；
- `SkillLauncher reserved1 -> skill_id`、`reserved2 -> value`（后续技能等级）、`reserved4 -> delay`、`reserved6=Force -> group=FORCE`；`cond_status` 映射异常状态条件，并为 `Stunlike` 增加任一击倒类状态匹配；
- 三族共 131 条记录只做聚焦转换和逐条内存核对，转换规则进度为 137/174，本批继续不重建 V3。

`OneTimeBoostSkillAttack`、`RandomMoveLoc` 已分别覆盖真端全部 13、29 条效果：

- 一次性技能攻击将 `reserved1/2` 映射为伤害百分比等级公式，`reserved3/4` 映射为固定伤害公式，`reserved5` 映射物理/魔法/全部技能类型，`reserved6/7` 映射生效次数公式；
- `reserved8/9` 与 `reserved11/12` 分别补齐旧模板遗漏的技能命中百分比和固定值公式；运行时按当前物理或魔法命中换算为命中修正，并在最后一次匹配攻击后移除效果；
- 随机位移将 `reserved1/2 -> distance_delta/distance`，`reserved3=1/2` 映射为前方/后方；位移距离现在按技能等级计算，不再丢失真端增量字段；
- 两族共 42 条记录只做聚焦转换和逐条内存核对，转换规则进度为 139/174，本批继续不重建 V3。

`SkillATKDrain_Instant`、`SpellATKDrain_Instant` 已分别覆盖真端全部 92、126 条效果：

- 物理吸取将 `reserved1/2` 保存为武器伤害百分比公式，`reserved3/4` 保存为固定伤害公式；法术吸取以 `reserved1/2` 为主伤害，`reserved8/9` 按 `reserved5` 保存为固定或百分比附加伤害；运行时现在可同时结算主伤害、固定伤害和百分比伤害，不再丢弃其中一组；
- `reserved14/15`、`reserved16/17` 分别映射为 HP、MP 吸取百分比的 `delta/value` 公式；法术 `reserved6` 显式控制魔增应用，补齐旧模板只保存固定吸取比例且始终沿用技能级魔增标记的问题；
- 法术 `reserved3/4 + reserved7` 与物理 `reserved20/21 + reserved22` 通过现有 `modifiers` 保存条件伤害公式，覆盖 TYPE_A/B/C/D、TRICODARK、LIVINGWATER、UNDEAD、PARALYZE 和背后攻击；公式保持 `delta x skillLevel + value`，不使用旧模板已经求值的结果；
- 两族共 218 条记录只做聚焦转换和逐条内存核对，`errors=0`；转换规则进度为 141/174，本批继续不重建 V3。

`DashATK`、`BackDashATK`、`MoveBehindATK` 已分别覆盖真端全部 55、28、20 条效果：

- 三族共用物理伤害公式：`reserved2 != 0 && reserved4 == 0` 时按武器伤害百分比结算，否则 `reserved3/4 -> delta/value` 按技能等级计算固定伤害；
- `BackDashATK reserved12 -> distance`，保留真端 5/15/25 米三档后撤距离；前冲和绕后落点由双方碰撞体积、朝向及地形碰撞计算，不存在 XML 距离等级公式；
- `DashATK reserved20/21 + reserved22` 通过现有 `modifiers` 保留条件附加伤害等级公式，当前 12 条覆盖 `TYPE_B/C/D`；这补齐旧模板只保存已求值结果、丢失 `delta/value` 公式的问题；
- `MoveBehindATK reserved5=-300` 和三条 `BackDashATK reserved13=20` 在对应真端参数检查、位移和伤害路径中均未读取，继续保留在 `retail_fields`；三族共 103 条只做聚焦转换和逐条内存核对，转换规则进度为 144/174，本批继续不重建 V3。

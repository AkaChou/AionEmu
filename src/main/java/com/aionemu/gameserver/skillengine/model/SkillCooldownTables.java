package com.aionemu.gameserver.skillengine.model;

/**
 * 技能冷却查表：烙印强化技能按技能 ID 的冷却缩减表。
 * Skill cooldown tables: stigma-enchant cooldown reductions keyed by skill id.
 *
 * <p>该类型只服务 {@link Skill}：纯静态查表（技能 ID → 冷却缩减秒数），不持有状态、
 * 不创建对象；对外仍通过 {@link Skill} 的原 public 方法访问（门面签名不变）。
 * This type only serves {@link Skill}: a pure static lookup (skill id to cooldown reduction)
 * holding no state and allocating nothing. External callers keep using the original public
 * {@link Skill} facade method with unchanged signature.</p>
 */
final class SkillCooldownTables {

	/**
	 * 仅静态查表，禁止实例化。
	 * Static tables only; not instantiable.
	 */
	private SkillCooldownTables() {
	}

	/**
	 * 计算烙印附魔冷却。
	 * Computes stigma enchant cooldown.
	 *
	 * @param skill 技能实例 / skill
	 * @param cooldown 基础冷却 / base cooldown
	 * @return 最终冷却 / final cooldown
	 */
	static int StigmaEnchantCoolDown(Skill skill, int cooldown) {
		if (skill == null) {
			return 0;
		}
		int SkillLevel = skill.getSkillLevel();
		if (skill.getSkillTemplate().getCooldownDelta() != 0) {
			return Math.max(0, cooldown + skill.getSkillTemplate().getCooldownDelta() * SkillLevel);
		}
		return switch (skill.getSkillId()) { // 气魄 / Dauntless Spirit
			// 气魄 / Dauntless Spirit
			// 气魄 / Dauntless Spirit
			// 气魄 / Dauntless Spirit
			// 气魄 / Dauntless Spirit
			// 气魄 / Dauntless Spirit
			// 气魄 / Dauntless Spirit
			// 气魄 / Dauntless Spirit
			// Wind Lance
			// Wind Lance
			// Wind Lance
			// Wind Lance
			// Wind Lance
			// Wind Lance
			// 回旋一击 / Whirling Strike
			// 回旋一击 / Whirling Strike
			// 回旋一击 / Whirling Strike
			// 透视陷阱 / Trap Of Clairvoyance
			// 透视陷阱 / Trap Of Clairvoyance
			// 冰河重击 / Glacial Shard
			// 冰河重击 / Glacial Shard
			// 冰河重击 / Glacial Shard
			// 灭火 / Annihilation
			// 灭火 / Annihilation
			// 灭火 / Annihilation
			// 灭火 / Annihilation
			// 灭火 / Annihilation
			// 灭火 / Annihilation
			// 灭火 / Annihilation
			// 灭火 / Annihilation
			// 生命之咒语 / Word Of Life
			// 生命之咒语 / Word Of Life
			// 生命之咒语 / Word Of Life
			// 生命之咒语 / Word Of Life
			// 生命之咒语 / Word Of Life
			// 生命之咒语 / Word Of Life
			// 生命之咒语 / Word Of Life
			// 生命之咒语 / Word Of Life
			// 波动攻击 / Disorienting Blow
			// 波动攻击 / Disorienting Blow
			// 波动攻击 / Disorienting Blow
			// 波动攻击 / Disorienting Blow
			// 波动攻击 / Disorienting Blow
			// 波动攻击 / Disorienting Blow
			// 爆裂 / Burst
			// 爆裂 / Burst
			// 爆裂 / Burst
			// 爆裂 / Burst
			// 爆裂 / Burst
			// 爆裂 / Burst
			// 爆裂 / Burst
			// 爆裂 / Burst
			// Word of Instigation
			// Word of Instigation
			// Word of Instigation
			// 魔力之恩惠 I / Stopping Power
			// 填装魔力弹 I / Autoload
			// 必中魔眼 / Sighting
			// 必中魔眼 / Sighting
			// 必中魔眼 / Sighting
			// 必中魔眼 / Sighting
			// 必中魔眼 / Sighting
			// 必中魔眼 / Sighting
			// 盔甲破坏 / Drillbore
			// 盔甲破坏 / Drillbore
			// 盔甲破坏 / Drillbore
			// 盔甲破坏 / Drillbore
			// 盔甲破坏 / Drillbore
			// 盔甲破坏 / Drillbore
			// 盔甲破坏 / Drillbore
			// 盔甲破坏 / Drillbore
			// 要害戳刺 / Debilitating Blade
			// 要害戳刺 / Debilitating Blade
			// 要害戳刺 / Debilitating Blade
			// 要害戳刺 / Debilitating Blade
			// 要害戳刺 / Debilitating Blade
			// 要害戳刺 / Debilitating Blade
			// Aether Recharge
			// Aether Recharge
			// Aether Recharge
			// Aether Recharge
			// Aether Recharge
			// Aether Recharge
			// Aether Recharge
			// Aether Recharge
			// 电场束缚 / Convulsion Beam
			// 电场束缚 / Convulsion Beam
			// 电场束缚 / Convulsion Beam
			// 电场束缚 / Convulsion Beam
			// 电场束缚 / Convulsion Beam
			// 电场束缚 / Convulsion Beam
			// Invigorating Strike
			// Invigorating Strike
			// Invigorating Strike
			// 愤怒诱发 / Incite Rage
			// 愤怒诱发 / Incite Rage
			// 愤怒诱发 / Incite Rage
			// 愤怒诱发 / Incite Rage
			// 愤怒诱发 / Incite Rage
			// 愤怒诱发 / Incite Rage
			// 愤怒诱发 / Incite Rage
			// 愤怒诱发 / Incite Rage
			// 保护之盾 / Holy Shield
			// 保护之盾 / Holy Shield
			// 保护之盾 / Holy Shield
			// 保护之盾 / Holy Shield
			// 保护之盾 / Holy Shield
			// 保护之盾 / Holy Shield
			// 处决一击 / Punishing Thrust
			// 处决一击 / Punishing Thrust
			// 处决一击 / Punishing Thrust
			// 处决一击 / Punishing Thrust
			// 处决一击 / Punishing Thrust
			// 处决一击 / Punishing Thrust
			// 处决一击 / Punishing Thrust
			// 处决一击 / Punishing Thrust
			// Explosive Rebranding
			// Explosive Rebranding
			// Explosive Rebranding
			// 昏厥之刃 / Quickening Doom
			// 昏厥之刃 / Quickening Doom
			// 昏厥之刃 / Quickening Doom
			// 愤怒之眼 I / Eye Of Wrath
			// 影子下坠 / Shadowfall
			// 魔力诅咒 I / Magic's Freedom
			// 精灵强化:强化甲胄 I / Armor Spirit
			// 召唤:治愈之气息 / Summon Healing Servant
			// 召唤:治愈之气息 / Summon Healing Servant
			// 召唤:治愈之气息 / Summon Healing Servant
			// 召唤:治愈之气息 / Summon Healing Servant
			// 召唤:治愈之气息 / Summon Healing Servant
			// 召唤:治愈之气息 / Summon Healing Servant
			// 召唤:治愈之气息 / Summon Healing Servant
			// 召唤:治愈之气息 / Summon Healing Servant
			// 召唤:治愈之气息 / Summon Healing Servant
			// 召唤:治愈之气息 / Summon Healing Servant
			// 召唤:治愈之气息 / Summon Healing Servant
			// 召唤:治愈之气息 / Summon Healing Servant
			// 再生之光辉 / Splendor Of Rebirth
			// 再生之光辉 / Splendor Of Rebirth
			// 再生之光辉 / Splendor Of Rebirth
			// 再生之光辉 / Splendor Of Rebirth
			// 再生之光辉 / Splendor Of Rebirth
			// 再生之光辉 / Splendor Of Rebirth
			// 恢复阻断 I / Festering Wound
			// 霹雳 / Call Lightning
			// 霹雳 / Call Lightning
			// 霹雳 / Call Lightning
			// 平稳变奏曲 / Resonant Hymn
			// 平稳变奏曲 / Resonant Hymn
			// 平稳变奏曲 / Resonant Hymn
			// 平稳变奏曲 / Resonant Hymn
			// 平稳变奏曲 / Resonant Hymn
			// 平稳变奏曲 / Resonant Hymn
			// 平稳变奏曲 / Resonant Hymn
			// Blazing Requiem
			// Blazing Requiem
			// Blazing Requiem
			// 免罪旋律 / Chorus Of Blessing
			// 免罪旋律 / Chorus Of Blessing
			// 免罪旋律 / Chorus Of Blessing
			// 不和谐音 / Treble Cleave
			// 不和谐音 / Treble Cleave
			// 不和谐音 / Treble Cleave
			// Mvt.2: Summer
			// Mvt.2: Summer
			// Mvt.2: Summer
			// Mvt.2: Summer
			// Mvt.2: Summer
			// Mvt.2: Summer
			// Mvt.3: Autumn
			// Mvt.3: Autumn
			// Mvt.3: Autumn
			// Mvt.3: Autumn
			// Mvt.3: Autumn
			// Mvt.3: Autumn
			// 莫斯奇狂想曲 / Paean Of Pain
			// 莫斯奇狂想曲 / Paean Of Pain
			// 莫斯奇狂想曲 / Paean Of Pain
			// 莫斯奇狂想曲 / Paean Of Pain
			// 莫斯奇狂想曲 / Paean Of Pain
			// 莫斯奇狂想曲 / Paean Of Pain
			// Combustible Cacophony
			// Combustible Cacophony
			// Combustible Cacophony
			// Combustible Cacophony
			// Combustible Cacophony
			// Combustible Cacophony
			// Combustible Cacophony
			// Combustible Cacophony
			// 影子下坠 / Shadowfall
			// 影子下坠 / Shadowfall
			// 影子下坠 / Shadowfall
			// 影子下坠 / Shadowfall
			// 影子下坠 / Shadowfall
			case 564, 565, 566, 567, 568, 569, 570, 571, 727, 728, 729, 730, 731, 732, 755, 756, 757, 1100, 1101, 1324,
			     1325, 1326, 1640, 1641, 1642, 1643, 1644, 1645, 1646, 1647, 1727, 1728, 1729, 1730, 1731, 1732, 1733,
			     1734, 1863, 1864, 1865, 1866, 1867, 1868, 1883, 1884, 1885, 1886, 1887, 1888, 1889, 1890, 1907, 1908,
			     1909, 2046, 2054, 2268, 2269, 2270, 2271, 2272, 2273, 2391, 2392, 2393, 2394, 2395, 2396, 2397, 2398,
			     2409, 2410, 2411, 2412, 2413, 2414, 2464, 2467, 2470, 2473, 2476, 2479, 2482, 2485, 2711, 2712, 2713,
			     2714, 2715, 2716, 2919, 2920, 2921, 2945, 2946, 2947, 2948, 2949, 2950, 2951, 2952, 2961, 2962, 2963,
			     2964, 2965, 2966, 3147, 3148, 3149, 3150, 3151, 3152, 3153, 3154, 3242, 3243, 3244, 3246, 3247, 3248,
			     3312, 3330, 3731, 3796, 3980, 3981, 3982, 3983, 3984, 3985, 3986, 3987, 3988, 3989, 3990, 3991, 3998,
			     3999, 4000, 4001, 4002, 4003, 4134, 4164, 4165, 4166, 4384, 4385, 4386, 4387, 4388, 4389, 4390, 4474,
			     4477, 4480, 4484, 4485, 4486, 4487, 4488, 4489, 4491, 4492, 4493, 4494, 4495, 4496, 4497, 4498, 4499,
			     4500, 4501, 4502, 4524, 4525, 4526, 4527, 4528, 4529, 4572, 4573, 4574, 4575, 4576, 4577, 4578, 4579,
			     4591, 4592, 4593, 4594, 4595, 4596 -> // 影子下坠 / Shadowfall
				cooldown - 6 * SkillLevel; // 魔法防御 / Magical Defense
			// Unraveling Assault
			// Unraveling Assault
			// Unraveling Assault
			// 岩石召唤 / Summon Rock
			// 岩石召唤 / Summon Rock
			// 岩石召唤 / Summon Rock
			// 岩石召唤 / Summon Rock
			// 岩石召唤 / Summon Rock
			// 岩石召唤 / Summon Rock
			// 疾行激励 / Acceleration Cheer
			// 疾行激励 / Acceleration Cheer
			// 疾行激励 / Acceleration Cheer
			// 疾行激励 / Acceleration Cheer
			// 疾行激励 / Acceleration Cheer
			// 疾行激励 / Acceleration Cheer
			// 疾行激励 / Acceleration Cheer
			// 疾行激励 / Acceleration Cheer
			// 功率最大化 / Aethercharged Steel
			// 功率最大化 / Aethercharged Steel
			// 功率最大化 / Aethercharged Steel
			// 功率最大化 / Aethercharged Steel
			// 功率最大化 / Aethercharged Steel
			// 功率最大化 / Aethercharged Steel
			// 精灵强化:治愈 I / Healing Spirit
			// 全力疾行 I / Power Sprint
			// 弱化之印 / Enfeebling Burst
			// 弱化之印 / Enfeebling Burst
			// 弱化之印 / Enfeebling Burst
			// 弱化之印 / Enfeebling Burst
			// 弱化之印 / Enfeebling Burst
			case 600, 641, 642, 643, 1351, 1352, 1353, 1354, 1355, 1356, 1801, 1802, 1803, 1804, 1805, 1806, 1807, 1808,
			     2750, 2751, 2752, 2753, 2754, 2755, 3590, 3903, 4182, 4183, 4184, 4185, 4186,
			     4187 -> // 弱化之印 / Enfeebling Burst
				cooldown - 9 * SkillLevel; // 枯竭波 / Exhausting Wave
			// 枯竭波 / Exhausting Wave
			// 枯竭波 / Exhausting Wave
			// 枯竭波 / Exhausting Wave
			// 枯竭波 / Exhausting Wave
			// 枯竭波 / Exhausting Wave
			// 斩脚 / Tendon Slice
			// 斩脚 / Tendon Slice
			// 斩脚 / Tendon Slice
			// 斩脚 / Tendon Slice
			// 斩脚 / Tendon Slice
			// 斩脚 / Tendon Slice
			// 抓脚 I / Ankle Snare
			// 地震波动 / Earthquake Wave
			// 地震波动 / Earthquake Wave
			// 地震波动 / Earthquake Wave
			// 地震波动 / Earthquake Wave
			// 地震波动 / Earthquake Wave
			// 地震波动 / Earthquake Wave
			// 地震波动 / Earthquake Wave
			// 地震波动 / Earthquake Wave
			// 重生波 / Revival Wave
			// 重生波 / Revival Wave
			// 重生波 / Revival Wave
			// 重生波 / Revival Wave
			// 重生波 / Revival Wave
			// 重生波 / Revival Wave
			// 减速陷阱 / Trap Of Slowing
			// 减速陷阱 / Trap Of Slowing
			// 减速陷阱 / Trap Of Slowing
			// 减速陷阱 / Trap Of Slowing
			// 减速陷阱 / Trap Of Slowing
			// 减速陷阱 / Trap Of Slowing
			// 减速陷阱 / Trap Of Slowing
			// 减速陷阱 / Trap Of Slowing
			// 减速陷阱 / Trap Of Slowing
			// 减速陷阱 / Trap Of Slowing
			// 减速陷阱 / Trap Of Slowing
			// 减速陷阱 / Trap Of Slowing
			// 减速陷阱 / Trap Of Slowing
			// 减速陷阱 / Trap Of Slowing
			// 减速陷阱 / Trap Of Slowing
			// 减速陷阱 / Trap Of Slowing
			// 猎人的决心 I / Hunter's Might
			// 缚天陷阱 / Skybound Trap
			// 缚天陷阱 / Skybound Trap
			// 缚天陷阱 / Skybound Trap
			// 缚天陷阱 / Skybound Trap
			// 缚天陷阱 / Skybound Trap
			// 缚天陷阱 / Skybound Trap
			// 缚天陷阱 / Skybound Trap
			// 缚天陷阱 / Skybound Trap
			// 缚天陷阱 / Skybound Trap
			// 缚天陷阱 / Skybound Trap
			// 缚天陷阱 / Skybound Trap
			// 缚天陷阱 / Skybound Trap
			// 缚天陷阱 / Skybound Trap
			// 缚天陷阱 / Skybound Trap
			// 缚天陷阱 / Skybound Trap
			// 缚天陷阱 / Skybound Trap
			// Ripthread Shot
			// Ripthread Shot
			// Ripthread Shot
			// 暴风重击 / Storm Strike
			// 暴风重击 / Storm Strike
			// 暴风重击 / Storm Strike
			// 暴风重击 / Storm Strike
			// 暴风重击 / Storm Strike
			// 暴风重击 / Storm Strike
			// 暴风重击 / Storm Strike
			// 暴风重击 / Storm Strike
			// Resonant Strike
			// Resonant Strike
			// Resonant Strike
			// 束缚炮 / Paralysis Cannon
			// 束缚炮 / Paralysis Cannon
			// 束缚炮 / Paralysis Cannon
			// 束缚炮 / Paralysis Cannon
			// 束缚炮 / Paralysis Cannon
			// 束缚炮 / Paralysis Cannon
			// 灵魂炮 / Missile Guide
			// 灵魂炮 / Missile Guide
			// 灵魂炮 / Missile Guide
			// 灵魂炮 / Missile Guide
			// 灵魂炮 / Missile Guide
			// 灵魂炮 / Missile Guide
			// 灵魂炮 / Missile Guide
			// 灵魂炮 / Missile Guide
			// Sequential Fire
			// Sequential Fire
			// Sequential Fire
			// Pulverizer Cannon
			// Pulverizer Cannon
			// Pulverizer Cannon
			// 魔力凝聚 / Life Support Trigger
			// 魔力凝聚 / Life Support Trigger
			// 魔力凝聚 / Life Support Trigger
			// 魔力凝聚 / Life Support Trigger
			// 魔力凝聚 / Life Support Trigger
			// 魔力凝聚 / Life Support Trigger
			// 魔力凝聚 / Life Support Trigger
			// 魔力凝聚 / Life Support Trigger
			// 精神破坏 / Divine Justice
			// 精神破坏 / Divine Justice
			// 精神破坏 / Divine Justice
			// 精神破坏 / Divine Justice
			// 精神破坏 / Divine Justice
			// 精神破坏 / Divine Justice
			// Fangdrop Stab
			// Fangdrop Stab
			// Fangdrop Stab
			// Scoundrel's Bond
			// 雾砂攻击 / Venomous Strike
			// 雾砂攻击 / Venomous Strike
			// 雾砂攻击 / Venomous Strike
			// 雾砂攻击 / Venomous Strike
			// 雾砂攻击 / Venomous Strike
			// 雾砂攻击 / Venomous Strike
			// 雾砂攻击 / Venomous Strike
			// 逃跑姿态 I / Break Away
			// 命令:守护之墙 I / Spirit Wall Of Protection
			// 大地之守护 / Earthen Call
			// 大地之守护 / Earthen Call
			// 大地之守护 / Earthen Call
			// 大地之守护 / Earthen Call
			// 大地之守护 / Earthen Call
			// 大地之守护 / Earthen Call
			// 大地之守护 / Earthen Call
			// 大地之守护 / Earthen Call
			// 黑暗之诅咒 / Withering Gloom
			// 黑暗之诅咒 / Withering Gloom
			// 黑暗之诅咒 / Withering Gloom
			// 黑暗之诅咒 / Withering Gloom
			// 黑暗之诅咒 / Withering Gloom
			// 黑暗之诅咒 / Withering Gloom
			// 黑暗之诅咒 / Withering Gloom
			// 拯救之手 / Saving Grace
			// 拯救之手 / Saving Grace
			// 拯救之手 / Saving Grace
			// 拯救之手 / Saving Grace
			// 拯救之手 / Saving Grace
			// 拯救之手 / Saving Grace
			// 拯救之手 / Saving Grace
			// 拯救之手 / Saving Grace
			// 净化之水 / Ripple Of Purification
			// 净化之水 / Ripple Of Purification
			// 净化之水 / Ripple Of Purification
			// 净化之水 / Ripple Of Purification
			// 净化之水 / Ripple Of Purification
			// 净化之水 / Ripple Of Purification
			// 闪光 I / Blinding Light
			// 吸收之咒语 / Healing Conduit
			// 麻痹回声 I / Staggered Rest
			// 吸收之咒语 / Healing Conduit
			// 吸收之咒语 / Healing Conduit
			// 吸收之咒语 / Healing Conduit
			// 吸收之咒语 / Healing Conduit
			// 吸收之咒语 / Healing Conduit
			// 吸收之咒语 / Healing Conduit
			// 吸收之咒语 / Healing Conduit
			case 539, 540, 541, 542, 543, 544, 612, 613, 614, 615, 616, 617, 618, 698, 699, 700, 701, 702, 703, 704,
			     705, 749, 750, 751, 752, 753, 754, 849, 850, 851, 852, 853, 854, 855, 856, 857, 858, 859, 860, 861,
			     862, 863, 864, 888, 962, 963, 964, 965, 966, 967, 968, 969, 970, 971, 972, 973, 974, 975, 976, 977,
			     1006, 1007, 1008, 1486, 1487, 1488, 1489, 1490, 1491, 1492, 1493, 1901, 1902, 1903, 2109, 2110, 2111,
			     2112, 2113, 2114, 2274, 2277, 2280, 2283, 2286, 2289, 2292, 2295, 2371, 2374, 2377, 2380, 2381, 2382,
			     2450, 2451, 2452, 2453, 2454, 2455, 2456, 2457, 2939, 2940, 2941, 2942, 2943, 2944, 3239, 3240, 3241,
			     3245, 3255, 3256, 3257, 3258, 3259, 3260, 3261, 3327, 3531, 3562, 3563, 3564, 3565, 3566, 3567, 3568,
			     3569, 3575, 3576, 3577, 3578, 3579, 3580, 3581, 3924, 3925, 3926, 3927, 3928, 3929, 3930, 3931, 3992,
			     3993, 3994, 3995, 3996, 3997, 4135, 4368, 4490, 4631, 4632, 4633, 4634, 4635, 4636, 4637,
			     4638 -> // 吸收之咒语 / Healing Conduit
				cooldown - 24 * SkillLevel; // Battle Banner
			// Battle Banner
			// Battle Banner
			// Battle Banner
			// Battle Banner
			// Battle Banner
			// 抵抗的决心 I / Nature's Resolve
			// 祝福之弓 I / Bow Of Blessing
			// 冰雪甲胄 / Wintry Armor
			// 冰雪甲胄 / Wintry Armor
			// 冰雪甲胄 / Wintry Armor
			// 冰面 / Ice Sheet
			// 冰面 / Ice Sheet
			// 冰面 / Ice Sheet
			// 冰面 / Ice Sheet
			// 冰面 / Ice Sheet
			// 冰面 / Ice Sheet
			// 冰面 / Ice Sheet
			// 冰面 / Ice Sheet
			// 冰面 / Ice Sheet
			// 冰面 / Ice Sheet
			// 冰面 / Ice Sheet
			// 冰面 / Ice Sheet
			// 冰面 / Ice Sheet
			// 冰面 / Ice Sheet
			// 冰面 / Ice Sheet
			// 冰面 / Ice Sheet
			// 睡眠暴风 I / Sleeping Storm
			// 元素结界 I / Elemental Ward
			// 召唤台风 / Manifest Tornado
			// 召唤台风 / Manifest Tornado
			// 召唤台风 / Manifest Tornado
			// 召唤台风 / Manifest Tornado
			// 召唤台风 / Manifest Tornado
			// 召唤台风 / Manifest Tornado
			// 召唤台风 / Manifest Tornado
			// 召唤台风 / Manifest Tornado
			// 召唤台风 / Manifest Tornado
			// 召唤台风 / Manifest Tornado
			// 召唤台风 / Manifest Tornado
			// 召唤台风 / Manifest Tornado
			// 召唤台风 / Manifest Tornado
			// 召唤台风 / Manifest Tornado
			// Aetherblaze
			// Aetherblaze
			// Aetherblaze
			// 幻影漩涡 / Illusion Storm
			// 幻影漩涡 / Illusion Storm
			// 幻影漩涡 / Illusion Storm
			// 幻影漩涡 / Illusion Storm
			// 幻影漩涡 / Illusion Storm
			// 幻影漩涡 / Illusion Storm
			// 气概 / Rise
			// 气概 / Rise
			// 气概 / Rise
			// 气概 / Rise
			// 气概 / Rise
			// 气概 / Rise
			// 气概 / Rise
			// 风之祝福 / Blessing Of Wind
			// 风之祝福 / Blessing Of Wind
			// 风之祝福 / Blessing Of Wind
			// 风之祝福 / Blessing Of Wind
			// 风之祝福 / Blessing Of Wind
			// 风之祝福 / Blessing Of Wind
			// 铁壁之咒语 / Elemental Screen
			// 铁壁之咒语 / Elemental Screen
			// 铁壁之咒语 / Elemental Screen
			// Debilitating Incantation
			// Debilitating Incantation
			// Debilitating Incantation
			// 魔力之息 / Nature's Favor
			// 魔力之息 / Nature's Favor
			// 魔力之息 / Nature's Favor
			// 魔力之息 / Nature's Favor
			// 魔力之息 / Nature's Favor
			// 魔力之息 / Nature's Favor
			// 魔力之息 / Nature's Favor
			// 魔力之息 / Nature's Favor
			// Pursuit Stance
			// Pursuit Stance
			// Pursuit Stance
			// 灵敏度提升 / Aimbot Assist
			// 灵敏度提升 / Aimbot Assist
			// 灵敏度提升 / Aimbot Assist
			// 灵敏度提升 / Aimbot Assist
			// 灵敏度提升 / Aimbot Assist
			// 灵敏度提升 / Aimbot Assist
			// 灵敏度提升 / Aimbot Assist
			// 灵敏度提升 / Aimbot Assist
			// 魔力屏障 / Trauma Plate Trigger
			// 魔力屏障 / Trauma Plate Trigger
			// 魔力屏障 / Trauma Plate Trigger
			// 魔力屏障 / Trauma Plate Trigger
			// 魔力屏障 / Trauma Plate Trigger
			// 魔力屏障 / Trauma Plate Trigger
			// 吸收反射膜 / Leeching Steel
			// 吸收反射膜 / Leeching Steel
			// 吸收反射膜 / Leeching Steel
			// 吸收反射膜 / Leeching Steel
			// 吸收反射膜 / Leeching Steel
			// 吸收反射膜 / Leeching Steel
			// 吸收反射膜 / Leeching Steel
			// 吸收反射膜 / Leeching Steel
			// Nerve Pulse
			// Nerve Pulse
			// Nerve Pulse
			// Explosive Exhaust
			// Explosive Exhaust
			// Explosive Exhaust
			// Powerspike Trigger
			// Powerspike Trigger
			// Powerspike Trigger
			// Eternal Denial
			// Eternal Denial
			// Eternal Denial
			// Shield of Vengeance
			// 阻断之甲 / Aether Armor
			// 阻断之甲 / Aether Armor
			// 阻断之甲 / Aether Armor
			// 阻断之甲 / Aether Armor
			// 阻断之甲 / Aether Armor
			// 束缚波 / Punishing Wave
			// 束缚波 / Punishing Wave
			// 束缚波 / Punishing Wave
			// 束缚波 / Punishing Wave
			// 束缚波 / Punishing Wave
			// 束缚波 / Punishing Wave
			// 坚固的盾牌 I / Shield Of Faith
			// 激昂 I / Divine Fury
			// 起死回生 / Prayer Of Resilience
			// 起死回生 / Prayer Of Resilience
			// 起死回生 / Prayer Of Resilience
			// 起死回生 / Prayer Of Resilience
			// 起死回生 / Prayer Of Resilience
			// 起死回生 / Prayer Of Resilience
			// Shimmerbomb
			// Shimmerbomb
			// Shimmerbomb
			// 六感最大化 / Sensory Boost
			// 涂毒 / Apply Lethal Venom
			// 涂毒 / Apply Lethal Venom
			// 涂毒 / Apply Lethal Venom
			// 涂毒 / Apply Lethal Venom
			// 涂毒 / Apply Lethal Venom
			// 涂毒 / Apply Lethal Venom
			// 影子步行 I / Shadow Walk
			// 奇袭斩 / Dash And Slash
			// 奇袭斩 / Dash And Slash
			// 奇袭斩 / Dash And Slash
			// 奇袭斩 / Dash And Slash
			// 奇袭斩 / Dash And Slash
			// 奇袭斩 / Dash And Slash
			// 命中之契约 I / Oath Of Accuracy
			// Spirit's Empowerment
			// Spirit's Empowerment
			// Spirit's Empowerment
			// 隐身之光辉 I / Cloaking Word
			// 黄泉之诅咒 / Infernal Blight
			// 黄泉之诅咒 / Infernal Blight
			// 黄泉之诅咒 / Infernal Blight
			// 命令:毁灭 I / Spirit Burn-to-Ashes
			// Blood Funnel
			// Blood Funnel
			// Blood Funnel
			// Restoration Relief
			// Restoration Relief
			// Restoration Relief
			// 痛苦连锁 / Chain Of Suffering
			// 痛苦连锁 / Chain Of Suffering
			// 痛苦连锁 / Chain Of Suffering
			// 痛苦连锁 / Chain Of Suffering
			// 痛苦连锁 / Chain Of Suffering
			// 痛苦连锁 / Chain Of Suffering
			// 恢复之佑护I / Noble Grace
			// 恢复之佑护I / Noble Grace
			// 恢复之佑护I / Noble Grace
			// 恢复之佑护I / Noble Grace
			// 恢复之佑护I / Noble Grace
			case 657, 658, 659, 660, 661, 662, 1009, 1057, 1305, 1306, 1307, 1308, 1309, 1310, 1311, 1312, 1313, 1314,
			     1315, 1316, 1317, 1318, 1319, 1320, 1321, 1322, 1323, 1339, 1402, 1460, 1461, 1462, 1463, 1464, 1465,
			     1466, 1467, 1468, 1469, 1470, 1471, 1472, 1473, 1540, 1541, 1542, 1550, 1551, 1552, 1553, 1554, 1555,
			     1607, 1608, 1609, 1610, 1611, 1612, 1613, 1651, 1652, 1653, 1654, 1655, 1656, 1832, 1833, 1834, 1904,
			     1905, 1906, 2033, 2034, 2035, 2036, 2037, 2038, 2039, 2040, 2368, 2369, 2370, 2383, 2384, 2385, 2386,
			     2387, 2388, 2389, 2390, 2458, 2459, 2460, 2461, 2462, 2463, 2825, 2826, 2827, 2828, 2829, 2830, 2831,
			     2832, 2849, 2850, 2851, 2852, 2854, 2858, 2861, 2862, 2863, 2915, 2916, 2917, 2918, 2934, 2935, 2936,
			     2937, 2938, 2968, 2969, 2970, 2971, 2972, 2973, 2974, 3035, 3155, 3156, 3157, 3158, 3159, 3160, 3236,
			     3237, 3238, 3319, 3321, 3322, 3323, 3324, 3325, 3326, 3329, 3332, 3333, 3334, 3335, 3336, 3337, 3480,
			     3541, 3542, 3543, 3544, 3545, 3546, 3547, 3836, 3849, 3850, 3851, 3932, 3933, 3934, 4144, 4145, 4146,
			     4147, 4148, 4149, 4188, 4189, 4190, 4191, 4192, 4614 -> // 六感最大化 / Sensory Boost
				cooldown - 36 * SkillLevel; // 威胁的咆哮 / Howl
			// 威胁的咆哮 / Howl
			// 威胁的咆哮 / Howl
			// 威胁的咆哮 / Howl
			// 威胁的咆哮 / Howl
			// 威胁的咆哮 / Howl
			// 威胁的咆哮 / Howl
			// 威胁的咆哮 / Howl
			// Night Haze
			// Night Haze
			// Night Haze
			// 闪电陷阱 / Staggering Trap
			// 闪电陷阱 / Staggering Trap
			// 闪电陷阱 / Staggering Trap
			// 闪电陷阱 / Staggering Trap
			// 闪电陷阱 / Staggering Trap
			// 闪电陷阱 / Staggering Trap
			// 活力交换 I / Exchange Vitality
			// 衰弱之诅咒 / Curse Of Weakness
			// 衰弱之诅咒 / Curse Of Weakness
			// 衰弱之诅咒 / Curse Of Weakness
			// 衰弱之诅咒 / Curse Of Weakness
			// 衰弱之诅咒 / Curse Of Weakness
			// 衰弱之诅咒 / Curse Of Weakness
			// 衰弱之诅咒 / Curse Of Weakness
			// 衰弱之诅咒 / Curse Of Weakness
			// Slumberswept Wind
			// Slumberswept Wind
			// Slumberswept Wind
			// Repulsion Field
			// Repulsion Field
			// Repulsion Field
			// 伊德保护膜 / Kinetic Bulwark
			// 伊德保护膜 / Kinetic Bulwark
			// 伊德保护膜 / Kinetic Bulwark
			// 庇护之盔甲 / Prayer of Victory
			// 庇护之盔甲 / Prayer of Victory
			// 庇护之盔甲 / Prayer of Victory
			// 庇护之盔甲 / Prayer of Victory
			// 庇护之盔甲 / Prayer of Victory
			// 庇护之盔甲 / Prayer of Victory
			// 觉悟 I / Deadly Abandon
			// Command: Absorb Wounds
			// Summon Vexing Energy
			// Summon Vexing Energy
			// Summon Vexing Energy
			// Summon Vexing Energy
			// Summon Vexing Energy
			case 683, 684, 685, 686, 687, 688, 689, 690, 936, 937, 938, 1060, 1061, 1062, 1063, 1064, 1065, 1327, 1329,
			     1330, 1331, 1332, 1333, 1334, 1335, 1336, 1340, 1341, 1342, 1418, 1419, 1420, 2579, 2580, 2581, 2926,
			     2927, 2928, 2929, 2930, 2931, 3320, 3549, 3906, 3907, 3908, 3909, 3910, 3911 -> // Summon Vexing Energy
				cooldown - 60 * SkillLevel; // 主神的保护 I / Empyrean Providence
			case 2922, 3904 -> // 必死的交换 I / Reverse Condition
				cooldown - 120 * SkillLevel;
			// 高阶守护者变身 5.1【天族】 / ArchDaeva Transformation 5.1 [Elyos]
			// Transformation: Avatar Of Fire.
			// Transformation: Avatar Of Water.
			// Transformation: Avatar Of Earth.
			// Transformation: Avatar Of Wind.
			// 高阶守护者变身 5.1【魔族】 / ArchDaeva Transformation 5.1 [Asmodians]
			// Transformation: Avatar Of Fire.
			// Transformation: Avatar Of Water.
			// Transformation: Avatar Of Earth.
			case 4752, 4757, 4762, 4768, 4804, 4805, 4806, 4807 -> // Transformation: Avatar Of Wind.
				cooldown - 2500 * SkillLevel;
			default -> cooldown;
		};
	}
}

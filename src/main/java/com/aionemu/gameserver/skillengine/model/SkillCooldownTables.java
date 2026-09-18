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
		switch (skill.getSkillId()) {
		case 564: // 气魄 / Dauntless Spirit
		case 565: // 气魄 / Dauntless Spirit
		case 566: // 气魄 / Dauntless Spirit
		case 567: // 气魄 / Dauntless Spirit
		case 568: // 气魄 / Dauntless Spirit
		case 569: // 气魄 / Dauntless Spirit
		case 570: // 气魄 / Dauntless Spirit
		case 571: // 气魄 / Dauntless Spirit
		case 727: // Wind Lance
		case 728: // Wind Lance
		case 729: // Wind Lance
		case 730: // Wind Lance
		case 731: // Wind Lance
		case 732: // Wind Lance
		case 755: // 回旋一击 / Whirling Strike
		case 756: // 回旋一击 / Whirling Strike
		case 757: // 回旋一击 / Whirling Strike
		case 1100: // 透视陷阱 / Trap Of Clairvoyance
		case 1101: // 透视陷阱 / Trap Of Clairvoyance
		case 1324: // 冰河重击 / Glacial Shard
		case 1325: // 冰河重击 / Glacial Shard
		case 1326: // 冰河重击 / Glacial Shard
		case 1640: // 灭火 / Annihilation
		case 1641: // 灭火 / Annihilation
		case 1642: // 灭火 / Annihilation
		case 1643: // 灭火 / Annihilation
		case 1644: // 灭火 / Annihilation
		case 1645: // 灭火 / Annihilation
		case 1646: // 灭火 / Annihilation
		case 1647: // 灭火 / Annihilation
		case 1727: // 生命之咒语 / Word Of Life
		case 1728: // 生命之咒语 / Word Of Life
		case 1729: // 生命之咒语 / Word Of Life
		case 1730: // 生命之咒语 / Word Of Life
		case 1731: // 生命之咒语 / Word Of Life
		case 1732: // 生命之咒语 / Word Of Life
		case 1733: // 生命之咒语 / Word Of Life
		case 1734: // 生命之咒语 / Word Of Life
		case 1863: // 波动攻击 / Disorienting Blow
		case 1864: // 波动攻击 / Disorienting Blow
		case 1865: // 波动攻击 / Disorienting Blow
		case 1866: // 波动攻击 / Disorienting Blow
		case 1867: // 波动攻击 / Disorienting Blow
		case 1868: // 波动攻击 / Disorienting Blow
		case 1883: // 爆裂 / Burst
		case 1884: // 爆裂 / Burst
		case 1885: // 爆裂 / Burst
		case 1886: // 爆裂 / Burst
		case 1887: // 爆裂 / Burst
		case 1888: // 爆裂 / Burst
		case 1889: // 爆裂 / Burst
		case 1890: // 爆裂 / Burst
		case 1907: // Word of Instigation
		case 1908: // Word of Instigation
		case 1909: // Word of Instigation
		case 2046: // 魔力之恩惠 I / Stopping Power
		case 2054: // 填装魔力弹 I / Autoload
		case 2268: // 必中魔眼 / Sighting
		case 2269: // 必中魔眼 / Sighting
		case 2270: // 必中魔眼 / Sighting
		case 2271: // 必中魔眼 / Sighting
		case 2272: // 必中魔眼 / Sighting
		case 2273: // 必中魔眼 / Sighting
		case 2391: // 盔甲破坏 / Drillbore
		case 2392: // 盔甲破坏 / Drillbore
		case 2393: // 盔甲破坏 / Drillbore
		case 2394: // 盔甲破坏 / Drillbore
		case 2395: // 盔甲破坏 / Drillbore
		case 2396: // 盔甲破坏 / Drillbore
		case 2397: // 盔甲破坏 / Drillbore
		case 2398: // 盔甲破坏 / Drillbore
		case 2409: // 要害戳刺 / Debilitating Blade
		case 2410: // 要害戳刺 / Debilitating Blade
		case 2411: // 要害戳刺 / Debilitating Blade
		case 2412: // 要害戳刺 / Debilitating Blade
		case 2413: // 要害戳刺 / Debilitating Blade
		case 2414: // 要害戳刺 / Debilitating Blade
		case 2464: // Aether Recharge
		case 2467: // Aether Recharge
		case 2470: // Aether Recharge
		case 2473: // Aether Recharge
		case 2476: // Aether Recharge
		case 2479: // Aether Recharge
		case 2482: // Aether Recharge
		case 2485: // Aether Recharge
		case 2711: // 电场束缚 / Convulsion Beam
		case 2712: // 电场束缚 / Convulsion Beam
		case 2713: // 电场束缚 / Convulsion Beam
		case 2714: // 电场束缚 / Convulsion Beam
		case 2715: // 电场束缚 / Convulsion Beam
		case 2716: // 电场束缚 / Convulsion Beam
		case 2919: // Invigorating Strike
		case 2920: // Invigorating Strike
		case 2921: // Invigorating Strike
		case 2945: // 愤怒诱发 / Incite Rage
		case 2946: // 愤怒诱发 / Incite Rage
		case 2947: // 愤怒诱发 / Incite Rage
		case 2948: // 愤怒诱发 / Incite Rage
		case 2949: // 愤怒诱发 / Incite Rage
		case 2950: // 愤怒诱发 / Incite Rage
		case 2951: // 愤怒诱发 / Incite Rage
		case 2952: // 愤怒诱发 / Incite Rage
		case 2961: // 保护之盾 / Holy Shield
		case 2962: // 保护之盾 / Holy Shield
		case 2963: // 保护之盾 / Holy Shield
		case 2964: // 保护之盾 / Holy Shield
		case 2965: // 保护之盾 / Holy Shield
		case 2966: // 保护之盾 / Holy Shield
		case 3147: // 处决一击 / Punishing Thrust
		case 3148: // 处决一击 / Punishing Thrust
		case 3149: // 处决一击 / Punishing Thrust
		case 3150: // 处决一击 / Punishing Thrust
		case 3151: // 处决一击 / Punishing Thrust
		case 3152: // 处决一击 / Punishing Thrust
		case 3153: // 处决一击 / Punishing Thrust
		case 3154: // 处决一击 / Punishing Thrust
		case 3242: // Explosive Rebranding
		case 3243: // Explosive Rebranding
		case 3244: // Explosive Rebranding
		case 3246: // 昏厥之刃 / Quickening Doom
		case 3247: // 昏厥之刃 / Quickening Doom
		case 3248: // 昏厥之刃 / Quickening Doom
		case 3312: // 愤怒之眼 I / Eye Of Wrath
		case 3330: // 影子下坠 / Shadowfall
		case 3731: // 魔力诅咒 I / Magic's Freedom
		case 3796: // 精灵强化:强化甲胄 I / Armor Spirit
		case 3980: // 召唤:治愈之气息 / Summon Healing Servant
		case 3981: // 召唤:治愈之气息 / Summon Healing Servant
		case 3982: // 召唤:治愈之气息 / Summon Healing Servant
		case 3983: // 召唤:治愈之气息 / Summon Healing Servant
		case 3984: // 召唤:治愈之气息 / Summon Healing Servant
		case 3985: // 召唤:治愈之气息 / Summon Healing Servant
		case 3986: // 召唤:治愈之气息 / Summon Healing Servant
		case 3987: // 召唤:治愈之气息 / Summon Healing Servant
		case 3988: // 召唤:治愈之气息 / Summon Healing Servant
		case 3989: // 召唤:治愈之气息 / Summon Healing Servant
		case 3990: // 召唤:治愈之气息 / Summon Healing Servant
		case 3991: // 召唤:治愈之气息 / Summon Healing Servant
		case 3998: // 再生之光辉 / Splendor Of Rebirth
		case 3999: // 再生之光辉 / Splendor Of Rebirth
		case 4000: // 再生之光辉 / Splendor Of Rebirth
		case 4001: // 再生之光辉 / Splendor Of Rebirth
		case 4002: // 再生之光辉 / Splendor Of Rebirth
		case 4003: // 再生之光辉 / Splendor Of Rebirth
		case 4134: // 恢复阻断 I / Festering Wound
		case 4164: // 霹雳 / Call Lightning
		case 4165: // 霹雳 / Call Lightning
		case 4166: // 霹雳 / Call Lightning
		case 4384: // 平稳变奏曲 / Resonant Hymn
		case 4385: // 平稳变奏曲 / Resonant Hymn
		case 4386: // 平稳变奏曲 / Resonant Hymn
		case 4387: // 平稳变奏曲 / Resonant Hymn
		case 4388: // 平稳变奏曲 / Resonant Hymn
		case 4389: // 平稳变奏曲 / Resonant Hymn
		case 4390: // 平稳变奏曲 / Resonant Hymn
		case 4474: // Blazing Requiem
		case 4477: // Blazing Requiem
		case 4480: // Blazing Requiem
		case 4484: // 免罪旋律 / Chorus Of Blessing
		case 4485: // 免罪旋律 / Chorus Of Blessing
		case 4486: // 免罪旋律 / Chorus Of Blessing
		case 4487: // 不和谐音 / Treble Cleave
		case 4488: // 不和谐音 / Treble Cleave
		case 4489: // 不和谐音 / Treble Cleave
		case 4491: // Mvt.2: Summer
		case 4492: // Mvt.2: Summer
		case 4493: // Mvt.2: Summer
		case 4494: // Mvt.2: Summer
		case 4495: // Mvt.2: Summer
		case 4496: // Mvt.2: Summer
		case 4497: // Mvt.3: Autumn
		case 4498: // Mvt.3: Autumn
		case 4499: // Mvt.3: Autumn
		case 4500: // Mvt.3: Autumn
		case 4501: // Mvt.3: Autumn
		case 4502: // Mvt.3: Autumn
		case 4524: // 莫斯奇狂想曲 / Paean Of Pain
		case 4525: // 莫斯奇狂想曲 / Paean Of Pain
		case 4526: // 莫斯奇狂想曲 / Paean Of Pain
		case 4527: // 莫斯奇狂想曲 / Paean Of Pain
		case 4528: // 莫斯奇狂想曲 / Paean Of Pain
		case 4529: // 莫斯奇狂想曲 / Paean Of Pain
		case 4572: // Combustible Cacophony
		case 4573: // Combustible Cacophony
		case 4574: // Combustible Cacophony
		case 4575: // Combustible Cacophony
		case 4576: // Combustible Cacophony
		case 4577: // Combustible Cacophony
		case 4578: // Combustible Cacophony
		case 4579: // Combustible Cacophony
		case 4591: // 影子下坠 / Shadowfall
		case 4592: // 影子下坠 / Shadowfall
		case 4593: // 影子下坠 / Shadowfall
		case 4594: // 影子下坠 / Shadowfall
		case 4595: // 影子下坠 / Shadowfall
		case 4596: // 影子下坠 / Shadowfall
			return cooldown - 6 * SkillLevel;
		case 600: // 魔法防御 / Magical Defense
		case 641: // Unraveling Assault
		case 642: // Unraveling Assault
		case 643: // Unraveling Assault
		case 1351: // 岩石召唤 / Summon Rock
		case 1352: // 岩石召唤 / Summon Rock
		case 1353: // 岩石召唤 / Summon Rock
		case 1354: // 岩石召唤 / Summon Rock
		case 1355: // 岩石召唤 / Summon Rock
		case 1356: // 岩石召唤 / Summon Rock
		case 1801: // 疾行激励 / Acceleration Cheer
		case 1802: // 疾行激励 / Acceleration Cheer
		case 1803: // 疾行激励 / Acceleration Cheer
		case 1804: // 疾行激励 / Acceleration Cheer
		case 1805: // 疾行激励 / Acceleration Cheer
		case 1806: // 疾行激励 / Acceleration Cheer
		case 1807: // 疾行激励 / Acceleration Cheer
		case 1808: // 疾行激励 / Acceleration Cheer
		case 2750: // 功率最大化 / Aethercharged Steel
		case 2751: // 功率最大化 / Aethercharged Steel
		case 2752: // 功率最大化 / Aethercharged Steel
		case 2753: // 功率最大化 / Aethercharged Steel
		case 2754: // 功率最大化 / Aethercharged Steel
		case 2755: // 功率最大化 / Aethercharged Steel
		case 3590: // 精灵强化:治愈 I / Healing Spirit
		case 3903: // 全力疾行 I / Power Sprint
		case 4182: // 弱化之印 / Enfeebling Burst
		case 4183: // 弱化之印 / Enfeebling Burst
		case 4184: // 弱化之印 / Enfeebling Burst
		case 4185: // 弱化之印 / Enfeebling Burst
		case 4186: // 弱化之印 / Enfeebling Burst
		case 4187: // 弱化之印 / Enfeebling Burst
			return cooldown - 9 * SkillLevel;
		case 539: // 枯竭波 / Exhausting Wave
		case 540: // 枯竭波 / Exhausting Wave
		case 541: // 枯竭波 / Exhausting Wave
		case 542: // 枯竭波 / Exhausting Wave
		case 543: // 枯竭波 / Exhausting Wave
		case 544: // 枯竭波 / Exhausting Wave
		case 612: // 斩脚 / Tendon Slice
		case 613: // 斩脚 / Tendon Slice
		case 614: // 斩脚 / Tendon Slice
		case 615: // 斩脚 / Tendon Slice
		case 616: // 斩脚 / Tendon Slice
		case 617: // 斩脚 / Tendon Slice
		case 618: // 抓脚 I / Ankle Snare
		case 698: // 地震波动 / Earthquake Wave
		case 699: // 地震波动 / Earthquake Wave
		case 700: // 地震波动 / Earthquake Wave
		case 701: // 地震波动 / Earthquake Wave
		case 702: // 地震波动 / Earthquake Wave
		case 703: // 地震波动 / Earthquake Wave
		case 704: // 地震波动 / Earthquake Wave
		case 705: // 地震波动 / Earthquake Wave
		case 749: // 重生波 / Revival Wave
		case 750: // 重生波 / Revival Wave
		case 751: // 重生波 / Revival Wave
		case 752: // 重生波 / Revival Wave
		case 753: // 重生波 / Revival Wave
		case 754: // 重生波 / Revival Wave
		case 849: // 减速陷阱 / Trap Of Slowing
		case 850: // 减速陷阱 / Trap Of Slowing
		case 851: // 减速陷阱 / Trap Of Slowing
		case 852: // 减速陷阱 / Trap Of Slowing
		case 853: // 减速陷阱 / Trap Of Slowing
		case 854: // 减速陷阱 / Trap Of Slowing
		case 855: // 减速陷阱 / Trap Of Slowing
		case 856: // 减速陷阱 / Trap Of Slowing
		case 857: // 减速陷阱 / Trap Of Slowing
		case 858: // 减速陷阱 / Trap Of Slowing
		case 859: // 减速陷阱 / Trap Of Slowing
		case 860: // 减速陷阱 / Trap Of Slowing
		case 861: // 减速陷阱 / Trap Of Slowing
		case 862: // 减速陷阱 / Trap Of Slowing
		case 863: // 减速陷阱 / Trap Of Slowing
		case 864: // 减速陷阱 / Trap Of Slowing
		case 888: // 猎人的决心 I / Hunter's Might
		case 962: // 缚天陷阱 / Skybound Trap
		case 963: // 缚天陷阱 / Skybound Trap
		case 964: // 缚天陷阱 / Skybound Trap
		case 965: // 缚天陷阱 / Skybound Trap
		case 966: // 缚天陷阱 / Skybound Trap
		case 967: // 缚天陷阱 / Skybound Trap
		case 968: // 缚天陷阱 / Skybound Trap
		case 969: // 缚天陷阱 / Skybound Trap
		case 970: // 缚天陷阱 / Skybound Trap
		case 971: // 缚天陷阱 / Skybound Trap
		case 972: // 缚天陷阱 / Skybound Trap
		case 973: // 缚天陷阱 / Skybound Trap
		case 974: // 缚天陷阱 / Skybound Trap
		case 975: // 缚天陷阱 / Skybound Trap
		case 976: // 缚天陷阱 / Skybound Trap
		case 977: // 缚天陷阱 / Skybound Trap
		case 1006: // Ripthread Shot
		case 1007: // Ripthread Shot
		case 1008: // Ripthread Shot
		case 1486: // 暴风重击 / Storm Strike
		case 1487: // 暴风重击 / Storm Strike
		case 1488: // 暴风重击 / Storm Strike
		case 1489: // 暴风重击 / Storm Strike
		case 1490: // 暴风重击 / Storm Strike
		case 1491: // 暴风重击 / Storm Strike
		case 1492: // 暴风重击 / Storm Strike
		case 1493: // 暴风重击 / Storm Strike
		case 1901: // Resonant Strike
		case 1902: // Resonant Strike
		case 1903: // Resonant Strike
		case 2109: // 束缚炮 / Paralysis Cannon
		case 2110: // 束缚炮 / Paralysis Cannon
		case 2111: // 束缚炮 / Paralysis Cannon
		case 2112: // 束缚炮 / Paralysis Cannon
		case 2113: // 束缚炮 / Paralysis Cannon
		case 2114: // 束缚炮 / Paralysis Cannon
		case 2274: // 灵魂炮 / Missile Guide
		case 2277: // 灵魂炮 / Missile Guide
		case 2280: // 灵魂炮 / Missile Guide
		case 2283: // 灵魂炮 / Missile Guide
		case 2286: // 灵魂炮 / Missile Guide
		case 2289: // 灵魂炮 / Missile Guide
		case 2292: // 灵魂炮 / Missile Guide
		case 2295: // 灵魂炮 / Missile Guide
		case 2371: // Sequential Fire
		case 2374: // Sequential Fire
		case 2377: // Sequential Fire
		case 2380: // Pulverizer Cannon
		case 2381: // Pulverizer Cannon
		case 2382: // Pulverizer Cannon
		case 2450: // 魔力凝聚 / Life Support Trigger
		case 2451: // 魔力凝聚 / Life Support Trigger
		case 2452: // 魔力凝聚 / Life Support Trigger
		case 2453: // 魔力凝聚 / Life Support Trigger
		case 2454: // 魔力凝聚 / Life Support Trigger
		case 2455: // 魔力凝聚 / Life Support Trigger
		case 2456: // 魔力凝聚 / Life Support Trigger
		case 2457: // 魔力凝聚 / Life Support Trigger
		case 2939: // 精神破坏 / Divine Justice
		case 2940: // 精神破坏 / Divine Justice
		case 2941: // 精神破坏 / Divine Justice
		case 2942: // 精神破坏 / Divine Justice
		case 2943: // 精神破坏 / Divine Justice
		case 2944: // 精神破坏 / Divine Justice
		case 3239: // Fangdrop Stab
		case 3240: // Fangdrop Stab
		case 3241: // Fangdrop Stab
		case 3245: // Scoundrel's Bond
		case 3255: // 雾砂攻击 / Venomous Strike
		case 3256: // 雾砂攻击 / Venomous Strike
		case 3257: // 雾砂攻击 / Venomous Strike
		case 3258: // 雾砂攻击 / Venomous Strike
		case 3259: // 雾砂攻击 / Venomous Strike
		case 3260: // 雾砂攻击 / Venomous Strike
		case 3261: // 雾砂攻击 / Venomous Strike
		case 3327: // 逃跑姿态 I / Break Away
		case 3531: // 命令:守护之墙 I / Spirit Wall Of Protection
		case 3562: // 大地之守护 / Earthen Call
		case 3563: // 大地之守护 / Earthen Call
		case 3564: // 大地之守护 / Earthen Call
		case 3565: // 大地之守护 / Earthen Call
		case 3566: // 大地之守护 / Earthen Call
		case 3567: // 大地之守护 / Earthen Call
		case 3568: // 大地之守护 / Earthen Call
		case 3569: // 大地之守护 / Earthen Call
		case 3575: // 黑暗之诅咒 / Withering Gloom
		case 3576: // 黑暗之诅咒 / Withering Gloom
		case 3577: // 黑暗之诅咒 / Withering Gloom
		case 3578: // 黑暗之诅咒 / Withering Gloom
		case 3579: // 黑暗之诅咒 / Withering Gloom
		case 3580: // 黑暗之诅咒 / Withering Gloom
		case 3581: // 黑暗之诅咒 / Withering Gloom
		case 3924: // 拯救之手 / Saving Grace
		case 3925: // 拯救之手 / Saving Grace
		case 3926: // 拯救之手 / Saving Grace
		case 3927: // 拯救之手 / Saving Grace
		case 3928: // 拯救之手 / Saving Grace
		case 3929: // 拯救之手 / Saving Grace
		case 3930: // 拯救之手 / Saving Grace
		case 3931: // 拯救之手 / Saving Grace
		case 3992: // 净化之水 / Ripple Of Purification
		case 3993: // 净化之水 / Ripple Of Purification
		case 3994: // 净化之水 / Ripple Of Purification
		case 3995: // 净化之水 / Ripple Of Purification
		case 3996: // 净化之水 / Ripple Of Purification
		case 3997: // 净化之水 / Ripple Of Purification
		case 4135: // 闪光 I / Blinding Light
		case 4368: // 吸收之咒语 / Healing Conduit
		case 4490: // 麻痹回声 I / Staggered Rest
		case 4631: // 吸收之咒语 / Healing Conduit
		case 4632: // 吸收之咒语 / Healing Conduit
		case 4633: // 吸收之咒语 / Healing Conduit
		case 4634: // 吸收之咒语 / Healing Conduit
		case 4635: // 吸收之咒语 / Healing Conduit
		case 4636: // 吸收之咒语 / Healing Conduit
		case 4637: // 吸收之咒语 / Healing Conduit
		case 4638: // 吸收之咒语 / Healing Conduit
			return cooldown - 24 * SkillLevel;
		case 657: // Battle Banner
		case 658: // Battle Banner
		case 659: // Battle Banner
		case 660: // Battle Banner
		case 661: // Battle Banner
		case 662: // Battle Banner
		case 1009: // 抵抗的决心 I / Nature's Resolve
		case 1057: // 祝福之弓 I / Bow Of Blessing
		case 1305: // 冰雪甲胄 / Wintry Armor
		case 1306: // 冰雪甲胄 / Wintry Armor
		case 1307: // 冰雪甲胄 / Wintry Armor
		case 1308: // 冰面 / Ice Sheet
		case 1309: // 冰面 / Ice Sheet
		case 1310: // 冰面 / Ice Sheet
		case 1311: // 冰面 / Ice Sheet
		case 1312: // 冰面 / Ice Sheet
		case 1313: // 冰面 / Ice Sheet
		case 1314: // 冰面 / Ice Sheet
		case 1315: // 冰面 / Ice Sheet
		case 1316: // 冰面 / Ice Sheet
		case 1317: // 冰面 / Ice Sheet
		case 1318: // 冰面 / Ice Sheet
		case 1319: // 冰面 / Ice Sheet
		case 1320: // 冰面 / Ice Sheet
		case 1321: // 冰面 / Ice Sheet
		case 1322: // 冰面 / Ice Sheet
		case 1323: // 冰面 / Ice Sheet
		case 1339: // 睡眠暴风 I / Sleeping Storm
		case 1402: // 元素结界 I / Elemental Ward
		case 1460: // 召唤台风 / Manifest Tornado
		case 1461: // 召唤台风 / Manifest Tornado
		case 1462: // 召唤台风 / Manifest Tornado
		case 1463: // 召唤台风 / Manifest Tornado
		case 1464: // 召唤台风 / Manifest Tornado
		case 1465: // 召唤台风 / Manifest Tornado
		case 1466: // 召唤台风 / Manifest Tornado
		case 1467: // 召唤台风 / Manifest Tornado
		case 1468: // 召唤台风 / Manifest Tornado
		case 1469: // 召唤台风 / Manifest Tornado
		case 1470: // 召唤台风 / Manifest Tornado
		case 1471: // 召唤台风 / Manifest Tornado
		case 1472: // 召唤台风 / Manifest Tornado
		case 1473: // 召唤台风 / Manifest Tornado
		case 1540: // Aetherblaze
		case 1541: // Aetherblaze
		case 1542: // Aetherblaze
		case 1550: // 幻影漩涡 / Illusion Storm
		case 1551: // 幻影漩涡 / Illusion Storm
		case 1552: // 幻影漩涡 / Illusion Storm
		case 1553: // 幻影漩涡 / Illusion Storm
		case 1554: // 幻影漩涡 / Illusion Storm
		case 1555: // 幻影漩涡 / Illusion Storm
		case 1607: // 气概 / Rise
		case 1608: // 气概 / Rise
		case 1609: // 气概 / Rise
		case 1610: // 气概 / Rise
		case 1611: // 气概 / Rise
		case 1612: // 气概 / Rise
		case 1613: // 气概 / Rise
		case 1651: // 风之祝福 / Blessing Of Wind
		case 1652: // 风之祝福 / Blessing Of Wind
		case 1653: // 风之祝福 / Blessing Of Wind
		case 1654: // 风之祝福 / Blessing Of Wind
		case 1655: // 风之祝福 / Blessing Of Wind
		case 1656: // 风之祝福 / Blessing Of Wind
		case 1832: // 铁壁之咒语 / Elemental Screen
		case 1833: // 铁壁之咒语 / Elemental Screen
		case 1834: // 铁壁之咒语 / Elemental Screen
		case 1904: // Debilitating Incantation
		case 1905: // Debilitating Incantation
		case 1906: // Debilitating Incantation
		case 2033: // 魔力之息 / Nature's Favor
		case 2034: // 魔力之息 / Nature's Favor
		case 2035: // 魔力之息 / Nature's Favor
		case 2036: // 魔力之息 / Nature's Favor
		case 2037: // 魔力之息 / Nature's Favor
		case 2038: // 魔力之息 / Nature's Favor
		case 2039: // 魔力之息 / Nature's Favor
		case 2040: // 魔力之息 / Nature's Favor
		case 2368: // Pursuit Stance
		case 2369: // Pursuit Stance
		case 2370: // Pursuit Stance
		case 2383: // 灵敏度提升 / Aimbot Assist
		case 2384: // 灵敏度提升 / Aimbot Assist
		case 2385: // 灵敏度提升 / Aimbot Assist
		case 2386: // 灵敏度提升 / Aimbot Assist
		case 2387: // 灵敏度提升 / Aimbot Assist
		case 2388: // 灵敏度提升 / Aimbot Assist
		case 2389: // 灵敏度提升 / Aimbot Assist
		case 2390: // 灵敏度提升 / Aimbot Assist
		case 2458: // 魔力屏障 / Trauma Plate Trigger
		case 2459: // 魔力屏障 / Trauma Plate Trigger
		case 2460: // 魔力屏障 / Trauma Plate Trigger
		case 2461: // 魔力屏障 / Trauma Plate Trigger
		case 2462: // 魔力屏障 / Trauma Plate Trigger
		case 2463: // 魔力屏障 / Trauma Plate Trigger
		case 2825: // 吸收反射膜 / Leeching Steel
		case 2826: // 吸收反射膜 / Leeching Steel
		case 2827: // 吸收反射膜 / Leeching Steel
		case 2828: // 吸收反射膜 / Leeching Steel
		case 2829: // 吸收反射膜 / Leeching Steel
		case 2830: // 吸收反射膜 / Leeching Steel
		case 2831: // 吸收反射膜 / Leeching Steel
		case 2832: // 吸收反射膜 / Leeching Steel
		case 2849: // Nerve Pulse
		case 2850: // Nerve Pulse
		case 2851: // Nerve Pulse
		case 2852: // Explosive Exhaust
		case 2854: // Explosive Exhaust
		case 2858: // Explosive Exhaust
		case 2861: // Powerspike Trigger
		case 2862: // Powerspike Trigger
		case 2863: // Powerspike Trigger
		case 2915: // Eternal Denial
		case 2916: // Eternal Denial
		case 2917: // Eternal Denial
		case 2918: // Shield of Vengeance
		case 2934: // 阻断之甲 / Aether Armor
		case 2935: // 阻断之甲 / Aether Armor
		case 2936: // 阻断之甲 / Aether Armor
		case 2937: // 阻断之甲 / Aether Armor
		case 2938: // 阻断之甲 / Aether Armor
		case 2968: // 束缚波 / Punishing Wave
		case 2969: // 束缚波 / Punishing Wave
		case 2970: // 束缚波 / Punishing Wave
		case 2971: // 束缚波 / Punishing Wave
		case 2972: // 束缚波 / Punishing Wave
		case 2973: // 束缚波 / Punishing Wave
		case 2974: // 坚固的盾牌 I / Shield Of Faith
		case 3035: // 激昂 I / Divine Fury
		case 3155: // 起死回生 / Prayer Of Resilience
		case 3156: // 起死回生 / Prayer Of Resilience
		case 3157: // 起死回生 / Prayer Of Resilience
		case 3158: // 起死回生 / Prayer Of Resilience
		case 3159: // 起死回生 / Prayer Of Resilience
		case 3160: // 起死回生 / Prayer Of Resilience
		case 3236: // Shimmerbomb
		case 3237: // Shimmerbomb
		case 3238: // Shimmerbomb
		case 3319: // 六感最大化 / Sensory Boost
		case 3321: // 涂毒 / Apply Lethal Venom
		case 3322: // 涂毒 / Apply Lethal Venom
		case 3323: // 涂毒 / Apply Lethal Venom
		case 3324: // 涂毒 / Apply Lethal Venom
		case 3325: // 涂毒 / Apply Lethal Venom
		case 3326: // 涂毒 / Apply Lethal Venom
		case 3329: // 影子步行 I / Shadow Walk
		case 3332: // 奇袭斩 / Dash And Slash
		case 3333: // 奇袭斩 / Dash And Slash
		case 3334: // 奇袭斩 / Dash And Slash
		case 3335: // 奇袭斩 / Dash And Slash
		case 3336: // 奇袭斩 / Dash And Slash
		case 3337: // 奇袭斩 / Dash And Slash
		case 3480: // 命中之契约 I / Oath Of Accuracy
		case 3541: // Spirit's Empowerment
		case 3542: // Spirit's Empowerment
		case 3543: // Spirit's Empowerment
		case 3544: // 隐身之光辉 I / Cloaking Word
		case 3545: // 黄泉之诅咒 / Infernal Blight
		case 3546: // 黄泉之诅咒 / Infernal Blight
		case 3547: // 黄泉之诅咒 / Infernal Blight
		case 3836: // 命令:毁灭 I / Spirit Burn-to-Ashes
		case 3849: // Blood Funnel
		case 3850: // Blood Funnel
		case 3851: // Blood Funnel
		case 3932: // Restoration Relief
		case 3933: // Restoration Relief
		case 3934: // Restoration Relief
		case 4144: // 痛苦连锁 / Chain Of Suffering
		case 4145: // 痛苦连锁 / Chain Of Suffering
		case 4146: // 痛苦连锁 / Chain Of Suffering
		case 4147: // 痛苦连锁 / Chain Of Suffering
		case 4148: // 痛苦连锁 / Chain Of Suffering
		case 4149: // 痛苦连锁 / Chain Of Suffering
		case 4188: // 恢复之佑护I / Noble Grace
		case 4189: // 恢复之佑护I / Noble Grace
		case 4190: // 恢复之佑护I / Noble Grace
		case 4191: // 恢复之佑护I / Noble Grace
		case 4192: // 恢复之佑护I / Noble Grace
		case 4614: // 六感最大化 / Sensory Boost
			return cooldown - 36 * SkillLevel;
		case 683: // 威胁的咆哮 / Howl
		case 684: // 威胁的咆哮 / Howl
		case 685: // 威胁的咆哮 / Howl
		case 686: // 威胁的咆哮 / Howl
		case 687: // 威胁的咆哮 / Howl
		case 688: // 威胁的咆哮 / Howl
		case 689: // 威胁的咆哮 / Howl
		case 690: // 威胁的咆哮 / Howl
		case 936: // Night Haze
		case 937: // Night Haze
		case 938: // Night Haze
		case 1060: // 闪电陷阱 / Staggering Trap
		case 1061: // 闪电陷阱 / Staggering Trap
		case 1062: // 闪电陷阱 / Staggering Trap
		case 1063: // 闪电陷阱 / Staggering Trap
		case 1064: // 闪电陷阱 / Staggering Trap
		case 1065: // 闪电陷阱 / Staggering Trap
		case 1327: // 活力交换 I / Exchange Vitality
		case 1329: // 衰弱之诅咒 / Curse Of Weakness
		case 1330: // 衰弱之诅咒 / Curse Of Weakness
		case 1331: // 衰弱之诅咒 / Curse Of Weakness
		case 1332: // 衰弱之诅咒 / Curse Of Weakness
		case 1333: // 衰弱之诅咒 / Curse Of Weakness
		case 1334: // 衰弱之诅咒 / Curse Of Weakness
		case 1335: // 衰弱之诅咒 / Curse Of Weakness
		case 1336: // 衰弱之诅咒 / Curse Of Weakness
		case 1340: // Slumberswept Wind
		case 1341: // Slumberswept Wind
		case 1342: // Slumberswept Wind
		case 1418: // Repulsion Field
		case 1419: // Repulsion Field
		case 1420: // Repulsion Field
		case 2579: // 伊德保护膜 / Kinetic Bulwark
		case 2580: // 伊德保护膜 / Kinetic Bulwark
		case 2581: // 伊德保护膜 / Kinetic Bulwark
		case 2926: // 庇护之盔甲 / Prayer of Victory
		case 2927: // 庇护之盔甲 / Prayer of Victory
		case 2928: // 庇护之盔甲 / Prayer of Victory
		case 2929: // 庇护之盔甲 / Prayer of Victory
		case 2930: // 庇护之盔甲 / Prayer of Victory
		case 2931: // 庇护之盔甲 / Prayer of Victory
		case 3320: // 觉悟 I / Deadly Abandon
		case 3549: // Command: Absorb Wounds
		case 3906: // Summon Vexing Energy
		case 3907: // Summon Vexing Energy
		case 3908: // Summon Vexing Energy
		case 3909: // Summon Vexing Energy
		case 3910: // Summon Vexing Energy
		case 3911: // Summon Vexing Energy
			return cooldown - 60 * SkillLevel;
		case 2922: // 主神的保护 I / Empyrean Providence
		case 3904: // 必死的交换 I / Reverse Condition
			return cooldown - 120 * SkillLevel;
		// 高阶守护者变身 5.1【天族】 / ArchDaeva Transformation 5.1 [Elyos]
		case 4752: // Transformation: Avatar Of Fire.
		case 4757: // Transformation: Avatar Of Water.
		case 4762: // Transformation: Avatar Of Earth.
		case 4768: // Transformation: Avatar Of Wind.
			// 高阶守护者变身 5.1【魔族】 / ArchDaeva Transformation 5.1 [Asmodians]
		case 4804: // Transformation: Avatar Of Fire.
		case 4805: // Transformation: Avatar Of Water.
		case 4806: // Transformation: Avatar Of Earth.
		case 4807: // Transformation: Avatar Of Wind.
			return cooldown - 2500 * SkillLevel;
		}
		return cooldown;
	}
}

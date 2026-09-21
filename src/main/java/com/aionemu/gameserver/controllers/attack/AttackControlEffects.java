package com.aionemu.gameserver.controllers.attack;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.WeaponType;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 攻击暴击控制效果域：判定控制系技能表，并在物理暴击时按武器类型尝试施加踉跄/摔倒。
 * Attack critical control-effect domain: the control-skill table plus stagger/stumble attempts on
 * physical crits by weapon type.
 *
 * <p>该类型只服务 {@link AttackUtil}：全部为静态纯查表/规则函数，不持有状态、不创建对象；
 * 对外仍通过 {@link AttackUtil} 的原 public static 方法访问（门面签名不变）。
 * This type only serves {@link AttackUtil}: every function is a static pure table lookup or rule
 * without state or allocation. External callers keep using the original public static
 * {@link AttackUtil} facade methods with unchanged signatures.</p>
 */
final class AttackControlEffects {

	/**
	 * 仅静态规则，禁止实例化。
	 * Static rules only; not instantiable.
	 */
	private AttackControlEffects() {
	}

	/**
	 * 判断技能是否自带硬直类效果（暴击触发硬直时应跳过）。
	 * Returns whether the skill already applies stagger/stumble-like effects (skip crit procs).
	 *
	 * @param skillId 技能 ID / skill id
	 * @return 是否自带相关效果 / whether skill already applies the effect
	 * @author KorLightNing
	 */
	static boolean isSkillEffect(int skillId) {
		switch (skillId) {
		// 踉跄效果 / Stagger Effect
		case 1054: // Finishing Arrow I.
		case 1055: // Finishing Arrow II.
		case 1056: // Finishing Arrow III.
		case 1102: // Rupture Arrow I.
		case 1103: // Rupture Arrow II.
		case 1104: // Rupture Arrow III.
		case 1105: // Rupture Arrow IV.
		case 1106: // Rupture Arrow V.
		case 1107: // Rupture Arrow VI.
		case 1108: // Rupture Arrow VII.
		case 1109: // Rupture Arrow VIII.
		case 1110: // Rupture Arrow IX.
		case 1226: // Frozen Shock I.
		case 1227: // Frozen Shock II.
		case 1228: // Frozen Shock III.
		case 1229: // Frozen Shock IV.
		case 1230: // Frozen Shock V.
		case 1231: // Frozen Shock VI.
		case 1232: // Frozen Shock VII.
		case 1233: // Frozen Shock VIII.
		case 1234: // Frozen Shock IX.
		case 1235: // Frozen Shock X.
		case 1236: // Frozen Shock XI.
		case 1237: // Frozen Shock XII.
		case 1258: // Aetherflame I.
		case 4728: // [ArchDaeva] Aetherflame 5.1
		case 1826: // Tremor I.
		case 1827: // Tremor II.
		case 1828: // Tremor III.
		case 1829: // Tremor IV.
		case 1830: // Tremor V.
		case 1831: // Tremor VI.
		case 2055: // Trunk Shot I.
		case 2056: // Trunk Shot II.
		case 2057: // Trunk Shot III.
		case 2058: // Trunk Shot IV.
		case 2059: // Trunk Shot V.
		case 2060: // Trunk Shot VI.
		case 2061: // Trunk Shot VII.
		case 2062: // Trunk Shot VIII.
		case 2063: // Trunk Shot IX.
		case 2064: // Trunk Shot X.
		case 2065: // Trunk Shot XI.
		case 2232: // Shock & Awe I.
		case 2235: // Shock & Awe II.
		case 2238: // Shock & Awe III.
		case 2241: // Shock & Awe IV.
		case 2244: // Shock & Awe V.
		case 2247: // Shock & Awe VI.
		case 2250: // Shock & Awe VII.
		case 2253: // Shock & Awe VIII.
		case 3614: // Stone Shock I.
		case 3615: // Stone Shock II.
		case 3616: // Stone Shock III.
		case 3617: // Stone Shock IV.
		case 3618: // Stone Shock V.
		case 3619: // Stone Shock VI.
		case 3620: // Stone Shock VII.
		case 3621: // Stone Shock VIII.
		case 3622: // Stone Shock IX.
		case 3623: // Stone Shock X.
		case 3624: // Stone Shock XI.
		case 4396: // Chorus Of Fortitude I.
		case 4397: // Chorus Of Fortitude II.
		case 4398: // Chorus Of Fortitude III.
		case 4399: // Chorus Of Fortitude IV.
		case 4522: // Sonic Gust I.
		case 4523: // Sonic Gust II.
		case 4790: // [ArchDaeva] Sonic Gust 5.1
			// 绊倒效果 / Stumble Effect
		case 519: // Explosion Of Rage I.
		case 520: // Explosion Of Rage II.
		case 521: // Explosion Of Rage III.
		case 522: // 激怒爆炸 IV 效果 / Explosion Of Rage IV.
		case 523: // Explosion Of Rage V.
		case 524: // Explosion Of Rage VI.
		case 525: // Explosion Of Rage VII.
		case 526: // Explosion Of Rage VIII.
		case 527: // Explosion Of Rage IX.
		case 528: // Explosion Of Rage X.
		case 529: // Explosion Of Rage XI.
		case 530: // Explosion Of Rage XII.
		case 531: // Crushing Blow I.
		case 532: // Crushing Blow II.
		case 533: // Crushing Blow III.
		case 534: // Crushing Blow IV.
		case 535: // Crushing Blow V.
		case 536: // Crushing Blow VI.
		case 537: // Crushing Blow VII.
		case 538: // Crushing Blow VIII.
		case 555: // Seismic Billow I.
		case 556: // Seismic Billow II.
		case 557: // Seismic Billow III.
		case 558: // Seismic Billow IV.
		case 559: // Seismic Billow V.
		case 560: // Seismic Billow VI.
		case 561: // Seismic Billow VII.
		case 562: // Seismic Billow VIII.
		case 584: // Spite Strike I.
		case 585: // Spite Strike II.
		case 586: // Spite Strike III.
		case 587: // Spite Strike IV.
		case 588: // Spite Strike V.
		case 589: // Spite Strike VI.
		case 621: // Wrathful Explosion I.
		case 622: // Wrathful Explosion II.
		case 623: // Wrathful Explosion III.
		case 624: // Wrathful Strike I.
		case 625: // Wrathful Strike II.
		case 626: // Wrathful Strike III.
		case 627: // Wrathful Strike IV.
		case 628: // Wrathful Strike V.
		case 629: // Wrathful Strike VI.
		case 630: // Wrathful Strike VII.
		case 631: // Wrathful Strike VIII.
		case 632: // Wrathful Strike IX.
		case 633: // Wrathful Strike X.
		case 634: // Wrathful Strike XI.
		case 635: // Wrathful Wave I.
		case 636: // Wrathful Wave II.
		case 637: // Wrathful Wave III.
		case 638: // Wrathful Wave IV.
		case 639: // Wrathful Wave V.
		case 640: // Wrathful Wave VI.
		case 728: // Wind Lance I.
		case 729: // Wind Lance II.
		case 730: // Wind Lance III.
		case 731: // Wind Lance IV.
		case 732: // Wind Lance V.
		case 733: // Severe Precision Cut I.
		case 734: // Severe Precision Cut II.
		case 735: // Severe Precision Cut III.
		case 736: // Severe Precision Cut IV.
		case 737: // Severe Precision Cut V.
		case 738: // Severe Precision Cut VI.
		case 1863: // Disorienting Blow I.
		case 1864: // Disorienting Blow II.
		case 1865: // Disorienting Blow III.
		case 1866: // Disorienting Blow IV.
		case 1867: // Disorienting Blow V.
		case 1868: // Disorienting Blow VI.
		case 1875: // Pentacle Shock I.
		case 1876: // Pentacle Shock II.
		case 1877: // Pentacle Shock III.
		case 1878: // Pentacle Shock IV.
		case 1879: // Pentacle Shock V.
		case 1880: // Pentacle Shock VI.
		case 1881: // Pentacle Shock VII.
		case 1882: // Pentacle Shock VIII.
		case 1891: // Soul Crush I.
		case 1892: // Soul Crush II.
		case 1893: // Soul Crush III.
		case 1894: // Soul Crush IV.
		case 1895: // Soul Crush V.
		case 1896: // Soul Crush VI.
		case 1897: // Soul Crush VII.
		case 1898: // Soul Crush VIII.
		case 2399: // Beatdown I.
		case 2530: // Annihilation Barrage I.
		case 2531: // Annihilation Barrage II.
		case 2532: // Annihilation Barrage III.
		case 2533: // Annihilation Barrage IV.
		case 2534: // Annihilation Barrage V.
		case 2535: // Annihilation Barrage VI.
		case 2568: // Uppercut I.
		case 2569: // Uppercut II.
		case 2570: // Uppercut III.
		case 2571: // Uppercut IV.
		case 2606: // Kinetic Slam I.
		case 2609: // Kinetic Slam II.
		case 2612: // Kinetic Slam III.
		case 2615: // Kinetic Slam IV.
		case 2618: // Kinetic Slam V.
		case 2621: // Kinetic Slam VI.
		case 2624: // Kinetic Slam VII.
		case 2627: // Kinetic Slam VIII.
		case 2630: // Kinetic Slam IX.
		case 2633: // Kinetic Slam X.
		case 2336: // Kinetic Slam XI.
		case 2639: // Kinetic Slam XII.
		case 4797: // [ArchDaeva] Kinetic Slam 5.1
		case 4798: // [ArchDaeva] Kinetic Slam 5.1
		case 4799: // [ArchDaeva] Kinetic Slam 5.1
		case 2923: // Shieldburst I.
		case 2924: // Shieldburst II.
		case 2925: // Shieldburst III.
		case 3106: // Face Smash I.
		case 3107: // Face Smash II.
		case 3108: // Face Smash III.
		case 3109: // Face Smash IV.
		case 3110: // Face Smash V.
		case 3111: // Face Smash VI.
		case 3112: // Face Smash VII.
		case 3113: // Swinging Shield Counter I.
		case 3114: // Swinging Shield Counter II.
		case 3115: // Swinging Shield Counter III.
		case 3125: // Sword Storm I.
		case 3126: // Sword Storm II.
		case 3330: // Shadowfall I.
		case 4591: // Shadowfall II.
		case 4592: // Shadowfall III.
		case 4593: // Shadowfall IV.
		case 4594: // Shadowfall V.
		case 4595: // Shadowfall VI.
		case 4596: // Shadowfall VII.
			// 空中击飞效果 / Openaerial Effect
		case 545: // Aerial Lockdown I.
		case 546: // Aerial Lockdown II.
		case 547: // Aerial Lockdown III.
		case 548: // Aerial Lockdown IV.
		case 549: // Aerial Lockdown V.
		case 550: // Aerial Lockdown VI.
		case 551: // Aerial Lockdown VII.
		case 552: // Aerial Lockdown VIII.
		case 553: // Aerial Lockdown IX.
		case 1184: // Aether's Hold I.
		case 1185: // Aether's Hold II.
		case 1186: // Aether's Hold III.
		case 1187: // Aether's Hold IV.
		case 1188: // Aether's Hold V.
		case 1189: // Aether's Hold VI.
		case 1190: // Aether's Hold VII.
		case 1191: // Aether's Hold VIII.
		case 2109: // Paralysis Cannon I.
		case 2110: // Paralysis Cannon II.
		case 2111: // Paralysis Cannon III.
		case 2112: // Paralysis Cannon IV.
		case 2113: // Paralysis Cannon V.
		case 2114: // Paralysis Cannon VI.
		case 3406: // Binding Rune I.
		case 3407: // Binding Rune II.
		case 3408: // Binding Rune III.
		case 3409: // Binding Rune IV.
		case 3410: // Binding Rune V.
		case 3411: // Binding Rune VI.
		case 3412: // Binding Rune VII.
		case 3413: // Binding Rune VIII.
		case 3414: // Binding Rune IX.
			// 拉拽效果 / Pulled Effect
		case 326: // 守护星之擒拿 / Sweeping Hook.
		case 2967: // 幻影摄捕 I / Illusion Chains.
		case 4721: // [ArchDaeva] Illusion Chains 5.1
		case 3071: // 活捉 I / Ensnaring Blow.
		case 3123: // Doom Lure.
		case 3162: // Divine Grasp I.
		case 3163: // Divine Grasp II.
		case 3164: // Divine Grasp III.
		case 3165: // Divine Grasp IV.
		case 3166: // Divine Grasp V.
		case 3167: // Divine Grasp VI.
			return true;
		}
		return false;
	}

	/**
	 * 物理暴击时按武器类型尝试触发硬直/踉跄类效果。
	 * On physical critical, attempts to apply stagger/stumble based on weapon type.
	 *
	 * @param attacker 攻击玩家 / attacking player
	 * @param attacked 被攻击者 / attacked
	 * @param returnSkill 触发来源技能 ID（0 表示普通攻击） / source skill id (0 for auto-attack)
	 */
	static void applyEffectOnCritical(Player attacker, Creature attacked, int returnSkill) {
		int skillId = 0;

		// 拥有解除感电的玩家不受影响 / players with Remove Shock cant be effected
		for (Effect ef : attacked.getEffectController().getAbnormalEffects()) {
			if (ef.getSkillId() == 1968) {
				return;
			}
		}

		WeaponType mainHandWeaponType = attacker.getEquipment().getMainHandWeaponType();

		if (mainHandWeaponType != null) {
			switch (mainHandWeaponType) {
			case POLEARM_2H:
			case CANNON_2H:
			case STAFF_2H:
			case SWORD_2H:
			case KEYBLADE_2H:
			case KEYHAMMER_2H:
				skillId = 8218;
				break;
			case BOW:
				skillId = 8217;
				break;
			default:
				break;
			}
		}

		if (skillId == 0) {
			return;
		}
		// 正式服该效果每次暴击以基础几率 10% 加额外加成触发。 / On retail this effect apply on each crit with 10% of base chance plus bonus
		// 效果穿透已在上方计算 / effect penetration calculated above
		if (Rnd.get(100) > (6 * attacked.getPulledMulti())) {
			return;
		}
		if (isSkillEffect(returnSkill)) {
			return;
		}

		SkillTemplate template = DataManager.SKILL_DATA.getSkillTemplate(skillId);

		if (template == null) {
			return;
		}

		Effect e = new Effect(attacker, attacked, template, template.getLvl(), 0);
		e.initialize();
		e.applyEffect();
	}

}

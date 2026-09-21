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
		return switch (skillId) {
			// 踉跄效果 / Stagger Effect
			// Finishing Arrow I.
			// Finishing Arrow II.
			// Finishing Arrow III.
			// Rupture Arrow I.
			// Rupture Arrow II.
			// Rupture Arrow III.
			// Rupture Arrow IV.
			// Rupture Arrow V.
			// Rupture Arrow VI.
			// Rupture Arrow VII.
			// Rupture Arrow VIII.
			// Rupture Arrow IX.
			// Frozen Shock I.
			// Frozen Shock II.
			// Frozen Shock III.
			// Frozen Shock IV.
			// Frozen Shock V.
			// Frozen Shock VI.
			// Frozen Shock VII.
			// Frozen Shock VIII.
			// Frozen Shock IX.
			// Frozen Shock X.
			// Frozen Shock XI.
			// Frozen Shock XII.
			// Aetherflame I.
			// [ArchDaeva] Aetherflame 5.1
			// Tremor I.
			// Tremor II.
			// Tremor III.
			// Tremor IV.
			// Tremor V.
			// Tremor VI.
			// Trunk Shot I.
			// Trunk Shot II.
			// Trunk Shot III.
			// Trunk Shot IV.
			// Trunk Shot V.
			// Trunk Shot VI.
			// Trunk Shot VII.
			// Trunk Shot VIII.
			// Trunk Shot IX.
			// Trunk Shot X.
			// Trunk Shot XI.
			// Shock & Awe I.
			// Shock & Awe II.
			// Shock & Awe III.
			// Shock & Awe IV.
			// Shock & Awe V.
			// Shock & Awe VI.
			// Shock & Awe VII.
			// Shock & Awe VIII.
			// Stone Shock I.
			// Stone Shock II.
			// Stone Shock III.
			// Stone Shock IV.
			// Stone Shock V.
			// Stone Shock VI.
			// Stone Shock VII.
			// Stone Shock VIII.
			// Stone Shock IX.
			// Stone Shock X.
			// Stone Shock XI.
			// Chorus Of Fortitude I.
			// Chorus Of Fortitude II.
			// Chorus Of Fortitude III.
			// Chorus Of Fortitude IV.
			// Sonic Gust I.
			// Sonic Gust II.
			// [ArchDaeva] Sonic Gust 5.1
			// 绊倒效果 / Stumble Effect
			// Explosion Of Rage I.
			// Explosion Of Rage II.
			// Explosion Of Rage III.
			// 激怒爆炸 IV 效果 / Explosion Of Rage IV.
			// Explosion Of Rage V.
			// Explosion Of Rage VI.
			// Explosion Of Rage VII.
			// Explosion Of Rage VIII.
			// Explosion Of Rage IX.
			// Explosion Of Rage X.
			// Explosion Of Rage XI.
			// Explosion Of Rage XII.
			// Crushing Blow I.
			// Crushing Blow II.
			// Crushing Blow III.
			// Crushing Blow IV.
			// Crushing Blow V.
			// Crushing Blow VI.
			// Crushing Blow VII.
			// Crushing Blow VIII.
			// Seismic Billow I.
			// Seismic Billow II.
			// Seismic Billow III.
			// Seismic Billow IV.
			// Seismic Billow V.
			// Seismic Billow VI.
			// Seismic Billow VII.
			// Seismic Billow VIII.
			// Spite Strike I.
			// Spite Strike II.
			// Spite Strike III.
			// Spite Strike IV.
			// Spite Strike V.
			// Spite Strike VI.
			// Wrathful Explosion I.
			// Wrathful Explosion II.
			// Wrathful Explosion III.
			// Wrathful Strike I.
			// Wrathful Strike II.
			// Wrathful Strike III.
			// Wrathful Strike IV.
			// Wrathful Strike V.
			// Wrathful Strike VI.
			// Wrathful Strike VII.
			// Wrathful Strike VIII.
			// Wrathful Strike IX.
			// Wrathful Strike X.
			// Wrathful Strike XI.
			// Wrathful Wave I.
			// Wrathful Wave II.
			// Wrathful Wave III.
			// Wrathful Wave IV.
			// Wrathful Wave V.
			// Wrathful Wave VI.
			// Wind Lance I.
			// Wind Lance II.
			// Wind Lance III.
			// Wind Lance IV.
			// Wind Lance V.
			// Severe Precision Cut I.
			// Severe Precision Cut II.
			// Severe Precision Cut III.
			// Severe Precision Cut IV.
			// Severe Precision Cut V.
			// Severe Precision Cut VI.
			// Disorienting Blow I.
			// Disorienting Blow II.
			// Disorienting Blow III.
			// Disorienting Blow IV.
			// Disorienting Blow V.
			// Disorienting Blow VI.
			// Pentacle Shock I.
			// Pentacle Shock II.
			// Pentacle Shock III.
			// Pentacle Shock IV.
			// Pentacle Shock V.
			// Pentacle Shock VI.
			// Pentacle Shock VII.
			// Pentacle Shock VIII.
			// Soul Crush I.
			// Soul Crush II.
			// Soul Crush III.
			// Soul Crush IV.
			// Soul Crush V.
			// Soul Crush VI.
			// Soul Crush VII.
			// Soul Crush VIII.
			// Beatdown I.
			// Annihilation Barrage I.
			// Annihilation Barrage II.
			// Annihilation Barrage III.
			// Annihilation Barrage IV.
			// Annihilation Barrage V.
			// Annihilation Barrage VI.
			// Uppercut I.
			// Uppercut II.
			// Uppercut III.
			// Uppercut IV.
			// Kinetic Slam I.
			// Kinetic Slam II.
			// Kinetic Slam III.
			// Kinetic Slam IV.
			// Kinetic Slam V.
			// Kinetic Slam VI.
			// Kinetic Slam VII.
			// Kinetic Slam VIII.
			// Kinetic Slam IX.
			// Kinetic Slam X.
			// Kinetic Slam XI.
			// Kinetic Slam XII.
			// [ArchDaeva] Kinetic Slam 5.1
			// [ArchDaeva] Kinetic Slam 5.1
			// [ArchDaeva] Kinetic Slam 5.1
			// Shieldburst I.
			// Shieldburst II.
			// Shieldburst III.
			// Face Smash I.
			// Face Smash II.
			// Face Smash III.
			// Face Smash IV.
			// Face Smash V.
			// Face Smash VI.
			// Face Smash VII.
			// Swinging Shield Counter I.
			// Swinging Shield Counter II.
			// Swinging Shield Counter III.
			// Sword Storm I.
			// Sword Storm II.
			// Shadowfall I.
			// Shadowfall II.
			// Shadowfall III.
			// Shadowfall IV.
			// Shadowfall V.
			// Shadowfall VI.
			// Shadowfall VII.
			// 空中击飞效果 / Openaerial Effect
			// Aerial Lockdown I.
			// Aerial Lockdown II.
			// Aerial Lockdown III.
			// Aerial Lockdown IV.
			// Aerial Lockdown V.
			// Aerial Lockdown VI.
			// Aerial Lockdown VII.
			// Aerial Lockdown VIII.
			// Aerial Lockdown IX.
			// Aether's Hold I.
			// Aether's Hold II.
			// Aether's Hold III.
			// Aether's Hold IV.
			// Aether's Hold V.
			// Aether's Hold VI.
			// Aether's Hold VII.
			// Aether's Hold VIII.
			// Paralysis Cannon I.
			// Paralysis Cannon II.
			// Paralysis Cannon III.
			// Paralysis Cannon IV.
			// Paralysis Cannon V.
			// Paralysis Cannon VI.
			// Binding Rune I.
			// Binding Rune II.
			// Binding Rune III.
			// Binding Rune IV.
			// Binding Rune V.
			// Binding Rune VI.
			// Binding Rune VII.
			// Binding Rune VIII.
			// Binding Rune IX.
			// 拉拽效果 / Pulled Effect
			// 守护星之擒拿 / Sweeping Hook.
			// 幻影摄捕 I / Illusion Chains.
			// [ArchDaeva] Illusion Chains 5.1
			// 活捉 I / Ensnaring Blow.
			// Doom Lure.
			// Divine Grasp I.
			// Divine Grasp II.
			// Divine Grasp III.
			// Divine Grasp IV.
			// Divine Grasp V.
			case 1054, 1055, 1056, 1102, 1103, 1104, 1105, 1106, 1107, 1108, 1109, 1110, 1226, 1227, 1228, 1229, 1230,
			     1231, 1232, 1233, 1234, 1235, 1236, 1237, 1258, 4728, 1826, 1827, 1828, 1829, 1830, 1831, 2055, 2056,
			     2057, 2058, 2059, 2060, 2061, 2062, 2063, 2064, 2065, 2232, 2235, 2238, 2241, 2244, 2247, 2250, 2253,
			     3614, 3615, 3616, 3617, 3618, 3619, 3620, 3621, 3622, 3623, 3624, 4396, 4397, 4398, 4399, 4522, 4523,
			     4790, 519, 520, 521, 522, 523, 524, 525, 526, 527, 528, 529, 530, 531, 532, 533, 534, 535, 536, 537,
			     538, 555, 556, 557, 558, 559, 560, 561, 562, 584, 585, 586, 587, 588, 589, 621, 622, 623, 624, 625,
			     626, 627, 628, 629, 630, 631, 632, 633, 634, 635, 636, 637, 638, 639, 640, 728, 729, 730, 731, 732,
			     733, 734, 735, 736, 737, 738, 1863, 1864, 1865, 1866, 1867, 1868, 1875, 1876, 1877, 1878, 1879, 1880,
			     1881, 1882, 1891, 1892, 1893, 1894, 1895, 1896, 1897, 1898, 2399, 2530, 2531, 2532, 2533, 2534, 2535,
			     2568, 2569, 2570, 2571, 2606, 2609, 2612, 2615, 2618, 2621, 2624, 2627, 2630, 2633, 2336, 2639, 4797,
			     4798, 4799, 2923, 2924, 2925, 3106, 3107, 3108, 3109, 3110, 3111, 3112, 3113, 3114, 3115, 3125, 3126,
			     3330, 4591, 4592, 4593, 4594, 4595, 4596, 545, 546, 547, 548, 549, 550, 551, 552, 553, 1184, 1185,
			     1186, 1187, 1188, 1189, 1190, 1191, 2109, 2110, 2111, 2112, 2113, 2114, 3406, 3407, 3408, 3409, 3410,
			     3411, 3412, 3413, 3414, 326, 2967, 4721, 3071, 3123, 3162, 3163, 3164, 3165, 3166,
			     3167 -> // Divine Grasp VI.
				true;
			default -> false;
		};
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

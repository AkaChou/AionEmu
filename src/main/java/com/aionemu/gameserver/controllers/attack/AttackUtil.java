package com.aionemu.gameserver.controllers.attack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.SkillElement;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.ItemSlot;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.item.ItemAttackType;
import com.aionemu.gameserver.model.templates.item.WeaponType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TARGET_SELECTED;
import com.aionemu.gameserver.skillengine.change.Func;
import com.aionemu.gameserver.skillengine.effect.modifier.ActionModifier;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.HitType;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.stats.CalculationType;
import com.aionemu.gameserver.utils.stats.StatFunctions;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 攻击伤害与命中状态计算工具类。
 * Utility for attack damage and hit-status calculations.
 *
 * @author ATracer
 */
public class AttackUtil {

	/**
	 * 计算物理自动攻击的结果列表。
	 * Calculates physical auto-attack results.
	 *
	 * attacker
	 * attacked
	 *
	 * @return 攻击结果列表 / list of attack results
	 */
	public static List<AttackResult> calculateAutoAttackPhysical(Creature attacker, Creature attacked) {
	    List<AttackResult> attackList = new ArrayList<>();
	    AttackStatus status = calculatePhysicalStatus(attacker, attacked, true);
		List<AttackResult> weaponResults = StatFunctions.calculateAttackDamage(attacker, SkillElement.NONE, status);
		for (int i = 0; i < weaponResults.size(); i++) {
			AttackResult result = weaponResults.get(i);
			float damage = applyPhysicalAutoAttackModifiers(attacker, attacked, result.getExactDamage());
			boolean mainHand = i == 0;
			splitPhysicalDamage(attacker, attacked, mainHand, getPhysicalHitCount(attacker, mainHand), Math.round(damage),
					result.getAttackStatus(), attackList);
		}
	    applyDamageMultiplier(attackList);
	    modifyDamageByNpcAi(attacker, attacked, attackList);
	    
	    attacked.getObserveController().checkShieldStatus(attackList, null, attacker);
	    return attackList;
	}

	/**
	 * 应用物理自动攻击的防御与修正后伤害。
	 * Applies physical auto-attack defense and damage adjustments.
	 *
	 * attacker
	 * attacked
	 * raw damage
	 * @return 修正后伤害 / adjusted damage
	 */
	private static float applyPhysicalAutoAttackModifiers(Creature attacker, Creature attacked, float damage) {
		float pDef = attacked.getGameStats().getPDef().getBonus() + StatFunctions.getMovementModifier(attacked,
				StatEnum.PHYSICAL_DEFENSE, attacked.getGameStats().getPDef().getBase());
		damage -= pDef * 0.10f;
		if (damage <= 0) {
			damage = 1;
		}
		return StatFunctions.adjustDamages(attacker, attacked, damage, 0, true, SkillElement.NONE);
	}

	/**
	 * 返回物理武器本次攻击的命中段数。
	 * Returns the hit count for a physical weapon attack.
	 *
	 * attacker
	 * whether main hand
	 * hit count
	 */
	private static int getPhysicalHitCount(Creature attacker, boolean mainHand) {
		if (!(attacker instanceof Player)) {
			return 1;
		}
		Item weapon = mainHand ? ((Player) attacker).getEquipment().getMainHandWeapon()
				: ((Player) attacker).getEquipment().getOffHandWeapon();
		return weapon == null ? 1 : Rnd.get(1, weapon.getItemTemplate().getWeaponStats().getHitCount());
	}

	/**
	 * 对结果列表应用全局伤害倍率，并保证非闪避命中至少为 1。
	 * Applies the global damage multiplier; non-dodge hits are at least 1.
	 *
	 * @param attackList 攻击结果列表 / attack result list
	 */
	private static void applyDamageMultiplier(List<AttackResult> attackList) {
		for (AttackResult result : attackList) {
			AttackStatus status = result.getAttackStatus();
			int damage = result.getDamage();
			damage = StatFunctions.applyDamageMultiplier(damage);
			if (damage <= 0 && status != AttackStatus.DODGE && status != AttackStatus.OFFHAND_DODGE) {
				damage = 1;
			}
			result.setDamage(damage);
		}
	}

	/**
	 * 若任一方为 NPC，则按 AI 修正结果列表中的伤害。
	 * If either side is an NPC, adjusts damages via AI hooks.
	 *
	 * attacker
	 * attacked
	 * @param attackResults 攻击结果列表 / attack results
	 */
	private static void modifyDamageByNpcAi(Creature attacker, Creature attacked, List<AttackResult> attackResults) {
		if (!(attacker instanceof Npc || attacked instanceof Npc)) {
			return;
		}
		for (AttackResult result : attackResults) {
			result.setDamage(modifyDamageByNpcAi(attacker, attacked, result.getDamage()));
		}
	}

	/**
	 * 按 NPC AI 修正单次伤害。
	 * Adjusts a single damage value via NPC AI hooks.
	 *
	 * attacker
	 * attacked
	 * raw damage
	 * @return 修正后伤害 / adjusted damage
	 */
	private static int modifyDamageByNpcAi(Creature attacker, Creature attacked, int damage) {
		if (attacker instanceof Npc) {
			damage = attacker.getAi2().modifyOwnerDamage(damage);
		}
		if (attacked instanceof Npc) {
			damage = attacked.getAi2().modifyDamage(damage);
		}
		return damage;
	}

	/**
	 * 计算格挡后的伤害（官服 5.8 机制：盾牌/武器减伤与盾牌上限）。
	 * Calculates blocked damage (retail 5.8: shield/weapon reduce and shield max block).
	 *
	 * attacked
	 * raw damage
	 * @return 格挡后伤害 / damage after block
	 */
	private static int calculateBlockedDamage(Creature attacked, int damage) {
		// 检查是否有盾牌
		if (attacked instanceof Player) {
			Player player = (Player) attacked;
			Item shield = player.getEquipment().getEquippedShield();
			if (shield != null) {
				// 【盾牌防御计算】
				// 1. 获取盾牌减伤百分比（橙色盾牌+10以上为70%）
				int shieldDefensePercent = player.getGameStats().getPositiveReverseStat(StatEnum.DAMAGE_REDUCE, damage);
				
				// 2. 计算理论减伤金额
				int theoreticalBlockedDamage = (int) (damage * shieldDefensePercent * 0.01f);
				
				// 3. 获取盾牌的最大防御值（伤害承受上限）
				int maxBlock = shield.getItemTemplate().getWeaponStats().getReduceMax();
				
				// 4. 检查是否过载
				// 如果理论减伤金额 > 最大防御值，则实际减伤 = 最大防御值（过载）
				// 否则实际减伤 = 理论减伤金额（未过载）
				int actualBlockedDamage;
				if (maxBlock > 0 && theoreticalBlockedDamage > maxBlock) {
					// 过载情况：按最大防御值计算
					actualBlockedDamage = maxBlock;
				} else {
					// 未过载情况：按理论减伤金额计算
					actualBlockedDamage = theoreticalBlockedDamage;
				}
				
				// 5. 计算实际伤害
				damage -= actualBlockedDamage;
				return damage;
			}
		}
		
		// 【武器防御计算】
		// 武器防御伤害削减为40%，没有最大防御值限制
		int weaponDefensePercent = attacked.getGameStats().getPositiveReverseStat(StatEnum.DAMAGE_REDUCE, damage);
		int actualBlockedDamage = (int) (damage * weaponDefensePercent * 0.01f);
		damage -= actualBlockedDamage;
		return damage;
	}

	/**
	 * 计算魔法自动攻击结果。
	 * Calculates magical auto-attack results.
	 *
	 * attacker
	 * attacked
	 * @param elem 技能元素 / skill element
	 * calculation types
	 *
	 * @return 攻击结果列表 / list of attack results
	 */
	public static List<AttackResult> calculateMagAttackResult(Creature attacker, Creature attacked, SkillElement elem,
			CalculationType... calculationTypes) {
		AttackStatus status = calculateMagicalStatus(attacker, attacked, 100, false, true);
		List<AttackResult> attackList = StatFunctions.calculateAttackDamage(attacker, elem, status, calculationTypes);
		applyMagicalAutoAttackModifiers(attacker, attacked, elem, attackList);
		applyAdditionalHitCount(attacker, status, attackList);
		applyDamageMultiplier(attackList);
		modifyDamageByNpcAi(attacker, attacked, attackList);
		attacked.getObserveController().checkShieldStatus(attackList, null, attacker);
		return attackList;
	}

	/**
	 * 应用魔法自动攻击的防御、暴击与元素修正。
	 * Applies magical auto-attack defense, critical and element adjustments.
	 *
	 * attacker
	 * attacked
	 * @param elem 技能元素 / skill element
	 * @param attackList 攻击结果列表 / attack result list
	 */
	private static void applyMagicalAutoAttackModifiers(Creature attacker, Creature attacked, SkillElement elem,
			List<AttackResult> attackList) {
		for (AttackResult result : attackList) {
			if (AttackStatus.getBaseStatus(result.getAttackStatus()) == AttackStatus.RESIST) {
				result.setDamage(0);
				continue;
			}
			float damage = result.getExactDamage();
			float mDef = attacked.getGameStats().getMDef().getBonus() + StatFunctions.getMovementModifier(attacked,
					StatEnum.MAGICAL_DEFEND, attacked.getGameStats().getMDef().getBase());
			damage -= mDef * 0.10f;
			if (result.getAttackStatus().isCritical()) {
				damage = calculateWeaponCritical(attacked, damage, null, StatEnum.MAGICAL_CRITICAL_DAMAGE_REDUCE);
			}
			damage = StatFunctions.adjustDamages(attacker, attacked, damage, 0, true, elem);
			result.setDamage(Math.max(1, Math.round(damage)));
		}
	}

	/**
	 * 按武器命中段数追加额外物理命中。
	 * Appends extra physical hits based on weapon hit count.
	 *
	 * attacker
	 * attack status
	 * @param attackList 攻击结果列表 / attack result list
	 */
	private static void applyAdditionalHitCount(Creature attacker, AttackStatus status, List<AttackResult> attackList) {
		if (!(attacker instanceof Player) || status == AttackStatus.DODGE || status == AttackStatus.RESIST) {
			return;
		}
		for (int i = 0, originalSize = Math.min(attackList.size(), 2); i < originalSize; i++) {
			Item weapon = i == 0 ? ((Player) attacker).getEquipment().getMainHandWeapon()
					: ((Player) attacker).getEquipment().getOffHandWeapon();
			if (weapon == null) {
				continue;
			}
			int extraHits = Rnd.get(0, weapon.getItemTemplate().getWeaponStats().getHitCount()) - 1;
			for (int hit = 0; hit < extraHits && attackList.get(i).getDamage() >= 10; hit++) {
				attackList.add(new AttackResult(Math.round(attackList.get(i).getDamage() * 0.1f),
						i == 0 ? AttackStatus.NORMALHIT : AttackStatus.OFFHAND_NORMALHIT, attackList.get(i).getDamageType()));
			}
		}
	}

	/**
	 * 按真端随机伤害表的 20 个等概率区间对技能伤害做波动。
	 * Randomizes skill damage using the retail table's 20 equally likely ranges.
	 *
	 * @param randomDamageType 随机伤害类型 / random damage type
	 * raw damage
	 * @return 波动后伤害 / randomized damage
	 */
	private static float randomizeDamage(int randomDamageType, float damage) {
		return damage * getRandomDamagePercent(randomDamageType, Rnd.get(20)) / 100f;
	}

	static int getRandomDamagePercent(int randomDamageType, int range) {
		switch (randomDamageType) {
		case 1:
			return range < 7 ? 50 : range < 13 ? 100 : 150;
		case 2:
			return range < 14 ? 60 : 200;
		case 3:
			return range < 7 ? 90 : range < 13 ? 100 : 110;
		case 6:
			return range < 14 ? 100 : 200;
		default:
			return 100;
		}
	}

	/**
	 * 计算物理攻击状态与伤害（委托自动攻击计算）。
	 * Calculates physical attack status and damage (delegates to auto-attack).
	 *
	 * attacker
	 * attacked
	 *
	 * @return 攻击结果列表 / list of attack results
	 */
	public static List<AttackResult> calculatePhysicalAttackResult(Creature attacker, Creature attacked) {
	    return calculateAutoAttackPhysical(attacker, attacked);
	}

	/**
	 * 按武器命中段数拆分物理伤害并生成攻击结果。
	 * Splits physical damage by weapon hit count into attack results.
	 *
	 * attacker
	 * attacked
	 * whether main hand
	 * hit count
	 * total damage
	 * attack status
	 * @param attackList 结果列表（输出） / output result list
	 * attack result list
	 */
	private static final List<AttackResult> splitPhysicalDamage(final Creature attacker, final Creature attacked,
			boolean mainHand, int hitCount, int damage, AttackStatus status, List<AttackResult> attackList) {
		WeaponType weaponType;
		switch (AttackStatus.getBaseStatus(status)) {
		case BLOCK:
			int reduce = damage - attacked.getGameStats().getPositiveReverseStat(StatEnum.DAMAGE_REDUCE, damage);
			if (attacked instanceof Player) {
				Item shield = ((Player) attacked).getEquipment().getEquippedShield();
				if (shield != null) {
					int reduceMax = shield.getItemTemplate().getWeaponStats().getReduceMax();
					if (reduceMax > 0 && reduceMax < reduce) {
						reduce = reduceMax;
					}
				}
			}
			damage -= reduce;
			if (damage < 1) {
				damage = 1;
			}
			break;
		case DODGE:
			damage = 0;
			break;
		case PARRY:
			damage *= 0.6;
			if (damage < 1) {
				damage = 1;
			}
			break;
		default:
			break;
		}

		if (status.isCritical()) {
			if (attacker instanceof Player) {
				weaponType = mainHand ? ((Player) attacker).getEquipment().getMainHandWeaponType()
						: ((Player) attacker).getEquipment().getOffHandWeaponType();
				damage = (int) calculateWeaponCritical(attacked, damage, weaponType,
						StatEnum.PHYSICAL_CRITICAL_DAMAGE_REDUCE);
				// 暴击时触发绊倒/踉跄计算 / Proc Stumble/Stagger on Crit calculation
				if (mainHand) {
					applyEffectOnCritical((Player) attacker, attacked, 0);
				}
			} else {
				damage = (int) calculateWeaponCritical(attacked, damage, null,
						StatEnum.PHYSICAL_CRITICAL_DAMAGE_REDUCE);
			}
		}

		if (damage == 1 || damage == 2) {
			attackList.add(new AttackResult(1, status, HitType.PHHIT));
			return attackList;
		}
		int[] hitDamages = splitPhysicalDamageValues(hitCount, damage);
		for (int i = 0; i < hitDamages.length; i++) {
			AttackStatus hitStatus = i == 0 ? status
					: mainHand ? AttackStatus.NORMALHIT : AttackStatus.OFFHAND_NORMALHIT;
			attackList.add(new AttackResult(hitDamages[i], hitStatus, HitType.PHHIT));
		}
		return attackList;
	}

	/**
	 * 将总伤害拆分为多段命中伤害数组。
	 * Splits total damage into per-hit damage values.
	 *
	 * hit count
	 * total damage
	 * per-hit damages
	 */
	static int[] splitPhysicalDamageValues(int hitCount, int damage) {
		int[] hitDamages = new int[Math.max(1, hitCount)];
		hitDamages[0] = damage;
		for (int i = 1; i < hitDamages.length; i++) {
			hitDamages[i] = Math.round(damage * 0.1f);
		}
		return hitDamages;
	}

	/**
	 * 计算武器暴击伤害（无额外暴击伤害加成）。
	 * Calculates weapon critical damage (no extra crit add).
	 *
	 * attacked
	 * raw damage
	 * weapon type
	 * @param stat 暴击减伤属性 / critical damage reduce stat
	 * @return 暴击后伤害 / damage after critical
	 */
	private static float calculateWeaponCritical(Creature attacked, float damages, WeaponType weaponType,
			StatEnum stat) {
		return calculateWeaponCritical(attacked, damages, weaponType, 0, stat);
	}

	/**
	 * 计算武器暴击伤害（含额外暴击伤害加成）。
	 * Calculates weapon critical damage including extra crit add.
	 *
	 * attacked
	 * raw damage
	 * weapon type
	 * @param critAddDmg 额外暴击伤害 / extra critical damage
	 * @param stat 暴击减伤属性 / critical damage reduce stat
	 * @return 暴击后伤害 / damage after critical
	 */
	private static float calculateWeaponCritical(Creature attacked, float damages, WeaponType weaponType,
			int critAddDmg, StatEnum stat) {
		int fortitude = attacked instanceof Player ? attacked.getGameStats().getStat(stat, 0).getCurrent() : 0;
		return Math.round(damages * calculateWeaponCriticalMultiplier(weaponType, stat, fortitude, critAddDmg));
	}

	/**
	 * 计算武器暴击倍率系数。
	 * Calculates the weapon critical damage multiplier.
	 *
	 * weapon type
	 * @param stat 暴击减伤属性 / critical damage reduce stat
	 * @param fortitude 坚韧/暴击减伤当前值 / fortitude current value
	 * @param critAddDmg 额外暴击伤害 / extra critical damage
	 * critical multiplier
	 */
	static float calculateWeaponCriticalMultiplier(WeaponType weaponType, StatEnum stat, int fortitude, int critAddDmg) {
		float coefficient = 1.5f;
		if (stat == StatEnum.PHYSICAL_CRITICAL_DAMAGE_REDUCE && weaponType != null) {
			coefficient = switch (weaponType) {
			case GUN_1H:
			case DAGGER_1H:
				yield 2.3f;
			case SWORD_1H:
				yield 2.2f;
			case MACE_1H:
				yield 2f;
			case SWORD_2H:
			case POLEARM_2H:
				yield 1.8f;
			case STAFF_2H:
			case BOW:
				yield 1.7f;
			default:
				yield 1.5f;
			};
		}
		return coefficient - fortitude / 1000f + critAddDmg / 100f;
	}

	/**
	 * 计算技能物理/主攻结果，写入 Effect 的伤害与攻击状态。
	 * Calculates skill physical/main-attack result and writes damage/status into the Effect.
	 *
	 * effect
	 * skill damage
	 * @param modifier 伤害修正器 / damage modifier
	 * @param func 加成方式（加算/百分比） / add or percent function
	 * @param randomDamage 随机伤害类型 / random damage type
	 * accuracy modifier
	 * critical probability
	 * @param critAddDmg 额外暴击伤害 / extra critical damage
	 * @param cannotMiss 是否不可未命中 / whether cannot miss
	 * @param shared 是否分摊伤害 / whether damage is shared
	 * @param ignoreShield 是否忽略护盾 / whether to ignore shield
	 * whether main hand
	 */
	public static void calculateSkillResult(Effect effect, int skillDamage, ActionModifier modifier, Func func,
			int randomDamage, int accMod, int criticalProb, int critAddDmg, boolean cannotMiss, boolean shared,
			boolean ignoreShield, boolean isMainHand) {
		Creature effector = effect.getEffector();
		Creature effected = effect.getEffected();

		float damage = 0;
		int baseAttack = 0;
		/**
	 * 部分高阶守护者装备对特定怪物类型提供战斗属性加成。 / - Some Archdaeva equipment will give boosted combat stats against certain monster types. - If the gear and the monster type match, you will get bonus damage. - Some items focus on a single monster type while others can affect multiple types. - There are four monster types in total: Warrior, Assassin, Mage, and Special
	 */
		if (effector.getEffectController().hasAbnormalEffect(22987)
				&& effector.getEffectController().hasAbnormalEffect(22988)
				&& effector.getEffectController().hasAbnormalEffect(22989)
				&& effector.getEffectController().hasAbnormalEffect(22990)) {
			damage = StatFunctions.calculatePhysicalAttackDamageNoDef(effect.getEffector(), effect.getEffected(), true)
					* 2 / 100;
		}
		CalculationType[] calculationTypes = new CalculationType[] { CalculationType.SKILL };
		if (effector instanceof Player && ((Player) effector).getEquipment().hasDualWeaponEquipped(ItemSlot.SUB_HAND)) {
			calculationTypes = ArrayUtils.add(calculationTypes, CalculationType.DUAL_WIELD);
		}
		AttackStatus status = AttackStatus.NORMALHIT;
		if (effector.getAttackType() == ItemAttackType.PHYSICAL) {
			status = calculatePhysicalStatus(effector, effected, true, accMod, criticalProb, true, cannotMiss);
		} else {
			status = calculateMagicalStatus(effector, effected, criticalProb, true, effect.getSkillTemplate().isMcritApplied());
		}
		if (effector.getAttackType() == ItemAttackType.PHYSICAL) {
			CalculationType[] baseCalculationTypes = ArrayUtils.add(calculationTypes, CalculationType.APPLY_POWER_SHARD_DAMAGE);
			baseAttack = effector.getGameStats().getMainHandPAttack(baseCalculationTypes).getBase();
			CalculationType[] damageCalculationTypes = ArrayUtils.add(baseCalculationTypes, CalculationType.REMOVE_POWER_SHARD);
			damage = 0;
			for (AttackResult result : StatFunctions.calculateAttackDamage(effector, SkillElement.NONE, status, damageCalculationTypes)) {
				damage += result.getExactDamage();
			}
		} else {
			if (isMainHand) {
				baseAttack = effector.getGameStats().getMainHandMAttack(calculationTypes).getBase();
			} else {
				baseAttack = effector.getGameStats().getOffHandMAttack(calculationTypes).getBase();
			}
			damage = StatFunctions.calculateMagicalAttackDamage(effector, effected,
					effector.getAttackType().getMagicalElement(), isMainHand);
		}

		// 添加技能伤害 / add skill damage
		if (func != null) {
			switch (func) {
			case ADD:
				damage += skillDamage;
				break;
			case PERCENT:
				damage += baseAttack * skillDamage / 100f;
				break;
			default:
				break;
			}
		}

		// 添加额外伤害 / add bonus damage
		if (modifier != null) {
			int bonus = modifier.analyze(effect);
			switch (modifier.getFunc()) {
			case ADD:
				damage += bonus;
				break;
			case PERCENT:
				damage += baseAttack * bonus / 100f;
				break;
			default:
				break;
			}
		}

		float damageMultiplier = effector.getObserveController().getBasePhysicalDamageMultiplier(true);
		damage = Math.round(damage * damageMultiplier);

		// 眩晕射击等技能的随机伤害实现 / implementation of random damage for skills like Stunning Shot, etc
		if (randomDamage > 0) {
			// 【修复】使用 randomizeDamage 方法计算随机伤害
			damage = randomizeDamage(randomDamage, damage);
		}

		if (status.isCritical()) {
			if (effector instanceof Player) {
				WeaponType weaponType = ((Player) effector).getEquipment().getMainHandWeaponType();
				damage = (int) calculateWeaponCritical(effected, damage, weaponType, critAddDmg,
						StatEnum.PHYSICAL_CRITICAL_DAMAGE_REDUCE);
				// 暴击时触发绊倒/踉跄计算 / Proc Stumble/Stagger on Crit calculation
				applyEffectOnCritical((Player) effector, effected, effect.getSkillId());
			} else {
				damage = (int) calculateWeaponCritical(effected, damage, null, critAddDmg,
						StatEnum.PHYSICAL_CRITICAL_DAMAGE_REDUCE);
			}
		}

		float pDef = effected.getGameStats().getPDef().getBonus() + StatFunctions.getMovementModifier(effected,
				StatEnum.PHYSICAL_DEFENSE, effected.getGameStats().getPDef().getBase());
		damage -= (pDef * 0.10f);

		switch (AttackStatus.getBaseStatus(status)) {
		case BLOCK:
			damage = calculateBlockedDamage(effected, Math.round(damage));
			break;
		case PARRY:
			damage *= 0.6;
			break;
		default:
			break;
		}

		if (effector instanceof Npc) {
			damage = effector.getAi2().modifyOwnerDamage(Math.round(damage));
		}

		if (shared && !effect.getSkill().getEffectedList().isEmpty()) {
			damage /= effect.getSkill().getEffectedList().size();
		}

		damage = StatFunctions.adjustDamages(effect.getEffector(), effect.getEffected(), damage,
				effect.getPvpDamage(), true);

		damage = StatFunctions.applyDamageMultiplier(Math.round(damage));

		if (damage < 0) {
			damage = 0;
		}

		if (effected instanceof Npc) {
			damage = effected.getAi2().modifyDamage(Math.round(damage));
		}

		calculateEffectResult(effect, effected, Math.round(damage), status, HitType.PHHIT, ignoreShield);
	}

	/**
	 * 将伤害结果写回 Effect，并可选触发护盾检测。
	 * Writes the damage result back into the Effect and optionally checks shields.
	 *
	 * effect
	 * effected
	 * damage
	 * attack status
	 * hit type
	 * @param ignoreShield 是否忽略护盾 / whether to ignore shield
	 */
	private static void calculateEffectResult(Effect effect, Creature effected, int damage, AttackStatus status,
			HitType hitType, boolean ignoreShield) {
		AttackResult attackResult = new AttackResult(damage, status, hitType);

		if (!ignoreShield) {
			effected.getObserveController().checkShieldStatus(Collections.singletonList(attackResult), effect,
					effect.getEffector());
		}

		effect.setReserved1(attackResult.getDamage());
		effect.setAttackStatus(attackResult.getAttackStatus());
		effect.setLaunchSubEffect(attackResult.isLaunchSubEffect());
		effect.setReflectedDamage(attackResult.getReflectedDamage());
		effect.setReflectedSkillId(attackResult.getReflectedSkillId());
		effect.setMpShield(attackResult.getShieldMp());
		effect.setProtectedDamage(attackResult.getProtectedDamage());
		effect.setProtectedSkillId(attackResult.getProtectedSkillId());
		effect.setProtectorId(attackResult.getProtectorId());
		effect.setShieldDefense(attackResult.getShieldType());
	}

	/**
	 * 计算持续魔法技能（DoT）的单次伤害。
	 * Calculates a single tick of magical over-time (DoT) skill damage.
	 *
	 * effect
	 * skill damage
	 * skill element
	 * tick position
	 * @param useMagicBoost 是否使用魔法增强 / whether to use magic boost
	 * critical probability
	 * @param critAddDmg 额外暴击伤害 / extra critical damage
	 * tick damage
	 */
	public static int calculateMagicalOverTimeSkillResult(Effect effect, int skillDamage, SkillElement element,
			int position, boolean useMagicBoost, int criticalProb, int critAddDmg) {
		Creature effector = effect.getEffector();
		Creature effected = effect.getEffected();

		float damageMultiplier = effector.getObserveController().getBaseMagicalDamageMultiplier();

		int damage = Math.round(StatFunctions.calculateMagicalSkillDamage(effect.getEffector(), effect.getEffected(),
				skillDamage, 0, element, useMagicBoost, false, false, effect.getSkillTemplate().getPvpDamage())
				* damageMultiplier);

		AttackStatus status = effect.getAttackStatus();
		// 仅在尚未强制时计算攻击状态 / calculate attack status only if it has not been forced already
		if (status == AttackStatus.NORMALHIT && position == 1) {
			status = calculateMagicalStatus(effector, effected, criticalProb, true, effect.getSkillTemplate().isMcritApplied());
		}

		switch (status) {
		case CRITICAL:
			if (effector instanceof Player) {
				WeaponType weaponType = ((Player) effector).getEquipment().getMainHandWeaponType();
				damage = (int) calculateWeaponCritical(effected, damage, weaponType, critAddDmg,
						StatEnum.MAGICAL_CRITICAL_DAMAGE_REDUCE);
			} else {
				damage = (int) calculateWeaponCritical(effected, damage, null, critAddDmg,
						StatEnum.MAGICAL_CRITICAL_DAMAGE_REDUCE);
			}
			break;
		default:
			break;
		}

		damage = Math.round(StatFunctions.adjustDamages(effector, effected, damage,
				effect.getSkillTemplate().getPvpDamage(), false, element));

		damage = StatFunctions.applyDamageMultiplier(damage);

		if (damage <= 0) {
			damage = 1;
		}

		if (effected instanceof Npc) {
			damage = effected.getAi2().modifyDamage(damage);
		}
		return damage;
	}

	/**
	 * 计算魔法技能结果（使用默认参数的便捷重载）。
	 * Calculates magical skill result (convenience overload with defaults).
	 *
	 * effect
	 * skill damage
	 * @param modifier 伤害修正器 / damage modifier
	 * skill element
	 */
	public static void calculateMagicalSkillResult(Effect effect, int skillDamage, ActionModifier modifier,
			SkillElement element) {
		calculateMagicalSkillResult(effect, skillDamage, modifier, element, true, true, false, Func.ADD, 100, 0, false,
				false);
	}

	/**
	 * 计算魔法技能完整结果并写入 Effect。
	 * Calculates full magical skill result and writes it into the Effect.
	 *
	 * effect
	 * skill damage
	 * @param modifier 伤害修正器 / damage modifier
	 * skill element
	 * @param useMagicBoost 是否使用魔法增强 / whether to use magic boost
	 * @param useKnowledge 是否使用知识属性 / whether to use knowledge
	 * @param noReduce 是否不做减伤 / whether to skip reduction
	 * @param func 加成方式 / add or percent function
	 * critical probability
	 * @param critAddDmg 额外暴击伤害 / extra critical damage
	 * @param shared 是否分摊伤害 / whether damage is shared
	 * @param ignoreShield 是否忽略护盾 / whether to ignore shield
	 */
	public static void calculateMagicalSkillResult(Effect effect, int skillDamage, ActionModifier modifier,
			SkillElement element, boolean useMagicBoost, boolean useKnowledge, boolean noReduce, Func func,
			int criticalProb, int critAddDmg, boolean shared, boolean ignoreShield) {
		Creature effector = effect.getEffector();
		Creature effected = effect.getEffected();

		float damageMultiplier = effector.getObserveController().getBaseMagicalDamageMultiplier();
		int baseAttack = effector.getGameStats().getMainHandPAttack().getBase(); // Npc spells scale with this
		int damages = 0;
		int bonus = 0;

		if (func.equals(Func.PERCENT) && effector instanceof Npc) {
			damages = Math.round(baseAttack * skillDamage / 100f);
		} else {
			damages = skillDamage;
		}

		// 添加额外伤害 / add bonus damage
		if (modifier != null) {
			bonus = modifier.analyze(effect);
			switch (modifier.getFunc()) {
			case ADD:
				break;
			case PERCENT:
				if (effector instanceof Npc) {
					bonus = Math.round(baseAttack * bonus / 100f);
				}
				break;
			default:
				break;
			}
		}
		int damage = Math
				.round(StatFunctions.calculateMagicalSkillDamage(effect.getEffector(), effect.getEffected(), damages,
						bonus, element, useMagicBoost, useKnowledge, noReduce, effect.getSkillTemplate().getPvpDamage())
						* damageMultiplier);

		AttackStatus status = calculateMagicalStatus(effector, effected, criticalProb, true, effect.getSkillTemplate().isMcritApplied());

		switch (status) {
		case CRITICAL:
			if (effector instanceof Player) {
				WeaponType weaponType = ((Player) effector).getEquipment().getMainHandWeaponType();
				damage = (int) calculateWeaponCritical(effected, damage, weaponType, critAddDmg,
						StatEnum.MAGICAL_CRITICAL_DAMAGE_REDUCE);
			} else {
				damage = (int) calculateWeaponCritical(effected, damage, null, critAddDmg,
						StatEnum.MAGICAL_CRITICAL_DAMAGE_REDUCE);
			}
			break;
		default:
			break;
		}

		if (effector instanceof Npc) {
			damage = effector.getAi2().modifyOwnerDamage(damage);
		}

		if (shared && !effect.getSkill().getEffectedList().isEmpty()) {
			damage /= effect.getSkill().getEffectedList().size();
		}

		damage = (int) StatFunctions.adjustDamages(effector, effected, damage,
				effect.getSkillTemplate().getPvpDamage(), false, element);
		damage = StatFunctions.applyDamageMultiplier(damage);

		if (effected instanceof Npc) {
			damage = effected.getAi2().modifyDamage(damage);
		}

		calculateEffectResult(effect, effected, damage, status, HitType.MAHIT, ignoreShield);
	}

	/**
	 * 计算物理攻击状态（默认参数便捷重载）。
	 * Calculates physical attack status (convenience overload with defaults).
	 *
	 * attacker
	 * attacked
	 * whether main hand
	 * attack status
	 */
	public static AttackStatus calculatePhysicalStatus(Creature attacker, Creature attacked, boolean isMainHand) {
		return calculatePhysicalStatus(attacker, attacked, isMainHand, 0, 100, false, false);
	}

	/**
	 * 计算物理攻击状态：闪避/格挡/招架与暴击组合。
	 * Calculates physical attack status: dodge/block/parry and critical combinations.
	 *
	 * attacker
	 * attacked
	 * whether main hand
	 * accuracy modifier
	 * critical probability
	 * @param isSkill 是否技能攻击 / whether skill attack
	 * @param cannotMiss 是否不可未命中 / whether cannot miss
	 * attack status
	 */
	public static AttackStatus calculatePhysicalStatus(Creature attacker, Creature attacked, boolean isMainHand,
			int accMod, int criticalProb, boolean isSkill, boolean cannotMiss) {
		AttackStatus status = AttackStatus.NORMALHIT;

		if (!cannotMiss) { // Parry can only be done with weapon, blocking - with a shield. These
							// 限制不适用于 NPC。正式服 NPC 不需要护盾或武器即可 / limitations don't apply to npc. Retail npc don't need a shield or weapon to
							// 格挡/招架 / block/parry
			if (!isSkill && StatFunctions.calculatePhysicalDodgeRate(attacker, attacked, accMod)) {
				status = AttackStatus.DODGE;
			} else if (attacked instanceof Player && ((Player) attacked).getEquipment().isShieldEquipped()
					&& StatFunctions.calculatePhysicalBlockRate(attacker, attacked, accMod)) {
				status = AttackStatus.BLOCK;
			} else if (attacked instanceof Npc && StatFunctions.calculatePhysicalBlockRate(attacker, attacked, accMod)) {
				status = AttackStatus.BLOCK;
			} else if (attacked instanceof Player && ((Player) attacked).getEquipment().getMainHandWeaponType() != null
					&& StatFunctions.calculatePhysicalParryRate(attacker, attacked, accMod)) {
				status = AttackStatus.PARRY;
			} else if (attacked instanceof Npc && StatFunctions.calculatePhysicalParryRate(attacker, attacked, accMod)) {
				status = AttackStatus.PARRY;
			}
		} else {
			/**
			 * AlwaysParry / AlwaysBlock。
	 * Check AlwaysDodge, AlwaysParry, AlwaysBlock
			 */
			StatFunctions.calculatePhysicalDodgeRate(attacker, attacked, accMod);
			StatFunctions.calculatePhysicalParryRate(attacker, attacked, accMod);
			StatFunctions.calculatePhysicalBlockRate(attacker, attacked, accMod);
		}

		if (StatFunctions.calculatePhysicalCriticalRate(attacker, attacked, isMainHand, criticalProb, isSkill)) {
			switch (status) {
			case BLOCK:
				status = AttackStatus.CRITICAL_BLOCK;
				break;
			case PARRY:
				status = AttackStatus.CRITICAL_PARRY;
				break;
			case DODGE:
				status = AttackStatus.CRITICAL_DODGE;
				break;
			default:
				status = AttackStatus.CRITICAL;
				break;
			}
		}
		return isMainHand ? status : AttackStatus.getOffHandStats(status);
	}

	/**
	 * 计算魔法攻击状态（默认应用魔法暴击）。
	 * Calculates magical attack status (applies magical critical by default).
	 * <p>
	 * Every +100 delta of (MR - MA) = +10% resist; difference of 1000 = 100% resist.
	 *
	 * attacker
	 * attacked
	 * critical probability
	 * @param isSkill 是否技能攻击 / whether skill attack
	 * attack status
	 */
	public static AttackStatus calculateMagicalStatus(Creature attacker, Creature attacked, int criticalProb,
			boolean isSkill) {
		return calculateMagicalStatus(attacker, attacked, criticalProb, isSkill, true);
	}

	/**
	 * 计算魔法攻击状态：抵抗与魔法暴击。
	 * Calculates magical attack status: resist and magical critical.
	 *
	 * attacker
	 * attacked
	 * critical probability
	 * @param isSkill 是否技能攻击 / whether skill attack
	 * @param applyMcrit 是否应用魔法暴击 / whether to apply magical critical
	 * attack status
	 */
	public static AttackStatus calculateMagicalStatus(Creature attacker, Creature attacked, int criticalProb,
			boolean isSkill, boolean applyMcrit) {
		if (!isSkill) {
			if (Rnd.get(0, 1000) < StatFunctions.calculateMagicalResistRate(attacker, attacked, 0, SkillElement.NONE)) {
				return AttackStatus.RESIST;
			}
		}

		if (StatFunctions.calculateMagicalCriticalRate(attacker, attacked, criticalProb, applyMcrit)) {
			return AttackStatus.CRITICAL;
		}

		return AttackStatus.NORMALHIT;
	}

	/**
	 * 取消所有以该目标为首目标的施法。
	 * Cancels casts from anyone currently targeting this creature as first target.
	 *
	 * target creature
	 */
	public static void cancelCastOn(final Creature target) {
		target.getKnownList().doOnAllPlayers(new Visitor<Player>() {

			@Override
			public void visit(Player observer) {
				if (observer.getTarget() == target) {
					cancelCast(observer, target);
				}
			}
		});

		target.getKnownList().doOnAllNpcs(new Visitor<Npc>() {

			@Override
			public void visit(Npc observer) {
				if (observer.getTarget() == target) {
					cancelCast(observer, target);
				}
			}
		});
	}

	/**
	 * 若生物正在对该目标施法则取消当前技能。
	 * Cancels the creature's current skill if it is casting on the target.
	 *
	 * caster
	 * target
	 */
	private static void cancelCast(Creature creature, Creature target) {
		if (target != null && creature.getCastingSkill() != null) {
			if (creature.getCastingSkill().getFirstTarget().equals(target)) {
				creature.getController().cancelCurrentSkill();
			}
		}
	}

	/**
	 * 清除所有玩家对该生物的选中目标。
	 * Clears this creature as the selected target for all observing players.
	 *
	 * target creature
	 */
	public static void removeTargetFrom(final Creature object) {
		removeTargetFrom(object, false);
	}

	/**
	 * 清除选中该生物的玩家目标；可选仅在不可见时清除。
	 * Clears players targeting this creature; optionally only when they cannot see it.
	 *
	 * target creature
	 * @param validateSee 是否校验可见性 / whether to validate visibility
	 */
	public static void removeTargetFrom(final Creature object, final boolean validateSee) {
		object.getKnownList().doOnAllPlayers(new Visitor<Player>() {

			@Override
			public void visit(Player observer) {
				if (validateSee && observer.getTarget() == object) {
					if (!observer.canSee(object)) {
						observer.setTarget(null);
						// 正式服数据包 / retail packet (//fsc 0x44 dhdd 0 0 0 0) right after SM_PLAYER_STATE
						PacketSendUtility.sendPacket(observer, new SM_TARGET_SELECTED(observer));
					}
				} else if (observer.getTarget() == object) {
					observer.setTarget(null);
					// 正式服数据包 / retail packet (//fsc 0x44 dhdd 0 0 0 0) right after SM_PLAYER_STATE
					PacketSendUtility.sendPacket(observer, new SM_TARGET_SELECTED(observer));
				}
			}
		});
	}

	/**
	 * 判断技能是否自带硬直类效果（暴击触发硬直时应跳过）。
	 * Returns whether the skill already applies stagger/stumble-like effects (skip crit procs).
	 *
	 * skill id
	 * @return 是否自带相关效果 / whether skill already applies the effect
	 * @author KorLightNing
	 */
	public static boolean isSkillEffect(int skillId) {
		switch (skillId) {
		/**
	 * 【踉跄效果】 / Stagger Effect
	 */
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
			/**
	 * 【绊倒效果】 / Stumble Effect
	 */
		case 519: // Explosion Of Rage I.
		case 520: // Explosion Of Rage II.
		case 521: // Explosion Of Rage III.
		case 522: // Explosion Of Rage IV.
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
			/**
	 * 【空中击飞效果】 / Openaerial Effect
	 */
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
			/**
	 * **************** [Pulled Effect] * *****************
	 */
		case 326: // Sweeping Hook.
		case 2967: // Illusion Chains.
		case 4721: // [ArchDaeva] Illusion Chains 5.1
		case 3071: // Ensnaring Blow.
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
	 * attacking player
	 * attacked
	 * @param returnSkill 触发来源技能 ID（0 表示普通攻击） / source skill id (0 for auto-attack)
	 */
	public static void applyEffectOnCritical(Player attacker, Creature attacked, int returnSkill) {
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

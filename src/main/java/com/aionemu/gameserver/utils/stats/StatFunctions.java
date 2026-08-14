package com.aionemu.gameserver.utils.stats;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.configs.main.FallDamageConfig;
import com.aionemu.gameserver.configs.main.RateConfig;
import com.aionemu.gameserver.configs.main.SkillConfig;
import com.aionemu.gameserver.controllers.attack.AttackResult;
import com.aionemu.gameserver.controllers.attack.AttackStatus;
import com.aionemu.gameserver.controllers.observer.AttackerCriticalStatus;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.SkillElement;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Homing;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.Servant;
import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.model.gameobjects.SummonedObject;
import com.aionemu.gameserver.model.gameobjects.Trap;
import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RewardType;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.gameobjects.siege.SiegeNpc;
import com.aionemu.gameserver.model.stats.calc.AdditionStat;
import com.aionemu.gameserver.model.stats.calc.StatCapUtil;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.container.CombatMode;
import com.aionemu.gameserver.model.stats.container.CreatureGameStats;
import com.aionemu.gameserver.model.stats.container.PlayerGameStats;
import com.aionemu.gameserver.model.stats.container.RatioType;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.item.ArmorType;
import com.aionemu.gameserver.model.templates.item.WeaponStats;
import com.aionemu.gameserver.model.templates.npc.AbyssNpcType;
import com.aionemu.gameserver.model.templates.npc.NpcRating;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS;
import com.aionemu.gameserver.skillengine.model.HitType;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 核心战斗与属性计算工具：经验/DP/AP/GP 奖励、物理与魔法伤害、闪避/招架/格挡/暴击与跌落伤害
 * Core combat and stat math: XP/DP/AP/GP rewards, physical/magical damage, dodge/parry/block/crit and fall damage
 */
public class StatFunctions {

	/**
	 * 计算单人击杀目标的经验奖励
	 * Calculate solo XP reward from target
	 *
	 * 玩家 / Player
	 * Target
	 * @return 单人经验奖励 / Solo XP reward
	 */
	public static long calculateSoloExperienceReward(Player player, Creature target) {
		int playerLevel = player.getCommonData().getLevel();
		int targetLevel = target.getLevel();
		long baseXP = ((Npc) target).getObjectTemplate().getStatsTemplate().getMaxXp();
		int xpPercentage = XPRewardEnum.xpRewardFrom(targetLevel - playerLevel);
		return (int) Math.floor(baseXP * xpPercentage / 100d);
	}

	/**
	 * 计算队伍击杀目标的经验奖励（按范围内最高等级）
	 * Calculate group XP reward from target using max level in range
	 *
	 * @param maxLevelInRange 范围内最高等级 / Max level in range
	 * Target
	 * @return 队伍经验奖励 / Group XP reward
	 */
	public static long calculateGroupExperienceReward(int maxLevelInRange, Creature target) {
		int targetLevel = target.getLevel();
		long baseXP = ((Npc) target).getObjectTemplate().getStatsTemplate().getMaxXp();
		int xpPercentage = XPRewardEnum.xpRewardFrom(targetLevel - maxLevelInRange);
		return (int) Math.floor(baseXP * xpPercentage / 100d);
	}

	/**
	 * 计算单人击杀目标的 DP 奖励
	 * Calculate solo DP reward from target
	 *
	 * 玩家 / Player
	 * Target
	 * Solo DP reward
	 */
	public static int calculateSoloDPReward(Player player, Creature target) {
		int playerLevel = player.getCommonData().getLevel();
		int targetLevel = target.getLevel();
		NpcRating npcRating = ((Npc) target).getObjectTemplate().getRating();
		int baseDP = targetLevel * calculateRatingMultipler(npcRating);
		int xpPercentage = XPRewardEnum.xpRewardFrom(targetLevel - playerLevel);
		float rate = player.getRates().getDpNpcRate();
		return (int) Math.floor(baseDP * xpPercentage * rate / 100);
	}

	/**
	 * 计算 PvE 击杀获得的 AP
	 * Calculate AP gained from PvE kill
	 *
	 * 玩家 / Player
	 * Target
	 * AP reward
	 */
	public static int calculatePvEApGained(Player player, Creature target) {
		float apPercentage = target instanceof SiegeNpc ? 100f
				: APRewardEnum.apReward(player.getAbyssRank().getRank().getId());
		boolean lvlDiff = player.getCommonData().getLevel() - target.getLevel() > 10;
		float apNpcRate = ApNpcRating(((Npc) target).getObjectTemplate().getRating());
		return (int) (lvlDiff ? 1
				: RewardType.AP_NPC.calcReward(player, (int) Math.floor(15 * apPercentage * apNpcRate / 100)));
	}

	/**
	 * 计算 PvP 死亡损失的 AP
	 * Calculate AP lost on PvP death
	 *
	 * @param defeated 被击败玩家 / Defeated player
	 * Winner
	 * AP lost
	 */
	public static int calculatePvPApLost(Player defeated, Player winner) {
		int pointsLost = Math
				.round(defeated.getAbyssRank().getRank().getPointsLost() * defeated.getRates().getApPlayerLossRate());
		int difference = winner.getLevel() - defeated.getLevel();
		if (difference > 4) {
			pointsLost = Math.round(pointsLost * 0.1f);
		} else {
			switch (difference) {
			case 3:
				pointsLost = Math.round(pointsLost * 0.85f);
				break;
			case 4:
				pointsLost = Math.round(pointsLost * 0.65f);
				break;
			}
		}
		return pointsLost;
	}

	/**
	 * 计算 PvP 击杀获得的 AP
	 * Calculate AP gained from PvP kill
	 *
	 * @param defeated 被击败玩家 / Defeated player
	 * @param maxRank 击杀方最高军衔 / Winner max rank
	 * @param maxLevel 击杀方最高等级 / Winner max level
	 * AP gained
	 */
	public static int calculatePvpApGained(Player defeated, int maxRank, int maxLevel) {
		int pointsGained = defeated.getAbyssRank().getRank().getPointsGained();
		int difference = maxLevel - defeated.getLevel();
		if (difference > 4) {
			pointsGained = Math.round(pointsGained * 0.1f);
		} else if (difference < -3) {
			pointsGained = Math.round(pointsGained * 1.3f);
		} else {
			switch (difference) {
			case 3:
				pointsGained = Math.round(pointsGained * 0.85f);
				break;
			case 4:
				pointsGained = Math.round(pointsGained * 0.65f);
				break;
			case -2:
				pointsGained = Math.round(pointsGained * 1.1f);
				break;
			case -3:
				pointsGained = Math.round(pointsGained * 1.2f);
				break;
			}
		}
		int winnerAbyssRank = maxRank;
		int defeatedAbyssRank = defeated.getAbyssRank().getRank().getId();
		int abyssRankDifference = winnerAbyssRank - defeatedAbyssRank;
		if (winnerAbyssRank <= 7 && abyssRankDifference > 0) {
			float penaltyPercent = abyssRankDifference * 0.05f;
			pointsGained -= Math.round(pointsGained * penaltyPercent);
		}
		return pointsGained;
	}

	/**
	 * 计算 PvP 死亡损失的 GP
	 * Calculate GP lost on PvP death
	 *
	 * @param defeated 被击败玩家 / Defeated player
	 * Winner
	 * GP lost
	 */
	public static int calculatePvPGpLost(Player defeated, Player winner) {
		int pointsLost = Math
				.round(defeated.getAbyssRank().getRank().getPointsLost() * defeated.getRates().getGpPlayerLossRate());
		// 等级惩罚计算 / Level penalty calculation
		int difference = winner.getLevel() - defeated.getLevel();
		if (difference > 4) {
			pointsLost = Math.round(pointsLost * 0.1f);
		} else {
			switch (difference) {
			case 3:
				pointsLost = Math.round(pointsLost * 0.85f);
				break;
			case 4:
				pointsLost = Math.round(pointsLost * 0.65f);
				break;
			}
		}
		return pointsLost;
	}

	/**
	 * 计算 PvP 击杀获得的经验
	 * Calculate XP gained from PvP kill
	 *
	 * @param defeated 被击败玩家 / Defeated player
	 * @param maxRank 击杀方最高军衔 / Winner max rank
	 * @param maxLevel 击杀方最高等级 / Winner max level
	 * XP gained
	 */
	public static int calculatePvpXpGained(Player defeated, int maxRank, int maxLevel) {
		int pointsGained = 5000;
		int difference = maxLevel - defeated.getLevel();
		if (difference > 4) {
			pointsGained = Math.round(pointsGained * 0.1f);
		} else if (difference < -3) {
			pointsGained = Math.round(pointsGained * 1.3f);
		} else {
			switch (difference) {
			case 3:
				pointsGained = Math.round(pointsGained * 0.85f);
				break;
			case 4:
				pointsGained = Math.round(pointsGained * 0.65f);
				break;
			case -2:
				pointsGained = Math.round(pointsGained * 1.1f);
				break;
			case -3:
				pointsGained = Math.round(pointsGained * 1.2f);
				break;
			}
		}
		int winnerAbyssRank = maxRank;
		int defeatedAbyssRank = defeated.getAbyssRank().getRank().getId();
		int abyssRankDifference = winnerAbyssRank - defeatedAbyssRank;
		if (winnerAbyssRank <= 7 && abyssRankDifference > 0) {
			float penaltyPercent = abyssRankDifference * 0.05f;
			pointsGained -= Math.round(pointsGained * penaltyPercent);
		}
		return pointsGained;
	}

	/**
	 * 计算 PvP 击杀获得的 DP
	 * Calculate DP gained from PvP kill
	 *
	 * @param defeated 被击败玩家 / Defeated player
	 * @param maxRank 击杀方最高军衔 / Winner max rank
	 * @param maxLevel 击杀方最高等级 / Winner max level
	 * DP gained
	 */
	public static int calculatePvpDpGained(Player defeated, int maxRank, int maxLevel) {
		int pointsGained = 0;
		int baseDp = 1064;
		int dpPerRank = 57;
		pointsGained = (defeated.getAbyssRank().getRank().getId() - maxRank) * dpPerRank + baseDp;
		pointsGained = StatFunctions.adjustPvpDpGained(pointsGained, defeated.getLevel(), maxLevel);
		return pointsGained;
	}

	/**
	 * 按等级差调整 PvP DP 奖励
	 * Adjust PvP DP reward by level difference
	 *
	 * Base points
	 * @param defeatedLvl 被击败等级 / Defeated level
	 * @param killerLvl 击杀者等级 / Killer level
	 * Adjusted DP
	 */
	public static int adjustPvpDpGained(int points, int defeatedLvl, int killerLvl) {
		int pointsGained = points;
		int difference = killerLvl - defeatedLvl;
		if (difference >= 10) {
			pointsGained = 0;
		} else if (difference < 10 && difference >= 0) {
			pointsGained -= pointsGained * difference * 0.1;
		} else if (difference <= -10) {
			pointsGained *= 1.1;
		} else if (difference > -10 && difference < 0) {
			pointsGained += pointsGained * Math.abs(difference) * 0.01;
		}
		return pointsGained;
	}

	/**
	 * 计算队伍击杀目标的 DP 奖励
	 * Calculate group DP reward from target
	 *
	 * 玩家 / Player
	 * Target
	 * Group DP reward
	 */
	public static int calculateGroupDPReward(Player player, Creature target) {
		int playerLevel = player.getCommonData().getLevel();
		int targetLevel = target.getLevel();
		NpcRating npcRating = ((Npc) target).getObjectTemplate().getRating();
		int baseDP = targetLevel * calculateRatingMultipler(npcRating);
		int xpPercentage = XPRewardEnum.xpRewardFrom(targetLevel - playerLevel);
		float rate = player.getRates().getDpNpcRate();
		return (int) Math.floor(baseDP * xpPercentage * rate / 100);
	}

	/**
	 * 按 BOOST_HATE 属性计算仇恨（当前主要用于技能）
	 * Calculate hate based on BOOST_HATE (currently used mainly from skills)
	 *
	 * Creature
	 * @param value 基础仇恨 / Base hate value
	 * Final hate
	 */
	public static int calculateHate(Creature creature, int value) {
		Stat2 stat = new AdditionStat(StatEnum.BOOST_HATE, value, creature, 0.1f);
		return (int) (creature.getGameStats().getStat(StatEnum.BOOST_HATE, stat).getCurrent());
	}

	/**
	 * 计算一次攻击的主/副手伤害结果列表
	 * Calculate main/off-hand attack damage results for one attack
	 *
	 * Attacker
	 * Skill element
	 * Attack status
	 * @param calculationTypes 计算类型标记 / Calculation type flags
	 * @return 攻击结果列表 / Attack result list
	 */
	public static List<AttackResult> calculateAttackDamage(Creature attacker, SkillElement element, AttackStatus status,
			CalculationType... calculationTypes) {
		List<AttackResult> attackResultList = new ArrayList<AttackResult>();
		AttackStatus baseStatus = AttackStatus.getBaseStatus(status);
		if (baseStatus == AttackStatus.DODGE || baseStatus == AttackStatus.RESIST) {
			attackResultList.add(new AttackResult(0, baseStatus));
			return attackResultList;
		}

		Stat2 mainHandAttack;
		Stat2 offHandAttack = null;
		HitType hitType = HitType.PHHIT;
		if (element == SkillElement.NONE) {
			mainHandAttack = attacker.getGameStats().getMainHandPAttack(calculationTypes);
			if (attacker instanceof Player) {
				offHandAttack = ((Player) attacker).getGameStats().getOffHandPAttack(calculationTypes);
			}
		} else {
			hitType = HitType.MAHIT;
			mainHandAttack = attacker.getGameStats().getMainHandMAttack(calculationTypes);
			if (attacker instanceof Player) {
				offHandAttack = ((Player) attacker).getGameStats().getOffHandMAttack(calculationTypes);
			}
		}

		if (attacker instanceof Player) {
			Player player = (Player) attacker;
			Equipment equipment = player.getEquipment();
			Item mainHandWeapon = equipment.getMainHandWeapon();
			if (mainHandWeapon != null) {
				Item offHandWeapon = equipment.getOffHandWeapon();
				WeaponStats mainWeaponStats = mainHandWeapon.getItemTemplate().getWeaponStats();
				WeaponStats offWeaponStats = offHandWeapon == null || offHandWeapon == mainHandWeapon
						|| offHandWeapon.getItemTemplate().getArmorType() == ArmorType.SHIELD ? null : offHandWeapon.getItemTemplate().getWeaponStats();
				if (mainWeaponStats != null) {
					float mainHandDamage = mainHandAttack.getExactCurrent();
					float offHandDamage = offHandAttack == null ? 0 : offHandAttack.getExactCurrent();
					if (ArrayUtils.contains(calculationTypes, CalculationType.SKILL)) {
						if (offWeaponStats != null) {
							float totalBaseDamage = (offHandAttack.getExactBaseWithoutBaseRate() * player.getGameStats().getSkillEfficiency()
									+ mainHandAttack.getExactBaseWithoutBaseRate()) * 0.8f;
							mainHandDamage = (mainHandAttack.getExactCurrentWithoutFixedBonus() + totalBaseDamage * offHandAttack.getFixedBonusRate()) * 0.8f;
							offHandDamage = (offHandAttack.getExactCurrentWithoutFixedBonus() + totalBaseDamage * mainHandAttack.getFixedBonusRate()) * 0.8f
									* player.getGameStats().getSkillEfficiency();
						}
					} else if (offWeaponStats != null && Rnd.get(0, 999) >= player.getGameStats().getMaxDamageChance()) {
						offHandDamage *= player.getGameStats().getMinDamageRatio();
						if (offHandDamage <= 0) {
							offHandDamage = 1;
						}
					}
					attackResultList.add(new AttackResult(mainHandDamage, status, hitType));
					if (offWeaponStats != null) {
						attackResultList.add(new AttackResult(offHandDamage, AttackStatus.getOffHandStats(status), hitType));
					}
				}
			} else {
				float damage = Rnd.get(16, 20) * (1 + ((player.getGameStats().getPower().getCurrent() - 100) * 0.7f) / 100f)
						+ mainHandAttack.getExactBonus();
				attackResultList.add(new AttackResult(damage, status, hitType));
			}
		} else {
			if (attacker instanceof Npc npc && npc.getObjectTemplate() != null
					&& npc.getObjectTemplate().getStatsTemplate() != null
					&& npc.getObjectTemplate().getStatsTemplate().hasRetailDamageRange()) {
				int minDamage = npc.getObjectTemplate().getStatsTemplate().getMinDamage();
				int maxDamage = npc.getObjectTemplate().getStatsTemplate().getMaxDamage();
				attackResultList.add(new AttackResult(scaleNpcAttackDamage(rollNpcAttackDamage(minDamage, maxDamage), minDamage,
						maxDamage, mainHandAttack.getExactCurrent()), status, hitType));
			} else {
				int val = attacker instanceof Homing ? 100 : Rnd.get(80, 120);
				attackResultList.add(new AttackResult(mainHandAttack.getExactCurrent() * val / 100f, status, hitType));
			}
		}
		return attackResultList;
	}

	public static int rollNpcAttackDamage(int minDamage, int maxDamage) {
		return Rnd.get(Math.min(minDamage, maxDamage), Math.max(minDamage, maxDamage));
	}

	/** 保留真实随机区间，同时应用 NPC 当前攻击属性修正。 / Preserves the retail roll while applying current attack modifiers. */
	static float scaleNpcAttackDamage(int rolledDamage, int minDamage, int maxDamage, float currentAttack) {
		int rangeTotal = minDamage + maxDamage;
		return rangeTotal == 0 ? rolledDamage : rolledDamage * currentAttack / (rangeTotal * 0.5f);
	}

	/**
	 * 计算魔法技能伤害
	 * Calculate magical skill damage
	 *
	 * Speller
	 * Target
	 * Base damages
	 * @param bonus 额外加成 / Bonus damage
	 * Skill element
	 * @param useMagicBoost 是否使用魔增 / Whether to use magic boost
	 * @param useKnowledge 是否使用智力 / Whether to use knowledge
	 * @param noReduce 是否跳过减伤 / Whether to skip reductions
	 * PvP damage parameter
	 * @return 魔法技能伤害 / Magical skill damage
	 */
	public static int calculateMagicalSkillDamage(Creature speller, Creature target, int baseDamages, int bonus,
			SkillElement element, boolean useMagicBoost, boolean useKnowledge, boolean noReduce,
			boolean useMagicalDefense, int pvpDamage) {
		return calculateMagicalSkillDamage(speller, target, baseDamages, bonus, element, useMagicBoost, useKnowledge,
				noReduce, useMagicalDefense, pvpDamage, true, 1f, 0);
	}

	public static int calculateMagicalSkillDamage(Creature speller, Creature target, int baseDamages, int bonus,
			SkillElement element, boolean useMagicBoost, boolean useKnowledge, boolean noReduce,
			boolean useMagicalDefense, int pvpDamage, boolean randomizeNpcDamage, float skillDamageMultiplier,
			int flatSkillDamage) {
		CreatureGameStats<?> sgs = speller.getGameStats();
		CreatureGameStats<?> tgs = target.getGameStats();
		float damages = baseDamages;
		if (!noReduce) {
			int magicBoost = useMagicBoost ? sgs.getMBoost().getCurrent() : 0;
			if (!(speller instanceof Trap)) {
				magicBoost -= tgs.getMBResist().getCurrent();
			}
			magicBoost = scaleMagicBoostDifference(magicBoost, target.getStatRatio());
			int knowledge = useKnowledge ? sgs.getKnowledge().getCurrent() : 100;
			damages *= calculateMagicalSkillDamageFactor(magicBoost, knowledge);
			damages = sgs.getStat(StatEnum.BOOST_SPELL_ATTACK, (int) damages).getCurrent();
		}
		damages += bonus;
		damages = applyOneTimeSkillAttack(damages, flatSkillDamage, skillDamageMultiplier);
		if (!noReduce) {
			damages = applyPveLevelPenalty(speller, target, damages);
			damages = applyMagicalDefenseModifiers(speller, target, damages, element, useMagicalDefense);
		}
		if (damages < 0) {
			damages = 0;
		} else if (randomizeNpcDamage && speller instanceof Npc && !(speller instanceof SummonedObject)) {
			int rnd = (int) (damages * 0.08f);
			damages += Rnd.get(-rnd, rnd);
		}
		return Math.round(damages);
	}

	public static int calculateMagicalOverTimeSkillDamage(Creature speller, Creature target, int baseDamage,
			SkillElement element, boolean useMagicBoost, boolean useMagicalDefense) {
		CreatureGameStats<?> sgs = speller.getGameStats();
		CreatureGameStats<?> tgs = target.getGameStats();
		float damage = baseDamage;
		if (useMagicBoost) {
			int magicBoost = sgs.getMBoost().getCurrent();
			if (!(speller instanceof Trap)) {
				magicBoost -= tgs.getMBResist().getCurrent();
			}
			damage *= calculateMagicalSkillDamageFactor(scaleMagicBoostDifference(magicBoost, target.getStatRatio()), 100);
			damage = sgs.getStat(StatEnum.BOOST_SPELL_ATTACK, (int) damage).getCurrent();
		}
		float magicalDefense = useMagicalDefense ? tgs.getMDef().getCurrent() : 0;
		float elementalDefense = 0;
		if (element != SkillElement.NONE) {
			elementalDefense = tgs.getMagicalDefenseFor(element) / speller.getStatRatio();
			elementalDefense = applyElementalDefenseLowerCap(elementalDefense, target instanceof Player, target.getLevel());
		}
		return Math.round(Math.max(0, applyMagicalDefenseModifiers(damage, magicalDefense, elementalDefense,
				getElementalDefenseDenominator(speller, target))));
	}

	static int scaleMagicBoostDifference(int magicBoostDifference, float targetStatRatio) {
		return (int) (capMagicBoostForDamage(magicBoostDifference) / Math.max(1f, targetStatRatio));
	}

	static int applyOneTimeSkillAttack(float damage, int flatDamage, float multiplier) {
		return Math.round(damage * multiplier) + flatDamage;
	}

	static int capMagicBoostForDamage(int magicBoost) {
		if (magicBoost < 0) {
			return 0;
		}
		return Math.min(magicBoost, SkillConfig.MAGICBOOST_CAP);
	}

	static float calculateMagicalSkillDamageFactor(int magicBoost, int knowledge) {
		return knowledge / 100f + capMagicBoostForDamage(magicBoost) / 1000f;
	}

	public static float applyMagicalDefenseModifiers(Creature attacker, Creature target, float damage,
			SkillElement element, boolean useMagicalDefense) {
		float magicalDefense = 0;
		if (useMagicalDefense) {
			magicalDefense = target.getGameStats().getMDef().getBonus()
					+ getMovementModifier(target, StatEnum.MAGICAL_DEFEND, target.getGameStats().getMDef().getBase());
		}
		float elementalDefense = 0;
		if (element != SkillElement.NONE) {
			elementalDefense = getMovementModifier(target, SkillElement.getResistanceForElement(element),
					target.getGameStats().getMagicalDefenseFor(element));
			elementalDefense /= attacker.getStatRatio();
			elementalDefense = applyElementalDefenseLowerCap(elementalDefense, target instanceof Player, target.getLevel());
		}
		return applyMagicalDefenseModifiers(damage, magicalDefense, elementalDefense,
				getElementalDefenseDenominator(attacker, target));
	}

	static float applyMagicalDefenseModifiers(float damage, float magicalDefense, float elementalDefense,
			float elementalDefenseDenominator) {
		damage -= magicalDefense * 0.1f;
		return damage * (1 - elementalDefense / elementalDefenseDenominator);
	}

	/** 应用真实物理防御与对象系数。 / Applies retail physical defense and object coefficient. */
	public static float applyPhysicalDefenseModifiers(float damage, float physicalDefense, float statRatio) {
		return (damage - physicalDefense * 0.1f) / Math.max(1f, statRatio);
	}

	/**
	 * 应用全局伤害倍率配置
	 * Apply global damage multiplier from rate config
	 *
	 * Raw damage
	 *
	 * @param damage
	 * @return 缩放后伤害 / Scaled damage
	 */
	public static int applyDamageMultiplier(int damage) {
		return Math.round(damage * RateConfig.DAMAGE_MULTIPLIER);
	}

	private static int getElementalDefenseDenominator(Creature attacker, Creature target) {
		if (target instanceof Player) {
			return 1300 + Math.max(0, Math.min(attacker.getLevel(), target.getLevel()) - 50) * 10;
		}
		return 1300;
	}

	static float applyElementalDefenseLowerCap(float elementalDefense, boolean playerTarget, int level) {
		int lowerCap = playerTarget ? -Math.max(1000, 500 + Math.max(0, level) * 10) : -1300;
		return Math.max(elementalDefense, lowerCap);
	}

	/**
	 * 计算魔法暴击是否触发
	 * Calculate whether a magical critical hit occurs
	 *
	 * Attacker
	 * Attacked
	 * @param criticalProb 暴击概率修正 / Critical probability modifier
	 * Whether critical
	 */
	public static boolean calculateMagicalCriticalRate(Creature attacker, Creature attacked, int criticalProb) {
		return calculateMagicalCriticalRate(attacker, attacked, criticalProb, true);
	}

	/**
	 * 计算魔法暴击是否触发（可关闭魔暴）
	 * Calculate whether a magical critical hit occurs (optional mcrit apply)
	 *
	 * Attacker
	 * Attacked
	 * @param criticalProb 暴击概率修正 / Critical probability modifier
	 * @param applyMcrit 是否应用魔法暴击 / Whether to apply magical crit
	 * Whether critical
	 */
	public static boolean calculateMagicalCriticalRate(Creature attacker, Creature attacked, int criticalProb, boolean applyMcrit) {
		if (!applyMcrit) {
			return false;
		}
		if (attacker instanceof Servant || attacker instanceof Homing) {
			return false;
		}

		int critical = calculateEffectiveMagicalCritical(attacker.getGameStats().getMCritical().getCurrent(),
				attacked.getGameStats().getSpellResist().getCurrent(),
				attacked.getGameStats().getStat(StatEnum.MAGICAL_CRITICAL_REDUCE_RATE, 0).getCurrent(), criticalProb,
				attacked.isInState(CreatureState.RESTING), attacked.getStatRatio());
		return Rnd.nextInt(1000) < critical;
	}

	static int calculateEffectiveMagicalCritical(int critical, int spellResist, int criticalReduceRate, int criticalProb,
			boolean resting, float defenderStatRatio) {
		return calculateEffectiveCritical(critical, spellResist, criticalReduceRate, criticalProb, resting, defenderStatRatio);
	}

	/**
	 * 按 NPC 评级返回 DP 倍率
	 * Return DP multiplier by NPC rating
	 *
	 * NPC rating
	 * Multiplier
	 */
	public static int calculateRatingMultipler(NpcRating npcRating) {
		// 兼容回退：正式服按 NPC 存 DP，当前模板未暴露。 / Compatibility fallback: retail stores DP per NPC, which current templates do not expose.
		int multipler;
		switch (npcRating) {
		case JUNK:
			multipler = 1;
			break;
		case NORMAL:
			multipler = 2;
			break;
		case ELITE:
			multipler = 3;
			break;
		case HERO:
			multipler = 4;
			break;
		case LEGENDARY:
			multipler = 5;
			break;
		default:
			multipler = 1;
		}
		return multipler;
	}

	/**
	 * 按 NPC 评级返回 AP 倍率
	 * Return AP multiplier by NPC rating
	 *
	 * NPC rating
	 * AP multiplier
	 */
	public static int ApNpcRating(NpcRating npcRating) {
		int multipler;
		switch (npcRating) {
		case JUNK:
			multipler = 1;
			break;
		case NORMAL:
			multipler = 2;
			break;
		case ELITE:
			multipler = 4;
			break;
		case HERO:
			multipler = 5;
			break;
		case LEGENDARY:
			multipler = 6;
			break;
		default:
			multipler = 1;
		}
		return multipler;
	}

	/**
	 * 按等级差与 PvP/PvE 比率调整伤害
	 * Adjust damage by level difference and PvP/PvE ratios
	 *
	 * Attacker
	 * Target
	 * Base damages
	 * PvP damage parameter
	 * @param useMovement 是否应用移动修正 / Whether to apply movement modifier
	 * @return 调整后伤害 / Adjusted damage
	 */
	public static float adjustDamages(Creature attacker, Creature target, float damages, int pvpDamage,
			boolean useMovement) {
		return adjustDamages(attacker, target, damages, pvpDamage, useMovement, SkillElement.NONE, HitType.PHHIT);
	}

	/**
	 * 按等级差、PvP/PvE 比率与元素调整伤害
	 * Adjust damage by level difference, PvP/PvE ratios and element
	 *
	 * Attacker
	 * Target
	 * Base damages
	 * PvP damage parameter
	 * @param useMovement 是否应用移动修正 / Whether to apply movement modifier
	 * Skill element
	 * @return 调整后伤害 / Adjusted damage
	 */
	public static float adjustDamages(Creature attacker, Creature target, float damages, int pvpDamage,
			boolean useMovement, SkillElement element) {
		if (element == null) {
			element = SkillElement.NONE;
		}
		HitType hitType = element == SkillElement.NONE ? HitType.PHHIT : HitType.MAHIT;
		return adjustDamages(attacker, target, damages, pvpDamage, useMovement, element, hitType);
	}

	public static float adjustDamages(Creature attacker, Creature target, float damages, int pvpDamage,
			boolean useMovement, SkillElement element, HitType hitType) {
		if (element == null) {
			element = SkillElement.NONE;
		}
		if (attacker instanceof Npc && ((Npc) attacker).getAbyssNpcType() == AbyssNpcType.ARTIFACT) {
			return damages;
		}

		if (attacker.isPvpTarget(target)) {
			boolean magical = hitType == HitType.MAHIT;
			int pvpAttackBonus = getPvpRatio(attacker, StatEnum.PVP_ATTACK_RATIO,
					magical ? StatEnum.PVP_ATTACK_RATIO_MAGICAL : StatEnum.PVP_ATTACK_RATIO_PHYSICAL);
			int pvpDefenceBonus = getPvpRatio(target, StatEnum.PVP_DEFEND_RATIO,
					magical ? StatEnum.PVP_DEFEND_RATIO_MAGICAL : StatEnum.PVP_DEFEND_RATIO_PHYSICAL);
			damages = applyPvpDamageModifiers(damages, pvpDamage, pvpAttackBonus, pvpDefenceBonus);
		} else {
			int pveAttackBonus = attacker.getGameStats().getStat(StatEnum.PVE_ATTACK_RATIO, 0).getCurrent();
			int pveDefenceBonus = target.getGameStats().getStat(StatEnum.PVE_DEFEND_RATIO, 0).getCurrent();
			switch (hitType) {
			case PHHIT:
				pveAttackBonus += attacker.getGameStats().getStat(StatEnum.PVE_ATTACK_RATIO_PHYSICAL, 0).getCurrent();
				pveDefenceBonus += target.getGameStats().getStat(StatEnum.PVE_DEFEND_RATIO_PHYSICAL, 0).getCurrent();
				break;
			case MAHIT:
				pveAttackBonus += attacker.getGameStats().getStat(StatEnum.PVE_ATTACK_RATIO_MAGICAL, 0).getCurrent();
				pveDefenceBonus += target.getGameStats().getStat(StatEnum.PVE_DEFEND_RATIO_MAGICAL, 0).getCurrent();
				break;
				default:
					break;
			}
			damages = applyPveDamageModifiers(damages, pveAttackBonus, pveDefenceBonus);
		}
		if (useMovement) {
			damages = movementDamageBonus(attacker, damages);
		}
		return damages;
	}

	/**
	 * 真实在玩家攻击带有 limitAttr 的 NPC 时追加一次伤害。
	 * Retail adds an additional damage portion for a player's matching limit attribute.
	 */
	public static float applyLimitAttributeBonus(Creature attacker, Creature target, float finalDamage, float baseDamage) {
		if (finalDamage <= 0 || baseDamage <= 0 || !(attacker instanceof Player) || !(target instanceof Npc npc)
				|| npc.getObjectTemplate() == null || npc.getObjectTemplate().getStatsTemplate() == null) {
			return finalDamage;
		}
		StatEnum limitAttribute = getPveAttackRatioStat(npc.getRace());
		if (limitAttribute == null) {
			return finalDamage;
		}
		int value = attacker.getGameStats().getStat(limitAttribute, 0).getCurrent();
		return applyLimitAttributeBonus(finalDamage, baseDamage, value,
				npc.getObjectTemplate().getStatsTemplate().getLimitAttributeReduceValue());
	}

	static float applyLimitAttributeBonus(float finalDamage, float baseDamage, int value, int reduceValue) {
		int effectiveValue = value - reduceValue;
		return finalDamage > 0 && baseDamage > 0 && effectiveValue > 0
				? finalDamage + baseDamage * effectiveValue / 1000f : finalDamage;
	}

	static int getPvpRatio(Creature creature, StatEnum generalStat, StatEnum typedStat) {
		Integer absoluteValue = creature.getGameStats().getSetStatValue(typedStat);
		if (absoluteValue != null) {
			return absoluteValue;
		}
		return creature.getGameStats().getStat(generalStat, 0).getCurrent()
				+ creature.getGameStats().getStat(typedStat, 0).getCurrent();
	}

	static float applyPvpDamageModifiers(float damage, int pvpDamage, int attackBonus, int defenceBonus) {
		if (pvpDamage > 0) {
			damage *= pvpDamage * 0.01f;
		}
		attackBonus = StatCapUtil.limitValueForPvpOrPveStat(CombatMode.PVP, RatioType.ATTACK, attackBonus);
		defenceBonus = StatCapUtil.limitValueForPvpOrPveStat(CombatMode.PVP, RatioType.DEFENSE, defenceBonus);
		float ratio = 1f + (attackBonus - defenceBonus) / 1000f;
		return damage * 0.42f * Math.max(0.1f, Math.min(1.5f, ratio));
	}

	static float applyPveDamageModifiers(float damage, int attackBonus, int defenceBonus) {
		attackBonus = StatCapUtil.limitValueForPvpOrPveStat(CombatMode.PVE, RatioType.ATTACK, attackBonus);
		defenceBonus = StatCapUtil.limitValueForPvpOrPveStat(CombatMode.PVE, RatioType.DEFENSE, defenceBonus);
		float ratio = 1f + attackBonus / 1000f - defenceBonus / 1000f;
		return damage * Math.max(0.1f, Math.min(12f, ratio));
	}

	public static float applyPveLevelPenalty(Creature attacker, Creature target, float damage) {
		if (attacker.isPvpTarget(target) || !(attacker instanceof Player player) || !(target instanceof Npc)) {
			return damage;
		}
		return damage * getNpcDamageFactor(target.getLevel() - attacker.getLevel(), player.isArchDaeva());
	}

	static float getNpcDamageFactor(int levelDiff, boolean archDaeva) {
		int graceLevels = archDaeva ? 3 : 2;
		return Math.max(0, 1f - Math.max(0, levelDiff - graceLevels) * 0.1f);
	}

	static StatEnum getPveAttackRatioStat(Race race) {
		return switch (race) {
			case TYPE_A -> StatEnum.PVE_ATTACK_RATIO_TYPE_A;
			case TYPE_B -> StatEnum.PVE_ATTACK_RATIO_TYPE_B;
			case TYPE_C -> StatEnum.PVE_ATTACK_RATIO_TYPE_C;
			case TYPE_D -> StatEnum.PVE_ATTACK_RATIO_TYPE_D;
			case TYPE_E -> StatEnum.PVE_ATTACK_RATIO_TYPE_E;
			default -> null;
		};
	}

	/**
	 * 计算物理闪避是否触发
	 * Calculate whether a physical dodge occurs
	 *
	 * Attacker
	 * Attacked
	 * Accuracy modifier
	 * Whether dodged
	 */
	public static boolean calculatePhysicalDodgeRate(Creature attacker, Creature attacked, int accMod) {
		return calculatePhysicalDodgeRate(attacker, attacked, accMod, true);
	}

	public static boolean calculatePhysicalDodgeRate(Creature attacker, Creature attacked, int accMod,
			boolean isMainHand) {
		if (attacker.getObserveController().hasAlwaysHit()) {
			return false;
		}
		// 检查攻击者是否目盲 / check if attacker is blinded
		if (attacker.getObserveController().checkAttackerStatus(AttackStatus.DODGE)) {
			return true;
		}
		// 始终检查闪避 / check always dodge
		if (attacked.getObserveController().checkAttackStatus(AttackStatus.DODGE)) {
			return true;
		}
		if (attacked.isInState(CreatureState.RESTING)) {
			return false;
		}

		float accuracy = getPhysicalAccuracy(attacker, isMainHand);
		float dodge = attacked.getGameStats().getEvasion().getCurrent();
		if (isPlayerPvp(attacker, attacked)) {
			accuracy += attacker.getGameStats().getStat(StatEnum.PVP_HIT_ACCURACY, 0).getCurrent();
			dodge += attacked.getGameStats().getStat(StatEnum.PVP_DODGE, 0).getCurrent();
		}
		boolean archDaevaAttacker = attacker instanceof Player player && player.isArchDaeva();
		if (attacked instanceof Npc) {
			if (!archDaevaAttacker) {
				dodge = applyNpcAvoidanceLevelBonus(dodge, attacked.getLevel() - attacker.getLevel(), 0.1f);
			}

			// 静态 NPC 永不闪避 / static npcs never dodge
			if (((Npc) attacked).hasEntity()) {
				return false;
			}
		}
		dodge += getMovementModifier(attacked, StatEnum.EVASION, attacked.getGameStats().getEvasion().getBase())
				- attacked.getGameStats().getEvasion().getBase();
		int dodgeRate = calculateAvoidanceRate(dodge, accuracy, accMod, attacker.getStatRatio(), 300);
		if (archDaevaAttacker && attacked instanceof Npc) {
			dodgeRate = Math.max(0, Math.min(900, dodgeRate + calculateLevelResistModifier(attacker.getLevel(),
					attacked.getLevel(), true, false)));
		}
		return Rnd.nextInt(1000) < dodgeRate;
	}

	/**
	 * 计算物理招架是否触发
	 * Calculate whether a physical parry occurs
	 *
	 * Attacker
	 * Attacked
	 * Whether parried
	 */
	public static boolean calculatePhysicalParryRate(Creature attacker, Creature attacked) {
		return calculatePhysicalParryRate(attacker, attacked, 0);
	}

	/**
	 * 计算物理招架是否触发（带命中修正）
	 * Calculate whether a physical parry occurs with accuracy modifier
	 *
	 * Attacker
	 * Attacked
	 * Accuracy modifier
	 * Whether parried
	 */
	public static boolean calculatePhysicalParryRate(Creature attacker, Creature attacked, int accMod) {
		return calculatePhysicalParryRate(attacker, attacked, accMod, true);
	}

	public static boolean calculatePhysicalParryRate(Creature attacker, Creature attacked, int accMod,
			boolean isMainHand) {
		if (attacker.getObserveController().hasAlwaysHit()) {
			return false;
		}
		// 始终检查招架 / check always parry
		if (attacked.getObserveController().checkAttackStatus(AttackStatus.PARRY)) {
			return true;
		}
		float accuracy = getPhysicalAccuracy(attacker, isMainHand);
		float parry = attacked.getGameStats().getParry().getCurrent();
		if (isPlayerPvp(attacker, attacked)) {
			accuracy += attacker.getGameStats().getStat(StatEnum.PVP_HIT_ACCURACY, 0).getCurrent();
			parry += attacked.getGameStats().getStat(StatEnum.PVP_PARRY, 0).getCurrent();
		}
		if (attacked instanceof Npc) {
			parry = applyNpcAvoidanceLevelBonus(parry, attacked.getLevel() - attacker.getLevel(), 0.2f);
		}
		parry += getMovementModifier(attacked, StatEnum.PARRY, attacked.getGameStats().getParry().getBase())
				- attacked.getGameStats().getParry().getBase();
		return calculatePhysicalEvasion(parry, accuracy, accMod, attacker.getStatRatio(), 400);
	}

	/**
	 * 计算物理格挡是否触发
	 * Calculate whether a physical block occurs
	 *
	 * Attacker
	 * Attacked
	 * Whether blocked
	 */
	public static boolean calculatePhysicalBlockRate(Creature attacker, Creature attacked) {
		return calculatePhysicalBlockRate(attacker, attacked, 0);
	}

	/**
	 * 计算物理格挡是否触发（带命中修正）
	 * Calculate whether a physical block occurs with accuracy modifier
	 *
	 * Attacker
	 * Attacked
	 * Accuracy modifier
	 * Whether blocked
	 */
	public static boolean calculatePhysicalBlockRate(Creature attacker, Creature attacked, int accMod) {
		return calculatePhysicalBlockRate(attacker, attacked, accMod, true);
	}

	public static boolean calculatePhysicalBlockRate(Creature attacker, Creature attacked, int accMod,
			boolean isMainHand) {
		if (attacker.getObserveController().hasAlwaysHit()) {
			return false;
		}
		if (attacked.getObserveController().checkAttackStatus(AttackStatus.BLOCK)) {
			return true;
		}
		float accuracy = getPhysicalAccuracy(attacker, isMainHand);
		float block = attacked.getGameStats().getBlock().getCurrent();
		if (isPlayerPvp(attacker, attacked)) {
			accuracy += attacker.getGameStats().getStat(StatEnum.PVP_HIT_ACCURACY, 0).getCurrent();
			block += attacked.getGameStats().getStat(StatEnum.PVP_BLOCK, 0).getCurrent();
		}
		if (attacked instanceof Npc) {
			block = applyNpcAvoidanceLevelBonus(block, attacked.getLevel() - attacker.getLevel(), 0.2f);
		}
		block += getMovementModifier(attacked, StatEnum.BLOCK, attacked.getGameStats().getBlock().getBase())
				- attacked.getGameStats().getBlock().getBase();
		return calculatePhysicalEvasion(block, accuracy, accMod, attacker.getStatRatio(), 500);
	}

	static float applyNpcAvoidanceLevelBonus(float defense, int levelDifference, float bonusPerLevel) {
		return defense * (1 + Math.max(0, levelDifference - 2) * bonusPerLevel);
	}

	private static float getPhysicalAccuracy(Creature attacker, boolean isMainHand) {
		if (attacker instanceof Player && !isMainHand) {
			return ((PlayerGameStats) attacker.getGameStats()).getOffHandPAccuracy().getCurrent();
		}
		return attacker.getGameStats().getMainHandPAccuracy().getCurrent();
	}

	private static boolean isPlayerPvp(Creature attacker, Creature attacked) {
		return attacker instanceof Player && attacked instanceof Player;
	}

	/**
	 * 根据防御-命中差值与上限判定是否闪避/招架/格挡
	 * Resolve dodge/parry/block success from defense-accuracy difference and upper cap
	 *
	 * Defense value
	 * Accuracy value
	 * Accuracy modifier
	 * Attacker stat ratio
	 * Probability upper cap
	 *
	 * @return 是否成功规避 / Whether avoidance succeeds
	 */
	public static boolean calculatePhysicalEvasion(float defense, float accuracy, int accMod, float attackerStatRatio,
			int upperCap) {
		return Rnd.nextInt(1000) < calculateAvoidanceRate(defense, accuracy, accMod, attackerStatRatio, upperCap);
	}

	static int calculateAvoidanceRate(float defense, float accuracy, int accMod, float attackerStatRatio, int upperCap) {
		return Math.max(0, Math.min(upperCap, (int) ((defense - accuracy - accMod) / attackerStatRatio)));
	}

	/**
	 * 计算物理暴击是否触发
	 * Calculate whether a physical critical hit occurs
	 *
	 * Attacker
	 * Attacked
	 * Whether main hand
	 * @param criticalProb 暴击概率修正 / Critical probability modifier
	 * @param isSkill 是否技能攻击 / Whether skill attack
	 * Whether critical
	 */
	public static boolean calculatePhysicalCriticalRate(Creature attacker, Creature attacked, boolean isMainHand,
			int criticalProb, boolean isSkill) {
		if (attacker instanceof Servant || attacker instanceof Homing) {
			return false;
		}
		int critical;
		if (attacker instanceof Player && !isMainHand) {
			critical = ((PlayerGameStats) attacker.getGameStats()).getOffHandPCritical().getCurrent();
		} else {
			critical = attacker.getGameStats().getMainHandPCritical().getCurrent();
		}
		AttackerCriticalStatus acStatus = attacker.getObserveController()
				.checkAttackerCriticalStatus(AttackStatus.CRITICAL, isSkill);
		if (acStatus.isResult()) {
			if (acStatus.isPercent()) {
				critical *= 1 + acStatus.getValue() / 100f;
			} else {
				return Rnd.nextInt(1000) < acStatus.getValue();
			}
		}
		int criticalRate = calculateEffectiveCritical(critical, attacked.getGameStats().getStrikeResist().getCurrent(),
				attacked.getGameStats().getStat(StatEnum.PHYSICAL_CRITICAL_REDUCE_RATE, 0).getCurrent(), criticalProb,
				attacked.isInState(CreatureState.RESTING), attacked.getStatRatio());
		return Rnd.nextInt(1000) < criticalRate;
	}

	static int calculateEffectiveCritical(int critical, int criticalResist, int criticalReduceRate, int criticalProb,
			boolean resting, float defenderStatRatio) {
		int adjustedCritical = (int) (critical * criticalProb / 100f) + (resting ? 500 : 0);
		return Math.max(0, Math.min(500,
				(int) ((adjustedCritical - criticalResist - criticalReduceRate) / defenderStatRatio)));
	}

	/**
	 * 计算魔法抗性概率
	 * Calculate magical resist rate
	 *
	 * Attacker
	 * Attacked
	 * Accuracy modifier
	 * @return 抗性概率值 / Resist rate value
	 */
	public static int calculateMagicalResistRate(Creature attacker, Creature attacked, int accMod) {
		return calculateMagicalResistRate(attacker, attacked, accMod, SkillElement.NONE);
	}

	/**
	 * 计算指定元素的魔法抗性概率
	 * Calculate magical resist rate for a specific element
	 *
	 * Attacker
	 * Attacked
	 * Accuracy modifier
	 * Skill element
	 * @return 抗性概率值 / Resist rate value
	 */
	public static int calculateMagicalResistRate(Creature attacker, Creature attacked, int accMod, SkillElement element) {
		if (attacker.getObserveController().hasAlwaysNoResist()) {
			return 0;
		}
		if (attacked.getObserveController().checkAttackStatus(AttackStatus.RESIST)) {
			return 1000;
		}
		if (element != SkillElement.NONE && attacked instanceof Summon summon
				&& element == summon.getAlwaysResistElement()) {
			return 1000;
		}

		int magicalResist = attacked.getGameStats().getMResist().getCurrent();
		int magicalAccuracy = attacker.getGameStats().getMAccuracy().getCurrent();
		if (isPlayerPvp(attacker, attacked)) {
			magicalResist += attacked.getGameStats().getStat(StatEnum.PVP_MAGICAL_RESIST, 0).getCurrent();
			magicalAccuracy += attacker.getGameStats().getStat(StatEnum.PVP_MAGICAL_HIT_ACCURACY, 0).getCurrent();
		}
		int resistRate = calculateMagicalResistDifference(magicalResist, magicalAccuracy, accMod,
				attacked.isInState(CreatureState.RESTING), attacker.getStatRatio());
		resistRate = Math.max(0, Math.min(500, resistRate));
		return Math.max(0, Math.min(900, resistRate + calculateLevelResistModifier(attacker.getLevel(),
				attacked.getLevel(), attacker instanceof Player player && player.isArchDaeva(), isPlayerPvp(attacker, attacked))));
	}

	static int calculateMagicalResistDifference(int magicalResist, int magicalAccuracy, int accMod, boolean resting,
			float attackerStatRatio) {
		int difference = magicalResist - magicalAccuracy - accMod;
		return (int) ((resting ? difference * 0.5f : difference) / attackerStatRatio);
	}

	static int calculateLevelResistModifier(int attackerLevel, int defenderLevel, boolean attackerArchDaeva,
			boolean playerPvp) {
		if (attackerArchDaeva && playerPvp) {
			return 0;
		}
		int levelDifference = defenderLevel - attackerLevel;
		int graceLevels = attackerArchDaeva ? 6 : 4;
		if (levelDifference > graceLevels) {
			return (levelDifference - graceLevels) * 100;
		}
		return levelDifference < -4 ? (levelDifference + 4) * 100 : 0;
	}

	/**
	 * 计算跌落伤害；返回是否强制回城
	 * Calculate fall damage; returns whether the player is forced to bind location
	 *
	 * 玩家 / Player
	 * Fall distance
	 * @param stoped 是否已停止 / Whether stopped
	 * @return 是否强制回城 / Whether forced to bind location
	 */
	public static boolean calculateFallDamage(Player player, float distance, boolean stoped) {
		if (player.isInvul()) {
			return false;
		}

		if (distance >= FallDamageConfig.MAXIMUM_DISTANCE_DAMAGE || !stoped) {
			player.getController().onStopMove();
			player.getFlyController().onStopGliding(false);
			player.getLifeStats().reduceHp(player.getLifeStats().getMaxHp() + 1, player);
			return true;
		}
		else if (distance >= FallDamageConfig.MINIMUM_DISTANCE_DAMAGE) {
			float dmgPerMeter = player.getLifeStats().getMaxHp() * FallDamageConfig.FALL_DAMAGE_PERCENTAGE / 100f;
			int damage = (int) (distance * dmgPerMeter);
			player.getLifeStats().reduceHp(damage, player);
			player.getObserveController().notifyAttackedObservers(player);
			PacketSendUtility.sendPacket(player, new SM_ATTACK_STATUS(player, player, SM_ATTACK_STATUS.TYPE.FALL_DAMAGE, 0, -damage));
		}

		return false;
	}

	/**
	 * 按移动朝向修正玩家属性（前进减防、侧移加闪避等）
	 * Modify player stats by movement heading (forward defense cut, side evasion bonus, etc.)
	 *
	 * Creature
	 *
	 * @param stat 属性枚举 / Stat enum
	 * @param value 原始属性值 / Original stat value
	 * @param value
	 * @return 修正后属性值 / Modified stat value
	 */
	public static float getMovementModifier(Creature creature, StatEnum stat, float value) {
		if (!(creature instanceof Player) || stat == null) {
			return value;
		}
		Player player = (Player) creature;
		int h = player.getMoveController().getMovementHeading();
		if (h < 0) {
			return value;
		}
		return applyMovementStatModifier(h, stat, value);
	}

	static float applyMovementStatModifier(int heading, StatEnum stat, float value) {
		return switch (stat) {
			case PHYSICAL_DEFENSE, MAGICAL_DEFEND -> heading == 0 ? value * 0.8f : value;
			case WATER_RESISTANCE, WIND_RESISTANCE, FIRE_RESISTANCE, EARTH_RESISTANCE,
					ELEMENTAL_RESISTANCE_DARK, ELEMENTAL_RESISTANCE_LIGHT -> heading == 0 ? value * 0.5f : value;
			case EVASION -> heading != 0 && heading != 4 ? value + 300 : value;
			case PARRY, BLOCK -> heading >= 3 && heading <= 5 ? value + 500 : value;
			case SPEED -> heading == 2 || heading == 6 ? value * 0.8f
					: heading >= 3 && heading <= 5 ? value * 0.6f : value;
			default -> value;
		};
	}

	private static float movementDamageBonus(Creature creature, float value) {
		if (!(creature instanceof Player)) {
			return value;
		}
		Player player = (Player) creature;
		int h = player.getMoveController().getMovementHeading();
		if (h < 0) {
			return value;
		}
		switch (h) {
		case 7:
		case 0:
		case 1:
			value = value * 1.1f;
			break;
		case 6:
		case 2:
			value -= value * 0.2f;
			break;
		case 5:
		case 4:
		case 3:
			value -= value * 0.2f;
			break;
		}
		return value;
	}

}

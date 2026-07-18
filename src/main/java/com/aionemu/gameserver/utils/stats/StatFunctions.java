package com.aionemu.gameserver.utils.stats;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

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
import com.aionemu.gameserver.model.PlayerClass;
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
import com.aionemu.gameserver.model.gameobjects.siege.SiegeNpc;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.siege.Influence;
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
import com.google.common.base.Preconditions;

/**
 * 核心战斗与属性计算工具：经验/DP/AP/GP 奖励、物理与魔法伤害、闪避/招架/格挡/暴击与跌落伤害
 * Core combat and stat math: XP/DP/AP/GP rewards, physical/magical damage, dodge/parry/block/crit and fall damage
 */
@Slf4j
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
			int val = attacker instanceof Homing ? 100 : Rnd.get(80, 120);
			attackResultList.add(new AttackResult(mainHandAttack.getExactCurrent() * val / 100f, status, hitType));
		}
		return attackResultList;
	}

	/**
	 * 计算物理普攻伤害（含目标防御）
	 * Calculate physical auto-attack damage including target defense
	 *
	 * Attacker
	 * Target
	 * Whether main hand
	 * @return 对目标造成的伤害 / Damage dealt to target
	 */
	public static int calculatePhysicalAttackDamage(Creature attacker, Creature target, boolean isMainHand) {
		Stat2 pAttack;
		if (isMainHand) {
			pAttack = attacker.getGameStats().getMainHandPAttack();
		} else {
			pAttack = ((Player) attacker).getGameStats().getOffHandPAttack();
		}
		float resultDamage = pAttack.getCurrent();
		float baseDamage = pAttack.getBase();
		if (attacker instanceof Player) {
			Equipment equipment = ((Player) attacker).getEquipment();
			Item weapon;
			if (isMainHand) {
				weapon = equipment.getMainHandWeapon();
			} else {
				weapon = equipment.getOffHandWeapon();
			}

			if (weapon != null) {
				WeaponStats weaponStat = weapon.getItemTemplate().getWeaponStats();
				if (weaponStat == null) {
					return 0;
				}
				int totalMin = weaponStat.getMinDamage();
				int totalMax = weaponStat.getMaxDamage();
				if (totalMax - totalMin < 1) {
					log.warn(I18n.get("log.bbf300e93b6a"));
					log.warn(I18n.get("log.9b25cf6c9dbb", String.valueOf(equipment.getMainHandWeapon().getItemTemplate().getTemplateId())));
					log.warn(I18n.get("log.c4ee9fd27bbf", String.valueOf(totalMin)));
					log.warn(I18n.get("log.c24cbde05206", String.valueOf(totalMax)));
				}
				float power = attacker.getGameStats().getPower().getCurrent() * 0.01f;
				int diff = Math.round((totalMax - totalMin) * power / 2);
				resultDamage = pAttack.getBonus() + baseDamage;
				// 按 WeaponDualEffect 数值调整 / adjust with value from WeaponDualEffect
				// 使伤害下限更低，副手伤害更随机。 / it makes lower cap of damage lower, so damage is more random on offhand
				int negativeDiff = diff;
				if (!isMainHand) {
					negativeDiff = (int) Math.round((200 - ((Player) attacker).getDualEffectValue()) * 0.01 * diff);
				}
				resultDamage += Rnd.get(-negativeDiff, diff);
				// 添加力量碎片伤害 / add powerShard damage
				if (attacker.isInState(CreatureState.POWERSHARD)) {
					Item firstShard;
					Item secondShard = null;
					if (isMainHand) {
						firstShard = equipment.getMainHandPowerShard();
						if (weapon.getItemTemplate().isTwoHandWeapon()) {
							secondShard = equipment.getOffHandPowerShard();
						}
					} else {
						firstShard = equipment.getOffHandPowerShard();
					}

					if (firstShard != null) {
						equipment.usePowerShard(firstShard, 1);
						resultDamage += firstShard.getItemTemplate().getWeaponBoost();
					}

					if (secondShard != null) {
						equipment.usePowerShard(secondShard, 1);
						resultDamage += secondShard.getItemTemplate().getWeaponBoost();
					}
				}
			} else {// if hand attack
				int totalMin = 16;
				int totalMax = 20;

				float power = attacker.getGameStats().getPower().getCurrent() * 0.01f;
				int diff = Math.round((totalMax - totalMin) * power / 2);
				resultDamage = pAttack.getBonus() + baseDamage;
				resultDamage += Rnd.get(-diff, diff);
			}
		} else {
			int rnd = (int) (resultDamage * 0.25);
			resultDamage += Rnd.get(-rnd, rnd);
		}
		// 减去防御 / subtract defense
		float pDef = target.getGameStats().getPDef().getBonus()
				+ getMovementModifier(target, StatEnum.PHYSICAL_DEFENSE, target.getGameStats().getPDef().getBase());
		resultDamage -= (pDef * 0.10f);

		if (resultDamage <= 0) {
			resultDamage = 1;
		}
    	return Math.max(1, Math.round(resultDamage));
	}

	/**
	 * 计算物理普攻伤害（忽略目标防御）
	 * Calculate physical auto-attack damage without applying target defense
	 *
	 * Attacker
	 * Target
	 * Whether main hand
	 * @return 忽略防御后的伤害 / Damage without defense reduction
	 */
	public static int calculatePhysicalAttackDamageNoDef(Creature attacker, Creature target, boolean isMainHand) {
		Stat2 pAttack;
		if (isMainHand) {
			pAttack = attacker.getGameStats().getMainHandPAttack();
		} else {
			pAttack = ((Player) attacker).getGameStats().getOffHandPAttack();
		}
		float resultDamage = pAttack.getCurrent();
		float baseDamage = pAttack.getBase();
		if (attacker instanceof Player) {
			Equipment equipment = ((Player) attacker).getEquipment();
			Item weapon;
			if (isMainHand) {
				weapon = equipment.getMainHandWeapon();
			} else {
				weapon = equipment.getOffHandWeapon();
			}

			if (weapon != null) {
				WeaponStats weaponStat = weapon.getItemTemplate().getWeaponStats();
				if (weaponStat == null) {
					return 0;
				}
				int totalMin = weaponStat.getMinDamage();
				int totalMax = weaponStat.getMaxDamage();
				if (totalMax - totalMin < 1) {
					log.warn(I18n.get("log.bbf300e93b6a"));
					log.warn(I18n.get("log.9b25cf6c9dbb", String.valueOf(equipment.getMainHandWeapon().getItemTemplate().getTemplateId())));
					log.warn(I18n.get("log.c4ee9fd27bbf", String.valueOf(totalMin)));
					log.warn(I18n.get("log.c24cbde05206", String.valueOf(totalMax)));
				}
				float power = attacker.getGameStats().getPower().getCurrent() * 0.01f;
				int diff = Math.round((totalMax - totalMin) * power / 2);
				resultDamage = pAttack.getBonus() + baseDamage;
				// 按 WeaponDualEffect 数值调整 / adjust with value from WeaponDualEffect
				// 使伤害下限更低，副手伤害更随机。 / it makes lower cap of damage lower, so damage is more random on offhand
				int negativeDiff = diff;
				if (!isMainHand) {
					negativeDiff = (int) Math.round((200 - ((Player) attacker).getDualEffectValue()) * 0.01 * diff);
				}
				resultDamage += Rnd.get(-negativeDiff, diff);
				// 添加力量碎片伤害 / add powerShard damage
				if (attacker.isInState(CreatureState.POWERSHARD)) {
					Item firstShard;
					Item secondShard = null;
					if (isMainHand) {
						firstShard = equipment.getMainHandPowerShard();
						if (weapon.getItemTemplate().isTwoHandWeapon()) {
							secondShard = equipment.getOffHandPowerShard();
						}
					} else {
						firstShard = equipment.getOffHandPowerShard();
					}

					if (firstShard != null) {
						equipment.usePowerShard(firstShard, 1);
						resultDamage += firstShard.getItemTemplate().getWeaponBoost();
					}

					if (secondShard != null) {
						equipment.usePowerShard(secondShard, 1);
						resultDamage += secondShard.getItemTemplate().getWeaponBoost();
					}
				}
			} else {// if hand attack
				int totalMin = 16;
				int totalMax = 20;

				float power = attacker.getGameStats().getPower().getCurrent() * 0.01f;
				int diff = Math.round((totalMax - totalMin) * power / 2);
				resultDamage = pAttack.getBonus() + baseDamage;
				resultDamage += Rnd.get(-diff, diff);
			}
		} else {
			int rnd = (int) (resultDamage * 0.25);
			resultDamage += Rnd.get(-rnd, rnd);
		}

		if (resultDamage <= 0) {
			resultDamage = 1;
		}
    	return Math.round(resultDamage);
	}

	/**
	 * 计算魔法普攻伤害（含元素抗性与魔防）
	 * Calculate magical auto-attack damage with elemental resist and magic defense
	 *
	 * Attacker
	 * Target
	 * Skill element
	 * Whether main hand
	 * Magical damage
	 */
	public static int calculateMagicalAttackDamage(Creature attacker, Creature target, SkillElement element,
			boolean isMainHand) {
		Preconditions.checkNotNull(element, "Skill element should be NONE instead of null");
		Stat2 mAttack;

		if (isMainHand) {
			mAttack = attacker.getGameStats().getMainHandMAttack();
		} else {
			mAttack = attacker.getGameStats().getOffHandMAttack();
		}
		float resultDamage = mAttack.getCurrent();

		if (attacker instanceof Player) {
			Equipment equipment = ((Player) attacker).getEquipment();
			Item weapon = equipment.getMainHandWeapon();

			if (weapon != null) {
				WeaponStats weaponStat = weapon.getItemTemplate().getWeaponStats();
				if (weaponStat == null) {
					return 0;
				}
				int totalMin = weaponStat.getMinDamage();
				int totalMax = weaponStat.getMaxDamage();
				if (totalMax - totalMin < 1) {
					log.warn(I18n.get("log.bbf300e93b6a"));
					log.warn(I18n.get("log.9b25cf6c9dbb", String.valueOf(equipment.getMainHandWeapon().getItemTemplate().getTemplateId())));
					log.warn(I18n.get("log.c4ee9fd27bbf", String.valueOf(totalMin)));
					log.warn(I18n.get("log.c24cbde05206", String.valueOf(totalMax)));
				}
				float knowledge = attacker.getGameStats().getKnowledge().getCurrent() * 0.01f;
				int diff = Math.round((totalMax - totalMin) * knowledge / 2);
				resultDamage = mAttack.getBonus()
						+ getMovementModifier(attacker, StatEnum.MAGICAL_ATTACK, mAttack.getBase());
				resultDamage += Rnd.get(-diff, diff);

				if (attacker.isInState(CreatureState.POWERSHARD)) {
					Item firstShard = equipment.getMainHandPowerShard();
					Item secondShard = equipment.getOffHandPowerShard();
					if (firstShard != null) {
						equipment.usePowerShard(firstShard, 1);
						resultDamage += firstShard.getItemTemplate().getWeaponBoost();
					}

					if (secondShard != null) {
						equipment.usePowerShard(secondShard, 1);
						resultDamage += secondShard.getItemTemplate().getWeaponBoost();
					}
				}
			}
		}

		if (element != SkillElement.NONE) {
			float elementalDef = getMovementModifier(target, SkillElement.getResistanceForElement(element),
					target.getGameStats().getMagicalDefenseFor(element));
			resultDamage = Math.round(resultDamage * (1 - elementalDef / 1300f));
		}

		float mDef = target.getGameStats().getMDef().getBonus()
				+ getMovementModifier(target, StatEnum.MAGICAL_DEFEND, target.getGameStats().getMDef().getBase());
		resultDamage -= (mDef * 0.10f);

		if (resultDamage <= 0) {
			resultDamage = 1;
		}
    	return Math.max(1, Math.round(resultDamage));
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
		CreatureGameStats<?> sgs = speller.getGameStats();
		CreatureGameStats<?> tgs = target.getGameStats();
		float damages = baseDamages;
		if (!noReduce) {
			int magicBoost = useMagicBoost ? sgs.getMBoost().getCurrent() : 0;
			if (!(speller instanceof Trap)) {
				magicBoost -= tgs.getMBResist().getCurrent();
			}
			int knowledge = useKnowledge ? sgs.getKnowledge().getCurrent() : 100;
			damages *= (1 + capMagicBoostForDamage(magicBoost) / (knowledge * 10f));
			damages = sgs.getStat(StatEnum.BOOST_SPELL_ATTACK, (int) damages).getCurrent();
		}
		damages += bonus;
		if (!noReduce && element != SkillElement.NONE) {
			float elementalDef = getMovementModifier(target, SkillElement.getResistanceForElement(element),
					tgs.getMagicalDefenseFor(element));
			damages *= (1 - elementalDef / getElementalDefenseDenominator(speller, target));
			if (useMagicalDefense) {
				float mDef = target.getGameStats().getMDef().getBonus()
						+ getMovementModifier(target, StatEnum.MAGICAL_DEFEND, target.getGameStats().getMDef().getBase());
				damages -= mDef * 0.10f;
			}
		}
		if (damages < 0) {
			damages = 0;
		} else if (speller instanceof Npc && !(speller instanceof SummonedObject)) {
			int rnd = (int) (damages * 0.08f);
			damages += Rnd.get(-rnd, rnd);
		}
		return Math.round(damages);
	}

	static int capMagicBoostForDamage(int magicBoost) {
		if (magicBoost < 0) {
			return 0;
		}
		return Math.min(magicBoost, SkillConfig.MAGICBOOST_CAP);
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

		int pvpResist = attacked instanceof Player
				? attacked.getGameStats().getStat(StatEnum.PVP_MAGICAL_RESIST, 0).getCurrent() : 0;
		int critical = calculateEffectiveMagicalCritical(attacker.getGameStats().getMCritical().getCurrent(),
				attacked.getGameStats().getSpellResist().getCurrent(), pvpResist, criticalProb);
		return Rnd.nextInt(1000) < critical;
	}

	static int calculateEffectiveMagicalCritical(int critical, int spellResist, int pvpResist, int criticalProb) {
		return Math.max(0, Math.round((critical - spellResist - pvpResist) * criticalProb / 100f));
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
		return adjustDamages(attacker, target, damages, pvpDamage, useMovement, SkillElement.NONE);
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
		if (attacker instanceof Npc && ((Npc) attacker).getAbyssNpcType() == AbyssNpcType.ARTIFACT) {
			return damages;
		}

		if (attacker.isPvpTarget(target)) {
			if (pvpDamage > 0) {
				damages *= pvpDamage * 0.01;
			}
			damages *= 0.42f;
			int pvpAttackBonus = attacker.getGameStats().getStat(StatEnum.PVP_ATTACK_RATIO, 0).getCurrent();
			int pvpDefenceBonus = target.getGameStats().getStat(StatEnum.PVP_DEFEND_RATIO, 0).getCurrent();
			switch (element) {
			case NONE:
				pvpAttackBonus += attacker.getGameStats().getStat(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 0).getCurrent();
				pvpDefenceBonus += target.getGameStats().getStat(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 0).getCurrent();
				break;
			case FIRE:
			case WATER:
			case WIND:
			case EARTH:
			case LIGHT:
			case DARK:
				pvpAttackBonus += attacker.getGameStats().getStat(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 0).getCurrent();
				pvpDefenceBonus += target.getGameStats().getStat(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 0).getCurrent();
				break;
			default:
				break;
			}
			if (attacker.getRace() != target.getRace() && !attacker.isInInstance()) {
				pvpAttackBonus += Math.round(GameRuntimeServices.influence().getPvpRaceBonus(attacker.getRace()) * 1000 - 1000);
			}
			pvpAttackBonus = StatCapUtil.limitValueForPvpOrPveStat(CombatMode.PVP, RatioType.ATTACK, pvpAttackBonus);
			pvpDefenceBonus = StatCapUtil.limitValueForPvpOrPveStat(CombatMode.PVP, RatioType.DEFENSE, pvpDefenceBonus);
			damages *= Math.max(0.1f, 1f + (pvpAttackBonus - pvpDefenceBonus) / 1000f);
		} else {
			if (attacker instanceof Player && target instanceof Npc) {
				int levelDiff = target.getLevel() - attacker.getLevel();
				damages *= 1f - getNpcLevelDiffMod(levelDiff, 0);
			}
			int pveAttackBonus = attacker.getGameStats().getStat(StatEnum.PVE_ATTACK_RATIO, 0).getCurrent();
			int pveDefenceBonus = target.getGameStats().getStat(StatEnum.PVE_DEFEND_RATIO, 0).getCurrent();
			StatEnum targetRaceAttackRatio = getPveAttackRatioStat(target.getRace());
			if (targetRaceAttackRatio != null) {
				pveAttackBonus += attacker.getGameStats().getStat(targetRaceAttackRatio, 0).getCurrent();
			}
			switch (element) {
			case NONE:
				pveAttackBonus += attacker.getGameStats().getStat(StatEnum.PVE_ATTACK_RATIO_PHYSICAL, 0).getCurrent();
				pveDefenceBonus += target.getGameStats().getStat(StatEnum.PVE_DEFEND_RATIO_PHYSICAL, 0).getCurrent();
				break;
			case FIRE:
			case WATER:
			case WIND:
			case EARTH:
			case LIGHT:
			case DARK:
				pveAttackBonus += attacker.getGameStats().getStat(StatEnum.PVE_ATTACK_RATIO_MAGICAL, 0).getCurrent();
				pveDefenceBonus += target.getGameStats().getStat(StatEnum.PVE_DEFEND_RATIO_MAGICAL, 0).getCurrent();
				break;
				default:
					break;
				}
			pveAttackBonus = StatCapUtil.limitValueForPvpOrPveStat(CombatMode.PVE, RatioType.ATTACK, pveAttackBonus);
			pveDefenceBonus = StatCapUtil.limitValueForPvpOrPveStat(CombatMode.PVE, RatioType.DEFENSE, pveDefenceBonus);
			damages *= Math.max(0.1f, 1f + (pveAttackBonus - pveDefenceBonus) / 1000f);
		}
		if (useMovement) {
			damages = movementDamageBonus(attacker, damages);
		}
		if (attacker instanceof Player) {
			PlayerClass playerClass = ((Player) attacker).getPlayerClass();
			if (playerClass != null) {
				switch (playerClass) {
				case AETHERTECH:
					damages *= 0.8f;
					break;
				case GUNSLINGER:
					damages *= 0.7f;
					break;
				case SONGWEAVER:
					damages *= 0.7f;
					break;
				case SORCERER:
					damages *= 0.7f;
					break;
				default:
					damages *= 1f;
				}
			}
		}
		return damages;
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

		float accuracy = attacker.getGameStats().getMainHandPAccuracy().getCurrent();
		float dodge = 0;
		if (attacked instanceof Player) {
			dodge = attacked.getGameStats().getEvasion().getBonus()
					+ getMovementModifier(attacked, StatEnum.EVASION, attacked.getGameStats().getEvasion().getBase())
					+ attacked.getGameStats().getStat(StatEnum.PVP_DODGE, 0).getCurrent();
		} else {
			dodge = attacked.getGameStats().getEvasion().getBonus()
					+ getMovementModifier(attacked, StatEnum.EVASION, attacked.getGameStats().getEvasion().getBase());
		}
		float dodgeRate = calculateAvoidanceDifference(dodge, accuracy, accMod);
		if (attacked instanceof Npc) {
			int levelDiff = attacked.getLevel() - attacker.getLevel();
			dodgeRate *= 1 + getNpcLevelDiffMod(levelDiff, 0);

			// 静态 NPC 永不闪避 / static npcs never dodge
			if (((Npc) attacked).hasEntity()) {
				return false;
			}
		}
		return calculatePhysicalEvasion(dodgeRate, 300);
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
		if (attacker.getObserveController().hasAlwaysHit()) {
			return false;
		}
		// 始终检查招架 / check always parry
		if (attacked.getObserveController().checkAttackStatus(AttackStatus.PARRY)) {
			return true;
		}
		float accuracy = attacker.getGameStats().getMainHandPAccuracy().getCurrent();
		float parry = 0;
		if (attacked instanceof Player) {
			parry = attacked.getGameStats().getParry().getBonus()
					+ getMovementModifier(attacked, StatEnum.PARRY, attacked.getGameStats().getParry().getBase())
					+ attacked.getGameStats().getStat(StatEnum.PVP_PARRY, 0).getCurrent();
		} else {
			parry = attacked.getGameStats().getParry().getBonus()
					+ getMovementModifier(attacked, StatEnum.PARRY, attacked.getGameStats().getParry().getBase());
		}
		float parryRate = calculateAvoidanceDifference(parry, accuracy, accMod);
		return calculatePhysicalEvasion(parryRate, 400);
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
		if (attacker.getObserveController().hasAlwaysHit()) {
			return false;
		}
		if (attacked.getObserveController().checkAttackStatus(AttackStatus.BLOCK)) {
			return true;
		}
		float accuracy = attacker.getGameStats().getMainHandPAccuracy().getCurrent();
		float block = 0;
		if (attacked instanceof Player) {
			block = attacked.getGameStats().getBlock().getBonus()
					+ getMovementModifier(attacked, StatEnum.BLOCK, attacked.getGameStats().getBlock().getBase())
					+ attacked.getGameStats().getStat(StatEnum.PVP_BLOCK, 0).getCurrent();
		} else {
			block = attacked.getGameStats().getBlock().getBonus()
					+ getMovementModifier(attacked, StatEnum.BLOCK, attacked.getGameStats().getBlock().getBase());
		}
		float blockRate = calculateAvoidanceDifference(block, accuracy, accMod);
		return calculatePhysicalEvasion(blockRate, 500);
	}

	static float calculateAvoidanceDifference(float defense, float accuracy, int accMod) {
		return defense - accuracy - accMod;
	}

	/**
	 * 根据防御-命中差值与上限判定是否闪避/招架/格挡
	 * Resolve dodge/parry/block success from defense-accuracy difference and upper cap
	 *
	 * @param diff 防御与命中差值 / Defense-accuracy difference
	 * Probability upper cap
	 *
	 * @return 是否成功规避 / Whether avoidance succeeds
	 */
	public static boolean calculatePhysicalEvasion(float diff, int upperCap) {
		diff = diff * 0.6f + 50;
		if (diff > upperCap) {
			diff = upperCap;
		}
		return Rnd.nextInt(1000) < diff;
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
				critical *= (1 + acStatus.getValue() / 100);
			} else {
				return Rnd.nextInt(1000) < acStatus.getValue();
			}
		}
		critical = attacked.getGameStats().getPositiveReverseStat(StatEnum.PHYSICAL_CRITICAL_RESIST, critical)
				- attacker.getGameStats().getStat(StatEnum.PVP_HIT_ACCURACY, 0).getCurrent();
		critical *= (float) criticalProb / 100f;
		double criticalRate;
		if (critical <= 500) {
			criticalRate = critical * 0.1f;
		} else if (critical <= 600) {
			criticalRate = (500 * 0.1f) + ((critical - 500) * 0.05f);
		} else {
			criticalRate = (500 * 0.1f) + (160 * 0.05f) + ((critical - 600) * 0.02f);
		}
		return Rnd.nextInt(100) < criticalRate;
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

		int levelDiff = attacked.getLevel() - attacker.getLevel();
		int magicalResist = attacked.getGameStats().getMResist().getCurrent();
		int resistRate = attacked.getGameStats().getMResist().getCurrent()
				- attacker.getGameStats().getMAccuracy().getCurrent()
				- attacker.getGameStats().getStat(StatEnum.PVP_MAGICAL_HIT_ACCURACY, 0).getCurrent() - accMod;

		if (magicalResist > 0 && levelDiff > 4) {
			resistRate += (levelDiff - 4) * 100;
		}

		if (attacker instanceof Player && attacked instanceof Player) {
			return Math.min(500, resistRate);
		}

		return Math.min(900, resistRate);
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
		switch (h) {
		case 7:
		case 0:
		case 1:
			switch (stat) {
			case WATER_RESISTANCE:
			case WIND_RESISTANCE:
			case FIRE_RESISTANCE:
			case EARTH_RESISTANCE:
			case ELEMENTAL_RESISTANCE_DARK:
			case ELEMENTAL_RESISTANCE_LIGHT:
			case PHYSICAL_DEFENSE:
				return value * 0.8f;
			default:
				break;
			}
			break;
		case 6:
		case 2:
			switch (stat) {
			case EVASION:
				return value + 300;
			case SPEED:
				return value * 0.8f;
			default:
				break;
			}
			break;
		case 5:
		case 4:
		case 3:
			switch (stat) {
			case PARRY:
			case BLOCK:
				return value + 500;
			case SPEED:
				return value * 0.6f;
			default:
				break;
			}
			break;
		}
		return value;
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

	private static float getNpcLevelDiffMod(int levelDiff, int base) {
		switch (levelDiff) {
		case 3:
			return 0.1f;
		case 4:
			return 0.2f;
		case 5:
			return 0.3f;
		case 6:
			return 0.4f;
		case 7:
			return 0.5f;
		case 8:
			return 0.6f;
		case 9:
			return 0.7f;
		default:
			if (levelDiff > 9)
				return 0.8f;
		}
		return base;
	}
}

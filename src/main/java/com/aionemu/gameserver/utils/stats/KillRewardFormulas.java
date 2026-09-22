package com.aionemu.gameserver.utils.stats;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.siege.SiegeNpc;
import com.aionemu.gameserver.model.gameobjects.player.RewardType;
import com.aionemu.gameserver.model.templates.npc.NpcRating;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 击杀奖励公式域：独立/组队经验、DP 奖励与 PVP 点数（AP/GP/XP/DP）得失计算。
 * Kill-reward formula domain: solo/group XP, DP rewards and PVP AP/GP/XP/DP gains and losses.
 *
 * <p>该类型只服务 {@link StatFunctions}：奖励数值公式随版本/配置调整，与战斗命中/伤害公式
 *（留在 {@link StatFunctions}）变化原因不同。全部为静态纯函数，不持有状态、不创建对象；
 * 对外仍通过 {@link StatFunctions} 的原 public static 方法访问（门面签名不变）。
 * This type only serves {@link StatFunctions}: reward tuning changes for different reasons than the
 * combat hit/damage formulas (which stay in {@link StatFunctions}). All static pure functions
 * without state or allocation; external callers keep using the original public static
 * {@link StatFunctions} facade methods with unchanged signatures.</p>
 */
final class KillRewardFormulas {

	/**
	 * 仅静态公式，禁止实例化。
	 * Static formulas only; not instantiable.
	 */
	private KillRewardFormulas() {
	}

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
		int baseDP = targetLevel * StatFunctions.calculateRatingMultipler(npcRating);
		int xpPercentage = XPRewardEnum.xpRewardFrom(targetLevel - playerLevel);
		float rate = player.getRates().getDpNpcRate();
		return (int) Math.floor(baseDP * xpPercentage * rate / 100);
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
		int baseDP = targetLevel * StatFunctions.calculateRatingMultipler(npcRating);
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
		float apNpcRate = StatFunctions.ApNpcRating(((Npc) target).getObjectTemplate().getRating());
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
			pointsLost = switch (difference) {
				case 3 -> Math.round(pointsLost * 0.85f);
				case 4 -> Math.round(pointsLost * 0.65f);
				default -> pointsLost;
			};
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
			pointsGained = switch (difference) {
				case 3 -> Math.round(pointsGained * 0.85f);
				case 4 -> Math.round(pointsGained * 0.65f);
				case -2 -> Math.round(pointsGained * 1.1f);
				case -3 -> Math.round(pointsGained * 1.2f);
				default -> pointsGained;
			};
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
			pointsLost = switch (difference) {
				case 3 -> Math.round(pointsLost * 0.85f);
				case 4 -> Math.round(pointsLost * 0.65f);
				default -> pointsLost;
			};
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
            pointsGained = switch (difference) {
                case 3 -> Math.round(pointsGained * 0.85f);
                case 4 -> Math.round(pointsGained * 0.65f);
                case -2 -> Math.round(pointsGained * 1.1f);
                case -3 -> Math.round(pointsGained * 1.2f);
                default -> pointsGained;
            };
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

}

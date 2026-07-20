package com.aionemu.gameserver.model.instance.playerreward;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceBuff;
import com.aionemu.gameserver.services.instance.InstanceSettlementService.ArenaReward;

/**
 * PvPArena 玩家奖励，用于副本相关逻辑。
 * Pv P Arena Player Reward for instance logic.
 */

public class PvPArenaPlayerReward extends InstancePlayerReward {
	private int position;
	private final int scoreFloor;
	private final int maximumTimeBonus;
	private int timeBonus;
	private int participation;
	private long absentMillis;
	private long absenceStartedAt;
	private boolean finalScore;
	// <欧比斯点数> / <Abyss Points>
	private int basicAP;
	private int rankingAP;
	private int scoreAP;
	// <荣耀点数> / <Glory Points>
	private int basicGP;
	private int rankingGP;
	private int scoreGP;
	private int item1Id;
	private int basicItem1;
	private int playItem1;
	private int rankItem1;
	private int item2Id;
	private int basicItem2;
	private int playItem2;
	private int rankItem2;
	private int bonusItem1Id;
	private int bonusItem1Count;
	private int bonusItem2Id;
	private int bonusItem2Count;
	private int buffDurationSeconds;
	private double rewardRate = 1;
	private final InstanceBuff boostMorale;
	private InstanceBuff stageBuff;

	public PvPArenaPlayerReward(Integer object, int initialScore, int scoreFloor, int timeBonus, byte buffId) {
		super(object);
		super.addPoints(initialScore);
		this.scoreFloor = scoreFloor;
		this.maximumTimeBonus = timeBonus;
		boostMorale = new InstanceBuff(buffId);
	}

	@Override
	public void addPoints(int points) {
		super.addPoints(points);
		if (getPoints() < scoreFloor) {
			super.addPoints(scoreFloor - getPoints());
		}
	}

	/** 获取坐标。 / Returns the position. */
	public int getPosition() {
		return position;
	}

	/** 设置坐标。 / Sets the position. */
	public void setPosition(int position) {
		this.position = position;
	}

	/** 返回时间加成 / Returns the time bonus*/
	public int getTimeBonus() {
		return timeBonus > 0 ? timeBonus : 0;
	}

	public void beginAbsence() {
		beginAbsence(System.currentTimeMillis());
	}

	public void endAbsence() {
		endAbsence(System.currentTimeMillis());
	}

	public long getAbsentMillis() {
		return absentMillis;
	}

	public long getAbsenceStartedAt() {
		return absenceStartedAt;
	}

	public void restoreAbsence(long absentMillis, long absenceStartedAt) {
		this.absentMillis = Math.max(0, absentMillis);
		this.absenceStartedAt = Math.max(0, absenceStartedAt);
	}

	void beginAbsence(long now) {
		if (absenceStartedAt == 0) {
			absenceStartedAt = now;
		}
	}

	void endAbsence(long now) {
		if (absenceStartedAt != 0) {
			absentMillis += Math.max(0, now - absenceStartedAt);
			absenceStartedAt = 0;
		}
	}

	public void finalizePlaytimeBonus(long totalPlayMillis, long endedAt) {
		if (totalPlayMillis <= 0) {
			throw new IllegalArgumentException("Arena play time must be positive");
		}
		long totalAbsentMillis = absentMillis;
		if (absenceStartedAt != 0) {
			totalAbsentMillis += Math.max(0, endedAt - absenceStartedAt);
		}
		long playedMillis = Math.max(0, totalPlayMillis - Math.min(totalPlayMillis, totalAbsentMillis));
		timeBonus = (int) (playedMillis * maximumTimeBonus / totalPlayMillis);
		participation = (int) (playedMillis * 100 / totalPlayMillis);
		finalScore = true;
	}

	public void restoreFinalScore(int timeBonus, int participation) {
		this.timeBonus = Math.max(0, timeBonus);
		this.participation = Math.max(0, Math.min(100, participation));
		this.finalScore = true;
	}

	// <欧比斯点数> / <Abyss Points>
	/** 返回基础欧比斯点 / Returns the basic ap */
	public int getBasicAP() {
		return basicAP;
	}

	/** 获取排行欧比斯点数。 / Returns the ranking ap. */
	public int getRankingAP() {
		return rankingAP;
	}

	/** 返回 score ap / Returns the score ap */
	public int getScoreAP() {
		return scoreAP;
	}

	// <荣耀点数> / <Glory Points>
	/** 返回基础荣耀点 / Returns the basic gp */
	public int getBasicGP() {
		return basicGP;
	}

	/** 获取排行荣耀点数。 / Returns the ranking gp. */
	public int getRankingGP() {
		return rankingGP;
	}

	/** 返回 score gp / Returns the score gp */
	public int getScoreGP() {
		return scoreGP;
	}

	public int getParticipationPercent() {
		return participation;
	}

	/** 返回 score points / Returns the score points */
	public int getScorePoints() {
		return getPoints() + (finalScore ? timeBonus : 0);
	}

	/**
	 * @return Whether boost morale
	 */
	public boolean hasBoostMorale() {
		return boostMorale.hasInstanceBuff();
	}

	/** 应用士气强化效果 / Apply boost morale effect */
	public void applyBoostMoraleEffect(Player player, int durationSeconds) {
		buffDurationSeconds = durationSeconds;
		boostMorale.applyEffect(player, durationSeconds * 1000);
	}

	/** 结束士气强化效果 / End Boost Morale Effect */
	public void endBoostMoraleEffect(Player player) {
		boostMorale.endEffect(player);
	}

	public void applyStageBuff(Player player, int buffId, int durationMillis) {
		endStageBuff(player);
		stageBuff = new InstanceBuff(buffId);
		stageBuff.applyEffect(player, durationMillis);
	}

	public void endStageBuff(Player player) {
		if (stageBuff != null) {
			stageBuff.endEffect(player);
			stageBuff = null;
		}
	}

	/** 返回 remaning time / Returns the remaning time */
	public int getRemaningTime() {
		int time = boostMorale.getRemaningTime();
		if (time >= 0 && time < buffDurationSeconds) {
			return buffDurationSeconds - time;
		}
		return 0;
	}

	public void applyArenaReward(ArenaReward reward) {
		basicAP = reward.basicAp();
		scoreAP = reward.playAp();
		rankingAP = reward.rankAp();
		basicGP = reward.basicGp();
		scoreGP = reward.playGp();
		rankingGP = reward.rankGp();
		item1Id = reward.item1Id();
		basicItem1 = reward.basicItem1();
		playItem1 = reward.playItem1();
		rankItem1 = reward.rankItem1();
		item2Id = reward.item2Id();
		basicItem2 = reward.basicItem2();
		playItem2 = reward.playItem2();
		rankItem2 = reward.rankItem2();
		bonusItem1Id = reward.bonusItem1Id();
		bonusItem1Count = reward.bonusItem1Count();
		bonusItem2Id = reward.bonusItem2Id();
		bonusItem2Count = reward.bonusItem2Count();
	}

	public int getItem1Id() {
		return item1Id;
	}

	public int getBasicItem1() {
		return basicItem1;
	}

	public int getPlayItem1() {
		return playItem1;
	}

	public int getRankItem1() {
		return rankItem1;
	}

	public int getItem2Id() {
		return item2Id;
	}

	public int getBasicItem2() {
		return basicItem2;
	}

	public int getPlayItem2() {
		return playItem2;
	}

	public int getRankItem2() {
		return rankItem2;
	}

	public int getBonusItem1Id() {
		return bonusItem1Id;
	}

	public int getBonusItem1Count() {
		return bonusItem1Count;
	}

	public int getBonusItem2Id() {
		return bonusItem2Id;
	}

	public int getBonusItem2Count() {
		return bonusItem2Count;
	}

	public double getRewardRate() {
		return rewardRate;
	}

	public void setRewardRate(double rewardRate) {
		if (!Double.isFinite(rewardRate) || rewardRate < 0) {
			throw new IllegalArgumentException("Arena reward rate cannot be negative");
		}
		this.rewardRate = rewardRate;
	}
}

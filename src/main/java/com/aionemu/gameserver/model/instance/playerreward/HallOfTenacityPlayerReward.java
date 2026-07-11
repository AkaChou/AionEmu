package com.aionemu.gameserver.model.instance.playerreward;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceBuff;

/**
 * HallOfTenacity 玩家奖励，用于副本相关逻辑。
 * Hall Of Tenacity Player Reward for instance logic.
 *
 * @author Ranastic
 */
public class HallOfTenacityPlayerReward extends InstancePlayerReward {

	private int position;
	private int zone;
	private int timeBonus;
	private InstanceBuff boostMorale;
	private int rewardAp;
	private int rewardExp;
	private int competitionPoint;

	public HallOfTenacityPlayerReward(Integer object, int timeBonus, byte buffId) {
		super(object);
		this.timeBonus = timeBonus;
		boostMorale = new InstanceBuff(buffId);
	}

	/** 获取坐标。 / Returns the position. */
	public int getPosition() {
		return position;
	}

	/** 设置坐标。 / Sets the position. */
	public void setPosition(int position) {
		this.position = position;
	}

	/** 获取区域。 / Returns the zone. */
	public int getZone() {
		return zone;
	}

	/** 设置区域。 / Sets the zone. */
	public void setZone(int zone) {
		this.zone = zone;
	}

	/** 返回 score points / Returns the score points */
	public int getScorePoints() {
		return timeBonus + getPoints();
	}

	/** 返回 participation / Returns the participation */
	public float getParticipation() {
		return (float) getTimeBonus() / timeBonus;
	}

	/** 返回时间加成 / Returns the time bonus*/
	public int getTimeBonus() {
		return timeBonus > 0 ? timeBonus : 0;
	}

	/**
	 * @return Whether boost morale / Whether boost morale
	 */
	public boolean hasBoostMorale() {
		return boostMorale.hasInstanceBuff();
	}

	/** 应用士气强化效果 / Apply boost morale effect */
	public void applyBoostMoraleEffect(Player player) {
		boostMorale.applyEffect(player, 20000);
	}

	/** 结束士气强化效果 / End Boost Morale Effect */
	public void endBoostMoraleEffect(Player player) {
		boostMorale.endEffect(player);
	}

	/** 返回 remaning time / Returns the remaning time */
	public int getRemaningTime() {
		int time = boostMorale.getRemaningTime();
		if (time >= 0 && time < 20) {
			return 20 - time;
		}
		return 0;
	}

	/** 获取奖励欧比斯点数。 / Returns the reward ap. */
	public int getRewardAp() {
		return rewardAp;
	}

	/** 设置奖励欧比斯点数。 / Sets the reward ap. */
	public void setRewardAp(int rewardAp) {
		this.rewardAp = rewardAp;
	}

	/** 获取奖励经验。 / Returns the reward exp. */
	public int getRewardExp() {
		return rewardExp;
	}

	/** 设置奖励经验。 / Sets the reward exp. */
	public void setRewardExp(int rewardExp) {
		this.rewardExp = rewardExp;
	}

	/** 返回 competition point / Returns the competition point */
	public int getCompetitionPoint() {
		return competitionPoint;
	}

	/** 设置 competition point / Sets the competition point */
	public void setCompetitionPoint(int competitionPoint) {
		this.competitionPoint = competitionPoint;
	}
}

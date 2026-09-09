package com.aionemu.gameserver.model.instance.playerreward;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceBuff;
import lombok.Getter;
import lombok.Setter;

/**
 * HallOfTenacity 玩家奖励，用于副本相关逻辑。
 * Hall Of Tenacity Player Reward for instance logic.
 *
 * @author Ranastic
 */
public class HallOfTenacityPlayerReward extends InstancePlayerReward {

	/** 获取坐标。 / Returns the position. */
	@Getter
	@Setter
	private int position;
	/** 获取区域。 / Returns the zone. */
	@Getter
	@Setter
	private int zone;
	private final int timeBonus;
	private final InstanceBuff boostMorale;
	/** 获取奖励欧比斯点数。 / Returns the reward ap. */
	@Getter
	@Setter
	private int rewardAp;
	/** 获取奖励经验。 / Returns the reward exp. */
	@Getter
	@Setter
	private int rewardExp;
	/** 返回 competition point / Returns the competition point */
	@Getter
	@Setter
	private int competitionPoint;

	public HallOfTenacityPlayerReward(Integer object, int timeBonus, byte buffId) {
		super(object);
		this.timeBonus = timeBonus;
		boostMorale = new InstanceBuff(buffId);
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
	 * @return 是否士气强化中 / whether boost morale
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
}

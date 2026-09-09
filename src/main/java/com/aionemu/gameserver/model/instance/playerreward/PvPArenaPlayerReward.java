package com.aionemu.gameserver.model.instance.playerreward;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceBuff;
import lombok.Getter;
import lombok.Setter;

/**
 * PvPArena 玩家奖励，用于副本相关逻辑。
 * Pv P Arena Player Reward for instance logic.
 */

public class PvPArenaPlayerReward extends InstancePlayerReward {
	/** 获取坐标。 / Returns the position. */
	@Getter
	@Setter
	private int position;
	private int timeBonus;
	private final float timeBonusModifier;
	// <欧比斯点数> / <Abyss Points>
	/** 返回基础欧比斯点 / Returns the basic ap */
	@Getter
	@Setter
	private int basicAP;
	/** 获取排行欧比斯点数。 / Returns the ranking ap. */
	@Getter
	@Setter
	private int rankingAP;
	/** 返回 score ap / Returns the score ap */
	@Getter
	@Setter
	private int scoreAP;
	// <荣耀点数> / <Glory Points>
	/** 返回基础荣耀点 / Returns the basic gp */
	@Getter
	@Setter
	private int basicGP;
	/** 获取排行荣耀点数。 / Returns the ranking gp. */
	@Getter
	@Setter
	private int rankingGP;
	/** 返回 score gp / Returns the score gp */
	@Getter
	@Setter
	private int scoreGP;
	/** 返回 basic crucible / Returns the basic crucible */
	@Getter
	@Setter
	private int basicCrucible;
	/** 返回 ranking crucible / Returns the ranking crucible */
	@Getter
	@Setter
	private int rankingCrucible;
	/** 返回 score crucible / Returns the score crucible */
	@Getter
	@Setter
	private int scoreCrucible;
	/** 设置 basic courage / Sets the basic courage */
	@Getter
	@Setter
	private int basicCourage;
	/** 设置 ranking courage / Sets the ranking courage */
	@Getter
	@Setter
	private int rankingCourage;
	/** 设置 score courage / Sets the score courage */
	@Getter
	@Setter
	private int scoreCourage;
	/** 返回 opportunity / Returns the opportunity */
	@Getter
	@Setter
	private int opportunity;
	/** 返回 glory ticket / Returns the glory ticket */
	@Getter
	@Setter
	private int gloryTicket;
	/** 返回 mithril medal / Returns the mithril medal */
	@Getter
	@Setter
	private int mithrilMedal;
	/** 返回 platinum medal / Returns the platinum medal */
	@Getter
	@Setter
	private int platinumMedal;
	/** 返回 glorious insignia / Returns the glorious insignia */
	@Getter
	@Setter
	private int gloriousInsignia;
	/** 设置 basic infinity / Sets the basic infinity */
	@Getter
	@Setter
	private int basicInfinity;
	/** 设置 ranking infinity / Sets the ranking infinity */
	@Getter
	@Setter
	private int rankingInfinity;
	/** 设置 score infinity / Sets the score infinity */
	@Getter
	@Setter
	private int scoreInfinity;
	/** 返回 life serum / Returns the life serum */
	@Getter
	@Setter
	private int lifeSerum;
	private long logoutTime;
	/**
	 * @return 是否已奖励 / whether rewarded
	 */
	@Getter
	private boolean isRewarded = false;
	private final InstanceBuff boostMorale;

	public PvPArenaPlayerReward(Integer object, int timeBonus, byte buffId) {
		super(object);
		super.addPoints(13000);
		this.timeBonus = timeBonus;
		timeBonusModifier = ((float) this.timeBonus / (float) 660000);
		boostMorale = new InstanceBuff(buffId);
	}

	/** 返回时间加成 / Returns the time bonus*/
	public int getTimeBonus() {
		return timeBonus > 0 ? timeBonus : 0;
	}

	/** 更新 log out time / Update log out time */
	public void updateLogOutTime() {
		logoutTime = System.currentTimeMillis();
	}

	/** 更新加成时间 / Update bonus time*/
	public void updateBonusTime() {
		int offlineTime = (int) (System.currentTimeMillis() - logoutTime);
		timeBonus -= offlineTime * timeBonusModifier;
	}

	/** 设置 rewarded / Sets the rewarded */
	public void setRewarded() {
		isRewarded = true;
	}

	/** 返回 participation / Returns the participation */
	public float getParticipation() {
		return (float) getTimeBonus() / timeBonus;
	}

	/** 返回 score points / Returns the score points */
	public int getScorePoints() {
		return timeBonus + getPoints();
	}

	/**
	 * @return Whether boost morale
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

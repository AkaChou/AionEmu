package com.aionemu.gameserver.model.instance.playerreward;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceBuff;

/**
 * PvPArena 玩家奖励，用于副本相关逻辑。
 * Pv P Arena Player Reward for instance logic.
 */

public class PvPArenaPlayerReward extends InstancePlayerReward {
	private int position;
	private int timeBonus;
	private float timeBonusModifier;
	// <欧比斯点数> / <Abyss Points>
	private int basicAP;
	private int rankingAP;
	private int scoreAP;
	// <荣耀点数> / <Glory Points>
	private int basicGP;
	private int rankingGP;
	private int scoreGP;
	private int basicCrucible;
	private int rankingCrucible;
	private int scoreCrucible;
	private int basicCourage;
	private int rankingCourage;
	private int scoreCourage;
	private int opportunity;
	private int gloryTicket;
	private int mithrilMedal;
	private int platinumMedal;
	private int gloriousInsignia;
	private int basicInfinity;
	private int rankingInfinity;
	private int scoreInfinity;
	private int lifeSerum;
	private long logoutTime;
	private boolean isRewarded = false;
	private InstanceBuff boostMorale;

	public PvPArenaPlayerReward(Integer object, int timeBonus, byte buffId) {
		super(object);
		super.addPoints(13000);
		this.timeBonus = timeBonus;
		timeBonusModifier = ((float) this.timeBonus / (float) 660000);
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

	/**
	 * @return Whether rewarded
	 */
	public boolean isRewarded() {
		return isRewarded;
	}

	/** 设置 rewarded / Sets the rewarded */
	public void setRewarded() {
		isRewarded = true;
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

	/** 设置 basic ap / Sets the basic ap */
	public void setBasicAP(int ap) {
		this.basicAP = ap;
	}

	/** 设置排行欧比斯点数。 / Sets the ranking ap. */
	public void setRankingAP(int ap) {
		this.rankingAP = ap;
	}

	/** 设置 score ap / Sets the score ap */
	public void setScoreAP(int ap) {
		this.scoreAP = ap;
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

	/** 设置 basic gp / Sets the basic gp */
	public void setBasicGP(int gp) {
		this.basicGP = gp;
	}

	/** 设置排行荣耀点数。 / Sets the ranking gp. */
	public void setRankingGP(int gp) {
		this.rankingGP = gp;
	}

	/** 设置 score gp / Sets the score gp */
	public void setScoreGP(int gp) {
		this.scoreGP = gp;
	}

	/** 返回 participation / Returns the participation */
	public float getParticipation() {
		return (float) getTimeBonus() / timeBonus;
	}

	/** 返回 basic crucible / Returns the basic crucible */
	public int getBasicCrucible() {
		return basicCrucible;
	}

	/** 返回 ranking crucible / Returns the ranking crucible */
	public int getRankingCrucible() {
		return rankingCrucible;
	}

	/** 返回 score crucible / Returns the score crucible */
	public int getScoreCrucible() {
		return scoreCrucible;
	}

	/** 设置 basic crucible / Sets the basic crucible */
	public void setBasicCrucible(int basicCrucible) {
		this.basicCrucible = basicCrucible;
	}

	/** 设置 ranking crucible / Sets the ranking crucible */
	public void setRankingCrucible(int rankingCrucible) {
		this.rankingCrucible = rankingCrucible;
	}

	/** 设置 score crucible / Sets the score crucible */
	public void setScoreCrucible(int scoreCrucible) {
		this.scoreCrucible = scoreCrucible;
	}

	/** 设置 basic courage / Sets the basic courage */
	public void setBasicCourage(int basicCourage) {
		this.basicCourage = basicCourage;
	}

	/** 设置 ranking courage / Sets the ranking courage */
	public void setRankingCourage(int rankingCourage) {
		this.rankingCourage = rankingCourage;
	}

	/** 设置 score courage / Sets the score courage */
	public void setScoreCourage(int scoreCourage) {
		this.scoreCourage = scoreCourage;
	}

	/** 返回 basic courage / Returns the basic courage */
	public int getBasicCourage() {
		return basicCourage;
	}

	/** 返回 ranking courage / Returns the ranking courage */
	public int getRankingCourage() {
		return rankingCourage;
	}

	/** 返回 score courage / Returns the score courage */
	public int getScoreCourage() {
		return scoreCourage;
	}

	/** 返回 opportunity / Returns the opportunity */
	public int getOpportunity() {
		return opportunity;
	}

	/** 设置 opportunity / Sets the opportunity */
	public void setOpportunity(int opportunity) {
		this.opportunity = opportunity;
	}

	/** 返回 glory ticket / Returns the glory ticket */
	public int getGloryTicket() {
		return gloryTicket;
	}

	/** 设置 glory ticket / Sets the glory ticket */
	public void setGloryTicket(int gloryTicket) {
		this.gloryTicket = gloryTicket;
	}

	/** 返回 mithril medal / Returns the mithril medal */
	public int getMithrilMedal() {
		return mithrilMedal;
	}

	/** 设置 mithril medal / Sets the mithril medal */
	public void setMithrilMedal(int mithrilMedal) {
		this.mithrilMedal = mithrilMedal;
	}

	/** 返回 platinum medal / Returns the platinum medal */
	public int getPlatinumMedal() {
		return platinumMedal;
	}

	/** 设置 platinum medal / Sets the platinum medal */
	public void setPlatinumMedal(int platinumMedal) {
		this.platinumMedal = platinumMedal;
	}

	/** 返回 glorious insignia / Returns the glorious insignia */
	public int getGloriousInsignia() {
		return gloriousInsignia;
	}

	/** 设置 glorious insignia / Sets the glorious insignia */
	public void setGloriousInsignia(int gloriousInsignia) {
		this.gloriousInsignia = gloriousInsignia;
	}

	/** 返回 life serum / Returns the life serum */
	public int getLifeSerum() {
		return lifeSerum;
	}

	/** 设置 life serum / Sets the life serum */
	public void setLifeSerum(int lifeSerum) {
		this.lifeSerum = lifeSerum;
	}

	/** 设置 basic infinity / Sets the basic infinity */
	public void setBasicInfinity(int basicInfinity) {
		this.basicInfinity = basicInfinity;
	}

	/** 设置 ranking infinity / Sets the ranking infinity */
	public void setRankingInfinity(int rankingInfinity) {
		this.rankingInfinity = rankingInfinity;
	}

	/** 设置 score infinity / Sets the score infinity */
	public void setScoreInfinity(int scoreInfinity) {
		this.scoreInfinity = scoreInfinity;
	}

	/** 返回 basic infinity / Returns the basic infinity */
	public int getBasicInfinity() {
		return basicInfinity;
	}

	/** 返回 ranking infinity / Returns the ranking infinity */
	public int getRankingInfinity() {
		return rankingInfinity;
	}

	/** 返回 score infinity / Returns the score infinity */
	public int getScoreInfinity() {
		return scoreInfinity;
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

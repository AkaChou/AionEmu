package com.aionemu.gameserver.model.instance.playerreward;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceBuff;

/**
 * EvergaleCanyon 玩家奖励，用于副本相关逻辑。
 * Evergale Canyon Player Reward for instance logic.
 */

public class EvergaleCanyonPlayerReward extends InstancePlayerReward {
	private int timeBonus;
	private long logoutTime;
	private float timeBonusModifier;
	private Race race;
	private int rewardAp;
	private int rewardGp;
	private int rewardExp;
	private int bonusAp;
	private int bonusGp;
	private int bonusExp;
	private int brokenSpinel;
	private float rewardCount;
	private int idEternityWarStigma;
	private int coinIdEternityWar01;
	private int cashMinionContract01;
	private InstanceBuff boostMorale;

	public EvergaleCanyonPlayerReward(Integer object, int timeBonus, byte buffId, Race race) {
		super(object);
		this.timeBonus = timeBonus;
		timeBonusModifier = ((float) this.timeBonus / (float) 660000);
		this.race = race;
		boostMorale = new InstanceBuff(buffId);
	}

	/** 返回 participation / Returns the participation */
	public float getParticipation() {
		return (float) getTimeBonus() / timeBonus;
	}

	/** 返回 score points / Returns the score points */
	public int getScorePoints() {
		return timeBonus + getPoints();
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

	/** 获取种族。 / Returns the race. */
	public Race getRace() {
		return race;
	}

	/** 获取奖励计数。 / Returns the reward count. */
	public int getRewardCount() {
		return (int) rewardCount;
	}

	/** 返回 broken spinel / Returns the broken spinel */
	public int getBrokenSpinel() {
		return brokenSpinel;
	}

	/** 返回 id eternity war stigma / Returns the id eternity war stigma */
	public int getIDEternityWarStigma() {
		return idEternityWarStigma;
	}

	/** 返回 coin id eternity war 01 / Returns the coin id eternity war 01 */
	public int getCoinIdEternityWar01() {
		return coinIdEternityWar01;
	}

	/** 返回 cash minion contract 01 / Returns the cash minion contract 01 */
	public int getCashMinionContract01() {
		return cashMinionContract01;
	}

	/** 设置 broken spinel / Sets the broken spinel */
	public void setBrokenSpinel(int reward) {
		this.brokenSpinel = reward;
	}

	/** 设置 id eternity war stigma / Sets the id eternity war stigma */
	public void setIDEternityWarStigma(int reward) {
		this.idEternityWarStigma = reward;
	}

	/** 设置 coin id eternity war 01 / Sets the coin id eternity war 01 */
	public void setCoinIdEternityWar01(int reward) {
		this.coinIdEternityWar01 = reward;
	}

	/** 设置 cash minion contract 01 / Sets the cash minion contract 01 */
	public void setCashMinionContract01(int reward) {
		this.cashMinionContract01 = reward;
	}

	/** 设置奖励计数。 / Sets the reward count. */
	public void setRewardCount(float rewardCount) {
		this.rewardCount = rewardCount;
	}

	// Ap
	/** 获取奖励欧比斯点数。 / Returns the reward ap. */
	public int getRewardAp() {
		return rewardAp;
	}

	/** 设置奖励欧比斯点数。 / Sets the reward ap. */
	public void setRewardAp(int rewardAp) {
		this.rewardAp = rewardAp;
	}

	/** 获取加成欧比斯点数。 / Returns the bonus ap. */
	public int getBonusAp() {
		return bonusAp;
	}

	/** 设置加成欧比斯点数。 / Sets the bonus ap. */
	public void setBonusAp(int bonusAp) {
		this.bonusAp = bonusAp;
	}

	// Gp
	/** 返回 reward gp / Returns the reward gp */
	public int getRewardGp() {
		return rewardGp;
	}

	/** 设置 reward gp / Sets the reward gp */
	public void setRewardGp(int rewardGp) {
		this.rewardGp = rewardGp;
	}

	/** 返回加成荣耀点 / Returns the bonus gp */
	public int getBonusGp() {
		return bonusGp;
	}

	/** 设置加成荣耀点 / Sets the bonus gp*/
	public void setBonusGp(int bonusGp) {
		this.bonusGp = bonusGp;
	}

	// 经验 / Exp
	/** 获取奖励经验。 / Returns the reward exp. */
	public int getRewardExp() {
		return rewardExp;
	}

	/** 设置奖励经验。 / Sets the reward exp. */
	public void setRewardExp(int rewardExp) {
		this.rewardExp = rewardExp;
	}

	/** 获取加成经验。 / Returns the bonus exp. */
	public int getBonusExp() {
		return bonusExp;
	}

	/** 设置加成经验。 / Sets the bonus exp. */
	public void setBonusExp(int bonusExp) {
		this.bonusExp = bonusExp;
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

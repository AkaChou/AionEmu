package com.aionemu.gameserver.model.instance.playerreward;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceBuff;
import lombok.Getter;
import lombok.Setter;

/**
 * EvergaleCanyon 玩家奖励，用于副本相关逻辑。
 * Evergale Canyon Player Reward for instance logic.
 */

@Getter
@Setter
public class EvergaleCanyonPlayerReward extends InstancePlayerReward {
	private int timeBonus;
	private long logoutTime;
	private final float timeBonusModifier;
	/** 获取种族。 / Returns the race. */
	private final Race race;
	/** 获取奖励欧比斯点数。 / Returns the reward ap. */
	private int rewardAp;
	/** 返回 reward gp / Returns the reward gp */
	private int rewardGp;
	/** 获取奖励经验。 / Returns the reward exp. */
	private int rewardExp;
	/** 获取加成欧比斯点数。 / Returns the bonus ap. */
	private int bonusAp;
	/** 返回加成荣耀点 / Returns the bonus gp */
	private int bonusGp;
	/** 获取加成经验。 / Returns the bonus exp. */
	private int bonusExp;
	/** 返回 broken spinel / Returns the broken spinel */
	private int brokenSpinel;
	/** 设置奖励计数。 / Sets the reward count. */
	private float rewardCount;
	private int idEternityWarStigma;
	/** 返回 coin id eternity war 01 / Returns the coin id eternity war 01 */
	private int coinIdEternityWar01;
	/** 返回 cash minion contract 01 / Returns the cash minion contract 01 */
	private int cashMinionContract01;
	private final InstanceBuff boostMorale;

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

	/** 获取奖励计数。 / Returns the reward count. */
	public int getRewardCount() {
		return (int) rewardCount;
	}

	/** 返回 id eternity war stigma / Returns the id eternity war stigma */
	public int getIDEternityWarStigma() {
		return idEternityWarStigma;
	}

	/** 设置 id eternity war stigma / Sets the id eternity war stigma */
	public void setIDEternityWarStigma(int reward) {
		this.idEternityWarStigma = reward;
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

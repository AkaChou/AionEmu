package com.aionemu.gameserver.model.instance.playerreward;

import java.util.Arrays;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceBuff;

public class BattlegroundPlayerReward extends InstancePlayerReward {
	private final Race race;
	private final long joinedAt;
	private final InstanceBuff boostMorale;
	private final int[] rewardItemIds = new int[5];
	private final long[] rewardItemCounts = new long[5];
	private long logoutAt;
	private long offlineMillis;
	private double participation = 1;
	private int rewardAp;
	private int rewardGp;
	private int rewardExp;
	private int bonusAp;
	private int bonusGp;
	private int bonusExp;

	public BattlegroundPlayerReward(int objectId, byte buffId, Race race) {
		this(objectId, buffId, race, System.currentTimeMillis());
	}

	public BattlegroundPlayerReward(int objectId, byte buffId, Race race, long joinedAt) {
		super(objectId);
		this.race = race;
		this.joinedAt = joinedAt;
		boostMorale = new InstanceBuff(buffId);
	}

	public Race getRace() {
		return race;
	}

	public int getScorePoints() {
		return getPoints();
	}

	public double calculateParticipation(long battleStartedAt, long endedAt) {
		long duration = Math.max(1, endedAt - battleStartedAt);
		long inactive = Math.max(0, Math.max(joinedAt, battleStartedAt) - battleStartedAt) + offlineMillis;
		if (logoutAt > 0) {
			inactive += Math.max(0, endedAt - logoutAt);
		}
		participation = Math.max(0, Math.min(1, (double) (duration - Math.min(duration, inactive)) / duration));
		return participation;
	}

	public double getParticipation() {
		return participation;
	}

	public long getJoinedAt() {
		return joinedAt;
	}

	public long getLogoutAt() {
		return logoutAt;
	}

	public long getOfflineMillis() {
		return offlineMillis;
	}

	public void restoreActivity(long logoutAt, long offlineMillis) {
		this.logoutAt = logoutAt;
		this.offlineMillis = Math.max(0, offlineMillis);
	}

	public void updateLogOutTime() {
		if (logoutAt == 0) {
			logoutAt = System.currentTimeMillis();
		}
	}

	public void updateBonusTime() {
		if (logoutAt > 0) {
			offlineMillis += Math.max(0, System.currentTimeMillis() - logoutAt);
			logoutAt = 0;
		}
	}

	public void setSettlementValues(int rewardExp, int bonusExp, int rewardAp, int bonusAp,
			int rewardGp, int bonusGp) {
		this.rewardExp = rewardExp;
		this.bonusExp = bonusExp;
		this.rewardAp = rewardAp;
		this.bonusAp = bonusAp;
		this.rewardGp = rewardGp;
		this.bonusGp = bonusGp;
	}

	public int getRewardAp() { return rewardAp; }
	public int getRewardGp() { return rewardGp; }
	public int getRewardExp() { return rewardExp; }
	public int getBonusAp() { return bonusAp; }
	public int getBonusGp() { return bonusGp; }
	public int getBonusExp() { return bonusExp; }

	public void clearRewardItems() {
		Arrays.fill(rewardItemIds, 0);
		Arrays.fill(rewardItemCounts, 0);
	}

	public void setRewardItem(int slot, int itemId, long count) {
		if (slot < 0 || slot >= rewardItemIds.length || itemId <= 0 || count <= 0) {
			throw new IllegalArgumentException("Invalid battleground reward item slot");
		}
		rewardItemIds[slot] = itemId;
		rewardItemCounts[slot] = count;
	}

	public int getRewardItemId(int slot) {
		return rewardItemIds[slot];
	}

	public long getRewardItemCount(int slot) {
		return rewardItemCounts[slot];
	}

	public boolean hasBoostMorale() {
		return boostMorale.hasInstanceBuff();
	}

	public void applyBoostMoraleEffect(Player player) {
		boostMorale.applyEffect(player, 20000);
	}

	public void endBoostMoraleEffect(Player player) {
		boostMorale.endEffect(player);
	}

	public int getRemaningTime() {
		int time = boostMorale.getRemaningTime();
		return time >= 0 && time < 20 ? 20 - time : 0;
	}
}

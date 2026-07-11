package com.aionemu.gameserver.model.gameobjects;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.TeamMember;
import com.aionemu.gameserver.model.team2.TemporaryPlayerTeam;
import com.aionemu.gameserver.model.team2.alliance.PlayerAlliance;
import com.aionemu.gameserver.model.team2.common.legacy.LootGroupRules;

/**
 * 掉落 NPC 游戏对象。
 * Drop Npc game object.
 *
 * @author Simple
 */
public class DropNpc {

	private final int objectId;
	private Set<Integer> allowedLooters = new HashSet<>();
	private Collection<Player> inRangePlayers = new ArrayList<Player>();
	private Collection<Player> playerStatus = new ArrayList<Player>();
	private Player lootingPlayer = null;
	private int distributionId = 0;
	private boolean distributionType;
	private int currentIndex = 0;
	private WeakReference<TemporaryPlayerTeam<? extends TeamMember<Player>>> lootingTeam;
	private int lootingTeamId;
	private int maxRoll;
	private LootGroupRules lastLootGroupRules;
	private boolean isFreeForAll = false;
	private long remainingDecayTime;

	public DropNpc(int objectId) {
		this.objectId = objectId;
	}

	/** 设置 allowed looters / Sets the allowed looters */
	public void setAllowedLooters(Set<Integer> allowedLooters) {
		this.allowedLooters = allowedLooters;
	}

	/** 设置 allowed looter / Sets the allowed looter */
	public void setAllowedLooter(Player player) {
		allowedLooters.add(player.getObjectId());
	}

	/** 返回 allowed looters / Returns the allowed looters */
	public Set<Integer> getAllowedLooters() {
		return allowedLooters;
	}

	/** 是否 allowed to loot / Whether allowed to loot */
	public boolean isAllowedToLoot(Player player) {
		return isFreeForAll || allowedLooters.contains(player.getObjectId());
	}

	/**
	 * @param player the lootingPlayer to set
	 */
	public void setLootingPlayer(Player player) {
		this.lootingPlayer = player;
	}

	/**
	 * @return lootingPlayer
	 */
	public Player getLootingPlayer() {
		return lootingPlayer;
	}

	/**
	 * @return the beingLooted
	 */
	public boolean isBeingLooted() {
		return lootingPlayer != null;
	}

	/**
	 * @param distributionId
	 */
	public void setDistributionId(int distributionId) {
		this.distributionId = distributionId;
	}

	/**
	 * @return the DistributionId
	 */
	public int getDistributionId() {
		return distributionId;
	}

	/**
	 * @param distributionType
	 */
	public void setDistributionType(boolean distributionType) {
		this.distributionType = distributionType;
	}

	/**
	 * @return the DistributionType
	 */
	public boolean getDistributionType() {
		return distributionType;
	}

	/**
	 * @param currentIndex
	 */
	public void setCurrentIndex(int currentIndex) {
		this.currentIndex = currentIndex;
	}

	/**
	 * @return currentIndex
	 */
	public int getCurrentIndex() {
		return currentIndex;
	}

	/** 返回 looting team id / Returns the looting team id */
	public int getLootingTeamId() {
		return lootingTeamId;
	}

	/** 返回 max roll / Returns the max roll */
	public int getMaxRoll() {
		return maxRoll;
	}

	/** 返回 loot group rules / Returns the loot group rules */
	public LootGroupRules getLootGroupRules() {
		TemporaryPlayerTeam<? extends TeamMember<Player>> team = lootingTeam == null ? null : lootingTeam.get();
		if (team != null) {
			lastLootGroupRules = team.getLootGroupRules();
		}
		return lastLootGroupRules;
	}

	/** 设置 looting team / Sets the looting team */
	public void setLootingTeam(TemporaryPlayerTeam<? extends TeamMember<Player>> team) {
		lootingTeam = new WeakReference<>(team);
		lootingTeamId = team.getTeamId();
		maxRoll = team instanceof PlayerAlliance alliance && alliance.isInLeague() ? 10000
				: team instanceof PlayerAlliance ? 1000 : 100;
		lastLootGroupRules = team.getLootGroupRules();
	}

	/** 设置 in range players / Sets the in range players */
	public void setInRangePlayers(Collection<Player> inRangePlayers) {
		this.inRangePlayers = inRangePlayers;
	}

	/**
	 * @return the inRangePlayers
	 */
	public Collection<Player> getInRangePlayers() {
		return inRangePlayers;
	}

	/**
	 * @param player
	 */
	public void addPlayerStatus(Player player) {
		playerStatus.add(player);
	}

	/**
	 * @param player
	 */
	public void delPlayerStatus(Player player) {
		playerStatus.remove(player);
	}

	/**
	 * @return the playerStatus
	 */
	public Collection<Player> getPlayerStatus() {
		return playerStatus;
	}

	/**
	 * @return true if player is found in list
	 */
	public boolean containsPlayerStatus(Player player) {
		return playerStatus.contains(player);
	}

	/**
	 * @return isFreeForAll.
	 */
	public boolean isFreeForAll() {
		return isFreeForAll;
	}

	/** Start free for all / Start free for all */
	public void startFreeForAll() {
		isFreeForAll = true;
		distributionId = 0;
		allowedLooters.clear();
	}

	/** 返回对象 ID / Returns the object id */
	public final int getObjectId() {
		return objectId;
	}

	/** 返回 remaining decay time / Returns the remaining decay time */
	public long getRemainingDecayTime() {
		return remainingDecayTime;
	}

	/** 设置 remaining decay time / Sets the remaining decay time */
	public void setRemainingDecayTime(long remainingDecayTime) {
		this.remainingDecayTime = remainingDecayTime;
	}
}

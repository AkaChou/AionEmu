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

	/** 设置允许拾取者集合 / Sets the allowed looters */
	public void setAllowedLooters(Set<Integer> allowedLooters) {
		this.allowedLooters = allowedLooters;
	}

	/** 添加允许拾取者 / Adds an allowed looter */
	public void setAllowedLooter(Player player) {
		allowedLooters.add(player.getObjectId());
	}

	/** 返回允许拾取者集合 / Returns the allowed looters */
	public Set<Integer> getAllowedLooters() {
		return allowedLooters;
	}

	/** 是否允许拾取 / Whether allowed to loot */
	public boolean isAllowedToLoot(Player player) {
		return isFreeForAll || allowedLooters.contains(player.getObjectId());
	}

	/**
	 * 设置正在拾取的玩家。
	 * Sets the player currently looting.
	 *
	 * @param player 正在拾取的玩家 / the lootingPlayer to set
	 */
	public void setLootingPlayer(Player player) {
		this.lootingPlayer = player;
	}

	/**
	 * 返回正在拾取的玩家。
	 * Returns the player currently looting.
	 *
	 * @return 正在拾取的玩家 / lootingPlayer
	 */
	public Player getLootingPlayer() {
		return lootingPlayer;
	}

	/**
	 * 是否正在被拾取。
	 * Whether the drop is being looted.
	 *
	 * @return 是否正在被拾取 / whether being looted
	 */
	public boolean isBeingLooted() {
		return lootingPlayer != null;
	}

	/**
	 * 设置分配 ID。
	 * Sets the distribution id.
	 *
	 * @param distributionId 分配 ID / distribution id
	 */
	public void setDistributionId(int distributionId) {
		this.distributionId = distributionId;
	}

	/**
	 * 返回分配 ID。
	 * Returns the distribution id.
	 *
	 * @return 分配 ID / distribution id
	 */
	public int getDistributionId() {
		return distributionId;
	}

	/**
	 * 设置分配类型。
	 * Sets the distribution type.
	 *
	 * @param distributionType 分配类型 / distribution type
	 */
	public void setDistributionType(boolean distributionType) {
		this.distributionType = distributionType;
	}

	/**
	 * 返回分配类型。
	 * Returns the distribution type.
	 *
	 * @return 分配类型 / distribution type
	 */
	public boolean getDistributionType() {
		return distributionType;
	}

	/**
	 * 设置当前索引。
	 * Sets the current index.
	 *
	 * @param currentIndex 当前索引 / current index
	 */
	public void setCurrentIndex(int currentIndex) {
		this.currentIndex = currentIndex;
	}

	/**
	 * 返回当前索引。
	 * Returns the current index.
	 *
	 * @return 当前索引 / current index
	 */
	public int getCurrentIndex() {
		return currentIndex;
	}

	/** 返回拾取队伍 ID / Returns the looting team id */
	public int getLootingTeamId() {
		return lootingTeamId;
	}

	/** 返回最大点数 / Returns the max roll */
	public int getMaxRoll() {
		return maxRoll;
	}

	/** 返回拾取规则 / Returns the loot group rules */
	public LootGroupRules getLootGroupRules() {
		TemporaryPlayerTeam<? extends TeamMember<Player>> team = lootingTeam == null ? null : lootingTeam.get();
		if (team != null) {
			lastLootGroupRules = team.getLootGroupRules();
		}
		return lastLootGroupRules;
	}

	/** 设置拾取队伍 / Sets the looting team */
	public void setLootingTeam(TemporaryPlayerTeam<? extends TeamMember<Player>> team) {
		lootingTeam = new WeakReference<>(team);
		lootingTeamId = team.getTeamId();
		maxRoll = team instanceof PlayerAlliance alliance && alliance.isInLeague() ? 10000
				: team instanceof PlayerAlliance ? 1000 : 100;
		lastLootGroupRules = team.getLootGroupRules();
	}

	/** 设置范围内的玩家 / Sets the in-range players */
	public void setInRangePlayers(Collection<Player> inRangePlayers) {
		this.inRangePlayers = inRangePlayers;
	}

	/**
	 * 返回范围内的玩家。
	 * Returns the players in range.
	 *
	 * @return 范围内玩家 / in-range players
	 */
	public Collection<Player> getInRangePlayers() {
		return inRangePlayers;
	}

	/**
	 * 添加玩家状态。
	 * Adds a player status.
	 *
	 * @param player 玩家 / player
	 */
	public void addPlayerStatus(Player player) {
		playerStatus.add(player);
	}

	/**
	 * 移除玩家状态。
	 * Removes a player status.
	 *
	 * @param player 玩家 / player
	 */
	public void delPlayerStatus(Player player) {
		playerStatus.remove(player);
	}

	/**
	 * 返回玩家状态集合。
	 * Returns the player status collection.
	 *
	 * @return 玩家状态 / player status
	 */
	public Collection<Player> getPlayerStatus() {
		return playerStatus;
	}

	/**
	 * 玩家是否在状态列表中。
	 * Whether the player is in the status list.
	 *
	 * @return 是否在列表中 / true if found
	 */
	public boolean containsPlayerStatus(Player player) {
		return playerStatus.contains(player);
	}

	/**
	 * 是否自由拾取。
	 * Whether the drop is free for all.
	 *
	 * @return 是否自由拾取 / whether free for all
	 */
	public boolean isFreeForAll() {
		return isFreeForAll;
	}

	/** 开始自由拾取 / Starts free for all */
	public void startFreeForAll() {
		isFreeForAll = true;
		distributionId = 0;
		allowedLooters.clear();
	}

	/** 返回对象 ID / Returns the object id */
	public final int getObjectId() {
		return objectId;
	}

	/** 返回剩余消失时间 / Returns the remaining decay time */
	public long getRemainingDecayTime() {
		return remainingDecayTime;
	}

	/** 设置剩余消失时间 / Sets the remaining decay time */
	public void setRemainingDecayTime(long remainingDecayTime) {
		this.remainingDecayTime = remainingDecayTime;
	}
}

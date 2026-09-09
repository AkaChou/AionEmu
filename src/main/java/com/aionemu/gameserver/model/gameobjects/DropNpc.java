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
import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;

/**
 * 掉落 NPC 游戏对象。
 * Drop Npc game object.
 *
 * @author Simple
 */
@RequiredArgsConstructor
public class DropNpc {

	private final int objectId;
	/** 设置允许拾取者集合 / Sets the allowed looters */
	@Getter
	@Setter
	private Set<Integer> allowedLooters = new HashSet<>();
	/** 设置范围内的玩家 / Sets the in-range players */
	@Getter
	@Setter
	private Collection<Player> inRangePlayers = new ArrayList<Player>();
	/**
	 * 返回玩家状态集合。
	 * Returns the player status collection.
	 *
	 * @return 玩家状态 / player status
	 */
	@Getter
	private final Collection<Player> playerStatus = new ArrayList<Player>();
	/**
	 * 设置正在拾取的玩家。
	 * Sets the player currently looting.
	 *
	 * @param player 正在拾取的玩家 / the lootingPlayer to set
	 */
	@Getter
	@Setter
	private Player lootingPlayer = null;
	/**
	 * 设置分配 ID。
	 * Sets the distribution id.
	 *
	 * @param distributionId 分配 ID / distribution id
	 */
	@Getter
	@Setter
	private int distributionId = 0;
	/**
	 * 设置分配类型。
	 * Sets the distribution type.
	 *
	 * @param distributionType 分配类型 / distribution type
	 */
	@Setter
	private boolean distributionType;
	/**
	 * 设置当前索引。
	 * Sets the current index.
	 *
	 * @param currentIndex 当前索引 / current index
	 */
	@Getter
	@Setter
	private int currentIndex = 0;
	private WeakReference<TemporaryPlayerTeam<? extends TeamMember<Player>>> lootingTeam;
	/** 返回拾取队伍 ID / Returns the looting team id */
	@Getter
	private int lootingTeamId;
	/** 返回最大点数 / Returns the max roll */
	@Getter
	private int maxRoll;
	private LootGroupRules lastLootGroupRules;
	/**
	 * 是否自由拾取。
	 * Whether the drop is free for all.
	 *
	 * @return 是否自由拾取 / whether free for all
	 */
	@Getter
	private boolean isFreeForAll = false;
	/** 返回剩余消失时间 / Returns the remaining decay time */
	@Getter
	@Setter
	private long remainingDecayTime;

	/** 添加允许拾取者 / Adds an allowed looter */
	public void setAllowedLooter(Player player) {
		allowedLooters.add(player.getObjectId());
	}

	/** 是否允许拾取 / Whether allowed to loot */
	public boolean isAllowedToLoot(Player player) {
		return isFreeForAll || allowedLooters.contains(player.getObjectId());
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
	 * 返回分配类型。
	 * Returns the distribution type.
	 *
	 * @return 分配类型 / distribution type
	 */
	public boolean getDistributionType() {
		return distributionType;
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
	 * 玩家是否在状态列表中。
	 * Whether the player is in the status list.
	 *
	 * @return 是否在列表中 / true if found
	 */
	public boolean containsPlayerStatus(Player player) {
		return playerStatus.contains(player);
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
}

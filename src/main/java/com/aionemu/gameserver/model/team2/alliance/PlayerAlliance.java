package com.aionemu.gameserver.model.team2.alliance;

import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.TeamType;
import com.aionemu.gameserver.model.team2.TemporaryPlayerTeam;
import com.aionemu.gameserver.model.team2.league.League;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.google.common.base.Preconditions;

/**
 * 玩家联盟，用于团队2相关逻辑。
 * Player Alliance for team 2 logic.
 */

public class PlayerAlliance extends TemporaryPlayerTeam<PlayerAllianceMember> {
	private final Map<Integer, PlayerAllianceGroup> groups = new HashMap<Integer, PlayerAllianceGroup>();
	private final List<Integer> viceCaptainIds = new CopyOnWriteArrayList<Integer>();
	private int allianceReadyStatus;
	private TeamType type;
	private League league;
	private int killCount = 0;
	private int bgIndex = -1;

	public PlayerAlliance(PlayerAllianceMember leader, TeamType type) {
		super(GameWorldBootstrapServices.idFactory().nextId());
		this.type = type;
		initializeTeam(leader);
		for (int groupId = 1000; groupId <= 1003; groupId++) {
			groups.put(groupId, new PlayerAllianceGroup(this, groupId));
		}
	}

	/** 添加成员 / Adds member */
	@Override
	public void addMember(PlayerAllianceMember member) {
		super.addMember(member);
		PlayerAllianceGroup openAllianceGroup = getOpenAllianceGroup();
		openAllianceGroup.addMember(member);
	}

	/** 移除成员 / Removes member */
	@Override
	public void removeMember(PlayerAllianceMember member) {
		super.removeMember(member);
		member.getPlayerAllianceGroup().removeMember(member);
	}

	/** 是否已满。 / Whether Full. */
	@Override
	public boolean isFull() {
		return size() == 24;
	}

	/** 返回最小经验玩家等级 / Returns the min exp player level*/
	@Override
	public int getMinExpPlayerLevel() {
		int minLevel = 99;
		for (PlayerAllianceMember member : members.values()) {
			minLevel = Math.min(minLevel, member.getLevel());
		}
		return minLevel;
	}

	/** 返回最大经验玩家等级 / Returns the max exp player level*/
	@Override
	public int getMaxExpPlayerLevel() {
		int maxLevel = 1;
		for (PlayerAllianceMember member : members.values()) {
			maxLevel = Math.max(maxLevel, member.getLevel());
		}
		return maxLevel;
	}

	/** 返回未满的联盟队伍 / Returns the open alliance group */
	public PlayerAllianceGroup getOpenAllianceGroup() {
		lock();
		try {
			for (int groupId = 1000; groupId <= 1003; groupId++) {
				PlayerAllianceGroup playerAllianceGroup = groups.get(groupId);
				if (!playerAllianceGroup.isFull()) {
					return playerAllianceGroup;
				}
			}
		} finally {
			unlock();
		}
		throw new IllegalStateException("All alliance groups are full.");
	}

	/** 获取联盟队伍。 / Returns the alliance group. */
	public PlayerAllianceGroup getAllianceGroup(Integer allianceGroupId) {
		PlayerAllianceGroup allianceGroup = groups.get(allianceGroupId);
		Preconditions.checkNotNull(allianceGroup, "No such alliance group " + allianceGroupId);
		return allianceGroup;
	}

	/** 返回副队长 ID 列表 / Returns the vice captain ids */
	public final List<Integer> getViceCaptainIds() {
		return viceCaptainIds;
	}

	/**
	 * 判断玩家是否为副队长。
	 * Checks whether the player is a vice captain.
	 *
	 * @param player 玩家 / player
	 * @return 是副队长时为 {@code true} / {@code true} if vice captain
	 */
	public final boolean isViceCaptain(Player player) {
		return viceCaptainIds.contains(player.getObjectId());
	}

	/**
	 * 判断玩家是否为队长或副队长。
	 * Checks whether the player is the leader or a vice captain.
	 *
	 * @param player 玩家 / player
	 * @return 是队长或副队长时为 {@code true} / {@code true} if leader or vice captain
	 */
	public final boolean isSomeCaptain(Player player) {
		return isLeader(player) || isViceCaptain(player);
	}

	/** 返回联盟就绪状态 / Returns the alliance ready status */
	public int getAllianceReadyStatus() {
		return allianceReadyStatus;
	}

	/** 设置联盟就绪状态 / Sets the alliance ready status */
	public void setAllianceReadyStatus(int allianceReadyStatus) {
		this.allianceReadyStatus = allianceReadyStatus;
	}

	/** 获取战团。 / Returns the league. */
	public final League getLeague() {
		return league;
	}

	/** 设置战团。 / Sets the league. */
	public final void setLeague(League league) {
		this.league = league;
	}

	/**
	/**
	 * 是否已加入联合部队。
	 * Whether the alliance is in a league.
	 *
	 * @return 已加入联合部队时为 {@code true} / {@code true} if in league
	 */
	public final boolean isInLeague() {
		return this.league != null;
	}

	/** 小队大小 / Group Size*/
	public final int groupSize() {
		return groups.size();
	}

	/** 返回组 / Returns the groups*/
	public final Collection<PlayerAllianceGroup> getGroups() {
		return groups.values();
	}

	/** 获取团队类型。 / Returns the team type. */
	public TeamType getTeamType() {
		return type;
	}

	/** 设置击杀数 / Sets the kill count */
	public void setKillCount(int killCount) {
		this.killCount = killCount;
	}

	/** 返回击杀数 / Returns the kill count */
	public int getKillCount() {
		return killCount;
	}

	/** 设置战场索引 / Sets the bg index */
	public void setBgIndex(int bgIndex) {
		this.bgIndex = bgIndex;
	}

	/** 返回战场索引 / Returns the bg index */
	public int getBgIndex() {
		return bgIndex;
	}
}

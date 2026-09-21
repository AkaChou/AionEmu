package com.aionemu.gameserver.model.team2.group;

import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

import java.util.Collection;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.TeamType;
import com.aionemu.gameserver.model.team2.TemporaryPlayerTeam;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * 玩家队伍，用于团队2相关逻辑。
 * Player Group for team 2 logic.
 */

@Getter
@Setter
public class PlayerGroup extends TemporaryPlayerTeam<PlayerGroupMember> {
	private final TeamType type;
	/** 设置 bg index / Sets the bg index */
	private int bgIndex = -1;
	/** 设置 kill count / Sets the kill count */
	private int killCount = 0;
	/** 设置 buff id / Sets the buff id */
	private int buffId = 0;
	private final PlayerGroupStats playerGroupStats;
	private final Map<Integer, Player> groupMembers = new LinkedHashMap<Integer, Player>();

	public PlayerGroup(PlayerGroupMember leader, TeamType type) {
		super(GameWorldBootstrapServices.idFactory().nextId());
		this.playerGroupStats = new PlayerGroupStats(this);
		this.type = type;
		initializeTeam(leader);
	}

	/** 添加 member / Adds member */
	@Override
	public void addMember(PlayerGroupMember member) {
		super.addMember(member);
		playerGroupStats.onAddPlayer(member);
		member.getObject().setPlayerGroup2(this);
	}

	/** 移除 member / Removes member */
	@Override
	public void removeMember(PlayerGroupMember member) {
		super.removeMember(member);
		playerGroupStats.onRemovePlayer(member);
		member.getObject().setPlayerGroup2(null);
	}

	/** 是否已满。 / Whether Full. */
	@Override
	public boolean isFull() {
		return size() == 6;
	}

	/** 返回最小经验玩家等级 / Returns the min exp player level*/
	@Override
	public int getMinExpPlayerLevel() {
		return playerGroupStats.getMinExpPlayerLevel();
	}

	/** 返回最大经验玩家等级 / Returns the max exp player level*/
	@Override
	public int getMaxExpPlayerLevel() {
		return playerGroupStats.getMaxExpPlayerLevel();
	}

	/** 获取团队类型。 / Returns the team type. */
	public TeamType getTeamType() {
		return type;
	}

	/** 返回 member obj ids / Returns the member obj ids */
	public Collection<Integer> getMemberObjIds() {
		return groupMembers.keySet();
	}

	/** 返回组 ID / Returns the group id */
	public int getGroupId() {
		return this.getObjectId();
	}
}

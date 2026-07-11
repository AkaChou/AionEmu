package com.aionemu.gameserver.model.team2.group;

import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

import java.util.Collection;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.TeamType;
import com.aionemu.gameserver.model.team2.TemporaryPlayerTeam;
import com.aionemu.gameserver.utils.idfactory.IDFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 玩家队伍，用于团队2相关逻辑。
 * Player Group for team 2 logic.
 */

public class PlayerGroup extends TemporaryPlayerTeam<PlayerGroupMember> {
	private TeamType type;
	private int bgIndex = -1;
	private int killCount = 0;
	private int buffId = 0;
	private final PlayerGroupStats playerGroupStats;
	private Map<Integer, Player> groupMembers = new LinkedHashMap<Integer, Player>();

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

	/** 设置 kill count / Sets the kill count */
	public void setKillCount(int killCount) {
		this.killCount = killCount;
	}

	/** 返回 kill count / Returns the kill count */
	public int getKillCount() {
		return killCount;
	}

	/** 设置 bg index / Sets the bg index */
	public void setBgIndex(int bgIndex) {
		this.bgIndex = bgIndex;
	}

	/** 返回 bg index / Returns the bg index */
	public int getBgIndex() {
		return bgIndex;
	}

	/** 返回 member obj ids / Returns the member obj ids */
	public Collection<Integer> getMemberObjIds() {
		return groupMembers.keySet();
	}

	/** 返回组 ID / Returns the group id */
	public int getGroupId() {
		return this.getObjectId();
	}

	/** 设置 buff id / Sets the buff id */
	public void setBuffId(int buffId) {
		this.buffId = buffId;
	}

	/** 返回增益 ID / Returns the buff id */
	public int getBuffId() {
		return buffId;
	}
}

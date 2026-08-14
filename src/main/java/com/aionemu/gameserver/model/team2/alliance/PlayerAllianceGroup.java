package com.aionemu.gameserver.model.team2.alliance;

import com.aionemu.gameserver.model.team2.TemporaryPlayerTeam;

/**
 * 玩家联盟队伍，用于团队2相关逻辑。
 * Player Alliance Group for team 2 logic.
 *
 * @author ATracer
 */
public class PlayerAllianceGroup extends TemporaryPlayerTeam<PlayerAllianceMember> {

	private final PlayerAlliance alliance;

	public PlayerAllianceGroup(PlayerAlliance alliance, Integer objId) {
		super(objId);
		this.alliance = alliance;
	}

	/** 添加成员 / Adds member */
	@Override
	public void addMember(PlayerAllianceMember member) {
		super.addMember(member);
		member.setPlayerAllianceGroup(this);
		member.setAllianceId(getTeamId());
	}

	/** 移除成员 / Removes member */
	@Override
	public void removeMember(PlayerAllianceMember member) {
		super.removeMember(member);
		member.setPlayerAllianceGroup(null);
	}

	/** 是否已满。 / Whether Full. */
	@Override
	public boolean isFull() {
		return size() == 6;
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

	/** 获取联盟。 / Returns the alliance. */
	public PlayerAlliance getAlliance() {
		return alliance;
	}
}

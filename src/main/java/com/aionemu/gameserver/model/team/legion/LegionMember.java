package com.aionemu.gameserver.model.team.legion;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * 军团 Member，用于团队相关逻辑。
 * Legion Member for team logic.
 *
 * @author Simple
 */
@Getter
@Setter
@NoArgsConstructor
public class LegionMember {
	/**
	 * @param objectId the objectId to set
	 */
	private int objectId = 0;
	/**
	 * @param legion the legion to set
	 */
	protected Legion legion = null;
	/**
	 * @param nickname the nickname to set
	 */
	protected String nickname = "";
	/**
	 * @param selfIntro the selfIntro to set
	 */
	protected String selfIntro = "";
	/**
	 * @return the challengeScore
	 */
	protected int challengeScore;
	/**
	 * @param rank the rank to set
	 */
	protected LegionRank rank = LegionRank.VOLUNTEER;

	/**
	 * 若玩家稍后定义则调用此构造。
	 * Called when the player is defined later.
	 */
	public LegionMember(int objectId) {
		this.objectId = objectId;
	}

	/**
	 * 创建军团时调用此构造。
	 * Called when a legion is created.
	 */
	public LegionMember(int objectId, Legion legion, LegionRank rank) {
		this.setObjectId(objectId);
		this.setLegion(legion);
		this.setRank(rank);
	}

	/**
	 * @return 是否军团长 / Whether brigade general
	 */
	public boolean isBrigadeGeneral() {
		return rank == LegionRank.BRIGADE_GENERAL;
	}

	/**
	 * @param amount
	 */
	public void increaseChallengeScore(int amount) {
		this.challengeScore += amount;
	}

	/**
	 * 检查成员是否拥有指定权限。
	 * Checks whether the member has the given rights.
	 *
	 * @param permissions 权限掩码 / permissions mask
	 * @return 是否拥有权限 / whether the member has the rights
	 */
	public boolean hasRights(LegionPermissionsMask permissions) {
		int legionarPermission = 0;
		switch (this.getRank()) {
		case BRIGADE_GENERAL:
			return true;
		case DEPUTY:
			legionarPermission = legion.getDeputyPermission();
			break;
		case CENTURION:
			legionarPermission = legion.getCenturionPermission();
			break;
		case LEGIONARY:
			legionarPermission = legion.getLegionaryPermission();
			break;
		case VOLUNTEER:
			legionarPermission = legion.getVolunteerPermission();
			break;
		}
		return permissions.can(legionarPermission);
	}
}

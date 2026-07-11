package com.aionemu.gameserver.model.team2.alliance;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.PlayerTeamMember;

/**
 * 玩家联盟 Member，用于团队2相关逻辑。
 * Player Alliance Member for team 2 logic.
 *
 * @author ATracer
 */
public class PlayerAllianceMember extends PlayerTeamMember {

	private int allianceId;

	public PlayerAllianceMember(Player player) {
		super(player);
	}

	/** 返回 alliance id / Returns the alliance id */
	public int getAllianceId() {
		return allianceId;
	}

	/** 设置 alliance id / Sets the alliance id */
	public void setAllianceId(int allianceId) {
		this.allianceId = allianceId;
	}

	/** 获取玩家联盟队伍。 / Returns the player alliance group. */
	public final PlayerAllianceGroup getPlayerAllianceGroup() {
		return getObject().getPlayerAllianceGroup2();
	}

	/** 设置玩家联盟队伍。 / Sets the player alliance group. */
	public final void setPlayerAllianceGroup(PlayerAllianceGroup playerAllianceGroup) {
		getObject().setPlayerAllianceGroup2(playerAllianceGroup);
	}
}

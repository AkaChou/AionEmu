package com.aionemu.gameserver.model.team2.common.events;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.TeamMember;
import com.aionemu.gameserver.model.team2.TemporaryPlayerTeam;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SHOW_BRAND;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.google.common.base.Predicate;

/**
 * ShowBrand 活动，用于团队2相关逻辑。
 * Show Brand Event for team 2 logic.
 *
 * @author ATracer
 */
public class ShowBrandEvent<T extends TemporaryPlayerTeam<? extends TeamMember<Player>>> extends AlwaysTrueTeamEvent
		implements Predicate<Player> {

	private final T team;
	private final int targetObjId;
	private final int brandId;

	public ShowBrandEvent(T team, int targetObjId, int brandId) {
		this.team = team;
		this.targetObjId = targetObjId;
		this.brandId = brandId;
	}

	/** 处理活动。 / Handle event. */
	@Override
	public void handleEvent() {
		team.applyOnMembers(this);
	}

	/** 应用。 / Apply. */
	@Override
	public boolean apply(Player member) {
		PacketSendUtility.sendPacket(member, new SM_SHOW_BRAND(brandId, targetObjId));
		return true;
	}
}

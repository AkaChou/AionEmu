package com.aionemu.gameserver.model.team2.common.events;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.TemporaryPlayerTeam;

/**
 * ChangeLeader 活动，用于团队2相关逻辑。
 * Change Leader Event for team 2 logic.
 *
 * @author ATracer
 */
@Slf4j(topic = "com.aionemu.gameserver.model.team2.group.events.ChangeGroupLeaderEvent")
public abstract class ChangeLeaderEvent<T extends TemporaryPlayerTeam<?>> extends AbstractTeamPlayerEvent<T> {

	public ChangeLeaderEvent(T team, Player eventPlayer) {
		super(team, eventPlayer);
	}

	/**
	 * 新队长要么为空，要么应当在线。
	 * New leader either is null or should be online.
	 */
	@Override
	public boolean checkCondition() {
		return eventPlayer == null || eventPlayer.isOnline();
	}

	/** 应用。 / Apply. */
	@Override
	public boolean apply(Player player) {
		if (!player.getObjectId().equals(team.getLeader().getObjectId()) && player.isOnline()) {
			changeLeaderTo(player);
			return false;
		}
		return true;
	}

	/**
	 * @param oldLeader 旧队长 / old leader
	 */
	protected void checkLeaderChanged(Player oldLeader) {
		if (team.isLeader(oldLeader)) {
			log.info(I18n.get("log.6718e5fd978c", team.size(), team.onlineMembers()));
		}
	}

	protected abstract void changeLeaderTo(Player player);
}

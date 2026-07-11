package com.aionemu.gameserver.model.team2.common.events;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.TeamEvent;
import com.aionemu.gameserver.model.team2.TemporaryPlayerTeam;
import com.google.common.base.Predicate;

/**
 * 抽象团队玩家活动，用于团队2相关逻辑。
 * Abstract Team Player Event for team 2 logic.
 *
 * @author ATracer
 */
public abstract class AbstractTeamPlayerEvent<T extends TemporaryPlayerTeam<?>>
		implements Predicate<Player>, TeamEvent {

	protected final T team;
	protected final Player eventPlayer;

	public AbstractTeamPlayerEvent(T team, Player eventPlayer) {
		this.team = team;
		this.eventPlayer = eventPlayer;
	}
}

package com.aionemu.gameserver.model.team2.common.events;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.TeamMember;
import com.aionemu.gameserver.model.team2.TemporaryPlayerTeam;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 团队基纳 Distribution 活动，用于团队2相关逻辑。
 * Team Kinah Distribution Event for team 2 logic.
 *
 * @author ATracer
 */
public class TeamKinahDistributionEvent<T extends TemporaryPlayerTeam<? extends TeamMember<Player>>>
		extends AbstractTeamPlayerEvent<T> {

	private final long amount;
	private long rewardPerPlayer;
	private long teamSize;

	public TeamKinahDistributionEvent(T team, Player distributor, long amount) {
		super(team, distributor);
		this.amount = amount;
	}

	/**
	 * @return 检查条件是否满足 / Check condition
	 */
	@Override
	public boolean checkCondition() {
		return team.hasMember(eventPlayer.getObjectId());
	}

	/** 处理活动。 / Handle event. */
	@Override
	public void handleEvent() {
		if (eventPlayer.getInventory().getKinah() < amount) {
			PacketSendUtility.sendPacket(eventPlayer, SM_SYSTEM_MESSAGE.STR_NOT_ENOUGH_MONEY);
			return;
		}

		teamSize = team.onlineMembers();
		if (teamSize > 1 && teamSize <= amount && eventPlayer.getInventory().tryDecreaseKinah(amount)) {
			rewardPerPlayer = amount / teamSize;
			team.applyOnMembers(this);
		}
	}

	/** 应用。 / Apply. */
	@Override
	public boolean apply(Player member) {
		if (member.isOnline()) {
			if (member.equals(eventPlayer)) {
				member.getInventory().increaseKinah(rewardPerPlayer);
				PacketSendUtility.sendPacket(eventPlayer,
						new SM_SYSTEM_MESSAGE(1390247, amount, teamSize, rewardPerPlayer));
			} else {
				member.getInventory().increaseKinah(rewardPerPlayer);
				PacketSendUtility.sendPacket(member,
						new SM_SYSTEM_MESSAGE(1390248, eventPlayer.getName(), amount, teamSize, rewardPerPlayer));
			}
		}
		return true;
	}
}

package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.configs.main.MembershipConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 设置额外称号（Bonus Title）的客户端包。
 * Client packet to set the player's bonus title.
 *
 * @author Ranastic (Encom)
 */
public class CM_BONUS_TITLE extends AionClientPacket {
	private int bonusTitleId;

	public CM_BONUS_TITLE(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		bonusTitleId = readH();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (bonusTitleId != 0xFFFF) {
			if (!player.getTitleList().contains(bonusTitleId)
					&& !player.havePermission(MembershipConfig.TITLES_ADDITIONAL_ENABLE)) {
				return;
			}
		}
		player.getTitleList().setBonusTitle(bonusTitleId);
	}
}

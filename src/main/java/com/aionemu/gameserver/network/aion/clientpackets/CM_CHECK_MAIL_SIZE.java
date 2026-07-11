package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 校验邮箱信件数量并同步邮件列表的客户端包。
 * Client packet to verify mailbox letter count and sync the mail list.
 *
 * @author Rinzler (Encom)
 */
public class CM_CHECK_MAIL_SIZE extends AionClientPacket {
	public int mailSize;

	public CM_CHECK_MAIL_SIZE(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		mailSize = readC();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player.getMailbox().getLetters().size() != mailSize) {
			return;
		} else {
			player.getMailbox().sendMailList(false);
		}
	}
}

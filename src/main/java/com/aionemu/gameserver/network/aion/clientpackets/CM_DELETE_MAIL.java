package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.mail.MailService;

/**
 * 请求删除邮件的客户端包。
 * Client packet requesting deletion of mail.
 *
 * @author kosyachok
 */
public class CM_DELETE_MAIL extends AionClientPacket {

	int[] mailObjId;

	/**
	 * 构造客户端包实例。
	 * Constructs a new client packet instance.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_DELETE_MAIL(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		int count = readC();
		mailObjId = new int[count];
		for (int i = 0; i < count; i++) {
			readC(); // 未知 / unk
			mailObjId[i] = readD();
		}
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		GameCoreGameplayServices.mailService().deleteMail(player, mailObjId);
	}
}
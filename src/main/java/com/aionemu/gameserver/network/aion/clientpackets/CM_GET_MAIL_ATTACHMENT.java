package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.mail.MailService;

/**
 * 领取邮件附件的客户端包。
 * Client packet that collects mail attachments.
 *
 * @author kosyachok
 */
public class CM_GET_MAIL_ATTACHMENT extends AionClientPacket {

	private int mailObjId;
	private int attachmentType;

	/**
	 * 构造客户端包实例。
	 * Constructs a new client packet instance.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_GET_MAIL_ATTACHMENT(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		mailObjId = readD();
		attachmentType = readC(); // 0 - item , 1 - kinah, 2- AP ??
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		GameCoreGameplayServices.mailService().getAttachments(player, mailObjId, attachmentType);
	}
}
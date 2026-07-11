package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.model.gameobjects.LetterType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.mail.MailService;

/**
 * 客户端发送邮件请求包。
 * Client packet for sending mail.
 *
 * @author Aion Gates, xTz
 */
public class CM_SEND_MAIL extends AionClientPacket {
	private String recipientName;
	private String title;
	private String message;
	private int itemObjId;
	private int itemCount;
	private int kinahCount;
	private int idLetterType;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_SEND_MAIL(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		recipientName = readS();
		title = readS();
		message = readS();
		itemObjId = readD();
		itemCount = readD();
		readD();
		kinahCount = readD();
		readD();
		idLetterType = readC();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (!player.isTrading() && kinahCount < 1000000000 && kinahCount > -1 && itemCount > -2) {
			GameCoreGameplayServices.mailService().sendMail(player, recipientName, title, message, itemObjId, itemCount, kinahCount,
					0, LetterType.getLetterTypeById(idLetterType));
		}
	}
}

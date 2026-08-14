package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 客户端对 SM_QUESTION_WINDOW 确认框的响应包。
 * Client response to an SM_QUESTION_WINDOW confirmation dialog.
 *
 * @author Ben
 * @author Sarynth
 */
public class CM_QUESTION_RESPONSE extends AionClientPacket {

	private int questionid;
	private int response;
	@SuppressWarnings("unused")
	private int senderid;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_QUESTION_RESPONSE(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readImpl() {
		questionid = readD();
		response = readC(); // 是/否 / y/n
		readC(); // unk 0x00 - 0x01 ?
		readH();
		senderid = readD();
		readD();
		readH();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player.isTrading()) {
			return;
		}
		player.getResponseRequester().respond(questionid, response);
	}
}

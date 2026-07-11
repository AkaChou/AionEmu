package com.aionemu.gameserver.network.aion.clientpackets;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MACRO_RESULT;
import com.aionemu.gameserver.services.player.PlayerService;

/**
	 * 负责删除宏的数据包。 / Packet that is responsible for macro deletion.<br> Client sends id in the macro list.<br> For instance client has 4 macros and we are going to delete macro #3.<br> Client sends request to delete macro #3.<br> And macro #4 becomes macro #3.<br> So we have to use a list to store macros properly.
	 */

/**
 * 删除指定槽位宏的客户端包。
 * Client packet for deleting a macro by slot.
 *
 * @author SoulKeeper
 */
@Slf4j
public class CM_MACRO_DELETE extends AionClientPacket {

	/**
	 * 待删除的宏 ID / Macro id that has to be deleted
	 */
	private int macroPosition;
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_MACRO_DELETE(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	/**
	 * 读取宏 ID / Reading macro id
	 */
	@Override
	protected void readImpl() {
		macroPosition = readC();
	}

	/**
	 * 记录日志 / Logging
	 */
	@Override
	protected void runImpl() {
		log.debug("Request to delete macro #" + macroPosition);

		PlayerService.removeMacro(getConnection().getActivePlayer(), macroPosition);

		sendPacket(SM_MACRO_RESULT.SM_MACRO_DELETED);
	}
}

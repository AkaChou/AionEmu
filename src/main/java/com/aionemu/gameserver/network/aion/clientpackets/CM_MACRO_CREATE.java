package com.aionemu.gameserver.network.aion.clientpackets;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MACRO_RESULT;
import com.aionemu.gameserver.services.player.PlayerService;

/**
	 * 创建请求。 / Request to create.
	 */

/**
 * 创建或更新宏的客户端包。
 * Client packet for creating or updating a macro.
 *
 * @author SoulKeeper
 */
@Slf4j
public class CM_MACRO_CREATE extends AionClientPacket {

	/**
	 * 宏编号 .Fist 为 1 , second 为 2.Starting 从 1 , not 从 0。 / Macro number. Fist is 1, second is 2. Starting from 1, not from 0
	 */
	private int macroPosition;
	/**
	 * 表示宏的 XML / XML that represents the macro
	 */
	private String macroXML;
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_MACRO_CREATE(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	/**
	 * 读取宏数据 / Read macro data
	 */
	@Override
	protected void readImpl() {
		macroPosition = readC();
		macroXML = readS();
	}

	/**
	 * 记录日志 / Logging
	 */
	@Override
	protected void runImpl() {
		log.debug(String.format("Created Macro #%d: %s", macroPosition, macroXML));

		PlayerService.addMacro(getConnection().getActivePlayer(), macroPosition, macroXML);

		sendPacket(SM_MACRO_RESULT.SM_MACRO_CREATED);
	}
}

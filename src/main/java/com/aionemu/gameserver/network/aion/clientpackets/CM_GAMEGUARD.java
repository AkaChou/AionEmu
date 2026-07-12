package com.aionemu.gameserver.network.aion.clientpackets;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.antihack.AntiHackService;

/**
 * 反外挂校验数据的客户端包。
 * anti-hack validation data.
 *
 * @author Alcapwnd
 */
@Slf4j
public class CM_GAMEGUARD extends AionClientPacket {

	private int size;

	/**
	 * 构造客户端包实例。
	 * Constructs a new client packet instance.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_GAMEGUARD(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aionemu.commons.network.packet.BaseClientPacket#readImpl()
	 */
	@Override
	protected void readImpl() {
		size = readD();
		readB(size);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aionemu.commons.network.packet.BaseClientPacket#runImpl()
	 */
	@Override
	protected void runImpl() {
		log.info(I18n.get("log.4ba720f2d8b1", size));
		Player player = getConnection().getActivePlayer();
		AntiHackService.checkAionBin(size, player);
	}
}
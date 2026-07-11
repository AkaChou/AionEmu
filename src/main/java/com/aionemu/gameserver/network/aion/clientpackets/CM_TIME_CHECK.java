package com.aionemu.gameserver.network.aion.clientpackets;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TIME_CHECK;

/**
 * 客户端时间校验/心跳请求包，回传客户端时间戳用于同步。
 * Client packet for time check (ping/pong); echoes the client timestamp for sync.
 *
 * @author -Nemesiss-
 */
public class CM_TIME_CHECK extends AionClientPacket {

	/**
	 * 纳秒时间 / 1000000 / Nano time / 1000000
	 */
	private int nanoTime;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_TIME_CHECK(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readImpl() {
		nanoTime = readD();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void runImpl() {
		AionConnection client = getConnection();
		int timeNow = (int) (System.nanoTime() / 1000000);
		@SuppressWarnings("unused")
		int diff = timeNow - nanoTime;
		client.sendPacket(new SM_TIME_CHECK(nanoTime));

		// log.info(I18n.get("log.432d248cd1f8", nanoTime, timeNow, diff));
	}
}

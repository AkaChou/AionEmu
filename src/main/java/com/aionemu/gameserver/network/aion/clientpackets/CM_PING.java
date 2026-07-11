package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.configs.main.SecurityConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PONG;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.audit.AuditLogger;

/**
 * 客户端心跳包，用于保活并检测异常过快的 ping 间隔（加速外挂）。
 * Client keep-alive ping packet; detects abnormally fast ping intervals (timer cheats).
 *
 * @author -Nemesiss- modified by Undertrey
 */
public class CM_PING extends AionClientPacket {

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_PING(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readImpl() {
		readH(); // 未知 / unk
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		long lastMS = getConnection().getLastPingTimeMS();

		if (lastMS > 0 && player != null) {
			long pingInterval = System.currentTimeMillis() - lastMS;
			// PingInterval should be 3min (180000ms)
			if (pingInterval < SecurityConfig.PING_INTERVAL * 1000) {// client timer cheat
				AuditLogger.info(player, "Possible client timer cheat kicking player: " + pingInterval + ", ip="
						+ getConnection().getIP());
				if (SecurityConfig.SECURITY_ENABLE) {
					PacketSendUtility.sendMessage(player,
							"You have been triggered Speed Hack detection so you're disconnected.");
					getConnection().closeNow();
				}
			}
		}
		getConnection().setLastPingTimeMS(System.currentTimeMillis());
		sendPacket(new SM_PONG());
	}
}

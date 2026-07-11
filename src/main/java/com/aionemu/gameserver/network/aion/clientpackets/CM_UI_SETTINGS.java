package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 客户端 UI 设置同步请求包（界面布局、快捷键、房屋好友等）。
 * Client packet syncing UI settings (layout, shortcuts, house buddies, etc.).
 *
 * @author ATracer
 */
public class CM_UI_SETTINGS extends AionClientPacket {

	int settingsType;
	byte[] data;
	int size;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_UI_SETTINGS(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		settingsType = readC();
		readH();
		size = readH();
		data = readB(getRemainingBytes());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();

		if (settingsType == 0) {
			player.getPlayerSettings().setUiSettings(data);
		} else if (settingsType == 1) {
			player.getPlayerSettings().setShortcuts(data);
		} else if (settingsType == 2) {
			player.getPlayerSettings().setHouseBuddies(data);
		}
	}
}

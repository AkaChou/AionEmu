package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.HouseObject;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.world.World;

/**
 * 客户端使用房屋物件请求包，触发对应 {@link HouseObject} 对话逻辑。
 * Client packet for using a house object; dispatches to the {@link HouseObject} dialog handler.
 */
public class CM_USE_HOUSE_OBJECT extends AionClientPacket {

	int itemObjectId;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_USE_HOUSE_OBJECT(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	protected void readImpl() {
		itemObjectId = readD();
	}

	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player == null) {
			return;
		}
		VisibleObject visObject = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findVisibleObject(itemObjectId);
		if (visObject == null) {
			return;
		}
		if (visObject instanceof HouseObject) {
			((HouseObject<?>) visObject).getController().onDialogRequest(player);
		}
	}
}

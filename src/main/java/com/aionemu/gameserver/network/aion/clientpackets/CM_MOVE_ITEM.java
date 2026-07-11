package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.item.ItemMoveService;

/**
 * 在背包/装备等仓库间移动物品的客户端包。
 * Client packet for moving an item between inventory/equipment storages.
 *
 * @author alexa026, kosyachok
 */
public class CM_MOVE_ITEM extends AionClientPacket {

	/**
	 * 客户端要对话的目标对象 ID，0 表示取消选择 / Target object id that client wants to TALK WITH or 0 if wants to unselect
	 */
	private int targetObjectId;
	private byte source;
	private byte destination;
	private short slot;
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_MOVE_ITEM(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		targetObjectId = readD();// 空 / empty
		source = readSC(); // FROM (0 - player inventory, 1 - regular warehouse, 2 - account warehouse, 3 -
							// 军团 / legion
		destination = readSC(); // TO
		slot = readSH();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		ItemMoveService.moveItem(player, targetObjectId, source, destination, slot);
	}
}

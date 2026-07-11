package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.actions.ExtractAction;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection;

/**
 * 请求提取装备强化等级的客户端包。
 * Client packet requesting enchantment extraction from an item.
 */
public class CM_ENCHANTMENT_EXTRACTION extends AionClientPacket {

	private int itemId;

	/**
	 * 构造客户端包实例。
	 * Constructs a new client packet instance.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_ENCHANTMENT_EXTRACTION(int opcode, AionConnection.State state, AionConnection.State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		itemId = readD();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		Item item = player.getInventory().getItemByObjId(itemId);
		ExtractAction action = new ExtractAction();
		if (action.canAct(player, item, item)) {
			action.act(player, item, item);
		}
	}
}

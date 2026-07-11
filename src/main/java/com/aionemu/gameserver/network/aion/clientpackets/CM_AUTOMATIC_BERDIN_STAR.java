package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.actions.AbstractItemAction;
import com.aionemu.gameserver.model.templates.item.actions.ItemActions;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 自动使用伯丁之星（Berdin Star）类道具的客户端包。
 * Client packet to automatically use a Berdin Star item.
 *
 * @author Wnkrz
 */
public class CM_AUTOMATIC_BERDIN_STAR extends AionClientPacket {
	private int ItemObjectId;

	public CM_AUTOMATIC_BERDIN_STAR(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		ItemObjectId = readD();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		Item item = player.getInventory().getItemByObjId(ItemObjectId);
		ItemActions itemActions = item.getItemTemplate().getActions();
		for (AbstractItemAction itemAction : itemActions.getItemActions()) {
			if (itemAction.canAct(player, item, null)) {
				itemAction.act(player, item, null);
			}
		}
	}
}

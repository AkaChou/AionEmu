package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.actions.CompositionAction;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.restrictions.RestrictionsManager;

/**
 * 请求合成魔力石的客户端包。
 * Client packet requesting composition of manastones.
 *
 * @developer_note_dont_remove tool:165010001, 1st stone:166000094, 2nd stone: 166000095
 */
public class CM_COMPOSITE_STONES extends AionClientPacket {
	private int compinationToolItemObjectId;
	private int firstItemObjectId;
	private int secondItemObjectId;

	/**
	 * 构造客户端包实例。
	 * Constructs a new client packet instance.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_COMPOSITE_STONES(int opcode, AionConnection.State state, AionConnection.State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		compinationToolItemObjectId = readD();
		firstItemObjectId = readD();
		secondItemObjectId = readD();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player == null) {
			return;
		}
		if (player.isProtectionActive()) {
			player.getController().stopProtectionActiveTask();
		}
		if (player.isCasting()) {
			player.getController().cancelCurrentSkill();
		}
		Item tools = player.getInventory().getItemByObjId(compinationToolItemObjectId);
		if (tools == null) {
			return;
		}
		Item first = player.getInventory().getItemByObjId(firstItemObjectId);
		if (first == null) {
			return;
		}
		Item second = player.getInventory().getItemByObjId(secondItemObjectId);
		if (second == null) {
			return;
		}
		if (!RestrictionsManager.canUseItem(player, tools)) {
			return;
		}
		CompositionAction action = new CompositionAction();
		if (!action.canAct(player, tools, first, second)) {
			return;
		}
		action.act(player, tools, first, second);
	}
}
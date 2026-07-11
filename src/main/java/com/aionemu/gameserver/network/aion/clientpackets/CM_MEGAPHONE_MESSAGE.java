package com.aionemu.gameserver.network.aion.clientpackets;

import java.util.Iterator;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MEGAPHONE_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;

/**
 * 使用喇叭道具发送全服/阵营喊话的客户端包。
 * Client packet for megaphone shout messages (server/faction-wide).
 *
 * @author Ranastic
 */
public class CM_MEGAPHONE_MESSAGE extends AionClientPacket {
	private String chatMessage;
	private int itemObjectId;
	private boolean isAll = false;
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_MEGAPHONE_MESSAGE(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}
	/**
	 * 读取喇叭消息、道具对象 ID 与范围标志。
	 * Reads megaphone message, item object id, and range flag.
	 */
	@Override
	protected void readImpl() {
		chatMessage = readS();
		itemObjectId = readD();
	}
	/**
	 * 消耗喇叭道具并广播喊话消息。
	 * Consumes the megaphone item and broadcasts the shout.
	 */
	@Override
	protected void runImpl() {
		final Player activePlayer = getConnection().getActivePlayer();
		if (activePlayer == null) {
			return;
		}
		Item item = activePlayer.getInventory().getItemByObjId(itemObjectId);
		if (item == null) {
			return;
		}
		if ((item.getItemId() >= 188910000) && (item.getItemId() <= 188910009)) {
			this.isAll = true;
		}
		if ((item.getItemId() >= 188930000) && (item.getItemId() <= 188930008)) {
			this.isAll = true;
		}
		boolean deleteItem = activePlayer.getInventory().decreaseByObjectId(this.itemObjectId, 1);
		if (!deleteItem) {
			return;
		}
		Iterator<Player> players = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
		while (players.hasNext()) {
			Player player = (Player) players.next();
			if (this.isAll) {
				PacketSendUtility.sendPacket(player,
						new SM_MEGAPHONE_MESSAGE(activePlayer, this.chatMessage, item.getItemId(), this.isAll));
			} else if (activePlayer.getRace() == player.getRace()) {
				PacketSendUtility.sendPacket(player,
						new SM_MEGAPHONE_MESSAGE(activePlayer, this.chatMessage, item.getItemId(), this.isAll));
			}
		}
	}
}

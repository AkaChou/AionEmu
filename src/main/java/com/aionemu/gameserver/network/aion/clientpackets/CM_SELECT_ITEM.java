package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.DisassembleItem;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SELECT_ITEM_ADD;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 客户端自选物品箱选择奖励请求包。
 * Client packet for selecting a reward from a selectable item box.
 *
 * @author LightNing (ENCOM)
 */
public class CM_SELECT_ITEM extends AionClientPacket
{

	private int uniqueItemId;
	private int index;
	@SuppressWarnings("unused")
	private int unk;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_SELECT_ITEM(int opcode, AionConnection.State state, AionConnection.State... restStates)
	{
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl()
	{
		this.uniqueItemId = readD();
		this.unk = readD();
		this.index = readC();

	}

	@Override
	protected void runImpl()
	{
		final Player player = getConnection().getActivePlayer();
		final Item item = player.getInventory().getItemByObjId(this.uniqueItemId);
		if (item == null) {
			return;
		}
		final int nameId = item.getNameId();
		sendPacket(new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), player.getObjectId(), uniqueItemId, item.getItemId(), 0, 1, 1));
		boolean delete = player.getInventory().decreaseByObjectId(uniqueItemId, 1L);
		if (delete) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300423, new DescriptionId(nameId)));
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400452, new DescriptionId(nameId)));
			DisassembleItem selectitem = player.getDisassemblyItemLists().get(index);
			sendPacket(new SM_SELECT_ITEM_ADD(uniqueItemId, 0));
			ItemService.addItem(player, selectitem.getItemId(), selectitem.getCount());
		}
	}
}

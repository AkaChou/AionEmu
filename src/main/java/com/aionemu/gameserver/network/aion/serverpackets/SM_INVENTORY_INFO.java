package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.Collections;
import java.util.List;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob;

/**
 * 向客户端发送背包物品信息的服务端包。
 * Server packet that sends inventory item information to the client.
 *
 * @author -Nemesiss-
 * @updater alexa026
 * @finisher Avol ;d modified by ATracer
 * @fixedby -Nemesiss- :D
 */
public class SM_INVENTORY_INFO extends AionServerPacket {

	public static final int EMPTY = 0;
	public static final int FULL = 1;
	public int npcExpandsSize = 0;
	public int questExpandsSize = 0;

	private List<Item> items;
	private Player player;

	public int packetType = FULL;
	private boolean isFirstPacket;

	/**
	 * 构造完整背包信息包，包含物品列表与背包扩展容量。
	 * Creates a full inventory info packet with items and cube expand sizes.
	 *
	 * @param isFirstPacket 是否为首包 / whether this is the first packet
	 * @param items 物品列表 / list of items
	 * @param npcExpandsSize NPC 背包扩展格数 / cube expand size from NPC
	 * @param questExpandsSize 任务背包扩展格数 / cube expand size from quest
	 * target player
	 */
	public SM_INVENTORY_INFO(boolean isFirstPacket, List<Item> items, int npcExpandsSize, int questExpandsSize,
			Player player) {
		// 这应能防止客户端崩溃，但需查明物品何时为 null。 / this should prevent client crashes but need to discover when item is null
		items.removeAll(Collections.singletonList(null));
		this.isFirstPacket = isFirstPacket;
		this.items = items;
		this.npcExpandsSize = npcExpandsSize;
		this.questExpandsSize = questExpandsSize;
		this.player = player;
	}

	/**
	 * 构造空背包信息包。
	 * Creates an empty inventory info packet.
	 */
	public SM_INVENTORY_INFO() {
		this.packetType = EMPTY;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		if (this.packetType == EMPTY) {
			writeD(0);
			writeH(0);
			return;
		}

		// 背包部分有问题。 / something wrong with cube part.
		writeC(isFirstPacket ? 1 : 0);
		writeC(npcExpandsSize); // cube size from npc (so max 5 for now)
		writeC(questExpandsSize); // cube size from quest (so max 2 for now)
		writeC(0); // unk?
		writeH(items.size()); // number of entries

		for (Item item : items) {
			writeItemInfo(item);
		}
	}

	private void writeItemInfo(Item item) {
		ItemTemplate itemTemplate = item.getItemTemplate();

		writeD(item.getObjectId());
		writeD(itemTemplate.getTemplateId());
		writeNameId(itemTemplate.getNameId());

		ItemInfoBlob itemInfoBlob = ItemInfoBlob.getFullBlob(player, item);
		itemInfoBlob.writeMe(getBuf());

		writeH((int) (item.getEquipmentSlot() & 0xFFFF));
		// 可能是装备权限，与被动技能学习相关 / probably a right to equip the item, related to passive skill learn
		writeC(itemTemplate.isCloth() ? 1 : 0);
	}
}

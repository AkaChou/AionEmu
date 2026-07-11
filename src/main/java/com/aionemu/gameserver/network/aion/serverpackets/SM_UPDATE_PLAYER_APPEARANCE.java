package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.List;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.items.GodStone;
import com.aionemu.gameserver.model.items.ItemSlot;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.services.EnchantService;

/**
 * 更新玩家外观装备（皮肤、神石、染色、强化等级）的服务端包。
 * Server packet that updates a player's visual equipment (skin, godstone, dye, enchant).
 */
public class SM_UPDATE_PLAYER_APPEARANCE extends AionServerPacket {
	public int playerId;
	public int size;
	public List<Item> items;

	/**
	 * player object id
	 * @param items    外观装备列表 / visual equipment list
	 */
	public SM_UPDATE_PLAYER_APPEARANCE(int playerId, List<Item> items) {
		this.playerId = playerId;
		this.items = items;
		this.size = items.size();
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(playerId);

		int mask = 0;
		for (Item item : items) {
			if (item.getItemTemplate().isTwoHandWeapon()) {
				ItemSlot[] slots = ItemSlot.getSlotsFor(item.getEquipmentSlot());
				mask |= slots[0].getSlotIdMask();
			} else {
				mask |= item.getEquipmentSlot();
			}
		}

		writeD(mask); // item size HBS

		for (Item item : items) {
			writeD(item.getItemSkinTemplate().getTemplateId());
			GodStone godStone = item.getGodStone();
			writeD(godStone != null ? godStone.getItemId() : 0);
			writeD(item.getItemColor());
			writeH(EnchantService.EnchantLevel(item));// unk (0x00)
			writeH(0x00);
		}
	}
}

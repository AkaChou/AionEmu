package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.HashMap;
import java.util.Map;

import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.dorinerk_wardrobe.PlayerWardrobeEntry;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 月之商城（Luna Shop）操作结果的服务端包。
 * Server packet for Luna shop operation results.
 */
public class SM_LUNA_SHOP extends AionServerPacket {

	private int actionId;
	private int unk1;
	private int slotSize;
	private int fail;
	private ItemTemplate item;
	// 卡鲁内克的工坊 / Karunerk's Workshop
	private int craftItemId;
	private int craftItemCount;
	// 塔基的冒险 / Taki's Adventure
	private int indun_id;
	// 穆尼伦克的宝藏 / Munirunerk's Treasure
	private HashMap<Integer, Long> munirunerk_treasure;

	private int isApply;
	private int applySlot;
	private int itemId;
	private int itemSize;
	private boolean success;
	private long itemCount;

	/**
	 * 通用动作构造，仅指定 actionId。
	 * Generic action constructor with action id only.
	 *
	 * action type
	 */
	public SM_LUNA_SHOP(int actionId) {
		this.actionId = actionId;
	}

	/**
	 * 卡鲁内克工坊（Karunerk's Workshop）制作结果。
	 * Karunerk's Workshop craft result.
	 *
	 * action type
	 * crafted item id
	 * crafted item count
	 * whether craft succeeded
	 */
	public SM_LUNA_SHOP(int actionId, int craftItemId, int craftItemCount, boolean success) {
		this.actionId = actionId;
		this.craftItemId = craftItemId;
		this.craftItemCount = craftItemCount;
		this.success = success;
	}

	/**
	 * 塔基冒险（Taki's Adventure）副本相关。
	 * Taki's Adventure instance-related payload.
	 *
	 * action type
	 * @param indun_id 副本/实例 ID / instance id
	 */
	public SM_LUNA_SHOP(int actionId, int indun_id) {
		this.actionId = actionId;
		this.indun_id = indun_id;
	}

	/**
	 * 穆尼鲁内克宝藏箱开启结果。
	 * Munirunerk's Treasure chest open result.
	 *
	 * @param munirunerk_treasure 奖励物品映射（物品 ID → 数量） / reward map (item id → count)
	 */
	public SM_LUNA_SHOP(HashMap<Integer, Long> munirunerk_treasure) {
		this.actionId = 12;
		this.munirunerk_treasure = munirunerk_treasure;
	}

	/**
	 * 多里内克衣柜（Dorinerk's Wardrobe）外观应用。
	 * Dorinerk's Wardrobe appearance apply.
	 *
	 * action type
	 * whether applied
	 * apply slot
	 * item id
	 * @param unk1 未知字段 / unknown field
	 */
	public SM_LUNA_SHOP(int actionId, int isApply, int applySlot, int itemId, int unk1) {
		this.actionId = actionId;
		this.isApply = isApply;
		this.applySlot = applySlot;
		this.itemId = itemId;
		this.unk1 = unk1;
	}

	/**
	 * 衣柜槽位/物品数量同步。
	 * Wardrobe slot and item size sync.
	 *
	 * action type
	 * slot size
	 * item size
	 */
	public SM_LUNA_SHOP(int actionId, int slotSize, int itemSize) {
		this.actionId = actionId;
		this.slotSize = slotSize;
		this.itemSize = itemSize;
	}

	/**
	 * 物品相关操作结果（含成功/失败标志）。
	 * Item-related operation result with success/fail flag.
	 *
	 * action type
	 * @param item 物品模板 / item template
	 * @param fail 失败标志（0 成功 / 1 失败） / fail flag (0 success / 1 fail)
	 */
	public SM_LUNA_SHOP(int actionId, ItemTemplate item, int fail) {
		this.actionId = actionId;
		this.item = item;
		this.fail = fail;
	}

	/**
	 * 物品奖励/展示（含数量）。
	 * Item reward/display payload with count.
	 *
	 * action type
	 * item id
	 * item count
	 */
	public SM_LUNA_SHOP(int actionId, int itemId, long itemCount) {
		this.actionId = actionId;
		this.itemId = itemId;
		this.itemCount = itemCount;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		Player player = con.getActivePlayer();
		writeC(actionId);
		switch (actionId) {
		case 0:
			writeC(0);
			writeD(indun_id);
			break;
		case 2:
			writeC(fail);
			switch (fail) {
			case 0:
				PacketSendUtility.sendPacket(player,
						new SM_SYSTEM_MESSAGE(1330049, new DescriptionId(item.getNameId())));// Success
				break;
			case 1:
				PacketSendUtility.sendPacket(player,
						new SM_SYSTEM_MESSAGE(1330050, new DescriptionId(item.getNameId())));// Fail
				break;
			}
			break;
		case 3:
			writeC(success ? 0 : 1);// Success = 0 Fail = 1
			writeH(1);// unk 0x01
			writeD(craftItemId);// productid
			writeQ(craftItemCount);// quantity
			break;
		case 4:
			writeC(0);
			break;
		case 5:
			writeC(0);
			writeC(0);
			writeC(0);
			break;
		case 6:
			writeD(53);
			break;
		case 7:
			writeD(55);
			break;
		case 8:// dorinerk'swardrobe
			writeC(0x00);
			writeC(slotSize);
			writeH(itemSize);
			for (int i = 0; i < itemSize; i++) {
				for (PlayerWardrobeEntry ce : player.getWardrobe().getAllWardrobe()) {
					writeC(ce.getSlot());
					writeD(ce.getItemId());
					writeD(0x00);
					writeD(0x01);
				}
			}
			break;
		case 9:
			writeC(0x00);
			writeC(slotSize); // Also possible = writeH(slotSize * 256)
			break;
		case 10:
			writeC(isApply);
			writeC(applySlot);
			writeD(itemId);
			writeD(unk1);
			break;
		case 11:
			writeC(0x00);
			writeC(indun_id);
			writeD(0x01);
			break;
		case 12:// open chest
			writeC(0);// 未知 / unk
			writeH(3);// size always 3
			for (Map.Entry<Integer, Long> e : munirunerk_treasure.entrySet()) {
				writeD(e.getKey());
				writeQ(e.getValue());
			}
			break;
		case 14:
			writeC(1); // free enter = 1
			writeD(indun_id);
			break;
		case 15:
			int dice = player.getLunaDiceGame();
			writeC(0);
			writeH(dice);
			writeC(0);
			break;
		case 16:
			writeC(0);
			writeH(1);
			writeD(itemId); // ItemId
			writeQ(itemCount); // Item Count
			break;
		}
	}
}

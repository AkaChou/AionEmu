package com.aionemu.gameserver.network.aion.serverpackets;

import lombok.extern.slf4j.Slf4j;
import java.util.List;

import com.aionemu.gameserver.model.templates.item.DisassembleItem;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
/**
 * 向客户端展示可选物品列表（分解/选择奖励窗口）。
 * Server packet presenting a selectable item list (disassemble/reward picker) to the client.
 */
@Slf4j

public class SM_SELECT_ITEM extends AionServerPacket {
	private int uniqueItemId;
	private List<DisassembleItem> selsetitems;

	/**
	 * 使用给定参数构造 SM_SELECT_ITEM 包。
	 * Creates a SM_SELECT_ITEM packet with the given parameters.
	 *
	 * @param selsetitem 可选物品列表 / selectable items
	 * unique item id
	 */
	public SM_SELECT_ITEM(List<DisassembleItem> selsetitem, int uniqueItemId) {
		this.uniqueItemId = uniqueItemId;
		this.selsetitems = selsetitem;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(this.uniqueItemId);
		writeD(0x00);
		writeC(this.selsetitems.size());
		for (int slotCount = 0; slotCount < selsetitems.size(); slotCount++) {
			writeC(slotCount);
			DisassembleItem rt = this.selsetitems.get(slotCount);
			ItemTemplate itemTemplate = DataManager.ITEM_DATA.getItemTemplate(rt.getItemId());
			writeD(rt.getItemId());
			writeD(rt.getCount());
			writeC(itemTemplate.getOptionSlotBonus() > 0 ? 255 : 0);
			writeC(itemTemplate.getMaxEnchantBonus() > 0 ? 255 : 0);
			if ((itemTemplate.isArmor()) || (itemTemplate.isWeapon())) {
				writeC(-1);
			} else {
				writeC(0);
			}
			if ((itemTemplate.isCloth()) || (itemTemplate.getOptionSlotBonus() > 0)
					|| (itemTemplate.getMaxEnchantBonus() > 0)) {
				writeC(1);
			} else {
				writeC(0);
			}
		}
	}
}

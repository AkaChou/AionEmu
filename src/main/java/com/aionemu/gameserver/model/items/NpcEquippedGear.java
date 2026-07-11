package com.aionemu.gameserver.model.items;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.loadingutils.adapters.NpcEquipmentList;
import com.aionemu.gameserver.dataholders.loadingutils.adapters.NpcEquippedGearAdapter;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;

/**
 * NpcEquippedGear，用于物品相关逻辑。
 * Npc Equipped Gear for items logic.
 *
 * @author Luno
 */
@XmlJavaTypeAdapter(NpcEquippedGearAdapter.class)
public class NpcEquippedGear implements Iterable<Entry<ItemSlot, ItemTemplate>> {

	private Map<ItemSlot, ItemTemplate> items;
	private short mask;

	private NpcEquipmentList v;

	public NpcEquippedGear(NpcEquipmentList v) {
		this.v = v;
	}

	/**
	 * @return short
	 */
	public short getItemsMask() {
		if (items == null) {
			init();
		}
		return mask;
	}

	/** 返回迭代器。 / Returns iterator. */
	@Override
	public Iterator<Entry<ItemSlot, ItemTemplate>> iterator() {
		if (items == null) {
			init();
		}
		return items.entrySet().iterator();
	}

	/**
	 * Here NPC equipment mask is initialized. All NPC slot masks should be lower than 65536
	 */
	public void init() {
		synchronized (this) {
			if (items == null) {
				items = new TreeMap<ItemSlot, ItemTemplate>();
				int[] itemIds = v.itemIds != null ? v.itemIds : new int[0];
				for (int itemId : itemIds) {
					ItemTemplate item = DataManager.ITEM_DATA.getItemTemplate(itemId);
					if (item == null) {
						continue;
					}
					ItemSlot[] itemSlots = ItemSlot.getSlotsFor(item.getItemSlot());
					for (ItemSlot itemSlot : itemSlots) {
						if (items.get(itemSlot) == null) {
							items.put(itemSlot, item);
							mask |= itemSlot.getSlotIdMask();
							break;
						}
					}
				}
			}
			v = null;
		}
	}

	/**
	 * @param itemSlot
	 * @return
	 */
	public ItemTemplate getItem(ItemSlot itemSlot) {
		return items != null ? items.get(itemSlot) : null;
	}
}

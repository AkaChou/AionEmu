package com.aionemu.gameserver.network.aion.iteminfo;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Set;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.items.ManaStone;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob.ItemBlobType;

/**
 * 合成/融合物品信息 Blob。
 * 写入融合物品 ID 及其魔石数据（古代石优先于普通石）。
 * Blob for composite/fusioned item info.
 * Writes the fusioned item id and its mana stones (ancient stones before basic ones).
 *
 * @author -Nemesiss-
 * @modified Rolandas
 */
public class CompositeItemBlobEntry extends ItemBlobEntry {

	/**
	 * 构造合成物品 Blob 条目。
	 * Constructs a composite-item blob entry.
	 */
	CompositeItemBlobEntry() {
		super(ItemBlobType.COMPOSITE_ITEM);
	}

	/**
	 * 将本 Blob 的具体内容写入缓冲区。
	 * Writes this blob's concrete payload into the buffer.
	 */
	@Override
	public void writeThisBlob(ByteBuffer buf) {
		Item item = ownerItem;
		writeD(buf, item.getFusionedItemId());
		writeFusionStones(buf);
		writeH(buf, 0);
	}

	/**
	 * 写入融合物品上的魔石列表。
	 * Writes the mana stones socketed on the fusioned item.
	 */
	private void writeFusionStones(ByteBuffer buf) {
		Item item = ownerItem;
		int count = 0;
		if (item.hasFusionStones()) {
			Set<ManaStone> itemStones = item.getFusionStones();
			ArrayList<ManaStone> basicStones = new ArrayList<ManaStone>();
			ArrayList<ManaStone> ancientStones = new ArrayList<ManaStone>();
			for (ManaStone itemStone : itemStones) {
				if (itemStone.isBasic()) {
					basicStones.add(itemStone);
				} else {
					ancientStones.add(itemStone);
				}
			}
			if (item.getFusionedItemTemplate().getSpecialSlots() > 0) {
				if (ancientStones.size() > 0) {
					for (ManaStone ancientStone : ancientStones) {
						if (count == 6) {
							break;
						}
						writeD(buf, ancientStone.getItemId());
						count++;
					}
				}
				for (int i = count; i < item.getFusionedItemTemplate().getSpecialSlots(); i++) {
					writeD(buf, 0);
					count++;
				}
			}
			for (ManaStone basicFusionStone : basicStones) {
				if (count == 6) {
					break;
				}
				writeD(buf, basicFusionStone.getItemId());
				count++;
			}
			skip(buf, (6 - count) * 4);
		} else {
			skip(buf, 24);
		}
	}

	/**
	 * 返回本 Blob 负载的字节长度。
	 * Returns the payload size of this blob in bytes.
	 */
	@Override
	public int getSize() {
		return 30;
	}
}

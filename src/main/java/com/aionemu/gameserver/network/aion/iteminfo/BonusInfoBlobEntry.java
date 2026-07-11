package com.aionemu.gameserver.network.aion.iteminfo;

import java.nio.ByteBuffer;

import com.aionemu.gameserver.configs.administration.DeveloperConfig;
import com.aionemu.gameserver.model.stats.calc.functions.StatRateFunction;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob.ItemBlobType;

/**
 * 物品属性加成 Blob。
 * 写入属性类型、数值及是否为比率修正。
 * Blob for item stat bonuses.
 * Writes stat type, value, and whether the modifier is rate-based.
 *
 * @author Rolandas
 */
public class BonusInfoBlobEntry extends ItemBlobEntry {

	/**
	 * 构造属性加成 Blob 条目。
	 * Constructs a stat-bonus blob entry.
	 */
	public BonusInfoBlobEntry() {
		super(ItemBlobType.STAT_BONUSES);
	}

	/**
	 * 将本 Blob 的具体内容写入缓冲区。
	 * Writes this blob's concrete payload into the buffer.
	 */
	@Override
	public void writeThisBlob(ByteBuffer buf) {
		if (DeveloperConfig.ITEM_STAT_ID > 0) {
			writeH(buf, DeveloperConfig.ITEM_STAT_ID);
			writeD(buf, 10);
			writeC(buf, 0);
		} else {
			writeH(buf, modifier.getName().getItemStoneMask());
			writeD(buf, modifier.getValue() * modifier.getName().getSign());
			writeC(buf, modifier instanceof StatRateFunction ? 1 : 0);
		}
	}

	/**
	 * 返回本 Blob 负载的字节长度。
	 * Returns the payload size of this blob in bytes.
	 */
	@Override
	public int getSize() {
		return 7;
	}
}

package com.aionemu.gameserver.network.aion.iteminfo;

import java.nio.ByteBuffer;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.templates.item.Stigma;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob.ItemBlobType;

/**
 * 尘晶（Stigma）技能信息 Blob。
 * 写入尘晶关联技能 ID 与所需碎片数量。
 * Blob containing stigma skill info.
 * Writes linked skill ids and required shard count.
 *
 * @author -Nemesiss-
 * @modified Rolandas
 */
public class StigmaInfoBlobEntry extends ItemBlobEntry {

	/**
	 * 构造尘晶信息 Blob 条目。
	 * Constructs a stigma-info blob entry.
	 */
	StigmaInfoBlobEntry() {
		super(ItemBlobType.STIGMA_INFO);
	}

	/**
	 * 将本 Blob 的具体内容写入缓冲区。
	 * Writes this blob's concrete payload into the buffer.
	 */
	@Override
	public void writeThisBlob(ByteBuffer buf) {
		Item item = ownerItem;
		Stigma stigma = item.getItemTemplate().getStigma();

		writeD(buf, stigma.getSkills().get(0).getSkillId());// skill id 1
		if (stigma.getSkills().size() >= 2) {
			writeD(buf, stigma.getSkills().get(1).getSkillId());// skill id 2
		} else {
			writeD(buf, 0);
		}
		writeD(buf, stigma.getShard());

		skip(buf, 192);
		writeH(buf, 0x1); // 未知 / unk
		writeH(buf, 0);
		skip(buf, 96);
		writeH(buf, 0); // 未知 / unk
	}

	/**
	 * 返回本 Blob 负载的字节长度。
	 * Returns the payload size of this blob in bytes.
	 */
	@Override
	public int getSize() {
		return 8 + 4 + 192 + 4 + 96 + 2;
	}
}

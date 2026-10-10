package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob.ItemBlobType;
import lombok.AllArgsConstructor;

/**
 * 重新鉴定预览服务端包，向客户端发送预览后的物品信息。
 * Server packet sending the reidentify preview to the client.
 * <p>属性取暂存值（{@link Item#getPreviewReidentifySocket()} 等），
 * 使客户端在确认前显示待定结果，取消时仍能回退到原属性。
 * Attribute values come from the staged preview accessors so the client shows the pending
 * result before confirmation and can still fall back on cancel.</p>
 */
@AllArgsConstructor
public class SM_TUNE_RESULT extends AionServerPacket {
	private final Player player;
	private final Item item;
	private final int tuningScrollId;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(item.getObjectId());
		writeD(tuningScrollId);
		writeC(item.getPreviewReidentifyBonus());
		ItemInfoBlob.newPreviewManaSocketsEntry(player, item, item.getPreviewReidentifySocket(),
				item.getPreviewReidentifyEnchantBonus()).writeThisBlob(getBuf());
		writeC(0);
		writeC(0);
	}
}

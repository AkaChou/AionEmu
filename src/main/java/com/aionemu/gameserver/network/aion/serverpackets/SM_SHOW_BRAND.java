package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端显示队伍/联盟标记（Brand）。
 * Server packet displaying a party/alliance brand marker on the client.
 *
 * @author Sweetkr
 */
public class SM_SHOW_BRAND extends AionServerPacket {

	private int brandId;
	private int targetObjectId;

	/**
	 * 使用给定参数构造 SM_SHOW_BRAND 包。
	 * Creates a SM_SHOW_BRAND packet with the given parameters.
	 *
	 * brand id
	 * target object id
	 */
	public SM_SHOW_BRAND(int brandId, int targetObjectId) {
		this.brandId = brandId;
		this.targetObjectId = targetObjectId;
	}

	@Override
	protected void writeImpl(AionConnection con) {

		writeH(0x01);
		writeD(0x01); // 未知 / unk
		writeD(brandId);
		writeD(targetObjectId);
	}
}

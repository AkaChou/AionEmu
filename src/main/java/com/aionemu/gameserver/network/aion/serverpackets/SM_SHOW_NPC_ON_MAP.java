package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端在地图上高亮显示指定 NPC 位置。
 * Server packet highlighting an NPC position on the client map.
 * @author Lyahim
 */
@AllArgsConstructor
public class SM_SHOW_NPC_ON_MAP extends AionServerPacket {

	private final int npcid;
	private final int worldid;
	private final float x;
	private final float y;
	private final float z;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(this.npcid);
		writeD(this.worldid);
		writeD(this.worldid);
		writeF(this.x);
		writeF(this.y);
		writeF(this.z);
	}
}

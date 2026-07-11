package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端在地图上高亮显示指定 NPC 位置。
 * Server packet highlighting an NPC position on the client map.
 *
 * @author Lyahim
 */
public class SM_SHOW_NPC_ON_MAP extends AionServerPacket {

	private int npcid, worldid;
	private float x, y, z;

	/**
	 * 使用给定参数构造 SM_SHOW_NPC_ON_MAP 包。
	 * Creates a SM_SHOW_NPC_ON_MAP packet with the given parameters.
	 *
	 * NPC 模板 ID / npc template id
	 * world map id
	 * @param x X 坐标 / x coordinate
	 * @param y Y 坐标 / y coordinate
	 * @param z Z 坐标 / z coordinate
	 */
	public SM_SHOW_NPC_ON_MAP(int npcid, int worldid, float x, float y, float z) {
		this.npcid = npcid;
		this.worldid = worldid;
		this.x = x;
		this.y = y;
		this.z = z;
	}

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

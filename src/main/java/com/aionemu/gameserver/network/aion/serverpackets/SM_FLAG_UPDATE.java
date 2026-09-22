package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 战场旗帜更新包：同步旗帜 NPC 模板 ID 与对象 ID。
 * Battlefield flag update packet: flag NPC template id and object id.
 * @author wanke
 */
@AllArgsConstructor
public class SM_FLAG_UPDATE extends AionServerPacket {

	Npc npc;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(npc.getNpcId());
		writeD(npc.getObjectId());
	}
}

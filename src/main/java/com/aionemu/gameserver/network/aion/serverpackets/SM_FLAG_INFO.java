package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 战场旗帜信息包：同步旗帜 NPC 的血量与坐标。
 * Battlefield flag info packet: flag NPC HP and position.
 *
 * @author Ace
 */
public class SM_FLAG_INFO extends AionServerPacket {
	int count;
	private Creature _npc;
	private NpcTemplate npcTemplate;
	private int npcId;

	/**
	 * 按旗帜序号与 NPC 构造信息包。
	 * Creates a flag info packet for the given index and flag NPC.
	 *
	 * @param count 旗帜序号 / flag index
	 * @param npc 旗帜 NPC，可为空 / flag NPC, may be null
	 */
	public SM_FLAG_INFO(int count, Npc npc) {
		this.count = count;
		this._npc = npc;
		npcId = npc.getNpcId();
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeH(count);
		if (_npc != null) {
			writeD(npcId);
			writeD(_npc.getObjectId());
			writeD(_npc.getLifeStats().getCurrentHp());
			writeD(_npc.getLifeStats().getMaxHp());
			writeF(_npc.getX());
			writeF(_npc.getY());
			writeF(_npc.getZ());
		} else {
			writeD(0);
			writeD(0);
			writeD(0);
			writeD(0);
			writeF(0);
			writeF(0);
			writeF(0);
		}
	}
}

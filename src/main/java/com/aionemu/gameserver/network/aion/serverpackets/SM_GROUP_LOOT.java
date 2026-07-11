package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端发送小队拾取/掷骰相关信息的服务端包。
 * roll information to the client. / roll information to the client.
 */
public class SM_GROUP_LOOT extends AionServerPacket {
	private int groupId;
	private int index;
	private int unk2;
	private int itemId;
	private int unk3;
	private int lootCorpseId;
	private int distributionId;
	private int playerId;
	private long luck;

	/**
	 * @param groupId 小队 ID；为 0 时启动掷骰选项 / Group ID; 0 starts the roll options
	 * Related player ID
	 * Item template ID
	 * @param lootCorpseId 掉落尸体对象 ID / Loot corpse object ID
	 * Distribution mode ID
	 * Luck value
	 * @param index 拾取索引 / Loot index
	 */
	public SM_GROUP_LOOT(int groupId, int playerId, int itemId, int lootCorpseId, int distributionId, long luck,
			int index) {
		this.groupId = groupId;
		this.index = index;
		this.unk2 = 1;
		this.itemId = itemId;
		this.unk3 = 0;
		this.lootCorpseId = lootCorpseId;
		this.distributionId = distributionId;
		this.playerId = playerId;
		this.luck = luck;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(groupId);
		writeD(index);
		writeD(unk2);
		writeD(itemId);
		writeC(unk3);
		writeC(0); // 3.0
		writeC(0); // 3.5
		writeD(lootCorpseId);
		writeC(distributionId);
		writeD(playerId);
		writeD((int) luck);
	}
}

package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * NPC/玩家情绪状态包：同步对象 ID、状态与情绪类型。
 * NPC/player emotion state packet: object id, state and emotion type.
 */
public class SM_EMOTION_NPC extends AionServerPacket {
	private final Creature npc;
	private final int state;
	private final int emotionType;

	/**
	 * 构造 NPC 情绪状态包。
	 * Creates an NPC emotion state packet.
	 * @param npc 目标 NPC / target NPC
	 * @param state 动画状态 / animation state
	 * @param et 情绪类型 / emotion type
	 */
	public SM_EMOTION_NPC(Creature npc, int state, EmotionType et) {
		this.npc = npc;
		this.state = state;
		this.emotionType = et.getTypeId();
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(npc.getObjectId());
		writeC(state);
		writeD(emotionType);
		writeD(0);
	}
}

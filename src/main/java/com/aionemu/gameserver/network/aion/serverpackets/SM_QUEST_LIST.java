package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.List;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.questEngine.model.QuestState;

/**
 * 向客户端同步当前进行中的任务列表。
 * Server packet synchronizing the active quest list to the client.
 */
public class SM_QUEST_LIST extends AionServerPacket {
	private List<QuestState> questState;

	/**
	 * 使用给定参数构造 SM_QUEST_LIST 包。
	 * Creates a SM_QUEST_LIST packet with the given parameters.
	 *
	 * @param questState 任务状态列表 / quest state list
	 */
	public SM_QUEST_LIST(List<QuestState> questState) {
		this.questState = questState;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeH(0x01);
		writeH(-questState.size() & 0xFFFF);
		for (QuestState qs : questState) {
			writeD(qs.getQuestId());
			writeC(qs.getStatus().value());
			writeD(qs.getQuestVars().getQuestVars());
			writeC(qs.getCompleteCount());
		}
		questState = null;
	}
}

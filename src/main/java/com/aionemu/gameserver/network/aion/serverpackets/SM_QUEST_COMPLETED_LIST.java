package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.List;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.questEngine.model.QuestState;

/**
 * 向客户端同步已完成任务列表（支持分包发送）。
 * Server packet synchronizing the completed-quest list to the client (chunked when large).
 */
public class SM_QUEST_COMPLETED_LIST extends AionServerPacket {

    private static final int MAX_PACKET_SIZE = 8000;

    private static final int QUEST_SIZE = 12;

    private final List<QuestState> allQuests;
    private final int startIndex;
    private final int totalSize;

    /**
     * 使用给定参数构造 SM_QUEST_COMPLETED_LIST 包。
     * Creates a SM_QUEST_COMPLETED_LIST packet with the given parameters.
     *
     * @param allQuests 任务状态列表 / quest state list
     */
    public SM_QUEST_COMPLETED_LIST(List<QuestState> allQuests) {
        this(allQuests, 0, allQuests.size());
    }

    private SM_QUEST_COMPLETED_LIST(List<QuestState> allQuests, int startIndex, int totalSize) {
        this.allQuests = allQuests;
        this.startIndex = startIndex;
        this.totalSize = totalSize;
    }

    @Override
    protected void writeImpl(AionConnection con) {
        int maxQuestsThisPacket = (MAX_PACKET_SIZE - 4) / QUEST_SIZE;

        int endIndex = Math.min(startIndex + maxQuestsThisPacket, totalSize);
        int chunkSize = endIndex - startIndex;

        writeC(1);
        writeC(startIndex == 0 ? 0 : 1);
        writeH(-totalSize & 0xFFFF);

        int index = 0;
        for (QuestState qs : allQuests) {
            if (index >= startIndex && index < endIndex) {
                writeD(qs.getQuestId());
                writeD(qs.getCompleteCount());
                writeD(1);
            }
            index++;
        }

        if (endIndex < totalSize) {
            SM_QUEST_COMPLETED_LIST nextPart = new SM_QUEST_COMPLETED_LIST(allQuests, endIndex, totalSize);
            con.sendPacket(nextPart);
        }
    }
}

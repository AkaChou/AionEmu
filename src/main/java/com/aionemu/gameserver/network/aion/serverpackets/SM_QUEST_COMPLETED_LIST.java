package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.questEngine.model.QuestState;

import javolution.util.FastList;

public class SM_QUEST_COMPLETED_LIST extends AionServerPacket {

    private static final int MAX_PACKET_SIZE = 8000;

    private static final int QUEST_SIZE = 12;

    private final FastList<QuestState> allQuests;
    private final int startIndex;
    private final int totalSize;

    public SM_QUEST_COMPLETED_LIST(FastList<QuestState> allQuests) {
        this(allQuests, 0, allQuests.size());
    }

    private SM_QUEST_COMPLETED_LIST(FastList<QuestState> allQuests, int startIndex, int totalSize) {
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
        } else {
            FastList.recycle(allQuests);
        }
    }
}
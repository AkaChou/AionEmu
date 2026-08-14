package com.aionemu.chatserver.model.channel;

import com.aionemu.chatserver.model.ChannelType;
import com.aionemu.chatserver.model.Race;

/**
 * 组队招募（LFG）聊天频道。
 * Looking-for-group chat channel.
 *
 * @author ATracer
 */
public class LfgChannel extends RaceChannel {

    /**
     * 创建组队招募频道。
     * Creates a looking-for-group channel.
     *
     * @param race 阵营 / race
     * @param identifier 字符串标识 / string identifier
     */
    public LfgChannel(Race race, String identifier) {
        super(ChannelType.GROUP, race, identifier);
    }
}

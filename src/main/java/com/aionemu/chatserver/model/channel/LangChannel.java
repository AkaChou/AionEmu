package com.aionemu.chatserver.model.channel;

import com.aionemu.chatserver.model.ChannelType;
import com.aionemu.chatserver.model.Race;

/**
 * 语言聊天频道。
 * Language chat channel.
 */
public class LangChannel extends RaceChannel {

    /**
     * 创建语言频道。
     * Creates a language channel.
     *
     * @param race 阵营 / race
     * @param identifier 字符串标识 / string identifier
     */
    public LangChannel(Race race, String identifier) {
        super(ChannelType.LANG, race, identifier);
    }
}

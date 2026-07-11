package com.aionemu.chatserver.model.channel;

import com.aionemu.chatserver.model.ChannelType;
import com.aionemu.chatserver.model.Race;

/**
 * 交易聊天频道。
 * Trade chat channel.
 *
 * @author ATracer
 */
public class TradeChannel extends RaceChannel {

    /**
     * 创建交易频道。
     * Creates a trade channel.
     *
     * 阵营 / race
     * @param identifier 字符串标识 / string identifier
     */
    public TradeChannel(Race race, String identifier) {
        super(ChannelType.TRADE, race, identifier);
    }
}

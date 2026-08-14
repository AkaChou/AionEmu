package com.aionemu.chatserver.model.channel;

import com.aionemu.chatserver.model.ChannelType;
import com.aionemu.chatserver.model.Race;
import lombok.Getter;

/**
 * 按种族划分的频道抽象基类。
 * Abstract base for race-scoped chat channels.
 *
 * @author ATracer
 */
public abstract class RaceChannel extends Channel {

    /**
     * 频道所属种族。
     * Race this channel belongs to.
     */
    @Getter
    protected Race race;

    /**
     * 创建种族频道。
     * Creates a race-scoped channel.
     *
     * @param channelType 频道类型 / channel type
     * @param race 阵营 / race
     * @param identifier 字符串标识 / string identifier
     */
    public RaceChannel(ChannelType channelType, Race race, String identifier) {
        super(channelType, identifier);
        this.race = race;
    }

}

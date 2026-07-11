package com.aionemu.chatserver.model.channel;

import com.aionemu.chatserver.model.ChannelType;
import com.aionemu.chatserver.model.Race;
import lombok.Getter;

/**
 * 按地图区域划分的公共聊天频道。
 * Public chat channel scoped by world map region.
 *
 * @author ATracer
 */
public class RegionChannel extends RaceChannel {

    /**
     * 地图 ID。
     * Map id.
     */
    @Getter
    protected int mapId;

    /**
     * 创建区域公共频道。
     * Creates a region public channel.
     *
     * map id
     * 阵营 / race
     * @param identifier 字符串标识 / string identifier
     */
    public RegionChannel(int mapId, Race race, String identifier) {
        super(ChannelType.PUBLIC, race, identifier);
        this.mapId = mapId;
    }

}

package com.aionemu.chatserver.model.channel;

import com.aionemu.chatserver.model.ChannelType;
import com.aionemu.chatserver.model.Gender;
import com.aionemu.chatserver.model.PlayerClass;
import com.aionemu.chatserver.model.Race;
import lombok.Getter;

/**
 * 按职业与性别划分的聊天频道。
 * Job-and-gender scoped chat channel.
 *
 * @author ATracer
 */
public class JobChannel extends RaceChannel {

    /**
     * 职业。
     * Player class.
     */
    @Getter
    private PlayerClass playerClass;
    /**
     * 性别。
     * Gender.
     */
    @Getter
    private Gender gender;

    /**
     * 创建职业频道。
     * Creates a job channel.
     *
     * gender
     * player class
     * 阵营 / race
     * @param identifier 字符串标识 / string identifier
     */
    public JobChannel(Gender gender, PlayerClass playerClass, Race race, String identifier) {
        super(ChannelType.JOB, race, identifier);
        this.playerClass = playerClass;
        this.gender = gender;
    }

}

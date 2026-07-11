package com.aionemu.chatserver.model;

import lombok.Getter;

/**
 * 角色阵营（种族）枚举。
 * Player race (faction) enumeration.
 *
 * @author ATracer
 */
public enum Race {

    /**
     * 天族 / Elyos
     */
    ELYOS(0),
    /**
     * 魔族 / Asmodians
     */
    ASMODIANS(1);

    /**
     * 种族 ID。
     * Race identifier.
     */
    @Getter
    private int raceId;

    /**
     * 构造种族枚举。
     * Constructs a race enum value.
     *
     * race id
     */
    private Race(int raceId) {
        this.raceId = raceId;
    }

}

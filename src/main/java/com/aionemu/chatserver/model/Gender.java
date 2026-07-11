package com.aionemu.chatserver.model;

import lombok.Getter;

/**
 * 角色性别枚举。
 * Player gender enumeration.
 *
 * @author ATracer
 */
public enum Gender {

    /**
     * 男性 / Male
     */
    MALE(0),
    /**
     * 女性 / Female
     */
    FEMALE(1);

    /**
     * 性别 ID。
     * Gender identifier.
     */
    @Getter
    private int genderId;

    /**
     * 构造性别枚举。
     * Constructs a gender enum value.
     *
     * gender id
     */
    private Gender(int genderId) {
        this.genderId = genderId;
    }

}

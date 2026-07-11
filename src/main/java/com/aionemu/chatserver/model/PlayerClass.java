package com.aionemu.chatserver.model;

import lombok.Getter;

/**
 * 玩家职业枚举。
 * Player class enumeration.
 *
 * @author ATracer
 */
public enum PlayerClass {

    /**
     * 战士 / Warrior
     */
    WARRIOR(0),
    /**
     * 剑星 / Gladiator
     */
    GLADIATOR(1),
    /**
     * 守护星 / Templar
     */
    TEMPLAR(2),
    /**
     * 斥候 / Scout
     */
    SCOUT(3),
    /**
     * 杀星 / Assassin
     */
    ASSASSIN(4),
    /**
     * 弓星 / Ranger
     */
    RANGER(5),
    /**
     * 法师 / Mage
     */
    MAGE(6),
    /**
     * 魔道星 / Sorcerer
     */
    SORCERER(7),
    /**
     * 精灵星 / Spirit Master
     */
    SPIRIT_MASTER(8),
    /**
     * 祭司 / Priest
     */
    PRIEST(9),
    /**
     * 治愈星 / Cleric
     */
    CLERIC(10),
    /**
     * 护法星 / Chanter
     */
    CHANTER(11),
    /**
     * 工程师 / Engineer
     */
    ENGINEER(12),
    /**
     * 机甲星 / Rider (Aethertech)
     */
    RIDER(13),
    /**
     * 枪炮星 / Gunner
     */
    GUNNER(14),
    /**
     * 艺术家 / Artist
     */
    ARTIST(15),
    /**
     * 吟游星 / Bard
     */
    BARD(16),
    /**
     * 全部职业 / All classes
     */
    ALL(17);

    /**
     * 职业 ID。
     * Class identifier.
     */
    @Getter
    private byte classId;

    /**
     * 构造职业枚举。
     * Constructs a player class enum value.
     *
     * class id
     */
    private PlayerClass(int classId) {
        this.classId = (byte) classId;
    }

}

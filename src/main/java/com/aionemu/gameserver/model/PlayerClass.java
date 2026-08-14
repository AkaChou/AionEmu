package com.aionemu.gameserver.model;

import lombok.Getter;

import jakarta.xml.bind.annotation.XmlEnum;

import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 玩家职业枚举。
 * Player Class enumeration.
 */

@XmlEnum
public enum PlayerClass {
	/** 战士（起始职业） / Warrior (starting class). */
	WARRIOR(0, true),
	/** 剑星 / Gladiator */
	GLADIATOR(1),
	/** 守护星 / Templar */
	TEMPLAR(2),
	/** 斥候（起始职业） / Scout (starting class) */
	SCOUT(3, true),
	/** 杀星 / Assassin */
	ASSASSIN(4),
	/** 弓星 / Ranger */
	RANGER(5),
	/** 法师（起始职业） / Mage (starting class) */
	MAGE(6, true),
	/** 魔道星 / Sorcerer */
	SORCERER(7),
	/** 精灵星 / Spirit Master */
	SPIRIT_MASTER(8),
	/** 祭司（起始职业） / Priest (starting class) */
	PRIEST(9, true),
	/** 治愈星 / Cleric */
	CLERIC(10),
	/** 护法星 / Chanter */
	CHANTER(11),

	// 资讯类 4.3/4.5 / News Class 4.3/4.5
	/** 技师（起始职业） / Technist (starting class). */
	TECHNIST(12, true),
	/** 机甲星 / Aethertech */
	AETHERTECH(13),
	/** 枪炮星 / Gunslinger */
	GUNSLINGER(14),
	/** 缪斯（起始职业） / Muse (starting class) */
	MUSE(15, true),
	/** 吟游星 / Songweaver */
	SONGWEAVER(16),
	/** 全部 / All */
	ALL(17);

	@Getter
	private byte classId;
	private int idMask;
	@Getter
	private boolean startingClass;

	private PlayerClass(int classId) {
		this(classId, false);
	}

	private PlayerClass(int classId, boolean startingClass) {
		this.classId = (byte) classId;
		this.startingClass = startingClass;
		this.idMask = (int) Math.pow(2, classId);
	}

	/** 返回按 ID 的玩家职业 / Returns the player class by id */
	public static PlayerClass getPlayerClassById(byte classId) {
		for (PlayerClass pc : values()) {
			if (pc.getClassId() == classId) {
				return pc;
			}
		}
		throw new IllegalArgumentException("There is no player class with id " + classId);
	}

	/** 返回初始职业 / Returns the starting class for*/
	public static PlayerClass getStartingClassFor(PlayerClass pc) {
		switch (pc) {
		case ASSASSIN:
		case RANGER:
			return SCOUT;
		case GLADIATOR:
		case TEMPLAR:
			return WARRIOR;
		case CHANTER:
		case CLERIC:
			return PRIEST;
		case SORCERER:
		case SPIRIT_MASTER:
			return MAGE;
		// 资讯类 4.3/4.5 / News Class 4.3/4.5
		case SONGWEAVER:
			return MUSE;
		case AETHERTECH:
		case GUNSLINGER:
			return TECHNIST;
		case SCOUT:
		case WARRIOR:
		case PRIEST:
		case MAGE:
		case MUSE:
		case TECHNIST:
			return pc;
		default:
			throw new IllegalArgumentException("Given player class is starting class: " + pc);
		}
	}

	/** 按字符串返回玩家职业 / Returns the player class by string*/
	public static PlayerClass getPlayerClassByString(String fieldName) {
		for (PlayerClass pc : values()) {
			if (pc.toString().equals(fieldName)) {
				return pc;
			}
		}
		return null;
	}

	/** 获取掩码。 / Returns the mask. */
	public int getMask() {
		return idMask;
	}

	/** 获取职业类型。 / Returns the class type. */
	public String getClassType(Player player) {
		String type = null;
		switch (player.getPlayerClass()) {
		case TEMPLAR:
		case ASSASSIN:
		case RANGER:
		case GLADIATOR:
		case GUNSLINGER:
			type = "PHYSICAL";
			break;
		case SORCERER:
		case CHANTER:
		case CLERIC:
		case SPIRIT_MASTER:
		case SONGWEAVER:
		case AETHERTECH:
			type = "MAGICAL";
			break;
		default:
			break;
		}
		return type;
	}
}

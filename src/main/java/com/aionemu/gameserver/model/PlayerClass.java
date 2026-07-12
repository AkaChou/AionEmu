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
	/** 战士 / Warrior. */
	WARRIOR(0, true), GLADIATOR(1), TEMPLAR(2), SCOUT(3, true), ASSASSIN(4), RANGER(5), MAGE(6, true), SORCERER(7),
	/** 精灵星 / Spirit Master */
	SPIRIT_MASTER(8), PRIEST(9, true), CLERIC(10), CHANTER(11),

	// 资讯类 4.3/4.5 / News Class 4.3/4.5
	/** 技师 / Technist. */
	TECHNIST(12, true), AETHERTECH(13), GUNSLINGER(14), MUSE(15, true), SONGWEAVER(16), ALL(17);

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

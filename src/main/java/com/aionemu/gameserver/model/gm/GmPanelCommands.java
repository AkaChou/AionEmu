package com.aionemu.gameserver.model.gm;

/**
 * GM 面板 Commands 枚举。
 * Gm Panel Commands enumeration.
 *
 * @author Ever' - Magenik
 */
public enum GmPanelCommands {

	/**
	 * @标准功能页签
	 * @STANDARD FUNCTION TAB
	 */
	REMOVE_SKILL_DELAY_ALL, ITEMCOOLTIME, CLEARUSERCOOLT, SET_MAKEUP_BONUS, SET_VITALPOINT, SET_DISABLE_ITEMUSE_GAUGE,
	/** 小队召回 / Partyrecall. */
	PARTYRECALL, ATTRBONUS, TELEPORTTO, RESURRECT, INVISIBLE, VISIBLE,
	/**
	 * @角色设置页签
	 * @CHARACTER SETTING TAB
	 */
	LEVELDOWN, LEVELUP, CHANGECLASS, CLASSUP, DELETECQUEST, ADDQUEST, ENDQUEST, SETINVENTORYGROWTH, SKILLPOINT,
	/** 合成技能 / Combineskill. */
	COMBINESKILL, ADDSKILL, DELETESKILL, GIVETITLE,
	/**
	 * @综合功能页签
	 * @OVERALL FUNCTION TAB
	 */
	ENCHANT100, FREEFLY,
	/**
	 * @NPC 任务物品页签
	 * @NPC QUEST ITEM TAB
	 */
	TELEPORT_TO_NAMED, WISH, WISHID, DELETE_ITEMS, SET_ENCHANTCOUNT,
	/**
	 * @玩家信息
	 * @PLAYER INFO
	 */
	BOOKMARK_ADD, SEARCH;

	/** 获取值。 / Returns the value. */
	public static GmPanelCommands getValue(String command) {
		for (GmPanelCommands value : values()) {
			if (value.name().equals(command.toUpperCase())) {
				return value;
			}
		}
		throw new IllegalArgumentException("Invalid GmPanelCommands id: " + command);
	}
}

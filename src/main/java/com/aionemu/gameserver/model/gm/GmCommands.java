package com.aionemu.gameserver.model.gm;

/**
 * GmCommands 枚举。
 * Gm Commands enumeration.
 */

public enum GmCommands {
	/** Gm Dialog Teleportto / Gm Dialog Teleportto */
	GM_DIALOG_TELEPORTTO, GM_DIALOG_RECALL, GM_DIALOG, GM_DIALOG_POS, GM_DIALOG_MEMO, GM_DIALOG_BOOKMARK,
	/** Gm Dialog Inventory / Gm Dialog Inventory */
	GM_DIALOG_INVENTORY, GM_DIALOG_SKILL, GM_DIALOG_STATUS, GM_DIALOG_QUEST, GM_DIALOG_REFRESH, GM_DIALOG_WAREHOUSE,
	/** Gm Dialog Mail / Gm Dialog Mail */
	GM_DIALOG_MAIL, GM_POLL_DIALOG, GM_POLL_DIALOG_SUBMIT, GM_BOOKMARK_DIALOG, GM_BOOKMARK_DIALOG_ADD_BOOKMARK,
	/** Gm Memo Dialog / Gm Memo Dialog */
	GM_MEMO_DIALOG, GM_MEMO_DIALOG_ADD_MEMO, GM_DIALOG_CHECK_BOT1, GM_DIALOG_CHECK_BOT99,
	/** Gm Indicator Dialog Tooltip Housing Mode / Gm Indicator Dialog Tooltip Housing Mode */
	GM_INDICATOR_DIALOG_TOOLTIP_HOUSING_MODE, GM_DIALOG_CHARACTER, GM_DIALOG_OPTION, GM_DIALOG_BUILDER_CONTROL,
	/** Gm Dialog Builder Command / Gm Dialog Builder Command */
	GM_DIALOG_BUILDER_COMMAND;

	/** 获取值。 / Returns the value. */
	public static GmCommands getValue(String command) {
		for (GmCommands value : values()) {
			if (value.name().equals(command.toUpperCase())) {
				return value;
			}
		}
		throw new IllegalArgumentException("Invalid GmCommands id: " + command);
	}
}

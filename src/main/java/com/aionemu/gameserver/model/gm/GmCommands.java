package com.aionemu.gameserver.model.gm;

/**
 * GmCommands 枚举。
 * Gm Commands enumeration.
 */

public enum GmCommands {
	/** GM 对话框：传送到 / Gm Dialog Teleportto */
	GM_DIALOG_TELEPORTTO, GM_DIALOG_RECALL, GM_DIALOG, GM_DIALOG_POS, GM_DIALOG_MEMO, GM_DIALOG_BOOKMARK,
	/** GM 对话框：物品栏 / Gm Dialog Inventory */
	GM_DIALOG_INVENTORY, GM_DIALOG_SKILL, GM_DIALOG_STATUS, GM_DIALOG_QUEST, GM_DIALOG_REFRESH, GM_DIALOG_WAREHOUSE,
	/** GM 对话框：邮件 / Gm Dialog Mail */
	GM_DIALOG_MAIL, GM_POLL_DIALOG, GM_POLL_DIALOG_SUBMIT, GM_BOOKMARK_DIALOG, GM_BOOKMARK_DIALOG_ADD_BOOKMARK,
	/** GM 备忘录对话框 / Gm Memo Dialog */
	GM_MEMO_DIALOG, GM_MEMO_DIALOG_ADD_MEMO, GM_DIALOG_CHECK_BOT1, GM_DIALOG_CHECK_BOT99,
	/** GM 指示对话框提示：房屋模式 / Gm Indicator Dialog Tooltip Housing Mode */
	GM_INDICATOR_DIALOG_TOOLTIP_HOUSING_MODE, GM_DIALOG_CHARACTER, GM_DIALOG_OPTION, GM_DIALOG_BUILDER_CONTROL,
	/** GM 对话框：建造者命令 / Gm Dialog Builder Command */
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

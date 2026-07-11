package com.aionemu.gameserver.configs.administration;

import com.aionemu.commons.configuration.Property;

/**
 * GM 管理面板各功能所需的最低权限等级配置。
 * Minimum access-level requirements for GM administration panel features.
 *
 * @author Hoo
 */
public class PanelConfig {

	/**
	 * 可使用技能面板的最低权限等级。
	 * Minimum access level for the skill panel.
	 */
	@Property(key = "gameserver.administration.skilpanel", defaultValue = "3")
	public static int SKILL_PANEL_LEVEL;

	/**
	 * 可使用删除任务面板的最低权限等级。
	 * Minimum access level for the delete-quest panel.
	 */
	@Property(key = "gameserver.administration.delquestpanel", defaultValue = "3")
	public static int DELQUEST_PANEL_LEVEL;

	/**
	 * 可使用完成任务面板的最低权限等级。
	 * Minimum access level for the end-quest panel.
	 */
	@Property(key = "gameserver.administration.endquestpanel", defaultValue = "3")
	public static int ENDQUEST_PANEL_LEVEL;

	/**
	 * 可使用授予称号面板的最低权限等级。
	 * Minimum access level for the give-title panel.
	 */
	@Property(key = "gameserver.administration.givetitlepanel", defaultValue = "3")
	public static int GIVETITLE_PANEL_LEVEL;

	/**
	 * 可使用开始任务面板的最低权限等级。
	 * Minimum access level for the start-quest panel.
	 */
	@Property(key = "gameserver.administration.startquestpanel", defaultValue = "3")
	public static int STARTQUEST_PANEL_LEVEL;
}

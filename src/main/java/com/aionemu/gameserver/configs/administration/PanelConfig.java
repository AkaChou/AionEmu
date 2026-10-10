package com.aionemu.gameserver.configs.administration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * GM 管理面板各功能所需的最低权限等级配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Minimum access-level requirements for GM administration panel features supporting Spring Boot ConfigurationProperties and static facade access.
 * @author Hoo
 */
@Component
@ConfigurationProperties(prefix = "gameserver")
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

	@Getter
	private final AdministrationProperties administration = new AdministrationProperties();

	@Getter
	public static class AdministrationProperties {
		public int getSkilpanel() {
			return SKILL_PANEL_LEVEL;
		}

		public void setSkilpanel(int skilpanel) {
			SKILL_PANEL_LEVEL = skilpanel;
		}

		public int getDelquestpanel() {
			return DELQUEST_PANEL_LEVEL;
		}

		public void setDelquestpanel(int delquestpanel) {
			DELQUEST_PANEL_LEVEL = delquestpanel;
		}

		public int getEndquestpanel() {
			return ENDQUEST_PANEL_LEVEL;
		}

		public void setEndquestpanel(int endquestpanel) {
			ENDQUEST_PANEL_LEVEL = endquestpanel;
		}

		public int getGivetitlepanel() {
			return GIVETITLE_PANEL_LEVEL;
		}

		public void setGivetitlepanel(int givetitlepanel) {
			GIVETITLE_PANEL_LEVEL = givetitlepanel;
		}

		public int getStartquestpanel() {
			return STARTQUEST_PANEL_LEVEL;
		}

		public void setStartquestpanel(int startquestpanel) {
			STARTQUEST_PANEL_LEVEL = startquestpanel;
		}
	}
}

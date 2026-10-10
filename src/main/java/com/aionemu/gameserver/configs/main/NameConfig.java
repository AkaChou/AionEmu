package com.aionemu.gameserver.configs.main;

import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * 角色名称校验与禁用词相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Character name validation and forbidden word related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 */
@Component
@ConfigurationProperties(prefix = "gameserver")
public class NameConfig {

	/**
	 * 是否允许自定义名称规则。
	 * Whether custom name rules are allowed.
	 */
	@Property(key = "gameserver.name.allow.custom", defaultValue = "false")
	public static boolean ALLOW_CUSTOM_NAMES;

	/**
	 * 角色名称正则表达式。
	 * Regular expression pattern for character names.
	 */
	@Property(key = "gameserver.name.characterpattern", defaultValue = "[a-zA-Z]{2,16}")
	public static Pattern CHAR_NAME_PATTERN;

	/**
	 * 名称中禁止出现的字符序列。
	 * Forbidden character sequences in names.
	 */
	@Property(key = "gameserver.name.forbidden.sequences", defaultValue = "")
	public static String NAME_SEQUENCE_FORBIDDEN;

	/**
	 * 是否启用客户端禁用名称列表。
	 * Whether client forbidden name list is enabled.
	 */
	@Property(key = "gameserver.name.forbidden.enable.client", defaultValue = "true")
	public static boolean NAME_FORBIDDEN_ENABLE;

	/**
	 * 客户端禁用名称列表内容。
	 * Client forbidden name list content.
	 */
	@Property(key = "gameserver.name.forbidden.client", defaultValue = "")
	public static String NAME_FORBIDDEN_CLIENT;

	@Getter
	private final NameProperties name = new NameProperties();

	@Getter
	public static class NameProperties {
		private final AllowProperties allow = new AllowProperties();
		private final ForbiddenProperties forbidden = new ForbiddenProperties();

		public String getCharacterpattern() {
			return CHAR_NAME_PATTERN != null ? CHAR_NAME_PATTERN.pattern() : "";
		}

		public void setCharacterpattern(String characterpattern) {
			CHAR_NAME_PATTERN = Pattern.compile(characterpattern);
		}

		public static class AllowProperties {
			public boolean isCustom() {
				return ALLOW_CUSTOM_NAMES;
			}

			public void setCustom(boolean custom) {
				ALLOW_CUSTOM_NAMES = custom;
			}
		}

		@Getter
		public static class ForbiddenProperties {
			private final EnableProperties enable = new EnableProperties();

			public String getSequences() {
				return NAME_SEQUENCE_FORBIDDEN;
			}

			public void setSequences(String sequences) {
				NAME_SEQUENCE_FORBIDDEN = sequences;
			}

			public String getClient() {
				return NAME_FORBIDDEN_CLIENT;
			}

			public void setClient(String client) {
				NAME_FORBIDDEN_CLIENT = client;
			}

			public static class EnableProperties {
				public boolean isClient() {
					return NAME_FORBIDDEN_ENABLE;
				}

				public void setClient(boolean client) {
					NAME_FORBIDDEN_ENABLE = client;
				}
			}
		}
	}
}

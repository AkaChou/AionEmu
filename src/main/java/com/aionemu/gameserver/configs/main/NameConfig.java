package com.aionemu.gameserver.configs.main;

import java.util.regex.Pattern;

import com.aionemu.commons.configuration.Property;

/**
 * 角色名称校验与禁用词相关配置。
 * Character name validation and forbidden word related configuration.
 */
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
}

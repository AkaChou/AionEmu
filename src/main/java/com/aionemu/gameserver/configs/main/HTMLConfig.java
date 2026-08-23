package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * HTML 欢迎页与缓存相关配置。
 * HTML welcome and cache related configuration.
 */
public class HTMLConfig {
	/**
	 * 是否启用欢迎 HTML。
	 * Whether welcome HTML is enabled.
	 */
	@Property(key = "gameserver.html.welcome.enable", defaultValue = "false")
	public static boolean ENABLE_HTML_WELCOME;
	/**
	 * 是否启用新手指引 HTML。
	 * Whether guide HTML pages are enabled.
	 */
	@Property(key = "gameserver.html.guides.enable", defaultValue = "true")
	public static boolean ENABLE_GUIDES;
	/**
	 * HTML 静态资源根目录。
	 * HTML static resources root directory.
	 */
	@Property(key = "gameserver.html.root", defaultValue = "./data/static_data/HTML/")
	public static String HTML_ROOT;
	/**
	 * HTML 文件编码。
	 * HTML file encoding.
	 */
	@Property(key = "gameserver.html.encoding", defaultValue = "UTF-8")
	public static String HTML_ENCODING;
}
